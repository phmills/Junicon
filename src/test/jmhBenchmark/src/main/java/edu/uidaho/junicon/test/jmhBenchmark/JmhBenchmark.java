//========================================================================
// Copyright (c) 2015 Orielle, LLC.  
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
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
package edu.uidaho.junicon.test.jmhBenchmark;

import edu.uidaho.junicon.test.junicon.*;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks a given class.
 * Performs warmup iterations, then test iterations for
 * a given class with a main method parameterized by sample size.
 * Outputs the microseconds of cpu time, including user and system time,
 * for each iteration.
 *
 * @author Peter Mills
 */
@State(Scope.Benchmark)
public class JmhBenchmark {

  // Command line arguments
  private static String scriptName = null;
  private static String[] scriptArgs = new String[0];

  private static int warmups = 1; 
  private static int iterations = 1; 
  private static boolean optionIsReflective = false;

  // For error messages
  private static PrintStream err = System.err;

  //====================================================================
  // Constructors
  //====================================================================

  public JmhBenchmark () {
  }

  //====================================================================
  // Jmh parameters
  //====================================================================
  @Param({})
  public String className;

  @Param({})
  public String sampleSize;

  @Param({})
  public boolean isReflective;

  //====================================================================
  // Main.
  //====================================================================

  /**
   * Main program.  Runs benchmark for className.main(sampleSize).
   * <BR>
   * Usage: benchmark -r -w warmups -n iterations -s sampleSize className args
   * <BR>
   * Outputs: results by iteration. Optionally amean, stddev, confidence.
   * @param args	command line arguments, will override Spring settings.
   */
  public static void main(String[] args) throws RunnerException {
	boolean invalidArgs = processCommandLineArgs(args);
	if (invalidArgs) { System.exit(0); }

	ChainedOptionsBuilder chained = new OptionsBuilder()
		.include(JmhBenchmark.class.getSimpleName())
		.warmupIterations(warmups)
		.measurementIterations(iterations)
		.forks(1);
	chained = chained.param("className", scriptName.toString());
	chained = chained.param("sampleSize", 
		(scriptArgs.length < 1) ? "1" : scriptArgs[0]);
	chained = chained.param("isReflective",
		Boolean.toString(optionIsReflective));
	Options opt = chained.build();
	new Runner(opt).run();
  }

  //====================================================================
  // Process command line arguments
  //====================================================================

  /**
   * Process command line arguments.
   * @return if invalid args
   */
  public static boolean processCommandLineArgs (String[] args) {
    boolean hasStdin = false;
    boolean printHelp = false;
    String usage = "";
    boolean invalidArgs = false;

	if (args == null) { args = new String[0]; }

	usage = "Usage: jmhBenchmark \t[-h (help)]"
	+ "\n\t\t"
	+ "[-r (reflectively invoke class)]"
	+ "\n\t\t"
	+ "[-w warmups] [-i iterations] className sampleSize [args...]";

	usage += "\t"
	+ "Performs benchmark by first running warmup iterations, then"
	+ "\n\t\t"
	+ "runs benchmark iterations for className.main(sampleSize).";

	// Process "-" prefixed command line arguments
	int i = 0;
	String arg = "";
	while ((! invalidArgs) && (i < args.length) && (args[i] != null) &&
			args[i].startsWith("-")) {
	    arg = args[i++];
	    switch (arg) {
		    case "-h": printHelp = true; break;
		    case "-r": optionIsReflective = true; break;
		    case "-w": if ((i < args.length) && (args[i] != null)) {
				  Number num = stringToInteger(args[i++]);
				  if (num == null) { invalidArgs = true;
				  } else { warmups = num.intValue(); }
				} else { invalidArgs = true; };
				break;
		    case "-i": if ((i < args.length) && (args[i] != null)) {
				  Number num = stringToInteger(args[i++]);
				  if (num == null) { invalidArgs = true;
				  } else { iterations = num.intValue(); }
				} else { invalidArgs = true; };
				break;
		    case "-": hasStdin = true;
				break;
		    default: invalidArgs = true;
				break;
	    }
	}

	// Process remaining arguments, first nonOption is scriptName
	if ((! invalidArgs) && (i < args.length) && (args[i] != null)) {
		// Use first nonOption arg as script name
		scriptName = args[i++];

		// Pass remaining arguments to script
		if (i < args.length) {
			scriptArgs = Arrays.copyOfRange(args, i, args.length);
		}
	}

	// Invalid Options ?
	if (invalidArgs) {
		err.println("Invalid option " + (arg==null?"":arg));
	} else {
	  if ((scriptName == null) || scriptName.toString().isEmpty()) {
		err.println("Invalid option: missing className");
		invalidArgs = true;
	  }
	}
	if (invalidArgs || printHelp) {
		err.println(usage);
		invalidArgs = true;
	}
	return invalidArgs;
  }

  //=========================================================================
  // Setup benchmark.
  //=========================================================================
  Method method;
  String[] mainArgs;

  @Setup(Level.Trial)
  public void prepare() {
      try {
	// Load class
	if (isReflective) {
		Class<?> clazz = Class.forName(className);
		method = clazz.getMethod("main", String[].class);
	}
	// mainArgs = scriptArgs.toArray(new String[0]);
	mainArgs = new String[] { sampleSize };
      } catch (Exception e) {
	e.printStackTrace();
      }
    }

  @TearDown(Level.Trial)
  public void shutdown() {
  }

  //=========================================================================
  // Run benchmark.
  //=========================================================================
  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)	// java.util.concurrent.TimeUnit
  public void runTest () {
      try {
	if (isReflective) {
	    method.invoke(null, (Object) mainArgs);
	} else {
	    switch (className) {

	    	case "Matrix": Matrix.main(mainArgs);
			break;
	    	case "NewInstances": NewInstances.main(mainArgs);
			break;
	    	case "Pidigits": Pidigits.main(mainArgs);
			break;
	    	case "PidigitsFast": PidigitsFast.main(mainArgs);
			break;
	    	case "QuickSort": QuickSort.main(mainArgs);
			break;
	    	case "QuickSortFast": QuickSortFast.main(mainArgs);
			break;

	    	case "Fannkuch": Fannkuch.main(mainArgs);
			break;
	    	case "Mandelbrot": Mandelbrot.main(mainArgs);
			break;
	    	case "MeteorContest": MeteorContest.main(mainArgs);
			break;
	    	case "Nbody": Nbody.main(mainArgs);
			break;
	    	case "SpectralNorm": SpectralNorm.main(mainArgs);
			break;

		default: break;
	    }
	}
      } catch (Exception e) {
	e.printStackTrace();
      }
  }

  //=========================================================================
  // Command line utility methods.
  //=========================================================================
  private static Number stringToInteger (String str) {
	if ((str == null) || str.isEmpty()) { return null; }
	Number num = null;
	try {
		num = Integer.valueOf(str);
	} catch (NumberFormatException e) {
		return null;
	}
	return num;
  }

}

//==== END OF FILE
