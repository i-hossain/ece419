import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

public class ZkConnector implements Watcher {

    // ZooKeeper Object
    ZooKeeper zooKeeper;

    // To block any operation until ZooKeeper is connected. It's initialized
    // with count 1, that is, ZooKeeper connect state.
    CountDownLatch connectedSignal = new CountDownLatch(1);
    
    // ACL, set to Completely Open
    protected static final List<ACL> acl = Ids.OPEN_ACL_UNSAFE;
    
    // for znode paths
    public static final String SLASH = "/";

    /**
     * Connects to ZooKeeper servers specified by hosts.
     */
    public void connect(String hosts) {
	    try {
	    	zooKeeper = new ZooKeeper(
	                hosts, // ZooKeeper service hosts
	                5000,  // Session timeout in milliseconds
	                this); // watcher - see process method for callbacks
			connectedSignal.await();
		} catch (InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * Closes connection with ZooKeeper
     */
    public void close() throws InterruptedException {
	    zooKeeper.close();
    }

    /**
     * @return the zooKeeper
     */
    public ZooKeeper getZooKeeper() {
        // Verify ZooKeeper's validity
        if (null == zooKeeper || !zooKeeper.getState().equals(States.CONNECTED)) {
	        throw new IllegalStateException ("ZooKeeper is not connected.");
        }
        return zooKeeper;
    }

    protected Stat exists(String path, Watcher watch) {
        
        Stat stat = null;
        try {
            stat = zooKeeper.exists(path, watch);
        } catch(Exception e) {
        }
        
        return stat;
    }

    protected KeeperException.Code create(String path, String data, CreateMode mode) {
        
        try {
            byte[] byteData = null;
            if(data != null) {
                byteData = data.getBytes();
            }
            zooKeeper.create(path, byteData, acl, mode);
            
        } catch(KeeperException e) {
            return e.code();
        } catch(Exception e) {
            return KeeperException.Code.SYSTEMERROR;
        }
        
        return KeeperException.Code.OK;
    }

    public void process(WatchedEvent event) {
        // release lock if ZooKeeper is connected.
        if (event.getState() == KeeperState.SyncConnected) {
            connectedSignal.countDown();
        }
    }
    
    public String createPath(String znode) {
    	return (SLASH + znode);
    }
    
    public String appendPath(String path, String znode) {
    	return (path + SLASH + znode);
    }
    
    public boolean checkAndCreate(String path, String data, CreateMode mode, Watcher watch) throws KeeperException, InterruptedException {
        Stat stat = this.exists(path, watch);
        if (stat == null) {              // znode doesn't exist; let's try creating it
//        	System.out.println("Creating " + path);
        	Code ret = this.create(path,
                    data,
                    mode);
            if (ret == Code.OK) {
//            	System.out.println("Created " + path);
            	return true;
            }
        }
        return false;
    }
    
    public boolean createGroup(String path, String data, Watcher watch) throws KeeperException, InterruptedException {
        return checkAndCreate(path, data, CreateMode.PERSISTENT, watch);
    }
    
    public boolean joinGroup(String path, String data, Watcher watch) throws KeeperException, InterruptedException {
    	// if the service dies then the service is automatically removed from the group
        return checkAndCreate(path, data, CreateMode.EPHEMERAL, watch);
    }
    
    public boolean createzDaGroupz(String path, String data, Watcher watch) {
        try {
			return checkAndCreate(path, data, CreateMode.PERSISTENT, watch);
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return false;
    }
    
    public boolean joinzDaGroupz(String path, String data, Watcher watch) {
    	// if the service dies then the service is automatically removed from the group
        try {
			return checkAndCreate(path, data, CreateMode.EPHEMERAL, watch);
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return false;
    }
    
    public void leaveGroup(String path) throws KeeperException, InterruptedException {
    	// if the service dies then the service is automatically removed from the group
        zooKeeper.delete(path, -1);
    }
    
    public void listChildren(String path, Watcher watcher) throws KeeperException, InterruptedException {
        List<String> children = zooKeeper.getChildren(path, watcher);
        
        if (children.isEmpty()) {
            System.out.println("No members in " + path);
            System.out.println("--------------------");
            return;
        }
        System.out.println("Members in " + path);
        System.out.println(children);
        System.out.println("--------------------");
    }
}

