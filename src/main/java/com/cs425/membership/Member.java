package com.cs425.membership;

import java.util.Date;

import com.cs425.membership.MembershipList.MemberList;
import com.cs425.membership.MembershipList.MemberListEntry;

public class Member {
    public String host;
    public int port;
    public Date timestamp;

    public MemberListEntry memberEntry;
    private MemberList memberList;

    public Member(String host, int port, Date timestamp) {
        assert(host != null);
        assert(timestamp != null);

        this.host = host;
        this.port = port;
        this.timestamp = timestamp;

        this.memberEntry = new MemberListEntry(host, port, timestamp);
        this.memberList = new MemberList(this.memberEntry);
    }
}
