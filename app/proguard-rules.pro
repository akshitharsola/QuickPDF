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

# iText7 PDF library rules - optimized to keep only essential classes
-keep class com.itextpdf.kernel.pdf.PdfReader { *; }
-keep class com.itextpdf.kernel.pdf.PdfWriter { *; }
-keep class com.itextpdf.kernel.pdf.PdfDocument { *; }
-keep class com.itextpdf.kernel.pdf.ReaderProperties { *; }
-keep class com.itextpdf.kernel.pdf.DocumentProperties { *; }
-keep class com.itextpdf.kernel.exceptions.** { *; }
-keep class com.itextpdf.io.exceptions.** { *; }
-dontwarn com.itextpdf.**

# Essential dontwarn rules for iText7 compatibility
-dontwarn org.slf4j.**
-dontwarn javax.xml.**
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn javax.swing.**

# Keep classes that are referenced via reflection
-keepclassmembers class ** {
    @com.itextpdf.** *;
}

# SLF4J logging framework (used by iText) - keep minimal
-dontwarn org.slf4j.impl.StaticLoggerBinder
-keep class org.slf4j.Logger { *; }
-keep class org.slf4j.LoggerFactory { *; }
-dontwarn org.slf4j.**