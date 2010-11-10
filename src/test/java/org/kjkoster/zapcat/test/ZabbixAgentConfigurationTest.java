package org.kjkoster.zapcat.test;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Properties;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kjkoster.zapcat.Agent;
import org.kjkoster.zapcat.zabbix.JMXHelper;
import org.kjkoster.zapcat.zabbix.ZabbixAgent;

/**
 * Test cases to test the configuration options of the Zabbix agent.
 * 
 * @author Kees Jan Koster &lt;kjkoster@kjkoster.org&gt;
 */
public class ZabbixAgentConfigurationTest {
    private static final int DEFAULTPORT = ZabbixAgent.DEFAULT_PORT;

    private static final int PROPERTYPORT = DEFAULTPORT + 1;

    final Properties originalProperties = (Properties) System.getProperties()
            .clone();

    /**
     * Restore the system properties and check that the agent is dead.
     * 
     * @throws Exception
     *             When the test failed.
     */
    @Before
    public void setUp() throws Exception {
        assertNull(System.getProperty(ZabbixAgent.PORT_PROPERTY));
        assertNull(System.getProperty(ZabbixAgent.ADDRESS_PROPERTY));

        assertAgentDown(DEFAULTPORT);
        assertAgentDown(PROPERTYPORT);
    }

    /**
     * Check that the agent is dead.
     * 
     * @throws Exception
     *             When the test failed.
     */
    @After
    public void tearDown() throws Exception {
        System.setProperties(originalProperties);

        setUp();
    }

    /**
     * Test that we can start and stop the agent.
     * 
     * @throws Exception
     *             When the test failed.
     */
    @Test
    public void testStartAndStop() throws Exception {
        final Agent agent = new ZabbixAgent();

        assertAgentUp(DEFAULTPORT);
        assertAgentDown(PROPERTYPORT);

        agent.stop();
    }

    /**
     * Test that the agent can handle connection errors.
     * 
     * @throws Exception
     *             When the test failed.
     */
    @Test
    public void testTouchAndTouch() throws Exception {
        final Agent agent = new ZabbixAgent();

        assertAgentUp(DEFAULTPORT);
        assertAgentUp(DEFAULTPORT);

        assertAgentDown(PROPERTYPORT);

        agent.stop();
    }

    /**
     * Test that we can use a Java system property to configure the port number
     * on the agent. The property should override the default port number.
     * 
     * @throws Exception
     *             When the test failed.
     */
    @Test
    public void testPortAsProperty() throws Exception {
        System.setProperty(ZabbixAgent.PORT_PROPERTY, "" + PROPERTYPORT);
        assertEquals("" + PROPERTYPORT, System
                .getProperty(ZabbixAgent.PORT_PROPERTY));

        final Agent agent = new ZabbixAgent();

        assertAgentDown(DEFAULTPORT);
        assertAgentUp(PROPERTYPORT);

        agent.stop();
    }

    private void assertAgentDown(final int port) throws Exception {
        try {
            JMXHelper.query(new ObjectName(
                    "org.kjkoster.zapcat:type=Agent,port=" + port), "Port");
            fail();
        } catch (InstanceNotFoundException e) {
            // this is supposed to happen
        }
        try {
            new Socket(InetAddress.getLocalHost(), port);
            fail();
        } catch (ConnectException e) {
            // this is supposed to happen
        }
    }

    private void assertAgentUp(final int port) throws Exception {
        // give the agent some time to open the port
        Thread.sleep(100);

        assertEquals("" + port, JMXHelper.query(new ObjectName(
                "org.kjkoster.zapcat:type=Agent,port=" + port), "Port"));
        assertEquals("*", JMXHelper.query(new ObjectName(
                "org.kjkoster.zapcat:type=Agent,port=" + port), "BindAddress"));
        final Socket touch = new Socket(InetAddress.getLocalHost(), port);
        touch.close();
    }
}
