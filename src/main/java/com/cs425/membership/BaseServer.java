package com.cs425.membership;

import java.net.InetAddress;

public class BaseServer {
    public Member self;
    public Introducer introducer;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new Exception("Not enough arguments");
        }

        if (args.length == 1) {
            new Introducer(Integer.parseInt(args[0])).start();
        }

        else {
            int port = Integer.parseInt(args[0]);
            String introducerHost = args[1];
            int introducerPort = Integer.parseInt(args[2]);

            new Member(InetAddress.getLocalHost().getHostName(), port, introducerHost, introducerPort).start();
        }
    }
}
