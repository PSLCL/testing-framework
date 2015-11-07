@echo off
setlocal
if "[%1]"=="[]" goto nuke
set cleanbuild="true"
goto core

:nuke
rd /s \Users\cadams\.ivy2\cache\com.pslcl.dtf
rd /s \Users\cadams\.ivy2\local\com.pslcl.dtf

:core
cd dtf-core
if "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto exec
echo "dtf-core build failed"
goto exit

:exec
cd ..\dtf-exec
echo "dtf-core build ok"
if "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto art
echo "dtf-exec build failed"
goto exit

:art
cd ..\dtf-ivy-artifact
echo "dtf-exec build ok"
if "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto runner
echo "dtf-ivy-artifact build failed"
goto exit

:runner
cd ..\dtf-runner
echo "dtf-ivy-artifact build ok"
if "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto awsa
echo "dtf-runner build failed"
goto exit

:awsa
cd ..\dtf-aws-attr
echo "dtf-runner build ok"
if "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto awsr
echo "dtf-aws-attr build failed"
goto exit

:awsr
cd ..\dtf-aws-resource
echo "dtf-aws-attr build ok"
if "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto ok
echo "dtf-aws-resource build failed"
goto exit

:ok
cd ..\..
echo "dtf-aws-resource build ok"

:exit
endlocal