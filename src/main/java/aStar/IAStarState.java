package aStar;

import java.util.Collection;
import java.util.List;

public interface IAStarState extends Comparable<IAStarState> {

	/**
	 * @return returns the neighbouring states to this IAStarState
	 * @throws CloneNotSupportedException
	 */
	public Collection<? extends IAStarState> expand() throws CloneNotSupportedException;

	/**
	 * Returns the state value for this state. The state value is defined as v(s) + k*h(s)
	 * @return v(s) + k*h(s)
	 */
	public double getStateValue();

	/**
	 * @return a boolean if the current state is a goal state or not.
	 */
	public boolean hasReachedGoal();
	
	public List<String> getActionsToGetHere();

}
