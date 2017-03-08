 
Junicon Transformational Interpreter
====================================
Junicon is a Java-based interpreter for the Unicon programming language,
implemented using program transformation.

Documentation
-------------
Documentation can be found in the [Wiki pages](../../wiki/Home).  
The list of changes can be found in the Release Notes.

Dependencies
------------
- Java SE 8 or above.
    - JRE (Java Runtime Environment) to run Junicon.
    - JDK (Java Development Kit) if compiling Junicon programs
      or building Junicon from source.  
- Maven 3.0 or above, if building from source.
- Operating system:
    - On Linux/MacOS, csh is required to run the executables.
    - On Windows, if using csh and not exe, Cygwin or the equivalent is required.

Installation
------------
1. From the latest release, download junicon for Linux/MacOS, or junicon.exe for Windows.  
        All dependencies are already in the distribution, including Groovy.
2. Download and install the current version of the Java runtime.
3. Make sure java or java.exe is in your path,
        or set JAVA_HOME to the location of your Java installation.
4. Optionally, add the junicon executable to your PATH:  
        Linux/MacOS/Cygwin: export PATH=yourDirectory/junicon:$PATH  
        Windows: set PATH="yourDirectory/junicon.exe";%PATH%

Usage
-----
Run `junicon -h` to see the program options.

Building Junicon
----------------
1. Download a snapshot of the source, and unzip it.
2. Download and install the current version of the Java JDK.
3. Download and install the current version of Maven.
4. Set M2_HOME to the location of Maven.  
        Also set JAVA_HOME to the location of your Java JDK installation.       
5. Run `bin/build` or `bin/build.bat` to build Junicon.
6. Type `clean install site` at the prompt, or just hit enter.
7. Move junicon or junicon.exe from `distribution/target/` to your desired
        installation directory.

Credits
-------
Author: Peter Mills  
Contributors: Thanks to Rob Kleffner for implementing many built-in Icon functions.

License
-------
The software in this repository is licensed under the terms of the
BSD 2-Clause Simplified License.
