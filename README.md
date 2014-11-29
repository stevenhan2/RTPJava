Instructions for running RTP basic server/client tests (Not FTA):

1. make clean
2. make test
3. on one terminal run java RTPTestServer 8888 (or any other port)
4. on one terminal run java RTPTestClient 8888 (or any other port, but they have to be the same)