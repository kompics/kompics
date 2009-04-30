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

import se.sics.kompics.Request;

import com.sun.org.apache.xpath.internal.XPathAPI;

public abstract class Job extends Request implements Serializable  {


	private static final long serialVersionUID = 3831799496529156008L;

	private final String DUMMY_POM = "src/main/resources/se/sics/kompics/kdld/pom.xml";

	private final String POM_FILENAME = "pom.xml";

	private final int id;

	private final String filePath;
	private final String sepStr;

	private final String repoId;
	private final String repoUrl;
	private final String repoName;

	private final String groupId;
	private final String artifactId;
	private final String version;

	private final String mainClass;
	private final List<String> args;

	private final String envPath;

	private final File xmlFile = new File(DUMMY_POM);

	private final Document xmlDoc;

	public Job(int id, String repoId, String repoUrl, String repoName, String groupId,
			String artifactId, String version, String mainClass, List<String> args)
			throws DummyPomConstructionException {
		this.id = id;
		this.repoId = repoId;
		this.repoUrl = repoUrl;
		this.repoName = repoName;
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.mainClass = mainClass;
		this.args = args;

		// Set the directory for storing/accessing the DummyPom file. 
//		envPath = System.getProperty("java.io.tmpdir");
		envPath = System.getProperty("kompics.home");
		if (envPath == null)
		{
			throw new IllegalStateException("kompics.home environment variable was not set.");
		}
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

		// printElements("root", root, 6);
	}

	public boolean writeToFile() throws DummyPomConstructionException {

		if (envPath == null)
		{
			throw new 
				IllegalStateException("You must call 'initEnv', before writing dummy pom to file");
		}
		
		try {
			// Prepare the DOM document for writing
			Source source = new DOMSource(xmlDoc);

			if (new File(envPath).exists() == false) {
				throw new IllegalStateException(
						"Tmp directory not found for installing dummy poms.");
			}

			if (new File(envPath, filePath).exists() == false) {

				if ((new File(envPath, filePath).mkdirs()) == false) {
					throw new DummyPomConstructionException(
							"Couldn't create directories for pom.xml file : " + envPath + sepStr
									+ filePath + "\nCheck file permissions for this directory.");
				}
			}
			String fileName = filePath + sepStr + POM_FILENAME;
			File file = new File(envPath, fileName);

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

		}

		return true;
	}

//	public void assemblyAssembly() throws DummyPomConstructionException {
//		mvn("assembly:assembly");
//	}

//	public void execExec() throws DummyPomConstructionException {
//		mvn("exec:exec");
//	}
//
//	private void mvn(String command) throws DummyPomConstructionException {
//		Configuration configuration = new DefaultConfiguration().setUserSettingsFile(
//				MavenEmbedder.DEFAULT_USER_SETTINGS_FILE).setClassLoader(
//				Thread.currentThread().getContextClassLoader()).setGlobalSettingsFile(
//				MavenEmbedder.DEFAULT_GLOBAL_SETTINGS_FILE);
//
//		MavenEmbedder embedder = null;
//		try {
//			embedder = new MavenEmbedder(configuration);
//		} catch (MavenEmbedderException e1) {
//			e1.printStackTrace();
//			throw new DummyPomConstructionException(e1.getMessage());
//		}
//
//		File pomDir = new File(envPath, filePath);
//		MavenExecutionRequest requestExec = new DefaultMavenExecutionRequest().setBaseDirectory(
//				pomDir).setGoals(Arrays.asList(new String[] { command }));
//		requestExec.setInteractiveMode(false);
//		requestExec.setLoggingLevel(1);
//		requestExec.setShowErrors(true);
//		mavenExec(requestExec, embedder);
//	}
//
//	private void mavenExec(MavenExecutionRequest request, MavenEmbedder embedder)
//			throws DummyPomConstructionException {
//		MavenExecutionResult result = embedder.execute(request);
//		// ----------------------------------------------------------------------------
//		// You may want to inspect the project after the execution.
//		// ----------------------------------------------------------------------------
//		if (mavenResult(result, embedder) == false) {
//			throw new DummyPomConstructionException("Maven exec:exec problem");
//		}
//		MavenProject project = result.getProject();
//
//		// Do something with the project
//		String gId = project.getGroupId();
//		String aId = project.getArtifactId();
//		String v = project.getVersion();
//		String name = project.getName();
//		String environment = project.getProperties().getProperty("environment");
//		System.out.println("You are working in the '" + environment + "' environment! " + gId + ":"
//				+ aId + ":" + v + " - " + name);
//	}
//
//	private boolean mavenResult(MavenExecutionResult result, MavenEmbedder embedder)
//			throws DummyPomConstructionException {
//		if (result.hasExceptions()) {
//			try {
//				String failMsg = ((Exception) result.getExceptions().get(0)).getMessage();
//				System.err.println(failMsg);
//				embedder.stop();
//			} catch (MavenEmbedderException e) {
//				throw new DummyPomConstructionException(e.getMessage());
//			}
//			return false;
//		}
//		return true;
//	}

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

	private void updateDependency() throws DummyPomConstructionException {
		try {
			NodeIterator groupIter = XPathAPI.selectNodeIterator(xmlDoc,
					"/project/dependencies/dependency/groupId");
			Node n1 = groupIter.nextNode();
			Element e1 = (Element) n1;
			e1.setTextContent(groupId);
			NodeIterator artifactIter = XPathAPI.selectNodeIterator(xmlDoc,
					"/project/dependencies/dependency/artifactId");
			Element e2 = (Element) artifactIter.nextNode();
			e2.setTextContent(artifactId);
			NodeIterator versionIter = XPathAPI.selectNodeIterator(xmlDoc,
					"/project/dependencies/dependency/version");
			Element e3 = (Element) versionIter.nextNode();
			e3.setTextContent(version);

		} catch (TransformerException e) {
			e.printStackTrace();
			throw new DummyPomConstructionException(e.getMessage());
		}

	}

	private void updateRepository() throws DummyPomConstructionException {
		try {
			NodeIterator groupIter = XPathAPI.selectNodeIterator(xmlDoc,
					"/project/repositories/repository/id");
			Node n1 = groupIter.nextNode();
			Element e1 = (Element) n1;
			e1.setTextContent(repoId);

			NodeIterator artifactIter = XPathAPI.selectNodeIterator(xmlDoc,
					"/project/repositories/repository/name");
			Element e2 = (Element) artifactIter.nextNode();
			e2.setTextContent(repoName);

			NodeIterator versionIter = XPathAPI.selectNodeIterator(xmlDoc,
					"/project/repositories/repository/url");
			Element e3 = (Element) versionIter.nextNode();
			e3.setTextContent(repoUrl);

		} catch (TransformerException e) {
			e.printStackTrace();
			throw new DummyPomConstructionException(e.getMessage());
		}

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

	public int getId() {
		return id;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getRepoId() {
		return repoId;
	}

	public String getRepoUrl() {
		return repoUrl;
	}

	public String getRepoName() {
		return repoName;
	}

	public String getMainClass() {
		return mainClass;
	}
	
	public List<String> getArgs() {
		return args;
	}
	
	public String[] getArgsAsArray() {
		return args.toArray(new String[args.size()]);
	}
}
