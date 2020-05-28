package demo;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPReceiver {
    public static void main(String[] args) {
        System.out.println("RECEIVER ------------------");
        // Instância de um conversor para lidar com os dados.
        Converter conv = new Converter();

        try {
            // Inicia o socket na porta 9000.
            DatagramSocket receiver = new DatagramSocket(9000);
            System.out.println("Esperando por mensagens...");

            /*
             * Valor esperado do próximo ACK a ser recebido
             * e variável para armazenar a mensagem completa para exibição ao final.
             */
            int nextACK = 0;
            String completeMessage = "";

            while(true) {
                /*
                 * Cria um array de bytes para armazenar os dados do datagrama
                 * e espera seu recebimento.
                 */
                byte[] recvBuffer = new byte[1024];
                DatagramPacket receivePckt = new DatagramPacket(recvBuffer, recvBuffer.length);
                receiver.receive(receivePckt);

                // Armazena endereço e portas do sender.
                InetAddress ip = receivePckt.getAddress();
                int port = receivePckt.getPort();

                // Converte o datagrama de volta ao objeto Packet.
                Packet pckt = (Packet) conv.convertBackToObject(receivePckt.getData());

                // Verifica se o valor do pacote corresponde ao esperado (comportamento cumulativo).
                if (pckt.getSequenceNumber() == nextACK) {
                    // Informa seus dados no console depois que os extrai do objeto.
                    String text = new String(pckt.getMessage());
                    System.out.println("Mensagem do remetente recebida! ");
                    System.out.println("Conteudo: '" + text + "'");
                    System.out.println("Numero de sequencia: " + pckt.getSequenceNumber());
                    System.out.println("[ Enviando ACK " + pckt.getSequenceNumber() + " ]");
                    System.out.println();

                    /*
                     * Cria um ACK como resposta, o converte em bytes
                     * e o envia de volta ao remetente através de um datagrama.
                     */
                    ACK response = new ACK(pckt.getSequenceNumber());
                    byte[] ack = conv.convertToBytes(response);
                    DatagramPacket sendPckt =
                            new DatagramPacket(ack, ack.length, ip, port);
                    receiver.send(sendPckt);

                    // Incrementa o valor do datagrama esperado e compõe a mensagem final para exibição.
                    nextACK++;
                    completeMessage += text + " ";

                } else {
                    // Tratamento dos pacotes com valor diferente do esperado.

                    // Valor igual a -1: Indica a finalização do processo após enviar um ACK -1 de volta.
                    if (pckt.getSequenceNumber() == -1) {
                        ACK response = new ACK(pckt.getSequenceNumber());

                        byte[] ack = conv.convertToBytes(response);
                        DatagramPacket sendPckt =
                                new DatagramPacket(ack, ack.length, ip, port);
                        receiver.send(sendPckt);

                        System.out.println("Finalizado!");
                        System.out.println("Mensagem completa: " + completeMessage);
                        break;

                    }
                    // Valor menor que o esperado: Indica o recebimento de uma duplicata e a descarta.
                    else if (pckt.getSequenceNumber() < nextACK) {
                        String text = new String(pckt.getMessage());
                        System.out.println("Mensagem do remetente recebida! ");
                        System.out.println("Erro: Duplicata");
                        System.out.println("Conteudo: '" + text + "'");
                        System.out.println("Numero de sequencia: " + pckt.getSequenceNumber());
                        System.out.println("[ Mensagem descartada ]");
                        System.out.println();
                    }
                    // Valor maior que o esperado: Indica o recebimento de um pacote fora de ordem e o descarta.
                    else if (pckt.getSequenceNumber() > nextACK) {
                        String text = new String(pckt.getMessage());
                        System.out.println("Mensagem do remetente recebida! ");
                        System.out.println("Erro: Fora de ordem");
                        System.out.println("Conteudo: '" + text + "'");
                        System.out.println("Numero de sequencia: " + pckt.getSequenceNumber());
                        System.out.println("[ Mensagem descartada ]");
                        System.out.println();
                    }
                }

            }


        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
