<?xml version="1.0"?>
<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
	xmlns:xalan="http://xml.apache.org/xalan"
	xmlns:exslt="http://exslt.org/common"
	xmlns:cmd="edu.uidaho.junicon.support.transforms.TransformSupport"
	exclude-result-prefixes = "cmd xalan exslt">

	<!-- USED xalan:nodeset() instead of exslt:node-set() -->
	<!--
	<xsl:include href="debug.xsl"/>
	-->
	<xsl:output method="xml" omit-xml-declaration="yes"/>

<!-- =========================================================================
  Copyright (c) 2012 Orielle, LLC.
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:

    1. Redistributions of source code must retain the above copyright notice,
       this list of conditions and the following disclaimer. 
    2. Redistributions in binary form must reproduce the above copyright notice,
       this list of conditions and the following disclaimer in the documentation
       and/or other materials provided with the distribution. 

  This software is provided by the copyright holders and contributors "as is"
  and any express or implied warranties, including, but not limited to, the
  implied warranties of merchantability and fitness for a particular purpose
  are disclaimed. In no event shall the copyright holder or contributors be
  liable for any direct, indirect, incidental, special, exemplary, or
  consequential damages (including, but not limited to, procurement of
  substitute goods or services; loss of use, data, or profits; or business
  interruption) however caused and on any theory of liability, whether in
  contract, strict liability, or tort (including negligence or otherwise)
  arising in any way out of the use of this software, even if advised of the
  possibility of such damage.
========================================================================== -->

<!-- ===================================================================== -->
<!-- Author: Peter Mills                                                   -->
<!-- ===================================================================== -->
	
<!--
#===========================================================================
# PURPOSE: Recursively rewrites primary expressions o.f(x).c[i](y) into normal
#	form, to make iteration explicit over nested generation expressions.
#	Transforms reduce primaries to iterator products over INVOKE p.f(x),
#	INDEX p.c[i], and simple OBJREF p.x.   ATOMs, if they are prefixed
#	in rewriting with objref p, are changed into simple OBJREF.
# EXAMPLE: (e).f(x,e') ==> (o in rw((e))) & (y in rw(e')) &
#	(z in @{o.deref().f(x,y.deref())})
# EXAMPLE: e.e'(x,y)[z] ==> (o in rw(e)) & (f in rw(o.e')) &
#	(x' in rw(x)) & (y' in rw(y)) & (r in f(x',y')) &
#	(z' in z) & (s in r[z'])
#
# Parameters
# ==========
# SIGNATURE: rw(e,prefix), or in XSLT, <template match=e <with-param prefix>>
# PARAMETERS:
#	prefix (objref-so-far) to prepend to rewritten expressions.
#		prefix carries the object reference
#		built using the last iterator variable, or previous identifier 
#		or operator, used for chaining together iterator products.
#		@deref indicates deref atom step, o@deref.x => o.deref().x
#	isInvokeStep if have already rewritten head of p(e') or p[e']
# SETS ATTRIBUTES:
#	ATOM, OBJREF: @isPrimaryField, @isArgument, @inLeftIterator, @isSimple
#		   @isAllocation if atom is "new C" rewritten into "C.new"
#	EXPRESSION: @endsInIterator,
#		    @lastBinding (last temporary identifier in iterator chain),
#		    @tmpVarName (synthesized temporary, EXPRESSION or STATEMENT)
#		    @isTmpVar for IconTmp instead of IconVar
#	DECLARATION: @isTmpSynthetic for synthesized temporary local variable
#		    @isTmpVar for IconTmp instead of IconVar
#	INVOKE:	@isOperator @symbol if invoke holds operator e.g. +(x,y)
#		   @isCommand if invoke holds command
#	INDEX: @tmpVarList @tmpVarIndex @tmpVarSlice - used to freeze index
#		(No longer done. OLD VERSION: before indexCreator.)
#
#	@isPrimaryField: if is atom in complex primary o.f(x).c[i] excluding
#		arguments, i.e., is in objref o.f or invokeHead c,
#		but excluding arguments x and i.
#		A simple objref o.f can only occur as a residue after rewriting,
#		and is treated the same as an identifier.
#		In o.f(p) & q.y, o.f is isPrimaryField,
#			while standalone q.y is not.
#		Similarly in f(p), f is isPrimaryField since it is invokeHead,
#			while p isArgument.
#	@isArgument: if is argument in invoke, index, or collection literal.
#		@isPrimaryField and @isArgument are mutually exclusive.
#		Example: o.f(x) where o and f are isPrimaryField,
#		while x isArgument but not isPrimaryField.
#	@inLeftIterator: if atom x is lhs (left-hand-side) of iterator (x in I).
#		x is not isPrimaryField, since it is not in objref nor in invokeHead.
#	@isSimple: if is identifier or literal atom, residual objref,
#		or collection literal, or if operator in invoke, e.g., +(x,y).
#	@endsInIterator: if product ends in iterator, e.g., (x in I) & (y in J).
#		Used by createBoundIterator, so if rewritten is inLeftIterator
#		(ends in iterator) or isSimple, do not rewrap as iterator.
#	@lastBinding: For a given expression that endsInIterator, lastBinding
#		carries the last iterator variable in the iterator product.
#		For a simple expression, i.e., an identifier,
#		it is just the identifier.
#		Lastbinding is always an identifier, not an object reference.
#		Lastbinding is used for chaining together iterator products.
#	@tmpVarName: name of temporary variable
#	@isOperator: if INVOKE holds operator or construct that has been changed
#		into a function call with a @symbol attribute holding operator,
#		for example "to(from,upto)" with @symbol=to
#	@isCommand: if INVOKE is rewritten from space-separated command line.
#	@isAllocation: if ATOM holds "new C" allocation that has been translated
#		into "C.new" for use as a function call "C.new()".
#		This is required to normalize function parameters in new.
#		Atom/DOTNAME/CLASSNAME will be classname without "new",
#		for example "x.y.new()" has INVOKE/ATOM/DOTNAME/CLASSNAME=x.y
# INTRODUCES ATTRIBUTES:
#	@lift if ATOM, LITERAL, GROUP, OBJREF, INVOKE, or INDEX should be lifted
#	@deref (ATOM[@deref]) if atom should be dereferenced,
#		and OBJREF[@deref] if first atom should be dereferenced,
#		which will be of form:  o.deref().x.y.z, or o.deref().
#	@isderef (ATOM[@isderef]) if preceeding atom should be dereferenced.
#		and this is the ".deref()" segment.
# NEW TAGS:
#   INVOKE/IDENTIFIER{@symbol} is function invocation using operator symbols.
#	This is used to allow certain infix operators to be normalized as
#	functions instead of transforming them into maps over iterator products.
#	      e to e' => to(e,e') => (x in e) & (y in e') & (z in @name(x,y))
#	@name is used as the function name, instead of the operator symbol.
#	Normalization need only add the setup transform:
#		e to e' => IDENTIFIER{@symbol}/to (e,e')
#	that will then normalized as a function invocation.
# TAG taxonomy: see Junicon.jjt grammar
#	DECLARATION @isParameter, @isGlobalVariable,
#		@isLocalVariable (or class field), @isClassOrMethod, @isTmpVar
#	TYPE : dotName used in import, package, superclass, new allocation
#	ATOM : identifier|dotName used in expression
# NOTATION: EXPR[@attr]/<f> means tag EXPR with attribute attr and child f.
#
# Action
# ======
# 1. Rewrite primaries e.e' | e(e')e" | e[e']e" proceeding left to right,
#		       for example f(x).c[i](y):
#	e.e'	    rw(OBJREF e.e', p) => (o in rw(e,p)) & rw(OBJREF e', <o>)
#	e(e')e"	    rw(INVOKE e(e')e", p) => (o in rw(e,p)) & rw"(INVOKE (e')e", <o>)
#	e[e']e"	    rw(INVOKE e[e']e", p) => (o in rw(e,p)) & rw"(INVOKE [e']e", <o>)
#	x	    rw(ATOM e, p) => OBJREF p.e - but only inside primary
#	p(e1,e2)e"  rw"(INVOKE (e1,e2)e", p) => (x in rw(e1,)) & (y in rw(e2,))
#			& (o in INVOKE dispatch(p)(<x>,<y>)) & rw"(e", <o>)
#	p[e1,e2]e" => p[e1][e2]e" # Rewrite multidimensional index
#	p[e]e"  rw"(INVOKE [e]e", p) => (x in rw(e)) & 
#			& (o in INDEX p[<x>) & rw"(e", <o>)
#	NOTE: e[SLICE/x..y] => same as above, but with [<i>..<j>]
#
#   where rw, rw" correspond to modes default and invokestep for primary, and
#
#   where we skip bound iterator creation (o in rw(e,p)) if e is
#	an identifier or literal, or a simple
#	object reference, method reference, or collection 
#	that uses only identifiers or literals,
#	and just use the prefixed original term p.e (or e if p is empty)
#	instead of <o> in the above products,
#   and where we also skip iterator creation if e ends in a created iterator,
#	and use the last binding of e instead of o in the above products.
#   Lastly, we skip bound iterator creation (o in rw(e,p)) for the last invoke
#	or index step in a primary, and just lift it, since there is no
#	further need for a bound variable, i.e.,
#	p[e'] => (x in rw(e',p) & !p[<x>].
#	For example, x.f(y)[z] ? (o in !x.f(y)) & !o[z].
#
#   and where <x> means deref variable or index x, i.e., x.deref()
#
#   We also skip over redundant parenthesis, e.g. (x.y).z or ((x))
#   in testing whether to skip iterator formation.
#
#   Must do for all x, all i when have multiple parameters: e(x,...) or e[i,...]
#	e.g., (y in rw(x)) & (y' in rw(x')) & ... & (z in f(y,y',...))
#
#   Prefix formation that skips iterator creation is described as follows:
#	if (endsInIterator) { lastbinding.deref()
#	} else {	// skipped create iterator, so e is simple
#	    if rw(e,p) is OBJREF { rw(e,p)  // rw(e) absorbed p
#	    } else {
#		if p!empty {p.}  // Skipped but did not absorb p (never happens)
#		if (rw(e,p) is ATOM or GROUP) { rw(e,p)	// since rw(e,p) == e
#		} else { lastbinding of rw(e,p) }
#	} }
#   NOTE: if rw(e,p) absorbed p (i.e., is OBJREF), we just use rw(e,p)
#   instead of p.e; otherwise use p.rw(e) == p.e since rw(e,p)==e and p=empty.
#		
# 2. Rewrite collection literals into normal form:
#	LIST [e1, e2]   => (x in rw(e1)) & (y in rw(e2)) & [x,y]
#	MAP  [ek:ev, ...] => (k in rw(ek)) & (v in rw(ev)) & (...) & {k:v, ...}
#	SET  {e1, e2}   => (x in rw(e1)) & (y in rw(e2)) & {x,y}
#
# 3. Lifting in later transforms
#
# Lift means to reify a primary using closures
# and then promote the result of its evaluation to an iterator.
# Later transforms after normalization will
# lift atoms, invocations, and literals to iterators as follows:
# (a) lift atoms (identifier, literal) to singleton iterators if not
#	isPrimaryField and not isArgument and not inLeftIterator (lhs of in).
# (b) lift objref (which will only be residual simple p.x) to singleton iterator
#	if not isPrimaryField and not isArgument in invoke, index, or collection
#		e.x => (p in e) & p.x			// lift p.x
#		p.f(x) => (z in p.f(x))			// not lift p.f
#		Can happen:    (x in I) & x.y     3(x)
#		Cannot happen: (x in I) & y     o.3(x)
# (c) lift collection literal to singleton iterator if not isArgument.
# (d) lift all invoke, index.
#=============================================================================
# XSLT how to's:
#=============================================================================
#   position() starts at 1, i.e., child index origin is 1.
#   boolean($foo) tests if string or nodeset is nonempty.
#   xalan:nodeset($foo)/*[1] should be used to access result tree fragment
#	formed from variable or apply-templates, if has single node.
#   node() matches text and element nodes, but not attribute nodes.
#   variable holding copy-of or apply-templates must be wrapped as single node,
#	e.g., <xsl:variable> <RESULT> apply-templates </RESULT> </xsl:variable>
#	Similarly, should wrap every template result in tag, not fragment set,
#	otherwise apply-template to collected results will ignore fragments.
#	e.g., template <copy> blah </copy>
#   match=foo/bar matches bar with a foo parent; foo can still match elsewhere.
#	The match context is the node being matched or one of its ancestors.
#   union x|y can only occur in the first step of a path, e.g. (x|y)/z
#	or inside a predicate, e.g. x[y | z]
#=============================================================================
-->

<!--
#=============================================================================
# Root template, top level.
#=============================================================================
-->

<!-- ==== Root template ==== -->
<xsl:template match="/" priority="2">
	<!--
	#====
	# Preprocess Phase0.
	#====
	-->
	<xsl:variable name="preprocessedPhase0">
		<xsl:apply-templates select="*" mode="preprocessPhase0"/>
	</xsl:variable>

	<!--
	#====
	# Preprocess Phase1.
	#====
	-->
	<xsl:variable name="preprocessedPhase1">
		<xsl:apply-templates select="xalan:nodeset($preprocessedPhase0)/*[1]" mode="preprocessPhase1"/>
	</xsl:variable>

	<!--
	#====
	# Preprocess.
	#====
	-->
	<xsl:variable name="preprocessed">
		<xsl:apply-templates select="xalan:nodeset($preprocessedPhase1)/*[1]" mode="preprocess"/>
	</xsl:variable>

	<!--
	#====
	# Normalize program.
	#====
	-->
	<xsl:variable name="normalized">
		<xsl:apply-templates select="xalan:nodeset($preprocessed)/*[1]"/>
	</xsl:variable>

	<!--
	#====
	# Postprocess.
	#====
	-->
	<xsl:variable name="postprocessed">
		<xsl:apply-templates select="xalan:nodeset($normalized)/*[1]" mode="startPostprocessTopLevel"/>
	</xsl:variable>
	<xsl:copy-of select="xalan:nodeset($postprocessed)/*[1]"/>

	<!-- debug
	<xsl:variable name="printTitle" select="cmd:println('Debug')"/>
	<xsl:apply-templates select="xalan:nodeset($postprocessed)/*[1]" mode="printAll"/>
	-->

</xsl:template>

