package demo;

import java.io.Serializable;

public class ACK implements Serializable {
    private int sequenceNumber;

    // Construtor da classe ACK, que recebe seu valor. Inclui método para o retornar.
    public ACK(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getId() {
        return this.sequenceNumber;
    }
}
