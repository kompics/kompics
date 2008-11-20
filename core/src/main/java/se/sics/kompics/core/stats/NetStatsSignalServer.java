package se.sics.kompics.core.stats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class NetStatsSignalServer extends Thread {

	private int port;

	private ServerSocket serverSocket;
	private Socket socket;

	public NetStatsSignalServer(int port) {
		this.port = port;
	}

	@Override
	public void run() {
		init();

		BufferedReader in = new BufferedReader(new InputStreamReader(socket
				.getInputStream()));
		while (true) {
			String line = in.readLine();
			if (line == null)
				break;

			if (line.startsWith("measure")) {
				line = line.substring(7);
				int cnt = Integer.parseInt(line);
				server.measure(cnt);
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

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
