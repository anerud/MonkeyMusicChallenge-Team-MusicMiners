package agent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import environment.Buff;
import environment.GameState;
import environment.Item;
import Util.ArrayIndexComparator;
import Util.Position;
import Util.PositionTuple;
import Util.Util;
import aStar.AStar;
import aStar.BoardState;
import aStar.IAStarState;

public class AgentNeighbourhoodHeuristic {

	private boolean useNewHeuristic = true;
	private boolean newHeuristicIsGreedy = true;
	private boolean largeWorld = false;
	private int nExtraNodesInNH = 0;
	private int maxNHSize = 5;
	private int maxPermSize = 5;
	private int nTurnsInRowTooLong = 0;
	private double hNewConst = 1.3;
	private String heuristicOP = "min"; //"min" and "average" supported
	private double[][] distances = null;
	private AStar aStar = new AStar();

	private File logFile;
	private GameState gs = null;
	private Map<String, Object> nextCommand;
	private Stage1Plan s1p = null;

	private HashMap<PositionTuple,IAStarState> alreadyComputedAStar = new HashMap<PositionTuple,IAStarState>();
	private HashMap<PositionTuple,IAStarState> alreadyComputedAStarWithoutTraversingItems = new HashMap<PositionTuple,IAStarState>();

	public Map<String, Object> move(final JSONObject gameState) {
		long t1 = System.currentTimeMillis();
		Map<String, Object> returnMove = measuredMove(gameState);
		long t2 = System.currentTimeMillis();
		if(t2-t1 > 950){
			nTurnsInRowTooLong++;
		} else {
			nTurnsInRowTooLong = 0;
		}
		if(nTurnsInRowTooLong >= 3 && maxNHSize > 1) {
			largeWorld = true;
			maxNHSize--;
			nTurnsInRowTooLong = 0;
		}
		System.out.println("maxNHSize: " + maxNHSize);
		System.out.println("Total time of AI: " + (System.currentTimeMillis() - t1));
		return returnMove;
	}

