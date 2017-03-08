package edu.uidaho.junicon.test.junicon;
import edu.uidaho.junicon.runtime.junicon.iterators.*;
import edu.uidaho.junicon.runtime.junicon.constructs.*;
import edu.uidaho.junicon.runtime.junicon.operators.*;
import edu.uidaho.junicon.runtime.junicon.annotations.*;
import static edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.FAIL;
import static edu.uidaho.junicon.runtime.junicon.operators.IconFunctions.*;
import static edu.uidaho.junicon.runtime.junicon.operators.UniconFunctions.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
public class QuickSort {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	private static MethodBodyCache staticMethodCache = new MethodBodyCache();
	// Static method initializer cache
	private static ConcurrentHashMap<String,Object> initialMethodCache = new ConcurrentHashMap();
	// Method references
	@MMethodRef(name="main", methodName="main_m")
	public Object main = (VariadicFunction) this::main_m;
	@MMethodRef(name="quicksort", methodName="quicksort")
	public Object quicksort = (VariadicFunction) this::quicksort;
	@MMethodRef(name="quickpartition", methodName="quickpartition")
	public Object quickpartition = (VariadicFunction) this::quickpartition;
	@MMethodRef(name="sortop", methodName="sortop")
	public static Object sortop = (VariadicFunction) (Object... args_19) -> sortop(args_19);
	@MMethodRef(name="cmp", methodName="cmp")
	public static Object cmp = (VariadicFunction) (Object... args_20) -> cmp(args_20);
	// Constructors
	public QuickSort() {
		;
	}
	// Static variadic constructor
	public static VariadicFunction QuickSort = (Object... args_15) -> {
		return new QuickSort();
	};
	// Method static variables
	private static Object pivotL_s;
	private static IconVar pivotL_s_r = new IconVar(()-> pivotL_s, (rhs)-> pivotL_s=rhs).local();
	// Methods
	@MMethod(name="main", methodName="main_m")
	@MParameter(name="args", reifiedName="args_r", type="", isVararg=true)
	@MLocal(name="size", reifiedName="size_r", type="")
	@MLocal(name="m", reifiedName="m_r", type="")
	@MLocal(name="i", reifiedName="i_r", type="")
	@MLocal(name="result", reifiedName="result_r", type="")
	private IIconIterator main_m (Object... args_21) {
		// Reuse method body
		IconIterator body = methodCache.getFree("main_m");
		if (body != null) { return body.reset().unpackArgs(args_21); };
		// Reified parameters
		IconVar args_r = new IconVar().local();
		// Temporaries
		IconTmp x_2_r = new IconTmp();
		IconTmp x_1_r = new IconTmp();
		IconTmp x_4_r = new IconTmp();
		IconTmp x_3_r = new IconTmp();
		// Locals
		IconVar size_r = new IconVar().local();
		IconVar m_r = new IconVar().local();
		IconVar i_r = new IconVar().local();
		IconVar result_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack = (Object... params) -> {
			if (params ==  null) { params = IIconAtom.getEmptyArray(); };
			args_r.set((params.length > 0) ? Arrays.asList(params).subList(0, params.length) : new ArrayList());
			// Reset locals
			size_r.set(null);
			m_r.set(null);
			i_r.set(null);
			result_r.set(null);
			return null;
		};
		// Method body
		body = new IconSequence(new IconAssign().over(new IconSingleton(size_r), new IconOperation(IconOperators.plusUnary).over(new IconIndexIterator(args_r, IconValue.create(1)))), new IconAssign().over(new IconSingleton(m_r), IconVarIterator.createAsList(()-> new IconList())), new IconEvery(new IconAssign().over(new IconSingleton(i_r), new IconToIterator(IconValue.create(1), size_r)), new IconBlock( () -> {
			// Temporaries
			IconTmp x_0_r = new IconTmp();
			return new IconProduct(new IconIn(x_0_r, new IconOperation(IconOperators.plus).over((new IconOperation(IconOperators.minus).over(new IconSingleton(size_r), new IconSingleton(i_r))), new IconValueIterator(1))), new IconInvokeIterator(()-> ((VariadicFunction) put).apply(m_r.deref(), x_0_r.deref())));
		}
 )), new IconInvokeIterator(()-> ((VariadicFunction) write).apply("Before: ")), new IconProduct(new IconIn(x_2_r, new IconProduct(new IconIn(x_1_r, new IconOperation(IconOperators.timesUnary).over(new IconSingleton(m_r))), new IconIndexIterator(m_r, x_1_r))), new IconInvokeIterator(()-> ((VariadicFunction) write).apply(x_2_r.deref()))), new IconAssign().over(new IconSingleton(result_r), new IconInvokeIterator(()-> ((VariadicFunction) quicksort).apply(m_r.deref(), cmp))), new IconInvokeIterator(()-> ((VariadicFunction) write).apply("After:  ")), new IconProduct(new IconIn(x_4_r, new IconProduct(new IconIn(x_3_r, new IconOperation(IconOperators.timesUnary).over(new IconSingleton(m_r))), new IconIndexIterator(result_r, x_3_r))), new IconInvokeIterator(()-> ((VariadicFunction) write).apply(x_4_r.deref()))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body.setCache(methodCache, "main_m");
		body.setUnpackClosure(unpack).unpackArgs(args_21);
		return body;
	}
	@MMethod(name="quicksort", methodName="quicksort")
	@MParameter(name="X", reifiedName="X_r", type="")
	@MParameter(name="op", reifiedName="op_r", type="")
	@MParameter(name="lower", reifiedName="lower_r", type="")
	@MParameter(name="upper", reifiedName="upper_r", type="")
	@MLocal(name="pivot", reifiedName="pivot_r", type="")
	@MLocal(name="x", reifiedName="x_r", type="")
	public IIconIterator quicksort (Object... args_22) {
		// Reuse method body
		IconIterator body_23 = methodCache.getFree("quicksort_m");
		if (body_23 != null) { return body_23.reset().unpackArgs(args_22); };
		// Reified parameters
		IconVar X_r = new IconVar().local();
		IconVar op_r = new IconVar().local();
		IconVar lower_r = new IconVar().local();
		IconVar upper_r = new IconVar().local();
		// Locals
		IconVar pivot_r = new IconVar().local();
		IconVar x_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_24 = (Object... params_25) -> {
			if (params_25 ==  null) { params_25 = IIconAtom.getEmptyArray(); };
			X_r.set((params_25.length > 0) ? params_25[0] : null);
			op_r.set((params_25.length > 1) ? params_25[1] : null);
			lower_r.set((params_25.length > 2) ? params_25[2] : null);
			upper_r.set((params_25.length > 3) ? params_25[3] : null);
			// Reset locals
			pivot_r.set(null);
			x_r.set(null);
			return null;
		};
		// Method body
		body_23 = new IconSequence(new IconIf(new IconAssign().over(new IconOperation(IconOperators.failIfNonNull).over(new IconSingleton(lower_r)), new IconValueIterator(1)), new IconSequence(new IconAssign().over(new IconSingleton(upper_r), new IconOperation(IconOperators.timesUnary).over(new IconSingleton(X_r))), new IconAssign().over(new IconSingleton(op_r), new IconInvokeIterator(()-> ((VariadicFunction) sortop).apply(op_r.deref(), X_r.deref()))))), new IconIf(new IconOperation(IconOperators.greaterThan).over(new IconOperation(IconOperators.minus).over(new IconSingleton(upper_r), new IconSingleton(lower_r)), new IconValueIterator(0)), new IconBlock( () -> {
			// Temporaries
			IconTmp x_5_r = new IconTmp();
			return new IconSequence(new IconEvery(new IconAssign().over(new IconSingleton(x_r), new IconInvokeIterator(()-> ((VariadicFunction) quickpartition).apply(X_r.deref(), op_r.deref(), lower_r.deref(), upper_r.deref()))), new IconAssign().over(new IconConcat(new IconOperation(IconOperators.failIfNonNull).over(new IconSingleton(pivot_r)), new IconSingleton(X_r)), new IconSingleton(x_r))), new IconAssign().over(new IconSingleton(X_r), new IconProduct(new IconIn(x_5_r, new IconOperation(IconOperators.minus).over(new IconSingleton(pivot_r), new IconValueIterator(1))), new IconInvokeIterator(()-> quicksort(X_r.deref(), op_r.deref(), lower_r.deref(), x_5_r.deref())))), new IconAssign().over(new IconSingleton(X_r), new IconInvokeIterator(()-> quicksort(X_r.deref(), op_r.deref(), pivot_r.deref(), upper_r.deref()))));
		}
 )), new IconReturn(new IconSingleton(X_r)), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_23.setCache(methodCache, "quicksort_m");
		body_23.setUnpackClosure(unpack_24).unpackArgs(args_22);
		return body_23;
	}
	@MMethod(name="quickpartition", methodName="quickpartition")
	@MParameter(name="X", reifiedName="X_r", type="")
	@MParameter(name="op", reifiedName="op_r", type="")
	@MParameter(name="lower", reifiedName="lower_r", type="")
	@MParameter(name="upper", reifiedName="upper_r", type="")
	@MLocal(name="pivot", reifiedName="pivot_r", type="")
	@MLocal(name="pivotL_s", reifiedName="pivotL_s_r", type="")
	public IIconIterator quickpartition (Object... args_26) {
		// Reuse method body
		IconIterator body_27 = methodCache.getFree("quickpartition_m");
		if (body_27 != null) { return body_27.reset().unpackArgs(args_26); };
		// Reified parameters
		IconVar X_r = new IconVar().local();
		IconVar op_r = new IconVar().local();
		IconVar lower_r = new IconVar().local();
		IconVar upper_r = new IconVar().local();
		// Temporaries
		IconTmp x_6_r = new IconTmp();
		IconTmp x_7_r = new IconTmp();
		IconTmp x_8_r = new IconTmp();
		IconTmp x_9_r = new IconTmp();
		IconTmp x_10_r = new IconTmp();
		// Locals
		IconVar pivot_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_28 = (Object... params_29) -> {
			if (params_29 ==  null) { params_29 = IIconAtom.getEmptyArray(); };
			X_r.set((params_29.length > 0) ? params_29[0] : null);
			op_r.set((params_29.length > 1) ? params_29[1] : null);
			lower_r.set((params_29.length > 2) ? params_29[2] : null);
			upper_r.set((params_29.length > 3) ? params_29[3] : null);
			// Reset locals
			pivot_r.set(null);
			return null;
		};
		// Initialize method on first use
		if (initialMethodCache.get("quickpartition_m") == null) {
			initialMethodCache.computeIfAbsent("quickpartition_m", (java.util.function.Function)  (arg) -> {
				(new IconAssign().over(new IconSingleton(pivotL_s_r), new IconInvokeIterator(()-> ((VariadicFunction) list).apply(IconNumber.create(3))))).nextOrNull(); return true;
			});
		}
		// Method body
		body_27 = new IconSequence(new IconAssign().over(new IconIndexIterator(pivotL_s_r, IconValue.create(1)), new IconIndexIterator(X_r, lower_r)), new IconAssign().over(new IconIndexIterator(pivotL_s_r, IconValue.create(2)), new IconIndexIterator(X_r, upper_r)), new IconAssign().over(new IconIndexIterator(pivotL_s_r, IconValue.create(3)), new IconProduct(new IconIn(x_6_r, new IconOperation(IconOperators.plus).over(new IconSingleton(lower_r), new IconOperation(IconOperators.questionMarkUnary).over((new IconOperation(IconOperators.minus).over(new IconSingleton(upper_r), new IconSingleton(lower_r)))))), new IconIndexIterator(X_r, x_6_r))), new IconIf(new IconProduct(new IconProduct(new IconIn(x_7_r, new IconIndexIterator(pivotL_s_r, IconValue.create(2))), new IconIn(x_8_r, new IconIndexIterator(pivotL_s_r, IconValue.create(1)))), new IconInvokeIterator(()-> ((VariadicFunction) op_r.deref()).apply(x_7_r.deref(), x_8_r.deref()))), new IconAssign().swap().over(new IconIndexIterator(pivotL_s_r, IconValue.create(2)), new IconIndexIterator(pivotL_s_r, IconValue.create(1)))), new IconIf(new IconProduct(new IconProduct(new IconIn(x_9_r, new IconIndexIterator(pivotL_s_r, IconValue.create(3))), new IconIn(x_10_r, new IconIndexIterator(pivotL_s_r, IconValue.create(2)))), new IconInvokeIterator(()-> ((VariadicFunction) op_r.deref()).apply(x_9_r.deref(), x_10_r.deref()))), new IconAssign().swap().over(new IconIndexIterator(pivotL_s_r, IconValue.create(3)), new IconIndexIterator(pivotL_s_r, IconValue.create(2)))), new IconAssign().over(new IconSingleton(pivot_r), new IconIndexIterator(pivotL_s_r, IconValue.create(2))), new IconAssign().augment(IconOperators.minus).over(new IconSingleton(lower_r), new IconValueIterator(1)), new IconAssign().augment(IconOperators.plus).over(new IconSingleton(upper_r), new IconValueIterator(1)), new IconWhile(new IconOperation(IconOperators.lessThan).over(new IconSingleton(lower_r), new IconSingleton(upper_r)), new IconBlock( () -> {
			// Temporaries
			IconTmp x_12_r = new IconTmp();
			IconTmp x_11_r = new IconTmp();
			IconTmp x_14_r = new IconTmp();
			IconTmp x_13_r = new IconTmp();
			return new IconSequence(new IconWhile(new IconProduct(new IconIn(x_12_r, new IconProduct(new IconIn(x_11_r, new IconAssign().augment(IconOperators.minus).over(new IconSingleton(upper_r), new IconValueIterator(1))), new IconIndexIterator(X_r, x_11_r))), new IconInvokeIterator(()-> ((VariadicFunction) op_r.deref()).apply(pivot_r.deref(), x_12_r.deref())))), new IconWhile(new IconProduct(new IconIn(x_14_r, new IconProduct(new IconIn(x_13_r, new IconAssign().augment(IconOperators.plus).over(new IconSingleton(lower_r), new IconValueIterator(1))), new IconIndexIterator(X_r, x_13_r))), new IconInvokeIterator(()-> ((VariadicFunction) op_r.deref()).apply(x_14_r.deref(), pivot_r.deref())))), new IconIf(new IconOperation(IconOperators.lessThan).over(new IconSingleton(lower_r), new IconSingleton(upper_r)), new IconAssign().swap().over(new IconIndexIterator(X_r, lower_r), new IconIndexIterator(X_r, upper_r))));
		}
 )), new IconSuspend(new IconSingleton(lower_r)), new IconSuspend(new IconSingleton(X_r)), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_27.setCache(methodCache, "quickpartition_m");
		body_27.setUnpackClosure(unpack_28).unpackArgs(args_26);
		return body_27;
	}
	@MMethod(name="sortop", methodName="sortop")
	@MParameter(name="op", reifiedName="op_r", type="")
	@MParameter(name="X", reifiedName="X_r", type="")
	public static IIconIterator sortop (Object... args_30) {
		// Reuse method body
		IconIterator body_31 = staticMethodCache.getFree("sortop_m");
		if (body_31 != null) { return body_31.reset().unpackArgs(args_30); };
		// Reified parameters
		IconVar op_r = new IconVar().local();
		IconVar X_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_32 = (Object... params_33) -> {
			if (params_33 ==  null) { params_33 = IIconAtom.getEmptyArray(); };
			op_r.set((params_33.length > 0) ? params_33[0] : null);
			X_r.set((params_33.length > 1) ? params_33[1] : null);
			return null;
		};
		// Method body
		body_31 = new IconSequence(new IconReturn(new IconVarIterator(()-> cmp, (rhs)-> cmp=rhs)), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_31.setCache(staticMethodCache, "sortop_m");
		body_31.setUnpackClosure(unpack_32).unpackArgs(args_30);
		return body_31;
	}
	@MMethod(name="cmp", methodName="cmp")
	@MParameter(name="a", reifiedName="a_r", type="")
	@MParameter(name="b", reifiedName="b_r", type="")
	public static IIconIterator cmp (Object... args_34) {
		// Reuse method body
		IconIterator body_35 = staticMethodCache.getFree("cmp_m");
		if (body_35 != null) { return body_35.reset().unpackArgs(args_34); };
		// Reified parameters
		IconVar a_r = new IconVar().local();
		IconVar b_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_36 = (Object... params_37) -> {
			if (params_37 ==  null) { params_37 = IIconAtom.getEmptyArray(); };
			a_r.set((params_37.length > 0) ? params_37[0] : null);
			b_r.set((params_37.length > 1) ? params_37[1] : null);
			return null;
		};
		// Method body
		body_35 = new IconSequence(new IconReturn(new IconOperation(IconOperators.lessThan).over(new IconSingleton(a_r), new IconSingleton(b_r))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_35.setCache(staticMethodCache, "cmp_m");
		body_35.setUnpackClosure(unpack_36).unpackArgs(args_34);
		return body_35;
	}
	// Static main method
	public static void main(String... args_38) {
		QuickSort c = new QuickSort(); VariadicFunction m = (VariadicFunction) c.main;
		IconCoExpression.activate(null, null, new IconCoExpression(
		 (Object... coexpr) -> {
			return ((IIconIterator) m.apply((Object[]) args_38)); },
		 () -> { return IconList.createArray(); } ));
	}
}
