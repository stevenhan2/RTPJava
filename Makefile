all:
	javac -g RTP.java

clean:
	rm *.class

reset:
	rm *.class
	javac -g RTP.java

test:
	javac -g RTPTestServer.java
	javac -g RTPTestClient.java