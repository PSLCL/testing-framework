@ECHO OFF
ECHO Running testrun.bat
timeout /t 2
if "%1" == "pass" (
  ECHO Pass
  EXIT /B 0
)
if "%1" == "fail" (
  ECHO Fail
  EXIT /B 1
)
