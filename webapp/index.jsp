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

<%@ page import="org.kjkoster.zapcat.zabbix.JMXHelper"%>
<%@ page import="javax.management.ObjectName" %>
<%
    final String objectName = "org.kjkoster.zapcat:type=Agent,port="
            + System.getProperty("org.kjkoster.zapcat.zabbix.port",
                    "10052");
    final String port = JMXHelper.query(new ObjectName(objectName), "Port");
    String address = JMXHelper.query(new ObjectName(objectName), "BindAddress");
    if (address.equals("*")) {
        address = "all available addresses";
    }
    
    String server = "a type of application server that Zapcat is not familiar with";
    String howto = null;
    String generator = null;
    
    try {
    	// jboss.system:type=Server is a more friendly version.
			server = "JBoss " + JMXHelper.query(new ObjectName("jboss.system:type=Server"),"VersionNumber");
			generator = "zabbix-jboss-definition.xml";
    } catch (Exception e) {
        // ok, I guess we are not JBoss
    }
    
    if (server == null) {
	    try {
    	    server = JMXHelper.query(new ObjectName("Catalina:type=Server"), "serverInfo");
    		howto = "http://www.kjkoster.org/zapcat/Tomcat_How_To.html";
        	generator = "zabbix-tomcat-definition.xml";
    	} catch (Exception e) {
        	// ok, I guess we are not Tomcat
    	}
    }

    if (server == null) {
	    try {
    	    server = "Oracle IAS " + JMXHelper.query(new ObjectName("oc4j:j2eeType=J2EEServer,name=standalone"), "serverVersion");
        	howto = "http://www.kjkoster.org/zapcat/Oracle_IAS_OC4J_How_To.html";
	    } catch (Exception e) {
    	    // ok, I guess we are not Oracle IAS
    	}
    }
    
    if (server == null) {
	    try {
    	    server = "Jetty " + JMXHelper.query(new ObjectName("org.mortbay.jetty:type=server,id=0"), "version");
        	howto = "http://www.kjkoster.org/zapcat/Jetty_How_To.html";
  	  	} catch (Exception e) {
    	    // ok, I guess we are not Jetty
    	}
    }
%>
<html>
<head>
<title>Welcome to Zapcat</title>
<link href="zapcat.css" rel="stylesheet" type="text/css">
</head>
<body>
<h1>Welcome to Zapcat</h1>
<p>Welcome to the Zapcat servlet engine plugin. This plugin is the
quickest way to enable Zapcat on a servlet engine such as JBoss, Tomcat, Oracle
IAS or Jetty.</p>
<p>The Zapcat agent is listening on port&nbsp;<%=port%>, and bound
to <%=address%>.</p>

<p>It looks like you use <%= server %>.

<% if (howto != null) { %>
There is an official Zapcat for <a href="<%= howto %>"><%= server %>
how-to</a>. On that page, you will find step-by-step instructions that guide
you through the process of hooking Zabbix up to your application server.
<% } else { %>
There is no official how-to for this type of application server. Even so, you can
monitor the basics of this Java process easily. Best start out by applying the
Java template.
<% } %>

<% if (generator != null) { %>
You can generate a custom host definition by <a href="<%= generator %>">clicking
here</a>. You will find more information about the custom host generator in the
how-to.
<% } else { %>
There is no custom host definition generator for this type of application server.
<% } %>
</p>

<p>If you would like to add your own statistics to you host
definition, the <a href="mbeans.jsp">mbean list</a> is a page that gives
you a conveniently formatted list of all the mbeans in your server.</p>
</body>
</html>
