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
public class SpectralNorm {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Method references
	@MMethodRef(name="eval_A", methodName="eval_A")
	public Object eval_A = (VariadicFunction) this::eval_A;
	@MMethodRef(name="eval_A_times_u", methodName="eval_A_times_u")
	public Object eval_A_times_u = (VariadicFunction) this::eval_A_times_u;
	@MMethodRef(name="eval_At_times_u", methodName="eval_At_times_u")
	public Object eval_At_times_u = (VariadicFunction) this::eval_At_times_u;
	@MMethodRef(name="eval_AtA_times_u", methodName="eval_AtA_times_u")
	public Object eval_AtA_times_u = (VariadicFunction) this::eval_AtA_times_u;
	@MMethodRef(name="run_spectralnorm", methodName="run_spectralnorm")
	public Object run_spectralnorm = (VariadicFunction) this::run_spectralnorm;
	@MMethodRef(name="main", methodName="main_m")
	public Object main = (VariadicFunction) this::main_m;
	// Constructors
	public SpectralNorm() {
		;
	}
	// Static variadic constructor
	public static VariadicFunction SpectralNorm = (Object... args) -> {
		return new SpectralNorm();
	};
	// Methods
	@MMethod(name="eval_A", methodName="eval_A")
	@MParameter(name="i", reifiedName="i_r", type="")
	@MParameter(name="j", reifiedName="j_r", type="")
	public IIconIterator eval_A (Object... args) {
		// Reuse method body
		IconIterator body = methodCache.getFree("eval_A_m");
		if (body != null) { return body.reset().unpackArgs(args); };
		// Reified parameters
		IconVar i_r = new IconVar().local();
		IconVar j_r = new IconVar().local();
		// Temporaries
		IconTmp x_0_r = new IconTmp();
		IconTmp x_1_r = new IconTmp();
		// Unpack parameters
		VariadicFunction unpack = (Object... params) -> {
			if (params ==  null) { params = IIconAtom.getEmptyArray(); };
			i_r.set((params.length > 0) ? params[0] : null);
			j_r.set((params.length > 1) ? params[1] : null);
			return null;
		};
		// Method body
		body = new IconSequence(new IconReturn(new IconOperation(IconOperators.division).over(new IconValueIterator(1.0), (new IconOperation(IconOperators.plus).over(new IconOperation(IconOperators.plus).over(new IconProduct(new IconProduct(new IconIn(x_0_r, new IconOperation(IconOperators.times).over((new IconOperation(IconOperators.plus).over(new IconSingleton(i_r), new IconSingleton(j_r))), (new IconOperation(IconOperators.plus).over(new IconOperation(IconOperators.plus).over(new IconSingleton(i_r), new IconSingleton(j_r)), new IconValueIterator(1))))), new IconIn(x_1_r, new IconOperation(IconOperators.minusUnary).over(new IconValueIterator(1)))), new IconInvokeIterator(()-> ((VariadicFunction) ishift).apply(x_0_r.deref(), x_1_r.deref()))), new IconSingleton(i_r)), new IconValueIterator(1))))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body.setCache(methodCache, "eval_A_m");
		body.setUnpackClosure(unpack).unpackArgs(args);
		return body;
	}
	@MMethod(name="eval_A_times_u", methodName="eval_A_times_u")
	@MParameter(name="u", reifiedName="u_r", type="")
	@MParameter(name="resulted_list", reifiedName="resulted_list_r", type="")
	@MLocal(name="u_len", reifiedName="u_len_r", type="")
	@MLocal(name="i", reifiedName="i_r", type="")
	@MLocal(name="partial_sum", reifiedName="partial_sum_r", type="")
	@MLocal(name="j", reifiedName="j_r", type="")
	public IIconIterator eval_A_times_u (Object... args_13) {
		// Reuse method body
		IconIterator body_14 = methodCache.getFree("eval_A_times_u_m");
		if (body_14 != null) { return body_14.reset().unpackArgs(args_13); };
		// Reified parameters
		IconVar u_r = new IconVar().local();
		IconVar resulted_list_r = new IconVar().local();
		// Temporaries
		IconTmp x_2_r = new IconTmp();
		// Locals
		IconVar u_len_r = new IconVar().local();
		IconVar i_r = new IconVar().local();
		IconVar partial_sum_r = new IconVar().local();
		IconVar j_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_15 = (Object... params_16) -> {
			if (params_16 ==  null) { params_16 = IIconAtom.getEmptyArray(); };
			u_r.set((params_16.length > 0) ? params_16[0] : null);
			resulted_list_r.set((params_16.length > 1) ? params_16[1] : null);
			// Reset locals
			u_len_r.set(null);
			i_r.set(null);
			partial_sum_r.set(null);
			j_r.set(null);
			return null;
		};
		// Method body
		body_14 = new IconSequence(new IconAssign().over(new IconSingleton(u_len_r), new IconOperation(IconOperators.timesUnary).over(new IconSingleton(u_r))), new IconEvery(new IconAssign().over(new IconSingleton(i_r), new IconProduct(new IconIn(x_2_r, new IconOperation(IconOperators.minus).over(new IconSingleton(u_len_r), new IconValueIterator(1))), new IconToIterator(IconValue.create(0), x_2_r))), new IconBlock( () -> {
			// Temporaries
			IconTmp x_3_r = new IconTmp();
			IconTmp x_5_r = new IconTmp();
			return new IconSequence(new IconAssign().over(new IconSingleton(partial_sum_r), new IconValueIterator(0)), new IconEvery(new IconAssign().over(new IconSingleton(j_r), new IconProduct(new IconIn(x_3_r, new IconOperation(IconOperators.minus).over(new IconSingleton(u_len_r), new IconValueIterator(1))), new IconToIterator(IconValue.create(0), x_3_r))), new IconBlock( () -> {
				// Temporaries
				IconTmp x_4_r = new IconTmp();
				return new IconAssign().augment(IconOperators.plus).over(new IconSingleton(partial_sum_r), new IconOperation(IconOperators.times).over(new IconInvokeIterator(()-> ((VariadicFunction) eval_A).apply(i_r.deref(), j_r.deref())), new IconProduct(new IconIn(x_4_r, new IconOperation(IconOperators.plus).over(new IconSingleton(j_r), new IconValueIterator(1))), new IconIndexIterator(u_r, x_4_r))));
			}
 )), new IconAssign().over(new IconProduct(new IconIn(x_5_r, new IconOperation(IconOperators.plus).over(new IconSingleton(i_r), new IconValueIterator(1))), new IconIndexIterator(resulted_list_r, x_5_r)), new IconSingleton(partial_sum_r)));
		}
 )), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_14.setCache(methodCache, "eval_A_times_u_m");
		body_14.setUnpackClosure(unpack_15).unpackArgs(args_13);
		return body_14;
	}
	@MMethod(name="eval_At_times_u", methodName="eval_At_times_u")
	@MParameter(name="u", reifiedName="u_r", type="")
	@MParameter(name="resulted_list", reifiedName="resulted_list_r", type="")
	@MLocal(name="u_len", reifiedName="u_len_r", type="")
	@MLocal(name="i", reifiedName="i_r", type="")
	@MLocal(name="partial_sum", reifiedName="partial_sum_r", type="")
	@MLocal(name="j", reifiedName="j_r", type="")
	public IIconIterator eval_At_times_u (Object... args_17) {
		// Reuse method body
		IconIterator body_18 = methodCache.getFree("eval_At_times_u_m");
		if (body_18 != null) { return body_18.reset().unpackArgs(args_17); };
		// Reified parameters
		IconVar u_r = new IconVar().local();
		IconVar resulted_list_r = new IconVar().local();
		// Temporaries
		IconTmp x_6_r = new IconTmp();
		// Locals
		IconVar u_len_r = new IconVar().local();
		IconVar i_r = new IconVar().local();
		IconVar partial_sum_r = new IconVar().local();
		IconVar j_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_19 = (Object... params_20) -> {
			if (params_20 ==  null) { params_20 = IIconAtom.getEmptyArray(); };
			u_r.set((params_20.length > 0) ? params_20[0] : null);
			resulted_list_r.set((params_20.length > 1) ? params_20[1] : null);
			// Reset locals
			u_len_r.set(null);
			i_r.set(null);
			partial_sum_r.set(null);
			j_r.set(null);
			return null;
		};
		// Method body
		body_18 = new IconSequence(new IconAssign().over(new IconSingleton(u_len_r), new IconOperation(IconOperators.timesUnary).over(new IconSingleton(u_r))), new IconEvery(new IconAssign().over(new IconSingleton(i_r), new IconProduct(new IconIn(x_6_r, new IconOperation(IconOperators.minus).over(new IconSingleton(u_len_r), new IconValueIterator(1))), new IconToIterator(IconValue.create(0), x_6_r))), new IconBlock( () -> {
			// Temporaries
			IconTmp x_7_r = new IconTmp();
			IconTmp x_9_r = new IconTmp();
			return new IconSequence(new IconAssign().over(new IconSingleton(partial_sum_r), new IconValueIterator(0)), new IconEvery(new IconAssign().over(new IconSingleton(j_r), new IconProduct(new IconIn(x_7_r, new IconOperation(IconOperators.minus).over(new IconSingleton(u_len_r), new IconValueIterator(1))), new IconToIterator(IconValue.create(0), x_7_r))), new IconBlock( () -> {
				// Temporaries
				IconTmp x_8_r = new IconTmp();
				return new IconAssign().augment(IconOperators.plus).over(new IconSingleton(partial_sum_r), new IconOperation(IconOperators.times).over(new IconInvokeIterator(()-> ((VariadicFunction) eval_A).apply(j_r.deref(), i_r.deref())), new IconProduct(new IconIn(x_8_r, new IconOperation(IconOperators.plus).over(new IconSingleton(j_r), new IconValueIterator(1))), new IconIndexIterator(u_r, x_8_r))));
			}
 )), new IconAssign().over(new IconProduct(new IconIn(x_9_r, new IconOperation(IconOperators.plus).over(new IconSingleton(i_r), new IconValueIterator(1))), new IconIndexIterator(resulted_list_r, x_9_r)), new IconSingleton(partial_sum_r)));
		}
 )), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_18.setCache(methodCache, "eval_At_times_u_m");
		body_18.setUnpackClosure(unpack_19).unpackArgs(args_17);
		return body_18;
	}
	@MMethod(name="eval_AtA_times_u", methodName="eval_AtA_times_u")
	@MParameter(name="u", reifiedName="u_r", type="")
	@MParameter(name="out", reifiedName="out_r", type="")
	@MParameter(name="tmp", reifiedName="tmp_r", type="")
	public IIconIterator eval_AtA_times_u (Object... args_21) {
		// Reuse method body
		IconIterator body_22 = methodCache.getFree("eval_AtA_times_u_m");
		if (body_22 != null) { return body_22.reset().unpackArgs(args_21); };
		// Reified parameters
		IconVar u_r = new IconVar().local();
		IconVar out_r = new IconVar().local();
		IconVar tmp_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_23 = (Object... params_24) -> {
			if (params_24 ==  null) { params_24 = IIconAtom.getEmptyArray(); };
			u_r.set((params_24.length > 0) ? params_24[0] : null);
			out_r.set((params_24.length > 1) ? params_24[1] : null);
			tmp_r.set((params_24.length > 2) ? params_24[2] : null);
			return null;
		};
		// Method body
		body_22 = new IconSequence(new IconInvokeIterator(()-> ((VariadicFunction) eval_A_times_u).apply(u_r.deref(), tmp_r.deref())), new IconInvokeIterator(()-> ((VariadicFunction) eval_At_times_u).apply(tmp_r.deref(), out_r.deref())), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_22.setCache(methodCache, "eval_AtA_times_u_m");
		body_22.setUnpackClosure(unpack_23).unpackArgs(args_21);
		return body_22;
	}
	@MMethod(name="run_spectralnorm", methodName="run_spectralnorm")
	@MParameter(name="av", reifiedName="av_r", type="")
	@MLocal(name="n", reifiedName="n_r", type="")
	@MLocal(name="u", reifiedName="u_r", type="")
	@MLocal(name="v", reifiedName="v_r", type="")
	@MLocal(name="tmp", reifiedName="tmp_r", type="")
	@MLocal(name="vBv", reifiedName="vBv_r", type="")
	@MLocal(name="vv", reifiedName="vv_r", type="")
	@MLocal(name="i", reifiedName="i_r", type="")
	@MLocal(name="vi", reifiedName="vi_r", type="")
	public IIconIterator run_spectralnorm (Object... args_25) {
		// Reuse method body
		IconIterator body_26 = methodCache.getFree("run_spectralnorm_m");
		if (body_26 != null) { return body_26.reset().unpackArgs(args_25); };
		// Reified parameters
		IconVar av_r = new IconVar().local();
		// Temporaries
		IconTmp x_10_r = new IconTmp();
		IconTmp x_12_r = new IconTmp();
		IconTmp x_11_r = new IconTmp();
		// Locals
		IconVar n_r = new IconVar().local();
		IconVar u_r = new IconVar().local();
		IconVar v_r = new IconVar().local();
		IconVar tmp_r = new IconVar().local();
		IconVar vBv_r = new IconVar().local();
		IconVar vv_r = new IconVar().local();
		IconVar i_r = new IconVar().local();
		IconVar vi_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_27 = (Object... params_28) -> {
			if (params_28 ==  null) { params_28 = IIconAtom.getEmptyArray(); };
			av_r.set((params_28.length > 0) ? params_28[0] : null);
			// Reset locals
			n_r.set(null);
			u_r.set(null);
			v_r.set(null);
			tmp_r.set(null);
			vBv_r.set(null);
			vv_r.set(null);
			i_r.set(null);
			vi_r.set(null);
			return null;
		};
		// Method body
		body_26 = new IconSequence(new IconAssign().over(new IconSingleton(n_r), new IconProduct(new IconIn(x_10_r, new IconIndexIterator(av_r, IconValue.create(1))), new IconInvokeIterator(()-> ((VariadicFunction) integer).apply(x_10_r.deref())))), new IconAssign().over(new IconSingleton(u_r), new IconInvokeIterator(()-> ((VariadicFunction) list).apply(n_r.deref(), IconNumber.create(1.0)))), new IconAssign().over(new IconSingleton(v_r), new IconInvokeIterator(()-> ((VariadicFunction) list).apply(n_r.deref(), IconNumber.create(1.0)))), new IconAssign().over(new IconSingleton(tmp_r), new IconInvokeIterator(()-> ((VariadicFunction) list).apply(n_r.deref(), IconNumber.create(1.0)))), new IconEvery(new IconToIterator(IconValue.create(1), IconValue.create(10)), new IconSequence(new IconInvokeIterator(()-> ((VariadicFunction) eval_AtA_times_u).apply(u_r.deref(), v_r.deref(), tmp_r.deref())), new IconInvokeIterator(()-> ((VariadicFunction) eval_AtA_times_u).apply(v_r.deref(), u_r.deref(), tmp_r.deref())))), new IconAssign().over(new IconSingleton(vBv_r), new IconAssign().over(new IconSingleton(vv_r), new IconValueIterator(0))), new IconEvery(new IconAssign().over(new IconSingleton(i_r), new IconToIterator(IconValue.create(1), n_r)), new IconSequence(new IconAssign().over(new IconSingleton(vi_r), new IconIndexIterator(v_r, i_r)), new IconAssign().augment(IconOperators.plus).over(new IconSingleton(vBv_r), new IconOperation(IconOperators.times).over(new IconIndexIterator(u_r, i_r), new IconSingleton(vi_r))), new IconAssign().augment(IconOperators.plus).over(new IconSingleton(vv_r), new IconOperation(IconOperators.times).over(new IconSingleton(vi_r), new IconSingleton(vi_r))))), new IconProduct(new IconIn(x_12_r, new IconProduct(new IconIn(x_11_r, new IconOperation(IconOperators.division).over(new IconSingleton(vBv_r), new IconSingleton(vv_r))), new IconInvokeIterator(()-> ((VariadicFunction) sqrt).apply(x_11_r.deref())))), new IconInvokeIterator(()-> ((VariadicFunction) write).apply(x_12_r.deref()))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_26.setCache(methodCache, "run_spectralnorm_m");
		body_26.setUnpackClosure(unpack_27).unpackArgs(args_25);
		return body_26;
	}
	@MMethod(name="main", methodName="main_m")
	@MParameter(name="av", reifiedName="av_r", type="", isVararg=true)
	private IIconIterator main_m (Object... args_29) {
		// Reuse method body
		IconIterator body_30 = methodCache.getFree("main_m");
		if (body_30 != null) { return body_30.reset().unpackArgs(args_29); };
		// Reified parameters
		IconVar av_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_31 = (Object... params_32) -> {
			if (params_32 ==  null) { params_32 = IIconAtom.getEmptyArray(); };
			av_r.set((params_32.length > 0) ? Arrays.asList(params_32).subList(0, params_32.length) : new ArrayList());
			return null;
		};
		// Method body
		body_30 = new IconSequence(new IconInvokeIterator(()-> ((VariadicFunction) run_spectralnorm).apply(av_r.deref())), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_30.setCache(methodCache, "main_m");
		body_30.setUnpackClosure(unpack_31).unpackArgs(args_29);
		return body_30;
	}
	// Static main method
	public static void main(String... args_33) {
		SpectralNorm c = new SpectralNorm(); VariadicFunction m = (VariadicFunction) c.main;
		IconCoExpression.activate(null, null, new IconCoExpression(
		 (Object... coexpr) -> {
			return ((IIconIterator) m.apply((Object[]) args_33)); },
		 () -> { return IconList.createArray(); } ));
	}
}
