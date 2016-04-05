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
		if (args.length != 3) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. ClientDriver zkServer:clientPort job/status hash");
            return;
        }
    	
    	ClientDriver fs = new ClientDriver(args[0]);
    	fs.start(args[1], args[2]);
	}
	
	private void start(String jobOrStatus, String hash) {
		// TODO Auto-generated method stub
		String ipResult = null;
		String trackerPath = zkc.createPath(TRACKER);
		Stat s = zkc.exists(trackerPath, new Watcher() {
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
		
		String [] address = null;
		do {
			try {
				byte [] taskstatus = zkc.getZooKeeper().getData(trackerPath, false, null);
				
				if (taskstatus != null) {
		    		// task is done
		    		ipResult = (new String(taskstatus, "UTF-8"));
//		    		System.out.println("ipAddr: " + ipResult);
		    		
		    		address = ipResult.split(":");
		    		
//		    		System.out.println(address[0] + " -- " + address[1]);
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
		} while (address == null);
		
		try {
			socket = new Socket(address[0], Integer.parseInt(address[1]));
			
//			System.out.println("d1");		
			
			if (jobOrStatus.toLowerCase().equals(JOB) || jobOrStatus.toLowerCase().equals(STATUS)) {
				
				JTPacket sendpacket = new JTPacket();
				sendpacket.query = jobOrStatus.toLowerCase();
				sendpacket.hash = hash;

				ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
				os.writeObject(sendpacket);
				
				ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
				JTPacket recvpacket = (JTPacket)is.readObject();
				
				if (recvpacket.status == JTPacket.success) {
					System.out.println ("Password found: " + recvpacket.result);
				}
				else if (recvpacket.status == JTPacket.failure) {
					System.out.println ("Failed: Password not found");
				}
				else {
					System.out.println ("Status code: " + recvpacket.status + " " + JTPacket.statusText[recvpacket.status]);
				}
				
//				System.out.println("RESULT: " + recvpacket.result);
				
//				System.out.println("connected to server");
//				
//				FSPacket sendpacket = new FSPacket();
//				sendpacket.partitionID = 2;
//				
//				System.out.println("Sending Packet: " + sendpacket.partitionID);
//
//				ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
//				
//				System.out.println("d2");
//				
//				os.writeObject(sendpacket);
//				
//				ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
//				
//				System.out.println("d3");
//					
//				FSPacket recvpacket = (FSPacket)is.readObject();
//				
//				System.out.println("PARTITION: " + recvpacket.partitionID);
//				System.out.println("WORDS: " + recvpacket.list.size());
//				
			} else {
				System.out.println(jobOrStatus);
			}
			
//			System.out.println("d4");
			
			socket.close();
		} catch (NumberFormatException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// socket crashed
			// ignore
		}
	}

	public ClientDriver(String hosts) {
		
		zkc = new ZkConnector();
        zkc.connect(hosts);
	}
	
	
	
}
