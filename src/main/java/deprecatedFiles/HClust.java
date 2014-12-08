package deprecatedFiles;

import java.util.ArrayList;

/**
 * An implementation of Hierarchical clustering
 * @author Sebbe
 *
 */
public class HClust {
	
	private double[][] d;
	private int n;
	private int ccId = 0; //current cluster in dendrogram
	private ArrayList<Cluster> clusters;
	private Cluster[] orderedClusters;
	private double[][] dendrogram;
	private String method;
	
	public HClust(double[][] d){
		this.d = d;
		n = d.length;
	}
	
	private void init(){
		clusters = new ArrayList<Cluster>();
		orderedClusters = new Cluster[2*n-1];
		dendrogram = new double[n-1][3];
		for(ccId = 0; ccId < n; ccId++) {
			Cluster c = new Cluster(ccId);
			orderedClusters[ccId] = c;
			clusters.add(c);
		}
		ccId--;
	}
	
	public Cluster[] cluster(String method) throws deprecatedFiles.HClust.Cluster.NoSuchMethodExcetion {
		this.method = method;
		clust(n);
		return orderedClusters;
	}
	
	public Cluster earlyStopCluster(String method, int stopSize) throws deprecatedFiles.HClust.Cluster.NoSuchMethodExcetion {
		this.method = method;
		return clust(stopSize);
	}
	
	
	/**
	 * The Hierarchical clustering algorithm
	 * @param stopSize the alternative to stop at the first encounter of a cluster of size stopSize or larger
	 * To complete entire clustering set stopSize = n;
	 * @throws deprecatedFiles.HClust.Cluster.NoSuchMethodExcetion 
	 */
	private Cluster clust(int stopSize) throws deprecatedFiles.HClust.Cluster.NoSuchMethodExcetion{
		init();
		for(int i = 0; i < n-1; i++) {
			// Increment cluster index
			ccId++;
			int minV1 = 0;
			int minV2 = 1;
			double minDist = Double.MAX_VALUE;
			for(int v1 = 0; v1 < clusters.size()-1; v1++) {
				Cluster c1 = clusters.get(v1);
				for(int v2 = v1+1; v2 < clusters.size(); v2++) {
					Cluster c2 = clusters.get(v2);
					double dist = c1.distance(c2);
					if(dist < minDist){
						minV1 = v1;
						minV2 = v2;
						minDist = dist;
					}
				}
			}
			// Merge two closest clusters
			ArrayList<Integer> newNodes = new ArrayList<Integer>();
			newNodes.addAll(clusters.get(minV2).nodes);
			newNodes.addAll(clusters.get(minV1).nodes);
			Cluster newClust = new Cluster(newNodes,ccId);
			clusters.add(newClust);
			orderedClusters[ccId] = newClust;
			
			// Update dendrogram
			dendrogram[ccId-n][0] = clusters.get(minV1).id + 1;
			dendrogram[ccId-n][1] = clusters.get(minV2).id + 1;
			dendrogram[ccId-n][2] = minDist;
			
			// Remove merged clusters from the clusters to consider
			clusters.remove(minV2); //Remove v2 before v1 since v2 > v1.
			clusters.remove(minV1);
			
			if(newClust.nodes.size() >= stopSize) {
				return newClust;
			}
		}
		return orderedClusters[ccId];
	}
	
	public Cluster[] orderedClusters(){
		return orderedClusters;
	}
	
	public double[][] getDendrogram(){
		return dendrogram;
	}
	
	public class Cluster{
		ArrayList<Integer> nodes;
		int id;
		
		public Cluster(ArrayList<Integer> nodes, int id) {
			this.nodes = nodes;
			this.id = id;
		}
		
		public Cluster(Integer id) {
			this.nodes = new ArrayList<Integer>();
			nodes.add(id);
			this.id = id;
		}
		
		public double distance(Cluster c) throws NoSuchMethodExcetion {
			double dist;
			if(method.equals("single")) {
				dist = Double.POSITIVE_INFINITY;
				for(int v1 : nodes) {
					for(int v2 : c.nodes) {
						dist = Math.min(dist, d[v1][v2]);
					}
				}
			} else if (method.equals("complete")) {
				dist = Double.NEGATIVE_INFINITY;
				for(int v1 : nodes) {
					for(int v2 : c.nodes) {
						if(d[v1][v2] >= dist) {
							dist = d[v1][v2];
						}
//						dist = Math.max(dist, d[v1][v2]);
					}
				}
			} else if (method.equals("average")) {
				dist = 0;
				for(int v1 : nodes) {
					for(int v2 : c.nodes) {
						dist += d[v1][v2];
					}
				}
				dist /= nodes.size()*c.nodes.size();
			} else{
		        throw new NoSuchMethodExcetion();
			}
			return dist;
		}
		
		public class NoSuchMethodExcetion extends Exception{}
	}
}
