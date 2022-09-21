package com.cs425.membership.Messages;

import java.io.Serializable;

public class Message implements Serializable {
    public MessageType type;
    private String[] params;

    public Message (MessageType type, String [] params) {
        this.type=type;
        params=params;
    }
}
