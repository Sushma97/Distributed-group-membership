package com.cs425.membership;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.cs425.membership.MembershipList.MemberList;
import com.cs425.membership.MembershipList.MemberListEntry;
import com.cs425.membership.Messages.Message;
import com.cs425.membership.Messages.MessageHandler;
import com.cs425.membership.Messages.TCPMessage;
import com.cs425.membership.Messages.TCPMessage.MessageType;

public class Member {

    // Member & introducer info
    private String host;
    private int port;
    private Date timestamp;
    
    private String introducerHost;
    private int introducerPort;

    // Sockets
    private ServerSocket server;
    private DatagramSocket socket;

    // Membership list and own entry
    private volatile MemberList memberList;
    public MemberListEntry selfEntry;

    // Threading resources
    private Thread mainProtocolThread;
    private Thread UDPListenerThread;
    private Thread TCPListenerThread;

    private AtomicBoolean joined;
    private AtomicBoolean end;
    private AtomicBoolean ackReceived;

    public Member(String host, int port, String introducerHost, int introducerPort) {
        assert(host != null);
        assert(timestamp != null);

        this.host = host;
        this.port = port;

        this.introducerHost = introducerHost;
        this.introducerPort = introducerPort;
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
                String command = stdin.readLine();

                switch (command) {
                    case "join":
                        joinGroup();
                        break;
                
                    case "leave":
                        leaveGroup();
                        break;
                
                    case "list_mem":
                        System.out.println(memberList);
                        break;
                
                    case "self_id":
                        System.out.println(selfEntry);
                        break;
                
                    default:
                    System.out.println("Unrecognized command, type 'join', 'leave', 'list_mem', or 'self_id'");
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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

        UDPListenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Member.this.UDPListener();
            }
        });
        UDPListenerThread.start();

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
        mainProtocolThread.start();

        joined.set(true);
    }

    private MemberListEntry getGroupProcess() throws UnknownHostException, IOException, ClassNotFoundException {
        Socket introducer = new Socket(introducerHost, introducerPort);
        ObjectInputStream input = new ObjectInputStream(introducer.getInputStream());
        ObjectOutputStream output = new ObjectOutputStream(introducer.getOutputStream());

        // Send self entry to introducer
        output.writeObject(selfEntry);

        // receive running process
        MemberListEntry runningProcess = (MemberListEntry) input.readObject();

        // Close resources
        output.close();
        input.close();
        introducer.close();

        return runningProcess;
    }

    private MemberList requestMemberList(MemberListEntry groupProcess) throws UnknownHostException, IOException, ClassNotFoundException {
        Socket client = new Socket(groupProcess.getHostname(), groupProcess.getPort());
        ObjectInputStream input = new ObjectInputStream(client.getInputStream());
        ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());

        // Request membership list
        output.writeObject(new TCPMessage(MessageType.MemberListRequest, selfEntry));
        MemberList retrievedList = (MemberList) input.readObject();

        // Close resources
        output.close();
        input.close();
        client.close();

        return retrievedList;
    }

    private void leaveGroup() throws IOException, InterruptedException {
        // Do nothing if not joined
        if (!joined.get()) {
            return;
        }

        // Disseminate leave
        disseminateMessage(new TCPMessage(MessageType.Leave, selfEntry));

        // Close resources
        end.set(true);
        mainProtocolThread.join();
        UDPListenerThread.join();
        server.close();
        TCPListenerThread.join();

        // TODO log own leave time

        joined.set(false);
    }

    // Uses fire-and-forget paradigm
    private void disseminateMessage(TCPMessage message) {
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

                    // Send message
                    output.writeObject(message);

                    // Close resources
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
                    synchronized (memberList) {
                        if (memberList.removeEntry(message.getSubjectEntry())) {
                            // TODO log crash
                        }
                    }
                    break;
                case MemberListRequest:
                    synchronized (memberList) {
                        output.writeObject(memberList);
                    }
                    break;
                default:
                    break;
            }

            // Close resources
            output.close();
            input.close();
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // For receiving UDP messages and responding
    private void UDPListener() {
        try{
            Object packet = null;
            while(!end.get()) {
                packet = UDPProcessing.receivePacket(socket);
                System.out.println("UDP Listener " + System.currentTimeMillis() + " waiting for next message");
            }
            processMsg(packet);
        }
        catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    public void processMsg(Object packet){

    }

    // For sending pings and checking ack
    private void mainProtocol() {
        while(!end.get()) {
            List<MemberListEntry> successors = memberList.getSuccessors();
            for (MemberListEntry member: successors) {
                long startTime = System.currentTimeMillis();
                ackReceived.set(false);

                try {
                    ping(member);
                    
                    ackReceived.wait(500);

                    // TODO we need to ensure the ack is from the successor we expect
                    // (in case an ACK is received after the timeout)
                    if (!ackReceived.get()) {
                        synchronized (memberList) {
                            if(memberList.removeEntry(member)) {
                                // TODO log crash
                            }
                        }
                        disseminateMessage(new TCPMessage(MessageType.Crash, selfEntry));
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void ping(MemberListEntry member) throws IOException {
        Message msg = MessageHandler.pingMessage(member.toString()).getMessage();
        UDPProcessing.sendPacket(socket, msg, member.getHostname(), member.getPort());
    }
}
