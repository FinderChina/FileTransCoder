echo off

set "JAR_HOME=%cd%"
set "JAVA_HOME="
rem eg: JAVA_HOME=C:/Program Files/Java/jdk1.8.0_191/bin/

set /p num="Enter a number to compile the Java file and package it: "

set "candp=0"
if "%num%" geq "0" if "%num%" leq "9" (
    set "candp=1"
)
 
if "%candp%"=="1" (
    echo 1/6. prepare MF file
    "%JAVA_HOME%javac.exe" -encoding UTF-8 -cp "..\lib\*" -d %JAR_HOME%\TransCoder %JAR_HOME%\..\*.java
    copy %JAR_HOME%\MANIFEST.MF3 %JAR_HOME%\MANIFEST.MF /Y

    echo 2/6. create temp files
    if exist temp-jar-content rmdir /s /q temp-jar-content
    mkdir temp-jar-content

    echo 3/6. copy java files to temp directoty
    xcopy "%JAR_HOME%\..\*.java" temp-jar-content /Y

    echo 4/6. copy class files to temp directoty
    xcopy "%JAR_HOME%\TransCoder\*" temp-jar-content /S /Y

    echo 5/6. package jar with sources
    "%JAVA_HOME%jar.exe" cvfm  %JAR_HOME%/FileManagerUI.jar %JAR_HOME%/MANIFEST.MF -C temp-jar-content/ .

    "%JAVA_HOME%java.exe" -version
    pause 
    echo 6/6. clean up temp files
    rmdir /S /Q temp-jar-content
    del /Q %JAR_HOME%\TransCoder\*.*
    del /Q %JAR_HOME%\MANIFEST.MF
) else (
    echo Directly run the FileManagerUI.jar file ...
)

%JAVA_HOME%java -jar FileManagerUI.jar
pause
