package demo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Delay extends Thread {
    DatagramSocket sender;
    DatagramPacket pckt;

    public Delay(DatagramSocket sender, DatagramPacket pckt) {
        this.sender = sender;
        this.pckt = pckt;
    }

    public void run() {
        try {
            super.sleep(300);
            sender.send(pckt);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
