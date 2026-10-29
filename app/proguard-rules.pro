# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ============================================
# GENERAL
# ============================================

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# ============================================
# APP MODELS (Gson serialization)
# ============================================
# Keep all model classes used by Gson/Retrofit
-keep class com.beeflirt.beetalk.models.** { *; }

# ============================================
# RETROFIT
# ============================================
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ============================================
# OKHTTP
# ============================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ============================================
# GSON
# ============================================
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================
# GLIDE
# ============================================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}

# ============================================
# FIREBASE
# ============================================
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ============================================
# GOOGLE PLAY SERVICES
# ============================================
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ============================================
# ADMOB
# ============================================
-keep class com.google.android.gms.ads.** { *; }

# ============================================
# APPLOVIN MAX SDK
# ============================================
-keep class com.applovin.** { *; }
-dontwarn com.applovin.**

# ============================================
# LOTTIE
# ============================================
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# ============================================
# CARDSTACKVIEW
# ============================================
-keep class com.yuyakaido.android.cardstackview.** { *; }

# ============================================
# SWEETALERT
# ============================================
-keep class cn.pedant.SweetAlert.** { *; }

# ============================================
# VOLLEY
# ============================================
-keep class com.android.volley.** { *; }
-dontwarn com.android.volley.**

# ============================================
# ANDROIDX SECURITY (EncryptedSharedPreferences)
# ============================================
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ============================================
# WEBVIEW (if used with JS interface)
# ============================================
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}

# ============================================
# ROOM DATABASE
# ============================================
-keep class com.beeflirt.beetalk.db.entity.** { *; }
-keep class com.beeflirt.beetalk.db.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**

# ============================================
# AMAZON PRIVACY PASS
# ============================================
-dontwarn com.amazon.privacypass.PrivacyPass
-dontwarn com.amazon.privacypass.VerificationContext
-dontwarn com.amazon.privacypass.callback.AttestAPICallback
