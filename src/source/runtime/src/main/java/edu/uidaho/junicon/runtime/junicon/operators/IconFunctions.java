//========================================================================
// Copyright (c) 2012 Orielle, LLC.  
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
package edu.uidaho.junicon.runtime.junicon.operators;

import edu.uidaho.junicon.runtime.junicon.iterators.*;
import edu.uidaho.junicon.runtime.junicon.constructs.IconScan;
import edu.uidaho.junicon.runtime.junicon.constructs.IconCoExpression;
import edu.uidaho.junicon.runtime.junicon.constructs.IconPromote;
import static edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.FAIL;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.Optional;

import java.util.Collections;
import java.util.Comparator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.io.Reader;
import java.io.Writer;
import java.io.RandomAccessFile;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Defines the built-in Icon functions.
 * The built-in functions are exposed as method references
 * so they can be assignable.
 * <P>
 * Normally, method references within a generated Junicon class are not intended
 * to be assignable and will be ambiguous under Groovy if done so, since in
 * Groovy a method takes precedence over a field closure with the same name
 * in invocation, while in Java they are in separate namespaces.
 * Assignment works here because we are exposing the references
 * outside of their class.
 * <P>
 * Provides support to memorize the association of
 * a function name to its initial method reference,
 * for use by proc("name",0).
 * <P>
 * Memorization is one way to provide metadata
 * for operators, functions, and keywords.
 * Annotations are another way to provide metadata,
 * using @MFunction, @Moperator, and @MKeyword.
 * <P>
 * USAGE: When compiling to Java: import static IconFunctions.*;
 *
 * @author Peter Mills
 * @author Rob Kleffner
 */
public class IconFunctions {
  private static Map<String, VariadicFunction> symbolToFunction = new HashMap();

  //==========================================================================
  // Expose functions as method references.
  //==========================================================================

  //====
  // Input-output functions.
  //====
  public static VariadicFunction writeln = memorize("writeln", IconFunctions::writeln);

  //====
  // Collection functions.
  //====
  public static VariadicFunction delete = IconFunctions::delete;
  public static VariadicFunction get = IconFunctions::get;
  public static VariadicFunction insert = IconFunctions::insert;
  public static VariadicFunction key = IconFunctions::key;
  public static VariadicFunction list = memorize("list", IconFunctions::list);
  public static VariadicFunction member = IconFunctions::member;
  public static VariadicFunction min = IconFunctions::min;
  public static VariadicFunction max = IconFunctions::max;
  public static VariadicFunction pull = IconFunctions::pull;
  public static VariadicFunction pop = memorize("pop", IconFunctions::pop);
  public static VariadicFunction push = memorize("push", IconFunctions::push);
  public static VariadicFunction put = memorize("put", IconFunctions::put);
  public static VariadicFunction set = memorize("set", IconFunctions::set);
  public static VariadicFunction sort = IconFunctions::sort;
  public static VariadicFunction sortf = IconFunctions::sortf;
  public static VariadicFunction sortn = IconFunctions::sortn;
  public static VariadicFunction table = memorize("table", IconFunctions::table);

  //====
  // Concurrency functions.
  //====
  public static VariadicFunction spawnThread = memorize("spawnThread", IconFunctions::spawnThread);

  //====
  // Conversion functions.
  //====
  public static VariadicFunction integer = IconFunctions::integer;
  public static VariadicFunction numeric = IconFunctions::numeric;
  public static VariadicFunction ord = IconFunctions::ord;
  public static VariadicFunction real = IconFunctions::real;
  public static VariadicFunction string = IconFunctions::string;

  //====
  // Arithmetic functions.
  //====
  public static VariadicFunction abs = IconFunctions::abs;
  public static VariadicFunction acos = IconFunctions::acos;
  public static VariadicFunction asin = IconFunctions::asin;
  public static VariadicFunction atan = IconFunctions::atan;
  public static VariadicFunction atanh = IconFunctions::atanh;
  public static VariadicFunction cos = IconFunctions::cos;
  public static VariadicFunction dtor = IconFunctions::dtor;
  public static VariadicFunction exp = IconFunctions::exp;
  public static VariadicFunction iand = IconFunctions::iand;
  public static VariadicFunction icom = IconFunctions::icom;
  public static VariadicFunction ior = IconFunctions::ior;
  public static VariadicFunction ixor = IconFunctions::ixor;
  public static VariadicFunction ishift = memorize("ishift", IconFunctions::ishift);
  public static VariadicFunction log = IconFunctions::log;
  public static VariadicFunction rtod = IconFunctions::rtod;
  public static VariadicFunction sin = IconFunctions::sin;
  public static VariadicFunction sqrt = IconFunctions::sqrt;
  public static VariadicFunction tan = IconFunctions::tan;
  
  //====
  // String utility functions.
  //====
  public static VariadicFunction bal = IconFunctions::bal;
  public static VariadicFunction charUnicon = IconFunctions::charUnicon;
  public static VariadicFunction center = IconFunctions::center;
  public static VariadicFunction left = IconFunctions::left;
  public static VariadicFunction map = IconFunctions::map;
  public static VariadicFunction repl = IconFunctions::repl;
  public static VariadicFunction reverse = IconFunctions::reverse;
  public static VariadicFunction right = IconFunctions::right;
  public static VariadicFunction trim = IconFunctions::trim;

  //====
  // String scanning functions.
  //====
  public static VariadicFunction find = memorize("find", IconFunctions::find);
  public static VariadicFunction match = memorize("match", IconFunctions::match);
  public static VariadicFunction move = memorize("move", IconFunctions::move);
  public static VariadicFunction tab = memorize("tab", IconFunctions::tab);
  public static VariadicFunction upto = memorize("upto", IconFunctions::upto);
  public static VariadicFunction many = memorize("many", IconFunctions::many);
  public static VariadicFunction any = memorize("any", IconFunctions::any);
  public static VariadicFunction pos = memorize("pos", IconFunctions::pos);

  //==========================================================================
  // Memorization.
  //==========================================================================
  /**
   * Memorize initial function associated to a given symbol.
   * Returns initial function.
   */
  public static <T> VariadicFunction<T,T> memorize (String symbol,
		VariadicFunction<T,T> function) {
	if ((symbol == null) || (function == null)) { return null; }
	symbolToFunction.put(symbol, function);
	return function;
  }
		
