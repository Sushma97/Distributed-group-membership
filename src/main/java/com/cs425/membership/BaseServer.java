package com.cs425.membership;

import java.net.InetAddress;

/**
 * Starting point of the program
 */
public class BaseServer {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("Not enough arguments");
        }

        // Starting introducer only needs port number
        if (args.length == 1) {
            new Introducer(Integer.parseInt(args[0])).start();
        }

        // Starting member needs port number and introducer details
        else {
            int port = Integer.parseInt(args[0]);
            String introducerHost = args[1];
            int introducerPort = Integer.parseInt(args[2]);

            new Member(InetAddress.getLocalHost().getHostName(), port, introducerHost, introducerPort).start();
        }
    }
}
