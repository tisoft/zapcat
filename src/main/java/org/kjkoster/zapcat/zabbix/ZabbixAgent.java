package org.kjkoster.zapcat.zabbix;

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

import javax.management.ObjectName;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kjkoster.zapcat.Agent;

/**
 * A passive Zabbix agent. This agent starts and manages a daemon thread.
 * <p>
 * The agent uses an executor service to handle the JMX queries that come in.
 * This allows us to handle a few queries concurrently.
 * <p>
 * Configuration done through system properties override default values.
 * 
 * @author Kees Jan Koster &lt;kjkoster@kjkoster.org&gt;
 */
public final class ZabbixAgent implements Agent, Runnable {
    private static final Logger log = Logger.getLogger(ZabbixAgent.class.getName());

    /**
     * The default port that the Zapcat agents listens on.
     */
    public static final int DEFAULT_PORT = 10052;

    /**
     * The property key indicating the port number.
     */
    public static final String PORT_PROPERTY = "org.kjkoster.zapcat.zabbix.port";

    /**
     * The property key indicating the bind address.
     */
    public static final String ADDRESS_PROPERTY = "org.kjkoster.zapcat.zabbix.address";

    /**
     * The property key indicating the protocol version to use.
     */
    public static final String PROTOCOL_PROPERTY = "org.kjkoster.zapcat.zabbix.protocol";

    /**
     * The property key for the whitelist of hosts that may connect to the
     * agent.
     */
    public static final String WHITELIST_PROPERTY = "org.kjkoster.zapcat.whitelist";

    /**
     * The property key for the JMX URL."
     */
    public static final String JMX_URL_PROPERTY = "org.kjkoster.zapcat.jmx.url";

    /**
     * The property key for the JMX Username.
     */
    public static final String JMX_USERNAME_PROPERTY = "org.kjkoster.zapcat.jmx.username";
    /**
     * The property key for the JMX Password.
     */
    public static final String JMX_PASSWORD_PROPERTY = "org.kjkoster.zapcat.jmx.password";

    // the address to bind to (or 'null' to bind to any available interface).
    private final InetAddress address;

    // the port to bind to.
    private final int port;

    private final Thread daemon;

    private ServerSocket serverSocket = null;

    private volatile boolean stopping = false;

    private final Set<String> whitelist;

    /**
     * Configure a new Zabbix agent. Each agent needs the local port number to
     * run. This constructor configures the port number by checking for a system
     * property named "org.kjkoster.zapcat.zabbix.port". If it is not there, the
     * port number defaults to 10052.
     * <p>
     * An optional address on which the agent will listen can be specified by
     * setting a property named "org.kjkoster.zapcat.zabbix.address". If this
     * property is not set, this Zabbix agent will listen on any available
     * address.
     */
    public ZabbixAgent() {
        this(null, DEFAULT_PORT, true);
    }

    /**
     * Configure a new Zabbix agent to listen on a specific address and port
     * number.
     * <p>
     * Direct use of this method in your code is discouraged, since it places
     * address and port number configuration in the hands of developers. That is
     * a task that should normally be left to system administrators and done
     * through system properties. Please use the default constructor instead.
     * 
     * @param address
     *            The address to listen on, or 'null' to listen on any available
     *            address.
     * @param port
     *            The port number to listen on.
     */
    public ZabbixAgent(final InetAddress address, final int port, boolean isDaemon) {
        final String propertyAddress = System.getProperty(ADDRESS_PROPERTY);
        InetAddress resolved = null;
        if (propertyAddress != null) {
            try {
                resolved = InetAddress.getByName(propertyAddress);
            } catch (UnknownHostException e) {
                log.log(Level.WARNING, "Unable to resolve " + propertyAddress
                        + " as a host name, ignoring setting", e);
            }
        }
        this.address = resolved == null ? address : resolved;

        final String propertyPort = System.getProperty(PORT_PROPERTY);
        this.port = propertyPort == null ? port : Integer
                .parseInt(propertyPort);

        if (System.getProperty(WHITELIST_PROPERTY) != null) {
            whitelist = new HashSet<String>();
            whitelist.addAll(Arrays.asList(System.getProperty(
                    WHITELIST_PROPERTY).split(",\\s*")));
        } else {
            whitelist = null;
        }

        daemon = new Thread(this, "Zabbix-agent");
        daemon.setDaemon(isDaemon);
        daemon.start();
    }