  /**
   * Get initial function associated to a given symbol.
   */
  public static <T> VariadicFunction<T,T> getMemorized (String symbol) {
	return symbolToFunction.get(symbol);
  }

  //==========================================================================
  // Input-output functions.
  //==========================================================================

  /**
   * Write out arguments, and print newline.
   * If args is null, just prints newline.
   * <P>
   * Like Java, if null is passed it sets the varags Object array to null.
   * To pass a null element, use a cast (Object)null.
   */
  public static Object writeln (Object... args) {
	if (args != null) {
	    for (Object i : args) { System.out.print(i); }
	}
	System.out.println();
	return null;
  }

  //==========================================================================
  // Collection functions.
  //==========================================================================

  /**
   * If X is a set or list, removes each xi from X. If X is a table,
   * deletes element for each key xi in X. Produces X.
   * @return A set or a table
   */
  public static Object delete (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      if (args[0] instanceof Set) {
          for (int i = 1; i < args.length; i++) {
              ((Set) args[0]).remove(args[i]);
          }
          return args[0];
      }
      if (args[0] instanceof Map) {
          for (int i = 1; i < args.length; i++) {
              ((Map) args[0]).remove(args[i]);
          }
          return args[0];
      }
      if (args[0] instanceof List) {
          for (int i = 1; i < args.length; i++) {
              Number r = IconNumber.toNumber(args[i]);
              if (r == null) {
                  throw new RuntimeException("Error code 101: not integer");
              }
              ((List) args[0]).remove(r.intValue());
          }
          return args[0];
      }
      throw new RuntimeException("Error code 122: not set or table");
  }
  
  /**
   * Produces the leftmost element of L and removes it from L. Fails if L
   * is empty. A synonym for pop.
   * @return The leftmost element of L
   */
  public static Object get (Object... args) {
      return pop(args);
  }
  
  /**
   * If X is a table, insert(X,x1,x2) inserts key x1 with value x2. If X is a set,
   * insert(X,x1) inserts x1. Produces X.
   * @return A set or a table
   */
  public static Object insert (Object... args) {
      if ((args == null) || (args.length < 2)) { return FAIL; }
      if (args[0] instanceof List) {
          Number r = IconNumber.toBigNumber(args[1]);
          if (r == null) {
              throw new RuntimeException("Error code 101: not integer");
          }
	  int index = r.intValue() - IconNumber.getIndexOrigin();
          if (args.length == 3) {
              ((List) args[0]).add(index, args[2]);
          } else {
              ((List) args[0]).add(index, null);
          }
	  return args[0];
      }
      if (args[0] instanceof Map) {
          if (args.length == 2) {
              ((Map) args[0]).put(args[1], null);
          } else {
              ((Map) args[0]).put(args[1], args[2]);
          }
	  return args[0];
      }
      if (args[0] instanceof Set) {
          ((Set) args[0]).add(args[1]);
	  return args[0];
      }
      throw new RuntimeException("Error code 122: not set or table");
  }
  
  /**
   * Generates the keys in table T, or the list of indexes from 1 to *L if a list.
   * @return A generated sequence
   */
  public static Object key (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      if (args[0] instanceof Map) {
          return new IconPromote(new ArrayList(((Map) args[0]).keySet())).setListAsValue();
      }
      if (args[0] instanceof List) {
          return new IconPromote(
              IntStream.rangeClosed(1, ((List)args[0]).size()+1)
                       .boxed()
                       .collect(Collectors.toList()));
      }
      throw new RuntimeException("Error code 124: not table");
  }

  /**
   * Create a list of size i in which each value is x.  Returns list.
   * USAGE: list(i, x)
   * If x is omitted it defaults to null.
   * Default for i is 0.
   */
  public static Object list (Object... args) {
	long size = 0;
	Object value = null;
	if (args != null) {
	    if (args.length > 0) {
		Number num = IconNumber.toNumber(args[0]); // null if not number
		if (num == null) { size = 0;
		} else { size = num.longValue(); };
	    }
	    if (args.length > 1) { value = args[1]; }
	}
	ArrayList ls = new ArrayList();
	for (int i=0; i<size; i++) {
		ls.add(value);
	}
	return ls;
  }
  
  /**
   * If X is a set or list, produces X if x is in X. If X is a table,
   * produces X if x is a key of an element in X.
   * @return X
   */
  public static Object member (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      if (args[0] instanceof List) {
          for (int i = 1; i < args.length; i++) {
              if (!((List) args[0]).contains(args[i])) { return FAIL; }
          }
          return args[0];
      }
      if (args[0] instanceof Set) {
          Set l = (Set) args[0];
          for (int i = 1; i < args.length; i++) {
              if (!((Set) args[0]).contains(args[i])) { return FAIL; }
          }
          return args[0];
      }
      if (args[0] instanceof Map) {
          for (int i = 1; i < args.length; i++) {
              if (!((Map) args[0]).containsKey(args[1])) { return FAIL; }
          }
          return args[0];
      }
      throw new RuntimeException("Error code 122: not set or table");
  }
  
  /**
   * Produces the rightmost element of L and removes it from L. Fails if
   * L is empty.
   * @return The rightmost element of L
   */
  public static Object pull (Object... args) {
      if ((args == null) || (args.length == 0)) { return FAIL; }
      int count = 1;
      if (args.length > 1) {
          Number r = IconNumber.toNumber(args[1]);
          if (r == null) {
              throw new RuntimeException("Error code 101: not integer");
          }
          count = r.intValue();
      }
      if (!(args[0] instanceof List)) {
          throw new RuntimeException("Error code 108: not list");
      }
      List x = (List) args[0];
      
      Object last = null;
      for (int i = 0; i < count; i++) {
          last = x.remove(x.size() - 1);
      }
      return last;
  }

  /**
   * Pop from front of list.  Returns the removed element.
   */
  public static Object pop (Object... args) {
	if ((args == null) || (args.length == 0)) { return FAIL; }
	if (!(args[0] instanceof List)) { return args[0]; }
	List x = (List) args[0];
        if (x.isEmpty()) { return FAIL; }
	return x.remove(0);
  }

  /**
   * Push in front of list.  Returns list.
   */
  public static Object push (Object... args) {
	if ((args == null) || (args.length == 0)) { return FAIL; }
	if (!(args[0] instanceof List)) { return args[0]; }
	List x = (List) args[0];
	x.addAll(0, Arrays.asList(args).subList(1,args.length));
	return x;
  }

