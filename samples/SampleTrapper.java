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

import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import org.kjkoster.zapcat.Trapper;
import org.kjkoster.zapcat.zabbix.ZabbixTrapper;

public class SampleTrapper {
    public static void main(String[] args) throws Exception {
        Trapper trapper = null;
        try {
            trapper = new ZabbixTrapper("192.168.0.150", "mac.kjkoster.org");

            trapper.send("java.version", System.getProperty("java.version"));

            trapper.send("compiler.name",
                    new ObjectName("java.lang:type=Compilation"), "Name");

            trapper.every(30, TimeUnit.SECONDS, "compiler.time",
                    new ObjectName("java.lang:type=Compilation"), "TotalCompilationTime");

            // simulate lots of important work being done...
            Thread.sleep(Long.MAX_VALUE);
        } finally {
            if (trapper != null) {
                trapper.stop();
            }
        }
    }
}
