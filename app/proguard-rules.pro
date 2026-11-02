-keep class androidx.security.crypto.** { *; }

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepclassmembers class com.kiosk.launcher.MainActivity$* {
    *;
}
