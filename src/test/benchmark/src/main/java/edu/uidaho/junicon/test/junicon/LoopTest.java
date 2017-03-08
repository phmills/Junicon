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
public class LoopTest {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Method references
	@MMethodRef(name="loop", methodName="loop")
	public Object loop = (VariadicFunction) this::loop;
	@MMethodRef(name="main", methodName="main_m")
	public Object main = (VariadicFunction) this::main_m;
	// Constructor fields
	@MField(name="lower", reifiedName="lower_r", type="", isConstructorField=true)
	public Object lower;
	// Reified constructor fields
	private IconVar lower_r = new IconVar(()-> lower, (rhs)-> lower=rhs);
	// Constructors
	public LoopTest() { ;}
	public LoopTest(Object lower) {
		this.lower = lower;
	}
	// Static variadic constructor
	public static VariadicFunction LoopTest = (Object... args_1) -> {
		if (args_1 ==  null) { args_1 = IIconAtom.getEmptyArray(); };
		return new LoopTest((args_1.length > 0) ? args_1[0] : null);
	};
	// Methods
	@MMethod(name="loop", methodName="loop")
	@MParameter(name="from", reifiedName="from_r", type="")
	@MParameter(name="bound", reifiedName="bound_r", type="")
	@MLocal(name="count", reifiedName="count_r", type="")
	public IIconIterator loop (Object... args_4) {
		// Reuse method body
		IconIterator body = methodCache.getFree("loop_m");
		if (body != null) { return body.reset().unpackArgs(args_4); };
		// Reified parameters
		IconVar from_r = new IconVar().local();
		IconVar bound_r = new IconVar().local();
		// Locals
		IconVar count_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack = (Object... params) -> {
			if (params ==  null) { params = IIconAtom.getEmptyArray(); };
			from_r.set((params.length > 0) ? params[0] : null);
			bound_r.set((params.length > 1) ? params[1] : null);
			// Reset locals
			count_r.set(null);
			return null;
		};
		// Method body
		body = new IconSequence(new IconEvery((new IconToIterator(from_r, bound_r)), new IconSequence(new IconAssign().over(new IconSingleton(count_r), new IconSingleton(from_r)), new IconWhile((new IconOperation(IconOperators.lessThanOrEquals).over(new IconSingleton(count_r), new IconSingleton(bound_r))), new IconAssign().augment(IconOperators.plus).over(new IconSingleton(count_r), new IconValueIterator(1))))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body.setCache(methodCache, "loop_m");
		body.setUnpackClosure(unpack).unpackArgs(args_4);
		return body;
	}
	@MMethod(name="main", methodName="main_m")
	@MParameter(name="args", reifiedName="args_r", type="", isVararg=true)
	@MLocal(name="x", reifiedName="x_r", type="")
	private IIconIterator main_m (Object... args_5) {
		// Reuse method body
		IconIterator body_6 = methodCache.getFree("main_m");
		if (body_6 != null) { return body_6.reset().unpackArgs(args_5); };
		// Reified parameters
		IconVar args_r = new IconVar().local();
		// Locals
		IconVar x_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_7 = (Object... params_8) -> {
			if (params_8 ==  null) { params_8 = IIconAtom.getEmptyArray(); };
			args_r.set((params_8.length > 0) ? Arrays.asList(params_8).subList(0, params_8.length) : new ArrayList());
			// Reset locals
			x_r.set(null);
			return null;
		};
		// Method body
		body_6 = new IconSequence(new IconAssign().over(new IconSingleton(x_r), new IconOperation(IconOperators.plusUnary).over(new IconIndexIterator(args_r, IconValue.create(1)))), new IconAssign().over(new IconSingleton(lower_r), new IconValueIterator(1)), new IconInvokeIterator(()-> ((VariadicFunction) loop).apply(lower, x_r.deref())), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_6.setCache(methodCache, "main_m");
		body_6.setUnpackClosure(unpack_7).unpackArgs(args_5);
		return body_6;
	}
	// Static main method
	public static void main(String... args_9) {
		LoopTest c = new LoopTest(); VariadicFunction m = (VariadicFunction) c.main;
		IconCoExpression.activate(null, null, new IconCoExpression(
		 (Object... coexpr) -> {
			return ((IIconIterator) m.apply((Object[]) args_9)); },
		 () -> { return IconList.createArray(); } ));
	}
}
