package se.sics.kompics.wan.hosts;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Start;
import se.sics.kompics.wan.config.MasterConfiguration;
import se.sics.kompics.wan.hosts.events.AddNodesRequest;
import se.sics.kompics.wan.hosts.events.AddNodesResponse;
import se.sics.kompics.wan.hosts.events.GetNodesRequest;
import se.sics.kompics.wan.hosts.events.GetNodesResponse;
import se.sics.kompics.wan.hosts.events.HostsInit;
import se.sics.kompics.wan.hosts.events.RemoveNodesRequest;
import se.sics.kompics.wan.hosts.events.RemoveNodesResponse;
import se.sics.kompics.wan.ssh.Host;

/**
 * The PLabComponent is used to access
 * <ul>
 * <li>the planetlab API and a cache of that data stored in a local DB.</li>
 * <li>CoMon statistics for planetlab hosts. </li>
 * </ul>
 * <br/>
 * (1) The planetlab API consists of operations to get the set of nodes in
 *     planetlab, add nodes to your slice, get the nodes associated with your slice, 
 *     get the nodes not associated with your slice, get the boot state of nodes,
 *     etc. 
 *     For performance, a local cache of the planetlab hosts associated with your
 *     slice are stored in a local DB. These are loaded when the component is
 *     initialized. 
 * <br/>
 * (2) CoMon are statistics about the runtime state of planetlab
 *     nodes. They include information such as response time for hosts,
 *     number of active slices on the host, boot state of the host, 
 *     CPU activity on the host, etc.
 * 
 * @author jdowling
 *
 */
public class HostsXMLComponent extends ComponentDefinition {

    private static final String HOSTS_DISK_CACHE_FILE = "hosts_cache.xml.gz";
    private final Logger logger = LoggerFactory.getLogger(HostsXMLComponent.class);

    private Negative<HostsPort> hostsPort = negative(HostsPort.class);

    private Set<Host> readyHosts = new HashSet<Host>();

    public HostsXMLComponent() {
        subscribe(handleGetNodesRequest, hostsPort);
        subscribe(handleAddNodesRequest, hostsPort);
        subscribe(handleRemoveNodesRequest, hostsPort);

        subscribe(handleInit, control);
        subscribe(handleStart, control);
    }
    public Handler<Start> handleStart = new Handler<Start>() {
        public void handle(Start event) {
            logger.info("Reading hosts from disk");
            readyHosts.addAll(MasterConfiguration.getHosts());
            Set<Host> diskHosts = hostStoreReadFromDisk();
            if (diskHosts != null) {
            	readyHosts.addAll(diskHosts);
            }
        }
    };
    public Handler<HostsInit> handleInit = new Handler<HostsInit>() {

        public void handle(HostsInit event) {

            // which store to use: hibernate, xml, jnlp, etc

            readyHosts = event.getHosts();
        }
    };
    private Handler<AddNodesRequest> handleAddNodesRequest = new Handler<AddNodesRequest>() {

        public void handle(AddNodesRequest event) {

            logger.info("HostsXMLComponent is adding hosts");

            Set<Host> hosts = event.getHosts();
            readyHosts.addAll(hosts);

            hostStoreWriteToDisk(readyHosts);

            trigger(new AddNodesResponse(event, true), hostsPort);
        }
    };
    private Handler<RemoveNodesRequest> handleRemoveNodesRequest = new Handler<RemoveNodesRequest>() {

        public void handle(RemoveNodesRequest event) {

            Set<Host> hosts = event.getHosts();
            readyHosts.removeAll(hosts);

            hostStoreWriteToDisk(readyHosts);

            trigger(new RemoveNodesResponse(event, true), hostsPort);
        }
    };
    private Handler<GetNodesRequest> handleGetNodesRequest = new Handler<GetNodesRequest>() {

        public void handle(GetNodesRequest event) {
            logger.info("HostsXMLComponent is getting thehosts");

            GetNodesResponse respEvent = new GetNodesResponse(event, readyHosts);
            trigger(respEvent, hostsPort);
        }
    };

    @SuppressWarnings("unchecked")
    private Set<Host> hostStoreReadFromDisk() {
    	Set<Host> persistentHosts = null;
        try {
            XMLDecoder decoder = new XMLDecoder(new GZIPInputStream(
                    new BufferedInputStream(new FileInputStream(
                    HOSTS_DISK_CACHE_FILE))));

            persistentHosts = (Set<Host>) decoder.readObject();
            decoder.close();
            if (persistentHosts == null) {
                System.err.println("disk cache store == null");
            }

        } catch (FileNotFoundException e) {
            System.out.println("Disk cache not found");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return persistentHosts;
    }

    private void hostStoreWriteToDisk(Set<Host> store) {
        try {
            XMLEncoder encoder = new XMLEncoder(new GZIPOutputStream(
                    new BufferedOutputStream(new FileOutputStream(
                    HOSTS_DISK_CACHE_FILE))));
            encoder.writeObject(store);
            encoder.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void removeDiskCache() {
        File diskCache = new File(HOSTS_DISK_CACHE_FILE);
        diskCache.delete();
    }
}
