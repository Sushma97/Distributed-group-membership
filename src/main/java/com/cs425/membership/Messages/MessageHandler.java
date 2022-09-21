package com.cs425.membership.Messages;

public class MessageHandler {
    private Message message;

    public MessageHandler(MessageType type, String [] args) {
        message =new Message(type, args);
    }

    public Message getMessage(){
        return message;
    }

    public static MessageHandler pingMessage(String messageVal){
        String[] args = new String[1];
        args[0] = messageVal;
        MessageHandler handler = new MessageHandler(MessageType.Ping, args);
        return handler;
    }
}
