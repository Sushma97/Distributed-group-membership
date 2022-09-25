package com.cs425.membership;

import com.cs425.membership.MembershipList.MemberList;
import com.cs425.membership.MembershipList.MemberListEntry;
import com.cs425.membership.Messages.TCPMessage;
import com.cs425.membership.Messages.TCPMessage.MessageType;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public Member(String host, int port, String introducerHost, int introducerPort) {
        assert(host != null);
        assert(timestamp != null);

        this.host = host;
        this.port = port;

        this.introducerHost = introducerHost;
        this.introducerPort = introducerPort;

        joined = new AtomicBoolean();
        end = new AtomicBoolean();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void start() throws ClassNotFoundException, InterruptedException {
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

        end.set(false);

        // Initialize incarnation identity
        this.timestamp = new Date();
        this.selfEntry = new MemberListEntry(host, port, timestamp);

        // Get member already in group
        MemberListEntry groupProcess = getGroupProcess();

        if (groupProcess != null) {
            // Get member list from group member and add self
            memberList = requestMemberList(groupProcess);
            memberList.addNewOwner(selfEntry);
        } else {
            // This is the first member of the group
            System.out.println("First member of group");
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

        // Communicate join
        disseminateMessage(new TCPMessage(MessageType.Join, selfEntry));
        // TODO log own join time

        // Start main protocol
        mainProtocolThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Member.this.mainProtocol();
            }
        });
        // TODO uncomment
        mainProtocolThread.setDaemon(true);
        mainProtocolThread.start();

        joined.set(true);
    }

    private MemberListEntry getGroupProcess() throws UnknownHostException, IOException, ClassNotFoundException {
        System.out.println("Connecting to introducer at " + introducerHost + ":" + introducerPort);
        Socket introducer = new Socket(introducerHost, introducerPort);
        System.out.println("Connected to " + introducer.toString());
        ObjectOutputStream output = new ObjectOutputStream(introducer.getOutputStream());
        ObjectInputStream input = new ObjectInputStream(introducer.getInputStream());
        System.out.println("IO streams created");

        // Send self entry to introducer
        output.writeObject(selfEntry);
        output.flush();
        System.out.println("Wrote self entry");

        // receive running process
        MemberListEntry runningProcess = (MemberListEntry) input.readObject();
        System.out.println("Received group process");

        // Close resources
        input.close();
        output.close();
        introducer.close();

        System.out.println("Connection to introducer closed");

        return runningProcess;
    }

    private MemberList requestMemberList(MemberListEntry groupProcess) throws UnknownHostException, IOException, ClassNotFoundException {
        Socket client = new Socket(groupProcess.getHostname(), groupProcess.getPort());
        ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());
        ObjectInputStream input = new ObjectInputStream(client.getInputStream());


        // Request membership list
        output.writeObject(new TCPMessage(MessageType.MemberListRequest, selfEntry));
        output.flush();
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

        // Disseminate leave if necessary
        if (sendMessage) {
            disseminateMessage(new TCPMessage(MessageType.Leave, selfEntry));
        }

        // Close resources
        end.set(true);
        // TODO make sure we can actually end this thread
        mainProtocolThread.join();
        // TODO make sure we can actually end this thread

//        UDPListenerThread.join();
        server.close();
        TCPListenerThread.join();

        // TODO log own leave time

        memberList = null;
        selfEntry = null;

        joined.set(false);
    }

    // Uses fire-and-forget paradigm
    public void disseminateMessage(TCPMessage message) {
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
                System.out.println("TCP server socket closed.");
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
            TCPMessage message = (TCPMessage) input.readObject();

            // perform appropriate action
            switch(message.getMessageType()) {
                case Join:
                    synchronized (memberList) {
                        if (memberList.addEntry(message.getSubjectEntry())) {
                            // TODO log join
                        }
                    }
                    break;
                case Leave:
                    synchronized (memberList) {
                        if (memberList.removeEntry(message.getSubjectEntry())) {
                            // TODO log leave
                        }
                    }
                    break;
                case Crash:
                    if (selfEntry.equals(message.getSubjectEntry())) {
                        // False crash of this node detected
                        System.out.println("\nFalse positive crash of this node detected. Stopping execution.\n");

                        // Leave group silently
                        leaveGroup(false);

                        // Command prompt
                        System.out.print("MemberProcess$ ");
                        break;
                    }
                    synchronized (memberList) {
                        if (memberList.removeEntry(message.getSubjectEntry())) {
                            // TODO log crash
                        }
                    }
                    break;
                case MemberListRequest:
                    synchronized (memberList) {
                        output.writeObject(memberList);
                        output.flush();
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
            TCPMessage message = new TCPMessage(TCPMessage.MessageType.Ping, sender);
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
                    ping(member, selfEntry);
                    ackSignal.wait(timeOut);
                }

                // Handle ack timeout
                if (!ackSignal.get()) {
                    // Disseminate message first in case of false positive
                    disseminateMessage(new TCPMessage(TCPMessage.MessageType.Crash, member));

                    // Then remove entry
                    synchronized (memberList) {
                        if (memberList.removeEntry(member)) {
                            // TODO log crash
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
