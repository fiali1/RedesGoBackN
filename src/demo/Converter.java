package demo;

import java.io.*;

public class Converter {
    /*
     * Converte uma String única em um array de Strings
     * formado por suas palavras individuais, junto da pontuação.
     * Em seguida, cria um Packet utilizando cada palavra e seu índice
     * no array como dados na chamada do construtor.
     * Adiciona cada um num array de Packets e o retorna.
     */
    public static Packet[] createPacketArray(String text) {
        String[] split = text.split(" ");
        int length = split.length;

        Packet[] packetArray = new Packet[length + 1];

        int i = 0;
        while (i < split.length) {
            byte[] msgItem = split[i].getBytes();
            int seqNumber = i;

            packetArray[i] = new Packet(msgItem, seqNumber);
            i++;
        }

        packetArray[i] = new Packet("".getBytes(), -1);

        return packetArray;
    }

    /*
     * Converte o objeto Packet a ser enviado pelo sender/receiver
     * num array de bytes para ser atrelado ao DatagramPacket
     */
    public static byte[] convertToBytes(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);

        return baos.toByteArray();
    }

    /*
     * Converte o array de bytes recebido pelo sender/receiver no objeto Packet/ACK
     * para acessar suas informações, que incluem os dados e número de sequência/valor.
     */
    public static Object convertBackToObject(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);

        return ois.readObject();
    }
}