	public Map<String, Object> measuredMove(final JSONObject gameState) {

		gs = new GameState(gameState, gs);

		// Next command object
		nextCommand = new HashMap<String, Object>();

		// Compute distance and actions to closest user
		Position closestUserPos = getPositionToClosestUser(gs.agentPos, false);
		if(closestUserPos == null){
			closestUserPos = getPositionToClosestUser(gs.agentPos, true);
		}
		List<String> actionsToClosestUser = getActionsToPosition(closestUserPos, false);

		// Try to place trap
		if(useTrap() && !timeIsRunningOut(actionsToClosestUser)) {
			System.out.println("Next action: use trap");
			nextCommand.put("command", "use");
			nextCommand.put("item", "trap");
			return nextCommand;
		}

		// Try to use banana if possible
		if(useBanana()){
			System.out.println("Next action: use banana");
			nextCommand.put("command", "use");
			nextCommand.put("item", "banana");
			return nextCommand;
		}

		// Check to see if there are a playlist in reach, then be greedy and try to take it.
		if(!useNewHeuristic || newHeuristicIsGreedy) {
			Position closestPlaylistPos = getPositionToClosestPlaylist(gs.agentPos);
			if(closestPlaylistPos != null){
				List<String> actionsToClosestPlaylist = getActionsToPosition(closestPlaylistPos, false);
				if(!actionsToClosestPlaylist.isEmpty() && actionsToClosestPlaylist.size() <  3 && !gs.isInventoryFull()) {
					System.out.println("Wow! That playlist looks tasty! " + closestPlaylistPos + "!!!");
					appendNextAction(actionsToClosestPlaylist);
					return nextCommand;
				}
			}
		}

		// Tackle mode!
		List<String> actionsToOpponent = getActionsToPosition(gs.enemyPos, false);
		if(tackleMode(actionsToOpponent.size(), actionsToClosestUser.size(), 
				distanceBetweenPositions(gs.enemyPos, closestUserPos, false))){
			System.out.println("Next action: BERSERK!!!11!1!!11!!one!!11!");
			if(actionsToOpponent.size() > 0) {
				appendNextAction(actionsToOpponent);
				return nextCommand;
			}
		}

		// Time is almost shorter than distance to closest user
		if(timeIsRunningOut(actionsToClosestUser) || (!useNewHeuristic && gs.isInventoryFull())) {
			System.out.println("Return to user at " + closestUserPos + "!!!");
			appendNextAction(actionsToClosestUser);
			return nextCommand;
		}

		// Inventory is full
		if(gs.isInventoryFull()) {
			if(distances != null && !largeWorld) {
				// Afford to compute best null neighbourhood
				s1p = getBestnHood2(distances);
			}
			Position user = gs.users.get(s1p.u);
			System.out.println("Return to user at " + user);
			List<String> actionsToS1PUser = getActionsToPosition(user, false);
			if(actionsToS1PUser.size() > 0) {
				appendNextAction(actionsToS1PUser);
			} else {
				appendNextAction(actionsToClosestUser);
			}
			return nextCommand;
		}

		// Compute all pairwise distances between items
		distances = computeItemDistances();

		int[] bestNHood = null;

		if(useNewHeuristic) {
			// The new heuristic which finds the best neighbourhood;
			s1p = getBestnHood2(distances);
			StringBuilder sb = new StringBuilder();
			sb.append("New Heuristic says: ");
			sb.append(stage1PlanToString(s1p));
			System.out.println(sb.toString());

			// If null neighbourhood is best --> Return to null plan's user
			if(s1p.isNullPlan() || s1p.nHood == null) { 
				Position user = gs.users.get(s1p.u);
				List<String> actionsToS1PUser = getActionsToPosition(user, false);
				if(actionsToS1PUser.size() > 0){
					appendNextAction(actionsToS1PUser);
					return nextCommand;
				} else { // There are no plan in the time frame --> tackle mode
					actionsToOpponent = getActionsToPosition(gs.enemyPos, false);
					if(tackleMode(actionsToOpponent.size(), actionsToClosestUser.size(), 
							distanceBetweenPositions(gs.enemyPos, closestUserPos, false))){
						System.out.println("Next action: BERSERK!!!11!1!!11!!one!!11!");
						appendNextAction(actionsToOpponent);
						return nextCommand;
					}
				}

				//If we cant enter tackle mode then stay idle
				System.out.println("Next action: idle. Can't figure out what to do.");
				nextCommand.put("command", "idle");
				return nextCommand;
			}

			// Set best neighbourhood for TSP calculations further down
			bestNHood = s1p.nHood;

		} else { //Use older version of heuristic (Not used in tournament)
			// Create all neighbourhoods of all sizes
			List<int[]> nHoods = new ArrayList<int[]>();
			for(int tripSize = 1 ; tripSize <= Math.min(gs.slotsLeftInInventory()+nExtraNodesInNH,maxNHSize); tripSize++) {
				nHoods.addAll(getNeighbourhoods(distances, tripSize));
			}

			// Get the best neighbourhood
			bestNHood = getBestnHood(nHoods);
			if(bestNHood == null){
				System.out.println("Next action: idle. Can't figure out what to do.");
				nextCommand.put("command", "idle");
				return nextCommand;
			}

			// Check if its better to return to user than pursue more items
			double valueOfBestNHood = valueOfNeighbourhood(bestNHood);
			double valueOfReturningItems = valueOfReturningItems();
			if(valueOfReturningItems < valueOfBestNHood){
				System.out.println("Return to user at " + closestUserPos + "!!!");
				appendNextAction(actionsToClosestUser);
				return nextCommand;
			}
		}

		// Get the best TSP in the chosen neighbourhood
		int[] bestPerm = getBestPermutation(bestNHood, Math.min(gs.slotsLeftInInventory(),maxPermSize), true);
		printBestNHood(bestNHood, bestPerm);

		// Construct the positions in plan and the actions
		List<String> nextActions = new ArrayList<String>();
		LinkedList<Position> posInPlan = new LinkedList<Position>();
		for(int i = 0; i < bestPerm.length; i++) {
			Position pos = gs.items.get(bestNHood[bestPerm[i]]).p;
			posInPlan.add(pos);
		}

		// Try to inject a banana in the path if it is beneficial
		injectBanana(posInPlan);

		// Create the actions for the final plan
		for(Position pos : posInPlan) {
			nextActions.addAll(getActionsToPosition(pos, true));
		}

		// Construct next command to send to user
		nextCommand = appendNextAction(nextActions);

		// Log
		//		log();
		return nextCommand;
	}

