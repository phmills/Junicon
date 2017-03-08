//========================================================================
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
//========================================================================
#include <stdlib.h>	/* system, NULL, EXIT_FAILURE */
#include <stdio.h>	/* printf */
#include <string.h>
#include <process.h>
#include <iostream>	/* cout, cin, cerr */
#include <fstream>
#include <iterator>
#include <algorithm>
// using namespace std; 

/**
 * Execute attached Windows cmd script.
 *
 * This program runs cmd.exe on the appended bat command file.  The bat file
 * and any jar file should be attached as a payload to this executable.
 * The executable is thus formed by sandwitching this script between
 * batrunner.exe and another zip or jar file: [batrunner][bat][jar].
 * The concatenated file can double both as a jar/zip and as an executable,
 * because zip files read from the end while executables read from the front.
 *
 * Batrunner runs this script by first scanning it into a string
 * from the sandwitched executable, and then running it in memory via
 * cmd /q /s /v:on /c "command".   No temporary files are used.
 * Cmd.exe must be in the current path.
 * The bat file must consist of multiple commands joined with &.
 * Batrunner prefixes the script with the original command-line arguments.
 * The following prefix will be inserted by BatRunner:
 *	(setlocal enabledelayedexpansion) & (
 *	set args=...command line arguments...) & (
 * The user command file should be of the form:
 *	#!/batrunner begin
 *	myCommand) & (
 *	moreCommands)
 *	#!/batrunner end
 *
 * BatRunner sets the environment variable JARRUNNER_ARG0 to
 * this executable filename, and also sets JARRUNNER_ISWINDOWS=true.
 * The executable filename will have .exe appended if not there,
 * so that the original file name is available within a Java application.
 * This workaround is because arg0 is dropped from Java's main(args).
 *
 * Compile under Visual Studio C++ with:
 *	vcvars32.bat
 *	cl.exe /EHsc BatRunner.cpp
 * Compile under mingw with:
 *	g++ BatRunner.cpp -static -o batrunner.exe
 */
class FileSearch {	// Search binary file for string, accumulating text
	// std::search does not work on single pass istreambuf_iterator in VC++
    std::istreambuf_iterator<char> eof; 
    std::istreambuf_iterator<char> *iter;
    std::ifstream *thisFile;
    char *buf;
    int bufsize;	// Maximum search string length
    int halfsize;
    int buflen;		// Buffer length
    int filepos;	// Begin scan position in buffer
  public:
    std::string text;
    bool isEof;
    FileSearch (std::string filename, int maxString) {
	thisFile = new std::ifstream(filename.c_str(), std::ios_base::binary); 
	iter = new std::istreambuf_iterator<char>(*thisFile);
	isEof = (eof == *iter);
	halfsize = maxString;
	bufsize = maxString*2;
	buf = new char[bufsize];
	buflen = 0;
	filepos = 0;
	text = "";
    }
    bool search (std::string match, bool accumulate) {
	int pos;		// String scan position
	int bufpos;		// Buffer scan position
	bool isMatch = false;
	int len = match.length();
	while ((! isMatch) && (! isEof)) {
	  pos = 0;
	  bufpos = filepos;
	  isMatch = true;
	  while (pos < len) {
	    if (bufpos >= buflen) {	// Add char to buffer from file iterator
		if (eof == *iter) {
			isEof = true;
			isMatch = false;
			break;
		}
		if (buflen >= bufsize) {	// Shift left
			for (int i=0; i < halfsize; i++) {
				buf[i] = buf[i+halfsize];
			}
			bufpos -= halfsize;
			buflen -= halfsize;
			filepos -= halfsize;
		}
		buf[bufpos] = *(*iter)++;
		buflen++;
	    }
	    if (buf[bufpos] != match[pos]) {	// Mismatch, char not candidate 
		if (accumulate) {
			text += buf[bufpos];
		}
		filepos++;
		isMatch = false;
		break;
	    }
	    pos++;
	    bufpos++;
	  }
	}
	if (isMatch) { filepos = bufpos; }	// Skip over match
	if (isEof) {
	    thisFile->close();
	    std::cerr << "Header \"" << match.c_str()
		<< "\" not found in executable" << std::endl; 
	}
	return isMatch;
    }
};

