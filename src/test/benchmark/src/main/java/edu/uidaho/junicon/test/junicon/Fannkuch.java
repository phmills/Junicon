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
public class Fannkuch {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Method references
	@MMethodRef(name="do_fannkuch", methodName="do_fannkuch")
	public Object do_fannkuch = (VariadicFunction) this::do_fannkuch;
	@MMethodRef(name="run_fannkuch", methodName="run_fannkuch")
	public Object run_fannkuch = (VariadicFunction) this::run_fannkuch;
	@MMethodRef(name="main", methodName="main_m")
	public Object main = (VariadicFunction) this::main_m;
	// Constructors
	public Fannkuch() {
		;
	}
	// Static variadic constructor
	public static VariadicFunction Fannkuch = (Object... args) -> {
		return new Fannkuch();
	};
	// Methods
	@MMethod(name="do_fannkuch", methodName="do_fannkuch")
	@MParameter(name="n", reifiedName="n_r", type="")
	@MLocal(name="flipsCount", reifiedName="flipsCount_r", type="")
	@MLocal(name="maxFlipsCount", reifiedName="maxFlipsCount_r", type="")
	@MLocal(name="checksum", reifiedName="checksum_r", type="")
	@MLocal(name="perm", reifiedName="perm_r", type="")
	@MLocal(name="permSign", reifiedName="permSign_r", type="")
	@MLocal(name="perm0", reifiedName="perm0_r", type="")
	@MLocal(name="perm1", reifiedName="perm1_r", type="")
	@MLocal(name="count", reifiedName="count_r", type="")
	@MLocal(name="i", reifiedName="i_r", type="")
	@MLocal(name="k", reifiedName="k_r", type="")
	@MLocal(name="kk", reifiedName="kk_r", type="")
	@MLocal(name="top", reifiedName="top_r", type="")
	@MLocal(name="flag", reifiedName="flag_r", type="")
	@MLocal(name="r", reifiedName="r_r", type="")
	public IIconIterator do_fannkuch (Object... args) {
		// Reuse method body
		IconIterator body = methodCache.getFree("do_fannkuch_m");
		if (body != null) { return body.reset().unpackArgs(args); };
		// Reified parameters
		IconVar n_r = new IconVar().local();
		// Temporaries
		IconTmp x_0_r = new IconTmp();
		// Locals
		IconVar flipsCount_r = new IconVar().local();
		IconVar maxFlipsCount_r = new IconVar().local();
		IconVar checksum_r = new IconVar().local();
		IconVar perm_r = new IconVar().local();
		IconVar permSign_r = new IconVar().local();
		IconVar perm0_r = new IconVar().local();
		IconVar perm1_r = new IconVar().local();
		IconVar count_r = new IconVar().local();
		IconVar i_r = new IconVar().local();
		IconVar k_r = new IconVar().local();
		IconVar kk_r = new IconVar().local();
		IconVar top_r = new IconVar().local();
		IconVar flag_r = new IconVar().local();
		IconVar r_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack = (Object... params) -> {
			if (params ==  null) { params = IIconAtom.getEmptyArray(); };
			n_r.set((params.length > 0) ? params[0] : null);
			// Reset locals
			flipsCount_r.set(null);
			maxFlipsCount_r.set(null);
			checksum_r.set(null);
			perm_r.set(null);
			permSign_r.set(null);
			perm0_r.set(null);
			perm1_r.set(null);
			count_r.set(null);
			i_r.set(null);
			k_r.set(null);
			kk_r.set(null);
			top_r.set(null);
			flag_r.set(null);
			r_r.set(null);
			return null;
		};
		// Method body
		body = new IconSequence(new IconAssign().over(new IconSingleton(maxFlipsCount_r), new IconAssign().over(new IconSingleton(checksum_r), new IconValueIterator(0))), new IconAssign().over(new IconSingleton(permSign_r), new IconValueIterator(1)), new IconAssign().over(new IconSingleton(perm1_r), new IconInvokeIterator(()-> ((VariadicFunction) list).apply(n_r.deref()))), new IconEvery(new IconAssign().over(new IconProduct(new IconIn(x_0_r, new IconAssign().over(new IconSingleton(i_r), new IconToIterator(IconValue.create(1), n_r))), new IconIndexIterator(perm1_r, x_0_r)), new IconOperation(IconOperators.minus).over(new IconSingleton(i_r), new IconValueIterator(1)))), new IconAssign().over(new IconSingleton(count_r), new IconInvokeIterator(()-> ((VariadicFunction) copy).apply(perm1_r.deref()))), new IconRepeat(new IconSequence(new IconAssign().over(new IconSingleton(k_r), new IconIndexIterator(perm1_r, IconValue.create(1))), new IconIf(new IconOperation(IconOperators.notSameNumberAs).over(new IconSingleton(k_r), new IconValueIterator(0)), new IconBlock( () -> {
			// Temporaries
			IconTmp x_1_r = new IconTmp();
			return new IconSequence(new IconAssign().over(new IconSingleton(perm_r), new IconInvokeIterator(()-> ((VariadicFunction) copy).apply(perm1_r.deref()))), new IconAssign().over(new IconSingleton(flipsCount_r), new IconValueIterator(1)), new IconAssign().over(new IconSingleton(kk_r), new IconProduct(new IconIn(x_1_r, new IconOperation(IconOperators.plus).over(new IconSingleton(k_r), new IconValueIterator(1))), new IconIndexIterator(perm_r, x_1_r))), new IconWhile(new IconOperation(IconOperators.notSameNumberAs).over(new IconSingleton(kk_r), new IconValueIterator(0)), new IconBlock( () -> {
				// Temporaries
				IconTmp x_2_r = new IconTmp();
				IconTmp x_3_r = new IconTmp();
				IconTmp x_4_r = new IconTmp();
				return new IconSequence(new IconAssign().over(new IconSingleton(top_r), new IconOperation(IconOperators.plus).over(new IconSingleton(k_r), new IconValueIterator(2))), new IconEvery(new IconAssign().over(new IconSingleton(i_r), new IconProduct(new IconIn(x_2_r, new IconOperation(IconOperators.division).over((new IconOperation(IconOperators.plus).over(new IconSingleton(k_r), new IconValueIterator(1))), new IconValueIterator(2))), new IconToIterator(IconValue.create(1), x_2_r))), new IconAssign().swap().over(new IconIndexIterator(perm_r, i_r), new IconProduct(new IconIn(x_3_r, new IconOperation(IconOperators.minus).over(new IconSingleton(top_r), new IconSingleton(i_r))), new IconIndexIterator(perm_r, x_3_r)))), new IconAssign().augment(IconOperators.plus).over(new IconSingleton(flipsCount_r), new IconValueIterator(1)), new IconAssign().over(new IconSingleton(k_r), new IconSingleton(kk_r)), new IconAssign().over(new IconSingleton(kk_r), new IconProduct(new IconIn(x_4_r, new IconOperation(IconOperators.plus).over(new IconSingleton(kk_r), new IconValueIterator(1))), new IconIndexIterator(perm_r, x_4_r))));
			}
 )), new IconIf(new IconOperation(IconOperators.lessThan).over(new IconSingleton(maxFlipsCount_r), new IconSingleton(flipsCount_r)), new IconAssign().over(new IconSingleton(maxFlipsCount_r), new IconSingleton(flipsCount_r))), new IconIf(new IconOperation(IconOperators.sameNumberAs).over(new IconSingleton(permSign_r), new IconValueIterator(1)), new IconAssign().augment(IconOperators.plus).over(new IconSingleton(checksum_r), new IconSingleton(flipsCount_r)), new IconAssign().augment(IconOperators.minus).over(new IconSingleton(checksum_r), new IconSingleton(flipsCount_r))));
		}
 )), new IconAssign().over(new IconSingleton(flag_r), new IconValueIterator(1)), new IconIf(new IconOperation(IconOperators.sameNumberAs).over(new IconSingleton(permSign_r), new IconValueIterator(1)), new IconSequence(new IconAssign().swap().over(new IconIndexIterator(perm1_r, IconValue.create(1)), new IconIndexIterator(perm1_r, IconValue.create(2))), new IconAssign().over(new IconSingleton(permSign_r), new IconValueIterator(0))), new IconBlock( () -> {
			// Temporaries
			IconTmp x_5_r = new IconTmp();
			return new IconSequence(new IconAssign().swap().over(new IconIndexIterator(perm1_r, IconValue.create(2)), new IconIndexIterator(perm1_r, IconValue.create(3))), new IconAssign().over(new IconSingleton(permSign_r), new IconValueIterator(1)), new IconEvery(new IconAssign().over(new IconSingleton(r_r), new IconProduct(new IconIn(x_5_r, new IconOperation(IconOperators.minus).over(new IconSingleton(n_r), new IconValueIterator(1))), new IconToIterator(IconValue.create(3), x_5_r))), new IconBlock( () -> {
				// Temporaries
				IconTmp x_6_r = new IconTmp();
				return new IconSequence(new IconIf(new IconOperation(IconOperators.notSameNumberAs).over(new IconIndexIterator(count_r, r_r), new IconValueIterator(0)), new IconSequence(new IconAssign().over(new IconSingleton(flag_r), new IconValueIterator(0)), new IconBreak(new IconNullIterator()))), new IconAssign().over(new IconIndexIterator(count_r, r_r), new IconOperation(IconOperators.minus).over(new IconSingleton(r_r), new IconValueIterator(1))), new IconAssign().over(new IconSingleton(perm0_r), new IconInvokeIterator(()-> ((VariadicFunction) pop).apply(perm1_r.deref()))), new IconProduct(new IconIn(x_6_r, new IconOperation(IconOperators.plus).over(new IconSingleton(r_r), new IconValueIterator(1))), new IconInvokeIterator(()-> ((VariadicFunction) insert).apply(perm1_r.deref(), x_6_r.deref(), perm0_r.deref()))));
			}
 )), new IconIf(new IconOperation(IconOperators.sameNumberAs).over(new IconSingleton(flag_r), new IconValueIterator(1)), new IconSequence(new IconAssign().over(new IconSingleton(r_r), new IconSingleton(n_r)), new IconIf(new IconOperation(IconOperators.sameNumberAs).over(new IconIndexIterator(count_r, r_r), new IconValueIterator(0)), new IconSequence(new IconInvokeIterator(()-> ((VariadicFunction) writeln).apply(checksum_r.deref())), new IconReturn(new IconSingleton(maxFlipsCount_r)))))), new IconAssign().augment(IconOperators.minus).over(new IconIndexIterator(count_r, r_r), new IconValueIterator(1)));
		}
 )))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body.setCache(methodCache, "do_fannkuch_m");
		body.setUnpackClosure(unpack).unpackArgs(args);
		return body;
	}
	@MMethod(name="run_fannkuch", methodName="run_fannkuch")
	@MParameter(name="av", reifiedName="av_r", type="")
	@MLocal(name="n", reifiedName="n_r", type="")
	@MLocal(name="res", reifiedName="res_r", type="")
	public IIconIterator run_fannkuch (Object... args_8) {
		// Reuse method body
		IconIterator body_9 = methodCache.getFree("run_fannkuch_m");
		if (body_9 != null) { return body_9.reset().unpackArgs(args_8); };
		// Reified parameters
		IconVar av_r = new IconVar().local();
		// Temporaries
		IconTmp x_7_r = new IconTmp();
		// Locals
		IconVar n_r = new IconVar().local();
		IconVar res_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_10 = (Object... params_11) -> {
			if (params_11 ==  null) { params_11 = IIconAtom.getEmptyArray(); };
			av_r.set((params_11.length > 0) ? params_11[0] : null);
			// Reset locals
			n_r.set(null);
			res_r.set(null);
			return null;
		};
		// Method body
		body_9 = new IconSequence(new IconAssign().over(new IconSingleton(n_r), new IconProduct(new IconIn(x_7_r, new IconIndexIterator(av_r, IconValue.create(1))), new IconInvokeIterator(()-> ((VariadicFunction) integer).apply(x_7_r.deref())))), new IconAssign().over(new IconSingleton(res_r), new IconInvokeIterator(()-> ((VariadicFunction) do_fannkuch).apply(n_r.deref()))), new IconInvokeIterator(()-> ((VariadicFunction) writeln).apply("Pfannkuchen(", n_r.deref(), ") = ", res_r.deref())), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_9.setCache(methodCache, "run_fannkuch_m");
		body_9.setUnpackClosure(unpack_10).unpackArgs(args_8);
		return body_9;
	}
	@MMethod(name="main", methodName="main_m")
	@MParameter(name="av", reifiedName="av_r", type="", isVararg=true)
	private IIconIterator main_m (Object... args_12) {
		// Reuse method body
		IconIterator body_13 = methodCache.getFree("main_m");
		if (body_13 != null) { return body_13.reset().unpackArgs(args_12); };
		// Reified parameters
		IconVar av_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_14 = (Object... params_15) -> {
			if (params_15 ==  null) { params_15 = IIconAtom.getEmptyArray(); };
			av_r.set((params_15.length > 0) ? Arrays.asList(params_15).subList(0, params_15.length) : new ArrayList());
			return null;
		};
		// Method body
		body_13 = new IconSequence(new IconInvokeIterator(()-> ((VariadicFunction) run_fannkuch).apply(av_r.deref())), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_13.setCache(methodCache, "main_m");
		body_13.setUnpackClosure(unpack_14).unpackArgs(args_12);
		return body_13;
	}
	// Static main method
	public static void main(String... args_16) {
		Fannkuch c = new Fannkuch(); VariadicFunction m = (VariadicFunction) c.main;
		IconCoExpression.activate(null, null, new IconCoExpression(
		 (Object... coexpr) -> {
			return ((IIconIterator) m.apply((Object[]) args_16)); },
		 () -> { return IconList.createArray(); } ));
	}
}
