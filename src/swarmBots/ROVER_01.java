package swarmBots;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import common.Communication;
import common.CommunicationHelper;
import common.Coord;
import common.MapTile;
import common.PlanetMap;
import common.Rover;
import common.ScanMap;
import enums.RoverConfiguration;
import enums.RoverDriveType;
import enums.Terrain;
import rover_logic.SearchLogic;

import enums.RoverToolType;
import controlServer.RoverCommandProcessor;
import controlServer.RoverStats;

/**
 * The seed that this program is built on is a chat program example found here:
 * http://cs.lmu.edu/~ray/notes/javanetexamples/ Many thanks to the authors for
 * publishing their code examples
 */

@SuppressWarnings("unused")
public class ROVER_01 extends Rover {
	



	public ROVER_01() {
		// constructor
		System.out.println("ROVER_01 rover object constructed");
		rovername = "ROVER_01";
	}
	
	public ROVER_01(String serverAddress) {
		// constructor
		System.out.println("ROVER_01 rover object constructed");
		rovername = "ROVER_01";
		SERVER_ADDRESS = serverAddress;
	}

	/**
	 * Connects to the server then enters the processing loop.
	 */
	private void run() throws IOException, InterruptedException {
		 String url = "http://localhost:3000/api";
	        String corp_secret = "gz5YhL70a2";

	        Communication com = new Communication(url, rovername, corp_secret);
		// Make connection to SwarmServer and initialize streams
		Socket socket = null;
		try {
			socket = new Socket(SERVER_ADDRESS, PORT_ADDRESS);

			receiveFrom_RCP = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			sendTo_RCP = new PrintWriter(socket.getOutputStream(), true);
			
			// Need to allow time for the connection to the server to be established
			sleepTime = 301;
			
			// Process all messages from server, wait until server requests Rover ID
			// name - Return Rover Name to complete connection
			while (true) {
				String line = receiveFrom_RCP.readLine();
				if (line.startsWith("SUBMITNAME")) {
					//This sets the name of this instance of a swarmBot for identifying the thread to the server
					sendTo_RCP.println(rovername); 
					break;
				}
			}
	
	
			
			/**
			 *  ### Setting up variables to be used in the Rover control loop ###
			 */
			int stepCount = 0;	
			String line = "";	
			boolean goingSouth = false;
			boolean stuck = false; // just means it did not change locations between requests,
									// could be velocity limit or obstruction etc.
			boolean blocked = false;
	
			String[] cardinals = new String[4];
			cardinals[0] = "N";
			cardinals[1] = "E";
			cardinals[2] = "S";
			cardinals[3] = "W";	
			String currentDir = cardinals[0];		
			
			/**
			 *  ### Retrieve static values from RCP ###
			 */		
			// **** get equipment listing ****			
			equipment = getEquipment();
			System.out.println(rovername + " equipment list results " + equipment + "\n");
			
			
			// **** Request START_LOC Location from SwarmServer **** this might be dropped as it should be (0, 0)
			StartLocation = getStartLocation();
			System.out.println(rovername + " START_LOC " + StartLocation);
			
			
			// **** Request TARGET_LOC Location from SwarmServer ****
			TargetLocation = getTargetLocation();
			System.out.println(rovername + " TARGET_LOC " + TargetLocation);
			
			
	

			/**
			 *  ####  Rover controller process loop  ####
			 */
			
			
			while (true) {                     //<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
				
				// **** Request Rover Location from RCP ****
				currentLoc = getCurrentLocation();//will also send the current moving 
				
				
//			        System.err.println("GOT: " + com.getRoverLocations());
			        
//				System.out.println(rovername + " currentLoc at start: " + currentLoc);
				
				// after getting location set previous equal current to be able to check for stuckness and blocked later
				previousLoc = currentLoc;		
				
				

				// ***** do a SCAN *****
				// gets the scanMap from the server based on the Rover current location
				scanMap = doScan(); 
				// prints the scanMap to the Console output for debug purposes
//				scanMap.debugPrintMap();
				
		
							
				// ***** get TIMER time remaining *****
				timeRemaining = getTimeRemaining();
				
				MapTile[][] scanMapTiles =scanMap.getScanMap();
//				  System.out.println("post message: " + 
				com.postScanMapTiles(currentLoc, scanMapTiles);
//				  );

				// another call for current location
				SearchLogic search = new SearchLogic();
				//get an item from the available sciences to be harvested
				Coord destination = new Coord(20, 20);
				
				 List<String> moves = search.Astar(currentLoc, destination, scanMapTiles, RoverDriveType.WHEELS, 
						 jsonToMap(com.getGlobalMap()));
//				 System.out.println("\n\n\n");
//		         System.out.println(rovername + "currentLoc: " + currentLoc + ", destination: " + destination);
//		         System.out.println(rovername + " moves: " + moves.toString());
//		         System.out.println("\n\n\n");
//		         
		 		String route = "";
		 		for(String s: moves){
		 			route += s + " ";
		 		}
		 		route.trim();
		 		

				// ***** MOVING *****

		 		int centerIndex = (scanMap.getEdgeSize() - 1)/2;
		 		//workaround until fix bug Astar has rover
		 		if(!stuck && stepCount==0){
		 			String nextMove = moves.get(0);
			 		switch (nextMove) {
				 		case "N": moveNorth(route); currentDir = "N"; break; 
				 		case "E": moveEast(route); currentDir = "E"; break;
				 		case "W": moveWest(route); currentDir = "W"; break;
				 		case "S": moveSouth(route); currentDir = "S"; break;
				 		default: currentDir = moveWander(route, currentDir, centerIndex, scanMapTiles); break;	
			 		}
		 		}else {
		 			currentDir = moveWander(route, currentDir, centerIndex, scanMapTiles);
		 			stepCount++;
		 			if(stepCount==5) stepCount=0;
		 		}
		 		/*
				// try moving east 5 block if blocked
				if (blocked) {
					scanMapTiles = scanMap.getScanMap();
					int centerIndex = (scanMap.getEdgeSize() - 1)/2;
				
					
					 if (!scanMapTiles[centerIndex+1][centerIndex].getHasRover() 
								&& scanMapTiles[centerIndex+1][centerIndex].getTerrain() == Terrain.SOIL) {
				
							moveEast(route);
							blocked = false;
							currentDir ="E";
						}
					 else if (!scanMapTiles[centerIndex][centerIndex-1].getHasRover() 
							&& scanMapTiles[centerIndex][centerIndex-1].getTerrain() == Terrain.SOIL) {
//						 System.out.println("Blocked, moving North");
						moveNorth(route);
						blocked = false;
						currentDir ="N";
						
					}
					
					else if (!scanMapTiles[centerIndex][centerIndex+1].getHasRover() 
							&& scanMapTiles[centerIndex][centerIndex+1].getTerrain() == Terrain.SOIL) {
			
//						System.out.println("Blocked, moving South");
						moveSouth(route);
						blocked = false;
						currentDir ="S";
						
					}
					
					else{
						
						moveWest(route);
						
						blocked =false;
						currentDir ="W";
					}
						
					
					
				} else {
					scanMapTiles = scanMap.getScanMap();
					int centerIndex = (scanMap.getEdgeSize() - 1)/2;
					
					if(currentDir =="N"){
						
					
						 if (!scanMapTiles[centerIndex][centerIndex-1].getHasRover() 
									&& scanMapTiles[centerIndex][centerIndex-1].getTerrain() == Terrain.SOIL) {
								moveNorth(route);
//								System.out.println("Not blocked, moving North");
							}
						 else
							 blocked = true;
					}
					else if(currentDir == "S"){
						 if (!scanMapTiles[centerIndex][centerIndex+1].getHasRover() 
								&& scanMapTiles[centerIndex][centerIndex+1].getTerrain() == Terrain.SOIL) {
//							 System.out.println("Not blocked, moving SOuth");
							moveSouth(route);
//							
						}
						 else
							 blocked = true;
					}
						
					else if(currentDir == "E"){
						 if (!scanMapTiles[centerIndex+1][centerIndex].getHasRover() 
								&& scanMapTiles[centerIndex+1][centerIndex].getTerrain() == Terrain.SOIL) {		
							moveEast(route);
						}
						 else 
							 blocked = true;
					}
						
					else{
						if (!scanMapTiles[centerIndex][centerIndex+1].getHasRover() 
								&& scanMapTiles[centerIndex][centerIndex+1].getTerrain() == Terrain.SOIL) {
							moveWest(route);
						}
					 else
						 blocked = true;
					}
				
				
					
						
					
					
				}
				
				*/
		 		
		 	// ***** GATHERING *****
		 		centerIndex = (scanMap.getEdgeSize() - 1)/2;
		 		if ((scanMapTiles[centerIndex][centerIndex].getScience().toString().equals("ORGANIC"))) {
		 			gather();
		 			
		 		}
	             //////////////////////////
				currentLoc = getCurrentLocation();

	
				// test for stuckness
				stuck = currentLoc.equals(previousLoc);	
				
				// this is the Rovers HeartBeat, it regulates how fast the Rover cycles through the control loop
				Thread.sleep(sleepTime);
				
//				System.out.println("ROVER_01 ------------ bottom process control --------------"); 
			}  // END of Rover control While(true) loop
		
		// This catch block closes the open socket connection to the server
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
	        if (socket != null) {
	            try {
	            	socket.close();
	            } catch (IOException e) {
	            	System.out.println("ROVER_01 problem closing socket");
	            }
	        }
	    }

	} // END of Rover run thread
	