<!--
#=============================================================================
# Preprocess Phase0.
#  Change "method" to "procedure" if top-level and not embedded.
#  So can use "def" as "method" for outer function, under -java-syntax.
#=============================================================================
-->
<xsl:template match="STATEMENT[KEYWORD[string()='method']][not(ancestor::*[self::STATEMENT[KEYWORD[string()='class']]])]" mode="preprocessPhase0" priority="2">
  <xsl:variable name="isEmbedded" select="cmd:TisProperty('Properties', 'isEmbedded', 'true')"/>

  <xsl:copy>
  <xsl:copy-of select="@*"/>
  <xsl:choose>
    <xsl:when test="boolean($isEmbedded)">
	<xsl:apply-templates select="node()" mode="preprocessPhase0"/>
    </xsl:when>
    <xsl:otherwise>
	<KEYWORD><xsl:text>procedure</xsl:text></KEYWORD>
	<xsl:apply-templates select="node()[not(self::KEYWORD[string()='method'])]" mode="preprocessPhase0"/>
    </xsl:otherwise>
  </xsl:choose>
  </xsl:copy>
</xsl:template>

<!--
#====
# Default template to copy nodes through
#====
-->
<xsl:template match="@*|node()" mode="preprocessPhase0">
  <xsl:copy>
	<xsl:apply-templates select="@*|node()" mode="preprocessPhase0"/>
  </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Preprocess Phase1.
#   Change procedures to static methods, and move into a file's class.
#====
# Procedures are transformed to static methods.
# 1. For procedures in a file without a class, a surrounding class is created.
#	The name of the class is the filename,
#	or if no filename, with the name of the first procedure.
# 2. For procedures in a file with a class, they are moved into the class.
# ALGORITHM: Normalize Phase1 preprocess:
#	First procedure: If no class, createClassForProcedures.
#	First class: pulls in all procedures.
#	Procedure otherwise: delete it, since pulled into class.
#	createClassForProcedures: create class, pulls in procedures.
#		If filename use it for class name.
#		Otherwise use first procedure name for class name.
#	pullInProcedures: pulls in all procedures, transforms to methods
#	procedureAsStaticMethod:
#	   procedure foo(args) { body } => static method foo(args) { body }
#=============================================================================
-->

<!-- Create class for first procedure, if no classes -->
<xsl:template match="STATEMENT[KEYWORD[string()='procedure']][not(following-sibling::*[self::STATEMENT[KEYWORD[string()='procedure']]])]" mode="preprocessPhase1" priority="2">
    <xsl:if test="not(parent::*[STATEMENT[KEYWORD[string()='class']]])">
	<xsl:apply-templates select="parent::*" mode="createClassForProcedures"/>
    </xsl:if>
</xsl:template>

<!-- Pull procedures into first class, place at end of class inside block -->
<xsl:template match="STATEMENT[KEYWORD[string()='class']][not(following-sibling::*[self::STATEMENT[KEYWORD[string()='class']]]) and parent::*[STATEMENT[KEYWORD[string()='procedure']]]]" mode="preprocessPhase1" priority="2">
  <xsl:copy>
  <xsl:copy-of select="@*"/>
	<xsl:apply-templates select="node()" mode="classPullInProcedures">
		<xsl:with-param name="root" select="parent::*"/>
	</xsl:apply-templates>
  </xsl:copy>
</xsl:template>

<!-- Delete procedures since absorbed into class -->
<xsl:template match="STATEMENT[KEYWORD[string()='procedure']]" mode="preprocessPhase1" priority="1">
</xsl:template>

<!--
#====
# Place initial clause inside block, so can move it to static class initializer.
#	initial expr; => initial {expr};
#====
-->
<!-- Initial clauses use block to enclose temporaries, so can move them -->
<xsl:template match="STATEMENT[KEYWORD[string()='initial']][not(EXPRESSION/GROUP/BLOCK)]" mode="preprocessPhase1" priority="1">
  <xsl:copy>
  <xsl:copy-of select="@*"/>
    <KEYWORD>initial</KEYWORD>
    <EXPRESSION>
	<GROUP>
	    <BLOCK>
		<DELIMITER ID="{"/>
		    <xsl:apply-templates select="EXPRESSION" mode="preprocessPhase1"/>
		<DELIMITER ID="}"/>
	    </BLOCK>
	</GROUP>
    </EXPRESSION>
    <DELIMITER ID=";"/>
  </xsl:copy>
</xsl:template>

<!--
#====
# Pull procedures as static methods into existing class, place inside block.
#====
-->
<!-- Pull procedures into first class, place at end of class -->
<xsl:template match="BLOCK" mode="classPullInProcedures" priority="2">
  <xsl:param name="root"/>

  <xsl:copy>
  <xsl:copy-of select="@*"/>
	<xsl:copy-of select="DELIMITER[@ID='{']"/>
	<xsl:apply-templates select="node()[not(self::DELIMITER[@ID='{' or @ID='}'])]" mode="preprocessPhase1"/>
	<xsl:apply-templates select="$root" mode="pullInProcedures"/>
	<xsl:copy-of select="DELIMITER[@ID='}']"/>
  </xsl:copy>
</xsl:template>

<!-- Default template to preprocess non-block nodes -->
<xsl:template match="@*|node()" mode="classPullInProcedures">
   <xsl:apply-templates select="." mode="preprocessPhase1"/>
</xsl:template>

<!--
#====
# Create class for procedure, and pull in all procedures as static methods.
# Adds attribute: STATEMENT[KEYWORD[class]][@isProcedure]
#====
-->
<xsl:template match="*" mode="createClassForProcedures">

  <xsl:variable name="hasFilename" select="boolean(cmd:ThasProperty('Properties', 'filename'))"/>
  <!-- Get filename, deleting any .suffix -->
  <xsl:variable name="filename" select="substring-before(cmd:TgetProperty('Properties', 'filename'), '.')"/>

  <xsl:variable name="classname">
    <xsl:choose>
      <xsl:when test="boolean($filename)">
	<!-- Get classname from filename, deleting any .suffix -->
	<xsl:value-of select="$filename"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="STATEMENT[KEYWORD[string()='procedure']][1]/DECLARATION/IDENTIFIER"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

    <STATEMENT>
	<xsl:attribute name="isProcedure">true()</xsl:attribute>
        <KEYWORD>class</KEYWORD>
        <DECLARATION isClassOrMethod="true">
            <IDENTIFIER>
		<xsl:value-of select="$classname"/>
	    </IDENTIFIER>
        </DECLARATION>
        <TUPLE>
            <DELIMITER ID="("/>
            <DELIMITER ID=")"/>
        </TUPLE>
        <BLOCK>
            <DELIMITER ID="{"/>
		<xsl:apply-templates select="." mode="pullInProcedures"/>
            <DELIMITER ID="}"/>
        </BLOCK>
        <DELIMITER ID=";"/>
    </STATEMENT>
</xsl:template>

<!--
#====
# Pull in procedures as static methods
# Adds attribute: STATEMENT[KEYWORD[static method]][@isProcedure]
#====
-->
<xsl:template match="STATEMENT[KEYWORD[string()='procedure']]" mode="pullInProcedures" priority="2">
  <xsl:copy>
  <xsl:copy-of select="@*"/>
  <xsl:attribute name="isProcedure">true()</xsl:attribute>
    <xsl:apply-templates select="node()" mode="procedureAsStaticMethod"/>
  </xsl:copy>
</xsl:template>

<!-- Default template to only take procedures -->
<xsl:template match="@*|node()" mode="pullInProcedures">
	<xsl:apply-templates select="@*|node()" mode="pullInProcedures"/>
</xsl:template>

<!--
#====
# Change procedure to static method
#====
-->
<xsl:template match="KEYWORD[string()='procedure']" mode="procedureAsStaticMethod" priority="2">
	<KEYWORD>
		<xsl:copy-of select="@*"/>
		<xsl:text>static</xsl:text>
	</KEYWORD>
	<KEYWORD>
		<xsl:text>method</xsl:text>
	</KEYWORD>
</xsl:template>

<!-- Default template to preprocess non-keyword nodes -->
<xsl:template match="@*|node()" mode="procedureAsStaticMethod">
	<xsl:apply-templates select="." mode="preprocessPhase1"/>
</xsl:template>

<!--
#====
# Handle binary operations with multiple operators that are left associative.
#	Example: 1+1-1 is parsed as OPERATION<1 OPERATOR+ 1 OPERATOR- 1>
#		and is left-associative as: (1+1)-1
#	LR parse has iteration as left associative, and recursion as right.
# OPERATION<x OPERATOR1 y OPERATOR2 z> ==>
#	OPERATION< EXPRESSION< recurse(OPERATION<x OPERATOR1 y>) OPERATOR2 z >>>
#====
-->
<xsl:template match="OPERATION[@isBinary][count(OPERATOR) > 1]" priority="2" mode="preprocessPhase1">

  <xsl:variable name="leftOperands">
    <EXPRESSION>
	<!-- Everything that is before a following operator -->
	<OPERATION>
	  <xsl:copy-of select="@*"/>
	  <xsl:copy-of select="node()[following-sibling::*[self::OPERATOR]]"/>
	</OPERATION>
    </EXPRESSION>
  </xsl:variable>

  <xsl:copy>
  <xsl:copy-of select="@*"/>
	<xsl:apply-templates select="xalan:nodeset($leftOperands)/EXPRESSION" mode="preprocessPhase1"/>
	<xsl:copy-of select="node()[not(following-sibling::*[self::OPERATOR])]"/>
  </xsl:copy>

</xsl:template>

<!--
#====
# Default template to copy nodes through
#====
-->
<xsl:template match="@*|node()" mode="preprocessPhase1">
  <xsl:copy>
	<xsl:apply-templates select="@*|node()" mode="preprocessPhase1"/>
  </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Preprocess.
#   Find variable declarations, for use in creating unique temporaries.
#   Wrap empty Atoms as expressions.
#   Translate constructs (to, new, commands) to function calls.
#	x to y by z => to(x,y,z) where may omit "by z"
#	command args => command(args)
#	new c.o => c.o.new as ATOM[@isAllocation]/DOTNAME/CLASSNAME.new
#   Introduced function calls are indicated by
#	INVOKE[@isOperator @isCommand @symbol (for operator)]
#	
#   Translate groups:
#	e{e1,...,e2} => e([create e1,...,create e2])	// co-expressions
#	(e,e') => e & e'
#	[: e :] => [: e		// list comprehension translated to operator
#   Translate data-parallel:
#	|<>| f(e) => |> parallel(<>f, <>e)
#   Wrap embedded script annotation @<script>{< foo >} as expression.
#   Translate records to classes: record x(y,z); => class x(y,z) { };
#=============================================================================
-->
<!--
#====
# Find all referenced or declared variables, and add to list of variable names.
#   Used to generate unique temporaries.
#   Only the first atom field of an OBJREF or METHODREF can be a reference. 
# Tags METHODREF: ATOM/METHODREF/
#		  DOTNAME/{ATOM[IDENTIFIER] (.ATOM[IDENTIFIER])*} :: IDENTIFIER
#====
-->
<xsl:template match="ATOM[IDENTIFIER][not((parent::OBJECT or parent::DOTNAME) and (position() &gt; 1))] | DECLARATION[IDENTIFIER]" mode="preprocess">
	<xsl:variable name="varName">
		<xsl:value-of select="*/text()"/>
	</xsl:variable>
	<xsl:variable name="isUnique" select="cmd:TaddUniqueName($varName)"/>

	<xsl:copy-of select="."/>
</xsl:template>

<!--
#================================================================
# If e is EMPTY, change to null.
#    ATOM[@emptyInner or @emptyTrailing] => ATOM[@emptyInner]/LITERAL/null
#    ATOM[@allEmpty]                     => ATOM[@allEmpty]  /LITERAL/null
#================================================================
-->
<xsl:template match="ATOM[@emptyInner or @emptyTrailing]" mode="preprocess" priority="2">
	<ATOM emptyInner="true()">
		<LITERAL isWord="true">null</LITERAL>
	</ATOM>
</xsl:template>

<xsl:template match="ATOM[@allEmpty]" mode="preprocess" priority="2">
	<ATOM allEmpty="true()">
		<LITERAL isWord="true">null</LITERAL>
	</ATOM>
</xsl:template>


<!--
#================================================================
# Translate records to classes: record x(y,z); => class x(y,z) { };
# TAGS record: STATEMENT/< KEYWORD[record] DECLARATION[id] TUPLE DELIMITER[;] >
#	@isRecord=true
#================================================================
-->
<xsl:template match="STATEMENT[KEYWORD[string()='record']]" mode="preprocess" priority="2">
  <xsl:copy>
	<xsl:copy-of select="@*"/>
	<xsl:attribute name="isRecord">
		<xsl:value-of select="true()"/>
	</xsl:attribute>
	<KEYWORD>class</KEYWORD>
	<!-- xsl:attribute abbreviation: name="{$var}" -->
	<xsl:apply-templates select="DECLARATION" mode="preprocess"/>
	<xsl:apply-templates select="TUPLE" mode="preprocess"/>
	<BLOCK>
	    <DELIMITER ID="{"/>
	    <DELIMITER ID="}"/>
	</BLOCK>
	<DELIMITER ID=";"/>
  </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Translate certain operators (to, !), commands, and new to function calls.
#=============================================================================
-->

<!--
#================================================================
# Translate certain operations, control constructs, and &keywords
#	to function calls.
# If the keyword or operator symbol is in OperatorAsFunction
# or OperatorAsGenerator,
# treats operator as a generator function over values or atoms that returns an
# iterator, and thus in normalization will extract arguments to
# make iteration explicit.
# Examples include prototypical generator functions such as "to" and "!".
# These non-monogenic generators are thus translated to ersatz functions,
# i.e., synthetic higher-order functions.
#
# Example: Translate x to y by z => to(x,y,z) where may omit "by z".
# TAGS: OPERATION/EXPR KEYWORD[to] EXPR
#================================================================
-->
<xsl:template match="OPERATION | STATEMENT[@isControl] | ATOM[ANDKEYWORD]" mode="preprocess" priority="2">
  <xsl:variable name="symbol" select="OPERATOR[1] | KEYWORD[1] | ANDKEYWORD[1]"/>
  <xsl:variable name="symbolText">
	<xsl:value-of select="OPERATOR[1] | KEYWORD[1] | ANDKEYWORD[1]"/>
  </xsl:variable>
  <xsl:variable name="hasOperatorAsFunction" select="cmd:ThasProperty('OperatorAsFunction', $symbolText)"/>
  <xsl:variable name="hasOperatorAsGenerator" select="cmd:ThasProperty('OperatorAsGenerator', $symbolText)"/>
  <xsl:choose>
    <xsl:when test="boolean($hasOperatorAsGenerator) or boolean($hasOperatorAsFunction)">
	<xsl:apply-templates select="." mode="createFunction">
		<xsl:with-param name="symbol" select="$symbol"/>
	</xsl:apply-templates>
    </xsl:when>
    <xsl:otherwise>
	<xsl:copy>
	<xsl:copy-of select="@*"/>
		<xsl:apply-templates select="node()" mode="preprocess"/>
	</xsl:copy>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>
  
