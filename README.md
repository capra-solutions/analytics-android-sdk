# Capra Analytics Android SDK

Native Android SDK for Capra Analytics platform. Track screen views, video engagement, push notifications, scroll depth, conversions, and custom events.

## Features

- **Heartbeat with Dynamic Intervals**: 30s base interval, increases to 120s when user is inactive
- **Active Time Tracking**: Only counts foreground time, excludes background
- **Scroll Depth Tracking**: RecyclerView, NestedScrollView, ScrollView support
- **User Segments**: Cohort analysis with custom segments
- **Conversion Tracking**: Track subscriptions, purchases, registrations
- **Offline Support**: Events are queued and sent when network is available
- **Automatic Session Management**: New session after 30 min background

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
    implementation 'com.github.capra-solutions:analytics-android-sdk:2.0.1'
}
```

## Migration from DMBIAnalytics 1.x

If upgrading from version 1.x:

1. Update import: `import site.dmbi.analytics.*` -> `import solutions.capra.analytics.*`
2. Rename class: `DMBIAnalytics` -> `CapraAnalytics`
3. Rename config: `DMBIConfiguration` -> `CapraConfiguration`
4. Update endpoint: `https://realtime.dmbi.site/e` -> `https://t.capra.solutions/e`

Note: Storage keys have changed, so user sessions will be reset after upgrade.

## Quick Start

### 1. Initialize in Application Class

```kotlin
import solutions.capra.analytics.CapraAnalytics

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        CapraAnalytics.configure(
            context = this,
            siteId = "your-site-android",
            endpoint = "https://t.capra.solutions/e"
        )
    }
}
```

### 2. Track Screens

```kotlin
override fun onResume() {
    super.onResume()
    CapraAnalytics.trackScreen(
        name = "ArticleDetail",
        url = "app://article/$articleId",
        title = article.title
    )
}
```

### 3. Scroll Tracking

```kotlin
// Attach to RecyclerView
CapraAnalytics.attachScrollTracking(recyclerView)

// Or NestedScrollView
CapraAnalytics.attachScrollTracking(nestedScrollView)

// Or report manually (for custom scroll implementations)
CapraAnalytics.reportScrollDepth(75) // 75%

// Get current scroll depth
val depth = CapraAnalytics.getCurrentScrollDepth()

// Detach when leaving screen
CapraAnalytics.detachScrollTracking()
```

### 4. User Types & Segments

```kotlin
// Set user type
CapraAnalytics.setUserType(UserType.SUBSCRIBER) // anonymous, logged, subscriber, premium

// Add user segments for cohort analysis
CapraAnalytics.addUserSegment("sports_fan")
CapraAnalytics.addUserSegment("premium_reader")

// Remove segment
CapraAnalytics.removeUserSegment("sports_fan")

// Get all segments
val segments = CapraAnalytics.getUserSegments()
```

### 5. Conversion Tracking

```kotlin
// Simple conversion
CapraAnalytics.trackConversion(
    id = "sub_123",
    type = "subscription",
    value = 99.99,
    currency = "TRY"
)

// Detailed conversion with properties
CapraAnalytics.trackConversion(
    Conversion(
        id = "purchase_456",
        type = "purchase",
        value = 149.99,
        currency = "TRY",
        properties = mapOf(
            "product_id" to "prod_123",
            "category" to "premium"
        )
    )
)
```

### 6. Video Tracking

#### Manual Tracking

```kotlin
// Video started playing
CapraAnalytics.trackVideoPlay(
    videoId = "vid123",
    title = "Video Title",
    duration = 180f,
    position = 0f
)

// Video progress (quartiles)
CapraAnalytics.trackVideoProgress(
    videoId = "vid123",
    duration = 180f,
    position = 45f,
    percent = 25
)

// Video completed
CapraAnalytics.trackVideoComplete(
    videoId = "vid123",
    duration = 180f
)
```

#### Auto-Tracking with Player Wrappers

SDK includes wrappers for popular video players that automatically track play, pause, progress (25%, 50%, 75%, 100%), and complete events.

**ExoPlayer:**
```kotlin
// Add dependency: implementation("androidx.media3:media3-exoplayer:1.2.0")

import solutions.capra.analytics.players.ExoPlayerWrapper

val exoPlayer = ExoPlayer.Builder(context).build()
val wrapper = ExoPlayerWrapper(exoPlayer)
wrapper.attach(
    videoId = "vid123",
    title = "Video Title"
)

// When done:
wrapper.detach()
```

