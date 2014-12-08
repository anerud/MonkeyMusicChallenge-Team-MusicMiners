package environment;

import Util.Position;
import Util.Util;

public class Item implements Comparable<Item>{

	public Position p;
	public Position userPos;
	public String itemName;

	public Item(Position p, Position userPos, String itemName) {
		this.p = p;
		this.userPos = userPos;
		this.itemName = itemName;
	}

	@Override
	public int compareTo(Item i) {
		return (int) (i.p.distanceTo(i.userPos) - p.distanceTo(userPos));
	}

}
