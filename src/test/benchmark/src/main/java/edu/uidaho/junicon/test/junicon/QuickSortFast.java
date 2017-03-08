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
public class QuickSortFast {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Method references
	@MMethodRef(name="main", methodName="main_m")
	public Object main = (VariadicFunction) this::main_m;
	@MMethodRef(name="quicksort", methodName="quicksort")
	public Object quicksort = (VariadicFunction) this::quicksort;
	@MMethodRef(name="quickpartition", methodName="quickpartition")
	public Object quickpartition = (VariadicFunction) this::quickpartition;
	// Constructors
	public QuickSortFast() {
		;
	}
	// Static variadic constructor
	public static VariadicFunction QuickSortFast = (Object... args_10) -> {
		return new QuickSortFast();
	};
	// Methods
	@MMethod(name="main", methodName="main_m")
	@MParameter(name="args", reifiedName="args_r", type="", isVararg=true)
	@MLocal(name="size", reifiedName="size_r", type="")
	@MLocal(name="m", reifiedName="m_r", type="")
	@MLocal(name="i", reifiedName="i_r", type="")
	@MLocal(name="result", reifiedName="result_r", type="")
	private IIconIterator main_m (Object... args_14) {
		// Reuse method body
		IconIterator body = methodCache.getFree("main_m");
		if (body != null) { return body.reset().unpackArgs(args_14); };
		// Reified parameters
		IconVar args_r = new IconVar().local();
		// Temporaries
		IconTmp x_2_r = new IconTmp();
		IconTmp x_1_r = new IconTmp();
		IconTmp x_3_r = new IconTmp();
		IconTmp x_5_r = new IconTmp();
		IconTmp x_4_r = new IconTmp();
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
 )), new IconInvokeIterator(()-> ((VariadicFunction) write).apply("Before: ")), new IconProduct(new IconIn(x_2_r, new IconProduct(new IconIn(x_1_r, new IconOperation(IconOperators.timesUnary).over(new IconSingleton(m_r))), new IconIndexIterator(m_r, x_1_r))), new IconInvokeIterator(()-> ((VariadicFunction) write).apply(x_2_r.deref()))), new IconAssign().over(new IconSingleton(result_r), new IconProduct(new IconIn(x_3_r, new IconOperation(IconOperators.timesUnary).over(new IconSingleton(m_r))), new IconInvokeIterator(()-> ((VariadicFunction) quicksort).apply(m_r.deref(), IconNumber.create(1), x_3_r.deref())))), new IconInvokeIterator(()-> ((VariadicFunction) write).apply("After:  ")), new IconProduct(new IconIn(x_5_r, new IconProduct(new IconIn(x_4_r, new IconOperation(IconOperators.timesUnary).over(new IconSingleton(m_r))), new IconIndexIterator(result_r, x_4_r))), new IconInvokeIterator(()-> ((VariadicFunction) write).apply(x_5_r.deref()))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body.setCache(methodCache, "main_m");
		body.setUnpackClosure(unpack).unpackArgs(args_14);
		return body;
	}
	@MMethod(name="quicksort", methodName="quicksort")
	@MParameter(name="X", reifiedName="X_r", type="")
	@MParameter(name="lower", reifiedName="lower_r", type="")
	@MParameter(name="upper", reifiedName="upper_r", type="")
	@MLocal(name="pivot", reifiedName="pivot_r", type="")
	public IIconIterator quicksort (Object... args_15) {
		// Reuse method body
		IconIterator body_16 = methodCache.getFree("quicksort_m");
		if (body_16 != null) { return body_16.reset().unpackArgs(args_15); };
		// Reified parameters
		IconVar X_r = new IconVar().local();
		IconVar lower_r = new IconVar().local();
		IconVar upper_r = new IconVar().local();
		// Locals
		IconVar pivot_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_17 = (Object... params_18) -> {
			if (params_18 ==  null) { params_18 = IIconAtom.getEmptyArray(); };
			X_r.set((params_18.length > 0) ? params_18[0] : null);
			lower_r.set((params_18.length > 1) ? params_18[1] : null);
			upper_r.set((params_18.length > 2) ? params_18[2] : null);
			// Reset locals
			pivot_r.set(null);
			return null;
		};
		// Method body
		body_16 = new IconSequence(new IconIf(new IconOperation(IconOperators.greaterThan).over(new IconOperation(IconOperators.minus).over(new IconSingleton(upper_r), new IconSingleton(lower_r)), new IconValueIterator(0)), new IconBlock( () -> {
			// Temporaries
			IconTmp x_6_r = new IconTmp();
			return new IconSequence(new IconAssign().over(new IconSingleton(pivot_r), new IconInvokeIterator(()-> ((VariadicFunction) quickpartition).apply(X_r.deref(), lower_r.deref(), upper_r.deref()))), new IconProduct(new IconIn(x_6_r, new IconOperation(IconOperators.minus).over(new IconSingleton(pivot_r), new IconValueIterator(1))), new IconInvokeIterator(()-> quicksort(X_r.deref(), lower_r.deref(), x_6_r.deref()))), new IconInvokeIterator(()-> quicksort(X_r.deref(), pivot_r.deref(), upper_r.deref())));
		}
 )), new IconReturn(new IconSingleton(X_r)), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_16.setCache(methodCache, "quicksort_m");
		body_16.setUnpackClosure(unpack_17).unpackArgs(args_15);
		return body_16;
	}
	@MMethod(name="quickpartition", methodName="quickpartition")
	@MParameter(name="X", reifiedName="X_r", type="")
	@MParameter(name="lower", reifiedName="lower_r", type="")
	@MParameter(name="upper", reifiedName="upper_r", type="")
	@MLocal(name="pivot", reifiedName="pivot_r", type="")
	@MLocal(name="mid", reifiedName="mid_r", type="")
	@MLocal(name="xlower", reifiedName="xlower_r", type="")
	@MLocal(name="xupper", reifiedName="xupper_r", type="")
	public IIconIterator quickpartition (Object... args_19) {
		// Reuse method body
		IconIterator body_20 = methodCache.getFree("quickpartition_m");
		if (body_20 != null) { return body_20.reset().unpackArgs(args_19); };
		// Reified parameters
		IconVar X_r = new IconVar().local();
		IconVar lower_r = new IconVar().local();
		IconVar upper_r = new IconVar().local();
		// Temporaries
		IconTmp x_7_r = new IconTmp();
		// Locals
		IconVar pivot_r = new IconVar().local();
		IconVar mid_r = new IconVar().local();
		IconVar xlower_r = new IconVar().local();
		IconVar xupper_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_21 = (Object... params_22) -> {
			if (params_22 ==  null) { params_22 = IIconAtom.getEmptyArray(); };
			X_r.set((params_22.length > 0) ? params_22[0] : null);
			lower_r.set((params_22.length > 1) ? params_22[1] : null);
			upper_r.set((params_22.length > 2) ? params_22[2] : null);
			// Reset locals
			pivot_r.set(null);
			mid_r.set(null);
			xlower_r.set(null);
			xupper_r.set(null);
			return null;
		};
		// Method body
		body_20 = new IconSequence(new IconAssign().over(new IconSingleton(xlower_r), new IconIndexIterator(X_r, lower_r)), new IconAssign().over(new IconSingleton(xupper_r), new IconIndexIterator(X_r, upper_r)), new IconAssign().over(new IconSingleton(mid_r), new IconProduct(new IconIn(x_7_r, new IconOperation(IconOperators.plus).over(new IconSingleton(lower_r), new IconOperation(IconOperators.questionMarkUnary).over((new IconOperation(IconOperators.minus).over(new IconSingleton(upper_r), new IconSingleton(lower_r)))))), new IconIndexIterator(X_r, x_7_r))), new IconIf((new IconOperation(IconOperators.lessThan).over(new IconSingleton(xupper_r), new IconSingleton(xlower_r))), new IconAssign().swap().over(new IconSingleton(xupper_r), new IconSingleton(xlower_r))), new IconIf((new IconOperation(IconOperators.lessThan).over(new IconSingleton(xupper_r), new IconSingleton(mid_r))), new IconAssign().swap().over(new IconSingleton(mid_r), new IconSingleton(xupper_r))), new IconIf((new IconOperation(IconOperators.lessThan).over(new IconSingleton(mid_r), new IconSingleton(xlower_r))), new IconAssign().swap().over(new IconSingleton(mid_r), new IconSingleton(xlower_r))), new IconAssign().over(new IconSingleton(pivot_r), new IconSingleton(mid_r)), new IconAssign().augment(IconOperators.minus).over(new IconSingleton(lower_r), new IconValueIterator(1)), new IconAssign().augment(IconOperators.plus).over(new IconSingleton(upper_r), new IconValueIterator(1)), new IconWhile(new IconOperation(IconOperators.lessThan).over(new IconSingleton(lower_r), new IconSingleton(upper_r)), new IconBlock( () -> {
			// Temporaries
			IconTmp x_8_r = new IconTmp();
			IconTmp x_9_r = new IconTmp();
			return new IconSequence(new IconWhile((new IconOperation(IconOperators.lessThan).over(new IconSingleton(pivot_r), new IconProduct(new IconIn(x_8_r, new IconAssign().augment(IconOperators.minus).over(new IconSingleton(upper_r), new IconValueIterator(1))), new IconIndexIterator(X_r, x_8_r))))), new IconWhile((new IconOperation(IconOperators.lessThan).over(new IconProduct(new IconIn(x_9_r, new IconAssign().augment(IconOperators.plus).over(new IconSingleton(lower_r), new IconValueIterator(1))), new IconIndexIterator(X_r, x_9_r)), new IconSingleton(pivot_r)))), new IconIf(new IconOperation(IconOperators.lessThan).over(new IconSingleton(lower_r), new IconSingleton(upper_r)), new IconAssign().swap().over(new IconIndexIterator(X_r, lower_r), new IconIndexIterator(X_r, upper_r))));
		}
 )), new IconReturn(new IconSingleton(lower_r)), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_20.setCache(methodCache, "quickpartition_m");
		body_20.setUnpackClosure(unpack_21).unpackArgs(args_19);
		return body_20;
	}
	// Static main method
	public static void main(String... args_23) {
		QuickSortFast c = new QuickSortFast(); VariadicFunction m = (VariadicFunction) c.main;
		IconCoExpression.activate(null, null, new IconCoExpression(
		 (Object... coexpr) -> {
			return ((IIconIterator) m.apply((Object[]) args_23)); },
		 () -> { return IconList.createArray(); } ));
	}
}
