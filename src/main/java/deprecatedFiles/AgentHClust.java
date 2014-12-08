package deprecatedFiles;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import deprecatedFiles.HClust.Cluster;
import deprecatedFiles.HClust.Cluster.NoSuchMethodExcetion;
import environment.GameState;
import environment.Item;
import Util.Position;
import Util.PositionTuple;
import Util.Util;
import aStar.AStar;
import aStar.BoardState;
import aStar.IAStarState;

public class AgentHClust {

	private File logFile;

	private GameState gs = null;
	private Map<String, Object> nextCommand;

	private HashMap<PositionTuple,IAStarState> alreadyComputedAStar = new HashMap<PositionTuple,IAStarState>();

	public Map<String, Object> move(final JSONObject gameState) throws NoSuchMethodExcetion {

		gs = new GameState(gameState, gs);

		// Next command object
		nextCommand = new HashMap<String, Object>();

		// Try to use banana if possible
		if(useBanana()){
			nextCommand.put("command", "use");
			nextCommand.put("item", "banana");
			return nextCommand;
		}

		long t1 = System.currentTimeMillis();
		double[][] distances = computeDistances();
		HClust hc = new HClust(distances);

		Cluster c = hc.earlyStopCluster("average", gs.inventorySize - gs.inventory.size());
		System.out.println("Time: " + (System.currentTimeMillis() - t1));
		StringBuilder cString = new StringBuilder();
		for(int i : c.nodes) {
			Item item = gs.items.get(i);
			cString.append(item.itemName + " at pos " + item.p + " ");
		}
		System.out.println(cString);

		int[] perm = getBestPermutation(c.nodes, gs.inventorySize - gs.inventory.size());

		System.out.println("Best order:");
		cString = new StringBuilder();
		for(int i : perm) {
			Item item = gs.items.get(c.nodes.get(i));
			cString.append(item.itemName + " at pos " + item.p + " ");
		}
		System.out.println(cString);

		//		Cluster[] clusterOrder = hc.cluster("average");
		//		System.out.println("Time: " + (System.currentTimeMillis() - t1));
		//		
		//		saveDendrogramToFile(hc.getDendrogram());
		//		saveDistances(distances);
		//		
		//		for(Cluster c : clusterOrder) {
		//			StringBuilder cString = new StringBuilder();
		//			Collections.sort(c.nodes);
		//			for(int i : c.nodes) {
		//				Item item = gs.items.get(i);
		//				cString.append(item.itemName + " at pos " + item.p + " ");
		//			}
		//			System.out.println(cString);
		//		}

		// Construct next command
		nextCommand.put("command", "move");

		Item nextItem = gs.items.get(c.nodes.get(perm[0]));
		List<String> nextActions = getActionsToItem(nextItem);
		if(gs.containsBuff("speedy") && nextActions.size() >= 2) {
			JSONArray actions = new JSONArray();
			actions.put(nextActions.get(0));
			actions.put(nextActions.get(1));
			nextCommand.put("directions", actions);
		} else {
			String action = nextActions.get(0);
			nextCommand.put("direction", action);
		}

		log();
		return nextCommand;
	}

	private List<String> getActionsToItem(Item item) {
		PositionTuple pt = new PositionTuple(gs.agentPos, item.p);
		IAStarState gs = alreadyComputedAStar.get(pt);
		return gs.getActionsToGetHere();
	}

	private int[] getBestPermutation(ArrayList<Integer> nodes, int k) {
		List<int[]> perms = Util.nPermuteK(nodes.size(), k);
		double minDist = Double.POSITIVE_INFINITY;
		int[] minPerm = perms.get(0);
		for(int[] perm : perms) {
			double permDist = 0;
			double nPoints = 0;
			for(int i = 0; i < perm.length; i++){
				Position p1;
				Position p2;
				if(i == 0) {
					p1 = gs.agentPos;
				} else {
					p1 = gs.items.get(nodes.get(perm[i-1])).p;
				}
				p2 = gs.items.get(nodes.get(perm[i])).p;
				nPoints += gs.numberOfPointsOfPosition(p2);
				permDist += distanceBetweenPositions(p1, p2);
			}
			if(permDist/nPoints < minDist){
				minDist = permDist/nPoints; 
				minPerm = perm;
			}
		}
		return minPerm;
	}

