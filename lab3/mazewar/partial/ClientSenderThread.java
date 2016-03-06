import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;

public class ClientSenderThread implements Runnable {

    private MSocket mSocket = null;
    private BlockingQueue<MPacket> eventQueue = null;
    private Hashtable<String, Client> clientTable = null;
    
    public ClientSenderThread(MSocket mSocket,
                              BlockingQueue eventQueue,
                              Hashtable<String, Client> clientTable){
        this.mSocket = mSocket;
        this.eventQueue = eventQueue;
        this.clientTable = clientTable;
    }
    
    public void run() {
        MPacket toServer = null;
        if(Debug.debug) System.out.println("Starting ClientSenderThread");
        while(true){
            try{                
                //Take packet from queue
                toServer = (MPacket)eventQueue.take();
                if(Debug.debug) System.out.println("Sending " + toServer);
                mSocket.writeObject(toServer);    
            }catch(InterruptedException e){
                e.printStackTrace();
                Thread.currentThread().interrupt();    
            }
            
        }
    }
}
