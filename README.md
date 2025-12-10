# DMBI Analytics Android SDK

Native Android SDK for DMBI Analytics platform. Track screen views, video engagement, push notifications, and custom events.

## Installation

### Gradle (JitPack)

Add JitPack repository to your root `build.gradle`:

```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency:

```gradle
dependencies {
    implementation 'com.github.dmbi-analytics:analytics-android-sdk:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>site.dmbi.analytics</groupId>
    <artifactId>analytics</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### 1. Initialize in Application Class

```kotlin
import site.dmbi.analytics.DMBIAnalytics

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        DMBIAnalytics.configure(
            context = this,
            siteId = "your-site-android",
            endpoint = "https://realtime.dmbi.site/e"
        )
    }
}
```

### 2. Track Screens

```kotlin
// In your activities/fragments
override fun onResume() {
    super.onResume()
    DMBIAnalytics.trackScreen(
        name = "ArticleDetail",
        url = "app://article/$articleId",
        title = article.title
    )
}
```

### 3. Track Videos

```kotlin
// Video started playing
DMBIAnalytics.trackVideoPlay(
    videoId = "vid123",
    title = "Video Title",
    duration = 180f,
    position = 0f
)

// Video progress (quartiles)
DMBIAnalytics.trackVideoProgress(
    videoId = "vid123",
    duration = 180f,
    position = 45f,
    percent = 25
)

// Video paused
DMBIAnalytics.trackVideoPause(
    videoId = "vid123",
    position = 90f,
    percent = 50
)

// Video completed
DMBIAnalytics.trackVideoComplete(
    videoId = "vid123",
    duration = 180f
)
```

### 4. Track Push Notifications

```kotlin
// In your FirebaseMessagingService
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    DMBIAnalytics.trackPushReceived(
        notificationId = remoteMessage.data["notification_id"],
        title = remoteMessage.notification?.title,
        campaign = remoteMessage.data["campaign"]
    )
}

// When notification is opened
fun handleNotificationOpen(intent: Intent) {
    DMBIAnalytics.trackPushOpened(
        notificationId = intent.getStringExtra("notification_id"),
        title = intent.getStringExtra("title"),
        campaign = intent.getStringExtra("campaign")
    )
}
```

### 5. User Login State

```kotlin
// When user logs in
DMBIAnalytics.setLoggedIn(true)

// When user logs out
DMBIAnalytics.setLoggedIn(false)
```

### 6. Custom Events

```kotlin
DMBIAnalytics.trackEvent(
    name = "article_share",
    properties = mapOf(
        "article_id" to "12345",
        "share_platform" to "twitter"
    )
)
```

## Advanced Configuration

```kotlin
val config = DMBIConfiguration.Builder(
    siteId = "your-site-android",
    endpoint = "https://realtime.dmbi.site/e"
)
    .heartbeatInterval(60_000L)      // Heartbeat every 60 seconds
    .batchSize(10)                   // Send events in batches of 10
    .flushInterval(30_000L)          // Flush every 30 seconds
    .sessionTimeout(30 * 60 * 1000L) // New session after 30 min background
    .debugLogging(true)              // Enable debug logs
    .build()

DMBIAnalytics.configure(this, config)
```

## Java Support

The SDK supports both Kotlin and Java:

```java
// Java initialization
DMBIAnalytics.configure(context, "your-site-android", "https://realtime.dmbi.site/e");

// Java screen tracking
DMBIAnalytics.trackScreen("Home", "app://home", "Home Screen");

// Java event tracking
Map<String, Object> props = new HashMap<>();
props.put("article_id", "12345");
DMBIAnalytics.trackEvent("article_view", props);
```

## Features

- **Automatic Session Management**: Sessions are automatically created on app launch and after 30 minutes of inactivity
- **Secure User ID**: User ID is stored in EncryptedSharedPreferences and persists across app reinstalls
- **Offline Support**: Events are queued and sent when network is available
- **Heartbeat**: Periodic heartbeats enable real-time concurrent user tracking
- **App Lifecycle**: Automatic tracking of app open/close events via ProcessLifecycleOwner
- **Video Tracking**: Track video impressions, plays, progress, pauses, and completions

## Requirements

- Android SDK 21+ (Android 5.0 Lollipop)
- Kotlin 1.9+

## Permissions

The SDK requires the following permissions (automatically merged):

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## ProGuard

If using ProGuard, add the following rules:

```proguard
-keep class site.dmbi.analytics.** { *; }
-keepclassmembers class site.dmbi.analytics.** { *; }
```

## License

MIT License
