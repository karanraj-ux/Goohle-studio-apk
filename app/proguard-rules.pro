-keep class com.google.errorprone.annotations.** { *; }
-dontwarn com.google.errorprone.annotations.**

# Keep all our custom classes
-keep class com.example.** { *; }

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class kotlinx.coroutines.** { *; }

# Keep ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Room
-keep class androidx.room.** { *; }

# Keep Moshi
-keep class com.squareup.moshi.** { *; }
-keep class * {
    @com.squareup.moshi.JsonClass <fields>;
}

# Keep Retrofit
-keep class retrofit2.** { *; }

# Prevent missing class warnings
-dontwarn kotlinx.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn com.squareup.moshi.**