  /**
   * Append to list.  Returns list.
   */
  public static Object put (Object... args) {
	if ((args == null) || (args.length == 0)) { return FAIL; }
	if (!(args[0] instanceof List)) { return args[0]; }
	List x = (List) args[0];
	x.addAll(Arrays.asList(args).subList(1,args.length));
	return x;
  }

  /**
   * Generates the infinite sequence i, i+j, i+2j, i+3j. j may not be zero.
   */
  public static Object seq (Object... args) {
      long start = 1, increment = 1;
      
      if (args.length > 0) {
          Number r = IconNumber.toBigNumber(args[1]);
          if (r == null) {
              throw new RuntimeException("Error code 101: not integer");
          }
          start = r.longValue();
      }
      
      if (args.length > 1) {
          Number r = IconNumber.toBigNumber(args[1]);
          if (r == null) {
              throw new RuntimeException("Error code 101: not integer");
          }
          increment = r.longValue();
          if (increment == 0) {
              throw new RuntimeException("Error code 211: increment by zero");
          }
      }

      // TODO fix this to return iterator, see find()
      
      long by = increment;
      return new IconPromote(LongStream.iterate(start, x -> x + by).iterator());
  }

  /**
   * set(list)
   */
  public static Object set (Object... args) {
	if ((args != null) && (args.length > 0)) {
		return new LinkedHashSet((Collection) args[0]);
	}
	return new LinkedHashSet();
  }

  /**
   * Sorts a list, set, or the fields of a record.
   * @return A list object
   */
  public static Object sort (Object... args) {
      if ((args == null) || (args.length == 0)) { return FAIL; }
      
      if (args[0] instanceof List) {
          List l = new ArrayList((List) args[0]);
          l.sort(new UniconComparator());
          return l;
      }
      
      if (args[0] instanceof Set) {
          List l = new ArrayList((Set) args[0]);
          l.sort(new UniconComparator());
          return l;
      }
      
      if (args[0] instanceof Map) {
          Map t = (Map) args[0];
          int i = 1;
          if (args.length > 1) {
              Number r = IconNumber.toNumber(args[1]);
              if (r == null) {
                  throw new RuntimeException("Error code 101: not integer");
              }
              i = r.intValue();
              if (i < 1 || i > 4) {
                  throw new RuntimeException("Error code 205: not 1, 2, 3, or 4");
              }
          }
          
          Comparator c = new UniconComparator();
          
          ArrayList<List> l = new ArrayList<>();
          for (Map.Entry<Object, Object> e : (Set<Map.Entry>)t.entrySet()) {
              List tl = new ArrayList();
              tl.add(e.getKey());
              tl.add(e.getValue());
              l.add(tl);
          }
          
          if (i % 2 == 0) {
              l.sort((List o1, List o2) -> c.compare(o1.get(0), o2.get(0)));
          } else {
              l.sort((List o1, List o2) -> c.compare(o1.get(1), o2.get(1)));
          }
          
          if (i == 1 || i == 2) {
              return l;
          }
          
          if (i == 3 || i == 4) {
              List flat = new ArrayList();
              for (List e : l) {
                  flat.add(e.get(0));
                  flat.add(e.get(1));
              }
              return flat;
          }
      }
      
      List l = IconField.objectAsList(args[0]);
      l.sort(new UniconComparator());
      return l;
  }
  
  static class UniconComparator implements Comparator<Object> {
      @Override
      public int compare(Object o1, Object o2) {
          if (o1 == o2)
              return 0;
          if (o1 == null) return -1;
          if (o2 == null) return 1;
          
          if (o1.getClass().isInstance(o2)) {
              if (o1 instanceof Comparable) {
                  return ((Comparable) o1).compareTo(o2);
              } else {
                  if (o1.equals(o2)) return 0;
                  else return -1;
              }
          }
          
          if (o1 instanceof Integer || o1 instanceof BigInteger) return -1;
          else if (o2 instanceof Integer || o2 instanceof BigInteger) return 1;
          
          if (o1 instanceof Number) return -1;
          else if (o2 instanceof Number) return 1;
          
          if (o1 instanceof CharSequence) return -1;
          else if (o2 instanceof CharSequence) return 1;
          
          // TODO: to properly handle csets here, we need to distinguish between
          // regular sets and csets (i.e., need to use a different class entirely)
          
          // TODO: window object tests should go here
          
          if (o1 instanceof Writer) return -1;
          else if (o2 instanceof Writer) return 1;
          if (o1 instanceof Reader) return -1;
          else if (o2 instanceof Reader) return 1;
          if (o1 instanceof RandomAccessFile) return -1;
          else if (o2 instanceof RandomAccessFile) return 1;
          
          if (o1 instanceof IconCoExpression) return -1;
          else if (o2 instanceof IconCoExpression) return 1;
          
          if (o1 instanceof List) return -1;
          else if (o2 instanceof List) return 1;
          
          if (o1 instanceof Set) return -1;
          else if (o2 instanceof Set) return 1;
          
          if (o1 instanceof Map) return -1;
          else if (o2 instanceof Map) return 1;
          
          return -1;
      }
  }
  
  public static Object sortf (Object... args) {
      if ((args == null) || (args.length == 0)) { return FAIL; }
      
      int index = 1;
      if (args.length > 1) {
          Number r = IconNumber.toNumber(args[1]);
          if (r == null) {
              throw new RuntimeException("Error code 101: not integer");
          }
          index = r.intValue();
          if (index == 0) {
              throw new RuntimeException("Error code 205: cannot be 0");
          }
      }
      
      if (args[0] instanceof List) {
          ((List) args[0]).sort(new SortFComparator(index));
          return args[0];
      }
      
      if (args[0] instanceof Set) {
          List l = new ArrayList((Set) args[0]);
          l.sort(new SortFComparator(index));
          return l;
      }
      
      List l = IconField.objectAsList(args[0]);
      l.sort(new SortFComparator(index));
      return l;
  }
  
  static class SortFComparator implements Comparator<Object> {
      private final int index;
      private final UniconComparator basis;
      
      public SortFComparator(int i) {
          index = i;
          basis = new UniconComparator();
      }
      
