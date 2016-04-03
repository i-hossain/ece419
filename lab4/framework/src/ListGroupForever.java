import java.util.concurrent.Semaphore;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;


public class ListGroupForever implements Runnable {
    ZkConnector zkc;
    Watcher watcher;
    Semaphore sem;
    String path;
    
    public ListGroupForever(ZkConnector zkc, String path) {
    	this.zkc = zkc;
    	
    	this.path = path;
    	
    	sem = new Semaphore(1);
        
        watcher = new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (event.getType() == Event.EventType.NodeChildrenChanged) {
                    sem.release();
                }
            }
        };
    }
    
    public void listForever(String path) throws KeeperException, InterruptedException {    	
    	sem.acquire();
        while (true) {
            zkc.listChildren(path, watcher);
            sem.acquire();
        }
    }

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			listForever(path);
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
