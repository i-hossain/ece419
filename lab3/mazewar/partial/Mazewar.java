/*
Copyright (C) 2004 Geoffrey Alan Washburn
   
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
   
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
   
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
*/
  
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;

/**
 * The entry point and glue code for the game.  It also contains some helpful
 * global utility methods.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class Mazewar extends JFrame {
	
	private final String TAG = this.getClass().getSimpleName();
		public static final int MIN_OTHER_CLIENTS = 1;

        /**
         * The default width of the {@link Maze}.
         */
        private final int mazeWidth = 20;

        /**
         * The default height of the {@link Maze}.
         */
        private final int mazeHeight = 10;

        /**
         * The default random seed for the {@link Maze}.
         * All implementations of the same protocol must use 
         * the same seed value, or your mazes will be different.
         */
        private final int mazeSeed = 42;

        /**
         * The {@link Maze} that the game uses.
         */
        private Maze maze = null;

        /**
         * The Mazewar instance itself. 
         */
        private Mazewar mazewar = null;
        private MSocket mSocket = null;
        private ObjectOutputStream out = null;
        private ObjectInputStream in = null;

        /**
         * The {@link GUIClient} for the game.
         */
        private GUIClient guiClient = null;
        
        
        /**
         * A map of {@link Client} clients to client name.
         */
        private Hashtable<String, Client> clientTable = null;

        /**
         * A queue of events.
         */
