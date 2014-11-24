all:
	javac -g RTP.java

clean:
	rm *.class

reset:
	rm *.class
	javac -g RTP.java