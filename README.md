Visit https://github.com/wufufufu/RTPJava for markdown README for convenience

##*Instructions for running RTP basic server/client tests (Not FTA):*
RTPTestClient defaults to binding to port 4000 as a convenience for NetEmu. 
1. make clean
2. make test
3. NetEmu 8000
3. on one terminal run java RTPTestServer 4001 (or any other port)
4. on one terminal run java RTPTestClient 8000 (or any other port, but they have to be the same)

##*Instructions to run FTAClient and FTAServer*
1. make
2. NetEmu 8000
3. java FTAServer 8000 (or any other port) localhost 8000
4. java FTAClient 8000 localhost
5. you will be provided with 2 options in FTAClient: connect-get and close. 
	connect-get connects to the server and gets the file from the server
	close exists the program.

##*What we implemented:*
* YES	connect-get
* NO	get 					RTP is bidirectional, but I ran out of time
* NO	connect 				RTP is bidirectional, but I ran out of time
* YES	post
* NO	terminate				No time to implement, but would be part of * state system with FIN flag and closing states similar to TCP
* NO	window 					Did not implement pipelining
* NO	disconnect 				No time to implement, but would be part of state system with FIN flag and closing

*I was able to send a file using:*
0. make
1. python NetEmu.py 8000 -l 10 -c 10 -d 10 -D 100 -r 25
2. java FTAServer 5001 localhost 8000
3. java FTAClient 5000 localhost 8000
4. connect-get /Users/shan/file.txt