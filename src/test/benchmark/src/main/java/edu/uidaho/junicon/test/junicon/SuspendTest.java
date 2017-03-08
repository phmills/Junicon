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
public class SuspendTest {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Method references
	@MMethodRef(name="rangeEvery", methodName="rangeEvery")
	public Object rangeEvery = (VariadicFunction) this::rangeEvery;
	@MMethodRef(name="range", methodName="range")
	public Object range = (VariadicFunction) this::range;
	@MMethodRef(name="main", methodName="main_m")
	public Object main = (VariadicFunction) this::main_m;
	// Constructor fields
	@MField(name="lower", reifiedName="lower_r", type="", isConstructorField=true)
	public Object lower;
	// Reified constructor fields
	private IconVar lower_r = new IconVar(()-> lower, (rhs)-> lower=rhs);
	// Constructors
	public SuspendTest() { ;}
	public SuspendTest(Object lower) {
		this.lower = lower;
	}
	// Static variadic constructor
	public static VariadicFunction SuspendTest = (Object... args_1) -> {
		if (args_1 ==  null) { args_1 = IIconAtom.getEmptyArray(); };
		return new SuspendTest((args_1.length > 0) ? args_1[0] : null);
	};
	// Methods
	@MMethod(name="rangeEvery", methodName="rangeEvery")
	@MParameter(name="upto", reifiedName="upto_r", type="")
	@MLocal(name="i", reifiedName="i_r", type="")
	@MLocal(name="j", reifiedName="j_r", type="")
	public IIconIterator rangeEvery (Object... args_5) {
		// Reuse method body
		IconIterator body = methodCache.getFree("rangeEvery_m");
		if (body != null) { return body.reset().unpackArgs(args_5); };
		// Reified parameters
		IconVar upto_r = new IconVar().local();
		// Temporaries
		IconTmp x_0_r = new IconTmp();
		// Locals
		IconVar i_r = new IconVar().local();
		IconVar j_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack = (Object... params) -> {
			if (params ==  null) { params = IIconAtom.getEmptyArray(); };
			upto_r.set((params.length > 0) ? params[0] : null);
			// Reset locals
			i_r.set(null);
			j_r.set(null);
			return null;
		};
		// Method body
		body = new IconSequence(new IconEvery((new IconAssign().over(new IconSingleton(i_r), new IconProduct(new IconIn(x_0_r, new IconInvokeIterator(()-> ((VariadicFunction) SuspendTest).apply())), new IconInvokeIterator(()-> ((VariadicFunction) IconField.getFieldValue(x_0_r, "range")).apply(lower, upto_r.deref()))))), new IconAssign().over(new IconSingleton(j_r), new IconSingleton(i_r))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body.setCache(methodCache, "rangeEvery_m");
		body.setUnpackClosure(unpack).unpackArgs(args_5);
		return body;
	}
	@MMethod(name="range", methodName="range")
	@MParameter(name="from", reifiedName="from_r", type="")
	@MParameter(name="bound", reifiedName="bound_r", type="")
	@MLocal(name="count", reifiedName="count_r", type="")
	public IIconIterator range (Object... args_6) {
		// Reuse method body
		IconIterator body_7 = methodCache.getFree("range_m");
		if (body_7 != null) { return body_7.reset().unpackArgs(args_6); };
		// Reified parameters
		IconVar from_r = new IconVar().local();
		IconVar bound_r = new IconVar().local();
		// Locals
		IconVar count_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_8 = (Object... params_9) -> {
			if (params_9 ==  null) { params_9 = IIconAtom.getEmptyArray(); };
			from_r.set((params_9.length > 0) ? params_9[0] : null);
			bound_r.set((params_9.length > 1) ? params_9[1] : null);
			// Reset locals
			count_r.set(null);
			return null;
		};
		// Method body
		body_7 = new IconSequence(new IconAssign().over(new IconSingleton(count_r), new IconSingleton(from_r)), new IconWhile((new IconOperation(IconOperators.lessThanOrEquals).over(new IconSingleton(count_r), new IconSingleton(bound_r))), new IconSequence(new IconSuspend(new IconSingleton(count_r)), new IconAssign().augment(IconOperators.plus).over(new IconSingleton(count_r), new IconValueIterator(1)))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_7.setCache(methodCache, "range_m");
		body_7.setUnpackClosure(unpack_8).unpackArgs(args_6);
		return body_7;
	}
	@MMethod(name="main", methodName="main_m")
	@MParameter(name="args", reifiedName="args_r", type="", isVararg=true)
	@MLocal(name="x", reifiedName="x_r", type="")
	private IIconIterator main_m (Object... args_10) {
		// Reuse method body
		IconIterator body_11 = methodCache.getFree("main_m");
		if (body_11 != null) { return body_11.reset().unpackArgs(args_10); };
		// Reified parameters
		IconVar args_r = new IconVar().local();
		// Locals
		IconVar x_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_12 = (Object... params_13) -> {
			if (params_13 ==  null) { params_13 = IIconAtom.getEmptyArray(); };
			args_r.set((params_13.length > 0) ? Arrays.asList(params_13).subList(0, params_13.length) : new ArrayList());
			// Reset locals
			x_r.set(null);
			return null;
		};
		// Method body
		body_11 = new IconSequence(new IconAssign().over(new IconSingleton(x_r), new IconOperation(IconOperators.plusUnary).over(new IconIndexIterator(args_r, IconValue.create(1)))), new IconAssign().over(new IconSingleton(lower_r), new IconValueIterator(1)), new IconInvokeIterator(()-> ((VariadicFunction) rangeEvery).apply(x_r.deref())), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_11.setCache(methodCache, "main_m");
		body_11.setUnpackClosure(unpack_12).unpackArgs(args_10);
		return body_11;
	}
	// Static main method
	public static void main(String... args_14) {
		SuspendTest c = new SuspendTest(); VariadicFunction m = (VariadicFunction) c.main;
		IconCoExpression.activate(null, null, new IconCoExpression(
		 (Object... coexpr) -> {
			return ((IIconIterator) m.apply((Object[]) args_14)); },
		 () -> { return IconList.createArray(); } ));
	}
}