	private boolean tackleMode(int actionsToOpponent, int actionsToClosestUser, double actionsBetweenOpponentAndClosestUserToMe) {
		return gs.isInventoryEmpty() && actionsToOpponent <= 2 && actionsToClosestUser < actionsBetweenOpponentAndClosestUserToMe;
	}


	//Injects a banana in the plan if it reduces total distance of plan
	private void injectBanana(LinkedList<Position> posInPlan) {
		Position firstPos = posInPlan.getFirst();
		double origDist = distanceBetweenPositions(gs.agentPos, firstPos, true);
		double minBananaDist = Double.POSITIVE_INFINITY;
		Position minBanana = null;
		for(Position b : gs.bananas){
			double bananaDist1 = distanceBetweenPositions(gs.agentPos, b, true);
			double bananaDist2 = bananaDist1 + distanceBetweenPositions(b, firstPos, true);
			if(bananaDist2 < origDist + 5) { // It is a candidate banana
				if(bananaDist1 < minBananaDist) // Pick the banana closest to user!
					minBanana = b;
				minBananaDist = bananaDist1;
			}
		}

		// If set of rules are fulfilled
		if(minBanana != null && !gs.isInventoryFull() && !gs.inventoryContainsBanana()) {
			System.out.println("Banana at " + minBanana + " injected!");
			posInPlan.addFirst(minBanana);
		}
	}

