package com.example.data.model

enum class FileStatus {
    ACTIVE,
    EXPIRED,
    REVOKED,
    SELF_DESTRUCTED
}

enum class UserRole(val displayName: String) {
    OWNER("Owner"),
    ADMIN("Administrator"),
    SECURITY_MANAGER("Security Manager"),
    VIEWER("Viewer / Recipient")
}

enum class ActionType(val label: String) {
    FILE_ENCRYPTED("File Encrypted"),
    FILE_DECRYPTED("File Decrypted"),
    RSA_KEY_GENERATED("RSA Key Pair Generated"),
    LINK_CREATED("Share Link Generated"),
    PASSWORD_VERIFIED("Password Verified"),
    FAILED_PASSWORD_ATTEMPT("Failed Password Attempt"),
    PERMISSION_CHANGED("Permissions Modified"),
    FILE_REVOKED("File Revoked"),
    AUTO_EXPIRED("Auto-Expired"),
    DESTRUCT_TRIGGERED("Self-Destructed")
}

enum class SeverityLevel {
    INFO,
    WARNING,
    SECURITY_ALERT
}

enum class ExpiryOption(val label: String, val durationMillis: Long?) {
    MINS_15("15 Minutes", 15 * 60 * 1000L),
    HOURS_1("1 Hour", 60 * 60 * 1000L),
    HOURS_24("24 Hours", 24 * 60 * 60 * 1000L),
    DAYS_7("7 Days", 7 * 24 * 60 * 60 * 1000L),
    DAYS_30("30 Days", 30L * 24 * 60 * 60 * 1000L),
    NEVER("Never", null)
}