<!--
#============================================================================
# Creates function from operation or control statement, in preprocess mode.
#	Takes */EXPRESSION => INVOKE op(EXPR1,EXPR2,...)
#============================================================================
-->
<xsl:template match="*" mode="createFunction" priority="2">
  <xsl:param name="symbol" select="OPERATOR[1] | KEYWORD[1] | ANDKEYWORD[1]"/>

  <xsl:variable name="args">
      <ARGS>
	<xsl:if test="not(self::ATOM[ANDKEYWORD])">
	  <xsl:apply-templates select="*[not(self::OPERATOR or self::KEYWORD or self::DELIMITER)]" mode="preprocess"/>
	</xsl:if>
      </ARGS>
  </xsl:variable>

  <EXPRESSION>
    <INVOKE isOperator="true()" symbol="{$symbol}">
	<ATOM>
	    <IDENTIFIER>
		<xsl:copy-of select="$symbol/@*"/>
		<xsl:value-of select="$symbol"/>
	    </IDENTIFIER>
	</ATOM>
	<TUPLE>
	    <DELIMITER ID="("/>
		<xsl:apply-templates select="xalan:nodeset($args)/ARGS/*" mode="extractParamsToList">
			<xsl:with-param name="justCopy" select="true()"/>
		</xsl:apply-templates>
	    <DELIMITER ID=")"/>
	</TUPLE>
    </INVOKE>
  </EXPRESSION>
</xsl:template>

<!--
#================================================================
# Translate f!x => f(IconList.listToArray(x))
#================================================================
-->
<xsl:template match="OPERATION[@isBinary and OPERATOR[string()='!']]" mode="preprocess" priority="3">
    <EXPRESSION>
	<INVOKE>
	  <xsl:apply-templates select="*[1]" mode="preprocess"/>
	  <TUPLE>
	    <DELIMITER ID="("/>
	    <EXPRESSION>
	      <OBJREF>
		<ATOM>
		    <IDENTIFIER>IconList</IDENTIFIER>
		</ATOM>
		<DELIMITER ID="."/>
		<INVOKE>
		    <ATOM>
			<IDENTIFIER>listToArray</IDENTIFIER>
		    </ATOM>
		    <TUPLE>
			<DELIMITER ID="("/>
			<EXPRESSION>
			<xsl:apply-templates select="*[3]" mode="preprocess"/>
			</EXPRESSION>
			<DELIMITER ID=")"/>
		    </TUPLE>
		</INVOKE>
	      </OBJREF>
	    </EXPRESSION>
	    <DELIMITER ID=")"/>
	  </TUPLE>
        </INVOKE>
    </EXPRESSION>
</xsl:template>

<!--
#================================================================
# Translate command args => command(args)
#================================================================
-->
<xsl:template match="COMMAND" mode="preprocess">
	<xsl:variable name="args">
	   <ARGUMENTS>
		<xsl:apply-templates select="*[position() > 1]" mode="preprocess"/>
	   </ARGUMENTS>
	</xsl:variable>

	<INVOKE isCommand="true()">
		<!-- transform will pull in prefix -->
		<xsl:copy-of select="*[1]"/>
		<TUPLE>
			<DELIMITER ID="("/>
			<xsl:apply-templates select="xalan:nodeset($args)/ARGUMENTS/*" mode="extractParamsToList">
			    <xsl:with-param name="justCopy" select="true()"/>
			</xsl:apply-templates>
			<DELIMITER ID=")"/>
		</TUPLE>
	</INVOKE>
</xsl:template>

<!--
#================================================================
# Translate new c.o => c.o.new as ATOM[@isAllocation]/DOTNAME/CLASSNAME.new
#================================================================
-->
<xsl:template match="ALLOCATION" mode="preprocess">
	<ATOM isAllocation="true()">
	    <DOTNAME>
		<CLASSNAME>
			<xsl:copy-of select="TYPE/IDENTIFIER | TYPE/DOTNAME/*"/>
		</CLASSNAME>
		<DELIMITER ID="."/>
		<IDENTIFIER>
			<xsl:copy-of select="KEYWORD[string() = 'new']/@*"/>
			<xsl:text>new</xsl:text>
		</IDENTIFIER>
	    </DOTNAME>
	</ATOM>
</xsl:template>

<!--
#================================================================
# Translate e{e1,e2} => e([create e1,create e2])  for co-expression
#================================================================
-->
<xsl:template match="SET[parent::INVOKE]" mode="preprocess">
	<TUPLE>
	    <xsl:copy-of select="@*"/>
	    <DELIMITER ID="("/>
	    <EXPRESSION> <GROUP> <LIST>
	    <DELIMITER ID="["/>

	    <!-- Keep expressions and commas -->
	    <xsl:apply-templates select="*[not(self::DELIMITER[@ID='{' or @ID='}'])]" mode="abbreviateCoexpr"/>

	    <DELIMITER ID="]"/>
	    </LIST> </GROUP> </EXPRESSION>
	    <DELIMITER ID=")"/>
	</TUPLE>
</xsl:template>

<xsl:template match="DELIMITER" mode="abbreviateCoexpr" priority="2">
	<xsl:copy-of select="."/>
</xsl:template>

<xsl:template match="*" mode="abbreviateCoexpr">
	<EXPRESSION>
	<STATEMENT control="true">
	<KEYWORD>create</KEYWORD>
		<xsl:apply-templates select="." mode="preprocess"/>
	</STATEMENT>
	</EXPRESSION>
</xsl:template>

<!--
#================================================================
# Translate (e,e') => e & e'
#================================================================
-->
<xsl:template match="GROUP[TUPLE[DELIMITER[@ID=',']]]" mode="preprocess">
        <PRODUCT>
	    <xsl:copy-of select="@*"/>
	    <!-- Keep expressions and commas -->
	    <xsl:apply-templates select="*[not(self::DELIMITER[@ID='(' or @ID=')'])]" mode="abbreviateProduct"/>
        </PRODUCT>
</xsl:template>

<xsl:template match="DELIMITER[@ID=',']" mode="abbreviateProduct" priority="2">
	<!-- <DELIMITER ID="&amp;"/> -->
	<OPERATOR>
		<xsl:text>&amp;</xsl:text>
	</OPERATOR>
</xsl:template>

<xsl:template match="*" mode="abbreviateProduct">
	<xsl:apply-templates select="." mode="preprocess"/>
</xsl:template>