**YouTube Player:**
```kotlin
// Add dependency: implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

import solutions.capra.analytics.players.YouTubePlayerWrapper

youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
    override fun onReady(youTubePlayer: YouTubePlayer) {
        val wrapper = YouTubePlayerWrapper(youTubePlayer)
        wrapper.attach(videoId = "dQw4w9WgXcQ", title = "Video Title")
        youTubePlayer.loadVideo("dQw4w9WgXcQ", 0f)
    }
})
```

**Dailymotion Player:**
```kotlin
// Add dependency: implementation("com.dailymotion.player.android:sdk:1.2.7")
// Add repository: maven { url = uri("https://mvn.dailymotion.com/repository/releases/") }

import solutions.capra.analytics.players.DailymotionPlayerWrapper

val wrapper = DailymotionPlayerWrapper()

Dailymotion.createPlayer(
    context = context,
    playerId = "YOUR_PLAYER_ID",
    videoListener = wrapper.videoListener,
    playerListener = wrapper.playerListener
) { player ->
    playerView.setPlayer(player)
    wrapper.attach(videoId = "x8abc123", title = "Video Title")
    player.loadContent(videoId = "x8abc123")
}
```

### 7. Push Notifications

```kotlin
// In FirebaseMessagingService
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    CapraAnalytics.trackPushReceived(
        notificationId = remoteMessage.data["notification_id"],
        title = remoteMessage.notification?.title,
        campaign = remoteMessage.data["campaign"]
    )
}

// When notification is opened
CapraAnalytics.trackPushOpened(
    notificationId = intent.getStringExtra("notification_id"),
    title = intent.getStringExtra("title"),
    campaign = intent.getStringExtra("campaign")
)
```

### 8. Engagement Metrics

```kotlin
// Get active time (excludes background)
val activeSeconds = CapraAnalytics.getActiveTimeSeconds()

// Get heartbeat count
val pingCount = CapraAnalytics.getPingCounter()

// Record user interaction (resets inactivity timer)
CapraAnalytics.recordInteraction()
```

### 9. Custom Events

```kotlin
CapraAnalytics.trackEvent(
    name = "article_share",
    properties = mapOf(
        "article_id" to "12345",
        "share_platform" to "twitter"
    )
)
```

## Advanced Configuration

```kotlin
val config = CapraConfiguration.Builder(
    siteId = "your-site-android",
    endpoint = "https://t.capra.solutions/e"
)
    .heartbeatInterval(30_000L)        // Base heartbeat: 30 seconds
    .maxHeartbeatInterval(120_000L)    // Max when inactive: 120 seconds
    .inactivityThreshold(30_000L)      // Inactive after 30 seconds
    .batchSize(10)                     // Send events in batches of 10
    .flushInterval(30_000L)            // Flush every 30 seconds
    .sessionTimeout(30 * 60 * 1000L)   // New session after 30 min background
    .debugLogging(true)                // Enable debug logs
    .build()

CapraAnalytics.configure(this, config)
```

## Comparison with Competitors

| Feature | Chartbeat | Marfeel | Capra SDK |
|---------|-----------|---------|-----------|
| Heartbeat | 15s | 10s | 30s (dynamic) |
| Scroll tracking | Yes | Yes | Yes |
| Active time | ? | Yes | Yes |
| Dynamic interval | Yes | No | Yes |
| Conversions | No | Yes | Yes |
| User segments | Yes | Yes | Yes |
| Offline storage | No | No | Yes |

## Java Support

```java
// Java initialization
CapraAnalytics.configure(context, "your-site-android", "https://t.capra.solutions/e");

// Java screen tracking
CapraAnalytics.trackScreen("Home", "app://home", "Home Screen");

// Java user type
CapraAnalytics.setUserType(UserType.SUBSCRIBER);

// Java conversion
CapraAnalytics.trackConversion("sub_123", "subscription", 99.99, "TRY");
```

## Requirements

- Android SDK 21+ (Android 5.0 Lollipop)
- Kotlin 1.9+

## Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## ProGuard

```proguard
-keep class solutions.capra.analytics.** { *; }
-keepclassmembers class solutions.capra.analytics.** { *; }
```

## License

MIT License
