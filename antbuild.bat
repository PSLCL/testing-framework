@echo off
setlocal
if "[%1]"=="[]" goto nuke
set cleanbuild="true"
goto first

:nuke
rd /s \Users\cadams\.ivy2\cache
rd /s \Users\cadams\.ivy2\local\org.opendof.tools-interface-repository

:first
cd interface-repository-data-accessor
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto core
echo "interface-repository-data-accessor build failed"
goto exit

:core
echo "interface-repository-data-accessor build ok"
cd ..\interface-repository-core
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto allseen
echo "interface-repository-core build failed"
goto exit

:allseen
echo "interface-repository-core build ok"
cd ..\interface-repository-allseen
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto opendof
echo "interface-repository-allseen build failed"
goto exit

:opendof
echo "interface-repository-allseen build ok"
cd ..\interface-repository-opendof
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto mysql
echo "interface-repository-opendof build failed"
goto exit

:mysql
echo "interface-repository-opendof build ok"
cd ..\interface-repository-mysql
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto servlet
echo "interface-repository-mysql build failed"
goto exit

:servlet
echo "interface-repository-mysql build ok"
cd ..\interface-repository-servlet
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto cli
echo "interface-repository-servlet build failed"
goto exit

:cli
echo "interface-repository-servlet build ok"
cd ..\interface-repository-cli
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto web
echo "interface-repository-cli build failed"
goto exit

:web
echo "interface-repository-cli build ok"
cd ..\interface-repository-web
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto app
echo "interface-repository-web build failed"
goto exit

:app
echo "interface-repository-web build ok"
cd ..\interface-repository
IF "%cleanbuild%"=="" (call ant -q dist-app) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto ok
echo "interface-repository build failed"
goto exit

:ok
cd ..\..
echo "interface-repository build ok"

:exit