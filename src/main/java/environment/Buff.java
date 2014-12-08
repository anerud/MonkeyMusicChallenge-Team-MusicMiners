package environment;

public class Buff {

	public String buffName;
	public int duration;

	public Buff(String buffName, int duration){
		this.buffName = buffName;
		this.duration = duration;
	}

	@Override 
	public boolean equals(Object o) {
		if(o instanceof Buff) {
			Buff b = (Buff) o;
			return buffName.equals(b.buffName);
		}
		return false;
	}

}
