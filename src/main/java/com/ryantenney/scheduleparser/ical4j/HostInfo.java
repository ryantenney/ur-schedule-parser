package com.ryantenney.scheduleparser.ical4j;

/**
 * Provides platform-independent host information.
 *
 * @author fortuna
 *
 */
public interface HostInfo {

    /**
     * @return a name for the host machine
     */
    String getHostName();
}