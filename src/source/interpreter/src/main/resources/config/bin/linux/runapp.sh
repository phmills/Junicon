#!/bin/csh -f

#=========================================================================
# Copyright (c) 2015 Orielle, LLC.
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#  1. Redistributions of source code must retain the above copyright
#     notice, this list of conditions and the following disclaimer.
#  2. Redistributions in binary form must reproduce the above copyright
#     notice, this list of conditions and the following disclaimer in the
#     documentation and/or other materials provided with the distribution.
# This software is provided by the copyright holders and contributors
# "as is" and any express or implied warranties, including, but not
# limited to, the implied warranties of merchantability and fitness for
# a particular purpose are disclaimed. In no event shall the copyright
# holder or contributors be liable for any direct, indirect, incidental,
# special, exemplary, or consequential damages (including, but not
# limited to, procurement of substitute goods or services; loss of use,
# data, or profits; or business interruption) however caused and on any
# theory of liability, whether in contract, strict liability, or tort
# (including negligence or otherwise) arising in any way out of the use
# of this software, even if advised of the possibility of such damage.
#=========================================================================

#====
# Runs the junicon interpreter.  Also performs compile and extract tasks.
#
# This script will either interpret, translate, compile, or extract.
# These options are mutually exclusive.
# If compiling and the file does not end in Java, it will first translate.
# If explicitly asked to translate, compile or extract options are ignored.
#
# The interpreter is formed by appending junicon's jar file to this script.
# The concatenated file can double both as a jar/zip and as an executable,
# because zip files read from the end, while executables read from the front.
#
# If JAVA_HOME is defined, uses that for java; otherwise searches the PATH.
# If CLASSPATH is defined, adds this jarfile to the classpath.
#
# Sets the environment variable JARRUNNER_ARG0 to this executable jar filename.
# If running under Windows, also sets JARRUNNER_ISWINDOWS=true.
#====

#=============================================================================
# Process command-line options
#=============================================================================
set OPTIONS = ""			# Pass unrecognized options
unset inv_opt has_more_opt
unset eopt elopt ewopt Copt Ropt Jopt oopt
unset Xcopt Xropt
unset performCompile performExtract
unset mopt eopt  # manifest, entrypoint

