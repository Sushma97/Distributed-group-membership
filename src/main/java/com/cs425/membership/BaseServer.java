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

import com.sun.*;

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
            String introducerHost = args[2];
            int introducerPort = Integer.parseInt(args[2]);

            new Member(InetAddress.getLocalHost().getHostName(), port, introducerHost, introducerPort).start();
        }
    }
}
