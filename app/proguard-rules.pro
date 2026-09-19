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

# -----------------------------
# Media3 / ExoPlayer
# -----------------------------
-keep class androidx.media3.common.Player { *; }
-keep class androidx.media3.common.Player$Listener { *; }
-keep class androidx.media3.common.MediaItem { *; }
-keep class androidx.media3.common.PlaybackException { *; }
-keep class androidx.media3.common.Tracks { *; }
-keep class androidx.media3.common.TrackSelectionOverride { *; }
-keep class androidx.media3.common.VideoSize { *; }
-keep class androidx.media3.common.AudioAttributes { *; }
-keep class androidx.media3.exoplayer.ExoPlayer { *; }
-keep class androidx.media3.exoplayer.DefaultLoadControl { *; }
-keep class androidx.media3.exoplayer.trackselection.DefaultTrackSelector { *; }
-keep class androidx.media3.ui.PlayerView { *; }
-dontwarn androidx.media3.**

# -----------------------------
# Google Mobile Ads / AdMob
# -----------------------------
-keep class com.google.android.gms.ads.AdRequest { *; }
-keep class com.google.android.gms.ads.AdError { *; }
-keep class com.google.android.gms.ads.LoadAdError { *; }
-keep class com.google.android.gms.ads.FullScreenContentCallback { *; }
-keep class com.google.android.gms.ads.MobileAds { *; }
-keep class com.google.android.gms.ads.initialization.OnInitializationCompleteListener { *; }
-keep class com.google.android.gms.ads.interstitial.InterstitialAd { *; }
-keep class com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback { *; }
-dontwarn com.google.android.gms.ads.**
