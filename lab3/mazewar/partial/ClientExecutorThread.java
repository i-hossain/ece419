import java.util.Hashtable;


public class ClientExecutorThread implements Runnable {
	private final String TAG = this.getClass().getSimpleName();
	
	private Hashtable<String, Client> clientTable = null;
	private BuffQueue bqs;
	
	public ClientExecutorThread(Hashtable<String, Client> clientTable, BuffQueue buffers) {
		Debug.log(TAG, "Init");
		
		this.clientTable = clientTable;
		this.bqs = buffers;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		MPacket head = null;
    	Client client = null;
    	while(true){
    		head = bqs.recvQueue.peek();
    		if (head != null && head.acks.size() == bqs.otherClients.size()) {
    			//packet is ready to be executed :)
    			bqs.recvQueue.remove(head);
    			
    			// Execute the action
    			client = clientTable.get(head.name);
    			executeAction(client, head);
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
        }else if(received.event == MPacket.MP){
            client.moveProjectile();
        }
        else{
            throw new UnsupportedOperationException();
        }
    }
}
