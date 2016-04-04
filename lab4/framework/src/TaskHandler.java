import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.Semaphore;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class TaskHandler implements Runnable {
	
	ZkConnector zkc;
    Watcher watcher;
    Semaphore sem = new Semaphore(1);
    String taskPath;
    int resultCount = 0;
    
    public static final String SUCCESS = "SUCCESS";
    public static final String FAIL = "FAIL";

	public TaskHandler(ZkConnector zkc, String hashPath) {
		// TODO Auto-generated constructor stub
		this.zkc = zkc;
    	this.taskPath = hashPath;
    	
    	watcher = new Watcher() {
            @Override
            public void process(WatchedEvent event) {
            	sem.release();
            }
        };
	}

	@Override
	public void run() {
		System.out.println("Started TaskChecker " + taskPath);
		do {
        	try {
        		sem.acquire();
        		
				// TODO Auto-generated method stub
		
				List<String> taskParts = zkc.getZooKeeper().getChildren(taskPath, watcher);
				
				for (String partition : taskParts) {
					// the task is not complete yet
	    			String acqPath = zkc.appendPath(taskPath, partition);
	    			parseResult(taskPath, acqPath);
	    		}
        	} catch (KeeperException | InterruptedException | UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				// ignore no node exception
    			//System.out.println("EXCEPTION: " + e.getMessage());
        		e.printStackTrace();
        		sem.release();
			}
        } while (resultCount < JobTracker.NUM_DICT_PART);
		
		System.out.println("Finished TaskChecker " + taskPath);
	}

	private void parseResult(String taskPath, String acqPath) throws KeeperException, InterruptedException, UnsupportedEncodingException {
		// TODO Auto-generated method stub
		String acceptTaskPath = zkc.appendPath(acqPath, Worker.TASK_ACCEPTED);
    	byte [] taskstatus = zkc.getZooKeeper().getData(acqPath, false, null);
    	if (taskstatus != null) {
    		// task is done
    		String [] result = (new String(taskstatus, "UTF-8")).split("-");
    		
    		System.out.println("Result " + acqPath + " : " + new String(taskstatus, "UTF-8"));
    		
    		if (result[0].equals(SUCCESS)) {
    			zkc.getZooKeeper().setData(taskPath, result[1].getBytes(), -1);
    		}
    		resultCount++;
    		
    		try {
    			// this is if worker didnt delete the znode
    			zkc.leaveGroup(acceptTaskPath);
    			zkc.leaveGroup(acqPath);
    		} catch (KeeperException e) {
				// TODO Auto-generated catch block
				// ignore no node exception
			}
    	}
	}

}
