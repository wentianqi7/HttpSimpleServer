import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import javax.imageio.ImageIO;

public class HttpHandlerThread implements Runnable {
	private Socket clientSock;
	private DataOutputStream outStream;
	private BufferedReader inStream;
	// store input as string
	private String buffer;
	// path of file, read from argument
	private String path;
	// hashmap used to save http error code and its contents
	private HashMap<Integer, byte[]> httpCode;
	// charset used for image transfer
	private final String CHARSET = "ISO-8859-1";

	@SuppressWarnings("serial")
	public HttpHandlerThread(Socket socket, String path) {
		this.clientSock = socket;
		this.path = path;
		outStream = null;
		inStream = null;
		buffer = "";
		// hashmap for part of the http status code
		httpCode = new HashMap<Integer, byte[]>() {
			{
				put(200, "HTTP/1.0 200 OK\r\nServer: Simple/1.0\r\n".getBytes());
				put(400,
						"HTTP/1.0 400 Bad Request\r\nServer: Simple/1.0\r\n\r\n"
								.getBytes());
				put(404,
						"HTTP/1.0 404 Not Found\r\nServer: Simple/1.0\r\n\r\n"
								.getBytes());
				put(501,
						"HTTP/1.0 501 Method Unimplemented\r\nServer: Simple/1.0\r\n\r\n"
								.getBytes());
				put(505,
						"HTTP/1.0 505 HTTP Version Not Supported\r\nServer: Simple/1.0\r\n\r\n"
								.getBytes());
			}
		};
	}

	@Override
	public void run() {
		try {
			/* Read the data echoed by the client */
			inStream = new BufferedReader(new InputStreamReader(
					clientSock.getInputStream()));
			/* Write the date to the client */
			outStream = new DataOutputStream(clientSock.getOutputStream());
			buffer = inStream.readLine().trim();
			System.out.println("Read from client "
					+ clientSock.getInetAddress() + ":" + clientSock.getPort()
					+ " " + buffer);

			HttpResponse(buffer, path, outStream);

			clientSock.close();
		} catch (IOException e) {
			clientSock = null;
		}
	}

	public void HttpResponse(String buffer, String path,
			DataOutputStream outStream) throws IOException {
		if (buffer == null) {
			// 400 bad request
			outStream.write(httpCode.get(400));
			outStream.flush();
			return;
		}
		// retrieve the first line of client request
		String[] lines = buffer.split("\r\n");
		if (lines.length < 1) {
			// 400 bad request
			outStream.write(httpCode.get(400));
			outStream.flush();
			return;
		}
		String request = lines[0];

		// check the request type
		if (!request.toLowerCase().startsWith("get")
				&& !request.toLowerCase().startsWith("head")) {
			// 501 not implement
			outStream.write(httpCode.get(501));
			outStream.flush();
			return;
		}

		String[] fields = request.trim().split(" ");
		if (fields.length != 3) {
			// 400 bad request
			outStream.write(httpCode.get(400));
			outStream.flush();
			return;
		}
		String reqType = fields[0];
		String subPath = fields[1];
		String httpVersion = fields[2];

		// check http version
		if (!httpVersion.toLowerCase().startsWith("http/")) {
			// 400 bad request
			outStream.write(httpCode.get(400));
			outStream.flush();
			return;
		} else if (!httpVersion.substring(5).startsWith("1.0")) {
			System.out.println(httpVersion.substring(5));
			// 505 http version not supported
			outStream.write(httpCode.get(505));
			outStream.flush();
			return;
		}

		// assign index.html to default path
		if (subPath.equals("/")) {
			subPath += "index.html";
		}

		// deal with valid get/head request
		ByteArrayOutputStream response = new ByteArrayOutputStream();
		String type;

		type = GetMime.getMimeType(subPath);

		// set default type as text/plain
		if (type == null) {
			type = "text/plain";
		}
		response.write("Content-Type: ".getBytes());
		response.write(type.getBytes());
		response.write("\r\n\r\n".getBytes());

		// only return head if request type is head
		if (reqType.toLowerCase().startsWith("head")) {
			outStream.write(response.toByteArray());
			outStream.flush();
			return;
		}

		// retrieve response body
		String url = path + subPath;

		// retrieve file
		byte[] outbuf = new byte[4096];
		int len;

		// byte[] data = Files.readAllBytes(file);
		FileInputStream in = new FileInputStream(url);
		while ((len = in.read(outbuf, 0, outbuf.length)) > 0) {
			outStream.write(outbuf, 0, len);
		}

		outStream.flush();
	}
}
