package com.cs425.membership;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class BaseServer {
    public List<String> membershipList;
    private Member introducer;
    public Member self;

    public BaseServer() {
    }

    public BaseServer(int port, String introducerHost, int introducerPort) throws UnknownHostException {
        self = new Member(InetAddress.getLocalHost().getHostName(), port, introducerHost, introducerPort);
        System.out.println("New node : " + self.selfEntry.toString());
        // introducer = new Member(introducerHost, introducerPort, new Date(0));
    }

    public void start() throws IOException, InterruptedException {
        Socket client;
        while (true) {
            try {
                // connect to introducer
                client = new Socket(this.introducer.getHost(), this.introducer.getPort());
                Scanner input = new Scanner(new InputStreamReader((client.getInputStream())));
                input.useDelimiter(("\n"));
                PrintWriter output = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
                output.println(this.self.selfEntry.toString());
                output.flush();
                while (input.hasNext()){
                    String newJoin = input.next();
                    membershipList.add(newJoin);

                }
                input.close();
                //TODO : Fault detection process
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}
