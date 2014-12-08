package Util;

public class Position{

	private int x;
	private int y;

	public Position(int y, int x){
		this.y = y;
		this.x = x;
	}

	public Position(Position p) {
		this.x = p.x;
		this.y = p.y;
	}

	@Override
	public boolean equals(Object o){
		if(o instanceof Position) {
			Position p = (Position) o;
			return (p.x == this.x && p.y == this.y);
		}
		return false;
	}

	@Override
	public String toString(){
		return "(" + y + "," + x + ")";
	}

	@Override
	public int hashCode() {
		return 13*x + 31*y;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int distanceTo(Position p) {
		return Math.abs(this.x - p.x) + Math.abs(this.y - p.y);
	}

	public double angleBetween(Position p) {
		double length1 = Math.sqrt(x*x + y*y);
		double length2 = Math.sqrt(p.x*p.x + p.y*p.y);
		return Math.acos((x*p.x + y*p.y)/(length1*length2));
	}
}
