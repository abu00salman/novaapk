# NOVA DRIVE — no reflection is used; keep entry points declared in the manifest.
-keep class com.abu00salman.novadrive.data.NovaNotificationListener { *; }
-dontwarn org.jetbrains.annotations.**

# Nova AI — Anthropic SDK (Jackson/Kotlin reflection) must keep its models and annotations.
-keep class com.anthropic.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class kotlin.Metadata { *; }
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
-dontwarn com.anthropic.**
-dontwarn com.fasterxml.jackson.**
-dontwarn com.github.victools.**
-dontwarn io.swagger.**
-dontwarn io.opentelemetry.**
-dontwarn com.standardwebhooks.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn java.beans.**
-dontwarn javax.annotation.**
-dontwarn org.slf4j.**
-dontwarn kotlin.reflect.jvm.internal.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
