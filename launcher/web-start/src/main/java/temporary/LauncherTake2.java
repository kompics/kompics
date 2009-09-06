package temporary;

import java.io.IOException;

import mine.ProcLauncher;
import se.sics.kompics.p2p.systems.bootstrap.server.BootstrapServerMain;
import se.sics.kompics.p2p.systems.cyclon.CyclonPeerMain;
import se.sics.kompics.p2p.systems.cyclon.monitor.server.CyclonMonitorServerMain;

public class LauncherTake2 {
	public static void main(String[] args) throws IOException,
			InterruptedException {

		String classPath = System.getProperty("java.class.path");

		ProcLauncher launcher = new ProcLauncher();
		launcher.addProcess(0, BootstrapServerMain.class.getCanonicalName(),
				classPath, new Configuration2(7001).set());
		launcher.addProcess(0, CyclonMonitorServerMain.class.getCanonicalName(),
				classPath, new Configuration2(7003).set());
		launcher.addProcess(1000, CyclonPeerMain.class.getCanonicalName(),
				classPath, new Configuration2(4001).set(), "-Dpeer.id=3");
		launcher.addProcess(1000, CyclonPeerMain.class.getCanonicalName(),
				classPath, new Configuration2(4003).set(), "-Dpeer.id=100");
		launcher.addProcess(1000, CyclonPeerMain.class.getCanonicalName(),
				classPath, new Configuration2(4005).set(), "-Dpeer.id=1024");
		launcher.addProcess(1000, CyclonPeerMain.class.getCanonicalName(),
				classPath, new Configuration2(4007).set(), "-Dpeer.id=2048");
		launcher.addProcess(1000, CyclonPeerMain.class.getCanonicalName(),
				classPath, new Configuration2(4009).set(), "-Dpeer.id=3192");
		launcher.addProcess(1000, CyclonPeerMain.class.getCanonicalName(),
				classPath, new Configuration2(4011).set(), "-Dpeer.id=4096");
		launcher.launchAll();
	}
}
