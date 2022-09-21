package com.cs425.membership.Messages;

import java.io.Serializable;

import com.cs425.membership.MembershipList.MemberListEntry;

public class  GossipMessage implements Serializable {

    private MessageType messageType;
    private MemberListEntry messageTopic;
    private int TTL;

    public GossipMessage(MessageType messageType, MemberListEntry messageTopic, int TTL) {
        assert(TTL > 0);

        this.messageType = messageType;
        this.messageTopic = messageTopic;
        this.TTL = TTL;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public MemberListEntry getMessageTopic() {
        return messageTopic;
    }

    /**
     * Decrements ttl
     */
    public void decrementTTL() {
        this.TTL--;
    }

    /**
     * @return true if this message is still alive
     */
    public boolean isAlive() {
        return this.TTL > 0;
    }

    
}