	/**
	 * Shortest distance as defined by distance function. U define urself what the distance function is here.
	 * @return all pairwise distances according to ur distance function.
	 */
	private double[][] computeDistances() {
		double[][] d = new double[gs.items.size()][gs.items.size()];
		double[] ad = new double[gs.items.size()];
		for(int i1 = 0; i1 < gs.items.size()-1; i1++){
			double dist = distanceBetweenPositions(gs.agentPos,gs.items.get(i1).p);
			ad[i1] = dist;
			for(int i2 = i1+1; i2 < gs.items.size(); i2++){
				dist = distanceBetweenPositions(gs.items.get(i1).p,gs.items.get(i2).p);
				double nPoints = gs.numberOfPointsOfPosition(gs.items.get(i1).p) + 
						gs.numberOfPointsOfPosition(gs.items.get(i2).p);
				dist = Math.min((ad[i1] + dist)/nPoints,
						(ad[i2] + dist)/nPoints);
				d[i1][i2] = dist;
				d[i2][i1] = dist;
			}
		}
		return d;
	}

	private boolean useBanana() {
		for(int i = 0; i < gs.inventory.size(); i++) {
			if(gs.inventory.get(i).equals("banana")) {
				return true;
			}
		}
		return false;
	}

	private double distanceBetweenPositions(Position p1, Position p2){
		IAStarState goalState = null;
		PositionTuple startGoalTuple = new PositionTuple(p1,p2);

		// If distance is not precomputed, then compute 
		if(!alreadyComputedAStar.containsKey(startGoalTuple)) {
			IAStarState startingState = new BoardState(gs,p1,p2,new LinkedList<String>(),
					new HashSet<Position>(), new HashMap<PositionTuple, Double>());
			AStar aStar = new AStar(startingState);
			aStar.run();
			goalState = aStar.getCurrentState();
			if(goalState.hasReachedGoal()) {
				IAStarState reversedGoalState= new BoardState(gs, p2, p1,
						Util.reversePath(goalState.getActionsToGetHere()),
						BoardState.getVisitedStates(), BoardState.getHeuristicMap());
				PositionTuple reversedStartGoalTuple = new PositionTuple(p2, p1);
				alreadyComputedAStar.put(reversedStartGoalTuple, reversedGoalState);
				alreadyComputedAStar.put(startGoalTuple, goalState);
			}
		} else {
			goalState = alreadyComputedAStar.get(startGoalTuple);
		}

		// If the goal is reachable set distance to path length, else infinity
		double distance = Double.POSITIVE_INFINITY;
		if(goalState.hasReachedGoal()) {
			distance = goalState.getActionsToGetHere().size();
		}
		return distance;
	}

	private void saveDendrogramToFile(double[][] dendrogram) {
		FileWriter fw;
		BufferedWriter bw;
		File dg;
		try {
			logFile = new File("dendrogram.txt");
			if(logFile.exists()){
				logFile.delete();
			}
			logFile.createNewFile();
			fw = new FileWriter(logFile, false);
			bw = new BufferedWriter(fw);
			for(int i = 0; i < dendrogram.length; i++) {
				bw.write(dendrogram[i][0] + " " + dendrogram[i][1] +  " " + dendrogram[i][2] + "\n");
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveDistances(double[][] d) {
		FileWriter fw;
		BufferedWriter bw;
		File dg;
		try {
			logFile = new File("distances.txt");
			if(logFile.exists()){
				logFile.delete();
			}
			logFile.createNewFile();
			fw = new FileWriter(logFile, false);
			bw = new BufferedWriter(fw);
			for(int i = 0; i < d.length; i++) {
				for(int j = 0; j < d.length; j++) {
					bw.write(d[i][j] + " ");
				}
				bw.write("\n");
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void log(){
		// Logging starts here {
		FileWriter fw;
		BufferedWriter bw;
		try {
			if(logFile == null) {
				logFile = new File("log.txt");
				if(logFile.exists()){
					logFile.delete();
				}
				logFile.createNewFile();
				fw = new FileWriter(logFile, true);
				bw = new BufferedWriter(fw);
				bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
				bw.write("%%%%%%%%%%%%%%%%% NEW GAME %%%%%%%%%%%%%%%%%\n");
				bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n\n\n");
			} else {
				logFile = new File("log.txt");
				fw = new FileWriter(logFile, true);
				bw = new BufferedWriter(fw);
			}
			bw.write("Command sent = " + nextCommand +"\n");
			bw.write("\n\n\n");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// } Logging ends here
	}
}
