import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;

public class MulticastSocket {
	// packet queues
	//The queue of packets to send
	private BlockingQueue<MPacket> egressQueue = null;
    //The queue of packets received
	private BlockingQueue<MPacket> ingressQueue = null;
	
	// list of clients
	private Hashtable<String, Client> clientTable = null;
	
	// vector clocks
	public static class VectorClock extends HashMap<String, Integer> implements Serializable, Comparable<VectorClock> {
		public static int IDENTICAL = 0;
		public static int BEFORE = 0;
		public static int AFTER = 0;
		public static int CONCURRENT = 0;
		
		// Incr vector clock for the client
		public void incr(String key) {
			// If contains key then incr else put
			Integer val = this.get(key);
		    if (val == null) {
		    	this.put(key, 1);
		    }
		    else {
		    	this.put(key, val++);
		    }
		}
		
		@Override
		public Integer get(Object key)
		{
			Integer val = super.get(key);

			if (val == null)
				val = 0;

			return val;
		}
		
		public static VectorClock merge(VectorClock first, VectorClock second)
		{
			VectorClock newClock = new VectorClock();

			// Insert all elements from the first clock
			for (String entry : first.keySet())
			{
				newClock.put(entry, first.get(entry));
			}

			// Replace with larger elements from the second clock
			for (String entry : second.keySet())
			{
				if (!newClock.containsKey(entry) || newClock.get(entry) < second.get(entry))
				{
					newClock.put(entry, second.get(entry));
				}
			}

			// Return the merged clock
			return newClock;
		}
		
		// compare vector clocks
		public int compareTo(VectorClock v) {
			boolean isIdentical = true;
			boolean isBefore = true;
			boolean isAfter = true;
			
			for (String key : this.keySet()) {
				if (v.containsKey(key)) {
					if (this.get(key) < v.get(key)) {
						isIdentical = false;
						isAfter = false;
					}
					else if (this.get(key) > v.get(key)) {
						isIdentical = false;
						isBefore = false;
					}
				}
				else if (this.get(key) != 0) {
					// the value in v is 0 but the value in this is bigger
					isIdentical = false;
					isBefore = false;
				}
			}
			
			if (isIdentical) {
				return VectorClock.IDENTICAL;
			}
			else if (isAfter && !isBefore) {
				return VectorClock.AFTER;
			}
			else if (isBefore && !isAfter) {
				return VectorClock.BEFORE;
			}
			
			return VectorClock.CONCURRENT;
		}
	}
	
}