      @Override
      public int compare(Object o1, Object o2) {
          List o1l, o2l;
          if (o1 instanceof List) { o1l = (List) o1; }
          else if (o1 instanceof Set) { o1l = new ArrayList((Set) o1); }
          else { o1l = IconField.objectAsList(o1); }
          
          if (o2 instanceof List) { o2l = (List) o2; }
          else if (o2 instanceof Set) { o2l = new ArrayList((Set) o2); }
          else { o2l = IconField.objectAsList(o2); }
          
          if (o1l.size() < Math.abs(index)) return -1;
          if (o2l.size() < Math.abs(index)) return 1;
          
          if (index < 0) {
              return basis.compare(o1l.get(o1l.size() + index), o2l.get(o2l.size() + index));
          }
          return basis.compare(o1l.get(index-1), o2l.get(index-1));
      }
  }

  /**
   * table(k,v,...,default)
   * table(k,v,...)
   * table(default)
   * table() - null default.
   * Like map but if key not found return default, i.e. table[undefined]=default
   */
  public static Object table (Object... args) {
	IconMap table = new IconMap(args);
	if ((args != null) && (args.length > 0)) {
		if (1 == (args.length % 2)) {
			table.setDefault(args[args.length - 1]);
		}
	}
	return table;
  }

  /**
   * Numerically sorts a list.
   * Sorts as BigInteger, if IconNumber.getIsIntegerPrecision();
   * otherwise sorts as Long.
   * @return Sorted list
   */
  public static Object sortn (Object... args) {
	if ((args == null) || (args.length == 0)) { return FAIL; }
	if (! (args[0] instanceof List)) {
		return FAIL;
	}
	try {
	  if (IconNumber.getIsIntegerPrecision()) {
		List<BigInteger> c = new ArrayList((List) args[0]);
		c.sort(null);
		return c;
		//====
		// List<BigInteger> c = (List) args[0];
		// return c.stream().sorted((x,y) -> x.compareTo(y))
		// .collect(Collectors.toList()));
		//====
	  } else {
		List<Long> c = new ArrayList((List) args[0]);
		c.sort(null);
		return c;
	  }
	} catch (ClassCastException e) {
		return FAIL;
	}
  }
      
  /**
   * Takes the minimum of a list of integers.
   * Compares as BigInteger, if IconNumber.getIsIntegerPrecision();
   * otherwise compares as Long.
   * @return minimum, or FAIL if list is empty.
   */
  public static Object min (Object... args) {
	if ((args == null) || (args.length == 0)) { return FAIL; }
	if (! (args[0] instanceof List)) {
		return FAIL;
	}
	List c = (List) args[0];
	Optional result = c.stream().min(new UniconComparator());
		// c.stream().min((x,y) -> x.compareTo(y));
	if (result.isPresent()) { return result.get();
	} else { return FAIL; }
  }

  /**
   * Takes the maximum of a list of integers.
   * Compares as BigInteger, if IconNumber.getIsIntegerPrecision();
   * otherwise compares as Long.
   * @return minimum, or FAIL if list is empty.
   */
  public static Object max (Object... args) {
	if ((args == null) || (args.length == 0)) { return FAIL; }
	if (! (args[0] instanceof List)) {
		return FAIL;
	}
	List c = (List) args[0];
	Optional result = c.stream().max(new UniconComparator());
		// c.stream().max((x,y) -> x.compareTo(y));
	if (result.isPresent()) { return result.get();
	} else { return FAIL; }
  }

  //==========================================================================
  // Concurrency functions.
  //==========================================================================

  /**
   * Spawn co-expression in new thread.
   * Returns co-expression.
   * USAGE: spawnThread(coexpr)
   */
  public static Object spawnThread (Object... args) {
	if ((args == null) || (args.length < 1)) { return null; }
	IconCoExpression coexpr = (IconCoExpression) args[0];
	if (coexpr == null) { return null; };
	return coexpr.refresh().createThread();
  }

  //==========================================================================
  // Conversion functions.
  //==========================================================================

  /**
   * Produces the integer result of converting the argument x.
   * Fails if the conversion is not possible.
   * @return An integer or failure.
   */
  public static Object integer (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r = IconNumber.toBigNumber(args[0]);
      if (r == null) {
          return FAIL;
      }
      return IconNumber.asInteger(r);
  }
  
  /**
   * Produces an integer or real number resulting from the conversion of argument x.
   * Fails if the conversion is not possible.
   * @return An integer or real, or failure.
   */
  public static Object numeric (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r = IconNumber.toBigNumber(args[0]);
      if (r == null) { return FAIL; }
      return r;
  }
  
  /**
   * Produces an integer between 0 and 255 representing the character code for
   * the one character string s.
   * @return A character code as an integer
   */
  public static Object ord (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      if (!(args[0] instanceof CharSequence)) {
          throw new RuntimeException("Error code 103: not string");
      }
      CharSequence s = (CharSequence)args[0];
      if (s.length() > 1) {
          throw new RuntimeException("Error code 205: length of string > 1");
      }
      return (int)s.charAt(0);
  }
  
  /**
   * Produces a real number result from converting argument x.
   * Fails if the conversion is not possible.
   * @return A real number
   */
  public static Object real (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r = IconNumber.toBigNumber(args[0]);
      if (r == null) { return FAIL; }
      return IconNumber.asReal(r);
  }
  
  /**
   * Produces a string resulting from converting x.
   * Since Java objects have toString(), failure isn't an option.
   * @return A string representation of the argument
   */
  public static Object string (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      return args[0].toString();
  }
  
  //==========================================================================
  // Arithmetic functions.
  //==========================================================================

  /**
   * Produces the maximum of r or -r
   * @return r if r>=0, else -r
   */
  public static Object abs (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r = IconNumber.toBigNumber(args[0]);
      if (r == null) {
          throw new RuntimeException("Error code 102: not number");
      }
      
      if (r instanceof BigInteger) {
          return ((BigInteger) r).abs();
      }
      if (r instanceof BigDecimal) {
          return ((BigDecimal) r).abs();
      }
      if ((r instanceof Float) || (r instanceof Double)) {
	  return Math.abs(r.doubleValue());
      }
      return Math.abs(r.longValue());
  }
  
  /**
   * Produces the arc cosine of r. The argument is given in radians.
   * @return A real number
   */
  public static Object acos (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r = IconNumber.toBigNumber(args[0]);
      if (r == null) {
          throw new RuntimeException("Error code 102: not number");
      }
      
      return Math.acos(r.doubleValue());
  }
  