<!--
#================================================================
# Translate [: e :] => [: e
#	Unary operation for list comprehension.
#================================================================
-->
<xsl:template match="GROUP[COMPREHENSION]" mode="preprocess">
    <EXPRESSION>
	<OPERATION isUnary="true">
	    <OPERATOR>[:</OPERATOR>
	    <xsl:apply-templates select="COMPREHENSION/EXPRESSION" mode="preprocess"/>
	</OPERATION>
    </EXPRESSION>
</xsl:template>

<!--
#================================================================
# Translate: case expr of { expr1:expr2 ; default:exprn }
#	=> if (x:=expr) then { if (x === expr1) then expr2 else
#		{ if ... else exprn | fail } }
# TAGS: STATEMENT/<EXPR BLOCK/KEYVALUE/<EXPR|KEYWORD[default] : EXPR> >
# Algorithm:
# Generate tmpvar ; Save default
#   if <EXPR @tmpVar> <x := expr>
#   then <EXPR> apply-templates: caseToIf over BLOCK/KEYVALUE [not default]
# 		with-param name = tmpvar, default
#================================================================
-->
<xsl:template match="STATEMENT[KEYWORD[string()='case']]" mode="preprocess" priority="3">
  <xsl:variable name="tmpVarName" select="cmd:TgetNewUnique('c')"/>

  <!-- Put default last -->
  <xsl:variable name="remainderCase">
    <BLOCK>
	<xsl:copy-of select="BLOCK/*[self::KEYVALUE[not(KEYWORD[string()='default'])]]"/>
	<xsl:copy-of select="BLOCK/*[self::KEYVALUE[KEYWORD[string()='default']]]"/>
    </BLOCK>
  </xsl:variable>

  <xsl:copy>
  <xsl:copy-of select="@*"/>
  <xsl:attribute name="tmpVarName">
	<xsl:value-of select="$tmpVarName"/>
  </xsl:attribute>
	<KEYWORD>if</KEYWORD>
	<EXPRESSION> <GROUP> <TUPLE>
		<DELIMITER ID="("/>
		<EXPRESSION> <ASSIGN>
		    <ATOM>
			<IDENTIFIER>
			    <xsl:value-of select="$tmpVarName"/>
			</IDENTIFIER>
		    </ATOM>
		    <OPERATOR>:=</OPERATOR>
		    <xsl:apply-templates select="EXPRESSION" mode="preprocess"/>
		</ASSIGN> </EXPRESSION>
		<DELIMITER ID=")"/>
	</TUPLE> </GROUP> </EXPRESSION>
	<KEYWORD>then</KEYWORD>
	<EXPRESSION>
		<xsl:apply-templates select="xalan:nodeset($remainderCase)/BLOCK" mode="caseToIf">
			<xsl:with-param name="tmpVarName" select="$tmpVarName"/>
		</xsl:apply-templates>
	</EXPRESSION>
  </xsl:copy>
</xsl:template>

<!--
#====
# caseToIf: if default, then default/expr
#	    if just empty, "&fail"
#   Otherwise: if <x === expr1> then expr2 else
#	       apply-templates caseToIf over KEYVALUE[* > 1][not default]
# TAGS: BLOCK/KEYVALUE/<EXPR|KEYWORD[default] : EXPR>
#====
-->
<!-- Default-only case BLOCK -->
<xsl:template match="BLOCK[KEYVALUE[KEYWORD[string()='default']]]" mode="caseToIf" priority="2">
    <xsl:apply-templates select="KEYVALUE[KEYWORD[string()='default']]/EXPRESSION" mode="preprocess"/>
</xsl:template>

<!-- Empty case BLOCK -->
<xsl:template match="BLOCK[not(KEYVALUE)]" mode="caseToIf" priority="2">
  <EXPRESSION>
	<ATOM> <ANDKEYWORD>
		<OPERATOR>&amp;</OPERATOR>
		<KEYWORD>fail</KEYWORD>
	</ANDKEYWORD> </ATOM>
  </EXPRESSION>
</xsl:template>

<!-- Nonempty case BLOCK -->
<xsl:template match="BLOCK[KEYVALUE[not(KEYWORD[string()='default'])]]" mode="caseToIf" priority="3">
  <xsl:param name="tmpVarName"/>

  <xsl:variable name="remainderCase">
    <BLOCK>
	<xsl:copy-of select="*[position() > 1]"/>
    </BLOCK>
  </xsl:variable>

  <STATEMENT isControl="true">
	<KEYWORD>if</KEYWORD>
	<EXPRESSION> <GROUP> <TUPLE>
		<DELIMITER ID="("/>
		<EXPRESSION> <OPERATION isBinary="true">
		    <ATOM>
			<IDENTIFIER>
				<xsl:value-of select="$tmpVarName"/>
			</IDENTIFIER>
		    </ATOM>
		    <OPERATOR isBoolean="true">
			<xsl:text>===</xsl:text>
		    </OPERATOR>
		    <xsl:apply-templates select="KEYVALUE[1]/*[1]" mode="preprocess"/>
		</OPERATION> </EXPRESSION>
		<DELIMITER ID=")"/>
	</TUPLE> </GROUP> </EXPRESSION>
	<KEYWORD>then</KEYWORD>
		    <xsl:apply-templates select="KEYVALUE[1]/*[3]" mode="preprocess"/>
	<KEYWORD>else</KEYWORD>
	<EXPRESSION>
		<xsl:apply-templates select="xalan:nodeset($remainderCase)/BLOCK" mode="caseToIf">
			<xsl:with-param name="tmpVarName" select="$tmpVarName"/>
		</xsl:apply-templates>
	</EXPRESSION>
  </STATEMENT>
</xsl:template>

<!--
#================================================================
# Translate data-parallel.
#	|<>| f(e) => |> parallel(<>f, <>e)
# Picks up name for "parallel" function from Spring OperatorAsFunction.
# Must be higher priority than template to change operator to function,
# since are in addition threading it off.
#================================================================
-->
<xsl:template match="OPERATION[OPERATOR[string()='|&lt;&gt;|']]" mode="preprocess" priority="3">
  <xsl:variable name="symbol" select="OPERATOR[1]"/>

  <!-- Use Spring property to configure -->
  <xsl:variable name="hasAsFunction" select="cmd:ThasProperty('OperatorAsFunction', $symbol)"/>

  <xsl:copy>
  <xsl:copy-of select="@*"/>
            <OPERATOR>|&gt;</OPERATOR>
            <INVOKE>
                <ATOM>
                    <IDENTIFIER>
  <xsl:choose>
    <xsl:when test="boolean($hasAsFunction)">
	<xsl:variable name="asFunction" select="cmd:TgetProperty('OperatorAsFunction', $symbol)"/>
	<xsl:value-of select="$asFunction"/>
    </xsl:when>
    <xsl:otherwise>
	<xsl:text>parallel</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
		    </IDENTIFIER>
                </ATOM>
                <TUPLE>
                    <DELIMITER ID="("/>
                    <EXPRESSION>
                        <OPERATION isUnary="true">
                            <OPERATOR>&lt;&gt;</OPERATOR>
			    <!-- insert "f" -->
			    <xsl:apply-templates select="INVOKE/*[1]" mode="preprocess"/>
                        </OPERATION>
                    </EXPRESSION>
                    <DELIMITER ID=","/>
                    <EXPRESSION>
                        <OPERATION isUnary="true">
                            <OPERATOR>&lt;&gt;</OPERATOR>
			    <!-- insert "e" -->
			    <xsl:apply-templates select="INVOKE/TUPLE/*[not(self::DELIMITER[@ID='(' or @ID=')'])]" mode="preprocess"/>
			</OPERATION>
                    </EXPRESSION>
                    <DELIMITER ID=")"/>
                </TUPLE>
            </INVOKE>
  </xsl:copy>
</xsl:template>

<!-- 
#====
# Wrap embedded script annotation @<script>{< foo >} as expression.
#	Must do this in case script is inside argument list f(,)
#	since invoke rewrites EXPRESSION/*.
#====
-->
<xsl:template match="EXPRESSION[ANNOTATION]" mode="preprocess" priority="3">
    <xsl:param name="prefix"/>
    <EXPRESSION>
	<xsl:copy-of select="."/>
    </EXPRESSION>
</xsl:template>
<!--
<xsl:template match="EXPRESSION[ANNOTATION[QUALIFIED[DOTNAME[IDENTIFIER[string() = 'script']]]] and EXPRESSION[ATOM[LITERAL[@isBigLiteral]]]]" mode="preprocess" priority="3">
</xsl:template>
-->

<!--
#================================================================
# Default template to copy nodes through
#================================================================
-->
<xsl:template match="@*|node()" mode="preprocess">
	<xsl:copy>
	    	<xsl:apply-templates select="@*|node()" mode="preprocess"/>
	</xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Postprocess.
#   Create local declarations for all temporary variables within block scope.
#   Insert @lift attribute so we can see effect of normalization.
#	Lift all invoke and index, and lift atoms (identifier references,
#	literals), objref's, and collections
#	if not isPrimaryField or isArgument or inLeftIterator, and isSimple.
#	Must also test for previously existing iterators (x in I).
#   Create class unique cache name, @classCachename, for use by methods.
#	Also done for procedures which are turned into classes.
#	Also creates @rhsUnique for use when reify variables to IconVar closures
#   Create unique method name in class, @methodUniquename, for use in cache.
#   Create unique closure name in class, @closureUniquename, for use in cache.
# TAGS program: PROGRAM/ < STATEMENT|EXPRESSION DELIMITER; >
#=============================================================================
-->
<!--
#====
# Create top-level local declarations outside block scope.
# Top level output: PROGRAM / STATEMENT(locals;) EXPRESSION|STATEMENT DELIM(;)
#====
-->
<xsl:template match="*" mode="startPostprocessTopLevel" priority="1">
   <xsl:copy>
	<xsl:variable name="blockDepth" select="count(ancestor-or-self::BLOCK)"/> 
	<!-- Create temporaries for EXPRESSION and STATEMENT -->
	<xsl:apply-templates select="descendant-or-self::*[self::EXPRESSION or self::STATEMENT][@tmpVarName and ($blockDepth = count(ancestor::BLOCK))]" mode="createLocal">
		<!-- create rhsUnique attribute since top-level -->
		<xsl:with-param name="createRhsUnique" select="true()"/>
	</xsl:apply-templates>

	<!-- process top-level STATEMENT | EXPRESSION -->
	<xsl:apply-templates select="*" mode="postprocess">
		<xsl:with-param name="isTopLevel" select="true()"/>
	</xsl:apply-templates>
   </xsl:copy>
</xsl:template>

<!--
#====
# Create local declarations for all tmpVarName within block scope.
#====
-->
<xsl:template match="BLOCK" mode="postprocess" priority="2">
    <xsl:copy>
	<xsl:copy-of select="@*"/>
	<xsl:if test="self::BLOCK[not(@isClosure)]">
		<DELIMITER ID="{"/>
	</xsl:if>
	<xsl:variable name="blockDepth" select="count(ancestor-or-self::BLOCK)"/> 

	<!-- Create temporaries for tmpVarName in EXPRESSION and STATEMENT -->
	<xsl:apply-templates select="descendant-or-self::*[self::EXPRESSION or self::STATEMENT][@tmpVarName and ($blockDepth = count(ancestor::BLOCK))]" mode="createLocal"/>

	<xsl:apply-templates select="*[not(self::DELIMITER[@ID='{' or @ID='}'])]" mode="postprocess"/>
	<xsl:if test="self::BLOCK[not(@isClosure)]">
		<DELIMITER ID="}"/>
	</xsl:if>
    </xsl:copy>
</xsl:template>

<!--
#====
# Insert @lift attribute.
#   Lift if invoke or index, or if
#   simple identifier, literal, objref, methodref, or collection
#   that is not a primary field or argument.
#   (i.e., if @isSimple and not(@isPrimaryField or @isArgument) ).
#   Collection will only be left as argument or leading primary field if simple.
#   Do not lift identifier in lhs of created iterator,
#   or in lhs of existing iterator.
#   ANDKEYWORD may or may not be simple, depending on Spring configuration map.
#====
-->
<xsl:template match="INVOKE | INDEX | ATOM[IDENTIFIER | DOTNAME | METHODREF | LITERAL | ANDKEYWORD][@isSimple and not(@isPrimaryField or @isArgument or @inLeftIterator or parent::EXPRESSION[parent::TUPLE[parent::ITERATOR]])] | OBJREF[not(@isPrimaryField or @isArgument)] | GROUP[LIST | MAP | SET][not(@isPrimaryField or @isArgument)]" mode="postprocess" priority="2">
	<xsl:copy>		
		<xsl:copy-of select="@*"/>
		<xsl:attribute name="lift">true()</xsl:attribute>
		<xsl:copy-of select="node()"/>
		<!-- <xsl:apply-templates select="node()" mode="postprocess"/>
		-->
	</xsl:copy>		
</xsl:template>

<!--
#====
# In top-level statements or expressions, create:
#   @classCachename class unique cache name, for use by methods.
#   @staticClassCacheName, @initialClassCacheName.
#   @rhsUnique for use when reify varaibles to IconVar closures.
#   @argsUniquename, @xOperandUnique, @yOperandUnique.
#   @bodyUniquename, @unpackUniquename, @unpackArgsUniquename
#====
-->
<xsl:template match="STATEMENT | EXPRESSION" mode="postprocess" priority="2">
    <xsl:param name="isTopLevel" select="false()"/>
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:if test="boolean($isTopLevel)">

	<!-- methodCache: create unique cache name -->
	<xsl:attribute name="classCachename">
		<xsl:value-of select="cmd:TgetReusableUnique('methodCache')"/>
	</xsl:attribute>

	<!-- staticMethodCache: create unique cache name for static methods -->
	<xsl:attribute name="classStaticCachename">
		<xsl:value-of select="cmd:TgetReusableUnique('staticMethodCache')"/>
	</xsl:attribute>

	<!-- classInitialCachename: static method initializer cache -->
	<xsl:attribute name="classInitialCachename">
		<xsl:value-of select="cmd:TgetReusableUnique('initialMethodCache')"/>
	</xsl:attribute>

      </xsl:if>

      <!-- For procedure, set up methodname -->
      <xsl:if test="self::STATEMENT[KEYWORD[string() = 'procedure']]">
	<xsl:variable name="methodname">
		<xsl:value-of select="DECLARATION[IDENTIFIER]"/>
	</xsl:variable>

	<!-- create unique method name -->
	<xsl:attribute name="methodUniquename">
		<xsl:value-of select="cmd:TgetMinimalUnique($methodname, '_m')"/>
	</xsl:attribute>
      </xsl:if>

      <!-- For method, set up methodname -->
      <xsl:if test="self::STATEMENT[KEYWORD[(string() = 'method') or (string() = 'initially')]]">
	<xsl:variable name="methodname">
		<xsl:value-of select="DECLARATION[IDENTIFIER] | KEYWORD[(string() = 'initially')]"/>
	</xsl:variable>

	<!-- create unique method name -->
	<xsl:attribute name="methodUniquename">
		<xsl:value-of select="cmd:TgetMinimalUnique($methodname, '_m')"/>
	</xsl:attribute>
      </xsl:if>

      <!-- <xsl:copy-of select="node()"/> -->
      <xsl:apply-templates select="node()" mode="postprocess"/>

    </xsl:copy>
</xsl:template>

<!--
#====
# Create @closureUniquename, unique closure name, for use in class cache.
#====
-->
<xsl:template match="CLOSURE" mode="postprocess" priority="2">
    <xsl:copy>
	<xsl:copy-of select="@*"/>

	<!-- create unique closure name in class -->
	<xsl:attribute name="closureUniquename">
		<xsl:value-of select="cmd:TgetMinimalUnique('closure')"/>
	</xsl:attribute>

	<!-- <xsl:copy-of select="node()"/> -->
	<xsl:apply-templates select="node()" mode="postprocess"/>
    </xsl:copy>
</xsl:template>

<!--
#====
# Default template to copy nodes through
#====
-->
<xsl:template match="@*|node()" mode="postprocess">
	<xsl:copy>
	    	<xsl:apply-templates select="@*|node()" mode="postprocess"/>
	</xsl:copy>
</xsl:template>

<!--
#====
# Create local declaration for temporary variable.
#	(No longer done:  OLD VERSION: before indexCreator.
#	 Also creates index locals if tmpVarIndex or tmpVarSlice is non-empty.)
# TAGS local: ENUM/DECLARATION/IDENTIFIER
#====
-->
<xsl:template match="*" mode="createLocal" priority="2">
   <xsl:param name="tmpVarName" select="@tmpVarName"/>
   <xsl:param name="isTmpVar" select="@isTmpVar"/>
   <xsl:param name="createRhsUnique" select="false()"/>
   <xsl:param name="didIndex" select="false()"/>

   <STATEMENT>
	<xsl:if test="boolean($createRhsUnique)">
	  <!-- rhs: create unique rhs variable name for IconVar closures -->
	  <xsl:attribute name="rhsUnique">
		<xsl:value-of select="cmd:TgetReusableUnique('rhs')"/>
	  </xsl:attribute>
	</xsl:if>

	<KEYWORD><xsl:text>local</xsl:text></KEYWORD>
	<ENUM>
	    <DECLARATION isLocalVariable="true()" isTmpSynthetic="true()" originalName="{$tmpVarName}">
		<xsl:if test="boolean($isTmpVar)">
		    <xsl:attribute name="isTmpVar">
			<xsl:value-of select="true()"/>
		    </xsl:attribute>
		</xsl:if>
		<IDENTIFIER>
		    <xsl:value-of select="$tmpVarName"/>
		</IDENTIFIER>
	    </DECLARATION>
	</ENUM>
	<DELIMITER ID=";"/>
   </STATEMENT>

</xsl:template>

<!--
#=============================================================================
# Normalize.
#=============================================================================
-->

<!--
#=============================================================================
# Rewrite ATOM in primary mode.
# Apply prefix to ATOM: if (prefix) <EXPR @lastBinding=e> OBJREF/p.e
#			else <ATOM @lastBinding=e>
# Prefix is of the form: OBJREF/{ATOM . ATOM ...}
#=============================================================================
-->
<!--
#====
# IDENTIFIER.  If e is identifier then just use it, set @lastBinding=e
#====
-->
<xsl:template match="ATOM[IDENTIFIER]" priority="2">
	<xsl:param name="prefix"/>

	<xsl:choose>
	    <xsl:when test="not(boolean($prefix))">
	      <xsl:copy>		
		<xsl:copy-of select="@*"/>
		<xsl:attribute name="isSimple">true()</xsl:attribute>
		<xsl:attribute name="lastBinding">
			<xsl:value-of select="IDENTIFIER/text()"/>
		</xsl:attribute>
		<xsl:copy-of select="node()"/>
	      </xsl:copy>		
	    </xsl:when>
	    <xsl:otherwise>
	      <OBJREF>
		<xsl:copy-of select="@*"/>
		<xsl:attribute name="isSimple">true()</xsl:attribute>
		<xsl:attribute name="lastBinding">
			<xsl:value-of select="IDENTIFIER/text()"/>
		</xsl:attribute>
		<xsl:copy-of select="$prefix/@deref"/>  <!-- @deref -->
		<xsl:copy-of select="$prefix/*"/>
		<DELIMITER ID="."/>
		<xsl:copy-of select="."/>
	      </OBJREF>
	    </xsl:otherwise>
	</xsl:choose>
</xsl:template>

<!--
#====
# ANDKEYWORD.  Translate to LITERAL or DOTNAME.
# &keyword symbols are translated using OperatorAsFunction, SymbolAsIterator,
#	SymbolAsValue, SymbolAsVariable, and SymbolAsProperty.
#	SymbolAsProperty is an IIconAtom with get() and set().
# If the symbol is in OperatorAsFunction it was already changed in normalization
#	to a no-arg function call, which is lifted later as an INVOKE.
# If the symbol is in SymbolAsValue/Variable/Property/Iterator
#	it is changed to a LITERAL or DOTNAME, respectively.
# Default is to map &keyword to SymbolAsVariable,
#	and prefix it with "IconKeyword.".
# Tags ANDKEYWORD: ATOM/ANDKEYWORD/<OPERATOR_& KEYWORD>
# Output Tags: ATOM [@symbolAsValue/Property/Variable/Iterator] /LITERAL|DOTNAME
#====
-->
<xsl:template match="ATOM[ANDKEYWORD]" priority="2">
  <xsl:param name="prefix"/>	<!-- ignored -->

  <!-- pick up if is simple from Spring configuration map -->
  <xsl:variable name="symbol">
	<xsl:value-of select="ANDKEYWORD"/>
  </xsl:variable>

  <!-- Use Spring property to configure -->
  <xsl:variable name="hasAsValue" select="cmd:ThasProperty('SymbolAsValue', $symbol)"/>
  <xsl:variable name="hasAsVariable" select="cmd:ThasProperty('SymbolAsVariable', $symbol)"/>
  <xsl:variable name="hasAsProperty" select="cmd:ThasProperty('SymbolAsProperty', $symbol)"/>
  <xsl:variable name="hasAsIterator" select="cmd:ThasProperty('SymbolAsIterator', $symbol)"/>

  <xsl:copy>		
  <xsl:copy-of select="@*"/>

  <xsl:choose>
    <xsl:when test="boolean($hasAsValue)">
	<xsl:attribute name="symbolAsValue">true()</xsl:attribute>
	<xsl:attribute name="isSimple">true()</xsl:attribute>
	<xsl:attribute name="lastBinding">
	    <xsl:value-of select="cmd:TgetProperty('SymbolAsValue', $symbol)"/>
	</xsl:attribute>
	<LITERAL>
	    <xsl:value-of select="cmd:TgetProperty('SymbolAsValue', $symbol)"/>
	</LITERAL>
    </xsl:when>
    <xsl:when test="boolean($hasAsProperty)">
	<xsl:attribute name="symbolAsProperty">true()</xsl:attribute>
	<xsl:attribute name="isSimple">true()</xsl:attribute>
	<xsl:attribute name="lastBinding">
	    <xsl:value-of select="cmd:TgetProperty('SymbolAsProperty', $symbol)"/>
	</xsl:attribute>
	<DOTNAME>
	    <xsl:value-of select="cmd:TgetProperty('SymbolAsProperty', $symbol)"/>
	</DOTNAME>
    </xsl:when>
    <xsl:when test="boolean($hasAsIterator)">
	<xsl:attribute name="symbolAsIterator">true()</xsl:attribute>
	<!-- <xsl:attribute name="isSimple">false()</xsl:attribute> -->
	<xsl:attribute name="lastBinding">
	    <xsl:value-of select="cmd:TgetProperty('SymbolAsIterator', $symbol)"/>
	</xsl:attribute>
	<DOTNAME>
	    <xsl:value-of select="cmd:TgetProperty('SymbolAsIterator', $symbol)"/>
	</DOTNAME>
    </xsl:when>
    <xsl:when test="boolean($hasAsVariable)">
	<xsl:attribute name="symbolAsVariable">true()</xsl:attribute>
	<xsl:attribute name="isSimple">true()</xsl:attribute>
	<xsl:attribute name="lastBinding">
	    <xsl:value-of select="cmd:TgetProperty('SymbolAsVariable', $symbol)"/>
	</xsl:attribute>
	<DOTNAME>
	    <xsl:value-of select="cmd:TgetProperty('SymbolAsVariable', $symbol)"/>
	</DOTNAME>
    </xsl:when>
    <xsl:otherwise>
	<!-- Default is hasAsVariable -->
	<xsl:attribute name="symbolAsVariable">true()</xsl:attribute>
	<xsl:attribute name="isSimple">true()</xsl:attribute>
	<xsl:attribute name="lastBinding">
	    <xsl:value-of select="concat($prefixAndKeyword, substring($symbol, 2))"/>
	</xsl:attribute>
	<DOTNAME>
	    <xsl:value-of select="concat($prefixAndKeyword, substring($symbol, 2))"/>
	</DOTNAME>
    </xsl:otherwise>
  </xsl:choose>

  </xsl:copy>		
</xsl:template>

<!--
#====
# LITERAL. If e is literal in argument, then just use it, set @lastBinding=e
#====
-->
<xsl:template match="ATOM[LITERAL]" priority="2">
	<xsl:param name="prefix"/>	<!-- object ref head -->
	<xsl:copy>		
		<xsl:copy-of select="@*"/>
		<xsl:attribute name="isSimple">true()</xsl:attribute>
		<xsl:attribute name="lastBinding">
			<xsl:value-of select="LITERAL"/>
		</xsl:attribute>
		<xsl:copy-of select="node()"/>
	</xsl:copy>		
</xsl:template>

<!--
#====
# DOTNAME.  If e is dotname in command or new allocation, just use it.
#====
-->
<xsl:template match="ATOM[DOTNAME]" priority="2">
	<xsl:param name="prefix"/>	<!-- object ref head -->
	<xsl:copy>		
		<xsl:copy-of select="@*"/>
		<xsl:attribute name="isSimple">true()</xsl:attribute>
		<xsl:attribute name="lastBinding">
			<xsl:apply-templates select="." mode="toText"/>
		</xsl:attribute>
		<xsl:copy-of select="node()"/>
	</xsl:copy>		
</xsl:template>

<!--
#====
# METHODREF.
#====
-->
<xsl:template match="ATOM[METHODREF]" priority="2">
	<xsl:param name="prefix"/>	<!-- object ref head -->
	<xsl:copy>		
		<xsl:copy-of select="@*"/>
		<xsl:attribute name="isSimple">true()</xsl:attribute>
		<xsl:attribute name="lastBinding">
			<xsl:apply-templates select="." mode="toText"/>
		</xsl:attribute>
		<!--
		<xsl:copy-of select="node()"/>
		-->
		<xsl:apply-templates select="node()" mode="castToAttribute"/>
	</xsl:copy>		
</xsl:template>

<!-- 
#====
# EMBEDDED SCRIPT ANNOTATION.  Leave alone, transform will rewrite it later.
#====
-->
<xsl:template match="EXPRESSION[ANNOTATION[QUALIFIED[DOTNAME[IDENTIFIER[string() = 'script']]]] and EXPRESSION[ATOM[LITERAL[@isBigLiteral]]]]" priority="2">
	<xsl:param name="prefix"/>
	<xsl:copy-of select="."/>
</xsl:template>

<!--
#====
# ToText.  Printable value of node with delimiters.
#====
-->
<xsl:template match="DELIMITER" mode="toText" priority="2">
	<xsl:value-of select="@ID"/>
</xsl:template>
<xsl:template match="*" mode="toText" priority="1">
	<xsl:apply-templates select="node()" mode="toText"/>
</xsl:template>
<xsl:template match="text()" mode="toText">
	<xsl:value-of select="."/>
</xsl:template>

<!--
#====
# AddPrimaryAttributes.
# Add isPrimaryField or isArgument attributes to primary node.
#====
-->
<xsl:template match="*" mode="addPrimaryAttributes" priority="2">
	<xsl:param name="isPrimaryField"/> <!-- term in objref or invokeHead -->
	<xsl:param name="isArgument"/> <!-- in invoke, index, or collection -->
	<xsl:param name="cast"/>
	<xsl:param name="castText"/>
	<xsl:copy>		
		<xsl:copy-of select="@*"/>
		<xsl:if test="boolean($isPrimaryField)">
			<xsl:attribute name="isPrimaryField">true()</xsl:attribute>
		</xsl:if>
		<xsl:if test="boolean($isArgument)">
			<xsl:attribute name="isArgument">true()</xsl:attribute>
		</xsl:if>
		<xsl:if test="boolean($cast)">
			<xsl:attribute name="cast">
				<xsl:value-of select="$castText"/>
			</xsl:attribute>
		</xsl:if>
		<xsl:copy-of select="node()"/>
	</xsl:copy>		
</xsl:template>

<!--
#=============================================================================
# Skip over redundant parenthesis: (())=>(), (x)=>x, (x.y)=>x.y
#	((Cast) x) => (Cast) x
# TAGS parenthesis: GROUP/TUPLE/DELIMITER EXPRESSION... DELIMITER
# TAGS cast in parenthesis: GROUP/TUPLE/EXPRESSION/EXPRESSION<CAST expr>
#	Except: ((Cast) x).y::z which is DOTNAME<GROUP/TUPLE/EXPRESSION/CAST>
#=============================================================================
-->
<xsl:template match="GROUP[TUPLE[count(*) = 3][EXPRESSION[count(*) = 1][GROUP[TUPLE] | ATOM | OBJREF | GROUP[LIST | MAP | SET] | EXPRESSION[CAST][ATOM | GROUP[LIST | MAP | SET]]]]]" priority="2">
	<xsl:param name="prefix"/>
	<xsl:apply-templates select="*/*/*">
		<xsl:with-param name="prefix" select="$prefix"/>
	</xsl:apply-templates>
</xsl:template>

<!--
#=============================================================================
# Detect cast. Must be singular cast, possibly nested in parenthesis.
#	For example:  ((List) x.y.z) 
#
# Cast only matters for primaries.
# 	((Cast) expr).y.f(z)
# 	f((Cast) expr)
#	((Cast) x).y::f(z)  must be dotname.
# Cast is detected by createBoundIterator when rewriting a primary step.
# Otherwise, discard cast when rewrite.
# 1. EXPRESSION/<CAST expr> => EXPRESSION/expr[@cast=CAST]
# 2. Primary step is rewritten: then cast on "EXPRESSION" ==> tmp prefix | arg
# 3. Primary step is simple: then cast is on it.
# Thus, cast is on simple primary: OBJREF | ATOM | GROUP | LITERAL | METHODREF
#
# TAGS parenthesis: GROUP/TUPLE/DELIMITER EXPRESSION... DELIMITER
# TAGS cast: EXPRESSION<CAST/TUPLE/TYPE item>
#	Except: ((Cast) x).y::z which is DOTNAME<GROUP/TUPLE/EXPRESSION/CAST>
#=============================================================================
-->
<xsl:template match="EXPRESSION[CAST]" mode="detectCast" priority="2">
  <!-- Get item cast, instead of cast itself, and insert cast attribute -->
  <xsl:param name="getCastItem" select="false()"/>
  <xsl:choose>
    <xsl:when test="boolean($getCastItem)">
	<xsl:apply-templates select="*[2]" mode="insertCastAttribute">
		<xsl:with-param name="cast" select="CAST/TUPLE/TYPE"/>
	</xsl:apply-templates>
    </xsl:when>
    <xsl:otherwise>
	<xsl:copy-of select="CAST/TUPLE/TYPE"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- Drop past nested parentheses, or just parenthesized cast in OBJREF -->
<xsl:template match="GROUP[TUPLE[count(*) = 3][EXPRESSION[(count(*) = 1) or CAST]]]" mode="detectCast" priority="2">
  <xsl:param name="getCastItem" select="false()"/>
  <xsl:apply-templates select="*/*" mode="detectCast">
	<xsl:with-param name="getCastItem" select="$getCastItem"/>
  </xsl:apply-templates>
</xsl:template>

<!-- Drop past nested expressions -->
<xsl:template match="EXPRESSION[count(*) = 1]" mode="detectCast" priority="1">
  <xsl:param name="getCastItem" select="false()"/>
  <xsl:apply-templates select="*" mode="detectCast">
	<xsl:with-param name="getCastItem" select="$getCastItem"/>
  </xsl:apply-templates>
</xsl:template>

<!-- ==== Default template: stop -->
<xsl:template match="@*|node()" mode="detectCast">
  <xsl:param name="getCastItem" select="false()"/>
</xsl:template>

<!-- Insert cast attribute -->
<xsl:template match="*" mode="insertCastAttribute" priority="2">
  <xsl:param name="cast"/>
  <xsl:copy>		
  <xsl:copy-of select="@*"/>
	<xsl:if test="boolean($cast)">
		<xsl:attribute name="cast">
			<xsl:apply-templates select="$cast" mode="toText"/>
		</xsl:attribute>
	</xsl:if>
	<xsl:copy-of select="node()"/>
  </xsl:copy>		
</xsl:template>

<!--
#=============================================================================
# Discard cast when rewrite primary expression.
#=============================================================================
-->
<xsl:template match="EXPRESSION[CAST]" priority="2">
  <xsl:param name="prefix"/>

  <xsl:apply-templates select="*[2]">
	<xsl:with-param name="prefix" select="$prefix"/>
  </xsl:apply-templates>
</xsl:template>

<!--
#=============================================================================
# Change cast to attribute.
#   If detect cast, adds @Cast, and rewrites without cast.
#	EXPRESSION/<CAST expr> => EXPRESSION/expr[@cast=CAST]
#   Pulls up cast from nested parenthesis or nested EXPRESSION.
#=============================================================================
-->
<xsl:template match="*" mode="castToAttribute" priority="2">
  <xsl:variable name="castRTF">
	<xsl:apply-templates select="." mode="detectCast"/>
  </xsl:variable>

  <xsl:variable name="cast">
	<xsl:value-of select="xalan:nodeset($castRTF)/*[1]"/>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="boolean($cast)">
	<xsl:apply-templates select="." mode="detectCast">
		<xsl:with-param name="getCastItem" select="true()"/>
	</xsl:apply-templates>
    </xsl:when>
    <xsl:otherwise>
	<xsl:copy>
	<xsl:copy-of select="@*"/>
		<xsl:apply-templates select="node()" mode="castToAttribute"/>
	</xsl:copy>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- Default template to copy nodes through -->
<xsl:template match="@*|node()" mode="castToAttribute">
  <xsl:copy>
	<xsl:apply-templates select="@*|node()" mode="castToAttribute"/>
  </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Create bound iterator over rewritten primary expression:
#		e => (tmp in rw(e,prefix))
#	or just return e and not create bound iterator if e (endsInIterator ||
#		(isSimple && (isPrimaryField|isArgument|inLeftIterator)))
#   Since only attempt to createBoundIterator when isPrimaryField | isArgument
#	and never when inLeftIterator, above reduces to:
#	Not create iterator if e (endsInIterator || isSimple)
# CreateBoundIterator is the gateway to handle all terms in a primary expression
#		i.e., all primary fields and arguments.
#	for o.f(x), createBoundIterator will be called to handle each of o, f,
#	and x, and sets "isPrimaryField" (i.e., is inObjref | inInvokeHead),
#	or "isArgument" from its input parameters.
#	It also sets "inLeftIterator" for synthesized temporaries.
# Formally: e => <EXPR @lastBinding=o @endsInIterator> ITERATOR/(o in rw(e,p))
#		where o = get new tmp via static cut-through
# (1) Try to normalize e.
# (2) Skip iterator creation if simple and not a primary field or argument:
#     If e is identifier or if argument literal or simple objref, then just
#     use it, set @lastBinding=e and @isPrimaryField but not @endsInIterator.
# (3) Skip iterator creation if rewritten endsInIterator,
#		i.e., do not rewrap as iterator.
#	XX: e(x)[i].z(y) => o in ((f in e) & (z in f(x)) & (i in z[i])) & ...
# (4) Get tmp variable and create (tmp in rw(e,p))
# Transform must later create local declaration for temporaries (tmpVarName)
# Cast: rewrite(EXPRESSION/<CAST expr>) ==> rewrite(EXPRESSION/expr[@cast=CAST])
#	Cast is carried through to (tmp prefix | function arguments)
#=============================================================================
-->

<!-- ==== if e is expression, get tmp var, form iterator using normalized e -->
<xsl:template match="*" mode="createBoundIterator" priority="1">
	<xsl:param name="prefix"/>	<!-- object ref head -->
	<xsl:param name="doNotRewrite"/>  <!-- don't rewrite just copy entry -->
	<xsl:param name="isPrimaryField"/> <!-- term in objref or invokeHead -->
	<xsl:param name="isArgument"/>	<!-- in invoke, index, or collection -->
	<xsl:param name="doNotCreateIterator"/> <!-- just rewrite, add attr -->

	<xsl:variable name="rewritten">
	<xsl:choose>
	<xsl:when test="boolean($doNotRewrite)">
		<xsl:copy-of select="."/>
	</xsl:when>
	<xsl:otherwise>
	    <xsl:apply-templates select=".">
		<xsl:with-param name="prefix" select="$prefix"/>
	    </xsl:apply-templates>
	</xsl:otherwise>
	</xsl:choose>
	</xsl:variable>

	<xsl:variable name="castRTF">
		<xsl:apply-templates select="." mode="detectCast"/>
	</xsl:variable>

	<xsl:variable name="cast">
		<xsl:value-of select="xalan:nodeset($castRTF)/*[1]"/>
	</xsl:variable>

	<xsl:variable name="castText">
		<xsl:apply-templates select="xalan:nodeset($castRTF)/*[1]" mode="toText"/>
	</xsl:variable>

	<!--
	#====
	# Skip iterator creation
	# If rewritten endsInIterator or isSimple, ie, do not rewrap in iterator
	#   if (endsInIterator ||
	#	(isSimple && (isPrimaryField | isArgument | inLeftIterator)))
	# Have not yet extracted as argument, so must use $isArgument
	#====
	-->
	<xsl:choose>
	<xsl:when test="boolean($doNotCreateIterator) or xalan:nodeset($rewritten)/*[1][@endsInIterator or (@isSimple and (boolean($isPrimaryField) or boolean($isArgument) or @inLeftIterator))]">
		<!-- make sure to copy in isPrimaryField or isArgument attributes -->
		<xsl:apply-templates select="xalan:nodeset($rewritten)/*[1]" mode="addPrimaryAttributes">
			<xsl:with-param name="isPrimaryField" select="$isPrimaryField"/>
			<xsl:with-param name="isArgument" select="$isArgument"/>
			<xsl:with-param name="cast" select="$cast"/>
			<xsl:with-param name="castText" select="$castText"/>
		</xsl:apply-templates>
	</xsl:when>
	<xsl:otherwise>

	<!-- choose iterator tmp first letter, e.g., o.f(x).c[i] -->
	<xsl:variable name="beginTmp">	<!-- begin temporary with this -->
	  <IDENTIFIER>
	  <xsl:choose>
	    <xsl:when test="self::INVOKE/OBJREF"><xsl:text>o</xsl:text></xsl:when>
	    <xsl:when test="following-sibling::*[1][self::INVOKE]"><xsl:text>f</xsl:text></xsl:when>
	    <xsl:when test="following-sibling::*[1][self::SUBSCRIPT]"><xsl:text>i</xsl:text></xsl:when>
	    <xsl:when test="self::INVOKE"><xsl:text>x</xsl:text></xsl:when>
	    <xsl:when test="self::INDEX"><xsl:text>i</xsl:text></xsl:when>
	    <xsl:otherwise><xsl:text>x</xsl:text></xsl:otherwise>
	  </xsl:choose>
	  </IDENTIFIER>
	</xsl:variable>
	<xsl:variable name="beginTmpText">
		<xsl:value-of select="xalan:nodeset($beginTmp)/IDENTIFIER/text()"/>
	</xsl:variable>

	<!-- get tmp variable name -->
	<xsl:variable name="tmpVarName" select="cmd:TgetNewUnique($beginTmpText)"/>

	<!-- create iterator -->
	<EXPRESSION tmpVarName="{$tmpVarName}" isTmpVar="true()" lastBinding="{$tmpVarName}" endsInIterator="true()" isBoundIterator="true()">
	    <xsl:if test="boolean($isPrimaryField)">
		<xsl:attribute name="isPrimaryField">true()</xsl:attribute>
	    </xsl:if>
	    <xsl:if test="boolean($isArgument)">
		<xsl:attribute name="isArgument">true()</xsl:attribute>
	    </xsl:if>
	    <xsl:if test="boolean($cast)">
		<xsl:attribute name="cast">
			<xsl:value-of select="$castText"/>
		</xsl:attribute>
	    </xsl:if>

	    <ITERATOR>
	      <TUPLE>
		<DELIMITER ID="("/>
		<EXPRESSION>

		<!-- iterator variable inLeftIterator so won't lift -->
		<ATOM inLeftIterator="true()" isSimple="true()">	   
		  <IDENTIFIER>
		    <xsl:value-of select="$tmpVarName"/>
		  </IDENTIFIER>
		</ATOM>
		<KEYWORD><xsl:text>in</xsl:text></KEYWORD>
		<EXPRESSION>
		    <xsl:copy-of select="xalan:nodeset($rewritten)/*[1]"/>
		    <!-- not same as copy-of "$rewritten", drops attributes -->
		</EXPRESSION>

		</EXPRESSION>
		<DELIMITER ID=")"/>
	      </TUPLE>
	    </ITERATOR>
	</EXPRESSION>

	</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<!-- ==== Default template to copy thru for mode (attributes only) -->
<!--
<xsl:template match="@*|node()" mode="createBoundIterator">
        <xsl:copy>
		<xsl:apply-templates select="@*|node()" mode="createBoundIterator"/>
	</xsl:copy>
</xsl:template>
-->

<!--
#=============================================================================
# Rewrite primary: e.e' | e(e')e" | e[e']e"
#	Informally: e.e' => (o in rewrite(e,p)) & rewrite(e' with <o>. prefix)
# e.e' => (o in rw(e,p)) & rw(OBJREF/e', o.deref()) with head EXPR @lastBinding=o
#	where if e is identifier then skip iterator former, @lastBinding=e, and no deref
# e(e')e" | e[e']e" => (o in rw(e,p)) & rw"(INVOKE/e'e", o.deref())
#	with head EXPR @lastBinding=o, and with param=isInvokeStep on rw(e'e").
# 
# Rewrite invoke or index step: p(e)e' or p[e]e' where already rewrote head p
#	If isInvokeStep, then are rewriting p(e)[]... or p[e]()...
#	and have already rewritten head p.  In this case,
# rw": p(e)e' | p[e]e' => (o in rw(e,p)) & rw"(INVOKE/e', o.deref())
#	with param=isInvokeStep,
#   where rw" is mode=invokeStep with a separate template to rewrite p(e)
#   by extracting parameters and creating an invocation.
#====
# EXAMPLE: e.e'(x,y)[z] ==> (o in rw(e)) & (f in rw(o.e')) &
#	(x' in rw(x)) & (y' in rw(y)) & (r in f(x',y')) &
#	(z' in z) & (s in r[z'])
#====
# Rewrite carries last iterator variable in @lastBinding flag, used for chaining
#====
# TAGS invoke: INVOKE OBJREF|ATOM TUPLE/EXPRESSION
#	       INDEX  OBJREF|ATOM SUBSCRIPT/EXPRESSION
#=============================================================================
-->
<xsl:template match="OBJREF | INVOKE" priority="2">
	<xsl:param name="prefix"/>
	<xsl:param name="isLastObjrefStep" select="true()"/>
		<!-- if just INVOKE outside of OBJREF, then isLastObjrefStep -->

  <!--
  #====
  # Create iterator (x in rw(e1)) from first term e1 in e1.e2 or e1(e2)
  #	Leaves it alone if e1 is identifier or simple term.
  # rwFirst = rw(*[1],p) mode=createBoundIterator
  #====
  -->
	<xsl:variable name="rwFirstRTF">    <!-- RTF is result tree fragment -->
	    <!--
	    #====
	    # e.e" or e(e')e" or e[e]e" : do (o in e) and save in rwFirst.
	    # if rwFirst endsInIterator,
	    #	createBoundIterator does not rewrap as iterator.
	    # After that, will append .o to form prefix p,
	    #	then do rest of : p.e" or p(e')e" or p[e']e"
	    #====
	    -->
		<xsl:apply-templates select="*[1]" mode="createBoundIterator">
		    <xsl:with-param name="prefix" select="$prefix"/>
		    <xsl:with-param name="isPrimaryField" select="true()"/>
		</xsl:apply-templates>
	</xsl:variable>
  <!-- bindingFirstTerm = rwFirst[@lastBinding] -->
	<xsl:variable name="rwFirst" select="xalan:nodeset($rwFirstRTF)/*[1]"/>
	<xsl:variable name="bindingFirstTerm" select="$rwFirst/@lastBinding"/>

  <!--
  #====
  # Create prefix.
  # Form prefix from the first term's lastBinding if it endsInIterator,
  #	or from the previous prefix and the rewritten first term.
  #====
  -->
    <xsl:variable name="prefixSecond">
	<xsl:apply-templates select="$rwFirst" mode="createPrefix">
		<xsl:with-param name="prefix" select="$prefix"/>
	</xsl:apply-templates>
    </xsl:variable>

  <!--
  #====
  # Rewrite rest of expression e2 using prefix.
  #
  # OBJREF: a.b as {Expr Dot Expr}   where count(*)==3
  # INVOKE: f(x)[y] as {Expr Tuple Subscript}   where count(*)==3
  #         if invokeStep, is (x)[y] as {Tuple Subscript},
  #         	with prefix holding function or index name
  #	    f(x)[y] are done by re-invoking this template with invokeStep
  #		over (x)[y] and prefix=f, so will create iterator over f(x)
  #		and then form its product with rest which is 2ndPrefix[y]
  #====
  # rwRest = rw(rest, prefixSecond)    i.e., rewrite(e' with <o> prefix)
  #
  # if (current-tag = OBJREF)
  #	if (count(*) > 2) rest = OBJREF/*[position() > 2] else rest=*[1]
  #	rwRest = rw(rest,prefixSecond)
  # else  
  #	rest = INVOKE/*[position() > 1]
  #	rwRest = rw(rest,prefixSecond) param=isInvokeStep
  #====
  -->
    <xsl:variable name="rwRestRTF">

	<!--
	#====
	# Do rest of: e" for p.e"
	#====
	-->
	<xsl:if test="self::OBJREF">
	    <xsl:variable name="rest">
		<!-- Skip over a. in a.b or a.b.c -->
		<xsl:choose>
		  <!-- We have a.b.c and not a.b -->
		  <xsl:when test="count(*) > 3">
			<!-- Skip over a. in a.b.c leaving b.c -->
			<OBJREF>
				<xsl:copy-of select="*[position() > 2]"/>
			</OBJREF>
		  </xsl:when>
		  <!-- We have a.b and not a.b.c -->
		  <xsl:otherwise>
			<!-- Skip over a. in a.b leaving b -->
			<xsl:copy-of select="*[position() > 2]"/>
		  </xsl:otherwise>
		</xsl:choose>
	    </xsl:variable>
	    <xsl:apply-templates select="xalan:nodeset($rest)/*[1]">
		<xsl:with-param name="prefix" select="xalan:nodeset($prefixSecond)/*[1]"/>
		<xsl:with-param name="isLastObjrefStep" select="count(*) &lt;= 3"/>
	    </xsl:apply-templates>
	</xsl:if>

	<!--
	#====
	# Apply InvokeChain over (e')(e") or [e'][e"] using prefix p,
	# from e(e')(e") or e[e'][e"] where e resolved to prefix p.
	# For multidimensional index, first rewrite  p[e1,e2]e" => p[e1][e2]e"
	#====
	-->
	<xsl:if test="self::INVOKE and (count(*) > 1)">
	    <xsl:variable name="rest">
		<!--
		#====
		# Skip over f in f(x) or f[i]
		#====
		-->
		<INVOKE>
			<xsl:copy-of select="@*"/>
			<xsl:apply-templates select="*[position() > 1]" mode="rewriteIndex"/>
		</INVOKE>
	    </xsl:variable>
	    <xsl:apply-templates select="xalan:nodeset($rest)/*[1]" mode="invokeChain">
		<xsl:with-param name="prefix" select="xalan:nodeset($prefixSecond)/*[1]"/>
		<xsl:with-param name="isLastObjrefStep" select="$isLastObjrefStep"/>
	    </xsl:apply-templates>
	</xsl:if>
    </xsl:variable>

  <!--
  #====
  # Form product of first term iterator and rewritten second term.
  #	if rwRest is empty, just return rwFirst.
  #	if rwFirst is empty, just return rwRest.
  # Product is formed by:
  #   if rwFirst/endsInIterator
  #	return <EXPR @lastBinding=rwRest/@lastBinding> PRODUCT/rwFirst & rwRest 
  #   else return rwRest
  #====
  -->
      <xsl:variable name="rwRest" select="xalan:nodeset($rwRestRTF)/*[1]"/>

      <!--
      #====
      # If rwFirst @isSimple and thus not(@endsInIterator),
      # it is a prefix that is absorbed either into the rest of the objref,
      # or used as the function name in an invoke.
      #====
      -->
      <xsl:choose>
      <xsl:when test="$rwFirst[@endsInIterator]">
	<xsl:apply-templates select="." mode="createProduct">
		<xsl:with-param name="rwFirst" select="$rwFirst"/>
		<xsl:with-param name="rwRest" select="$rwRest"/>
		<xsl:with-param name="endsInIterator" select="$rwRest/@endsInIterator"/>
	</xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
	<xsl:copy-of select="$rwRest"/>	  <!-- copy-of drops RTF attributes -->
      </xsl:otherwise>
      </xsl:choose>

</xsl:template>

<!--
#=============================================================================
# Rewrite invoke chain p(e)e" or p[e]e", where p is the already rewritten prefix
#   1. Form prefix
#   2. Invokestep over first term p(e) or p[e]
#   3. Invokechain over remainder of e" which is sequence of () or p[]
#   4. Create product of invokestep and invokechain
# It is assumed that we have already rewritten any multidimensional index
# for the entire invoke chain p[e]e".
#=============================================================================
-->
<xsl:template match="INVOKE" mode="invokeChain">
	<xsl:param name="prefix"/>
	<xsl:param name="isLastObjrefStep" select="true()"/>
		<!-- if just INVOKE outside of OBJREF, then isLastObjrefStep -->

	<!--
	#====
	# InvokeStep over (e')(e")... [e'][e"]... using prefix p:
	#	(1) Do p(e') or p[e'] using invokeOrIndexStep
	#	(2) Form prefix from (o in rw(p(e')))
	#	(3) Follow below to do InvokeStep over <o>(e")
	# Will pick up o from saving result of p(e') or p[e'] in rwFirst.
	#====
	-->
	    <!-- Form OBJREF from prefix if has dot or implicit dot in DEREF -->
	    <xsl:variable name="objrefFromPrefix">
		<xsl:choose>
		<xsl:when test="$prefix[DELIMITER[@ID='.'] or DEREF]">
			<xsl:copy-of select="$prefix"/>		<!-- @deref -->
		</xsl:when>
		<xsl:otherwise>
			<xsl:copy-of select="$prefix/*"/>
		</xsl:otherwise>
		</xsl:choose>
	    </xsl:variable>

	<!-- Skip iterator creation for last invoke step in primary -->
	<xsl:variable name="rwFirstRTF">
	    <xsl:apply-templates select="." mode="invokeOrIndexStep">
		<xsl:with-param name="prefix" select="xalan:nodeset($objrefFromPrefix)/*[1]"/>
		<xsl:with-param name="isLastInvokeStep" select="(count(*) = 1) and boolean($isLastObjrefStep)"/>
	    </xsl:apply-templates>
	</xsl:variable>

  <!-- bindingFirstTerm = rwFirst[@lastBinding] -->
	<xsl:variable name="rwFirst" select="xalan:nodeset($rwFirstRTF)/*[1]"/>
	<xsl:variable name="bindingFirstTerm" select="$rwFirst/@lastBinding"/>

  <!--
  #====
  # Create prefix.
  # Form prefix from the first term's lastBinding if it endsInIterator,
  #	or from the previous prefix and the rewritten first term.
  #====
  -->
    <xsl:variable name="prefixSecond">
	<xsl:apply-templates select="$rwFirst" mode="createPrefix">
		<xsl:with-param name="prefix" select="$prefix"/>
	</xsl:apply-templates>
    </xsl:variable>

  <!--
  #====
  # Rewrite rest of expression e2 using prefix.
  #
  # INVOKE: f(x)[y] as {Expr Tuple Subscript}   where count(*)==3
  #         if invokeStep, is (x)[y] as {Tuple Subscript},
  #         	with prefix holding function or index name
  #	    f(x)[y] are done by re-invoking this template with invokeStep
  #		over (x)[y] and prefix=f, so will create iterator over f(x)
  #		and then form its product with rest which is 2ndPrefix[y]
  #====
  # rest = INVOKE/*[position() > 1]
  # rwRest = rw(rest,prefixSecond) param=isInvokeStep
  #		i.e., rewrite(e' with <o> prefix)
  #====
  -->
    <xsl:variable name="rwRestRTF">
	<!--
	#====
	# Apply InvokeStep over (e')(e") or [e'][e"] using prefix p,
	# from e(e')(e") or e[e'][e"] where e resolved to prefix p.
	#====
	-->
	<xsl:if test="self::INVOKE and (count(*) > 1)">
	    <xsl:variable name="rest">
		<!--
		#====
		# Skip over f in f(x) or f[i],
		# or if invokestep skip over (x) in (x)[i]
		#====
		-->
		<INVOKE>
			<xsl:copy-of select="@*"/>
			<xsl:copy-of select="*[position() > 1]"/>
		</INVOKE>
	    </xsl:variable>
	    <xsl:apply-templates select="xalan:nodeset($rest)/*[1]" mode="invokeChain">
		<xsl:with-param name="prefix" select="xalan:nodeset($prefixSecond)/*[1]"/>
		<xsl:with-param name="isLastObjrefStep" select="$isLastObjrefStep"/>
	    </xsl:apply-templates>
	</xsl:if>
    </xsl:variable>

  <!--
  #====
  # Form product of first term iterator and rewritten second term.
  #	if rwRest is empty, just return rwFirst.
  #	if rwFirst is empty, just return rwRest.
  #====
  -->
      <xsl:variable name="rwRest" select="xalan:nodeset($rwRestRTF)/*[1]"/>

      <xsl:apply-templates select="." mode="createProduct">
	<xsl:with-param name="rwFirst" select="$rwFirst"/>
	<xsl:with-param name="rwRest" select="$rwRest"/>
	<xsl:with-param name="endsInIterator" select="$rwRest/@endsInIterator"/>
      </xsl:apply-templates>

</xsl:template>

<!--
#=============================================================================
# Create prefix.
# Form prefix from the first term's lastBinding if it endsInIterator,
#	or from the previous prefix and the rewritten first term.
#	In the latter case,
#	if the rewritten first term absorbed the prefix (is an OBJREF)
#	just use it, otherwise use p.rw(e) == p.e since rw(e,p)==e and p=empty.
#
#	if (rwfirst[@endsInIterator]) prefixSecond = o.deref() else rwFirst
#		where o = bindingFirstTerm
#		where rwFirst is equivalent to p.e
#
# If bindingFirstTerm is from rwFirst[ATOM or GROUP] then use the latter,
#	so preserve attributes if is from identifier and not temporary.
# If invokeStep, prefix is function name and so is ignored in later chains.
#
# INVARIANT: if isInvokeStep then rwFirst[@endsInIterator]
#	since for p(x)[y] will have invocation iterator (o in p(x)) & rest(o).
# INVARIANT: if endsInIterator then lastbinding (i.e., bindingFirstTerm)
#		and lastbinding will be to a temporary.
# INVARIANT: lastBinding iff (endsInIterator or (isSimple and not group))
#	     lastBinding is always an identifier, not an object reference.
# INVARIANT: endsInIterator iff isSimple or already rewritten with iterator
#====
# if (endsInIterator) { lastbinding.deref()
# } else {	// skipped create iterator, so e is simple
#	if rw(e) is OBJREF { rw(e)	// rw(e) absorbed p
#	} else {
#		if prefix:  p.	// Skipped but did not absorb p (never happens)
#		if (rw(e) is ATOM or GROUP) { rw(e)	// since rw(e) == e
#		} else { lastbinding }
#	} }
#====
# TAGS objref:  OBJREF / ATOM|GROUP (DELIMITER[@ID="."] ATOM|GROUP)* [DEREF]
#	where DEREF = ?.deref()
#=============================================================================
-->
<xsl:template match="*" mode="createPrefix">
  <xsl:param name="prefix"/>
  <xsl:param name="rwFirst" select="."/>

  <xsl:variable name="bindingFirstTerm" select="$rwFirst/@lastBinding"/>

	<xsl:choose>
	<xsl:when test="$rwFirst[@endsInIterator]">

	  <OBJREF isPrimaryField="true()" isSimple="true()" deref="true()">
	    <ATOM isPrimaryField="true()" isSimple="true()" deref="true()">
		<xsl:if test="$rwFirst[@cast] and not(boolean($prefix))">
			<xsl:attribute name="cast">
				<xsl:value-of select="$rwFirst/@cast"/>
			</xsl:attribute>
		</xsl:if>
		<IDENTIFIER>
		    <xsl:value-of select="$bindingFirstTerm"/>
		</IDENTIFIER>
	    </ATOM>
	    <!-- add .deref() if endsInIterator (true if isInvokeStep) -->
	    <DEREF isderef="true">
	      <DELIMITER ID="." isderef="true()"/>
	      <ATOM isPrimaryField="true()" isderef="true()">	<!-- @deref -->
		<xsl:text>deref()</xsl:text>
	      </ATOM>
	    </DEREF>
	  </OBJREF>

	</xsl:when>
	<xsl:otherwise>

	  <!-- if not invokeStep and rwFirst is OBJREF, just use it -->
	  <xsl:choose>
	  <xsl:when test="$rwFirst[self::OBJREF]">
		<xsl:copy-of select="$rwFirst"/>
	  </xsl:when>
	  <xsl:otherwise>

	  <OBJREF isPrimaryField="true()" isSimple="true()">
	    <xsl:if test="boolean($prefix)">
		<xsl:copy-of select="$prefix/@deref"/>	  <!-- @deref -->
		<!-- If have prefix, do not have cast in OBJREF
		<xsl:copy-of select="$prefix/@cast"/>
		-->
		<xsl:copy-of select="$prefix/*"/>
		<DELIMITER ID="."/>
	    </xsl:if>

	    <!--
	    #====
	    # If rwFirst is atom or group, use it to preserve compiler info, ie,
	    #   simple identifier, literal, objref, methodref, or collection
	    #   that is not a primary field or argument.
	    #====
	    -->
	    <xsl:choose>
	    <xsl:when test="$rwFirst[self::ATOM[IDENTIFIER or DOTNAME or METHODREF or LITERAL or ANDKEYWORD] or self::GROUP[LIST or MAP or SET]]">
		<xsl:copy-of select="$rwFirst"/>
	    </xsl:when>
	    <xsl:otherwise>
		<ATOM isPrimaryField="true()" isSimple="true()">
		    <xsl:if test="$rwFirst[@cast] and not(boolean($prefix))">
			<xsl:attribute name="cast">
				<xsl:value-of select="$rwFirst/@cast"/>
			</xsl:attribute>
		    </xsl:if>
		    <IDENTIFIER>
			<xsl:value-of select="$bindingFirstTerm"/>
		    </IDENTIFIER>
		</ATOM>
	    </xsl:otherwise>
	    </xsl:choose>
	  </OBJREF>

	  </xsl:otherwise>
	  </xsl:choose>
	</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<!--
#=============================================================================
# RewriteIndex:
#	For multidimensional index, rewrite  p[e1,e2]e" ==> p[e1][e2]e"
#	XX: NOT: For slicing, rewrite p[e1..e2]e" ==> p[to(e1,e2)]e"
# TAGS invoke: INVOKE/TUPLE/EXPRESSION
#	INVOKE/SUBSCRIPT/EXPRESSION|DELIMITER[@ID=',']
#	INVOKE/SUBSCRIPT/SLICE/EXPRESSION
# Note that below we have already deleted the first child p from INVOKE,
#	so that the remaining children are a chain of TUPLE and SUBSCRIPT.
#=============================================================================
-->
<xsl:template match="*" mode="rewriteIndex" priority="2">
  <xsl:param name="prefix"/>	<!-- Ignored here -->

  <xsl:choose>
	<!-- Slice [e1..e2] : leave alone -->
	<xsl:when test="self::SUBSCRIPT/SLICE">
		<xsl:copy-of select="."/>
	</xsl:when>

	<!-- Multidimensional index: p[e1,e2]e" ==> p[e1][e2]e" -->
	<xsl:when test="self::SUBSCRIPT/DELIMITER[@ID=',']">
	    <xsl:apply-templates select="self::SUBSCRIPT/EXPRESSION" mode="createSubscript"/>
	</xsl:when>

	<!-- Leave alone single index or invoke -->
	<xsl:otherwise>
		<xsl:copy-of select="."/>
	</xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ==== Default template to copy nodes through -->
<xsl:template match="@*|node()" mode="rewriteIndex">
   <xsl:copy>
	<xsl:apply-templates select="@*|node()" mode="rewriteIndex"/>
   </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# CreateSubscript:  EXPR => SUBSCRIPT/EXPR
#=============================================================================
-->
<xsl:template match="*" mode="createSubscript" priority="2">
	<SUBSCRIPT>
		<DELIMITER ID="["/>
		<xsl:copy-of select="."/>
		<DELIMITER ID="]"/>
	</SUBSCRIPT>
</xsl:template>

<!--
#=============================================================================
# InvokeOrIndexStep: p(e1,e2) | p[e1,e2] | p[e1..e2] - already rewrote head p
#	p(e1,e2) => (x in rw(e1)) & (y in rw(e2)) & (o in p(x,y))
#	p[e1,e2] => similar with (o in p[x,y])
#	p[e1..e2] => similar with (o in p[x..y])
# OUTPUT: ITERATOR/(o in INVOKE/p(rw(params)) ) @lastBinding=o @endsInIterator
#         ITERATOR/(o in INDEX/p(rw(params)) ) @lastBinding=o @endsInIterator
#         ITERATOR/(o in SLICE/p(rw(params)) ) @lastBinding=o @endsInIterator
# Introduces "INDEX" tag instead of INVOKE/SUBSCRIPT.
# PROBLEM: Index origin shift from 1 to 0 for Groovy
# TAGS invoke: INVOKE/TUPLE/EXPRESSION
#	       INVOKE/SUBSCRIPT/EXPRESSION
#	       INVOKE/SUBSCRIPT/SLICE/EXPRESSION
#=============================================================================
-->
<xsl:template match="INVOKE" mode="invokeOrIndexStep" priority="2">
	<xsl:param name="prefix"/>
	<xsl:param name="isLastInvokeStep" select="false()"/>

	<!-- Build up parameter list - will be atom or iterator -->
	<!-- Only operate over first node, ignore rest -->
	<xsl:variable name="iters">
	   <ITERATORS>
		<xsl:apply-templates select="*[1][self::TUPLE]/EXPRESSION/* | *[1][self::SUBSCRIPT]/EXPRESSION/* | *[1][self::SUBSCRIPT]/SLICE/EXPRESSION/*" mode="createBoundIterator">
			<xsl:with-param name="isArgument" select="true()"/>
		</xsl:apply-templates>
	   </ITERATORS>
	</xsl:variable>

	<!-- Build up iterator product, keep only iterators not simple terms -->
	<xsl:variable name="itersOnly">
	    <xsl:if test="xalan:nodeset($iters)/ITERATORS/EXPRESSION[@endsInIterator]">
	    <ITERATORS>
		<xsl:copy-of select="xalan:nodeset($iters)/ITERATORS/EXPRESSION[@endsInIterator]"/>
	    </ITERATORS>
	    </xsl:if>
	</xsl:variable>
	<xsl:variable name="iterProduct">
		<xsl:apply-templates select="xalan:nodeset($itersOnly)/*[1]" mode="extractProduct"/>
	</xsl:variable>

	<!-- create invoke expression content: from invoke or from index -->
	<xsl:variable name="content">
		  <xsl:choose>
		  <xsl:when test="*[1][self::TUPLE]">
		    <INVOKE>
			<xsl:copy-of select="@*"/>
			<xsl:copy-of select="$prefix"/>
			<TUPLE>
			<DELIMITER ID="("/>
			<!-- Extract parameter list, are names or literals -->
			<xsl:apply-templates select="xalan:nodeset($iters)/ITERATORS/*" mode="extractParamsToList"/>
			<DELIMITER ID=")"/>
			</TUPLE>
		    </INVOKE>
		  </xsl:when>

		  <!-- SLICE (XX: NOT changed to SUBSCRIPT[to(x,y)]) -->
		  <xsl:when test="*[1][self::SUBSCRIPT/SLICE]">
		    <INDEX>
			<xsl:copy-of select="$prefix"/>
			<SLICE>
			  <DELIMITER ID="["/>
			  <xsl:apply-templates select="xalan:nodeset($iters)/ITERATORS/*" mode="extractParamsToList">
				<!--
				<xsl:with-param name="delimiter" select="'..'"/>
				-->
				<xsl:with-param name="delimiter" select="*[1][self::SUBSCRIPT]/SLICE/OPERATOR/text()"/>
			  </xsl:apply-templates>
			  <DELIMITER ID="]"/>
			</SLICE>
		    </INDEX>
		  </xsl:when>

		  <xsl:otherwise>     <!-- when test="*[1][self::SUBSCRIPT]" -->
		    <INDEX>
			<xsl:copy-of select="$prefix"/>
			<SUBSCRIPT>
			<DELIMITER ID="["/>
			<xsl:apply-templates select="xalan:nodeset($iters)/ITERATORS/*" mode="extractParamsToList"/>
			<DELIMITER ID="]"/>
			</SUBSCRIPT>
		    </INDEX>
		  </xsl:otherwise>
		  </xsl:choose>
	</xsl:variable>

	<!--
	#====
	# Create iterator over invoke or index operation, in content variable.
	# Skip iterator creation for last invoke step in primary.
	#====
	-->
	<xsl:variable name="invokeIterator">
		<xsl:apply-templates select="xalan:nodeset($content)/*[1]" mode="createBoundIterator">
		    <xsl:with-param name="prefix" select="$prefix"/>
		    <xsl:with-param name="isPrimaryField" select="true()"/>
		    <xsl:with-param name="doNotRewrite" select="true()"/>
		    <xsl:with-param name="doNotCreateIterator" select="$isLastInvokeStep"/>
		</xsl:apply-templates>
	</xsl:variable>

	<!--
	#====
	# Create product of parameter iterators with invokeIterator.
	#    if (iters) <EXPR @lastBinding=o @endsInIterator> PRODUCT/iterProduct & invokeIterator
	#    else <EXPR @lastBinding=o> invokeIterator
	#====
	-->
	<xsl:apply-templates select="xalan:nodeset($invokeIterator)/*[1]" mode="createProduct">
		<xsl:with-param name="rwFirst" select="xalan:nodeset($iterProduct)/*[1]"/>
		<!--
		<xsl:with-param name="endsInIterator" select="true()"/>
		-->
		<xsl:with-param name="endsInIterator" select="not(boolean($isLastInvokeStep))"/>
	</xsl:apply-templates>

</xsl:template>

<!-- ==== Default template to copy nodes through -->
<xsl:template match="@*|node()" mode="invokeOrIndexStep">
   <xsl:copy>
	<xsl:apply-templates select="@*|node()" mode="invokeOrIndexStep"/>
   </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# List/map/set literals - group rewrites
# List: [e1, e2]   ==> (x in rw(e1)) & (y in rw(e2)) & [x,y]
# Map: [ek:ev, ...] ==> (k in rw(ek)) & (v in rw(ev)) & (...) & {k:v, ...}
# Set: {e1, e2}   ==> (x in rw(e1)) & (y in rw(e2)) & {x,y}
#=============================================================================
-->
<xsl:template match="GROUP[LIST | MAP | SET]" priority="2">
	<xsl:param name="prefix"/>
	<xsl:apply-templates select="*[1]" mode="collection">
		<xsl:with-param name="prefix" select="$prefix"/>
	</xsl:apply-templates>
</xsl:template>

<!-- Drop past GROUP down into LIST, MAP, or SET node -->
<xsl:template match="GROUP/LIST | GROUP/MAP | GROUP/SET" mode="collection">
	<xsl:param name="prefix"/>	<!-- ignored, never occurs -->

	<!-- Build up parameter list - will be atom or iterator -->
	<xsl:variable name="iters">
	    <ITERATORS>
		<xsl:apply-templates select="self::LIST/EXPRESSION/* | self::MAP/KEYVALUE/EXPRESSION/* | self::SET/EXPRESSION/*" mode="createBoundIterator">
			<xsl:with-param name="isArgument" select="true()"/>
		</xsl:apply-templates>
	    </ITERATORS>
	</xsl:variable>

	<!-- Build up iterator product, keep only iterators not simple terms -->
	<xsl:variable name="itersOnly">
	    <xsl:if test="xalan:nodeset($iters)/ITERATORS/EXPRESSION[@endsInIterator]">
	    <ITERATORS>
		<xsl:copy-of select="xalan:nodeset($iters)/ITERATORS/EXPRESSION[@endsInIterator]"/>
	    </ITERATORS>
	    </xsl:if>
	</xsl:variable>
	<xsl:variable name="iterProduct">
		<xsl:apply-templates select="xalan:nodeset($itersOnly)/*[1]" mode="extractProduct"/>
	</xsl:variable>

	<!-- create collection [list,...] without XX: @lastBinding=o @endsInIterator -->
	<xsl:variable name="collection">
		<GROUP>
		<xsl:copy-of select="../@*"/>
		<xsl:attribute name="isSimple">true()</xsl:attribute>
		<xsl:copy>
		    <xsl:copy-of select="@*"/>
		    <DELIMITER ID="["/>
		    <xsl:choose>
		    <xsl:when test="self::MAP">
			  <!-- Extract parameter list, are names or literals -->
			  <xsl:apply-templates select="xalan:nodeset($iters)/ITERATORS/*" mode="extractParamsToKeyvalue"/>
			  <!-- Handle empty map [:], emptylist is just no EXPR -->
			  <xsl:if test="self::MAP/DELIMITER[@ID=':']">
			    <KEYVALUE>
			    <xsl:copy-of select="self::MAP/DELIMITER[@ID=':']"/>
			    </KEYVALUE>
			  </xsl:if>
		    </xsl:when>
		    <xsl:otherwise>	<!-- LIST -->
			  <!-- Extract parameter list, are names or literals -->
			  <xsl:apply-templates select="xalan:nodeset($iters)/ITERATORS/*" mode="extractParamsToList"/>
		    </xsl:otherwise>
		    </xsl:choose>
		    <DELIMITER ID="]"/>
		</xsl:copy>
		</GROUP>
	</xsl:variable>

	<!--
	#====
	# Create product of parameter iterators with collection
	# if (iters) <EXPR @lastBinding=o @endsInIterator>
	#			PRODUCT/iterProduct & collection
	# else <EXPR @lastBinding=o> collection
	#====
	-->
	<xsl:apply-templates select="xalan:nodeset($collection)/*[1]" mode="createProduct">
		<xsl:with-param name="rwFirst" select="xalan:nodeset($iterProduct)/*[1]"/>
		<!-- Does not end with iterator
		# <xsl:with-param name="endsInIterator" select="true()"/>
		-->
	</xsl:apply-templates>
</xsl:template>

<!--
#=============================================================================
# Create product from iterator list.   Also can create CONCAT.
# Optionally adds @map=operator and @modifiers attributes.
#	<EXPR @lastBinding=$lastBinding> PRODUCT/*[1] & extractProduct(*[rest])
#=============================================================================
-->
<xsl:template match="node()" mode="extractProduct" priority="2">
    <!-- matched node holds iterators -->
    <xsl:param name="lastBinding"/>	<!-- create lastBinding attribute -->
    <xsl:param name="isConcat" select="false()"/>
    <xsl:param name="map"/>
    <xsl:param name="modifiers"/>

    <xsl:choose>
    <xsl:when test="count(*) > 1">
	<!-- Form iterator product -->
	<xsl:variable name="rest">
	    <ITERATORS>
		<xsl:copy-of select="*[position() > 1]"/>
	    </ITERATORS>
	</xsl:variable>
	<EXPRESSION>
	    <xsl:if test="boolean($lastBinding)">	<!-- COULD remove if -->
		<xsl:attribute name="lastBinding">
			<xsl:value-of select="$lastBinding"/>
		</xsl:attribute>
	    </xsl:if>
	    <xsl:choose>
	    <xsl:when test="boolean($isConcat)">
	      <CONCAT>
		<xsl:if test="boolean($map)">
		    <xsl:attribute name="map">
			<xsl:value-of select="$map"/>
		    </xsl:attribute>
		</xsl:if>
		<xsl:if test="boolean($modifiers)">
		    <xsl:attribute name="modifiers">
			<xsl:value-of select="$modifiers"/>
		    </xsl:attribute>
		</xsl:if>

		<xsl:copy-of select="*[1]"/>
		<!-- <DELIMITER ID="|"/> -->
		<OPERATOR>
			<xsl:text>|</xsl:text>
		</OPERATOR>
		<xsl:apply-templates select="xalan:nodeset($rest)/*[1]" mode="extractProduct">
			<xsl:with-param name="isConcat" select="$isConcat"/>
		</xsl:apply-templates>
	      </CONCAT>
	    </xsl:when>
	    <xsl:otherwise>
	      <PRODUCT>
		<xsl:if test="boolean($map)">
		    <xsl:attribute name="map">
			<xsl:value-of select="$map"/>
		    </xsl:attribute>
		</xsl:if>
		<xsl:if test="boolean($modifiers)">
		    <xsl:attribute name="modifiers">
			<xsl:value-of select="$modifiers"/>
		    </xsl:attribute>
		</xsl:if>

		<xsl:copy-of select="*[1]"/>
		<!-- <DELIMITER ID="&amp;"/> -->
		<OPERATOR>
			<xsl:text>&amp;</xsl:text>
		</OPERATOR>
		<xsl:apply-templates select="xalan:nodeset($rest)/*[1]" mode="extractProduct"/>
	      </PRODUCT>
	    </xsl:otherwise>
	    </xsl:choose>
	</EXPRESSION>
    </xsl:when>
    <xsl:otherwise>
	<xsl:copy-of select="*"/>	<!-- step inside ITERATORS -->
    </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<!-- ==== Default template to copy nodes through -->
<xsl:template match="@*|node()" mode="extractProduct">
   <xsl:copy>
	<xsl:apply-templates select="@*|node()" mode="extractProduct"/>
   </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Create product from two iterators : rwFirst & match()
#	<EXPR @lastBinding=rwRest/@lastBinding> PRODUCT/rwFirst & rwRest
#    If rwRest is empty, just return rwFirst.
#    If rwFirst is empty, just return rwRest.
# rwRest is matched node, or can be specified by parameter "rwRest".
#=============================================================================
-->
<xsl:template match="node()" mode="createProduct" priority="2">
    <!-- rwRest is matched node -->
    <xsl:param name="rwFirst"/>	        <!-- can be empty -->
    <xsl:param name="rwRest" select="."/>  <!-- can be empty -->
    <xsl:param name="endsInIterator"/>	<!-- create endsInIterator attribute -->
    <xsl:param name="map"/>
    <xsl:param name="modifiers"/>

    <xsl:variable name="lastBinding" select="$rwRest/@lastBinding"/>

    <xsl:choose>
    <xsl:when test="not(boolean($rwRest))">
	<xsl:copy-of select="$rwFirst"/>
    </xsl:when>
    <xsl:when test="not(boolean($rwFirst))">
	<xsl:copy-of select="$rwRest"/>
    </xsl:when>
    <xsl:otherwise>

	<EXPRESSION>
	    <xsl:if test="boolean($lastBinding)">
		<xsl:attribute name="lastBinding">
			<xsl:value-of select="$lastBinding"/>
		</xsl:attribute>
	    </xsl:if>
	    <xsl:if test="boolean($endsInIterator)">	<!-- COULD remove if -->
		<xsl:attribute name="endsInIterator">true()</xsl:attribute>
	    </xsl:if>
	    <!-- Enclose product in parenthesis -->
	    <!--
	    <TUPLE>
	    <DELIMITER ID="("/>
	    <EXPRESSION>
	    -->

	    <PRODUCT>
		<xsl:if test="boolean($map)">
		    <xsl:attribute name="map">
			<xsl:value-of select="$map"/>
		    </xsl:attribute>
		</xsl:if>
		<xsl:if test="boolean($modifiers)">
		    <xsl:attribute name="modifiers">
			<xsl:value-of select="$modifiers"/>
		    </xsl:attribute>
		</xsl:if>

		<xsl:copy-of select="$rwFirst"/>
		<!-- <DELIMITER ID="&amp;"/> -->
		<OPERATOR>
			<xsl:text>&amp;</xsl:text>
		</OPERATOR>
		<xsl:copy-of select="$rwRest"/>
	    </PRODUCT>

	    <!--
	    </EXPRESSION>
	    <DELIMITER ID=")"/>
	    </TUPLE>
	    -->
	</EXPRESSION>
    </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<!-- ==== Default template to copy nodes through -->
<xsl:template match="@*|node()" mode="createProduct">
   <xsl:copy>
	<xsl:apply-templates select="@*|node()" mode="createProduct"/>
   </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Extract and format argument lists:
#	ATOM|OBJREF => @lastBinding
#	ITERATOR => @lastBinding.deref()
#=============================================================================
-->
<!-- extract parameters from iterator list -->

<!-- ATOM, OBJREF, GROUP collection literals are isSimple and not iterators -->

<!-- Extract iterator (x in I) into x.deref() -->
<xsl:template match="EXPRESSION[@endsInIterator]" mode="extractParams" priority="2">
	<EXPRESSION>
	<OBJREF isSimple="true()" isArgument="true()" deref="true()">
		<!-- isPrimaryField and isArgument are mutually exclusive
		# <xsl:attribute name="isPrimaryField">true()</xsl:attribute>
		-->
		<ATOM isPrimaryField="true()" deref="true()">
		  <xsl:if test="@cast">
			<xsl:attribute name="cast">
				<xsl:value-of select="@cast"/>
			</xsl:attribute>
		  </xsl:if>
		  <IDENTIFIER>
		    <xsl:value-of select="@lastBinding"/>
		  </IDENTIFIER>
		</ATOM>
		<!-- add .deref() if endsInIterator -->
		<DEREF isderef="true()">
		  <DELIMITER ID="." isderef="true"/>
		  <ATOM isPrimaryField="true()" isderef="true()"> <!-- @deref -->
		   <xsl:text>deref()</xsl:text>
		  </ATOM>
		</DEREF>
	</OBJREF>
	</EXPRESSION>
</xsl:template>

<!-- ==== Default template to copy nodes through -->
<xsl:template match="EXPRESSION" mode="extractParams" priority="1">
	<xsl:copy-of select="."/>
</xsl:template>

<!-- ==== Wrap non-iterators as expressions -->
<xsl:template match="*" mode="extractParams">
	<EXPRESSION>
	<xsl:copy-of select="."/>
	</EXPRESSION>
</xsl:template>

<!--
#=============================================================================
# Format parameters into list (x,y)
#=============================================================================
-->
<xsl:template match="node()" mode="extractParamsToList" priority="2">
  <xsl:param name="delimiter" select="','"/>
  <xsl:param name="justCopy"/>

	<!-- prepend comma if not first -->
	<xsl:if test="position() > 1">
		<DELIMITER ID="{$delimiter}"/>
	</xsl:if>
	<xsl:choose>
	  <xsl:when test="boolean($justCopy)">
	    <EXPRESSION>
	    	<xsl:copy-of select="."/>
	    </EXPRESSION>
	  </xsl:when>
	  <xsl:otherwise>
	    <xsl:apply-templates select="." mode="extractParams"/>
	  </xsl:otherwise>
	</xsl:choose>
</xsl:template>

<!-- ==== Default template to pass through -->
<xsl:template match="@*|node()" mode="extractParamsToList">
	<xsl:apply-templates select="@*|node()" mode="extractParamsToList"/>
</xsl:template>

<!--
#=============================================================================
# Format parameters into keyvalue list (x:y,...)
#=============================================================================
-->
<xsl:template match="node()" mode="extractParamsToKeyvalue" priority="2">
	<xsl:param name="delimiter" select="','"/>
	<xsl:param name="keydelimiter" select="':'"/>
	<xsl:if test="position() mod 2 = 1">	
		<!-- prepend comma if not first -->
		<xsl:if test="position() > 1">
			<DELIMITER ID="{$delimiter}"/>
		</xsl:if>

		<!-- first of pair -->
		<KEYVALUE>
		<xsl:apply-templates select="." mode="extractParams"/>

		<!-- second of pair -->
		<DELIMITER ID="{$keydelimiter}"/>
		<xsl:apply-templates select="following-sibling::*[1]" mode="extractParams"/>
		</KEYVALUE>
	</xsl:if>
</xsl:template>

<!-- ==== Default template to pass through -->
<xsl:template match="@*|node()" mode="extractParamsToKeyvalue">
	<xsl:apply-templates select="@*|node()" mode="extractParamsToKeyvalue"/>
</xsl:template>

<!--
#=============================================================================
# Constructor definitions.
#=============================================================================
-->
<xsl:variable name="prefixAndKeyword" select="'IconKeywords.'"/>

<!--
#=============================================================================
# Default templates
#=============================================================================
-->

<!-- ==== Default template to copy nodes through -->
<xsl:template match="@*|node()">
  <xsl:param name="prefix"/>
  <xsl:copy>
    	<xsl:apply-templates select="@*|node()">
		<xsl:with-param name="prefix" select="$prefix"/>
	</xsl:apply-templates>
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>
