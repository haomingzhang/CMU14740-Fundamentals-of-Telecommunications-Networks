/**
 * test for our ftp service
 */
package applications;

import java.io.IOException;
import datatypes.DatagramUtils;

public class TestFtp {

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		
		if (args.length != 3){
			System.err.println("<serverPort> <clientPort> <ip(127.0.0.1)>");
			System.exit(0);
		}
		
		String ip = args[2];
		if (DatagramUtils.checkIP(ip) && DatagramUtils.checkPort(args[0]) && DatagramUtils.checkPort(args[1])) {
			short portServer = Short.parseShort(args[0]);
			short portClient = Short.parseShort(args[1]);
			// start server
			ServerFtp s = new ServerFtp(portServer);
			s.start();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {}
			// start multiple client
			ClientFtp c = new ClientFtp(portClient, portServer, "bigfile.txt");
			c.start();
			ClientFtp c1 = new ClientFtp((short)(portClient + 1), (short)(portServer + 1), "smallfile.txt");
			c1.start();
			s.join();
			c.join();
			c1.join();
			System.exit(0);
		} else {
			System.err.println("<serverPort> <clientPort> <ip(127.0.0.1)>");
			System.exit(0);
		}
	}

}