  /**
   * Produces the arc sine of r. The argument is given in radians.
   * @return A real number
   */
  public static Object asin (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r = IconNumber.toBigNumber(args[0]);
      if (r == null) {
          throw new RuntimeException("Error code 102: not number");
      }
      
      return Math.asin(r.doubleValue());
  }
  
  /**
   * Produces the arc tangent of r. The argument is given in radians.
   * @return A real number
   */
  public static Object atan (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r = IconNumber.toBigNumber(args[0]);
      if (r == null) {
          throw new RuntimeException("Error code 102: not number");
      }
      
      return Math.atan(r.doubleValue());
  }
  
  /**
   * Produces the hyperbolic inverse tangent of r. The argument is given in radians.
   * @return A real number
   */
  public static Object atanh (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r = IconNumber.toBigNumber(args[0]);
      if (r == null) {
          throw new RuntimeException("Error code 102: not number");
      }
      
      return 0.5 * Math.log((r.doubleValue() + 1)/(r.doubleValue() - 1));
  }
  
  /**
   * Produces the cosine of an angle r.
   * The argument r is specified to be in radians.
   * @return A real number
   */
  public static Object cos (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r = IconNumber.toBigNumber(args[0]);
      if (r == null) {
          throw new RuntimeException("Error code 102: not number");
      }
      
      return Math.cos(r.doubleValue());
  }
  
  /**
   * Produces the equivalent of r degrees in radians
   * @return A real number
   */
  public static Object dtor (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r = IconNumber.toBigNumber(args[0]);
      if (r == null) {
          throw new RuntimeException("Error code 102: not number");
      }
      
      return Math.toRadians(r.doubleValue());
  }
  
  /**
   * Produces the result of &e ^ r.
   * The argument r is specified to be in radians.
   * @return A real number
   */
  public static Object exp (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r = IconNumber.toBigNumber(args[0]);
      if (r == null) {
          throw new RuntimeException("Error code 102: not number");
      }
      
      return Math.exp(r.doubleValue());
  }
  
  /**
   * Produces the bitwise and of two integers i1 and i2.
   * @return An integer
   */
  public static Object iand (Object... args) {
	if ((args == null) || (args.length < 2)) { return FAIL; }
        Number[] aligned = IconNumber.alignBigIntegers(args[0], args[1]);
        if (aligned == null) {
            throw new RuntimeException("Error code 101: not integer");
        }
        Number xn = aligned[0];
        Number yn = aligned[1];
        Number isBig = aligned[2];
	if ((isBig == IconNumber.BIGDECIMAL) || (isBig == IconNumber.REAL)) {
            throw new RuntimeException("Error code 101: not integer");
	}
        if (isBig == IconNumber.BIGINTEGER) {
		return ((BigInteger) xn).and((BigInteger)yn);
        }
	return xn.intValue() & yn.intValue();
  }
  
  /**
   * Produces the bitwise complement of an integer i1.
   * @return An integer
   */
  public static Object icom (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number xn = IconNumber.toBigNumber(args[0]);
      if (xn == null) {
          throw new RuntimeException("Error code 101: not integer");
      }
      if (xn instanceof BigDecimal) {
          throw new RuntimeException("Error code 101: not integer");
      }
      if (xn instanceof BigInteger) {
          return ((BigInteger) xn).not();
      }
      return ~xn.intValue();
  }
  
  /**
   * Produces the bitwise or of two integers i1 and i2.
   * @return An integer
   */
  public static Object ior (Object... args) {
	if ((args == null) || (args.length < 2)) { return FAIL; }
        Number[] aligned = IconNumber.alignBigIntegers(args[0], args[1]);
        if (aligned == null) {
            throw new RuntimeException("Error code 101: not integer");
        }
        Number xn = aligned[0];
        Number yn = aligned[1];
        Number isBig = aligned[2];
	if ((isBig == IconNumber.BIGDECIMAL) || (isBig == IconNumber.REAL)) {
            throw new RuntimeException("Error code 101: not integer");
	}
        if (isBig == IconNumber.BIGINTEGER) {
		return ((BigInteger) xn).or((BigInteger)yn);
        }
	return xn.intValue() | yn.intValue();
  }

  /**
   * Bit shift x by y positions.
   * If y > 0, shifts left with zero fill.
   * If y < 0, shifts right with sign extension.
   * Vacated bit positions are filled with 0.
   * For BigInteger, since << and >> do not work,
   * must use shiftLeft() and shiftRight().
   * Error if either operand is not an integer.
   */
  public static Object ishift (Object... args) {
	if ((args == null) || (args.length < 2)) { return FAIL; }
	Number xn = IconNumber.toBigNumber(args[0]);
	Number yn = IconNumber.toNumber(args[1]);
	if ((xn == null) || (yn == null)) {	// return FAIL;
		throw new RuntimeException("Error code 101: not integer");
	}
	int shift = yn.intValue();

	// Most common case will be BigDecimal or BigInteger
	if (xn instanceof BigInteger) {
	    if (shift >= 0) {
		return ((BigInteger) xn).shiftLeft(shift);
	    }
	    return ((BigInteger) xn).shiftRight(-shift);
	}
	if (xn instanceof BigDecimal) {
		throw new RuntimeException("Error code 101: not integer");
	}
	// Let Java throw error on other non-integer arguments
	if (shift >= 0) {
		return xn.longValue() << shift;
	}
	return xn.longValue() >> (-shift);
  }
  
  /**
   * Produces the bitwise exclusive or of two integers i1 and i2.
   * @return An integer
   */
  public static Object ixor (Object... args) {
	if ((args == null) || (args.length < 2)) { return FAIL; }
        Number[] aligned = IconNumber.alignBigIntegers(args[0], args[1]);
        if (aligned == null) {
            throw new RuntimeException("Error code 101: not integer");
        }
        Number xn = aligned[0];
        Number yn = aligned[1];
        Number isBig = aligned[2];
	if ((isBig == IconNumber.BIGDECIMAL) || (isBig == IconNumber.REAL)) {
            throw new RuntimeException("Error code 101: not integer");
	}
        if (isBig == IconNumber.BIGINTEGER) {
		return ((BigInteger) xn).xor((BigInteger)yn);
        }
	return xn.intValue() ^ yn.intValue();
  }
  
