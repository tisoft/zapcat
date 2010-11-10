package org.kjkoster.zapcat.openfire;

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

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import java.util.logging.Logger;
import java.util.logging.Level;

//import org.apache.log4j.BasicConfigurator;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.util.JiveGlobals;
//import org.jivesoftware.util.Log;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.jivesoftware.util.PropertyEventListener;
import org.kjkoster.zapcat.Agent;
import org.kjkoster.zapcat.zabbix.ZabbixAgent;

/**
 * This plugin enables a Zabbix server to query Openfire in a JMX style.
 * 
 * @author Guus der Kinderen &lt;guus@nimbuzz.com&gt;
 */

public class ZapcatPlugin implements Plugin, PropertyEventListener {

    private static final String ZAPCAT_PORT = "zapcat.port";

    private static final String ZAPCAT_INETADDRESS = "zapcat.inetaddress";

    private Agent agent = null;

    private InetAddress inetAddress = null;

    private int port = ZabbixAgent.DEFAULT_PORT;
	private static final Logger log = Logger
            .getLogger(ZapcatPlugin.class.getName());
    /**
     * @see org.jivesoftware.openfire.container.Plugin#initializePlugin(org.jivesoftware.openfire.container.PluginManager,
     *      java.io.File)
     */
    
    public void initializePlugin(final PluginManager manager,
            final File pluginDirectory) {
        log.info("Initializing Zapcat Plugin.");

        log.info("Initializing Log4J for Zapcat.");
        //BasicConfigurator.configure(new OpenfireLog4jAppender());

        PropertyEventDispatcher.addListener(this);
        final String address = JiveGlobals.getProperty(ZAPCAT_INETADDRESS);
        if (address != null) {
            try {
                inetAddress = InetAddress.getByName(address);
            } catch (UnknownHostException ex) {
                log.log(Level.WARNING,
                        "Unable to parse InetAddress from property. "
                        + "Using default value instead.", ex);
                inetAddress = null;
            }
        }

        port = JiveGlobals
                .getIntProperty(ZAPCAT_PORT, ZabbixAgent.DEFAULT_PORT);
        agent = new ZabbixAgent(inetAddress, port, true);
    }

    /**
     * @see org.jivesoftware.openfire.container.Plugin#destroyPlugin()
     */
    
    public void destroyPlugin() {
        log.info("Destroying Zapcat Plugin.");
        PropertyEventDispatcher.removeListener(this);

        if (agent != null) {
            agent.stop();
        }
    }

    /**
     * Reloads the agent with the new attributes. This method will not reload
     * the agent if the new attributes are no different from the old ones. This
     * method will restart the Agent if no attribute change was detected, but
     * the Agent was not running.
     * 
     * @param addr
     *            The address to listen on, or 'null' to listen on any available
     *            address.
     * @param p
     *            The port number to listen on.
     */
    private void restartAgent(final InetAddress addr, final int p) {
        boolean restartAgent = false;
        // check if this changes the current value.
        if (addr != null && !addr.equals(inetAddress)) {
            inetAddress = addr;
            restartAgent = true;
        } else if (inetAddress != null && !inetAddress.equals(addr)) {
            inetAddress = addr;
            restartAgent = true;
        }

        if (port != p) {
            port = p;
            restartAgent = true;
        }

        if (!restartAgent && agent != null) {
            // no need to change anything.
            return;
        }

        if (agent != null) {
            log.finer("Stopping agent that's currently running.");
            agent.stop();
        }

        log.finer("Starting new agent.");
        agent = new ZabbixAgent(inetAddress, port, true);
    }

    /**
     * @see org.jivesoftware.util.PropertyEventListener#propertySet(java.lang.String,
     *      java.util.Map)
     */
    public void propertySet(final String property,
            final Map<String, Object> params) {
        if (ZAPCAT_INETADDRESS.equals(property)) {
            final String addr = (String) params.get("value");
            try {
                final InetAddress iAddr;
                if (addr == null || "".equals(addr.trim())) {
                    iAddr = null;
                } else {
                    iAddr = InetAddress.getByName(addr);
                }
                restartAgent(iAddr, port);
            } catch (UnknownHostException ex) {
                log.log(Level.WARNING,
                        "Unable to parse inetaddress from new property. "
                        + "Using old value instead.", ex);
            }
        } else if (ZAPCAT_PORT.equals(property)) {
            restartAgent(inetAddress, Integer.parseInt((String) params
                    .get("value")));
        }
    }

    /**
     * @see org.jivesoftware.util.PropertyEventListener#propertyDeleted(java.lang.String,
     *      java.util.Map)
     */
    public void propertyDeleted(final String property,
            final Map<String, Object> params) {
        if (ZAPCAT_INETADDRESS.equals(property)) {
            restartAgent(null, port);
        } else if (ZAPCAT_PORT.equals(property)) {
            restartAgent(inetAddress, ZabbixAgent.DEFAULT_PORT);
        }
    }

    /**
     * @see org.jivesoftware.util.PropertyEventListener#xmlPropertySet(java.lang.String,
     *      java.util.Map)
     */
    public void xmlPropertySet(final String property,
            final Map<String, Object> params) {
        // not used.
    }

    /**
     * @see org.jivesoftware.util.PropertyEventListener#xmlPropertyDeleted(java.lang.String,
     *      java.util.Map)
     */
    public void xmlPropertyDeleted(final String property,
            final Map<String, Object> params) {
        // not used.
    }
}
