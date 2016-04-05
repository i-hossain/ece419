import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.data.Stat;


public class JobTracker {
	public ZkConnector zkc;
	public static final String TRACKER = "tracker";
	public static final String TASK_GROUP = "tasks";
	public static final String HANDLER = "handler";
	
	public static int NUM_DICT_PART = 2658;
	
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
    	tasksPath = zkc.createPath(TASK_GROUP);
    	zkc.createzDaGroupz(tasksPath, null, null);
    	
    	String trackerPath = zkc.createPath(TRACKER);
    	if (zkc.joinzDaGroupz(trackerPath, trackerData, watcher))
    		dozDaShiz();
    	
    	System.out.println("Sleeping...");
        while (true) {
            try{ Thread.sleep(5000); } catch (Exception e) {}
        }
	}

	private void dozDaShiz() {
		// TODO Auto-generated method stub
		
		Socket clientsock = null;
		ObjectInputStream in;
		ObjectOutputStream out;
		
		while (true) {
			try {
				clientsock = myServerSock.accept();
				
				in = new ObjectInputStream(clientsock.getInputStream());
				out = new ObjectOutputStream(clientsock.getOutputStream());
				
				// get query from client
				JTPacket request = (JTPacket) in.readObject();
				
				System.out.println("QUERY: " + request.query);
		        System.out.println("HASH: " + request.hash);
		        
		        JTPacket response = new JTPacket();
		        
		        String hashPath = zkc.appendPath(tasksPath, request.hash);
		        
		        if (request.query.equals(ClientDriver.JOB)) {
//		        	Stat stat = zkc.exists(hashPath, null);
//		        	if (stat != null) {
//		        		// job already submitted
//		        		response.status = "Job already submitted!";
//		        		
//		        		String handlerPath = zkc.appendPath(hashPath, HANDLER);
//						zkc.joinzDaGroupz(handlerPath, null, null);
//		        	}
//		        	else {
		        		if (zkc.createzDaGroupz(hashPath, null, null) == false) {
		        			// job already submitted
			        		response.status = JTPacket.submitted;
		        		}
		        		else {
		        			System.out.println("Started CreatePartition " + hashPath);
			        		for(int i = 1; i <= JobTracker.NUM_DICT_PART; i++) {
			            		String tpath = zkc.appendPath(hashPath, String.valueOf(i));
			            		zkc.createzDaGroupz(tpath, null, null);
			            	}
			        		System.out.println("Finished CreatePartition " + hashPath);
			        		
//							executor.submit(new TaskHandler(zkc, hashPath));
							
							response.status = JTPacket.already_submitted;
		        		}
		        		
//						String handlerPath = zkc.appendPath(hashPath, HANDLER);
//						zkc.joinzDaGroupz(handlerPath, null, null);
		        } 
		        else if (request.query.equals(ClientDriver.STATUS)) {
		        	Stat stat = zkc.exists(hashPath, null);
		        	if (stat == null) {
		        		// job was never submitted
		        		response.status = JTPacket.never_submitted;
		        	}
		        	else {
		        		byte [] taskstatus = zkc.getZooKeeper().getData(hashPath, false, null);
		        		
		        		if (taskstatus == null) {
		        			// job is in progress or it failed
		        			
		        			List<String> taskParts = zkc.getZooKeeper().getChildren(hashPath, false);
		        			
		        			if (taskParts.size() != 0)
		        				response.status = JTPacket.in_progress;
		        			else
		        				response.status = JTPacket.failure;
		        		}
		        		else {
		        			String [] result = (new String(taskstatus, "UTF-8")).split("-");
		        			
		        			System.out.println("taskstatus " + new String(taskstatus, "UTF-8"));
		        			
		        			response.status = JTPacket.success;
		        			response.result = result[1];
		        		}
		        		
		        		// Special case where handler crashed.. 
//			        	String handlerPath = zkc.appendPath(hashPath, HANDLER);
//			        	Stat statH = zkc.exists(handlerPath, null);
//			        	
//			        	if(statH == null) {
//			        		executor.submit(new TaskHandler(zkc, hashPath));
//			        		zkc.joinzDaGroupz(handlerPath, null, null);
//			        	}
		        	}
		        }
		        
		        out.writeObject(response);
				
			} catch (IOException | ClassNotFoundException | KeeperException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public JobTracker(String hosts) {
        zkc = new ZkConnector();
        zkc.connect(hosts);
        
        try {
			myServerSock = new ServerSocket(0);
			trackerData = InetAddress.getLocalHost().toString().split("/")[0] + ":" + myServerSock.getLocalPort();
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
                String trackerPath = zkc.createPath(TRACKER);
                
                if(path.equalsIgnoreCase(trackerPath)) {
                    if (type == EventType.NodeDeleted) {
                        System.out.println(trackerPath + " deleted! Let's go!");       
                        if (zkc.joinzDaGroupz(trackerPath, trackerData, watcher))
                    		dozDaShiz();
                    }
                    if (type == EventType.NodeCreated) {
                        System.out.println(trackerPath + " created!");
                        try{ Thread.sleep(5000); } catch (Exception e) {}
                        zkc.joinzDaGroupz(trackerPath, trackerData, watcher);
//                        if (zkc.joinzDaGroupz(trackerPath, trackerData, watcher))
//                    		dozDaShiz();
                    }
                }
        
            } };
    }
}