//        private BlockingQueue eventQueue = null;
        private BuffQueue bqs = null;
        
        /**
         * The panel that displays the {@link Maze}.
         */
        private OverheadMazePanel overheadPanel = null;

        /**
         * The table the displays the scores.
         */
        private JTable scoreTable = null;
        
        /** 
         * Create the textpane statically so that we can 
         * write to it globally using
         * the static consolePrint methods  
         */
        private static final JTextPane console = new JTextPane();
        
        private Map<Client, Projectile> myProjMap = new ConcurrentHashMap<Client, Projectile>();
        
        private VectorClock myClock = new VectorClock();
      
        /** 
         * Write a message to the console followed by a newline.
         * @param msg The {@link String} to print.
         */ 
        public static synchronized void consolePrintLn(String msg) {
                console.setText(console.getText()+msg+"\n");
        }
        
        /** 
         * Write a message to the console.
         * @param msg The {@link String} to print.
         */ 
        public static synchronized void consolePrint(String msg) {
                console.setText(console.getText()+msg);
        }
        
        /** 
         * Clear the console. 
         */
        public static synchronized void clearConsole() {
           console.setText("");
        }
        
        /**
         * Static method for performing cleanup before exiting the game.
         */
        public static void quit() {
                // Put any network clean-up code you might have here.
                // (inform other implementations on the network that you have 
                //  left, etc.)
                

                System.exit(0);
        }
       
        /** 
         * The place where all the pieces are put together. 
         */
        public Mazewar(String serverHost, int serverPort, int clientPort) throws IOException,
                                                ClassNotFoundException {
                super("ECE419 Mazewar");
                consolePrintLn("ECE419 Mazewar started!");
                
                // Create the maze
                maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);
                assert(maze != null);
                
                // Have the ScoreTableModel listen to the maze to find
                // out how to adjust scores.
                ScoreTableModel scoreModel = new ScoreTableModel();
                assert(scoreModel != null);
                maze.addMazeListener(scoreModel);
                
                // Throw up a dialog to get the GUIClient name.
                String name = JOptionPane.showInputDialog("Enter your name");
                if((name == null) || (name.length() == 0)) {
                  Mazewar.quit();
                }
                
                // mSocket = new MSocket(serverHost, serverPort);
                // //Send hello packet to server
                // MPacket hello = new MPacket(name, MPacket.HELLO, MPacket.HELLO_INIT);
                // hello.mazeWidth = mazeWidth;
                // hello.mazeHeight = mazeHeight;
                
                // if(Debug.debug) System.out.println("Sending hello");
                // mSocket.writeObject(hello);
                // if(Debug.debug) System.out.println("hello sent");
                // //Receive response from server
                // MPacket resp = (MPacket)mSocket.readObject();
                // if(Debug.debug) System.out.println("Received response from server");

                
                
                //Establish serversock and wait for connections. This is done before sending a hello packet to ensure that the clients are listening on open ports
                //or else it could run into race condition  if a client tries to establish a connection to a host that hasn't opened a socket yet.
                
                final int cPort = clientPort;
                bqs = new BuffQueue();
                
                Thread t = new Thread() {
	            	MServerSocket clientSock = new MServerSocket(cPort);
	            	
                	public void run() {
                		MSocket mSocket = null;
                		MPacket rPack = null;
                		
                		while (true) {
							try {
								mSocket = clientSock.accept();
								rPack = (MPacket)mSocket.readObject();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (ClassNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
	                		
	                		bqs.socketMap.put(rPack.pid, mSocket);
	                		bqs.recvSeqNo.put(rPack.pid, new AtomicInteger(1));
	                		bqs.sendSeqNo.put(rPack.pid, new AtomicInteger(1));
	                		
	                		//Start a new listener thread 
//	                        new Thread(new ClientListenerThread(rPack.pid, myClock, bqs)).start();
                		}
                	}
                };
                
                t.start();
                
                Socket socket = new Socket(serverHost, serverPort);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                MPacket hello = new MPacket(name, MPacket.HELLO, MPacket.HELLO_INIT);
                hello.mazeWidth = mazeWidth;
                hello.mazeHeight = mazeHeight;
                hello.port = clientPort;
                
                if(Debug.debug) System.out.println("Sending hello");
	            out.writeObject(hello);
	            if(Debug.debug) System.out.println("hello sent");
	            //Receive response from server
	            MPacket resp = (MPacket)in.readObject();
	            if(Debug.debug) System.out.println("Received response from server");

                
//////////////////////////////////////////////////////////////////////////////////////////////////////////////

                bqs.cData = resp.clientData;
                
                Debug.log(TAG, Arrays.toString(resp.clientData));
                
                for (int i = 0; i < resp.clientData.length; i++) {
                	if (resp.clientData[i].name.equals(name)) {
                		bqs.myPid = resp.clientData[i].pid;
                		break;
                	}
                }
                
                ClientData ccd;
                for (int i = bqs.myPid - 1; i >= 0 ; i--) {   
                	ccd = resp.clientData[i];
                	MSocket cSocket = new MSocket(ccd.host, ccd.port);
                	cSocket.writeObject(new MPacket(MPacket.HELLO, MPacket.HELLO_WORLD, bqs.myPid));
                	Debug.log(TAG, "Initializing " + ccd.pid);
                	
                	// init bqs data structures
                	bqs.socketMap.put(ccd.pid, cSocket);
                	bqs.recvSeqNo.put(ccd.pid, new AtomicInteger(1));
        			bqs.sendSeqNo.put(ccd.pid, new AtomicInteger(1));
        			
        			//Start a new listener thread 
//                    new Thread(new ClientListenerThread(ccd.pid, myClock, bqs)).start(); 
                }
                
                while (bqs.socketMap.size() < MIN_OTHER_CLIENTS);
                
//                for(Integer pid : bqs.socketMap.keySet()) {
//                	Debug.log(TAG, "init: " + pid);
//                	bqs.recvSeqNo.put(pid, new AtomicInteger(1));
//        			bqs.sendSeqNo.put(pid, new AtomicInteger(1));
//                }
                
                bqs.recvSeqNo.put(bqs.myPid, new AtomicInteger(1));
    			bqs.sendSeqNo.put(bqs.myPid, new AtomicInteger(1));
                
                //Initialize queue of events
//                eventQueue = new LinkedBlockingQueue<MPacket>();
                maze.addEventQueue(bqs.eventQueue);
                
                maze.addProjMap(myProjMap);
                
                //Initialize hash table of clients to client name 
                clientTable = new Hashtable<String, Client>(); 
                
                // Create the GUIClient and connect it to the KeyListener queue
                //RemoteClient remoteClient = null;
                for(Player player: resp.players){  
                        if(player.name.equals(name)){
                        	if(Debug.debug)System.out.println("Adding guiClient: " + player);
                                guiClient = new GUIClient(name, bqs.eventQueue, myProjMap);
                                maze.addClientAt(guiClient, player.point, player.direction);
                                this.addKeyListener(guiClient);
                                clientTable.put(player.name, guiClient);
                                //TODO: 
                                maze.addMyClient(player.name, guiClient);
                        }else{
                        	if(Debug.debug)System.out.println("Adding remoteClient: " + player);
                                RemoteClient remoteClient = new RemoteClient(player.name);
                                maze.addClientAt(remoteClient, player.point, player.direction);
                                clientTable.put(player.name, remoteClient);
                        }
                }
                
                // Use braces to force constructors not to be called at the beginning of the
                // constructor.
                /*
                {
                        maze.addClient(new RobotClient("Norby"));
                        maze.addClient(new RobotClient("Robbie"));
                        maze.addClient(new RobotClient("Clango"));
                        maze.addClient(new RobotClient("Marvin"));
                }
                */

                
                // Create the panel that will display the maze.
                overheadPanel = new OverheadMazePanel(maze, guiClient);
                assert(overheadPanel != null);
                maze.addMazeListener(overheadPanel);
                
                // Don't allow editing the console from the GUI
                console.setEditable(false);
                console.setFocusable(false);
                console.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
               
                // Allow the console to scroll by putting it in a scrollpane
                JScrollPane consoleScrollPane = new JScrollPane(console);
                assert(consoleScrollPane != null);
                consoleScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Console"));
                
                // Create the score table
                scoreTable = new JTable(scoreModel);
                assert(scoreTable != null);
                scoreTable.setFocusable(false);
                scoreTable.setRowSelectionAllowed(false);

                // Allow the score table to scroll too.
                JScrollPane scoreScrollPane = new JScrollPane(scoreTable);
                assert(scoreScrollPane != null);
                scoreScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Scores"));
                
                // Create the layout manager
                GridBagLayout layout = new GridBagLayout();
                GridBagConstraints c = new GridBagConstraints();
                getContentPane().setLayout(layout);
                
                // Define the constraints on the components.
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1.0;
                c.weighty = 3.0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                layout.setConstraints(overheadPanel, c);
                c.gridwidth = GridBagConstraints.RELATIVE;
                c.weightx = 2.0;
                c.weighty = 1.0;
                layout.setConstraints(consoleScrollPane, c);
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1.0;
                layout.setConstraints(scoreScrollPane, c);
                                
                // Add the components
                getContentPane().add(overheadPanel);
                getContentPane().add(consoleScrollPane);
                getContentPane().add(scoreScrollPane);
                
                // Pack everything neatly.
                pack();

                // Let the magic begin.
                setVisible(true);
                overheadPanel.repaint();
                this.requestFocusInWindow();
        }

        /*
        *Starts the ClientSenderThread, which is 
         responsible for sending events
         and the ClientListenerThread which is responsible for 
         listening for events
        */
        private void startThreads(){
        	for(ClientData c : bqs.cData) {  
        		if(c.pid == bqs.myPid) {
        			// its our local client
        			//Start a new sender thread 
                    new Thread(new ClientSenderThread(bqs.myPid, myClock, bqs)).start();
        		}
        		//Start a new listener thread 
                new Thread(new ClientListenerThread(c.pid, myClock, bqs)).start(); 
	        } 
        	
        	new Thread(new ClientExecutorThread(clientTable, bqs)).start(); 
        }

        
        /**
         * Entry point for the game.  
         * @param args Command-line arguments.
         */
        public static void main(String args[]) throws IOException,
                                        ClassNotFoundException{

             String host = args[0];
             int port = Integer.parseInt(args[1]);
             int clientport = Integer.parseInt(args[2]);
             /* Create the GUI */
             Mazewar mazewar = new Mazewar(host, port, clientport);
             mazewar.startThreads();
        }
}
