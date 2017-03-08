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
  Copyright (c) 2013 Orielle, LLC.
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
# PURPOSE: Last step of transforming Unicon into Groovy.
#   Translates:
#	Control constructs and operators into IconIterator constructors.
#	Classes into Groovy style classes, using mixins for multiple inheritance
#	Methods into Groovy parameterized closures using def.
#	Globals and procedures into statics in classes, class G { static def G }
#	Lifts all invoke and index, as well as atoms, objrefs, and collections
#		if not isPrimaryField or isArgument or inIterator.
#	Interpretive mode: drops "def" for globals and procedures.
# PARAMETERS:   The following are parameters set by the interpreter.
#	isInterpretive: if top level expression is interpreted or compiled.
#		Top level expressions are outside of a class or procedure.
# PARAMETERS:   The following are fixed configuration switches.
#	classMethodInitialModifier: Modifier used for initial clauses in
#		class methods and closures.  Default is ''. 
#		Can be set to 'static', which is what is used for procedures.
#	shellPrefix: prefix to prepend to commands which omit parenthesis, e.g.,
#		load foo => prefix.load(foo)
#	useDispatch: if surround method calls with a dispatch call, e.g.,
#		f(x) => dispatch(f)(x).
#		where dispatch is an overridable method in IconIterator.
#		Dispatch potentially causes problems with invoking native Java.
#	canOmitMethodArgs: if can omit method and closure arguments, foo(,,1)
#		If true, method is defined using varargs foo(Object... args),
#		and each method name must be unique and cannot be overloaded.
#	declareReifiedVariables: if should create local declarations for
#		reified variables, e.g., for variable x,
#			def x_r=new IconVar(()->x, (rhs)->x=rhs)
#		and then use these reified variables instead of constructors
#		in the method body.
# ATTRIBUTES on references:
#	@isLocal (to local variable or parameter in method or closure
#		including temporaries, or to temporary at top-level expression)
#	@isToClassField (to a class field or class parameter).
#	@isToGlobal (also sets @isToClassField)
#	@isToClass (if is reference to class)
#	@isToThisClass (if is reference to a containing class)
#	@isToMethodLocal, @isToClosure, @isToParameter, @isToTmp, @type
#	@matchesMethodName
#	@declBlockDepth (block depth of declaration for local reference)
# ATTRIBUTES on STATEMENT or EXPRESSION:
#	@blockDepth
# DERIVED ATTRIBUTES: these attributes can be derived from above, if needed:
#	@isToExtern  if !(@isLocal || @isToClassField || @isToGlobal
#			|| @isToClass)
#	@alreadyReified if ((@isLocal and @isToMethodLocal) or @isToTmp)
#		variable is just reified with object.
#
#=============================================================================
# SUMMARY OF TRANSFORMS
#=============================================================================
#
# Junicon follows the Java package model and its scoping rules.
# Procedures and globals are mapped into the Java model
# by making them static variables in a class with the same name.
# Function-like class instantiation is mapped into Java static factories.
#
# Globals
# =======
# global G                ==> new IconGlobal("G")
# procedure P(x) {body}   ==> class P { static method P(x) {body} }
#					=> static def P = {x -> body}
# class C:E:M(fields) {body} ==> class C extends E {
#					C(x) { constructor }
#					static method C(x) { new C(x) } }
#					=> static def C = {x -> new C(x)}
# foo::C(x)               ==> foo.C.C(x)
# ::C(x)                  ==> C.C(x)
# C(x)                    ==> C(x) # instantiation using function instead of new
#				   # visible by import static foo.C.C or foo.*.*
#
# Packages and import
# ===================
# package x.y             ==> unchanged  # Java style
# import x.y              ==> unchanged  # Slight overlap with Unicon if just x
#
# Legacy Unicon notation
#   package "x/y"         ==> package x.y       # Legacy Unicon package notation
#   import "x/y" | link "foo" ==> import static x.y.*.*  ;  import x.y.*
#                                        # Picks up globals and classes
#
# Local and static variables
# ==========================
# static => static local
# local x;                ==> def x;
# local x=e, y=e;         ==> def x,y; x:=e; y:=e;
# local type x=e, y=e;    ==> type x,y; x:=e; y:=e;
# In class:    local x=e; => def x:=e.next();
# Interpretive: local x=e; => x:=e.next();
#		where next() is nextAtom()?.deref()
#
# Blocks and closures
# ===================
# {locals; body} => IconBlock(()->{locals; body}) if not method|procedure|class
# {locals=init; body} => {locals; {init;body}} if not class
# {e1; ...; e2} => new IconSequence(e1, ..., e2)	
#
# Local declarations in methods and classes
#   method() {locals; body} => method() {locals; {body;fail}}
#   class {locals=init} => class { def locals=init.nextAtom()?.deref() }
#
# In class, local initializers are not moved into the block body.
# In method or procedure, omit IconBlock but still move initializers into body.
# IconBlock delegates to closure invoke that returns new IconIterator.
# 	new IconBlock(()->{decl; new IconIterator(rest)})
#
# Lambda expressions, i.e., parameterized closures
#   (x,y) -> {locals; body} => {x,y -> {locals; body}}
#
# Classes, methods, and procedures 
# ================================
# class C:E:M (fields) => class C extends E {
#	@mixin M;	# @mixin for multiple inheritance
#	def classCachename = new MethodBodyCache();
#		# From attribute @classCachename, for lookup from methods
#	def fields;				# for constructor fields
#	def locals=init.nextAtom()?.deref();		# for locals=init;
#	C() { super(); initially() }
#	C(x) { super(); this.x=x; initially() }
#	static def C = { Object... args ->	# unpack args before invoke C(x)
#		if (args == null) args = new Object[];
#		new C((args.length > i)?args[i]:null, 
#			...for each constructor arg) }
#	methods() {};
#	initially() {}; }
# import static C.C	# added if interpretive and not nested class.
#
# Method definitions are transformed to parameterized closures.
# 	method f(x,y) {locals; body} =>
#	    def f = { x,y -> locals; return new IconIterator(body ; fail) }
# Methods optimize using a cache for the iterator body:
#	    def f = { x,y -> locals; return classCachename.getFree(f_uniq) ?: 
# 		 new IconIterator(body;fail).setCache(classCachename, f_uniq); }
# Method invocations were already normalized to iteration over the
#	returned generator: f(x,y) => (x' in x) & (y' in y) & (z in f(x',y'))
#
# Interpretive vs. compiled mode
# =============================
# Interpretive mode: at top level outside any class or procedure.
# 	def x...; => x...;	# if interpretive and outermost
#	import static C.C	# for defined classes, globals, and procedures
#	(foo).next()		# outermost statements or expressions
#		where next() is nextAtom()?.deref()
#
# Control constructs 
# ==================
# if e1 then e2 else e3 => IconIf(e1,e2,e3)	
# Similar translation to IconIterator subtypes for:
#	while until every repeat case next break create fail return suspend
# 
# Operators
# =========
# x op y => new IconOperation(IconOperator.binary((x,y)->x op y), true,x,y, ...)
# op x => new IconOperation(IconOperator.unary((x)->op x), x)
# (e,e') => e & e'    3(e,e') == select ith (of mutual eval, which is implicit)
# e1 & e2 => new IconProduct(e1, e2)
# 
# Assignment: normal, reversible, augmented, swap
# ==========
# x:=y => new IconOperation(new IconAssign(),x,y)
#	 		# Will reify x,y after normalization
# x<-y => new IconOperation(new IconAssign().undoable(),x,y)
# x+:=y => new IconOperation(new IconAssign().augment(op),x,y)
# x:=:y => new IconOperation(new IconAssign().swap(),x,y)
# 
# Iterators 
# =========
# (x in e) => new IconInIterator(   // Bind variable to generator
# 		new IconVar((get:()->x, set:(y)->x=y), e)
# (<> e) => new IconFirstClass (e)  // wraps iterator in singleton IconIterator
# (e to e1 by e2) => new IconToIterator(e, e1, e2)
# !g => new IconPromote (g)
# 
# Co-expressions
# ==============
# create e => new IconCoExpression( {all_referenced_locals x,y,z -> e},
#		{()->[all_referenced_locals]} )
#	Similar to IconIndexIterator.
#	Singleton next freezes locals as {()->[locals]}(), returns iterator
#	whose restart resets inneriterator to {locals -> e}(frozenLocals).
#	and whose next is: inneriterator.next()
# Thus, create e => <>{(local_refs) -> e}({()->[local_refs]})
#
# Primary expressions: atom evaluation using closures
# ===================================================
# (a) lift atoms (identifier, literal) if not isPrimaryField
#		(includes if lhs of in)
#    atom/identifier x => new IconVarIterator(()->x, (y)->x=y)
#				// Just promote to atom if @inLeftIterator.
#    atom/literal l    => new IconValueIterator(literal)
#				// So is iterator, but keep raw as possible
#    atom/andkeyword k => handled like operator or function depending on config
#    atom/packagref    => change : to .
# (b) lift objref if not isArgument and not isPrimaryField
#    objref o.x.y  => new IconFieldIterator(o,"x","y")
# (c) lift collection literal if not isArgument and isSimple
#    group [c] => new IconVarIterator(()->[c], null)
#		  // Immutable so cannot set.
# (d) lift all invoke, index
#    index c[i] =>  new IconIndexIterator(()->c, ()->i)
#    invoke f(x) => new IconInvokeIterator(()->f(x))
#
# Numbers using arbitrary precision arithmetic
# ============================================
# 10 => 10G	// bigInteger, bigDecimal is automatic
# if off: 10.0 => 10.0D	// default in Groovy is to force BigDecimal on reals
#
# Big quotes, i.e., big literals or block quotes
# ==============================================
# x := {< code literal >}	# Block quotes
# {< raw Groovy code >}	# Native code execution
#
# Global variable processing.
# ==============================================
# 1. Preprocess global: add top-level global declarations to topLevelGlobals.
# 2. Preprocess atom references: set @isToGlobal if found global in scope.
# 	If reference not found (extern), see if is in topLevelGlobals.
#	Do not need to look in /*/global.
# 3. Transform global:
#	At top level, if interactive, create global. Otherwise just skip.
#		global g ==> g_r = new IconGlobal("g")
#	(Similar to how top-level temporaries handled.)
# 4. Create global redirection field in class,
#	for globals declared in class or declared at top level in /*/global.
#	In line-by-line interactive mode (as opposed to aggregate script file),
#	will not find /*/*global.  This is OK, since the field will
#	have been defined at top-level, and will dynamically resolve to that.
#	For Java, however, which is script file, we need the redirection field.
#
#=============================================================================
# Preprocess:
# ===========
# Insert isLocal tag on atoms: scope up for isLocalVariable, isParameter.
#
# Postprocess:
# ===========
# Translate fake normalized functions back to constructs.
#                   Was normalized to    Must transform to
#                   =================    =================
#   (e to e1 by e2) => to(e,e1,e2)    => IconToIterator(e, e1, e2)
#   e e1 e2         => e(e1,e2)       => shellPrefix.e(e1,e2)
#   new C()         => C.new()        => new C()
#	
# Change number literals to use arbitrary precision arithmetic.
# Wrap single quote and big quote literals with static utility invocations.
#
# TAGS invoke: INVOKE[@isOperator | @isCommand]
#	       C.new as ATOM[@isAllocation]/DOTNAME/CLASSNAME.new
# PROBLEM: grammar syntax allows new C without following parenthesis.
#
#=============================================================================
# Done in normalization:
# ======================
# Declare undeclared locals (temporaries).
#	block//undeclared i => local i	// if not declared and $isTmpVar
# Changed into function calls: to, commands, new
# Translate groups:
#	e{e1,...,e2} => e([create e1,...,create e2])
#	(e1,...,e2) => e1 & ... & e2  // if not e(e1,...,e2)
#=============================================================================
-->

<!--
#=============================================================================
# Parameters set by the interpreter.
#   If interpretive, can have top level expression outside of class or procedure
#	If true will generate import static for class.
#   If isJava, will output Java instead of Groovy code.
#   If useLambdaExpressions, for Java output
#	will use closures and method references instead of inner classes.
#   If methodAsClosure, will use closures to directly define methods,
#	rather than exposing methods as closures or method references.
#	Not allowed for Java, as Java does not allow forward references
#	inside closures; moreover, we would like to define methods normally
#	whenever possible, so they may be used by external code.
#=============================================================================
-->
<!-- Dynamically set
<xsl:param name="isInterpretive" select="cmd:ThasProperty('Properties', 'isInterpretive')"/>
-->
<xsl:param name="isJava" select="cmd:ThasProperty('Properties', 'isToJava')"/>
<xsl:param name="useLambdaExpressions" select="cmd:ThasProperty('Properties', 'useLambdaExpressions')"/>
<xsl:param name="methodAsClosure" select="cmd:ThasProperty('Properties', 'methodAsClosure')"/>

<!-- Derived configuration parameters, used internally -->
<xsl:variable name="useInnerClasses" select="not(boolean($useLambdaExpressions))"/>
<xsl:variable name="asMethodNotClosure" select="boolean($isJava) or not(boolean($methodAsClosure))"/>
<xsl:variable name="addAnnotations" select="boolean($isJava)"/>

<!--
#=============================================================================
# Fixed configuration switches for code generation.   Do not alter.
#=============================================================================
-->
<xsl:param name="classMethodInitialModifier" select="''"/>
    <!-- Modifier used for initial clauses in class methods and closures -->
    <!-- <xsl:param name="classMethodInitialModifier" select="'static '"/> -->
<xsl:param name="shellPrefix"/>

<!--
#=============================================================================
# Root template, top level.
#=============================================================================
-->

<!-- ==== Root template -->
<xsl:template match="/" priority="2">
	<!-- <xsl:apply-templates/> -->

	<!--
	#====
	# Preprocess.   Phases 1 and 2.
	#====
	-->
	<xsl:variable name="preprocessedPhase1">
		<xsl:apply-templates select="*" mode="preprocessPhase1"/>
	</xsl:variable>

	<xsl:variable name="preprocessed">
		<xsl:apply-templates select="xalan:nodeset($preprocessedPhase1)/*[1]" mode="preprocess"/>
	</xsl:variable>

	<!--
	#====
	# Transform program.
	#====
	-->
	<xsl:variable name="transformed">
		<xsl:apply-templates select="xalan:nodeset($preprocessed)/*[1]" mode="startTransformTopLevel"/>
	</xsl:variable>

	<!--
	#====
	# Postprocess.
	#====
	-->
	<xsl:variable name="postprocessed">
	<xsl:apply-templates select="xalan:nodeset($transformed)/*[1]" mode="postprocess"/>
	</xsl:variable>
	<xsl:copy-of select="xalan:nodeset($postprocessed)/*[1]"/>

	<!-- debug
	<xsl:variable name="printTitle" select="cmd:println('Debug')"/>
	<xsl:apply-templates select="xalan:nodeset($postprocessed)/*[1]" mode="printAll"/>
	-->

</xsl:template>

<!--
#=============================================================================
# Preprocess : Phase 1.  Handle declarations.
#   Rename static declarations inside a method or closure to use a unique name
#	within the class.
#   Find top-level global variables.
#   Find defined class names.
#   Add @originalName to declarations.
#=============================================================================
-->

<!--
#====
# Record top-level class names.
#====
-->
<xsl:template match="STATEMENT[KEYWORD[string()='class']]" mode="preprocessPhase1" priority="2">
    <xsl:variable name="classname">
	<xsl:value-of select="DECLARATION[IDENTIFIER]"/>
    </xsl:variable>

    <xsl:variable name="isTopLevelClass" select="cmd:TaddClass($classname)"/>

    <xsl:copy>
	<xsl:copy-of select="@*"/>
	<xsl:apply-templates select="node()" mode="preprocessPhase1"/>
    </xsl:copy>
</xsl:template>

<!--
#====
# Rename static declarations inside a method, procedure, initially, or closure
#		to use a unique name within the class.
#	Method statics are tagged with an "@isMethodStatic" attribute.
# Statics at the top-level outside of a class, or static class fields,
#	will not have "@isMethodStatic".
# For all local or static declarations,
#	"@originalName" holds the original name before any uniqueification.
# Note that one must allow for the possibility of multiple statics of the 
#	same name within nested blocks inside a method.
# TAGS declaration: STATEMENT[KEYWORD[static]]/(ENUM/DECL | ENUM/ASSIGN/DECL)
#====
-->
<xsl:template match="DECLARATION[@isLocalVariable][IDENTIFIER]" mode="preprocessPhase1" priority="2">
    <xsl:copy>
	<xsl:copy-of select="@*"/>

	<xsl:variable name="varname">
		<xsl:value-of select="IDENTIFIER"/>
	</xsl:variable>

	<!--
	#====
	# Save original name before renaming.
	#====
	-->
	<xsl:attribute name="originalName">
		<xsl:value-of select="$varname"/>
	</xsl:attribute>

	<!--
	#====
	# Rename static declaration in method, initially, procedure, or closure
	#	but not in class field.
	#====
	-->
	<xsl:choose>
	  <xsl:when test="(parent::ENUM[parent::STATEMENT[KEYWORD[string()='static']]] or parent::ASSIGN[parent::ENUM[parent::STATEMENT[KEYWORD[string()='static']]]]) and (ancestor::CLOSURE or ancestor::STATEMENT[KEYWORD[string()='method' or string()='procedure' or string()='initially']])">

	  <xsl:attribute name="isMethodStatic">true()</xsl:attribute>

	  <IDENTIFIER>
		<xsl:copy-of select="IDENTIFIER/@*"/>	<!-- preserve attr -->
		<!-- deriveReifiedVarName -->
		<xsl:value-of select="cmd:TgetMinimalUnique($varname, '_s')"/>
	  </IDENTIFIER>
	</xsl:when>

	<xsl:otherwise>
		<xsl:copy-of select="*"/>
	</xsl:otherwise>
	</xsl:choose>
    </xsl:copy>
</xsl:template>

<!--
#====
# Add originalName attribute to parameter declarations, so that
#	all declarations have originalName.
# Record top-level global variables.
# TAGS parameter: TUPLE/< QUALIFIED/< DECLARATION/IDENTIFIER
#			[:Type] [: or = TYPE<LITERAL>] > [SUBSCRIPT] >
#====
-->
<xsl:template match="DECLARATION[@isParameter or @isGlobalVariable][IDENTIFIER]" mode="preprocessPhase1" priority="2">

    <xsl:copy>
	<xsl:copy-of select="@*"/>

	<xsl:variable name="varname">
		<xsl:value-of select="IDENTIFIER"/>
	</xsl:variable>

	<!--
	#====
	# Record top-level global variables.
	#====
	-->
	<xsl:if test="@isGlobalVariable and not(ancestor::STATEMENT[KEYWORD[string()='class'] or KEYWORD[string()='procedure']])">
	    <xsl:variable name="isTopLevelGlobal" select="cmd:TaddGlobal($varname)"/>
	</xsl:if>

	<!--
	#====
	# Save original name.
	#====
	-->
	<xsl:attribute name="originalName">
		<xsl:value-of select="$varname"/>
	</xsl:attribute>

	<!-- <xsl:copy-of select="*"/> -->
	<xsl:apply-templates select="*" mode="preprocessPhase1"/>
    </xsl:copy>
</xsl:template>

<!--
#====
# Detect varargs parameters, set @isVarargs @isLastParameter, and strip "...".
# TAGS parameter: TUPLE/< QUALIFIED/< DECLARATION/IDENTIFIER
#			[:Type] [: or = TYPE<LITERAL>] > [SUBSCRIPT] >
#====
-->
<xsl:template match="QUALIFIED[DECLARATION[@isParameter]]" mode="preprocessPhase1" priority="2">
    <xsl:copy>
	<xsl:copy-of select="@*"/>

	<xsl:if test="following-sibling::*[1][self::SUBSCRIPT] or TYPE[DELIMITER[@ID='...']]">
		<xsl:attribute name="isVarargs">true()</xsl:attribute>
	</xsl:if>

	<xsl:if test="not(following-sibling::*[1][QUALIFIED])">
		<xsl:attribute name="isLastParameter">true()</xsl:attribute>
	</xsl:if>

	<xsl:apply-templates select="node()" mode="preprocessPhase1"/>
    </xsl:copy>
</xsl:template>

<!--
#====
# Strip varargs DELIMITER[@ID='...'] from TYPE.
#====
-->
<xsl:template match="TYPE[DELIMITER[@ID='...']]" mode="preprocessPhase1">
	<xsl:copy>
	    	<xsl:apply-templates select="@*" mode="preprocessPhase1"/>
	    	<xsl:apply-templates select="node()[not(self::DELIMITER[@ID='...'])]" mode="preprocessPhase1"/>
	</xsl:copy>
</xsl:template>

<!-- ==== Default template to copy nodes through -->
<xsl:template match="@*|node()" mode="preprocessPhase1">
	<xsl:copy>
	    	<xsl:apply-templates select="@*|node()" mode="preprocessPhase1"/>
	</xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Preprocess : Phase 2.  Handle references to identifiers.
#   Find if an identifier reference is to a local variable or parameter
#	in a method or closure (@isLocal),
#	or is to a class field or class parameter (@isToClassField),
#	or is to a global (@isToGlobal),
#	or is to a class (@isToClass, @isToThisClass).
#	If none of these, then is to an external reference (@isToExtern).
#   Insert @isLocal or @isToClassField tag on atom references:
#		scope up for isLocalVariable, isParameter.
#	If not @isLocal or @isToClassField, then is to external reference.
#	Watch out for (x,y)->locals;body, not just f(x,y){locals;body}
#   Also sets:  @isToTmp, @isToMethodStatic, @isToMethodLocal,
#		@isToClosure, @isToParameter, @type (if type set)
#   Change atom references to method statics to use the unique name.
#	Normalization renamed static declarations inside a method or closure
#	to use a unique name within the class.
#=============================================================================
-->

<!--
#====
# Find if an identifier reference is to a local variable or parameter
#	in a method or closure (@isLocal),
#	or is to a class field or class parameter (@isToClassField),
#	or is to a global (@isToGlobal, also sets @isToClassField).
#	Globals may occur as a class field, or at the top-level outside a class.
#	@isLocal is also set for top level expressions outside any class or
#	procedure if reference is to a temporary and thus local.
#	Local *and* static references in methods are dereferenced on return,
#	and are so indicated by an IconVar.local().
#	However, temporaries can never be returned, so .local() is superfluous.
# An identifier reference can also be the lead name in an object reference. 
# 1. Look up for ancestor blocks but not beyond procedure or method boundary,
#	i.e., excluding class blocks assuming no nested classes.  BLOCK will be:
#	GROUP/BLOCK, CLOSURE/BLOCK, STATEMENT/BLOCK (class, method, procedure).
# 2. Look down from the ancestor blocks for local declarations
#	(locals are inside the block),
#	or look down one level from parent of the block for matching parameters.
#	Parameter declarations will be TUPLE/QUALIFIED/DECLARATION[@isParameter]
#	Local declarations will be BLOCK/STATEMENT[local | static]/
#		ENUM[DECLARATION][@isLocalVariable]
#	    or  ASSIGN[DECLARATION[@isLocalVariable]
# 3. Look up for matching global declarations, if not resolved.
#
# We assume that all local declarations are at the front of a block.
# Can weaken this restriction if make each occurance of locals a block,
# eg. {locals1; body1; locals2; body2; } => { locals1; body1; {locals2; body2;}}
#
# FORMAT parameter: param[:type][: or =literal] [] (if varags last parameter)
# TAGS parameter: TUPLE/< QUALIFIED/< DECLARATION/IDENTIFIER
#			[:Type] [: or = TYPE<LITERAL>] > [SUBSCRIPT] >
#	TAGS type: TYPE/< DOTNAME/IDENTIFIER | IDENTIFIER >
#	TAGS typeLiteral: TYPE/LITERAL
# TAGS local:     STATEMENT[KEYWORD/local]/ENUM/DECLARATION/IDENTIFIER
#				| ENUM/ASSIGN/DECLARATION/IDENTIFIER
#====
-->
<xsl:template match="ATOM[IDENTIFIER][not(parent::OBJECT and (position() &gt; 1))]" mode="preprocess" priority="2">
    <xsl:variable name="varname">
	<xsl:value-of select="IDENTIFIER/text()"/>
    </xsl:variable>

    <xsl:copy>
        <xsl:copy-of select="@*"/>

	<!--
	#====
	# Find if the reference matches a method name.
	# This does not mean the reference scopes up to that method,
	# the reference can be to a local variable or parameter,
	# as well as to a matching class field.
	#====
	-->
	<xsl:variable name="matchesMethodName" select="ancestor::STATEMENT[KEYWORD[string()='method' or string()='initially' or string()='procedure'] and DECLARATION[IDENTIFIER[(string()=$varname) and not(string()='main')]]]"/>

	<xsl:if test="boolean($matchesMethodName)">
		<xsl:attribute name="matchesMethodName">true()</xsl:attribute>
	</xsl:if>

	<!--
	#====
	# Find if is reference to a local variable or parameter in
	#	a method or closure.
	# Take first and closest block holding a matching local or parameter.
	#	For locals, this will be BLOCK holding local declaration.
	#	For parameters, this will be BLOCK after TUPLE in parent method.
	# Then see if reference was to a static in a method, procedure,
	#	or initially, and get the identifier for @staticDeclId attribute
	# If reference is to a top level variable outside class scope,
	#	will not find any declaration, and will treat it as an extern.
	# Note that statics are allowed in closures, ie. lambda expressions.
	#====
	-->
	<xsl:variable name="blockWithDeclaration" select="ancestor::BLOCK[parent::*[not(self::STATEMENT[KEYWORD[string()='class']])]][parent::*[TUPLE[1]/QUALIFIED/DECLARATION[@isParameter]/IDENTIFIER[string()=$varname]] or STATEMENT[KEYWORD[string()='local' or string()='static']]/ENUM[DECLARATION[@isLocalVariable][@originalName = $varname] or ASSIGN/DECLARATION[@isLocalVariable][@originalName = $varname]]][1]"/>

	<xsl:variable name="declaration" select="(($blockWithDeclaration/parent::*/TUPLE[1]/QUALIFIED/DECLARATION[@isParameter][IDENTIFIER[string()=$varname]]) | ($blockWithDeclaration/STATEMENT[KEYWORD[string()='local' or string()='static']]/ENUM/DECLARATION[@isLocalVariable][@originalName = $varname]) | ($blockWithDeclaration/STATEMENT[KEYWORD[string()='local' or string()='static']]/ASSIGN/DECLARATION[@isLocalVariable][@originalName = $varname]))[1]"/>

	<xsl:variable name="hasTopLevelGlobal" select="cmd:ThasGlobal($varname)"/>
	<xsl:variable name="hasTopLevelClass" select="cmd:ThasClass($varname)"/>

	<xsl:choose>
	<xsl:when test="boolean($blockWithDeclaration)">

	    <!--
	    #====
	    # See if reference was to method static declaration in block.
	    # If so, must turn off @isLocal flag, and turn on @isToClassField.
	    # Also set: @isToMethodStatic
	    #===
	    -->
	    <xsl:variable name="uniqueMethodStaticName">
		<xsl:value-of select="$blockWithDeclaration/STATEMENT[KEYWORD[string()='static']]/ENUM/DECLARATION[@isLocalVariable and @isMethodStatic and (@originalName = $varname)]/IDENTIFIER | $blockWithDeclaration/STATEMENT[KEYWORD[string()='static']]/ENUM/ASSIGN/DECLARATION[@isLocalVariable and @isMethodStatic and (@originalName = $varname)]/IDENTIFIER"/>
	    </xsl:variable>

	    <!-- Locals *and* statics in methods are dereferenced on return -->
	    <xsl:if test="not(boolean($uniqueMethodStaticName))">
		<xsl:attribute name="isLocal">true()</xsl:attribute>

		<!-- Record number of blocks's at or above declaration -->
		<xsl:variable name="blockDepth" select="count($blockWithDeclaration/ancestor-or-self::BLOCK)"/>
		<xsl:attribute name="declBlockDepth">
			<xsl:value-of select="$blockDepth"/>
		</xsl:attribute>
	    </xsl:if>

	    <!-- Set: @isToMethodLocal -->
	    <xsl:if test="$blockWithDeclaration[parent::STATEMENT[KEYWORD[(string()='method') or (string='initially')]]]">
		<xsl:attribute name="isToMethodLocal">true()</xsl:attribute>
	    </xsl:if>

	    <!-- Set: @isToClosure -->
	    <xsl:if test="$blockWithDeclaration[parent::CLOSURE]">
		<xsl:attribute name="isToClosure">true()</xsl:attribute>
	    </xsl:if>

	    <!-- Set: @isToParameter -->
	    <xsl:if test="$blockWithDeclaration[parent::*[TUPLE[1]/QUALIFIED/DECLARATION[@isParameter]/IDENTIFIER[string()=$varname]]]">
		<xsl:attribute name="isToParameter">true()</xsl:attribute>
	    </xsl:if>

	    <!-- Set: @isToTmp -->
	    <xsl:if test="$blockWithDeclaration[STATEMENT[KEYWORD[string()='local']]/ENUM[DECLARATION[@isLocalVariable and @isTmpVar][@originalName = $varname]]]">
		<xsl:attribute name="isToTmp">true()</xsl:attribute>
	    </xsl:if>

	    <!-- Set: @type -->
	    <xsl:if test="$declaration/TYPE">
		<xsl:attribute name="type">$declaration/TYPE</xsl:attribute>
	    </xsl:if>

	    <!-- Rename if reference was to method static declaration -->
	    <xsl:choose>
		<xsl:when test="boolean($uniqueMethodStaticName)">
		  <xsl:attribute name="isToClassField">true()</xsl:attribute>
		  <xsl:attribute name="isToMethodStatic">true()</xsl:attribute>
		  <IDENTIFIER>
			<xsl:copy-of select="IDENTIFIER/@*"/>
			<xsl:value-of select="$uniqueMethodStaticName"/>
		  </IDENTIFIER>
		</xsl:when>
		<xsl:otherwise>
			<xsl:copy-of select="*"/>
		</xsl:otherwise>
	    </xsl:choose>
	</xsl:when>

	<!--
	#====
	# Find if reference is to temporary, for a top-level expression
	#	that is outside of a class or procedure.
	# Top-level temporary will be just below the root. Alternatively, can
	# see if any ancestor PRODUCT has an EXPRESSION[ITERATOR] child with
	# matching @tmpVarName, then reference is to a temporary.
	# If temporary is not top-level, will pick up in above @isLocal scoping.
	# Used in conjunction with declareReifiedTemporaries.
	#====
	-->
	<xsl:when test="not(ancestor::STATEMENT[KEYWORD[string()='class'] or KEYWORD[string()='procedure']]) and ancestor::*[descendant-or-self::STATEMENT[KEYWORD[string()='local']]/ENUM[DECLARATION[@isLocalVariable and @isTmpVar][@originalName = $varname]]]">
	    <xsl:attribute name="isLocal">true()</xsl:attribute>
	    <xsl:attribute name="isToTmp">true()</xsl:attribute>
	    <xsl:copy-of select="*"/>
	</xsl:when>

	<!--
	#====
	# Find if is reference to a class field or class parameter.
	# isLocal scope takes precedence over isToClassField, since
	# methods, blocks and closures are seen first within the class.
	#====
	-->
	<xsl:when test="ancestor::STATEMENT[KEYWORD[string()='class']][TUPLE[1]/QUALIFIED/DECLARATION[@isParameter]/IDENTIFIER[string()=$varname] | BLOCK/STATEMENT[KEYWORD[string()='local' or string()='static' or string()='global']]/ENUM[DECLARATION[@isLocalVariable or @isGlobalVariable]/IDENTIFIER[string()=$varname] or ASSIGN/DECLARATION[@isLocalVariable]/IDENTIFIER[string()=$varname]]]">
	    <xsl:attribute name="isToClassField">true()</xsl:attribute>

	    <!--
	    #====
	    # Find if is reference to a global inside a class.
	    # Sets @isToGlobal.  Also sets @isToClassField.
	    #====
	    -->
	    <xsl:if test="ancestor::STATEMENT[KEYWORD[string()='class']][BLOCK/STATEMENT[KEYWORD[string()='global']]/ENUM[DECLARATION[@isGlobalVariable]/IDENTIFIER[string()=$varname]]]">
		<xsl:attribute name="isToGlobal">true()</xsl:attribute>
	    </xsl:if>

	    <xsl:copy-of select="*"/>
	</xsl:when>

	<!--
	#====
	# Find if is reference to a top-level global outside a class.
	# Sets @isToGlobal.
	# Also sets @isToClassField, since will create global field in class.
	#====
	-->
	<xsl:when test="boolean($hasTopLevelGlobal)">
		<xsl:attribute name="isToGlobal">true()</xsl:attribute>
		<xsl:attribute name="isToClassField">true()</xsl:attribute>
		<xsl:copy-of select="*"/>
	</xsl:when>
	<!--
	# <xsl:when test="/*/STATEMENT[KEYWORD[string()='global']]/ENUM[DECLARATION[@isGlobalVariable]/IDENTIFIER[string()=$varname]]">
	-->

	<!--
	#====
	# Find if is reference to a top-level class.
	# Sets @isToClass, @isToThisClass.
	#====
	-->
	<xsl:when test="boolean($hasTopLevelClass)">
	    <xsl:attribute name="isToClass">true()</xsl:attribute>
	    <xsl:if test="ancestor::STATEMENT[KEYWORD[string()='class']][DECLARATION[IDENTIFIER[string()=$varname]]]">
		<xsl:attribute name="isToThisClass">true()</xsl:attribute>
	    </xsl:if>
	    <xsl:copy-of select="*"/>
	</xsl:when>

	<xsl:otherwise>
		<xsl:attribute name="isToExtern">true()</xsl:attribute>
		<xsl:copy-of select="*"/>
	</xsl:otherwise>
	</xsl:choose>
    </xsl:copy>
</xsl:template>

<!--
#====
# Add @blockDepth attribute to statements or expressions.
# Used to shadow referenced locals in co-expression "create",
# in conjunction with @declBlockDepth of ATOM local references.
# Condition for shadowing is: reference declBlockDepth <= create blockDepth
# EXAMPLE:  { local x;  create x }	// Must shadow local x in create.
#====
-->
<xsl:template match="STATEMENT | EXPRESSION" mode="preprocess" priority="2">
	<xsl:copy>
	<xsl:copy-of select="@*"/>
	    <xsl:variable name="blockDepth" select="count(ancestor::BLOCK)"/> 
	    <xsl:attribute name="blockDepth">
		<xsl:value-of select="$blockDepth"/>
	    </xsl:attribute>
	    <xsl:apply-templates select="node()" mode="preprocess"/>
	</xsl:copy>
</xsl:template>

<!--
#====
# <xsl:template match="STATEMENT[@isControl][KEYWORD[string()='create']]" mode="preprocess" priority="2">
#====
-->

<!--
#====
# if canOmitMethodArgs, ATOM[@emptyInner] null => OMIT
# TAGS invoke: INVOKE/<OBJREF|ATOM, TUPLE/EXPRESSION/ATOM>
#              INDEX   OBJREF|ATOM SUBSCRIPT/EXPRESSION
#====
-->
<xsl:template match="ATOM[@emptyInner or @emptyTrailing][parent::EXPRESSION[parent::TUPLE[parent::INVOKE]]]" mode="preprocess" priority="2">
		<xsl:copy>
		<xsl:copy-of select="@*"/>
			<LITERAL isWord="true()">IconTypes.OMIT</LITERAL>
		</xsl:copy>
</xsl:template>

<!--
#====
# Default template to copy nodes through
#====
-->
<xsl:template match="@*|node()" mode="preprocess">
	<xsl:copy>
	    	<xsl:apply-templates select="@*|node()" mode="preprocess"/>
	</xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Postprocess.
#=============================================================================
-->

