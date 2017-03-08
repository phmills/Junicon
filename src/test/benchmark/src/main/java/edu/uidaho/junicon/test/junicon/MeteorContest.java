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
class xy {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Constructor fields
	@MField(name="x", reifiedName="x_r", type="", isConstructorField=true)
	public Object x;
	@MField(name="y", reifiedName="y_r", type="", isConstructorField=true)
	public Object y;
	// Reified constructor fields
	private IconVar x_r = new IconVar(()-> x, (rhs)-> x=rhs);
	private IconVar y_r = new IconVar(()-> y, (rhs)-> y=rhs);
	// Constructors
	public xy() { ;}
	public xy(Object x, Object y) {
		this.x = x;
		this.y = y;
	}
	// Static variadic constructor
	public static VariadicFunction xy = (Object... args) -> {
		if (args ==  null) { args = IIconAtom.getEmptyArray(); };
		return new xy((args.length > 0) ? args[0] : null, (args.length > 1) ? args[1] : null);
	};
}
public class MeteorContest {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Static method initializer cache
	private static ConcurrentHashMap<String,Object> initialMethodCache = new ConcurrentHashMap();
	// Method references
	@MMethodRef(name="findFreeCell", methodName="findFreeCell")
	public Object findFreeCell = (VariadicFunction) this::findFreeCell;
	@MMethodRef(name="floodFill", methodName="floodFill")
	public Object floodFill = (VariadicFunction) this::floodFill;
	@MMethodRef(name="noIslands", methodName="noIslands")
	public Object noIslands = (VariadicFunction) this::noIslands;
	@MMethodRef(name="getBitmask", methodName="getBitmask")
	public Object getBitmask = (VariadicFunction) this::getBitmask;
	@MMethodRef(name="allBitmasks", methodName="allBitmasks")
	public Object allBitmasks = (VariadicFunction) this::allBitmasks;
	@MMethodRef(name="generateBitmasks", methodName="generateBitmasks")
	public Object generateBitmasks = (VariadicFunction) this::generateBitmasks;
	@MMethodRef(name="solveCell", methodName="solveCell")
	public Object solveCell = (VariadicFunction) this::solveCell;
	@MMethodRef(name="solve", methodName="solve")
	public Object solve = (VariadicFunction) this::solve;
	@MMethodRef(name="stringOfMasks", methodName="stringOfMasks")
	public Object stringOfMasks = (VariadicFunction) this::stringOfMasks;
	@MMethodRef(name="inverse", methodName="inverse")
	public Object inverse = (VariadicFunction) this::inverse;
	@MMethodRef(name="printSolution", methodName="printSolution")
	public Object printSolution = (VariadicFunction) this::printSolution;
	@MMethodRef(name="valid", methodName="valid")
	public Object valid = (VariadicFunction) this::valid;
	@MMethodRef(name="legal", methodName="legal")
	public Object legal = (VariadicFunction) this::legal;
	@MMethodRef(name="zerocount", methodName="zerocount")
	public Object zerocount = (VariadicFunction) this::zerocount;
	@MMethodRef(name="move_E", methodName="move_E")
	public Object move_E = (VariadicFunction) this::move_E;
	@MMethodRef(name="move_W", methodName="move_W")
	public Object move_W = (VariadicFunction) this::move_W;
	@MMethodRef(name="move_NE", methodName="move_NE")
	public Object move_NE = (VariadicFunction) this::move_NE;
	@MMethodRef(name="move_NW", methodName="move_NW")
	public Object move_NW = (VariadicFunction) this::move_NW;
	@MMethodRef(name="move_SE", methodName="move_SE")
	public Object move_SE = (VariadicFunction) this::move_SE;
	@MMethodRef(name="move_SW", methodName="move_SW")
	public Object move_SW = (VariadicFunction) this::move_SW;
	@MMethodRef(name="run_meteorcontest", methodName="run_meteorcontest")
	public Object run_meteorcontest = (VariadicFunction) this::run_meteorcontest;
	@MMethodRef(name="main", methodName="main_m")
	public Object main = (VariadicFunction) this::main_m;
	// Constructors
	public MeteorContest() {
		;
	}
	// Static variadic constructor
	public static VariadicFunction MeteorContest = (Object... args) -> {
		return new MeteorContest();
	};
	// Locals
	@MField(name="width", reifiedName="width_r", type="", isConstructorField=false)
	public Object width;
	private IconVar width_r = new IconVar(()-> width, (rhs)-> width=rhs);
	@MField(name="height", reifiedName="height_r", type="", isConstructorField=false)
	public Object height;
	private IconVar height_r = new IconVar(()-> height, (rhs)-> height=rhs);
	@MField(name="masksAtCell", reifiedName="masksAtCell_r", type="", isConstructorField=false)
	public Object masksAtCell;
	private IconVar masksAtCell_r = new IconVar(()-> masksAtCell, (rhs)-> masksAtCell=rhs);
	@MField(name="solutions", reifiedName="solutions_r", type="", isConstructorField=false)
	public Object solutions;
	private IconVar solutions_r = new IconVar(()-> solutions, (rhs)-> solutions=rhs);
	@MField(name="masks", reifiedName="masks_r", type="", isConstructorField=false)
	public Object masks;
	private IconVar masks_r = new IconVar(()-> masks, (rhs)-> masks=rhs);
	@MField(name="directions", reifiedName="directions_r", type="", isConstructorField=false)
	public Object directions;
	private IconVar directions_r = new IconVar(()-> directions, (rhs)-> directions=rhs);
	@MField(name="rotate", reifiedName="rotate_r", type="", isConstructorField=false)
	public Object rotate;
	private IconVar rotate_r = new IconVar(()-> rotate, (rhs)-> rotate=rhs);
	@MField(name="flip", reifiedName="flip_r", type="", isConstructorField=false)
	public Object flip;
	private IconVar flip_r = new IconVar(()-> flip, (rhs)-> flip=rhs);
	@MField(name="moves", reifiedName="moves_r", type="", isConstructorField=false)
	public Object moves;
	private IconVar moves_r = new IconVar(()-> moves, (rhs)-> moves=rhs);
	@MField(name="pieces", reifiedName="pieces_r", type="", isConstructorField=false)
	public Object pieces;
	private IconVar pieces_r = new IconVar(()-> pieces, (rhs)-> pieces=rhs);
	// Method static variables
	private static Object zeros_in_4bits_s;
	private static IconVar zeros_in_4bits_s_r = new IconVar(()-> zeros_in_4bits_s, (rhs)-> zeros_in_4bits_s=rhs).local();
	// Methods
	@MMethod(name="findFreeCell", methodName="findFreeCell")
	@MParameter(name="board", reifiedName="board_r", type="")
	@MLocal(name="bitposn", reifiedName="bitposn_r", type="")
	@MLocal(name="y", reifiedName="y_r", type="")
	@MLocal(name="x", reifiedName="x_r", type="")
	public IIconIterator findFreeCell (Object... args) {
		// Reuse method body
		IconIterator body = methodCache.getFree("findFreeCell_m");
		if (body != null) { return body.reset().unpackArgs(args); };
		// Reified parameters
		IconVar board_r = new IconVar().local();
		// Temporaries
		IconTmp x_0_r = new IconTmp();
		// Locals
		IconVar bitposn_r = new IconVar().local();
		IconVar y_r = new IconVar().local();
		IconVar x_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack = (Object... params) -> {
			if (params ==  null) { params = IIconAtom.getEmptyArray(); };
			board_r.set((params.length > 0) ? params[0] : null);
			// Reset locals
			bitposn_r.set(null);
			y_r.set(null);
			x_r.set(null);
			return null;
		};
		// Method body
		body = new IconSequence(new IconAssign().over(new IconSingleton(bitposn_r), new IconValueIterator(1)), new IconEvery(new IconAssign().over(new IconSingleton(y_r), new IconProduct(new IconIn(x_0_r, new IconOperation(IconOperators.minus).over(new IconSingleton(height_r), new IconValueIterator(1))), new IconToIterator(IconValue.create(0), x_0_r))), new IconBlock( () -> {
			// Temporaries
			IconTmp x_1_r = new IconTmp();
			return new IconEvery(new IconAssign().over(new IconSingleton(x_r), new IconProduct(new IconIn(x_1_r, new IconOperation(IconOperators.minus).over(new IconSingleton(width_r), new IconValueIterator(1))), new IconToIterator(IconValue.create(0), x_1_r))), new IconSequence(new IconIf(new IconOperation(IconOperators.sameNumberAs).over(new IconInvokeIterator(()-> ((VariadicFunction) iand).apply(board_r.deref(), bitposn_r.deref())), new IconValueIterator(0)), new IconReturn(new IconInvokeIterator(()-> ((VariadicFunction) xy.xy).apply(x_r.deref(), y_r.deref())))), new IconAssign().over(new IconSingleton(bitposn_r), new IconInvokeIterator(()-> ((VariadicFunction) ishift).apply(bitposn_r.deref(), IconNumber.create(1))))));
		}
 )), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body.setCache(methodCache, "findFreeCell_m");
		body.setUnpackClosure(unpack).unpackArgs(args);
		return body;
	}
	@MMethod(name="floodFill", methodName="floodFill")
	@MParameter(name="board", reifiedName="board_r", type="")
	@MParameter(name="coords", reifiedName="coords_r", type="")
	@MLocal(name="x", reifiedName="x_r", type="")
	@MLocal(name="y", reifiedName="y_r", type="")
	@MLocal(name="bitposn", reifiedName="bitposn_r", type="")
	public IIconIterator floodFill (Object... args_65) {
		// Reuse method body
		IconIterator body_66 = methodCache.getFree("floodFill_m");
		if (body_66 != null) { return body_66.reset().unpackArgs(args_65); };
		// Reified parameters
		IconVar board_r = new IconVar().local();
		IconVar coords_r = new IconVar().local();
		// Temporaries
		IconTmp x_2_r = new IconTmp();
		IconTmp x_5_r = new IconTmp();
		IconTmp x_4_r = new IconTmp();
		IconTmp x_3_r = new IconTmp();
		// Locals
		IconVar x_r = new IconVar().local();
		IconVar y_r = new IconVar().local();
		IconVar bitposn_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_67 = (Object... params_68) -> {
			if (params_68 ==  null) { params_68 = IIconAtom.getEmptyArray(); };
			board_r.set((params_68.length > 0) ? params_68[0] : null);
			coords_r.set((params_68.length > 1) ? params_68[1] : null);
			// Reset locals
			x_r.set(null);
			y_r.set(null);
			bitposn_r.set(null);
			return null;
		};
		// Method body
		body_66 = new IconSequence(new IconAssign().over(new IconSingleton(x_r), new IconFieldIterator(coords_r, "x")), new IconAssign().over(new IconSingleton(y_r), new IconFieldIterator(coords_r, "y")), new IconAssign().over(new IconSingleton(bitposn_r), new IconProduct(new IconIn(x_2_r, new IconOperation(IconOperators.plus).over(new IconSingleton(x_r), new IconOperation(IconOperators.times).over(new IconSingleton(width_r), new IconSingleton(y_r)))), new IconInvokeIterator(()-> ((VariadicFunction) ishift).apply(IconNumber.create(1), x_2_r.deref())))), new IconIf(new IconConcat((new IconNot(new IconInvokeIterator(()-> ((VariadicFunction) valid).apply(x_r.deref(), y_r.deref())))), (new IconOperation(IconOperators.notSameNumberAs).over(new IconInvokeIterator(()-> ((VariadicFunction) iand).apply(board_r.deref(), bitposn_r.deref())), new IconValueIterator(0)))), new IconReturn(new IconSingleton(board_r))), new IconAssign().over(new IconSingleton(board_r), new IconInvokeIterator(()-> ((VariadicFunction) ior).apply(board_r.deref(), bitposn_r.deref()))), new IconEvery(new IconAssign().over(new IconSingleton(board_r), new IconProduct(new IconIn(x_5_r, new IconProduct(new IconIn(x_4_r, new IconProduct(new IconIn(x_3_r, (new IconPromote(moves_r))), new IconInvokeIterator(()-> ((VariadicFunction) x_3_r.deref()).apply(x_r.deref(), y_r.deref())))), new IconInvokeIterator(()-> floodFill(board_r.deref(), x_4_r.deref())))), new IconInvokeIterator(()-> ((VariadicFunction) ior).apply(board_r.deref(), x_5_r.deref()))))), new IconReturn(new IconSingleton(board_r)), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_66.setCache(methodCache, "floodFill_m");
		body_66.setUnpackClosure(unpack_67).unpackArgs(args_65);
		return body_66;
	}
	@MMethod(name="noIslands", methodName="noIslands")
	@MParameter(name="mask", reifiedName="mask_r", type="")
	@MLocal(name="zeroes", reifiedName="zeroes_r", type="")
	@MLocal(name="new_zeroes", reifiedName="new_zeroes_r", type="")
	public IIconIterator noIslands (Object... args_69) {
		// Reuse method body
		IconIterator body_70 = methodCache.getFree("noIslands_m");
		if (body_70 != null) { return body_70.reset().unpackArgs(args_69); };
		// Reified parameters
		IconVar mask_r = new IconVar().local();
		// Locals
		IconVar zeroes_r = new IconVar().local();
		IconVar new_zeroes_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_71 = (Object... params_72) -> {
			if (params_72 ==  null) { params_72 = IIconAtom.getEmptyArray(); };
			mask_r.set((params_72.length > 0) ? params_72[0] : null);
			// Reset locals
			zeroes_r.set(null);
			new_zeroes_r.set(null);
			return null;
		};
		// Method body
		body_70 = new IconSequence(new IconAssign().over(new IconSingleton(zeroes_r), new IconInvokeIterator(()-> ((VariadicFunction) zerocount).apply(mask_r.deref()))), new IconIf(new IconOperation(IconOperators.lessThan).over(new IconSingleton(zeroes_r), new IconValueIterator(5)), new IconReturn(new IconFail())), new IconWhile(new IconOperation(IconOperators.notSameNumberAs).over(new IconSingleton(mask_r), new IconValueIterator("16r3FFFFFFFFFFFF", -1)), new IconBlock( () -> {
			// Temporaries
			IconTmp x_6_r = new IconTmp();
			return new IconSequence(new IconAssign().over(new IconSingleton(mask_r), new IconProduct(new IconIn(x_6_r, new IconInvokeIterator(()-> ((VariadicFunction) findFreeCell).apply(mask_r.deref()))), new IconInvokeIterator(()-> ((VariadicFunction) floodFill).apply(mask_r.deref(), x_6_r.deref())))), new IconAssign().over(new IconSingleton(new_zeroes_r), new IconInvokeIterator(()-> ((VariadicFunction) zerocount).apply(mask_r.deref()))), new IconIf(new IconOperation(IconOperators.lessThan).over((new IconOperation(IconOperators.minus).over(new IconSingleton(zeroes_r), new IconSingleton(new_zeroes_r))), new IconValueIterator(5)), new IconReturn(new IconFail())), new IconAssign().over(new IconSingleton(zeroes_r), new IconSingleton(new_zeroes_r)));
		}
 )), new IconReturn(new IconNullIterator()), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_70.setCache(methodCache, "noIslands_m");
		body_70.setUnpackClosure(unpack_71).unpackArgs(args_69);
		return body_70;
	}
	@MMethod(name="getBitmask", methodName="getBitmask")
	@MParameter(name="x", reifiedName="x_r", type="")
	@MParameter(name="y", reifiedName="y_r", type="")
	@MParameter(name="piece", reifiedName="piece_r", type="")
	@MLocal(name="mask", reifiedName="mask_r", type="")
	@MLocal(name="cell", reifiedName="cell_r", type="")
	@MLocal(name="results", reifiedName="results_r", type="")
	public IIconIterator getBitmask (Object... args_73) {
		// Reuse method body
		IconIterator body_74 = methodCache.getFree("getBitmask_m");
		if (body_74 != null) { return body_74.reset().unpackArgs(args_73); };
		// Reified parameters
		IconVar x_r = new IconVar().local();
		IconVar y_r = new IconVar().local();
		IconVar piece_r = new IconVar().local();
		// Temporaries
		IconTmp x_7_r = new IconTmp();
		// Locals
		IconVar mask_r = new IconVar().local();
		IconVar cell_r = new IconVar().local();
		IconVar results_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_75 = (Object... params_76) -> {
			if (params_76 ==  null) { params_76 = IIconAtom.getEmptyArray(); };
			x_r.set((params_76.length > 0) ? params_76[0] : null);
			y_r.set((params_76.length > 1) ? params_76[1] : null);
			piece_r.set((params_76.length > 2) ? params_76[2] : null);
			// Reset locals
			mask_r.set(null);
			cell_r.set(null);
			results_r.set(null);
			return null;
		};
		// Method body
		body_74 = new IconSequence(new IconAssign().over(new IconSingleton(mask_r), new IconProduct(new IconIn(x_7_r, (new IconOperation(IconOperators.plus).over(new IconSingleton(x_r), new IconOperation(IconOperators.times).over(new IconSingleton(width_r), new IconSingleton(y_r))))), new IconInvokeIterator(()-> ((VariadicFunction) ishift).apply(IconNumber.create(1), x_7_r.deref())))), new IconEvery(new IconAssign().over(new IconSingleton(cell_r), new IconPromote(piece_r)), new IconBlock( () -> {
			// Temporaries
			IconTmp i_7_r = new IconTmp();
			IconTmp x_9_r = new IconTmp();
			IconTmp x_8_r = new IconTmp();
			return new IconSequence(new IconAssign().over(new IconSingleton(results_r), new IconProduct(new IconIn(i_7_r, new IconIndexIterator(moves_r, cell_r)), new IconInvokeIterator(()-> ((VariadicFunction) i_7_r.deref()).apply(x_r.deref(), y_r.deref())))), new IconAssign().over(new IconSingleton(x_r), new IconFieldIterator(results_r, "x")), new IconAssign().over(new IconSingleton(y_r), new IconFieldIterator(results_r, "y")), new IconIf(new IconProduct((new IconOperation(IconOperators.lessThan).over(new IconOperation(IconOperators.lessThanOrEquals).over(new IconValueIterator(0), new IconSingleton(x_r)), new IconSingleton(width_r))), (new IconOperation(IconOperators.lessThan).over(new IconOperation(IconOperators.lessThanOrEquals).over(new IconValueIterator(0), new IconSingleton(y_r)), new IconSingleton(height_r)))), new IconAssign().over(new IconSingleton(mask_r), new IconProduct(new IconIn(x_9_r, new IconProduct(new IconIn(x_8_r, (new IconOperation(IconOperators.plus).over(new IconSingleton(x_r), new IconOperation(IconOperators.times).over(new IconSingleton(width_r), new IconSingleton(y_r))))), new IconInvokeIterator(()-> ((VariadicFunction) ishift).apply(IconNumber.create(1), x_8_r.deref())))), new IconInvokeIterator(()-> ((VariadicFunction) ior).apply(mask_r.deref(), x_9_r.deref())))), new IconReturn(new IconFail())));
		}
 )), new IconReturn(new IconSingleton(mask_r)), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_74.setCache(methodCache, "getBitmask_m");
		body_74.setUnpackClosure(unpack_75).unpackArgs(args_73);
		return body_74;
	}
	@MMethod(name="allBitmasks", methodName="allBitmasks")
	@MParameter(name="piece", reifiedName="piece_r", type="")
	@MParameter(name="color", reifiedName="color_r", type="")
	@MLocal(name="bitmasks", reifiedName="bitmasks_r", type="")
	@MLocal(name="rotations", reifiedName="rotations_r", type="")
	@MLocal(name="y", reifiedName="y_r", type="")
	@MLocal(name="x", reifiedName="x_r", type="")
	@MLocal(name="mask", reifiedName="mask_r", type="")
	@MLocal(name="cell", reifiedName="cell_r", type="")
	public IIconIterator allBitmasks (Object... args_77) {
		// Reuse method body
		IconIterator body_78 = methodCache.getFree("allBitmasks_m");
		if (body_78 != null) { return body_78.reset().unpackArgs(args_77); };
		// Reified parameters
		IconVar piece_r = new IconVar().local();
		IconVar color_r = new IconVar().local();
		// Locals
		IconVar bitmasks_r = new IconVar().local();
		IconVar rotations_r = new IconVar().local();
		IconVar y_r = new IconVar().local();
		IconVar x_r = new IconVar().local();
		IconVar mask_r = new IconVar().local();
		IconVar cell_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_79 = (Object... params_80) -> {
			if (params_80 ==  null) { params_80 = IIconAtom.getEmptyArray(); };
			piece_r.set((params_80.length > 0) ? params_80[0] : null);
			color_r.set((params_80.length > 1) ? params_80[1] : null);
			// Reset locals
			bitmasks_r.set(null);
			rotations_r.set(null);
			y_r.set(null);
			x_r.set(null);
			mask_r.set(null);
			cell_r.set(null);
			return null;
		};
		// Method body
		body_78 = new IconSequence(new IconAssign().over(new IconSingleton(bitmasks_r), IconVarIterator.createAsList(()-> new IconList())), new IconEvery(new IconPromote(IconValue.create(2)), new IconBlock( () -> {
			// Temporaries
			IconTmp x_10_r = new IconTmp();
			IconTmp x_18_r = new IconTmp();
			IconTmp x_17_r = new IconTmp();
			IconTmp x_19_r = new IconTmp();
			return new IconSequence(new IconEvery(new IconAssign().over(new IconSingleton(rotations_r), new IconProduct(new IconIn(x_10_r, (new IconOperation(IconOperators.minus).over(new IconValueIterator(6), new IconOperation(IconOperators.times).over(new IconValueIterator(3), (new IconIf(new IconOperation(IconOperators.sameNumberAs).over(new IconSingleton(color_r), new IconValueIterator(4)), new IconValueIterator(1), new IconValueIterator(0))))))), new IconToIterator(IconValue.create(1), x_10_r))), new IconBlock( () -> {
				// Temporaries
				IconTmp x_11_r = new IconTmp();
				IconTmp x_15_r = new IconTmp();
				IconTmp x_14_r = new IconTmp();
				IconTmp x_16_r = new IconTmp();
				return new IconSequence(new IconEvery(new IconAssign().over(new IconSingleton(y_r), new IconProduct(new IconIn(x_11_r, new IconOperation(IconOperators.minus).over(new IconSingleton(height_r), new IconValueIterator(1))), new IconToIterator(IconValue.create(0), x_11_r))), new IconBlock( () -> {
					// Temporaries
					IconTmp x_12_r = new IconTmp();
					return new IconEvery(new IconAssign().over(new IconSingleton(x_r), new IconProduct(new IconIn(x_12_r, new IconOperation(IconOperators.minus).over(new IconSingleton(width_r), new IconValueIterator(1))), new IconToIterator(IconValue.create(0), x_12_r))), new IconBlock( () -> {
						// Temporaries
						IconTmp x_13_r = new IconTmp();
						return new IconIf(new IconProduct(new IconIn(x_13_r, new IconAssign().over(new IconSingleton(mask_r), new IconInvokeIterator(()-> ((VariadicFunction) getBitmask).apply(x_r.deref(), y_r.deref(), piece_r.deref())))), new IconInvokeIterator(()-> ((VariadicFunction) noIslands).apply(x_13_r.deref()))), new IconInvokeIterator(()-> ((VariadicFunction) put).apply(bitmasks_r.deref(), mask_r.deref())));
					}
 ));
				}
 )), new IconEvery(new IconAssign().over(new IconProduct(new IconIn(x_15_r, new IconAssign().over(new IconSingleton(cell_r), new IconProduct(new IconIn(x_14_r, new IconOperation(IconOperators.timesUnary).over(new IconSingleton(piece_r))), new IconToIterator(IconValue.create(1), x_14_r)))), new IconIndexIterator(piece_r, x_15_r)), new IconProduct(new IconIn(x_16_r, new IconIndexIterator(piece_r, cell_r)), new IconIndexIterator(rotate_r, x_16_r)))));
			}
 )), new IconEvery(new IconAssign().over(new IconProduct(new IconIn(x_18_r, new IconAssign().over(new IconSingleton(cell_r), new IconProduct(new IconIn(x_17_r, new IconOperation(IconOperators.timesUnary).over(new IconSingleton(piece_r))), new IconToIterator(IconValue.create(1), x_17_r)))), new IconIndexIterator(piece_r, x_18_r)), new IconProduct(new IconIn(x_19_r, new IconIndexIterator(piece_r, cell_r)), new IconIndexIterator(flip_r, x_19_r)))));
		}
 )), new IconReturn(new IconSingleton(bitmasks_r)), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_78.setCache(methodCache, "allBitmasks_m");
		body_78.setUnpackClosure(unpack_79).unpackArgs(args_77);
		return body_78;
	}
	@MMethod(name="generateBitmasks", methodName="generateBitmasks")
	@MLocal(name="piece", reifiedName="piece_r", type="")
	@MLocal(name="m", reifiedName="m_r", type="")
	@MLocal(name="cellMask", reifiedName="cellMask_r", type="")
	@MLocal(name="cellCounter", reifiedName="cellCounter_r", type="")
	@MLocal(name="j", reifiedName="j_r", type="")
	@MLocal(name="color", reifiedName="color_r", type="")
	public IIconIterator generateBitmasks (Object... args_81) {
		// Reuse method body
		IconIterator body_82 = methodCache.getFree("generateBitmasks_m");
		if (body_82 != null) { return body_82.reset().unpackArgs(args_81); };
		// Locals
		IconVar piece_r = new IconVar().local();
		IconVar m_r = new IconVar().local();
		IconVar cellMask_r = new IconVar().local();
		IconVar cellCounter_r = new IconVar().local();
		IconVar j_r = new IconVar().local();
		IconVar color_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_83 = (Object... params_84) -> {
			if (params_84 ==  null) { params_84 = IIconAtom.getEmptyArray(); };
			// Reset locals
			piece_r.set(null);
			m_r.set(null);
			cellMask_r.set(null);
			cellCounter_r.set(null);
			j_r.set(null);
			return null;
		};
		// Method body
		body_82 = new IconSequence(new IconAssign().over(new IconSingleton(color_r), new IconValueIterator(0)), new IconEvery(new IconAssign().over(new IconSingleton(piece_r), new IconPromote(pieces_r)), new IconBlock( () -> {
			// Temporaries
			IconTmp x_20_r = new IconTmp();
			IconTmp x_21_r = new IconTmp();
			return new IconSequence(new IconAssign().over(new IconSingleton(m_r), new IconProduct(new IconIn(x_20_r, new IconInvokeIterator(()-> ((VariadicFunction) allBitmasks).apply(piece_r.deref(), color_r.deref()))), new IconInvokeIterator(()-> ((VariadicFunction) sort).apply(x_20_r.deref())))), new IconAssign().over(new IconSingleton(cellMask_r), new IconProduct(new IconIn(x_21_r, (new IconOperation(IconOperators.minus).over(new IconOperation(IconOperators.times).over(new IconSingleton(width_r), new IconSingleton(height_r)), new IconValueIterator(1)))), new IconInvokeIterator(()-> ((VariadicFunction) ishift).apply(IconNumber.create(1), x_21_r.deref())))), new IconAssign().over(new IconSingleton(cellCounter_r), new IconOperation(IconOperators.minus).over(new IconOperation(IconOperators.times).over(new IconSingleton(width_r), new IconSingleton(height_r)), new IconValueIterator(1))), new IconAssign().over(new IconSingleton(j_r), new IconOperation(IconOperators.timesUnary).over(new IconSingleton(m_r))), new IconWhile(new IconOperation(IconOperators.greaterThan).over(new IconSingleton(j_r), new IconValueIterator(0)), new IconBlock( () -> {
				// Temporaries
				IconTmp x_22_r = new IconTmp();
				return new IconIf(new IconOperation(IconOperators.sameNumberAs).over(new IconProduct(new IconIn(x_22_r, new IconIndexIterator(m_r, j_r)), new IconInvokeIterator(()-> ((VariadicFunction) iand).apply(x_22_r.deref(), cellMask_r.deref()))), new IconSingleton(cellMask_r)), new IconBlock( () -> {
					// Temporaries
					IconTmp x_25_r = new IconTmp();
					IconTmp x_23_r = new IconTmp();
					IconTmp i_23_r = new IconTmp();
					IconTmp x_24_r = new IconTmp();
					IconTmp x_26_r = new IconTmp();
					return new IconSequence(new IconProduct(new IconProduct(new IconIn(x_25_r, new IconProduct(new IconProduct(new IconIn(x_23_r, new IconOperation(IconOperators.plus).over(new IconSingleton(cellCounter_r), new IconValueIterator(1))), new IconIn(i_23_r, new IconIndexIterator(masksAtCell_r, x_23_r))), new IconProduct(new IconIn(x_24_r, new IconOperation(IconOperators.plus).over(new IconSingleton(color_r), new IconValueIterator(1))), new IconIndexIterator(i_23_r, x_24_r)))), new IconIn(x_26_r, new IconIndexIterator(m_r, j_r))), new IconInvokeIterator(()-> ((VariadicFunction) put).apply(x_25_r.deref(), x_26_r.deref()))), new IconAssign().augment(IconOperators.minus).over(new IconSingleton(j_r), new IconValueIterator(1)));
				}
 ), new IconBlock( () -> {
					// Temporaries
					IconTmp x_27_r = new IconTmp();
					return new IconSequence(new IconAssign().over(new IconSingleton(cellMask_r), new IconProduct(new IconIn(x_27_r, new IconOperation(IconOperators.minusUnary).over(new IconValueIterator(1))), new IconInvokeIterator(()-> ((VariadicFunction) ishift).apply(cellMask_r.deref(), x_27_r.deref())))), new IconAssign().augment(IconOperators.minus).over(new IconSingleton(cellCounter_r), new IconValueIterator(1)));
				}
 ));
			}
 )), new IconAssign().augment(IconOperators.plus).over(new IconSingleton(color_r), new IconValueIterator(1)));
		}
 )), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_82.setCache(methodCache, "generateBitmasks_m");
		body_82.setUnpackClosure(unpack_83).unpackArgs(args_81);
		return body_82;
	}
	@MMethod(name="solveCell", methodName="solveCell")
	@MParameter(name="cell", reifiedName="cell_r", type="")
	@MParameter(name="board", reifiedName="board_r", type="")
	@MParameter(name="n", reifiedName="n_r", type="")
	@MLocal(name="s", reifiedName="s_r", type="")
	@MLocal(name="color", reifiedName="color_r", type="")
	@MLocal(name="mask", reifiedName="mask_r", type="")
	public IIconIterator solveCell (Object... args_85) {
		// Reuse method body
		IconIterator body_86 = methodCache.getFree("solveCell_m");
		if (body_86 != null) { return body_86.reset().unpackArgs(args_85); };
		// Reified parameters
		IconVar cell_r = new IconVar().local();
		IconVar board_r = new IconVar().local();
		IconVar n_r = new IconVar().local();
		// Temporaries
		IconTmp x_28_r = new IconTmp();
		// Locals
		IconVar s_r = new IconVar().local();
		IconVar color_r = new IconVar().local();
		IconVar mask_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_87 = (Object... params_88) -> {
			if (params_88 ==  null) { params_88 = IIconAtom.getEmptyArray(); };
			cell_r.set((params_88.length > 0) ? params_88[0] : null);
			board_r.set((params_88.length > 1) ? params_88[1] : null);
			n_r.set((params_88.length > 2) ? params_88[2] : null);
			// Reset locals
			s_r.set(null);
			color_r.set(null);
			mask_r.set(null);
			return null;
		};
		// Method body
		body_86 = new IconSequence(new IconIf(new IconOperation(IconOperators.greaterThanOrEquals).over(new IconOperation(IconOperators.timesUnary).over(new IconSingleton(solutions_r)), new IconSingleton(n_r)), new IconReturn(new IconNullIterator())), new IconIf(new IconOperation(IconOperators.sameNumberAs).over(new IconSingleton(board_r), new IconValueIterator("16r3FFFFFFFFFFFF", -1)), new IconSequence(new IconAssign().over(new IconSingleton(s_r), new IconInvokeIterator(()-> ((VariadicFunction) stringOfMasks).apply(masks))), new IconInvokeIterator(()-> ((VariadicFunction) put).apply(solutions, s_r.deref(), s_r.deref())), new IconReturn(new IconNullIterator()))), new IconIf(new IconOperation(IconOperators.notSameNumberAs).over(new IconProduct(new IconIn(x_28_r, new IconInvokeIterator(()-> ((VariadicFunction) ishift).apply(IconNumber.create(1), cell_r.deref()))), new IconInvokeIterator(()-> ((VariadicFunction) iand).apply(board_r.deref(), x_28_r.deref()))), new IconValueIterator(0)), new IconBlock( () -> {
			// Temporaries
			IconTmp x_29_r = new IconTmp();
			return new IconSequence(new IconProduct(new IconIn(x_29_r, new IconOperation(IconOperators.minus).over(new IconSingleton(cell_r), new IconValueIterator(1))), new IconInvokeIterator(()-> solveCell(x_29_r.deref(), board_r.deref(), n_r.deref()))), new IconReturn(new IconNullIterator()));
		}
 )), new IconIf(new IconOperation(IconOperators.lessThan).over(new IconSingleton(cell_r), new IconValueIterator(0)), new IconReturn(new IconNullIterator())), new IconEvery(new IconAssign().over(new IconSingleton(color_r), new IconToIterator(IconValue.create(1), IconValue.create(10))), new IconIf(new IconOperation(IconOperators.sameNumberAs).over(new IconIndexIterator(masks_r, color_r), new IconValueIterator(0)), new IconBlock( () -> {
			// Temporaries
			IconTmp x_31_r = new IconTmp();
			IconTmp x_30_r = new IconTmp();
			IconTmp i_30_r = new IconTmp();
			return new IconEvery(new IconAssign().over(new IconSingleton(mask_r), new IconProduct(new IconIn(x_31_r, new IconProduct(new IconProduct(new IconIn(x_30_r, new IconOperation(IconOperators.plus).over(new IconSingleton(cell_r), new IconValueIterator(1))), new IconIn(i_30_r, new IconIndexIterator(masksAtCell_r, x_30_r))), new IconIndexIterator(i_30_r, color_r))), new IconPromote(x_31_r))), new IconIf(new IconOperation(IconOperators.sameNumberAs).over(new IconInvokeIterator(()-> ((VariadicFunction) iand).apply(mask_r.deref(), board_r.deref())), new IconValueIterator(0)), new IconBlock( () -> {
				// Temporaries
				IconTmp x_32_r = new IconTmp();
				IconTmp x_33_r = new IconTmp();
				return new IconSequence(new IconAssign().over(new IconIndexIterator(masks_r, color_r), new IconSingleton(mask_r)), new IconProduct(new IconProduct(new IconIn(x_32_r, new IconOperation(IconOperators.minus).over(new IconSingleton(cell_r), new IconValueIterator(1))), new IconIn(x_33_r, new IconInvokeIterator(()-> ((VariadicFunction) ior).apply(board_r.deref(), mask_r.deref())))), new IconInvokeIterator(()-> solveCell(x_32_r.deref(), x_33_r.deref(), n_r.deref()))), new IconAssign().over(new IconIndexIterator(masks_r, color_r), new IconValueIterator(0)));
			}
 )));
		}
 ))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_86.setCache(methodCache, "solveCell_m");
		body_86.setUnpackClosure(unpack_87).unpackArgs(args_85);
		return body_86;
	}
	@MMethod(name="solve", methodName="solve")
	@MParameter(name="n", reifiedName="n_r", type="")
	public IIconIterator solve (Object... args_89) {
		// Reuse method body
		IconIterator body_90 = methodCache.getFree("solve_m");
		if (body_90 != null) { return body_90.reset().unpackArgs(args_89); };
		// Reified parameters
		IconVar n_r = new IconVar().local();
		// Temporaries
		IconTmp x_34_r = new IconTmp();
		// Unpack parameters
		VariadicFunction unpack_91 = (Object... params_92) -> {
			if (params_92 ==  null) { params_92 = IIconAtom.getEmptyArray(); };
			n_r.set((params_92.length > 0) ? params_92[0] : null);
			return null;
		};
		// Method body
		body_90 = new IconSequence(new IconInvokeIterator(()-> ((VariadicFunction) generateBitmasks).apply()), new IconProduct(new IconIn(x_34_r, new IconOperation(IconOperators.minus).over(new IconOperation(IconOperators.times).over(new IconSingleton(width_r), new IconSingleton(height_r)), new IconValueIterator(1))), new IconInvokeIterator(()-> ((VariadicFunction) solveCell).apply(x_34_r.deref(), IconNumber.create(0), n_r.deref()))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_90.setCache(methodCache, "solve_m");
		body_90.setUnpackClosure(unpack_91).unpackArgs(args_89);
		return body_90;
	}
	@MMethod(name="stringOfMasks", methodName="stringOfMasks")
	@MParameter(name="masks", reifiedName="masks_r", type="")
	@MLocal(name="s", reifiedName="s_r", type="")
	@MLocal(name="mask", reifiedName="mask_r", type="")
	@MLocal(name="color", reifiedName="color_r", type="")
	public IIconIterator stringOfMasks (Object... args_93) {
		// Reuse method body
		IconIterator body_94 = methodCache.getFree("stringOfMasks_m");
		if (body_94 != null) { return body_94.reset().unpackArgs(args_93); };
		// Reified parameters
		IconVar masks_r = new IconVar().local();
		// Locals
		IconVar s_r = new IconVar().local();
		IconVar mask_r = new IconVar().local();
		IconVar color_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_95 = (Object... params_96) -> {
			if (params_96 ==  null) { params_96 = IIconAtom.getEmptyArray(); };
			masks_r.set((params_96.length > 0) ? params_96[0] : null);
			// Reset locals
			s_r.set(null);
			mask_r.set(null);
			color_r.set(null);
			return null;
		};
		// Method body
		body_94 = new IconSequence(new IconAssign().over(new IconSingleton(s_r), new IconValueIterator("")), new IconAssign().over(new IconSingleton(mask_r), new IconValueIterator(1)), new IconEvery(new IconPromote(height_r), new IconEvery(new IconPromote(width_r), new IconSequence(new IconEvery(new IconAssign().over(new IconSingleton(color_r), new IconToIterator(IconValue.create(0), IconValue.create(9))), new IconBlock( () -> {
			// Temporaries
			IconTmp x_36_r = new IconTmp();
			IconTmp x_35_r = new IconTmp();
			return new IconIf(new IconOperation(IconOperators.notSameNumberAs).over(new IconProduct(new IconIn(x_36_r, new IconProduct(new IconIn(x_35_r, new IconOperation(IconOperators.plus).over(new IconSingleton(color_r), new IconValueIterator(1))), new IconIndexIterator(masks_r, x_35_r))), new IconInvokeIterator(()-> ((VariadicFunction) iand).apply(x_36_r.deref(), mask_r.deref()))), new IconValueIterator(0)), new IconSequence(new IconAssign().augment(IconOperators.stringConcat).over(new IconSingleton(s_r), new IconSingleton(color_r)), new IconBreak(new IconNullIterator())), new IconIf(new IconOperation(IconOperators.sameNumberAs).over(new IconSingleton(color_r), new IconValueIterator(9)), new IconAssign().augment(IconOperators.stringConcat).over(new IconSingleton(s_r), new IconValueIterator("."))));
		}
 )), new IconAssign().over(new IconSingleton(mask_r), new IconInvokeIterator(()-> ((VariadicFunction) ishift).apply(mask_r.deref(), IconNumber.create(1))))))), new IconReturn(new IconSingleton(s_r)), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_94.setCache(methodCache, "stringOfMasks_m");
		body_94.setUnpackClosure(unpack_95).unpackArgs(args_93);
		return body_94;
	}
	@MMethod(name="inverse", methodName="inverse")
	@MParameter(name="s", reifiedName="s_r", type="")
	@MLocal(name="ns", reifiedName="ns_r", type="")
	@MLocal(name="x", reifiedName="x_r", type="")
	@MLocal(name="y", reifiedName="y_r", type="")
	public IIconIterator inverse (Object... args_97) {
		// Reuse method body
		IconIterator body_98 = methodCache.getFree("inverse_m");
		if (body_98 != null) { return body_98.reset().unpackArgs(args_97); };
		// Reified parameters
		IconVar s_r = new IconVar().local();
		// Temporaries
		IconTmp x_37_r = new IconTmp();
		IconTmp x_38_r = new IconTmp();
		IconTmp x_39_r = new IconTmp();
		// Locals
		IconVar ns_r = new IconVar().local();
		IconVar x_r = new IconVar().local();
		IconVar y_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_99 = (Object... params_100) -> {
			if (params_100 ==  null) { params_100 = IIconAtom.getEmptyArray(); };
			s_r.set((params_100.length > 0) ? params_100[0] : null);
			// Reset locals
			ns_r.set(null);
			x_r.set(null);
			y_r.set(null);
			return null;
		};
		// Method body
		body_98 = new IconSequence(new IconAssign().over(new IconSingleton(ns_r), new IconSingleton(s_r)), new IconProduct(new IconIn(x_37_r, new IconInvokeIterator(()-> ((VariadicFunction) image).apply(s_r.deref()))), new IconInvokeIterator(()-> ((VariadicFunction) write).apply(x_37_r.deref()))), new IconEvery(new IconAssign().over(new IconSingleton(x_r), new IconProduct(new IconIn(x_38_r, new IconOperation(IconOperators.minus).over(new IconSingleton(width_r), new IconValueIterator(1))), new IconToIterator(IconValue.create(0), x_38_r))), new IconEvery(new IconAssign().over(new IconSingleton(y_r), new IconProduct(new IconIn(x_39_r, new IconOperation(IconOperators.minus).over(new IconSingleton(height_r), new IconValueIterator(1))), new IconToIterator(IconValue.create(0), x_39_r))), new IconBlock( () -> {
			// Temporaries
			IconTmp x_40_r = new IconTmp();
			IconTmp x_41_r = new IconTmp();
			return new IconAssign().over(new IconProduct(new IconIn(x_40_r, new IconOperation(IconOperators.plus).over((new IconOperation(IconOperators.plus).over(new IconSingleton(x_r), new IconOperation(IconOperators.times).over(new IconSingleton(y_r), new IconSingleton(width_r)))), new IconValueIterator(1))), new IconIndexIterator(ns_r, x_40_r)), new IconProduct(new IconIn(x_41_r, new IconOperation(IconOperators.plus).over((new IconOperation(IconOperators.plus).over(new IconOperation(IconOperators.minus).over(new IconOperation(IconOperators.minus).over(new IconSingleton(width_r), new IconSingleton(x_r)), new IconValueIterator(1)), new IconOperation(IconOperators.times).over((new IconOperation(IconOperators.minus).over(new IconSingleton(width_r), new IconSingleton(y_r), new IconValueIterator(1))), new IconSingleton(width_r)))), new IconValueIterator(1))), new IconIndexIterator(s_r, x_41_r)));
		}
 ))), new IconReturn(new IconSingleton(s_r)), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_98.setCache(methodCache, "inverse_m");
		body_98.setUnpackClosure(unpack_99).unpackArgs(args_97);
		return body_98;
	}
	@MMethod(name="printSolution", methodName="printSolution")
	@MParameter(name="solution", reifiedName="solution_r", type="")
	@MLocal(name="y", reifiedName="y_r", type="")
	@MLocal(name="x", reifiedName="x_r", type="")
	public IIconIterator printSolution (Object... args_101) {
		// Reuse method body
		IconIterator body_102 = methodCache.getFree("printSolution_m");
		if (body_102 != null) { return body_102.reset().unpackArgs(args_101); };
		// Reified parameters
		IconVar solution_r = new IconVar().local();
		// Temporaries
		IconTmp x_42_r = new IconTmp();
		// Locals
		IconVar y_r = new IconVar().local();
		IconVar x_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_103 = (Object... params_104) -> {
			if (params_104 ==  null) { params_104 = IIconAtom.getEmptyArray(); };
			solution_r.set((params_104.length > 0) ? params_104[0] : null);
			// Reset locals
			y_r.set(null);
			x_r.set(null);
			return null;
		};
		// Method body
		body_102 = new IconSequence(new IconEvery(new IconAssign().over(new IconSingleton(y_r), new IconProduct(new IconIn(x_42_r, new IconOperation(IconOperators.minus).over(new IconSingleton(height_r), new IconValueIterator(1))), new IconToIterator(IconValue.create(0), x_42_r))), new IconBlock( () -> {
			// Temporaries
			IconTmp x_43_r = new IconTmp();
			IconTmp x_45_r = new IconTmp();
			IconTmp x_44_r = new IconTmp();
			return new IconSequence(new IconEvery(new IconAssign().over(new IconSingleton(x_r), new IconProduct(new IconIn(x_43_r, new IconOperation(IconOperators.minus).over(new IconSingleton(width_r), new IconValueIterator(1))), new IconToIterator(IconValue.create(0), x_43_r))), new IconProduct(new IconIn(x_45_r, new IconProduct(new IconIn(x_44_r, new IconOperation(IconOperators.plus).over((new IconOperation(IconOperators.plus).over(new IconSingleton(x_r), new IconOperation(IconOperators.times).over(new IconSingleton(y_r), new IconSingleton(width_r)))), new IconValueIterator(1))), new IconIndexIterator(solution_r, x_44_r))), new IconInvokeIterator(()-> ((VariadicFunction) writes).apply(x_45_r.deref(), " ")))), new IconIf(new IconOperation(IconOperators.sameNumberAs).over((new IconOperation(IconOperators.remainder).over(new IconSingleton(y_r), new IconValueIterator(2))), new IconValueIterator(0)), new IconSequence(new IconInvokeIterator(()-> ((VariadicFunction) write).apply()), new IconInvokeIterator(()-> ((VariadicFunction) writes).apply(" "))), new IconInvokeIterator(()-> ((VariadicFunction) write).apply())));
		}
 )), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_102.setCache(methodCache, "printSolution_m");
		body_102.setUnpackClosure(unpack_103).unpackArgs(args_101);
		return body_102;
	}
	@MMethod(name="valid", methodName="valid")
	@MParameter(name="x", reifiedName="x_r", type="")
	@MParameter(name="y", reifiedName="y_r", type="")
	public IIconIterator valid (Object... args_105) {
		// Reuse method body
		IconIterator body_106 = methodCache.getFree("valid_m");
		if (body_106 != null) { return body_106.reset().unpackArgs(args_105); };
		// Reified parameters
		IconVar x_r = new IconVar().local();
		IconVar y_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_107 = (Object... params_108) -> {
			if (params_108 ==  null) { params_108 = IIconAtom.getEmptyArray(); };
			x_r.set((params_108.length > 0) ? params_108[0] : null);
			y_r.set((params_108.length > 1) ? params_108[1] : null);
			return null;
		};
		// Method body
		body_106 = new IconSequence(new IconReturn(new IconProduct((new IconOperation(IconOperators.lessThan).over(new IconOperation(IconOperators.lessThanOrEquals).over(new IconValueIterator(0), new IconSingleton(x_r)), new IconSingleton(width_r))), (new IconOperation(IconOperators.lessThan).over(new IconOperation(IconOperators.lessThanOrEquals).over(new IconValueIterator(0), new IconSingleton(y_r)), new IconSingleton(height_r))))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_106.setCache(methodCache, "valid_m");
		body_106.setUnpackClosure(unpack_107).unpackArgs(args_105);
		return body_106;
	}
	@MMethod(name="legal", methodName="legal")
	@MParameter(name="mask", reifiedName="mask_r", type="")
	@MParameter(name="board", reifiedName="board_r", type="")
	public IIconIterator legal (Object... args_109) {
		// Reuse method body
		IconIterator body_110 = methodCache.getFree("legal_m");
		if (body_110 != null) { return body_110.reset().unpackArgs(args_109); };
		// Reified parameters
		IconVar mask_r = new IconVar().local();
		IconVar board_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_111 = (Object... params_112) -> {
			if (params_112 ==  null) { params_112 = IIconAtom.getEmptyArray(); };
			mask_r.set((params_112.length > 0) ? params_112[0] : null);
			board_r.set((params_112.length > 1) ? params_112[1] : null);
			return null;
		};
		// Method body
		body_110 = new IconSequence(new IconReturn(new IconOperation(IconOperators.sameNumberAs).over(new IconInvokeIterator(()-> ((VariadicFunction) iand).apply(mask_r.deref(), board_r.deref())), new IconValueIterator(0))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_110.setCache(methodCache, "legal_m");
		body_110.setUnpackClosure(unpack_111).unpackArgs(args_109);
		return body_110;
	}
	@MMethod(name="zerocount", methodName="zerocount")
	@MParameter(name="mask", reifiedName="mask_r", type="")
	@MLocal(name="x", reifiedName="x_r", type="")
	@MLocal(name="zeros_in_4bits_s", reifiedName="zeros_in_4bits_s_r", type="")
	@MLocal(name="sum", reifiedName="sum_r", type="")
	public IIconIterator zerocount (Object... args_113) {
		// Reuse method body
		IconIterator body_114 = methodCache.getFree("zerocount_m");
		if (body_114 != null) { return body_114.reset().unpackArgs(args_113); };
		// Reified parameters
		IconVar mask_r = new IconVar().local();
		// Locals
		IconVar x_r = new IconVar().local();
		IconVar sum_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_115 = (Object... params_116) -> {
			if (params_116 ==  null) { params_116 = IIconAtom.getEmptyArray(); };
			mask_r.set((params_116.length > 0) ? params_116[0] : null);
			// Reset locals
			x_r.set(null);
			return null;
		};
		// Initialize method on first use
		if (initialMethodCache.get("zerocount_m") == null) {
			initialMethodCache.computeIfAbsent("zerocount_m", (java.util.function.Function)  (arg) -> {
				(new IconAssign().over(new IconSingleton(zeros_in_4bits_s_r), IconVarIterator.createAsList(()-> new IconList(IconNumber.create(4), IconNumber.create(3), IconNumber.create(3), IconNumber.create(2), IconNumber.create(3), IconNumber.create(2), IconNumber.create(2), IconNumber.create(1), IconNumber.create(3), IconNumber.create(2), IconNumber.create(2), IconNumber.create(1), IconNumber.create(2), IconNumber.create(1), IconNumber.create(1), IconNumber.create(0))))).nextOrNull(); return true;
			});
		}
		// Method body
		body_114 = new IconSequence(new IconAssign().over(new IconSingleton(sum_r), new IconOperation(IconOperators.minusUnary).over(new IconValueIterator(2))), new IconEvery(new IconAssign().over(new IconSingleton(x_r), new IconToIterator(IconValue.create(0), IconValue.create(48), IconValue.create(4))), new IconBlock( () -> {
			// Temporaries
			IconTmp x_49_r = new IconTmp();
			IconTmp x_47_r = new IconTmp();
			IconTmp x_46_r = new IconTmp();
			IconTmp x_48_r = new IconTmp();
			return new IconAssign().augment(IconOperators.plus).over(new IconSingleton(sum_r), new IconProduct(new IconIn(x_49_r, new IconOperation(IconOperators.plus).over(new IconValueIterator(1), new IconProduct(new IconProduct(new IconIn(x_47_r, new IconProduct(new IconIn(x_46_r, new IconInvokeIterator(()-> ((VariadicFunction) ishift).apply(IconNumber.create(15), x_r.deref()))), new IconInvokeIterator(()-> ((VariadicFunction) iand).apply(x_46_r.deref(), mask_r.deref())))), new IconIn(x_48_r, new IconOperation(IconOperators.minusUnary).over(new IconSingleton(x_r)))), new IconInvokeIterator(()-> ((VariadicFunction) ishift).apply(x_47_r.deref(), x_48_r.deref()))))), new IconIndexIterator(zeros_in_4bits_s_r, x_49_r)));
		}
 )), new IconReturn(new IconSingleton(sum_r)), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_114.setCache(methodCache, "zerocount_m");
		body_114.setUnpackClosure(unpack_115).unpackArgs(args_113);
		return body_114;
	}
	@MMethod(name="move_E", methodName="move_E")
	@MParameter(name="x", reifiedName="x_r", type="")
	@MParameter(name="y", reifiedName="y_r", type="")
	public IIconIterator move_E (Object... args_117) {
		// Reuse method body
		IconIterator body_118 = methodCache.getFree("move_E_m");
		if (body_118 != null) { return body_118.reset().unpackArgs(args_117); };
		// Reified parameters
		IconVar x_r = new IconVar().local();
		IconVar y_r = new IconVar().local();
		// Temporaries
		IconTmp x_50_r = new IconTmp();
		// Unpack parameters
		VariadicFunction unpack_119 = (Object... params_120) -> {
			if (params_120 ==  null) { params_120 = IIconAtom.getEmptyArray(); };
			x_r.set((params_120.length > 0) ? params_120[0] : null);
			y_r.set((params_120.length > 1) ? params_120[1] : null);
			return null;
		};
		// Method body
		body_118 = new IconSequence(new IconReturn(new IconProduct(new IconIn(x_50_r, new IconOperation(IconOperators.plus).over(new IconSingleton(x_r), new IconValueIterator(1))), new IconInvokeIterator(()-> ((VariadicFunction) xy.xy).apply(x_50_r.deref(), y_r.deref())))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_118.setCache(methodCache, "move_E_m");
		body_118.setUnpackClosure(unpack_119).unpackArgs(args_117);
		return body_118;
	}
	@MMethod(name="move_W", methodName="move_W")
	@MParameter(name="x", reifiedName="x_r", type="")
	@MParameter(name="y", reifiedName="y_r", type="")
	public IIconIterator move_W (Object... args_121) {
		// Reuse method body
		IconIterator body_122 = methodCache.getFree("move_W_m");
		if (body_122 != null) { return body_122.reset().unpackArgs(args_121); };
		// Reified parameters
		IconVar x_r = new IconVar().local();
		IconVar y_r = new IconVar().local();
		// Temporaries
		IconTmp x_51_r = new IconTmp();
		// Unpack parameters
		VariadicFunction unpack_123 = (Object... params_124) -> {
			if (params_124 ==  null) { params_124 = IIconAtom.getEmptyArray(); };
			x_r.set((params_124.length > 0) ? params_124[0] : null);
			y_r.set((params_124.length > 1) ? params_124[1] : null);
			return null;
		};
		// Method body
		body_122 = new IconSequence(new IconReturn(new IconProduct(new IconIn(x_51_r, new IconOperation(IconOperators.minus).over(new IconSingleton(x_r), new IconValueIterator(1))), new IconInvokeIterator(()-> ((VariadicFunction) xy.xy).apply(x_51_r.deref(), y_r.deref())))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_122.setCache(methodCache, "move_W_m");
		body_122.setUnpackClosure(unpack_123).unpackArgs(args_121);
		return body_122;
	}
	@MMethod(name="move_NE", methodName="move_NE")
	@MParameter(name="x", reifiedName="x_r", type="")
	@MParameter(name="y", reifiedName="y_r", type="")
	public IIconIterator move_NE (Object... args_125) {
		// Reuse method body
		IconIterator body_126 = methodCache.getFree("move_NE_m");
		if (body_126 != null) { return body_126.reset().unpackArgs(args_125); };
		// Reified parameters
		IconVar x_r = new IconVar().local();
		IconVar y_r = new IconVar().local();
		// Temporaries
		IconTmp x_52_r = new IconTmp();
		IconTmp x_53_r = new IconTmp();
		// Unpack parameters
		VariadicFunction unpack_127 = (Object... params_128) -> {
			if (params_128 ==  null) { params_128 = IIconAtom.getEmptyArray(); };
			x_r.set((params_128.length > 0) ? params_128[0] : null);
			y_r.set((params_128.length > 1) ? params_128[1] : null);
			return null;
		};
		// Method body
		body_126 = new IconSequence(new IconReturn(new IconProduct(new IconProduct(new IconIn(x_52_r, new IconOperation(IconOperators.plus).over(new IconSingleton(x_r), (new IconOperation(IconOperators.remainder).over(new IconSingleton(y_r), new IconValueIterator(2))))), new IconIn(x_53_r, new IconOperation(IconOperators.minus).over(new IconSingleton(y_r), new IconValueIterator(1)))), new IconInvokeIterator(()-> ((VariadicFunction) xy.xy).apply(x_52_r.deref(), x_53_r.deref())))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_126.setCache(methodCache, "move_NE_m");
		body_126.setUnpackClosure(unpack_127).unpackArgs(args_125);
		return body_126;
	}
	@MMethod(name="move_NW", methodName="move_NW")
	@MParameter(name="x", reifiedName="x_r", type="")
	@MParameter(name="y", reifiedName="y_r", type="")
	public IIconIterator move_NW (Object... args_129) {
		// Reuse method body
		IconIterator body_130 = methodCache.getFree("move_NW_m");
		if (body_130 != null) { return body_130.reset().unpackArgs(args_129); };
		// Reified parameters
		IconVar x_r = new IconVar().local();
		IconVar y_r = new IconVar().local();
		// Temporaries
		IconTmp x_54_r = new IconTmp();
		IconTmp x_55_r = new IconTmp();
		// Unpack parameters
		VariadicFunction unpack_131 = (Object... params_132) -> {
			if (params_132 ==  null) { params_132 = IIconAtom.getEmptyArray(); };
			x_r.set((params_132.length > 0) ? params_132[0] : null);
			y_r.set((params_132.length > 1) ? params_132[1] : null);
			return null;
		};
		// Method body
		body_130 = new IconSequence(new IconReturn(new IconProduct(new IconProduct(new IconIn(x_54_r, new IconOperation(IconOperators.minus).over(new IconOperation(IconOperators.plus).over(new IconSingleton(x_r), (new IconOperation(IconOperators.remainder).over(new IconSingleton(y_r), new IconValueIterator(2)))), new IconValueIterator(1))), new IconIn(x_55_r, new IconOperation(IconOperators.minus).over(new IconSingleton(y_r), new IconValueIterator(1)))), new IconInvokeIterator(()-> ((VariadicFunction) xy.xy).apply(x_54_r.deref(), x_55_r.deref())))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_130.setCache(methodCache, "move_NW_m");
		body_130.setUnpackClosure(unpack_131).unpackArgs(args_129);
		return body_130;
	}
	@MMethod(name="move_SE", methodName="move_SE")
	@MParameter(name="x", reifiedName="x_r", type="")
	@MParameter(name="y", reifiedName="y_r", type="")
	public IIconIterator move_SE (Object... args_133) {
		// Reuse method body
		IconIterator body_134 = methodCache.getFree("move_SE_m");
		if (body_134 != null) { return body_134.reset().unpackArgs(args_133); };
		// Reified parameters
		IconVar x_r = new IconVar().local();
		IconVar y_r = new IconVar().local();
		// Temporaries
		IconTmp x_56_r = new IconTmp();
		IconTmp x_57_r = new IconTmp();
		// Unpack parameters
		VariadicFunction unpack_135 = (Object... params_136) -> {
			if (params_136 ==  null) { params_136 = IIconAtom.getEmptyArray(); };
			x_r.set((params_136.length > 0) ? params_136[0] : null);
			y_r.set((params_136.length > 1) ? params_136[1] : null);
			return null;
		};
		// Method body
		body_134 = new IconSequence(new IconReturn(new IconProduct(new IconProduct(new IconIn(x_56_r, new IconOperation(IconOperators.plus).over(new IconSingleton(x_r), (new IconOperation(IconOperators.remainder).over(new IconSingleton(y_r), new IconValueIterator(2))))), new IconIn(x_57_r, new IconOperation(IconOperators.plus).over(new IconSingleton(y_r), new IconValueIterator(1)))), new IconInvokeIterator(()-> ((VariadicFunction) xy.xy).apply(x_56_r.deref(), x_57_r.deref())))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_134.setCache(methodCache, "move_SE_m");
		body_134.setUnpackClosure(unpack_135).unpackArgs(args_133);
		return body_134;
	}
	@MMethod(name="move_SW", methodName="move_SW")
	@MParameter(name="x", reifiedName="x_r", type="")
	@MParameter(name="y", reifiedName="y_r", type="")
	public IIconIterator move_SW (Object... args_137) {
		// Reuse method body
		IconIterator body_138 = methodCache.getFree("move_SW_m");
		if (body_138 != null) { return body_138.reset().unpackArgs(args_137); };
		// Reified parameters
		IconVar x_r = new IconVar().local();
		IconVar y_r = new IconVar().local();
		// Temporaries
		IconTmp x_58_r = new IconTmp();
		IconTmp x_59_r = new IconTmp();
		// Unpack parameters
		VariadicFunction unpack_139 = (Object... params_140) -> {
			if (params_140 ==  null) { params_140 = IIconAtom.getEmptyArray(); };
			x_r.set((params_140.length > 0) ? params_140[0] : null);
			y_r.set((params_140.length > 1) ? params_140[1] : null);
			return null;
		};
		// Method body
		body_138 = new IconSequence(new IconReturn(new IconProduct(new IconProduct(new IconIn(x_58_r, new IconOperation(IconOperators.minus).over(new IconOperation(IconOperators.plus).over(new IconSingleton(x_r), (new IconOperation(IconOperators.remainder).over(new IconSingleton(y_r), new IconValueIterator(2)))), new IconValueIterator(1))), new IconIn(x_59_r, new IconOperation(IconOperators.plus).over(new IconSingleton(y_r), new IconValueIterator(1)))), new IconInvokeIterator(()-> ((VariadicFunction) xy.xy).apply(x_58_r.deref(), x_59_r.deref())))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_138.setCache(methodCache, "move_SW_m");
		body_138.setUnpackClosure(unpack_139).unpackArgs(args_137);
		return body_138;
	}
	@MMethod(name="run_meteorcontest", methodName="run_meteorcontest")
	@MParameter(name="argv", reifiedName="argv_r", type="")
	public IIconIterator run_meteorcontest (Object... args_141) {
		// Reuse method body
		IconIterator body_142 = methodCache.getFree("run_meteorcontest_m");
		if (body_142 != null) { return body_142.reset().unpackArgs(args_141); };
		// Reified parameters
		IconVar argv_r = new IconVar().local();
		// Temporaries
		IconTmp x_60_r = new IconTmp();
		IconTmp x_61_r = new IconTmp();
		IconTmp x_62_r = new IconTmp();
		// Unpack parameters
		VariadicFunction unpack_143 = (Object... params_144) -> {
			if (params_144 ==  null) { params_144 = IIconAtom.getEmptyArray(); };
			argv_r.set((params_144.length > 0) ? params_144[0] : null);
			return null;
		};
		// Method body
		body_142 = new IconSequence(new IconIf(new IconOperation(IconOperators.lessThan).over(new IconOperation(IconOperators.timesUnary).over(new IconSingleton(argv_r)), new IconValueIterator(1)), new IconInvokeIterator(()-> ((VariadicFunction) stop).apply("usage: meteor-contest num"))), new IconAssign().over(new IconSingleton(width_r), new IconValueIterator(5)), new IconAssign().over(new IconSingleton(height_r), new IconValueIterator(10)), new IconAssign().over(new IconSingleton(directions_r), new IconInvokeIterator(()-> ((VariadicFunction) table).apply("E", IconNumber.create(0), "NE", IconNumber.create(1), "NW", IconNumber.create(2), "W", IconNumber.create(3), "SW", IconNumber.create(4), "SE", IconNumber.create(5)))), new IconAssign().over(new IconSingleton(rotate_r), new IconInvokeIterator(()-> ((VariadicFunction) table).apply("E", "NE", "NE", "NW", "NW", "W", "W", "SW", "SW", "SE", "SE", "E"))), new IconAssign().over(new IconSingleton(flip_r), new IconInvokeIterator(()-> ((VariadicFunction) table).apply("E", "W", "NE", "NW", "NW", "NE", "W", "E", "SW", "SE", "SE", "SW"))), new IconAssign().over(new IconSingleton(moves_r), new IconInvokeIterator(()-> ((VariadicFunction) table).apply("E", move_E, "W", move_W, "NE", move_NE, "NW", move_NW, "SE", move_SE, "SW", move_SW))), new IconAssign().over(new IconSingleton(pieces_r), IconVarIterator.createAsList(()-> new IconList(new IconList("E", "E", "E", "SE"), new IconList("SE", "SW", "W", "SW"), new IconList("W", "W", "SW", "SE"), new IconList("E", "E", "SW", "SE"), new IconList("NW", "W", "NW", "SE", "SW"), new IconList("E", "E", "NE", "W"), new IconList("NW", "NE", "NE", "W"), new IconList("NE", "SE", "E", "NE"), new IconList("SE", "SE", "E", "SE"), new IconList("E", "NW", "NW", "NW")))), new IconAssign().over(new IconSingleton(solutions_r), IconVarIterator.createAsList(()-> new IconList())), new IconAssign().over(new IconSingleton(masks_r), new IconInvokeIterator(()-> ((VariadicFunction) list).apply(IconNumber.create(10), IconNumber.create(0)))), new IconAssign().over(new IconSingleton(masksAtCell_r), new IconProduct(new IconIn(x_60_r, new IconOperation(IconOperators.times).over(new IconSingleton(width_r), new IconSingleton(height_r))), new IconInvokeIterator(()-> ((VariadicFunction) list).apply(x_60_r.deref())))), new IconEvery(new IconAssign().over(new IconPromote(masksAtCell_r), IconVarIterator.createAsList(()-> new IconList(new IconList(), new IconList(), new IconList(), new IconList(), new IconList(), new IconList(), new IconList(), new IconList(), new IconList(), new IconList())))), new IconProduct(new IconIn(x_61_r, new IconIndexIterator(argv_r, IconValue.create(1))), new IconInvokeIterator(()-> ((VariadicFunction) solve).apply(x_61_r.deref()))), new IconProduct(new IconIn(x_62_r, new IconOperation(IconOperators.timesUnary).over(new IconSingleton(solutions_r))), new IconInvokeIterator(()-> ((VariadicFunction) write).apply(x_62_r.deref(), " solutions found\n"))), new IconIf((new IconOperation(IconOperators.greaterThan).over(new IconOperation(IconOperators.timesUnary).over(new IconSingleton(solutions_r)), new IconValueIterator(0))), new IconBlock( () -> {
			// Temporaries
			IconTmp x_63_r = new IconTmp();
			IconTmp x_64_r = new IconTmp();
			return new IconSequence(new IconProduct(new IconIn(x_63_r, new IconInvokeIterator(()-> ((VariadicFunction) min).apply(solutions))), new IconInvokeIterator(()-> ((VariadicFunction) printSolution).apply(x_63_r.deref()))), new IconInvokeIterator(()-> ((VariadicFunction) write).apply()), new IconProduct(new IconIn(x_64_r, new IconInvokeIterator(()-> ((VariadicFunction) max).apply(solutions))), new IconInvokeIterator(()-> ((VariadicFunction) printSolution).apply(x_64_r.deref()))), new IconInvokeIterator(()-> ((VariadicFunction) write).apply()));
		}
 )), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_142.setCache(methodCache, "run_meteorcontest_m");
		body_142.setUnpackClosure(unpack_143).unpackArgs(args_141);
		return body_142;
	}
	@MMethod(name="main", methodName="main_m")
	@MParameter(name="argv", reifiedName="argv_r", type="", isVararg=true)
	private IIconIterator main_m (Object... args_145) {
		// Reuse method body
		IconIterator body_146 = methodCache.getFree("main_m");
		if (body_146 != null) { return body_146.reset().unpackArgs(args_145); };
		// Reified parameters
		IconVar argv_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_147 = (Object... params_148) -> {
			if (params_148 ==  null) { params_148 = IIconAtom.getEmptyArray(); };
			argv_r.set((params_148.length > 0) ? Arrays.asList(params_148).subList(0, params_148.length) : new ArrayList());
			return null;
		};
		// Method body
		body_146 = new IconSequence(new IconInvokeIterator(()-> ((VariadicFunction) run_meteorcontest).apply(argv_r.deref())), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_146.setCache(methodCache, "main_m");
		body_146.setUnpackClosure(unpack_147).unpackArgs(args_145);
		return body_146;
	}
	// Static main method
	public static void main(String... args_149) {
		MeteorContest c = new MeteorContest(); VariadicFunction m = (VariadicFunction) c.main;
		IconCoExpression.activate(null, null, new IconCoExpression(
		 (Object... coexpr) -> {
			return ((IIconIterator) m.apply((Object[]) args_149)); },
		 () -> { return IconList.createArray(); } ));
	}
}
