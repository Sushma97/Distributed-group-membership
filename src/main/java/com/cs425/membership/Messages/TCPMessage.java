package com.cs425.membership.Messages;

import java.io.Serializable;

import com.cs425.membership.MembershipList.MemberListEntry;

public class TCPMessage implements Serializable {
    public static enum MessageType {
        Join,
        Leave,
        Crash,
        Ping,
        Ack,
        MemberListRequest,
        IntroducerCheckAlive
    }

    private MessageType messageType;
    private MemberListEntry subjectEntry;

    public TCPMessage(MessageType messageType, MemberListEntry subjectEntry) {
        this.messageType = messageType;
        this.subjectEntry = subjectEntry;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public MemberListEntry getSubjectEntry() {
        return subjectEntry;
    }
}
