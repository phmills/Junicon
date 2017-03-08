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
package edu.uidaho.junicon.test.benchmark;

import edu.uidaho.junicon.test.junicon.*;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.lang.management.*;

/**
 * Benchmarks a given class.
 * Performs warmup iterations, then test iterations for
 * a given class with a main method parameterized by sample size.
 * Outputs the milliseconds of cpu time, including user and system time,
 * for each iteration.
 *
 * @author Peter Mills
 */
public class Benchmark {

  // Command line arguments
  private String scriptName = null;
  private String[] mainArgs = new String[0];

  private int warmups = 1; 
  private int iterations = 1; 
  private boolean isReflective = false;

  // For error messages
  private static PrintStream err = System.err;

  //====================================================================
  // Constructors
  //====================================================================

  public Benchmark () {
  }

  //====================================================================
  // Main.
  //====================================================================

  /**
   * Main program.  Runs benchmark for classname.main(sampleSize).
   * <BR>
   * Usage: benchmark -r -w warmups -n iterations classname sampleSize [args]
   * <BR>
   * Outputs: results by iteration. Optionally amean, stddev, confidence.
   * @param args	command line arguments, will override Spring settings.
   */
  public static void main (String[] args)
  {
      try {
	//====
	// Create shell and process command line arguments.
	//====
	Benchmark shell = new Benchmark();  // shell = createSpringBean();
					    // if (shell == null) { return; }
	boolean invalidArgs = shell.processCommandLineArgs(args);
	if (! invalidArgs) {
		shell.apply(null);
	}
	System.exit(0);
    } catch (IllegalArgumentException e) {
    } catch (Throwable e) {
	e.printStackTrace();
    }
  }

  //====================================================================
  // Process command line arguments
  //====================================================================

  /**
   * Process command line arguments.
   * @return if invalid args.
   */
  public boolean processCommandLineArgs (String[] args) {
    boolean hasStdin = false;
    boolean printHelp = false;
    String usage = "";
    boolean invalidArgs = false;

	if (args == null) { args = new String[0]; }

	usage = "Usage: benchmark \t[-h (help)]"
	+ "\n\t\t"
	+ "[-r (reflectively invoke class)]"
	+ "\n\t\t"
	+ "[-w warmups] [-i iterations] classname sampleSize [args...]";

	usage += "\n\t"
	+ "Performs benchmark by first running warmup iterations, then"
	+ "\n\t\t"
	+ "runs benchmark iterations for classname.main(sampleSize).";

	// Process "-" prefixed command line arguments
	int i = 0;
	String arg = "";
	while ((! invalidArgs) && (i < args.length) && (args[i] != null) &&
			args[i].startsWith("-")) {
	    arg = args[i++];
	    switch (arg) {
		    case "-h": printHelp = true; break;
		    case "-r": isReflective = true; break;
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

	//====
	// Process remaining arguments, first nonOption is scriptName
	//====
	if ((! invalidArgs) && (i < args.length) && (args[i] != null)) {
		// Use first nonOption arg as script name
		scriptName = args[i++];

		// Pass remaining arguments to script
		if (i < args.length) {
			mainArgs = Arrays.copyOfRange(args, i, args.length);
		}
	}

	// Invalid Options ?
	if (invalidArgs) {
		err.println("Invalid option " + (arg==null?"":arg));
	} else {
	  if ((scriptName == null) || scriptName.isEmpty()) {
		err.println("Invalid option: missing classname");
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

  private void setupTest () {
      try {
	// Load class
	if (isReflective) {
		Class<?> clazz = Class.forName(scriptName);
		method = clazz.getMethod("main", String[].class);
	}
      } catch (Exception e) {
	e.printStackTrace();
      }
  }

  private void runTest () {
      try {
	if (isReflective) {
	    method.invoke(null, (Object) mainArgs);
	} else {
	    switch (scriptName) {
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
  // Run benchmark.
  //=========================================================================
  /**
   * Run main program.
   */
  private void apply (Object... args) {
      List<Long> results = new ArrayList();
      try {
	// Setup test
	setupTest();

	// Run warmup iterations
	System.out.println("**** Warmup iterations");
	for (int i=0; i<warmups; i++) {
		System.out.println("Iteration " + (i+1));
		runTest();
	}

	// Run test iterations
	System.out.println("**** Test iterations");
	for (int i=0; i<iterations; i++) {
		long start = getCpuTime();
		System.out.println("Iteration " + (i+1));
		runTest();
		long duration = getCpuTime() - start;
		System.out.println("Iteration " + (i+1)
			+ " time (ns): " + duration);
		results.add(duration);
	}

	// Statistical analysis
	double mean = average(results);
	double stddev = standardDeviation(results, mean);
	double confidence = confidenceInterval(stddev, results.size(), false);
	System.out.println("Mean " + mean + " +- " + confidence
		+ " (Confidence @99%)");

      } catch (Exception e) {
	e.printStackTrace();
      } finally {
	err.close();	// in case output file
      }
  }

  //=========================================================================
  // Command line utility methods.
  //=========================================================================
  /**
   * Convert string to integer.
   */
  public static Number stringToInteger (String str) {
	if ((str == null) || str.isEmpty()) { return null; }
	Number num = null;
	try {
		num = Integer.valueOf(str);
	} catch (NumberFormatException e) {
		return null;
	}
	return num;
  }

  //=========================================================================
  // Benchmark utility methods.
  //=========================================================================
  /**
   * Get CPU time in nanoseconds, including both system and user time.
   */
  public static long getCpuTime( ) {
	ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
	return bean.isCurrentThreadCpuTimeSupported( ) ?
		bean.getCurrentThreadCpuTime( ) : 0L;
  }
 
  /**
   * Get user time in nanoseconds.
   */
  public static long getUserTime( ) {
	ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
	return bean.isCurrentThreadCpuTimeSupported( ) ?
		bean.getCurrentThreadUserTime( ) : 0L;
  }

  //=========================================================================
  // Statistical analysis: Mean, standard deviation, confidence interval
  //=========================================================================
  public static double average (List<Long> list) {
	if (list == null) { return 0; }
	double total = 0;
	for (long i : list) { total += i; }
	return total/list.size();
  }

  public static double standardDeviation (List<Long> list, double mean) {
	if (list == null) { return 0; }
	double total = 0;
	for (long i : list) { total += ((i - mean)*(i - mean)); }
	return Math.sqrt(total/list.size());
  }
  // list-1 for sample stddev, Bessels correction

  public static double confidenceInterval (double stddev, long numruns,
		boolean is95not99) {
	double coeff95 = 1.96;
	double coeff99 = 2.575;		// 2.58
	double coeff = coeff99;
	if (is95not99) { coeff = coeff95; }
	return (coeff * stddev)/Math.sqrt((double) numruns);
  }

  public static double geometricMean (List<Long> list) {
	if (list == null) { return 0; }
	double total = 1;
	for (long i : list) { total *= i; }
	return Math.pow(total, (1.0/list.size()));
  }

}

//==== END OF FILE
