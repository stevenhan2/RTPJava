Instructions for running RTP basic server/client tests (Not FTA):

1. make clean
2. make test
3. on one terminal run java RTPTestServer 8888 (or any other port)
4. on one terminal run java RTPTestClient 8888 (or any other port, but they have to be the same)

Instructions to run FTAClient and FTAServer

1. run javac RTPTestServer.java 8000
2. run java RTPTestServer 8000 (or any other port) localhost
3. run javac RTPTestServer.java
4. run java RTPTestServer 8000 localhost