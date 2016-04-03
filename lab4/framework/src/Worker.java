import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.Watcher.Event.KeeperState;
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
    	String path = zkc.createPath(GROUP);
    	try {
			zkc.createGroup(path, null, null);
			String memberPath = zkc.appendPath(path, member);
			zkc.joinGroup(memberPath, null, null);
			
			workForever();
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Worker(String hosts) {
        zkc = new ZkConnector();
        try {
            zkc.connect(hosts);
        } catch(Exception e) {
            System.out.println("Zookeeper connect "+ e.getMessage());
        }
        
        watcher = new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (event.getType() == Event.EventType.NodeChildrenChanged) {
                    sem.release();
                }
            }
        };
        
        String [] temp = ManagementFactory.getRuntimeMXBean().getName().split("@");
    	member = temp[1].split("\\.")[0] + "-" + temp[0];
    	
    	System.out.println("Created worker: " + member);
    }
    
    public void workForever() throws KeeperException, InterruptedException {
    	String path = zkc.createPath(JobTracker.TASK_GROUP);;
    	
    	
    	sem.acquire();
        while (true) {
        	//zkc.listChildren(path, watcher);
        	List<String> children = zkc.getZooKeeper().getChildren(path, watcher);
        	
        	for (String taskHash : children) {
        		// we have some tasks
        		try {
	        		String taskPath = zkc.appendPath(path, taskHash);

        			List<String> taskParts = zkc.getZooKeeper().getChildren(taskPath, watcher);
        			
    				for (String partition : taskParts) {
    					// the task is not complete yet
            			String acqPath = zkc.appendPath(taskPath, partition);
            			if(takeTask(acqPath)) {
            				// we have taken the task
            				// now do stuff
            				Boolean result = doTask(taskHash, Integer.parseInt(partition));
            				zkc.getZooKeeper().setData(acqPath, result.toString().getBytes(), -1);
            				releaseTask(acqPath);
            			}
            		}

        		} catch (KeeperException e) {
    				// TODO Auto-generated catch block
    				// ignore no node exception
        			System.out.println("EXCEPTION: " + e.getMessage());
        			if (e.getMessage() != "NONODE")
        				e.printStackTrace();
    			}
        	}
        	
            sem.acquire();
        }
    }
    
    private void releaseTask(String acqPath) throws KeeperException, InterruptedException {
    	String acceptTaskPath = zkc.appendPath(acqPath, TASK_ACCEPTED);
    	zkc.leaveGroup(acceptTaskPath);
	}

	private boolean takeTask(String acqPath) throws KeeperException, InterruptedException {
    	String acceptTaskPath = zkc.appendPath(acqPath, TASK_ACCEPTED);
    	byte [] taskstatus = zkc.getZooKeeper().getData(acceptTaskPath, false, null);
    	if (taskstatus == null)
    		return zkc.joinGroup(acceptTaskPath, null, null);
    	else
    		return false;
    }
    
    private boolean doTask(String hash, Integer partition) {
        return true; 
    }
}
