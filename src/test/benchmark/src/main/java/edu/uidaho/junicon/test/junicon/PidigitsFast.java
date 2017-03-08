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
public class PidigitsFast {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Method references
	@MMethodRef(name="main", methodName="main_m")
	public Object main = (VariadicFunction) this::main_m;
	@MMethodRef(name="pidigits", methodName="pidigits")
	public Object pidigits = (VariadicFunction) this::pidigits;
	// Constructors
	public PidigitsFast() {
		;
	}
	// Static variadic constructor
	public static VariadicFunction PidigitsFast = (Object... args_1) -> {
		return new PidigitsFast();
	};
	// Methods
	@MMethod(name="main", methodName="main_m")
	@MParameter(name="args", reifiedName="args_r", type="", isVararg=true)
	private IIconIterator main_m (Object... args_4) {
		// Reuse method body
		IconIterator body = methodCache.getFree("main_m");
		if (body != null) { return body.reset().unpackArgs(args_4); };
		// Reified parameters
		IconVar args_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack = (Object... params) -> {
			if (params ==  null) { params = IIconAtom.getEmptyArray(); };
			args_r.set((params.length > 0) ? Arrays.asList(params).subList(0, params.length) : new ArrayList());
			return null;
		};
		// Method body
		body = new IconSequence(new IconInvokeIterator(()-> ((VariadicFunction) pidigits).apply(args_r.deref())), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body.setCache(methodCache, "main_m");
		body.setUnpackClosure(unpack).unpackArgs(args_4);
		return body;
	}
	@MMethod(name="pidigits", methodName="pidigits")
	@MParameter(name="args", reifiedName="args_r", type="")
	@MLocal(name="N", reifiedName="N_r", type="")
	@MLocal(name="i", reifiedName="i_r", type="")
	@MLocal(name="k", reifiedName="k_r", type="")
	@MLocal(name="ns", reifiedName="ns_r", type="")
	@MLocal(name="a", reifiedName="a_r", type="")
	@MLocal(name="t", reifiedName="t_r", type="")
	@MLocal(name="u", reifiedName="u_r", type="")
	@MLocal(name="k1", reifiedName="k1_r", type="")
	@MLocal(name="n", reifiedName="n_r", type="")
	@MLocal(name="d", reifiedName="d_r", type="")
	@MLocal(name="cse", reifiedName="cse_r", type="")
	public IIconIterator pidigits (Object... args_5) {
		// Reuse method body
		IconIterator body_6 = methodCache.getFree("pidigits_m");
		if (body_6 != null) { return body_6.reset().unpackArgs(args_5); };
		// Reified parameters
		IconVar args_r = new IconVar().local();
		// Locals
		IconVar N_r = new IconVar().local();
		IconVar i_r = new IconVar().local();
		IconVar k_r = new IconVar().local();
		IconVar ns_r = new IconVar().local();
		IconVar a_r = new IconVar().local();
		IconVar t_r = new IconVar().local();
		IconVar u_r = new IconVar().local();
		IconVar k1_r = new IconVar().local();
		IconVar n_r = new IconVar().local();
		IconVar d_r = new IconVar().local();
		IconVar cse_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_7 = (Object... params_8) -> {
			if (params_8 ==  null) { params_8 = IIconAtom.getEmptyArray(); };
			args_r.set((params_8.length > 0) ? params_8[0] : null);
			// Reset locals
			N_r.set(null);
			i_r.set(null);
			k_r.set(null);
			ns_r.set(null);
			a_r.set(null);
			t_r.set(null);
			u_r.set(null);
			k1_r.set(null);
			n_r.set(null);
			d_r.set(null);
			cse_r.set(null);
			return null;
		};
		// Method body
		body_6 = new IconSequence(new IconAssign().over(new IconSingleton(N_r), new IconOperation(IconOperators.plusUnary).over(new IconIndexIterator(args_r, IconValue.create(1)))), new IconAssign().over(new IconSingleton(i_r), new IconAssign().over(new IconSingleton(k_r), new IconAssign().over(new IconSingleton(ns_r), new IconAssign().over(new IconSingleton(a_r), new IconAssign().over(new IconSingleton(t_r), new IconAssign().over(new IconSingleton(u_r), new IconValueIterator(0))))))), new IconAssign().over(new IconSingleton(k1_r), new IconAssign().over(new IconSingleton(n_r), new IconAssign().over(new IconSingleton(d_r), new IconValueIterator(1)))), new IconRepeat(new IconSequence(new IconAssign().augment(IconOperators.plus).over(new IconSingleton(k_r), new IconValueIterator(1)), new IconAssign().over(new IconSingleton(t_r), new IconInvokeIterator(()-> ((VariadicFunction) ishift).apply(n_r.deref(), IconNumber.create(1)))), new IconAssign().augment(IconOperators.times).over(new IconSingleton(n_r), new IconSingleton(k_r)), new IconAssign().augment(IconOperators.plus).over(new IconSingleton(a_r), new IconSingleton(t_r)), new IconAssign().augment(IconOperators.plus).over(new IconSingleton(k1_r), new IconValueIterator(2)), new IconAssign().augment(IconOperators.times).over(new IconSingleton(a_r), new IconSingleton(k1_r)), new IconAssign().augment(IconOperators.times).over(new IconSingleton(d_r), new IconSingleton(k1_r)), new IconIf(new IconOperation(IconOperators.greaterThanOrEquals).over(new IconSingleton(a_r), new IconSingleton(n_r)), new IconSequence(new IconAssign().over(new IconSingleton(cse_r), new IconOperation(IconOperators.plus).over(new IconOperation(IconOperators.times).over(new IconSingleton(n_r), new IconValueIterator(3)), new IconSingleton(a_r))), new IconAssign().over(new IconSingleton(t_r), new IconOperation(IconOperators.division).over(new IconSingleton(cse_r), new IconSingleton(d_r))), new IconAssign().over(new IconSingleton(u_r), new IconOperation(IconOperators.plus).over(new IconOperation(IconOperators.remainder).over(new IconSingleton(cse_r), new IconSingleton(d_r)), new IconSingleton(n_r))), new IconIf(new IconOperation(IconOperators.greaterThan).over(new IconSingleton(d_r), new IconSingleton(u_r)), new IconSequence(new IconAssign().over(new IconSingleton(ns_r), new IconOperation(IconOperators.plus).over(new IconOperation(IconOperators.times).over(new IconSingleton(ns_r), new IconValueIterator(10)), new IconSingleton(t_r))), new IconAssign().augment(IconOperators.plus).over(new IconSingleton(i_r), new IconValueIterator(1)), new IconIf(new IconOperation(IconOperators.sameNumberAs).over(new IconOperation(IconOperators.remainder).over(new IconSingleton(i_r), new IconValueIterator(10)), new IconValueIterator(0)), new IconAssign().over(new IconSingleton(ns_r), new IconValueIterator(0))), new IconIf(new IconOperation(IconOperators.greaterThanOrEquals).over(new IconSingleton(i_r), new IconSingleton(N_r)), new IconBreak(new IconNullIterator())), new IconAssign().over(new IconSingleton(a_r), new IconOperation(IconOperators.times).over((new IconOperation(IconOperators.minus).over(new IconSingleton(a_r), new IconOperation(IconOperators.times).over(new IconSingleton(d_r), new IconSingleton(t_r)))), new IconValueIterator(10))), new IconAssign().augment(IconOperators.times).over(new IconSingleton(n_r), new IconValueIterator(10)))))))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_6.setCache(methodCache, "pidigits_m");
		body_6.setUnpackClosure(unpack_7).unpackArgs(args_5);
		return body_6;
	}
	// Static main method
	public static void main(String... args_9) {
		PidigitsFast c = new PidigitsFast(); VariadicFunction m = (VariadicFunction) c.main;
		IconCoExpression.activate(null, null, new IconCoExpression(
		 (Object... coexpr) -> {
			return ((IIconIterator) m.apply((Object[]) args_9)); },
		 () -> { return IconList.createArray(); } ));
	}
}
