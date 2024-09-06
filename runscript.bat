@echo off
:: If you already have a valid JAVA_HOME environment variable set, feel free to comment the below two lines
set JAVA_HOME=C:\Users\lenovo\.jdks\openjdk-21+35_windows-x64_bin\jdk-21
set path=%JAVA_HOME%\bin;%path%
set path=C:\Users\lenovo\.m2\repository\allure\allure-2.25.0\bin;%path%
cd Test-Output/Reports/Allure
allure generate allure-results --clean -o allure-report
open allure-report
pause
exit