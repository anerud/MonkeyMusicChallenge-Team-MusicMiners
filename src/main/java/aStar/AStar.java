package aStar;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

public class AStar {
	
	private PriorityQueue<IAStarState> q;
	private IAStarState currentState;
    public int nStatesChecked;
    public int nStatesAddedToQ;
    public long totalTime;
    


    /**
	 * Creates an AStar object with an empty priority queue
	 */
	public AStar() {
		q = new PriorityQueue<IAStarState>();
	}
	
	/**
	 * Creates an AStar object with a starting point.
	 * @param startingPoint
	 */
	public AStar(IAStarState startingPoint) {
		q = new PriorityQueue<>();
		q.add(startingPoint);
	}
	
	public void resetToStartState(IAStarState startingPoint){
		q = new PriorityQueue<>();
		q.add(startingPoint);
		nStatesChecked = 0;
	    nStatesAddedToQ = 0;
	    totalTime = 0;
	}
	
	/**
	 * Runs the aStar algorithm until the goal is reached or
	 * until the search space is explored.
	 * @return false if the goal could not be reached. True if
	 * the goal was reached.
	 */
	public void run() {
		if(q.peek().hasReachedGoal()) {
			currentState = q.poll();
			return;
		}
        long startTime;
        long endTime = 0;
        long timeDiff = 0;
        totalTime = 0;
		do {
            startTime = System.currentTimeMillis();
			currentState = q.poll();
			
            Collection<? extends IAStarState> neighbours = new LinkedList<IAStarState>();
			try {
				neighbours = currentState.expand();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			for(IAStarState s : neighbours) {
				if(s.hasReachedGoal()) {
					currentState = s;
					return;
				}
			}
			//Check neighbours for goalState
			q.addAll(neighbours);
			
            endTime = System.currentTimeMillis();
            timeDiff = endTime - startTime;
            totalTime += timeDiff;

            nStatesChecked++;
            nStatesAddedToQ += neighbours.size();

		} while (!q.isEmpty());
	}
	
	public boolean hasReachedGoal() {
		return currentState.hasReachedGoal();
	}
	
	/**
	 * @return the current state
	 */
	public IAStarState getCurrentState(){
		return currentState;
	}
	
	public List<String> getActionsToGoal(){
		return currentState.getActionsToGetHere();
	}

}
