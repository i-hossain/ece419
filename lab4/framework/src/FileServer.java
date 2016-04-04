import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;



public class FileServer {
	
	public static final String FILESERVER = "fileServer";
	public ZkConnector zkc;
	
	private ServerSocket myServerSock;
	
	private Watcher watcher;
	
	private String fileServerData = "";

	static final int partition_size = 100;
	private static int partitionID = 1;	
	
	private static List<String> list = new ArrayList<String>();
	private static Map<Integer, List> dictionary = new HashMap<Integer, List>();
	
	
	
	public static void main(String[] args) throws IOException
	{
		if (args.length != 2) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. FileServer zkServer:clientPort");
            return;
        }
    	
    	FileServer fs = new FileServer(args[0]);
    	fs.start(args[1]);
	}
	
	private void start(String file) throws IOException {
		
		String trackerPath = zkc.createPath(FILESERVER);
    	zkc.joinzDaGroupz(trackerPath, fileServerData, watcher);
		
		FileInputStream fstream = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String line;
		int reset_count = 0;
		
		String word = null;
		String hash = null;

		while ((line = br.readLine()) != null)
		{
			word = line;
			//hash = getHash(word);
			list.add(word);
			reset_count++;
			if (reset_count == partition_size - 1) {
				reset_count = 0; //Reset counter for next partition
				dictionary.put(partitionID, list); //Insert the partition into the HashMap
				partitionID++; //Increment Partition ID
				list = new ArrayList<String>(); // Work with the new list
			}
		}
		
		// add the last list to the dictionary and break
		if (!list.isEmpty()) {
			dictionary.put(partitionID, list);
		}

		//Close the input stream
		br.close();
		
		//checkDictionary();
		
		Thread t = new Thread() {
//			ServerSocket severSock = new ServerSocket(0);
        	
        	public void run() {
        		Socket socket = null;
        		FSPacket fspacket = null;
        		
        		while (true) {
					try {
						socket = myServerSock.accept();
						ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
						fspacket = (FSPacket)is.readObject();
						
						int partition = fspacket.getID();
						FSPacket newFSpacket = new FSPacket(partition, dictionary.get(partition));
						ObjectOutputStream os;
						
						os = new ObjectOutputStream(socket.getOutputStream());
						os.writeObject(newFSpacket);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
        	}
        };
        
        t.start();
	}
	
	public FileServer(String hosts) {
        zkc = new ZkConnector();
        zkc.connect(hosts);
        
        try {
			myServerSock = new ServerSocket(0);
			fileServerData = InetAddress.getLocalHost().toString().split("/")[1] + ":" + myServerSock.getLocalPort();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        System.out.println("File Server Data: " + fileServerData);
        
        watcher = new Watcher() { // Anonymous Watcher
            @Override
            public void process(WatchedEvent event) {
            	String path = event.getPath();
                EventType type = event.getType();
                String myPath = zkc.createPath(FILESERVER);
                
                if(path.equalsIgnoreCase(myPath)) {
                    if (type == EventType.NodeDeleted) {
                        System.out.println(myPath + " deleted! Let's go!");       
                        zkc.joinzDaGroupz(myPath, fileServerData, watcher);
                    }
                    if (type == EventType.NodeCreated) {
                        System.out.println(myPath + " created!");
                        try{ Thread.sleep(5000); } catch (Exception e) {}
                        zkc.joinzDaGroupz(myPath, fileServerData, watcher);
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
		    } catch (Exception nsae) {
		    // ignore
		    }
		    return hash;
	}
	
	public static void checkDictionary () {
		
		int count = 0;
		
		for (int i = 0; i < dictionary.size(); i++){
			for (int j = 0; j < dictionary.get(i+1).size(); j++) {
				System.out.printf("Partition ID: %d, Word: %s\n", i+1, dictionary.get(i+1).get(j));
				count++;
			}
		}
		
		System.out.println("Dictionary Size: " + dictionary.size());
		System.out.println("Number of words: " + count);
		
	}

}
