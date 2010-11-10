package org.kjkoster.zapcat.zabbix;

import java.io.IOException;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

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

/**
 * A data object used to send data to the monitoring server. Note that this
 * object may either contain a literal value, or a JMX query.
 * <p>
 * JMX queries are performed when <code>getValue()</code> is invoked, not when
 * the object is constructed. This means that consecutive calls to
 * <code>getValue()</code> may yield different results.
 * 
 * @author Kees Jan Koster &lt;kjkoster@kjkoster.org&gt;
 */
final class Item {
    private final String host;

    private final String key;

    private final String value;

    private final ObjectName objectName;

    private final String attribute;

    /**
     * Create a literal value item.
     * 
     * @param host
     *            The host configuration to send the item to.
     * @param key
     *            The monitoring server's key for this statistic.
     * @param value
     *            The literal value.
     */
    public Item(final String host, final String key, final String value) {
        if (host == null || "".equals(host.trim())) {
            throw new IllegalArgumentException("empty host");
        }
        if (key == null || "".equals(key.trim())) {
            throw new IllegalArgumentException("empty key");
        }
        if (value == null) {
            throw new IllegalArgumentException("null value for key '" + key
                    + "'");
        }

        this.host = host;
        this.key = key;
        this.value = value;
        this.objectName = null;
        this.attribute = null;
    }

    /**
     * Create a JMX query item.
     * 
     * @param host
     *            The host configuration to send the item to.
     * @param key
     *            The monitoring server's key for this statistic.
     * @param objectName
     *            The JMX object to query.
     * @param attribute
     *            The attribute on that object.
     */
    public Item(final String host, final String key,
            final ObjectName objectName, final String attribute) {
        if (key == null || "".equals(key.trim())) {
            throw new IllegalArgumentException("empty key");
        }
        if (objectName == null) {
            throw new IllegalArgumentException("null objectname for key '"
                    + key + "'");
        }
        if (attribute == null) {
            throw new IllegalArgumentException("null attribute for key '" + key
                    + "'");
        }

        this.host = host;
        this.key = key;
        this.value = null;
        this.objectName = objectName;
        this.attribute = attribute;
    }

    /**
     * Find the item's host.
     * 
     * @return The monitoring server's host for this item.
     */
    public String getHost() {
        return host;
    }

    /**
     * Find the item's key.
     * 
     * @return The monitoring server's key for this item.
     */
    public String getKey() {
        return key;
    }

    /**
     * Determine the value for this item. If this is a literal item, return its
     * value. If this is a JMX query item, perform the query.
     * 
     * @return The current value for this item.
     * @throws InstanceNotFoundException
     *             When the specified mbean could not be located.
     * @throws ReflectionException
     *             When there was a problem inspecting the mbean.
     * @throws MBeanException
     *             When there was a problem inspecting the mbean.
     * @throws AttributeNotFoundException
     *             When the specified attribute could not be found.
     */
    public String getValue() throws InstanceNotFoundException,
            AttributeNotFoundException, MBeanException, ReflectionException, IOException {
        if (attribute == null) {
            return value;
        }

        return JMXHelper.query(objectName, attribute);
    }
}
