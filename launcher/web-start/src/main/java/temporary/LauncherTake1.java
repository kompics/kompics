package temporary;

import java.io.IOException;

import mine.ProcLauncher;
import se.sics.kompics.p2p.systems.bootstrap.server.BootstrapServerMain;
import se.sics.kompics.p2p.systems.chord.ChordPeerMain;
import se.sics.kompics.p2p.systems.chord.monitor.server.ChordMonitorServerMain;

public class LauncherTake1 {
	public static void main(String[] args) throws IOException,
			InterruptedException {

		String classPath = System.getProperty("java.class.path");

		ProcLauncher launcher = new ProcLauncher();
		launcher.addProcess(0, BootstrapServerMain.class.getCanonicalName(),
				classPath, new Configuration(7001).set());
		launcher.addProcess(0, ChordMonitorServerMain.class.getCanonicalName(),
				classPath, new Configuration(7003).set());
		launcher.addProcess(1000, ChordPeerMain.class.getCanonicalName(),
				classPath, new Configuration(4001).set(), "-Dpeer.id=3");
		launcher.addProcess(1000, ChordPeerMain.class.getCanonicalName(),
				classPath, new Configuration(4003).set(), "-Dpeer.id=100");
		launcher.addProcess(1000, ChordPeerMain.class.getCanonicalName(),
				classPath, new Configuration(4005).set(), "-Dpeer.id=1024");
		launcher.addProcess(1000, ChordPeerMain.class.getCanonicalName(),
				classPath, new Configuration(4007).set(), "-Dpeer.id=2048");
		launcher.addProcess(1000, ChordPeerMain.class.getCanonicalName(),
				classPath, new Configuration(4009).set(), "-Dpeer.id=3072");
		launcher.addProcess(1000, ChordPeerMain.class.getCanonicalName(),
				classPath, new Configuration(40011).set(), "-Dpeer.id=4096");
		launcher.addProcess(1000, ChordPeerMain.class.getCanonicalName(),
				classPath, new Configuration(40013).set(), "-Dpeer.id=5555");
		launcher.addProcess(1000, ChordPeerMain.class.getCanonicalName(),
				classPath, new Configuration(4015).set(), "-Dpeer.id=6789");
		launcher.launchAll();
	}
}
