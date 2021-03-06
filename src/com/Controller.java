package com;

import com.parsing.Parser;
import com.parsing.messages.Message;
import com.parsing.messages.payloads.PayloadFactory;
import com.parsing.messages.payloads.types.BlockPayload;
import com.parsing.messages.payloads.types.PayloadTypes;
import com.parsing.messages.MessagesTypes;

import java.io.IOException;
import java.util.*;
import java.util.List;

public class Controller implements IController {

    private BlockChain blockChain;
    public List<Transaction> receivedTransactions;
    private Block currentBlock;
    private int difficulty = 3;
    private int type = 1;
    private int blockThreshold = 2;
    private Set<String> coins;

    public Controller(){
        blockChain = new BlockChain(blockChain.getGenesisBlock());
        receivedTransactions = new ArrayList<>();
        coins = new HashSet<>();
    }

    @Override
    public boolean verifyTransaction(Transaction tx) {
        System.out.println("1======> "+ (getTransaction(tx.getTransactionID()) == null));
//        System.out.println("2======> " + tx.verify(this));
//        System.out.println("3======>" + checkDoubleSpendsWithBlocks(tx));
//        System.out.println("4======>" + checkDoubleSpendsWithReceivedTrans(tx));
        return (getTransaction(tx.getTransactionID()) == null) && tx.verify(this)
                && !checkDoubleSpendsWithBlocks(tx)
                && !checkDoubleSpendsWithReceivedTrans(tx);
    }

    @Override
    public void receiveBlock(Block b) {
        Block block = b;
        boolean verifyhash = block.verifyHash();
        boolean addblock = blockChain.addBlock(block);
//        System.out.println("verifyhash" + verifyhash);
//        System.out.println("addblock"+ addblock);
        if(!verifyhash && !addblock)
            return;
//        blockChain.addBlock(b.copy());
        System.out.println("# of Nodes: " + blockChain.getNumberOfNodes());
        System.out.println("Chain Depth: " + blockChain.depth());
//        System.out.println("here1---------------");
//        blockChain.printChain();
//        System.out.println("here2-----------------");
        block.printBlock();

    }

    @Override
    public void mineBlock() throws IOException {
        if (currentBlock == null){
            currentBlock = new Block(blockChain.getChainHead().block.getHash());
            currentBlock.setBlockThreshold(blockThreshold);
        }
        List<Transaction> pendingTransactions = new ArrayList<>();

        while (!receivedTransactions.isEmpty()) {
                pendingTransactions.add(receivedTransactions.remove(0));
        }

        currentBlock.setTransactions(List.copyOf(pendingTransactions));
        currentBlock.setMerkleTreeRoot(currentBlock.calculateMerkleTreeRoot());
        currentBlock.solveBlock(type, difficulty);
        currentBlock.setHash(currentBlock.calculateBlockHash());
        handleCoins();
        pendingTransactions.clear();
        broadcastBlock();
    }

    @Override
    public void getReceivedTransactions(Transaction transaction) {
        if((getTransaction(transaction.getTransactionID()) == null) && transaction.isInitialTransaction()){
            receivedTransactions.add(transaction);
        } else {
            if (verifyTransaction(transaction)){
                receivedTransactions.add(transaction);
            }
        }
    }

    @Override
    public void broadcastBlock() throws IOException {
        NodeSender nodeSender = new NodeSender();
        Message blockMessage = new Message();
        PayloadFactory payloadFactory = new PayloadFactory();
        BlockPayload blockPayload = (BlockPayload) payloadFactory.getPayload(PayloadTypes.BLOCK_PAYLOAD);

        blockPayload.setHash(currentBlock.getHash());
        blockPayload.setMerkleTreeRoot(currentBlock.getMerkleTreeRoot());
        blockPayload.setTimeStamp(currentBlock.getTimeStamp());
        blockPayload.setPrevBlockHash(currentBlock.getPrevBlockHash());
        blockPayload.setTransactions(currentBlock.getTransactions());
        blockPayload.setSpentcoins(currentBlock.getSpentcoins());
        blockPayload.setNonce(currentBlock.getNonce());
        blockMessage.setMessagePayload(blockPayload);

        blockMessage.setMessageType(MessagesTypes.BLOCK_MESSAGE.toString());
        Parser parser = new Parser();
        String message = parser.serializeMessage(blockMessage);
        nodeSender.send(message, IPsDTO.clientsIPs, IPsDTO.minersPorts);
        currentBlock = null;
    }

    private void handleCoins (){
        List<Transaction> transactions = currentBlock.getTransactions();
        for (Transaction transaction: transactions) {
            TransactionInput input = transaction.getInput();
            String coin = input.getPrevTX() + " " + input.getPrevOutputIndex();
            coins.add(coin);
        }
        currentBlock.setSpentcoins(coins);
    }

    private boolean checkDoubleSpendsWithBlocks (Transaction transaction){
        TransactionInput input = transaction.getInput();
        String coin = input.getPrevTX() + " " + input.getPrevOutputIndex();
        return blockChain.checkDoubleSpend(coin);
    }

    private boolean checkDoubleSpendsWithReceivedTrans (Transaction tx){
        String coin2 = tx.getInput().getPrevTX() + " " + tx.getInput().getPrevOutputIndex();
        for (Transaction transaction: receivedTransactions){
            String coin = transaction.getInput().getPrevTX() + " " + transaction.getInput().getPrevOutputIndex();
            if (coin2.equals(coin))
                return true;
        }
        return false;
    }

    public Transaction getTransaction(long txid) {
        Block block = null;
        BlockChain bc = blockChain.traverseChain(txid);
        if (bc != null) {
            block = bc.block;
            if (block != null)
                return block.getTransaction(txid + "");
        }
        return null;
        
    }

    public Transaction getTransactionInCurrentBlock (long txid){
        for (Transaction transaction:this.receivedTransactions){
            if (transaction.getTransactionID() == txid)
                return transaction;
        }
        return null;
    }
}
