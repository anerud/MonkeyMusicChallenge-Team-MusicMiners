package deprecatedFiles;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONArray;
import org.json.JSONObject;

import environment.GameState;
import Util.Position;
import Util.PositionTuple;
import Util.Stat;
import Util.Util;
import aStar.AStar;
import aStar.BoardState;
import aStar.IAStarState;
import agent.Trip;

public class Agent {

	private File file;
	private GameState gs = null;
	private Stat<Long> aStarTime = new Stat<Long>();
	Trip minTrip;

	private int trackingMemorySize = 3;
	private LinkedList<Position> trackingMemory = new LinkedList<Position>(); 
	private int currentIndex = 0;
	private HashMap<PositionTuple,IAStarState> alreadyComputedAStar = new HashMap<PositionTuple,IAStarState>();

	public Map<String, Object> move(final JSONObject gameState) {

		gs = new GameState(gameState, gs);

		// Update tracking memory
		trackingMemory.addLast(gs.enemyPos);
		if(trackingMemory.size() > trackingMemorySize) {
			trackingMemory.removeFirst();
		}

		// Next command object
		final Map<String, Object> nextCommand = new HashMap<String, Object>();

		// Try to use banana if possible
		if(useBanana()){
			nextCommand.put("command", "use");
			nextCommand.put("item", "banana");
			return nextCommand;
		}

		//		int minDist = Integer.MAX_VALUE;
		//		Position minPos = new Position(-1, -1);
		//		List<String> minActions = new ArrayList<String>();
		//		String minTile = "";
		//		long totalTime = 0;
		//		for(int y = 0; y < gs.nRows; y++) {
		//			for(int x = 0; x < gs.nCols; x++) {
		//				boolean acceptedTile = false;
		//				String tile = gs.gameTiles[y][x];
		//				if(gs.inventory.size() < gs.inventorySize) {
		//					acceptedTile = Util.isValuable(tile);
		//				} else {
		//					acceptedTile = tile.equals("user");
		//				}
		//				if(acceptedTile) {
		//					IAStarState startingPoint = new BoardState(
		//							gs,
		//							gs.agentPos, 
		//							new Position(y,x), 
		//							new LinkedList<String>(),
		//							new HashSet<String>());
		//					AStar aStar = new AStar(startingPoint);
		//					aStar.run();
		//					totalTime += aStar.totalTime;
		//					if(aStar.hasReachedGoal()) {
		//						List<String> actions = aStar.getActionsToGoal();
		//						int distance = actions.size();
		//						if (distance < minDist) {
		//							minActions = actions;
		//							minDist = distance;
		//							minPos = new Position(y,x);
		//							minTile = tile;
		//						}
		//					}
		//				}
		//			}
		//		}

		//		HashMap<Position,Double> enemyHeadings = predictEnemyMovement();
		//		for(Position p : enemyHeadings.keySet()) {
		//			System.out.println(tileOf(p) + " at " +p + ": " + enemyHeadings.get(p));
		//		}

		long totalTime = System.currentTimeMillis();
		Trip minTrip = getBestTrip(Math.min(gs.inventorySize-gs.inventory.size()+1,2));
		totalTime = System.currentTimeMillis() - totalTime;

		// Construct next command
		nextCommand.put("command", "move");
		if(gs.containsBuff("speedy") && minTrip.tripActions.size() >= 2) {
			JSONArray actions = new JSONArray();
			actions.put(minTrip.tripActions.get(0));
			actions.put(minTrip.tripActions.get(1));
			nextCommand.put("directions", actions);
		} else {
			String action = minTrip.tripActions.get(0);
			nextCommand.put("direction", action);
		}

		// Logging starts here {
		FileWriter fw;
		BufferedWriter bw;

		try {
			if(file == null) {
				file = new File("log.txt");
				if(file.exists()){
					file.delete();
				}
				file.createNewFile();
				fw = new FileWriter(file, true);
				bw = new BufferedWriter(fw);
				bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
				bw.write("%%%%%%%%%%%%%%%%% NEW GAME %%%%%%%%%%%%%%%%%\n");
				bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n\n\n");
			} else {
				file = new File("log.txt");
				fw = new FileWriter(file, true);
				bw = new BufferedWriter(fw);
			}

			if(minTrip.tripValue < Double.MAX_VALUE) {
				bw.write(gs + "\n");
				StringBuilder sb = new StringBuilder();
				for(Position p : minTrip.trip) {
					bw.write(gs.tileOf(p) + " at pos = \t\t" + p + "\n");
				}
				bw.write("distance = " + minTrip.tripValue + "\n");
				bw.write("Plan = " + Util.planToString(minTrip.tripActions) + "\n");
				bw.write("Time spent getBestTrip()* = " + totalTime +"\n");

			} else {
				bw.write("No min tile found!");
			}
			bw.write("Command sent = " + nextCommand +"\n");
			bw.write("\n\n\n");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// } Logging ends here


		currentIndex = (currentIndex + 1)%trackingMemorySize;
		return nextCommand;
	}

	public boolean useBanana() {
		for(int i = 0; i < gs.inventory.size(); i++) {
			if(gs.inventory.get(i).equals("banana")) {
				return true;
			}
		}
		return false;
	}

	private HashMap<Position,Double> predictEnemyMovement(){
		HashMap<Position,Double> predictedProbabilities = new HashMap<Position,Double>();
		Position changeInPos = new Position(0, 0);
		Position lastPos = null;
		Position currentPos = null;
		Iterator<Position> it = trackingMemory.iterator();

		//Build vector of enemy's movements
		while(it.hasNext()) {
			if(lastPos != null) {
				currentPos = it.next();
				int dx = currentPos.getX() - lastPos.getX();
				int dy = currentPos.getY() - lastPos.getY();
				changeInPos.setX(changeInPos.getX() + dx);
				changeInPos.setY(changeInPos.getY() + dy);
				lastPos = currentPos;
			} else {
				lastPos = it.next();
			}
		}

		// Define the probability proportional to nPoints*(PI-angle)/distance
		double totalValue = 0;
		for(int y = 0; y < gs.nRows; y++) {
			for(int x = 0; x < gs.nCols; x++) {
				String tile = gs.gameTiles[y][x];
				if(gs.numberOfPointsOfTile(tile, false) > 0) {
					Position dPos = new Position(y-gs.enemyPos.getY(), x-gs.enemyPos.getX());
					double angle = changeInPos.angleBetween(dPos);
					IAStarState startingPoint = new BoardState(
							gs,
							gs.enemyPos, 
							new Position(y,x), 
							new LinkedList<String>(),
							new HashSet<Position>(),
							new HashMap<PositionTuple, Double>());
					AStar aStar = new AStar(startingPoint);
					aStar.run();
					double distance = Double.MAX_VALUE;
					if(aStar.hasReachedGoal()) {
						List<String> actions = aStar.getActionsToGoal();
						distance = actions.size();
					}

					double value = (Math.PI-angle)*gs.numberOfPointsOfTile(tile, false)/(distance*distance);
					predictedProbabilities.put(new Position(y,x), value);
					totalValue += value;
				}
			}
		}

		for(Position p : predictedProbabilities.keySet()){
			predictedProbabilities.put(p, predictedProbabilities.get(p)/totalValue);
		}

		return predictedProbabilities;
	}

	private Trip getBestTrip(int tripSize) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Trip> future = executor.submit(new BestTripComputer(tripSize));
		try {
			Trip minTrip = future.get(7000, TimeUnit.MILLISECONDS);
			System.out.println("Returned in time!");
			executor.shutdownNow();
			return minTrip;
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			System.out.println("Interrupted");
			return minTrip;
		}

	}

