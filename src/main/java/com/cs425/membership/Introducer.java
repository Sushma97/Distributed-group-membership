package com.cs425.membership;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.cs425.membership.MembershipList.MemberListEntry;
import com.cs425.membership.Messages.TCPMessage;
import com.cs425.membership.Messages.TCPMessage.MessageType;

public class Introducer {

    Queue<MemberListEntry> recentJoins;
    int port;

    public Introducer(int port) throws UnknownHostException {
        this.port = port;
        recentJoins = new LinkedList<MemberListEntry>();
    }

    public void start() throws InterruptedException {
        Joiner newjoin = new Joiner(this.port);
        newjoin.start();
        // TODO: process to run fault detection. either print membership list, leave or join.
        // newjoin.setEnd();
        // newjoin.join();
    }

    private class Joiner extends Thread {
        private int port;
        private AtomicBoolean end;
        public Joiner(int port) {
            this.port = port;
            end = new AtomicBoolean(
                    false
            );
        }

        @Override
        public void run() {
            ServerSocket server;
            try {
                server = new ServerSocket(this.port);

                while (!end.get()){
                    System.out.println("Waiting for request at " + server.toString());
                    Socket request = server.accept();
                    System.out.println("Connection established on port " + request.getLocalPort() + " with " + request.toString());
                    ObjectOutputStream output = new ObjectOutputStream(request.getOutputStream());
                    ObjectInputStream input = new ObjectInputStream(request.getInputStream());
                    System.out.println("IO Streams created");

                    MemberListEntry newEntry;
                    try {
                        newEntry = (MemberListEntry) input.readObject();
                        System.out.println("Member joining: " + newEntry.toString());
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        continue;
                    }

                    // Connect to non-faulty process
                    MemberListEntry groupMember = recentJoins.peek();
                    while (groupMember != null) {
                        try {
                            Socket tryConnection = new Socket(groupMember.getHostname(), groupMember.getPort());
                            ObjectOutputStream tryConnectionOutput = new ObjectOutputStream(tryConnection.getOutputStream());

                            tryConnectionOutput.writeObject(new TCPMessage(MessageType.IntroducerCheckAlive, null));

                            tryConnectionOutput.close();
                            tryConnection.close();
                            break;
                        } catch (Exception e) {
                            // remove faulty/left process and choose next one
                            recentJoins.poll();
                            groupMember = recentJoins.peek();
                        }
                    }

                    output.writeObject(groupMember);
                    output.flush();
                    output.close();
                    input.close();
                    request.close();

                    recentJoins.add(newEntry);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


}
