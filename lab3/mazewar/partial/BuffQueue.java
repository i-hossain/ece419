import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class BuffQueue {
	public static int INIT_CAPACITY = 10;
	
	public HashMap<Integer, AtomicInteger> sendSeqNo = new HashMap<Integer, AtomicInteger>();
	
	public HashMap<Integer, AtomicInteger> recvSeqNo = new HashMap<Integer, AtomicInteger>();
	
	public BlockingQueue<MPacket> reSendQueue = new LinkedBlockingQueue<MPacket>();
	
	public PriorityBlockingQueue<MPacket> recvQueue = new PriorityBlockingQueue<MPacket>(BuffQueue.INIT_CAPACITY, MPacket.COMPARE_BY_CLOCK);
	
	public PriorityBlockingQueue<MPacket> recvPackets = new PriorityBlockingQueue<MPacket>(BuffQueue.INIT_CAPACITY, MPacket.COMPARE_BY_SEQNO);
	
	public ConcurrentHashMap<Integer, Client> otherClients;
	
	public ConcurrentHashMap<Integer, MSocket> socketMap = new ConcurrentHashMap<Integer, MSocket>();
	
	public BlockingQueue<MPacket> eventQueue = new LinkedBlockingQueue<MPacket>();
}
