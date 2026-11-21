const { initializeTestEnvironment, assertFails, assertSucceeds } = require("/usr/local/lib/node_modules/@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_1";
const BOB_UID = "bob_student_2";
const ADMIN_UID = "admin_taimiya";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date();
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated user: cannot read profiles, posts, messages, notifications, or reports", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("campus_profiles").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("posts").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("messages").where("participants", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("notifications").where("recipientId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("reports").where("reporterId", "==", ALICE_UID).get());
});

test("Profile registration: blocks self-assigned admin role or verified privilege escalation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const badAdminProfile = {
    uid: ALICE_UID,
    name: "Alice Student",
    role: "admin",
    grade: "Class 10",
    gradeLocked: true,
    isApproved: true,
    isVerified: true,
    isSuspended: false,
    isBlackSociety: false,
    isClassRepresentative: false,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
  await assertFails(aliceDb.collection("campus_profiles").doc(ALICE_UID).set(badAdminProfile));

  const validStudent = {
    uid: ALICE_UID,
    name: "Alice Student",
    role: "student",
    grade: "Class 10",
    gradeLocked: true,
    isApproved: true,
    isVerified: false,
    isSuspended: false,
    isBlackSociety: false,
    isClassRepresentative: false,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
  await assertSucceeds(aliceDb.collection("campus_profiles").doc(ALICE_UID).set(validStudent));
});

test("PII Isolation: Bob cannot read Alice private info (email/settings); Alice & Admin can", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("private").doc("info").set({
      userId: ALICE_UID,
      email: "alice@bsd.edu",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
    await ctx.firestore().collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: "zencloud0006@gmail.com",
      createdAt: nowTs(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").doc("info").get());

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("info").get());

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "zencloud0006@gmail.com", email_verified: true }).firestore();
  await assertSucceeds(adminDb.collection("users").doc(ALICE_UID).collection("private").doc("info").get());
});

test("Email spoofing test: unverified admin email token cannot access admin endpoints", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_user", {
    email: "zencloud0006@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(spoofDb.collection("admins").doc("spoof_user").set({
    uid: "spoof_user",
    email: "zencloud0006@gmail.com",
    createdAt: nowTs(),
  }));
});