int main (int argc, char* argv[], char* envp[]) {
  char *thisFileName;		// Arg0 to this process
  std::string beginMarker("#!/batrunner begin");	// Beginning of bat file
  std::string endMarker("#!/batrunner end");		// End of bat file
  char *command;		// Invoked command
  char **mainArgs;		// Invoked arguments
  char **mainEnv;
  std::string arg0;		// Argument 0 to pass to invoked command
  std::string thisExe;		// Full name of this invoked file, with .exe

  //==========================================================================
  // Scan bat command from this file.
  //==========================================================================

  //====
  // Get invoked file name.  Append .exe if not there, for correct jar file name
  //====
  thisFileName = argv[0];
  if (thisFileName == NULL) { thisFileName = (char *) ""; }
  thisExe = thisFileName;
  std::string suffix = ".exe";
  if (! ((thisExe.length() >= suffix.length()) &&
        	(0 == thisExe.compare (thisExe.length() - suffix.length(),
			suffix.length(), suffix)))) {
	thisExe += ".exe";
  }

  //====
  // Find beginMarker in this file: first time is to skip over constant pool
  //====
  FileSearch fileSearcher(thisExe.c_str(), beginMarker.length());
  if (! fileSearcher.search(beginMarker, false)) {
	return 1;
  }
  if (! fileSearcher.search(beginMarker, false)) {
	return 1;
  }

  //====
  // Find endMarker in this file, copying text in between to buffer
  //====
  if (! fileSearcher.search(endMarker, true)) {
	return 1;
  }

  //==========================================================================
  // Create bat command to execute
  //==========================================================================

  // Copy in other options argv[1..end].  Must quote strings, if have spaces.
  std::string preArgs("(setlocal enabledelayedexpansion) & (set args=");
  std::string postArgs(") & (");

  std::string batArgs("");
  std::string *quoted;
  quoted = new std::string[argc];
  for (int i=1; i<argc; i++) {		// argv[1] --> mainArgs[optionsOffset]
	quoted[i] = argv[i];
	if (quoted[i].find(' ')) {
		quoted[i] = "\"" + quoted[i] + "\"";
	}
	if (i > 1) { batArgs += " "; }
	batArgs += quoted[i];
  }

  // Form batCommand, change newlines to spaces 
  std::string batCommand = preArgs + batArgs + postArgs + fileSearcher.text;

  for (int i = 0; i < batCommand.length(); i++) {
	if ((batCommand[i] == '\n') || (batCommand[i] == '\r')) {
		batCommand[i] = ' ';
	}
  }

  // Set process to execute:  cmd.exe /s /q /v:on /c "command"
  mainArgs = new char*[7];
  command = (char *) "cmd.exe";		// "C:\\Windows\\System32\\cmd.exe";
  arg0 = thisExe;
  mainArgs[0] = (char *) arg0.c_str(); // Name of process
  mainArgs[1] = (char *) "/s";
  mainArgs[2] = (char *) "/q";
  mainArgs[3] = (char *) "/v:on";
  mainArgs[4] = (char *) "/c";
  mainArgs[5] = (char *) batCommand.c_str();
  mainArgs[6] = NULL;

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

  //==========================================================================
  // Invoke bat command.
  //==========================================================================
#ifdef _P_WAIT
	_spawnvpe(_P_WAIT, command, (char **) mainArgs, (char **) mainEnv);
#else
	spawnvpe(P_WAIT, command, (char **) mainArgs, (char **) mainEnv);
#endif

  return 0;
}

//==== END OF FILE
