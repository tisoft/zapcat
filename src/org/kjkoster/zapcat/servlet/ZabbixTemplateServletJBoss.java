package org.kjkoster.zapcat.servlet;

// TODO: Convert this from javax.management mbean server to org.jboss mbean stuff ;)

/* This file is part of Zapcat.
 *
 * Zapcat is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Zapcat is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Zapcat. If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.logging.Logger;
import java.util.logging.Level;
import org.kjkoster.zapcat.zabbix.JMXHelper;
import org.kjkoster.zapcat.zabbix.ZabbixAgent;

/**
 * A servlet that generates the Tomcat Zabbix template. We generate the template
 * for Tomcat because it is so configuraton-dependent. Zabbix really is not able
 * to deal with very dynamic systems.
 * 
 * @author Kees Jan Koster &lt;kjkoster@kjkoster.org&gt;
 */
public class ZabbixTemplateServletJBoss extends HttpServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1245376184346210185L;
	
	private static final Logger log = Logger
            .getLogger(ZabbixTemplateServletJBoss.class.getName());

    private enum Type {
        /**
         * Floating point data.
         */
        Float,

        /**
         * Character data, up to 255 bytes long.
         */
        Character,

        /**
         * Integer data, must be positive.
         */
        Integer;

        int getValue() {
            switch (this) {
            case Float:
                return 0;
            case Character:
                return 1;
            case Integer:
                return 3;
            }

            throw new IllegalArgumentException("unknown value " + this);
        }
    }

    private enum Time {
        /**
         * For configuration items, poll this item only once per hour.
         */
        OncePerHour,

        /**
         * For normal statistics, poll this item twice pr minute.
         */
        TwicePerMinute;

        int getValue() {
            switch (this) {
            case OncePerHour:
                return 3600;
            case TwicePerMinute:
                return 30;
            }

            throw new IllegalArgumentException("unknown value " + this);
        }
    }

    private enum Store {
        /**
         * Store the value as-is.
         */
        AsIs,

        /**
         * Store the value as delta, interpreting the data on a per-second
         * basis.
         */
        AsDelta
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        final PrintWriter out = response.getWriter();
        final MBeanServerConnection mbeanserver = JMXHelper.getMBeanServer();
        try {
            final Set<ObjectName> managers = mbeanserver.queryNames(
            		new ObjectName("jboss.web:type=Manager,*"), null);
            final Set<ObjectName> processors = mbeanserver.queryNames(
                    new ObjectName("jboss.web:type=GlobalRequestProcessor,*"),
                    null);

            ZabbixTemplateServletJBoss t = new ZabbixTemplateServletJBoss();
            response.setContentType("text/xml");
            t.writeHeader(out);
            t.writeItems(out, processors, managers);
            t.writeTriggers(out, processors);
            t.writeGraphs(out, processors, managers);
            t.writeFooter(out);
        } catch (Exception e) {
            log.log(Level.SEVERE, "unable to generate template", e);
            e.printStackTrace(out);
        } finally {
            out.flush();
        }
    }

    private void writeHeader(final PrintWriter out) throws UnknownHostException {
        out.println("<?xml version=\"1.0\"?>");
        out.println("<zabbix_export version=\"1.0\" date=\""
                + new SimpleDateFormat("dd.MM.yy").format(new Date())
                + "\" time=\""
                + new SimpleDateFormat("HH.mm").format(new Date()) + "\">");
        out.println("  <hosts>");

        out.println("    <host name=\"jboss_"
                + InetAddress.getLocalHost().getHostName().replaceAll(
                        "[^a-zA-Z0-9]+", "_") + "\">");
        out.println("      <dns>" + InetAddress.getLocalHost().getHostName()
                + "</dns>");
        out.println("      <ip>" + InetAddress.getLocalHost().getHostAddress()
                + "</ip>");
        out.println("      <port>"
                + System.getProperty(ZabbixAgent.PORT_PROPERTY, ""
                        + ZabbixAgent.DEFAULT_PORT) + "</port>");
        out.println("      <groups>");
        out.println("      </groups>");
    }

    private void writeItems(final PrintWriter out,
            final Set<ObjectName> processors, final Set<ObjectName> managers)
            throws MalformedObjectNameException {
        out.println("      <items>");
        writeItem(out, "JBoss version",
        		new ObjectName("jboss.system:type=Server"), "VersionNumber",
        		Type.Character, null, Store.AsIs, Time.OncePerHour);
        writeProcessorItems(out, processors);
        writeManagerItems(out, managers);
        out.println("      </items>");
    }

    private void writeProcessorItems(final PrintWriter out,
            final Set<ObjectName> processors)
            throws MalformedObjectNameException {
        for (final ObjectName processor : processors) {
            final String name = name(processor);
            final ObjectName threadpool = new ObjectName(
                    "jboss.web:type=ThreadPool,name=" + name);
            final String port = port(name);
            final String address = address(name);

            writeItem(out, name + " bytes received per second", processor,
                    "bytesReceived", Type.Float, "B", Store.AsDelta,
                    Time.TwicePerMinute);
            writeItem(out, name + " bytes sent per second", processor,
                    "bytesSent", Type.Float, "B", Store.AsDelta,
                    Time.TwicePerMinute);
            writeItem(out, name + " requests per second", processor,
                    "requestCount", Type.Float, null, Store.AsDelta,
                    Time.TwicePerMinute);
            writeItem(out, name + " errors per second", processor,
                    "errorCount", Type.Float, null, Store.AsDelta,
                    Time.TwicePerMinute);
            writeItem(out, name + " processing time per second", processor,
                    "processingTime", Type.Float, "s", Store.AsDelta,
                    Time.TwicePerMinute);

            writeItem(out, name + " threads max", threadpool, "maxThreads",
                    Type.Integer, null, Store.AsIs, Time.OncePerHour);
            writeItem(out, name + " threads allocated", threadpool,
                    "currentThreadCount", Type.Integer, null, Store.AsIs,
                    Time.TwicePerMinute);
            writeItem(out, name + " threads busy", threadpool,
                    "currentThreadsBusy", Type.Integer, null, Store.AsIs,
                    Time.TwicePerMinute);

            if (name.startsWith("http")) {
            	log.fine("Writing: " + "jboss.web:type=ProtocolHandler,port=" + port + ",address=" + address);
                writeItem(out, name + " gzip compression", new ObjectName(
                        "jboss.web:type=ProtocolHandler,port=" + port + ",address=" + address),
                        "compression", Type.Character, null, Store.AsIs,
                        Time.OncePerHour);
            }
        }
    }

    private void writeManagerItems(final PrintWriter out,
            final Set<ObjectName> managers) {
        for (final ObjectName manager : managers) {
            writeItem(out, "sessions " + path(manager) + " active", manager,
                    "activeSessions", Type.Integer, null, Store.AsIs,
                    Time.TwicePerMinute);
            writeItem(out, "sessions " + path(manager) + " peak", manager,
                    "maxActiveSessions", Type.Float, null, Store.AsIs,
                    Time.TwicePerMinute);
            writeItem(out, "sessions " + path(manager) + " rejected", manager,
                    "rejectedSessions", Type.Integer, null, Store.AsIs,
                    Time.TwicePerMinute);
        }
    }

    private void writeItem(final PrintWriter out, final String description,
            final ObjectName objectname, final String attribute,
            final Type type, final String units, final Store store,
            final Time time) {
        out.println("        <item type=\"0\" key=\"jmx[" + objectname + "]["
                + attribute + "]\" value_type=\"" + type.getValue() + "\">");
        out.println("          <description>" + description + "</description>");
        out.println("          <delay>" + time.getValue() + "</delay>");
        out.println("          <history>90</history>");
        out.println("          <trends>365</trends>");
        if (units != null) {
            out.println("          <units>" + units + "</units>");
        }
        if (store == Store.AsDelta) {
            out.println("          <delta>1</delta>");
        }
        // we assume that all time is logged in milliseconds...
        if ("s".equals(units)) {
            out.println("          <multiplier>1</multiplier>");
            out.println("          <formula>0.001</formula>");

        } else {
            out.println("          <formula>1</formula>");
        }
        out.println("          <snmp_community>public</snmp_community>");
        out
                .println("          <snmp_oid>interfaces.ifTable.ifEntry.ifInOctets.1</snmp_oid>");
        out.println("          <snmp_port>161</snmp_port>");
        out.println("        </item>");
    }

    private void writeTriggers(final PrintWriter out,
            final Set<ObjectName> processors)
            throws MalformedObjectNameException {
        out.println("      <triggers>");
        writeProcessorTriggers(out, processors);
        out.println("      </triggers>");
    }

    private void writeProcessorTriggers(final PrintWriter out,
            final Set<ObjectName> processors)
            throws MalformedObjectNameException {
        for (final ObjectName processor : processors) {
            final String name = name(processor);
            final ObjectName threadpool = new ObjectName(
                    "jboss.web:type=ThreadPool,name=" + name);
            final String port = port(name);
            final String address = address(name);

            if (name.startsWith("http")) {
                writeTrigger(out, "gzip compression is off for connector "
                        + name + " on {HOSTNAME}",
                        "{{HOSTNAME}:jmx[jboss.web:type=ProtocolHandler,port="
                                + port + ",address=" + address + "][compression].str(off)}=1", 2);
            }
            writeTrigger(out, "70% " + name
                    + " worker threads busy on {HOSTNAME}", "{{HOSTNAME}:jmx["
                    + threadpool
                    + "][currentThreadsBusy].last(0)}>({{HOSTNAME}:jmx["
                    + threadpool + "][maxThreads].last(0)}*0.7)", 4);
        }
    }

    private void writeTrigger(final PrintWriter out, final String description,
            final String expression, final int priority) {
        out.println("        <trigger>");
        out.println("          <description>" + description + "</description>");
        out.println("          <expression>" + expression + "</expression>");
        out.println("          <priority>" + priority + "</priority>");
        out.println("        </trigger>");
    }

    private void writeGraphs(final PrintWriter out,
            final Set<ObjectName> processors, final Set<ObjectName> managers)
            throws MalformedObjectNameException {
        out.println("      <graphs>");
        writeProcessorGraphs(out, processors);
        writeManagerGraphs(out, managers);
        out.println("      </graphs>");
    }

    private void writeProcessorGraphs(final PrintWriter out,
            final Set<ObjectName> processors)
            throws MalformedObjectNameException {
        for (final ObjectName processor : processors) {
            final String name = name(processor);
            final ObjectName threadpool = new ObjectName(
                    "jboss.web:type=ThreadPool,name=" + name);

            writeGraph(out, name + " worker threads", threadpool, "maxThreads",
                    "currentThreadsBusy", "currentThreadCount");
        }
    }

    private void writeManagerGraphs(final PrintWriter out,
            final Set<ObjectName> managers) {
        for (final ObjectName manager : managers) {
            writeGraph(out, "sessions " + path(manager), manager,
                    "rejectedSessions", "activeSessions", "maxActiveSessions");
        }
    }

    private void writeGraph(final PrintWriter out, final String name,
            final ObjectName objectname, final String redAttribute,
            final String greenAttribute, final String blueAttribute) {
        out.println("        <graph name=\"" + name
                + "\" width=\"900\" height=\"200\">");
        out.println("          <show_work_period>1</show_work_period>");
        out.println("          <show_triggers>1</show_triggers>");
        out.println("          <yaxismin>0.0000</yaxismin>");
        out.println("          <yaxismax>100.0000</yaxismax>");
        out.println("          <graph_elements>");
        out.println("            <graph_element item=\"{HOSTNAME}:jmx["
                + objectname + "][" + redAttribute + "]\">");
        out.println("              <color>990000</color>");
        out.println("              <yaxisside>1</yaxisside>");
        out.println("              <calc_fnc>2</calc_fnc>");
        out.println("              <periods_cnt>5</periods_cnt>");
        out.println("            </graph_element>");
        out.println("            <graph_element item=\"{HOSTNAME}:jmx["
                + objectname + "][" + greenAttribute + "]\">");
        out.println("              <color>009900</color>");
        out.println("              <yaxisside>1</yaxisside>");
        out.println("              <calc_fnc>2</calc_fnc>");
        out.println("              <periods_cnt>5</periods_cnt>");
        out.println("            </graph_element>");
        out.println("            <graph_element item=\"{HOSTNAME}:jmx["
                + objectname + "][" + blueAttribute + "]\">");
        out.println("              <color>000099</color>");
        out.println("              <yaxisside>1</yaxisside>");
        out.println("              <calc_fnc>2</calc_fnc>");
        out.println("              <periods_cnt>5</periods_cnt>");
        out.println("            </graph_element>");
        out.println("          </graph_elements>");
        out.println("        </graph>");
    }

    private String path(final ObjectName objectname) {
        final String name = objectname.toString();
        final int start = name.indexOf("path=") + 5;
        final int end = name.indexOf(',', start);

        return name.substring(start, end);
    }

    private String name(final ObjectName objectname) {
        final String name = objectname.toString();
        final int start = name.indexOf("name=") + 5;

        return name.substring(start);
    }
    
    private String address(final String name) {
    	final String addressPort = name.substring(name.indexOf('-') + 1);
    	return "%2F" + addressPort.substring(0,addressPort.indexOf('-'));
    }
    
    private String port (final String name) {
    	final String addressPort = name.substring(name.indexOf('-') + 1);
    	return addressPort.substring(addressPort.indexOf('-') + 1);
    }

    private void writeFooter(final PrintWriter out) {
        out.println("    </host>");
        out.println("  </hosts>");
        out.println("</zabbix_export>");
        out.println();
    }
}