	private class BestTripComputer implements Callable<Trip> {

		int tripSize;
		public BestTripComputer(int tripSize){
			this.tripSize = tripSize;
		}

		@Override
		public Trip call() throws Exception {
			int nItems = gs.items.size();
			long combTime = System.currentTimeMillis();
			List<int[]> combinations = Util.nPermuteK(nItems, tripSize);
			combTime = System.currentTimeMillis() - combTime;
			minTrip = new Trip();
			minTrip.tripValue = Double.NEGATIVE_INFINITY;
			long aStarTime = 0;
			int nTimesAStar = 0;
			long loopTime = System.currentTimeMillis();
			for(int[] l : combinations) {
				Trip trip = new Trip();
				for(int i = 0; i < l.length; i++) {
					Position startPoint;
					Position goalPoint;
					if(i <= 0) {
						startPoint = gs.agentPos;
					} else {
						startPoint = gs.items.get(l[i-1]).p;
					}
					goalPoint = gs.items.get(l[i]).p;
					IAStarState goalState = null;
					PositionTuple startGoalTuple = new PositionTuple(new Position(startPoint),new Position(goalPoint));
					long asTime = System.currentTimeMillis();
					if(!alreadyComputedAStar.containsKey(startGoalTuple)) {
						IAStarState startingState = new BoardState(
								gs,
								startPoint, 
								goalPoint, 
								new LinkedList<String>(),
								new HashSet<Position>(),
								new HashMap<PositionTuple, Double>());
						AStar aStar = new AStar(startingState);
						aStar.run();
						goalState = aStar.getCurrentState();
						if(goalState.hasReachedGoal()) {
							alreadyComputedAStar.put(new PositionTuple(startGoalTuple), goalState);
						}
						nTimesAStar++;

					} else {
						goalState = alreadyComputedAStar.get(startGoalTuple);
					}
					asTime = System.currentTimeMillis() - asTime;
					aStarTime += asTime;
					if(goalState.hasReachedGoal()) {
						List<String> actionsToGoal = goalState.getActionsToGetHere();
						trip.tripActions.addAll(actionsToGoal);
						trip.trip.add(goalPoint);
						if(!gs.isInventoryFull()) {
							trip.tripValue += gs.numberOfPointsOfPosition(goalPoint)/actionsToGoal.size();
						} else {
							if(gs.tileOf(goalPoint).equals("user")) {
								trip.tripValue += gs.numberOfPointsOfPosition(goalPoint)/actionsToGoal.size();
							}
						}

					} else {
						trip.tripValue = Double.NEGATIVE_INFINITY;
						break;
					}
				}
				if(trip.tripValue >= minTrip.tripValue) {
					minTrip = trip;
				}
			}
			loopTime = System.currentTimeMillis()-loopTime - aStarTime;
			System.out.println("Combtime: " + combTime);
			System.out.println("nComb: " + combinations.size());
			System.out.println("nTimesAStar: " + nTimesAStar);
			System.out.println("aStarTime: " + aStarTime);
			System.out.println("looptime: " + loopTime);
			return minTrip;
		}
	}
}
