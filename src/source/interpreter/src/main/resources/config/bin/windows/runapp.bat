@echo off

REM #=========================================================================
REM # Copyright (c) 2015 Orielle, LLC.
REM # All rights reserved.
REM #
REM # Redistribution and use in source and binary forms, with or without
REM # modification, are permitted provided that the following conditions
REM # are met:
REM #  1. Redistributions of source code must retain the above copyright
REM #     notice, this list of conditions and the following disclaimer.
REM #  2. Redistributions in binary form must reproduce the above copyright
REM #     notice, this list of conditions and the following disclaimer in the
REM #     documentation and/or other materials provided with the distribution.
REM # This software is provided by the copyright holders and contributors
REM # "as is" and any express or implied warranties, including, but not
REM # limited to, the implied warranties of merchantability and fitness for
REM # a particular purpose are disclaimed. In no event shall the copyright
REM # holder or contributors be liable for any direct, indirect, incidental,
REM # special, exemplary, or consequential damages (including, but not
REM # limited to, procurement of substitute goods or services; loss of use,
REM # data, or profits; or business interruption) however caused and on any
REM # theory of liability, whether in contract, strict liability, or tort
REM # (including negligence or otherwise) arising in any way out of the use
REM # of this software, even if advised of the possibility of such damage.
REM #=========================================================================

REM #====
REM # Runs the junicon interpreter.  Also performs compile and extract tasks.
REM #
REM # This script will either interpret, translate, compile, or extract.
REM # These options are mutually exclusive.
REM # If compiling and the file does not end in Java, it will first translate.
REM # If explicitly asked to translate, compile or extract options are ignored.
REM #
REM # The interpreter is formed by sandwitching this script between
REM # batrunner.exe and junicon's jar file: [batrunner.exe][this bat][jar file].
REM # The concatenated file can double both as a jar/zip and as an executable,
REM # because zip files read from the end while executables read from the front.
REM #
REM # Batrunner runs this script by first scanning it into a string
REM # from the sandwitched executable, and then running it in memory via
REM # cmd /q /s /v:on /c "command".   No temporary files are used.
REM # Batrunner prefixes this script with the command-line arguments,
REM # and also sets JARRUNNER_ARG0 to the original jar file name from argv[0].
REM #
REM # If JAVA_HOME is defined, uses that for java; otherwise searches the PATH.
REM # If CLASSPATH is defined, adds this jarfile to the classpath.
REM #
REM # If the environment variable JARRUNNER_ARG0 is defined, uses that as
REM # the executable jar filename; otherwise sets JARRUNNER_ARG0 to argv[0].
REM # ALso sets JARRUNNER_ISWINDOWS=true.
REM #====

