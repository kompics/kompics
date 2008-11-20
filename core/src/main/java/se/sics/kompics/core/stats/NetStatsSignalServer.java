package se.sics.kompics.core.stats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class NetStatsSignalServer extends Thread {

	private int port;
	private NetStatsServer server;

	private ServerSocket serverSocket;
	private Socket socket;
	private BufferedReader in;
	private BufferedWriter out;

	public NetStatsSignalServer(int port, NetStatsServer server) {
		this.port = port;
		this.server = server;
	}

	@Override
	public void run() {
		init();

		// signal client to start
		try {
			out.write("CONTINUE\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Signalled client to continue");

		while (true) {
			String line = null;
			try {
				line = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (line == null)
				break;

			if (line.startsWith("measure")) {
				line = line.substring(7);
				int cnt = Integer.parseInt(line);
				server.measure(cnt);
				
				// signal client to continue
				try {
					out.write("CONTINUE\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else if (line.startsWith("QUIT")){
				System.out.println("Quitting.");
				System.exit(0);
			} else {
				System.out
						.println("Ignored. Type measure4000 if you just joined 4000.");
			}
		}

		System.err.println("SignalServer terminated!");
	}

	private void init() {
		try {
			serverSocket = new ServerSocket(port);
			socket = serverSocket.accept();
			in = new BufferedReader(new InputStreamReader(socket
					.getInputStream()));
			out = new BufferedWriter(new OutputStreamWriter(socket
					.getOutputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
