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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ==================== ViewBinding 和泛型支持 ====================
# 保留泛型签名信息，这对于反射获取泛型参数至关重要
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# 保留所有 ViewBinding 类及其 inflate 方法
-keep class * implements androidx.viewbinding.ViewBinding {
    public static ** inflate(android.view.LayoutInflater);
    public static ** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
    public static ** bind(android.view.View);
    *** getRoot();
}

# 保留所有 DataBinding 类
-keep class * extends androidx.databinding.ViewDataBinding {
    public static ** inflate(android.view.LayoutInflater);
    public static ** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
    public static ** bind(android.view.View);
}

# 保留 BaseActivity 和 BaseFragment 的泛型信息
-keep class info.muge.appshare.base.BaseActivity {
    *;
}
-keep class info.muge.appshare.base.BaseFragment {
    *;
}

# 保留所有继承自 BaseActivity 和 BaseFragment 的类
-keep class * extends info.muge.appshare.base.BaseActivity {
    *;
}
-keep class * extends info.muge.appshare.base.BaseFragment {
    *;
}

# 保留所有 Activity 类
-keep class * extends androidx.appcompat.app.AppCompatActivity {
    public <init>(...);
}

# ==================== Kotlin 相关 ====================
# 保留 Kotlin 元数据注解
-keep class kotlin.Metadata { *; }

# 保留 Kotlin 反射相关
-keep class kotlin.reflect.** { *; }
-keep interface kotlin.reflect.** { *; }

# ==================== AndroidX 相关 ====================
# AndroidX Core
-keep class androidx.core.** { *; }
-dontwarn androidx.core.**

# Material Design Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ==================== BRV RecyclerView 框架 ====================
-keep class com.drake.brv.** { *; }
-dontwarn com.drake.brv.**

# ==================== 其他第三方库 ====================
# Pinyin4j
-keep class net.sourceforge.pinyin4j.** { *; }
-dontwarn net.sourceforge.pinyin4j.**

# XXPermissions
-keep class com.hjq.permissions.** { *; }
-dontwarn com.hjq.permissions.**

# ==================== 调试信息 ====================
# 保留行号信息，便于调试崩溃日志
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