<!--
#====
# Translate fake normalized functions back to constructs: to, !, new, command.
#                   Was normalized to    Must transform to
#                   =================    =================
#   (e to e1 by e2) => to(e,e1,e2)    => new IconToIterator(e, e1, e2)
#   e e1 e2         => e(e1,e2)       => shellPrefix.e(e1,e2)
#   new C()         => C.new()        => new C()
#
# Change number literals to use arbitrary precision arithmetic.
# Wrap single quote and big quote literals with static utility invocations.
#====
-->
<!--
#====
# Change C.new to new C
#====
-->
<xsl:template match="ATOM[@isAllocation]" mode="postprocess" priority="2">
    <xsl:copy>
    <xsl:apply-templates select="@*" mode="postprocess"/>
	<xsl:text>new </xsl:text>
	<xsl:apply-templates select="DOTNAME/CLASSNAME" mode="postprocess"/>
    </xsl:copy>
</xsl:template>

<!--
#====
# Single quotes: 'abc' => new IconSet('abc')
# TAGS single quote: ATOM[LITERAL @isSingleQuote]
#====
-->
<xsl:template match="ATOM[LITERAL[@isSingleQuote]]" mode="postprocess" priority="2">
  <xsl:copy>
  <xsl:apply-templates select="@*" mode="postprocess"/>
	<xsl:text>(</xsl:text>
	<xsl:value-of select="$newIconSet"/>
	<xsl:text>(</xsl:text>
		<xsl:copy-of select="LITERAL"/>
	<xsl:text>))</xsl:text>
  </xsl:copy>
</xsl:template>

<!--
#====
# Big quotes, i.e., big literals or block quotes: => "\escaped"
#====
-->
<xsl:template match="ATOM[LITERAL[@isBigLiteral]]" mode="postprocess" priority="2">
  <xsl:copy>
  <xsl:apply-templates select="@*" mode="postprocess"/>
	<xsl:text>&quot;</xsl:text>
	<xsl:value-of select="cmd:escapeLiteralString(LITERAL/text())"/>
	<xsl:text>&quot;</xsl:text>
  </xsl:copy>
</xsl:template>

<!--
#====
# Change numeric literals to use arbitrary precision arithmetic.
# If @noPrecision, the literal is already inside a new IconValueIterator()
# or IconValue.create() which will do the work of forcing precision.
# Otherwise, the number is inside a primary, and must use
# IconNumber.create() to control precision.
# If isRadix, must surround literal with quotes.
#
# In both cases, however,
# we must prevent Groovy from forcing arbitrary precision on decimals,
# so as to let IconNumber and IconValue.create() handle precision.
# If isReal and not($isJava),
#   Appends D after the number to turn it into a Double,
#	since the default in Groovy is to force BigDecimal on real numbers.
#   Also appends 0 to n., so Groovy won't complain.
#====
-->
<xsl:template match="ATOM[LITERAL[(@isInteger or @isReal or @isRadix)]]" mode="postprocess" priority="2">
  <xsl:variable name="literal" select="LITERAL"/>

  <xsl:copy>
  <xsl:apply-templates select="@*" mode="postprocess"/>
    <LITERAL>
	<!-- preserve attributes -->
	<xsl:apply-templates select="LITERAL/@*" mode="postprocess"/>

	<xsl:if test="LITERAL[not(@noPrecision)]">
		<xsl:value-of select="$newIconNumberCreate"/>
		<xsl:text>(</xsl:text>
	</xsl:if>

	<xsl:if test="LITERAL[@isRadix]">
		<xsl:text>"</xsl:text>
	</xsl:if>

	<xsl:copy-of select="LITERAL/node()"/>

	<!-- 3. => 3.0 since Groovy croaks on this -->
	<!-- 10.0D to turn off Groovy arbitrary precision -->
	<xsl:if test="LITERAL[@isReal] and not(boolean($isJava))">
	    <xsl:if test="substring($literal,string-length($literal)) = '.'">
		<xsl:text>0</xsl:text>
	    </xsl:if>
	    <xsl:text>D</xsl:text>
	</xsl:if>

	<xsl:if test="LITERAL[@isRadix]">
		<xsl:text>"</xsl:text>
	</xsl:if>

	<xsl:if test="LITERAL[not(@noPrecision)]">
		<xsl:if test="LITERAL[@isRadix]">
			<xsl:text>, -1</xsl:text>
		</xsl:if>
		<xsl:text>)</xsl:text>
	</xsl:if>

    </LITERAL>
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
# Default template: Drop any lift attributes so will not print
# To see ! in verbose mode for normalization, run with -J option.
#====
-->
<xsl:template match="@lift" priority="2" mode="postprocess"/>

<!--
#=============================================================================
# Transform.
#=============================================================================
-->

<!--
#=============================================================================
# Top level transform.
#   A top level statement or expression is outside of any class or procedure.
# TAGS program: PROGRAM/ < STATEMENT|EXPRESSION DELIMITER; >
#=============================================================================
-->
<xsl:template match="*" mode="startTransformTopLevel" priority="2">
    <xsl:copy>
	<!-- process top-level STATEMENT | EXPRESSION -->
	<xsl:apply-templates select="*" mode="transformTopLevel">
	</xsl:apply-templates>
    </xsl:copy>
</xsl:template>

<!--
#====
# Interpretive mode: at top level outside any class or procedure.
#	def x:=e => x=e.next()	# outermost local variable declarations
#	foo => (foo).next()	# outermost statements or expressions
#
# REVISED: if declare reified variables:
#	x:=init => x = null ; x_r = new IconVar(...) ; x = init;
# since otherwise init may reference reified before it is declared.
#
# TAGS top level: PROGRAM/<STATEMENT_locals; EXPRESSION|STATEMENT DELIM;>
#====
-->

<!--
#====
# Process top-level local declarations.
#====
-->
<xsl:template match="STATEMENT[KEYWORD[(string()='local') or (string()='static')]]" mode="transformTopLevel" priority="2">

  <xsl:variable name="isInterpretive" select="cmd:TisProperty('Properties', 'isInterpretive', 'true')"/>
  <xsl:variable name="isEmbedded" select="cmd:TisProperty('Properties', 'isEmbedded', 'true')"/>

  <xsl:copy>

  <!--
  #====
  # Extract any method statics in closures.
  #====
  -->
  <xsl:apply-templates select="." mode="extractMethodStatics"/>

  <!--
  #====
  # Interpretive top-level locals are defined normally and not reified,
  # since in later code we do not distinguish them from extern references,
  # which have no reified declaration.
  # However, top-level temporaries are generated in only reified form, since
  # references can pick up the fact that they are temporary from
  # the ancestor IconIn expression @tmpVarName attribute.
  #====
  -->
    <xsl:choose>
      <xsl:when test="boolean($isEmbedded)">
	<xsl:apply-templates select="." mode="extractLocals">
		<xsl:with-param name="omitDef" select="false()"/>
		<xsl:with-param name="doNext" select="true()"/>
		<xsl:with-param name="addSemicolon" select="true()"/>
		<xsl:with-param name="omitComments" select="true()"/>
		<xsl:with-param name="includeStatics" select="true()"/>
		<xsl:with-param name="modifiers" select="''"/>
		<xsl:with-param name="reifiedModifiers" select="''"/>
		<xsl:with-param name="tmpModifiers" select="''"/>
		<xsl:with-param name="addReified" select="true()"/>
		<!-- Take private off temporaries
		<xsl:with-param name="isMethodLocal" select="true()"/>
		-->
	</xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
	<xsl:apply-templates select="." mode="extractLocals">
		<xsl:with-param name="omitDef" select="boolean($isInterpretive)"/>
		<xsl:with-param name="doNext" select="true()"/>
		<xsl:with-param name="addSemicolon" select="true()"/>
		<xsl:with-param name="omitComments" select="true()"/>
		<xsl:with-param name="includeStatics" select="true()"/>
	</xsl:apply-templates>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:copy>
</xsl:template>

<!--
#====
# Process top-level method declarations.
#====
-->
<xsl:template match="STATEMENT[KEYWORD[string()='method']]" mode="transformTopLevel" priority="2">
  <xsl:param name="modifiers"/>	<!-- Default is public -->

  <xsl:variable name="isInterpretive" select="cmd:TisProperty('Properties', 'isInterpretive', 'true')"/>
  <xsl:variable name="isEmbedded" select="cmd:TisProperty('Properties', 'isEmbedded', 'true')"/>

    <xsl:variable name="classname" select="''"/>

    <xsl:variable name="classCachename">
	<xsl:value-of select="ancestor-or-self::*[(self::STATEMENT or self::EXPRESSION) and @classCachename][1]/@classCachename"/>
    </xsl:variable>

    <xsl:variable name="classStaticCachename">
	<xsl:value-of select="ancestor-or-self::*[(self::STATEMENT or self::EXPRESSION) and @classStaticCachename][1]/@classStaticCachename"/>
    </xsl:variable>

    <xsl:variable name="classInitialCachename">
	<xsl:value-of select="ancestor-or-self::*[(self::STATEMENT or self::EXPRESSION) and @classInitialCachename][1]/@classInitialCachename"/>
    </xsl:variable>

    <xsl:variable name="methodModifierRTF">
	<xsl:choose>
	  <xsl:when test="boolean($isJava)">
		<xsl:text>public</xsl:text>
	  </xsl:when>
	  <xsl:otherwise>
		<xsl:text>public</xsl:text>
	  </xsl:otherwise>
	</xsl:choose>
    </xsl:variable>
    <xsl:variable name="methodModifier">
	<xsl:value-of select="$methodModifierRTF"/>
    </xsl:variable>

    <!-- Test if only one procedure method, so can use static initializer -->
    <xsl:variable name="useStaticInitializer" select="false()"/>

    <xsl:copy>
    <xsl:copy-of select="@*"/>

	<!--
	#=====================================================================
	# // Method body cache
	#=====================================================================
	-->
	<STATEMENT>
		<COMMENT>
		<xsl:text>// Method body cache</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	</STATEMENT>

	<!-- Add unique cache declaration: def cache=new MethodBodyCache(); -->
	<xsl:choose>
	  <xsl:when test="KEYWORD[string()='static']">
	    <STATEMENT>
	    <xsl:text>private static MethodBodyCache </xsl:text>
	    <xsl:value-of select="$classStaticCachename"/>
	    <xsl:text> = new MethodBodyCache()</xsl:text>
	    <DELIMITER ID=";"/>
	    </STATEMENT>
	  </xsl:when>
	  <xsl:otherwise>
	    <STATEMENT>
	    <xsl:text>private MethodBodyCache </xsl:text>
	    <xsl:value-of select="$classCachename"/>
	    <xsl:text> = new MethodBodyCache()</xsl:text>
	    <DELIMITER ID=";"/>
	    </STATEMENT>
	  </xsl:otherwise>
	</xsl:choose>

	<!--
	#=====================================================================
	# // Static initializer cache, for non-static method initial clauses.
	#=====================================================================
	-->
	<xsl:if test="not(boolean($useStaticInitializer))">
	<xsl:if test="BLOCK/STATEMENT[KEYWORD[string()='initial']]">
	    <STATEMENT>
		<COMMENT>
		<xsl:text>// Static method initializer cache</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	    </STATEMENT>

	    <STATEMENT>
		<xsl:text>private static ConcurrentHashMap&lt;String,Object&gt; </xsl:text>
		<xsl:value-of select="$classInitialCachename"/>
		<xsl:text> = new ConcurrentHashMap()</xsl:text>
		<DELIMITER ID=";"/>
	    </STATEMENT>
	</xsl:if>
	</xsl:if>

	<!--
	#=====================================================================
	# // Add method references for method names.
	#=====================================================================
	-->
      <xsl:if test="boolean($asMethodNotClosure)">
	    <STATEMENT>
		<COMMENT>
		<xsl:text>// Method references</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	    </STATEMENT>
	    <xsl:apply-templates select="." mode="createMethodReference">
		<xsl:with-param name="modifiers" select="'public'"/>
		<xsl:with-param name="useMethodUniquename" select="false()"/>
			<!-- For non-main methods -->
		<xsl:with-param name="useMethodUniquenameIfMain" select="true()"/>
		<xsl:with-param name="classname" select="$classname"/>
	    </xsl:apply-templates>
      </xsl:if>

	<!--
	#=====================================================================
	# // Method statics
	#=====================================================================
	-->
	<xsl:apply-templates select="." mode="extractMethodStatics"/>

	<!--
	#=====================================================================
	# // Methods
	#=====================================================================
	-->
	    <STATEMENT>
		<COMMENT>
		<xsl:text>// Method</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	    </STATEMENT>

	<!--
	#====
	# Translate methods, i.e., body excluding locals.
	#====
	-->
	<xsl:apply-templates select="." mode="createMethod">
		<xsl:with-param name="classCachename" select="$classCachename"/>
		<xsl:with-param name="classStaticCachename" select="$classStaticCachename"/>
		<xsl:with-param name="classInitialCachename" select="$classInitialCachename"/>
		<xsl:with-param name="asMethodNotClosure" select="$asMethodNotClosure"/>

			<!-- For non-main methods -->
		<xsl:with-param name="modifiers" select="$methodModifier"/>
		<xsl:with-param name="useMethodUniquename" select="false()"/>

			<!-- For main method -->
		<xsl:with-param name="mainModifiers" select="'private'"/>
		<xsl:with-param name="useMethodUniquenameIfMain" select="$asMethodNotClosure"/>
		<xsl:with-param name="classname" select="$classname"/>
		<xsl:with-param name="useStaticInitializer" select="$useStaticInitializer"/>
	</xsl:apply-templates>

    </xsl:copy>
</xsl:template>

<!--
#====
# Shift back to transform mode for:
#	class, procedure, global, package, import, link, invocable
# Could move top-level create global here.
#====
-->
<xsl:template match="STATEMENT[KEYWORD[(string()='class') or (string()='procedure') or (string()='global') or (string()='package') or (string()='import') or (string()='link') or (string()='invocable')]]" mode="transformTopLevel" priority="2">
	<xsl:apply-templates select=".">
	</xsl:apply-templates>
</xsl:template>

<!--
#====
# Must transform top-level expressions, not just statements.
# Lowest priority here, must give higher priority to:
#	class, procedure, global, local (and standalone method)
# Must extract any statics in descendant closures (method statics).
#====
-->
<xsl:template match="STATEMENT | EXPRESSION" mode="transformTopLevel" priority="1">

  <xsl:variable name="isInterpretive" select="cmd:TisProperty('Properties', 'isInterpretive', 'true')"/>
  <xsl:variable name="isEmbedded" select="cmd:TisProperty('Properties', 'isEmbedded', 'true')"/>

  <xsl:choose>
  <xsl:when test="boolean($isInterpretive)">

    <!--
    #====
    # Extract any method statics in closures.
    #====
    -->
    <xsl:apply-templates select="." mode="extractMethodStatics"/>

    <!--
    #====
    # Implicit next() around expression iterator.
    # Add semicolon, unless isEmbedded.
    #====
    -->
    <STATEMENT>		<!-- for format -->
	<DELIMITER ID="("/>
		<xsl:apply-templates select="."/>
	<DELIMITER ID=")"/>
	<!-- Insert next() for top-level -->
	<xsl:text>.next()</xsl:text>
	<!-- Not need redundant ; after PROGRAM EXPR ; -->
	<xsl:if test="not(boolean($isEmbedded)) and not(parent::*[DELIMITER[@ID=';']])">
		<DELIMITER ID=";"/>
	</xsl:if>
    </STATEMENT>
  </xsl:when>
  <xsl:otherwise>
	<xsl:apply-templates select="."/>
  </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ==== Default template to shift back to transform mode -->
<xsl:template match="@*|node()" mode="transformTopLevel">
	<xsl:apply-templates select=".">
	</xsl:apply-templates>
</xsl:template>

<!--
#=============================================================================
# Local and static variable declarations.
#=============================================================================
-->

<!--
#====
# Extract local or static declarations from block.
# How we handle variable declarations:
#   Temporary: just reified
#	IconTmp t_r = new IconTmp();    // defines get and set over Object field
#   Method local or parameter: just reified
#	IconVar m_r = new IconVar();    // for method local or parameter
#   Class local or top level: regular & reified
#	Object x;  IconVar x_r = new IconVar(()->x, (rhs)->x=rhs)
#		   if ! $isJava, use IconRef/IconRefIterator instead of IconVar
#   Class field parameter: same as class local  (but handled by createField)
#
#   We use "t_r" and "m_r" instead of the original names,
#   the code looks more intuitive.
#   For external references that are not @isLocal or @isToClassField,
#   we don't create reified variables: we could create them per class and
#   method, but it doesn't save much work, since lift mode still has to
#   perform some dynamic reification for "o.x.y" anyway.
#
# createLocal
#   declareReified (class|top): private [static] IconVar<Type>
#		x_r = new IconVar(()->x, (rhs)->x=rhs)
#		where x_r = TgetSameUnique('x') and x is variable name.
#		if $isMethodLocal, then append: .local()
#		if ! $isJava, use IconRef/IconRefIterator instead of IconVar
#   isMethodLocal: if justResetToNull: x_r.set(null)
#	         else: IconVar<Type> m_r = new IconVar().local();
#   isToGlobal: IconGlobal<Type> t_r = new IconGlobal("t");
#   isTmp:  private IconTmp<Type> t_r = new IconTmp();
#   Otherwise class|top: if omitDef: x = null
#			 else: public [static] Object|Type x;
# createLocalAssign
#   isMethodLocal: x = assignment	(will later be transformed to use x_r)
#   Otherwise class|top:
#	if (! omitDef) public [static] Object|Type
#	x = assignment
#	if (doNext) .next()
# 
# Extract local or static declarations from block:
#   local x:=e => def x    | def x:=e  | def x:=e.nextAtom()?.deref()
#	       if justDefs | !justDefs | doNext
#   if omitDef => omit def
#   if omitDef and justDefs => "x=null" : this is for top-level declarations.
#   if includeAssignments, includes locals with assign.
#   if includeDeclarations, includes locals without assign.
#   if justResetToNull, resets non-temporaries to null,
#		typically for includeDeclarations and !includeAssignments.
#   if declareReified, creates reified variable declarations for non-temporaries
#	def x_r = new IconVar(()->x, (rhs)->x=rhs);
#	where x_r = TgetSameUnique('x') and x is variable name.
#   static => static local
#   local x;                ==> def x;
#   local x=e, y=e;         ==> def x,y; x:=e; y:=e;
#   local type x=e, y=e;    ==> type x,y; x:=e; y:=e;
# Wraps each local declaration or initializer with LOCAL tag.
# Result will have ";" delimiter if "addSemicolon".
# TAGS local: STATEMENT/LOCAL/DELIMITER;
#====
-->
<xsl:template match="STATEMENT[KEYWORD[(string()='local') or (string()='static')]]" mode="extractLocals" priority="2">
    <!--
    #====
    # Modifiers only applies to class fields, i.e., variables that are not
    # temporary, declareReified, isMethodLocal, or omitDef.
    # The latter are all explicitly private or package (no modifier).
    #====
    -->
    <xsl:param name="modifiers" select="'public '"/>
    <xsl:param name="reifiedModifiers" select="'private '"/>
    <xsl:param name="tmpModifiers" select="'private '"/>
    <xsl:param name="type" select="*[2][self::TYPE]"/>	<!-- Override type -->
    <xsl:param name="isStatic" select="KEYWORD[string()='static']"/>
    <xsl:param name="omitDef" select="false()"/>
    <xsl:param name="justDeclarations" select="false()"/>
    <xsl:param name="justResetToNull" select="false()"/>
    <xsl:param name="includeAssignments" select="true()"/>
    <xsl:param name="includeDeclarations" select="true()"/>
    <xsl:param name="includeTemporaries" select="true()"/>
    <xsl:param name="includeStatics" select="true()"/>
    <xsl:param name="doNext" select="false()"/>
    <xsl:param name="addSemicolon" select="false()"/>
    <xsl:param name="omitComments" select="false()"/>

    <!-- Create reified declaration in addition to each declaration -->
    <xsl:param name="addReified" select="false()"/>
    <xsl:param name="addReifiedIsMethodLocal" select="false()"/>
    <!-- Create only reified declaration -->
    <xsl:param name="declareReified" select="false()"/>

    <!-- Annotations -->
    <xsl:param name="addFieldAnnotation" select="false()"/>
    <xsl:param name="createLocalAnnotation" select="false()"/>

    <!-- Method or closure local: will append .local() to any declaration -->
    <xsl:param name="isMethodLocal" select="false()"/>
    <!-- Will truncate assignment to x=expr -->
    <xsl:param name="justAssignment" select="false()"/>

    <!-- <xsl:param name="isParameter" select="false()"/>  See: createField -->
    <xsl:param name="commentTitle"/>	<!-- Override for non-temporaries -->
    <xsl:param name="commentTitleTemporaries"/>	<!-- Override for temporaries -->

  <!--
  #====
  # Only generate statement if subset to apply to is non-empty:
  #	If includeAssignments then must have ENUM/ASSIGN.
  #	If includeDeclarations then must have ENUM/DECLARATION.
  #	If not includeStatics then must be local declaration.
  #====
  -->
  <xsl:if test="((boolean($includeAssignments) and ENUM/ASSIGN) or (boolean($includeDeclarations) and ENUM/DECLARATION)) and (boolean($includeStatics) or KEYWORD[(string()='local')])">
    <xsl:copy>
    <xsl:copy-of select="@*"/>

    <!--
    #====
    # Output comment if first temporary or first non-temporary.
    #====
    -->
    <xsl:if test="boolean($includeDeclarations) and not(boolean($omitComments))">
      <xsl:choose>
      <xsl:when test="ENUM/DECLARATION[@isTmpSynthetic]">
	<!-- temporary: if not temporary before, comment -->
	<xsl:if test="not(preceding-sibling::STATEMENT[KEYWORD[string()='local'] and ENUM/DECLARATION[@isTmpSynthetic]])">
	  <xsl:if test="not(boolean($declareReified)) and not(boolean($justResetToNull))">
	    <xsl:choose>
	    <xsl:when test="boolean($commentTitleTemporaries)">
		<xsl:value-of select="$commentTitleTemporaries"/>
	    </xsl:when>
	    <xsl:otherwise>
	      <STATEMENT>
		<COMMENT>
		<xsl:text>// Temporaries</xsl:text>
		</COMMENT>
	      <NEWLINE/>
	      </STATEMENT>
	    </xsl:otherwise>
	    </xsl:choose>
	  </xsl:if>
	</xsl:if>
      </xsl:when>
      <xsl:otherwise>
	<!-- non-temporary: if not non-temporary before, comment -->
	<xsl:if test="not(preceding-sibling::STATEMENT[KEYWORD[(string()='local') or (string()='static')] and not(ENUM/DECLARATION[@isTmpSynthetic])])">
	  <xsl:choose>
	  <xsl:when test="boolean($commentTitle)">
		<xsl:value-of select="$commentTitle"/>
	  </xsl:when>
	  <xsl:when test="boolean($justResetToNull)">
	    <STATEMENT>
		<COMMENT>
		<xsl:text>// Reset locals</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	    </STATEMENT>
	  </xsl:when>
	  <xsl:when test="boolean($declareReified)">
	    <STATEMENT>
		<COMMENT>
		<xsl:text>// Reified locals</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	    </STATEMENT>
	  </xsl:when>
	  <xsl:otherwise>
	    <STATEMENT>
		<COMMENT>
		<xsl:text>// Locals</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	    </STATEMENT>
	  </xsl:otherwise>
	  </xsl:choose>
	</xsl:if>
      </xsl:otherwise>
      </xsl:choose>
    </xsl:if>

    <!--
    #====
    # Create declarations or resets for locals without initializers.
    #====
    -->
    <xsl:if test="boolean($includeDeclarations)">
	<!--
	#====
	# Declare locals for method, class, or top-level expression.
	# No temporaries, parameters, or assignments here.
	# Method locals are just reified holding the object, with ".local()".
	# Can also just reset to null, or just declare reified.
	#====
	-->
	<xsl:apply-templates select="ENUM/DECLARATION[not(@isTmpSynthetic)]" mode="createLocalPlusReified">
		<xsl:with-param name="type" select="$type"/>
		<xsl:with-param name="modifiers" select="$modifiers"/>
		<xsl:with-param name="reifiedModifiers" select="$reifiedModifiers"/>
		<xsl:with-param name="tmpModifiers" select="$tmpModifiers"/>
		<xsl:with-param name="isStatic" select="$isStatic"/>
		<xsl:with-param name="omitDef" select="$omitDef"/>
		<xsl:with-param name="addSemicolon" select="$addSemicolon"/>
		<xsl:with-param name="isMethodLocal" select="$isMethodLocal"/>
		<xsl:with-param name="declareReified" select="$declareReified"/>
		<xsl:with-param name="justResetToNull" select="$justResetToNull"/>
		<xsl:with-param name="addReified" select="$addReified"/>
		<xsl:with-param name="addReifiedIsMethodLocal" select="$addReifiedIsMethodLocal"/>
		<xsl:with-param name="addFieldAnnotation" select="$addFieldAnnotation"/>
		<xsl:with-param name="createLocalAnnotation" select="$createLocalAnnotation"/>
	</xsl:apply-templates>

	<!--
	#====
	# Declare any temporary variables.  Omit .local().
	#====
	-->
	<xsl:if test="boolean($includeTemporaries) and (not(boolean($justResetToNull) or boolean($declareReified)))">
	  <xsl:apply-templates select="ENUM/DECLARATION[@isTmpSynthetic]" mode="createLocal">
		<xsl:with-param name="type" select="$type"/>
		<xsl:with-param name="modifiers" select="$modifiers"/>
		<xsl:with-param name="reifiedModifiers" select="$reifiedModifiers"/>
		<xsl:with-param name="tmpModifiers" select="$tmpModifiers"/>
		<xsl:with-param name="isStatic" select="$isStatic"/>
		<xsl:with-param name="omitDef" select="$omitDef"/>
		<xsl:with-param name="addSemicolon" select="$addSemicolon"/>
		<xsl:with-param name="isMethodLocal" select="$isMethodLocal"/>
		<xsl:with-param name="isTmpSynthetic" select="true()"/>
	  </xsl:apply-templates>
	</xsl:if>
    </xsl:if>

    <!--
    #====
    # Create either declarations or initializers for locals with inits.
    # No temporaries here, since they are not declared using assignments.
    #====
    -->
    <xsl:if test="boolean($includeAssignments)">
      <xsl:choose>
	<!--
	#====
	# Declare locals.
	#====
	-->
	<xsl:when test="boolean($declareReified) or boolean($justDeclarations) or boolean($justResetToNull)">
	  <xsl:apply-templates select="ENUM/ASSIGN/DECLARATION" mode="createLocalPlusReified">
	    <xsl:with-param name="type" select="$type"/>
	    <xsl:with-param name="modifiers" select="$modifiers"/>
	    <xsl:with-param name="reifiedModifiers" select="$reifiedModifiers"/>
	    <xsl:with-param name="tmpModifiers" select="$tmpModifiers"/>
	    <xsl:with-param name="isStatic" select="$isStatic"/>
	    <xsl:with-param name="omitDef" select="$omitDef"/>
	    <xsl:with-param name="addSemicolon" select="$addSemicolon"/>
	    <xsl:with-param name="isMethodLocal" select="$isMethodLocal"/>
	    <xsl:with-param name="declareReified" select="$declareReified"/>
	    <xsl:with-param name="justResetToNull" select="$justResetToNull"/>
	    <xsl:with-param name="addReified" select="$addReified"/>
	    <xsl:with-param name="addReifiedIsMethodLocal" select="$addReifiedIsMethodLocal"/>
	    <xsl:with-param name="addFieldAnnotation" select="$addFieldAnnotation"/>
	    <xsl:with-param name="createLocalAnnotation" select="$createLocalAnnotation"/>
	  </xsl:apply-templates>
	</xsl:when>

	<!--
	#====
	# Create assignment for locals.
	# For methods, will pull out assignment to place in method body,
	#	or will reset to null to place in unpackargs.
	# For class and top-level locals, will attach assignment to declaration.
	#====
	-->
	<xsl:otherwise>
	  <xsl:apply-templates select="ENUM/ASSIGN" mode="createLocalAssignPlusReified">
	    <xsl:with-param name="type" select="$type"/>
	    <xsl:with-param name="modifiers" select="$modifiers"/>
	    <xsl:with-param name="reifiedModifiers" select="$reifiedModifiers"/>
	    <xsl:with-param name="tmpModifiers" select="$tmpModifiers"/>
	    <xsl:with-param name="isStatic" select="$isStatic"/>
	    <xsl:with-param name="omitDef" select="$omitDef"/>
	    <xsl:with-param name="doNext" select="$doNext"/>
	    <xsl:with-param name="addSemicolon" select="$addSemicolon"/>
	    <xsl:with-param name="isMethodLocal" select="$isMethodLocal"/>
	    <xsl:with-param name="justAssignment" select="$justAssignment"/>
	    <xsl:with-param name="addReified" select="$addReified"/>
	    <xsl:with-param name="addReifiedIsMethodLocal" select="$addReifiedIsMethodLocal"/>
	    <xsl:with-param name="addFieldAnnotation" select="$addFieldAnnotation"/>
	  </xsl:apply-templates>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:if>

    </xsl:copy>
  </xsl:if>
</xsl:template>

<!--
#====
# Default template to drop any non-local nodes
#====
-->
<xsl:template match="@*|node()" mode="extractLocals"/>

<!--
#====
# Create local declaration, plus reified declaration if addReified.
#====
-->
<xsl:template match="DECLARATION" mode="createLocalPlusReified" priority="2">
    <xsl:param name="modifiers"/>
    <xsl:param name="reifiedModifiers"/>
    <xsl:param name="tmpModifiers"/>
    <xsl:param name="type"/>
    <xsl:param name="isStatic"/>
    <xsl:param name="omitDef" select="false()"/>
    <xsl:param name="addSemicolon" select="false()"/>
    <xsl:param name="isMethodLocal" select="false()"/>
    <xsl:param name="declareReified" select="false()"/>
    <xsl:param name="justResetToNull" select="false()"/>
    <xsl:param name="isTmpSynthetic" select="false()"/>
    <xsl:param name="addReified" select="false()"/>
    <xsl:param name="addReifiedIsMethodLocal" select="false()"/>
    <xsl:param name="addFieldAnnotation" select="false()"/>
    <xsl:param name="isConstructorField" select="false()"/>
    <xsl:param name="createLocalAnnotation" select="false()"/>

    <xsl:apply-templates select="." mode="createLocal">
	<xsl:with-param name="type" select="$type"/>
	<xsl:with-param name="modifiers" select="$modifiers"/>
	<xsl:with-param name="reifiedModifiers" select="$reifiedModifiers"/>
	<xsl:with-param name="tmpModifiers" select="$tmpModifiers"/>
	<xsl:with-param name="isStatic" select="$isStatic"/>
	<xsl:with-param name="omitDef" select="$omitDef"/>
	<xsl:with-param name="addSemicolon" select="$addSemicolon"/>
	<xsl:with-param name="isMethodLocal" select="$isMethodLocal"/>
	<xsl:with-param name="declareReified" select="$declareReified"/>
	<xsl:with-param name="justResetToNull" select="$justResetToNull"/>
	<xsl:with-param name="isTmpSynthetic" select="$isTmpSynthetic"/>
	<xsl:with-param name="addFieldAnnotation" select="$addFieldAnnotation"/>
	<xsl:with-param name="isConstructorField" select="$isConstructorField"/>
	<xsl:with-param name="createLocalAnnotation" select="$createLocalAnnotation"/>
    </xsl:apply-templates>

  <xsl:if test="boolean($addReified) and not(boolean($createLocalAnnotation))">
    <xsl:apply-templates select="." mode="createLocal">
	<xsl:with-param name="type" select="$type"/>
	<xsl:with-param name="modifiers" select="$modifiers"/>
	<xsl:with-param name="reifiedModifiers" select="$reifiedModifiers"/>
	<xsl:with-param name="tmpModifiers" select="$tmpModifiers"/>
	<xsl:with-param name="isStatic" select="$isStatic"/>
	<xsl:with-param name="omitDef" select="$omitDef"/>
	<xsl:with-param name="addSemicolon" select="$addSemicolon"/>
	<xsl:with-param name="isMethodLocal" select="$addReifiedIsMethodLocal"/>
	<xsl:with-param name="declareReified" select="true()"/>
    </xsl:apply-templates>
  </xsl:if>
</xsl:template>

<!--
#====
# Create local assign, plus reified declaration if addReified.
#====
-->
<xsl:template match="ASSIGN" mode="createLocalAssignPlusReified" priority="2">
    <xsl:param name="modifiers"/>	<!-- e.g., public -->
    <xsl:param name="reifiedModifiers"/>
    <xsl:param name="tmpModifiers"/>
    <xsl:param name="type"/>
    <xsl:param name="isStatic"/>
    <xsl:param name="omitDef" select="false()"/>
    <xsl:param name="doNext" select="false()"/>
    <xsl:param name="addSemicolon" select="false()"/>
    <xsl:param name="isMethodLocal" select="false()"/>
    <xsl:param name="justAssignment" select="false()"/>
    <xsl:param name="addReified" select="false()"/>
    <xsl:param name="addReifiedIsMethodLocal" select="false()"/>
    <xsl:param name="addFieldAnnotation" select="false()"/>

    <xsl:apply-templates select="." mode="createLocalAssign">
	<xsl:with-param name="type" select="$type"/>
	<xsl:with-param name="modifiers" select="$modifiers"/>
	<xsl:with-param name="reifiedModifiers" select="$reifiedModifiers"/>
	<xsl:with-param name="tmpModifiers" select="$tmpModifiers"/>
	<xsl:with-param name="isStatic" select="$isStatic"/>
	<xsl:with-param name="omitDef" select="$omitDef"/>
	<xsl:with-param name="doNext" select="$doNext"/>
	<xsl:with-param name="addSemicolon" select="$addSemicolon"/>
	<xsl:with-param name="isMethodLocal" select="$isMethodLocal"/>
	<xsl:with-param name="justAssignment" select="$justAssignment"/>
	<xsl:with-param name="addFieldAnnotation" select="$addFieldAnnotation"/>
    </xsl:apply-templates>

  <xsl:if test="boolean($addReified)">
    <xsl:apply-templates select="DECLARATION" mode="createLocal">
	<xsl:with-param name="type" select="$type"/>
	<xsl:with-param name="modifiers" select="$modifiers"/>
	<xsl:with-param name="reifiedModifiers" select="$reifiedModifiers"/>
	<xsl:with-param name="tmpModifiers" select="$tmpModifiers"/>
	<xsl:with-param name="isStatic" select="$isStatic"/>
	<xsl:with-param name="omitDef" select="$omitDef"/>
	<xsl:with-param name="doNext" select="false()"/>
	<xsl:with-param name="addSemicolon" select="$addSemicolon"/>
	<xsl:with-param name="isMethodLocal" select="$addReifiedIsMethodLocal"/>
	<xsl:with-param name="declareReified" select="true()"/>
    </xsl:apply-templates>
  </xsl:if>
</xsl:template>

<!--
#====
# Derive reified variable name.
# If isLocal, uses "x_r" instead of "x".
# Preserves DECLARATION/IDENTIFIER or ATOM/IDENTIFIER structure.
#====
-->
<xsl:template match="*" mode="deriveReifiedVarName" priority="2">
    <xsl:param name="doReified" select="false()"/>

    <xsl:variable name="origVarName">
	<xsl:value-of select="."/>
    </xsl:variable>

    <xsl:copy>
    <xsl:copy-of select="@*"/>
	<xsl:choose>
	<xsl:when test="boolean($doReified)">
	    <IDENTIFIER>
		<xsl:value-of select="cmd:TgetSameUnique($origVarName, '_r')"/>
	    </IDENTIFIER>
	</xsl:when>
	<xsl:otherwise>
		<xsl:copy-of select="*"/>
	</xsl:otherwise>
	</xsl:choose>
    </xsl:copy>
</xsl:template>

