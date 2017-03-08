#!/bin/csh -f

#=========================================================================
# Execute Java jar file.
#
# This script runs java on the main-class in the appended jar file.
# To create an application, the jar file should be appended to this executable,
# and its manifest should hold the main-class to be executed.
# The concatenated file can double both as a jar/zip and as an executable,
# because zip files read from the end, while executables read from the front.
#
# If JAVA_HOME is defined, uses that for java; otherwise searches the PATH.
# If CLASSPATH is defined, adds this jarfile to the classpath, and then
#	invokes JarRunner to reflectively invoke the main-class in the manifest.
#	This workaround is because "java -jar" ignores the user classpath.
# Otherwise, uses "java -jar" to invoke the self-contained executable jar.
#
# Sets the environment variable JARRUNNER_ARG0 to this executable jar filename.
# If running under Windows, also sets JARRUNNER_ISWINDOWS=true.
#=========================================================================

#====
# Program locations
#====
unset isWindows
if (("$OSTYPE" == cygwin) || ("$OSTYPE" == Interix)) set isWindows
set java = "java"
if ($?isWindows) set java = "java.exe"
if ($?JAVA_HOME) set java = "$JAVA_HOME/bin/$java"

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
		set CLASSPATH = "${CLASSPATH};$thisJarname"
	else
		set CLASSPATH = "${CLASSPATH}:$thisJarname"
	endif
endif
set jarrunner = "edu.uidaho.junicon.runtime.util.JarRunner"

#====
# Run main-class in jar's manifest
#====
if ($?CLASSPATH) then
	"$java" -cp "$CLASSPATH" "$jarrunner" "$thisJarname" "manifest" $*:q
else
	"$java" -jar "$thisJarname" $*:q
endif

exit

#====
# #!/bin/sh
# exec java -jar "$0" "$@"
# exit
#====
 
#==== END OF FILE

