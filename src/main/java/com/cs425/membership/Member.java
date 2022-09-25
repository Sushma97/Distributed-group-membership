package com.cs425.membership;

import com.cs425.membership.MembershipList.MemberList;
import com.cs425.membership.MembershipList.MemberListEntry;
import com.cs425.membership.Messages.Message;
import com.cs425.membership.Messages.Message.MessageType;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class Member {

    // Member & introducer info
    private String host;
    private int port;
    private Date timestamp;
    private String introducerHost;
    private int introducerPort;

    // Protocol settings
    private final long PROTOCOL_TIME = 1500;
    private final int NUM_MONITORS = 1;

    // Sockets
    private ServerSocket server;
    private DatagramSocket socket;

    // Membership list and owner entry
    private volatile MemberList memberList;
    public MemberListEntry selfEntry;

    // Threading resources
    private Thread mainProtocolThread;
    private Thread TCPListenerThread;

    private AtomicBoolean joined;
    private AtomicBoolean end;

    // Logger
    public static Logger logger = Logger.getLogger("MemberLogger");

    // Constructor and beginning of functionality
    public Member(String host, int port, String introducerHost, int introducerPort) throws SecurityException, IOException {
        assert(host != null);
        assert(timestamp != null);

        this.host = host;
        this.port = port;

        this.introducerHost = introducerHost;
        this.introducerPort = introducerPort;

        joined = new AtomicBoolean();
        end = new AtomicBoolean();

        Handler fh = new FileHandler("/srv/mp2_logs/member.log");
        logger.addHandler(fh);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void start() throws ClassNotFoundException, InterruptedException {
        logger.info("Member process started");
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try {
                System.out.print("MemberProcess$ ");
                String command = stdin.readLine();
                System.out.println();

                switch (command) {
                    case "join":
                        joinGroup();
                        break;
                
                    case "leave":
                        leaveGroup(true);
                        break;
                
                    case "list_mem":
                        if (joined.get()) {
                            System.out.println(memberList);
                        } else {
                            System.out.println("Not joined");
                        }
                        break;
                
                    case "list_self":
                        if (joined.get()) {
                            System.out.println(selfEntry);
                        } else {
                            System.out.println("Not joined");
                        }
                        break;
                
                    default:
                    System.out.println("Unrecognized command, type 'join', 'leave', 'list_mem', or 'list_self'");
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println();
        }
    }

    private void joinGroup() throws IOException, ClassNotFoundException {
        // Do nothing if already joined
        if(joined.get()) {
            return;
        }

        logger.info("Member process received join command");

        end.set(false);

        // Initialize incarnation identity
        this.timestamp = new Date();
        this.selfEntry = new MemberListEntry(host, port, timestamp);

        logger.info("New entry created: " + selfEntry);

        // Get member already in group
        MemberListEntry groupProcess = getGroupProcess();

        if (groupProcess != null) {
            // Get member list from group member and add self
            memberList = requestMemberList(groupProcess);
            memberList.addNewOwner(selfEntry);
            logger.info("Retrieved membership list");
        } else {
            // This is the first member of the group
            logger.info("First member of group");
            memberList = new MemberList(selfEntry);
        }

        // Start sockets/listeners
        server = new ServerSocket(port);
        TCPListenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Member.this.TCPListener();
            }
        });
        TCPListenerThread.start();
        logger.info("TCP Server started");

        // Communicate join
        disseminateMessage(new Message(MessageType.Join, selfEntry));
        logger.info("Process joined");

        // Start main protocol
        mainProtocolThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Member.this.mainProtocol();
            }
        });
        mainProtocolThread.setDaemon(true);
        mainProtocolThread.start();
        logger.info("Main protocol started");

        joined.set(true);
    }

    private MemberListEntry getGroupProcess() throws UnknownHostException, IOException, ClassNotFoundException {
        Socket introducer = new Socket(introducerHost, introducerPort);
        logger.info("Connected to " + introducer.toString());
        ObjectOutputStream output = new ObjectOutputStream(introducer.getOutputStream());
        ObjectInputStream input = new ObjectInputStream(introducer.getInputStream());

        // Send self entry to introducer
        output.writeObject(selfEntry);
        output.flush();

        logger.info("JOIN: Sent " + ObjectSize.sizeInBytes(selfEntry) + " bytes over TCP to introducer");

        // receive running process
        MemberListEntry runningProcess = (MemberListEntry) input.readObject();

        // Close resources
        input.close();
        output.close();
        introducer.close();

        logger.info("Connection to introducer closed");

        return runningProcess;
    }

    private MemberList requestMemberList(MemberListEntry groupProcess) throws UnknownHostException, IOException, ClassNotFoundException {
        Socket client = new Socket(groupProcess.getHostname(), groupProcess.getPort());
        ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());
        ObjectInputStream input = new ObjectInputStream(client.getInputStream());


        // Request membership list
        Message message = new Message(MessageType.MemberListRequest, selfEntry);

        output.writeObject(message);
        output.flush();
        logger.info("JOIN: Sent " + ObjectSize.sizeInBytes(message) + " bytes over TCP for membership list request");

        MemberList retrievedList = (MemberList) input.readObject();

        // Close resources
        input.close();
        output.close();
        client.close();

        return retrievedList;
    }

    private void leaveGroup(boolean sendMessage) throws IOException, InterruptedException {
        // Do nothing if not joined
        if (!joined.get()) {
            return;
        }

        logger.info("Leave command received");

        // Disseminate leave if necessary
        if (sendMessage) {
            disseminateMessage(new Message(MessageType.Leave, selfEntry));
            logger.info("Request to leave disseminated");
        }

        // Close resources
        end.set(true);

        mainProtocolThread.join();
        logger.info("Main Protocol stopped");

        server.close();
        TCPListenerThread.join();
        logger.info("TCP server closed");

        memberList = null;
        selfEntry = null;
        
        logger.info("Process left");
        joined.set(false);
    }

    // Uses fire-and-forget paradigm
    public void disseminateMessage(Message message) {
        synchronized (memberList) {
            for (MemberListEntry entry: memberList) {
                // Don't send a message to ourself
                if (entry.equals(selfEntry)) {
                    continue;
                }

                try {
                    // Open resources
                    Socket groupMember = new Socket(entry.getHostname(), entry.getPort());
                    ObjectOutputStream output = new ObjectOutputStream(groupMember.getOutputStream());
                    ObjectInputStream input = new ObjectInputStream(groupMember.getInputStream());

                    // Send message
                    output.writeObject(message);
                    output.flush();

                    switch (message.getMessageType()) {
                        case Join:
                            logger.info("JOIN: Sent " + ObjectSize.sizeInBytes(message) + " bytes over TCP for dissemination");
                            break;
                        case Leave:
                            logger.info("LEAVE: Sent " + ObjectSize.sizeInBytes(message) + " bytes over TCP for dissemination");
                            break;
                        case Crash:
                            logger.info("CRASH: Sent " + ObjectSize.sizeInBytes(message) + " bytes over TCP for dissemination");
                            break;
                        default:
                            assert(false);
                    }

                    // Close resources
                    input.close();
                    output.close();
                    groupMember.close();
                } catch (IOException e) {
                    continue;
                }
            }
        }
    }


    // Thread methods

    private void TCPListener() {
        while (!end.get()) {
            try {
                Socket client = server.accept();
                logger.info("TCP connection established from " + client.toString());

                // Process message in own thread to prevent race condition on membership list
                synchronized (TCPListenerThread) {
                    Thread processMessageThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Member.this.processTCPMessage(client);
                        }
                    });
                    processMessageThread.start();
                }
            } catch(SocketException e) {
                break;
            } catch (Exception e) {
                continue;
            }
        }
        
    }

    private void processTCPMessage(Socket client) {
        try {
            ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream input = new ObjectInputStream(client.getInputStream());

            // Recieve message
            Message message = (Message) input.readObject();

            // perform appropriate action
            switch(message.getMessageType()) {
                case Join:
                    logger.info("Received message for process joining group: " + message.getSubjectEntry());
                    synchronized (memberList) {
                        if (memberList.addEntry(message.getSubjectEntry())) {
                            logger.info("Process added to membership list: " + message.getSubjectEntry());
                        }
                    }
                    break;
                case Leave:
                    logger.info("Received message for process leaving group: " + message.getSubjectEntry());
                    synchronized (memberList) {
                        if (memberList.removeEntry(message.getSubjectEntry())) {
                            logger.info("Process removed from membership list: " + message.getSubjectEntry());
                        }
                    }
                    break;
                case Crash:
                    logger.warning("Message received for crashed process: " + message.getSubjectEntry());
                    if (selfEntry.equals(message.getSubjectEntry())) {
                        // False crash of this node detected
                        System.out.println("\nFalse positive crash of this node detected. Stopping execution.\n");
                        logger.warning("False positive crash of this node detected. Stopping execution.");

                        // Leave group silently
                        leaveGroup(false);

                        // Command prompt
                        System.out.print("MemberProcess$ ");
                        break;
                    }
                    synchronized (memberList) {
                        if (memberList.removeEntry(message.getSubjectEntry())) {
                            logger.info("Process removed from membership list: " + message.getSubjectEntry());
                        }
                    }
                    break;
                case MemberListRequest:
                    synchronized (memberList) {
                        output.writeObject(memberList);
                        output.flush();
                        logger.info("JOIN: Sent " + ObjectSize.sizeInBytes(message) + " bytes over TCP containing membership list");
                    }
                    break;
                // Do nothing
                case IntroducerCheckAlive:
                default:
                    break;
            }

            // Close resources
            input.close();
            output.close();
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // For receiving UDP messages and responding




    // For sending pings and checking ack

    private void mainProtocol() {
        try {
            socket = new DatagramSocket(selfEntry.getPort());

            List<AtomicBoolean> ackSignals = new ArrayList<>(NUM_MONITORS);
            for (int i = 0; i < NUM_MONITORS; i++) {
                ackSignals.add(new AtomicBoolean());
            }


            Receiver receiver = new Receiver(socket, selfEntry, end, ackSignals);
            receiver.setDaemon(true);
            receiver.start();
            logger.info("UDP Socket opened");
            while(!end.get()) {
                List<MemberListEntry> successors;
                synchronized (memberList) {
                    successors = memberList.getSuccessors(NUM_MONITORS);
                    receiver.updateAckers(successors);

                    for (int i = 0; i < successors.size(); i++) {
                        new SenderProcess(ackSignals.get(i), successors.get(i), 500).start();
                    }
                }
                sleepThread();
            }
            socket.close();
            logger.info("UDP Socket closed");
            receiver.join();
        }catch (SocketException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void sleepThread() {
        try {
            Thread.sleep(PROTOCOL_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class SenderProcess extends Thread {
        public MemberListEntry member;
        private AtomicBoolean ackSignal;
        private long timeOut;

        private void ping(MemberListEntry member, MemberListEntry sender) throws IOException {
            Message message = new Message(Message.MessageType.Ping, sender);
            UDPProcessing.sendPacket(socket, message, member.getHostname(), member.getPort());
        }

        public SenderProcess(AtomicBoolean ackSignal, MemberListEntry member, long timeOut)  {
            this.member = member;
            this.ackSignal = ackSignal;
            this.timeOut = timeOut;
        }

        @Override
        public void run() {

            try {
                // Ping successor
                synchronized (ackSignal) {
                    ackSignal.set(false);
                    logger.info("Pinging " + member);
                    ping(member, selfEntry);
                    ackSignal.wait(timeOut);
                }

                // Handle ACK timeout
                if (!ackSignal.get()) {
                    logger.warning("ACK not received from " + member);
                    logger.warning("Process failure detected detected: " + member);
                    // Disseminate message first in case of false positive
                    disseminateMessage(new Message(Message.MessageType.Crash, member));

                    // Then remove entry
                    synchronized (memberList) {
                        if (memberList.removeEntry(member)) {
                            logger.info("Process removed from membership list: " + member);
                        }
                    }
                } else {
                    logger.info("ACK received from " + member);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
