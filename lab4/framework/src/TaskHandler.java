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
            	//sem.release();
            }
        };
	}

	@Override
	public void run() {
		System.out.println("Started TaskChecker " + taskPath);
		
		int childNum = 0;
		
		String handlerPath = zkc.appendPath(taskPath, JobTracker.HANDLER);
		do {
        	try {
        		//sem.acquire();
        		
				// TODO Auto-generated method stub
		
				List<String> taskParts = zkc.getZooKeeper().getChildren(taskPath, null);
				
				childNum = taskParts.size();
				
				for (String partition : taskParts) {
					if (partition.equals(handlerPath))
						continue;
					// the task is not complete yet
	    			String acqPath = zkc.appendPath(taskPath, partition);
	    			parseResult(taskPath, acqPath);
	    		}
        	} catch (KeeperException | InterruptedException | UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				// ignore no node exception
    			//System.out.println("EXCEPTION: " + e.getMessage());
        		//e.printStackTrace();
        		//sem.release();
			}
        } while (childNum != 1);
		
		System.out.println("Finished TaskChecker " + taskPath);
		
		try {
			if (zkc.getZooKeeper().getData(taskPath, false, null) == null) {
				String result = FAIL + "-" + "password not found";
				zkc.getZooKeeper().setData(taskPath, result.getBytes(), -1);
			}
			
			// leave the handler group
			zkc.leaveGroup(handlerPath);
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void parseResult(String taskPath, String acqPath) throws KeeperException, InterruptedException, UnsupportedEncodingException {
		// TODO Auto-generated method stub
		String acceptTaskPath = zkc.appendPath(acqPath, Worker.TASK_ACCEPTED);
    	byte [] taskstatus = zkc.getZooKeeper().getData(acqPath, false, null);
    	if (taskstatus != null) {
    		// task is done
    		String resultString = new String(taskstatus, "UTF-8");
    		String [] result = resultString.split("-");
    		
//    		System.out.println("Result " + acqPath + " : " + resultString);
    		
    		if (result[0].equals(SUCCESS)) {
    			zkc.getZooKeeper().setData(taskPath, resultString.getBytes(), -1);
    		}
    		resultCount++;
    		
    		try {
    			// this is if worker didn't delete the znode
    			zkc.leaveGroup(acqPath);
    		} catch (KeeperException e) {
				// TODO Auto-generated catch block
				// ignore no node exception
    			System.out.println("Cant delete " + acqPath);
			}
    	}
	}

}
