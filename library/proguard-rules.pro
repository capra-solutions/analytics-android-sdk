# Keep all public classes and methods
-keep public class solutions.capra.analytics.CapraAnalytics { *; }
-keep public class solutions.capra.analytics.CapraConfiguration { *; }
-keep public class solutions.capra.analytics.CapraConfiguration$Builder { *; }

# Keep models
-keep class solutions.capra.analytics.models.** { *; }

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
