# KothaPad ProGuard & R8 Configuration

-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# Room Database entities and DAOs
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Data models
-keep class com.example.data.model.** { *; }

# Coroutines & Flow
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }
