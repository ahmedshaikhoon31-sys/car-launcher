package com.zarwa.launcher.media

import android.service.notification.NotificationListenerService

/**
 * Empty listener. Its only job is to exist so the system grants us access to
 * active media sessions (MediaSessionManager requires a NotificationListener
 * component to enumerate sessions). The user enables it once in
 * Settings > Notification access.
 */
class ZarwaNotificationListener : NotificationListenerService()
