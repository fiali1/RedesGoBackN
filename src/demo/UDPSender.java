package demo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class UDPSender {
    public static void sendDatagrams(DatagramSocket sender, Converter c, Packet[] pcktArray, InetAddress ip, int port, int opr) {
        // Definição das variáveis auxiliares
        int length = pcktArray.length;
        int windowSize = 3;
        int base = 0;
        int nextSeqNumber = 0;

        // Mantem o Sender em execução enquanto o processo não é finalizado
        while(true) {
            try {
                /*
                * Envio dos pacotes restritos à janela atual com início em base e fim em nextSequenceNumber - 1.
                * Verificação extra para checar se todos foram enviados, com nextSeqNumber sendo menor que length.
                 */
                while (nextSeqNumber - base < windowSize && nextSeqNumber < length) {
                    // Conversão de um Packet individual em bytes e confecção de um DatagramPacket.
                    byte[] pcktBytes = c.convertToBytes(pcktArray[nextSeqNumber]);
                    DatagramPacket dp =
                            new DatagramPacket(pcktBytes, pcktBytes.length, ip, port);

                    /*
                     * Envio normal: Quando opr é 0, os segmentos são enviados em sequência, um a um.
                     * Envio com perda de pacote: Quando opr é 1, o segmento 3 é "perdido" na primeira tentativa.
                     * Envio com atraso: Quando opr é 2, o segmento 3 é eviado pela classe Delay, simulando seu "atraso" com uma thread.
                     * Envio com duplicatas: Quando opr é 3, o segmento 1 é enviado duas vezes.
                     */
                    if (opr == 1 && nextSeqNumber == 3) {
                        System.out.println("[Mensagem " + nextSeqNumber + " sera perdida no envio ao destinatario]");
                        System.out.println();
                    } else if (opr == 2 && nextSeqNumber == 3) {
                        System.out.println("[Mensagem " + nextSeqNumber + " vai se atrasar no envio ao destinatario]");
                        System.out.println();

                        Delay d = new Delay(sender, dp);
                        d.start();
                    } else if (opr == 3 && nextSeqNumber == 1) {
                        System.out.println("[Mensagem " + nextSeqNumber + " vai ser duplicada no envio ao destinatario]");
                        System.out.println();
                        sender.send(dp);
                        sender.send(dp);

                        System.out.println("Mensagem enviada ao destinatario!");
                        System.out.println("Conteudo: '" + new String(pcktArray[nextSeqNumber].getMessage()) + "'");
                        System.out.println("Numero de sequencia: " + pcktArray[nextSeqNumber].getSequenceNumber());
                        System.out.println();
                    }
                    else {
                        sender.send(dp);
                    }

                    // Exibição das informações no console para todos os pacotes enviados, exceto o último (indicador de finalização).
                    if (pcktArray[nextSeqNumber].getSequenceNumber() != -1) {
                        System.out.println("Mensagem enviada ao destinatario!");
                        System.out.println("Conteudo: '" + new String(pcktArray[nextSeqNumber].getMessage()) + "'");
                        System.out.println("Numero de sequencia: " + pcktArray[nextSeqNumber].getSequenceNumber());
                        System.out.println();
                    }
                    nextSeqNumber++;
                }

                // Definição de um array de bytes para preparar o recebimento e armazenamento o datagrama contendo ACK do destinatário.
                byte[] ackBytes = new byte[1024];
                DatagramPacket ackPckt = new DatagramPacket(ackBytes, ackBytes.length);

                try {
                    // Define o timeout para o recebimento da confirmação do grupo de Packets enviados como 3s.
                    sender.setSoTimeout(3000);

                    // Espera o recebimento da confirmação e a converte de volta a um objeto ACK para então extrair seu valor.
                    sender.receive(ackPckt);
                    ACK ack = (ACK) c.convertBackToObject(ackPckt.getData());
                    int ackValue = ack.getId();

                    /*
                     * Atualiza o valor base com o maior valor entre o atual e o recebido.
                     * Reflete o comportamento cumulativo do GBN no destinatário.
                     */
                    if (ackValue > base)
                        base = ackValue;

                    // Um ACK com valor -1 indica que todos os pacotes foram enviados e recebidos, indicando o fim de execução ao sender.
                    if (ackValue == -1) {
                        System.out.println("Envio finalizado!");
                        break;
                    }

                    // Informa o recebimento de ACK e o seu valor
                    System.out.println("ACK" + ackValue + " recebido!");
                    System.out.println();

                } catch (SocketTimeoutException ste) {
                    /*
                    * Informa a ocorrência de timeout e janela do pacote perdido.
                    * Reenvia todos os pacotes da janela, iniciada em base e finalizada em base + windowSize.
                    * Esse processo ocorre da mesma maneira que o original
                    * (a não ser nos casos de envio com perda e atraso, uma vez que agora se garante seu recebimento).
                     */
                    System.out.println("{ TIMEOUT [" + base + ", " + (base + windowSize - 1) + "]  }");
                    for (int i = base; i < base + windowSize; i++) {
                        byte[] pcktBytes = new byte[1024];
                        pcktBytes = c.convertToBytes(pcktArray[i]);

                        DatagramPacket dp =
                                new DatagramPacket(pcktBytes, pcktBytes.length, ip, port);

                        sender.send(dp);
                        System.out.println("Mensagem " + i + " reenviada ao destinatario");
                    }
                    System.out.println();
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static void normalDelivery(DatagramSocket sender, Converter c, InetAddress ip, int port) {
        System.out.println("Envio normal ------------------------------");
        String mensagem1 = "O envio direto nao envolve perdas ou atraso de pacote e, por consequencia, nao ha timeout.";
        mensagem1 += " Isso pode ser observado na sequencia direta de mensagens enviadas e recebidas nos consoles.";

        System.out.println("Mensagem completa a ser enviada: " + mensagem1);
        System.out.println("(Sera dividida em palavras individuais)");
        System.out.println();

        Packet[] pcktArray = c.createPacketArray(mensagem1);

        // Opr definido como 0 para indicar o envio sem problemas.
        sendDatagrams(sender, c, pcktArray, ip, port, 0);
    }

    public static void lostDelivery(DatagramSocket sender, Converter c, InetAddress ip, int port) {
        System.out.println("Envio com perda ------------------------------");
        String[] msgArray = {"Esta mensagem", "tera o", "seu segmento", "3 PERDIDO.", "Acompanhe o", "console para", "checar o tratamento."};
        String mc = "";
        Packet[] pcktArray = new Packet[msgArray.length + 1];
        int i = 0;

        /*
         * Criação dos pacotes individuais e sua adição ao array.
         * Definição da mensagem completa para melhor visualização no console.
         */
        while (i < msgArray.length) {
            if (i != msgArray.length - 1)
                mc += msgArray[i] + " ";
            else
                mc += msgArray[i];

            Packet p = new Packet(msgArray[i].getBytes(), i);
            pcktArray[i] = p;
            i++;
        }
        // Pacote indicador de finalização.
        pcktArray[i] = new Packet("".getBytes(), -1);

        System.out.println("Mensagem completa: " + mc);
        System.out.println("(Sera dividida em conjuntos de palavras)");
        System.out.println();

        // Opr definido como 1 para indicar o envio com perda.
        sendDatagrams(sender, c, pcktArray, ip, port, 1);
    }

    public static void delayedDelivery(DatagramSocket sender, Converter c, InetAddress ip, int port) {
        System.out.println("Envio com perda ------------------------------");
        String[] msgArray = {"Um segmento", "desta mensagem", "ira sofrer", "um ATRASO.", "Acompanhe o", "console para", "checar o tratamento."};
        String mc = "";
        Packet[] pcktArray = new Packet[msgArray.length + 1];
        int i = 0;

        /*
         * Criação dos pacotes individuais e sua adição ao array.
         * Definição da mensagem completa para melhor visualização no console.
         */
        while (i < msgArray.length) {
            if (i != msgArray.length - 1)
                mc += msgArray[i] + " ";
            else
                mc += msgArray[i];

            Packet p = new Packet(msgArray[i].getBytes(), i);
            pcktArray[i] = p;
            i++;
        }
        // Pacote indicador de finalização.
        pcktArray[i] = new Packet("".getBytes(), -1);

        System.out.println("Mensagem completa: " + mc);
        System.out.println("(Sera dividida em conjuntos de palavras)");
        System.out.println();

        // Opr definido como 1 para indicar o envio com perda.
        sendDatagrams(sender, c, pcktArray, ip, port, 2);
    }

    public static void outOfOrderDelivery(DatagramSocket sender, Converter c, InetAddress ip, int port) {
        System.out.println("Envio fora de ordem ------------------------------");
        String[] msgArray = {"Esta mensagem", "sera enviada", "fora de ordem."};
        String mc = "";
        Packet[] pcktArray = new Packet[msgArray.length + 1];

        //Definição da mensagem completa para melhor visualização no console.
        for (int i = 0; i < msgArray.length; i++) {
            if (i != msgArray.length - 1) {
                mc += msgArray[i] + " ";
            }
            else {
                mc += msgArray[i];
            }
        }

        // Criação manual dos pacotes e inserção fora de ordem no array (0 e 1 invertidos).
        Packet p0 = new Packet(msgArray[1].getBytes(), 1);
        Packet p1 = new Packet(msgArray[0].getBytes(), 0);
        Packet p2 = new Packet(msgArray[2].getBytes(), 2);
        Packet p3 = new Packet("".getBytes(), -1);

        pcktArray[0] = p0;
        pcktArray[1] = p1;
        pcktArray[2] = p2;
        pcktArray[3] = p3;

        System.out.println("Mensagem completa: " + mc);
        System.out.println("(Sera dividida em conjuntos de palavras)");
        System.out.println();

        /*
         * Opr 0 para indicar o envio normal
         * (erro virá da ordem dos pacotes e seus números de sequência dentro do array).
         */
        sendDatagrams(sender, c, pcktArray, ip, port, 0);
    }

    public static void duplicateDelivery(DatagramSocket sender, Converter c, InetAddress ip, int port) {
        System.out.println("Envio com duplicatas ------------------------------");
        String[] msgArray = {"Esta mensagem", "sera REPETIDA.", "Cheque o console."};
        String mc = "";
        Packet[] pcktArray = new Packet[msgArray.length + 1];

        //Definição da mensagem completa para melhor visualização no console.
        for (int i = 0; i < msgArray.length; i++) {
            if (i != msgArray.length - 1) {
                mc += msgArray[i] + " ";
            }
            else {
                mc += msgArray[i];
            }
        }

        // Criação manual dos pacotes e inserção fora de ordem no array (0 e 1 invertidos).
        Packet p0 = new Packet(msgArray[0].getBytes(), 0);
        Packet p1 = new Packet(msgArray[1].getBytes(), 1);
        Packet p2 = new Packet(msgArray[2].getBytes(), 2);
        Packet p3 = new Packet("".getBytes(), -1);

        pcktArray[0] = p0;
        pcktArray[1] = p1;
        pcktArray[2] = p2;
        pcktArray[3] = p3;

        System.out.println("Mensagem completa: " + mc);
        System.out.println("(Sera dividida em conjuntos de palavras)");
        System.out.println();

        // Opr 3 para indicar o envio com duplicata.
        sendDatagrams(sender, c, pcktArray, ip, port, 3);
    }

    public static void main(String[] args) throws IOException {
        System.out.println("SENDER ------------------------------");

        /*
         * Definicao do DatagramSocket e do conversor,
         * para lidar com o envio e conversão dos dados, respectivamente.
         * Scanner para receber input do usuário.
         */
        DatagramSocket sender = new DatagramSocket();
        Converter c = new Converter();
        Scanner sc = new Scanner(System.in);

        // Dados para envio do DatagramPacket ao destinatario
        InetAddress ip = InetAddress.getByName("127.0.0.1");
        int port = 9000;

        // Menu para o uso do programa
        int opr = -1;
        System.out.println("Selecione uma operacao");
        System.out.println("1: Envio normal");
        System.out.println("2: Envio com perda");
        System.out.println("3: Envio com atraso");
        System.out.println("4: Envio fora de ordem");
        System.out.println("5: Envio com duplicatas");
        System.out.println("6: Encerrar o programa");
        System.out.println();
        System.out.print("Sua operacao: ");

        opr = sc.nextInt();
        switch (opr) {
            case 1:
                normalDelivery(sender, c, ip, port);
                break;
            case 2:
                lostDelivery(sender, c, ip, port);
                break;
            case 3:
                delayedDelivery(sender, c, ip, port);
                break;
            case 4:
                outOfOrderDelivery(sender, c, ip, port);
                break;
            case 5:
                duplicateDelivery(sender, c, ip, port);
                break;
            case 6:
                break;
            default:
                System.out.println("Por favor, insira valores validos. Tente novamente.");
                break;
        }

        System.out.println("------------------------------ Programa encerrado ------------------------------");
        return;
    }
}