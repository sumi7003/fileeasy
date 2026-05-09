package com.xplay.player

object ProductFlavorConfig {
    val isFileEasy: Boolean
        get() = BuildConfig.IS_FILEEASY

    val playerFeatureEnabled: Boolean
        get() = BuildConfig.PLAYER_FEATURE_ENABLED

    val productName: String
        get() = BuildConfig.PRODUCT_NAME

    val controlCenterName: String
        get() = BuildConfig.CONTROL_CENTER_NAME

    val loginSubtitle: String
        get() = BuildConfig.LOGIN_SUBTITLE

    val adminEntryLabel: String
        get() = BuildConfig.ADMIN_ENTRY_LABEL

    val serverNotificationName: String
        get() = BuildConfig.SERVER_NOTIFICATION_NAME

    val serverRunningDescription: String
        get() = if (isFileEasy) {
            "File service is running on port 3000"
        } else {
            "Media server is running on port 3000"
        }

    val apkBaseName: String
        get() = if (isFileEasy) "FileEasy" else "XplayPlayer"
}
