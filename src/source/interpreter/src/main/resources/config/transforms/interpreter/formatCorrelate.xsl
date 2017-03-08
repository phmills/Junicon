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
   # ACTION: Reconstruct concrete syntax for source-to-source error correlation.
   #	Keep as document instead of text.
   #====
   -->
  <xsl:output method="xml" omit-xml-declaration="yes"/>

  <!-- 
   # BLOCK followed by DELIMITER ID=; => delete ;
   -->
  <xsl:template match="DELIMITER[@ID=';' and preceding-sibling::BLOCK]"
		priority="1">
  </xsl:template>
	
  <!--
   # Default template - copy nodes through
   -->
  <xsl:template match="@*|node()">
	<xsl:copy>
		<xsl:apply-templates select="@*|node()"/>
	</xsl:copy>
  </xsl:template>

</xsl:stylesheet>