test("Posts & Comments: student cannot create pinned/official post, must attach postId filter on comments", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("posts").doc("post_1").set({
    authorId: ALICE_UID,
    authorName: "Alice",
    authorRole: "student",
    content: "Fake official notice",
    category: "Announcements",
    isOfficial: true,
    isPinned: true,
    likedBy: [],
    commentsCount: 0,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertSucceeds(aliceDb.collection("posts").doc("post_2").set({
    authorId: ALICE_UID,
    authorName: "Alice",
    authorRole: "student",
    content: "Hello campus!",
    category: "General",
    isOfficial: false,
    isPinned: false,
    likedBy: [],
    commentsCount: 0,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertSucceeds(aliceDb.collection("posts").doc("post_2").collection("comments").doc("c1").set({
    postId: "post_2",
    authorId: ALICE_UID,
    authorName: "Alice",
    authorRole: "student",
    content: "Nice!",
    createdAt: nowTs(),
  }));

  await assertSucceeds(aliceDb.collection("posts").doc("post_2").collection("comments").where("postId", "==", "post_2").get());
  await assertFails(aliceDb.collection("posts").where("authorId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("posts").where("isActive", "==", true).get());
});

test("Direct Messages: only sender/recipient in participants can read", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("messages").doc("msg_1").set({
      senderId: ALICE_UID,
      recipientId: BOB_UID,
      participants: [ALICE_UID, BOB_UID],
      content: "Hey Bob!",
      read: false,
      edited: false,
      reactions: [],
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("messages").where("participants", "array-contains", ALICE_UID).get());

  const outsiderDb = testEnv.authenticatedContext("eve_999").firestore();
  await assertFails(outsiderDb.collection("messages").doc("msg_1").get());
  await assertFails(outsiderDb.collection("messages").where("participants", "array-contains", ALICE_UID).get());
  await assertSucceeds(outsiderDb.collection("messages").where("participants", "array-contains", "eve_999").get());

  await assertSucceeds(aliceDb.collection("messages").doc("story_reply_msg_1").set({
    senderId: ALICE_UID,
    recipientId: BOB_UID,
    participants: [ALICE_UID, BOB_UID],
    content: "So cool!",
    storyId: "story_bob_1",
    storyAuthorName: "Bob",
    storyMediaUrl: "bsdmedia://story_thumb_1",
    storyMediaType: "image",
    storyCaption: "Morning assembly",
    read: false,
    edited: false,
    reactions: [],
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertFails(aliceDb.collection("messages").doc("story_reply_bad").set({
    senderId: ALICE_UID,
    recipientId: BOB_UID,
    participants: [ALICE_UID, BOB_UID],
    content: "Bad story media type",
    storyId: "story_bob_1",
    storyMediaType: "audio",
    read: false,
    edited: false,
    reactions: [],
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertSucceeds(aliceDb.collection("messages").doc("dm_reply_ok").set({
    senderId: ALICE_UID,
    recipientId: BOB_UID,
    participants: [ALICE_UID, BOB_UID],
    content: "Yes, see you at 10!",
    replyToMessageId: "msg_1",
    replyToSenderId: BOB_UID,
    replyToSenderName: "Bob",
    replyToContent: "Are we meeting?",
    replyToMediaType: "text",
    read: false,
    edited: false,
    reactions: [],
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertFails(aliceDb.collection("messages").doc("dm_reply_bad").set({
    senderId: ALICE_UID,
    recipientId: BOB_UID,
    participants: [ALICE_UID, BOB_UID],
    content: "Invalid reply media",
    replyToMessageId: "msg_1",
    replyToMediaType: "exe",
    read: false,
    edited: false,
    reactions: [],
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));
});

test("Reports: reporter cannot self-resolve and resolved report is locked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("reports").doc("rep_1").set({
    reporterId: ALICE_UID,
    targetType: "post",
    targetId: "post_2",
    targetSummary: "Bad post",
    reason: "Spam",
    status: "pending",
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertFails(aliceDb.collection("reports").doc("rep_1").update({
    status: "resolved",
    updatedAt: nowTs(),
  }));

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "zencloud0006@gmail.com", email_verified: true }).firestore();
  await assertSucceeds(adminDb.collection("reports").doc("rep_1").update({
    status: "resolved",
    actionTaken: "Removed",
    updatedAt: nowTs(),
  }));
  await assertFails(adminDb.collection("reports").doc("rep_1").update({
    status: "dismissed",
    updatedAt: nowTs(),
  }));
});

test("Admin Announcements Protection: normal user (even named Taimiya Amjad or demoted author) cannot delete or edit official announcements", async () => {
  const imposterUid = "imposter_taimiya";
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "zencloud0006@gmail.com",
    email_verified: true,
  }).firestore();
  const imposterDb = testEnv.authenticatedContext(imposterUid, {
    email: "student.imposter@bsd.edu",
    email_verified: true,
  }).firestore();

  // Normal user named "Taimiya Amjad" cannot create admin marker, admin profile, or admin-role post
  await assertFails(imposterDb.collection("admins").doc(imposterUid).set({
    uid: imposterUid,
    email: "student.imposter@bsd.edu",
    createdAt: nowTs(),
  }));
  await assertFails(imposterDb.collection("campus_profiles").doc(imposterUid).set({
    uid: imposterUid,
    name: "Taimiya Amjad",
    role: "admin",
    isApproved: true,
    isVerified: false,
    isSuspended: false,
    isBlackSociety: false,
    isClassRepresentative: false,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));
  await assertFails(imposterDb.collection("posts").doc("spoof_admin_post").set({
    authorId: imposterUid,
    authorName: "Taimiya Amjad",
    authorRole: "admin",
    content: "Spoofed notice",
    category: "Announcements",
    isOfficial: false,
    isPinned: false,
    likedBy: [],
    commentsCount: 0,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  // Admin creates an official pinned announcement
  await assertSucceeds(adminDb.collection("posts").doc("official_notice_1").set({
    authorId: ADMIN_UID,
    authorName: "Taimiya Amjad",
    authorRole: "admin",
    content: "BSD Public School annual exam timetable released.",
    category: "Announcements",
    isOfficial: true,
    isPinned: true,
    likedBy: [],
    commentsCount: 0,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  // Seed an official post authored by a now-demoted user to confirm even authorId match cannot bypass official protection
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("posts").doc("demoted_official_1").set({
      authorId: imposterUid,
      authorName: "Former Moderator",
      authorRole: "student",
      content: "Legacy official notice",
      category: "Announcements",
      isOfficial: true,
      isPinned: false,
      likedBy: [],
      commentsCount: 0,
      isActive: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
  });

  // Normal user cannot delete either official announcement
  await assertFails(imposterDb.collection("posts").doc("official_notice_1").delete());
  await assertFails(imposterDb.collection("posts").doc("demoted_official_1").delete());
  await assertFails(imposterDb.collection("posts").doc("demoted_official_1").update({
    content: "Tampered",
    category: "General",
    images: [],
    updatedAt: nowTs(),
  }));

  // Normal user CAN delete their own regular non-official post
  await assertSucceeds(imposterDb.collection("posts").doc("regular_student_post").set({
    authorId: imposterUid,
    authorName: "Taimiya Amjad",
    authorRole: "student",
    content: "My study notes",
    category: "General",
    isOfficial: false,
    isPinned: false,
    likedBy: [],
    commentsCount: 0,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));
  await assertSucceeds(imposterDb.collection("posts").doc("regular_student_post").delete());

  // Real verified admin CAN delete official announcement
  await assertSucceeds(adminDb.collection("posts").doc("official_notice_1").delete());
});

test("Admin Blue Tick, Class Rep, and Suspend moderation: blocks suspended users from posting/messaging and preserves verification safely", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "ZenCloud0006@gmail.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(aliceDb.collection("campus_profiles").doc(ALICE_UID).set({
    uid: ALICE_UID,
    name: "Alice Student",
    role: "student",
    grade: "Class 10",
    gradeLocked: true,
    isApproved: true,
    isVerified: false,
    isSuspended: false,
    isBlackSociety: false,
    isClassRepresentative: false,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  // Admin grants Blue Tick and Class Rep
  await assertSucceeds(adminDb.collection("campus_profiles").doc(ALICE_UID).update({
    isVerified: true,
    isClassRepresentative: true,
    classRepresentativeGrade: 10,
    updatedAt: nowTs(),
  }));

  // Admin suspends Alice -> Alice can no longer create posts or messages
  await assertSucceeds(adminDb.collection("campus_profiles").doc(ALICE_UID).update({
    isSuspended: true,
    updatedAt: nowTs(),
  }));

  await assertFails(aliceDb.collection("posts").doc("susp_post").set({
    authorId: ALICE_UID,
    authorName: "Alice Student",
    authorRole: "student",
    content: "Should be blocked",
    category: "General",
    isOfficial: false,
    isPinned: false,
    likedBy: [],
    commentsCount: 0,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertFails(aliceDb.collection("messages").doc("susp_msg").set({
    senderId: ALICE_UID,
    recipientId: BOB_UID,
    participants: [ALICE_UID, BOB_UID],
    content: "Blocked DM",
    read: false,
    edited: false,
    reactions: [],
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  // Unsuspending Alice restores ability to post
  await assertSucceeds(adminDb.collection("campus_profiles").doc(ALICE_UID).update({
    isSuspended: false,
    updatedAt: nowTs(),
  }));
  await assertSucceeds(aliceDb.collection("posts").doc("ok_post").set({
    authorId: ALICE_UID,
    authorName: "Alice Student",
    authorRole: "student",
    content: "Back online!",
    category: "General",
    isOfficial: false,
    isPinned: false,
    likedBy: [],
    commentsCount: 0,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));
});

test("Media Assets & Post Media Limits: admins can post unlimited media; students capped at 10 items and 25MB asset manifests", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "zencloud0006@gmail.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  const elevenImages = Array.from({ length: 11 }, (_, i) => `bsdmedia://asset_${i}`);
  await assertFails(aliceDb.collection("posts").doc("too_many").set({
    authorId: ALICE_UID,
    authorName: "Alice",
    authorRole: "student",
    content: "11 images",
    category: "General",
    images: elevenImages,
    isOfficial: false,
    isPinned: false,
    likedBy: [],
    commentsCount: 0,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertSucceeds(adminDb.collection("posts").doc("admin_unlimited").set({
    authorId: ADMIN_UID,
    authorName: "Taimiya Amjad",
    authorRole: "admin",
    content: "Gallery + Video",
    category: "Events",
    images: Array.from({ length: 15 }, (_, i) => `bsdvideo://vid_${i}`),
    isOfficial: true,
    isPinned: false,
    likedBy: [],
    commentsCount: 0,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  // Normal student can like the admin's >10-media post without failing validation
  await assertSucceeds(aliceDb.collection("posts").doc("admin_unlimited").update({
    likedBy: [ALICE_UID],
    updatedAt: nowTs(),
  }));

  // Student can upload a video asset up to 25MB (26,214,400 bytes / 59..75 chunks), blocked over 25MB or >75 chunks
  await assertSucceeds(aliceDb.collection("media_assets").doc("vid_alice_1").set({
    ownerId: ALICE_UID,
    mimeType: "video/mp4",
    chunkCount: 4,
    totalBytes: 1500000,
    createdAt: nowTs(),
  }));
  await assertSucceeds(aliceDb.collection("media_assets").doc("vid_alice_25mb").set({
    ownerId: ALICE_UID,
    mimeType: "video/mp4",
    chunkCount: 59,
    totalBytes: 26214400,
    createdAt: nowTs(),
  }));
  await assertFails(aliceDb.collection("media_assets").doc("vid_alice_too_many_chunks").set({
    ownerId: ALICE_UID,
    mimeType: "video/mp4",
    chunkCount: 76,
    totalBytes: 26214400,
    createdAt: nowTs(),
  }));
  await assertFails(aliceDb.collection("media_assets").doc("vid_alice_huge").set({
    ownerId: ALICE_UID,
    mimeType: "video/mp4",
    chunkCount: 120,
    totalBytes: 50000000,
    createdAt: nowTs(),
  }));
  await assertSucceeds(adminDb.collection("media_assets").doc("vid_admin_huge").set({
    ownerId: ADMIN_UID,
    mimeType: "video/mp4",
    chunkCount: 250,
    totalBytes: 100000000,
    createdAt: nowTs(),
  }));
});

test("Stories: private profile restricts direct get to followers; public profile allows any signed-in user to view & like story", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_viewer_3").firestore();
  const futureExpiry = new Date(Date.now() + 24 * 60 * 60 * 1000);

  // Start with a private profile for Alice
  await assertSucceeds(aliceDb.collection("campus_profiles").doc(ALICE_UID).set({
    uid: ALICE_UID,
    name: "Alice Student",
    role: "student",
    isApproved: true,
    isVerified: false,
    isSuspended: false,
    isBlackSociety: false,
    isClassRepresentative: false,
    isPrivateAccount: true,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertSucceeds(aliceDb.collection("stories").doc("story_alice_1").set({
    authorId: ALICE_UID,
    authorName: "Alice Student",
    authorRole: "student",
    mediaUrl: "bsdmedia://story_1",
    mediaType: "image",
    caption: "Morning assembly!",
    viewedBy: [],
    isActive: true,
    createdAt: nowTs(),
    expiresAt: futureExpiry,
  }));

  // While Alice is private & Bob is NOT following -> direct get fails
  await assertFails(bobDb.collection("stories").doc("story_alice_1").get());

  // Bob follows Alice -> direct get & marking viewed succeed, but deleting or editing caption fails
  await assertSucceeds(bobDb.collection("campus_follows").doc(`${BOB_UID}_${ALICE_UID}`).set({
    followerId: BOB_UID,
    targetId: ALICE_UID,
    isActive: true,
    createdAt: nowTs(),
  }));
  await assertSucceeds(bobDb.collection("stories").doc("story_alice_1").get());
  await assertSucceeds(bobDb.collection("stories").doc("story_alice_1").update({
    viewedBy: [BOB_UID],
    likedBy: [BOB_UID],
  }));
  await assertFails(bobDb.collection("stories").doc("story_alice_1").update({
    caption: "Tampered",
  }));
  await assertFails(bobDb.collection("stories").doc("story_alice_1").delete());

  // Non-follower Charlie cannot view while Alice is private
  await assertFails(charlieDb.collection("stories").doc("story_alice_1").get());

  // Once Alice switches her profile to public -> non-follower Charlie CAN view & like her story
  await assertSucceeds(aliceDb.collection("campus_profiles").doc(ALICE_UID).update({
    isPrivateAccount: false,
    updatedAt: nowTs(),
  }));
  await assertSucceeds(charlieDb.collection("stories").doc("story_alice_1").get());
  await assertSucceeds(charlieDb.collection("stories").doc("story_alice_1").update({
    viewedBy: [BOB_UID, "charlie_viewer_3"],
  }));

  // Author Alice can delete her own story
  await assertSucceeds(aliceDb.collection("stories").doc("story_alice_1").delete());
});

test("Story Highlights: owner can create with custom cover avatar, anyone signed-in can view, non-owner cannot mutate or delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("story_highlights").doc("hl_alice_1").set({
    ownerId: ALICE_UID,
    title: "Annual Sports",
    coverAvatar: "bsdmedia://custom_cover_1",
    items: [
      {
        mediaUrl: "bsdmedia://item_1",
        mediaType: "image",
        caption: "Relay race gold!",
      },
    ],
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertSucceeds(bobDb.collection("story_highlights").where("isActive", "==", true).get());
  await assertFails(bobDb.collection("story_highlights").doc("hl_alice_1").update({
    title: "Hacked",
    updatedAt: nowTs(),
  }));
  await assertFails(bobDb.collection("story_highlights").doc("hl_alice_1").delete());

  await assertSucceeds(aliceDb.collection("story_highlights").doc("hl_alice_1").update({
    title: "Sports 2026",
    coverAvatar: "bsdmedia://custom_cover_2",
    items: [
      {
        mediaUrl: "bsdmedia://item_1",
        mediaType: "image",
        caption: "Relay race gold!",
      },
    ],
    updatedAt: nowTs(),
  }));
  await assertSucceeds(aliceDb.collection("story_highlights").doc("hl_alice_1").delete());
});

test("Reels & Reel Comments: student creates reel, anyone signed-in can watch/like/comment, non-owner cannot edit/delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("reels").doc("reel_1").set({
    authorId: ALICE_UID,
    authorName: "Alice Student",
    authorRole: "student",
    videoUrl: "bsdvideo://reel_clip_1",
    caption: "Tug of War Finals!",
    category: "Sports",
    audioTitle: "Original audio",
    likedBy: [],
    commentsCount: 0,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertSucceeds(bobDb.collection("reels").where("isActive", "==", true).get());
  await assertSucceeds(bobDb.collection("reels").doc("reel_1").update({
    likedBy: [BOB_UID],
    commentsCount: 1,
    updatedAt: nowTs(),
  }));

  await assertSucceeds(bobDb.collection("reels").doc("reel_1").collection("comments").doc("rc_1").set({
    reelId: "reel_1",
    authorId: BOB_UID,
    authorName: "Bob",
    authorRole: "student",
    content: "Amazing shot!",
    createdAt: nowTs(),
  }));
  await assertSucceeds(bobDb.collection("reels").doc("reel_1").collection("comments").where("reelId", "==", "reel_1").get());

  // Non-owner cannot change caption or delete reel
  await assertFails(bobDb.collection("reels").doc("reel_1").update({
    caption: "Hacked caption",
    updatedAt: nowTs(),
  }));
  await assertFails(bobDb.collection("reels").doc("reel_1").delete());

  // Author can update caption and delete reel
  await assertSucceeds(aliceDb.collection("reels").doc("reel_1").update({
    caption: "Updated Sports Reel!",
    updatedAt: nowTs(),
  }));
  await assertSucceeds(aliceDb.collection("reels").doc("reel_1").delete());
});

test("Repost Post or Reel to Story: allows post_card and reel reposts with attribution, blocks invalid sharedSourceType", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const futureExpiry = new Date(Date.now() + 24 * 60 * 60 * 1000);

  await assertSucceeds(bobDb.collection("stories").doc("repost_post_card").set({
    authorId: BOB_UID,
    authorName: "Bob Student",
    authorRole: "student",
    mediaUrl: "shared_text_post://post_123",
    mediaType: "post_card",
    caption: "Must read!",
    sharedSourceType: "post",
    sharedSourceId: "post_123",
    sharedAuthorId: ALICE_UID,
    sharedAuthorName: "Alice",
    sharedSourceCaption: "Robotics club meets at 4 PM in Lab 2.",
    viewedBy: [],
    likedBy: [],
    isActive: true,
    createdAt: nowTs(),
    expiresAt: futureExpiry,
  }));

  await assertSucceeds(bobDb.collection("stories").doc("repost_reel_1").set({
    authorId: BOB_UID,
    authorName: "Bob Student",
    authorRole: "student",
    mediaUrl: "bsdvideo://reel_clip_1",
    mediaType: "video",
    caption: "Watch this reel!",
    sharedSourceType: "reel",
    sharedSourceId: "reel_1",
    sharedAuthorId: ALICE_UID,
    sharedAuthorName: "Alice",
    sharedSourceCaption: "Tug of War Finals!",
    viewedBy: [],
    likedBy: [],
    isActive: true,
    createdAt: nowTs(),
    expiresAt: futureExpiry,
  }));

  await assertFails(bobDb.collection("stories").doc("repost_bad").set({
    authorId: BOB_UID,
    authorName: "Bob Student",
    authorRole: "student",
    mediaUrl: "bsdmedia://img_1",
    mediaType: "image",
    caption: "Invalid source",
    sharedSourceType: "comment",
    viewedBy: [],
    isActive: true,
    createdAt: nowTs(),
    expiresAt: futureExpiry,
  }));
});

test("Story Music & Video Edits: allows music track, mute, trim, speed, filter, and overlay JSON within bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const futureExpiry = new Date(Date.now() + 24 * 60 * 60 * 1000);

  await assertSucceeds(aliceDb.collection("stories").doc("music_video_story_1").set({
    authorId: ALICE_UID,
    authorName: "Alice Student",
    authorRole: "student",
    mediaUrl: "bsdvideo://clip_edited_1",
    mediaType: "video",
    caption: "Sports day practice!",
    musicTitle: "Kesariya",
    musicArtist: "Arijit Singh",
    musicArtworkUrl: "https://is1-ssl.mzstatic.com/image/thumb/art.jpg",
    musicPreviewUrl: "https://audio-ssl.itunes.apple.com/itunes-assets/sample.m4a",
    musicStartSec: 8,
    muteOriginalAudio: true,
    videoStartMs: 1200,
    videoEndMs: 14500,
    videoSpeed: 1.5,
    videoFilter: "VIVID",
    videoOverlaysJson: "[{\"text\":\"Go BSD!\",\"x\":0.5,\"y\":0.3}]",
    viewedBy: [],
    likedBy: [],
    isActive: true,
    createdAt: nowTs(),
    expiresAt: futureExpiry,
  }));

  // Invalid video speed (> 3.0x) is blocked
  await assertFails(aliceDb.collection("stories").doc("music_video_story_bad_speed").set({
    authorId: ALICE_UID,
    authorName: "Alice Student",
    authorRole: "student",
    mediaUrl: "bsdvideo://clip_bad",
    mediaType: "video",
    caption: "Too fast",
    videoSpeed: 10.0,
    viewedBy: [],
    isActive: true,
    createdAt: nowTs(),
    expiresAt: futureExpiry,
  }));
});

test("Post & Reel Views, Comment Replies, and Comment Likes: signed-in users can view, reply, and like comments without altering content", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("posts").doc("post_views_1").set({
    authorId: ALICE_UID,
    authorName: "Alice Student",
    authorRole: "student",
    content: "Science fair starts soon!",
    category: "Events",
    images: [],
    isOfficial: false,
    isPinned: false,
    likedBy: [],
    viewedBy: [],
    commentsCount: 0,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertSucceeds(bobDb.collection("posts").doc("post_views_1").update({
    viewedBy: [BOB_UID],
  }));

  await assertSucceeds(aliceDb.collection("posts").doc("post_views_1").collection("comments").doc("c_parent").set({
    postId: "post_views_1",
    authorId: ALICE_UID,
    authorName: "Alice Student",
    authorRole: "student",
    content: "See you at Lab 2!",
    likedBy: [],
    createdAt: nowTs(),
  }));

  // Bob replies to Alice's comment
  await assertSucceeds(bobDb.collection("posts").doc("post_views_1").collection("comments").doc("c_reply").set({
    postId: "post_views_1",
    authorId: BOB_UID,
    authorName: "Bob Student",
    authorRole: "student",
    content: "On my way!",
    replyToCommentId: "c_parent",
    replyToAuthorName: "Alice Student",
    replyToContent: "See you at Lab 2!",
    likedBy: [],
    createdAt: nowTs(),
  }));

  // Bob can like Alice's comment, but cannot tamper with Alice's comment content
  await assertSucceeds(bobDb.collection("posts").doc("post_views_1").collection("comments").doc("c_parent").update({
    likedBy: [BOB_UID],
  }));
  await assertFails(bobDb.collection("posts").doc("post_views_1").collection("comments").doc("c_parent").update({
    content: "Tampered text",
    likedBy: [BOB_UID],
  }));

  // Reel views, comment replies, and comment likes
  await assertSucceeds(aliceDb.collection("reels").doc("reel_views_1").set({
    authorId: ALICE_UID,
    authorName: "Alice Student",
    authorRole: "student",
    videoUrl: "bsdvideo://clip_view_1",
    caption: "Basketball highlights",
    category: "Sports",
    audioTitle: "Original audio",
    likedBy: [],
    viewedBy: [],
    commentsCount: 0,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertSucceeds(bobDb.collection("reels").doc("reel_views_1").update({
    viewedBy: [BOB_UID],
  }));

  await assertSucceeds(aliceDb.collection("reels").doc("reel_views_1").collection("comments").doc("rc_parent").set({
    reelId: "reel_views_1",
    authorId: ALICE_UID,
    authorName: "Alice Student",
    authorRole: "student",
    content: "Nice buzzer beater!",
    likedBy: [],
    createdAt: nowTs(),
  }));

  await assertSucceeds(bobDb.collection("reels").doc("reel_views_1").collection("comments").doc("rc_reply").set({
    reelId: "reel_views_1",
    authorId: BOB_UID,
    authorName: "Bob Student",
    authorRole: "student",
    content: "Thanks!",
    replyToCommentId: "rc_parent",
    replyToAuthorName: "Alice Student",
    replyToContent: "Nice buzzer beater!",
    likedBy: [],
    createdAt: nowTs(),
  }));

  await assertSucceeds(bobDb.collection("reels").doc("reel_views_1").collection("comments").doc("rc_parent").update({
    likedBy: [BOB_UID],
  }));
  await assertFails(bobDb.collection("reels").doc("reel_views_1").collection("comments").doc("rc_parent").update({
    content: "Tampered reel comment",
  }));
});

test("Chat Notes & Last Seen: owner can update lastSeenAt and share colored song notes visible only to mutual followers", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const CHARLIE_UID = "charlie_stranger_3";
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const futureExpiry = new Date(Date.now() + 24 * 60 * 60 * 1000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("campus_profiles").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "Alice Student",
      role: "student",
      grade: "Class 10",
      gradeLocked: true,
      isApproved: true,
      isVerified: false,
      isSuspended: false,
      isBlackSociety: false,
      isClassRepresentative: false,
      isActive: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
    // One-way follow first: Alice -> Bob only (Bob does NOT follow Alice yet)
    await ctx.firestore().collection("campus_follows").doc(`${ALICE_UID}_${BOB_UID}`).set({
      followerId: ALICE_UID,
      targetId: BOB_UID,
      isActive: true,
      createdAt: nowTs(),
    });
  });

  // Owner can update lastSeenAt on campus_profiles
  await assertSucceeds(aliceDb.collection("campus_profiles").doc(ALICE_UID).update({
    lastSeenAt: nowTs(),
    updatedAt: nowTs(),
  }));

  // Non-owner cannot update another user's lastSeenAt
  await assertFails(bobDb.collection("campus_profiles").doc(ALICE_UID).update({
    lastSeenAt: nowTs(),
    updatedAt: nowTs(),
  }));

  // Invalid bubbleColor is rejected
  await assertFails(aliceDb.collection("campus_notes").doc(ALICE_UID).set({
    authorId: ALICE_UID,
    authorName: "Alice Student",
    text: "Study session!",
    bubbleColor: "neon_invalid",
    isActive: true,
    createdAt: nowTs(),
    expiresAt: futureExpiry,
  }));

  // Empty text without song is rejected
  await assertFails(aliceDb.collection("campus_notes").doc(ALICE_UID).set({
    authorId: ALICE_UID,
    authorName: "Alice Student",
    text: "",
    bubbleColor: "pink",
    isActive: true,
    createdAt: nowTs(),
    expiresAt: futureExpiry,
  }));

  // Valid colored note with song succeeds for owner
  await assertSucceeds(aliceDb.collection("campus_notes").doc(ALICE_UID).set({
    authorId: ALICE_UID,
    authorName: "Alice Student",
    text: "Listening on repeat!",
    bubbleColor: "dark_pink",
    musicTitle: "Kesariya",
    musicArtist: "Arijit Singh",
    musicPreviewUrl: "https://audio.example.com/kesariya.m4a",
    musicStartSec: 10,
    isActive: true,
    createdAt: nowTs(),
    expiresAt: futureExpiry,
  }));

  // Owner can read own note
  await assertSucceeds(aliceDb.collection("campus_notes").doc(ALICE_UID).get());

  // One-way follower (Bob) and non-follower (Charlie) are denied direct get
  await assertFails(bobDb.collection("campus_notes").doc(ALICE_UID).get());
  await assertFails(charlieDb.collection("campus_notes").doc(ALICE_UID).get());

  // Once Bob follows Alice back (mutual follow), Bob CAN read Alice's note; Charlie still denied
  await assertSucceeds(bobDb.collection("campus_follows").doc(`${BOB_UID}_${ALICE_UID}`).set({
    followerId: BOB_UID,
    targetId: ALICE_UID,
    isActive: true,
    createdAt: nowTs(),
  }));
  await assertSucceeds(bobDb.collection("campus_notes").doc(ALICE_UID).get());
  await assertFails(charlieDb.collection("campus_notes").doc(ALICE_UID).get());

  // Bob cannot delete Alice's note, Alice can delete her own note
  await assertFails(bobDb.collection("campus_notes").doc(ALICE_UID).delete());
  await assertSucceeds(aliceDb.collection("campus_notes").doc(ALICE_UID).delete());
});

test("AppUpdateConfig: signed-in users can read, non-admins denied write, admin can publish valid HTTPS APK update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "zencloud0006@gmail.com", email_verified: true }).firestore();

  const validUpdate = {
    latestVersionCode: 3,
    latestVersionName: "2.6.0",
    apkUrl: "https://example.com/releases/Squadify-v2.6.0.apk",
    releaseNotes: "Profile photo fix + auto cache clear",
    isMandatory: false,
    updatedBy: ADMIN_UID,
    updatedAt: nowTs(),
  };

  // Non-admin cannot publish update config
  await assertFails(aliceDb.collection("app_config").doc("update").set(validUpdate));

  // Admin cannot publish invalid non-http URL
  await assertFails(adminDb.collection("app_config").doc("update").set({
    ...validUpdate,
    apkUrl: "javascript:alert(1)",
  }));

  // Admin publishes valid update config
  await assertSucceeds(adminDb.collection("app_config").doc("update").set(validUpdate));

  // Any signed-in user can read update config; unauthenticated cannot
  await assertSucceeds(aliceDb.collection("app_config").doc("update").get());
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("app_config").doc("update").get());
});

test("Posts & Reels: supports imageAspectRatio (4:5/1:1/16:9/free) and song attachment creation/editing", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertFails(aliceDb.collection("posts").doc("bad_ratio_post").set({
    authorId: ALICE_UID,
    authorName: "Alice",
    authorRole: "student",
    content: "Bad ratio",
    category: "General",
    imageAspectRatio: "21:9",
    isOfficial: false,
    isPinned: false,
    likedBy: [],
    commentsCount: 0,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertSucceeds(aliceDb.collection("posts").doc("song_post_1").set({
    authorId: ALICE_UID,
    authorName: "Alice",
    authorRole: "student",
    content: "Evening campus vibes",
    category: "General",
    images: ["bsdmedia://photo_1"],
    imageAspectRatio: "4:5",
    musicTitle: "Apna Bana Le",
    musicArtist: "Arijit Singh",
    musicArtworkUrl: "https://is1-ssl.mzstatic.com/image/thumb/art.jpg",
    musicPreviewUrl: "https://audio-ssl.itunes.apple.com/itunes-assets/sample.m4a",
    musicStartSec: 5,
    isOfficial: false,
    isPinned: false,
    likedBy: [],
    commentsCount: 0,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertSucceeds(aliceDb.collection("posts").doc("song_post_1").update({
    imageAspectRatio: "16:9",
    musicTitle: "Ilahi",
    updatedAt: nowTs(),
  }));

  await assertFails(bobDb.collection("posts").doc("song_post_1").update({
    imageAspectRatio: "1:1",
    updatedAt: nowTs(),
  }));

  await assertSucceeds(aliceDb.collection("reels").doc("song_reel_1").set({
    authorId: ALICE_UID,
    authorName: "Alice",
    authorRole: "student",
    videoUrl: "bsdvideo://clip_1",
    caption: "Annual Fest",
    category: "Events",
    audioTitle: "Kesariya · Arijit Singh",
    musicTitle: "Kesariya",
    musicArtist: "Arijit Singh",
    musicArtworkUrl: "https://is1-ssl.mzstatic.com/image/thumb/art.jpg",
    musicPreviewUrl: "https://audio-ssl.itunes.apple.com/itunes-assets/sample.m4a",
    musicStartSec: 12,
    muteOriginalAudio: true,
    likedBy: [],
    commentsCount: 0,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  }));

  await assertSucceeds(aliceDb.collection("reels").doc("song_reel_1").update({
    muteOriginalAudio: false,
    audioTitle: "Original audio",
    updatedAt: nowTs(),
  }));
});

test("Notifications: sender can replace/delete old senderAvatar on sent notifications without tampering read/content; recipient marks read", async () => {
  const firebaseCompatAppPath = require.resolve("firebase/compat/app", {
    paths: ["/usr/local/lib/node_modules/@firebase/rules-unit-testing/node_modules", "/usr/local/lib/node_modules"],
  });
  const firebaseCompatFirestorePath = require.resolve("firebase/compat/firestore", {
    paths: ["/usr/local/lib/node_modules/@firebase/rules-unit-testing/node_modules", "/usr/local/lib/node_modules"],
  });
  const firebase = require(firebaseCompatAppPath);
  require(firebaseCompatFirestorePath);
  const { FieldValue } = firebase.firestore;
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext("eve_outsider").firestore();

  await assertSucceeds(aliceDb.collection("notifications").doc("notif_avatar_1").set({
    recipientId: BOB_UID,
    senderId: ALICE_UID,
    senderName: "Alice",
    senderAvatar: "data:image/jpeg;base64,old_avatar",
    type: "like",
    title: "New like",
    message: "Alice liked your post",
    read: false,
    createdAt: nowTs(),
  }));

  // Sender can update senderAvatar on notifications they sent when changing their pfp
  await assertSucceeds(aliceDb.collection("notifications").doc("notif_avatar_1").update({
    senderAvatar: "data:image/jpeg;base64,new_avatar",
  }));

  // Sender can permanently delete senderAvatar field when clearing pfp
  await assertSucceeds(aliceDb.collection("notifications").doc("notif_avatar_1").update({
    senderAvatar: FieldValue.delete(),
  }));

  // Sender cannot mark recipient's notification as read or alter message
  await assertFails(aliceDb.collection("notifications").doc("notif_avatar_1").update({
    read: true,
  }));
  await assertFails(aliceDb.collection("notifications").doc("notif_avatar_1").update({
    message: "Tampered message",
  }));

  // Unrelated outsider cannot query or update
  await assertFails(eveDb.collection("notifications").where("senderId", "==", ALICE_UID).get());
  await assertFails(eveDb.collection("notifications").doc("notif_avatar_1").update({
    senderAvatar: "data:image/jpeg;base64,hack",
  }));

  // Recipient can mark read automatically
  await assertSucceeds(bobDb.collection("notifications").doc("notif_avatar_1").update({
    read: true,
  }));
});



