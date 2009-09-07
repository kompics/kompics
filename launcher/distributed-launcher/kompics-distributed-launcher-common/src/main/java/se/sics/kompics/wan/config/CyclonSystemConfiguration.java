/**
 * This file is part of the Kompics P2P Framework.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.wan.config;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import java.util.logging.Level;
import java.util.logging.Logger;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.NetworkConfiguration;
import se.sics.kompics.network.Transport;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.monitor.cyclon.server.CyclonMonitorConfiguration;
import se.sics.kompics.p2p.overlay.cyclon.CyclonConfiguration;
import se.sics.kompics.wan.util.LocalNetworkConfiguration;
import se.sics.kompics.web.jetty.JettyWebServerConfiguration;

/**
 * The <code>Configuration</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: Configuration.java 1150 2009-09-02 00:00:40Z Cosmin $
 */
public class CyclonSystemConfiguration implements SystemConfiguration {

    final int networkPort;
    final int webPort;
    final int bootNetPort = 7001;
    final int bootWebPort = 7000;
    final int monitorNetPort = 7003;
    final int monitorWebPort = 7002;
    final String bootHost;
    final String monitorHost;
    JettyWebServerConfiguration jettyWebServerConfiguration;
    BootstrapConfiguration bootConfiguration;
    CyclonMonitorConfiguration monitorConfiguration;
    CyclonConfiguration cyclonConfiguration;
    NetworkConfiguration networkConfiguration;

    public CyclonSystemConfiguration(int networkPort, String bootHost, String monitorHost) {
        super();
        this.networkPort = networkPort;
        this.webPort = networkPort - 1;
        InetAddress ip = null;
        this.monitorHost = monitorHost;
        this.bootHost = bootHost;

        ip = LocalNetworkConfiguration.findLocalInetAddress();

        if (ip == null) {
            Logger.getLogger(CyclonSystemConfiguration.class.getName()).log(Level.SEVERE, null, "Couldn't find non-local network address at this host. Using loopback network address.");
            try {
                ip = InetAddress.getLocalHost();
            } catch (UnknownHostException ex) {
                Logger.getLogger(CyclonSystemConfiguration.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        int bootId = Integer.MAX_VALUE;
        int monitorId = Integer.MAX_VALUE - 1;

        InetAddress bootIp, monitorIp;
        try {
            bootIp = InetAddress.getByName(bootHost);
            monitorIp = InetAddress.getByName(monitorHost);
        } catch (UnknownHostException ex) {
            Logger.getLogger(CyclonSystemConfiguration.class.getName()).log(Level.SEVERE, null, ex);
            monitorIp = ip;
            bootIp = ip;
        }


        Address bootServerAddress = new Address(bootIp, bootNetPort, bootId);
        Address monitorServerAddress = new Address(monitorIp, monitorNetPort,
                monitorId);

        int webRequestTimeout = 5000;
        int webThreads = 2;

        String bootWebAddress = "http://" + bootIp.getHostAddress() + ":" + bootWebPort + "/";
        String monitorWebAddress = "http://" + monitorIp.getHostAddress() + ":" + monitorWebPort + "/";

        String homePage = "<h2>Welcome to the Kompics Peer-to-Peer Framework!</h2>" + "<a href=\"" + bootWebAddress + bootId + "/" + "\">Bootstrap Server</a><br>" + "<a href=\"" + monitorWebAddress + monitorId + "/" + "\">Monitor Server</a>";

        jettyWebServerConfiguration = new JettyWebServerConfiguration(ip,
                webPort, webRequestTimeout, webThreads, homePage);

        bootConfiguration = new BootstrapConfiguration(bootServerAddress,
                60000, 4000, 3, 30000, webPort, bootWebPort);

        monitorConfiguration = new CyclonMonitorConfiguration(
                monitorServerAddress, 10000, 1000, webPort, Transport.UDP);

        cyclonConfiguration = new CyclonConfiguration(5, 15, 1000, 3000,
                new BigInteger("2").pow(13), 20);

        networkConfiguration = new NetworkConfiguration(ip, networkPort, 0);
    }

    public Properties set() throws IOException {
        Properties p = new Properties();
        String c = File.createTempFile("jetty.web.", ".conf").getAbsolutePath();
        jettyWebServerConfiguration.store(c);
        System.setProperty("jetty.web.configuration", c);
        p.setProperty("jetty.web.configuration", c);

        c = File.createTempFile("bootstrap.", ".conf").getAbsolutePath();
        bootConfiguration.store(c);
        System.setProperty("bootstrap.configuration", c);
        p.setProperty("bootstrap.configuration", c);

        c = File.createTempFile("cyclon.monitor.", ".conf").getAbsolutePath();
        monitorConfiguration.store(c);
        System.setProperty("cyclon.monitor.configuration", c);
        p.setProperty("cyclon.monitor.configuration", c);

        c = File.createTempFile("cyclon.", ".conf").getAbsolutePath();
        cyclonConfiguration.store(c);
        System.setProperty("cyclon.configuration", c);
        p.setProperty("cyclon.configuration", c);

        c = File.createTempFile("network.", ".conf").getAbsolutePath();
        networkConfiguration.store(c);
        System.setProperty("network.configuration", c);
        p.setProperty("network.configuration", c);

        return p;
    }
}
