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
public class Mandelbrot {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Method references
	@MMethodRef(name="do_y", methodName="do_y")
	public Object do_y = (VariadicFunction) this::do_y;
	@MMethodRef(name="run_mandelbrot_sequential", methodName="run_mandelbrot_sequential")
	public Object run_mandelbrot_sequential = (VariadicFunction) this::run_mandelbrot_sequential;
	@MMethodRef(name="run_mandelbrot", methodName="run_mandelbrot")
	public Object run_mandelbrot = (VariadicFunction) this::run_mandelbrot;
	@MMethodRef(name="main", methodName="main_m")
	public Object main = (VariadicFunction) this::main_m;
	// Constructors
	public Mandelbrot() {
		;
	}
	// Static variadic constructor
	public static VariadicFunction Mandelbrot = (Object... args) -> {
		return new Mandelbrot();
	};
	// Locals
	@MField(name="w", reifiedName="w_r", type="", isConstructorField=false)
	public Object w;
	private IconVar w_r = new IconVar(()-> w, (rhs)-> w=rhs);
	@MField(name="h", reifiedName="h_r", type="", isConstructorField=false)
	public Object h;
	private IconVar h_r = new IconVar(()-> h, (rhs)-> h=rhs);
	@MField(name="wr", reifiedName="wr_r", type="", isConstructorField=false)
	public Object wr;
	private IconVar wr_r = new IconVar(()-> wr, (rhs)-> wr=rhs);
	@MField(name="hr", reifiedName="hr_r", type="", isConstructorField=false)
	public Object hr;
	private IconVar hr_r = new IconVar(()-> hr, (rhs)-> hr=rhs);
	// Methods
	@MMethod(name="do_y", methodName="do_y")
	@MParameter(name="y", reifiedName="y_r", type="")
	@MLocal(name="bit_num", reifiedName="bit_num_r", type="")
	@MLocal(name="byte_acc", reifiedName="byte_acc_r", type="")
	@MLocal(name="x", reifiedName="x_r", type="")
	@MLocal(name="Zr", reifiedName="Zr_r", type="")
	@MLocal(name="Zi", reifiedName="Zi_r", type="")
	@MLocal(name="Cr", reifiedName="Cr_r", type="")
	@MLocal(name="Ci", reifiedName="Ci_r", type="")
	@MLocal(name="Tr", reifiedName="Tr_r", type="")
	@MLocal(name="Ti", reifiedName="Ti_r", type="")
	@MLocal(name="i", reifiedName="i_r", type="")
	@MLocal(name="rv", reifiedName="rv_r", type="")
	@MLocal(name="iter", reifiedName="iter_r", type="")
	public IIconIterator do_y (Object... args) {
		// Reuse method body
		IconIterator body = methodCache.getFree("do_y_m");
		if (body != null) { return body.reset().unpackArgs(args); };
		// Reified parameters
		IconVar y_r = new IconVar().local();
		// Temporaries
		IconTmp x_0_r = new IconTmp();
		// Locals
		IconVar bit_num_r = new IconVar().local();
		IconVar byte_acc_r = new IconVar().local();
		IconVar x_r = new IconVar().local();
		IconVar Zr_r = new IconVar().local();
		IconVar Zi_r = new IconVar().local();
		IconVar Cr_r = new IconVar().local();
		IconVar Ci_r = new IconVar().local();
		IconVar Tr_r = new IconVar().local();
		IconVar Ti_r = new IconVar().local();
		IconVar i_r = new IconVar().local();
		IconVar rv_r = new IconVar().local();
		IconVar iter_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack = (Object... params) -> {
			if (params ==  null) { params = IIconAtom.getEmptyArray(); };
			y_r.set((params.length > 0) ? params[0] : null);
			// Reset locals
			bit_num_r.set(null);
			byte_acc_r.set(null);
			x_r.set(null);
			Zr_r.set(null);
			Zi_r.set(null);
			Cr_r.set(null);
			Ci_r.set(null);
			Tr_r.set(null);
			Ti_r.set(null);
			i_r.set(null);
			rv_r.set(null);
			iter_r.set(null);
			return null;
		};
		// Method body
		body = new IconSequence(new IconAssign().over(new IconSingleton(bit_num_r), new IconAssign().over(new IconSingleton(byte_acc_r), new IconValueIterator(0))), new IconAssign().over(new IconSingleton(rv_r), new IconValueIterator("")), new IconAssign().over(new IconSingleton(iter_r), new IconValueIterator(50)), new IconAssign().over(new IconSingleton(Ci_r), (new IconOperation(IconOperators.minus).over(new IconOperation(IconOperators.division).over(new IconOperation(IconOperators.times).over(new IconValueIterator(2.0), new IconSingleton(y_r)), new IconSingleton(hr_r)), new IconValueIterator(1.0)))), new IconEvery(new IconAssign().over(new IconSingleton(x_r), new IconProduct(new IconIn(x_0_r, new IconOperation(IconOperators.minus).over(new IconSingleton(w_r), new IconValueIterator(1))), new IconToIterator(IconValue.create(0), x_0_r))), new IconBlock( () -> {
			// Temporaries
			IconTmp x_1_r = new IconTmp();
			return new IconSequence(new IconAssign().over(new IconSingleton(Zr_r), new IconAssign().over(new IconSingleton(Zi_r), new IconAssign().over(new IconSingleton(Tr_r), new IconAssign().over(new IconSingleton(Ti_r), new IconValueIterator(0.0))))), new IconAssign().over(new IconSingleton(Cr_r), (new IconOperation(IconOperators.minus).over(new IconOperation(IconOperators.division).over(new IconOperation(IconOperators.times).over(new IconValueIterator(2.0), new IconSingleton(x_r)), new IconSingleton(wr_r)), new IconValueIterator(1.5)))), new IconEvery(new IconAssign().over(new IconSingleton(i_r), new IconProduct(new IconIn(x_1_r, new IconOperation(IconOperators.minus).over(new IconSingleton(iter_r), new IconValueIterator(1))), new IconToIterator(IconValue.create(0), x_1_r))), new IconSequence(new IconIf(new IconOperation(IconOperators.greaterThan).over(new IconOperation(IconOperators.plus).over(new IconSingleton(Tr_r), new IconSingleton(Ti_r)), new IconValueIterator(4.0)), new IconBreak(new IconNullIterator())), new IconAssign().over(new IconSingleton(Zi_r), new IconOperation(IconOperators.plus).over(new IconOperation(IconOperators.times).over(new IconOperation(IconOperators.times).over(new IconValueIterator(2.0), new IconSingleton(Zr_r)), new IconSingleton(Zi_r)), new IconSingleton(Ci_r))), new IconAssign().over(new IconSingleton(Zr_r), new IconOperation(IconOperators.plus).over(new IconOperation(IconOperators.minus).over(new IconSingleton(Tr_r), new IconSingleton(Ti_r)), new IconSingleton(Cr_r))), new IconAssign().over(new IconSingleton(Tr_r), new IconOperation(IconOperators.times).over(new IconSingleton(Zr_r), new IconSingleton(Zr_r))), new IconAssign().over(new IconSingleton(Ti_r), new IconOperation(IconOperators.times).over(new IconSingleton(Zi_r), new IconSingleton(Zi_r))))), new IconAssign().over(new IconSingleton(byte_acc_r), new IconInvokeIterator(()-> ((VariadicFunction) ishift).apply(byte_acc_r.deref(), IconNumber.create(1)))), new IconIf(new IconOperation(IconOperators.lessThanOrEquals).over(new IconOperation(IconOperators.plus).over(new IconSingleton(Tr_r), new IconSingleton(Ti_r)), new IconValueIterator(4.0)), new IconAssign().over(new IconSingleton(byte_acc_r), new IconInvokeIterator(()-> ((VariadicFunction) ior).apply(byte_acc_r.deref(), IconNumber.create(1))))), new IconAssign().augment(IconOperators.plus).over(new IconSingleton(bit_num_r), new IconValueIterator(1)), new IconIf(new IconOperation(IconOperators.sameNumberAs).over(new IconSingleton(bit_num_r), new IconValueIterator(8)), new IconSequence(new IconAssign().augment(IconOperators.stringConcat).over(new IconSingleton(rv_r), new IconInvokeIterator(()-> ((VariadicFunction) charUnicon).apply(byte_acc_r.deref()))), new IconAssign().over(new IconSingleton(byte_acc_r), new IconAssign().over(new IconSingleton(bit_num_r), new IconValueIterator(0))))));
		}
 )), new IconIf(new IconOperation(IconOperators.notSameNumberAs).over(new IconSingleton(bit_num_r), new IconValueIterator(0)), new IconBlock( () -> {
			// Temporaries
			IconTmp x_3_r = new IconTmp();
			IconTmp x_2_r = new IconTmp();
			return new IconSequence(new IconAssign().over(new IconSingleton(byte_acc_r), new IconProduct(new IconIn(x_3_r, new IconProduct(new IconIn(x_2_r, new IconOperation(IconOperators.minus).over(new IconValueIterator(8), new IconOperation(IconOperators.remainder).over(new IconSingleton(w_r), new IconValueIterator(8)))), new IconInvokeIterator(()-> ((VariadicFunction) abs).apply(x_2_r.deref())))), new IconInvokeIterator(()-> ((VariadicFunction) ishift).apply(byte_acc_r.deref(), x_3_r.deref())))), new IconAssign().augment(IconOperators.stringConcat).over(new IconSingleton(rv_r), new IconInvokeIterator(()-> ((VariadicFunction) charUnicon).apply(byte_acc_r.deref()))), new IconAssign().over(new IconSingleton(byte_acc_r), new IconAssign().over(new IconSingleton(bit_num_r), new IconValueIterator(0))));
		}
 )), new IconReturn(new IconSingleton(rv_r)), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body.setCache(methodCache, "do_y_m");
		body.setUnpackClosure(unpack).unpackArgs(args);
		return body;
	}
	@MMethod(name="run_mandelbrot_sequential", methodName="run_mandelbrot_sequential")
	@MParameter(name="argv", reifiedName="argv_r", type="")
	@MLocal(name="wL", reifiedName="wL_r", type="")
	@MLocal(name="rL", reifiedName="rL_r", type="")
	@MLocal(name="i", reifiedName="i_r", type="")
	public IIconIterator run_mandelbrot_sequential (Object... args_9) {
		// Reuse method body
		IconIterator body_10 = methodCache.getFree("run_mandelbrot_sequential_m");
		if (body_10 != null) { return body_10.reset().unpackArgs(args_9); };
		// Reified parameters
		IconVar argv_r = new IconVar().local();
		// Temporaries
		IconTmp x_4_r = new IconTmp();
		IconTmp x_5_r = new IconTmp();
		IconTmp x_6_r = new IconTmp();
		IconTmp x_7_r = new IconTmp();
		// Locals
		IconVar wL_r = new IconVar().local();
		IconVar rL_r = new IconVar().local();
		IconVar i_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_11 = (Object... params_12) -> {
			if (params_12 ==  null) { params_12 = IIconAtom.getEmptyArray(); };
			argv_r.set((params_12.length > 0) ? params_12[0] : null);
			// Reset locals
			wL_r.set(null);
			rL_r.set(null);
			i_r.set(null);
			return null;
		};
		// Method body
		body_10 = new IconSequence(new IconAssign().over(new IconSingleton(wL_r), new IconInvokeIterator(()-> ((VariadicFunction) list).apply())), new IconAssign().over(new IconSingleton(rL_r), new IconInvokeIterator(()-> ((VariadicFunction) list).apply())), new IconAssign().over(new IconSingleton(w_r), new IconAssign().over(new IconSingleton(h_r), new IconProduct(new IconIn(x_4_r, new IconIndexIterator(argv_r, IconValue.create(1))), new IconInvokeIterator(()-> ((VariadicFunction) integer).apply(x_4_r.deref()))))), new IconAssign().over(new IconSingleton(wr_r), new IconAssign().over(new IconSingleton(hr_r), new IconInvokeIterator(()-> ((VariadicFunction) real).apply(w)))), new IconInvokeIterator(()-> ((VariadicFunction) write).apply("P4\n", w, " ", h)), new IconEvery(new IconAssign().over(new IconSingleton(i_r), new IconProduct(new IconIn(x_5_r, new IconOperation(IconOperators.minus).over(new IconSingleton(h_r), new IconValueIterator(1))), new IconToIterator(IconValue.create(0), x_5_r))), new IconInvokeIterator(()-> ((VariadicFunction) put).apply(wL_r.deref(), i_r.deref()))), new IconAssign().over(new IconSingleton(rL_r), new IconProduct(new IconIn(x_6_r, new IconOperation(IconOperators.timesUnary).over(new IconSingleton(wL_r))), new IconInvokeIterator(()-> ((VariadicFunction) list).apply(x_6_r.deref())))), new IconEvery(new IconAssign().over(new IconSingleton(i_r), new IconProduct(new IconIn(x_7_r, new IconOperation(IconOperators.timesUnary).over(new IconSingleton(rL_r))), new IconToIterator(IconValue.create(1), x_7_r))), new IconBlock( () -> {
			// Temporaries
			IconTmp x_8_r = new IconTmp();
			return new IconAssign().over(new IconIndexIterator(rL_r, i_r), new IconProduct(new IconIn(x_8_r, new IconIndexIterator(wL_r, i_r)), new IconInvokeIterator(()-> ((VariadicFunction) do_y).apply(x_8_r.deref()))));
		}
 )), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_10.setCache(methodCache, "run_mandelbrot_sequential_m");
		body_10.setUnpackClosure(unpack_11).unpackArgs(args_9);
		return body_10;
	}
	@MMethod(name="run_mandelbrot", methodName="run_mandelbrot")
	@MParameter(name="argv", reifiedName="argv_r", type="")
	public IIconIterator run_mandelbrot (Object... args_13) {
		// Reuse method body
		IconIterator body_14 = methodCache.getFree("run_mandelbrot_m");
		if (body_14 != null) { return body_14.reset().unpackArgs(args_13); };
		// Reified parameters
		IconVar argv_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_15 = (Object... params_16) -> {
			if (params_16 ==  null) { params_16 = IIconAtom.getEmptyArray(); };
			argv_r.set((params_16.length > 0) ? params_16[0] : null);
			return null;
		};
		// Method body
		body_14 = new IconSequence(new IconInvokeIterator(()-> ((VariadicFunction) run_mandelbrot_sequential).apply(argv_r.deref())), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_14.setCache(methodCache, "run_mandelbrot_m");
		body_14.setUnpackClosure(unpack_15).unpackArgs(args_13);
		return body_14;
	}
	@MMethod(name="main", methodName="main_m")
	@MParameter(name="argv", reifiedName="argv_r", type="", isVararg=true)
	private IIconIterator main_m (Object... args_17) {
		// Reuse method body
		IconIterator body_18 = methodCache.getFree("main_m");
		if (body_18 != null) { return body_18.reset().unpackArgs(args_17); };
		// Reified parameters
		IconVar argv_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_19 = (Object... params_20) -> {
			if (params_20 ==  null) { params_20 = IIconAtom.getEmptyArray(); };
			argv_r.set((params_20.length > 0) ? Arrays.asList(params_20).subList(0, params_20.length) : new ArrayList());
			return null;
		};
		// Method body
		body_18 = new IconSequence(new IconInvokeIterator(()-> ((VariadicFunction) run_mandelbrot).apply(argv_r.deref())), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_18.setCache(methodCache, "main_m");
		body_18.setUnpackClosure(unpack_19).unpackArgs(args_17);
		return body_18;
	}
	// Static main method
	public static void main(String... args_21) {
		Mandelbrot c = new Mandelbrot(); VariadicFunction m = (VariadicFunction) c.main;
		IconCoExpression.activate(null, null, new IconCoExpression(
		 (Object... coexpr) -> {
			return ((IIconIterator) m.apply((Object[]) args_21)); },
		 () -> { return IconList.createArray(); } ));
	}
}
