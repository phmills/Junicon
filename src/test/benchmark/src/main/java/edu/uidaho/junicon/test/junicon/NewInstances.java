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
class C {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Constructors
	public C() {
		;
	}
	// Static variadic constructor
	public static VariadicFunction C = (Object... args_1) -> {
		return new C();
	};
}
class D {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Constructor fields
	@MField(name="a", reifiedName="a_r", type="", isConstructorField=true)
	public Object a;
	@MField(name="b", reifiedName="b_r", type="", isConstructorField=true)
	public Object b;
	@MField(name="c", reifiedName="c_r", type="", isConstructorField=true)
	public Object c;
	// Reified constructor fields
	private IconVar a_r = new IconVar(()-> a, (rhs)-> a=rhs);
	private IconVar b_r = new IconVar(()-> b, (rhs)-> b=rhs);
	private IconVar c_r = new IconVar(()-> c, (rhs)-> c=rhs);
	// Constructors
	public D() { ;}
	public D(Object a, Object b, Object c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}
	// Static variadic constructor
	public static VariadicFunction D = (Object... args_2) -> {
		if (args_2 ==  null) { args_2 = IIconAtom.getEmptyArray(); };
		return new D((args_2.length > 0) ? args_2[0] : null, (args_2.length > 1) ? args_2[1] : null, (args_2.length > 2) ? args_2[2] : null);
	};
}
class E {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Constructor fields
	@MField(name="a", reifiedName="a_r", type="", isConstructorField=true)
	public Object a;
	@MField(name="b", reifiedName="b_r", type="", isConstructorField=true)
	public Object b;
	@MField(name="c", reifiedName="c_r", type="", isConstructorField=true)
	public Object c;
	// Reified constructor fields
	private IconVar a_r = new IconVar(()-> a, (rhs)-> a=rhs);
	private IconVar b_r = new IconVar(()-> b, (rhs)-> b=rhs);
	private IconVar c_r = new IconVar(()-> c, (rhs)-> c=rhs);
	// Constructors
	public E() { ;}
	public E(Object a, Object b, Object c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}
	// Static variadic constructor
	public static VariadicFunction E = (Object... args_3) -> {
		if (args_3 ==  null) { args_3 = IIconAtom.getEmptyArray(); };
		return new E((args_3.length > 0) ? args_3[0] : null, (args_3.length > 1) ? args_3[1] : null, (args_3.length > 2) ? args_3[2] : null);
	};
	// Locals
		@MField(name="d", reifiedName="d_r", type="", isConstructorField=false)
public Object d = (new IconSingleton(a_r)).nextOrNull();
	private IconVar d_r = new IconVar(()-> d, (rhs)-> d=rhs);
		@MField(name="e", reifiedName="e_r", type="", isConstructorField=false)
public Object e = (new IconSingleton(b_r)).nextOrNull();
	private IconVar e_r = new IconVar(()-> e, (rhs)-> e=rhs);
		@MField(name="f", reifiedName="f_r", type="", isConstructorField=false)
public Object f = (new IconSingleton(c_r)).nextOrNull();
	private IconVar f_r = new IconVar(()-> f, (rhs)-> f=rhs);
}
class CreateNewInstances {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Method references
	@MMethodRef(name="test", methodName="test")
	public Object test = (VariadicFunction) this::test;
	@MMethodRef(name="calibrate", methodName="calibrate")
	public Object calibrate = (VariadicFunction) this::calibrate;
	// Constructors
	public CreateNewInstances() {
		;
	}
	// Static variadic constructor
	public static VariadicFunction CreateNewInstances = (Object... args_4) -> {
		return new CreateNewInstances();
	};
	// Locals
		@MField(name="version", reifiedName="version_r", type="", isConstructorField=false)
public Object version = (new IconValueIterator(2.0)).nextOrNull();
	private IconVar version_r = new IconVar(()-> version, (rhs)-> version=rhs);
		@MField(name="operations", reifiedName="operations_r", type="", isConstructorField=false)
public Object operations = (new IconOperation(IconOperators.plus).over(new IconOperation(IconOperators.plus).over(new IconValueIterator(3), new IconValueIterator(7)), new IconValueIterator(4))).nextOrNull();
	private IconVar operations_r = new IconVar(()-> operations, (rhs)-> operations=rhs);
	// Methods
	@MMethod(name="test", methodName="test")
	@MParameter(name="rounds", reifiedName="rounds_r", type="")
	@MLocal(name="i", reifiedName="i_r", type="")
	@MLocal(name="o", reifiedName="o_r", type="")
	@MLocal(name="o1", reifiedName="o1_r", type="")
	@MLocal(name="o2", reifiedName="o2_r", type="")
	@MLocal(name="p", reifiedName="p_r", type="")
	@MLocal(name="p1", reifiedName="p1_r", type="")
	@MLocal(name="p2", reifiedName="p2_r", type="")
	@MLocal(name="p3", reifiedName="p3_r", type="")
	@MLocal(name="p4", reifiedName="p4_r", type="")
	@MLocal(name="p5", reifiedName="p5_r", type="")
	@MLocal(name="p6", reifiedName="p6_r", type="")
	@MLocal(name="q", reifiedName="q_r", type="")
	@MLocal(name="q1", reifiedName="q1_r", type="")
	@MLocal(name="q2", reifiedName="q2_r", type="")
	@MLocal(name="q3", reifiedName="q3_r", type="")
	public IIconIterator test (Object... args_7) {
		// Reuse method body
		IconIterator body = methodCache.getFree("test_m");
		if (body != null) { return body.reset().unpackArgs(args_7); };
		// Reified parameters
		IconVar rounds_r = new IconVar().local();
		// Locals
		IconVar i_r = new IconVar().local();
		IconVar o_r = new IconVar().local();
		IconVar o1_r = new IconVar().local();
		IconVar o2_r = new IconVar().local();
		IconVar p_r = new IconVar().local();
		IconVar p1_r = new IconVar().local();
		IconVar p2_r = new IconVar().local();
		IconVar p3_r = new IconVar().local();
		IconVar p4_r = new IconVar().local();
		IconVar p5_r = new IconVar().local();
		IconVar p6_r = new IconVar().local();
		IconVar q_r = new IconVar().local();
		IconVar q1_r = new IconVar().local();
		IconVar q2_r = new IconVar().local();
		IconVar q3_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack = (Object... params) -> {
			if (params ==  null) { params = IIconAtom.getEmptyArray(); };
			rounds_r.set((params.length > 0) ? params[0] : null);
			// Reset locals
			i_r.set(null);
			o_r.set(null);
			o1_r.set(null);
			o2_r.set(null);
			p_r.set(null);
			p1_r.set(null);
			p2_r.set(null);
			p3_r.set(null);
			p4_r.set(null);
			p5_r.set(null);
			p6_r.set(null);
			q_r.set(null);
			q1_r.set(null);
			q2_r.set(null);
			q3_r.set(null);
			return null;
		};
		// Method body
		body = new IconSequence(new IconEvery((new IconAssign().over(new IconSingleton(i_r), new IconToIterator(IconValue.create(1), rounds_r))), new IconSequence(new IconAssign().over(new IconSingleton(o_r), new IconInvokeIterator(()-> ((VariadicFunction) C.C).apply())), new IconAssign().over(new IconSingleton(o1_r), new IconInvokeIterator(()-> ((VariadicFunction) C.C).apply())), new IconAssign().over(new IconSingleton(o2_r), new IconInvokeIterator(()-> ((VariadicFunction) C.C).apply())), new IconAssign().over(new IconSingleton(p_r), new IconInvokeIterator(()-> ((VariadicFunction) D.D).apply(i_r.deref(), i_r.deref(), IconNumber.create(3)))), new IconAssign().over(new IconSingleton(p1_r), new IconInvokeIterator(()-> ((VariadicFunction) D.D).apply(i_r.deref(), i_r.deref(), IconNumber.create(3)))), new IconAssign().over(new IconSingleton(p2_r), new IconInvokeIterator(()-> ((VariadicFunction) D.D).apply(i_r.deref(), IconNumber.create(3), IconNumber.create(3)))), new IconAssign().over(new IconSingleton(p3_r), new IconInvokeIterator(()-> ((VariadicFunction) D.D).apply(IconNumber.create(3), i_r.deref(), IconNumber.create(3)))), new IconAssign().over(new IconSingleton(p4_r), new IconInvokeIterator(()-> ((VariadicFunction) D.D).apply(i_r.deref(), i_r.deref(), i_r.deref()))), new IconAssign().over(new IconSingleton(p5_r), new IconInvokeIterator(()-> ((VariadicFunction) D.D).apply(IconNumber.create(3), i_r.deref(), IconNumber.create(3)))), new IconAssign().over(new IconSingleton(p6_r), new IconInvokeIterator(()-> ((VariadicFunction) D.D).apply(i_r.deref(), i_r.deref(), i_r.deref()))), new IconAssign().over(new IconSingleton(q_r), new IconInvokeIterator(()-> ((VariadicFunction) E.E).apply(i_r.deref(), i_r.deref(), IconNumber.create(3)))), new IconAssign().over(new IconSingleton(q1_r), new IconInvokeIterator(()-> ((VariadicFunction) E.E).apply(i_r.deref(), i_r.deref(), IconNumber.create(3)))), new IconAssign().over(new IconSingleton(q2_r), new IconInvokeIterator(()-> ((VariadicFunction) E.E).apply(i_r.deref(), i_r.deref(), IconNumber.create(3)))), new IconAssign().over(new IconSingleton(q3_r), new IconInvokeIterator(()-> ((VariadicFunction) E.E).apply(i_r.deref(), i_r.deref(), IconNumber.create(4)))), new IconNullIterator())), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body.setCache(methodCache, "test_m");
		body.setUnpackClosure(unpack).unpackArgs(args_7);
		return body;
	}
	@MMethod(name="calibrate", methodName="calibrate")
	@MParameter(name="rounds", reifiedName="rounds_r", type="")
	@MLocal(name="i", reifiedName="i_r", type="")
	public IIconIterator calibrate (Object... args_8) {
		// Reuse method body
		IconIterator body_9 = methodCache.getFree("calibrate_m");
		if (body_9 != null) { return body_9.reset().unpackArgs(args_8); };
		// Reified parameters
		IconVar rounds_r = new IconVar().local();
		// Locals
		IconVar i_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_10 = (Object... params_11) -> {
			if (params_11 ==  null) { params_11 = IIconAtom.getEmptyArray(); };
			rounds_r.set((params_11.length > 0) ? params_11[0] : null);
			// Reset locals
			i_r.set(null);
			return null;
		};
		// Method body
		body_9 = new IconSequence(new IconEvery((new IconAssign().over(new IconSingleton(i_r), new IconToIterator(IconValue.create(1), rounds_r))), new IconSequence(new IconNullIterator())), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_9.setCache(methodCache, "calibrate_m");
		body_9.setUnpackClosure(unpack_10).unpackArgs(args_8);
		return body_9;
	}
}
public class NewInstances {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Method references
	@MMethodRef(name="main", methodName="main_m")
	public Object main = (VariadicFunction) this::main_m;
	// Constructors
	public NewInstances() {
		;
	}
	// Static variadic constructor
	public static VariadicFunction NewInstances = (Object... args_12) -> {
		return new NewInstances();
	};
	// Methods
	@MMethod(name="main", methodName="main_m")
	@MParameter(name="args", reifiedName="args_r", type="", isVararg=true)
	@MLocal(name="t", reifiedName="t_r", type="")
	@MLocal(name="rounds", reifiedName="rounds_r", type="")
	private IIconIterator main_m (Object... args_14) {
		// Reuse method body
		IconIterator body_15 = methodCache.getFree("main_m");
		if (body_15 != null) { return body_15.reset().unpackArgs(args_14); };
		// Reified parameters
		IconVar args_r = new IconVar().local();
		// Locals
		IconVar t_r = new IconVar().local();
		IconVar rounds_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_16 = (Object... params_17) -> {
			if (params_17 ==  null) { params_17 = IIconAtom.getEmptyArray(); };
			args_r.set((params_17.length > 0) ? Arrays.asList(params_17).subList(0, params_17.length) : new ArrayList());
			// Reset locals
			t_r.set(null);
			return null;
		};
		// Method body
		body_15 = new IconSequence(new IconAssign().over(new IconSingleton(rounds_r), new IconIndexIterator(args_r, IconValue.create(1))), new IconInvokeIterator(()-> ((VariadicFunction) write).apply("Creating ", rounds_r.deref(), " newinstances")), new IconAssign().over(new IconSingleton(t_r), new IconInvokeIterator(()-> ((VariadicFunction) CreateNewInstances.CreateNewInstances).apply())), new IconInvokeIterator(()-> ((VariadicFunction) IconField.getFieldValue(t_r, "calibrate")).apply(rounds_r.deref())), new IconInvokeIterator(()-> ((VariadicFunction) IconField.getFieldValue(t_r, "test")).apply(rounds_r.deref())), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_15.setCache(methodCache, "main_m");
		body_15.setUnpackClosure(unpack_16).unpackArgs(args_14);
		return body_15;
	}
	// Static main method
	public static void main(String... args_18) {
		NewInstances c = new NewInstances(); VariadicFunction m = (VariadicFunction) c.main;
		IconCoExpression.activate(null, null, new IconCoExpression(
		 (Object... coexpr) -> {
			return ((IIconIterator) m.apply((Object[]) args_18)); },
		 () -> { return IconList.createArray(); } ));
	}
}
