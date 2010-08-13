<!--
    This file is part of Zapcat.

    Zapcat is free software: you can redistribute it and/or modify it under the
    terms of the GNU General Public License as published by the Free Software
    Foundation, either version 3 of the License, or (at your option) any later
    version.

    Zapcat is distributed in the hope that it will be useful, but WITHOUT ANY
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
    FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
    details.

    You should have received a copy of the GNU General Public License along
    with Zapcat. If not, see <http://www.gnu.org/licenses/>.
-->

<%@ page import="java.lang.reflect.Method" %>
<%@ page import="java.util.*" %>
<%@ page import="javax.management.*" %>
<%@ page import="javax.management.openmbean.CompositeData" %>
<%@ page import="org.kjkoster.zapcat.zabbix.JMXHelper" %>

<html>
<head>
<title>MBean List</title>
<link href="zapcat.css" rel="stylesheet" type="text/css">
</head>
<body>
<h1>MBean List</h1>
<p>This page gives you a list of all the mbeans and their attributes
that are available in this JVM. This information is formatted so that it
you can easily cut-and-paste them into the Zabbix configuration screens.
Their type information is provided, but information about their meaning
and significance can only be found on the Internet and in the various
forums.</p>
<p>Please be warned that the number and type of available mbeans may
change with different hardware, operating systems, JVM versions, JVM
configuration, Tomcat versions and Tomcat configuration options.</p>
<p>And yes, there are a lot of them. :-)</p>

<table>
	<%
		// we use Java 1.4-style iteration for older Jasper compilers
		final Iterator beanCounter = JMXHelper.getMBeanServer().queryNames(null, null).iterator();
		while(beanCounter.hasNext()) {
			final ObjectName mbean = (ObjectName) beanCounter.next();
	%>
	<tr>
		<td class="mbean" colspan="3">jmx[<%=mbean%>]</td>
	</tr>
	<%
			// we use Java 1.4-style iteration for older Jasper compilers
			final MBeanAttributeInfo[] info = JMXHelper.getMBeanServer().getMBeanInfo(mbean).getAttributes();
			for (int i = 0; i < info.length; i++) {
			    final MBeanAttributeInfo attrib = info[i];
	%>
	<tr>
		<td>&nbsp;&nbsp;</td>
		<td class="attrib">[<%=attrib.getName()%>]</td>
		<td class="attrib"><%=zabbixType(attrib.getType())%></td>
	</tr>
	<%
	    }
	    }
	%>
</table>
</body>

<%!private static String zabbixType(final String type) {
        if (type.equals("java.lang.String")
                || type.equals("javax.management.ObjectName")) {
            return "character";
        }
        if (type.equals("java.lang.Short") || type.equals("short")
                || type.equals("java.lang.Integer") || type.equals("int")
                || type.equals("java.lang.Long") || type.equals("long")) {
            return "numeric (integer 64bit)";
        }
        if (type.equals("java.lang.Float") || type.equals("float")
                || type.equals("java.lang.Double") || type.equals("double")) {
            return "numeric (float)";
        }

        return "unknown&nbsp;("
                + type
                + "),&nbsp;but&nbsp;you&nbsp;can&nbsp;try&nbsp;using&nbsp;'character'.";
    }%>