/**
 * This file is part of the Kompics P2P Framework.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.p2p.overlay.cyclon;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeMap;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

import se.sics.kompics.p2p.overlay.OverlayAddress;

/**
 * The <code>GraphUtil</code> class contains some utility code to generate an
 * adjacency matrix for a directed graph and use it to compute the diameter,
 * average path length, and the clustering coefficient, using n BFS traversals.
 * 
 * TODO You should extend this class to do the same thing for the undirected
 * graph corresponding to this directed graph, to obtain results for the
 * diameter and path length that are comparable to the results in the Cyclon
 * paper, if you are interested.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class GraphUtil {

	int n;
	byte m[][];
	public int dist[][];
	double inDegree[];
	int outDegree[];
	double clustering[];
	CyclonAddress[] a;
	HashMap<CyclonAddress, Integer> map;
	int[][] neighbors;
	SummaryStatistics inStats, outStats;

	int diameter = 0, infinitePathCount = 0;
	double avgPathLength = 0, avgIn = 0, avgOut = 0, avgClustering = 0;
	double minIn = n, maxIn = 0;

	public GraphUtil(TreeMap<OverlayAddress, CyclonNeighbors> alivePeers) {
		super();
		n = alivePeers.size();
		m = new byte[n][n];
		dist = new int[n][n];
		inDegree = new double[n];
		outDegree = new int[n];
		clustering = new double[n];
		a = new CyclonAddress[n];
		map = new HashMap<CyclonAddress, Integer>();
		neighbors = new int[n][];
		inStats = new SummaryStatistics();
		outStats = new SummaryStatistics();

		// map all alive nodes to a contiguous sequence of integers
		{
			int p = 0;
			for (OverlayAddress address : alivePeers.keySet()) {
				CyclonAddress src = (CyclonAddress) address;
				a[p] = src;
				map.put(src, p);
				p++;
			}
		}

		// build adjacency matrix
		int d = -1;
		{
			try {
				for (int s = 0; s < a.length; s++) {
					CyclonAddress src = a[s];
					CyclonNeighbors neigh = alivePeers.get(src);
					int nn = 0;
					for (CyclonNodeDescriptor desc : neigh.getDescriptors()) {
						CyclonAddress dst = desc.getCyclonAddress();
						if (!map.containsKey(dst)) {
							continue;
						}
						d = map.get(dst);
						m[s][d] = 1;
						inDegree[d]++;
						outDegree[s]++;
						nn++;
					}
					neighbors[s] = new int[nn];
					nn = 0;
					for (CyclonNodeDescriptor desc : neigh.getDescriptors()) {
						CyclonAddress dst = desc.getCyclonAddress();
						if (map.containsKey(dst)) {
							neighbors[s][nn++] = map.get(dst);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		// build distance matrix, clustering coefficient, average path length
		// diameter and average degrees
		{
			for (int i = 0; i < n; i++) {
				bfs(i, dist[i]);

				// we compute the clustering coefficient here
				int neigh[] = neighbors[i];
				if (neigh.length <= 1) {
					clustering[i] = 1.0;
					continue;
				}
				int edges = 0;

				for (int j = 0; j < neigh.length; j++) {
					for (int k = j + 1; k < neigh.length; k++) {
						if (m[neigh[j]][neigh[k]] > 0
								|| m[neigh[k]][neigh[j]] > 0) {
							++edges;
						}
					}
				}
				clustering[i] = ((edges * 2.0) / neigh.length)
						/ (neigh.length - 1);
			}
			int k = 0;
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					if (i == j)
						continue;
					if (dist[i][j] == n) {
						infinitePathCount++;
						continue;
					}
					if (dist[i][j] > diameter) {
						diameter = dist[i][j];
					}
					avgPathLength = (avgPathLength * k + dist[i][j]) / (k + 1);
					k++;
				}
				inStats.addValue(inDegree[i]);
				outStats.addValue(outDegree[i]);
				// avgIn = (avgIn * i + inDegree[i]) / (i + 1);
				// minIn = minIn > inDegree[i] ? inDegree[i] : minIn;
				// maxIn = maxIn < inDegree[i] ? inDegree[i] : maxIn;
				// avgOut = (avgOut * i + outDegree[i]) / (i + 1);
				avgClustering = (avgClustering * i + clustering[i]) / (i + 1);
			}
		}
	}

	private void bfs(int v, int d[]) {
		Queue<Integer> q = new LinkedList<Integer>();
		for (int i = 0; i < n; i++) {
			d[i] = n; // also means that the node has not been visited
		}
		d[v] = 0;
		q.offer(v);
		q.offer(0); // depth of v
		while (!q.isEmpty()) {
			int u = q.poll();
			int du = q.poll(); // depth of u

			for (int t = 0; t < neighbors[u].length; t++) {
				if (d[neighbors[u][t]] == n) {
					// on the first encounter, add to the queue
					d[neighbors[u][t]] = du + 1;
					q.offer(neighbors[u][t]);
					q.offer(du + 1);
				}
			}
		}
	}

	public int getInDegree(int v) {
		if (v < n) {
			return (int) inDegree[v];
		} else {
			return 0;
		}
	}

	public int getOutDegree(int v) {
		if (v < n) {
			return outDegree[v];
		} else {
			return 0;
		}
	}

	public double getClustering(int v) {
		if (v < n) {
			return clustering[v];
		} else {
			return 0;
		}
	}

	public double getMinInDegree() {
		// return minIn;
		return inStats.getMin();
	}

	public double getMaxInDegree() {
		// return maxIn;
		return inStats.getMax();
	}

	public double getMeanInDegree() {
		// return avgIn;
		return inStats.getMean();
	}

	public double getInDegreeStdDev() {
		// return avgIn;
		return inStats.getStandardDeviation();
	}

	public double getMeanOutDegree() {
		// return avgOut;
		return outStats.getMean();
	}

	public double getMeanClusteringCoefficient() {
		return avgClustering;
	}

	public double getMeanPathLength() {
		return avgPathLength;
	}

	public int getDiameter() {
		return diameter;
	}

	public SummaryStatistics getInStats() {
		return inStats;
	}

	public SummaryStatistics getOutStats() {
		return outStats;
	}

	public int getNetworkSize() {
		return n;
	}
	
	public double[] getInDegrees() {
		return inDegree;
	}
	
	public int getNodeIndexByAddress(OverlayAddress address) {
		return map.get(address);
	}

	public int getInfinitePathCount() {
		return infinitePathCount;
	}
}
