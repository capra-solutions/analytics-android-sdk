# DMBI Analytics Android SDK

Native Android SDK for DMBI Analytics platform. Track screen views, video engagement, push notifications, scroll depth, conversions, and custom events.

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
    implementation 'com.github.dmbi-analytics:analytics-android-sdk:1.0.2'
}
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
override fun onResume() {
    super.onResume()
    DMBIAnalytics.trackScreen(
        name = "ArticleDetail",
        url = "app://article/$articleId",
        title = article.title
    )
}
```

### 3. Scroll Tracking

```kotlin
// Attach to RecyclerView
DMBIAnalytics.attachScrollTracking(recyclerView)

// Or NestedScrollView
DMBIAnalytics.attachScrollTracking(nestedScrollView)

// Or report manually (for custom scroll implementations)
DMBIAnalytics.reportScrollDepth(75) // 75%

// Get current scroll depth
val depth = DMBIAnalytics.getCurrentScrollDepth()

// Detach when leaving screen
DMBIAnalytics.detachScrollTracking()
```

### 4. User Types & Segments

```kotlin
// Set user type
DMBIAnalytics.setUserType(UserType.SUBSCRIBER) // anonymous, logged, subscriber, premium

// Add user segments for cohort analysis
DMBIAnalytics.addUserSegment("sports_fan")
DMBIAnalytics.addUserSegment("premium_reader")

// Remove segment
DMBIAnalytics.removeUserSegment("sports_fan")

// Get all segments
val segments = DMBIAnalytics.getUserSegments()
```

### 5. Conversion Tracking

```kotlin
// Simple conversion
DMBIAnalytics.trackConversion(
    id = "sub_123",
    type = "subscription",
    value = 99.99,
    currency = "TRY"
)

// Detailed conversion with properties
DMBIAnalytics.trackConversion(
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

// Video completed
DMBIAnalytics.trackVideoComplete(
    videoId = "vid123",
    duration = 180f
)
```

### 7. Push Notifications

```kotlin
// In FirebaseMessagingService
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    DMBIAnalytics.trackPushReceived(
        notificationId = remoteMessage.data["notification_id"],
        title = remoteMessage.notification?.title,
        campaign = remoteMessage.data["campaign"]
    )
}

// When notification is opened
DMBIAnalytics.trackPushOpened(
    notificationId = intent.getStringExtra("notification_id"),
    title = intent.getStringExtra("title"),
    campaign = intent.getStringExtra("campaign")
)
```

### 8. Engagement Metrics

```kotlin
// Get active time (excludes background)
val activeSeconds = DMBIAnalytics.getActiveTimeSeconds()

// Get heartbeat count
val pingCount = DMBIAnalytics.getPingCounter()

// Record user interaction (resets inactivity timer)
DMBIAnalytics.recordInteraction()
```

### 9. Custom Events

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
    .heartbeatInterval(30_000L)        // Base heartbeat: 30 seconds
    .maxHeartbeatInterval(120_000L)    // Max when inactive: 120 seconds
    .inactivityThreshold(30_000L)      // Inactive after 30 seconds
    .batchSize(10)                     // Send events in batches of 10
    .flushInterval(30_000L)            // Flush every 30 seconds
    .sessionTimeout(30 * 60 * 1000L)   // New session after 30 min background
    .debugLogging(true)                // Enable debug logs
    .build()

DMBIAnalytics.configure(this, config)
```

## Comparison with Competitors

| Feature | Chartbeat | Marfeel | DMBI SDK |
|---------|-----------|---------|----------|
| Heartbeat | 15s | 10s | 30s (dynamic) |
| Scroll tracking | ✅ | ✅ | ✅ |
| Active time | ? | ✅ | ✅ |
| Dynamic interval | ✅ | ❌ | ✅ |
| Conversions | ❌ | ✅ | ✅ |
| User segments | ✅ | ✅ | ✅ |
| Offline storage | ❌ | ❌ | ✅ |

## Java Support

```java
// Java initialization
DMBIAnalytics.configure(context, "your-site-android", "https://realtime.dmbi.site/e");

// Java screen tracking
DMBIAnalytics.trackScreen("Home", "app://home", "Home Screen");

// Java user type
DMBIAnalytics.setUserType(UserType.SUBSCRIBER);

// Java conversion
DMBIAnalytics.trackConversion("sub_123", "subscription", 99.99, "TRY");
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
-keep class site.dmbi.analytics.** { *; }
-keepclassmembers class site.dmbi.analytics.** { *; }
```

## License

MIT License
