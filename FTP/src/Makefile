JAVAC = javac
JAVACFLAGS =
SRC= datatypes/Datagram.java \
	datatypes/DatagramUtils.java \
	datatypes/MyProtocol.java \
	datatypes/TTPPacket.java \
	datatypes/TTPReceiver.java \
	datatypes/TTPSegment.java \
	datatypes/WindowSender.java \
	applications/ClientFtp.java \
	applications/ServerFtp.java \
	applications/TestFtp.java \
	services/DatagramService.java \
	services/TTPService.java \
	services/TTPConnection.java
CLS= $(SRC:.java=.class)

all:  $(CLS)


.SUFFIXES : .class .java
.java.class :
	$(JAVAC) $(JAVACFLAGS) $<

clean:
	rm -f datatypes/*.class
	rm -f applications/*.class
	rm -f services/*.class