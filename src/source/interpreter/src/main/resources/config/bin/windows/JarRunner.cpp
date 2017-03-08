//============================================================================
// Copyright (c) 2015 Orielle, LLC.  
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// This software is provided by the copyright holders and contributors
// "as is" and any express or implied warranties, including, but not
// limited to, the implied warranties of merchantability and fitness for
// a particular purpose are disclaimed. In no event shall the copyright
// holder or contributors be liable for any direct, indirect, incidental,
// special, exemplary, or consequential damages (including, but not
// limited to, procurement of substitute goods or services; loss of use,
// data, or profits; or business interruption) however caused and on any
// theory of liability, whether in contract, strict liability, or tort
// (including negligence or otherwise) arising in any way out of the use
// of this software, even if advised of the possibility of such damage.
//============================================================================
#include <stdlib.h>	/* system, NULL, EXIT_FAILURE */
#include <stdio.h>	/* printf */
#include <string.h>
#include <process.h>
#include <iostream>	/* cout, cin, cerr */

/**
 * Execute attached Java jar file.
 *
 * This program runs java on the main-class in the appended jar file.
 * To create an application, the jar file should be appended to this executable,
 * and its manifest should hold the main-class to be executed.
 * The concatenated file can double both as a jar/zip and as an executable,
 * because zip files read from the end, while executables read from the front.
 *
 * If JAVA_HOME is defined, uses that for java; otherwise searches the PATH.
 * If CLASSPATH is defined, adds this jarfile to the classpath, and then
 *	invokes JarRunner to reflectively invoke the main-class in the manifest.
 *	This workaround is because "java -jar" ignores the user classpath.
 * Otherwise, uses "java -jar" to invoke the self-contained executable jar.
 *
 * Sets the environment variable JARRUNNER_ARG0 to this executable filename,
 * and also sets JARRUNNER_ISWINDOWS=true.
 * The executable filename will have .exe appended if not there,
 * so that the original jar file name is available within the Java application.
 * This workaround is because arg0 is dropped from Java's main(args).
 *
 * Compile under Visual Studio C++ with:
 *	vcvars32.bat
 *	cl.exe /EHsc JarRunner.cpp
 * Compile under mingw with:
 *	g++ JarRunner.cpp -static -o jarrunner.exe
 */
