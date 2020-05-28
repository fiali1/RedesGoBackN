package demo;

import java.io.Serializable;

public class Packet implements Serializable {
    private byte[] data;
    private int sequenceNumber;

    /*
     * Construtor da classe packet, que recebe uma String em bytes e o valor do número de sequência.
     * Inclui métodos para retornar estes valores.
     */
    public Packet(byte[] data, int sequenceNumber) {
        this.data = data;
        this.sequenceNumber = sequenceNumber;
    }

    public byte[] getMessage() {
        return this.data;
    }

    public int getSequenceNumber() {
        return this.sequenceNumber;
    }
}
