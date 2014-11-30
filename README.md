Instructions for running RTP basic server/client tests (Not FTA):

RTPTestClient defaults to binding to port 4000 as a convenience for NetEmu. 

1. make clean
2. make test
3. NetEmu 8000
3. on one terminal run java RTPTestServer 4001 (or any other port)
4. on one terminal run java RTPTestClient 8000 (or any other port, but they have to be the same)

Instructions to run FTAClient and FTAServer

1. on the terminal run javac FTAServer.java 8000
2. on the terminal run java FTAServer 8000 (or any other port) localhost
3. on a different terminal run javac FTAClient.java
4. on the same terminal as number 3 run java FTAClient 8000 localhost
5. you will be provided with 2 options in FTAClient: connect-get and close. 
	connect-get connects to the server and gets the file from the server
	close exists the program.