  /**
   * Produces the logarithm of r1 to the base r2. r2 defaults to &e.
   * USAGE: log(r1,r2:&e)
   * @return A real number
   */
  public static Object log (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r1 = IconNumber.toBigNumber(args[0]);
      if (r1 == null) {
          throw new RuntimeException("Error code 102: not number");
      }
      
      // If only one argument is given, r2 defaults to &e
      if (args.length < 2) {
          return Math.log(r1.doubleValue());
      }
      
      Number r2 = IconNumber.toBigNumber(args[1]);
      if (r2 == null) {
          throw new RuntimeException("Error code 102: not number");
      }
      
      // Simple change of base formula
      return Math.log(r1.doubleValue()) / Math.log(r2.doubleValue());
  }
  
  /**
   * Produces the equivalent of r radians in degrees
   * @return A real number
   */
  public static Object rtod (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r = IconNumber.toBigNumber(args[0]);
      if (r == null) {
          throw new RuntimeException("Error code 102: not number");
      }
      
      return Math.toDegrees(r.doubleValue());
  }
  
  /**
   * Produces the sine of an angle r.
   * The argument r is specified to be in radians.
   * @return A real number
   */
  public static Object sin (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r = IconNumber.toBigNumber(args[0]);
      if (r == null) {
          throw new RuntimeException("Error code 102: not number");
      }
      
      return Math.sin(r.doubleValue());
  }
  
  /**
   * Produces the square root of a number r.
   */
  public static Object sqrt (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r = IconNumber.toBigNumber(args[0]);
      if (r == null) {
          throw new RuntimeException("Error code 102: not number");
      }
      return Math.sqrt(r.doubleValue());
  }
  
  /**
   * Produces the tangent of an angle r.
   * The argument r is specified to be in radians.
   * @return A real number
   */
  public static Object tan (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r = IconNumber.toBigNumber(args[0]);
      if (r == null) {
          throw new RuntimeException("Error code 102: not number");
      }
      
      return Math.tan(r.doubleValue());
  }
  
  //==========================================================================
  // String utility functions.
  //==========================================================================
  
  /**
   * Generates the sequence of integer positions in s preceding a character of
   * c1 in s[i1:i2] that is balanced with respect to characters in c2 and c3,
   * but fails if there is no such position.
   * @return A sequence of integers
   */
  public static Object bal (Object... args) {
      // default arguments
      Set<String> c1 = IconKeywords.cset;
      Set<String> c2 = new LinkedHashSet<>();
      c2.add("(");
      Set<String> c3 = new LinkedHashSet<>();
      c3.add(")");
      String s = (String)IconKeywords.subject.get();
      int i1 = ((Number)IconKeywords.pos.get()).intValue();
      int i2 = 0;
      
      // TODO: this argument handling currently doesn't respect argument order.
      // However, this is a hard problem for bal, seems essentially equivalent to
      // implementing a miniature multi-dispatch algorithm, due to 6 possibly
      // defaulted arguments.
      int cnext = 0, inext = 0;
      for (Object o: args) {
          if (o instanceof Set) {
              if (cnext == 0) {
                  c1 = (Set<String>)o;
                  cnext += 1;
              } else if (cnext == 1) {
                  c2 = (Set<String>)o;
                  cnext += 1;
              } else {
                  c3 = (Set<String>)o;
              }
          }
          else if (o instanceof CharSequence) {
              s = (String)o;
          }
          else {
              Number r = IconNumber.toNumber(o);
              if (r == null) {
                  throw new RuntimeException("Error code 101: not integer");
              }
              if (inext == 0) {
                  i1 = r.intValue();
                  inext += 1;
              } else {
                  i2 = r.intValue();
              }
          }
      }
      
      int count = 0;
      List<Integer> results = new ArrayList<>();
      while (i1 < i2) {
          String c = s.substring(i1-1, i1);
          if (count == 0 && c1.contains(c)) {
              results.add(i1);
          }
          if (c2.contains(c)) { count += 1; }
          if (c3.contains(c)) { count -= 1; }
          if (count < 0) {
              return new IconPromote(results).setListAsValue();
          }
          i1 += 1;
      }
      return new IconPromote(results).setListAsValue();
  }
  
  /**
   * Produces a single character string whose internal representation is i.
   * @return A string
   */
  public static Object charUnicon (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number r = IconNumber.toNumber(args[0]);
      if (r == null) {
	throw new RuntimeException("Error code 101: not integer");
      }
      int charcode = r.intValue();
      return String.valueOf((char)charcode);
  }
  
  /**
   * Produces a string of size i in which s1 is centered, with s2 used for
   * padding on either side as necessary.
   * @return A string
   */
  public static Object center (Object... args) {
      return pad(PadType.CENTER, args);
  }
  
  /**
   * Produces a string of size i in which s1 is positioned at the left, with
   * s2 used for padding at the right as necessary.
   * @return A string
   */
  public static Object left (Object... args) {
      return pad(PadType.LEFT, args);
  }
  
  /**
   * Produces a string with the same length as s1 obtained by mapping characters
   * in s1 that occur in s2 into corresponding characters in s3.
   * @return A string
   */
  public static Object map (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      if (!(args[0] instanceof CharSequence)) {
          throw new RuntimeException("Error code 103: not string");
      }
      
      String s1 = args[0].toString();
      String s2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
      String s3 = "abcdefghijklmnopqrstuvwxyz";
      if (args.length > 1) {
          if (args[1] instanceof CharSequence) {
              s2 = args[1].toString();
          } else {
              throw new RuntimeException("Error code 103: not string");
          }
      }
      if (args.length > 2) {
          if (args[2] instanceof CharSequence) {
              s3 = args[2].toString();
          } else {
              throw new RuntimeException("Error code 103: not string");
          }
      }
      
      if (s2.length() != s3.length()) {
          throw new RuntimeException("Error code 208: *s2 ~= *s3");
      }
      
      for (int i = 0; i < s2.length(); i++) {
          s1 = s1.replace(s2.charAt(i), s3.charAt(i));
      }
      return s1;
  }
  
  /**
   * Produces a string consisting of i concatenations of s.
   * @return A string
   */
  public static Object repl (Object... args) {
      if ((args == null) || (args.length < 2)) { return FAIL; }
      if (!(args[0] instanceof CharSequence)) {
          throw new RuntimeException("Error code 103: not string");
      }
      Number r = IconNumber.toNumber(args[1]);
      if (r == null) {
          throw new RuntimeException("Error code 101: not integer");
      }
      if (r.intValue() < 0) {
          throw new RuntimeException("Error code 205: i < 0");
      }
      
      String s = args[0].toString();
      String result = "";
      for (int i = 0; i < r.intValue(); i++) {
          result = result.concat(s);
      }
      return result;
  }
  
