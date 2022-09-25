package com.cs425.membership;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPProcessing {
    public static void sendPacket(DatagramSocket socket, Serializable object, String host, int port) throws IOException {
        socket.send(convertToPacket(object, InetAddress.getByName(host), port));
    }

    public static Object receivePacket(DatagramSocket socket) throws IOException, ClassNotFoundException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        return convertFromPacket(packet);
    }

    private static DatagramPacket convertToPacket(Serializable object, InetAddress host, int port) throws IOException {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);

        objectOutput.writeObject(object);
        objectOutput.flush();
        byte[] bytes = byteOutput.toByteArray();

        byteOutput.close();
        objectOutput.close();

        return new DatagramPacket(bytes, bytes.length, host, port);
    }

    private static Object convertFromPacket(DatagramPacket packet) throws ClassNotFoundException, IOException {
        ByteArrayInputStream byteInput = new ByteArrayInputStream(packet.getData());
        ObjectInputStream objectInput = new ObjectInputStream(byteInput);

        Object obj = objectInput.readObject();

        byteInput.close();
        objectInput.close();

        return obj;
    }

}
