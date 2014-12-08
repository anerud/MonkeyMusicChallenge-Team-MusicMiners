package Util;


public class PositionTuple {
	
	Position start;
	Position end;
	
	public PositionTuple(Position start, Position end) {
		this.start = new Position(start);
		this.end = new Position(end);
	}
	
	public PositionTuple(PositionTuple pt) {
		this.start = new Position(pt.start);
		this.end = new Position(pt.end);;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof PositionTuple) {
			PositionTuple pt = (PositionTuple) o;
			return this.start.equals(pt.start) && this.end.equals(pt.end);
		}
		return false;
	}
	
	@Override
	public String toString(){
		return start.toString() + "-->" + end.toString();
	}
	
	@Override
    public int hashCode() {
        return 17*start.hashCode() + 23*end.hashCode();
    }

}
