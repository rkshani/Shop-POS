# Room
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# Koin
-keep class org.koin.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.dukan.pos.** {
    *** Companion;
}
-keepclasseswithmembers class com.dukan.pos.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ML Kit
-keep class com.google.mlkit.** { *; }
