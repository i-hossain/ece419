import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;


public class ClientDriver {

	public static final String TRACKER = "tracker";
	public static final String JOB = "job";
	public static final String STATUS = "status";
	public ZkConnector zkc;
	
	CountDownLatch trackerCreated = new CountDownLatch(1);
	private Socket socket;
	
	
	public static void main(String[] args) throws IOException
	{
		if (args.length != 2) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. ClientDriver zkServer:clientPort job/status hash");
            return;
        }
    	
    	ClientDriver fs = new ClientDriver(args[0]);
    	fs.start(args[1], args[2]);
	}
	
	private void start(String jobOrStatus, String hash) {
		// TODO Auto-generated method stub
		String ipResult = null;
		String path = zkc.createPath(TRACKER);
		Stat s = zkc.exists(path, new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					// TODO Auto-generated method stub
					trackerCreated.countDown();
				}	
	    	});
		if (s == null) {
			try {
				trackerCreated.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			byte [] taskstatus = zkc.getZooKeeper().getData(TRACKER, false, null);
			
			if (taskstatus != null) {
	    		// task is done
	    		ipResult = (new String(taskstatus, "UTF-8"));
	    		System.out.println("result: " + ipResult);
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String address [] = ipResult.split(":");
		
//		try {
//			socket = new Socket(address[0], Integer.parseInt(address[1]));
//			ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
//			ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
//			
//			if (jobOrStatus.toLowerCase().equals(JOB) || jobOrStatus.toLowerCase().equals(STATUS)) {
//				
//				JTPacket sendpacket = new JTPacket();
//				sendpacket.query = jobOrStatus.toLowerCase();
//				sendpacket.hash = hash;
//
//				os.writeObject(sendpacket);
//				
//				JTPacket recvpacket = (JTPacket)is.readObject();
//				System.out.println("STATUS: " + recvpacket.status);
//				System.out.println("RESULT: " + recvpacket.result);
//			}
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		
	}

	public ClientDriver(String hosts) {
		
		zkc = new ZkConnector();
        zkc.connect(hosts);
	}
	
	
	
}