	/**
	 * Computes the best neighbourhood to pursue (including null neighbourhood = return to user)
	 * @param distances pairwise distance between all items
	 * @return a Stage1Plan consisting of the best neighbourhood,
	 * the item which the neighbourhood is centered around,
	 * the best user to return to after neighbourhood is collected
	 * the size of the best NH,
	 * value of the Heuristic function
	 * 
	 */
	private Stage1Plan getBestnHood2(double[][] distances) {

		int maxTripSize = Math.min(gs.slotsLeftInInventory()+nExtraNodesInNH,maxNHSize);
		ArrayList<ArrayList<Double>> D = new ArrayList<ArrayList<Double>>(maxTripSize+1);
		ArrayList<ArrayList<Double>> P = new ArrayList<ArrayList<Double>>(maxTripSize+1);
		for(int k = 0; k <= maxTripSize; k++) {
			D.add(k, new ArrayList<Double>());
			P.add(k, new ArrayList<Double>());
		}
		double[] V = new double[gs.users.size()];
		double p_CI = gs.inventoryValue();

		double H_k_u_i_min = Double.POSITIVE_INFINITY;
		int k_min = 0;
		int k_i_min = 0;
		int k_u_min = 0;
		int[] nHood_min = null;
		for(int k = maxTripSize ; k >= 1 ; k--) {
			ArrayList<int[]> nHoods = getNeighbourhoods(distances, k);

			for(int i = 0; i < nHoods.size(); i++){
				int[] nHood = nHoods.get(i);
				Item centerItem = gs.items.get(nHood[0]);
				double D_k_i = 0;
				double P_k_i = gs.numberOfPointsOfPosition(centerItem.p);
				for(int j = 1; j < nHood.length; j++) {
					Item item = gs.items.get(nHood[j]);
					D_k_i += distanceBetweenPositions(centerItem.p, item.p, true);
					P_k_i += gs.numberOfPointsOfPosition(item.p);
				}

				D.get(k).add(i, D_k_i);
				P.get(k).add(i, P_k_i);
			}

			int nItems = gs.items.size();
			if(k == maxTripSize) { //Compute the value of all users
				for(int u = 0; u < gs.users.size(); u++) {
					if(heuristicOP.equals("min")) {
						V[u] = Double.POSITIVE_INFINITY;
					}
					Position user = gs.users.get(u);
					double nItemsReachable = 0;
					for(int i = 0; i < nItems; i++) {
						Item item = gs.items.get(i);
						double d_u_i = distanceBetweenPositions(user, item.p, true);
						if(d_u_i < Double.POSITIVE_INFINITY) {
							double D_c_i = D.get(maxTripSize).get(i);
							double P_c_i = P.get(maxTripSize).get(i);
							if(heuristicOP.equals("min")) {
								V[u] = Math.min(V[u], (d_u_i + D_c_i)/P_c_i);
							} else {
								V[u] += (d_u_i + D_c_i)/P_c_i;
								nItemsReachable++;
							}
						}
					}
					if(heuristicOP.equals("average")) {
						V[u] = V[u]/nItemsReachable;
					}
				}
			}

			double H_u_i_min = Double.POSITIVE_INFINITY;
			int i_min = 0;
			int u_min = 0;
			int[] minHood = null;

			for(int u = 0; u < gs.users.size(); u++) {
				Position user = gs.users.get(u);
				for(int i = 0; i < nItems; i++) {
					Item item = gs.items.get(i);
					double d_m_i = distanceBetweenPositions(gs.agentPos, item.p, true);
					double D_k_i = D.get(k).get(i);
					double d_i_u = distanceBetweenPositions(item.p,user, false);
					if(d_m_i + D_k_i + d_i_u < gs.nTurnsLeft) {
						double P_k_i = P.get(k).get(i);
						double H_u_i = (d_m_i + D_k_i + d_i_u)/(P_k_i + p_CI) + V[u];
						if(H_u_i < H_u_i_min) {
							H_u_i_min = H_u_i;
							u_min = u;
							i_min = i;
							minHood = nHoods.get(i);
						}
					}
				}
			}

			if(H_u_i_min < H_k_u_i_min) {
				H_k_u_i_min = H_u_i_min;
				k_min = k;
				k_i_min = i_min;
				k_u_min = u_min;
				nHood_min = minHood;
			}

		}

		Stage1Plan s1p = new Stage1Plan(nHood_min, k_i_min, k_u_min, k_min, H_k_u_i_min);

		if(gs.inventoryValue() > 0) {
			// Check if the null neighbourhood is better (go back to user)
			double nullH = Double.POSITIVE_INFINITY;
			int u_min = 0;
			for(int u = 0; u < gs.users.size(); u++) {
				Position user = gs.users.get(u);
				double d_m_u = distanceBetweenPositions(gs.agentPos, user, false);
				double H = d_m_u/p_CI + hNewConst*V[u];
				if(H < nullH) {
					nullH = H;
					u_min = u;
				}
			}
			if(nullH < H_k_u_i_min) {
				H_k_u_i_min = nullH;
				k_min = 0;
				k_i_min = -1;
				k_u_min = u_min;
				nHood_min = null;
				s1p = new Stage1Plan(nHood_min, k_i_min, k_u_min, k_min, H_k_u_i_min);
			}
		}

		return s1p;
	}

