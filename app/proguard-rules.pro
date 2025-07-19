# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Standard Android optimizations
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# iText7 PDF library rules
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**
-dontwarn org.slf4j.**
-dontwarn javax.xml.**
-dontwarn org.xml.**
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn javax.swing.**
-dontwarn javax.imageio.**
-dontwarn java.util.prefs.**
-dontwarn java.lang.management.**
-dontwarn java.security.cert.**
-dontwarn java.security.spec.**
-dontwarn java.text.**
-dontwarn java.time.**
-dontwarn java.nio.file.**
-dontwarn java.util.concurrent.**
-dontwarn java.util.function.**
-dontwarn java.util.stream.**
-dontwarn java.util.regex.**
-dontwarn java.util.zip.**
-dontwarn java.io.**
-dontwarn java.lang.ref.**
-dontwarn java.lang.reflect.**
-dontwarn java.math.**
-dontwarn java.net.**
-dontwarn java.util.**

# Keep classes that are referenced via reflection
-keepclassmembers class ** {
    @com.itextpdf.** *;
}

# SLF4J logging framework (used by iText)
-dontwarn org.slf4j.impl.StaticLoggerBinder
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# Removed PDFBox rules due to AWT incompatibility with Android