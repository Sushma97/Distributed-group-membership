package com.cs425.membership;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Introducer extends BaseServer{

    public Introducer(int port) throws UnknownHostException {
        super();
        this.self = new Member(InetAddress.getLocalHost().getHostName(), port, new Date(0));
    }

    @Override
    public void start() throws InterruptedException {
        Joiner newjoin = new Joiner(this.self.port);
        newjoin.setDaemon(true);
        newjoin.start();
        // TODO: process to run fault detection. either print membership list, leave or join.
        newjoin.setEnd();
        newjoin.join();
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

        public void setEnd(){
            end.set(true);
        }

        @Override
        public void run() {
            ServerSocket server;
            try {
                server = new ServerSocket(this.port);

                while (!end.get()){
                    Socket request = server.accept();
                    Scanner input = new Scanner(new InputStreamReader(request.getInputStream()));
                    input.useDelimiter("\n");
                    PrintWriter output = new PrintWriter(new OutputStreamWriter(request.getOutputStream()));
                    String memberId = input.next();
                    System.out.println("Member joining: " + memberId);
                    //Send out the membership list
                    for (String member: membershipList) {
                        output.println(member);
                    }
                    output.flush();
                    input.close();
                    membershipList.add(memberId);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


}
