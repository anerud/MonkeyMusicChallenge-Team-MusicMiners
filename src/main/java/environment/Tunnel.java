package environment;

import Util.Position;

public class Tunnel {

	public Position p;
	public int id;

	public Tunnel(Position p, int id) {
		this.p = p;
		this.id = id;
	}

	@Override 
	public boolean equals(Object o) {
		if(o instanceof Buff) {
			Tunnel t = (Tunnel) o;
			return t.id == id && t.p.equals(p);
		}
		return false;
	}

	public boolean isTwinTunnel(Tunnel t) {
		return t.id == id && !t.p.equals(p);
	}

	@Override
	public int hashCode() {
		return 17*p.hashCode() + 23*id;
	}
}
