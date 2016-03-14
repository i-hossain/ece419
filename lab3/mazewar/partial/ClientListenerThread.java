import java.io.IOException;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientListenerThread implements Runnable {
	private final String TAG = this.getClass().getSimpleName();

    private MSocket mSocket  =  null;
    private PriorityBlockingQueue<MPacket> clientQueue = null;
    
	private int peerId;
	private BuffQueue bqs;
	private VectorClock myClk;

    public ClientListenerThread(int peerId,
                                VectorClock myClock,
                                BuffQueue buffers){
    	Debug.log(TAG, "init");
    	
        this.peerId = peerId;
        this.myClk = myClock;
        this.bqs = buffers;
        if (peerId != bqs.myPid) {
        	this.mSocket = bqs.socketMap.get(peerId);
        	this.clientQueue = new PriorityBlockingQueue<MPacket>(BuffQueue.INIT_CAPACITY, MPacket.COMPARE_BY_SEQNO);
        }
        else
        	this.clientQueue = bqs.recvPackets;
    }

    public void run() {
    	Debug.log(TAG, "Client Listener Thread started");
    	
        Thread dequeueThread = new Thread() { 
        	public void run() {
        		Debug.log(TAG, "dequeue Thread started");
        		MPacket head = null;
            	MPacket temp;

        		AtomicInteger mySeqNo = bqs.recvSeqNo.get(peerId);
            	
            	while(true){
            		head = clientQueue.peek();
            		
            		if (head != null) {
            			
        				if (head.sequenceNumber == mySeqNo.intValue()) {
        					// packet is next (this is to ensure FIFO)
//        					Debug.log(TAG, "Received " + head);

        					// merge vector clock and increment my clock
        					myClk.mergeFrom(head.clock);
        					
        					// Filter packet types
        					if (head.type == MPacket.ACTION) {
        						// msg is next in sequence, broadcast ack and add to recv queue
        						clientQueue.remove(head);
        						
        						// Add to send Q for multicasting ACK
        						temp = new MPacket();
        						temp.makeAckOf(head);
        						temp.recvId = -1; // for multicast
        			
        						try {
									bqs.eventQueue.put(temp);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
        						bqs.recvQueue.add(head);
        						mySeqNo.incrementAndGet();
        						
        					} else if (head.type == MPacket.ACK) {
        						// incr ack counter
        						
        						for (MPacket mPkt : bqs.recvQueue) {
        							if (head.isAckFor(mPkt)) {
        								clientQueue.remove(head);
        								
        								// incr ack counter
        								mPkt.acks.put(head.senderId, true);
        								
        								mySeqNo.incrementAndGet();
        								break;
        							}
        						}
        					}
        				} 
        				else if (head.sequenceNumber < mySeqNo.intValue()) {
        					// packet was already received and processed
        					clientQueue.remove(head);
        					
        					// Filter packet types
        					if (head.type == MPacket.ACTION) {
        						// ACK was probably lost
        						temp = new MPacket();
        						temp.makeAckOf(head);
//        						try {
//        							bqs.eventQueue.put(temp);
//								} catch (InterruptedException e) {
//									// TODO Auto-generated catch block
//									e.printStackTrace();
//								}
        						
        					} else if (head.type == MPacket.ACK) {
        						// ack was retransmitted. drop it
        					}
        				}
        			}
            	}
        	}
        };

        dequeueThread.start();
        
        if (mSocket != null) {
//          Thread enqueueThread = new Thread() { 
//        	public void run() {
        		Debug.log(TAG, "enqueue Thread started");
        		MPacket received = null;
        		Debug.log(TAG, "Starting ClientListenerThread");
                while(true){
                    try{
                        received = (MPacket) mSocket.readObject();
                        Debug.log(TAG, "Queueing Received " + received);
                    	// Enqueue
                    	clientQueue.put(received);
                    }catch(IOException e){
                        e.printStackTrace();
                    }catch(ClassNotFoundException e){
                        e.printStackTrace();
                    }            
                }
//        	}
//        };
        }
    }
    
    
}
