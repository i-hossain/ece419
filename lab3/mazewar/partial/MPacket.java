import java.io.Serializable;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

public class MPacket implements Serializable {

    /*The following are the type of events*/
    public static final int HELLO = 100;
    public static final int ACTION = 200;
    public static final int ACK = 200;

    /*The following are the specific action 
    for each type*/
    /*Initial Hello*/
    public static final int HELLO_INIT = 101;
    /*Response to Hello*/
    public static final int HELLO_RESP = 102;

    /*Action*/
    public static final int UP = 201;
    public static final int DOWN = 202;
    public static final int LEFT = 203;
    public static final int RIGHT = 204;
    public static final int FIRE = 205;
    public static final int MP = 206;
    
    //These fields characterize the event  
    public int type;
    public int event; 

    //The name determines the client that initiated the event
    public String name;
    
    //The sequence number of the event
    public int sequenceNumber;

    //These are used to initialize the board
    public int mazeSeed;
    public int mazeHeight;
    public int mazeWidth; 
    public Player[] players;
    
    public int port;
    public ClientData[] clientData;

    public Projectile prj;
    
    public VectorClock clock;
    
    public int originId = -1;
    public int senderId = -1;
    public int recvId = -1;
	public int uniqId = -1;
	
	public ConcurrentHashMap<Integer, Boolean> acks = new ConcurrentHashMap<Integer, Boolean>();
	
    public static Comparator<MPacket> COMPARE_BY_SEQNO = new Comparator<MPacket>() {
		@Override
		public int compare(MPacket arg0, MPacket arg1) {
			// TODO Auto-generated method stub
			return (arg0.sequenceNumber < arg1.sequenceNumber) ? -1 : 1;
		}
    };

    public static Comparator<MPacket> COMPARE_BY_CLOCK = new Comparator<MPacket>() {
		@Override
		public int compare(MPacket arg0, MPacket arg1) {
			// TODO Auto-generated method stub
			int compr = arg0.clock.compareTo(arg1.clock);
			
			if (compr == VectorClock.BEFORE) {
				return -1;
			}
			else if (compr == VectorClock.AFTER) {
				return 1;
			}
			
			// if clocks are identical or concurrent, compare by pid
			return (arg0.senderId < arg1.senderId) ? -1 : 1;
		}
    };
    
    public MPacket(){}

    public MPacket(int type, int event){
        this.type = type;
        this.event = event;
    }
    
    public MPacket(String name, int type, int event){
        this.name = name;
        this.type = type;
        this.event = event;
    }
//
//    public MPacket(String name, int type, int event, Projectile prj){
//        this.name = name;
//        this.type = type;
//        this.event = event;
//        this.prj = prj; // Directed Projectile
//    }
    
    public MPacket(MPacket pkt) {
		// TODO Auto-generated constructor stub
    	this.name = pkt.name;
        this.type = pkt.type;
        this.event = pkt.event;
        this.prj = pkt.prj; // Directed Projectile
        
        this.originId = pkt.originId;
        this.senderId = pkt.senderId;
        this.recvId = pkt.recvId;
        this.uniqId = pkt.uniqId;
        this.clock = pkt.clock;
	}
    
    public void makeAckOf(MPacket pkt) {
		// TODO Auto-generated constructor stub
    	this.name = pkt.name;
        this.type = MPacket.ACK;
        this.event = pkt.event;
        this.prj = pkt.prj; // Directed Projectile
        
        this.originId = pkt.originId;
        
        // switch receiver and sender
        this.senderId = pkt.recvId;
        this.recvId = pkt.senderId;
        
        this.uniqId = pkt.uniqId;
        this.clock = pkt.clock;
	}

	public String toString(){
        String typeStr;
        String eventStr;
        
        switch(type){
            case 100:
                typeStr = "HELLO";
                break;
            case 200:
                typeStr = "ACTION";
                break;
            case 300:
                typeStr = "ACK";
                break;
            default:
                typeStr = "ERROR";
                break;        
        }
        switch(event){
            case 101:
                eventStr = "HELLO_INIT";
                break;
            case 102:
                eventStr = "HELLO_RESP";
                break;
            case 201:
                eventStr = "UP";
                break;
            case 202:
                eventStr = "DOWN";
                break;
            case 203:
                eventStr = "LEFT";
                break;
            case 204:
                eventStr = "RIGHT";
                break;
            case 205:
                eventStr = "FIRE";
                break;
            case 206:
                eventStr = "MP";
                break;
            default:
                eventStr = "ERROR";
                break;        
        }
        //MPACKET(NAME: name, <typestr: eventStr>, SEQNUM: sequenceNumber)
        String retString = String.format("MPACKET(NAME: %s, <%s: %s>, SEQNUM: %s)", name, 
            typeStr, eventStr, sequenceNumber);
        return retString;
    }
	
	public boolean isAckFor(MPacket arg0) {
		if (uniqId == arg0.uniqId && originId == arg0.originId)
			return true;
		return false;
	}
}
