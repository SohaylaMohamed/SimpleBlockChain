package com;

import com.parsing.Parser;
import com.parsing.messages.IMessage;
import com.parsing.messages.payloads.types.BlockPayload;
import com.parsing.messages.MessagesTypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

// Network Server class
public class MinerReceiver {
    public static void main(String[] args) throws IOException {
        // server is listening this port
        ServerSocket ss = new ServerSocket(5050);

        // running infinite loop for getting
        // client request
        while (true) {

            Socket s = null;

            try {
                // socket object to receive incoming client requests
                s = ss.accept();

                System.out.println("A new client is connected : " + s);

                // obtaining input and out streams
                DataInputStream dis = new DataInputStream(s.getInputStream());
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());

                System.out.println("Assigning new thread for this client");

                // create a new thread object
                Thread t = new MinerReceiverHandler(s, dis, dos);

                // Invoking the start() method
                t.start();

            } catch (Exception e) {
                s.close();
                e.printStackTrace();
            }
        }
    }
}

// Network ClientHandler class
class MinerReceiverHandler extends Thread {
    DateFormat fordate = new SimpleDateFormat("yyyy/MM/dd");
    DateFormat fortime = new SimpleDateFormat("hh:mm:ss");
    final DataInputStream dis;
    final DataOutputStream dos;
    final Socket s;


    // Constructor
    public MinerReceiverHandler(Socket s, DataInputStream dis, DataOutputStream dos) {
        this.s = s;
        this.dis = dis;
        this.dos = dos;
    }

    @Override
    public void run() {
        String received;
        String toreturn;
        while (true) {
            try {
                Controller controller = new Controller();

                // Ask user what he wants
                dos.writeUTF("What do you want?[Date | Time]..\n" +
                        "Type Exit to terminate connection.");

                // receive the answer from client
                received = dis.readUTF();

                // creating Date object
                Date date = new Date();

                // write on output stream based on the
                // answer from the client
                IMessage receivedMsg = new Parser().deSerializeMessage(received);
                if (receivedMsg.getMessageType().equals(MessagesTypes.BLOCK_MESSAGE.toString())){
                    BlockPayload blockPayload = (BlockPayload) receivedMsg.getMessagePayload();
                    Block b = new Block(blockPayload.getPrevBlockHash());
                    b.setHash(blockPayload.getHash());
                    b.setMerkleTreeRoot(blockPayload.getMerkleTreeRoot());
                    b.setTimeStamp(blockPayload.getTimeStamp());
                    b.setPrevBlockHash(blockPayload.getPrevBlockHash());
                    b.setTransactions(blockPayload.getTransactions());
                    controller.receiveBlock(b);
                } else {
                    dos.writeUTF("Invalid Message Type!");
                }

                System.out.println("com.Client " + this.s + " sends exit...");
                System.out.println("Closing this connection.");
                this.s.close();
                System.out.println("Connection closed");
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            // closing resources
            this.dis.close();
            this.dos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
