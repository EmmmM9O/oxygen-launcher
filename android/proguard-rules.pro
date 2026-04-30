-dontobfuscate

-keep class com.android.dex.** { *; }
-keep class com.android.dx.** { *; }
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

-dontwarn javax.naming.**
-dontwarn androidx.annotation.NonNull
-dontwarn androidx.annotation.Nullable
-dontwarn androidx.annotation.RequiresApi

-keep @oxygen.util.Keep class * { *; }
-keepclassmembers class * {
    @oxygen.util.Keep *;
}
-keepclassmembers class * {
    @oxygen.util.Keep <fields>;
}
-keep @interface @oxygen.util.Keep

#-printusage out.txt