<!--
#====
# Create local variable declaration, or create global redirection field.
#   declareReified (class|top): private [static] IconVar<Type>
#		x_r = new IconVar(()->x, (rhs)->x=rhs)
#		where x_r = TgetSameUnique('x') and x is variable name.
#	    if $isMethodLocal, then append: .local() so dereferenced on return.
#	    if ! $isJava, use IconRef/IconRefIterator instead of IconVar
#   isToGlobal: if omitDef: t_r = new IconGlobal("t");
#	        else:       IconGlobal<Type> t_r = new IconGlobal("t");
#   isTmpSynthetic:  if omitDef: t_r = new IconTmp();	(IconVar if ! isTmpVar)
#	    else if isMethodLocal: IconTmp<Type> t_r = new IconTmp();
#	    else: private IconTmp<Type> t_r = new IconTmp();
#   isMethodLocal: if justResetToNull: x_r.set(null)
#	           else: IconVar<Type> m_r = new IconVar().local();
#   Otherwise class|top-level field: if omitDef: x = null
#			 else: public [static] Object|Type x;
# NOTE: .local() is redundant, since know its local if not tmp and no setter.
# NOTE: IconTmp do not need .local(), since are never returned.
#====
-->
<xsl:template match="DECLARATION" mode="createLocal" priority="2">
    <!--
    #====
    # Modifiers only applies to class fields, i.e., variables that are not
    # temporary, declareReified, isMethodLocal, or omitDef.
    # The latter are all explicitly private or package (no modifier).
    #====
    -->
    <xsl:param name="modifiers"/>
    <xsl:param name="reifiedModifiers"/>
    <xsl:param name="tmpModifiers"/>
    <xsl:param name="type"/>
    <xsl:param name="isStatic"/>
    <xsl:param name="omitDef" select="false()"/>
    <xsl:param name="addSemicolon" select="false()"/>
    <xsl:param name="isMethodLocal" select="false()"/>
    <xsl:param name="declareReified" select="false()"/>
    <xsl:param name="justResetToNull" select="false()"/>
    <xsl:param name="isTmpSynthetic" select="false()"/>
    <xsl:param name="isTmpVar" select="@isTmpVar"/>
    <xsl:param name="isToGlobal" select="false()"/>
    <xsl:param name="addFieldAnnotation" select="false()"/>
    <xsl:param name="isConstructorField" select="false()"/>
    <xsl:param name="createLocalAnnotation" select="false()"/>

    <xsl:variable name="origVarName">
	<xsl:value-of select="."/>
    </xsl:variable>

    <!-- Use just reified name for local, tmp, reified field, or global -->
    <xsl:variable name="varNameRTF">
	<xsl:apply-templates select="." mode="deriveReifiedVarName">
		<xsl:with-param name="doReified" select="boolean($isMethodLocal) or boolean($isTmpSynthetic) or boolean($declareReified) or boolean($isToGlobal)"/>
	</xsl:apply-templates>
    </xsl:variable>
    <xsl:variable name="varName">
	<xsl:value-of select="$varNameRTF"/>
    </xsl:variable>

    <xsl:variable name="quotedOrigVarNameRTF">
	<xsl:text>"</xsl:text>
	<xsl:value-of select="$origVarName"/>
	<xsl:text>"</xsl:text>
    </xsl:variable>
    <xsl:variable name="quotedOrigVarName">
	<xsl:value-of select="$quotedOrigVarNameRTF"/>
    </xsl:variable>

    <xsl:variable name="reifiedVarName">
	<!-- deriveReifiedVarName -->
	<xsl:value-of select="cmd:TgetSameUnique($origVarName, '_r')"/>
    </xsl:variable>

    <xsl:variable name="staticTextRTF">
	<xsl:if test="boolean($isStatic)">
		<xsl:text>static </xsl:text>
	</xsl:if>
    </xsl:variable>
    <xsl:variable name="staticText">
	<xsl:value-of select="$staticTextRTF"/>
    </xsl:variable>

    <xsl:variable name="finalTextRTF">
	<xsl:if test="boolean($isJava) and boolean($useInnerClasses)">
		<xsl:text>final </xsl:text>
	</xsl:if>
    </xsl:variable>
    <xsl:variable name="finalText">
	<xsl:value-of select="$finalTextRTF"/>
    </xsl:variable>

    <xsl:variable name="typeTextRTF">
	<xsl:if test="boolean($type)">
		<xsl:text>&lt;</xsl:text>
		<xsl:value-of select="$type"/>
		<xsl:text>&gt;</xsl:text>
	</xsl:if>
    </xsl:variable>
    <xsl:variable name="typeText">
	<xsl:value-of select="$typeTextRTF"/>
    </xsl:variable>

    <xsl:variable name="typeOrObjectRTF">
      <xsl:choose>
	<xsl:when test="boolean($type)">
		<xsl:value-of select="$type"/>
		<xsl:text> </xsl:text>
	</xsl:when>
	<xsl:otherwise>
		<xsl:value-of select="$defObject"/>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="typeOrObject">
	<xsl:value-of select="$typeOrObjectRTF"/>
    </xsl:variable>

    <xsl:variable name="tmpConstructorRTF">
      <xsl:choose>
	<xsl:when test="boolean($isTmpVar)">
		<xsl:value-of select="$newIconTmpTmp"/>
	</xsl:when>
	<xsl:otherwise>
		<xsl:value-of select="$newIconTmpVar"/>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="tmpConstructor">
	<xsl:value-of select="$tmpConstructorRTF"/>
    </xsl:variable>

    <xsl:variable name="tmpDeclRTF">
      <xsl:choose>
	<xsl:when test="boolean($isTmpVar)">
		<xsl:value-of select="$IconTmpTmpDecl"/>
	</xsl:when>
	<xsl:otherwise>
		<xsl:value-of select="$IconTmpVarDecl"/>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="tmpDecl">
	<xsl:value-of select="$tmpDeclRTF"/>
    </xsl:variable>

  <LOCAL>

  <!--
  #====
  # Create annotation: @MField(name, reifiedName, type, isConstructorField)
  #====
  -->
  <xsl:if test="boolean($addFieldAnnotation) and boolean($addAnnotations)">
    <STATEMENT>
	<xsl:text>@MField(name=</xsl:text>
	    <xsl:text>"</xsl:text>
	    <xsl:value-of select="$varName"/>
	    <xsl:text>"</xsl:text>
	<xsl:text>, reifiedName=</xsl:text>
	    <xsl:text>"</xsl:text>
	    <xsl:value-of select="$reifiedVarName"/>
	    <xsl:text>"</xsl:text>
	<xsl:text>, type=</xsl:text>
	    <xsl:text>"</xsl:text>
	    <xsl:value-of select="$type"/>
	    <xsl:text>"</xsl:text>
	<xsl:text>, isConstructorField=</xsl:text>
	    <xsl:value-of select="string($isConstructorField)"/>
	<xsl:text>)</xsl:text>
    <NEWLINE/>
    </STATEMENT>
  </xsl:if>

    <STATEMENT>

    <xsl:choose>
      <!--
      #====
      # Create annotation: @MLocal(name, reifiedName, type)
      #====
      -->
      <xsl:when test="boolean($createLocalAnnotation)">
       <xsl:if test="boolean($addAnnotations)">
	<xsl:text>@MLocal(name=</xsl:text>
	    <xsl:text>"</xsl:text>
	    <xsl:value-of select="$varName"/>
	    <xsl:text>"</xsl:text>
	<xsl:text>, reifiedName=</xsl:text>
	    <xsl:text>"</xsl:text>
	    <xsl:value-of select="$reifiedVarName"/>
	    <xsl:text>"</xsl:text>
	<xsl:text>, type=</xsl:text>
	    <xsl:text>"</xsl:text>
	    <xsl:value-of select="$type"/>
	    <xsl:text>"</xsl:text>
	<xsl:text>)</xsl:text>
	<NEWLINE/>
       </xsl:if>
      </xsl:when>

      <!--
      #====
      # declareReified:  private [static] [final] IconVar<Type> x_r =
      #		new IconVar(()->x, (rhs)->x=rhs)
      #	        if $isMethodLocal, append: .local()
      #		if ! $isJava, use IconRef/IconRefIterator instead of IconVar
      #====
      -->
      <xsl:when test="boolean($declareReified)">
	<xsl:value-of select="cmd:Treplace('${mod}${static}${final}$constructor$type $var = ','${mod}',$reifiedModifiers,'${static}',$staticText,'${final}',$finalText,'$constructor',$IconVar,'$type',$typeText,'$var',$varName)"/>
	<xsl:apply-templates select="." mode="liftVariable">
		<xsl:with-param name="varName" select="$origVarName"/>
		<xsl:with-param name="type" select="$typeText"/>
		<xsl:with-param name="asIterator" select="false()"/>
		<xsl:with-param name="appendLocal" select="$isMethodLocal"/>
	</xsl:apply-templates>
      </xsl:when>

      <!--
      #====
      # isToGlobal: create global field.
      # private [static] IconGlobal<Type> t_r = new IconGlobal("t");
      #====
      -->
      <xsl:when test="boolean($isToGlobal) and boolean($omitDef)">
	<xsl:value-of select="cmd:Treplace('$var = $constructor$type($quotedOrigVarName)','${static}',$staticText,'$type',$typeText,'$var',$varName,'$IconGlobal',$IconGlobal,'$constructor',$newIconGlobal,'$quotedOrigVarName',$quotedOrigVarName)"/>
      </xsl:when>

      <xsl:when test="boolean($isToGlobal)">
	<xsl:value-of select="cmd:Treplace('private ${static}$IconGlobal$type $var = $constructor$type($quotedOrigVarName)','${static}',$staticText,'$type',$typeText,'$var',$varName,'$IconGlobal',$IconGlobal,'$constructor',$newIconGlobal,'$quotedOrigVarName',$quotedOrigVarName)"/>
      </xsl:when>

      <!--
      #====
      # isTmpSynthetic isTmpVar: private [final] IconTmp<Type> t = new IconTmp()
      # isTmpSynthetic:  private [final] IconVar<Type> t = new IconVar()
      #====
      -->
      <xsl:when test="boolean($isTmpSynthetic)">
	<xsl:choose>
	  <xsl:when test="boolean($omitDef)">
		<xsl:value-of select="cmd:Treplace('$var = $constructor$type()','$type',$typeText,'$var',$varName,'$constructor',$tmpConstructor)"/>
	  </xsl:when>
	  <xsl:when test="boolean($isMethodLocal)">
		<xsl:value-of select="cmd:Treplace('${final}$TmpDecl$type $var = $constructor$type()','${final}',$finalText,'$type',$typeText,'$var',$varName,'$TmpDecl',$tmpDecl,'$constructor',$tmpConstructor)"/>
	  </xsl:when>
	  <xsl:otherwise>
		<xsl:value-of select="cmd:Treplace('${mod}${final}$TmpDecl$type $var = $constructor$type()','${mod}',$tmpModifiers,'${final}',$finalText,'$type',$typeText,'$var',$varName,'$TmpDecl',$tmpDecl,'$constructor',$tmpConstructor)"/>
	  </xsl:otherwise>
	</xsl:choose>
      </xsl:when>

      <!--
      #====
      # isMethodLocal:  if justResetToNull: x.set(null)
      #                 else: IconVar<Type> m = new IconVar().local();
      #====
      -->
      <xsl:when test="boolean($isMethodLocal)">
	<xsl:choose>
	  <xsl:when test="boolean($justResetToNull)">
		<xsl:value-of select="cmd:Treplace('$var.set(null)','$var',$varName)"/>
	  </xsl:when>
	  <xsl:otherwise>
		<xsl:value-of select="cmd:Treplace('${final}$IconVar$type $var = $constructor$type().local()','${final}',$finalText,'$type',$typeText,'$var',$varName,'$IconVar',$IconVar,'$constructor',$newIconVar)"/>
	  </xsl:otherwise>
	</xsl:choose>
      </xsl:when>

      <!--
      #====
      # Otherwise class|top-level field.
      # if omitDef: x = null
      # else: public [static] Object|Type x
      #====
      -->
      <xsl:otherwise>
	<xsl:choose>
	  <xsl:when test="boolean($omitDef)">
		<xsl:copy-of select="."/>	<!-- DECLARATION[IDENTIFIER] -->
		<xsl:text> = null</xsl:text>
	  </xsl:when>
	  <xsl:otherwise>
		<xsl:value-of select="cmd:Treplace('$mod$static$type$var','$mod',$modifiers,'$static',$staticText,'$type',$typeOrObject,'$var',$origVarName)"/>
	  </xsl:otherwise>
	</xsl:choose>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:if test="boolean($addSemicolon)">
	<DELIMITER ID=";"/>
    </xsl:if>

    </STATEMENT>
  </LOCAL>
</xsl:template>

<!--
#====
# Create local assignment for class or top-level variable.
#   justAssignment: x = expr (to be moved inside method body as initializer)
#   Otherwise class|top:
#	if (omitDef) x = (expr)
#	else public [static] Object|Type x = (Type) (expr)
#	     if (doNext) .next()
#====
-->
<xsl:template match="ASSIGN" mode="createLocalAssign" priority="2">
    <!--
    #====
    # Modifiers only applies to class fields, i.e., variables that are not
    # temporary, declareReified, isMethodLocal, or omitDef.
    # The latter are all explicitly private or package (no modifier).
    #====
    -->
    <xsl:param name="modifiers"/>	<!-- e.g., public -->
    <xsl:param name="reifiedModifiers"/>
    <xsl:param name="tmpModifiers"/>
    <xsl:param name="type"/>
    <xsl:param name="isStatic"/>
    <xsl:param name="omitDef" select="false()"/>
    <xsl:param name="doNext" select="false()"/>
    <xsl:param name="addSemicolon" select="false()"/>
    <xsl:param name="isMethodLocal" select="false()"/>
    <xsl:param name="justAssignment" select="false()"/>
    <xsl:param name="addFieldAnnotation" select="false()"/>

    <!-- rhs: create unique rhs variable name for closures -->
    <xsl:variable name="rhsUnique">
	<xsl:value-of select="cmd:TgetReusableUnique('rhs')"/>
    </xsl:variable>

  <LOCAL>
  <STATEMENT>
      <xsl:choose>
	<!--
	#====
	# isMethodLocal: x = expr
	#====
	-->
	<xsl:when test="boolean($justAssignment)">
		<!--
		#====
		# Reformat as ASSIGN, change declaration to identifier.
		#====
		-->
		<xsl:variable name="assign">
		  <EXPRESSION rhsUnique="{$rhsUnique}">
		  <ASSIGN>
			<EXPRESSION>
			  <ATOM lift="true()" isSimple="true()" isLocal="true()">
				<!-- Be sure to create isLocal attribute -->
				<!-- Leave alone, will be transformed to x_r -->
		    		<xsl:copy-of select="DECLARATION/IDENTIFIER"/>
			  </ATOM>
			</EXPRESSION>
			<xsl:copy-of select="EXPRESSION"/>
		  </ASSIGN>
		  </EXPRESSION>
		</xsl:variable>
		<xsl:apply-templates select="xalan:nodeset($assign)/*[1]"/>
	</xsl:when>
	<!--
	#====
	# class | top level expression:
	#   if (omitDef) x = (expr)
	#   else public [static] Object|Type x = (expr)
	#        if (doNext) .next()
	#====
	-->
	<xsl:otherwise>
	  <xsl:choose>
	    <xsl:when test="boolean($omitDef)">
		<xsl:copy-of select="DECLARATION"/>
	    </xsl:when>
	    <xsl:otherwise>
		<!--
		#====
		# Declare class local: public [static] Object|Type x
		#====
		-->
	      <xsl:apply-templates select="DECLARATION" mode="createLocal">
		<xsl:with-param name="type" select="$type"/>
		<xsl:with-param name="modifiers" select="$modifiers"/>
		<xsl:with-param name="reifiedModifiers" select="$reifiedModifiers"/>
		<xsl:with-param name="tmpModifiers" select="$tmpModifiers"/>
		<xsl:with-param name="isStatic" select="$isStatic"/>
		<xsl:with-param name="omitDef" select="false()"/>
		<xsl:with-param name="addSemicolon" select="false()"/>
		<xsl:with-param name="isMethodLocal" select="$isMethodLocal"/>
		<xsl:with-param name="addFieldAnnotation" select="$addFieldAnnotation"/>
	      </xsl:apply-templates>
	    </xsl:otherwise>
	  </xsl:choose>
	  <OPERATOR>= </OPERATOR>

	  <!-- (Type) (expr).next(); -->
	  <xsl:if test="boolean($type)">
		<xsl:text>(</xsl:text>
			<xsl:value-of select="$type"/>
		<xsl:text>) </xsl:text>
	  </xsl:if>
	  <DELIMITER ID="("/>
	  <xsl:apply-templates select="EXPRESSION"/>
	  <DELIMITER ID=")"/>
	  <xsl:if test="boolean($doNext)">
		<!-- Insert next() for local assign -->
		<xsl:text>.nextOrNull()</xsl:text>
	  </xsl:if>

	</xsl:otherwise>
    </xsl:choose>

    <xsl:if test="boolean($addSemicolon)">
	<DELIMITER ID=";"/>
    </xsl:if>
  </STATEMENT>
  </LOCAL>
</xsl:template>

<!--
#====
# Create field from parameter: def x; | type x;
#   If declareReified, will instead declare "IconVar x_r = new IconVar(...).
# TAGS field: TUPLE/QUALIFIED/DECLARATION/IDENTIFIER
# TAGS parameter: TUPLE/< QUALIFIED/< DECLARATION/IDENTIFIER
#			[:Type] [: or = TYPE<LITERAL>] > [SUBSCRIPT] >
#====
-->
<xsl:template match="QUALIFIED" mode="createField" priority="2">
    <xsl:param name="modifiers" select="'public '"/>	<!-- e.g., public -->
    <xsl:param name="reifiedModifiers" select="'private '"/>
    <xsl:param name="type" select="TYPE[DOTNAME or IDENTIFIER]"/>
    <xsl:param name="isMethodLocal" select="false()"/> <!-- vs class field -->
		<!-- isMethodLocal is only used by declareReified -->
    <xsl:param name="declareReified" select="false()"/>
    <xsl:param name="addReified" select="false()"/>
    <xsl:param name="addReifiedIsMethodLocal" select="false()"/>
    <xsl:param name="addFieldAnnotation" select="false()"/>
    <xsl:param name="isConstructorField" select="false()"/>
    <xsl:param name="createParameterAnnotation" select="false()"/>
    <xsl:param name="forceLastVarargs" select="false()"/>	<!-- $isMain -->

  <!--
  #====
  # Create annotation: @MParameter(name,reifiedName,type,defaultValue,isVararg)
  #====
  -->
  <xsl:variable name="origVarName">
	<xsl:value-of select="DECLARATION/IDENTIFIER"/>
  </xsl:variable>

  <xsl:variable name="reifiedVarName">	<!-- deriveReifiedVarName -->
	<xsl:value-of select="cmd:TgetSameUnique($origVarName, '_r')"/>
  </xsl:variable>

  <xsl:variable name="defaultValue">
	<xsl:value-of select="TYPE[LITERAL]"/>
  </xsl:variable>

  <xsl:choose>
  <xsl:when test="boolean($createParameterAnnotation)">
    <xsl:if test="boolean($addAnnotations)">
      <STATEMENT>
	<xsl:text>@MParameter(name=</xsl:text>
	    <xsl:text>"</xsl:text>
	    <xsl:value-of select="$origVarName"/>
	    <xsl:text>"</xsl:text>
	<xsl:text>, reifiedName=</xsl:text>
	    <xsl:text>"</xsl:text>
	    <xsl:value-of select="$reifiedVarName"/>
	    <xsl:text>"</xsl:text>
	<xsl:text>, type=</xsl:text>
	    <xsl:text>"</xsl:text>
	    <xsl:value-of select="$type"/>
	    <xsl:text>"</xsl:text>
	<xsl:if test="boolean($defaultValue)">
	    <xsl:text>, defaultValue=</xsl:text>
	    <xsl:text>"</xsl:text>
	    <xsl:value-of select="$defaultValue"/>
	    <xsl:text>"</xsl:text>
	</xsl:if>

	<!--
	<xsl:if test="following-sibling::*[1][self::SUBSCRIPT] or (boolean($forceLastVarargs) and not(following-sibling::*[1][QUALIFIED]))">
	-->

	<xsl:if test="@isVarargs or (boolean($forceLastVarargs) and @isLastParameter)">
	    <xsl:text>, isVararg=true</xsl:text>
	</xsl:if>
	<xsl:text>)</xsl:text>
      <NEWLINE/>
      </STATEMENT>
    </xsl:if>
  </xsl:when>

  <xsl:otherwise>

  <!--
  #====
  # Create reified parameter declaration.
  #====
  -->
    <xsl:apply-templates select="DECLARATION" mode="createLocalPlusReified">
	<xsl:with-param name="type" select="$type"/>
	<xsl:with-param name="modifiers" select="$modifiers"/>
	<xsl:with-param name="reifiedModifiers" select="$reifiedModifiers"/>
	<xsl:with-param name="doNext" select="false()"/>
	<xsl:with-param name="addSemicolon" select="true()"/>
	<xsl:with-param name="omitComments" select="true()"/>
	<xsl:with-param name="isMethodLocal" select="$isMethodLocal"/>
	<xsl:with-param name="declareReified" select="$declareReified"/>
	<xsl:with-param name="addReified" select="$addReified"/>
	<xsl:with-param name="addReifiedIsMethodLocal" select="$addReifiedIsMethodLocal"/>
	<xsl:with-param name="addFieldAnnotation" select="$addFieldAnnotation"/>
	<xsl:with-param name="isConstructorField" select="$isConstructorField"/>
    </xsl:apply-templates>

  </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!--
#====
# Create global redirection field in the class,
# if there is a matching referenced global in the class.
# There cannot be a conflict with defined fields
# if there is a descendant reference to the global.
#	global x => Object x_r := new IconGlobal("x");
# TAGS global: STATEMENT/< KEYWORD/global ENUM/<DECLARATION/IDENTIFIER, ...> >
#====
-->
<xsl:template match="DECLARATION" mode="createGlobal" priority="2">
  <xsl:param name="class"/>

  <xsl:variable name="varname">
	<xsl:value-of select="IDENTIFIER"/>
  </xsl:variable>

  <!-- Create global field if matching referenced global in the class -->
  <xsl:if test="$class[descendant-or-self::ATOM[@isToGlobal][IDENTIFIER[string()=$varname]][not(parent::OBJECT and (position() &gt; 1))]]">
    <xsl:apply-templates select="." mode="createLocal">
	<xsl:with-param name="isStatic" select="false()"/>
	<xsl:with-param name="addSemicolon" select="true()"/>
	<xsl:with-param name="isToGlobal" select="true()"/>
	<xsl:with-param name="addFieldAnnotation" select="false()"/>
	<xsl:with-param name="createLocalAnnotation" select="false()"/>
    </xsl:apply-templates>
  </xsl:if>
</xsl:template>

<!--
#====
# Extract method static declarations into class.
#	Method statics may be anywhere inside method, initially, procedure,
#	or top-level closure.  They have already been made unique in the class.
# Operates on all descendants of given node.
#
# Method statics are promoted to static class fields.
# They may have assignment statements, so like class fields
# they have both plain and reified versions.
# However, the reified IconVar is .local() so it will be dereferenced on return.
# Transform preprocessing phase 2 ensures that references to method statics
# will turn off @isLocal and turn on @isToClassField, and sets @isToMethodStatic
#====
-->
<xsl:template match="*" mode="extractMethodStatics" priority="2">
	<!-- Output title -->
	<xsl:if test="descendant::STATEMENT[KEYWORD[string()='static']][ENUM[DECLARATION[@isMethodStatic] or ASSIGN/DECLARATION[@isMethodStatic]]]">
	    <STATEMENT>
		<COMMENT>
		<xsl:text>// Method static variables</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	    </STATEMENT>
	</xsl:if>

	<!-- Create reified variables, in extractLocals -->
	  <xsl:apply-templates select="descendant::STATEMENT[KEYWORD[string()='static']][ENUM[DECLARATION[@isMethodStatic] or ASSIGN/DECLARATION[@isMethodStatic]]]" mode="extractLocals">
		<xsl:with-param name="modifiers" select="'private '"/>
		<xsl:with-param name="reifiedModifiers" select="'private '"/>
		<xsl:with-param name="omitDef" select="false()"/>
		<xsl:with-param name="doNext" select="true()"/>
		<xsl:with-param name="addSemicolon" select="true()"/>
		<xsl:with-param name="omitComments" select="true()"/>
		<xsl:with-param name="isMethodLocal" select="false()"/>
		<xsl:with-param name="includeStatics" select="true()"/>
		<xsl:with-param name="addReified" select="true()"/>
		<xsl:with-param name="addReifiedIsMethodLocal" select="true()"/>
	  </xsl:apply-templates>
</xsl:template>

<!--
#=============================================================================
# Blocks and closures.
#=============================================================================
-->

<!-- 
#=============================================================================
# Wrap block with IconBlock if has locals and not method, closure, or class body
#   {locals; body} => IconBlock(()->{locals; body}) if GROUP/BLOCK with local|static
#				i.e., not method | procedure | closure | class
# Then translate block {e;e} into IconSequence, using createSequenceFromBlock.
#=============================================================================
-->
<xsl:template match="GROUP[BLOCK[STATEMENT[KEYWORD[string()='local' or string()='static']]]]" priority="2">
    <!--
    #====
    # Prefix to createSequenceFromBlock, after Locals and Temporaries.
    #	return new IconSequence(...)
    #====
    -->
    <xsl:variable name="prefix">
      <PREFIX>
	  <STATEMENT indent="true()">
	      <xsl:text>return </xsl:text>
	  </STATEMENT>
      </PREFIX>
    </xsl:variable>

    <xsl:variable name="closureBody">
	  <BODY>

	  <xsl:apply-templates select="BLOCK" mode="createSequenceFromBlock">
		<xsl:with-param name="prefix" select="xalan:nodeset($prefix)/PREFIX"/>
		<xsl:with-param name="noindent" select="true()"/>
	  </xsl:apply-templates>

	  </BODY>
    </xsl:variable>

    <xsl:copy>
    <xsl:copy-of select="@*"/>
	<xsl:value-of select="$newIconBlock"/>
	<xsl:text>(</xsl:text>

	<!-- BEGIN formatClosure -->
	<xsl:apply-templates select="." mode="formatClosure">
	  <xsl:with-param name="isEmptyArgs" select="true()"/>
	  <xsl:with-param name="newlineAtEnd" select="true()"/>
	  <xsl:with-param name="body" select="xalan:nodeset($closureBody)/BODY"/>
	</xsl:apply-templates>
	<!-- END formatClosure -->

	<xsl:text> )</xsl:text>
    </xsl:copy>
</xsl:template>

<xsl:template match="GROUP[BLOCK]" priority="1">
    <xsl:copy>
    <xsl:copy-of select="@*"/>
	<xsl:apply-templates select="BLOCK" mode="createSequenceFromBlock">
		<xsl:with-param name="endsWithSemicolon" select="false()"/>
	</xsl:apply-templates>
    </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Translate block {e;e} into IconSequence.
#	{locals=init; body} => {locals; {init;body}} if not class BLOCK
#	{e1; ...; e2} => new IconSequence(e1, ..., e2)
#
#   Will prepend locals, append fail or null, and wrap body with prefix & suffix
#   Does not copy over any outer {} delimiters,
#	which may be not be present anyway in BLOCK, if method or closure.
#   Block may be allempty, i.e. {}
#   {x;} => {x;null} : normalization translated grammar trailingEmpty to null.
#
# OUTPUT: BLOCKSEQUENCE (not BLOCK, since would cause redundant indent)
#   local defs;	// locals include temporaries
#   PREFIX new IconSequence(
#	  localsInit; body [;fail]   // body will not end with ;
#	| localsInit; null | fail    if no body
#   ) SUFFIX
#=============================================================================
-->
<xsl:template match="BLOCK" mode="createSequenceFromBlock" priority="2">
    <xsl:param name="appendFail" select="false()"/>
    <xsl:param name="prefix"/>
    <xsl:param name="suffix"/>
    <xsl:param name="endsWithSemicolon" select="true()"/>
    <xsl:param name="noindent" select="false()"/>

    <BLOCKSEQUENCE>		<!-- for format -->
    <xsl:copy-of select="@*"/>

    <!-- Create local declarations without their initializers -->
    <xsl:apply-templates select="*" mode="extractLocals">
	<xsl:with-param name="omitDef" select="false()"/>
	<xsl:with-param name="doNext" select="false()"/>
	<xsl:with-param name="addSemicolon" select="true()"/>
	<xsl:with-param name="justDeclarations" select="true()"/>
	<xsl:with-param name="isMethodLocal" select="true()"/>
	<xsl:with-param name="includeStatics" select="false()"/>
    </xsl:apply-templates>

    <!-- Create initializers for local declarations -->
    <xsl:variable name="localsInit">
	<LOCALSINIT>
	    <xsl:apply-templates select="*" mode="extractLocals">
		<xsl:with-param name="omitDef" select="true()"/>
		<xsl:with-param name="doNext" select="false()"/>
		<xsl:with-param name="addSemicolon" select="false()"/>
		<xsl:with-param name="justDeclarations" select="false()"/>
		<xsl:with-param name="includeAssignments" select="true()"/>
		<xsl:with-param name="includeDeclarations" select="false()"/>
		<xsl:with-param name="isMethodLocal" select="true()"/>
		<xsl:with-param name="justAssignment" select="true()"/>
		<xsl:with-param name="includeStatics" select="false()"/>
	    </xsl:apply-templates>
	</LOCALSINIT>
    </xsl:variable>

    <!-- Extract body elements {e;e} but without locals or {} delimiters -->
    <xsl:variable name="bodyNoLocals">
	<BODY>
	    <xsl:apply-templates select="*[not(self::STATEMENT[KEYWORD[string()='local' or string()='static']] or self::DELIMITER[@ID='{' or @ID='}'])]"/>
	</BODY>
    </xsl:variable>

    <!-- Create null or fail suffix -->
    <xsl:variable name="addFailorNull">
	<FAIL>
	    <xsl:choose>    <!-- non-empty block body will not end with ; -->
	    <xsl:when test="boolean(xalan:nodeset($bodyNoLocals)/BODY/*)">
		<!-- non-empty BODY -->
		<xsl:if test="boolean($appendFail)">
			<DELIMITER ID=";"/>
			<FRAGMENT>
				<xsl:value-of select="$newIconFail"/>
				<xsl:text>()</xsl:text>
			</FRAGMENT>
		</xsl:if>
	    </xsl:when>
	    <xsl:otherwise> <!-- no body, if localInits it ended in ; -->
		<xsl:choose>
		<xsl:when test="boolean($appendFail)">
			<FRAGMENT>
				<xsl:value-of select="$newIconFail"/>
				<xsl:text>()</xsl:text>
			</FRAGMENT>
		</xsl:when>
		<xsl:otherwise>  <!-- empty sequence must be null -->
			<FRAGMENT>
				<xsl:value-of select="$newIconNullIterator"/>
				<xsl:text>()</xsl:text>
			</FRAGMENT>
		</xsl:otherwise>
		</xsl:choose>
	    </xsl:otherwise>
	    </xsl:choose>
	</FAIL>
    </xsl:variable>

    <!--
    #====
    # Output: locals, prefix, IconSequence(localsInit, body, failOrNull)
    #	Locals will be temporaries first, then declared locals.
    #====
    -->
    <xsl:if test="boolean($prefix)">
	<xsl:copy-of select="$prefix/*"/>
    </xsl:if>

    <STATEMENT>		<!-- for format -->
    <xsl:if test="boolean($noindent)">
	<xsl:attribute name="noindent">true()</xsl:attribute>
    </xsl:if>

    <xsl:if test="boolean(xalan:nodeset($localsInit)/LOCALSINIT/*) or (count(xalan:nodeset($bodyNoLocals)/BODY/*) &gt; 1) or boolean(xalan:nodeset($addFailorNull)/FAIL/*)">
	<xsl:value-of select="$newIconSequence"/>
	<xsl:text>(</xsl:text>
    </xsl:if>

	<xsl:apply-templates select="xalan:nodeset($localsInit)/LOCALSINIT/*" mode="extractBlockToList"/>
	<xsl:apply-templates select="xalan:nodeset($bodyNoLocals)/BODY/*" mode="extractBlockToList"/>
	<xsl:apply-templates select="xalan:nodeset($addFailorNull)/FAIL/*" mode="extractBlockToList"/>

    <xsl:if test="boolean(xalan:nodeset($localsInit)/LOCALSINIT/*) or (count(xalan:nodeset($bodyNoLocals)/BODY/*) &gt; 1) or boolean(xalan:nodeset($addFailorNull)/FAIL/*)">
	<xsl:text>)</xsl:text>
    </xsl:if>

    <xsl:if test="boolean($suffix)">
	<xsl:copy-of select="$suffix/*"/>
    </xsl:if>

    <xsl:if test="boolean($endsWithSemicolon)">
	<DELIMITER ID=";"/>	<!-- for format: will appear in STATEMENT -->
    </xsl:if>
    </STATEMENT>

    </BLOCKSEQUENCE>
</xsl:template>

<!--
#====
# Extract block x;y into list x,y
#====
-->
<xsl:template match="DELIMITER[@ID=';']" mode="extractBlockToList" priority="2">
	<DELIMITER ID=","/>
</xsl:template>

<!-- Process each local x=,y= within each LOCALSINIT element -->
<xsl:template match="STATEMENT[LOCAL]" mode="extractBlockToList" priority="2">
	<xsl:apply-templates select="*" mode="extractBlockToList"/>
</xsl:template>

<!-- Append , after each LOCAL element -->
<xsl:template match="LOCAL" mode="extractBlockToList" priority="2">
	<xsl:copy-of select="."/>
	<DELIMITER ID=","/>
</xsl:template>

<xsl:template match="@*|node()" mode="extractBlockToList">
	<xsl:copy-of select="."/>
</xsl:template>

<!--
#=============================================================================
# Closure.
#	(x,y) -> {locals; body} => <>{x,y -> {locals; body}}
# TAGS closure: CLOSURE { TUPLE ARROW BLOCK }
#=============================================================================
-->
<xsl:template match="CLOSURE" priority="2">

    <!-- Look up to containing class to find classCachename -->
    <xsl:variable name="classCachename">
	<xsl:value-of select="ancestor::*[(self::STATEMENT or self::EXPRESSION) and @classCachename][1]/@classCachename"/>
    </xsl:variable>

    <!-- Initial clause is not allowed in a closure -->

    <xsl:copy> 
    <xsl:copy-of select="@*"/> 
      <xsl:value-of select="concat(' ', $newIconSingleton)"/>
      <xsl:text>(</xsl:text>
	<xsl:if test="boolean($isJava)">
		<xsl:text>(VariadicFunction)</xsl:text>
	</xsl:if>
	<xsl:apply-templates select="BLOCK" mode="createClosure">
		<xsl:with-param name="classCachename" select="$classCachename"/>
		<xsl:with-param name="closureUniquename" select="@closureUniquename"/>
		<xsl:with-param name="params" select="TUPLE"/>
	</xsl:apply-templates>
      <xsl:text>)</xsl:text>
    </xsl:copy> 
</xsl:template>

