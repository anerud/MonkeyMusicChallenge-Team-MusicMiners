package agent;

import java.util.ArrayList;

import environment.GameState;
import Util.Position;
import Util.Util;

public class Trip implements Comparable<Trip>{

	public ArrayList<Position> trip;
	public ArrayList<String> tripActions;
	public double tripValue;

	public Trip() {
		trip = new ArrayList<Position>();
		tripActions = new ArrayList<String>();
		tripValue = 0;
	}

	@Override
	public int compareTo(Trip t) {
		return Double.compare(t.tripValue,this.tripValue);
	}

	public double nPointsOnTrip(GameState gs){
		double nPoints = 0;
		for(Position p : trip) {
			nPoints += gs.numberOfPointsOfTile(gs.tileOf(p), false);
		}
		return nPoints;
	}

}
