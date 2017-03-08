<?xml version="1.0"?>
<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
	xmlns:xalan="http://xml.apache.org/xalan"
	xmlns:exslt="http://exslt.org/common"
	xmlns:cmd="edu.uidaho.junicon.support.transforms.TransformSupport"
	exclude-result-prefixes = "cmd xalan exslt">
	
	<!-- USED xalan:nodeset() instead of exslt:node-set() -->
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
	
<!--
#=============================================================================
# Print document to text, for debugging
#=============================================================================
-->
  <xsl:template match="DELIMITER" mode="printable" priority="2">
	<xsl:text> </xsl:text>
	<xsl:value-of select="@ID"/>
	<xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="text()" mode="printable" priority="1">
	<xsl:text> </xsl:text>
	<xsl:value-of select="."/>
	<xsl:text> </xsl:text>
  </xsl:template>
  
  <xsl:template match="@*" mode="printable" priority="1">
	<xsl:value-of select="local-name()"/>
	<xsl:text>=</xsl:text>
	<xsl:value-of select="."/>
	<xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="*" mode="printable" priority="1">
	<xsl:param name="showTags"/>
	<xsl:param name="showAttributes"/>
	<xsl:if test="boolean($showTags)">
		<xsl:value-of select="local-name()"/>
		<xsl:text>&lt; </xsl:text>
		<!-- <xsl:text>{ </xsl:text> -->
	</xsl:if>
	<xsl:if test="boolean($showAttributes) and @*">
		<xsl:text> @[</xsl:text>
		<xsl:apply-templates select="@*" mode="printable"/>
		<xsl:text>] </xsl:text>
	</xsl:if>
	<xsl:apply-templates select="node()" mode="printable">
		<xsl:with-param name="showTags" select="$showTags"/>
		<xsl:with-param name="showAttributes" select="$showAttributes"/>
	</xsl:apply-templates>
	<xsl:if test="boolean($showTags)">
		<xsl:text>&gt; </xsl:text>
		<!-- <xsl:text>} </xsl:text> -->
	</xsl:if>
  </xsl:template>

  <!-- Print nodeset without attributes -->
  <xsl:template match="*" mode="printNodes">
	<xsl:param name="title"/>
	<xsl:param name="showTags"/>
	<xsl:param name="showAttributes"/>
	<xsl:variable name="output">
		<xsl:apply-templates select="." mode="printable">
			<xsl:with-param name="showTags" select="$showTags"/>
			<xsl:with-param name="showAttributes" select="$showAttributes"/>
		</xsl:apply-templates>
	</xsl:variable>
	<xsl:variable name="printout">
		<xsl:value-of select="xalan:nodeset($output)"/>
	</xsl:variable>
	<xsl:variable name="printTitle" select="cmd:println($title)"/>
	<xsl:variable name="printOutput" select="cmd:println($printout)"/>
  </xsl:template>

  <!-- Print nodeset with all attributes -->
  <xsl:template match="*" mode="printAll">
	<xsl:param name="title"/>
	<xsl:apply-templates select="." mode="printNodes">
		<xsl:with-param name="title" select="$title"/>
		<xsl:with-param name="showTags" select="true()"/>
		<xsl:with-param name="showAttributes" select="true()"/>
	</xsl:apply-templates>
  </xsl:template>

  <!-- Print nodeset as text -->
  <xsl:template match="*" mode="printAsText">
	<xsl:param name="title"/>
	<xsl:apply-templates select="." mode="printNodes">
		<xsl:with-param name="title" select="$title"/>
	</xsl:apply-templates>
  </xsl:template>

  <!-- Print title line -->
  <xsl:template match="*" mode="printTitle">
	<xsl:param name="title"/>
	<xsl:variable name="printTitle" select="cmd:println($title)"/>
  </xsl:template>

  <!-- Print text line -->
  <xsl:template match="*" mode="printText">
	<xsl:variable name="printText" select="cmd:println(.)"/>
  </xsl:template>

  <!-- Debug: Print nodeset -->
  <xsl:template match="*" mode="debug">
	<!-- Comment out to turn off debugging
	-->
	<xsl:param name="title"/>
	<xsl:apply-templates select="." mode="printAsText">
		<xsl:with-param name="title" select="$title"/>
	</xsl:apply-templates>
  </xsl:template>

  <!-- Debug: Print text line -->
  <xsl:template match="*" mode="debugText">
	<!-- Comment out to turn off debugging
	-->
	<xsl:param name="title"/>
	<xsl:apply-templates select="." mode="printTitle">
		<xsl:with-param name="title" select="$title"/>
	</xsl:apply-templates>
  </xsl:template>

</xsl:stylesheet>
