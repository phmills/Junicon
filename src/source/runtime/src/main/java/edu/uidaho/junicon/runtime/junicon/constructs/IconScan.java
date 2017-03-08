//========================================================================
// Copyright (c) 2014 Orielle, LLC.  
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
package edu.uidaho.junicon.runtime.junicon.constructs;

import edu.uidaho.junicon.runtime.junicon.iterators.*;
import static edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.FAIL;

import java.util.function.UnaryOperator;
import java.util.function.BinaryOperator;

/**
 * Create string scanning environment for scan operator text?expr.
 * Associated within the scope of each operation is a scan environment.
 * This scan environment is a thread local that must be
 * saved and restored on exit and re-entry to the scan operator,
 * either on iteration or on suspend and resume.
 *
 * @author Peter Mills
 */
public class IconScan <T> extends IconComposition <T> {
  // Scan environment
  long pos = 0;	
  String subject = "";
  private IconScan<T> savedEnv = null;
  private IIconIterator<T> savedAdvice = null;
  int origin = IconNumber.getIndexOrigin();

  /**
   * The scan operator s?e, when mapped over a product,
   * will return the second argument.
   */
  private static BinaryOperator scanOperator = IconOperator.overAtoms(
    (Object x, Object y) -> {
	return y;
    });
  //====
  // * The scan operator s?e, when mapped over a product,
  // * will set subject=s and pos=e result.
  //====

  /**
   * The scan setup operator, when mapped over a iterator of subjects,
   * will set subject and pos=1.
   */
  private static UnaryOperator scanSetupSubject = IconOperator.overAtoms(
    (Object x) -> {
	if ((x == null) || (! (x instanceof CharSequence))) { return FAIL; }
	IconScan env = getScanEnv();
	env.setSubject(((CharSequence) x).toString());
	env.setPos(1);
	return x;
    });

  //==========================================================================
  // Thread local for current active scan environment.
  // Current scan environment is always non-null.
  //==========================================================================
  private static final ThreadLocal<IconScan> currentScanEnv = 
	new ThreadLocal () {
	    @Override protected IconScan initialValue() {
		 return new IconScan();
            }
  };

  public static IconScan getScanEnv () {
	return currentScanEnv.get();
  }

  public static void setScanEnv (IconScan scan) {
	if (scan == null) { return; }
	currentScanEnv.set(scan);
  }

  //==========================================================================
  // Constructors.
  //==========================================================================

  /**
   * No-arg constructor.
   */
  public IconScan () {
	product();
	map(scanOperator);
	setUseOnNextAdvice(true);
  }

  /**
   * Scan operator s?e.
   */
  public IconScan (IconIterator<T> x, IconIterator<T> y) {
	IconComposition<T> xsetup = new IconComposition(x);
	xsetup.map(scanSetupSubject);
	setX(xsetup);
	setY(y);
	product();
	map(scanOperator);
	setUseOnNextAdvice(true);
  }

  //==========================================================================
  // Setters for scan environment.
  //==========================================================================
  public long getPos () {
	return pos;
  }
  public void setPos (long pos) {
	this.pos = pos;
  }
  public String getSubject () {
	return subject;
  }
  public void setSubject (String subject) {
	if (subject == null) { subject = ""; }
	this.subject = subject;
  }
  public int getOrigin () {
	return origin;
  }
  public IconScan setOrigin (int origin) {
	this.origin = origin;
	return this;
  }

  /**
   * Sets current scan position if within subject.
   * If not in range of the subject, the set fails.
   * @return true if set succeeded, and false if set failed.
   */
  public boolean setPosWithin (long val) {
	int within = IconIndex.convertStringPosition(subject, (int) val,
		origin);
	if (within < 0) { return false; }
	this.pos = within + origin;
	return true;
  }

  //==========================================================================
  // Reset state on restart.
  //==========================================================================

  public void afterRestart () {
	setPos(0);
	setSubject("");
  }

  //=========================================================================
  // OnSuspend advice.
  // Save and restore the scan environment on exit and re-entry to scan scope.
  //=========================================================================

  public void afterNextBegin () {
	savedEnv = getScanEnv();		// Push this environment
	savedAdvice = getOnSuspendAdvice();
	setScanEnv(this);
	setOnSuspendAdvice(this);
  }

  public void afterNextEnd () {
	setScanEnv(savedEnv);		// Restore previous environment
	setOnSuspendAdvice(savedAdvice);
  }

  public void afterMethodReturn () {
	setScanEnv(savedEnv);		// Restore previous environment
	setOnSuspendAdvice(savedAdvice);
  }

  public void afterSuspend () {
	setScanEnv(savedEnv);		// Restore previous environment
  }

  public void beforeResume () {
	savedEnv = getScanEnv();		// Push this environment
	setScanEnv(this);
  }

  //==========================================================================
  // Create undo environment for function invocation.
  // Used in: Spring UndoFunction tab => IconScan.undo
  //	new IconInvokeIterator({->tab(arg)}).undoable(IconScan.undo)
  //==========================================================================

  public static IIconUndo createUndo () {
    return new IIconUndo () {
	long pos = 0;
	public void save (IIconAtom var) {
		pos = getScanEnv().getPos();
	}
	public void restore () {
		getScanEnv().setPos(pos);
 	}
    };
  }

}

//===========================================================================
// Configuration for string scanning.
// Spring: IteratorOverIteratorsBinary "?" => new IconScan().over
//	(unary already defined)
// Spring: UndoFunction tab => IconScan.createUndo()
//	IconInvokeIterator({->tab(arg)}).undoable(IconScan.createUndo())
// Functions: Tab function
//	tab() => IconScan.getScanEnv().getPos()/setPos(x)
//===========================================================================

//==== END OF FILE
