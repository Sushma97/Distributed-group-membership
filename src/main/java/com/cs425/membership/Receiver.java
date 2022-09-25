package com.cs425.membership;

import com.cs425.membership.MembershipList.MemberListEntry;
import com.cs425.membership.Messages.Message;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Receiver extends Thread {
    private DatagramSocket socket;
    public MemberListEntry selfEntry;
    private AtomicBoolean end;
    // private AtomicBoolean ackSignal;

    List<MemberListEntry> ackers;
    List<AtomicBoolean> ackSignals;

    public Receiver(DatagramSocket socket, MemberListEntry selfEntry, AtomicBoolean end, List<AtomicBoolean> ackSignals){
        this.socket = socket;
        this.selfEntry = selfEntry;
        this.end = end;
        this.ackSignals = ackSignals;

        this.ackers = new ArrayList<>();
    }

    @Override
    public void run() {
        while(!this.end.get()){
            try {
                Message packet = (Message) UDPProcessing.receivePacket(socket);
                processMsg(packet);
            } catch(SocketException e) {
                break;
            }
            catch (IOException | ClassNotFoundException e) {
                continue;
            }
        }
    }

    public void updateAckers(List<MemberListEntry> newAckers) {
        synchronized (ackers) {
            ackers = newAckers;
        }
    }

    public void processMsg(Message message) throws IOException {
        MemberListEntry subject = message.getSubjectEntry();
        switch(message.getMessageType()){
            case Ping:
                ack(subject, selfEntry);
                break;
            case Ack:
                synchronized (ackers) {
                    int boolIdx = ackers.indexOf(subject);
                    if (boolIdx == -1) {
                        break;
                    }

                    AtomicBoolean ackSignal = ackSignals.get(boolIdx);
                    synchronized (ackSignal) {
                        ackSignal.set(true);
                        ackSignal.notify();
                    }
                }
                break;
            default:
                break;
        }

    }

    private void ack(MemberListEntry member, MemberListEntry sender) throws IOException {
        Message message = new Message(Message.MessageType.Ack, sender);
        UDPProcessing.sendPacket(socket, message, member.getHostname(), member.getPort());
    }
}
