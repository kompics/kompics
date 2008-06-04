package se.sics.kompics.p2p.network.topology;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.UnknownHostException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import se.sics.kompics.network.Address;

public class TopologyParser implements ErrorHandler {

	private static Logger log = Logger.getLogger(TopologyParser.class);

	private final String topologyXMLschemaDescriptorPath = "topology.xsd";

	private int thisNodeNumber;

	private String topologyDescriptorFile;

	private DocumentBuilderFactory factory;

	private DocumentBuilder parser;

	private Document parsedDocument = null;

	public TopologyParser(int nodeNumber, String topologyDescriptorFilePath) {
		super();
		this.thisNodeNumber = nodeNumber;
		this.topologyDescriptorFile = topologyDescriptorFilePath;

		factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(false);

		SchemaFactory schemaFactory = SchemaFactory
				.newInstance("http://www.w3.org/2001/XMLSchema");

		try {
			factory.setSchema(schemaFactory.newSchema(TopologyParser.class
					.getResource(topologyXMLschemaDescriptorPath)));

		} catch (SAXException e) {
			e.printStackTrace();
			log.error("Wrong Schema");
		}
	}

	public NeighbourLinks parseTopologyFile(int nodesCount) {

		try {
			parser = factory.newDocumentBuilder();

			parser.setErrorHandler(this);

			log.info("Parsing topology file:" + topologyDescriptorFile);

			parsedDocument = parser.parse(new FileInputStream(
					topologyDescriptorFile));

			log.info("Parsing Successful");

		} catch (FileNotFoundException e1) {
			log.error("Parsing Failed");
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			log.error("Parsing Failed");
			e1.printStackTrace();
		} catch (SAXException e) {
			log.error("Parsing Failed");
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Parsing Failed");
			e.printStackTrace();
		}

		Element e = parsedDocument.getDocumentElement();

		log.info("NODES");

		NodeList nodes = e.getElementsByTagName("nodes");

		int nodesNumber = 0;

		if (nodes.getLength() == 1) {
			Element n = (Element) nodes.item(0);
			nodesNumber = Integer.parseInt(n.getAttribute("number"));

			log.info("Total number of nodes=" + nodesNumber);

		} else {

			log.fatal("Error in parsing \"nodes\" section");

			System.exit(1);

		}

		NodeList n = e.getElementsByTagName("node");

		// Creating Topology Descriptor
		NeighbourLinks topologyDescriptor = new NeighbourLinks(nodesCount);

		// For every node
		for (int i = 0; i < n.getLength(); i++) {
			Element x = (Element) n.item(i);

			String id = x.getAttribute("id");
			log
					.info("Node=" + i + ", Id=" + id + ", Ip="
							+ x.getAttribute("Ip") + ", port="
							+ x.getAttribute("port"));

			try {
				if (Integer.parseInt(id) < nodesCount) {
					Address ref = topologyDescriptor.addNode(id, x
							.getAttribute("Ip"), Integer.parseInt(x
							.getAttribute("port")));

					if (id.equals("" + thisNodeNumber)) {
						topologyDescriptor.setLocalAddress(ref);
					}
				}
			} catch (NumberFormatException e1) {
				log.error("Error in parsing in \"node\" section");
				e1.printStackTrace();
			} catch (UnknownHostException e1) {
				log.error("Error in parsing in \"node\" section");
				e1.printStackTrace();
			}

		}

		NodeList n1 = e.getElementsByTagName("link");

		log.info("LINKS");

		// For every link
		for (int i = 0; i < n1.getLength(); i++) {
			Element x = (Element) n1.item(i);

			String src = x.getAttribute("src_id");

			int srcNodeId = Integer.parseInt(src);

			String dst = x.getAttribute("dst_id");

			int dstNodeId = Integer.parseInt(dst);

			if (!topologyDescriptor.isNodePresent(dstNodeId)
					|| !topologyDescriptor.isNodePresent(srcNodeId)) {

				continue;
				// log
				// .fatal("Error parsing \"links\" section\n A link contains a
				// reference to a
				// non-existent node");
				//
				// System.exit(1);

			}
			if (dstNodeId == thisNodeNumber && srcNodeId == thisNodeNumber) {

				log
						.fatal("Error parsing \"links\" section\n A link contains the same node both as source and as destination");

				System.exit(1);

			}

			// Store only the links which have the node as one of the two edges
			if (srcNodeId == thisNodeNumber || dstNodeId == thisNodeNumber) {

				String latency = x.getAttribute("latency");

				String loss_rate = x.getAttribute("loss_rate");

				String undirected = x.getAttribute("undirected");

				Address srcNode = topologyDescriptor
						.getNode(new BigInteger(src));

				Address dstNode = topologyDescriptor
						.getNode(new BigInteger(dst));

				// (dstNodeId == thisNodeNumber ? srcNode : dstNode),
				// (srcNodeId == thisNodeNumber ? srcNode : dstNode),

				LinkDescriptor linkDescriptor;

				if (!undirected.equals("") && undirected.equals("true")) {

					linkDescriptor = topologyDescriptor.addLink(
							(srcNodeId == thisNodeNumber ? srcNode : dstNode),
							(dstNodeId == thisNodeNumber ? srcNode : dstNode),
							(latency.equals("") ? 0 : Long.parseLong(latency)),
							(loss_rate.equals("") ? 0 : Double
									.parseDouble(loss_rate)));

				} else {

					linkDescriptor = topologyDescriptor.addLink(srcNode,
							dstNode, (latency.equals("") ? 0 : Long
									.parseLong(latency)),
							(loss_rate.equals("") ? 0 : Double
									.parseDouble(loss_rate)));

				}

				log.info(linkDescriptor);

			}

		}

		return topologyDescriptor;
	}

	public void warning(SAXParseException e) {
		log.error("Validation failed=" + e.getMessage());
	}

	public void error(SAXParseException e) {
		log.error("Validation failed=" + e.getMessage());
	}

	public void fatalError(SAXParseException e) {
		log.error("Validation failed=" + e.getMessage());
	}
}