	// ####################### Support Methods #############################
	
	private Map<Coord, MapTile> jsonToMap(JSONArray data) {
		Map<Coord, MapTile> globalMap = new HashMap<>();
    	MapTile tempTile;

    	
        for (Object o : data) {
            JSONObject jsonObj = (JSONObject) o;
            boolean marked = (jsonObj.get("g") != null) ? true : false;
            int x = (int) (long) jsonObj.get("x");
            int y = (int) (long) jsonObj.get("y");
            Coord coord = new Coord(x, y);

            MapTile tile = CommunicationHelper.convertToMapTile(jsonObj);
            globalMap.put(coord, tile); 
        }
        
        return globalMap;
	}
	
	//find a task/science to be harvested
//	private void getNextTask(ArrayList<String> equipment){
//		 String url = "http://localhost:3000/api";
//	        String corp_secret = "gz5YhL70a2";
//
//	        RoverConfiguration rConfig = RoverConfiguration.getEnum("ROVER_01"); 
//             RoverStats rover = new RoverStats(rConfig);
//              
//	        Communication com = new Communication(url, rovername, corp_secret);
//		for (Object o : data) {
//
//            JSONObject jsonObj = (JSONObject) o;
//            String status = (String)jsonObj.get("harvestStatus");
//            
//            if(status.equals("OPEN")){
//            	
//            	
////            	int x = (int) (long) jsonObj.get("x");
////                int y = (int) (long) jsonObj.get("y");
////                Coord coord = new Coord(x, y);
//            }
//            
//		}
//
//	}