#====
# Scan for compile and extract options
#====
set argvCopy = ($argv:q)  # Save copy of argv
while (($#argv > 0) && ("$1" =~ -*))    # Process command-line options
  switch ("$1")
	case "-e":
		shift ; set eopt ; breaksw
	case "-el":
		shift ; set elopt ; breaksw
	case "-ew":
		shift ; set ewopt ; breaksw
	case "-C":
		shift ; set Copt ; breaksw
	case "-R":
		shift ; set Ropt ; breaksw
	case "-J":
		shift ; set Jopt ; breaksw
	case "-Xc":
		shift ; set Xcopt ; breaksw
	case "-Xr":
		shift ; set Xropt ; breaksw
	case "-o":
		shift ; set oopt="$1" ; shift ; breaksw
        default:
		set has_more_opt ; break
		# echo "Invalid option" $1 ; set inv_opt ; break
		# set OPTIONS = "$OPTIONS $1" ; shift ; breaksw
  endsw
end

if ($?eopt || $?elopt || $?ewopt || $?Copt) set performCompile
if ($?Xcopt || $?Xropt) set performExtract

if ($?performCompile && (($#argv < 1) || $?has_more_opt)) set inv_opt
if ($?performExtract && (($#argv > 0) || $?has_more_opt)) set inv_opt

if ($?inv_opt) then
  echo "USAGE: junicon [-eCRETG] [-o outputfile] file(s)"
  echo "ACTION: Translates filename to Java, and"
  echo "        compiles it into a self-contained executable jar file."
  echo "        If filename already ends in .java, skips translation."
  echo "        If translating to Java, only one filename is allowed."
  echo "OPTIONS:[-e (produce executable for the current operating system)]"
  echo "	[-el (override for Linux executable)]"
  echo "	[-ew (override for Windows executable)]"
  echo "	[-C (compile only, to jar file)]"
  echo "	[-R (do not include runtime in executable)]"
  echo "	[-J (Java syntax)]"
  echo "	[-E (preprocess only)]"
  echo "	[-T (just translate to Java)]"
  echo "	[-G (just translate to Groovy)]"
  echo "	[-Ic configDirectory (for startup files)]"
  echo " "
  echo "USAGE: junicon [-Xc] [-Xr]"
  echo "ACTION: Extract files from the junicon runtime."
  echo "OPTIONS:[-Xc (extract config directory holding startup files)]"
  echo "	[-Xr (extract runtime jar)]"

  exit(1)
endif

#=============================================================================
# Set up environment
#=============================================================================

#====
# Program locations
#====
unset isWindows
if (("$OSTYPE" == cygwin) || ("$OSTYPE" == Interix)) set isWindows
set java = "java"
set javac = "javac"
set jar = "jar"
if ($?isWindows) then
	set java = "java.exe"
	set javac = "javac.exe"
	set jar = "jar.exe"
endif
if ($?JAVA_HOME) then
	set java = "$JAVA_HOME/bin/$java"
	set javac = "$JAVA_HOME/bin/$javac"
	set jar = "$JAVA_HOME/bin/$jar"
endif

#====
# Set up classpath
#====
set thisJarname = "$0"
if ($?isWindows) then
    if ("$OSTYPE" == cygwin) then
    	set thisJarname = `cygpath -w -m "$thisJarname"`
    else
	set thisJarname = "$thisJarname:as^\^/^"
	set thisJarname = "$thisJarname:s^/dev/fs/C/^C:/^"
    endif
    setenv JARRUNNER_ISWINDOWS "true"
endif
setenv JARRUNNER_ARG0 "$thisJarname"
setenv JARRUNNER_IN_CMD_FILE "true"

if ($?CLASSPATH) then
  if ($?isWindows) then
	set CLASSPATH = "${CLASSPATH};${thisJarname}"
  else
	set CLASSPATH = "${CLASSPATH}:${thisJarname}"
  endif
endif

#====
# Set variables
#====
set filename = "$1"
set classname = "$filename:t:r"
set lastclassname = "$argv[$#argv]:t:r"
set javaname="${classname}.java"
set jarname = "${lastclassname}.jar"
set outputfile = "$lastclassname"
if ($?oopt) then
	set outputfile = "$oopt"
endif
unset isJava
if ("$1:e" == java) set isJava

set thisJarFromParent = "$thisJarname"
if (("$thisJarname" !~ "/*") && ("$thisJarname" !~ "*:*" )) then
	set thisJarFromParent = "../$thisJarname"
endif
set sfx = "config/bin/linux/jarrunner.sh"
if ($?ewopt || ($?isWindows && (! $?elopt))) then
	set outputfile = "${outputfile}.exe"
	set sfx = "config/bin/windows/jarrunner.exe"
endif
set manifest = "Manifest.mf"
if ($?mopt) then
	set manifest = "$mopt"
endif
set jarrunner = "edu.uidaho.junicon.runtime.util.JarRunner"
	
#=============================================================================
# Just run the Junicon interpreter, if not compile or extract
#=============================================================================
if (! ($?performCompile || $?performExtract)) then
    if ($?CLASSPATH) then
	"$java" -cp "$CLASSPATH" "$jarrunner" "$thisJarname" "manifest" $argvCopy[*]:q
    else
	"$java" -jar "$thisJarname" $argvCopy[*]:q
    endif
    exit
endif

#=============================================================================
# Extract files from the junicon runtime, if needed
#=============================================================================
if ($?performExtract) then
  if ($?Xcopt) then
    if (-d "config") then
	echo "Directory 'config' already exists. Overwrite? (y/n)"
	set response = $<
	if ("$response" !~ y*) exit
    endif
    echo "Extracting the Junicon config directory..."
    "$jar" xf "$thisJarname" "config"
  endif

  if ($?Xropt) then
    if (-d "classes") then
	echo "Directory 'classes' already exists. Overwrite? (y/n)"
	set response = $<
	if ("$response" !~ y*) exit
    endif
    if (! -d "classes") mkdir classes
    cd classes
    echo "Extracting the Junicon runtime..."
    "$jar" xf "$thisJarFromParent" "edu/uidaho/junicon/runtime"
    echo "Creating the junicon-runtime.jar file..."
    "$jar" cf "junicon-runtime.jar" edu/uidaho/junicon/runtime/*
    cd ..
    mv -i classes/junicon-runtime.jar .
    echo "Removing temporary classes directory"
    rm -I -r classes
  endif

  exit
endif

#=============================================================================
# Compile
#=============================================================================

#====
# Run the Junicon interpreter to generate Java code
#====
if (! $?isJava) then
    echo "Translating $filename to Java..."
    if (-e "$javaname") then
	echo "$javaname already exists."
	rm -i "$javaname"
    endif
    if ($?CLASSPATH) then
	"$java" -cp "$CLASSPATH" "$jarrunner" "$thisJarname" "manifest" -T $argvCopy[*]:q
    else
	"$java" -jar "$thisJarname" -T $argvCopy[*]:q
    endif
endif

#====
# Compile the generated code
#====
echo "Compiling..."
if (-d "classes") then
    echo "Temporary directory 'classes' already exists. Remove it ? (y/n)"
    set response = $<
    if ("$response" =~ y*) rm -r classes
endif
if (! -d "classes") mkdir classes
if (! $?CLASSPATH) set CLASSPATH="$thisJarname"
if ($?isJava) then
   cp -ip $*:q classes
   "$javac" -d classes -classpath "$CLASSPATH" $*:q
else
   cp -ip "$javaname" classes
   "$javac" -d classes -classpath "$CLASSPATH" "$javaname"
endif

#====
# Copy in the runtime
#====
if (! $?Ropt) then
  echo "Copying in the Junicon runtime..."
  cd classes
  "$jar" xf "$thisJarFromParent" "edu/uidaho/junicon/runtime"
  cd ..
endif

#====
# Jar the compiled code
#====
echo "Creating jar file..."
cd classes
if (-f "../$manifest") then
    "$jar" cmf "../$manifest" "$jarname" *
else
    "$jar" cf "$jarname" *
endif
cd ..
mv -i "classes/$jarname" .
#====
# "$jar" cfe "$jarname" "${eopt}" "${eopt}.class" *
#====

#====
# Produce executable: append jar file to the executable header
#====
if (! $?Copt) then
  echo "Creating executable..."
  cd classes
  "$jar" xf "$thisJarFromParent" "$sfx"
  cd ..
  if (-e "$outputfile") then
	echo "$outputfile already exists."
	rm -i "$outputfile"
  endif
  cat "classes/${sfx}" "$jarname" > "$outputfile"
  chmod +x "$outputfile"
endif

#====
# Cleanup
#====
echo "Cleaning up..."
echo "Removing temporary classes directory"
rm -I -r classes
if (! $?Copt) rm -i "$jarname"
rm -i "$javaname"
rm -i "$manifest"

echo "Done."
exit

#==== END OF FILE

