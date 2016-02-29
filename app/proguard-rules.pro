# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/pollas_p/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontoptimize
-dontobfuscate
-dontpreverify
-keepattributes Signature
-dontwarn scala.**
-ignorewarnings
# temporary workaround; see Scala issue SI-5397
-keep class scala.collection.SeqLike {
    public protected *;
}
-keep class net.sourceforge.zbar.** { *; }

## Support library v4 22.2.0
-dontwarn android.support.v4.app.**
-dontwarn android.support.v4.view.**
-dontwarn android.support.v4.widget.**

-keep class com.typesafe.**
-keep class akka.**
-keep class scala.collection.immutable.StringLike {
    *;
}
-keepclasseswithmembers class * {
    public <init>(java.lang.String, akka.actor.ActorSystem$Settings, akka.event.EventStream, akka.actor.Scheduler, akka.actor.DynamicAccess);
}
-keepclasseswithmembers class * {
    public <init>(akka.actor.ExtendedActorSystem);
}
-keep class scala.collection.SeqLike {
    public protected *;
}

## Keep spongy castle
-keep class org.spongycastle.**
-dontwarn org.spongycastle.jce.provider.X509LDAPCertStoreSpi
-dontwarn org.spongycastle.x509.util.LDAPStoreHelper

## Event bus
-keepclassmembers,includedescriptorclasses class ** { public void onEvent*(**); }

## Preference Fragment
-keep public class * extends android.preference.Preference
-keep public class * extends android.preference.PreferenceFragment
-keep public class * extends android.preference.PreferenceActivity
-keep class android.support.v4.** { *; }
-keep class android.support.v7.** { *; }

## Design library
-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }
-keep public class * extends android.support.design.widget.CoordinatorLayout$Behavior {
    public <init>(android.content.Context, android.util.AttributeSet);
}