    /**
     * Stop and clean up the used resources.
     * 
     * @see org.kjkoster.zapcat.Agent#stop()
     */
    public void stop() {
        stopping = true;

        try {
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (IOException e) {
            // ignore, we're going down anyway...
        }

        try {
            daemon.join();
        } catch (InterruptedException e) {
            // ignore, we're going down anyway...
        }

        log.fine("zabbix agent is done");
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        final ExecutorService handlers = new ThreadPoolExecutor(1, 5, 60L,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        final ObjectName mbeanName = JMXHelper.register(new Agent(),
                "org.kjkoster.zapcat:type=Agent,port=" + port);

        try {
            // 0 means 'use default backlog'
            serverSocket = new ServerSocket(port, 0, address);

            while (!stopping) {
                final Socket accepted = serverSocket.accept();
                log.fine("accepted connection from "
                        + accepted.getInetAddress().getHostAddress());

                if (acceptedByWhitelist(accepted.getInetAddress())) {
                    handlers.execute(new QueryHandler(accepted));
                } else {
                    log.warning( "rejecting ip address "
                            + accepted.getInetAddress().getHostAddress()
                            + ", it is not on the whitelist");
                }
            }
        } catch (IOException e) {
            if (!stopping) {
                log.log(Level.SEVERE, "caught exception, exiting", e);
            }
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                    serverSocket = null;
                }
            } catch (IOException e) {
                // ignore, we're going down anyway...
            }

            try {
                handlers.shutdown();
                handlers.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // ignore, we're going down anyway...
            }

            JMXHelper.unregister(mbeanName);
        }
    }

    private boolean acceptedByWhitelist(final InetAddress accepted) {
        if (whitelist == null) {
            return true;
        }

        for (final String allowed : whitelist) {
            try {
                if (accepted.equals(InetAddress.getByName(allowed))) {
                    return true;
                }
            } catch (UnknownHostException e) {
                log.log(Level.SEVERE, "invalid host '" + allowed
                                + "' on the white list", e);
            }
        }

        return false;
    }

    /**
     * The interface to our JMX representation.
     * 
     * @author Kees Jan Koster &lt;kjkoster@kjkoster.org&gt;
     */
    public interface AgentMBean {
        /**
         * Read the port number that the current agent is listening to.
         * 
         * @return The configured port number.
         */
        int getPort();

        /**
         * Read the bind address of the current agent.
         * 
         * @return The configured bind address, or '*' to indicate any address.
         */
        String getBindAddress();

        /**
         * Read the whitelist as a collection of strings in the format
         * &lt;supplied name&gt;&nbsp;(&lt;resolved address or exception&gt;).
         * <p>
         * Of course, this should really be a map, but jconsole does not deal
         * gracefully with maps.
         * 
         * @return The whitelist for this agent.
         */
        Collection<String> getWhitelist();
    }

    /**
     * Our JMX representation.
     * <p>
     * This class is <code>public</code> because some application servers
     * cannot access this mbean otherwise. JBoss version 4.0.2 for example.
     * 
     * @author Kees Jan Koster &lt;kjkoster@kjkoster.org&gt;
     */
    public class Agent implements AgentMBean {
        /**
         * @see org.kjkoster.zapcat.zabbix.AgentMBean#getPort()
         */
        public int getPort() {
            return port;
        }

        /**
         * @see org.kjkoster.zapcat.zabbix.ZabbixAgent.AgentMBean#getBindAddress()
         */
        public String getBindAddress() {
            return address == null ? "*" : address.getHostAddress();
        }

        /**
         * @see org.kjkoster.zapcat.zabbix.ZabbixAgent.AgentMBean#getWhitelist()
         */
        public Collection<String> getWhitelist() {
            final Collection<String> list = new HashSet<String>();
            for (final String host : whitelist) {
                try {
                    list.add(host + " ("
                            + InetAddress.getByName(host).getHostAddress()
                            + ")");
                } catch (UnknownHostException e) {
                    list.add(host + " (" + e.getClass().getName() + ": "
                            + e.getMessage() + ")");
                }
            }

            return list;
        }
    }

    public static final void main(String[] args) throws Exception {
        Properties properties=new Properties();
        Logger logger = Logger.getLogger(ZabbixAgent.class.getName());
       
        HashMap<String, String> pList = new HashMap();
        pList.put(PORT_PROPERTY, "port");
        pList.put(ADDRESS_PROPERTY, "address");
        pList.put(PROTOCOL_PROPERTY, "protocol");
        pList.put(WHITELIST_PROPERTY, "whitelist");
        pList.put(JMX_URL_PROPERTY, "jmx.url");
        pList.put(JMX_USERNAME_PROPERTY, "jmx.username");
        pList.put(JMX_PASSWORD_PROPERTY, "jmx.password");

        logger.setLevel(Level.FINER);
        
        logger.severe("severe");
        logger.warning("warning");      
        logger.info("info");
        logger.finer("finer");
        logger.finest("finest");

        System.out.println(logger.getLevel());

        try {
            FileInputStream input=new FileInputStream("zapcat.properties");
            try {
                properties.load(input);
                Iterator iterator = pList.keySet().iterator();
                while (iterator.hasNext()) {
                    String name = (String) iterator.next();
                    if (properties.containsKey(name)) {                       
                        logger.fine("found property " + name + " = " + properties.getProperty(name));
                        System.setProperty(name, properties.getProperty(name));
                    } else {
                        logger.warning("property " + name + " not found");
                    }
                }               
            } finally {
                input.close();
            }
        } catch (FileNotFoundException e) {
            System.err.println("zapcat.properties introuvable");
        }
        //starts Zabbix Agent
        String addressStr = System.getProperty(ADDRESS_PROPERTY);        
        InetAddress address=null;
        if (addressStr!=null) {
            address = InetAddress.getByName(addressStr);
        }
        new ZabbixAgent(address, DEFAULT_PORT, false);
    }
}
