# Add project specific ProGuard rules here.

# Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class * { *; }

# Kotlin / Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class kotlin.Result { *; }
-dontwarn kotlin.Result$*

# Application specifics
-keep class com.example.data.** { *; }
-keep class com.example.shield.** { *; }
-keep class com.example.calls.** { *; }

