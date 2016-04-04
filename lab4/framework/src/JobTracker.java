import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;


public class JobTracker {
	public ZkConnector zkc;
	public static final String TRACKER = "tracker";
	public static final String TASK_GROUP = "tasks";
	public static int NUM_DICT_PART = 10;
	
	private Watcher watcher;
	
	private String trackerData = "";
	private ServerSocket myServerSock;
	private String tasksPath;
	
	ExecutorService executor = Executors.newCachedThreadPool();
	
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
    	String trackerPath = zkc.createPath(TRACKER);
    	zkc.joinzDaGroupz(trackerPath, trackerData, watcher);
    	
    	tasksPath = zkc.createPath(TASK_GROUP);
    	zkc.createzDaGroupz(tasksPath, null, null);
    	
    	// get hash of a word
    	String word = "r41nb0w";
        String hash = getHash(word);
        System.out.println(hash);
        
        String hashPath = zkc.appendPath(tasksPath, hash);
		zkc.createzDaGroupz(hashPath, null, null);
		executor.submit(new TaskHandler(zkc, hashPath));
		
		for(int i = 1; i <= NUM_DICT_PART; i++) {
    		String tpath = zkc.appendPath(hashPath, String.valueOf(i));
    		zkc.createzDaGroupz(tpath, null, null);
//	    		new Thread(new ListGroupForever(zkc, tpath)).start();
    	}
        
        while(true);
	}

	public JobTracker(String hosts) {
        zkc = new ZkConnector();
        zkc.connect(hosts);
        
        try {
			myServerSock = new ServerSocket(0);
			trackerData = InetAddress.getLocalHost().toString().split("/")[1] + ":" + myServerSock.getLocalPort();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        System.out.println(trackerData);
        
        watcher = new Watcher() { // Anonymous Watcher
            @Override
            public void process(WatchedEvent event) {
            	String path = event.getPath();
                EventType type = event.getType();
                String myPath = zkc.createPath(TRACKER);
                
                if(path.equalsIgnoreCase(myPath)) {
                    if (type == EventType.NodeDeleted) {
                        System.out.println(myPath + " deleted! Let's go!");       
                        zkc.joinzDaGroupz(myPath, trackerData, watcher);
                    }
                    if (type == EventType.NodeCreated) {
                        System.out.println(myPath + " created!");
                        try{ Thread.sleep(5000); } catch (Exception e) {}
                        zkc.joinzDaGroupz(myPath, trackerData, watcher);
                    }
                }
        
            } };
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
