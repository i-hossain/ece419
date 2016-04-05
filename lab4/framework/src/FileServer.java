import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
	
	public static final String FILESERVER = "fileserver";
	public ZkConnector zkc;
	
	private ServerSocket myServerSock;
	
	private Watcher watcher;
	
	private String fileServerData = "";

	// num words 265,744
	
	// 265,744 / 34 = 7816
	
	static final int partition_size = 7816;
	private static int partitionID = 1;	
	
	private static List<String> list = new ArrayList<String>();
	private static Map<Integer, List<String>> dictionary = new HashMap<Integer, List<String>>();
	
	String file = "";
	
	public static void main(String[] args) throws IOException
	{
		if (args.length != 2) {
            System.out.println("Usage: java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. FileServer zkServer:clientPort");
            return;
        }
    	
    	FileServer fs = new FileServer(args[0], args[1]);
    	fs.start();
	}
	
	private void start() throws IOException {
		
		String trackerPath = zkc.createPath(FILESERVER);
    	if (zkc.joinzDaGroupz(trackerPath, fileServerData, watcher))
    		dozDaShiz();
    	
//    	System.out.println("Sleeping...");
//        while (true) {
//            try{ Thread.sleep(5000); } catch (Exception e) {}
//        }
	}
	
	private void dozDaShiz() {
		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String line;
		String word = null;
		try {
			while ((line = br.readLine()) != null)
			{
				word = line;
				//hash = getHash(word);
				list.add(word);
				if (list.size() == partition_size) {
					dictionary.put(partitionID, list); //Insert the partition into the HashMap
					partitionID++; //Increment Partition ID
					list = new ArrayList<String>(); // Work with the new list
				}
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// add the last list to the dictionary and break
		if (!list.isEmpty()) {
			dictionary.put(partitionID, list);
		}

		//Close the input stream
		try {
			br.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		System.out.println("Initialized dictionary");
		
//		checkDictionary();
		
//		Thread t = new Thread() {
////			ServerSocket severSock = new ServerSocket(0);
//        	
//        	public void run() {
		Socket socket = null;
		FSPacket fspacket = null;
		ObjectInputStream is;
		ObjectOutputStream os;
        		
		while (true) {
			try {
				socket = myServerSock.accept();
				is = new ObjectInputStream(socket.getInputStream());
				fspacket = (FSPacket)is.readObject();
				
//				System.out.println("PARTITION: " + fspacket.partitionID);
				
				fspacket.list = dictionary.get(fspacket.partitionID);
				
				os = new ObjectOutputStream(socket.getOutputStream());
				os.writeObject(fspacket);
			
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
//        	}
//        };
//        
//        t.start();
	}
	
	public FileServer(String hosts, String file) {
        zkc = new ZkConnector();
        zkc.connect(hosts);
        
        this.file = file;
        
        try {
			myServerSock = new ServerSocket(0);
			fileServerData = InetAddress.getLocalHost().toString().split("/")[0] + ":" + myServerSock.getLocalPort();
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
                        if (zkc.joinzDaGroupz(myPath, fileServerData, watcher))
                    		dozDaShiz();
                    }
                    if (type == EventType.NodeCreated) {
                        System.out.println(myPath + " created!");
                        try{ Thread.sleep(5000); } catch (Exception e) {}
                        zkc.joinzDaGroupz(myPath, fileServerData, watcher);
//                        if (zkc.joinzDaGroupz(myPath, fileServerData, watcher))
//                    		dozDaShiz();
                    }
                }
        
            } };
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
