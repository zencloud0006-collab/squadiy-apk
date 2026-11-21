package com.bsdcampus.app.model

object CampusConstants {
    const val APP_VERSION = "Version 2.7.0 • Unofficial Student Community"
    const val BUILD_NUMBER = "Build 4"
    const val SCHOOL_NAME = "BSD Public School"
    const val SCHOOL_FULL_TITLE = "Unofficial Student-Built Community Platform"
    const val UNOFFICIAL_SHORT_DISCLAIMER =
        "Unofficial student-built platform • Not affiliated with BSD Public School or its administration"
    const val UNOFFICIAL_FULL_DISCLAIMER =
        "Squadify is an independent, unofficial platform created by a student for students. It is NOT affiliated with, endorsed by, or connected to BSD Public School or its administration."
    const val DEVELOPER_SUPPORT_EMAIL = "founder@zendevelopment.in"
    const val SCHOOL_LOCATION = "Guraini, Jaunpur, Uttar Pradesh"
    const val SCHOOL_PHONE = "+91 7080672744"
    const val SCHOOL_EMAIL = "founder@zendevelopment.in"
    const val SCHOOL_FOUNDED = "2017"
    const val SCHOOL_CBSE_SINCE = "2018"
    const val SUPER_ADMIN_EMAIL = "zencloud0006@gmail.com"
    const val SUPER_ADMIN_NAME = "Taimiya Amjad"

    val LEADERSHIP = listOf(
        "Shashank Sir" to "Principal",
        "Bobby Sir" to "Vice Principal",
        "Sabajeet Sir" to "Director"
    )

    val SCHOOL_CLASSES: List<String> = (1..12).map { "Class $it" }

    val FACULTY_DEPARTMENTS: List<String> = listOf(
        "Science & Mathematics Department",
        "Humanities & Languages Department",
        "Computer Science & Robotics Department",
        "Early Years & Primary Division",
        "Sports & Physical Education",
        "Creative Arts & Music",
        "Commerce & Economics Department",
        "Social Sciences & History",
        "Administration & Student Affairs"
    )

    val POST_CATEGORIES: List<String> = listOf(
        "Announcements",
        "News",
        "Academic",
        "Achievements",
        "Sports",
        "Events",
        "General"
    )

    val REPORT_REASONS: List<String> = listOf(
        "Inappropriate or offensive language",
        "Harassment, bullying, or hate speech",
        "Spam or unauthorized promotional content",
        "Misinformation or impersonation",
        "Academic dishonesty or privacy violation"
    )

    val REACTION_EMOJIS: List<String> = listOf("❤️", "👍", "😂", "😮", "😢", "🔥", "⭐")

    val PRESET_AVATARS: List<Pair<String, String>> = listOf(
        "Squadify Crest" to "https://api.dicebear.com/7.x/bottts/svg?seed=Squadify",
        "Scholar" to "https://api.dicebear.com/7.x/avataaars/svg?seed=Scholar",
        "Innovator" to "https://api.dicebear.com/7.x/avataaars/svg?seed=Innovator",
        "Leader" to "https://api.dicebear.com/7.x/avataaars/svg?seed=Leader",
        "Creator" to "https://api.dicebear.com/7.x/avataaars/svg?seed=Creator",
        "Pioneer" to "https://api.dicebear.com/7.x/avataaars/svg?seed=Pioneer"
    )

    val SUPPORTED_LANGUAGES: List<String> = listOf(
        "English (US)",
        "Urdu (اردو)",
        "Hindi (हिन्दी)",
        "Arabic (العربية)",
        "Spanish (Español)"
    )

    fun defaultAvatarFor(name: String): String =
        "https://api.dicebear.com/7.x/bottts/svg?seed=${java.net.URLEncoder.encode(name.ifBlank { "BSD" }, "UTF-8")}"
}
