package com.bsdcampus.app.data

import com.bsdcampus.app.base.FirestoreEmulatorTestBase
import com.bsdcampus.app.model.CampusConstants
import com.bsdcampus.app.model.CampusPost
import com.bsdcampus.app.model.HighlightMediaItem
import com.bsdcampus.app.model.NoteBubbleColor
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class CampusRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun registerProfile_and_createPost_succeedForAuthenticatedStudent() = runBlocking {
        val uid = signInTestUser("alice_${UUID.randomUUID()}@bsd.edu")
        val repo = CampusRepository(firestore, auth)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.registerProfile(
                name = "Alice Student",
                email = "alice@bsd.edu",
                role = "student",
                grade = "Class 10",
                avatarUrl = null
            )
        }
        assertTrue("Profile creation should succeed: ${profileResult.exceptionOrNull()}", profileResult.isSuccess)
        val user = profileResult.getOrThrow()

        val postResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createPost(
                author = user,
                isAdmin = false,
                content = "Excited for BSD Annual Science Fair!",
                category = "Events",
                images = emptyList(),
                isOfficial = false,
                isPinned = false
            )
        }
        assertTrue("Post creation should succeed: ${postResult.exceptionOrNull()}", postResult.isSuccess)

        val posts = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePosts().first { list -> list.any { it.id == postResult.getOrThrow() } }
        }
        assertTrue(posts.any { it.authorId == uid && it.category == "Events" })
    }

    @Test
    fun normalUserNamedSuperAdmin_cannotAccessAdminOrDeleteOfficialAnnouncement() = runBlocking {
        val imposterUid = signInTestUser("imposter_${UUID.randomUUID()}@bsd.edu")
        val repo = CampusRepository(firestore, auth)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.registerProfile(
                name = CampusConstants.SUPER_ADMIN_NAME,
                email = "imposter@bsd.edu",
                role = "student",
                grade = "Class 10",
                avatarUrl = null
            )
        }
        assertTrue("Student profile creation should succeed", profileResult.isSuccess)
        val user = profileResult.getOrThrow()
        assertEquals("student", user.role)
        assertFalse("Display name alone must never grant admin privilege", user.isAdminUser)

        val isAdminMarker = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeIsAdminMarker(imposterUid).first()
        }
        assertFalse("Normal user must not have /admins marker", isAdminMarker)

        val officialPost = CampusPost(
            id = "official_${UUID.randomUUID()}",
            authorId = "real_admin_uid",
            authorName = CampusConstants.SUPER_ADMIN_NAME,
            authorRole = "admin",
            content = "Official Squadify Holiday Notice",
            category = "Announcements",
            isOfficial = true,
            isPinned = true
        )
        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.deletePost(officialPost, isAdmin = false)
        }
        assertTrue("Non-admin deletePost on official announcement must fail", deleteResult.isFailure)

        // Even directly via Firestore SDK, attempting to create an admin post fails rules
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("posts").document("bad_${UUID.randomUUID()}")
                    .set(officialPost.copy(authorId = imposterUid).toCreateMap())
                    .await()
            }
            fail("Non-admin must not be allowed to publish official/admin post")
        } catch (e: Exception) {
            val firestoreEx = e as? FirebaseFirestoreException ?: e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun observePosts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = CampusRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observePosts().first()
            }
            fail("Expected failure when unauthenticated")
        } catch (e: Exception) {
            assertTrue(
                e is IllegalStateException ||
                    e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
        }
    }

    @Test
    fun crossUserPrivateInfo_and_directMessageIsolation_failWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_priv_${UUID.randomUUID()}@bsd.edu")
        val aliceRepo = CampusRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.registerProfile("Alice Priv", "alice_priv@bsd.edu", "student", "Class 11", null).getOrThrow()
        }

        signInTestUser("bob_priv_${UUID.randomUUID()}@bsd.edu")
        val bobRepo = CampusRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observePrivateInfo(aliceUid).first()
            }
            fail("Bob must not read Alice's private PII doc")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun submitReport_createsPendingReport_forReporterOnly() = runBlocking {
        val reporterUid = signInTestUser("rep_${UUID.randomUUID()}@bsd.edu")
        val repo = CampusRepository(firestore, auth)

        val res = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitReport(
                targetType = "post",
                targetId = "post_abc",
                targetSummary = "Spam message",
                reason = "Spam or unauthorized promotional content",
                details = "Reported in test"
            )
        }
        assertTrue("Report submission should succeed", res.isSuccess)

        val reports = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeReports(isAdmin = false, uid = reporterUid).first { it.isNotEmpty() }
        }
        assertTrue(reports.any { it.reporterId == reporterUid && it.status == "pending" })
    }

    @Test
    fun adminModeration_verifySuspendClassRep_and_unlimitedMediaWorkEndToEnd() = runBlocking {
        val aliceEmail = "alice_mod_${UUID.randomUUID()}@bsd.edu"
        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = CampusRepository(firestore, auth)
        val alice = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.registerProfile("Alice Rep", aliceEmail, "student", "Class 10", null).getOrThrow()
        }

        // Sign in as Super Admin on Auth emulator and verify email token for Firestore rule isSuperAdminEmail()
        signInTestUser(CampusConstants.SUPER_ADMIN_EMAIL)
        markCurrentAuthUserVerifiedForEmulator()
        val adminRepo = CampusRepository(firestore, auth)
        val adminUser = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.registerProfile(CampusConstants.SUPER_ADMIN_NAME, CampusConstants.SUPER_ADMIN_EMAIL, "admin", null, null).getOrThrow()
        }
        assertTrue(adminUser.isAdminUser)

        // 1. Grant Blue Tick (isVerified)
        val verifyRes = withTimeout(DEFAULT_TIMEOUT_MS) { adminRepo.toggleVerifyUser(aliceUid, true) }
        assertTrue("toggleVerifyUser(true) should succeed: ${verifyRes.exceptionOrNull()}", verifyRes.isSuccess)
        val verifiedUsers = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeProfiles().first { list -> list.firstOrNull { it.uid == aliceUid }?.isVerified == true }
        }
        assertTrue(verifiedUsers.first { it.uid == aliceUid }.isVerified)

        // 2. Assign Class 10 Rep -> grants rep + keeps verified; Revoking Class Rep keeps manual Blue Tick intact
        val repRes = withTimeout(DEFAULT_TIMEOUT_MS) { adminRepo.assignClassRepresentative(aliceUid, 10, true) }
        assertTrue("assignClassRepresentative(true) should succeed: ${repRes.exceptionOrNull()}", repRes.isSuccess)
        val repProfile = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeProfiles().first { list ->
                val u = list.firstOrNull { it.uid == aliceUid }
                u?.isClassRepresentative == true && u.classRepresentativeGrade == 10
            }.first { it.uid == aliceUid }
        }
        assertTrue(repProfile.isVerified)

        val revokeRepRes = withTimeout(DEFAULT_TIMEOUT_MS) { adminRepo.assignClassRepresentative(aliceUid, 10, false) }
        assertTrue("assignClassRepresentative(false) should succeed", revokeRepRes.isSuccess)
        val revokedProfile = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeProfiles().first { list ->
                list.firstOrNull { it.uid == aliceUid }?.isClassRepresentative == false
            }.first { it.uid == aliceUid }
        }
        assertTrue("Revoking Class Rep should not wipe existing Blue Tick verification", revokedProfile.isVerified)

        // 3. Suspend user
        val suspendRes = withTimeout(DEFAULT_TIMEOUT_MS) { adminRepo.toggleSuspendUser(aliceUid, true) }
        assertTrue("toggleSuspendUser(true) should succeed: ${suspendRes.exceptionOrNull()}", suspendRes.isSuccess)

        // 4. Admin can publish >5 media items in a single post
        val manyMedia = (1..12).map { "bsdvideo://vid_$it" }
        val adminPostRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.createPost(adminUser, isAdmin = true, content = "Annual Day Highlights", category = "Events", images = manyMedia, isOfficial = true, isPinned = true)
        }
        assertTrue("Admin >5 media post should succeed: ${adminPostRes.exceptionOrNull()}", adminPostRes.isSuccess)
        val savedPost = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observePosts().first { list -> list.any { it.id == adminPostRes.getOrThrow() } }
                .first { it.id == adminPostRes.getOrThrow() }
        }
        assertEquals(12, savedPost.images.size)

        // 5. While Alice is suspended, creating a post as Alice is denied by Firestore rules
        signInTestUser(aliceEmail)
        val blockedPost = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createPost(alice, isAdmin = false, content = "Suspended post attempt", category = "General", images = emptyList(), isOfficial = false, isPinned = false)
        }
        assertTrue("Suspended user post must fail", blockedPost.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            blockedPost.exceptionOrNull()?.unwrapFirestoreException()?.code
        )
    }

    @Test
    fun stories_followOnlyGetAccess_markViewed_and_authorDelete_workEndToEnd() = runBlocking {
        val authorEmail = "story_author_${UUID.randomUUID()}@bsd.edu"
        val authorUid = signInTestUser(authorEmail)
        val authorRepo = CampusRepository(firestore, auth)
        val author = withTimeout(DEFAULT_TIMEOUT_MS) {
            authorRepo.registerProfile("Story Author", authorEmail, "student", "Class 12", null).getOrThrow()
        }

        val storyId = withTimeout(DEFAULT_TIMEOUT_MS) {
            authorRepo.createStory(
                author = author,
                isAdmin = false,
                mediaUrl = "bsdmedia://sample_story_1",
                mediaType = "image",
                caption = "Lab Robotics Demo"
            ).getOrThrow()
        }

        val viewerEmail = "story_viewer_${UUID.randomUUID()}@bsd.edu"
        val viewerUid = signInTestUser(viewerEmail)
        val viewerRepo = CampusRepository(firestore, auth)
        val viewer = withTimeout(DEFAULT_TIMEOUT_MS) {
            viewerRepo.registerProfile("Story Viewer", viewerEmail, "student", "Class 12", null).getOrThrow()
        }

        // 1. Public profile: non-follower can directly read author's story (for opening from author's profile)
        val publicStoryDoc = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("stories").document(storyId).get().await()
        }
        assertTrue("Public profile story should be readable by non-follower", publicStoryDoc.exists())

        // 2. Private profile: direct get on private author's story by non-follower must fail with PERMISSION_DENIED
        signInTestUser(authorEmail)
        val existingPrivate = withTimeout(FLOW_TIMEOUT_MS) {
            authorRepo.observePrivateInfo(authorUid).first { it != null }!!
        }
        val privRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            authorRepo.savePrivateSettings(
                existingPrivate.copy(isPrivateAccount = true)
            )
        }
        assertTrue("Setting author account private should succeed: ${privRes.exceptionOrNull()}", privRes.isSuccess)

        signInTestUser(viewerEmail)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("stories").document(storyId).get().await()
            }
            fail("Non-follower should not be able to directly read a private account's story")
        } catch (e: Exception) {
            val firestoreEx = e as? FirebaseFirestoreException ?: e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }

        // After following, viewer can read the private author's story, see it in observeStories, and mark it viewed
        val followRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            viewerRepo.toggleFollow(targetUserId = authorUid, currentlyFollowing = false, actor = viewer)
        }
        assertTrue("toggleFollow should succeed: ${followRes.exceptionOrNull()}", followRes.isSuccess)

        val fetchedDoc = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("stories").document(storyId).get().await()
        }
        assertTrue("Follower should be able to read story", fetchedDoc.exists())

        val storiesList = withTimeout(FLOW_TIMEOUT_MS) {
            viewerRepo.observeStories().first { list -> list.any { it.id == storyId } }
        }
        val story = storiesList.first { it.id == storyId }
        val markViewedRes = withTimeout(DEFAULT_TIMEOUT_MS) { viewerRepo.markStoryViewed(story) }
        assertTrue("markStoryViewed should succeed: ${markViewedRes.exceptionOrNull()}", markViewedRes.isSuccess)

        val updatedStory = withTimeout(FLOW_TIMEOUT_MS) {
            viewerRepo.observeStories().first { list ->
                list.firstOrNull { it.id == storyId }?.viewedBy?.contains(viewerUid) == true
            }.first { it.id == storyId }
        }
        assertTrue(updatedStory.viewedBy.contains(viewerUid))

        // Non-author cannot delete someone else's story
        val forbiddenDelete = withTimeout(DEFAULT_TIMEOUT_MS) {
            viewerRepo.deleteStory(updatedStory, isAdmin = false)
        }
        assertTrue("Non-author deleteStory must fail", forbiddenDelete.isFailure)

        // Author can delete their own story
        signInTestUser(authorEmail)
        val authorDelete = withTimeout(DEFAULT_TIMEOUT_MS) {
            authorRepo.deleteStory(updatedStory, isAdmin = false)
        }
        assertTrue("Author deleteStory should succeed: ${authorDelete.exceptionOrNull()}", authorDelete.isSuccess)
    }

    @Test
    fun storyLikes_highlightsWithCustomAvatar_and_messageReply_workEndToEnd() = runBlocking {
        val ownerEmail = "hl_owner_${UUID.randomUUID()}@bsd.edu"
        val ownerUid = signInTestUser(ownerEmail)
        val ownerRepo = CampusRepository(firestore, auth)
        val owner = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.registerProfile("Aarav Highlight", ownerEmail, "student", "Class 11", null).getOrThrow()
        }

        // 1. Create a highlight with a custom cover avatar
        val hlId = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.createHighlight(
                title = "Annual Fest",
                coverAvatar = "bsdmedia://custom_avatar_1",
                coverEmoji = "🏆",
                items = listOf(HighlightMediaItem(mediaUrl = "bsdmedia://fest_1", mediaType = "image", caption = "Stage!"))
            ).getOrThrow()
        }
        val highlights = withTimeout(FLOW_TIMEOUT_MS) {
            ownerRepo.observeHighlights().first { list -> list.any { it.id == hlId } }
        }
        val createdHl = highlights.first { it.id == hlId }
        assertEquals("bsdmedia://custom_avatar_1", createdHl.coverAvatar)
        assertEquals("Annual Fest", createdHl.title)

        // Update highlight title & custom cover
        val updateHlRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.updateHighlight(
                highlightId = hlId,
                title = "Fest 2026",
                coverAvatar = "bsdmedia://custom_avatar_2",
                coverEmoji = null,
                items = createdHl.items
            )
        }
        assertTrue("updateHighlight should succeed: ${updateHlRes.exceptionOrNull()}", updateHlRes.isSuccess)

        // Owner creates a story
        val storyId = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.createStory(owner, isAdmin = false, mediaUrl = "bsdmedia://story_like_1", mediaType = "image", caption = "Practice!")
                .getOrThrow()
        }

        // Peer signs in, follows owner, likes the story, and sends a reply message quoting a DM
        val peerEmail = "hl_peer_${UUID.randomUUID()}@bsd.edu"
        val peerUid = signInTestUser(peerEmail)
        val peerRepo = CampusRepository(firestore, auth)
        val peer = withTimeout(DEFAULT_TIMEOUT_MS) {
            peerRepo.registerProfile("Riya Peer", peerEmail, "student", "Class 11", null).getOrThrow()
        }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            peerRepo.toggleFollow(targetUserId = ownerUid, currentlyFollowing = false, actor = peer).getOrThrow()
        }

        // Peer cannot delete owner's highlight
        val forbiddenHlDelete = withTimeout(DEFAULT_TIMEOUT_MS) { peerRepo.deleteHighlight(hlId) }
        assertTrue("Non-owner cannot delete highlight", forbiddenHlDelete.isFailure)

        // Peer likes owner's story
        val story = withTimeout(FLOW_TIMEOUT_MS) {
            peerRepo.observeStories().first { list -> list.any { it.id == storyId } }.first { it.id == storyId }
        }
        val likeStoryRes = withTimeout(DEFAULT_TIMEOUT_MS) { peerRepo.toggleLikeStory(story, peer) }
        assertTrue("toggleLikeStory should succeed: ${likeStoryRes.exceptionOrNull()}", likeStoryRes.isSuccess)
        val likedStory = withTimeout(FLOW_TIMEOUT_MS) {
            peerRepo.observeStories().first { list ->
                list.firstOrNull { it.id == storyId }?.likedBy?.contains(peerUid) == true
            }.first { it.id == storyId }
        }
        assertTrue(likedStory.likedBy.contains(peerUid))

        // Peer sends a DM and then quotes it with replyToMessage
        withTimeout(DEFAULT_TIMEOUT_MS) {
            peerRepo.sendMessage(recipientId = ownerUid, content = "Original photo question").getOrThrow()
        }
        val originalMsg = withTimeout(FLOW_TIMEOUT_MS) {
            peerRepo.observeMessages(peerUid).first { list -> list.any { it.content == "Original photo question" } }
                .first { it.content == "Original photo question" }
        }
        val replyRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            peerRepo.sendMessage(
                recipientId = ownerUid,
                content = "Replying right here!",
                replyToMessage = originalMsg,
                replyToSenderName = "Riya Peer"
            )
        }
        assertTrue("sendMessage reply should succeed: ${replyRes.exceptionOrNull()}", replyRes.isSuccess)
        val replyMsg = withTimeout(FLOW_TIMEOUT_MS) {
            peerRepo.observeMessages(peerUid).first { list -> list.any { it.replyToMessageId == originalMsg.id } }
                .first { it.replyToMessageId == originalMsg.id }
        }
        assertEquals("Original photo question", replyMsg.replyToContent)
        assertEquals("text", replyMsg.replyToMediaType)

        // Owner deletes highlight cleanly
        signInTestUser(ownerEmail)
        val deleteHlRes = withTimeout(DEFAULT_TIMEOUT_MS) { ownerRepo.deleteHighlight(hlId) }
        assertTrue("Owner deleteHighlight should succeed: ${deleteHlRes.exceptionOrNull()}", deleteHlRes.isSuccess)
    }

    @Test
    fun reelsCrudAndComments_and_repostPostOrReelToStory_workEndToEnd() = runBlocking {
        val aliceEmail = "alice_reel_${UUID.randomUUID()}@bsd.edu"
        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = CampusRepository(firestore, auth)
        val alice = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.registerProfile("Alice Reel", aliceEmail, "student", "Class 12", null).getOrThrow()
        }

        // 1. Alice publishes a text-only post (9:16 ratio) and a reel (16:9 ratio, then updated to Original 'free')
        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createPost(
                author = alice,
                isAdmin = false,
                content = "BSD Debate Finals at 3 PM!",
                category = "Events",
                images = emptyList(),
                imageAspectRatio = "9:16",
                isOfficial = false,
                isPinned = false
            ).getOrThrow()
        }

        val reelId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createReel(
                author = alice,
                isAdmin = false,
                videoUrl = "bsdvideo://sample_reel_1",
                videoAspectRatio = "16:9",
                caption = "Robotics Club Bot Test",
                category = "Events",
                audioTitle = "Campus Beat"
            ).getOrThrow()
        }

        val createdReel = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeReels().first { list -> list.any { it.id == reelId } }
                .first { it.id == reelId }
        }
        assertEquals("Events", createdReel.category)
        assertEquals("Campus Beat", createdReel.audioTitle)
        assertEquals("16:9", createdReel.videoAspectRatio)

        val updateReelRatioRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.updateReel(
                reel = createdReel,
                isAdmin = false,
                newCaption = createdReel.caption,
                newCategory = createdReel.category,
                newAudioTitle = createdReel.audioTitle,
                newVideoAspectRatio = "free"
            )
        }
        assertTrue("updateReel ratio to 'free' should succeed: ${updateReelRatioRes.exceptionOrNull()}", updateReelRatioRes.isSuccess)
        val freeRatioReel = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeReels().first { list ->
                list.firstOrNull { it.id == reelId }?.videoAspectRatio == "free"
            }.first { it.id == reelId }
        }
        assertEquals("free", freeRatioReel.videoAspectRatio)

        // 2. Bob signs in, likes the reel, comments on it, and cannot delete Alice's reel
        val bobEmail = "bob_reel_${UUID.randomUUID()}@bsd.edu"
        val bobUid = signInTestUser(bobEmail)
        val bobRepo = CampusRepository(firestore, auth)
        val bob = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.registerProfile("Bob Reel", bobEmail, "student", "Class 12", null).getOrThrow()
        }

        val likeRes = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.toggleLikeReel(createdReel, bob) }
        assertTrue("toggleLikeReel should succeed: ${likeRes.exceptionOrNull()}", likeRes.isSuccess)

        val commentRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.addReelComment(createdReel, bob, "Awesome bot build!", isAdmin = false)
        }
        assertTrue("addReelComment should succeed: ${commentRes.exceptionOrNull()}", commentRes.isSuccess)

        val comments = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeReelComments(reelId).first { it.isNotEmpty() }
        }
        assertTrue(comments.any { it.authorId == bobUid && it.content == "Awesome bot build!" })

        val updatedReel = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeReels().first { list ->
                val r = list.firstOrNull { it.id == reelId }
                r?.likedBy?.contains(bobUid) == true && r.commentsCount == 1L
            }.first { it.id == reelId }
        }

        val forbiddenDelete = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.deleteReel(updatedReel, isAdmin = false)
        }
        assertTrue("Non-owner deleteReel must fail", forbiddenDelete.isFailure)

        // 3. Bob reposts both Alice's text-only post ("post_card") and Alice's reel ("video") to Bob's Story
        val repostPostStoryId = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.createStory(
                author = bob,
                isAdmin = false,
                mediaUrl = "shared_text_post://$postId",
                mediaType = "post_card",
                caption = "Everyone join!",
                sharedSourceType = "post",
                sharedSourceId = postId,
                sharedAuthorId = aliceUid,
                sharedAuthorName = "Alice Reel",
                sharedSourceCaption = "BSD Debate Finals at 3 PM!"
            ).getOrThrow()
        }

        val repostReelStoryId = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.createStory(
                author = bob,
                isAdmin = false,
                mediaUrl = "bsdvideo://sample_reel_1",
                mediaType = "video",
                caption = "Watch this reel!",
                sharedSourceType = "reel",
                sharedSourceId = reelId,
                sharedAuthorId = aliceUid,
                sharedAuthorName = "Alice Reel",
                sharedSourceCaption = "Robotics Club Bot Test"
            ).getOrThrow()
        }

        val bobStories = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeStories().first { list ->
                list.any { it.id == repostPostStoryId } && list.any { it.id == repostReelStoryId }
            }
        }
        val postCardStory = bobStories.first { it.id == repostPostStoryId }
        val reelStory = bobStories.first { it.id == repostReelStoryId }
        assertEquals("post_card", postCardStory.mediaType)
        assertEquals("post", postCardStory.sharedSourceType)
        assertEquals("video", reelStory.mediaType)
        assertEquals("reel", reelStory.sharedSourceType)

        // 4. Alice sees the repost notification and can delete her reel
        signInTestUser(aliceEmail)
        val aliceNotifs = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeNotifications(aliceUid).first { list ->
                list.any { it.title == "Shared to Story" }
            }
        }
        assertTrue(aliceNotifs.any { it.senderId == bobUid && it.type == "like" })

        val ownerDelete = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.deleteReel(updatedReel, isAdmin = false) }
        assertTrue("Owner deleteReel should succeed: ${ownerDelete.exceptionOrNull()}", ownerDelete.isSuccess)
    }

    @Test
    fun storyMusic_and_videoEdits_roundTripWithRules() = runBlocking {
        val email = "music_vid_${UUID.randomUUID()}@bsd.edu"
        signInTestUser(email)
        val repo = CampusRepository(firestore, auth)
        val user = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.registerProfile("Meera Music", email, "student", "Class 11", null).getOrThrow()
        }

        val overlayList = listOf(
            StoryOverlayItem(
                text = "Campus Fest!",
                xFraction = 0.32f,
                yFraction = 0.74f,
                scale = 1.45f,
                rotationDeg = -12f,
                color = StoryOverlayColor.PINK,
                style = StoryTextStyle.NEON,
                align = StoryTextAlign.LEFT
            ),
            StoryOverlayItem(
                text = "🎶",
                isEmoji = true,
                xFraction = 0.75f,
                yFraction = 0.25f,
                scale = 1.8f,
                rotationDeg = 18f
            )
        )
        val overlaysJson = StoryOverlayItem.serializeList(overlayList)
        val decodedOverlays = StoryOverlayItem.deserializeList(overlaysJson)
        assertEquals(2, decodedOverlays.size)
        assertEquals("Campus Fest!", decodedOverlays[0].text)
        assertEquals(0.32f, decodedOverlays[0].xFraction, 0.01f)
        assertEquals(StoryTextStyle.NEON, decodedOverlays[0].style)
        assertTrue(decodedOverlays[1].isEmoji)

        val track = StoryMusicTrack(
            trackId = 999L,
            title = "Kesariya",
            artist = "Arijit Singh",
            artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music112/300x300bb.jpg",
            previewUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview125/sample.m4a",
            durationMs = 30_000L
        )

        val storyId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createStory(
                author = user,
                isAdmin = false,
                mediaUrl = "bsdvideo://edited_vid_1",
                mediaType = "video",
                caption = "Edited vibe!",
                musicTrack = track,
                musicStartSec = 9,
                muteOriginalAudio = true,
                videoStartMs = 1500L,
                videoEndMs = 12500L,
                videoSpeed = 1.5,
                videoFilter = StoryFilterPreset.RETRO.name,
                videoOverlaysJson = overlaysJson
            ).getOrThrow()
        }

        val savedStory = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeStories().first { list -> list.any { it.id == storyId } }
                .first { it.id == storyId }
        }
        assertEquals("Kesariya", savedStory.musicTitle)
        assertEquals("Arijit Singh", savedStory.musicArtist)
        assertEquals(9, savedStory.musicStartSec)
        assertTrue(savedStory.muteOriginalAudio)
        assertEquals(1500L, savedStory.videoStartMs)
        assertEquals(12500L, savedStory.videoEndMs)
        assertEquals(1.5, savedStory.videoSpeed, 0.01)
        assertEquals("RETRO", savedStory.videoFilter)
        assertEquals(overlaysJson, savedStory.videoOverlaysJson)
    }

    @Test
    fun postAndReel_views_commentReplies_and_commentLikes_workEndToEnd() = runBlocking {
        val aliceEmail = "views_alice_${UUID.randomUUID()}@bsd.edu"
        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = CampusRepository(firestore, auth)
        val alice = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.registerProfile("Alice Views", aliceEmail, "student", "Class 12", null).getOrThrow()
        }

        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createPost(alice, isAdmin = false, content = "Lab Robotics Demo!", category = "Academic", images = emptyList(), isOfficial = false, isPinned = false).getOrThrow()
        }
        val reelId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createReel(alice, isAdmin = false, videoUrl = "bsdvideo://demo_reel_v1", caption = "Robotics Reel", category = "Academic", audioTitle = "Original audio").getOrThrow()
        }

        val bobEmail = "views_bob_${UUID.randomUUID()}@bsd.edu"
        val bobUid = signInTestUser(bobEmail)
        val bobRepo = CampusRepository(firestore, auth)
        val bob = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.registerProfile("Bob Views", bobEmail, "student", "Class 12", null).getOrThrow()
        }

        val post = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observePosts().first { list -> list.any { it.id == postId } }.first { it.id == postId }
        }
        val reel = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeReels().first { list -> list.any { it.id == reelId } }.first { it.id == reelId }
        }

        // Bob views both post and reel
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.markPostViewed(post) }.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.markReelViewed(reel) }.isSuccess)

        val viewedPost = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observePosts().first { list -> list.firstOrNull { it.id == postId }?.viewedBy?.contains(bobUid) == true }.first { it.id == postId }
        }
        assertTrue(viewedPost.viewedBy.contains(bobUid))
        val viewedReel = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeReels().first { list -> list.firstOrNull { it.id == reelId }?.viewedBy?.contains(bobUid) == true }.first { it.id == reelId }
        }
        assertTrue(viewedReel.viewedBy.contains(bobUid))

        // Bob comments on both, Alice replies and likes Bob's comments
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.addComment(viewedPost, bob, "Great robot!", isAdmin = false) }.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.addReelComment(viewedReel, bob, "Smooth movement!", isAdmin = false) }.isSuccess)

        val bobPostComment = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeComments(postId).first { it.isNotEmpty() }.first()
        }
        val bobReelComment = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeReelComments(reelId).first { it.isNotEmpty() }.first()
        }

        signInTestUser(aliceEmail)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.toggleLikeComment(postId, bobPostComment, alice) }.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.addComment(viewedPost, alice, "Thanks Bob!", isAdmin = false, replyToComment = bobPostComment) }.isSuccess)

        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.toggleLikeReelComment(reelId, bobReelComment, alice) }.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.addReelComment(viewedReel, alice, "Appreciate it!", isAdmin = false, replyToComment = bobReelComment) }.isSuccess)

        val allPostComments = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeComments(postId).first { list -> list.size >= 2 && list.any { it.likedBy.contains(aliceUid) } }
        }
        val likedParent = allPostComments.first { it.id == bobPostComment.id }
        val replyPostComment = allPostComments.first { it.replyToCommentId == bobPostComment.id }
        assertTrue(likedParent.likedBy.contains(aliceUid))
        assertEquals("Bob Views", replyPostComment.replyToAuthorName)
        assertEquals("Great robot!", replyPostComment.replyToContent)

        val allReelComments = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeReelComments(reelId).first { list -> list.size >= 2 && list.any { it.likedBy.contains(aliceUid) } }
        }
        val likedReelParent = allReelComments.first { it.id == bobReelComment.id }
        val replyReelComment = allReelComments.first { it.replyToCommentId == bobReelComment.id }
        assertTrue(likedReelParent.likedBy.contains(aliceUid))
        assertEquals("Bob Views", replyReelComment.replyToAuthorName)
        assertEquals("Smooth movement!", replyReelComment.replyToContent)
    }

    @Test
    fun chatNote_and_lastSeen_workEndToEnd() = runBlocking {
        val aliceEmail = "note_alice_${UUID.randomUUID()}@bsd.edu"
        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = CampusRepository(firestore, auth)
        val alice = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.registerProfile("Alice Note", aliceEmail, "student", "Class 11", null).getOrThrow()
        }

        // Update presence lastSeenAt
        val seenRes = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.updateLastSeen() }
        assertTrue("updateLastSeen should succeed: ${seenRes.exceptionOrNull()}", seenRes.isSuccess)
        val updatedProfile = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeProfiles().first { list ->
                list.firstOrNull { it.uid == aliceUid }?.lastSeenAt != null
            }.first { it.uid == aliceUid }
        }
        assertTrue(updatedProfile.lastSeenAt != null)

        // Share colored note with music
        val track = StoryMusicTrack(
            trackId = 777L,
            title = "Apna Bana Le",
            artist = "Arijit Singh",
            previewUrl = "https://audio-ssl.itunes.apple.com/preview_note.m4a",
            artworkUrl = "https://is1-ssl.mzstatic.com/art_note.jpg",
            durationMs = 30_000L
        )
        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertCampusNote(
                author = alice,
                text = "Library study mode",
                bubbleColor = NoteBubbleColor.DARK_PINK,
                musicTrack = track,
                musicStartSec = 6
            )
        }
        assertTrue("upsertCampusNote should succeed: ${saveRes.exceptionOrNull()}", saveRes.isSuccess)

        val savedNote = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeCampusNotes().first { list -> list.any { it.authorId == aliceUid } }
                .first { it.authorId == aliceUid }
        }
        assertEquals("Library study mode", savedNote.text)
        assertEquals("dark_pink", savedNote.bubbleColor)
        assertEquals("Apna Bana Le", savedNote.musicTitle)
        assertEquals(6, savedNote.musicStartSec)

        // Delete note
        val deleteRes = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.deleteCampusNote() }
        assertTrue("deleteCampusNote should succeed: ${deleteRes.exceptionOrNull()}", deleteRes.isSuccess)
        val notesAfterDelete = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeCampusNotes().first { list -> list.none { it.authorId == aliceUid } }
        }
        assertFalse(notesAfterDelete.any { it.authorId == aliceUid })
    }

    @Test
    fun deletePost_deletesPostDocumentAndOwnedMediaAssetsChunks() = runBlocking {
        val email = "del_media_${UUID.randomUUID()}@bsd.edu"
        val uid = signInTestUser(email)
        val repo = CampusRepository(firestore, auth)
        val user = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.registerProfile("Del Author", email, "student", "Class 12", null).getOrThrow()
        }

        val assetId = "vid_del_${UUID.randomUUID()}"
        val assetRef = firestore.collection("media_assets").document(assetId)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            assetRef.set(
                mapOf(
                    "ownerId" to uid,
                    "mimeType" to "video/mp4",
                    "chunkCount" to 1,
                    "totalBytes" to 4096,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
            assetRef.collection("chunks").document("chunk_0").set(
                mapOf(
                    "ownerId" to uid,
                    "index" to 0,
                    "data" to "AAAA"
                )
            ).await()
        }

        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createPost(
                author = user,
                isAdmin = false,
                content = "Video post to delete",
                category = "General",
                images = listOf("${MediaUtils.VIDEO_URI_PREFIX}$assetId"),
                isOfficial = false,
                isPinned = false
            ).getOrThrow()
        }

        val createdPost = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePosts().first { list -> list.any { it.id == postId } }.first { it.id == postId }
        }

        val delRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.deletePost(createdPost, isAdmin = false)
        }
        assertTrue("deletePost should succeed: ${delRes.exceptionOrNull()}", delRes.isSuccess)

        val postSnap = withTimeout(DEFAULT_TIMEOUT_MS) { firestore.collection("posts").document(postId).get().await() }
        val assetSnap = withTimeout(DEFAULT_TIMEOUT_MS) { assetRef.get().await() }
        val chunksSnap = withTimeout(DEFAULT_TIMEOUT_MS) { assetRef.collection("chunks").get().await() }
        assertFalse("Post document should be deleted from server", postSnap.exists())
        assertFalse("Owned media_assets document should be deleted to free cloud storage", assetSnap.exists())
        assertTrue("Owned media_assets chunks should be deleted", chunksSnap.isEmpty)
    }

    @Test
    fun updateOwnAvatar_deletesOldMediaAssetAndUpdatesOlderNotifications() = runBlocking {
        val aliceEmail = "avatar_alice_${UUID.randomUUID()}@bsd.edu"
        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = CampusRepository(firestore, auth)

        val oldAssetId = "avatar_old_${UUID.randomUUID()}"
        val oldAvatarUri = "${MediaUtils.IMAGE_ASSET_URI_PREFIX}$oldAssetId"
        val oldAssetRef = firestore.collection("media_assets").document(oldAssetId)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            oldAssetRef.set(
                mapOf(
                    "ownerId" to aliceUid,
                    "mimeType" to "image/jpeg",
                    "chunkCount" to 1,
                    "totalBytes" to 1024,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
            oldAssetRef.collection("chunks").document("chunk_0").set(
                mapOf(
                    "ownerId" to aliceUid,
                    "index" to 0,
                    "data" to "AAAA"
                )
            ).await()
        }

        val alice = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.registerProfile("Alice Avatar", aliceEmail, "student", "Class 12", oldAvatarUri).getOrThrow()
        }

        val bobEmail = "avatar_bob_${UUID.randomUUID()}@bsd.edu"
        val bobUid = signInTestUser(bobEmail)
        val bobRepo = CampusRepository(firestore, auth)
        val bob = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.registerProfile("Bob Target", bobEmail, "student", "Class 12", null).getOrThrow()
        }

        // Alice creates an older notification (e.g., follow) carrying oldAvatarUri
        signInTestUser(aliceEmail)
        val followRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.toggleFollow(targetUserId = bobUid, currentlyFollowing = false, actor = alice)
        }
        assertTrue("toggleFollow should succeed: ${followRes.exceptionOrNull()}", followRes.isSuccess)

        // Alice updates her avatar to a brand-new image
        val newAvatar = "data:image/jpeg;base64,new_avatar_payload"
        val avatarRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.updateOwnAvatar(newAvatar, isAdmin = false)
        }
        assertTrue("updateOwnAvatar should succeed: ${avatarRes.exceptionOrNull()}", avatarRes.isSuccess)

        val oldAssetSnap = withTimeout(DEFAULT_TIMEOUT_MS) { oldAssetRef.get().await() }
        val oldChunksSnap = withTimeout(DEFAULT_TIMEOUT_MS) { oldAssetRef.collection("chunks").get().await() }
        assertFalse("Old avatar media_assets document should be deleted permanently", oldAssetSnap.exists())
        assertTrue("Old avatar chunks should be deleted permanently", oldChunksSnap.isEmpty)

        // Bob checks older notifications: it now carries Alice's new avatar and no longer has the old one
        signInTestUser(bobEmail)
        val bobNotifs = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeNotifications(bobUid).first { it.isNotEmpty() }
        }
        assertTrue(bobNotifs.all { it.senderId == aliceUid && it.senderAvatar == newAvatar })
    }

    private suspend fun markCurrentAuthUserVerifiedForEmulator() {
        val user = auth.currentUser ?: return
        if (!user.isEmailVerified) {
            user.sendEmailVerification().await()
            val oobUrl = java.net.URL("http://127.0.0.1:9099/emulator/v1/projects/demo-no-project/oobCodes")
            val json = oobUrl.openConnection().getInputStream().bufferedReader().use { it.readText() }
            val match = Regex(""""oobLink"\s*:\s*"([^"]+)"""").findAll(json).lastOrNull()
            val oobLink = match?.groupValues?.get(1)?.replace("\\u0026", "&")
            if (!oobLink.isNullOrBlank()) {
                val verifyConn = (java.net.URL(oobLink).openConnection() as java.net.HttpURLConnection)
                verifyConn.responseCode
                verifyConn.disconnect()
            }
            user.reload().await()
            user.getIdToken(true).await()
        }
        assertTrue("Auth emulator user should now have emailVerified=true", auth.currentUser?.isEmailVerified == true)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
