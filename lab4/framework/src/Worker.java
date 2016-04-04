import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;


public class Worker {
	
	public static final String GROUP = "workers";
    public ZkConnector zkc;
    private Semaphore sem = new Semaphore(1);
    Watcher watcher;
	private String member;
    public static final String TASK_ACCEPTED = "accepted";
    
    CountDownLatch fileServerCreated = new CountDownLatch(1);
    
    public static void main(String[] args) {    	
    	if (args.length != 1) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. Worker zkServer:clientPort");
            return;
        }
 	
    	Worker w = new Worker(args[0]);
    	
    	w.start();
    }
    
    private void start() {
		// TODO Auto-generated method stub
//    	String path = zkc.createPath(GROUP);
//    	zkc.createzDaGroupz(path, null, null);
//    	String memberPath = zkc.appendPath(path, member);
//		zkc.joinzDaGroupz(memberPath, null, null);
		
		try {
			workForever();
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Worker(String hosts) {
        zkc = new ZkConnector();
        zkc.connect(hosts);
        
        watcher = new Watcher() {
            @Override
            public void process(WatchedEvent event) {
//                if (event.getType() == EventType.NodeChildrenChanged ||
//                		event.getType() == EventType.NodeDeleted) {
                    sem.release();
//                }
            }
        };
        
        String [] temp = ManagementFactory.getRuntimeMXBean().getName().split("@");
    	member = temp[1].split("\\.")[0] + "-" + temp[0];
    	
    	System.out.println("Created worker: " + member);
    }
    
    public void workForever() throws KeeperException, InterruptedException {
    	String path = zkc.createPath(JobTracker.TASK_GROUP);
    	zkc.createzDaGroupz(path, null, null);
    	
    	// wait for job trackers to start
//    	Stat s = zkc.exists(path, new Watcher() {
//								@Override
//								public void process(WatchedEvent event) {
//									// TODO Auto-generated method stub
//									trackerCreated.countDown();
//								}	
//					    	});
//    	if (s == null)
//    		trackerCreated.await();
    	
    	String handlerPath = zkc.createPath(JobTracker.HANDLER);
    	
        do {
        	sem.acquire();
        	
        	try {        		
	        	List<String> children = zkc.getZooKeeper().getChildren(path, watcher);
	        	
	        	for (String taskHash : children) {
        			// we have some tasks
	        		String taskPath = zkc.appendPath(path, taskHash);

        			List<String> taskParts = zkc.getZooKeeper().getChildren(taskPath, watcher);
        			
    				for (String partition : taskParts) {
    					if (partition.equals(handlerPath))
    						continue;
    					
    					// the task is not complete yet
            			String acqPath = zkc.appendPath(taskPath, partition);
            			if(takeTask(acqPath)) {
            				// we have taken the task
//            				System.out.println("We took " + acqPath);
            				// now do stuff
            				String result = doTask(taskHash, Integer.parseInt(partition));
            				zkc.getZooKeeper().setData(acqPath, result.getBytes(), -1);
            				releaseTask(acqPath);
            			}
            		}
	        	}
        	} catch (KeeperException e) {
				// TODO Auto-generated catch block
				// ignore no node exception
    			System.out.println("EXCEPTION: " + e.getMessage());
//        		e.printStackTrace();
        		sem.release();
			}
        } while (true);
    }
    
    private void releaseTask(String acqPath) throws KeeperException, InterruptedException {
    	String acceptTaskPath = zkc.appendPath(acqPath, TASK_ACCEPTED);
    	zkc.leaveGroup(acceptTaskPath);
	}

	private boolean takeTask(String acqPath) throws KeeperException, InterruptedException {
    	String acceptTaskPath = zkc.appendPath(acqPath, TASK_ACCEPTED);
    	byte [] taskstatus = zkc.getZooKeeper().getData(acqPath, false, null);
    	if (taskstatus == null)
    		return zkc.joinGroup(acceptTaskPath, null, null);
    	else
    		return false;
    }
	
	private String [] lookupFileServer() {
		String ipResult = null;
		String fsPath = zkc.createPath(FileServer.FILESERVER);
		Stat s = zkc.exists(fsPath, new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					// TODO Auto-generated method stub
					fileServerCreated.countDown();
				}	
	    	});
		if (s == null) {
			fileServerCreated.countDown();
		}
		
		do {
		
			try {
				byte [] taskstatus = zkc.getZooKeeper().getData(fsPath, false, null);
				
				if (taskstatus != null) {
		    		// task is done
		    		ipResult = (new String(taskstatus, "UTF-8"));
//		    		System.out.println("ipAddr: " + ipResult);
		    		
		    		String [] address = ipResult.split(":");
		    		
//		    		System.out.println(address[0] + " -- " + address[1]);
		    		
		    		return address;
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
		
		} while(true);
	}
	
	private List<String> getDictionary(int partition) {
		String [] address = lookupFileServer();
    	Socket socket = null;
    	
    	try {
			socket = new Socket(address[0], Integer.parseInt(address[1]));
			
			FSPacket sendpacket = new FSPacket();
			sendpacket.partitionID = partition;

			ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
			os.writeObject(sendpacket);
				
			ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
			FSPacket recvpacket = (FSPacket)is.readObject();
			
//			System.out.println("PARTITION: " + recvpacket.partitionID);
//			System.out.println("WORDS: " + recvpacket.list.size());
			
			socket.close();
			
			return recvpacket.list;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
    	
    	return null;
	}
    
    private String doTask(String hash, Integer partition) {
    	// contact fileserver
    	List<String> words = getDictionary(partition);
    	
    	if (words != null) {
    		for (String word : words) {
        		if(getHash(word).equals(hash)) {
        			return "SUCCESS" + "-" + word; 
        		}
        	}
    	}
    	
        return "FAIL" + "-" + "password not found";
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
