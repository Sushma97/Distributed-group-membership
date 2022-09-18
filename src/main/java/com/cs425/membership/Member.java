package com.cs425.membership;

public class Member {
    public String host;
    public int port;
    public long timestamp;
    public String memberString;

    public Member(String host, int port, long timestamp) {
        this.host = host;
        this.port = port;
        this.timestamp = timestamp;
        memberString = this.host + "-" + this.port + "-" + this.timestamp;
    }
}
