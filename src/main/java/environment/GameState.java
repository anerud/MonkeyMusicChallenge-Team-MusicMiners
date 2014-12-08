package environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import Util.Position;
import Util.Util;

public class GameState {
	
	public GameState lastGameState;
	public Position agentPos;
	public Position enemyPos;
	public String[][] gameTiles;

	public List<Item> items;
    public List<Position> bananas;
    public List<Position> openDoors;
    public List<Position> closedDoors;
    public List<Position> levers;

    public List<Position> users;
	public HashMap<Position,Tunnel> tunnels;
	public HashMap<Tunnel,Tunnel> twinTunnels;
	public int nRows;
	public int nCols;
	public int nTurnsLeft;
	public ArrayList<Buff> buffs;
	public ArrayList<String> inventory;
	public int inventorySize;
    public static boolean itemsTraversable = true;
	
	public GameState(JSONObject gameState, GameState lastGameState) {

		// Agent's position
		this.lastGameState = lastGameState;
		agentPos = new Position(gameState.getJSONArray("position").getInt(0), 
								gameState.getJSONArray("position").getInt(1));
		// The game board and enemy position
		JSONArray layout = gameState.getJSONArray("layout");
		nRows = layout.length();
		nCols = layout.getJSONArray(0).length();
		gameTiles = new String[nRows][nCols];
		items = new ArrayList<Item>();
		users = new ArrayList<Position>();
		bananas = new ArrayList<Position>();
		tunnels = new HashMap<Position,Tunnel>();
		twinTunnels = new HashMap<Tunnel,Tunnel>();
        openDoors = new ArrayList<>();
        closedDoors = new ArrayList<>();
        levers = new ArrayList<>();
		Position p = new Position(0,0);
		for (int y = 0; y < nRows; y++) {
			for (int x = 0; x < nCols; x++) {
				gameTiles[y][x] = layout.getJSONArray(y).getString(x);
				p.setX(x); p.setY(y);
				
				// All items in a list
				if(isItem(gameTiles[y][x])) {
					items.add(new Item(new Position(y,x),agentPos,gameTiles[y][x]));
				}
				
				// All users in a list
				if(gameTiles[y][x].equals("user")) {
					users.add(new Position(y,x));
				}
				
				// Add all bananas to a list
				if(gameTiles[y][x].equals("banana")) {
					bananas.add(new Position(y, x));
				}
				
				//Check if enemy position
				if(gameTiles[y][x].equals("monkey") && !agentPos.equals(p)) {
					enemyPos = new Position(y,x);
				}

                //Open Doors
                if(gameTiles[y][x].equals("open-door")) {
                    openDoors.add(new Position(y,x));
                }

                //Closed Doors
                if(gameTiles[y][x].equals("closed-door")) {
                    closedDoors.add(new Position(y,x));
                }

                //TODO
                //Levers
                if(gameTiles[y][x].equals("lever")) {
                    levers.add(new Position(y,x));
                }
                
                //TODO
                //Tunnels
				if(lastGameState == null) {
					if(gameTiles[y][x].startsWith("tunnel")) {
						// The form of a tunnel string is "tunnel-x" where x is the id
						Tunnel t = createTunnel(p);
						tunnels.put(t.p,t);
					}
				} else {
					this.tunnels = lastGameState.tunnels;
					this.twinTunnels = lastGameState.twinTunnels;
				}
			}
		}
		
		// figure out twin tunnels
		if(lastGameState == null) {
			Tunnel[] T = new Tunnel[tunnels.values().size()];
			T = tunnels.values().toArray(T);
			for(int i = 0; i < T.length-1; i++) {
				Tunnel t1 = T[i];
				for(int j = i; j < T.length; j++) {
					Tunnel t2 = T[j];
					if(t1.isTwinTunnel(t2)) {
						twinTunnels.put(t1, t2);
						twinTunnels.put(t2, t1);
					}
				}
			}
		}
//		Collections.sort(items);
//		items = items.subList(0, Math.min(40, items.size()));
//		System.out.println("nItems: " + items.size());
		
		// number of turns left
		nTurnsLeft = gameState.getInt("remainingTurns");
		
		// Buffs
		buffs = new ArrayList<Buff>();
		JSONObject JSONBuff = gameState.getJSONObject("buffs");
		Iterator<String> it = JSONBuff.keys();
		while(it.hasNext()) {
			String buffName = it.next();
			int duration = JSONBuff.getInt(buffName);
			buffs.add(new Buff(buffName, duration));
		}
		
		// Inventory size
		inventorySize = gameState.getInt("inventorySize");
		
		// Inventory
		inventory = new ArrayList<String>();
		int nInv = gameState.getJSONArray("inventory").length();
		for(int i = 0; i < nInv; i++) {
			inventory.add(gameState.getJSONArray("inventory").getString(i));
		}
	}

    public boolean traversableItemsChanged(){
        if(lastGameState.items.equals(this.items) && lastGameState.bananas.equals(this.bananas) &&
                lastGameState.openDoors.equals(this.openDoors)){
            return true;
        }
        return false;
    }

    public boolean isTraversableTile(Position p) {
//    	if(isInventoryFull() && p.equals(enemyPos)){
//    		return false;
//    	}
    	
    	String tile = gameTiles[p.getY()][p.getX()];
        if(itemsTraversable){
            return tile.equals("empty") ||
                    tile.equals("monkey") ||
                    tile.equals("playlist") ||
                    tile.equals("song") ||
                    tile.equals("album") ||
                    tile.equals("banana") ||
                    tile.equals("trap") ||
                    tile.equals("open-door") ||
                    tile.startsWith("tunnel");
        }
        return tile.equals("empty") ||
                tile.equals("monkey") ||
                tile.equals("open-door") ||
                tile.startsWith("tunnel");
    }

