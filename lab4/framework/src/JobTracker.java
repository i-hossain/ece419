import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.zookeeper.KeeperException;


public class JobTracker {
	public ZkConnector zkc;
	public static final String TASK_GROUP = "tasks";
	public static int NUM_DICT_PART = 10;
	
	private byte [] false_bytes = (new Boolean(false)).toString().getBytes();
	private byte [] true_bytes = (new Boolean(true)).toString().getBytes();
	
	public static void main(String[] args) {    	
    	if (args.length != 1) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. JobTracker zkServer:clientPort");
            return;
        }
    	
    	JobTracker jt = new JobTracker(args[0]);
    	
    	jt.start();
    }
    
    private void start() {
		// TODO Auto-generated method stub
    	String tasksPath = "";
    	try {
			tasksPath = zkc.createGroup("", TASK_GROUP);
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	// get hash of a word
    	String word = "r41nb0w";
        String hash = getHash(word);
        System.out.println(hash);
        
        String hashPath = "";
    	try {
			hashPath = zkc.createGroup(tasksPath, hash);
			
			for(int i = 1; i < NUM_DICT_PART; i++) {
	    		String tpath = zkc.createGroup(hashPath, String.valueOf(i));
	    		new Thread(new ListGroupForever(zkc, tpath)).start();
	    	}
			
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        while(true);
	}

	public JobTracker(String hosts) {
        zkc = new ZkConnector();
        try {
            zkc.connect(hosts);
        } catch(Exception e) {
            System.out.println("Zookeeper connect "+ e.getMessage());
        }
    }
    
    public static String getHash(String word) {

        String hash = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            BigInteger hashint = new BigInteger(1, md5.digest(word.getBytes()));
            hash = hashint.toString(16);
            while (hash.length() < 32) hash = "0" + hash;
        } catch (NoSuchAlgorithmException nsae) {
            // ignore
        }
        return hash;
    }
}