  /**
   * Produces the reverse of the string s or list L.
   * @return A string or list
   */
  public static Object reverse (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      if ((args[0] instanceof CharSequence)) {
          return new StringBuilder(args[0].toString()).reverse().toString();
      }
      if (args[0] instanceof List) {
          Collections.reverse((List) args[0]);
          return args[0];
      }
      
      throw new RuntimeException("Error code 110: not string or list");
  }
  
  /**
   * Produces a string of size i in which s1 is positioned at the right,
   * with s2 used for padding at the left as necessary.
   * @return A string
   */
  public static Object right (Object... args) {
      return pad(PadType.RIGHT, args);
  }
  
  /**
   * Removes the characters in c from s at the back (i=-1),
   * the front (i=1), or both ends (i=0). c defaults to a sequence containing
   * just the space character, and i defaults to -1.
   * @return A string
   */
  public static Object trim (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      if (!(args[0] instanceof CharSequence)) {
          throw new RuntimeException("Error code 103: not string");
      }
      
      int side = -1;
      String cset = " ";
      if (args.length > 1) {
          Number r = IconNumber.toNumber(args[1]);
          if (args[1] instanceof Set) {
              cset = "";
              Set<String> l = (Set) args[1];
              for (String s : l) {
                  cset = cset.concat(s);
              }
          } else if (r != null) {
              side = r.intValue();
          } else {
              throw new RuntimeException("Error code 104: not cset");
          }
      }
      
      if (args.length == 3) {
          Number r = IconNumber.toNumber(args[2]);
          if (r == null) {
              throw new RuntimeException("Error code 101: not integer");
          }
          side = r.intValue();
      }
      
      String input = args[0].toString();
      int length = input.length();
      int iter = 0;
      if (side > -1) {
          while (iter != length && cset.indexOf(input.charAt(iter)) != -1) {
              iter += 1;
          }
      }
      
      input = input.substring(iter);
      length = input.length();
      iter = length-1;
      
      if (side < 1) {
          while (iter != 0 && cset.indexOf(input.charAt(iter)) != -1) {
              iter -= 1;
          }
      }
      
      return input.substring(0, iter+1);
  }
  
  private static enum PadType { LEFT, CENTER, RIGHT }
  
  private static Object pad (PadType p, Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      if (!(args[0] instanceof CharSequence)) {
          throw new RuntimeException("Error code 103: not string");
      }
      
      int length = 1;
      String pad = " ";
      if (args.length > 1) {
          if (args[1] instanceof CharSequence) {
              pad = args[1].toString();
          } else {
              Number r = IconNumber.toNumber(args[1]);
              if (r == null) {
                  throw new RuntimeException("Error code 101: not integer");
              }
              length = r.intValue();
              if (length < 0) {
                  throw new RuntimeException("Error code 205: i < 0");
              }
          }
      }
      
      if (args.length == 3) {
          Number r = IconNumber.toNumber(args[2]);
          if (r == null) {
              throw new RuntimeException("Error code 101: not integer");
          }
          length = r.intValue();
      }
      
      String input = args[0].toString();
      StringBuilder result = new StringBuilder(input);
      switch (p) {
          case LEFT:
              if (length == input.length()) { return input; }
              if (length < input.length()) {
                  return input.substring(0, length);
              }
              while (result.length() < length) {
                  result.append(pad);
              }
              return result.toString().substring(result.length() - length);
          case RIGHT:
              if (length == input.length()) { return input; }
              if (length < input.length()) {
                  return input.substring(input.length() - length);
              }
              while (result.length() < length) {
                  result.insert(0, pad);
              }
              return result.toString().substring(result.length() - length);
          default:
              if (length == input.length()) { return input; }
              if (length < input.length()) {
                  return input.substring((input.length() - length+1)/2, length);
              }
              while (result.length() < length) {
                  result.insert(0, pad);
                  result.append(pad);
              }
              return result.toString().substring((result.length() - length+1)/2, length);
      }
  }

  //==========================================================================
  // String scanning functions.
  //==========================================================================

  /**
   * Generates the occurrences of s in the string subj[from:to].
   * USAGE: find(s, subj={@literal &}subject, from=1 if subj else pos, to=0)
   *	where "to" is exclusive and 0 denotes after end of string.
   * @return Iterator over the positions of s, or fail if not found.
   */
  public static Object find (Object... args) {
	return stringFunctions(StringFtype.FIND, args);
  }

  /**
   * Test if s is an initial substring of subj[from:to].
   * USAGE: match(s, subj={@literal &}subject, from=1 if subj else pos, to=0)
   * @return position beyond initial substring, or fail if not found.
   */
  public static Object match (Object... args) {
	return stringFunctions(StringFtype.MATCH, args);
  }

  /**
   * Produces substring at {@literal &}pos of length len.
   * Increments {@literal &}pos by len.
   * Reverses the assignment to {@literal &}pos if it is resumed.
   * USAGE: move(len)
   * @return {@literal &}subject[{@literal &}pos:{@literal &}pos+len], or fail if len is out of range.
   */
  public static Object move (Object... args) {
	return stringFunctions(StringFtype.MOVE, args);
  }

  /**
   * Produces substring from {@literal &}pos to "to", exclusive.
   * Sets {@literal &}pos to "to".
   * Reverses the assignment to {@literal &}pos if it is resumed.
   * USAGE: tab(to)
   * @return {@literal &}subject[{@literal &}pos:to], or fail if "to" is out of range.
   */
  public static Object tab (Object... args) {
	return stringFunctions(StringFtype.TAB, args);
  }

  /**
   * Generates locations where any character in c occurs in subj[from:to].
   * Accepts c as either character set or string equivalent.
   * USAGE: upto(c, subj={@literal &}subject, from=1 if subj else pos, to=0)
   * @return Iterator over the occurrences of s, or fail if not found.
   */
  public static Object upto (Object... args) {
	return stringFunctions(StringFtype.UPTO, args);
  }

