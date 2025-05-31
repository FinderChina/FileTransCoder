echo off

set "JAR_HOME=%cd%"
set "JAVA_HOME="

echo If your computer can run [javac] commands, you can execute this bat file. 
echo Otherwise, please follow the prompts to set up the 'JAVA_HOME' environment variable.
echo eg: set "JAVA_HOME=C:/Program Files/Java/jdk1.8.0_191/bin/"

set /p num="Enter a number to compile the Java file and package it: "

set "candp=0"
if "%num%" geq "0" if "%num%" leq "9" (
    set "candp=1"
)
 
if "%candp%"=="1" (
    "%JAVA_HOME%javac.exe" -encoding UTF-8 -d %JAR_HOME%/TransCoder %JAR_HOME%/../TransCoder.java
    copy %JAR_HOME%\MANIFEST.MF2 %JAR_HOME%\MANIFEST.MF /Y
    pause
    "%JAVA_HOME%jar.exe" cvfm %JAR_HOME%/TransCoder.jar %JAR_HOME%/MANIFEST.MF -C %JAR_HOME%/TransCoder/ .
    pause
    del /Q %JAR_HOME%\TransCoder\*.*
    del /Q %JAR_HOME%\MANIFEST.MF
) else (
    echo Directly run the TransCoder.jar file ...
)

%JAVA_HOME%java -jar TransCoder.jar
pause