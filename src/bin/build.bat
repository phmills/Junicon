@echo off

REM #=========================================================================
REM # Copyright (c) 2011 Orielle, LLC.
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
REM # Display title
REM #====
title Junicon Maven Build

REM #====
REM # Localize variables
REM #	Paths use Windows notation, with / for path separators.
REM #====
setlocal
set SAVED_CD=%CD%

REM #====
REM # Program locations
REM #====
set maven=mvn.bat
if defined M2_HOME (
	set maven=%M2_HOME%/bin/mvn.bat
	set path=%path%;%M2_HOME%/bin
)

REM #====
REM # Project home
REM #====
if not defined JUNICON_HOME set JUNICON_HOME=%~dp0%..
set JUNICON_HOME=%JUNICON_HOME:\=/%
cd %JUNICON_HOME:/=\%

REM #====
REM # Maven build (Phases: compile test package install deploy site clean)
REM #====
set buildcmd=
if "%*" == "" set /P buildcmd="Enter build command (default is clean install site): "
if "%*" == "" if "%buildcmd%" == "" set buildcmd=clean install site
%maven% %buildcmd% %*

cd %SAVED_CD%
endlocal
REM # pause

REM #==== END OF FILE