<!--
#=============================================================================
# Create Groovy closure for method or closure body.
#   (x,y) {locals; body} =>
#	{ x,y -> locals;  // includes normalization temporaries 
#		// Reuse method body
#		IconIterator body = cache.getFree(closureUniquename);
#		if (body != null) { return body.reset().unpackArgs(args) }
#
#		// Parameters		// Reified parameters
#		PREFIX to createSequenceFromBlock:
#			// Unpack arguments
#		        unpack = {args -> unpack parameters ; reset locals=null}
#
#			// Static method initializer
#			method initial expr => 
#			if (initialCache.get(methodName) == null) {
#			initialCache.computeIfAbsent(methodName, (Function)
#			new IconFunction(()->{[[expr.next()]]; return true;}));};
#
#		// Method body
#		createSequenceFromBlock (body;fail):
#			// Locals		
#			// Reified locals
#			// Temporaries
#			[ PREFIX goes here ]
#			rewrite(body;fail)
#		SUFFIX to createSequenceFromBlock:
#			setCache(classCachename, closureUniquename)
#			setUnpackClosure(unpack).unpackArgs(args);
#	}
# If asMethodNotClosure, just does:  (Object... args) { block }
# If canOmitMethodArgs, translates to:
# { Object... args -> def x; def y;	// Parameters as fields
#		      locals;		// With temporaries first
#     if (args == null) { args = IIconAtom.getEmptyArray(); };
#		// if ((args!=null) && (args.length > 0)) { }
#     Unpack params: for each param at position i, generate this code:
#       if param is varargs:
#	  varargs = (args.length > i) ?
#		Arrays.asList(args).subList(i,args.length) : new ArrayList();
#       otherwise if param has default:
#	  default = ((args.length > i) && (args[i] != OMIT)) ? args[i] : default;
#       otherwise: same as above, with null instead of default
#	  arg = (args.length > i) ? args[i] : null;
#   Body translated as above }
#=============================================================================
-->
<xsl:template match="BLOCK" mode="createClosure" priority="2">
    <xsl:param name="classCachename" select="cmd:TgetMinimalUnique('methodCache')"/>
    <xsl:param name="classInitialCachename" select="cmd:TgetMinimalUnique('initialMethodCache')"/>
    <xsl:param name="closureUniquename" select="cmd:TgetMinimalUnique('closure')"/>
    <xsl:param name="params"/>
    <xsl:param name="bodySuffix"/>	<!-- e.g., .next() if main() -->
    <xsl:param name="forceLastVarargs" select="false()"/>
	<!-- if isMain: keep main(args) as closure, but forceLastVarargs -->
    <xsl:param name="asVariadicFunction" select="false()"/>
    <xsl:param name="useMethodBodyCache" select="true()"/>
    <xsl:param name="asMethodNotClosure" select="false()"/>
	<!-- if asMethodNotClosure: (Object ... args) { block } -->
    <xsl:param name="appendFail" select="true()"/>
    <xsl:param name="isStatic" select="false()"/>
    <xsl:param name="useStaticInitializer" select="false()"/>
    <!-- 
    #====
    # For lambda's or create inside another method or lambda,
    # must uniquefy args,body,unpack,and params to avoid redeclare local.
    #====
    -->

    <!-- args: create unique name for varargs in closure -->
    <xsl:variable name="argsUniquename">
	<xsl:value-of select="cmd:TgetMinimalUnique('args')"/>
    </xsl:variable>

    <!-- body: create unique name for body in closure -->
    <xsl:variable name="bodyUniquename">
	<xsl:value-of select="cmd:TgetMinimalUnique('body')"/>
    </xsl:variable>

    <!-- unpack: create unique name for unpack in closure -->
    <xsl:variable name="unpackUniquename">
	<xsl:value-of select="cmd:TgetMinimalUnique('unpack')"/>
    </xsl:variable>

    <!-- unpackArgs: create unique name for unpack args in closure -->
    <xsl:variable name="unpackArgsUniquename">
	<xsl:value-of select="cmd:TgetMinimalUnique('params')"/>
    </xsl:variable>

    <!-- Unpack each parameter from args into corresponding parameter name --> 
    <xsl:variable name="unpackedArgs">
	    <ARGS>
		<xsl:apply-templates select="$params/*[not(self::DELIMITER)]" mode="unpackParameter">
			<xsl:with-param name="assignToParam" select="true()"/>
			<xsl:with-param name="argsUniquename" select="$unpackArgsUniquename"/>
			<xsl:with-param name="forceLastVarargs" select="$forceLastVarargs"/>
		</xsl:apply-templates>
	    </ARGS>
    </xsl:variable>

    <!--
    #====
    # Static method initializer : closure body
    #====
    -->
    <xsl:variable name="initialBody">
	<BODY>
	  <STATEMENT>		<!-- for format -->
		<DELIMITER ID="("/>
		<xsl:apply-templates select="STATEMENT[KEYWORD[string() = 'initial']]/EXPRESSION"/>
		<DELIMITER ID=")"/>
		<!-- Insert next() for initial body -->
		<xsl:text>.nextOrNull()</xsl:text>
		<xsl:text>; return true</xsl:text>
		<DELIMITER ID=";"/>
	  </STATEMENT>
	</BODY>
    </xsl:variable>
    <xsl:variable name="initialParams">
	<PARAMS>
	  <ARG>
	  	<xsl:text>arg</xsl:text>
	  </ARG>
	</PARAMS>
    </xsl:variable>

    <!--
    #====
    # Closure body
    #====
    -->
    <xsl:variable name="unpackBody">
	    <BODY>
	      <STATEMENT>
		<xsl:text>if (</xsl:text>
		<xsl:value-of select="$unpackArgsUniquename"/>
		<xsl:text> ==  null) { </xsl:text>
		<xsl:value-of select="$unpackArgsUniquename"/>
		<xsl:text> = IIconAtom.getEmptyArray(); }</xsl:text>

	        <DELIMITER ID=";"/>
	      </STATEMENT>

	      <xsl:apply-templates select="xalan:nodeset($unpackedArgs)/ARGS/*" mode="extractToStatement"/>

	      <!-- Reset non-initializer locals to null -->
	      <xsl:apply-templates select="*" mode="extractLocals">
		<xsl:with-param name="omitDef" select="true()"/>
		<xsl:with-param name="doNext" select="false()"/>
		<xsl:with-param name="addSemicolon" select="true()"/>
		<xsl:with-param name="justResetToNull" select="true()"/>
		<xsl:with-param name="includeAssignments" select="false()"/>
		<xsl:with-param name="includeDeclarations" select="true()"/>
		<xsl:with-param name="isMethodLocal" select="true()"/>
		<xsl:with-param name="includeStatics" select="false()"/>
	      </xsl:apply-templates>

	      <STATEMENT>
		<xsl:text>return null</xsl:text>
		<DELIMITER ID=";"/>
              </STATEMENT>

	    </BODY>
    </xsl:variable>

    <!--
    #====
    # Prefix to createSequenceFromBlock, before Locals and Temporaries.
    #	unpack = {args -> unpack parameters ; reset locals=null}
    #====
    -->
    <xsl:variable name="prefix">
      <PREFIX>
	<!--
	#====
	# Unpack parameters
	#====
	-->

	<!--
	#====
	# <xsl:if test="boolean($canOmitMethodArgs) and boolean(xalan:nodeset($unpackedArgs)/ARGS/*)">
	#====
	-->
	  <!-- "if (args == null) { args = IIconAtom.getEmptyArray(); };" -->
	  <STATEMENT>
		<COMMENT>
		<xsl:text>// Unpack parameters</xsl:text>
		</COMMENT>
	  <NEWLINE/>
	  </STATEMENT>

	  <!-- "def unpack = { Object... args -> unpack args; locals=null}" -->
	  <STATEMENT>
		<xsl:value-of select="$defClosure"/>
		<xsl:value-of select="$unpackUniquename"/>
		<xsl:text> =</xsl:text>

	<!-- BEGIN formatClosure -->
	<xsl:apply-templates select="." mode="formatClosure">
	  <xsl:with-param name="asVariadicFunction" select="true()"/>
	  <xsl:with-param name="argsUniquename" select="$unpackArgsUniquename"/>
	  <xsl:with-param name="body" select="xalan:nodeset($unpackBody)/BODY"/>
	</xsl:apply-templates>
	<!-- END formatClosure -->

	  <DELIMITER ID=";" nodelete="true()"/>

	  </STATEMENT>	<!-- END of unpack parameters -->

	<!-- "return cache.getFree(closure) ?: " -->

	<!--
	#====
	# Static initializer for non-static method initial clause:
	#	"initial" is not allowed in just closure
	#	So not need to create initialCache.
	# initial expr => if (initialCache.get(methodName) == null) {
	#	initialCache.computeIfAbsent(methodName,
	#	(java.util.function.Function)
	#	new IconFunction({ -> [[expr.next()]]; return true;} )); };
	# ConcurrentHashMap<String,Object> initialCache=new ConcurrentHashMap();
	#====
	-->
	<xsl:if test="not(boolean($isStatic) and boolean($useStaticInitializer))">
	<xsl:if test="STATEMENT[KEYWORD[string() = 'initial']]">
	    <STATEMENT>
		<COMMENT>
			<xsl:text>// Initialize method on first use</xsl:text>
		</COMMENT>
		<NEWLINE/>
	    </STATEMENT>

	    <STATEMENT>
		<xsl:text>if (</xsl:text>
		<xsl:value-of select="$classInitialCachename"/>
		<xsl:text>.get("</xsl:text>
		<xsl:value-of select="$closureUniquename"/>
		<xsl:text>") == null)</xsl:text>
		<BLOCK>
		  <DELIMITER ID="{"/>
		  <STATEMENT indent="true()">
		    <xsl:value-of select="$classInitialCachename"/>
		    <xsl:text>.computeIfAbsent("</xsl:text>
		    <xsl:value-of select="$closureUniquename"/>
		    <xsl:text>", (java.util.function.Function) </xsl:text>

		    <!-- BEGIN formatClosure -->
		    <xsl:apply-templates select="." mode="formatClosure">
			<xsl:with-param name="asFunction" select="true()"/>
			<xsl:with-param name="params" select="xalan:nodeset($initialParams)/PARAMS"/>
			<xsl:with-param name="body" select="xalan:nodeset($initialBody)/BODY"/>
		    </xsl:apply-templates>
		    <!-- END formatClosure -->

		    <xsl:text>);</xsl:text>
		    <NEWLINE/>
		  </STATEMENT>
		  <DELIMITER ID="}"/>
		</BLOCK>
		<DELIMITER ID=";"/>
	    </STATEMENT>
	</xsl:if>
	</xsl:if>

	<!--
	#====
	# Method body
	#====
	-->
	  <STATEMENT>
		<COMMENT>
		<xsl:text>// Method body</xsl:text>
		</COMMENT>
	  <NEWLINE/>
	  </STATEMENT>

	<STATEMENT indent="true()">		<!-- for format -->
		<!-- Must declare body if no cache: IconIterator body; -->
		<xsl:if test="not(boolean($useMethodBodyCache))">
			<xsl:text>IconIterator </xsl:text> <!-- IIcon -->
		</xsl:if>
		<xsl:value-of select="$bodyUniquename"/>
		<xsl:text> = </xsl:text>
	</STATEMENT>
      </PREFIX>
    </xsl:variable>

    <!--
    #====
    # Suffix to createSequenceFromBlock.
    #   e.g., .next() if top-level outermost.
    #====
    -->
    <xsl:variable name="suffix">
      <SUFFIX>
       <FRAGMENT>
	<!-- Any additions to body, before its semicolon, go here -->
	<xsl:if test="boolean($bodySuffix)">
		<xsl:copy-of select="$bodySuffix"/>
	</xsl:if>
       </FRAGMENT>
       <!-- for format: will appear in STATEMENT 
       <DELIMITER ID=";"/>	
       -->
      </SUFFIX>
    </xsl:variable>

    <!--
    #====
    # Main closure body
    #====
    -->
    <xsl:variable name="closureBody">
      <BODY>

      <xsl:if test="boolean($useMethodBodyCache)">
	<!--
	#====
	# Reuse method body
	#
	# If not in class or procedure, i.e. top-level interpretive,
	#	no methodCache has been defined.
	#====
	-->
	<STATEMENT>
		<COMMENT>
			<xsl:text>// Reuse method body</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	</STATEMENT>

	<xsl:if test="not(ancestor::STATEMENT[KEYWORD[string()='class' or string()='procedure']])">
	    <STATEMENT>		<!-- for format -->
		<xsl:text>MethodBodyCache </xsl:text>
		<xsl:value-of select="$classCachename"/>
		<xsl:text> = new MethodBodyCache();</xsl:text>
	      <NEWLINE/>	<!-- for format -->
	    </STATEMENT>
	</xsl:if>

	<!-- "IconIterator body = methodCache.getFree(name);" -->
	<STATEMENT>		<!-- for format -->
		<xsl:text>IconIterator </xsl:text> <!-- IIcon -->
			<!-- FASTER under Groovy if no interface IIcon -->
		<xsl:value-of select="$bodyUniquename"/>
		<xsl:text> = </xsl:text>
		<xsl:value-of select="$classCachename"/>
		<xsl:text>.getFree("</xsl:text>
		<xsl:value-of select="$closureUniquename"/>
		<xsl:text>");</xsl:text>
	    <NEWLINE/>		<!-- for format -->
	</STATEMENT>

	<!-- "if (body != null) { return body.reset().unpackArgs(args); };" -->
	<STATEMENT>		<!-- for format -->
		<xsl:text>if (</xsl:text>
		<xsl:value-of select="$bodyUniquename"/>
		<xsl:text> != null) { return </xsl:text>
		<xsl:value-of select="$bodyUniquename"/>
		<xsl:text>.reset().unpackArgs(</xsl:text>
		<xsl:value-of select="$argsUniquename"/>
		<xsl:text>); };</xsl:text>
	    <NEWLINE/>		<!-- for format -->
	</STATEMENT>

      </xsl:if>	<!-- useMethodBodyCache -->

	<!--
	#====
	# Parameter declarations
	#====
	-->
	  <!-- Create fields for parameters -->
	  <xsl:if test="boolean($params/*[not(self::DELIMITER)])">
	    <STATEMENT>
		<COMMENT>
		<xsl:text>// Reified parameters</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	    </STATEMENT>
	    <xsl:apply-templates select="$params/*[not(self::DELIMITER)]" mode="createField">
		<xsl:with-param name="isMethodLocal" select="true()"/>
	    </xsl:apply-templates>
	  </xsl:if>

	<!--
	#===
	# Create iterator sequence from block
	#===
	-->
	<xsl:apply-templates select="." mode="createSequenceFromBlock">
		<xsl:with-param name="appendFail" select="$appendFail"/>
		<xsl:with-param name="prefix" select="xalan:nodeset($prefix)/PREFIX"/>
		<xsl:with-param name="suffix" select="xalan:nodeset($suffix)/SUFFIX"/>
		<xsl:with-param name="noindent" select="true()"/>
	</xsl:apply-templates>

	  <STATEMENT>
		<COMMENT>
		<xsl:text>// Return body after unpacking arguments </xsl:text>
		</COMMENT>
	  <NEWLINE/>
	  </STATEMENT>

	<!--
	#====
	# body.setCache(c,n);
	# body.setUnpackClosure(cl).unpackArgs(args);
	# return body;
	#====
	-->
	<xsl:if test="boolean($useMethodBodyCache)">
	<STATEMENT>
		<xsl:value-of select="$bodyUniquename"/>
		<xsl:text>.setCache(</xsl:text>
		<xsl:value-of select="$classCachename"/>
		<DELIMITER ID=","/>
		<xsl:text>"</xsl:text>
		<xsl:value-of select="$closureUniquename"/>
		<xsl:text>")</xsl:text>
	    <DELIMITER ID=";"/>
	</STATEMENT>
	</xsl:if>	<!-- useMethodBodyCache -->

	<STATEMENT>
		<xsl:value-of select="$bodyUniquename"/>
		<xsl:text>.setUnpackClosure(</xsl:text>
		<xsl:value-of select="$unpackUniquename"/>
		<xsl:text>)</xsl:text>
		<xsl:text>.unpackArgs(</xsl:text>
		<xsl:value-of select="$argsUniquename"/>
		<xsl:text>)</xsl:text>
	    <DELIMITER ID=";"/>
	</STATEMENT>

	<STATEMENT>
		<xsl:text>return </xsl:text>
		<xsl:value-of select="$bodyUniquename"/>
	    <DELIMITER ID=";"/>
	</STATEMENT>

      </BODY>
    </xsl:variable>

    <!--
    #====
    # Create closure using prefix and suffix.
    #====
    -->
    <CLOSUREBLOCK>	<!-- fake BLOCK -->
    <xsl:copy-of select="@*"/>

	<!-- BEGIN formatClosure -->
	<xsl:apply-templates select="." mode="formatClosure">
	  <xsl:with-param name="asVariadicFunction" select="$asVariadicFunction"/>
	  <xsl:with-param name="asMethodNotClosure" select="$asMethodNotClosure"/>
	  <xsl:with-param name="argsUniquename" select="$argsUniquename"/>
	  <xsl:with-param name="body" select="xalan:nodeset($closureBody)/BODY"/>
	</xsl:apply-templates>
	<!-- END formatClosure -->

    </CLOSUREBLOCK>	<!-- fake BLOCK -->
</xsl:template>

<!--
#====
# Unpack parameter arg.  Acutally uses "x.set()" instead of "x=" below.
#  arg = (args.length > i) ? args[i] : null;
#  default = ((args.length > i) && (args[i] != OMIT)) ? args[i] : default;
#  varargs = (args.length > i) ?
#	Arrays.asList(args).subList(i,args.length) : new ArrayList();
# TAGS parameter: TUPLE/< QUALIFIED/< DECLARATION/IDENTIFIER
#			[:Type] [: or = TYPE<LITERAL>] > [SUBSCRIPT] >
# Parameter: assignToParam : generate "x=" before unpack statement.
#====
-->
<xsl:template match="QUALIFIED" mode="unpackParameter" priority="2">
    <xsl:param name="assignToParam" select="false()"/>
    <xsl:param name="argsUniquename"/>
    <xsl:param name="forceLastVarargs" select="false()"/>

    <xsl:variable name="pos" select="position()-1"/>

    <ARG>
	<xsl:if test="boolean($assignToParam)">
		<!--
		<xsl:copy-of select="DECLARATION/IDENTIFIER"/>
		-->
		<xsl:apply-templates select="DECLARATION" mode="deriveReifiedVarName">
			<xsl:with-param name="doReified" select="true()"/>
		</xsl:apply-templates>
		<xsl:text>.set(</xsl:text>
	</xsl:if>
		
	<xsl:choose>
	<!--
	#====
	# param is o[] varags: (args.length > i) ?
	#	Arrays.asList(args).subList(i,args.length) : new ArrayList();
	#====
	-->
	<xsl:when test="@isVarargs or (boolean($forceLastVarargs) and @isLastParameter)">
	  <xsl:text>(</xsl:text>
	  	<xsl:value-of select="$argsUniquename"/>
	  <xsl:text>.length &gt; </xsl:text>
	  	<xsl:copy-of select="$pos"/>
	  <xsl:text>) ? Arrays.asList(</xsl:text>
	  	<xsl:value-of select="$argsUniquename"/>
	  <xsl:text>).subList(</xsl:text>
	  	<xsl:copy-of select="$pos"/>
	  <xsl:text>, </xsl:text>
	  	<xsl:value-of select="$argsUniquename"/>
	  <xsl:text>.length) : new ArrayList()</xsl:text>
	</xsl:when>

	<!--
	#====
	# param is x=default:
	#	((args.length > i) && (args[i] != OMIT)) ? args[i] : default;
	#====
	-->
	<xsl:when test="TYPE[LITERAL]">
	  <xsl:text>((</xsl:text>
	  	<xsl:value-of select="$argsUniquename"/>
	  <xsl:text>.length &gt; </xsl:text>
	  	<xsl:copy-of select="$pos"/>
	  <xsl:text>) &amp;&amp; (</xsl:text>
	  	<xsl:value-of select="$argsUniquename"/>
	  <xsl:text>[</xsl:text>
	  	<xsl:copy-of select="$pos"/>
	  <xsl:text>] != IconTypes.OMIT)) ? </xsl:text>
	  	<xsl:value-of select="$argsUniquename"/>
	  <xsl:text>[</xsl:text>
	  	<xsl:copy-of select="$pos"/>
	  <xsl:text>] : </xsl:text>
		<xsl:copy-of select="TYPE[LITERAL]"/>
	</xsl:when>

	<!--
	#====
	# (args.length > i) ? args[i] : null;
	#====
	-->
	<xsl:otherwise>
	  <xsl:text>(</xsl:text>
	  	<xsl:value-of select="$argsUniquename"/>
	  <xsl:text>.length &gt; </xsl:text>
	  	<xsl:copy-of select="$pos"/>
	  <xsl:text>) ? </xsl:text>
	  	<xsl:value-of select="$argsUniquename"/>
	  <xsl:text>[</xsl:text>
	  	<xsl:copy-of select="$pos"/>
	  <xsl:text>] : null</xsl:text>
	</xsl:otherwise>
	</xsl:choose>

	<xsl:if test="boolean($assignToParam)">
		<xsl:text>)</xsl:text>
	</xsl:if>
    </ARG>
</xsl:template>

<!--
#=============================================================================
# Classes, methods, and procedures.
#=============================================================================
-->

<!--
#=============================================================================
# Class.
#   Add extends clause.
#   Add cache local.
#   Define constructor fields.
#   Extract locals.
#   Add constructors: normal, no-arg, and static varargs.
#   Translate methods, i.e., body excluding locals.
#   Translate initially.
# Adds "import static C.C" if interpretive and not nested in class.
# Class layout:
#	Method body cache
#	Static method initializer cache
#	Method references
#	Class parameters as fields
#	Constructors
#	Static variadic constructor // must go before locals in case they use it
#	Embedded scripts
#	Locals (fields) and statics
#	Method statics
#	Methods
#	Initially
#
#	Method {
#		Parameters as fields
#		Temporaries
#		Locals
#		Unpack parameters
#		Body }
#
# To avoid method collision, in class generation:
# 1. Uniqueify actual method, if main, or has same name as class and is Groovy.
#	To avoid collision with constructor or static void main(args).
# 2. Skip static constructor field, if has same name as class.
#	Methodref will override static constructor field.
# 3. Skip static void main(args) for main method,
#	if class named main and is Groovy.
# Groovy summary: Method with classname, in any form, is treated as constructor.
#	But can have field with same name as method or constructor.
#	Method takes priority over field with method ref.
#
# TAGS class: STATEMENT/< KEYWORD[class] DECLARATION/ID[classname]
#		QUALIFIED[extends] TUPLE (Param, Param) BLOCK { body } >
# TAGS method: STATEMENT/< KEYWORD[method] DECLARATION[methodname] params body >
# 	TAGS parameters: TUPLE/QUALIFIED/DECLARATION/IDENTIFIER
# 	TAGS body:       BLOCK
# Tags closure: GROUP/CLOSURE/< TUPLE[params] BLOCK[@isClosure]/Exprlist >
# Tags block:   GROUP/BLOCK/Exprlist
# Tags procedure: same as method
# Tags modifiers: @isRecord | @modifiers | param:modifiers
#=============================================================================
-->
<xsl:template match="STATEMENT[KEYWORD[string()='class']]" priority="2">
    <xsl:param name="modifiers"/>	<!-- Default is public -->

    <xsl:variable name="classname">
	<xsl:value-of select="DECLARATION[IDENTIFIER]"/>
    </xsl:variable>

    <xsl:variable name="classCachename">
	<xsl:value-of select="ancestor-or-self::*[(self::STATEMENT or self::EXPRESSION) and @classCachename][1]/@classCachename"/>
    </xsl:variable>

    <xsl:variable name="classStaticCachename">
	<xsl:value-of select="ancestor-or-self::*[(self::STATEMENT or self::EXPRESSION) and @classStaticCachename][1]/@classStaticCachename"/>
    </xsl:variable>

    <xsl:variable name="classInitialCachename">
	<xsl:value-of select="ancestor-or-self::*[(self::STATEMENT or self::EXPRESSION) and @classInitialCachename][1]/@classInitialCachename"/>
    </xsl:variable>

    <!-- args: create unique name for varargs in closure -->
    <xsl:variable name="argsUniquename">
	<xsl:value-of select="cmd:TgetReusableUnique('args')"/>
    </xsl:variable>

    <xsl:variable name="methodModifierRTF">
	<xsl:choose>
	  <xsl:when test="boolean($isJava)">
		<xsl:text>public</xsl:text>
	  </xsl:when>
	  <xsl:otherwise>
		<xsl:text>public</xsl:text>
	  </xsl:otherwise>
	</xsl:choose>
    </xsl:variable>
    <xsl:variable name="methodModifier">
	<xsl:value-of select="$methodModifierRTF"/>
    </xsl:variable>

    <!-- Test if there is only one static method, and it has initial clause -->
    <!--
    <xsl:variable name="staticMethods" select="BLOCK/STATEMENT[KEYWORD[(string()='method') or (string()='initially')] and KEYWORD[string()='static']]"/>
    <xsl:variable name="useStaticInitializer" select="boolean((count($staticMethods) = 1) and $staticMethods/BLOCK/STATEMENT[KEYWORD[string()='initial']])"/>
    -->
    <!-- Test if only one procedure method, so can use static initializer -->
    <xsl:variable name="useStaticInitializer" select="boolean(count(BLOCK/STATEMENT[KEYWORD[(string()='method') or (string()='initially')]]) = 1) and boolean(@isProcedure)"/>

  <xsl:variable name="hasFilename" select="boolean(cmd:ThasProperty('Properties', 'filename'))"/>
  <!-- Get filename, deleting any .suffix -->
  <xsl:variable name="filename" select="substring-before(cmd:TgetProperty('Properties', 'filename'), '.')"/>

    <xsl:copy>
    <xsl:copy-of select="@*"/>

	<!--
	#=====================================================================
	# // Class name [extends]
	#	Not public if is Java, and
	#	if filename is defined then classname not equal filename, or
	#	if filename undefined then derived from record or not last class
	#=====================================================================
	-->

	<!-- Create modifiers: modifier, empty, or public as default -->
	<xsl:choose>
	  <xsl:when test="boolean($modifiers)">
		<xsl:value-of select="$modifiers"/>
		<xsl:text> </xsl:text>
	  </xsl:when>
	  <xsl:when test="boolean($isJava) and ((boolean($filename) and not($filename=$classname)) or (not(boolean($filename)) and (@isRecord or self::*[following-sibling::*[self::STATEMENT[KEYWORD[string()='class']]]])))">
	  </xsl:when>
	  <xsl:otherwise>
		<xsl:value-of select="'public '"/>
	  </xsl:otherwise>
	</xsl:choose>

	<!-- Copy class name -->
	<xsl:text>class </xsl:text>
	<xsl:value-of select="$classname"/>

	<!-- Add extends clause: class x:y => x extends y -->
	<xsl:if test="QUALIFIED[TYPE]">
		<xsl:text> extends </xsl:text>
		<xsl:apply-templates select="QUALIFIED[TYPE]" mode="formatClassExtends"/>
	</xsl:if>

	<!-- FIXME TBD TODO: Add @mixin for multiple inheritance -->

	<BLOCK>
	<xsl:copy-of select="BLOCK/@*"/>	<!-- preserve attributes -->
	<DELIMITER ID="{"/>

	<!--
	#=====================================================================
	# // Method body cache
	#=====================================================================
	-->
	    <STATEMENT>
		<COMMENT>
		<xsl:text>// Method body cache</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	    </STATEMENT>

	<!-- Add unique cache declaration: def cache=new MethodBodyCache(); -->
	<STATEMENT>
	<xsl:text>private MethodBodyCache </xsl:text>
	<xsl:value-of select="$classCachename"/>
	<xsl:text> = new MethodBodyCache()</xsl:text>
	<DELIMITER ID=";"/>
	</STATEMENT>

	<!-- Add static cache declaration if there are static methods -->
	<xsl:if test="BLOCK/STATEMENT[KEYWORD[(string()='method') or (string()='initially')] and KEYWORD[string()='static']]">
	    <STATEMENT>
	    <xsl:text>private static MethodBodyCache </xsl:text>
	    <xsl:value-of select="$classStaticCachename"/>
	    <xsl:text> = new MethodBodyCache()</xsl:text>
	    <DELIMITER ID=";"/>
	    </STATEMENT>
	</xsl:if>

	<!--
	#=====================================================================
	# // Static initializer cache, for non-static method initial clauses.
	# //	Instead of bundling into class static initializer block.
	# ConcurrentHashMap<String,Object> initialCache=new ConcurrentHashMap();
	#=====================================================================
	-->
	<xsl:if test="not(boolean($useStaticInitializer))">
	<xsl:if test="BLOCK/STATEMENT[KEYWORD[(string()='method') or (string()='initially')]]/BLOCK/STATEMENT[KEYWORD[string()='initial']]">
	    <STATEMENT>
		<COMMENT>
		<xsl:text>// Static method initializer cache</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	    </STATEMENT>

	    <STATEMENT>
		<xsl:text>private static ConcurrentHashMap&lt;String,Object&gt; </xsl:text>
		<xsl:value-of select="$classInitialCachename"/>
		<xsl:text> = new ConcurrentHashMap()</xsl:text>
		<DELIMITER ID=";"/>
	    </STATEMENT>
	</xsl:if>
	</xsl:if>

	<!--
	#=====================================================================
	# // Add method references for method names.
	#=====================================================================
	#	Required since lambdas do not allow forward references,
	#	so we create true methods with the same name
	#	(which do allow forward references inside them),
	#	and redirect method references to them.
	#=====================================================================
	-->
      <xsl:if test="boolean($asMethodNotClosure)">
	<xsl:if test="BLOCK/STATEMENT[KEYWORD[(string()='method') or (string()='initially')]]">
	    <STATEMENT>
		<COMMENT>
		<xsl:text>// Method references</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	    </STATEMENT>
	    <xsl:apply-templates select="BLOCK/STATEMENT[KEYWORD[(string()='method') or (string()='initially')]]" mode="createMethodReference">
		<xsl:with-param name="modifiers" select="'public'"/>
		<xsl:with-param name="useMethodUniquename" select="false()"/>
			<!-- For non-main methods -->
		<xsl:with-param name="useMethodUniquenameIfMain" select="true()"/>
		<xsl:with-param name="classname" select="$classname"/>
	    </xsl:apply-templates>
	</xsl:if>
      </xsl:if>

	<!--
	#=====================================================================
	# // Constructor fields
	#=====================================================================
	-->

	<!-- Create fields for constructor args -->
	<xsl:if test="boolean(TUPLE/*[not(self::DELIMITER)])">
	    <STATEMENT>
		<COMMENT>
		<xsl:text>// Constructor fields</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	    </STATEMENT>
	    <xsl:apply-templates select="TUPLE/*[not(self::DELIMITER)]" mode="createField">
		<xsl:with-param name="isMethodLocal" select="false()"/>
		<!-- Create annotation: @MField -->
		<xsl:with-param name="addFieldAnnotation" select="true()"/>
		<xsl:with-param name="isConstructorField" select="true()"/>
		<!-- <xsl:with-param name="addReified" select="true()"/> -->
	    </xsl:apply-templates>

	    <!-- Create reified variables, in createField -->
	      <STATEMENT>
		<COMMENT>
		<xsl:text>// Reified constructor fields</xsl:text>
		</COMMENT>
	      <NEWLINE/>
	      </STATEMENT>
	      <xsl:apply-templates select="TUPLE/*[not(self::DELIMITER)]" mode="createField">
		<xsl:with-param name="isMethodLocal" select="false()"/>
		<xsl:with-param name="declareReified" select="true()"/>
	      </xsl:apply-templates>

	</xsl:if>

	<!--
	#=====================================================================
	# // Constructors
	#=====================================================================
	-->

	<STATEMENT>
		<COMMENT>
		<xsl:text>// Constructors</xsl:text>
		</COMMENT>
	<NEWLINE/>
	</STATEMENT>

	<!-- 
	#====
	# Add no-arg constructor if class has parameters, with super:
	#	def C() { [super();] initially(); }
	# We always create a no-arg constructor, even though Java automatically
	#	creates a no-arg constructor if there are no other constructors.
	# Must have at least one semicolon in body: def C() { ; }
	#====
	-->
	<xsl:if test="TUPLE[QUALIFIED]">
	    <STATEMENT>
		<xsl:text>public </xsl:text>	<!-- create modifiers -->
		<xsl:value-of select="$classname"/>
		<xsl:text>()</xsl:text>
		<BLOCK>
		<DELIMITER ID="{" nonewline="true()"/>
		  <xsl:if test="QUALIFIED[TYPE]">
			<xsl:text>super(); </xsl:text>
		  </xsl:if>
		  <xsl:choose>
		  <xsl:when test="STATEMENT[KEYWORD[string()='initially']]">
			<xsl:text>initially();</xsl:text>
		  </xsl:when>
		  <xsl:otherwise>
	    		<!-- empty statement if no other statement generated -->
			<!-- <xsl:text>new IconNullIterator();</xsl:text> -->
			<xsl:if test="not(QUALIFIED[TYPE])">
				<xsl:text>;</xsl:text>
			</xsl:if>
		  </xsl:otherwise>
		  </xsl:choose>
		<DELIMITER ID="}" noindent="true()" nonewline="true()"/>
		</BLOCK>
	    <NEWLINE/>
	    </STATEMENT>
	</xsl:if>

	<!--
	#====
	# Create constructor using any class parameters, using initially:
	#	public C(x) { super(); this.x=x; initially(); }
	# If java: public C(Object x) ...
	#====
	-->
	<STATEMENT indent="true()">
	<xsl:text>public </xsl:text>	<!-- create modifiers -->
	<xsl:value-of select="$classname"/>
	<xsl:choose>
	  <xsl:when test="boolean($isJava)">
		<TUPLE>
		<xsl:copy-of select="TUPLE/@*"/>
		<DELIMITER ID="("/>
		<xsl:apply-templates select="TUPLE/*[not(self::DELIMITER)]" mode="extractToList">
			<xsl:with-param name="prefix" select="'Object '"/>
		</xsl:apply-templates>
		<DELIMITER ID=")"/>
		</TUPLE>
	  </xsl:when>
	  <xsl:otherwise>
		<xsl:copy-of select="TUPLE"/>
	  </xsl:otherwise>
	</xsl:choose>

	<BLOCK>
	<DELIMITER ID="{"/>
	<xsl:if test="QUALIFIED[TYPE]">
	    <STATEMENT>
		<xsl:text>super()</xsl:text>
	    <DELIMITER ID=";"/>
	    </STATEMENT>
	</xsl:if>
	<xsl:apply-templates select="TUPLE/*[not(self::DELIMITER)]" mode="assignClassField"/>
	<xsl:choose>
	<xsl:when test="STATEMENT[KEYWORD[string()='initially']]">
	    <STATEMENT>
		<xsl:text>initially()</xsl:text>
	    <DELIMITER ID=";"/>
	    </STATEMENT>
	</xsl:when>
	<xsl:otherwise>
	    <!-- empty statement if no other statements generated -->
	    <xsl:if test="not(QUALIFIED[TYPE]) and not(TUPLE/*[not(self::DELIMITER)])">
	      <STATEMENT>
		<!-- <xsl:text>new IconNullIterator()</xsl:text> -->
		<DELIMITER ID=";"/>
	      </STATEMENT>
	    </xsl:if>
	</xsl:otherwise>
	</xsl:choose>
	<DELIMITER ID="}"/>
	</BLOCK>
	</STATEMENT>

	<!--
	#====
	# Add static variadic constructor:
	#	static def C = { Object... args -> return new C(args[0],...) }
	# OK to not return an iterator, will be promoted on invoke over C(x).
	#====
	-->
    <!--
    #====
    # Avoid method collision: Skip static constructor field,
    #	if method has same name as class.
    #====
    -->
    <xsl:if test="not(BLOCK/STATEMENT[KEYWORD[string()='method'] and DECLARATION[IDENTIFIER[string()=$classname]]])">
	<STATEMENT>
		<COMMENT>
		<xsl:text>// Static variadic constructor</xsl:text>
		</COMMENT>
	<NEWLINE/>
	</STATEMENT>

	<xsl:variable name="unpackedArgs">
	    <ARGS>
		<xsl:apply-templates select="TUPLE/*[not(self::DELIMITER)]" mode="unpackParameter">
			<xsl:with-param name="argsUniquename" select="$argsUniquename"/>
		</xsl:apply-templates>
	    </ARGS>
	</xsl:variable>

    <xsl:variable name="closureBody">
	  <BODY>

	  <xsl:if test="boolean(xalan:nodeset($unpackedArgs)/ARGS/*)">
	    <!-- "if (args == null) { args = IIconAtom.getEmptyArray(); };" -->
	    <STATEMENT>
		<xsl:text>if (</xsl:text>
		<xsl:value-of select="$argsUniquename"/>
		<xsl:text> ==  null) { </xsl:text>
		<xsl:value-of select="$argsUniquename"/>
		<xsl:text> = IIconAtom.getEmptyArray(); }</xsl:text>
	    <DELIMITER ID=";"/>
	    </STATEMENT>
	  </xsl:if>

	  <STATEMENT>
		<xsl:text>return new </xsl:text>
	  		<xsl:value-of select="$classname"/>
			<xsl:text>(</xsl:text>
		<xsl:apply-templates select="xalan:nodeset($unpackedArgs)/ARGS/*" mode="extractToList"/>
		<xsl:text>)</xsl:text>
	  <DELIMITER ID=";"/>
	  </STATEMENT>

	  </BODY>
    </xsl:variable>

	<!-- No asMethodNotClosure here. -->
	<STATEMENT>
	  <xsl:text>public static </xsl:text>  <!-- create modifiers -->
	  <xsl:value-of select="$defClosure"/>
	  <xsl:value-of select="$classname"/>
	  <xsl:text> =</xsl:text>

	<!-- BEGIN formatClosure -->
	<xsl:apply-templates select="." mode="formatClosure">
	  <xsl:with-param name="argsUniquename" select="$argsUniquename"/>
	  <xsl:with-param name="body" select="xalan:nodeset($closureBody)/BODY"/>
	</xsl:apply-templates>
	<!-- END formatClosure -->

	<DELIMITER ID=";" nodelete="true()"/>

	</STATEMENT>	<!-- END of no-arg constructor -->
    </xsl:if>

	<!--
	#=====================================================================
	# // Global redirection fields
	# Create global redirection fields for all globals above or inside
	# this class, if not in conflict with defined class fields.
	# There cannot be a conflict if there is a descendant reference
	# to the global.
	#	global x => Object x_r := new IconGlobal("x");
	#=====================================================================
	-->
	<!-- Create fields for all referenced globals -->
	<xsl:if test="/*/STATEMENT[KEYWORD[string()='global']] or BLOCK/STATEMENT[KEYWORD[string()='global']]">
	    <STATEMENT>
		<COMMENT>
		<xsl:text>// Global redirection fields</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	    </STATEMENT>
	    <xsl:apply-templates select="/*/STATEMENT[KEYWORD[string()='global']]/ENUM/DECLARATION[@isGlobalVariable]" mode="createGlobal">
		<xsl:with-param name="class" select="."/>
	    </xsl:apply-templates>
	    <xsl:apply-templates select="BLOCK/STATEMENT[KEYWORD[string()='global']]/ENUM/DECLARATION[@isGlobalVariable]" mode="createGlobal">
		<xsl:with-param name="class" select="."/>
	    </xsl:apply-templates>
	</xsl:if>

	<!--
	#=====================================================================
	# // Embedded scripts
	#=====================================================================
	-->
	<xsl:if test="BLOCK/EXPRESSION[EXPRESSION[ANNOTATION[QUALIFIED[DOTNAME[IDENTIFIER[string() = 'script']]]] and EXPRESSION[ATOM[LITERAL[@isBigLiteral]]]]]">
	    <STATEMENT>
		<COMMENT>
			<xsl:text>// Embedded scripts</xsl:text>
		</COMMENT>
	        <NEWLINE/>
	    </STATEMENT>
	    <STATEMENT indent="true">
		<xsl:apply-templates select="BLOCK/EXPRESSION[EXPRESSION[ANNOTATION[QUALIFIED[DOTNAME[IDENTIFIER[string() = 'script']]]] and EXPRESSION[ATOM[LITERAL[@isBigLiteral]]]]]" mode="insertEmbeddedScript"/>
	    </STATEMENT>
	</xsl:if>

	<!--
	#=====================================================================
	# // Locals (fields)
	# // Reified locals
	# // Static fields
	#=====================================================================
	-->

	<!-- Create local variables and their reified, in extractLocals -->
	  <xsl:apply-templates select="BLOCK/*" mode="extractLocals">
		<xsl:with-param name="omitDef" select="false()"/>
		<xsl:with-param name="doNext" select="true()"/>
		<xsl:with-param name="addSemicolon" select="true()"/>
		<xsl:with-param name="includeStatics" select="true()"/>
		<xsl:with-param name="addReified" select="true()"/>
		<xsl:with-param name="addFieldAnnotation" select="true()"/>
	  </xsl:apply-templates>

	<!--
	#=====================================================================
	# // Method statics
	# // Reified method statics
	#	Method statics can occur anywhere in the class, including
	#	in closures used in class variable initializers.
	#=====================================================================
	-->
	<xsl:apply-templates select="." mode="extractMethodStatics"/>

	<!--
	#=====================================================================
	# // Class static initializer block, for static methods with initial.
	# Optimization instead of initializer cache, which would also work.
	#====
	# Class static initializer, formed from initial clause.
	# If initial clause, add class static initializer block.
	#	initial expr; => static { [[expr]].next(); }
	#=====================================================================
	-->
	<xsl:if test="boolean($useStaticInitializer)">
	<xsl:if test="BLOCK/STATEMENT[KEYWORD[(string()='method') or (string()='initially')]]/BLOCK/STATEMENT[KEYWORD[string()='initial']]">
	    <STATEMENT>
		<COMMENT>
			<xsl:text>// Initialize static methods on first use</xsl:text>
		</COMMENT>
	        <NEWLINE/>
	    </STATEMENT>

	    <STATEMENT>
		<xsl:text>static </xsl:text>
		<BLOCK>
		  <DELIMITER ID="{"/>
		      <xsl:apply-templates select="BLOCK/STATEMENT[KEYWORD[(string()='method') or (string()='initially')]]/BLOCK/STATEMENT[KEYWORD[string()='initial']]/EXPRESSION" mode="createStaticInitialClause"/>
		  <DELIMITER ID="}"/>
		</BLOCK>
		<DELIMITER ID=";"/>
	    </STATEMENT>
	</xsl:if>
	</xsl:if>

	<!--
	#=====================================================================
	# // Methods
	#=====================================================================
	-->
	<xsl:if test="BLOCK/STATEMENT[KEYWORD[(string()='method') or (string()='initially')]]">
	    <STATEMENT>
		<COMMENT>
		<xsl:text>// Methods</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	    </STATEMENT>
	</xsl:if>

	<!--
	#====
	# Translate methods, i.e., body excluding locals.
	# Translate initially.
	#====
	# Class: OK to not use uniquename, except for main.
	#	 public method, except for main.
	#	 If main(), create static main.
	# Need to expose methods as method references to pass as first class
	#   citizens, and if isJava for external access as variadic functions.
	# Need methods (vs. methodref) for access by non-Junicon Java.
	#====
	-->
	<xsl:apply-templates select="BLOCK/STATEMENT[KEYWORD[(string()='method') or (string()='initially')]]" mode="createMethod">
		<xsl:with-param name="classCachename" select="$classCachename"/>
		<xsl:with-param name="classStaticCachename" select="$classStaticCachename"/>
		<xsl:with-param name="classInitialCachename" select="$classInitialCachename"/>
		<xsl:with-param name="asMethodNotClosure" select="$asMethodNotClosure"/>

			<!-- For non-main methods -->
		<xsl:with-param name="modifiers" select="$methodModifier"/>
		<xsl:with-param name="useMethodUniquename" select="false()"/>

			<!-- For main method -->
		<xsl:with-param name="mainModifiers" select="'private'"/>
		<xsl:with-param name="useMethodUniquenameIfMain" select="$asMethodNotClosure"/>
		<xsl:with-param name="classname" select="$classname"/>
		<xsl:with-param name="useStaticInitializer" select="$useStaticInitializer"/>
	</xsl:apply-templates>

	<!--
	#====
	# Define methods as methods, not closures, so can invoke them from Java.
	# 	We also expose methods as method references, to allow
	#	their use as first class citizens.
	# For main and Groovy procedures, must uniqueify the method name,
	#	to avoid conflict with static main method
	#	or procedure constructor.
	# THEN: in Java, don't need to use VariadicFunction.apply for:
	# 			x::f(args)
	#	but still do for: f(args), typed x.f(args)
	#	since these might be fields set to lambdas.
	# NOTE: Groovy does not allow any methods with the same name
	#	(nor static methods, nor constructors), but does allow closure
	#	fields and methods of the same name,
	#	with priority given to method in invocation.
	#====
	-->

	<!--
	#====
	# Create "static main(args)" if there is a main or static main method.
	#	def m=main;			// if exists static main
	#   OR: def c=new C(); def m=c.main;	// if exists local main
	#	m(args).next();
	#====
	# Avoid method collision: Skip static void main(args) for main method,
	#	if class named main and is Groovy.
	#       Instead main() is a constructor.
	#====
	-->
	<xsl:if test="($classname != 'main') or boolean($isJava)">
	  <xsl:if test="BLOCK/STATEMENT[KEYWORD[string()='method'] and DECLARATION[IDENTIFIER[string()='main']]]">
	    <xsl:apply-templates select="." mode="createMainMethod">
		<xsl:with-param name="classname" select="$classname"/>
		<xsl:with-param name="isStatic" select="BLOCK/STATEMENT[KEYWORD[string()='method'] and KEYWORD[string()='static'] and DECLARATION[IDENTIFIER[string()='main']]]"/>
	    </xsl:apply-templates>
	  </xsl:if>
	</xsl:if>

	<!--
	#====
	# End the class.
	#====
	-->
	<DELIMITER ID="}"/>
	</BLOCK>

    <!-- if interpretive mode, add: import static C.C -->
    <xsl:if test="not(boolean($isJava)) and not(ancestor::BLOCK)">
    			<!-- boolean($isInterpretive) -->
      <STATEMENT>
	<xsl:text>import static </xsl:text>
	<xsl:value-of select="$classname"/>
	<xsl:text>.</xsl:text>
	<xsl:value-of select="$classname"/>
      <DELIMITER ID=";"/>
      </STATEMENT>
    </xsl:if>

    </xsl:copy>
</xsl:template>

<!--
#====
# Create statement for static method initial clause.
#====
-->
<xsl:template match="*" mode="createStaticInitialClause" priority="2">
    <STATEMENT>		<!-- for format -->
	<DELIMITER ID="("/>
	    <xsl:apply-templates select="."/>
	<DELIMITER ID=")"/>
	<!-- Insert next() for static initial clause -->
	<xsl:text>.nextOrNull()</xsl:text>
	<DELIMITER ID=";"/>
    </STATEMENT>
</xsl:template>

<!--
#====
# CreateMethodReference from method name.
#   If isJava and useInnerClasses:
#     public [static] Object methodname = new VariadicFunction() {
#       public Object apply(Object... args) { return methodUniquename(args); }};
#   If isJava:
#     public Object methodname = (VariadicFunction) this::methodUniquename;
#     public static Object methodname = (VariadicFunction)
#		(Object... args) -> methodUniquename(args)
#   If Groovy:
#     public def methodname = this.&methodUniquename;
#     public static def methodname = this.&methodUniquename;
# For static method with same name as class, or for main method,
#	the actual method has a unique name.
# If not(useMethodUniquename and isMain), methodUniquename = methodname.
#====
-->
<xsl:template match="STATEMENT" mode="createMethodReference" priority="2">
    <xsl:param name="modifiers" select="'public'"/>
    <xsl:param name="isMain" select="DECLARATION[IDENTIFIER[string()='main']]"/>
    <xsl:param name="isStatic" select="KEYWORD[string()='static']"/>
	<!-- For non-main methods -->
    <xsl:param name="useMethodUniquename" select="false()"/>
	<!-- For main method, will override useMethodUniquename -->
    <xsl:param name="useMethodUniquenameIfMain" select="false()"/>
    <xsl:param name="classname" select="''"/>

    <!-- args: create unique name for varargs in closure -->
    <xsl:variable name="argsUniquename">
	<xsl:value-of select="cmd:TgetReusableUnique('args')"/>
    </xsl:variable>

    <xsl:variable name="methodname">
	<xsl:value-of select="DECLARATION[IDENTIFIER] | KEYWORD[(string()='initially')]"/>
    </xsl:variable>

    <xsl:variable name="methodUniquenameRTF">
      <xsl:choose>
      <!--
      #====
      # Avoid method collision: Use unique name for the actual method,
      #	  if main, or has same name as class and is Groovy (ie, not Java).
      #====
      -->
      <xsl:when test="boolean($useMethodUniquename) or (boolean($useMethodUniquenameIfMain) and (boolean($isMain) or (not(boolean($isJava)) and DECLARATION[IDENTIFIER[string()=$classname]])))">
	<xsl:value-of select="@methodUniquename"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="$methodname"/>
      </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="methodUniquename">
	<xsl:value-of select="$methodUniquenameRTF"/>
    </xsl:variable>

    <!--
    #====
    # Create annotation: @MMethodRef(name, methodName)
    #====
    -->
    <xsl:if test="boolean($addAnnotations)">
      <STATEMENT>
	<xsl:text>@MMethodRef(name=</xsl:text>
	    <xsl:text>"</xsl:text>
	    <xsl:value-of select="$methodname"/>
	    <xsl:text>"</xsl:text>
	<xsl:text>, methodName=</xsl:text>
	    <xsl:text>"</xsl:text>
	    <xsl:value-of select="$methodUniquename"/>
	    <xsl:text>"</xsl:text>
	<xsl:text>)</xsl:text>
      <NEWLINE/>
      </STATEMENT>
    </xsl:if>

    <!--
    #====
    # Create method reference.
    #====
    -->
    <xsl:copy> 
    <xsl:copy-of select="@*"/>
	<xsl:if test="boolean($modifiers)">
		<xsl:value-of select="$modifiers"/>
		<xsl:text> </xsl:text>
	</xsl:if>
	<xsl:if test="boolean($isStatic)">
		<xsl:text>static </xsl:text>
	</xsl:if>

	<xsl:value-of select="$defObject"/>
	<xsl:value-of select="$methodname"/>

	<xsl:choose>
	<xsl:when test="boolean($isJava) and boolean($useInnerClasses)">
		<xsl:value-of select="cmd:Treplace(' = new VariadicFunction() { public Object apply(Object... $args) { return $method($args); } }','$args',$argsUniquename,'$method',$methodUniquename)"/>
	</xsl:when>
	<xsl:when test="boolean($isJava)">
	  <xsl:choose>
	  <xsl:when test="boolean($isStatic)">
		<xsl:value-of select="cmd:Treplace(' = (VariadicFunction) (Object... $args) -> $method($args)','$args',$argsUniquename,'$method',$methodUniquename)"/>
	  </xsl:when>
	  <xsl:otherwise>
		<xsl:text> = (VariadicFunction) this::</xsl:text>
		<xsl:value-of select="$methodUniquename"/>
	  </xsl:otherwise>
	  </xsl:choose>
	</xsl:when>
	<xsl:otherwise>
		<xsl:text> = this.&amp;</xsl:text>
		<xsl:value-of select="$methodUniquename"/>
	</xsl:otherwise>
	</xsl:choose>

	<DELIMITER ID=";" nodelete="true()"/>
    </xsl:copy> 
</xsl:template>

<!--
#====
# Generate static main method.
#====
-->
<xsl:template match="*" mode="createMainMethod" priority="2">
  <xsl:param name="classname" select="'main'"/>
  <xsl:param name="isStatic" select="false()"/>

  <!-- args: create unique name for varargs in method -->
  <xsl:variable name="argsUniquename">
	<xsl:value-of select="cmd:TgetReusableUnique('args')"/>
  </xsl:variable>

  <!-- args: create unique name for varargs in coexpression closure -->
  <xsl:variable name="coexprUniquename">
	<xsl:value-of select="cmd:TgetReusableUnique('coexpr')"/>
  </xsl:variable>

  <!--
  #====
  # Main co-expression to be activated.
  #   (Object... args')-> { return (IIconIterator) m.apply((Object[]) args); }
  #   if !java: (Object... args')-> { return m(args); },
  #====
  -->
  <xsl:variable name="closureBody">
	<BODY>
	  <STATEMENT>
		<xsl:text>return </xsl:text>
		<xsl:choose>
		<xsl:when test="boolean($isJava)">
		    <xsl:text>((IIconIterator) m.apply((Object[]) </xsl:text>
			<xsl:value-of select="$argsUniquename"/>
		    <xsl:text>))</xsl:text>
		</xsl:when>
		<xsl:otherwise>
		    <xsl:text>m(</xsl:text>
			<xsl:value-of select="$argsUniquename"/>
		    <xsl:text>)</xsl:text>
		</xsl:otherwise>
		</xsl:choose>
		<DELIMITER ID=";" nonewline="true()"/>
	  </STATEMENT>
	</BODY>
  </xsl:variable>

  <!--
  #====
  # Empty environment for main co-expression.
  #	()->{return IconList.createArray()};
  #====
  -->
  <xsl:variable name="envBody">
	<BODY>
	  <STATEMENT noindent="true()">
		<xsl:text>return IconList.createArray()</xsl:text>
		<DELIMITER ID=";" nonewline="true()"/>
	  </STATEMENT>
	</BODY>
  </xsl:variable>

  <!--
  #====
  # Static main method.
  #====
  -->
	    <STATEMENT>
		<COMMENT>
		<xsl:text>// Static main method</xsl:text>
		</COMMENT>
	    <NEWLINE/>
	    </STATEMENT>

	  <STATEMENT indent="true">
		<xsl:text>public static void main(String... </xsl:text>
			<xsl:value-of select="$argsUniquename"/>
		<xsl:text>)</xsl:text>
	  <BLOCK>
	  <DELIMITER ID="{"/>

	    <!--
	    #====
	    # isStatic:
	    #   if java: VariadicFunction m = (VariadicFunction) main;
	    #   else:    def m = main;
	    # else:
	    #   if java: Classname c = new Classname();
	    #		 VariadicFunction m = (VariadicFunction) c.main;
	    #   else:    Classname c = new Classname();
	    #		 def m = c.main;
	    #====
	    -->
	    <STATEMENT>
	    <xsl:choose>
		<xsl:when test="boolean($isStatic)">
			<xsl:value-of select="$defClosure"/>
			<xsl:text>m = </xsl:text>
			<xsl:if test="boolean($isJava)">
				<xsl:text>(VariadicFunction) </xsl:text>
			</xsl:if>
			<xsl:text>main;</xsl:text>
		</xsl:when>
		<xsl:otherwise>
			<xsl:value-of select="$classname"/>
			<xsl:text> c = new </xsl:text>
			<xsl:value-of select="$classname"/>
			<xsl:text>(); </xsl:text>
			<xsl:value-of select="$defClosure"/>
			<xsl:text>m = </xsl:text>
			<xsl:if test="boolean($isJava)">
				<xsl:text>(VariadicFunction) </xsl:text>
			</xsl:if>
			<xsl:text>c.main;</xsl:text>
		</xsl:otherwise>
	    </xsl:choose>
	    <NEWLINE/>
	    </STATEMENT>

	    <!--
	    #====
	    # Activate main method as &main coexpression.
	    #====
	    # if java: IconCoExpression.activate(null, null,
	    #		new IconCoExpression( (Object... args')-> { return
	    #			(IIconIterator) m.apply((Object[]) args); },
	    #		()->{return IconList.createArray();}));
	    # else: m(args) instead of m.apply in above.
	    #==== If not activation, would be:
	    # if java: ((IIconIterator) m.apply((Object[]) args)).next();
	    # else:    (m(args)).next()
	    #====
	    -->
	    <STATEMENT>
		<xsl:text>IconCoExpression.activate(null, null, </xsl:text>
		<xsl:value-of select="$newIconCoExpression"/>
		<xsl:text>(</xsl:text>
		<NEWLINE/>
	    </STATEMENT>

	    <STATEMENT indent="true">
		<!-- BEGIN formatClosure -->
		<xsl:apply-templates select="." mode="formatClosure">
		  <xsl:with-param name="argsUniquename" select="$coexprUniquename"/>
		  <xsl:with-param name="asVariadicFunction" select="true()"/>
		  <xsl:with-param name="noindentEnd" select="true()"/>
		  <xsl:with-param name="body" select="xalan:nodeset($closureBody)/BODY"/>
		</xsl:apply-templates>
		<!-- END formatClosure -->

		<xsl:text>,</xsl:text>
		<NEWLINE/>
	    </STATEMENT>

	    <STATEMENT>
		<!-- BEGIN formatClosure -->
		<xsl:apply-templates select="." mode="formatClosure">
		  <xsl:with-param name="isEmptyArgs" select="true()"/>
		  <xsl:with-param name="newlineAtBegin" select="false()"/>
		  <xsl:with-param name="noindentEnd" select="true()"/>
		  <xsl:with-param name="body" select="xalan:nodeset($envBody)/BODY"/>
		</xsl:apply-templates>
		<!-- END formatClosure -->

		<xsl:text> ))</xsl:text>
		<DELIMITER ID=";" nodelete="true"/>
	    </STATEMENT>

	    <!--
	    #====
	    # Shutdown IconCoExpression executorService thread pool.
	    #====
	    # IconCoExpression.shutdown();
	    #====
	    -->
	    <STATEMENT>
		<xsl:text>IconCoExpression.shutdown()</xsl:text>
		<DELIMITER ID=";" nodelete="true"/>
	    </STATEMENT>

	  <DELIMITER ID="}"/>
	  </BLOCK>
	  </STATEMENT>
</xsl:template>

<!--
#====
# Format class extends.
# FORMAT super: :x:y
# TAGS super: QUALIFIED/
#	<TYPE/METHODREF/DOTNAME/ID::ID | <TYPE/DOTNAME/ID | TYPE/ID> >
# TODO TBD: For now, just extract first type element, others will be mixins.
#====
-->
<xsl:template match="QUALIFIED" mode="formatClassExtends" priority="2">
    <xsl:copy>
    <xsl:copy-of select="@*"/>
	<xsl:apply-templates select="*[1]"/>	<!-- TBD FIXME: TYPE -->
    </xsl:copy>
</xsl:template>

<!--
#====
# Assign class fields in constructor from class parameters: this.x = x;
#====
-->
<xsl:template match="QUALIFIED" mode="assignClassField" priority="2">
    <STATEMENT>
    <xsl:copy>
    <xsl:copy-of select="@*"/>
	<xsl:text>this.</xsl:text>
	<xsl:value-of select="DECLARATION[IDENTIFIER]"/>
	<xsl:text> = </xsl:text>
	<xsl:value-of select="DECLARATION[IDENTIFIER]"/>
    </xsl:copy>
    <DELIMITER ID=";"/>
    </STATEMENT>
</xsl:template>

<!--
#=============================================================================
# Create method in class.
#   method f(x,y) {locals; body} =>
#     [modifiers] def f = { x,y -> locals; // includes normalization temporaries
#		return classCachename.getFree(methodname_unique) ?: 
#			new IconIterator(body;fail)
#		setCache(classCachename, methodname_unique);
#	}
#
# For static method with same name as class, or for main method,
#	the actual method has a unique name.
# If (useMethodUniquename and isMain), methodname = @methodUniquename.
# Uniqueified method name, @methodUniquename, was created in normalization.
#
# TAGS method: STATEMENT/< KEYWORD[method] DECLARATION/IDENTIFIER[methodname]
#			   parameters body >
#	TAGS parameters: TUPLE/< QUALIFIED/< DECLARATION/IDENTIFIER
#				[:Type] [: or = TYPE<LITERAL>] > [SUBSCRIPT] >
#	TAGS body:       BLOCK
#=============================================================================
-->
<xsl:template match="STATEMENT" mode="createMethod" priority="2">
    <xsl:param name="classCachename"/>
    <xsl:param name="classStaticCachename"/>
    <xsl:param name="classInitialCachename"/>
    <xsl:param name="closureUniquename" select="@methodUniquename"/>
    <xsl:param name="asMethodNotClosure" select="false()"/>
    <xsl:param name="isMain" select="DECLARATION[IDENTIFIER[string()='main']]"/>
    <xsl:param name="isStatic" select="KEYWORD[string()='static']"/>
	<!-- For non-main methods -->
    <xsl:param name="modifiers" select="'public'"/>
    <xsl:param name="useMethodUniquename" select="false()"/>
	<!-- For main method -->
    <xsl:param name="mainModifiers" select="'private'"/>
    <xsl:param name="useMethodUniquenameIfMain" select="false()"/>
    <xsl:param name="classname" select="''"/>
    <xsl:param name="useStaticInitializer" select="false()"/>

    <xsl:variable name="originalMethodname">
	<xsl:value-of select="DECLARATION[IDENTIFIER] | KEYWORD[(string()='initially')]"/>
    </xsl:variable>

    <xsl:variable name="methodnameRTF">
      <xsl:choose>
      <!--
      #====
      # Avoid method collision: Use unique name for the actual method,
      #	  if main, or has same name as class and is Groovy (ie, not Java).
      #====
      -->
      <xsl:when test="boolean($useMethodUniquename) or (boolean($useMethodUniquenameIfMain) and (boolean($isMain) or (not(boolean($isJava)) and DECLARATION[IDENTIFIER[string()=$classname]])))">
	<xsl:value-of select="@methodUniquename"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="$originalMethodname"/>
      </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="methodname">
	<xsl:value-of select="$methodnameRTF"/>
    </xsl:variable>

    <!--
    #====
    # If static method, use classStaticCachename instead of classCachename.
    #====
    -->
    <xsl:variable name="cachenameRTF">
      <xsl:choose>
	<xsl:when test="boolean($isStatic)">
		<xsl:value-of select="$classStaticCachename"/>
	</xsl:when>
	<xsl:otherwise>
		<xsl:value-of select="$classCachename"/>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="cachename">
	<xsl:value-of select="$cachenameRTF"/>
    </xsl:variable>

    <!--
    #====
    # Create annotation: @MMethod(name, methodName)
    #			 @MParameters
    #                    @MLocals
    #====
    -->
    <xsl:if test="boolean($addAnnotations)">
      <STATEMENT>
	<xsl:text>@MMethod(name=</xsl:text>
	    <xsl:text>"</xsl:text>
	    <xsl:value-of select="$originalMethodname"/>
	    <xsl:text>"</xsl:text>
	<xsl:text>, methodName=</xsl:text>
	    <xsl:text>"</xsl:text>
	    <xsl:value-of select="$methodname"/>
	    <xsl:text>"</xsl:text>
	<xsl:text>)</xsl:text>
      <NEWLINE/>
      </STATEMENT>

      <xsl:apply-templates select="TUPLE/*[not(self::DELIMITER)]" mode="createField">
	<xsl:with-param name="createParameterAnnotation" select="true()"/>
	<xsl:with-param name="forceLastVarargs" select="$isMain"/>
      </xsl:apply-templates>

      <xsl:apply-templates select="BLOCK/*" mode="extractLocals">
	<xsl:with-param name="createLocalAnnotation" select="true()"/>
	<xsl:with-param name="includeTemporaries" select="false()"/>
	<xsl:with-param name="omitComments" select="true()"/>
	<xsl:with-param name="addSemicolon" select="false()"/>
	<xsl:with-param name="justDeclarations" select="true()"/>
	<xsl:with-param name="includeStatics" select="true()"/>
      </xsl:apply-templates>
    </xsl:if>

    <!--
    #====
    # Define method.
    # If Java: public VariadicFunction methodname = (Object... args) -> { body }
    # If Groovy: public def methodname = { Object... args -> { body } }
    # If asMethodNotClosure:
    #   private Object methodUniquename (Object... args) { body }
    #====
    -->
    <xsl:copy> 
    <xsl:copy-of select="@*"/>
	<xsl:if test="boolean($asMethodNotClosure)">
		<xsl:attribute name="indent" select="true()"/>
	</xsl:if>
	<xsl:if test="boolean($modifiers) and not(boolean($isMain))">
		<xsl:value-of select="$modifiers"/>
		<xsl:text> </xsl:text>
	</xsl:if>
	<xsl:if test="boolean($mainModifiers) and boolean($isMain)">
		<xsl:value-of select="$mainModifiers"/>
		<xsl:text> </xsl:text>
	</xsl:if>
	<xsl:if test="boolean($isStatic)">
		<xsl:text>static </xsl:text>
	</xsl:if>

	<xsl:choose>
	  <xsl:when test="boolean($asMethodNotClosure)">
		<xsl:value-of select="$defMethod"/>
		<xsl:value-of select="$methodname"/>
	  </xsl:when>
	  <xsl:otherwise>
		<xsl:value-of select="$defClosure"/>
		<xsl:value-of select="$methodname"/>
		<xsl:text> =</xsl:text>
	  </xsl:otherwise>
	  </xsl:choose>

	<xsl:apply-templates select="BLOCK" mode="createClosure">
		<xsl:with-param name="classCachename" select="$cachename"/>
		<xsl:with-param name="classInitialCachename" select="$classInitialCachename"/>
		<xsl:with-param name="closureUniquename" select="$closureUniquename"/>
		<xsl:with-param name="forceLastVarargs" select="$isMain"/>
		<xsl:with-param name="params" select="TUPLE"/>
		<xsl:with-param name="asMethodNotClosure" select="$asMethodNotClosure"/>
		<xsl:with-param name="isStatic" select="$isStatic"/>
		<xsl:with-param name="useStaticInitializer" select="$useStaticInitializer"/>
	</xsl:apply-templates>

    <xsl:choose>
      <xsl:when test="boolean($asMethodNotClosure)">
	<NEWLINE/>
      </xsl:when>
      <xsl:otherwise>
	<DELIMITER ID=";" nodelete="true()"/>
      </xsl:otherwise>
    </xsl:choose>

    </xsl:copy> 
</xsl:template>

<!--
#=============================================================================
# Bypass the initial clause in a method,
# when creating the procedure's static method.
# The initial clause was already placed in the class static initializer block.
# The class initializer is invoked on first use, and is thread-safe.
#=============================================================================
-->
<xsl:template match="STATEMENT[KEYWORD[string() = 'initial']]" priority="2">
</xsl:template>

<!--
#=============================================================================
# Global. At top-level, if interpretive, create global. Otherwise just skip.
#		global g  ==>  g_r = new IconGlobal("g");
#	Already recorded global in topLevelGlobals, in preprocessing stage.
# This transform could be moved to transformTopLevel.
# TAGS global: STATEMENT/< KEYWORD/global ENUM/<DECLARATION/IDENTIFIER, ...> >
#=============================================================================
# OLD VERSION: global G  ==> class G { static def G; }
#=============================================================================
-->
<xsl:template match="STATEMENT[KEYWORD[string()='global']]" priority="2">
  <xsl:param name="modifiers" select="'public'"/>

  <xsl:variable name="isInterpretive" select="cmd:TisProperty('Properties', 'isInterpretive', 'true')"/>
  <xsl:variable name="isEmbedded" select="cmd:TisProperty('Properties', 'isEmbedded', 'true')"/>

    <!-- If interpretive mode, just create local -->
    <xsl:if test="boolean($isInterpretive)">
	<xsl:apply-templates select="ENUM/DECLARATION[@isGlobalVariable]" mode="createLocal">
		<xsl:with-param name="omitDef" select="boolean($isInterpretive) and not(boolean($isEmbedded))"/>
		<xsl:with-param name="isToGlobal" select="true()"/>
		<xsl:with-param name="addSemicolon" select="true()"/>
		<xsl:with-param name="omitComments" select="true()"/>
	</xsl:apply-templates>
    </xsl:if>
</xsl:template>

<!--
#=============================================================================
# Package, control statements, and operators.
#=============================================================================
-->

<!--
#=============================================================================
# Package.   Omit package declaration if interpretive.
#   package x.y           ==> unchanged  # Java style
#   package "x/y"         ==> package x.y       # Legacy Unicon package notation
# TAGS package: package TYPE/<LITERAL | DOTNAME | IDENTIFIER>
#=============================================================================
-->
<xsl:template match="STATEMENT[KEYWORD[string()='package']]" priority="2">

  <xsl:variable name="isInterpretive" select="cmd:TisProperty('Properties', 'isInterpretive', 'true')"/>

  <xsl:choose>
    <xsl:when test="boolean($isInterpretive)">
	<!-- Omit package declaration if interpretive -->
    </xsl:when>
    <xsl:otherwise>
      <xsl:choose>
	<xsl:when test="TYPE[LITERAL]">
	  <!-- Strip quotes, change /=>. -->
	  <xsl:copy>
	    <xsl:copy-of select="@*"/>
	    <xsl:copy-of select="KEYWORD"/>
	    <xsl:variable name="packagename" select="TYPE/LITERAL"/>
	    <xsl:value-of select="translate(substring($packagename, 2, string-length($packagename) - 2), '/', '.')"/>
	    <xsl:copy-of select="*[not(self::TYPE or self::KEYWORD)]"/>
	  </xsl:copy>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:copy-of select="."/>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!--
#=============================================================================
# Import and link.
#   import [static] x.y	==> unchanged # Slight overlap with Unicon if just x
#   import "x/y"	==> import static x.y.*;  import x.y.*
#                                        # Picks up globals and classes
#   link "x/y"		==> import static x.y.*;  import x.y.*
# Must handle: import "x/y", x;
# TAGS import: TYPE/<DOTNAME | IDENTIFIER | LITERAL>  |  ENUMERATION/TYPE...
#=============================================================================
-->
<xsl:template match="STATEMENT[KEYWORD[string()='import' or string()='link']]" priority="2">
  <xsl:choose>
    <xsl:when test="ENUM">
	<xsl:apply-templates select="ENUM/TYPE" mode="singlePackageImport">
	    <xsl:with-param name="parent" select="."/>
	</xsl:apply-templates>
    </xsl:when>
    <xsl:otherwise>
	<xsl:apply-templates select="TYPE" mode="singlePackageImport">
	    <xsl:with-param name="parent" select="."/>
	</xsl:apply-templates>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="TYPE" mode="singlePackageImport" priority="2">
  <xsl:param name="parent"/>
  <xsl:param name="prefix" select="'import static '"/>
  <xsl:param name="suffix" select="'.*'"/>
  <xsl:param name="isRecursive" select="true()"/>

  <xsl:variable name="isLink" select="$parent/KEYWORD[string()='link']"/>
  <STATEMENT>
      <xsl:copy-of select="$parent/@*"/>
      <xsl:choose>
	<xsl:when test="LITERAL">
	    <!-- prefix TYPE[strip quotes, change /=>.] $suffix -->
	    <xsl:value-of select="$prefix"/>
	    <xsl:variable name="packagename" select="LITERAL"/>
	    <xsl:value-of select="translate(substring($packagename, 2, string-length($packagename) - 2), '/', '.')"/>
	    <xsl:if test="boolean($suffix)">
		<xsl:value-of select="$suffix"/>
	    </xsl:if>
	</xsl:when>
	<xsl:when test="boolean($isLink)">
	    <xsl:value-of select="$prefix"/>
	    <xsl:copy-of select="."/>
	    <xsl:if test="boolean($suffix)">
		<xsl:value-of select="$suffix"/>
	    </xsl:if>
	</xsl:when>
	<xsl:otherwise>
	    <xsl:copy-of select="$parent/KEYWORD"/>
	    <xsl:copy-of select="."/>
	</xsl:otherwise>
      </xsl:choose>
      <xsl:copy-of select="$parent/DELIMITER"/>
  </STATEMENT>
  <xsl:if test="$isRecursive and (LITERAL or boolean($isLink))">
	<xsl:apply-templates select="." mode="singlePackageImport">
	    <xsl:with-param name="parent" select="$parent"/>
	    <xsl:with-param name="prefix" select="'import '"/>
	    <xsl:with-param name="isRecursive" select="false()"/>
	</xsl:apply-templates>
  </xsl:if>
</xsl:template>

<!--
#=============================================================================
# Control statements.
#   Uses class constructor derived from first keyword.
#   if e1 then e2 else e3 => IconIf(e1,e2,e3)	
# TAGS control: STATEMENT[@isControl]
#=============================================================================
-->
<xsl:template match="STATEMENT[@isControl]" priority="2">

  <!-- capitalize first letter of construct -->
  <xsl:variable name="constructor">
	<xsl:apply-templates select="." mode="operatorToConstructor">
		<xsl:with-param name="symbol" select="KEYWORD[1]"/>
		<xsl:with-param name="isControlConstruct" select="true()"/>
	</xsl:apply-templates>
  </xsl:variable>

  <xsl:apply-templates select="." mode="createOperation">
       <xsl:with-param name="constructor" select="xalan:nodeset($constructor)/CONSTRUCTOR"/>
  </xsl:apply-templates>
</xsl:template>

<!--
#=============================================================================
# Maps operator symbols to operations over iterators using
#	OperatorOverIterators Unary/Binary, and OperatorOverAtoms Unary/Binary.
#
# Default is to translate operation to a map of an operator over iterators.
#   Uses operator as closure, e.g. x+y, and maps it over iterator product.
#   x op y => new IconOperation(
#		IconOperator.binary((x,y) -> x op y), x, y)
#   op x => new IconOperation(IconOperator.unary((x) -> op x), x)
#
# Boolean operators just return true or false in IconOperator.
# Boolean operators are promoted to return second arg if true, else fail
#	within IconOperation.
#
# TAGS operation: OPERATION [@isBinary @isUnary]
#       	  < OPERATOR [@isAugment @isBoolean]  args... >
#=============================================================================
-->
<xsl:template match="OPERATION" priority="2">

  <xsl:variable name="constructor">
	<xsl:apply-templates select="." mode="operatorToConstructor">
		<xsl:with-param name="symbol" select="OPERATOR[1]"/>
		<xsl:with-param name="isControlConstruct" select="false()"/>
	</xsl:apply-templates>
  </xsl:variable>

  <xsl:apply-templates select="." mode="createOperation">
       <xsl:with-param name="constructor" select="xalan:nodeset($constructor)/CONSTRUCTOR"/>
  </xsl:apply-templates>
</xsl:template>

<!--
#=============================================================================
# Generic template to translate operation or statement to constructor:
#	x op y      => constructor (x,y)  or constructor(op,x,y) if op!=null
#	keyword x y => constructor (x,y)
# Operands are pulled from the current node.
# If operator is given, it is used as the first argument before the operands.
# If hasParenthesis, it is assumed that the constructor already has
#	an opening parenthesis.
#=============================================================================
-->
<!-- Generic template to translate operation or statement to constructor -->
<xsl:template match="*" mode="createOperation" priority="2">
    <xsl:param name="constructor" select="'IconIterator'"/>
    <xsl:param name="operator"/> <!-- used as first argument before operands -->
    <xsl:param name="omitParenthesis" select="false()"/>
    <xsl:param name="map"/>		<!-- .map(operation) -->
    <xsl:param name="modifiers"/>	<!-- .setModifiers(modifiers) -->

    <xsl:variable name="args">
	<ARGS>
	    <xsl:apply-templates select="*[not(self::OPERATOR or self::KEYWORD or self::DELIMITER)]"/>
	</ARGS>
    </xsl:variable>

    <xsl:copy>
    <xsl:copy-of select="@*"/>
	<xsl:value-of select="$constructor"/>
	<xsl:if test="boolean($map)">
		<xsl:text>.map(</xsl:text>
		<xsl:value-of select="$map"/>
		<xsl:text>)</xsl:text>
	</xsl:if>
	<xsl:if test="boolean($modifiers)">
		<xsl:text>.setModifiers(</xsl:text>
		<xsl:value-of select="$modifiers"/>
		<xsl:text>)</xsl:text>
	</xsl:if>

	<xsl:if test="not(boolean($omitParenthesis))">
		<xsl:text>(</xsl:text>
	</xsl:if>
	<xsl:if test="boolean($operator)">
		<xsl:copy-of select="$operator"/>
		<DELIMITER ID=","/>
	</xsl:if>
	<xsl:apply-templates select="xalan:nodeset($args)/ARGS/*" mode="extractToList"/>
	<xsl:text>)</xsl:text>
    </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Map symbols to operations over iterators using
#     OperatorOverIterators Unary/Binary, and OperatorOverAtoms Unary/Binary.
# Symbols include operators, control constructs, and &keywords.
#
# Control constructs: if => newIconIf
# Other operators:    +  => new IconOperation(plus).over
#
# SEE: spring_config.xml for a full description.
# TAGS operation: OPERATION [@isBinary @isUnary]
#       	  < OPERATOR [@isAugment @isBoolean]  args... >
#=============================================================================
-->
<xsl:template match="*" mode="operatorToConstructor">
  <xsl:param name="symbol" select="OPERATOR[1]"/>
  <xsl:param name="isUnary" select="count(*) &lt;= 2"/>	<!-- @isUnary -->
  <xsl:param name="isControlConstruct" select="false()"/>

  <!-- Use Spring property to configure -->
  <xsl:variable name="hasOverIteratorsBinary" select="cmd:ThasProperty('OperatorOverIteratorsBinary', $symbol)"/>
  <xsl:variable name="hasOverIteratorsUnary" select="cmd:ThasProperty('OperatorOverIteratorsUnary', $symbol)"/>
  <xsl:variable name="hasOverAtomsBinary" select="cmd:ThasProperty('OperatorOverAtomsBinary', $symbol)"/>
  <xsl:variable name="hasOverAtomsUnary" select="cmd:ThasProperty('OperatorOverAtomsUnary', $symbol)"/>

  <!-- xop: create unique x operand name -->
  <xsl:variable name="xOperandUnique">
	<xsl:value-of select="cmd:TgetReusableUnique('xop')"/>
  </xsl:variable>

  <!-- yop: create unique y operator name -->
  <xsl:variable name="yOperandUnique">
	<xsl:value-of select="cmd:TgetReusableUnique('yop')"/>
  </xsl:variable>

  <CONSTRUCTOR>
      <xsl:choose>
	<xsl:when test="boolean($isUnary) and boolean($hasOverIteratorsUnary)">
		<xsl:variable name="overIterators" select="cmd:TgetProperty('OperatorOverIteratorsUnary', $symbol)"/>
		<xsl:value-of select="cmd:Treplace($overIterators, '$x', $xOperandUnique, '$y', $yOperandUnique, '$op', $symbol)"/>
	</xsl:when>
	<xsl:when test="not(boolean($isUnary)) and boolean($hasOverIteratorsBinary)">
		<xsl:variable name="overIterators" select="cmd:TgetProperty('OperatorOverIteratorsBinary', $symbol)"/>
		<xsl:value-of select="cmd:Treplace($overIterators, '$x', $xOperandUnique, '$y', $yOperandUnique, '$op', $symbol)"/>
	</xsl:when>
	<xsl:when test="(boolean($isUnary) and boolean($hasOverAtomsUnary)) or (not(boolean($isUnary)) and boolean($hasOverAtomsBinary))">
		<xsl:value-of select="$newIconOperation"/>
		<xsl:text>(</xsl:text>
		<xsl:apply-templates select="." mode="createOperator">
			<xsl:with-param name="symbol" select="$symbol"/>
			<xsl:with-param name="isUnary" select="$isUnary"/>
		</xsl:apply-templates>
		<xsl:text>).over</xsl:text>
	</xsl:when>
	<xsl:otherwise>
	  <!-- Use default -->
	  <xsl:choose> 
	    <xsl:when test="boolean($isControlConstruct)">
		<!-- Change "if" to "IconIf" by capitalizing first character -->
		  <xsl:value-of select="$newIconGenericHead"/>
		  <xsl:value-of select="concat(translate(substring($symbol, 1, 1), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ' ), substring($symbol, 2))"/>
		  <xsl:value-of select="$newIconGenericTail"/>
	    </xsl:when>
	    <xsl:otherwise>
	      <!-- Not control construct -->
		  <xsl:value-of select="$newIconOperation"/>
		  <xsl:text>(</xsl:text>
		  <xsl:apply-templates select="." mode="createOperator">
			<xsl:with-param name="symbol" select="$symbol"/>
			<xsl:with-param name="isUnary" select="$isUnary"/>
		  </xsl:apply-templates>
		  <xsl:text>).over</xsl:text>
	    </xsl:otherwise>
	  </xsl:choose>
	</xsl:otherwise>
      </xsl:choose>
  </CONSTRUCTOR>

</xsl:template>

<!--
#=============================================================================
# Maps symbol to operator using OperatorOverAtoms Unary/Binary.
#   If symbol is in OperatorOverAtoms, uses its IconOperator over atoms,
#   else defaults to:
#	IconOperator.binary(($x,$y) -> $x $op $y) [.setIsBoolean()]
#	IconOperator.unary(($x) -> $op $x)   [.setIsBoolean()]
# Substitutes: $x, $y, $op for unique x and y and operator symbol,
#	respectively.
# TAGS: by default assumes applied to OPERATION<OPERATOR>
#=============================================================================
-->
<xsl:template match="*" mode="createOperator">
  <xsl:param name="symbol" select="OPERATOR[1]"/>
  <xsl:param name="isUnary" select="count(*) &lt;= 2"/>
  <xsl:param name="isBoolean" select="OPERATOR[1][@isBoolean]"/>

  <!-- Use Spring property to configure -->
  <xsl:variable name="hasOverAtomsUnary" select="cmd:ThasProperty('OperatorOverAtomsUnary', $symbol)"/>
  <xsl:variable name="hasOverAtomsBinary" select="cmd:ThasProperty('OperatorOverAtomsBinary', $symbol)"/>

  <!-- xop: create unique x operand name -->
  <xsl:variable name="xOperandUnique">
	<xsl:value-of select="cmd:TgetReusableUnique('xop')"/>
  </xsl:variable>

  <!-- yop: create unique y operator name -->
  <xsl:variable name="yOperandUnique">
	<xsl:value-of select="cmd:TgetReusableUnique('yop')"/>
  </xsl:variable>

  <OPERATOR>

  <xsl:choose>
    <xsl:when test="boolean($isUnary) and boolean($hasOverAtomsUnary)">
	<xsl:variable name="overAtoms" select="cmd:TgetProperty('OperatorOverAtomsUnary', $symbol)"/>
	<xsl:value-of select="cmd:Treplace($overAtoms, '$x', $xOperandUnique, '$y', $yOperandUnique, '$op', $symbol)"/>
    </xsl:when>

    <xsl:when test="not(boolean($isUnary)) and boolean($hasOverAtomsBinary)">
	<xsl:variable name="overAtoms" select="cmd:TgetProperty('OperatorOverAtomsBinary', $symbol)"/>
	<xsl:value-of select="cmd:Treplace($overAtoms, '$x', $xOperandUnique, '$y', $yOperandUnique, '$op', $symbol)"/>
    </xsl:when>

    <xsl:otherwise>
      <!-- Binary or unary operator: {x,y -> x op y} | {x -> op x} -->
      <xsl:choose>
	<xsl:when test="boolean($isUnary)">
	  <xsl:value-of select="cmd:Treplace('IconOperator.unary($beginCl$x$bodyCl$op $x$endCl)', '$beginCl', $beginClosure, '$bodyCl', $bodyClosure, '$endCl', $endClosure, '$x', $xOperandUnique, '$y', $yOperandUnique, '$op', $symbol)"/>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:value-of select="cmd:Treplace('IconOperator.binary($beginCl$x,$y$bodyCl$x $op $y$endCl)', '$beginCl', $beginClosure, '$bodyCl', $bodyClosure, '$endCl', $endClosure, '$x', $xOperandUnique, '$y', $yOperandUnique, '$op', $symbol)"/>
	</xsl:otherwise>
      </xsl:choose>
      <!-- Cast operator as boolean if needed, so fails if false -->
      <!--
      <xsl:if test="boolean($isBoolean)">
	    <xsl:text>.setIsBoolean()</xsl:text>
      </xsl:if>
      -->
    </xsl:otherwise>
  </xsl:choose>

  </OPERATOR>
</xsl:template>

<!--
#=============================================================================
# Special operations over iterators that translate to nonstandard constructors:
#	product, assign, bounded iterator, coexpression.
#   x & y	=> IconProduct(x,y)
#   x := y	=> IconAssign(x,y,augment,swap)
#   |> x	=> IconCoExpression (x)		// Runs pipe for coexpression
#   |>> x	=> IconCoExpression (x)		// Runs proxy for coexpression
#   (x in e)    => IconIn (new IconVar(()->x, (y)->x=y), e)
#			// Bind variable to generator
#
# Other operations simply translated to constructors: concat, repeat, limit, not
#   x | y	=> IconConcat (x,y)
#   |x		=> IconRepeatUntilEmpty (x)	// (GCL: x*)
#   x\limit	=> IconLimit (x,limit) 		// (GCL: x:limit)
#   not x	=> IconNot (x)
#   <> x	=> IconFirstClass (x)
# Done in postprocess: e to e1 by e2	=> IconToIterator
#
# Other special operators:
#   \x  fails if null : this is a boolean operator
#   /x  fails if non-null
#
# NOTE: "Limit is computed before expr is evaluated,
#	an exception to the otherwise left-to-right evaluation of expressions".
#    This is handled properly by IconLimit, which sequences the limit
#    iterator first.
#
# TAGS: PRODUCT, ASSIGN, ITERATOR, OPERATION.
#	Product, assign, and iterator are not OPERATION tags.
#=============================================================================
-->
<!-- x & y	=> IconProduct (x,y) : can have @map=operation @modifiers=s -->
<xsl:template match="PRODUCT" priority="2">
    <xsl:apply-templates select="." mode="createOperation">
       <xsl:with-param name="constructor" select="$newIconProduct"/>
       <xsl:with-param name="map" select="@map"/>
       <xsl:with-param name="modifiers" select="@modifiers"/>
    </xsl:apply-templates>
</xsl:template>

<!--
# not x	=> IconNot (x)
# "not" is keyword that appears in operator, rather than operator.
-->
<xsl:template match="OPERATION[KEYWORD[(string()='not')]]" priority="3">
    <xsl:apply-templates select="." mode="createOperation">
       <xsl:with-param name="constructor" select="$newIconNot"/>
    </xsl:apply-templates>
</xsl:template>

<!--
#====
# Iteration.
#   (x in e) => new IconIn(   // Bind variable to generator
# 		new IconVar(get:()->x, set:(y)->x=y), e)
# TAGS iterator: ITERATOR/TUPLE/EXPRESSION/<ATOM_lhs EXPRESSION>
#====
-->
<xsl:template match="ITERATOR" priority="2">
    <xsl:apply-templates select="TUPLE/EXPRESSION" mode="createOperation">
       <xsl:with-param name="constructor" select="$newIconIn"/>
    </xsl:apply-templates>
</xsl:template>

<!--
#=============================================================================
# Assign.
#   x:=y => new IconOperation(new IconAssign(),x,y)
#   x<-y => new IconOperation(new IconAssign().isUndoable(),x,y)
#   x+:=y => new IconOperation(new IconAssign().augment(op),x,y)
#   x:=:y => new IconOperation(new IconAssign().swap(),x,y)
#
# TAGS assign: ASSIGN/OPERATOR[@isAugment @isReversible]
#	:=  Assign
#	:=: Swap
#	<-  Reversible assign
#	<-> Reversible swap
#	+:= Augmented assign operators (many)
#
# Augmented operators are reduced to normal operators by deleting ":=" suffix.
# Initializers already had lhs changed from declaration to atom.
#=============================================================================
-->
<xsl:template match="ASSIGN" priority="2">

    <!-- xop: create unique x operand name -->
    <xsl:variable name="xOperandUnique">
	<xsl:value-of select="cmd:TgetReusableUnique('xop')"/>
    </xsl:variable>

    <!-- yop: create unique y operator name -->
    <xsl:variable name="yOperandUnique">
	<xsl:value-of select="cmd:TgetReusableUnique('yop')"/>
    </xsl:variable>

    <xsl:variable name="constructor">
      <CONSTRUCTOR>
	<xsl:value-of select="$newIconAssign"/>
	<xsl:text>()</xsl:text>

	<!-- Augment: x,y -> x op y -->
	<xsl:if test="OPERATOR[1][@isAugment]">
	    <xsl:text>.augment(</xsl:text>
		<xsl:apply-templates select="." mode="createOperator">
		    <xsl:with-param name="symbol" select="substring-before(OPERATOR[1]/text(),':=')"/>
		    <xsl:with-param name="isBoolean" select="false()"/>
		</xsl:apply-templates>
	    <xsl:text>)</xsl:text>
	</xsl:if>

	<!-- Reversible -->
	<xsl:if test="OPERATOR[1][(string()='&lt;-') or (string()='&lt;->')]">
		<xsl:text>.undoable()</xsl:text>
	</xsl:if>

	<!-- Swap -->
	<xsl:if test="OPERATOR[1][(string()=':=:') or (string()='&lt;->')]">
		<xsl:text>.swap()</xsl:text>
	</xsl:if>

	<xsl:text>.over</xsl:text>
      </CONSTRUCTOR>
    </xsl:variable>

    <xsl:apply-templates select="." mode="createOperation">
       <xsl:with-param name="constructor" select="xalan:nodeset($constructor)/CONSTRUCTOR"/>
    </xsl:apply-templates>
</xsl:template>

<!--
#=============================================================================
# fail => new IconReturn(new IconFail())
#	  == return(&fail), where &fail => new IconFail()
#=============================================================================
-->
<xsl:template match="STATEMENT[@isControl][KEYWORD[string()='fail']]" priority="3">
  <xsl:copy>
	<xsl:value-of select="$newIconReturn"/>
	<xsl:text>(</xsl:text>
		<xsl:value-of select="$newIconFail"/>
		<xsl:text>()</xsl:text>
	<xsl:text>)</xsl:text>
  </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Co-expression create.
#   create e => new IconCoExpression(VariadicFunction creator, Callable getenv)
#	where creator = rewrite {(x,y,z) -> e }			// Shadow locals
#	  getenv  = () -> {return IconList.createArray(x_r.deref(),...)} // Capture env
#	  and x,y,z are all referenced locals		// {()->[x,y,z]}
# Thus, create e => <>{(local_refs) -> e}({()->[local_refs]})
#   @c => !c.next()
#   ^c => !c.restart()
#   *c => !c.count()
#
# For co-expressions, need to turn off methodBodyCache when create closure since
# it is used outside IconInvokeIterator, and will not free any cached body.
#
# TAGS: STATEMENT[@isControl] < KEYWORD[string()='create'] EXPRESSION >
#=============================================================================
-->
<xsl:template match="STATEMENT[@isControl][KEYWORD[string()='create']]" priority="3">
  <xsl:copy>
	<xsl:value-of select="$newIconCoExpression"/>
	<xsl:text>(</xsl:text>
	  <xsl:apply-templates select="*[2]" mode="shadowAsClosure"/>
	<xsl:text>, </xsl:text>
	  <xsl:apply-templates select="*[2]" mode="captureEnvironment"/>
	<xsl:text>).createCoExpression()</xsl:text>
  </xsl:copy>
</xsl:template>

<!--
#====
# Thread create. On next() creates thread for coexpression that runs to failure.
#   thread e => new IconCoExpression(creator,getenv).createThread()
#	where thread e = spawn(create e)
# TAGS: STATEMENT[@isControl] < KEYWORD[string()='thread'] EXPRESSION >
#====
-->
<xsl:template match="STATEMENT[@isControl][KEYWORD[string()='thread']]" priority="3">
  <xsl:copy>
	<xsl:value-of select="$newIconCoExpression"/>
	<xsl:text>(</xsl:text>
	  <xsl:apply-templates select="*[2]" mode="shadowAsClosure"/>
	<xsl:text>, </xsl:text>
	  <xsl:apply-templates select="*[2]" mode="captureEnvironment"/>
	<xsl:text>).createThread()</xsl:text>
  </xsl:copy>
</xsl:template>

<!--
#====
# Coexpression create.
#   |<> e => new IconCoExpression(creator,getenv).createCoExpression()
# TAGS operation: OPERATION [@isBinary @isUnary]
#       	  < OPERATOR [@isAugment @isBoolean]  args... >
#====
-->
<xsl:template match="OPERATION[@isUnary][OPERATOR[string()='|&lt;>']]" priority="3">
  <xsl:copy>
	<xsl:value-of select="$newIconCoExpression"/>
	<xsl:text>(</xsl:text>
	  <xsl:apply-templates select="*[2]" mode="shadowAsClosure"/>
	<xsl:text>, </xsl:text>
	  <xsl:apply-templates select="*[2]" mode="captureEnvironment"/>
	<xsl:text>).createCoExpression()</xsl:text>
  </xsl:copy>
</xsl:template>

<!--
#====
# First class expression create.
#   <> e => new IconCoExpression(creator,getenv).createFirstClass()
# TAGS operation: OPERATION [@isBinary @isUnary]
#       	  < OPERATOR [@isAugment @isBoolean]  args... >
#====
-->
<xsl:template match="OPERATION[@isUnary][OPERATOR[string()='&lt;>']]" priority="3">
  <xsl:copy>
	<xsl:value-of select="$newIconCoExpression"/>
	<xsl:text>(</xsl:text>
	  <xsl:apply-templates select="*[2]" mode="shadowAsClosure">
		<xsl:with-param name="cloneLocals" select="false()"/>
	  </xsl:apply-templates>
	<xsl:text>, </xsl:text>
	  <xsl:apply-templates select="*[2]" mode="captureEnvironment">
		<xsl:with-param name="cloneLocals" select="false()"/>
	  </xsl:apply-templates>
	<xsl:text>).createFirstClass()</xsl:text>
  </xsl:copy>
</xsl:template>

<!--
#====
# Proxy create.  Creates in-place generator that is proxy for coexpression.
#   |>> e => new IconCoExpression(creator,getenv).createProxy()
# TAGS operation: OPERATION [@isBinary @isUnary]
#       	  < OPERATOR [@isAugment @isBoolean]  args... >
#====
-->
<xsl:template match="OPERATION[@isUnary][OPERATOR[string()='|>>']]" priority="3">
  <xsl:copy>
	<xsl:value-of select="$newIconCoExpression"/>
	<xsl:text>(</xsl:text>
	  <xsl:apply-templates select="*[2]" mode="shadowAsClosure"/>
	<xsl:text>, </xsl:text>
	  <xsl:apply-templates select="*[2]" mode="captureEnvironment"/>
	<xsl:text>).createProxy()</xsl:text>
  </xsl:copy>
</xsl:template>

<!--
#====
# Pipe create.  On next() creates pipe for coexpression.
#   |> e => new IconCoExpression(creator,getenv).createPipe()
# TAGS operation: OPERATION [@isBinary @isUnary]
#       	  < OPERATOR [@isAugment @isBoolean]  args... >
#====
-->
<xsl:template match="OPERATION[@isUnary][OPERATOR[string()='|>']]" priority="3">
  <xsl:copy>
	<xsl:value-of select="$newIconCoExpression"/>
	<xsl:text>(</xsl:text>
	  <xsl:apply-templates select="*[2]" mode="shadowAsClosure"/>
	<xsl:text>, </xsl:text>
	  <xsl:apply-templates select="*[2]" mode="captureEnvironment"/>
	<xsl:text>).createPipe()</xsl:text>
  </xsl:copy>
</xsl:template>

<!--
#====
# Shadow expression as closure whose parameters are all of the
# referenced dynamic variables.
#	expr => rewrite {(x,y,z) -> expr}
# There is no methodbodycache for the creator closure.
#
# If not cloneLocals, will not clone environment, and returns simple closure.
#
# TAGS closure: CLOSURE { TUPLE ARROW BLOCK }
# TAGS block: BLOCK/STATEMENT/EXPRESSION
#====
-->
<xsl:template match="*" mode="shadowAsClosure" priority="2">
  <xsl:param name="cloneLocals" select="true()"/>

  <xsl:variable name="params">
    <PARAMS>
    <xsl:if test="boolean($cloneLocals)">
      <xsl:apply-templates select="." mode="declareReferenced">
	<xsl:with-param name="generateParameters" select="true()"/>
      </xsl:apply-templates>
    </xsl:if>
    </PARAMS>
  </xsl:variable>

  <xsl:variable name="tuple">
    <TUPLE>
	<DELIMITER ID="("/>
	<xsl:if test="boolean($cloneLocals)">
	  <!-- Extract parameter list, are names or literals -->
	  <xsl:apply-templates select="xalan:nodeset($params)/PARAMS/*" mode="extractToList"/>

	</xsl:if>
	<DELIMITER ID=")"/>
    </TUPLE>
  </xsl:variable>

  <xsl:variable name="body">
    <BLOCK>
	<!--
	# <xsl:copy-of select="*[1]"/>
	-->
	<xsl:apply-templates select="." mode="renameReferenced">
		<xsl:with-param name="renameList" select="xalan:nodeset($params)/PARAMS"/>
	</xsl:apply-templates>
    </BLOCK>
  </xsl:variable>

  <!-- Look up to containing class to find classCachename -->
  <xsl:variable name="classCachename">
	<xsl:value-of select="ancestor::*[(self::STATEMENT or self::EXPRESSION) and @classCachename][1]/@classCachename"/>
  </xsl:variable>

  <CLOSUREBLOCK>
    <xsl:copy-of select="@*"/> 
	<xsl:apply-templates select="xalan:nodeset($body)/BLOCK" mode="createClosure">
		<xsl:with-param name="classCachename" select="$classCachename"/>
		<xsl:with-param name="useMethodBodyCache" select="false()"/>
		<xsl:with-param name="asVariadicFunction" select="true()"/>
		<xsl:with-param name="params" select="xalan:nodeset($tuple)/TUPLE"/>
		<xsl:with-param name="appendFail" select="false()"/>
	</xsl:apply-templates>
  </CLOSUREBLOCK>

</xsl:template>

<!--
#====
# Capture environment as closure over all referenced dynamic variables.
#  expr => () -> {return IconList.createArray(x_r.deref(),...);} // Capture env for expr
# If not cloneLocals, will not clone environment, and returns empty closure.
#====
-->
<xsl:template match="*" mode="captureEnvironment" priority="2">
  <xsl:param name="cloneLocals" select="true()"/>

  <xsl:variable name="params">
    <PARAMS>
    <xsl:if test="boolean($cloneLocals)">
	<xsl:apply-templates select="." mode="declareReferenced">
	</xsl:apply-templates>
    </xsl:if>
    </PARAMS>
  </xsl:variable>

  <xsl:variable name="body">
    <BODY>
    <BLOCK>
      <STATEMENT>
	<xsl:text>return IconList.createArray</xsl:text>
	<TUPLE>
	  <DELIMITER ID="("/>
	  <xsl:if test="boolean($cloneLocals)">
	    <!-- Extract parameter list, are names or literals -->
	    <xsl:apply-templates select="xalan:nodeset($params)/PARAMS/*" mode="extractToList"/>
	  </xsl:if>
	  <DELIMITER ID=")"/>
	</TUPLE>
	<DELIMITER ID=";" nonewline="true()"/>
      </STATEMENT>
    </BLOCK>
    </BODY>
  </xsl:variable>

  <CLOSUREBLOCK>
	<!-- BEGIN formatClosure -->
	<xsl:apply-templates select="." mode="formatClosure">
	  <xsl:with-param name="isEmptyArgs" select="true()"/>
	  <xsl:with-param name="newlineAtEnd" select="true()"/>
	  <xsl:with-param name="body" select="xalan:nodeset($body)/BODY"/>
	</xsl:apply-templates>
	<!-- END formatClosure -->
  </CLOSUREBLOCK>

</xsl:template>

<!--
#====
# Create list of all referenced dynamic variables in an expression.
# Scopes down for all local variables and parameters that
# 	are referenced in given expression, excluding temporaries.
# Only keeps those whose local declarations are above the expression.
#
# Condition for shadowing is: reference declBlockDepth <= create blockDepth
# EXAMPLE:  { local x;  create x }	// Must shadow local x in create.
# EXAMPLE:  { create { local y; y } }	// Not shadow y
#
# If generateParameter, will generate a parameter declaration for it,
# else will generate a reified getter for it: x_r.deref()   // NOT: get()
#
# TAGS parameter: TUPLE/< QUALIFIED/< DECLARATION/IDENTIFIER
#			[:Type] [: or = TYPE<LITERAL>] > [SUBSCRIPT] >
#====
-->
<!-- Key used to remove duplicate references -->
<xsl:key name="keyAtomById" match="ATOM" use="IDENTIFIER"/>

<xsl:template match="*" mode="declareReferenced" priority="2">
  <xsl:param name="generateParameters" select="false()"/>

  <!-- Number of blocks at or above this statement or expression -->
  <xsl:variable name="blockDepth" select="@blockDepth"/>

  <!-- Referenced locals and parameters for given expression -->
  <xsl:apply-templates select="descendant-or-self::ATOM[IDENTIFIER][not(parent::OBJECT and (position() &gt; 1))][@isLocal and not(@isToTmp)][@declBlockDepth &lt;= $blockDepth]" mode="removeDuplicates">
    <xsl:with-param name="generateParameters" select="$generateParameters"/>
  </xsl:apply-templates>
</xsl:template>

<!--
#====
# Remove duplicate local references using an XSLT key,
#   and output either a parameter declaration, or a getter for the local.
# Renames the variable to make it unique,
#   since cannot redeclare locals in lambdas, and
#   records the @originalName.
#====
-->
<xsl:template match="ATOM[generate-id() = generate-id(key('keyAtomById', IDENTIFIER)[1])]" mode="removeDuplicates">
  <xsl:param name="generateParameters" select="false()"/>

      <xsl:variable name="varname" select="IDENTIFIER/text()"/>
      <xsl:variable name="renamed" select="cmd:TgetSameUnique($varname, '_s')"/>
      <xsl:choose>
	<xsl:when test="boolean($generateParameters)">
	  <QUALIFIED>
	    <DECLARATION isParameter="true()" originalName="{$varname}">
		<IDENTIFIER>
		    <xsl:value-of select="$renamed"/>
		</IDENTIFIER>
	    </DECLARATION>
	  </QUALIFIED>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:copy>
	    <xsl:attribute name="originalName">
		<xsl:value-of select="$varname"/>
	    </xsl:attribute>
	    <IDENTIFIER>
		<xsl:value-of select="cmd:TgetSameUnique($varname, '_r')"/>
		<xsl:text>.deref()</xsl:text>
	    </IDENTIFIER>
	  </xsl:copy>
	</xsl:otherwise>
      </xsl:choose>
</xsl:template>

<!--
#==== XSLT how to remove duplicates
# <xsl:key name="keyAtombyId" match="ATOM" use="IDENTIFIER"/>
# ATOM[generate-id() = generate-id(key('keyAtomById', IDENTIFIER)[1])]
# ATOM[not(IDENTIFIER/text() = preceding-sibling::ATOM/IDENTIFIER/text())]
#====
-->

<!--
#====
# Rename ATOM references in expression
# using a list of original names and renamed names.
# The rename list is the parameter list from declareReferenced.
#====
-->
<xsl:template match="ATOM[IDENTIFIER][not(parent::OBJECT and (position() &gt; 1))][@isLocal and not(@isToTmp)]" mode="renameReferenced" priority="2">
  <xsl:param name="renameList"/>

  <!-- renamed = Params//DECLARATION[@originalName = $varname]/IDENTIFIER -->
  <xsl:variable name="varname" select="IDENTIFIER/text()"/>
  <xsl:variable name="renameParam" select="($renameList/descendant-or-self::DECLARATION[@originalName = $varname])[1]"/>
  <xsl:variable name="renamed" select="$renameParam/IDENTIFIER/text()"/>

  <xsl:choose>
  <xsl:when test="boolean($renamed)">
    <xsl:copy>
	<!-- Preserve attributes, any non-IDENTIFIER -->
	<xsl:copy-of select="@*"/> 			
	<xsl:copy-of select="node()[not(self::IDENTIFIER)]"/>
	<IDENTIFIER>
	    <xsl:copy-of select="IDENTIFIER/@*"/>	<!-- preserve attr -->
	    <xsl:value-of select="$renamed"/>
	</IDENTIFIER>
    </xsl:copy>
  </xsl:when>
  <xsl:otherwise>
    <xsl:copy-of select="."/>
  </xsl:otherwise>
  </xsl:choose>

</xsl:template>

<!-- ==== Default template to copy nodes through -->
<xsl:template match="@*|node()" mode="renameReferenced">
    <xsl:param name="renameList"/>
    <xsl:copy>
	<xsl:apply-templates select="@*|node()" mode="renameReferenced">
	    <xsl:with-param name="renameList" select="$renameList"/>
	</xsl:apply-templates>
    </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Embedded script annotations.
#=============================================================================
-->

<!-- 
#=============================================================================
# Transform embedded script annotation into native code evaluation.
#	@<script> {< code >}  ==>  IconInvokeIterator(()->{code; return null}).
#		if code contains ";" otherwise { return code; }
# Tags annotation: EXPRESSION/<ANNOTATION EXPRESSION/BigLiteral>
#	ANNOTATION/<DELIMITER ID="<" QUALIFIED/DOTNAME OPERATOR ">" >
# Tags big literal: ATOM[DELIMITER="{<" LITERAL[@isBigLiteral][code]
#			 DELIMITER=">}"]
#=============================================================================
-->
<xsl:template match="EXPRESSION[ANNOTATION[QUALIFIED[DOTNAME[IDENTIFIER[string() = 'script']]]] and EXPRESSION[ATOM[LITERAL[@isBigLiteral]]]]" priority="2">
    <xsl:variable name="scriptCode">
	<xsl:value-of select="EXPRESSION/ATOM/LITERAL/node()"/>
    </xsl:variable>
    <xsl:variable name="closureBody">
      <xsl:choose>
      <xsl:when test="contains($scriptCode,';')">
	  <BODY>
	    <BODY>
	      <xsl:copy-of select="EXPRESSION/ATOM/LITERAL/node()"/>
	      <xsl:text> return null; </xsl:text>
	    </BODY>
	  </BODY>
      </xsl:when>
      <xsl:otherwise>
	  <BODY>
	    <BODY>
	      <xsl:text>return </xsl:text>
	      <xsl:copy-of select="EXPRESSION/ATOM/LITERAL/node()"/>
	      <xsl:text>; </xsl:text>
	    </BODY>
	  </BODY>
      </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:copy>
    <xsl:copy-of select="@*"/>
	<xsl:value-of select="$newIconInvokeIterator"/>
	<xsl:text>(</xsl:text>

	<!-- BEGIN formatClosure -->
	<xsl:apply-templates select="." mode="formatClosure">
	  <xsl:with-param name="isEmptyArgs" select="true()"/>
	  <xsl:with-param name="newlineAtEnd" select="true()"/>
	  <xsl:with-param name="body" select="xalan:nodeset($closureBody)/BODY"/>
	</xsl:apply-templates>
	<!-- END formatClosure -->

	<xsl:text> )</xsl:text>
    </xsl:copy>
</xsl:template>

<!--
#====
# Strip any other annotations.
#====
-->
<xsl:template match="EXPRESSION[ANNOTATION]" priority="1">
    <xsl:apply-templates select="EXPRESSION"/>
</xsl:template>

<!--
#====
# Insert embedded script.
# Occurs if outside method, i.e., parent is class, then just strip it:
#	@<script> {< code >}  ==> code
#====
-->
<xsl:template match="EXPRESSION[EXPRESSION[ANNOTATION]]" mode="insertEmbeddedScript" priority="2">
  <xsl:copy>
    <xsl:value-of select="EXPRESSION/EXPRESSION/ATOM/LITERAL/node()"/>
  </xsl:copy>
</xsl:template>
<!--
<xsl:when test="self::*[parent::BLOCK[parent::STATEMENT[KEYWORD[string()='class']]] or parent::EXPRESSION[parent::BLOCK[parent::STATEMENT[KEYWORD[string()='class']]]]]">
-->

<!-- Default template -->
<xsl:template match="@*|node()" mode="insertEmbeddedScript">
  <xsl:copy>
	<xsl:apply-templates select="@*|node()" mode="insertEmbeddedScript"/>
  </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Lift summary.
#   Lifting takes the low-level variable references and values,
#	as well as object references, collection literals, indexes, and 
#	function invocations, and changes them to concrete constructor calls.
#   Lifting a standalone variable or value will reify it to a property
#	(called an atom), and then promote it to an singleton iterator.
#   When not standalone, ie. inside a primary expression as a field or argument,
#	a symbol may instead be taken to an atom or just plain value.
#   Lifting is driven by 4 parameters, in order of priority:
#     asIterator lifts to an iterator over atoms.
#	This occurs outside a complex primary, i.e., field, invoke, or index.
#     asAtom lifts to a property.   The result is an IconAtom,
#	or Callable if not variable or field: x_r | {-> c}
#     asLambda lifts to a function name.
#     insideClosure lifts to a plain variable or value.
#	A special case is OBJREF, which can be both asAtom and insideClosure.
#     Otherwise, lifts to a plain variable or value.
#   AsAtom will trump insideClosure, except for objref and collection literals
#	where both can have effect.
#   Lift asAtom occurs in the following cases:
#     Index list setter (not insideClosure)
#     (x in I) inLeftIterator variable (not insideClosure).
#     Function over atoms, e.g. synthetic function for promote(args).
#	 Is also insideClosure (affects only objref).
#   Below we shift into lift mode for ATOM, OBJREF, INVOKE, INDEX types. 
#	If @lift, reify and promote to iterator.
#	If @inLeftIterator, promote (x in I) to atom.
#	Otherwise, lift with no parameter.
#=============================================================================
#   @lift, @inLeftIterator, @rhsUnique attributes were created in normalization
#   @rhsUnique is used when reify varaibles to IconVar closures.
#
#   atom/identifier x => new IconIterator(new IconVar(()->x, (rhs)->x=rhs))
#				If reference to local declaration, set local().
#				Just promote to atom if @inLeftIterator.
#   atom/literal l    => new IconIterator(new IconValue(literal))
#				10 => 10G so bigInteger, bigDecimal is automatic
#				x @isBigLiteral => escapeLiteralSequence (x)
#   atom/andkeyword k => new IconIterator(new IconValue(value))
#   atom/packagref    => change : to . and as DOTNAME
#
#   atom/dotname or
#   objref o.x  => new IconIterator(new IconVar(()->o.x, (rhs)->o.x=rhs))
#   group [c] => new IconIterator(new IconVar(()->[c], null))
#
#   index c[i] =>  new IconIndexIterator(()->c, ()->i)
#   invoke f(x) => new IconInvokeIterator(()->f(x))
#=============================================================================
# COMMENT: concretization parameters:
#	asIterator, asLambda, asAtom, isInsideClosure
#====
# asLambda is only used for function name in invoke.
#====
# asAtom is only used for index list setter, and promote function argument.
#   Both expect atoms, so temporaries use get() to pierce its held atom.
#   For index list: then !isInsideClosure:
#	o.deref() => ()->o.get(), literal|group => ()->lg,
#         atom: x_r or IconVar(()->x, (rhs)->x=rhs)
#   For promote args: then isInsideClosure, so o.deref() => o.get()
#	literal|group unchanged, atom as above.
#====
# @isLocal and @isToTmp attributes for variables:
#   If @isLocal, then x is reified, else if @isToClassField, have x and x_r.
#   If @isToTmp (and thus @isLocal), then x is reified and holds atom.
#     @isToTmp temporary appears only in expressions such as (t in I) 
#====
# deref() is used only in temporaries:
#   OBJREF with deref() will be of form: o.deref().x.y  |  o.deref()
#   If an object reference ends in deref(),
#   it will be a singular atom that is a temporary.  Such an
#   o.deref() without .x.y can only occur inside a primary
#   as an index list, function name, or argument, but never standalone.
#   So just o.deref() will never be lifted asIterator.
#   We thus can change o.deref() => o_r
#   when passed to IconIndex setter, or to IconField, which expects an atom|tmp,
#   and which must then use deref() instead of get() or recognize isTemporary.
#   Except for temporaries, deref() is always the same as get().
#====
# Reified variable declarations "x_r":
#   We optimize by, instead of doing a new IconVar creation in every expression
#   to create updateable references that can be returned (which would work),
#   we separately declare reified variables, which are final, and refer to them.
# Class fields are declared normally, and in addition have separate
#   reified variables that refer to them.
# Method parameters, method/block locals, and temporaries are
#   declared as reified variables which just hold the value.
# Temporaries appear in the left-hand side of a bound iterator,
#   e.g., (tmp in e), and hold an atom rather than a raw value.
# For external references, we don't declare reified variables,
#   but just construct them as needed in expressions.
#   (externals are not @isLocal or @isToClassField).
#   We could create them per class and
#   method, but it doesn't save much work, since lift mode still has to
#   perform dynamic reification for "o.x.y" anyway.
#=============================================================================
-->

<!--
#=============================================================================
# Shift into lift mode.
#	Will lift items that need to be lifted, either to iterators or to atoms.
# If isJava, must further resolve primaries, beyond lifting.
#=============================================================================
-->

<xsl:template match="ATOM[IDENTIFIER | DOTNAME] | OBJREF" priority="2">
  <xsl:apply-templates select="." mode="lift">
	<xsl:with-param name="asIterator" select="@lift"/>
	<xsl:with-param name="asAtom" select="@inLeftIterator"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="ATOM[METHODREF]" priority="2">
  <xsl:apply-templates select="." mode="lift">
	<xsl:with-param name="asIterator" select="@lift"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="ATOM[LITERAL]" priority="2">
  <xsl:apply-templates select="." mode="lift">
	<xsl:with-param name="asIterator" select="@lift"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="GROUP[LIST | MAP | SET]" priority="2">
  <xsl:apply-templates select="." mode="lift">
	<xsl:with-param name="asIterator" select="@lift"/>
  </xsl:apply-templates>
</xsl:template>

<!-- ANDKEYWORD were replaced in normalization with LITERAL or DOTNAME -->
<!--
<xsl:template match="ATOM[ANDKEYWORD]" priority="2">
  <xsl:apply-templates select="." mode="lift">
	<xsl:with-param name="asIterator" select="@lift"/>
  </xsl:apply-templates>
</xsl:template>
-->

<!--
#====
# PACKAGEREF: x::y => x.y
#====
-->
<xsl:template match="PACKAGEREF" priority="2">
  <xsl:copy>
  <xsl:apply-templates select="@*" mode="postprocess"/>
  <xsl:variable name="refText">
	<xsl:value-of select="."/>
  </xsl:variable>
  <xsl:value-of select="cmd:Treplace(., '::', '.')"/>
  </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Lift invoke and index.
#=============================================================================
-->

<!--
#====
# Lift index.
#    index c[i] =>  new IconIndexIterator(()->c, ()->i).origin(0|1)
#    slice c[i] =>  new IconIndexIterator(
#			()->c, ()->i, ()->e).origin(0|1)
# TAGS: INDEX/< ATOM|OBJREF, SUBSCRIPT/[i] | SLICE/[i..e] >
#	where [i] = DELIMITER[@ID="["] EXPRESSION DELIMITER[@ID="]"]
#	where [i..e] = DELIMITER[@ID="["] EXPRESSION DELIMITER[@ID=".."]
#			EXPRESSION DELIMITER[@ID="]"]
#====
-->
<xsl:template match="INDEX[@lift]" priority="2">
  <xsl:variable name="index" select="SUBSCRIPT/*[not(self::DELIMITER)]"/>
  <xsl:variable name="begin" select="SLICE/*[not(self::DELIMITER)][1]"/>
  <xsl:variable name="end" select="SLICE/*[not(self::DELIMITER)][2]"/>
  <xsl:variable name="indexOriginRTF">
    <xsl:choose>
	<xsl:when test="cmd:ThasProperty('Properties', 'index.origin')">
	    <xsl:value-of select="cmd:TgetProperty('Properties', 'index.origin')"/>
	</xsl:when>
	<xsl:otherwise>
	    <xsl:text>0</xsl:text>
	</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="indexOrigin">
	<xsl:value-of select="$indexOriginRTF"/>
  </xsl:variable>

  <!--
  #====
  # Derive list as atom.
  # If list is from temporary variable, i.e., OBJREF[DEREF[position()=last()]]
  #	where o.deref() gives "o", then index then uses ()->o as setter.
  #====
  -->
  <xsl:variable name="listIsAtom" select="*[1][self::ATOM[IDENTIFIER | DOTNAME] | self::OBJREF]"/>
  <xsl:variable name="listIsFromTemporary" select="*[1][self::OBJREF/*[self::DEREF and position()=last()]]"/>

  <!-- List setter -->
  <xsl:variable name="listAsAtom">
    <LIST>
	<xsl:apply-templates select="*[1]" mode="lift">
		<xsl:with-param name="asAtom" select="true()"/>
	</xsl:apply-templates>
    </LIST>
  </xsl:variable>

  <xsl:copy>
  <xsl:copy-of select="@*"/>

	<!--
	#====
	# Slice c[i:e]:
	#   new IconIndexIterator(listAtom, ()->begin, ()->end).origin(o)
	#	c[i +: e]:  .plus()
	#	c[i -: e]:  .minus()
	# Subscript c[i]:
	#   new IconIndexIterator(listAtom, ()->index).origin(o)
	#====
	-->
	<xsl:value-of select="$newIconIndexIterator"/>
	<xsl:text>(</xsl:text>
		<xsl:copy-of select="xalan:nodeset($listAsAtom)/LIST/node()"/>
	<xsl:text>, </xsl:text>
	    <xsl:choose>
	    <xsl:when test="SLICE">
		<xsl:apply-templates select="$begin" mode="lift">
			<xsl:with-param name="asAtom" select="true()"/>
		</xsl:apply-templates>
		<xsl:text>, </xsl:text>
		<xsl:apply-templates select="$end" mode="lift">
			<xsl:with-param name="asAtom" select="true()"/>
		</xsl:apply-templates>

		<xsl:choose>
		  <xsl:when test="SLICE/*[self::DELIMITER[@ID='+:']]">
			<xsl:text>).plus(</xsl:text>
		  </xsl:when>
		  <xsl:when test="SLICE/*[self::DELIMITER[@ID='-:']]">
			<xsl:text>).minus(</xsl:text>
		  </xsl:when>
		</xsl:choose>
	    </xsl:when>

	    <xsl:otherwise>
		<xsl:apply-templates select="$index" mode="lift">
			<xsl:with-param name="asAtom" select="true()"/>
		</xsl:apply-templates>
	    </xsl:otherwise>
	    </xsl:choose>

	<!-- If origin not defined, leave out to default to getIndexOrigin() -->
	<xsl:if test="cmd:ThasProperty('Properties', 'index.origin')">
		<xsl:text>).origin(</xsl:text>
		<xsl:value-of select="$indexOrigin"/>
	</xsl:if>
	<xsl:text>)</xsl:text>

  </xsl:copy>
</xsl:template>

<!--
#====
# Lift invoke: f(args).    We always lift invoke.
#    Let f' = resolve f asLambda.
#    Let args' = resolve each arg isInsideClosure.
#    if $isJava
#	if not((f is METHODREF) or
#	(f is ATOM[IDENTIFIER] and @matchesMethodName and not @isLocal)):
#		       ((VariadicFunction) f').apply(args')
#	else: f'(args')		XX: (Object) f'(args')
#    else leave alone: f'(args')
# The result is wrapped as: new IconInvokeIterator(()->result).
# If UndoFunction[symbol], then is wrapped as:
#	new IconInvokeIterator(()->result).undoable(IconFunction[symbol])
#
# The above optimization that bypasses methodref if @matchesMethodName is
# contingent on exposing public methods as method references of the same name.
#
# If keyword or symbol is in OperatorAsFunction, treats operator
# as a generator function over values or atoms that returns an iterator. In this
# case normalization will have extracted arguments to make iteration explicit.
# These non-monogenic generators are thus translated to ersatz functions,
# i.e., synthetic higher-order functions.
#
# TAGS: INVOKE/< ATOM|OBJREF, TUPLE/(args) >
#====
-->
<xsl:template match="INVOKE[@lift]" priority="2">
  <xsl:variable name="symbol" select="*[1]"/>
  <xsl:variable name="hasOperatorAsFunction" select="cmd:ThasProperty('OperatorAsFunction', $symbol)"/>
  <xsl:variable name="hasOperatorAsGenerator" select="cmd:ThasProperty('OperatorAsGenerator', $symbol)"/>
  <xsl:variable name="hasUndo" select="cmd:ThasProperty('UndoFunction', $symbol)"/>

  <!-- Function name -->
  <xsl:variable name="functionNameRTF">
    <xsl:apply-templates select="*[1]" mode="lift"> <!-- OBJREF|ATOM -->
	<xsl:with-param name="asLambda" select="true()"/>
	<xsl:with-param name="isInsideClosure" select="true()"/>
    </xsl:apply-templates>
  </xsl:variable>
  <xsl:variable name="functionNameTextRTF">
	<xsl:apply-templates select="xalan:nodeset($functionNameRTF)/*[1]" mode="toText"/>
  </xsl:variable>
  <xsl:variable name="functionName">
	<xsl:value-of select="$functionNameTextRTF"/>
  </xsl:variable>

  <xsl:variable name="hasFunctionOverAtoms" select="cmd:ThasProperty('FunctionOverAtoms', $functionName)"/>

  <!-- xop: create unique x operand name -->
  <xsl:variable name="xOperandUnique">
	<xsl:value-of select="cmd:TgetReusableUnique('xop')"/>
  </xsl:variable>

  <!-- yop: create unique y operator name -->
  <xsl:variable name="yOperandUnique">
	<xsl:value-of select="cmd:TgetReusableUnique('yop')"/>
  </xsl:variable>

  <xsl:copy>
  <xsl:copy-of select="@*"/>

  <xsl:choose>
  <xsl:when test="@isOperator and boolean($hasOperatorAsGenerator)">
	<!-- Operator synthetic function changed back to iterator over atoms -->
	<xsl:variable name="asFunction" select="cmd:TgetProperty('OperatorAsGenerator', $symbol)"/>
	<xsl:value-of select="cmd:Treplace($asFunction, '$x', $xOperandUnique, '$y', $yOperandUnique, '$op', $symbol)"/>

	<!-- Lift arguments as atoms -->
	<xsl:apply-templates select="TUPLE" mode="lift">
		<xsl:with-param name="asAtom" select="true()"/>
	</xsl:apply-templates>
  </xsl:when>
  <xsl:otherwise>

	<!-- Function invocation using closure of f(args) -->

	<xsl:value-of select="$newIconInvokeIterator"/>
	<xsl:text>(</xsl:text>
	<xsl:value-of select="$noargBeginClosure"/>

	<xsl:choose>
	  <xsl:when test="@isOperator and boolean($hasOperatorAsFunction)">
		<!-- Change function name: to(e) ==> IconOperators.to(e) -->
		<xsl:variable name="asFunction" select="cmd:TgetProperty('OperatorAsFunction', $symbol)"/>
		<xsl:value-of select="cmd:Treplace($asFunction, '$x', $xOperandUnique, '$y', $yOperandUnique, '$op', $symbol)"/>

		<!-- Lift arguments as atoms -->
		<xsl:apply-templates select="TUPLE" mode="lift">
			<xsl:with-param name="asAtom" select="boolean($hasFunctionOverAtoms)"/>
			<xsl:with-param name="isInsideClosure" select="true()"/>
		</xsl:apply-templates>
	  </xsl:when>

	  <xsl:when test="boolean($hasFunctionOverAtoms)">
		<xsl:variable name="asFunction" select="cmd:TgetProperty('FunctionOverAtoms', $functionName)"/>
		<xsl:value-of select="cmd:Treplace($asFunction, '$x', $xOperandUnique, '$y', $yOperandUnique, '$op', $symbol)"/>
		<xsl:apply-templates select="TUPLE" mode="lift">
			<xsl:with-param name="asAtom" select="true()"/>
			<xsl:with-param name="isInsideClosure" select="true()"/>
		</xsl:apply-templates>
	  </xsl:when>

	  <xsl:otherwise>

	    <!-- Method as method: bypass methodref if visible in scope -->
	    <xsl:if test="boolean($isJava)">
	      <xsl:choose>
		<xsl:when test="*[1][self::ATOM[METHODREF] or self::ATOM[IDENTIFIER][@matchesMethodName and not(@isLocal)]]">
			<!--
			# <xsl:text>(Object) </xsl:text>
			-->
		</xsl:when>
		<xsl:otherwise>
			<xsl:text>((VariadicFunction) </xsl:text>
		</xsl:otherwise>
	      </xsl:choose>
	    </xsl:if>

	    <!-- prepend shellPrefix for command -->
	    <xsl:if test="@isCommand and boolean($shellPrefix)">
		<xsl:value-of select="$shellPrefix"/>
	    </xsl:if>

	    <!-- Use function name -->
	    <xsl:apply-templates select="*[1]" mode="lift"> <!-- OBJREF|ATOM -->
		<xsl:with-param name="asLambda" select="true()"/>
		<xsl:with-param name="isInsideClosure" select="true()"/>
	    </xsl:apply-templates>

	    <!-- Method as method: bypass methodref if visible in scope -->
	    <xsl:if test="boolean($isJava) and not(*[1][self::ATOM[METHODREF] or self::ATOM[IDENTIFIER][@matchesMethodName and not(@isLocal)]])">
		<xsl:text>).apply</xsl:text>
	    </xsl:if>

	    <xsl:apply-templates select="TUPLE" mode="lift">
		<xsl:with-param name="isInsideClosure" select="true()"/>
	    </xsl:apply-templates>

	  </xsl:otherwise>
	</xsl:choose>

	<xsl:value-of select="$noargEndClosure"/>
	<xsl:text>)</xsl:text>

    <!--
    #====
    # If x.y::f(z): .setForceJava()
    #====
    -->
    <xsl:if test="*[1][self::ATOM[METHODREF]]">
	<xsl:text>.setForceJava()</xsl:text>
    </xsl:if>

    <!--
    #====
    # If undoable: .undoable($IconFunction[symbol])
    #====
    -->
    <xsl:if test="boolean($hasUndo)">
	<xsl:text>.undoable(</xsl:text>
	<xsl:value-of select="cmd:TgetProperty('UndoFunction', $symbol)"/>
	<xsl:text>)</xsl:text>
    </xsl:if>

  </xsl:otherwise>
  </xsl:choose>

  </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Lift mode.
#  Will lift items that need to be lifted
#	either to iterators or to atoms, depending on parameter "liftAsAtom".
#  If isJava, will further resolve primaries, beyond lifting.
#=============================================================================
-->

<!--
#====
# Lift ATOM[IDENTIFIER | DOTNAME].
#====
#   Let x' = if @isLocal or @isToClassField x_r, else x (external reference)
#   if asIterator, lift:
# 	if @isLocal or @isToClassField, then new IconSingleton(x')
# 	else LiftVariable:
#		if @symbolAsProperty, new IconVarIterator(x)
#		else if @symbolAsIterator, x
#		else new IconVarIterator(()->x, (rhs)->x=rhs)
#   else if asAtom:
# 	if @isLocal or @isToClassField, then x'
#	else LiftVariable:
#		if @symbolAsProperty, x
#		else new IconVar(()->x, (rhs)->x=rhs)
#		// even if @symbolAsIterator
#   else   // Inside primary expression, so isInsideClosure, e.g., ()->f(x)
# 	if @isLocal or @isToGlobal, then x_r.deref()
#	else if @symbolAsProperty, x.deref()	// NOT: get()
#	else if asLambda && @isToClass && (! isToThisClass) && (isJava)
#		then x.x static constructor
# 	else x		// beginClosure will pull in required isJava code
#====
-->
<xsl:template match="ATOM[IDENTIFIER | DOTNAME]" mode="lift" priority="2">
  <xsl:param name="asIterator"/>	<!-- Can have only one of as* -->
  <xsl:param name="asAtom"/>
  <xsl:param name="asLambda"/>
  <xsl:param name="isInsideClosure"/>	<!-- Additional modifier -->

  <!-- Must translate delimiters to text, e.g. for [1,2].x -->
  <xsl:variable name="varNameRTF">
	<xsl:apply-templates select="." mode="toText"/>
  </xsl:variable>
  <xsl:variable name="varName">
	<xsl:value-of select="$varNameRTF"/>
  </xsl:variable>

  <!--
  #====
  # Let x' = if @isLocal or @isToClassField x_r, else x
  #====
  -->
  <xsl:variable name="reifiedVarNameRTF">
    <xsl:choose>
      <xsl:when test="@isLocal or @isToClassField">
	<!-- deriveReifiedVarName -->
	<xsl:value-of select="cmd:TgetSameUnique($varName, '_r')"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="$varName"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="reifiedVarName">
	<xsl:value-of select="$reifiedVarNameRTF"/>
  </xsl:variable>

  <xsl:copy>
  <xsl:copy-of select="@*"/>

  <xsl:if test="not(boolean($asIterator) or boolean($asAtom))">
	<xsl:apply-templates select="." mode="castBegin"/>
  </xsl:if>

  <xsl:choose>
    <!--
    #====
    # if asIterator:
    #   if @isLocal or @isToClassField, then new IconSingleton(x')
    #   else new IconVarIterator(()->x, (rhs)->x=rhs)
    #====
    -->
    <xsl:when test="boolean($asIterator)">
      <xsl:choose>
	<xsl:when test="@isLocal or @isToClassField">
	    <xsl:value-of select="cmd:Treplace('$constructor($var)','$var',$reifiedVarName,'$constructor',$newIconSingleton)"/>
	</xsl:when>
	<xsl:otherwise>
	    <xsl:apply-templates select="." mode="liftVariable">
		<xsl:with-param name="varName" select="$varName"/>
		<xsl:with-param name="asIterator" select="$asIterator"/>
	    </xsl:apply-templates>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <!--
    #====
    # if asAtom:
    #   if @isLocal or @isToClassField, then x'
    #   else new IconVar(()->x, (rhs)->x=rhs)
    #====
    -->
    <xsl:when test="boolean($asAtom)">
      <xsl:choose>
	<xsl:when test="@isLocal or @isToClassField">
		<xsl:value-of select="$reifiedVarName"/>
	</xsl:when>
	<xsl:otherwise>
	    <xsl:apply-templates select="." mode="liftVariable">
		<xsl:with-param name="varName" select="$varName"/>
		<xsl:with-param name="asIterator" select="$asIterator"/>
	    </xsl:apply-templates>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <!--
    #====
    # otherwise: Inside primary expression, so isInsideClosure, e.g., ()->f(x)
    #   if @isLocal or @isToGlobal, then x_r.deref()
    #	else if @symbolAsProperty, x.deref()	// NOT: get()
    #	else if asLambda && @isToClass && (! isToThisClass) && (isJava)
    #		then x.x static constructor
    #   else x
    #====
    -->
    <xsl:when test="@isLocal or @isToGlobal">
	<xsl:value-of select="$reifiedVarName"/>
	<xsl:text>.deref()</xsl:text>
    </xsl:when>
    <xsl:when test="@symbolAsProperty">
	<xsl:copy-of select="node()"/>
	<xsl:text>.deref()</xsl:text>
    </xsl:when>
    <xsl:when test="boolean($asLambda) and @isToClass and not(@isToThisClass) and boolean($isJava)">
		<!-- not(boolean($isInterpretive)) -->
	<xsl:copy-of select="node()"/>
	<xsl:text>.</xsl:text>
	<xsl:copy-of select="node()"/>
    </xsl:when>
    <xsl:otherwise>
	<!-- <xsl:value-of select="$varName"/> -->
	<xsl:copy-of select="node()"/>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:if test="not(boolean($asIterator) or boolean($asAtom))">
	<xsl:apply-templates select="." mode="castEnd"/>
  </xsl:if>

  </xsl:copy>
</xsl:template>

<!--
#====
# Lift OBJREF.  o.x.y | o.deref().x.y where o is temporary
#====
#   Let objref' = if first o in o.x.y is @isLocal or @isToGlobal,
#		then o_r.deref().x.y,
#	 else leave alone
#   Let o' = o asAtom = [o]_RR = if o @isLocal or @isToClassField "o_r"
#	else if isInsideClosure "o"
#	else IconVar.create (()->o)
#			// Strip DEREF tag after o, IconField does deref() on o
#			// where deref() for a temporary does get().get()
#   if asIterator, new IconFieldIterator(o',"x","y")
#   else if asAtom,
#	if objref==o.deref()  // Temporary
#	     if isInsideClosure: o_r.getAtom()	[for promote(args)]
#	     else o_r				[for index list setter]
#	else if isInsideClosure new IconField(o', "x", "y")
#	     else: new IconField(o', "x", "y")
#   else // isInsideClosure
#	if isJava && ! o is typed && ! objref==o.deref(),
#	     IconField.getFieldValue(o',"x","y")
#	else objref' (leave alone)
#====
# Relation to concretization transforms in TR paper:
#   asIterator = [!x]_K (lift primary)
#   asAtom     = [x]_R (reified primary)
#   otherwise  = [x]_K (inside primary)
#   (asLambda is absorbed into invoke transform cases).
#====
# TAGS OBJREF[@deref]: will be of form   o.deref().x.y | o.deref()
# TAGS INVOKE: INVOKE/{ATOM TUPLE/EXPRESSION/OBJREF{ATOM DEREF} }
# TAGS INDEX:  INDEX/{OBJREF|GROUP|ATOM SUBSCRIPT}
#====
-->
<xsl:template match="OBJREF" mode="lift" priority="2">
  <xsl:param name="asIterator"/>	<!-- Can have only one of as* -->
  <xsl:param name="asAtom"/>
  <xsl:param name="asLambda"/>
  <xsl:param name="isInsideClosure"/>	<!-- Additional modifier -->

  <!-- Object name, i.e., first field in object reference, without DEREF -->
  <xsl:variable name="varName" select="*[1]"/>
  <xsl:variable name="varNameTextRTF">
	<xsl:apply-templates select="$varName" mode="toText"/>
  </xsl:variable>
  <xsl:variable name="varNameText">
	<xsl:value-of select="$varNameTextRTF"/>
  </xsl:variable>

  <!-- Object reference deref: if first field @islocal, ensure has deref() -->
  <xsl:variable name="objrefDerefRTF">
	<xsl:apply-templates select="." mode="derefFirstField"/>
  </xsl:variable>

  <!-- Object reference as text -->
  <xsl:variable name="objrefTextRTF">
	<!-- Must translate delimiters to text, e.g. for [1,2].x -->
	<xsl:apply-templates select="xalan:nodeset($objrefDerefRTF)/*[1]" mode="toText"/>
  </xsl:variable>
  <xsl:variable name="objrefText">
	<xsl:value-of select="$objrefTextRTF"/>
  </xsl:variable>

  <!--
  #====
  # Reified name: if @isLocal or @isToClassField o_r, else ()->o
  # Let o' = if @isLocal or @isToClassField "o_r"
  #   else if isInsideClosure "o"
  #   else IconVar.create( ()->o )
  #====
  -->
  <xsl:variable name="reifiedVarNameRTF">
    <xsl:choose>
      <xsl:when test="$varName[@isLocal or @isToClassField]">
	<!-- deriveReifiedVarName -->
	<xsl:value-of select="cmd:TgetSameUnique($varNameText, '_r')"/>
      </xsl:when>
      <xsl:when test="boolean($isInsideClosure)">
	<xsl:value-of select="$varNameText"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="$newAsAtomCreate"/>
	<xsl:text>(</xsl:text>
	<xsl:value-of select="$noargBeginClosure"/>
	<xsl:value-of select="$varNameText"/>
	<xsl:value-of select="$noargEndClosure"/>
	<xsl:text>)</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="reifiedVarName">
	<xsl:value-of select="$reifiedVarNameRTF"/>
  </xsl:variable>

  <xsl:copy>
  <xsl:copy-of select="@*"/>

  <xsl:if test="not(boolean($asIterator) or boolean($asAtom))">
	<xsl:apply-templates select="." mode="castBegin"/>
  </xsl:if>

  <xsl:choose>
    <!--
    #====
    # if (asAtom && (objref==o.deref()))
    #	if isInsideClosure o_r.getAtom()  [for promote(args)]
    #	else o_r  [for index]
    # else if (asIterator || asAtom
    #		|| (isJava && !typed && !objref==o.deref()))
    #     if asIterator, new IconFieldIterator(o',"x","y")
    #     else if asAtom, new IconField(o',"x","y")
    #     else IconField.getFieldValue(o',"x","y")
    # else objref' (leave alone)
    #====
    -->
    <xsl:when test="boolean($asAtom) and *[self::DEREF and position()=last()]">
	  <!-- Know this is a temporary, so must be reified. -->
	  <!-- <xsl:value-of select="$reifiedVarName"/> -->
	  <!-- deriveReifiedVarName -->
	  <xsl:value-of select="cmd:TgetSameUnique($varNameText, '_r')"/>
	  <xsl:if test="boolean($isInsideClosure)">
		<xsl:text>.getAtom()</xsl:text>
	  </xsl:if>
    </xsl:when>
    <xsl:when test="boolean($asIterator) or boolean($asAtom) or (boolean($isJava) and not($varName[@type]) and not(*[self::DEREF and position()=last()]))">
      <xsl:choose>
	<xsl:when test="boolean($asIterator)">
	  <xsl:value-of select="$newIconFieldIterator"/>
	  <xsl:text>(</xsl:text>
	</xsl:when>
	<xsl:when test="boolean($asAtom)">
	  <xsl:value-of select="$newIconField"/>
	  <xsl:text>(</xsl:text>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:value-of select="$newIconFieldValue"/>
	  <xsl:text>(</xsl:text>
	</xsl:otherwise>
      </xsl:choose>

      <xsl:value-of select="$reifiedVarName"/>
      <xsl:text>, </xsl:text>
      <xsl:apply-templates select="*[not(self::DELIMITER or self::DEREF or (position()=1))]" mode="extractToList">
	  <xsl:with-param name="prefix" select="'&quot;'"/>
	  <xsl:with-param name="suffix" select="'&quot;'"/>
      </xsl:apply-templates>
      <xsl:text>)</xsl:text>
    </xsl:when>
    <!--
    #====
    # otherwise: leave alone (objref)
    #====
    -->
    <xsl:otherwise>
	<xsl:value-of select="$objrefText"/>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:if test="not(boolean($asIterator) or boolean($asAtom))">
	<xsl:apply-templates select="." mode="castEnd"/>
  </xsl:if>

  </xsl:copy>
</xsl:template>

<!--
#====
# Deref first field of OBJREF or DOTNAME, if @isLocal and not already done so.
# Always change first field to reified variable name, if @isLocal. 
# For METHODREF, need to parenthesize even if count=1, since :: is changed to .
# TAGS: OBJREF/ATOM[@isLocal]/IDENTIFIER
#	DOTNAME/ATOM/IDENTIFIER (for METHODREF/DOTNAME as ATOM)
#====
-->
<xsl:template match="*" mode="derefFirstField">
  <!-- Force parenthesis if cast -->
  <xsl:param name="forceParenIfCast" select="false()"/>

  <xsl:copy>
  <xsl:copy-of select="@*"/>

    <!-- Cast first field, plus its deref -->
    <xsl:apply-templates select="*[1]" mode="castBegin">
	<xsl:with-param name="withParen" select="(boolean($forceParenIfCast) and *[1][@cast]) or ((self::*[DEREF]) and (count(*) > 2)) or (not(self::*[DEREF]) and (count(*) > 1))"/>
    </xsl:apply-templates>

    <!-- Object reference deref: if first field @islocal, ensure has deref() -->
    <xsl:choose>
      <xsl:when test="*[1][@isLocal or @isToGlobal]">
	<xsl:apply-templates select="*[1]" mode="deriveReifiedVarName">
		<xsl:with-param name="doReified" select="true()"/>
	</xsl:apply-templates>
	<xsl:if test="not(self::*[DEREF])">
	  <DEREF isderef="true">
	    <DELIMITER ID="." isderef="true"/>
	    <ATOM isderef="true()">
		<xsl:text>deref()</xsl:text>
	    </ATOM>
	  </DEREF>
	</xsl:if>
      </xsl:when>
      <xsl:otherwise>
	<xsl:copy-of select="*[1]"/>
      </xsl:otherwise>
    </xsl:choose>

    <!-- Process any deref, and end cast -->
    <xsl:if test="self::*[DEREF]">
	<xsl:copy-of select="DEREF"/>
    </xsl:if>
    <xsl:apply-templates select="*[1]" mode="castEnd">
	<xsl:with-param name="withParen" select="(boolean($forceParenIfCast) and *[1][@cast]) or ((self::*[DEREF]) and (count(*) > 2)) or (not(self::*[DEREF]) and (count(*) > 1))"/>
    </xsl:apply-templates>

    <!-- Copy rest after first field and any deref -->
    <xsl:copy-of select="*[position() &gt; 1][not(self::DEREF)]"/>

  </xsl:copy>
</xsl:template>

<!--
#====
# Lift variable asAtom or asIterator.
#   if @symbolAsProperty: if asIterator, new IconVarIterator<Type>(x), else x
#   else if @symbolAsIterator && asIterator: x,
#   else: new IconVar<Type> | IconVarIterator<Type> (()->x, (rhs)->x=rhs)
#   if $isMethodLocal, then append: .local()
#   if ! $isJava, use IconRef/IconRefIterator instead of IconVar
#	Alternatively, could use introduce interface into IconVar using Groovy:
#	new IconVar<Type> (()->x, (rhs)->(x=rhs) as Consumer)
# Lifting will reify a variable or value to a property (called an atom),
#   and then promote it to an iterator.
#====
-->
<xsl:template match="*" mode="liftVariable">
  <xsl:param name="varName" select="''"/>
  <xsl:param name="type" select="''"/>
  <xsl:param name="asIterator" select="false()"/>
  <xsl:param name="appendLocal" select="false()"/>

  <!-- rhs: create unique rhs variable name for closures -->
  <xsl:variable name="rhsUnique">
	<xsl:value-of select="cmd:TgetReusableUnique('rhs')"/>
  </xsl:variable>

  <!-- as Consumer to use if using IconRef instead of IconVar for Groovy -->
  <xsl:variable name="asConsumerRTF">
      <xsl:if test="not(boolean($isJava))">
	<!-- as Consumer, if just use IconVar above
	<xsl:text> as Consumer</xsl:text>
	-->
      </xsl:if>
  </xsl:variable>
  <xsl:variable name="asConsumer">
	<xsl:value-of select="$asConsumerRTF"/>
  </xsl:variable>

  <!--
  #====
  # if @symbolAsProperty: if asIterator, new IconVarIterator<Type>(x), else x
  # else if @symbolAsIterator && asIterator: x
  # else: new IconVar<Type> | IconVarIterator<Type> (()->x, (rhs)->x=rhs)
  #====
  -->
  <xsl:choose>
    <xsl:when test="@symbolAsProperty">
      <xsl:choose>
	<xsl:when test="boolean($asIterator)">
	  <xsl:value-of select="cmd:Treplace('$constructor$type($var)','$constructor',$newAsAtomIterator,'$type',$type,'$var',$varName)"/>
	</xsl:when>
	<xsl:otherwise>
	    <xsl:copy-of select="node()"/>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:when test="@symbolAsIterator and boolean($asIterator)">
	    <xsl:copy-of select="node()"/>
    </xsl:when>
    <xsl:otherwise>
      <!-- IconVar or IconVarIterator, possibly in Java -->
      <xsl:choose>
	<xsl:when test="boolean($asIterator)">
	<xsl:value-of select="cmd:Treplace('$constructor$type($noargBeginCl$var$noargEndCl, $beginCl$rhs$bodyCl$var=$rhs$endCl$asConsumer)','$constructor',$newIconVarIterator,'$type',$type,'$noargBeginCl',$noargBeginClosure,'$noargEndCl',$noargEndClosure,'$beginCl',$beginClosure,'$bodyCl',$bodyClosure,'$endCl',$endClosure,'$asConsumer',$asConsumer,'$var',$varName,'$rhs',$rhsUnique)"/>
	</xsl:when>
	<xsl:otherwise>
	<xsl:value-of select="cmd:Treplace('$constructor$type($noargBeginCl$var$noargEndCl, $beginCl$rhs$bodyCl$var=$rhs$endCl$asConsumer)','$constructor',$newIconVar,'$type',$type,'$noargBeginCl',$noargBeginClosure,'$noargEndCl',$noargEndClosure,'$beginCl',$beginClosure,'$bodyCl',$bodyClosure,'$endCl',$endClosure,'$asConsumer',$asConsumer,'$var',$varName,'$rhs',$rhsUnique)"/>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:if test="boolean($appendLocal)">
	<!-- IconVar().local() if @isLocal -->
	<xsl:text>.local()</xsl:text>
  </xsl:if>

</xsl:template>

<!--
#====
# Lift literal.
#   For numeric literals, arbitrary precision handling is done in postprocessing
# LITERAL:
#   if asIterator new IconValueIterator(c)
#   else if (asAtom) IconValue.create(c)	WAS: {-> c}
#   else leave alone
#====
-->
<xsl:template match="ATOM[LITERAL]" mode="lift" priority="2">
  <xsl:param name="asIterator"/>	<!-- Can have only one of as* -->
  <xsl:param name="asAtom"/>
  <xsl:param name="asLambda"/>		<!-- Ignored -->
  <xsl:param name="isInsideClosure"/>	<!-- Additional modifier -->

  <xsl:copy>
  <xsl:copy-of select="@*"/>

  <xsl:if test="not(boolean($asIterator) or boolean($asAtom))">
	<xsl:apply-templates select="." mode="castBegin"/>
  </xsl:if>

  <xsl:choose>
    <!-- Change IconValueIterator(null) to IconNullIterator() -->
    <xsl:when test="boolean($asIterator) and LITERAL[string()='null']">
	<xsl:value-of select="$newIconNullIterator"/>
	<xsl:text>()</xsl:text>
    </xsl:when>
    <xsl:when test="boolean($asIterator)">
	<xsl:value-of select="$newIconValueIterator"/>
	<xsl:text>(</xsl:text>
		<!-- <xsl:copy-of select="."/> -->
		<xsl:apply-templates select="." mode="setNoPrecision"/>
		<xsl:if test="LITERAL[@isRadix]">
			<xsl:text>, -1</xsl:text>
		</xsl:if>
	<xsl:text>)</xsl:text>
    </xsl:when>
    <xsl:when test="boolean($asAtom)">
	<xsl:value-of select="$newIconValueCreate"/>
	<xsl:text>(</xsl:text>
	<!-- <xsl:value-of select="$noargBeginClosure"/> -->
	<!-- <xsl:copy-of select="."/> -->
		<xsl:apply-templates select="." mode="setNoPrecision"/>
		<xsl:if test="LITERAL[@isRadix]">
			<xsl:text>, -1</xsl:text>
		</xsl:if>
	<!-- <xsl:value-of select="$noargEndClosure"/> -->
	<xsl:text>)</xsl:text>
    </xsl:when>
    <xsl:otherwise>
	<xsl:copy-of select="."/>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:if test="not(boolean($asIterator) or boolean($asAtom))">
	<xsl:apply-templates select="." mode="castEnd"/>
  </xsl:if>

  </xsl:copy>
</xsl:template>

<xsl:template match="ATOM[LITERAL]" mode="setNoPrecision" priority="2">
  <xsl:copy>
  <xsl:copy-of select="@*"/>
    <LITERAL noPrecision="true()">
	<xsl:copy-of select="LITERAL/@*"/>
	<xsl:copy-of select="LITERAL/node()"/>
    </LITERAL>
  </xsl:copy>
</xsl:template>

<!--
#====
# Lift collection literal: immutable so cannot set.
# GROUP: similar to literal or methodref, except must apply resolve inside it.
#   if asIterator, IconVarIterator.createAsList/Set/Map( ()->{below} )
#   else if (asAtom), if !insideClosure,
#			   IconVar.createAsList/Set/Map( ()->{below} )
#		      else IconValue.create( below )
#   else below	// Inside primary expression, so isInsideClosure
#   Below: if isJava, new IconList/IconMap/IconSet(lift:args)
#	else [lift:args]  // with no params for lift, except isInsideClosure
#   Simple collection is just literals or is empty.
#   Watch out. Normalization changed set to use [] instead of {}.
#   Watch out. Must always return a *new* list, so must have closure build it.
# TAGS LIST: <GROUP> <LIST> <DELIMITER ID="["/> <EXPRESSION>
# TAGS MAP:  <GROUP> <MAP> [ <KEYVALUE> <EXPRESSION> <EXPRESSION> ]
#====
-->
<xsl:template match="GROUP[LIST | MAP | SET]" mode="lift" priority="2">
  <xsl:param name="asIterator"/>
  <xsl:param name="asAtom"/>
  <xsl:param name="asLambda"/>
  <xsl:param name="isInsideClosure"/>

  <xsl:copy>
  <xsl:copy-of select="@*"/>

  <!-- Simple collection is just literals or is empty. -->
  <!--
  <xsl:variable name="isComplexCollection" select="not(*[@isEmpty]) and (*/EXPRESSION/*[not(self::ATOM[LITERAL])] or MAP/KEYVALUE/EXPRESSION/*[not(self::ATOM[LITERAL])])"/>
  -->

  <xsl:variable name="netInsideClosure" select="boolean($isInsideClosure) or boolean($asIterator) or boolean($asAtom)"/>

  <xsl:variable name="collectionTypeRTF">
	<xsl:choose>
	    <xsl:when test="LIST">
		<xsl:text>AsList</xsl:text>
	    </xsl:when>
	    <xsl:when test="MAP">
		<xsl:text>AsMap</xsl:text>
	    </xsl:when>
	    <xsl:when test="SET">
		<xsl:text>AsSet</xsl:text>
	    </xsl:when>
	</xsl:choose>
  </xsl:variable>
  <xsl:variable name="collectionType">
	<xsl:value-of select="$collectionTypeRTF"/>
  </xsl:variable>

  <xsl:if test="not(boolean($asIterator) or boolean($asAtom))">
	<xsl:apply-templates select="." mode="castBegin"/>
  </xsl:if>

  <xsl:if test="boolean($asIterator)">
	<xsl:value-of select="$newAsAtomIterator"/>
	<xsl:value-of select="$collectionType"/>
	<xsl:text>(</xsl:text>
	<xsl:value-of select="$noargBeginClosure"/>
  </xsl:if>

  <xsl:if test="boolean($asAtom)">
    <xsl:choose>
    <xsl:when test="not(boolean($isInsideClosure))">
	<xsl:value-of select="$newAsAtomPrefix"/>
	<xsl:value-of select="$collectionType"/>
	<xsl:text>(</xsl:text>
	<xsl:value-of select="$noargBeginClosure"/>
    </xsl:when>
    <xsl:otherwise>
	<xsl:value-of select="$newIconValueCreate"/>
	<xsl:text>(</xsl:text>
    </xsl:otherwise>
    </xsl:choose>
  </xsl:if>

  <xsl:choose>
    <!-- Groovy map complains on IconInteger(1):IconInteger(2) -->
    <xsl:when test="boolean($isJava) or MAP or SET">
      <xsl:choose>
	<xsl:when test="LIST">
	    <xsl:value-of select="$newIconList"/>
	    <xsl:text>(</xsl:text>
	    <LIST>
	      <xsl:apply-templates select="*/*[not(self::DELIMITER[@ID='[' or @ID=']'])]" mode="lift">
	      <xsl:with-param name="isInsideClosure" select="$netInsideClosure"/>
	      </xsl:apply-templates>
	    </LIST>
	    <xsl:text>)</xsl:text>
	</xsl:when>
	<xsl:when test="SET">
	    <xsl:value-of select="$newIconSet"/>
	    <xsl:text>(</xsl:text>
	    <!-- Watch out. Normalization changed set to use [] instead of {}-->
	    <SET>
	      <xsl:apply-templates select="*/*[not(self::DELIMITER[@ID='[' or @ID=']'])]" mode="lift">
	      <xsl:with-param name="isInsideClosure" select="$netInsideClosure"/>
	      </xsl:apply-templates>
	    </SET>
	    <xsl:text>)</xsl:text>
	</xsl:when>
	<xsl:when test="MAP">
	    <!-- lift args and reformat from [a:b,c:d] to (a,b,c,d) -->
	    <xsl:variable name="mapargs">
	      <ARGS>
	      <xsl:apply-templates select="*/KEYVALUE/EXPRESSION" mode="lift">
		<xsl:with-param name="isInsideClosure" select="$netInsideClosure"/>
	      </xsl:apply-templates>
	      </ARGS>
	    </xsl:variable>

	    <xsl:value-of select="$newIconMap"/>
	    <xsl:text>(</xsl:text>
	    <MAP>
	      <xsl:apply-templates select="xalan:nodeset($mapargs)/ARGS/*" mode="extractToList"/>
	    </MAP>
	    <xsl:text>)</xsl:text>
	</xsl:when>
      </xsl:choose>
    </xsl:when>
    <!-- Otherwise: pass collection literal directly to Groovy -->
    <xsl:otherwise>
      <xsl:apply-templates select="*" mode="lift">
	    <xsl:with-param name="isInsideClosure" select="$netInsideClosure"/>
      </xsl:apply-templates>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:if test="boolean($asIterator)">
	<xsl:value-of select="$noargEndClosure"/>
	<xsl:text>)</xsl:text>
  </xsl:if>
  <xsl:if test="boolean($asAtom)">
	<xsl:if test="not(boolean($isInsideClosure))">
	    <xsl:value-of select="$noargEndClosure"/>
	</xsl:if>
	<xsl:text>)</xsl:text>
  </xsl:if>

  <xsl:if test="not(boolean($asIterator) or boolean($asAtom))">
	<xsl:apply-templates select="." mode="castEnd"/>
  </xsl:if>

  </xsl:copy>
</xsl:template>

<!--
#====
# Lift methodref o.x.y::f
#   Let methodref' = if first o in o.x.y is @isLocal, then o_r.deref().x.y::f
#       ::x => x::x
#	if (! (isJava && !isLambda)) Substitute . for ::
#   if asIterator, new IconFieldIterator(new IconVar(()->methodref')) // freeze
#   else if (asAtom) 	// for index, should never happen
#	IconVar.create( ()->methodref' )
#   else methodref'	// Inside primary expression, so isInsideClosure
# Also called package reference in Icon, Groovy: foo::C => foo.C.C  ::C => C.C
# METHODREF:
#   We assume that any methodref has typed first field, so do not need to alter.
#   Treats like an unmodifiable variable.  Will leave alone.
#   However, we have to deref() the first term if is method local, like OBJREF.
#====
# o.x.y::f is a Java method reference, only needed for Java translation.
# The first field "o" must be explicitly typed and visible in the class or
# method, so that Java can infer the method from the object reference type.
# Otherwise, if o::f is not explicitly typed and visible in the class or method,
# would have to scope up for type resolution through superclasses and imports.
# An expression e::f is not allowed as this would require reflection for invoke,
# since e will transform to an untyped bound iterator.
# Thus, We treat a methodref like a dotname, that has no expressions in it.
#====
# Tags METHODREF: ATOM/METHODREF/
#		  DOTNAME/{ATOM[IDENTIFIER] (.ATOM[IDENTIFIER])*} :: IDENTIFIER
# TAGS allEmpty: ATOM[@allEmpty]/<LITERAL null>
#====
-->
<xsl:template match="ATOM[METHODREF]" mode="lift" priority="2">
  <xsl:param name="asIterator"/>
  <xsl:param name="asAtom"/>
  <xsl:param name="asLambda"/>
  <xsl:param name="isInsideClosure"/>

  <!-- Method reference: if first field @islocal, ensure has deref() -->
  <xsl:variable name="methodrefDerefRTF">
      <METHODREF>
	<xsl:choose>
	<!--
	#====
	# ::x => x::x
	#====
	-->
	<xsl:when test="METHODREF/DOTNAME/*[@allEmpty]">
	  <DOTNAME>
		<xsl:copy-of select="METHODREF/*[position() = last()]"/>
	  </DOTNAME>
	  <xsl:copy-of select="METHODREF/*[position() &gt; 1]"/>
	</xsl:when>
	<!--
	#====
	# o.x.y::f => o_r.deref().x.y::f if o is @isLocal
	#====
	-->
	<xsl:otherwise>
	  <xsl:apply-templates select="METHODREF/DOTNAME" mode="derefFirstField">
		<xsl:with-param name="forceParenIfCast" select="true()"/>
	  </xsl:apply-templates>
	  <xsl:copy-of select="METHODREF/*[position() &gt; 1]"/>
	</xsl:otherwise>
	</xsl:choose>
      </METHODREF>
  </xsl:variable>
  <xsl:variable name="methodrefTextRTF">
	<!-- Must translate delimiters to text, e.g. for [1,2].x -->
	<xsl:apply-templates select="xalan:nodeset($methodrefDerefRTF)/*[1]" mode="toText">
		<xsl:with-param name="castSpace" select="true()"/>
	</xsl:apply-templates>
  </xsl:variable>
  <xsl:variable name="methodrefTextDotRTF">
	<xsl:choose> 
	  <xsl:when test="boolean($isJava) and not(boolean($asLambda))">
	    <xsl:value-of select="$methodrefTextRTF"/>
	  </xsl:when>
	  <xsl:otherwise>
	    <xsl:value-of select="cmd:Treplace($methodrefTextRTF, '::', '.')"/>
	  </xsl:otherwise>
	</xsl:choose>
  </xsl:variable>
  <xsl:variable name="methodrefText">
	<xsl:value-of select="$methodrefTextDotRTF"/>
  </xsl:variable>

  <xsl:copy>
  <xsl:copy-of select="@*"/>

  <xsl:if test="not(boolean($asIterator) or boolean($asAtom))">
	<xsl:apply-templates select="." mode="castBegin"/>
  </xsl:if>

  <METHODREF>

  <xsl:choose>
    <xsl:when test="boolean($asIterator)">
	<xsl:value-of select="$newIconFieldIterator"/>
	<xsl:text>(</xsl:text>
	  <xsl:value-of select="$newAsAtomCreate"/>
	  <xsl:text>(</xsl:text>
		<xsl:value-of select="$noargBeginClosure"/>
		<xsl:value-of select="$methodrefText"/>
		<xsl:value-of select="$noargEndClosure"/>
	<xsl:text>))</xsl:text>
    </xsl:when>
    <xsl:when test="boolean($asAtom)">
	<xsl:value-of select="$newAsAtomCreate"/>
	<xsl:text>(</xsl:text>
	<xsl:value-of select="$noargBeginClosure"/>
	<xsl:value-of select="$methodrefText"/>
	<xsl:value-of select="$noargEndClosure"/>
	<xsl:text>)</xsl:text>
    </xsl:when>
    <xsl:otherwise>
	<xsl:value-of select="$methodrefText"/>
    </xsl:otherwise>
  </xsl:choose>

  </METHODREF>

  <xsl:if test="not(boolean($asIterator) or boolean($asAtom))">
	<xsl:apply-templates select="." mode="castEnd"/>
  </xsl:if>

  </xsl:copy>

</xsl:template>

<!--
#=============================================================================
# Cast begin and end.
#=============================================================================
-->

<!--
#====
# Cast begin, if @cast.  Adds extra parenthesis around cast: ((Cast) expr).
# Applies to: OBJREF, ATOM, LITERAL, GROUP.
#	OBJREF whole can be cast, or first field with or without deref.
#	On OBJREF, must wrap cast around (x.deref)
#====
-->
<xsl:template match="*" mode="castBegin">
  <xsl:param name="withParen"/>
  <xsl:if test="@cast">
	<xsl:if test="boolean($withParen)">
	  <DELIMITER ID="("/>
	</xsl:if>
	<CAST>
	  <DELIMITER ID="("/>
	  <xsl:value-of select="@cast"/>
	  <DELIMITER ID=")"/>
	  <xsl:text> </xsl:text>
	</CAST>
  </xsl:if>
</xsl:template>

<!--
#====
# Cast end, if @cast.  Adds extra parenthesis around cast: ((Cast) expr).
#====
-->
<xsl:template match="*" mode="castEnd">
  <xsl:param name="withParen"/>
  <xsl:if test="@cast and boolean($withParen)">
	<DELIMITER ID=")"/>
  </xsl:if>
</xsl:template>

<!--
#=============================================================================
# Default lift template.
#=============================================================================
-->

<!--
#====
# Default template to copy nodes through.
#====
-->
<xsl:template match="@*|node()" mode="lift">
  <xsl:param name="asIterator"/>	<!-- Can have only one of as* -->
  <xsl:param name="asAtom"/>
  <xsl:param name="asLambda"/>
  <xsl:param name="isInsideClosure"/>	<!-- Additional modifier -->
  <xsl:copy>
      <xsl:apply-templates select="@*|node()" mode="lift">
	<xsl:with-param name="asIterator" select="$asIterator"/>
	<xsl:with-param name="asAtom" select="$asAtom"/>
	<xsl:with-param name="asLambda" select="$asLambda"/>
	<xsl:with-param name="isInsideClosure" select="$isInsideClosure"/>
      </xsl:apply-templates>
  </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Extract into list x,y by inserting commas between elements.
#=============================================================================
-->
<xsl:template match="node()" mode="extractToList" priority="2">
	<xsl:param name="delimiter" select="','"/>
	<xsl:param name="prefix"/>
	<xsl:param name="suffix"/>
	<!-- prepend comma if not first -->
	<xsl:if test="position() > 1">
		<DELIMITER ID="{$delimiter}"/>
	</xsl:if>
	<xsl:if test="boolean($prefix)">
		<xsl:value-of select="$prefix"/>
	</xsl:if>
	<xsl:copy-of select="."/>
	<xsl:if test="boolean($suffix)">
		<xsl:value-of select="$suffix"/>
	</xsl:if>
</xsl:template>

<!-- ==== Default template to pass through -->
<xsl:template match="@*|node()" mode="extractToList">
	<xsl:apply-templates select="@*|node()" mode="extractToList"/>
</xsl:template>

<!--
#=============================================================================
# Extract into statement x;
#=============================================================================
-->
<xsl:template match="node()" mode="extractToStatement" priority="2">
	<STATEMENT>
	    <xsl:copy-of select="."/>
	    <DELIMITER ID=";"/>
	</STATEMENT>
</xsl:template>

<!-- ==== Default template to pass through -->
<xsl:template match="@*|node()" mode="extractToStatement">
	<xsl:apply-templates select="@*|node()" mode="extractToStatement"/>
</xsl:template>

<!--
#=============================================================================
# Extract to text.   Preserves text nodes, and extracts @ID from DELIMITER.
# WATCH OUT:  If needed, must first use postprocess to convert ATOM[LITERAL]>
#=============================================================================
-->
<xsl:template match="DELIMITER" mode="toText" priority="2">
	<xsl:value-of select="@ID"/>
</xsl:template>

<!-- Insert space after CAST, just for readability -->
<xsl:template match="CAST" mode="toText" priority="2">
  <xsl:param name="castSpace"/>
	<xsl:apply-templates select="@*|node()" mode="toText"/>
	<xsl:if test="boolean($castSpace)">
	    <xsl:text> </xsl:text>
	</xsl:if>
</xsl:template>

<xsl:template match="text()" mode="toText" priority="2">
	<xsl:value-of select="."/>
</xsl:template>
  
<xsl:template match="@*|node()" mode="toText">
  <xsl:param name="castSpace"/>
    <xsl:copy>
	<xsl:apply-templates select="@*|node()" mode="toText">
	    <xsl:with-param name="castSpace" select="$castSpace"/>
	</xsl:apply-templates>
    </xsl:copy>
</xsl:template>

<!--
#=============================================================================
# Closure formatting.
#    ()->f
#	noargBeginClosure: Java: "()-> " Groovy: "{-> "
#	noargEndClosure:   Java: ""	Groovy: "}"
#    ()->{f}
#	noargBeginClosureBlock: Java: " () ->"  Groovy: " -> "
#	noargEndClosureBlock:   Java: ""	Groovy: ""
#    (rhs)->x=rhs  
#	beginClosure: Java: "("		Groovy: "{"	// Expression
#	bodyClosure:  Java: ")-> "	Groovy: " -> "
#	endClosure:   Java: ""		Groovy: "}"
#    (args)->{block}
#	beginClosureBlock: Java: " ("	Groovy: ""	// Explicit block
#	bodyClosureBlock: Java: ") ->"	Groovy: " -> "	//   Java: params BLOCK
#	endClosureBlock: Java: ""	Groovy: ""	//   Groovy: BLOCK
# UseInnerClasses (if isJava):
#    ()->f
#	new Callable() { public Object call() { return // f
#	;} }
#    ()->{f}	// Just omit {}, code will insert BLOCK
#	new Callable() { public Object call()
#	;}
#    (rhs)->x=rhs  
#	new Consumer() { public void accept (Object //rhs
#	) { // x=rhs
#	;} }
#    (args)->{block}
#	new VariadicFunction() { public Object apply ( // Object... args
#	) // { block }
#	}
# def formatting: $defClosure
#	if ($isJava), "VariadicFunction "
#	else "def "  (OR: "Closure")
# def formatting: $defObject
#	if ($isJava), "Object "
#	else "def "  (OR: "Object")
#=============================================================================
-->
<xsl:variable name="noargBeginClosureRTF">
  <xsl:choose>
    <xsl:when test="boolean($isJava) and boolean($useInnerClasses)">
	<xsl:text>new Callable() { public Object call() { return </xsl:text>
    </xsl:when>
    <xsl:when test="boolean($isJava)">
	<xsl:text>()-> </xsl:text>
    </xsl:when>
    <xsl:otherwise>
	<xsl:text>{-> </xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="noargBeginClosure">
	<xsl:value-of select="$noargBeginClosureRTF"/>
</xsl:variable>

<xsl:variable name="noargEndClosureRTF">
  <xsl:choose>
    <xsl:when test="boolean($isJava) and boolean($useInnerClasses)">
	<xsl:text>;} }</xsl:text>
    </xsl:when>
    <xsl:when test="not(boolean($isJava))">
	<!--
	<xsl:text>} as Callable</xsl:text>
	-->
	<xsl:text>}</xsl:text>
    </xsl:when>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="noargEndClosure">
	<xsl:value-of select="$noargEndClosureRTF"/>
</xsl:variable>

<xsl:variable name="beginClosureRTF">
  <xsl:choose>
    <xsl:when test="boolean($isJava) and boolean($useInnerClasses)">
	<xsl:text>new Consumer() { public void accept(Object </xsl:text>
    </xsl:when>
    <xsl:when test="boolean($isJava)">
	<xsl:text>(</xsl:text>
    </xsl:when>
    <xsl:otherwise>
	<xsl:text>{</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="beginClosure">
	<xsl:value-of select="$beginClosureRTF"/>
</xsl:variable>

<xsl:variable name="bodyClosureRTF">
  <xsl:choose>
    <xsl:when test="boolean($isJava) and boolean($useInnerClasses)">
	<xsl:text>) {</xsl:text>
    </xsl:when>
    <xsl:when test="boolean($isJava)">
	<xsl:text>)-> </xsl:text>
    </xsl:when>
    <xsl:otherwise>
	<xsl:text> -> </xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="bodyClosure">
	<xsl:value-of select="$bodyClosureRTF"/>
</xsl:variable>

<xsl:variable name="endClosureRTF">
  <xsl:choose>
    <xsl:when test="boolean($isJava) and boolean($useInnerClasses)">
	<xsl:text>;} }</xsl:text>
    </xsl:when>
    <xsl:when test="not(boolean($isJava))">
	<xsl:text>}</xsl:text>
    </xsl:when>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="endClosure">
	<xsl:value-of select="$endClosureRTF"/>
</xsl:variable>

<xsl:variable name="beginClosureBlockRTF">
  <xsl:choose>
    <xsl:when test="boolean($isJava) and boolean($useInnerClasses)">
	<xsl:text> new VariadicFunction() { public Object apply(</xsl:text>
    </xsl:when>
    <xsl:when test="boolean($isJava)">
	<xsl:text> (</xsl:text>
    </xsl:when>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="beginClosureBlock">
	<xsl:value-of select="$beginClosureBlockRTF"/>
</xsl:variable>

<xsl:variable name="bodyClosureBlockRTF">
  <xsl:choose>
    <xsl:when test="boolean($isJava) and boolean($useInnerClasses)">
	<xsl:text>) </xsl:text>
    </xsl:when>
    <xsl:when test="boolean($isJava)">
	<xsl:text>) -></xsl:text>
    </xsl:when>
    <xsl:otherwise>
	<xsl:text> -> </xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="bodyClosureBlock">
	<xsl:value-of select="$bodyClosureBlockRTF"/>
</xsl:variable>

<xsl:variable name="endClosureBlockRTF">
  <xsl:choose>
    <xsl:when test="boolean($isJava) and boolean($useInnerClasses)">
	<xsl:text>}</xsl:text>
    </xsl:when>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="endClosureBlock">
	<xsl:value-of select="$endClosureBlockRTF"/>
</xsl:variable>

<xsl:variable name="noargBeginClosureBlockRTF">
  <xsl:choose>
    <xsl:when test="boolean($isJava) and boolean($useInnerClasses)">
	<xsl:text>new Callable() { public Object call()</xsl:text>
    </xsl:when>
    <xsl:when test="boolean($isJava)">
	<xsl:text> () -></xsl:text>
    </xsl:when>
    <xsl:otherwise>
	<xsl:text> -> </xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="noargBeginClosureBlock">
	<xsl:value-of select="$noargBeginClosureBlockRTF"/>
</xsl:variable>

<xsl:variable name="noargEndClosureBlockRTF">
  <xsl:choose>
    <xsl:when test="boolean($isJava) and boolean($useInnerClasses)">
	<xsl:text> }</xsl:text>
    </xsl:when>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="noargEndClosureBlock">
	<xsl:value-of select="$noargEndClosureBlockRTF"/>
</xsl:variable>

<xsl:variable name="defClosureRTF">
  <xsl:choose>
    <xsl:when test="boolean($isJava)">
	<xsl:text>VariadicFunction </xsl:text>
    </xsl:when>
    <xsl:otherwise>
	<xsl:text>def </xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="defClosure">
	<xsl:value-of select="$defClosureRTF"/>
</xsl:variable>

<xsl:variable name="defObjectRTF">
  <xsl:choose>
    <xsl:when test="boolean($isJava)">
	<xsl:text>Object </xsl:text>
    </xsl:when>
    <xsl:otherwise>
	<xsl:text>def </xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="defObject">
	<xsl:value-of select="$defObjectRTF"/>
</xsl:variable>

<xsl:variable name="defMethodRTF">
  <xsl:choose>
    <xsl:when test="boolean($isJava)">
	<xsl:text>IIconIterator </xsl:text>
    </xsl:when>
    <xsl:otherwise>
	<xsl:text>def </xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="defMethod">
	<xsl:value-of select="$defMethodRTF"/>
</xsl:variable>

<!--
#====
# Format variadic closure, depending on Java | Groovy
#   Java closure: (params)->{ block }
#   Groovy closure: { params -> block }
#   asMethodNotClosure: (params) { block }
# If asVariadicFunction, coerces if Groovy to: IconVariadicFunction(above)
# If asFunction, coerces if Groovy to: IconFunction(above)
# If isEmptyArgs, uses "()->{block}" for Java, or "->{block}" for Groovy
# Otherwise, if params is not empty, uses params instead of "Object... args"
#====
-->
<xsl:template match="*" mode="formatClosure" priority="2">
  <xsl:param name="asMethodNotClosure" select="false()"/>
  <xsl:param name="asVariadicFunction" select="false()"/>
  <xsl:param name="asFunction" select="false()"/>
  <xsl:param name="newlineAtBegin" select="true()"/>
  <xsl:param name="newlineAtEnd" select="false()"/>
  <xsl:param name="noindentEnd" select="false()"/>
  <xsl:param name="isEmptyArgs" select="false()"/>
  <!-- argsUniquename cannot be TgetReusableUnique if nested lambda -->
  <xsl:param name="argsUniquename"/>
  <xsl:param name="params"/>	<!-- PARAMS/* -->
  <xsl:param name="body"/>	<!-- BODY/* -->
  <!--
  <xsl:param name="isJava" select="false()"/>
  -->

  <xsl:choose>
    <xsl:when test="boolean($asMethodNotClosure)">
	<xsl:text> (Object... </xsl:text>
	<xsl:value-of select="$argsUniquename"/>
	<xsl:text>)</xsl:text>
    </xsl:when>

    <!-- Java closure: (Object... args) -> { body } -->
    <xsl:when test="boolean($isJava)">
      <xsl:choose>
	<xsl:when test="boolean($isEmptyArgs)">
		<xsl:value-of select="$noargBeginClosureBlock"/>
	</xsl:when>
	<xsl:otherwise>
		<xsl:value-of select="$beginClosureBlock"/>
		<xsl:choose>
		  <xsl:when test="boolean($params)">
			<xsl:copy-of select="$params/*"/>
		  </xsl:when>
		  <xsl:otherwise>
			<xsl:text>Object... </xsl:text>
			<xsl:value-of select="$argsUniquename"/>
		  </xsl:otherwise>
		</xsl:choose>
		<xsl:value-of select="$bodyClosureBlock"/>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:when test="boolean($asVariadicFunction)">
	<!-- Wrap Groovy closure as VariadicFunction that delegates to it. -->
	<xsl:text> new IconVariadicFunction(</xsl:text>
    </xsl:when>
    <xsl:when test="boolean($asFunction)">
	<!-- Wrap Groovy closure as VariadicFunction that delegates to it. -->
	<xsl:text> new IconFunction(</xsl:text>
    </xsl:when>
    <xsl:otherwise>
    </xsl:otherwise>
  </xsl:choose>

  <BLOCK>

  <!-- Groovy closure: { Object... args -> body } -->
  <xsl:choose>
    <xsl:when test="boolean($isJava) or boolean($asMethodNotClosure)">
      <xsl:choose>
	<xsl:when test="boolean($newlineAtBegin)">
	    <DELIMITER ID="{"/>
	</xsl:when>
	<xsl:otherwise>
	    <DELIMITER ID="{" nonewline="true()"/>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
	<DELIMITER ID="{" nonewline="true()"/>
	<STATEMENT noindent="true()">
	  <xsl:choose>
	    <xsl:when test="boolean($isEmptyArgs)">
		<xsl:value-of select="$noargBeginClosureBlock"/>
	    </xsl:when>
	    <xsl:otherwise>
		<xsl:value-of select="$beginClosureBlock"/>
		<xsl:choose>
		  <xsl:when test="boolean($params)">
			<xsl:copy-of select="$params/*"/>
		  </xsl:when>
		  <xsl:otherwise>
			<xsl:text>Object... </xsl:text>
			<xsl:value-of select="$argsUniquename"/>
		  </xsl:otherwise>
		</xsl:choose>
		<xsl:value-of select="$bodyClosureBlock"/>
	    </xsl:otherwise>
	  </xsl:choose>
	<xsl:if test="boolean($newlineAtBegin)">
	    <NEWLINE/>
	</xsl:if>
	</STATEMENT>
    </xsl:otherwise>
  </xsl:choose>

  <!-- FIX RIGHTHERE: below could be $body/node() -->
  <xsl:copy-of select="$body/*"/>

  <xsl:if test="not(boolean($asMethodNotClosure))">
    <xsl:choose>
      <xsl:when test="boolean($isEmptyArgs)">
	<xsl:value-of select="$noargEndClosureBlock"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="$endClosureBlock"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>

  <xsl:choose>
  <xsl:when test="boolean($newlineAtEnd) and boolean($noindentEnd)">
	<DELIMITER ID="}" noindent="true()"/>
  </xsl:when>
  <xsl:when test="boolean($newlineAtEnd)">
	<DELIMITER ID="}"/>
  </xsl:when>
  <xsl:when test="not(boolean($newlineAtEnd)) and boolean($noindentEnd)">
	<DELIMITER ID="}" nonewline="true" noindent="true()"/>
  </xsl:when>
  <xsl:otherwise>
	<DELIMITER ID="}" nonewline="true()"/>
  </xsl:otherwise>
  </xsl:choose>

  <xsl:if test="(boolean($asVariadicFunction) or boolean($asFunction)) and not(boolean($isJava) or boolean($asMethodNotClosure))">
	<xsl:text>)</xsl:text>
  </xsl:if>

  </BLOCK>

</xsl:template>

<!--
#=============================================================================
# Constructor definitions.
#=============================================================================
-->
<xsl:variable name="newIconBlock" select="'new IconBlock'"/>
<xsl:variable name="newIconNullIterator" select = "'new IconNullIterator'"/>
<xsl:variable name="newIconFail" select = "'new IconFail'"/>
<xsl:variable name="newIconReturn" select = "'new IconReturn'"/>
<xsl:variable name="newIconSequence" select = "'new IconSequence'"/>
<xsl:variable name="newIconSingleton" select = "'new IconSingleton'"/>
<xsl:variable name="newIconOperation" select = "'new IconOperation'"/>
<xsl:variable name="newIconProduct" select = "'new IconProduct'"/>
<xsl:variable name="newIconNot" select = "'new IconNot'"/>
<xsl:variable name="newIconIn" select = "'new IconIn'"/>
<xsl:variable name="newIconAssign" select = "'new IconAssign'"/>
<xsl:variable name="newIconInvokeIterator" select = "'new IconInvokeIterator'"/>
<xsl:variable name="newIconIndexIterator" select = "'new IconIndexIterator'"/>
<xsl:variable name="newIconFieldIterator" select = "'new IconFieldIterator'"/>
<xsl:variable name="newIconField" select = "'new IconField'"/>
<xsl:variable name="newIconFieldValue" select = "'IconField.getFieldValue'"/>
<xsl:variable name="newIconValueIterator" select = "'new IconValueIterator'"/>
<xsl:variable name="newIconCoExpression" select="'new IconCoExpression'"/>
<xsl:variable name="newIconList" select="'new IconList'"/>
<xsl:variable name="newIconMap" select="'new IconMap'"/>
<xsl:variable name="newIconSet" select="'new IconSet'"/>

<xsl:variable name="newIconVar" select = "concat('new ', $IconVar)"/>
<xsl:variable name="newIconVarIterator" select = "concat('new ', $IconVarIterator)"/>
<xsl:variable name="newIconTmpTmp" select = "'new IconTmp'"/>
<xsl:variable name="newIconTmpVar" select = "'new IconVar'"/>
<xsl:variable name="IconTmpTmpDecl" select = "'IconTmp'"/>
<xsl:variable name="IconTmpVarDecl" select = "'IconVar'"/>
<xsl:variable name="newIconGlobal" select = "'new IconGlobal'"/>
<xsl:variable name="IconGlobal" select = "'IconGlobal'"/>

<!-- Factory for asAtom -->
<xsl:variable name="newAsAtomCreate" select = "'new IconVar'"/>
<xsl:variable name="newAsAtomPrefix" select = "'IconVar.create'"/>
<xsl:variable name="newAsAtomList" select = "'IconVar.createAsList'"/>
<xsl:variable name="newAsAtomMap" select = "'IconVar.createAsMap'"/>
<xsl:variable name="newAsAtomSet" select = "'IconVar.createAsSet'"/>
<xsl:variable name="newAsAtomIterator" select = "'IconVarIterator.create'"/>
<xsl:variable name="newAsAtomIteratorList" select = "'IconVarIterator.createAsList'"/>
<xsl:variable name="newAsAtomIteratorMap" select = "'IconVarIterator.createAsMap'"/>
<xsl:variable name="newAsAtomIteratorSet" select = "'IconVarIterator.createAsSet'"/>
<xsl:variable name="newIconValueCreate" select = "'IconValue.create'"/>
<xsl:variable name="newIconNumberCreate" select = "'IconNumber.create'"/>

<!-- Generic control construct: IconIf -->
<xsl:variable name="newIconGenericHead" select = "'new Icon'"/>
<xsl:variable name="newIconGenericTail" select = "''"/>

<!--
#====
# Remaining new allocations:
# MethodBodyCache, ArrayList, ConcurrentHashMap, VariadicFunction, 
#	Consumer, Callable, IconVariadicFunction, IconFunction
# ALLOCATION, static main method
# return new
#====
-->

<!--
#=============================================================================
# Variable constructors that use closures for getters and setters.
# $IconVar: IconVar if Java | IconRef if Groovy.
# $IconVarIterator: IconVarIterator if Java | IconRefIterator if Groovy.
# The difference is that the Java versions have Callable instead of
# groovy Closure types.
#=============================================================================
-->
<xsl:variable name="IconVarRTF">
      <xsl:choose>
	<xsl:when test="boolean($isJava)">
		<xsl:text>IconVar</xsl:text>
	</xsl:when>
	<xsl:otherwise>
		<xsl:text>IconRef</xsl:text>
	</xsl:otherwise>
      </xsl:choose>
</xsl:variable>
<xsl:variable name="IconVar">
	<xsl:value-of select="$IconVarRTF"/>
</xsl:variable>

<xsl:variable name="IconVarIteratorRTF">
      <xsl:choose>
	<xsl:when test="boolean($isJava)">
		<xsl:text>IconVarIterator</xsl:text>
	</xsl:when>
	<xsl:otherwise>
		<xsl:text>IconRefIterator</xsl:text>
	</xsl:otherwise>
      </xsl:choose>
</xsl:variable>
<xsl:variable name="IconVarIterator">
	<xsl:value-of select="$IconVarIteratorRTF"/>
</xsl:variable>

<!--
#=============================================================================
# Default templates
#=============================================================================
-->

<!-- ==== Default template to copy nodes through -->
<xsl:template match="@*|node()">
	<xsl:copy>
		<xsl:apply-templates select="@*|node()"/>
	</xsl:copy>
</xsl:template>

</xsl:stylesheet>
