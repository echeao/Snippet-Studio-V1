# ==============================================================================
# Snippet Studio 生产环境混淆与代码缩减规则 (ProGuard / R8 Rules)
# ==============================================================================

# ===== 1. 基础属性保留与行号还原 =====
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===== 2. Room ORM 数据库与实体 =====
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.feige.snippetstudio.data.local.** { *; }
-keep class com.feige.snippetstudio.model.** { *; }
-dontwarn androidx.room.paging.**

# ===== 3. Eclipse JGit & Slf4j =====
# JGit 通过反射动态加载协议实现 (TransportHttp, TransportSsh 等) 和加解密算法
-keep class org.eclipse.jgit.** { *; }
-keep interface org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**

-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# 保持 Java 原生网络与加解密 Provider
-keepclassmembers class * extends java.security.Provider {
    public <init>(...);
}

# ===== 4. Sora-Editor 原生代码编辑器与 TextMate 语法引擎 =====
-keep class io.github.rosemoe.sora.** { *; }
-keepclassmembers class io.github.rosemoe.sora.** { *; }
-dontwarn io.github.rosemoe.sora.**

-keep class org.eclipse.tm4e.** { *; }
-dontwarn org.eclipse.tm4e.**

# 保持 TextMate 词法与语法定义 JSON 映射实体
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ===== 5. AndroidX DataStore & Kotlinx Coroutines =====
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn sun.misc.Unsafe
-dontwarn java.util.concurrent.Flow*

# ===== 6. WebView JavaScript 接口 (若有 HTML 预览交互) =====
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
