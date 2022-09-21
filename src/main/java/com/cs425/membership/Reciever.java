package com.cs425.membership;

import java.io.IOException;
import java.net.DatagramSocket;

public class Reciever extends Thread{
    private boolean exit = false;
    private DatagramSocket socket;


    public void processMsg(Object packet){

    }


    public void end(){
        exit = true;
    }


    @Override
    public void run(){
        try{
            Object packet = null;
            while(!exit){
                packet = UDPProcessing.receivePacket(socket);
                System.out.println("INFO Receiver "+System.currentTimeMillis()+" waiting for next message");
            }
            processMsg(packet);
        }
        catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
    }
}
