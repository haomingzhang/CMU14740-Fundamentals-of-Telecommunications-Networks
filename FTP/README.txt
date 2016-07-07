Authors: Haoming Zhang, Ying Duan
How to test the code:
This code can be tested running TestFtp.java under src/applications folder
The main function need 3 parameters: serverPort, clientPort and ip(127.0.0.1)
For example, after running Makefile, execute the following command 
		java TestFtp 11470 12470 127.0.0.1
It will start 2 clients simultaniously to request a small file(393B) and a large
file(3.4MB) from the server respectively and store the files under clientRoot folder


Code directory:
src folder is the main code folder
	applications : ClientFtp.java - ftp client
				   ServerFtp.java - ftp server
				   TestFtp.java - test code
	datatypes : Datagram.java - datagram packet used by datagram service
				DatagramUtils.java - util function such as convert byte array to Object
				MyProtocol.java - defined all the flag value used by TTP service
				TTPPacket.java - defined the TTP packet for TTP service
				TTPSegment.java - defined the segmentation packet used by fragmentation
				WindowSender.java - defined the main sender thread for TTP service
	services: DatagramService.java - datagram service provided by writeup
			  TTPConnection.java - send and receive TTP packet on TTP connections
			  TTPService.java - used by TTP service to initiate connections
clientRoot is the client space to store files after fetching them from server
serverRoot is the server space to store files that are served to clients