	/**
	 * best neighbourhood based on the heuristic value ...
	 * @param nHoods
	 * @return
	 */
	private int[] getBestnHood(List<int[]> nHoods) {

		int[] best = null;
		double bestValue = Double.MAX_VALUE;
		for(int[] nHood : nHoods){
			//TODO: if nominator > nTurnsLeft s� ska vi inte ta denna roundtrip.
			double value = valueOfNeighbourhood(nHood);
			if(value < bestValue){
				best = nHood;
				bestValue = value;
			}
		}
		return best;
	}

	private double valueOfNeighbourhood(int[] nHood) {
		Item centerItem = gs.items.get(nHood[0]);
		double nominator = distanceBetweenPositions(gs.agentPos, centerItem.p, true);
		double denominator = gs.numberOfPointsOfPosition(centerItem.p);
		StringBuilder sb = new StringBuilder();
		sb.append(centerItem.itemName + " at pos " + centerItem.p + "\t");
		for(int i = 1;i < nHood.length; i++){
			Item item = gs.items.get(nHood[i]);
			nominator += distanceBetweenPositions(item.p,centerItem.p, true);
			denominator += gs.numberOfPointsOfPosition(item.p);
			sb.append(item.itemName + "at pos " + item.p + "\t");
		}
		// Add distance back to closest user
		double distanceToClosestUser = Double.POSITIVE_INFINITY;
		Position posOfClosestUser = getPositionToClosestUser(centerItem.p, false);
		if(posOfClosestUser != null) {
			distanceToClosestUser = distanceBetweenPositions(centerItem.p, posOfClosestUser, true);
		}
		nominator += distanceToClosestUser;
		double value = nominator/denominator;
		sb.append(" | value = " + value);
		//            System.out.println(sb);

		return value;
	}

	private double valueOfReturningItems() {
		double distanceToClosestUser = Double.POSITIVE_INFINITY;
		Position posOfClosestUser = getPositionToClosestUser(gs.agentPos, false);
		if(posOfClosestUser != null) {
			distanceToClosestUser = distanceBetweenPositions(gs.agentPos, posOfClosestUser, true);
		}
		return distanceToClosestUser/gs.inventoryValue();
	}

	private ArrayList<int[]> getNeighbourhoods(double[][] distances, int nnSize) {
		int nItems = distances.length;
		nnSize = Math.min(nItems,nnSize); //Don't let null-pointer get u down, fight for the last pieces of items!!!	
		ArrayList<int[]> nHoods= new ArrayList<int[]>();
		for(int i = 0; i < gs.items.size(); i++) {
			double[] d = distances[i];
			ArrayIndexComparator comparator = new ArrayIndexComparator(d);
			Integer[] indexes = comparator.createIndexArray();
			Arrays.sort(indexes, comparator);
			int[] hood = new int[nnSize];
			for(int k = 0; k < nnSize; k++) {
				hood[k] = indexes[k];
			}
			nHoods.add(hood);
		}
		return nHoods;
	}

	private double distanceBetweenPositions(Position p1, Position p2, boolean traverseItems){
		GameState.itemsTraversable = traverseItems;
		IAStarState goalState = null;
		PositionTuple startGoalTuple = new PositionTuple(p1,p2);

		// If distance is not precomputed, then compute 
		if(!alreadyComputedAStar.containsKey(startGoalTuple) || !traverseItems) {
			IAStarState startingState = new BoardState(gs,p1,p2,new LinkedList<String>(),
					new HashSet<Position>(), new HashMap<PositionTuple, Double>());
			aStar.resetToStartState(startingState);
			aStar.run();
			goalState = aStar.getCurrentState();
			if(goalState.hasReachedGoal()) {
				IAStarState reversedGoalState= new BoardState(gs, p2, p2, Util.reversePath(goalState.getActionsToGetHere()),
						BoardState.getVisitedStates(), BoardState.getHeuristicMap());
				PositionTuple reversedStartGoalTuple = new PositionTuple(p2, p1);
				if(traverseItems){
					alreadyComputedAStar.put(reversedStartGoalTuple, reversedGoalState);
					alreadyComputedAStar.put(startGoalTuple, goalState);
				} else {
					alreadyComputedAStarWithoutTraversingItems.put(reversedStartGoalTuple, reversedGoalState);
					alreadyComputedAStarWithoutTraversingItems.put(startGoalTuple, goalState);
				}
			}
		} else {
			goalState = alreadyComputedAStar.get(startGoalTuple);
		}

		// If the goal is reachable set distance to path length, else infinity
		double distance = Double.POSITIVE_INFINITY;
		if(goalState.hasReachedGoal()) {
			distance = goalState.getActionsToGetHere().size();
		}

		//Set back to true (standard)
		GameState.itemsTraversable = true;

		return distance;
	}

