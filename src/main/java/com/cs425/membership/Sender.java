package com.cs425.membership;

import com.cs425.membership.MembershipList.MemberList;
import com.cs425.membership.MembershipList.MemberListEntry;
import com.cs425.membership.Messages.Message;
import com.cs425.membership.Messages.MessageHandler;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Sender extends Thread {
    private boolean exit = false;
    private MemberList memberList;
    private AtomicBoolean ackReceived;
    private long pingTimeOut;
    private DatagramSocket socket;

    public Sender(DatagramSocket socket, MemberList memberList, AtomicBoolean ackReceived, long pingTimeOut){
        this.socket = socket;
        this.memberList = memberList;
        this.ackReceived = ackReceived;
        this.pingTimeOut = pingTimeOut;
    }

    public void end(){
        exit = true;
    }

    private void ping(MemberListEntry member) throws IOException {
        Message msg = MessageHandler.pingMessage(member.toString()).getMessage();
        UDPProcessing.sendPacket(socket, msg, member.getHostname(), member.getPort());
    }

    @Override
    public void run(){

        while(!exit){
            long startTime = System.currentTimeMillis();
            ackReceived.set(false);
            List<MemberListEntry> successors = memberList.getSuccessors();
            for (MemberListEntry member: successors) {
                try {
                    ping(member);
                    synchronized (ackReceived) {
                        ackReceived.wait(startTime + pingTimeOut - System.currentTimeMillis());
                    }
                    if (!ackReceived.get()){
                        memberList.removeEntry(member);
                        // TODO disseminate crash
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
