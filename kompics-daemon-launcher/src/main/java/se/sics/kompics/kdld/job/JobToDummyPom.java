package se.sics.kompics.kdld.job;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;

import se.sics.kompics.kdld.daemon.Daemon;

import com.sun.org.apache.xpath.internal.XPathAPI;

public class JobToDummyPom extends Job implements Serializable {


	private static final long serialVersionUID = -6014083364061777714L;

	private final String DUMMY_POM = "src/main/resources/se/sics/kompics/kdld/pom.xml";

	private final String POM_FILENAME = "pom.xml";

	private final String filePath;
	private final String sepStr;

	private final File xmlFile = new File(DUMMY_POM);

	private final Document xmlDoc;

	public JobToDummyPom(int id, String repoId, String repoUrl, String repoName, String groupId,
			String artifactId, String version, String mainClass, List<String> args)
			throws DummyPomConstructionException {
		super(id,repoId,repoUrl,repoName,groupId,artifactId, version, mainClass,args);


		String groupPath = groupId.replace('.', File.separatorChar);
		char[] separator = new char[1];
		separator[0] = File.separatorChar;
		sepStr = new String(separator);
		filePath = groupPath + sepStr + artifactId;

		// Setup Dom Object

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new DummyPomConstructionException(e.getMessage());
		}

		try {
			xmlDoc = builder.parse(xmlFile);

			// Normalize text representation. Collapses adjacent text nodes into
			// one node.
			xmlDoc.getDocumentElement().normalize();

		} catch (SAXException e) {
			e.printStackTrace();
			throw new DummyPomConstructionException("Problem with Processing XML dummy pom.xml");
		} catch (IOException e) {
			e.printStackTrace();
			throw new DummyPomConstructionException("IOException when processing XML dummy pom.xml");
		}

		updateMainClass(mainClass, args);

		updateDependency();

		updateRepository();

		updateJobId(id);
		// printElements("root", root, 6);
	}

	public boolean createDummyPomFile() throws DummyPomConstructionException {

		try {
			// Prepare the DOM document for writing
			Source source = new DOMSource(xmlDoc);

			if (new File(Daemon.KOMPICS_HOME, filePath).exists() == false) {

				if ((new File(Daemon.KOMPICS_HOME, filePath).mkdirs()) == false) {
					throw new DummyPomConstructionException(
							"Couldn't create directories for pom.xml file : " + Daemon.KOMPICS_HOME
									+ sepStr + filePath
									+ "\nCheck file permissions for this directory.");
				}
			}
			String fileName = filePath + sepStr + POM_FILENAME;
			File file = new File(Daemon.KOMPICS_HOME, fileName);

			file.createNewFile();

			Result result = new StreamResult(file);

			// Write the DOM document to the file
			Transformer xformer = TransformerFactory.newInstance().newTransformer();
			xformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
			throw new DummyPomConstructionException(e.getMessage());
		} catch (TransformerException e) {
			e.printStackTrace();
			throw new DummyPomConstructionException(e.getMessage());
		} catch (IOException e) { // file couldn't be created.
			throw new DummyPomConstructionException(
					"Couldn't create pom.xml file : " + Daemon.KOMPICS_HOME
							+ sepStr + filePath
							+ "\nCheck file permissions for this directory.");				
		}

		return true;
	}

	private void printElements(String parentName, Node root, int numLevels) {

		if (root.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
			for (int k = 5; k > numLevels; k--) {
				System.out.println("  ");
			}

			parentName += ":" + root.getNodeName();
			System.out.println(parentName);
		} else if (root.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
			Text t = (Text) root;
			parentName += ":" + t.getData();
			System.out.println(parentName);
		}

		for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
			switch (child.getNodeType()) {
			case org.w3c.dom.Node.ELEMENT_NODE:
				NodeList grandChildren = child.getChildNodes();
				if (numLevels > 0) {
					for (int j = 0; j < grandChildren.getLength(); j++) {
						printElements(parentName, grandChildren.item(j), numLevels - 1);
					}
				}
				break;
			default:
				break;
			}
		}
	}
	
	
	private void updateJobId(int jobId) throws DummyPomConstructionException {
		updateElement("/project/properties/jobId", Integer.toString(jobId));
	}
	

	private void updateDependency() throws DummyPomConstructionException {
		updateElement("/project/dependencies/dependency/groupId", groupId);
		updateElement("/project/dependencies/dependency/artifactId", artifactId);
		updateElement("/project/dependencies/dependency/version", version);
	}

	private void updateRepository() throws DummyPomConstructionException {
		updateElement("/project/repositories/repository/id", repoId);
		updateElement("/project/repositories/repository/name", repoName);
		updateElement("/project/repositories/repository/url", repoUrl);
	}

	private void updateMainClass(String mainClass, List<String> args)
			throws DummyPomConstructionException {

		try {
			// update mainClass
			NodeIterator nl = XPathAPI.selectNodeIterator(xmlDoc,
					"/project/build/plugins/plugin/configuration/arguments/" + "argument");
			Node n;
			while ((n = nl.nextNode()) != null) {
				Element e = (Element) n;
				System.out.println(e.getTextContent());
				if (e.getTextContent().compareTo("se.sics.kompics.manual.example1.Root") == 0) {
					e.setTextContent(mainClass);
				}
			}

			// update args
			NodeIterator nArgs = XPathAPI.selectNodeIterator(xmlDoc,
					"/project/build/plugins/plugin/configuration/arguments");
			while ((n = nArgs.nextNode()) != null) {

				if (n.getNodeType() != Node.TEXT_NODE) {
					for (String arg : args) {
						Element a = xmlDoc.createElement("argument");
						a.setTextContent("-D" + arg);
						n.appendChild(a);
					}
				}
			}

		} catch (TransformerException e) {
			e.printStackTrace();
			throw new DummyPomConstructionException(e.getMessage());
		}

	}
	
	
	private void updateElement(String element, String updatedText) throws DummyPomConstructionException
	{
		try
		{
			NodeIterator groupIter = XPathAPI.selectNodeIterator(xmlDoc,
			element);
			Node n1 = groupIter.nextNode();
			Element e1 = (Element) n1;
			e1.setTextContent(updatedText);
		} catch (TransformerException e) {
			e.printStackTrace();
			throw new DummyPomConstructionException(e.getMessage());
		}			
	}

}