@echo off
chcp 65001 >nul
title ShiGuangJi Build ^& Install
setlocal
rem ===== 构建环境（已随项目放在本目录 dev-tools 下，不占 C 盘）=====
set "PROJECT=%~dp0"
set "JAVA_HOME=%PROJECT%dev-tools\jdk-17.0.2"
set "GRADLE_USER_HOME=%PROJECT%dev-tools\gradle-home"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "ADB=%PROJECT%dev-tools\android-sdk\platform-tools\adb.exe"
cd /d "%~dp0"

echo ==============================================
echo   拾光记 · 编译正式签名 APK
echo ==============================================
call gradlew.bat assembleRelease --console=plain
if errorlevel 1 (
  echo.
  echo [失败] 编译出错，请把上方报错信息发给开发者
  pause
  exit /b 1
)

echo.
echo ==============================================
echo   检查手机连接...
echo ==============================================
"%ADB%" get-state >nul 2>&1
if errorlevel 1 (
  echo [提示] 没有检测到手机。APK 已生成在：
  echo     app\build\outputs\apk\release\app-release.apk
  echo     以及项目根目录：拾光记-release.apk
  echo 可以手动把它传到手机上安装。
  echo 想用数据线安装：手机开 USB 调试并授权电脑后，重新运行本脚本。
  start "" explorer.exe "%~dp0"
  pause
  exit /b 0
)

echo 开始安装到手机...
"%ADB%" install -r "app\build\outputs\apk\release\app-release.apk"
if errorlevel 1 (
  echo.
  echo [提示] 安装未成功：
  echo   - 手机上弹出授权窗口时请点「允许」
  echo   - 小米：开发者选项里打开「USB 安装」
  echo   - OPPO：允许「USB 安装」/关闭安装安全拦截
  pause
  exit /b 1
)
echo.
echo [完成] 拾光记已经装到你的手机上了，去桌面找「拾光记」吧！
pause
