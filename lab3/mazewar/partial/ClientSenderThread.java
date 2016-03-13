import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientSenderThread implements Runnable {
	private final String TAG = this.getClass().getSimpleName();

    private int myID;
    
    private VectorClock myClk;
    private BuffQueue bqs;
    
    private int uniqueIdentifier = 0;
    
    public ClientSenderThread(int myID,
                              VectorClock myClock,
                              BuffQueue buffers){
        this.myID = myID;
        this.myClk = myClock;
        this.bqs = buffers;
    }
    
    public void run() {
        MPacket toServer = null;
        Debug.log(TAG, "Starting ClientSenderThread");
        while(true){
            try{                
                //Take packet from queue
                toServer = (MPacket) bqs.eventQueue.take();
                Debug.log(TAG, "Sending " + toServer);
                
                // Incr vector clock before sending
                myClk.incr(toServer.senderId);
				VectorClock vClone = myClk.clone();
				
				toServer.clock = vClone;
				
				// set unique id for each packet
				if (toServer.originId == -1 || toServer.senderId == -1 || toServer.uniqId == -1) {
					toServer.originId = myID;
					toServer.senderId = myID;
					toServer.uniqId = uniqueIdentifier++;
				}
				
				if (toServer.recvId == -1) {
					// multicast
					multicast(toServer);
				}
				else {
					// unicast
					sequencePacket(toServer);
				}    
            }catch(InterruptedException e){
                e.printStackTrace();
                Thread.currentThread().interrupt();    
            }
            
        }
    }
    
    public void multicast(MPacket pkt) throws InterruptedException {
		// increment local process state value in local vector
		MPacket toSend;
		
		// Send to clients
		for (Integer cID : bqs.otherClients.keySet()) {
			// timestamp message with local vector
			toSend = new MPacket(pkt);
			toSend.recvId = cID;
			sequencePacket(toSend);
		}
	}
	
	public void sequencePacket(MPacket pkt) throws InterruptedException {
//		PriorityBlockingQueue<PacketWrapper> writeQ = bqs.socketMap.get(cID);
		
		// get the seqno
		AtomicInteger mySeqNo = bqs.sendSeqNo.get(pkt.recvId);
		
		pkt.sequenceNumber = mySeqNo.intValue();
		
		// send the packet
		if (pkt.recvId == myID) {
			// send to self
			bqs.recvPackets.put(pkt);
		}
		else {
			// send to others
			MSocket sock = bqs.socketMap.get(pkt.recvId);
			
			if (sock == null) {
				Debug.log(TAG, "Socket is null " + pkt.recvId);
			}
			else {
				sock.writeObject(pkt);
			}
		}
		
		// incr seqno
		mySeqNo.incrementAndGet();
	}
}
