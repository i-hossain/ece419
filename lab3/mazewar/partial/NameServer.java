import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Random;

public class NameServer {
	private final String TAG = this.getClass().getSimpleName();;
	
    
	//The maximum of clients that will join
	//Server waits until the max number of clients to join 
    public static final int MIN_CLIENTS = 2;
//    private MServerSocket mServerSocket = null;
    private int clientCount; //The number of clients before game starts
    private static int PORT = 5555; //Default port
     private ClientSocket[] socketList = null; //A list of MSockets
    // private BlockingQueue eventQueue = null; //A list of events


	private ServerSocket serverSock;
    
    /*
    * Constructor
    */
    public NameServer(int port) throws IOException {
    	serverSock = new ServerSocket(PORT);
        clientCount = 0; 
        if(Debug.debug) System.out.println("Listening on port: " + port);
        socketList = new ClientSocket[MIN_CLIENTS];
    }
    
    public void handleHello(){
        
        //The number of players
        int playerCount = socketList.length;
        Random randomGen = null;
        Player[] players = new Player[playerCount];
        if(Debug.debug) System.out.println("In handleHello");
        MPacket hello = null;
        
        ClientData[] cData = new ClientData[playerCount];
        
        try{        
            for(int i=0; i<playerCount; i++) {
                hello = (MPacket)socketList[i].readObject();
                //Sanity check 
                if(hello.type != MPacket.HELLO){
                    throw new InvalidObjectException("Expecting HELLO Packet");
                }
                if(randomGen == null){
                   randomGen = new Random(hello.mazeSeed); 
                }
                //Get a random location for player
                Point point =
                    new Point(randomGen.nextInt(hello.mazeWidth),
                          randomGen.nextInt(hello.mazeHeight));
                
                //Start them all facing North
                Player player = new Player(hello.name, point, Player.North);
                players[i] = player;
                
                cData[i] = new ClientData(hello.name, i, socketList[i].getIP().getHostName(), hello.port);
            }
            
            hello.event = MPacket.HELLO_RESP;
            hello.players = players;
            hello.clientData = cData;
            
            //Now broadcast the HELLO
            Debug.log(TAG, "Sending " + hello);
            
            Debug.log(TAG, "Sending " + Arrays.toString(cData));
            
            for(ClientSocket cSock : socketList) {
            	cSock.writeObject(hello);
            	//Debug.log(TAG, )
            }
        } catch(IOException e){
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
    
    /*
    *Starts the listener and sender threads 
    */
    // public void startThreads() throws IOException{
    //     //Listen for new clients
    //     while(clientCount < MAX_CLIENTS){
    //         //Start a new listener thread for each new client connection
    //         MSocket mSocket = mServerSocket.accept();
            
    //         new Thread(new ServerListenerThread(mSocket, eventQueue)).start();
            
    //         mSocketList[clientCount] = mSocket;                            
            
    //         clientCount++;
    //     }
        
    //     //Start a new sender thread 
    //     new Thread(new ServerSenderThread(mSocketList, eventQueue)).start();    
    // }

        
    /*
    * Entry point for server
    */
    public static void main(String args[]) throws IOException {
        if(Debug.debug) System.out.println("Starting the server");
        // int port = Integer.parseInt(args[0]);
        NameServer server = new NameServer(PORT);
        
        server.start();    

    }

	private void start() {
		// TODO Auto-generated method stub
		try {
	         System.out.println("Server Waiting");

	         while (clientCount < MIN_CLIENTS) //
	         { 
	        	 socketList[clientCount] = new ClientSocket(serverSock.accept());
	        	 clientCount++;
	         }
	      }  
	      catch(Exception e) {
	    	  System.out.println(e);
	      }
		handleHello();
	}
}