	private List<String> getActionsToPosition(Position p, boolean traverseItems) {
		PositionTuple pt = new PositionTuple(gs.agentPos, p);
		if(!alreadyComputedAStar.containsKey(pt) || !traverseItems) {
			distanceBetweenPositions(gs.agentPos, p, traverseItems);
		}
		IAStarState gs;
		if(traverseItems) {
			gs = alreadyComputedAStar.get(pt);
		} else {
			gs = alreadyComputedAStarWithoutTraversingItems.get(pt);
		}
		if(gs == null){
			return new ArrayList<String>();
		} else {
			return gs.getActionsToGetHere();
		}
	}


	private Position getPositionToClosestUser(Position pos, boolean traverseItems) {
		double minDist = Double.POSITIVE_INFINITY;
		Position bestPos = null;
		for(Position p : gs.users) {
			double dist = distanceBetweenPositions(pos, p, traverseItems);
			if(dist < minDist) {
				minDist = dist;
				bestPos = p;
			}
		}
		return bestPos;
	}

	private Position getPositionToClosestPlaylist(Position pos) {
		double minDist = Double.POSITIVE_INFINITY;
		Position bestPos = null;
		for(Item i : gs.items) {
			if(i.itemName.equals("playlist")){
				double dist = distanceBetweenPositions(pos, i.p, true);
				if(dist < minDist) {
					minDist = dist;
					bestPos = i.p;
				}
			}
		}
		return bestPos;
	}

	private int[] getBestPermutation(int[] nodes, int k, boolean returnToUser) {
		k = Math.min(nodes.length, k);
		List<int[]> perms = Util.nPermuteK(nodes.length, k);
		double minDist = Double.POSITIVE_INFINITY;
		int[] minPerm = perms.get(0);
		for(int[] perm : perms) {
			double permDist = Double.POSITIVE_INFINITY;
			if(returnToUser) {
				double distanceToClosestUser = Double.POSITIVE_INFINITY;
				Item lastItem = gs.items.get(nodes[perm[perm.length-1]]);
				Position posOfClosestUser = getPositionToClosestUser(lastItem.p, false);
				if(posOfClosestUser != null) {
					distanceToClosestUser = distanceBetweenPositions(lastItem.p, posOfClosestUser, true);
				}
				permDist = getPermutationValue(nodes, perm, distanceToClosestUser);
			} else {
				permDist = getPermutationValue(nodes, perm, 0);
			}
			if(permDist < minDist){
				minDist = permDist; 
				minPerm = perm;
			}
		}
		return minPerm;
	}

	private double getPermutationValue(int[] nodes, int[] perm, double addToUser){
		double permDist = 0;
		double nPoints = 0;
		for(int i = 0; i < perm.length; i++){
			Position p1;
			Position p2;
			if(i == 0) {
				p1 = gs.agentPos;
			} else {
				p1 = gs.items.get(nodes[perm[i-1]]).p;
			}
			p2 = gs.items.get(nodes[perm[i]]).p;
			nPoints += gs.numberOfPointsOfPosition(p2);
			permDist += distanceBetweenPositions(p1, p2, true);
		}
		return (permDist + addToUser)/nPoints;
	}


