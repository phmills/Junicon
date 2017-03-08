<?xml version="1.0"?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" >

<!-- =========================================================================
  Copyright (c) 2011 Orielle, LLC.
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
	
  <!--
  #====
  # ACTION: Reconstruct concrete syntax in target languge.
  # Concrete syntax tags are:
  #	IDENTIFIER, LITERAL, KEYWORD, OPERATOR, DELIMITER.
  # Indent and newline directives:
  #	STATEMENT[@indent or @noindent]
  #	DELIMITER[@ID="{"][@nonewline]
  #	DELIMITER[@ID="}"][@nonewline or @noindent]
  #	DELIMITER[@ID=";"][@nonewline or @nodelete]
  #====
  -->
  <xsl:output method="text" indent="no"/>
  <xsl:preserve-space elements="NEWLINE"/>
  <xsl:strip-space elements="*"/>

  <!--
  #====
  # Specifies parents of text nodes that are to be
  # stripped if they contain only whitespace characters.
  #====
  <xsl:strip-space elements="*"/>
  <xsl:preserve-space elements="*"/>
  -->

  <xsl:variable name="indents" 
      select="'&#9;&#9;&#9;&#9;&#9;&#9;&#9;&#9;&#9;&#9;&#9;&#9;&#9;&#9;&#9;'" />

  <!--
  #====
  # Function to indent node according to BLOCK ancestors.
  #====
  -->
  <xsl:template match="*" mode="indent" priority="2">
	<xsl:param name="offset" select="'0'"/>
	<xsl:variable name="varspaces"
		select="substring($indents, 1, $offset + count(ancestor::BLOCK))"/>
	<xsl:value-of select="$varspaces" />
	<xsl:apply-templates select="node()"/>
  </xsl:template>

  <!--
  #====
  # Indent STATEMENT according to BLOCK ancestors, if inside BLOCK or PROGRAM
  #	and STATEMENT ends with ; or newline, or @indent, and not @noindent.
  #====
  -->
  <xsl:template match="STATEMENT[((DELIMITER[@ID=';'] or NEWLINE) or @indent) and not(@noindent) and ancestor::BLOCK]">
	<xsl:apply-templates select="." mode="indent"/>
  </xsl:template>

  <!--
  #====
  # ; inside STATEMENT => ; newline (if not @nonewline)
  #====
  -->
  <xsl:template match="DELIMITER[@ID=';' and parent::STATEMENT]">
    <xsl:choose>
    <xsl:when test="@nonewline">
	<xsl:text>; </xsl:text>
    </xsl:when>
    <xsl:otherwise>
	<xsl:text>;
</xsl:text>
    </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!--
  #====
  # NEWLINE inside STATEMENT => newline
  #====
  -->
  <xsl:template match="NEWLINE[parent::STATEMENT]">
	<xsl:text>
</xsl:text>
  </xsl:template>

  <!-- 
  #====
  # BLOCK followed by DELIMITER ID=; => delete ; if not @nodelete
  #====
  -->
  <xsl:template match="DELIMITER[@ID=';' and not(@nodelete) and preceding-sibling::*[1][self::BLOCK]]" priority="1">
  </xsl:template>
	
  <!-- 
  #====
  # { inside BLOCK => space it, and insert newline
  #====
  -->
  <xsl:template match="DELIMITER[@ID='{' and parent::BLOCK]" priority="1">
    <xsl:choose>
    <xsl:when test="@nonewline">
	<xsl:text> { </xsl:text>
    </xsl:when>
    <xsl:otherwise>
	<xsl:text> {
</xsl:text>
    </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- 
  #====
  # } inside BLOCK => insert newline, and indent it
  #====
  -->
  <xsl:template match="DELIMITER[@ID='}' and parent::BLOCK]" priority="1">
    <xsl:if test="not(@noindent)">
	<xsl:apply-templates select="." mode="indent">
		<xsl:with-param name="offset" select="'-1'"/>
	</xsl:apply-templates>
    </xsl:if>
    <xsl:choose>
    <xsl:when test="@nonewline">
	<xsl:text>}</xsl:text>
    </xsl:when>
    <xsl:otherwise>
	<xsl:text>}
</xsl:text>
    </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
	
  <!--
  #====
  # DELIMITER ID=x => x
  #====
  -->
  <xsl:template match="DELIMITER">
	<xsl:value-of select="@ID"/>
  </xsl:template>

  <!-- 
  #====
  # Insert space after ,
  #====
  -->
  <xsl:template match="DELIMITER[@ID=',']" priority="1">
	<xsl:text>, </xsl:text>
  </xsl:template>
	
  <!--
  #====
  # Insert space before and after OPERATOR, just for readability
  #====
  -->
  <xsl:template match="OPERATOR">
	<xsl:if test="preceding-sibling::*[1][not(self::OPERATOR or self::DELIMITER[@ID=',' or @ID='.'])]">
		<xsl:text> </xsl:text>
	</xsl:if>
	<xsl:value-of select="."/>
	<xsl:if test="following-sibling::*[1][not(self::DELIMITER)]">
		<xsl:text> </xsl:text>
	</xsl:if>
  </xsl:template>

  <!--
  #====
  # Insert space before and after KEYWORD
  #====
  -->
  <xsl:template match="KEYWORD" priority ="1">
	<xsl:if test="preceding-sibling::*[1][not(self::KEYWORD or self::DELIMITER[@ID=','])]">
		<xsl:text> </xsl:text>
	</xsl:if>
	<xsl:value-of select="."/>
	<xsl:text> </xsl:text>
  </xsl:template>

  <!--
  #====
  # Insert space after ATOM/IDENTIFIER if followed by another ATOM
  #====
  -->
  <xsl:template match="ATOM[IDENTIFIER][following-sibling::*[1][self::ATOM]]">
	<xsl:apply-templates select="@*"/>    <!-- print out normalized lift -->
	<xsl:value-of select="."/>
	<xsl:text> </xsl:text>
  </xsl:template>

  <!--
  #====
  # Insert space after CAST, just for readability
  #====
  -->
  <xsl:template match="CAST">
	<xsl:apply-templates select="@*|node()"/> 
	<xsl:text> </xsl:text>
  </xsl:template>

  <!-- 
  #====
  # Default template to copy text through
  #====
  -->

  <!-- Drop attributes, except lift below -->
  <xsl:template match="@*"/>
  <xsl:template match="node()">
	  <xsl:apply-templates select="@*|node()"/>
  </xsl:template>
	
  <!-- Print out text -->
  <xsl:template match="text()" priority="1">
	<xsl:value-of select="."/>
  </xsl:template>
  
  <!-- Print out lift if just normalize, so we can see where it occurs -->
  <xsl:template match="@lift" priority="2">
	<xsl:text>!</xsl:text>
  </xsl:template>

</xsl:stylesheet>
