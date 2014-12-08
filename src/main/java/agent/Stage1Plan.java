package agent;

public class Stage1Plan {

	int[] nHood;
	int i;
	int u;
	int k;
	double H;

	/**
	 * A Plan in an early stage which states the nodes of the neighbourhood,
	 * around item i, of size k and also what user u to return to.
	 * @param nHood the nodes of the neighbourhood (including i as first in the array)
	 * @param i the item which the neighbourhood is defined around (redundant info, but here for clarity)
	 * @param u the user to return to from the neighbourhood
	 * @param k the size of the neighbourhood (also redundant since k = nHood.length, but here for clarity)
	 * @param H the heuristic value for this early stage plan
	 */
	public Stage1Plan(int[] nHood, int i, int u, int k, double H){
		this.nHood = nHood;
		this.i = i;
		this.u = u;
		this.k = k;
		this.H = H;
	}

	/**
	 * A null plan is defined as the neighbourhood of size 0 (returning to the user).
	 * 
	 * @return true if the plan is a null plan, false otherwise.
	 */
	public boolean isNullPlan(){
		return this.nHood == null && i == -1 && H < Double.POSITIVE_INFINITY;
	}

}
