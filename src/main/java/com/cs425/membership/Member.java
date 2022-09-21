package com.cs425.membership;

import java.util.Date;
import java.util.List;

import com.cs425.membership.MembershipList.MemberList;
import com.cs425.membership.MembershipList.MemberListEntry;
import com.cs425.membership.Messages.GossipMessage;

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

    public void processGossip(GossipMessage message) {
        // Skip if this message already infected this node
        if(this.hasGossipMessage(message))
            return;

        // Process message
        switch(message.getMessageType()) {
        case Join:
            // TODO log
            memberList.addEntry(message.getMessageTopic());
        case Leave:
            // TODO log
            memberList.removeEntry(message.getMessageTopic());
            break;
        case Crash:
            // TODO log
            memberList.removeEntry(message.getMessageTopic());
            break;
        }
    }

    // TODO we need to keep track of messages already recieved
    private boolean hasGossipMessage(GossipMessage message) {
        return false;
    }
}
