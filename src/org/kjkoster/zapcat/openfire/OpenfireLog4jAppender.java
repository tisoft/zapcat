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

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;
import org.jivesoftware.util.Log;

/**
 * A Log4J appender that sends its log entries to the Openfire logs.
 * 
 * @author Guus der Kinderen &lt;guus@nimbuzz.com&gt;
 */
public class OpenfireLog4jAppender extends AppenderSkeleton {

    /**
     * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
     */
    // used to be backwards compatible. Ignore deprecation warnings.
    @SuppressWarnings("deprecation")
    @Override
    protected void append(final LoggingEvent event) {
        final String message = event.getMessage().toString();

        Throwable throwable = null;
        if (event.getThrowableInformation() != null) {
            throwable = event.getThrowableInformation().getThrowable();
        }

        switch (event.getLevel().toInt()) {
        case Priority.OFF_INT:
            // Logging turned off - do nothing.
            break;

        case Priority.FATAL_INT:
        case Priority.ERROR_INT:
            Log.error(message, throwable);
            break;

        case Priority.WARN_INT:
            Log.warn(message, throwable);
            break;

        case Priority.INFO_INT:
            Log.info(message, throwable);
            break;

        default:
            // DEBUG and below (trace, all)
            Log.debug(message, throwable);
            break;
        }
    }

    /**
     * @see org.apache.log4j.AppenderSkeleton#close()
     */
    public void close() {
        // There's nothing here to close.
    }

    /**
     * @see org.apache.log4j.AppenderSkeleton#requiresLayout()
     */
    public boolean requiresLayout() {
        // we're doing this quick and dirty.
        return false;
    }
}