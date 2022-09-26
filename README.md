# Distributed Group Membership

This project provides the ability to maintain a distributed group membership (failure detector) based on SWIM protocol.

## Design
We maintain full membership list at each machine and use ring as backbone.
Apart from introducer, each member has 2 important threads. 
1. To run a tcp server (to talk to introducer, disseminate join/leave msg) 
2. Failure detection protocol messages (Ping/Ack) over UDP 
Our implementation is scalable, since there is no limitation on number of nodes for failure detection.
Our messages are marshalled. We are converting the message object into byte array. 

## Joining 
Introducer node is responsible for new machine joining the group but introducer itself is not part of the group. 
Introducer runs a TCP server thread which listens to incoming join requests and processes each request in a separate thread (concurrent requests are served).
It maintains a queue of all the joined machines so far. For each join request, introducer checks the queue for any alive machines and provides the details of a machine that is alive over tcp connection to the new machine. 
The new machine requests (tcp) the machine already part of the group for the latest membership list and adds itself to the membership list. Once it's added successfully, the new machine disseminates its information
to the rest of the members in the group, so that they can update their membership list. 
Introducer is fault tolerant and introducer failure only limits new joins i.e, even if introducer fails, the failure detection continues. 

## Leaving
When a machine is leaving the group, it removes itself from the membership list and disseminates a leave message to the rest of the members of the group, so that they can update their membership list. 

## Failure detection
For a protocol time of 1.5 seconds, each member pings its successor and waits for certain time period for acknowledgement. If it does not receive any acknowledgement in the given time period then it disseminates a crash message to the 
rest of the members to update their membership list. Since it has removed the crashed member entry, in the next iteration, it pings the successor of the crashed member. If there are 3 consecutive crashes, then in a period of 4.5 seconds, it is detected. 
Thus, we meet the 5s completeness for 3 consecutive failures using just 1 monitor per node. It is then updated in all the machines quickly, since we are disseminating the crash message. 
Single machine failure is detected within 1.5s if it is a good network, if there is network latency, it will be detected in next time period. If we have more than 4 failures, it is detected after 5s. 

Since join/leave/crash uses dissemination to update membership list in the group, it takes less than a second, hence 3 consecutive failure is reflected within 6 seconds across all groups. 
We can achieve the completeness also by monitoring 3 successors for a period of 5s. Our code provides the flexibility to alter protocol time and number of successor/predecessor to be monitored. 

## Membership List
The membership list is a sorted tree-set, treated circularly (i.e. the last entry's successor is the first entry), and sorted. This facilitates fast insertion and deletion, while ensuring that every list has the same order at every process. This makes determining successors simple, no matter how often the list is updated. Methods are provided to add/delete entries, get $n$ successors, change the owner, and present the list as a readable string. The list also implements Serializable so it can be sent as a byte array, and Iterable for easy access to all entries.

Each entry contains a hostname, port, and join timestamp. The entry implements Serializable for messaging over the network, and compares first on timestamp, then hostname, then port, for sorting order.

## Rejoin
Membership list stores the ip address, port and joining timestamp (incarnation number) of each of the members. 
Since we are treating crash and leave as the same, by removing the member from membership list, for rejoin it's the same process as joining a new machine. 

## Logging
Logs are located in `/srv/mp2_logs/` with a name of either `introducer.log` or `member.log` depending on which type of process was run. Logs contain membership updates, as well as info on connection establishment, messages sent, messages received, and other useful debugging info.  
</br>
# Instructions
- STEP 1: Run the introducer
  * ssh into the machine ```ssh <NETID>@fa22-cs425-ggXX.cs.illinois.edu``` 
  * Clone the project ```https://gitlab.engr.illinois.edu/sushmam3/mp2_cs425_membership_protocol.git```
  * Build the project ```mvn -DskipTests package``` (the tests pass, but it's faster to build without them)
  * cd to scripts folder and run the introducer.sh ```./introducer.sh <port-number>```

- STEP 2: Run the member
  * ssh into the machine ```ssh <NETID>@fa22-cs425-ggXX.cs.illinois.edu```
  * Clone the project ```https://gitlab.engr.illinois.edu/sushmam3/mp2_cs425_membership_protocol.git```
  * Build the project ```mvn -DskipTests package```
  * cd to scripts folder and run the member.sh ```./member.sh <port-number> <introducer-host-name> <introducer-port-number>```
  * On the command prompt, there are three options. 
    * ```join``` - join the network
    * ```leave``` - leave the network
    * ```list_mem``` - Display the membership list
    * ```list_self``` - Display self information
  