REM #====
REM # The following prefix will be inserted by BatRunner:
REM #	(setlocal enabledelayedexpansion) & (
REM #	set args=...command line arguments...) & (
REM #====
#!/batrunner begin
set eopt=) & (
set elopt=) & (
set ewopt=) & (
set Copt=) & (
set Ropt=) & (
set Jopt=) & (
set oopt=) & (
set Xcopt=) & (
set Xropt=) & (
set performCompile=) & (
set performExtract=) & (

if defined args (set argsCopy=!args!) else (set argsCopy=)) & (
set invOpt=) & (
set hasMoreOpt=) & (
set lastArg=) & (
set nonOptions=) & (
set useNext=) & (

for %i in (!args!) do ((
  set option=%~i) & (
  set isOption=!option:~0,1!) & (
  if defined useNext ((
	set oopt=%~1) & (
	set useNext=)
  ) else (
	if "%~i"=="-e" ( set eopt=1 & set performCompile=1
	) else if "%~i"=="-el" ( set elopt=1 & set performCompile=1
	) else if "%~i"=="-ew" ( set ewopt=1 & set performCompile=1
	) else if "%~i"=="-C"  ( set Copt=1 & set performCompile=1
	) else if "%~i"=="-R"  ( set Ropt=1
	) else if "%~i"=="-J"  ( set Jopt=1
	) else if "%~i"=="-Xc"  ( set Xcopt=1 & set performExtract=1
	) else if "%~i"=="-Xr"  ( set Xropt=1 & set performExtract=1
	) else if "%~i"=="-o"  ( set useNext=1
	) else if "!isOption!"=="-" ( set hasMoreOpt=1
	) else (( set lastArg=%~i) & (
		  if defined nonOptions (set nonOptions=!nonOptions! %i) else (
		 	set nonOptions=%i))
	)
  ))
)) & (

if defined performCompile ((
  if not defined lastArg (set invOpt=1)) & (
  if defined hasMoreOpt (set invOpt=1))
)) & (

if defined performExtract ((
  if defined lastArg (set invOpt=1)) & (
  if defined hasMoreOpt (set invOpt=1))
)) & (

if defined invOpt ((
  echo USAGE: junicon ^[-eCRETG^] ^[-o outputfile^] files) & (
  echo ACTION: Translates filename to Java, and) & (
  echo         compiles it into a self-contained executable jar file.) & (
  echo         If filename already ends in .java, skips translation.) & (
  echo         If translating to Java, only one filename is allowed.) & (
  echo OPTIONS:^[-e ^(produce executable for current operating system^)^]) & (
  echo 	^[-el ^(override for Linux executable^)^]) & (
  echo 	^[-ew ^(override for Windows executable^)^]) & (
  echo 	^[-C ^(compile only, to jar file^)^]) & (
  echo 	^[-R ^(do not include runtime in executable^)^]) & (
  echo 	^[-J ^(java syntax^)^]) & (
  echo 	^[-E ^(preprocess only^)^]) & (
  echo 	^[-T ^(just translate to Java^)^]) & (
  echo 	^[-G ^(just translate to Groovy^)^]) & (
  echo 	^[-Ic configDirectory ^(for startup files^)^]) & (
  echo USAGE: junicon ^[-Xc^] ^[-Xr^]) & (
  echo ACTION: Extract files from the junicon runtime.) & (
  echo OPTIONS:^[-Xc ^(extract config directory holding startup files^)^]) & (
  echo 	^[-Xr ^(extract runtime jar^)^]) & (
  exit /b)
)) & (

set java=java.exe) & (
set javac=javac.exe) & (
set jar=jar.exe) & (
if defined JAVA_HOME ((
	set java=%JAVA_HOME%/bin/java.exe) & (
	set javac=%JAVA_HOME%/bin/javac.exe) & (
	set jar=%JAVA_HOME%/bin/jar.exe)
)) & (

for %A in ("%JARRUNNER_ARG0%") do ( set thisJarname=%~fA)) & (
if defined CLASSPATH set CLASSPATH=%CLASSPATH%;!thisJarname!) & (

for %A in ("!lastArg!") do ( set filename=%~A)) & (
for %A in ("!lastArg!") do ( set classname=%~nA)) & (
for %A in ("!lastArg!") do ( set extension=%~xA)) & (
set lastclassname=!classname!) & (
set javaname=!classname!.java) & (
set jarname=!lastclassname!.jar) & (
set outputfile=!lastclassname!) & (
if defined oopt set outputfile=!oopt!) & (
set isJava=) & (
if "!extension!"==".java" set isJava) & (

set thisJarFromParent=!thisJarname!) & (
set sfx=config\bin\linux\jarrunner.sh) & (
if not defined elopt ((
	set outputfile=!outputfile!.exe) & (
	set sfx=config\bin\windows\jarrunner.exe)
)) & (
set manifest=Manifest.mf) & (
set jarrunner=edu.uidaho.junicon.runtime.util.JarRunner) & (

if not defined performCompile ((
  if not defined performExtract ((
    if defined CLASSPATH (
      if defined argsCopy (
	call "!java!" -cp "!CLASSPATH!" !jarrunner! "!thisJarname!" "manifest" !argsCopy!
      ) else (
	call "!java!" -cp "!CLASSPATH!" !jarrunner! "!thisJarname!" "manifest"
      )
    ) else (
      if defined argsCopy (
	call "!java!" -jar "!thisJarname!" !argsCopy!
      ) else (
	call "!java!" -jar "!thisJarname!"
      )
    )) & (
    exit /b)
  ))
)) & (

if defined performExtract ((
  if defined Xcopt ((
    if exist config ((
	echo config already exists.) & (
	rmdir /S config)
    )) & (
    echo Extracting the Junicon config directory...) & (
    call "!jar!" xf "!thisJarname!" config)
  )) & (

  if defined Xropt ((
    if exist classes ((
	echo Temporary directory classes already exists.) & (
	rmdir /S classes)
    )) & (
    if not exist classes mkdir classes) & (
    cd classes) & (
    echo Extracting the Junicon runtime...) & (
    call "!jar!" xf "!thisJarFromParent!" "edu/uidaho/junicon/runtime") & (
    echo Creating the junicon-runtime.jar file...) & (
    call "!jar!" cf "junicon-runtime.jar" edu/uidaho/junicon/runtime/^*) & (
    cd ..) & (
    move /-Y classes\junicon-runtime.jar .) & (
    echo Removing temporary classes directory) & (
    rmdir /S classes)
  )) & (
  exit /b)
)) & (

if not defined isJava ((
    echo Translating !filename! to Java...) & (
    if exist "!javaname!" ((
	echo !javaname! already exists.) & (
	del /p "!javaname!")
    )) & (
    if defined CLASSPATH (
	call "!java!" -cp "!CLASSPATH!" !jarrunner! "!thisJarname!" "manifest" -T !argsCopy!
    ) else (
	call "!java!" -jar "!thisJarname!" -T !argsCopy!
    ))
)) & (

echo Compiling...) & (
if exist classes ((
	echo Temporary directory classes already exists.) & (
	rmdir /S classes)
)) & (
if not exist classes mkdir classes) & (
if not defined CLASSPATH ( set CLASSPATH=!thisJarname!)) & (

if defined isJava ((
    copy /-Y !nonOptions! classes) & (
    call "!javac!" -d classes -classpath "!CLASSPATH!" !nonOptions!)
) else ((
    copy /-Y "!javaname!" classes) & (
    call "!javac!" -d classes -classpath "!CLASSPATH!" "!javaname!")
)) & (

if not defined Ropt ((
  echo Copying in the Junicon runtime...) & (
  cd classes) & (
  call "!jar!" xf "!thisJarFromParent!" "edu/uidaho/junicon/runtime") & (
  cd ..)
)) & (

echo Creating jar file...) & (
cd classes) & (
if exist "../!manifest!" (
    call "!jar!" cmf "../!manifest!" "!jarname!" ^*
) else (
    call "!jar!" cf "!jarname!" ^*
)) & (
cd ..) & (
move /-Y classes\!jarname! .) & (

if not defined Copt ((
  echo Creating executable...) & (
  cd classes) & (
  call "!jar!" xf "!thisJarFromParent!" "!sfx!") & (
  cd ..) & (
  copy /-y /b "classes\!sfx!" + "!jarname!" "!outputfile!")
)) & (

echo Cleaning up...) & (
echo Removing temporary classes directory) & (
rmdir /S classes) & (
if not defined Copt ( del /p "!jarname!" )) & (
del /p "!javaname!") & (
del /p "!manifest!") & (

echo Done.)
#!/batrunner end
