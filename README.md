

# remote-akka-test
Test sending big messages via remote akka

Build the packages using sbt-native-packaging, e.g. `sbt universal:packageBin`

Copy remoteakkatestserver-1.0.zip to server, unzip, cd to bin folder, and execute:

`sh remoteakkatestserver -Dconfig.file=../conf/application.conf`

Copy remoteakkatestclient-1.0.zip to client, unzip, cd to bin folder, and execute:

`sh remoteakkatestclient -Dconfig.file=../conf/application.conf`


# The problem
I was facing an issue where the responses to concurrent requests to remote actors were taking long time to respond, aka 1 request takes 300 ms, but 100 concurrent requests took almost 30 seconds to complete! The request size is small, but response size was about 120 kB before serialization. I couldn't reproduce the issue in this sample project using String, the real world scenario had deep nested case class.

# Analysis
I had 100 actors, threads, and even DB connections available - so ruled out resources issues there. Ensured that there is no synchronization issues in the code.
The fast response time for 1 to 10 requests ruled out serialization/deserialization speed. I was using java's default serializer which is inefficient.
I measured the actual processing time by the actors and it was constant.
So it boiled down to the transport layer. The ping times were super fast, under 1 ms. The servers were on a gigabit lan with excellent throughput. So was wondering if I actually hit a bug in the akka's remoting protocol! 
Then I played around with the config and finally found a breakthrough when I modified the buffer sizes in client and server. Turns out the default buffer size was too small and were being queud up with backoff delays! 

Relevant Akka debug log:
DEBUG test-app akka.remote.EndpointWriter - Drained buffer with maxWriteCount: 50, fullBackoffCount: 546, smallBackoffCount: 2, noBackoffCount: 1 , adaptiveBackoff: 2000


# Solution
The buffer size directly limits the number of concurrent requests and responses! So modified the send and receive buffer sizes to support the required concurrency in both client and server. The settings are under remote.netty.tcp
