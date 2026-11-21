# PulseSpan ProGuard / R8 Rules
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-dontwarn kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.pulsespan.app.data.ai.**$$serializer { *; }
-keepclassmembers class com.pulsespan.app.data.ai.** {
    *** Companion;
}
-keepclasseswithmembers class com.pulsespan.app.data.ai.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
