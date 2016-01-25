import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class ClientListenerThread implements Runnable {

    private MSocket mSocket  =  null;
    private Hashtable<String, Client> clientTable = null;
    private BlockingQueue<MPacket> clientQueue = null;
    private int globalSequenceNumber;

    public ClientListenerThread( MSocket mSocket,
                                Hashtable<String, Client> clientTable){
        this.mSocket = mSocket;
        this.clientTable = clientTable;
        if(Debug.debug) System.out.println("Instatiating ClientListenerThread");
        this.clientQueue = new PriorityBlockingQueue<MPacket>();
        this.globalSequenceNumber = 0;
    }

    public void run() {
    	System.out.println("Client Listener Thread started");
    	
        Thread enqueueThread = new Thread() { 
        	public void run() {
        		System.out.println("enqueue Thread started");
        		MPacket received = null;
                if(Debug.debug) System.out.println("Starting ClientListenerThread");
                while(true){
                    try{
                        received = (MPacket) mSocket.readObject();
                        System.out.println("Queueing Received " + received);
                        try {
                        	// Enqueue
                        	clientQueue.put(received);
                        } catch (InterruptedException e) {
                        	e.printStackTrace();
                        }
                    }catch(IOException e){
                        e.printStackTrace();
                    }catch(ClassNotFoundException e){
                        e.printStackTrace();
                    }            
                }
        	}
        };
        
        Thread dequeueThread = new Thread() { 
        	public void run() {
        		System.out.println("dequeue Thread started");
        		MPacket head = null;
            	Client client = null;
            	while(true){
            		try {
	            		head = clientQueue.peek();
	            		if (head != null && head.sequenceNumber == globalSequenceNumber) {
	            			// print sqno
	            			System.out.println("sqno client: " + globalSequenceNumber);
	            			// Remove head
	            			head = clientQueue.take();
	            			// Execute the action
	            			client = clientTable.get(head.name);
	            			executeAction(client, head);
	            			// Incr the sequence number
	            			globalSequenceNumber++;
	            		}
            		} catch (InterruptedException e) {
                    	e.printStackTrace();
                    }
            	}
        	}
        	
        	public void executeAction(Client client, MPacket received) {
                if(received.event == MPacket.UP){
                    client.forward();
                }else if(received.event == MPacket.DOWN){
                    client.backup();
                }else if(received.event == MPacket.LEFT){
                    client.turnLeft();
                }else if(received.event == MPacket.RIGHT){
                    client.turnRight();
                }else if(received.event == MPacket.FIRE){
                    client.fire();
                }else{
                    throw new UnsupportedOperationException();
                }
            }
        };
        
        enqueueThread.start();
        dequeueThread.start();
    }
    
    
}
