# 拾光记混淆规则（当前 debug 不混淆；未来开启 release 混淆时保留以下规则）
-keep class com.shiguangji.app.core.ThemePack { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
