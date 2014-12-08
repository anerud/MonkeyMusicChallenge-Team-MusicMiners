package Util;

import java.util.ArrayList;
import java.util.List;

import environment.Buff;
import environment.GameState;

public class Util {

	/**
	 * Puts Strings of separate plans together to one string of the entire plan
	 * @param plan
	 * @return
	 */
	public static String planToString(List<String> plan){
		StringBuilder planString = new StringBuilder();
		for(int i = 0; i < plan.size(); i++) {
			planString.append(plan.get(i) + ", ");
		}
		return plan.toString();
	}

	public static List<int[]> nPermuteK(int n, int k){
		if(k > n) {
			k = n;
		}
		int[] startPerm = new int[k];
		for(int i = 0; i < startPerm.length; i++) {
			startPerm[i] = -1;
		}
		return nPermuteKHelper(startPerm ,n,k);
	}

	private static List<int[]> nPermuteKHelper(int[] perm ,int n, int kLeft){
		List<int[]> perms = new ArrayList<int[]>();
		if(kLeft <= 0) {
			return perms;
		}
		for(int i = 0; i < n; i++) {
			if(!permContains(perm, i)) {
				int[] newPerm = perm.clone();
				newPerm[newPerm.length-kLeft] = i;
				if(kLeft > 1) {
					List<int[]> l = nPermuteKHelper(newPerm,n,kLeft-1);
					perms.addAll(l);
				} else {
					perms.add(newPerm);
				}
			}
		}
		return perms;
	}

	private static boolean permContains(int[] perm, int i) {
		for(int j = 0; j < perm.length; j++) {
			if(perm[j]==i) {
				return true;
			}
		}
		return false;
	}

	public static List<String> reversePath(List<String> path) {
		List<String> reversedPath = new ArrayList<String>();
		for(int i = path.size()-1; i >= 0; i--) {
			if(path.get(i).equals("up")){
				reversedPath.add("down");
			} else if(path.get(i).equals("down")){
				reversedPath.add("up");
			} else if(path.get(i).equals("left")){
				reversedPath.add("right");
			} else {
				reversedPath.add("left");
			}
		}
		return reversedPath;
	}
}