	/**
	 * Shortest distance as defined by distance function. U define urself what the distance function is here.
	 * @return all pairwise distances according to ur distance function.
	 */
	private double[][] computeItemDistances() {
		double[][] d = new double[gs.items.size()][gs.items.size()];
		for(int i1 = 0; i1 < gs.items.size()-1; i1++){
			double dist = distanceBetweenPositions(gs.agentPos,gs.items.get(i1).p, true);
			for(int i2 = i1+1; i2 < gs.items.size(); i2++){
				dist = distanceBetweenPositions(gs.items.get(i1).p,gs.items.get(i2).p, true);
				double nPoints = gs.numberOfPointsOfPosition(gs.items.get(i1).p) + 
						gs.numberOfPointsOfPosition(gs.items.get(i2).p);
				dist = dist/nPoints;
				d[i1][i2] = dist;
				d[i2][i1] = dist;
			}
		}
		return d;
	}

	private boolean useBanana() {
		boolean haveBanana = gs.inventoryContainsBanana();
		return !gs.buffs.contains(new Buff("speedy", 1)) && haveBanana;
	}

	private boolean useTrap() {
		boolean haveTrap = gs.inventoryContainsTrap();

		// If u are in a tight area
		if(gs.tightnessOfPos(gs.agentPos) <= 2 && haveTrap) {
			return true;
		}

		// If u are next to a user
		if(distanceBetweenPositions(gs.agentPos, getPositionToClosestUser(gs.agentPos, true), true) <= 1){
			return haveTrap && !gs.isInventoryFull(); //TODO: remove inventoryfull when issue #9 is fixed
		}
		return false;
	}



	private void saveDistancesToFile(double[][] d) {
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

	private void printBestNHood(int[] bestNHood, int[] bestPerm) {
		StringBuilder sb = new StringBuilder();
		for(int i : bestPerm) {
			int ii = bestNHood[i];
			sb.append(gs.items.get(ii).itemName + " at pos " + gs.items.get(ii).p + "\t");
		}
		System.out.println(sb.toString());
	}

	private Map<String, Object> appendNextAction(List<String> nextActions) {
		nextCommand.put("command", "move");
		if(gs.containsBuff("speedy") && nextActions.size() >= 2) {
			JSONArray actions = new JSONArray();
			actions.put(nextActions.get(0));
			actions.put(nextActions.get(1));
			nextCommand.put("directions", actions);
			System.out.println("Next actions: " + actions);
		} else {
			String action = nextActions.get(0);
			System.out.println("Next action: " + action);
			nextCommand.put("direction", action);
		}
		return nextCommand;
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

	private boolean timeIsRunningOut(List<String> actionsToClosestUser){
		return actionsToClosestUser.size() >= gs.nTurnsLeft - 1;
	}

	private int[] getBestnHoodPerm(List<int[]> nHoods) {
		int[] minPerm = null;
		if(nHoods.size() > 0) {
			minPerm = nHoods.get(0);
			double minDist = Double.POSITIVE_INFINITY;;
			for(int[] nodes : nHoods) {
				int[] perm = getBestPermutation(nodes, nodes.length, true);
				double permDist = getPermutationValue(nodes, perm,0);
				if(permDist < minDist) {
					minPerm = perm;
					minDist = permDist;
				}
			}
		}
		return minPerm;
	}

	private String stage1PlanToString(Stage1Plan s1p) {
		StringBuilder sb = new StringBuilder();
		sb.append("Value = " + s1p.H + " | ");
		if(s1p.nHood != null) {
			for(int item : s1p.nHood) {
				sb.append(gs.items.get(item).itemName + " at pos " + gs.items.get(item).p + "\t");
			}
			sb.append(", return to user at " + gs.users.get(s1p.u));
		} else {
			if(s1p.isNullPlan()) {
				sb.append("(Null neighbourhood), return to user at " + gs.users.get(s1p.u));
			}
		}
		return sb.toString();
	}

}
