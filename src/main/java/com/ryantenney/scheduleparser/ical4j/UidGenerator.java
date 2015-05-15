package com.ryantenney.scheduleparser.ical4j;

import net.fortuna.ical4j.util.Dates;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.property.Uid;

import java.net.SocketException;

/**
 * $Id$
 *
 * Created on 11/03/2007
 *
 * Generates {@link Uid} properties in a similar fashion to that recommended in section 4.8.4.7 of the specification.
 * @author Ben Fortuna
 */
public class UidGenerator {

    private final String pid;

    private final HostInfo hostInfo;

    private static long lastMillis;

    /**
     * @param pid a unique process identifier for the host machine
     * @throws SocketException where host information cannot be retrieved
     */
    public UidGenerator(String pid) throws SocketException {
        this(new InetAddressHostInfo(), pid);
    }

    /**
     * @param hostInfo custom host information
     * @param pid a unique process identifier for the host machine
     */
    public UidGenerator(HostInfo hostInfo, String pid) {
        this.hostInfo = hostInfo;
        this.pid = pid;
    }

    /**
     * @return a unique component identifier
     */
    public Uid generateUid() {
        final StringBuilder b = new StringBuilder();
        b.append(uniqueTimestamp());
        b.append('-');
        b.append(pid);
        if (hostInfo != null) {
            b.append('@');
            b.append(hostInfo.getHostName());
        }
        return new Uid(b.toString());
    }

    /**
     * Generates a timestamp guaranteed to be unique for the current JVM instance.
     * @return a {@link DateTime} instance representing a unique timestamp
     */
    private static DateTime uniqueTimestamp() {
        long currentMillis;
        synchronized (UidGenerator.class) {
            currentMillis = System.currentTimeMillis();
            // guarantee uniqueness by ensuring timestamp is always greater
            // than the previous..
            if (currentMillis < lastMillis) {
                currentMillis = lastMillis;
            }
            if (currentMillis - lastMillis < Dates.MILLIS_PER_SECOND) {
                currentMillis += Dates.MILLIS_PER_SECOND;
            }
            lastMillis = currentMillis;
        }
        final DateTime timestamp = new DateTime(currentMillis);
        timestamp.setUtc(true);
        return timestamp;
    }
}