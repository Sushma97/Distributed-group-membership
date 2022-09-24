package com.cs425.membership;

import com.cs425.membership.MembershipList.MemberListEntry;
import com.cs425.membership.Messages.TCPMessage;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.concurrent.atomic.AtomicBoolean;

public class Receiver extends Thread {
    private boolean end=false;
    private DatagramSocket socket;
    public MemberListEntry selfEntry;
    private AtomicBoolean ackSignal;

    public Receiver(DatagramSocket socket, MemberListEntry selfEntry, AtomicBoolean ackSignal){
        this.socket = socket;
        this.selfEntry = selfEntry;
        this.ackSignal = ackSignal;
    }
    public void end(){
        this.end = true;
    }

    @Override
    public void run() {
        while(!this.end){
            try {
                TCPMessage packet = (TCPMessage) UDPProcessing.receivePacket(socket);
//                System.out.println("UDP Listener " + System.currentTimeMillis() + " waiting for next message");
                processMsg(packet);
            }
            catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        }



    public void processMsg(TCPMessage message) throws IOException {
        switch(message.getMessageType()){
            case Ping:
                ack(message.getSubjectEntry(), selfEntry);
                break;
            case Ack:
                ackSignal.set(true);
                synchronized (ackSignal) {
                    ackSignal.notify();
                }
                break;
            default:
                break;
        }

    }

    private void ack(MemberListEntry member, MemberListEntry sender) throws IOException {
        TCPMessage message = new TCPMessage(TCPMessage.MessageType.Ack, sender);
        UDPProcessing.sendPacket(socket, message, member.getHostname(), member.getPort());
    }
}
