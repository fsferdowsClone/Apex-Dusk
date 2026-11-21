# Preserve line numbers and source file names for Google Play Console crash deobfuscation
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep DataStore Preferences & Protobuf reflection fields
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep GLSurfaceView Renderer & AndroidViewModel constructors
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    public <init>(android.app.Application);
}
-keep class com.example.game.gl.** { *; }
-keep class com.example.game.model.** { *; }
