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
class xyz {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Constructor fields
	@MField(name="x", reifiedName="x_r", type="", isConstructorField=true)
	public Object x;
	@MField(name="y", reifiedName="y_r", type="", isConstructorField=true)
	public Object y;
	@MField(name="z", reifiedName="z_r", type="", isConstructorField=true)
	public Object z;
	// Reified constructor fields
	private IconVar x_r = new IconVar(()-> x, (rhs)-> x=rhs);
	private IconVar y_r = new IconVar(()-> y, (rhs)-> y=rhs);
	private IconVar z_r = new IconVar(()-> z, (rhs)-> z=rhs);
	// Constructors
	public xyz() { ;}
	public xyz(Object x, Object y, Object z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	// Static variadic constructor
	public static VariadicFunction xyz = (Object... args) -> {
		if (args ==  null) { args = IIconAtom.getEmptyArray(); };
		return new xyz((args.length > 0) ? args[0] : null, (args.length > 1) ? args[1] : null, (args.length > 2) ? args[2] : null);
	};
}
public class Nbody {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Method references
	@MMethodRef(name="combinations", methodName="combinations")
	public Object combinations = (VariadicFunction) this::combinations;
	@MMethodRef(name="advance", methodName="advance")
	public Object advance = (VariadicFunction) this::advance;
	@MMethodRef(name="report_energy", methodName="report_energy")
	public Object report_energy = (VariadicFunction) this::report_energy;
	@MMethodRef(name="offset_momentum", methodName="offset_momentum")
	public Object offset_momentum = (VariadicFunction) this::offset_momentum;
	@MMethodRef(name="run_nbody", methodName="run_nbody")
	public Object run_nbody = (VariadicFunction) this::run_nbody;
	@MMethodRef(name="main", methodName="main_m")
	public Object main = (VariadicFunction) this::main_m;
	// Constructors
	public Nbody() {
		;
	}
	// Static variadic constructor
	public static VariadicFunction Nbody = (Object... args) -> {
		return new Nbody();
	};
	// Locals
	@MField(name="PI", reifiedName="PI_r", type="", isConstructorField=false)
	public Object PI;
	private IconVar PI_r = new IconVar(()-> PI, (rhs)-> PI=rhs);
	@MField(name="SOLAR_MASS", reifiedName="SOLAR_MASS_r", type="", isConstructorField=false)
	public Object SOLAR_MASS;
	private IconVar SOLAR_MASS_r = new IconVar(()-> SOLAR_MASS, (rhs)-> SOLAR_MASS=rhs);
	@MField(name="DAYS_PER_YEAR", reifiedName="DAYS_PER_YEAR_r", type="", isConstructorField=false)
	public Object DAYS_PER_YEAR;
	private IconVar DAYS_PER_YEAR_r = new IconVar(()-> DAYS_PER_YEAR, (rhs)-> DAYS_PER_YEAR=rhs);
	@MField(name="BODIES", reifiedName="BODIES_r", type="", isConstructorField=false)
	public Object BODIES;
	private IconVar BODIES_r = new IconVar(()-> BODIES, (rhs)-> BODIES=rhs);
	@MField(name="SYSTEM", reifiedName="SYSTEM_r", type="", isConstructorField=false)
	public Object SYSTEM;
	private IconVar SYSTEM_r = new IconVar(()-> SYSTEM, (rhs)-> SYSTEM=rhs);
	@MField(name="PAIRS", reifiedName="PAIRS_r", type="", isConstructorField=false)
	public Object PAIRS;
	private IconVar PAIRS_r = new IconVar(()-> PAIRS, (rhs)-> PAIRS=rhs);
	// Methods
	@MMethod(name="combinations", methodName="combinations")
	@MParameter(name="L", reifiedName="L_r", type="")
	@MLocal(name="result", reifiedName="result_r", type="")
	@MLocal(name="x", reifiedName="x_r", type="")
	public IIconIterator combinations (Object... args) {
		// Reuse method body
		IconIterator body = methodCache.getFree("combinations_m");
		if (body != null) { return body.reset().unpackArgs(args); };
		// Reified parameters
		IconVar L_r = new IconVar().local();
		// Temporaries
		IconTmp x_7_r = new IconTmp();
		IconTmp x_2_r = new IconTmp();
		IconTmp x_1_r = new IconTmp();
		IconTmp x_0_r = new IconTmp();
		IconTmp x_6_r = new IconTmp();
		IconTmp x_5_r = new IconTmp();
		IconTmp x_3_r = new IconTmp();
		IconTmp x_4_r = new IconTmp();
		// Locals
		IconVar result_r = new IconVar().local();
		IconVar x_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack = (Object... params) -> {
			if (params ==  null) { params = IIconAtom.getEmptyArray(); };
			L_r.set((params.length > 0) ? params[0] : null);
			// Reset locals
			result_r.set(null);
			x_r.set(null);
			return null;
		};
		// Method body
		body = new IconSequence(new IconAssign().over(new IconSingleton(result_r), IconVarIterator.createAsList(()-> new IconList())), new IconEvery(new IconProduct(new IconIn(x_7_r, new IconProduct(new IconProduct(new IconIn(x_2_r, new IconProduct(new IconIn(x_1_r, new IconAssign().over(new IconSingleton(x_r), new IconProduct(new IconIn(x_0_r, new IconOperation(IconOperators.timesUnary).over(new IconSingleton(L_r))), new IconToIterator(IconValue.create(1), x_0_r)))), new IconIndexIterator(L_r, x_1_r))), new IconIn(x_6_r, new IconProduct(new IconIn(x_5_r, new IconProduct(new IconProduct(new IconIn(x_3_r, new IconOperation(IconOperators.plus).over(new IconSingleton(x_r), new IconValueIterator(1))), new IconIn(x_4_r, new IconOperation(IconOperators.timesUnary).over(new IconSingleton(L_r)))), new IconToIterator(x_3_r, x_4_r))), new IconIndexIterator(L_r, x_5_r)))), IconVarIterator.createAsList(()-> new IconList(x_2_r.deref(), x_6_r.deref())))), new IconInvokeIterator(()-> ((VariadicFunction) put).apply(result_r.deref(), x_7_r.deref())))), new IconReturn(new IconSingleton(result_r)), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body.setCache(methodCache, "combinations_m");
		body.setUnpackClosure(unpack).unpackArgs(args);
		return body;
	}
	@MMethod(name="advance", methodName="advance")
	@MParameter(name="dt", reifiedName="dt_r", type="")
	@MParameter(name="n", reifiedName="n_r", type="")
	@MLocal(name="i", reifiedName="i_r", type="")
	@MLocal(name="p", reifiedName="p_r", type="")
	@MLocal(name="p1", reifiedName="p1_r", type="")
	@MLocal(name="p11", reifiedName="p11_r", type="")
	@MLocal(name="v1", reifiedName="v1_r", type="")
	@MLocal(name="p2", reifiedName="p2_r", type="")
	@MLocal(name="p21", reifiedName="p21_r", type="")
	@MLocal(name="v2", reifiedName="v2_r", type="")
	@MLocal(name="dx", reifiedName="dx_r", type="")
	@MLocal(name="dy", reifiedName="dy_r", type="")
	@MLocal(name="dz", reifiedName="dz_r", type="")
	@MLocal(name="mag", reifiedName="mag_r", type="")
	@MLocal(name="b1m", reifiedName="b1m_r", type="")
	@MLocal(name="b2m", reifiedName="b2m_r", type="")
	@MLocal(name="s", reifiedName="s_r", type="")
	@MLocal(name="r", reifiedName="r_r", type="")
	@MLocal(name="v", reifiedName="v_r", type="")
	public IIconIterator advance (Object... args_48) {
		// Reuse method body
		IconIterator body_49 = methodCache.getFree("advance_m");
		if (body_49 != null) { return body_49.reset().unpackArgs(args_48); };
		// Reified parameters
		IconVar dt_r = new IconVar().local();
		IconVar n_r = new IconVar().local();
		// Locals
		IconVar i_r = new IconVar().local();
		IconVar p_r = new IconVar().local();
		IconVar p1_r = new IconVar().local();
		IconVar p11_r = new IconVar().local();
		IconVar v1_r = new IconVar().local();
		IconVar p2_r = new IconVar().local();
		IconVar p21_r = new IconVar().local();
		IconVar v2_r = new IconVar().local();
		IconVar dx_r = new IconVar().local();
		IconVar dy_r = new IconVar().local();
		IconVar dz_r = new IconVar().local();
		IconVar mag_r = new IconVar().local();
		IconVar b1m_r = new IconVar().local();
		IconVar b2m_r = new IconVar().local();
		IconVar s_r = new IconVar().local();
		IconVar r_r = new IconVar().local();
		IconVar v_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_50 = (Object... params_51) -> {
			if (params_51 ==  null) { params_51 = IIconAtom.getEmptyArray(); };
			dt_r.set((params_51.length > 0) ? params_51[0] : null);
			n_r.set((params_51.length > 1) ? params_51[1] : null);
			// Reset locals
			i_r.set(null);
			p_r.set(null);
			p1_r.set(null);
			p11_r.set(null);
			v1_r.set(null);
			p2_r.set(null);
			p21_r.set(null);
			v2_r.set(null);
			dx_r.set(null);
			dy_r.set(null);
			dz_r.set(null);
			mag_r.set(null);
			b1m_r.set(null);
			b2m_r.set(null);
			s_r.set(null);
			r_r.set(null);
			v_r.set(null);
			return null;
		};
		// Method body
		body_49 = new IconSequence(new IconEvery(new IconAssign().over(new IconSingleton(i_r), new IconToIterator(IconValue.create(1), n_r)), new IconSequence(new IconEvery(new IconAssign().over(new IconSingleton(p_r), new IconPromote(PAIRS_r)), new IconSequence(new IconAssign().over(new IconSingleton(p1_r), new IconIndexIterator(p_r, IconValue.create(1))), new IconAssign().over(new IconSingleton(p11_r), new IconIndexIterator(p1_r, IconValue.create(1))), new IconAssign().over(new IconSingleton(v1_r), new IconIndexIterator(p1_r, IconValue.create(2))), new IconAssign().over(new IconSingleton(p2_r), new IconIndexIterator(p_r, IconValue.create(2))), new IconAssign().over(new IconSingleton(p21_r), new IconIndexIterator(p2_r, IconValue.create(1))), new IconAssign().over(new IconSingleton(v2_r), new IconIndexIterator(p2_r, IconValue.create(2))), new IconAssign().over(new IconSingleton(dx_r), new IconOperation(IconOperators.minus).over(new IconFieldIterator(p11_r, "x"), new IconFieldIterator(p21_r, "x"))), new IconAssign().over(new IconSingleton(dy_r), new IconOperation(IconOperators.minus).over(new IconFieldIterator(p11_r, "y"), new IconFieldIterator(p21_r, "y"))), new IconAssign().over(new IconSingleton(dz_r), new IconOperation(IconOperators.minus).over(new IconFieldIterator(p11_r, "z"), new IconFieldIterator(p21_r, "z"))), new IconAssign().over(new IconSingleton(mag_r), new IconOperation(IconOperators.times).over(new IconSingleton(dt_r), (new IconOperation(IconOperators.powerOf).over((new IconOperation(IconOperators.plus).over(new IconOperation(IconOperators.plus).over(new IconOperation(IconOperators.times).over(new IconSingleton(dx_r), new IconSingleton(dx_r)), new IconOperation(IconOperators.times).over(new IconSingleton(dy_r), new IconSingleton(dy_r))), new IconOperation(IconOperators.times).over(new IconSingleton(dz_r), new IconSingleton(dz_r)))), new IconOperation(IconOperators.minusUnary).over(new IconValueIterator(1.5)))))), new IconAssign().over(new IconSingleton(b1m_r), new IconOperation(IconOperators.times).over(new IconIndexIterator(p1_r, IconValue.create(3)), new IconSingleton(mag_r))), new IconAssign().over(new IconSingleton(b2m_r), new IconOperation(IconOperators.times).over(new IconIndexIterator(p2_r, IconValue.create(3)), new IconSingleton(mag_r))), new IconAssign().augment(IconOperators.minus).over(new IconFieldIterator(v1_r, "x"), new IconOperation(IconOperators.times).over(new IconSingleton(dx_r), new IconSingleton(b2m_r))), new IconAssign().augment(IconOperators.minus).over(new IconFieldIterator(v1_r, "y"), new IconOperation(IconOperators.times).over(new IconSingleton(dy_r), new IconSingleton(b2m_r))), new IconAssign().augment(IconOperators.minus).over(new IconFieldIterator(v1_r, "z"), new IconOperation(IconOperators.times).over(new IconSingleton(dz_r), new IconSingleton(b2m_r))), new IconAssign().augment(IconOperators.plus).over(new IconFieldIterator(v2_r, "x"), new IconOperation(IconOperators.times).over(new IconSingleton(dx_r), new IconSingleton(b1m_r))), new IconAssign().augment(IconOperators.plus).over(new IconFieldIterator(v2_r, "y"), new IconOperation(IconOperators.times).over(new IconSingleton(dy_r), new IconSingleton(b1m_r))), new IconAssign().augment(IconOperators.plus).over(new IconFieldIterator(v2_r, "z"), new IconOperation(IconOperators.times).over(new IconSingleton(dz_r), new IconSingleton(b1m_r))))), new IconEvery(new IconAssign().over(new IconSingleton(s_r), new IconPromote(SYSTEM_r)), new IconSequence(new IconAssign().over(new IconSingleton(r_r), new IconIndexIterator(s_r, IconValue.create(1))), new IconAssign().over(new IconSingleton(v_r), new IconIndexIterator(s_r, IconValue.create(2))), new IconAssign().augment(IconOperators.plus).over(new IconIndexIterator(r_r, IconValue.create(1)), new IconOperation(IconOperators.times).over(new IconSingleton(dt_r), new IconFieldIterator(v_r, "x"))), new IconAssign().augment(IconOperators.plus).over(new IconIndexIterator(r_r, IconValue.create(2)), new IconOperation(IconOperators.times).over(new IconSingleton(dt_r), new IconFieldIterator(v_r, "y"))), new IconAssign().augment(IconOperators.plus).over(new IconIndexIterator(r_r, IconValue.create(3)), new IconOperation(IconOperators.times).over(new IconSingleton(dt_r), new IconFieldIterator(v_r, "z"))))))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_49.setCache(methodCache, "advance_m");
		body_49.setUnpackClosure(unpack_50).unpackArgs(args_48);
		return body_49;
	}
	@MMethod(name="report_energy", methodName="report_energy")
	@MLocal(name="p", reifiedName="p_r", type="")
	@MLocal(name="p11", reifiedName="p11_r", type="")
	@MLocal(name="p2", reifiedName="p2_r", type="")
	@MLocal(name="p21", reifiedName="p21_r", type="")
	@MLocal(name="dx", reifiedName="dx_r", type="")
	@MLocal(name="dy", reifiedName="dy_r", type="")
	@MLocal(name="dz", reifiedName="dz_r", type="")
	@MLocal(name="b", reifiedName="b_r", type="")
	@MLocal(name="v", reifiedName="v_r", type="")
	@MLocal(name="p1", reifiedName="p1_r", type="")
	@MLocal(name="e", reifiedName="e_r", type="")
	public IIconIterator report_energy (Object... args_52) {
		// Reuse method body
		IconIterator body_53 = methodCache.getFree("report_energy_m");
		if (body_53 != null) { return body_53.reset().unpackArgs(args_52); };
		// Locals
		IconVar p_r = new IconVar().local();
		IconVar p11_r = new IconVar().local();
		IconVar p2_r = new IconVar().local();
		IconVar p21_r = new IconVar().local();
		IconVar dx_r = new IconVar().local();
		IconVar dy_r = new IconVar().local();
		IconVar dz_r = new IconVar().local();
		IconVar b_r = new IconVar().local();
		IconVar v_r = new IconVar().local();
		IconVar p1_r = new IconVar().local();
		IconVar e_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_54 = (Object... params_55) -> {
			if (params_55 ==  null) { params_55 = IIconAtom.getEmptyArray(); };
			// Reset locals
			p_r.set(null);
			p11_r.set(null);
			p2_r.set(null);
			p21_r.set(null);
			dx_r.set(null);
			dy_r.set(null);
			dz_r.set(null);
			b_r.set(null);
			v_r.set(null);
			p1_r.set(null);
			return null;
		};
		// Method body
		body_53 = new IconSequence(new IconAssign().over(new IconSingleton(e_r), new IconValueIterator(0.0)), new IconEvery(new IconAssign().over(new IconSingleton(p_r), new IconPromote(PAIRS_r)), new IconSequence(new IconAssign().over(new IconSingleton(p1_r), new IconIndexIterator(p_r, IconValue.create(1))), new IconAssign().over(new IconSingleton(p11_r), new IconIndexIterator(p1_r, IconValue.create(1))), new IconAssign().over(new IconSingleton(p2_r), new IconIndexIterator(p_r, IconValue.create(2))), new IconAssign().over(new IconSingleton(p21_r), new IconIndexIterator(p2_r, IconValue.create(1))), new IconAssign().over(new IconSingleton(dx_r), new IconOperation(IconOperators.minus).over(new IconFieldIterator(p11_r, "x"), new IconFieldIterator(p21_r, "x"))), new IconAssign().over(new IconSingleton(dy_r), new IconOperation(IconOperators.minus).over(new IconFieldIterator(p11_r, "y"), new IconFieldIterator(p21_r, "y"))), new IconAssign().over(new IconSingleton(dz_r), new IconOperation(IconOperators.minus).over(new IconFieldIterator(p11_r, "z"), new IconFieldIterator(p21_r, "z"))), new IconAssign().augment(IconOperators.minus).over(new IconSingleton(e_r), new IconOperation(IconOperators.division).over(new IconOperation(IconOperators.times).over(new IconIndexIterator(p1_r, IconValue.create(3)), new IconIndexIterator(p2_r, IconValue.create(3))), (new IconOperation(IconOperators.powerOf).over((new IconOperation(IconOperators.plus).over(new IconOperation(IconOperators.times).over(new IconSingleton(dx_r), new IconSingleton(dx_r)), new IconOperation(IconOperators.times).over(new IconSingleton(dy_r), new IconSingleton(dy_r)), new IconOperation(IconOperators.times).over(new IconSingleton(dz_r), new IconSingleton(dz_r)))), new IconValueIterator(0.5))))))), new IconEvery(new IconAssign().over(new IconSingleton(b_r), new IconPromote(SYSTEM_r)), new IconSequence(new IconAssign().over(new IconSingleton(v_r), new IconIndexIterator(b_r, IconValue.create(2))), new IconAssign().augment(IconOperators.plus).over(new IconSingleton(e_r), new IconOperation(IconOperators.division).over(new IconOperation(IconOperators.times).over(new IconIndexIterator(b_r, IconValue.create(3)), (new IconOperation(IconOperators.plus).over(new IconOperation(IconOperators.plus).over(new IconOperation(IconOperators.times).over(new IconFieldIterator(v_r, "x"), new IconFieldIterator(v_r, "x")), new IconOperation(IconOperators.times).over(new IconFieldIterator(v_r, "y"), new IconFieldIterator(v_r, "y"))), new IconOperation(IconOperators.times).over(new IconFieldIterator(v_r, "z"), new IconFieldIterator(v_r, "z"))))), new IconValueIterator(2.))))), new IconInvokeIterator(()-> ((VariadicFunction) writeln).apply(e_r.deref())), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_53.setCache(methodCache, "report_energy_m");
		body_53.setUnpackClosure(unpack_54).unpackArgs(args_52);
		return body_53;
	}
	@MMethod(name="offset_momentum", methodName="offset_momentum")
	@MParameter(name="ref", reifiedName="ref_r", type="")
	@MLocal(name="s", reifiedName="s_r", type="")
	@MLocal(name="v", reifiedName="v_r", type="")
	@MLocal(name="m", reifiedName="m_r", type="")
	@MLocal(name="px", reifiedName="px_r", type="")
	@MLocal(name="py", reifiedName="py_r", type="")
	@MLocal(name="pz", reifiedName="pz_r", type="")
	public IIconIterator offset_momentum (Object... args_56) {
		// Reuse method body
		IconIterator body_57 = methodCache.getFree("offset_momentum_m");
		if (body_57 != null) { return body_57.reset().unpackArgs(args_56); };
		// Reified parameters
		IconVar ref_r = new IconVar().local();
		// Locals
		IconVar s_r = new IconVar().local();
		IconVar v_r = new IconVar().local();
		IconVar m_r = new IconVar().local();
		IconVar px_r = new IconVar().local();
		IconVar py_r = new IconVar().local();
		IconVar pz_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_58 = (Object... params_59) -> {
			if (params_59 ==  null) { params_59 = IIconAtom.getEmptyArray(); };
			ref_r.set((params_59.length > 0) ? params_59[0] : null);
			// Reset locals
			s_r.set(null);
			v_r.set(null);
			m_r.set(null);
			return null;
		};
		// Method body
		body_57 = new IconSequence(new IconAssign().over(new IconSingleton(px_r), new IconValueIterator(0)), new IconAssign().over(new IconSingleton(py_r), new IconValueIterator(0)), new IconAssign().over(new IconSingleton(pz_r), new IconValueIterator(0.0)), new IconEvery(new IconAssign().over(new IconSingleton(s_r), new IconPromote(SYSTEM_r)), new IconSequence(new IconAssign().over(new IconSingleton(v_r), new IconIndexIterator(s_r, IconValue.create(2))), new IconAssign().over(new IconSingleton(m_r), new IconIndexIterator(s_r, IconValue.create(3))), new IconAssign().augment(IconOperators.minus).over(new IconSingleton(px_r), new IconOperation(IconOperators.times).over(new IconFieldIterator(v_r, "x"), new IconSingleton(m_r))), new IconAssign().augment(IconOperators.minus).over(new IconSingleton(py_r), new IconOperation(IconOperators.times).over(new IconFieldIterator(v_r, "y"), new IconSingleton(m_r))), new IconAssign().augment(IconOperators.minus).over(new IconSingleton(pz_r), new IconOperation(IconOperators.times).over(new IconFieldIterator(v_r, "z"), new IconSingleton(m_r))))), new IconAssign().over(new IconSingleton(v_r), new IconIndexIterator(ref_r, IconValue.create(2))), new IconAssign().over(new IconSingleton(m_r), new IconIndexIterator(ref_r, IconValue.create(3))), new IconAssign().over(new IconFieldIterator(v_r, "x"), new IconOperation(IconOperators.division).over(new IconSingleton(px_r), new IconSingleton(m_r))), new IconAssign().over(new IconFieldIterator(v_r, "y"), new IconOperation(IconOperators.division).over(new IconSingleton(py_r), new IconSingleton(m_r))), new IconAssign().over(new IconFieldIterator(v_r, "z"), new IconOperation(IconOperators.division).over(new IconSingleton(pz_r), new IconSingleton(m_r))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_57.setCache(methodCache, "offset_momentum_m");
		body_57.setUnpackClosure(unpack_58).unpackArgs(args_56);
		return body_57;
	}
	@MMethod(name="run_nbody", methodName="run_nbody")
	@MParameter(name="argv", reifiedName="argv_r", type="")
	@MLocal(name="ref", reifiedName="ref_r", type="")
	public IIconIterator run_nbody (Object... args_60) {
		// Reuse method body
		IconIterator body_61 = methodCache.getFree("run_nbody_m");
		if (body_61 != null) { return body_61.reset().unpackArgs(args_60); };
		// Reified parameters
		IconVar argv_r = new IconVar().local();
		// Temporaries
		IconTmp x_10_r = new IconTmp();
		IconTmp x_8_r = new IconTmp();
		IconTmp x_9_r = new IconTmp();
		IconTmp x_19_r = new IconTmp();
		IconTmp x_13_r = new IconTmp();
		IconTmp x_11_r = new IconTmp();
		IconTmp x_12_r = new IconTmp();
		IconTmp x_17_r = new IconTmp();
		IconTmp x_14_r = new IconTmp();
		IconTmp x_15_r = new IconTmp();
		IconTmp x_16_r = new IconTmp();
		IconTmp x_18_r = new IconTmp();
		IconTmp x_27_r = new IconTmp();
		IconTmp x_21_r = new IconTmp();
		IconTmp x_20_r = new IconTmp();
		IconTmp x_25_r = new IconTmp();
		IconTmp x_22_r = new IconTmp();
		IconTmp x_23_r = new IconTmp();
		IconTmp x_24_r = new IconTmp();
		IconTmp x_26_r = new IconTmp();
		IconTmp x_36_r = new IconTmp();
		IconTmp x_30_r = new IconTmp();
		IconTmp x_28_r = new IconTmp();
		IconTmp x_29_r = new IconTmp();
		IconTmp x_34_r = new IconTmp();
		IconTmp x_31_r = new IconTmp();
		IconTmp x_32_r = new IconTmp();
		IconTmp x_33_r = new IconTmp();
		IconTmp x_35_r = new IconTmp();
		IconTmp x_44_r = new IconTmp();
		IconTmp x_38_r = new IconTmp();
		IconTmp x_37_r = new IconTmp();
		IconTmp x_42_r = new IconTmp();
		IconTmp x_39_r = new IconTmp();
		IconTmp x_40_r = new IconTmp();
		IconTmp x_41_r = new IconTmp();
		IconTmp x_43_r = new IconTmp();
		IconTmp x_45_r = new IconTmp();
		IconTmp x_46_r = new IconTmp();
		IconTmp x_47_r = new IconTmp();
		// Locals
		IconVar ref_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_62 = (Object... params_63) -> {
			if (params_63 ==  null) { params_63 = IIconAtom.getEmptyArray(); };
			argv_r.set((params_63.length > 0) ? params_63[0] : null);
			return null;
		};
		// Method body
		body_61 = new IconSequence(new IconAssign().over(new IconSingleton(ref_r), new IconValueIterator("sun")), new IconAssign().over(new IconSingleton(PI_r), new IconValueIterator(3.14159265358979323)), new IconAssign().over(new IconSingleton(SOLAR_MASS_r), new IconOperation(IconOperators.times).over(new IconOperation(IconOperators.times).over(new IconValueIterator(4), new IconSingleton(PI_r)), new IconSingleton(PI_r))), new IconAssign().over(new IconSingleton(DAYS_PER_YEAR_r), new IconValueIterator(365.24)), new IconAssign().over(new IconSingleton(BODIES_r), new IconProduct(new IconProduct(new IconIn(x_10_r, new IconProduct(new IconProduct(new IconIn(x_8_r, new IconInvokeIterator(()-> ((VariadicFunction) xyz.xyz).apply(IconNumber.create(0.0), IconNumber.create(0.0), IconNumber.create(0.0)))), new IconIn(x_9_r, new IconInvokeIterator(()-> ((VariadicFunction) xyz.xyz).apply(IconNumber.create(0.0), IconNumber.create(0.0), IconNumber.create(0.0))))), IconVarIterator.createAsList(()-> new IconList(x_8_r.deref(), x_9_r.deref(), SOLAR_MASS)))), new IconProduct(new IconIn(x_19_r, new IconProduct(new IconProduct(new IconIn(x_13_r, new IconProduct(new IconProduct(new IconIn(x_11_r, new IconOperation(IconOperators.minusUnary).over(new IconValueIterator(1.16032004402742839e+00))), new IconIn(x_12_r, new IconOperation(IconOperators.minusUnary).over(new IconValueIterator(1.03622044471123109e-01)))), new IconInvokeIterator(()-> ((VariadicFunction) xyz.xyz).apply(IconNumber.create(4.84143144246472090e+00), x_11_r.deref(), x_12_r.deref())))), new IconProduct(new IconIn(x_17_r, new IconProduct(new IconProduct(new IconIn(x_14_r, new IconOperation(IconOperators.times).over(new IconValueIterator(1.66007664274403694e-03), new IconSingleton(DAYS_PER_YEAR_r))), new IconProduct(new IconIn(x_15_r, new IconOperation(IconOperators.times).over(new IconValueIterator(7.69901118419740425e-03), new IconSingleton(DAYS_PER_YEAR_r))), new IconIn(x_16_r, new IconOperation(IconOperators.times).over(new IconOperation(IconOperators.minusUnary).over(new IconValueIterator(6.90460016972063023e-05)), new IconSingleton(DAYS_PER_YEAR_r))))), new IconInvokeIterator(()-> ((VariadicFunction) xyz.xyz).apply(x_14_r.deref(), x_15_r.deref(), x_16_r.deref())))), new IconIn(x_18_r, new IconOperation(IconOperators.times).over(new IconValueIterator(9.54791938424326609e-04), new IconSingleton(SOLAR_MASS_r))))), IconVarIterator.createAsList(()-> new IconList(x_13_r.deref(), x_17_r.deref(), x_18_r.deref())))), new IconProduct(new IconIn(x_27_r, new IconProduct(new IconProduct(new IconIn(x_21_r, new IconProduct(new IconIn(x_20_r, new IconOperation(IconOperators.minusUnary).over(new IconValueIterator(4.03523417114321381e-01))), new IconInvokeIterator(()-> ((VariadicFunction) xyz.xyz).apply(IconNumber.create(8.34336671824457987e+00), IconNumber.create(4.12479856412430479e+00), x_20_r.deref())))), new IconProduct(new IconIn(x_25_r, new IconProduct(new IconProduct(new IconIn(x_22_r, new IconOperation(IconOperators.times).over(new IconOperation(IconOperators.minusUnary).over(new IconValueIterator(2.76742510726862411e-03)), new IconSingleton(DAYS_PER_YEAR_r))), new IconProduct(new IconIn(x_23_r, new IconOperation(IconOperators.times).over(new IconValueIterator(4.99852801234917238e-03), new IconSingleton(DAYS_PER_YEAR_r))), new IconIn(x_24_r, new IconOperation(IconOperators.times).over(new IconValueIterator(2.30417297573763929e-05), new IconSingleton(DAYS_PER_YEAR_r))))), new IconInvokeIterator(()-> ((VariadicFunction) xyz.xyz).apply(x_22_r.deref(), x_23_r.deref(), x_24_r.deref())))), new IconIn(x_26_r, new IconOperation(IconOperators.times).over(new IconValueIterator(2.85885980666130812e-04), new IconSingleton(SOLAR_MASS_r))))), IconVarIterator.createAsList(()-> new IconList(x_21_r.deref(), x_25_r.deref(), x_26_r.deref())))), new IconProduct(new IconIn(x_36_r, new IconProduct(new IconProduct(new IconIn(x_30_r, new IconProduct(new IconProduct(new IconIn(x_28_r, new IconOperation(IconOperators.minusUnary).over(new IconValueIterator(1.51111514016986312e+01))), new IconIn(x_29_r, new IconOperation(IconOperators.minusUnary).over(new IconValueIterator(2.23307578892655734e-01)))), new IconInvokeIterator(()-> ((VariadicFunction) xyz.xyz).apply(IconNumber.create(1.28943695621391310e+01), x_28_r.deref(), x_29_r.deref())))), new IconProduct(new IconIn(x_34_r, new IconProduct(new IconProduct(new IconIn(x_31_r, new IconOperation(IconOperators.times).over(new IconValueIterator(2.96460137564761618e-03), new IconSingleton(DAYS_PER_YEAR_r))), new IconProduct(new IconIn(x_32_r, new IconOperation(IconOperators.times).over(new IconValueIterator(2.37847173959480950e-03), new IconSingleton(DAYS_PER_YEAR_r))), new IconIn(x_33_r, new IconOperation(IconOperators.times).over(new IconOperation(IconOperators.minusUnary).over(new IconValueIterator(2.96589568540237556e-05)), new IconSingleton(DAYS_PER_YEAR_r))))), new IconInvokeIterator(()-> ((VariadicFunction) xyz.xyz).apply(x_31_r.deref(), x_32_r.deref(), x_33_r.deref())))), new IconIn(x_35_r, new IconOperation(IconOperators.times).over(new IconValueIterator(4.36624404335156298e-05), new IconSingleton(SOLAR_MASS_r))))), IconVarIterator.createAsList(()-> new IconList(x_30_r.deref(), x_34_r.deref(), x_35_r.deref())))), new IconIn(x_44_r, new IconProduct(new IconProduct(new IconIn(x_38_r, new IconProduct(new IconIn(x_37_r, new IconOperation(IconOperators.minusUnary).over(new IconValueIterator(2.59193146099879641e+01))), new IconInvokeIterator(()-> ((VariadicFunction) xyz.xyz).apply(IconNumber.create(1.53796971148509165e+01), x_37_r.deref(), IconNumber.create(1.79258772950371181e-01))))), new IconProduct(new IconIn(x_42_r, new IconProduct(new IconProduct(new IconIn(x_39_r, new IconOperation(IconOperators.times).over(new IconValueIterator(2.68067772490389322e-03), new IconSingleton(DAYS_PER_YEAR_r))), new IconProduct(new IconIn(x_40_r, new IconOperation(IconOperators.times).over(new IconValueIterator(1.62824170038242295e-03), new IconSingleton(DAYS_PER_YEAR_r))), new IconIn(x_41_r, new IconOperation(IconOperators.times).over(new IconOperation(IconOperators.minusUnary).over(new IconValueIterator(9.51592254519715870e-05)), new IconSingleton(DAYS_PER_YEAR_r))))), new IconInvokeIterator(()-> ((VariadicFunction) xyz.xyz).apply(x_39_r.deref(), x_40_r.deref(), x_41_r.deref())))), new IconIn(x_43_r, new IconOperation(IconOperators.times).over(new IconValueIterator(5.15138902046611451e-05), new IconSingleton(SOLAR_MASS_r))))), IconVarIterator.createAsList(()-> new IconList(x_38_r.deref(), x_42_r.deref(), x_43_r.deref())))))))), new IconInvokeIterator(()-> ((VariadicFunction) table).apply("sun", x_10_r.deref(), "jupiter", x_19_r.deref(), "saturn", x_27_r.deref(), "uranus", x_36_r.deref(), "neptune", x_44_r.deref())))), new IconAssign().over(new IconSingleton(SYSTEM_r), IconVarIterator.createAsList(()-> new IconList())), new IconEvery(new IconProduct(new IconIn(x_45_r, new IconPromote(BODIES_r)), new IconInvokeIterator(()-> ((VariadicFunction) put).apply(SYSTEM, x_45_r.deref())))), new IconAssign().over(new IconSingleton(PAIRS_r), new IconInvokeIterator(()-> ((VariadicFunction) combinations).apply(SYSTEM))), new IconProduct(new IconIn(x_46_r, new IconIndexIterator(BODIES_r, ref_r)), new IconInvokeIterator(()-> ((VariadicFunction) offset_momentum).apply(x_46_r.deref()))), new IconInvokeIterator(()-> ((VariadicFunction) report_energy).apply()), new IconProduct(new IconIn(x_47_r, new IconIndexIterator(argv_r, IconValue.create(1))), new IconInvokeIterator(()-> ((VariadicFunction) advance).apply(IconNumber.create(0.01), x_47_r.deref()))), new IconInvokeIterator(()-> ((VariadicFunction) report_energy).apply()), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_61.setCache(methodCache, "run_nbody_m");
		body_61.setUnpackClosure(unpack_62).unpackArgs(args_60);
		return body_61;
	}
	@MMethod(name="main", methodName="main_m")
	@MParameter(name="argv", reifiedName="argv_r", type="", isVararg=true)
	private IIconIterator main_m (Object... args_64) {
		// Reuse method body
		IconIterator body_65 = methodCache.getFree("main_m");
		if (body_65 != null) { return body_65.reset().unpackArgs(args_64); };
		// Reified parameters
		IconVar argv_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_66 = (Object... params_67) -> {
			if (params_67 ==  null) { params_67 = IIconAtom.getEmptyArray(); };
			argv_r.set((params_67.length > 0) ? Arrays.asList(params_67).subList(0, params_67.length) : new ArrayList());
			return null;
		};
		// Method body
		body_65 = new IconSequence(new IconInvokeIterator(()-> ((VariadicFunction) run_nbody).apply(argv_r.deref())), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_65.setCache(methodCache, "main_m");
		body_65.setUnpackClosure(unpack_66).unpackArgs(args_64);
		return body_65;
	}
	// Static main method
	public static void main(String... args_68) {
		Nbody c = new Nbody(); VariadicFunction m = (VariadicFunction) c.main;
		IconCoExpression.activate(null, null, new IconCoExpression(
		 (Object... coexpr) -> {
			return ((IIconIterator) m.apply((Object[]) args_68)); },
		 () -> { return IconList.createArray(); } ));
	}
}
