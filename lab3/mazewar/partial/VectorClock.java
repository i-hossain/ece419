import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


public class VectorClock extends HashMap<Integer, Integer> implements Serializable, Comparable<VectorClock> {
	public static int IDENTICAL = 0;
	public static int BEFORE = 1;
	public static int AFTER = 2;
	public static int CONCURRENT = 3;
	
	// Incr vector clock for the client
	public synchronized void incr(Integer key) {
		// If contains key then incr else put
		Integer val = super.get(key);
	    if (val == null) {
	    	this.put(key, 1);
	    } else {
	    	this.put(key, val + 1);
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
		for (Integer entry : first.keySet())
		{
			newClock.put(entry, first.get(entry));
		}

		// Replace with larger elements from the second clock
		for (Integer entry : second.keySet())
		{
			if (!newClock.containsKey(entry) || newClock.get(entry) < second.get(entry))
			{
				newClock.put(entry, second.get(entry));
			}
		}

		// Return the merged clock
		return newClock;
	}
	
	public synchronized void mergeFrom(VectorClock second)
	{
		// Replace with larger elements from the second clock
		for (Integer key : second.keySet())
		{
			if (!super.containsKey(key) || super.get(key) < second.get(key))
			{
				super.put(key, second.get(key));
			}
		}
	}
	
	// compare vector clocks
	public int compareTo(VectorClock v) {
		boolean isBefore = false;
		boolean isAfter = false;
		
		Set<Integer> parentClocks = super.keySet();
		Set<Integer> vClocks = v.keySet();
		
//		System.out.println("this: " + parentClocks.size() + ", v: " + vClocks.size());
		
		SortedSet<Integer> commonClocks = new TreeSet<Integer>(parentClocks);
		commonClocks.retainAll(vClocks);
		
//		System.out.println("this: " + parentClocks.size() + ", v: " + vClocks.size() + ", com: " + commonClocks.size());
		
		if (vClocks.size() > commonClocks.size()) {
//			System.out.println("isb4 - 1");
			isBefore = true;
		}
		if (parentClocks.size() > commonClocks.size()) {
//			System.out.println("isaftr - 1");
			isAfter = true;
		}
		
		for (Integer key : commonClocks) {
			if (isBefore && isAfter)
				break;
			
			if (super.get(key) < v.get(key)) {
//				System.out.println("isb4 - 2");
				isBefore = true;
			}
			else if (super.get(key) > v.get(key)) {
//				System.out.println("isaftr - 2");
				isAfter = true;
			}
		}
		
		if (!isAfter && !isBefore) {
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
	
	@Override
	public synchronized VectorClock clone()
	{
		return (VectorClock) super.clone();
	}

	public synchronized void mergeFromAndIncr(Integer key, VectorClock clock) {
		// TODO Auto-generated method stub
		this.mergeFrom(clock);
//		this.incr(key);
	}
}
