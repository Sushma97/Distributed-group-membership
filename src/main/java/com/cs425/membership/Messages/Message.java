package com.cs425.membership.Messages;

import java.io.Serializable;

import com.cs425.membership.MembershipList.MemberListEntry;

/**
 * Communication message. Contains message type and the member details
 */
public class Message implements Serializable {
    public enum MessageType {
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

    public Message(MessageType messageType, MemberListEntry subjectEntry) {
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
