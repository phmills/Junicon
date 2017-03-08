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
public class Matrix {
	// Method body cache
	private MethodBodyCache methodCache = new MethodBodyCache();
	// Method references
	@MMethodRef(name="main", methodName="main_m")
	public Object main = (VariadicFunction) this::main_m;
	@MMethodRef(name="multiply_matrix", methodName="multiply_matrix")
	public Object multiply_matrix = (VariadicFunction) this::multiply_matrix;
	// Constructors
	public Matrix() {
		;
	}
	// Static variadic constructor
	public static VariadicFunction Matrix = (Object... args_5) -> {
		return new Matrix();
	};
	// Methods
	@MMethod(name="main", methodName="main_m")
	@MParameter(name="args", reifiedName="args_r", type="", isVararg=true)
	@MLocal(name="rows", reifiedName="rows_r", type="")
	@MLocal(name="cols", reifiedName="cols_r", type="")
	@MLocal(name="m1", reifiedName="m1_r", type="")
	@MLocal(name="m2", reifiedName="m2_r", type="")
	@MLocal(name="result", reifiedName="result_r", type="")
	@MLocal(name="i", reifiedName="i_r", type="")
	@MLocal(name="j", reifiedName="j_r", type="")
	@MLocal(name="row", reifiedName="row_r", type="")
	@MLocal(name="col", reifiedName="col_r", type="")
	private IIconIterator main_m (Object... args_8) {
		// Reuse method body
		IconIterator body = methodCache.getFree("main_m");
		if (body != null) { return body.reset().unpackArgs(args_8); };
		// Reified parameters
		IconVar args_r = new IconVar().local();
		// Temporaries
		IconTmp x_2_r = new IconTmp();
		IconTmp i_1_r = new IconTmp();
		// Locals
		IconVar rows_r = new IconVar().local();
		IconVar cols_r = new IconVar().local();
		IconVar m1_r = new IconVar().local();
		IconVar m2_r = new IconVar().local();
		IconVar result_r = new IconVar().local();
		IconVar i_r = new IconVar().local();
		IconVar j_r = new IconVar().local();
		IconVar row_r = new IconVar().local();
		IconVar col_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack = (Object... params) -> {
			if (params ==  null) { params = IIconAtom.getEmptyArray(); };
			args_r.set((params.length > 0) ? Arrays.asList(params).subList(0, params.length) : new ArrayList());
			// Reset locals
			rows_r.set(null);
			cols_r.set(null);
			m1_r.set(null);
			m2_r.set(null);
			result_r.set(null);
			i_r.set(null);
			j_r.set(null);
			row_r.set(null);
			col_r.set(null);
			return null;
		};
		// Method body
		body = new IconSequence(new IconAssign().over(new IconSingleton(rows_r), new IconIndexIterator(args_r, IconValue.create(1))), new IconAssign().over(new IconSingleton(cols_r), new IconSingleton(rows_r)), new IconAssign().over(new IconSingleton(m1_r), IconVarIterator.createAsList(()-> new IconList())), new IconEvery(new IconAssign().over(new IconSingleton(i_r), new IconToIterator(IconValue.create(1), rows_r)), new IconSequence(new IconAssign().over(new IconSingleton(row_r), IconVarIterator.createAsList(()-> new IconList())), new IconEvery(new IconAssign().over(new IconSingleton(j_r), new IconToIterator(IconValue.create(1), cols_r)), new IconBlock( () -> {
			// Temporaries
			IconTmp x_0_r = new IconTmp();
			return new IconProduct(new IconIn(x_0_r, new IconOperation(IconOperators.times).over(new IconSingleton(i_r), new IconSingleton(j_r))), new IconInvokeIterator(()-> ((VariadicFunction) put).apply(row_r.deref(), x_0_r.deref())));
		}
 )), new IconInvokeIterator(()-> ((VariadicFunction) put).apply(m1_r.deref(), row_r.deref())), new IconNullIterator())), new IconAssign().over(new IconSingleton(m2_r), IconVarIterator.createAsList(()-> new IconList())), new IconEvery(new IconAssign().over(new IconSingleton(i_r), new IconToIterator(IconValue.create(1), cols_r)), new IconSequence(new IconAssign().over(new IconSingleton(row_r), IconVarIterator.createAsList(()-> new IconList())), new IconEvery(new IconAssign().over(new IconSingleton(j_r), new IconToIterator(IconValue.create(1), rows_r)), new IconBlock( () -> {
			// Temporaries
			IconTmp x_1_r = new IconTmp();
			return new IconProduct(new IconIn(x_1_r, new IconOperation(IconOperators.times).over(new IconSingleton(i_r), new IconSingleton(j_r))), new IconInvokeIterator(()-> ((VariadicFunction) put).apply(row_r.deref(), x_1_r.deref())));
		}
 )), new IconInvokeIterator(()-> ((VariadicFunction) put).apply(m2_r.deref(), row_r.deref())), new IconNullIterator())), new IconAssign().over(new IconSingleton(result_r), new IconInvokeIterator(()-> ((VariadicFunction) multiply_matrix).apply(m1_r.deref(), m2_r.deref()))), new IconProduct(new IconIn(x_2_r, new IconProduct(new IconIn(i_1_r, new IconIndexIterator(result_r, rows_r)), new IconIndexIterator(i_1_r, rows_r))), new IconInvokeIterator(()-> ((VariadicFunction) write).apply("Result: ", x_2_r.deref()))), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body.setCache(methodCache, "main_m");
		body.setUnpackClosure(unpack).unpackArgs(args_8);
		return body;
	}
	@MMethod(name="multiply_matrix", methodName="multiply_matrix")
	@MParameter(name="m1", reifiedName="m1_r", type="")
	@MParameter(name="m2", reifiedName="m2_r", type="")
	@MLocal(name="result", reifiedName="result_r", type="")
	@MLocal(name="row1", reifiedName="row1_r", type="")
	@MLocal(name="row", reifiedName="row_r", type="")
	@MLocal(name="colIndex", reifiedName="colIndex_r", type="")
	@MLocal(name="rowIndex", reifiedName="rowIndex_r", type="")
	@MLocal(name="value", reifiedName="value_r", type="")
	public IIconIterator multiply_matrix (Object... args_9) {
		// Reuse method body
		IconIterator body_10 = methodCache.getFree("multiply_matrix_m");
		if (body_10 != null) { return body_10.reset().unpackArgs(args_9); };
		// Reified parameters
		IconVar m1_r = new IconVar().local();
		IconVar m2_r = new IconVar().local();
		// Locals
		IconVar result_r = new IconVar().local();
		IconVar row1_r = new IconVar().local();
		IconVar row_r = new IconVar().local();
		IconVar colIndex_r = new IconVar().local();
		IconVar rowIndex_r = new IconVar().local();
		IconVar value_r = new IconVar().local();
		// Unpack parameters
		VariadicFunction unpack_11 = (Object... params_12) -> {
			if (params_12 ==  null) { params_12 = IIconAtom.getEmptyArray(); };
			m1_r.set((params_12.length > 0) ? params_12[0] : null);
			m2_r.set((params_12.length > 1) ? params_12[1] : null);
			// Reset locals
			result_r.set(null);
			row1_r.set(null);
			row_r.set(null);
			colIndex_r.set(null);
			rowIndex_r.set(null);
			value_r.set(null);
			return null;
		};
		// Method body
		body_10 = new IconSequence(new IconAssign().over(new IconSingleton(result_r), IconVarIterator.createAsList(()-> new IconList())), new IconEvery(new IconAssign().over(new IconSingleton(row1_r), new IconPromote(m1_r)), new IconBlock( () -> {
			// Temporaries
			IconTmp x_3_r = new IconTmp();
			return new IconSequence(new IconAssign().over(new IconSingleton(row_r), IconVarIterator.createAsList(()-> new IconList())), new IconEvery(new IconAssign().over(new IconSingleton(colIndex_r), new IconProduct(new IconIn(x_3_r, new IconOperation(IconOperators.timesUnary).over(new IconSingleton(m1_r))), new IconToIterator(IconValue.create(1), x_3_r))), new IconBlock( () -> {
				// Temporaries
				IconTmp x_4_r = new IconTmp();
				return new IconSequence(new IconAssign().over(new IconSingleton(value_r), new IconValueIterator(0)), new IconEvery(new IconAssign().over(new IconSingleton(rowIndex_r), new IconProduct(new IconIn(x_4_r, new IconOperation(IconOperators.timesUnary).over(new IconSingleton(m2_r))), new IconToIterator(IconValue.create(1), x_4_r))), new IconBlock( () -> {
					// Temporaries
					IconTmp i_4_r = new IconTmp();
					return new IconAssign().augment(IconOperators.plus).over(new IconSingleton(value_r), new IconOperation(IconOperators.times).over(new IconIndexIterator(row1_r, rowIndex_r), new IconProduct(new IconIn(i_4_r, new IconIndexIterator(m2_r, rowIndex_r)), new IconIndexIterator(i_4_r, colIndex_r))));
				}
 )), new IconInvokeIterator(()-> ((VariadicFunction) put).apply(row_r.deref(), value_r.deref())));
			}
 )), new IconInvokeIterator(()-> ((VariadicFunction) put).apply(result_r.deref(), row_r.deref())));
		}
 )), new IconReturn(new IconSingleton(result_r)), new IconNullIterator(), new IconFail());
		// Return body after unpacking arguments 
		body_10.setCache(methodCache, "multiply_matrix_m");
		body_10.setUnpackClosure(unpack_11).unpackArgs(args_9);
		return body_10;
	}
	// Static main method
	public static void main(String... args_13) {
		Matrix c = new Matrix(); VariadicFunction m = (VariadicFunction) c.main;
		IconCoExpression.activate(null, null, new IconCoExpression(
		 (Object... coexpr) -> {
			return ((IIconIterator) m.apply((Object[]) args_13)); },
		 () -> { return IconList.createArray(); } ));
	}
}
