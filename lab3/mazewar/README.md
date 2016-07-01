----------------------------------------------
Team
----------------------------------------------
# Rushab Ramesh Kumar, rushab.kumar@mail.utoronto.ca
# Ismail Hossain, ridoy.hosssain@mail.utoronto.ca

-----------------------------------------
How to run the code?
-----------------------------------------

1. Make in the parent folder
2. Make in the partial directory if skipped Step 1. (make in parent should recursively call make in partial.)

3. ./server.sh <port> (or) java Server <port>
4. ./run.sh <host> <port> (or) java Mazewar <port>

NOTE: The game needs MAX_CLIENTS (set to 4 in this lab) to start playing.

-----------------------------------------------
General Code Desc
-----------------------------------------------

To solve the problem with ordering of packets and to maintain a consistent view of the game to all players, we
created blocking queues to store received packets. The ACTIONs in the packets are executed using a global sequence
number. (See ClientListenerThread.java)

The minor inconsistency we found out was the fact that the death of players were not synchronized. Which means that
the order of ACTIONs before and after death could be inconsistent among screens if the players are moving fast.
Example: 

Client 1
- move Left
- move Left
--- Client 1 dies on GUI
- move Left
- move Left

Client 2
- move Left C1
--- Client 1 dies on GUI
- move Left C1
- move Left C1
- move Left C1

This is because the packets might be delayed over MSocket.

To solve this: the team decided to synchronize the movement of bullets at every 200 ms. 
(See MazeImpl.java)

The team found that it could optimize the code further to prevent flooding of the network by FIRE packets 
when SPACE key is held down.
Fixing the minor inconsistency, the team found that the scoreboard seems to be more consistent too :)