int main (int argc, char* argv[], char* envp[]) {
  // First option is name of process: {"java", "java", "-jar", "this", NULL}
  int optionsOffset = 3;	// Start of command-line options, if java -jar
  int maxOptionsOffset = 6;	// Start of command-line options, if JarRunner
  char *javaHome;		// From environment
  char *classpath;
  char *thisFileName;		// Arg0 to this process
  char *command;		// Invoked command
  char **mainArgs;		// Invoked arguments
  char **mainEnv;
  std::string arg0;		// Quoted argument 0 to pass to invoked command
  std::string thisExe;		// Full name of this invoked file, with .exe
  std::string javaPath;
  std::string jarClasspath;
  bool isJavaHome = false;
  int len;

  mainArgs = new char*[argc + maxOptionsOffset];

  // Get invoked file name.  Append .exe if not there, for correct jar file name
  thisFileName = argv[0];
  if (thisFileName == NULL) { thisFileName = (char *) ""; }
  thisExe = thisFileName;
  std::string suffix = ".exe";
  if (! ((thisExe.length() >= suffix.length()) &&
        	(0 == thisExe.compare (thisExe.length() - suffix.length(),
			suffix.length(), suffix)))) {
	thisExe += ".exe";
  }

  // Set process to execute
  command = (char *) "java.exe";
  arg0 = command;

  // Set java program location from JAVA_HOME, if defined
  // if getenv(JAVA_HOME), set mainArgs[0] to it/bin/java, just do execv.
  javaHome = getenv("JAVA_HOME");
  if ((javaHome != NULL) && ((len = strlen(javaHome)) > 0)) {
	javaPath = javaHome;
	if ((javaHome[len-1] == '/') || (javaHome[len-1] == '\\')) {
		javaPath += "bin/java.exe";
	} else {
		javaPath += "/bin/java.exe";
	}
	command = (char *) javaPath.c_str();
	arg0 = "\"" + javaPath + "\"";
	isJavaHome = true;
  }

  mainArgs[0] = (char *) arg0.c_str(); // Name of process
  mainArgs[1] = (char *) "-jar"; // First option. JarRunner if CLASSPATH defined
  mainArgs[2] = (char *) thisExe.c_str(); // Name of file with jar is this

  // Decide if use "-cp classpath JarRunner" instead of "-jar"
  // if getenv(CLASSPATH), use JarRunner, in case have runnable there.
  classpath = getenv("CLASSPATH");
  if ((classpath != NULL) && ((len = strlen(classpath)) > 0)) {
	jarClasspath = classpath;
	jarClasspath += ";";
	jarClasspath += thisExe;
	optionsOffset = maxOptionsOffset;
	mainArgs[1] = (char *) "-cp";
	mainArgs[2] = (char *) jarClasspath.c_str();
	mainArgs[3] = (char *) "edu.uidaho.junicon.runtime.util.JarRunner";
	mainArgs[4] = (char *) thisExe.c_str();
	mainArgs[5] = (char *) "manifest";	// JarRunner uses manifest
  }

  // Copy in other options argv[1..end].  Must quote strings, if have spaces.
  std::string *quoted;
  quoted = new std::string[argc];
  for (int i=1; i<argc; i++) {		// argv[1] --> mainArgs[optionsOffset]
	quoted[i] = argv[i];
	if (quoted[i].find(' ')) {
		quoted[i] = "\"" + quoted[i] + "\"";
	}
	mainArgs[(i-1) + optionsOffset] = (char *) quoted[i].c_str();
  }

  // End mainArgs with NULL
  mainArgs[(argc-1) + optionsOffset] = NULL;

  // Copy over environment, add JARRUNNER_ARG0, JARRUNNER_ISWINDOWS
  int envc = 0;
  if (envp != NULL) {
	while (envp[envc] != NULL) { envc++; }
  }
  mainEnv = new char*[envc + 3];
  for (int i=0; i < envc; i++) {
	mainEnv[i] = envp[i];
  }
  std::string env_arg0("JARRUNNER_ARG0");
  env_arg0 += "=";
  env_arg0 += thisExe;
  mainEnv[envc] = (char *) env_arg0.c_str();
  mainEnv[envc + 1] = (char *) "JARRUNNER_ISWINDOWS=true";
  mainEnv[envc + 2] = NULL;

  // Debug
  /****
  std::cerr << "Arguments" << "\n";
  for (int i=0; (mainArgs[i] != NULL); i++) {
    std::cerr << mainArgs[i] << "\n";
  }
  std::cerr << "End of arguments" << "\n";
  std::cerr << "Using PATH " << (! isJavaHome) << "\n";

  std::cerr << "Environment" << "\n";
  for (int i=0; (mainEnv[i] != NULL); i++) {
    std::cerr << mainEnv[i] << "\n";
  }
  std::cerr << "End of environment" << "\n";
  ****/

  // Invoke java on this jar file
  if (isJavaHome) {
#ifdef _P_WAIT
	_spawnve(_P_WAIT, command, (char **) mainArgs, (char **) mainEnv);
#else
	spawnve(P_WAIT, command, (char **) mainArgs, (char **) mainEnv);
#endif
  } else {
#ifdef _P_WAIT
	_spawnvpe(_P_WAIT, command, (char **) mainArgs, (char **) mainEnv);
#else
	spawnvpe(P_WAIT, command, (char **) mainArgs, (char **) mainEnv);
#endif
  }
  return 0;
}

//====
// // P_WAIT, P_NOWAIT, P_OVERLAY, P_DETACH (Windows has _ prefix)
// int pid = _spawnvpe(_P_NOWAIT, command, args, env)
// _cwait(&termstat_int, pid, _WAIT_CHILD)
//====

//==== END OF FILE
