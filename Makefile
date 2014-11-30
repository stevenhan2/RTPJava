all:
	javac FTAServer.java
	javac FTAClient.java

clean:
	rm *.class

reset:
	rm *.class
	javac -g RTP.java

test:
	javac -g RTPTestServer.java
	javac -g RTPTestClient.java

