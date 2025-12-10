# Keep all public classes and methods
-keep public class site.dmbi.analytics.DMBIAnalytics { *; }
-keep public class site.dmbi.analytics.DMBIConfiguration { *; }
-keep public class site.dmbi.analytics.DMBIConfiguration$Builder { *; }

# Keep models
-keep class site.dmbi.analytics.models.** { *; }

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