  /**
   * Produces the position after the longest initial sequence of chars from c
   * in subj[from:to].
   * Accepts c as either character set or string equivalent.
   * USAGE: many(c, subj={@literal &}subject, from=1 if subj else pos, to=0)
   * @return position after subset of c in s, or fail if not found.
   */
  public static Object many (Object... args) {
	return stringFunctions(StringFtype.MANY, args);
  }

  /**
   * Produces the position after the first match of any char from c
   * in subj[from:to].
   * Accepts c as either character set or string equivalent.
   * USAGE: any(c, subj={@literal &}subject, from=1 if subj else pos, to=0)
   * @return first position of any c in s, or fail if not found.
   */
  public static Object any (Object... args) {
	return stringFunctions(StringFtype.ANY, args);
  }

  /**
   * Produces {@literal &}pos if i or its positive equivalent is equal
   * to {@literal &}pos.
   * USAGE: pos(i)
   * @return {@literal &}pos, or fails if not equal to {@literal &}pos.
   */
  public static Object pos (Object... args) {
	return stringFunctions(StringFtype.POS, args);
  }

  //==========================================================================
  // String utility functions
  //==========================================================================
  private static enum StringFtype { FIND, MATCH, UPTO, MANY, ANY,
					TAB, MOVE, POS };

  /**
   * Performs string functions on quadruple arguments.
   * For character set arguments, will also accepts string equivalent.
   * USAGE: stringFunction(str, subj={@literal &}subject, from=1 if subj else pos, to=0)
   */
  private static Object stringFunctions (StringFtype ftype, Object... args) {
	// Argument processing
	if ((args == null) || (args.length < 1) || (args[0] == null)) {
		return FAIL; }
	String str = null;
	String subj = null;
	int from = 1;
	int to = 0;
	IconScan env = IconScan.getScanEnv();

	// Argument is (position)
	if ((ftype == StringFtype.TAB) || (ftype == StringFtype.MOVE) ||
			(ftype == StringFtype.POS)) {
		subj = env.getSubject();
		from = (int) env.getPos();

		Number toNumber = IconNumber.toNumber(args[0]);
		if (toNumber == null) { return FAIL; }
		to = toNumber.intValue();
		if (ftype == StringFtype.MOVE) { to += from; }
		if (ftype == StringFtype.POS) {
			if (from == IconIndex.convertStringPosition(subj, to,
					IconScan.getScanEnv().getOrigin())) {
				return Long.valueOf(from);
			}
			return FAIL;
		}
	} else {
	  // Arguments are (str, subj=&subject, from=1 if subj else pos, to=0)
	  if ((ftype == StringFtype.UPTO) || (ftype == StringFtype.MANY) ||
			(ftype == StringFtype.ANY)) {
		// Character set to string
		if (args[0] instanceof CharSequence) {
			str = args[0].toString(); 
		} else if (args[0] instanceof Set) {
			str = ((Set<String>) args[0]).stream().map(Object::toString).collect(Collectors.joining());
		} else { return FAIL; }
	  } else {	// FIND or MATCH
		if (! (args[0] instanceof CharSequence)) { return FAIL; }
		str = args[0].toString();
	  }
	  if (args.length > 1) {
		if ((args[1] == null) || (! (args[1] instanceof CharSequence))){
			return FAIL; }
		subj = args[1].toString();
		if (args.length > 2) {
			Number fromNumber = IconNumber.toNumber(args[2]);
			if (fromNumber == null) { return FAIL; }
			from = fromNumber.intValue();
			if (args.length > 3) {
			    Number toNumber = IconNumber.toNumber(args[2]);
			    if (toNumber == null) { return FAIL; }
			    to = toNumber.intValue();
			}
		}
	  } else {
		subj = env.getSubject();
		from = (int) env.getPos();
	  }
	}

	// Convert string positions in subject[from:to] to use Java indexes
	int[] endUpdate = {-1};
	int origin = IconScan.getScanEnv().getOrigin();
		// int origin = IconNumber.getIndexOrigin();
	endUpdate[0] = to;
	int begin = IconIndex.adjustSlice(from, endUpdate, subj.length(),
		origin, true); // islice
	int end = endUpdate[0];
	if ((begin < 0) || (end < 0)) {	// ERROR: begin >= size || end > size
		return FAIL;
		// throw new IndexOutOfBoundsException(
		//	"Index out of bounds " + begin + " : " + end);
	}
	String target = subj.substring(begin, end);  // end is exclusive of last

	// Perform string function
	switch (ftype) {
	  case FIND: {	// Generate pos of str in target, offset by origin
		String text = str;	// Effectively final
		return new IconIterator() {
		  Matcher matcher;
		  public void afterRestart () {
			matcher = Pattern.compile(
				Pattern.quote(text)).matcher(target);
		  }
		  public IIconAtom provideNext () {
			if (! matcher.find()) {
				return FAIL;
			}
			return IconValue.create(
				matcher.start() + begin + origin );
		  }
		};
		}

	  case MATCH: {	// Test if s begins target, return position beyond it
		if (target.startsWith(str)) {
			return Long.valueOf(str.length() + begin + origin);
		}
		return FAIL;
		}

	  case UPTO: {	// Generate pos of any char of str in target
		String text = str;	// Effectively final
		return new IconIterator() {
		  Matcher matcher;
		  public void afterRestart () {
			matcher = Pattern.compile("[" +
				Pattern.quote(text) + "]").matcher(target);
		  }
		  public IIconAtom provideNext () {
			if (! matcher.find()) {
				return FAIL;
			}
			return IconValue.create(
				matcher.start() + begin + origin );
		  }
		};
		}

	  case MANY: {	// Pos after longest initial seq of chars from str
		Matcher matcher = Pattern.compile("[" +
			Pattern.quote(str) + "]+").matcher(target);
		if (matcher.lookingAt()) {	// end() index is exclusive
			return Long.valueOf(matcher.end() + begin + origin);
		}
		return FAIL;
		}

	  case ANY: {	// First pos of any char of str in target
		Matcher matcher = Pattern.compile("[" +
			Pattern.quote(str) + "]").matcher(target);
		if (matcher.find()) {
			return Long.valueOf(matcher.end() + begin + origin);
		}
		return FAIL;
		}

	  case MOVE: {	// Produces substring at &pos of length len.
		env.setPos(to);
		return target;
		}

	  case TAB: {	// Produces substring from &pos to "to".
		env.setPos(to);
		return target;
		}

	  default: return FAIL;
	}
  }

}

//==== END OF FILE
