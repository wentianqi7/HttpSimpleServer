import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Simple {
	private static ServerSocket srvSock;

	public static void main(String args[]) {
		String buffer = null;
		String path = "";
		int port = 8080;
		BufferedReader inStream = null;
		DataOutputStream outStream = null;

		/* Parse parameter and do args checking */
		if (args.length < 1) {
			System.err.println("Usage: java Server <port_number> <www_path>");
			System.exit(1);
		}

		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			System.err.println("Invalid port number");
			System.err.println("Usage: java Server <port_number> <www_path>");
			System.exit(1);
		}

		try {
			path = args[1].toString();
		} catch (Exception e) {
			System.err.println("Invalid www path");
			System.err.println("Usage: java Server <port_number> <www_path>");
			System.exit(1);
		}

		if (port > 65535 || port < 1024) {
			System.err.println("Port number must be in between 1024 and 65535");
			System.exit(1);
		}

		try {
			/*
			 * Create a socket to accept() client connections. This combines
			 * socket(), bind() and listen() into one call. Any connection
			 * attempts before this are terminated with RST.
			 */
			srvSock = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("Unable to listen on port " + port);
			System.exit(1);
		}

		while (true) {
			Socket clientSock;
			try {
				/*
				 * Get a sock for further communication with the client. This
				 * socket is sure for this client. Further connections are still
				 * accepted on srvSock
				 */
				clientSock = srvSock.accept();
				System.out.println("Accpeted new connection from "
						+ clientSock.getInetAddress() + ":"
						+ clientSock.getPort());

				// create a http handler thread to achieve concurrency
				HttpHandlerThread httpHandler = new HttpHandlerThread(
						clientSock, path);
				Thread thread = new Thread(httpHandler);
				thread.start();
			} catch (IOException e) {
				clientSock = null;
				continue;
			}
		}
	}
}
