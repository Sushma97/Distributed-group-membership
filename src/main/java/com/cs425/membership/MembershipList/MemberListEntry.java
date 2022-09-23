package com.cs425.membership.MembershipList;

import java.io.Serializable;
import java.util.Date;

public class MemberListEntry implements Serializable, Comparable<MemberListEntry> {
    private String hostname;
    private int port;
    private Date timestamp;

    public MemberListEntry(String hostname, int port, Date timestamp) {
        assert(hostname != null);
        assert(timestamp != null);

        this.hostname = hostname;
        this.port = port;
        this.timestamp = timestamp;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public boolean equals(MemberListEntry other) {
        return this.hostname.equals(other.hostname)
                    && this.port == other.port
                    && this.timestamp.equals(other.timestamp);
    }

    @Override
    public int compareTo(MemberListEntry other) {
        // Two entries should have the same timestamp iff they are the same incarnation
        assert(!other.timestamp.equals(this.timestamp) ^ this.equals(other));

        return this.timestamp.compareTo(other.timestamp);
    }

    @Override
    public String toString() {
        return this.hostname + "\t" + this.port + "\t" + this.timestamp.toString();
    }
}
