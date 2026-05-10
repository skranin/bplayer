# Keep the foreground service entry point — Media3 looks it up via component name.
-keep class com.bplayer.playback.PlayerService { *; }
-keep class * extends androidx.media3.session.MediaLibraryService { *; }
-keep class * extends androidx.media3.session.MediaSessionService { *; }

# Application class is referenced by name in the manifest.
-keep class com.bplayer.BPlayerApp { *; }

# Room generated implementations (Room ships consumer rules but be explicit for our DAOs).
-keep class com.bplayer.data.bookmarks.** { *; }

# Kotlin metadata required for reflection-aware libraries (Room, kotlinx.serialization).
-keep class kotlin.Metadata { *; }

# Suppress warnings from optional transitive deps that R8 sees but we don't actually use.
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
