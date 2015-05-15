package com.ryantenney.scheduleparser.ical4j;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * @author fortuna
 *
 */
public class InetAddressHostInfo implements HostInfo {

    private final InetAddress hostAddress;

    /**
     * @throws SocketException where an error occurs identifying the host address
     */
    public InetAddressHostInfo() throws SocketException {
        this(findNonLoopbackAddress());
    }

    /**
     * @param address a host address
     */
    public InetAddressHostInfo(InetAddress address) {
        this.hostAddress = address;
    }

    /**
     * {@inheritDoc}
     */
    public String getHostName() {
        return hostAddress.getHostName();
    }

    /**
     * Find a non loopback address for this machine on which to start the server.
     * @return a non loopback address
     * @throws SocketException if a socket error occurs
     */
    private static InetAddress findNonLoopbackAddress() throws SocketException {
        final Enumeration<NetworkInterface> enumInterfaceAddress = NetworkInterface.getNetworkInterfaces();
        while (enumInterfaceAddress.hasMoreElements()) {
            final NetworkInterface netIf = enumInterfaceAddress.nextElement();

            // Iterate over inet address
            final Enumeration<InetAddress> enumInetAdress = netIf.getInetAddresses();
            while (enumInetAdress.hasMoreElements()) {
                final InetAddress address = enumInetAdress.nextElement();
                if (!address.isLoopbackAddress()) {
                    return address;
                }
            }
        }
        return null;
    }

}