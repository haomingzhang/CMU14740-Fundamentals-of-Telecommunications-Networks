
/**
 * This class implemented a simple web server which conforms to a subset
 * of HTTP 1.0 and also supports concurrent clients.
 */
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
	private static ServerSocket srvSock;

	public static void main(String args[]) {
		int port = Integer.parseInt(args[0]);

		/* Parse parameter and do args checking */
		if (args.length < 1) {
			System.err.println("Usage: java Simple <port> <Absolute path to www directory>");
			System.exit(1);
		}

		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			System.err.println("Usage: java Simple <port> <Absolute path to www directory>");
			System.exit(1);
		}

		if (port > 65535 || port < 1024) {
			System.err.println("Port number must be in between 1024 and 65535");
			System.exit(1);
		}
		
		// check www folder
		File www = new File(args[1]);
		if (!www.isDirectory()) {
			System.err.println("Usage: java Simple <port> <Absolute path to www directory>");
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
				// System.out.println("listening");
				clientSock = srvSock.accept();
				System.out.println(
						"Accepted new connection from " + clientSock.getInetAddress() + ":" + clientSock.getPort());
				new Thread(new workerThread(clientSock, args[1])).start();
			} catch (IOException e) {
				continue;
			}
		}
	}
}

/**
 * Every time the server accepts a new connection, it will spawn a worker thread to 
 * read requests and response to clients.
 */
class workerThread implements Runnable {

	Socket clientSock;
	String www;
	
	public workerThread(Socket s, String rootDir) {
		clientSock = s;
		www = rootDir;
	}
	
	@Override
	public void run() {
		String buffer = null;
		BufferedReader inStream = null;
		DataOutputStream outStream = null;
		
		try {
			inStream = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
			outStream = new DataOutputStream(clientSock.getOutputStream());
			ArrayList<String> request = new ArrayList<String>();
			/* Read the data send by the client */
			while ((buffer = inStream.readLine()) != null) {
				if (buffer.length() == 0) {
					break;
				}
				// System.out.println(buffer.length());
				request.add(new String(buffer));

				System.out.println("Read from client " + clientSock.getInetAddress() + ":" + clientSock.getPort()
						+ " " + buffer);

			}

			System.out.println("processing:");
			System.out.println(request.get(0));

			/*
			 * Echo the data back and flush the stream to make sure that the
			 * data is sent immediately
			 */

			outStream.writeBytes(parse(request, www));
			outStream.flush();
			/* Interaction with this client complete, close() the socket */
			clientSock.close();
		} catch (IOException e) {
			clientSock = null;
			System.exit(1);
		}
		
	}
	
	/**
	 * check version number of request
	 * @param http
	 * @return 0 for HTTP1.0, 1 for HTTP 1.1 and -1 otherwise
	 */
	public static int checkVersion(String http) {
		String[] version = (http.split("/"))[1].split("\\.");
		if (version[0].equals("1") && (version[1].equals("0") || version[1].equals("1"))) {
			// 505
			return Integer.parseInt(version[1]);
		} else {
			return -1;
		}
	}

	/**
	 * build response for error condition
	 * 
	 * @param code
	 *            status code
	 * @param str
	 *            status message
	 * @return formated response string
	 */
	public static String sendError(int code, String str) {
		System.out.println("HTTP Error Code:" + code);
		String message = "";
		String explanation = "";

		switch (code) {
		case 400:
			message = "Bad request syntax ('" + str + "').";
			explanation = "400 = Bad request syntax or unsupported method.";
			break;
		case 404:
			message = "Not Found.";
			explanation = "404 = Nothing matches the given URI.";
			break;
		case 500:
			message = "Internal Server Error.";
			explanation = "500 = Server got itself in trouble.";
			break;
		case 501:
			message = "Unsupported method ('" + str + "').";
			explanation = "501 = Server does not support this operation.";
			break;
		case 505:
			message = "HTTP Version not supported.";
			explanation = "505 = Cannot fulfill request..";
			break;
		}

		// build body
		StringBuilder responseBody = new StringBuilder(
				"<head>\n<title>Error response</title>\n</head>\n<body>\n<h1>Error response</h1>\n<p>Error code ");
		responseBody.append(Integer.toString(code));
		responseBody.append(".\n<p>Message: ");
		responseBody.append(message);
		responseBody.append("\n<p>Error code explanation: ");
		responseBody.append(explanation);
		responseBody.append("\n</body>\n");

		Response response = new Response(code, responseBody.length(), "text/html", message);
		return response.buildResponse(responseBody.toString());
	}

	/**
	 * parse request string, look up for requested file and build response
	 * message
	 * 
	 * @param request
	 *            request string
	 * @param pathWWW
	 *            rootdir of server
	 * @return formated response string
	 */
	public static String parse(ArrayList<String> request, String pathWWW) {

		String[] words = request.get(0).split(" ");
		if (words.length != 3) {
			return sendError(400, request.get(0));
		}

		int version;
		if ((version = checkVersion(words[2])) != 0) {
			return sendError(505, words[2].split("/")[1]);
		}

		/*
		 * GET
		 */
		if (words[0].equals("GET")) {
			// complete path
			String fullPath = pathWWW + words[1];
			File file = new File(fullPath);
			if (file.isDirectory()) {
				file = new File(file, "index.html").getAbsoluteFile();
			}
			// if 404
			if (!file.exists()) {
				System.err.println("404: " + file.getPath());
				return sendError(404, null);
			}

			String name = file.getName();
			String extension = null;
			String basename = name;
			if (name.lastIndexOf('.') > 0) {
				extension = name.substring(name.lastIndexOf('.'));
				basename = name.substring(0, name.lastIndexOf('.'));
			}

			// build response
			try {
				String mime = GetMime.getMimeType(extension.toLowerCase());
				if (mime == null) {
					mime = "application/octet-stream";
				}
				FileReader fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
				String line;
				StringBuilder sb = new StringBuilder();
				while ((line = br.readLine()) != null) {
					sb.append(line);
					sb.append("\r\n");
				}
				Response response = new Response(200, file.length(), mime, "OK");
				br.close();
				fr.close();
				return response.buildResponse(sb.toString());
			} catch (IOException e) {
				e.printStackTrace();
				return sendError(500, null);
			}

			/*
			 * HEAD
			 */
		} else if (words[0].equals("HEAD")) {
			// complete path
			String fullPath = pathWWW + words[1];
			File file = new File(fullPath);
			if (file.isDirectory()) {
				file = new File(file, "index.html").getAbsoluteFile();
			}
			// if 404
			if (!file.exists()) {
				System.err.println("404: " + file.getPath());
				return sendError(404, null);
			}

			// get mime type
			String name = file.getName();
			String extension = null;
			String basename = name;
			if (name.lastIndexOf('.') > 0) {
				extension = name.substring(name.lastIndexOf('.'));
				basename = name.substring(0, name.lastIndexOf('.'));
			}
			String mime;
			try {
				mime = GetMime.getMimeType(extension.toLowerCase());
				if (mime == null) {
					mime = "application/octet-stream";
				}
				Response response = new Response(200, file.length(), mime, "OK");
				return response.buildResponse(null);
			} catch (IOException e) {
				e.printStackTrace();
				return sendError(500, null);
			}
			/*
			 * Other methods, 501
			 */
		} else {
			return sendError(501, words[0]);
		}
	}
	
}
