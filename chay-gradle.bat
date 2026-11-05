@echo off
rem ==========================================================================
rem  Chay Gradle cho backend MedCare tu mot duong dan KHONG DAU.
rem
rem  Ly do: Windows dang dung code page ANSI 1252, khong bieu dien duoc mot so
rem  chu tieng Viet trong ten thu muc du an. Java va Gradle truyen duong dan
rem  sang tien trinh con bang code page nay nen bi hong, gay ra loi
rem  GradleWrapperMain va ClassNotFoundException khi chay test.
rem
rem  Script tao (neu chua co) junction C:\medcare-demo tro toi thu muc du an
rem  (khong copy, khong di chuyen file) roi chay gradlew.bat tu do.
rem
rem  Cach dung:
rem    chay-gradle.bat              : bootRun, sau do mo http://localhost:8080
rem    chay-gradle.bat test         : chay unit test
rem    chay-gradle.bat build        : build jar
rem    chay-gradle.bat --stop       : dung Gradle daemon
rem ==========================================================================
setlocal
set "ASCII_ROOT=C:\medcare-demo"
set "SRC=%~dp0"
set "SRC=%SRC:~0,-1%"

if exist "%ASCII_ROOT%\chay-gradle.bat" goto run

if exist "%ASCII_ROOT%" (
  echo [LOI] %ASCII_ROOT% da ton tai nhung khong tro toi du an nay.
  echo       Hay xoa hoac doi ASCII_ROOT trong chay-gradle.bat.
  exit /b 1
)
mklink /J "%ASCII_ROOT%" "%SRC%" >nul
if errorlevel 1 (
  echo [LOI] Khong tao duoc junction %ASCII_ROOT%.
  exit /b 1
)
echo Da tao junction %ASCII_ROOT% tro toi thu muc du an.

:run
cd /d "%ASCII_ROOT%\backend"
if "%~1"=="" (
  call "%ASCII_ROOT%\backend\gradlew.bat" bootRun
) else (
  call "%ASCII_ROOT%\backend\gradlew.bat" %*
)
exit /b %ERRORLEVEL%
