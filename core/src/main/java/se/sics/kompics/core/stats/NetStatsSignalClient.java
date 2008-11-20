package se.sics.kompics.core.stats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class NetStatsSignalClient extends Thread {

	private int port;
	private SignalContinueHandler continueHandler;

	private Socket socket;
	private BufferedReader in;
	private BufferedWriter out;

	public NetStatsSignalClient(int port, SignalContinueHandler continueHandler) {
		this.port = port;
		this.continueHandler = continueHandler;
	}

	@Override
	public void run() {
		init();

		while (true) {
			String line = null;
			try {
				line = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (line == null)
				break;

			if (line.startsWith("CONTINUE")) {

				String command = continueHandler.cont();

				// signal server to continue
				try {
					out.write(command + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.out
						.println("Ignored. Type measure4000 if you just joined 4000.");
			}
		}

		System.err.println("SignalClient terminated!");
	}

	private void init() {
		try {
			InetSocketAddress socketAddress = new InetSocketAddress(System
					.getProperty("statServer"), port);

			socket = new Socket();
			socket.connect(socketAddress);

			in = new BufferedReader(new InputStreamReader(socket
					.getInputStream()));
			out = new BufferedWriter(new OutputStreamWriter(socket
					.getOutputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
