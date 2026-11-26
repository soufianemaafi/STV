# ProGuard/R8 rules for :soukitv
# Appended to proguard-android-optimize.txt (AGP default)

# ── [B1 — CRITIQUE] DTOs réseau Gson/Retrofit ────────────────────────────────
# Gson utilise la réflexion sur les constructeurs ET les membres. R8 peut
# renommer la classe entière même si @SerializedName protège les champs ;
# -keep sur toute la hiérarchie est la seule garantie robuste sans migrer
# vers Moshi Codegen ou kotlinx.serialization.
-keep class com.soufcom.soukitv.features.home.data.model.** { *; }

# ── Domain model Channel (préexistant, conservé) ─────────────────────────────
-keepclassmembers class com.soufcom.soukitv.features.home.domain.model.Channel { *; }

# ── Génériques Retrofit & attributs d'annotations ────────────────────────────
# Requis pour que Retrofit résout les types génériques (TypeToken, etc.)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# ── Retrofit ─────────────────────────────────────────────────────────────────
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# ── OkHttp ───────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**

# ── Gson ─────────────────────────────────────────────────────────────────────
-keep class com.google.gson.** { *; }

# ── JNI ─────────────────────────────────────────────────────────────────────
-keep class com.soufcom.soukitv.core.security.NativeSecurityBridge { *; }
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# ── Durcissement : suppression des traces en release (requiert l'optimisation R8) ──
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}
-assumenosideeffects class kotlin.io.ConsoleKt {
    public static void print(...);
    public static void println(...);
}

# ── Durcissement : aplatit les packages pour brouiller l'arborescence dans JADX ──
-repackageclasses 'o'
-allowaccessmodification
