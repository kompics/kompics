package se.sics.kompics.launch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Properties;

public class ProcessLauncher {

	public class Proc extends Thread implements Comparable<Proc> {
		private long delay;
		private String mainComponent;
		private String classPath;
		private Properties pProps;
		private String[] sProps;
		private int index;
		private Process process;
		private int processCount;
		private ProcessFrame mainFrame;
		private BufferedWriter input;

		public Proc(long delay, String mainComponent, String classPath,
				int index, Properties pProps, String sProps[]) {
			super();
			this.delay = delay;
			this.mainComponent = mainComponent;
			this.classPath = classPath;
			this.pProps = pProps;
			this.sProps = sProps;
			this.index = index;
		}

		public long getDelay() {
			return delay;
		}

		public String getMainComponent() {
			return mainComponent;
		}

		public String getClassPath() {
			return classPath;
		}

		@Override
		public int compareTo(Proc that) {
			if (this.delay < that.delay)
				return -1;
			if (this.delay > that.delay)
				return 1;
			return 0;
		}

		@SuppressWarnings("unchecked")
		public void run() {
			mainFrame = new ProcessFrame(this, index, processCount);
			mainFrame.setVisible(true);

			List<String> arguments = new ArrayList<String>();
			arguments.add("java");
			arguments.add("-classpath");
			arguments.add(classPath);

			Enumeration<String> keys = (Enumeration<String>) pProps
					.propertyNames();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				String value = pProps.getProperty(key);

				arguments.add("-D" + key + "=" + value);
			}

			String title = "";
			for (String s : sProps) {
				arguments.add(s);
				title += " " + s;
			}
			mainFrame.setTitle(mainFrame.getTitle() + title);

			arguments.add(mainComponent);

			ProcessBuilder pb = new ProcessBuilder(arguments);
			pb.redirectErrorStream(true);

			try {
				process = pb.start();
				BufferedReader out = new BufferedReader(new InputStreamReader(
						process.getInputStream()));
				input = new BufferedWriter(new OutputStreamWriter(process
						.getOutputStream()));

				String line;
				do {
					line = out.readLine();
					if (line != null) {
						if (line.equals("2DIE")) {
							if (process != null) {
								process.destroy();
								process = null;
							}
							break;
						}
						mainFrame.append(line + "\n");
					}
				} while (line != null);
			} catch (Throwable e) {
				mainFrame.append(e.getMessage());
			}
		}

		public final void kill(boolean dispose) {
			if (process != null) {
				process.destroy();
				process = null;
				mainFrame.append("Killed.");
				if (dispose)
					mainFrame.dispose();
			}
			if (mainFrame.isDisplayable() && dispose) {
				mainFrame.dispose();
			}
		}

		public final void killAll() {
			killAllProcs();
		}

		final void input(String string) throws IOException {
			input.write(string);
			input.write("\n");
			input.flush();
		}

		final void globalInput(String string) throws IOException {
			input(string);
		}
	}

	private PriorityQueue<Proc> procs = new PriorityQueue<Proc>();

	private long delay = 0;
	private int count = 0;
	private boolean stop = false;

	public void addProcess(long delay, String mainComponent, String classPath,
			Properties pProps, String... sProps) {
		this.delay += delay;
		count++;
		Proc p = new Proc(this.delay, mainComponent, classPath, count, pProps,
				sProps);
		procs.add(p);
	}

	public void launchAll() {
		long d = 0, dd;
		for (Proc p : procs) {
			p.processCount = count;
			dd = p.getDelay() - d;
			if (dd > 0) {
				try {
					Thread.sleep(dd);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if (!stop) {
				// launch p
				p.start();
			}
		}
	}

	public final void killAllProcs() {
		stop = true;
		for (Proc p : procs) {
			p.kill(true);
		}
	}
}
