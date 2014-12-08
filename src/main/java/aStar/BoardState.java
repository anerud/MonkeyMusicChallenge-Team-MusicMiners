package aStar;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import Util.Position;
import Util.PositionTuple;
import environment.GameState;
import environment.Tunnel;

public class BoardState implements IAStarState {
	
	private static Set<Position> visitedStates;
	private static HashMap<PositionTuple,Double> heuristicMap;
	private GameState gs;
	private Position currentPos;
	private Position goalPos;
	private List<String> actionsToGetHere;
	private final String[] directions = {"right","down","left","up"};
	
	public BoardState(
			final GameState gameState, 
			final Position currentPos, 
			final Position goalPos, 
			List<String> actionsToGetHere, 
			Set<Position> visitedStates,
			HashMap<PositionTuple,Double> heuristicMap){
		this.gs = gameState;
		this.currentPos = currentPos;
		this.goalPos = goalPos;
		this.actionsToGetHere = actionsToGetHere;
		BoardState.visitedStates = visitedStates;
		BoardState.heuristicMap = heuristicMap;
	}
	
	public BoardState(
			final GameState gameState, 
			final Position currentPos, 
			final Position goalPos, 
			List<String> actionsToGetHere){
		this.gs = gameState;
		this.currentPos = currentPos;
		this.goalPos = goalPos;
		this.actionsToGetHere = actionsToGetHere;
	}

	@Override
	public int compareTo(IAStarState arg0) {
		// Here one can decide whether one wants FIFO or LIFO behavior on queue by setting ">=" or ">".
		if (this.getStateValue() - arg0.getStateValue() >= 0) {
			return 1;
		}
		return -1;
	}

	@Override
	public Collection<? extends IAStarState> expand()
			throws CloneNotSupportedException {
		LinkedList<IAStarState> neighborStates = new LinkedList<IAStarState>();
		Position newPos = new Position(-1,-1);
		for(double i = 0; i < 4; i++) {
			int dy = (int) Math.sin(i/2*Math.PI);
			int dx = (int) Math.cos(i/2*Math.PI);
			int y = (int) (currentPos.getY() + dy);
			int x = (int) (currentPos.getX() + dx);
			newPos.setX(x); newPos.setY(y);
			if(gs.isOnBoard(newPos) && !visitedStates.contains(newPos)){
				visitedStates.add(new Position(newPos));
				if(gs.isTraversableTile(newPos) || posIsGoal(newPos)) {
					List<String> newActionsToGetHere = new LinkedList<String>(actionsToGetHere);
					newActionsToGetHere.add(directions[(int)i]);
					Tunnel t1 = gs.tunnelAtPos(newPos);
					if(t1 != null) {
						Tunnel t2 = gs.getTwinTunnel(t1);
						newPos.setY(t2.p.getY()); newPos.setX(t2.p.getX());
					}
					IAStarState s = new BoardState(gs, new Position(newPos), goalPos, newActionsToGetHere);
					neighborStates.add(s);
				}
			}
		}
		return neighborStates;
	}

	@Override
	public double getStateValue() {
		// Manhattan heuristic
		PositionTuple pt = new PositionTuple(currentPos, goalPos);
		double heuristic;
		if(heuristicMap.containsKey(pt)) {
			heuristic = heuristicMap.get(pt);
		} else {
			heuristic = currentPos.distanceTo(goalPos);
			
			//TODO: Take tunnels into account
			// For all pairs of tunnels check 
			// manhattan distance if one would first 
			// go to t1 then appear out of t2 and then go to goal
			
			//THIS TAKES ALOT OF TIME!!! (TODO: See if one can do it faster)
			for(Tunnel t1 : gs.tunnels.values()) {
				Tunnel t2 = gs.getTwinTunnel(t1);
				double dist1 = currentPos.distanceTo(t1.p);
				double dist2 = t2.p.distanceTo(goalPos);
				if(dist1 + dist2 < heuristic) {
					heuristic = dist1 + dist2;
				}
			}
			heuristicMap.put(pt, heuristic);
		}
		
		return actionsToGetHere.size() + heuristic;
			   
	}

	@Override
	public boolean hasReachedGoal() {
		return posIsGoal(currentPos);
	}
	
	private boolean posIsGoal(Position pos){
		return pos.equals(goalPos);
	}

	@Override
	public List<String> getActionsToGetHere() {
		return actionsToGetHere;
	}
	
	public void setActionsToGetHere(List<String> path) {
		actionsToGetHere = path;
	}
	
	public Position getCurrentPos(){
		return currentPos;
	}
	
	public static Set<Position> getVisitedStates(){
		return visitedStates;
	}
	
	public static HashMap<PositionTuple, Double> getHeuristicMap(){
		return heuristicMap;
	}
	
}
