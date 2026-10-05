# 拾光记 ☀️

一个**完全离线**的安卓日常记录应用：写日期、存照片、给每张照片写说明、分栏目收藏、
在「那年今日」回望过去、在「悄悄话」里只对自己说话。

> 纯离线设计：Manifest 不声明 `INTERNET` 权限，数据从系统层面就不会离开你的手机。

## 功能一览

| 功能 | 说明 |
|---|---|
| 📝 记录 | 选日期 + 多张照片（系统相册，免权限）+ 每张照片独立说明 + 正文 |
| 🗂 栏目 | 预置 5 个栏目，可新建/改名/换图标换色；删除栏目不删记录 |
| 🕰 那年今日 | 首页提醒卡 + 独立页面，回看往年同月同日的记录 |
| 🌙 悄悄话 | 独立私密板块：4 位 PIN + 指纹/面部（设备支持时），SHA-256 加盐存储 |
| 🎨 主题包 | 配色方案 = 一份 JSON 文件；内置 5 套，支持导入/导出/跨设备移植 |
| 💾 备份 | 数据库 + 全部照片打包 zip，随时导出恢复；校验版本兼容性 |
| 🔒 隐私 | 无网络权限、无广告、无追踪；照片存应用专属目录，卸载即清理 |

## 安装到手机（OPPO / Xiaomi 均适用）

1. 手机打开 **设置 → 关于手机 → 版本号**，连点 7 次打开开发者模式
2. **设置 → 其他设置（OPPO）/ 更多设置（小米）→ 开发者选项**，开启 **USB 调试**
3. 数据线连接电脑，手机弹窗选择「传输文件」并允许 USB 调试授权
4. 双击项目根目录的 **`编译并安装到手机.bat`**，等待完成即可
   （或手动：`gradlew.bat assembleDebug`，然后 `adb install -r app\build\outputs\apk\debug\app-debug.apk`）

小米手机若提示安装受限：开发者选项里打开「USB 安装」；
OPPO 若显示「安全守护」拦截：允许安装来源应用即可。

适配说明：已按 Android 15/16 规范处理全面屏手势区与挖孔避让（Reno12 Pro 四曲面边缘、
Xiaomi 17 均正常）；120Hz 高刷下动画全速；深色主题跟随主题包或系统取色。

## 开发与构建

- 技术栈：Kotlin 2.0 + Jetpack Compose + Material 3 + Room + DataStore + Coil + Biometric
- 最低支持 Android 8.0（minSdk 26），目标 Android 15（target 35）
- Android Studio：直接 Open 本目录，等待 Sync 完成即可 Run
- 命令行：`gradlew.bat assembleDebug`（需要 JDK 17 与 Android SDK，
  本机已配置在 `local.properties`）

## 存储规范（v1 起冻结）

```
Android/data/com.shiguangji.app/
├── files/Pictures/photos/<年>/<月>/<uuid>.<ext>   ← 照片归档目录
└── databases/shiguangji.db                        ← Room 数据库
```

- 数据库只存照片**相对路径**，备份恢复 = 目录整体搬家
- 备份 zip 结构：`manifest.json` + `db/shiguangji.db` + `photos/**`

## 版本兼容承诺（升级不丢数据）

1. **数据库**：Room `version` 递增时必须在 `AppDatabase.MIGRATIONS` 追加迁移对象，
   项目**未开启**破坏性迁移兜底——结构升级失败宁可报错也不清数据；
   每个版本的建表语句自动导出到 `app/schemas/` 供比对与测试
2. **主题包**：JSON 解析忽略未知字段、缺失字段回退默认值——
   旧版 App 能打开未来格式的主题包，精简主题包也能用
3. **备份文件**：manifest 记录 `schemaVersion`，来自更高版本的备份会拒绝恢复并提示升级
4. **设置存储**：DataStore 只增不改，读取全部带默认值

### 下一版改数据库的模板

```kotlin
// AppDatabase.kt
version = 2  // ① 版本 +1

val MIGRATIONS: Array<Migration> = arrayOf(
    object : Migration(1, 2) {  // ② 追加迁移，不要删旧的
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE entries ADD COLUMN mood TEXT NOT NULL DEFAULT ''")
        }
    }
)
```

## 主题包格式

```json
{
  "format": "shiguangji-theme-pack",
  "version": 1,
  "id": "t-night",
  "name": "月见夜话",
  "author": "你",
  "dark": true,
  "colors": { "paper": "#16131D", "brand": "#D9A05B", "grad1": "#C98A3D", "grad2": "#A96A24" }
}
```

颜色支持 `#RRGGBB` / `#AARRGGBB` / `rgba(r,g,b,a)`；`colors` 里任何字段都可省略（回退默认）。
把文件发给朋友或在「主题外观 → 导入主题包」选择即可换装。

## 目录结构

```
app/src/main/java/com/shiguangji/app/
├── ShiGuangApp.kt        # Application + 手工依赖图
├── MainActivity.kt       # 单 Activity + Compose 导航 + 底栏
├── core/                 # 主题包引擎 / 照片归档 / 备份恢复 / PIN
├── data/                 # Room 实体 / DAO / 仓库 / 设置
├── ui/                   # 10 个页面 + 主题映射 + 通用组件
└── util/Dates.kt         # 日期工具（那年今日匹配）
```