	/**
	 * Runs the client
	 */
	public static void main(String[] args) throws Exception {
		ROVER_01 client;
    	// if a command line argument is present it is used
		// as the IP address for connection to SwarmServer instead of localhost 
		
		if(!(args.length == 0)){
			client = new ROVER_01(args[0]);
		} else {
			client = new ROVER_01();
		}
		
		client.run();
	}
	
	public MapTile getNextTile(String direction, int centerIndex, MapTile[][] scanMapTiles){
		MapTile nextTile = new MapTile();
		switch (direction) {
			case "N": nextTile = scanMapTiles[centerIndex][centerIndex - 1]; break; 
			case "E": nextTile = scanMapTiles[centerIndex + 1][centerIndex]; break;
			case "W": nextTile = scanMapTiles[centerIndex - 1][centerIndex]; break;
			case "S": nextTile = scanMapTiles[centerIndex][centerIndex + 1]; break;
			default: break;	
		}
		
		return nextTile;	
	}
	
	public boolean isBlock(MapTile nextTile){
		return nextTile.getHasRover() || !(nextTile.getTerrain() == Terrain.SOIL || nextTile.getTerrain() == Terrain.GRAVEL);
	}
	
	public String moveWander(String route, String currentDir, int centerIndex, MapTile[][] scanMapTiles ){
		//moving same direction if not block
		if (!isBlock(getNextTile(currentDir, centerIndex, scanMapTiles))) {
	 		switch (currentDir) {
		 		case "N": moveNorth(route); break; 
		 		case "E": moveEast(route); break;
		 		case "W": moveWest(route); break;
		 		case "S": moveSouth(route); break;
		 		default: break;	
			}
		} else {
			System.out.println("block");
			if (!isBlock(getNextTile("E", centerIndex, scanMapTiles))) {
				moveEast(route);
				currentDir = "E";
			}else if (!isBlock(getNextTile("N", centerIndex, scanMapTiles))) {
				moveNorth(route);
				currentDir = "N";
			}else if (!isBlock(getNextTile("S", centerIndex, scanMapTiles))) {
				moveSouth(route);
				currentDir = "S";
			}else {
				moveWest(route);
				currentDir = "W";
			}
		}
		return currentDir;
	}
}

