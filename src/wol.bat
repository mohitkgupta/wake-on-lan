@echo off

if /i ""%1"" equ ""-all""    goto run
if /i ""%1"" equ ""-single"" goto run
if /i ""%1"" equ ""-range""  goto run
if /i ""%1"" equ ""-help""   goto help
goto help

:run
rem echo %JAVA_HOME%\bin
"%JAVA_HOME%/bin/java" -classpath .;wol.jar;lib/activation.jar;lib/commons-beanutils.jar;lib/commons-collections.jar;lib/commons-digester.jar;lib/commons-logging.jar;lib/log4j.jar;"%JAVA_HOME%/lib/tools.jar" com.vedantatree.wol.WOLLauncher %1
goto end

:help
echo WOL is a command line utility to boot system remotely.
echo Usage: wol (option)
echo where possible options include:
echo     -all       Boot all the system mentioned in configuration file
echo     -single    Boot single system mentioned in configuration file
echo     -range     Boot all the system between the range mentioned in configuration file
goto end


:end