	public Tunnel createTunnel(Position p) {
		Tunnel t = tunnelAtPos(p);
		if(t != null) {
			return t;
		}
		String tunnel = gameTiles[p.getY()][p.getX()];
		if(tunnel.startsWith("tunnel")) {
			String[] tunnelParts = tunnel.split("-");
			int id = Integer.parseInt(tunnelParts[tunnelParts.length-1]);
			t = new Tunnel(new Position(p), id);
		} else {
			System.out.println("The string \"" + tunnel + "\" does not start with tunnel!");
		}
		if(t == null) {
			System.out.println("No tunnel created since there is no tunnel at" + p + "!!!");
			StringBuilder sb = new StringBuilder();
			sb.append("Only tunnels at: ");
			for(Tunnel tt : tunnels.values()) {
				sb.append(tt.p +", ");
			}
			sb.append("\n");
			System.out.println(sb.toString());
			System.out.println("nRows = " + nRows +" | " + "nCols = " + nCols);
		}
		return t;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("agentPos = " + agentPos + "\n");
		sb.append("enemyPos = " + enemyPos + "\n");
		sb.append("nRows = " + nRows + "\n");
		sb.append("nCols = " + nCols + "\n");
		sb.append("nTurnsLeft = " + nTurnsLeft + "\n");
		
		sb.append("buffs = [");
		for(int i = 0; i < buffs.size(); i++) {
			sb.append("\n\t{");
			sb.append("buffName = " + buffs.get(i).buffName + ", duration = " + buffs.get(i).duration);
			sb.append("}");
			if(i != buffs.size()-1) {
				sb.append(", ");
			} else {
				sb.append("\n");
			}
		}
		sb.append("]\n");
		
		sb.append("inventorySize = " + inventorySize + "\n");
		
		sb.append("inventory = [");
		for(int i = 0; i < inventory.size(); i++) {
			sb.append("\n\t\"");
			sb.append(inventory.get(i));
			sb.append("\"");
			if(i != inventory.size()-1) {
				sb.append(", ");
			} else{
				sb.append("\n");
			}
		}
		sb.append("]");
		return sb.toString();
	}
	
	public String tileOf(Position p){
		return gameTiles[p.getY()][p.getX()];
	}
	
	public boolean isInventoryFull(){
		return (inventorySize-inventory.size() <= 0);
	}

    public boolean isInventoryEmpty(){
        return inventory.size() == 0;
    }

	public Tunnel getTwinTunnel(Tunnel t1) {
		return twinTunnels.get(t1);
	}
	
	public Tunnel tunnelAtPos(Position p) {
		return tunnels.get(p);
	}

	public boolean isTunnelAtPos(Position p) {
		return tunnelAtPos(p) != null;
	}
	
	public int slotsLeftInInventory() {
		return inventorySize - inventory.size();
	}
	
	public boolean containsBuff(String buffName) {
		Buff b;
		for(int i = 0; i < buffs.size(); i++) {
			b = buffs.get(i);
			if(b.buffName.equals(buffName)){
				return true;
			}
		}
		return false;
	}
	
	public double numberOfPointsOfPosition(Position p) {
		String tile = tileOf(p);
		if(tile.equals("user")) {
			double nPoints = 0;
			for(String t : inventory) {
				nPoints += numberOfPointsOfTile(t, false);
			}
			return nPoints/2;
		} else {
			return numberOfPointsOfTile(tile, false);
		}
	}
	
	public double numberOfPointsOfTile(String tile, boolean onlyPointsForMusic) {
		if(tile.equals("song")) {
			return 1;
		} else if(tile.equals("album")) {
			return 2;
		} else if(tile.equals("playlist")) {
			return 4;
		} else if(tile.equals("banana") && !onlyPointsForMusic) {
			return 0;
		} else if(tile.equals("trap") && !onlyPointsForMusic) {
			return 1.9;
		} else {
			return 0;
		}
	}

	public boolean isItem(String tile) {
		return tile.equals("song") ||
			   tile.equals("album") ||
			   tile.equals("playlist") ||
               tile.equals("trap");
	}

    public double inventoryValue() {
        double value = 0;
        for(String item :inventory){
            value += numberOfPointsOfTile(item, true);
        }
        return value;
    }

	public int tightnessOfPos(Position p) {
		int tightness = 0;
		Position newPos = new Position(-1,-1);
		for(double i = 0; i < 4; i++) {
			int dy = (int) Math.sin(i/2*Math.PI);
			int dx = (int) Math.cos(i/2*Math.PI);
			int y = (int) (p.getY() + dy);
			int x = (int) (p.getX() + dx);
			newPos.setY(y); newPos.setX(x);
			if(isOnBoard(newPos)) {
				String tile = gameTiles[y][x];
				if(isTraversableTile(newPos)) {
					tightness++;
				}
			} 
		}
		return tightness;
	}

	public boolean isOnBoard(Position p) {
		return (p.getY() >= 0 && p.getY() < nRows) && (p.getX() >= 0 && p.getX() < nCols);
	}
	
	public boolean inventoryContainsBanana(){
		for(int i = 0; i < inventory.size(); i++) {
			if(inventory.get(i).equals("banana")) {
				return true;
			}
		}
		return false;
	}
	
	public boolean inventoryContainsTrap() {
		for(int i = 0; i < inventory.size(); i++) {
			if(inventory.get(i).equals("trap")) {
				return true;
			}
		}
		return false;
	}

}
