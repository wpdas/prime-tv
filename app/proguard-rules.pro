# PrimeTV — regras R8 para o build release.
# Room, Retrofit, OkHttp, Media3, Hilt e Coil já trazem consumer rules nos próprios artefatos.

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.wpdas.primetv.**$$serializer { *; }
-keepclassmembers class com.wpdas.primetv.** { *** Companion; }
-keepclasseswithmembers class com.wpdas.primetv.** { kotlinx.serialization.KSerializer serializer(...); }

# --- Retrofit: interfaces de API com generics ---
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# --- Tink ---
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.errorprone.annotations.**

# --- Diagnóstico de crash sem expor segredos ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
