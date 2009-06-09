package se.sics.kompics.wan.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Map;

import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.ssh.jsch.ScpWagon;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.FileUtils;

public class DefaultTransporter extends AbstractLogEnabled implements Transporter, Initializable {

	
	
	/**
	 * @plexus.requirement role="org.apache.maven.wagon.Wagon"
	 */
	private Map<String, StreamWagon> wagons;

	/**
	 * @plexus.configuration
	 */
	private String proxyUrl = null;

	private ProxyInfo proxyInfo;

	/**
	 * @see org.ambiance.transport.Transporter#get(java.lang.String, java.io.File)
	 */
	public void get(String url, File file) throws TransporterException {
		url = url.replaceAll(" ", "%20");
		
		String protocol = PathUtils.protocol(url);
		
		try {
			// Wagon-file does not support url string format (with %20 for ex.)
			if("file".equals(protocol))
				url = URLDecoder.decode(url, "UTF-8");
			
			Wagon wagon = new ScpWagon();
//			Wagon wagon = new LightweightHttpWagon();
//			Wagon wagon = wagons.get(protocol);
			Repository repo = getRepository(url);
			
//			if(wagon instanceof ScpWagon) {
//				((ScpWagon) wagon).setInteractive(false);
//				((ScpWagon) wagon).getKnownHostsProvider().storeKnownHosts(repo.getHost());
//			}
			
			wagon.connect(repo, getAuthentificationInfo(url), proxyInfo);
			wagon.get(PathUtils.filename(url), file);
			wagon.disconnect();
		} catch (Throwable t) {
			getLogger().error("Error while getting file", t);
			throw new TransporterException("Error while getting file", t);
		}
	}

	public InputStream getAsStream(String url) throws TransporterException {
		return getAsStream(url, 32768);
	}
	
	/**
	 * @see org.ambiance.transport.Transporter#getAsStream(java.lang.String, int)
	 */
	public InputStream getAsStream(String url, int bufferSize) throws TransporterException {
		url = url.replaceAll(" ", "%20");
		
		String protocol = PathUtils.protocol(url);
		
		try {
			// Wagon-file does not support url string format (with %20 for ex.)
			if("file".equals(protocol))
				url = URLDecoder.decode(url, "UTF-8");
			
			StreamWagon wagon = wagons.get(protocol);
			Repository repo = getRepository(url);

			InputData input = new InputData();
			input.setResource(new Resource(PathUtils.filename(url)));
			
			wagon.connect(repo, getAuthentificationInfo(url), proxyInfo);
			wagon.fillInputData(input);
			wagon.disconnect();
			
			return new BufferedInputStream(input.getInputStream(), bufferSize);
		} catch (Throwable t) {
			getLogger().error("Error while getting file", t);
			throw new TransporterException("Error while getting file", t);
		}

	}

	/**
	 * @see org.ambiance.transport.Transporter#put(java.io.File, java.lang.String)
	 */
	public void put(File file, String url) throws TransporterException {

		Wagon wagon = wagons.get(PathUtils.protocol(url));
		if (null == wagon)
			throw new TransporterException("Protocol [" + PathUtils.protocol(url) + "] not supported.");

		Repository repo = getRepository(url);

		AuthenticationInfo authInfo = getAuthentificationInfo(url);

		try {
			wagon.connect(repo, authInfo, proxyInfo);

			// if dest url not contains a filename
			if ("".equals(FileUtils.filename(url)))
				url += file.getName();

			wagon.put(file, PathUtils.filename(url));
			wagon.disconnect();
		} catch (Throwable t) {
			getLogger().error("Error while putting file", t);
			throw new TransporterException("Error while getting file", t);
		}
	}

	private Repository getRepository(String url) {
		Repository repo = new Repository();
		int index = url.indexOf(PathUtils.filename(url));
		if (index != 0)
			repo.setUrl(url.substring(0, url.indexOf(PathUtils.filename(url))));
		else
			repo.setUrl(url);
		return repo;
	}

	private AuthenticationInfo getAuthentificationInfo(String url) {
		AuthenticationInfo authInfo = null;
		if (null != PathUtils.user(url)) {
			authInfo = new AuthenticationInfo();
			authInfo.setUserName(PathUtils.user(url));
			authInfo.setPassword(PathUtils.password(url));
		}
		return authInfo;
	}

	public void initialize() throws InitializationException {
		// Initialize Proxy Info
		if (null == proxyUrl) {
			// If the proxyUrl is not configure, tries to recover from system
			proxyUrl = System.getenv("http_proxy");
		}

		if (proxyUrl != null && !proxyUrl.equals("")) {
			proxyInfo = new ProxyInfo();
			proxyInfo.setHost(PathUtils.host(proxyUrl));
			proxyInfo.setPort(PathUtils.port(proxyUrl));
		}
	}

}
