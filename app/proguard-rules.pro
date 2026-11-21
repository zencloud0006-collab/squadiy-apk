# Keep Firebase models from ProGuard obfuscation
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class com.bsdcampus.app.model.** { *; }
