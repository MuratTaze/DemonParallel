package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import labelPropagation.Community;
import labelPropagation.CommunityList;

public class CommunityMerger {
	private CommunityList<Integer> pool = null;
	public CommunityMerger() {
		super();
		// TODO Auto-generated constructor stub
	}
	public CommunityList<Integer> getPool() {
		return pool;
	}
	public void setPool(CommunityList<Integer> pool) {
		this.pool = pool;
	}
	public CommunityMerger(CommunityList<Integer> pool) {
		this.pool=pool;
		// TODO Auto-generated constructor stub
	}
	/**
	 * Inverted indexed based merging. First creates inverted index list from
	 * communities. For each vertex , we have corresponding communities in
	 * posting list. Then this inverted index list is used to build community
	 * dependency graph. By using this dependency graph we perform merging.
	 * There is no unnecessary comparison.
	 * 
	 * @param mergeFactor
	 */
	public void improvedGraphBasedMerge(double mergeFactor) {
		// System.out.println("Merging---> Started.");
		constructInvertedIndex();
		int n = pool.getCommunities().size();
		int[] temporaryPool = new int[n];
		boolean[] needsMergeCheck = new boolean[n];
		int i = n - 2;
		boolean merged = false;
		while (i >= 0) {
			Community<Integer> mergerComm = pool.getCommunities().get(i);
			if (mergerComm == null) {
				i = i - 1;
				continue;
			}
			do {
				int temporaryPoolSize = 0;
				for (Community<Integer> dependecy : mergerComm.getDependencyList()) {
					int indexOfCommunity = dependecy.getIndex();
					if (indexOfCommunity > mergerComm.getIndex()) {
						if (!merged)
							needsMergeCheck[indexOfCommunity] = true;
						temporaryPool[temporaryPoolSize++] = indexOfCommunity;
					}
				}
				merged = false;
				for (int k = 0; k < temporaryPoolSize; ++k) {
					int index = temporaryPool[k];
					Community<Integer> mergedComm = pool.getCommunities().get(index);
					if (!needsMergeCheck[index])
						continue;
					if (isMergible(mergerComm, mergedComm, mergeFactor)) {
						merged = true;
						for (Community<Integer> c : mergedComm.getDependencyList()) {
							if (c.getIndex() > mergerComm.getIndex())
								needsMergeCheck[c.getIndex()] = true;
						}
						mergerComm = merge(mergerComm, mergedComm, true);
					} else {
						needsMergeCheck[index] = false;
					}
				}
			} while (merged);
			i = i - 1;
		}
	}

	private Community<Integer> merge(Community<Integer> mergerComm, Community<Integer> mergedComm,
			boolean withDependencies) {
		// members
		int size1 = mergerComm.getMembers().size();
		int size2 = mergedComm.getMembers().size();
		if (size1 > size2 || (size1 == size2 && mergerComm.getIndex() < mergedComm.getIndex())) {
			mergerComm.getMembers().addAll(mergedComm.getMembers());
			if (withDependencies) {
				mergerComm.getDependencyList().remove(mergedComm);
				mergedComm.getDependencyList().remove(mergerComm);
				for (Community<Integer> c : mergedComm.getDependencyList()) {
					c.getDependencyList().add(mergerComm);
					c.getDependencyList().remove(mergedComm);
				}
				mergerComm.getDependencyList().addAll(mergedComm.getDependencyList());
			}
			pool.getCommunities().set(mergedComm.getIndex(), null);
			return mergerComm;
		} else {
			merge(mergedComm, mergerComm, withDependencies);
			pool.getCommunities().set(mergedComm.getIndex(), null);
			pool.getCommunities().set(mergerComm.getIndex(), mergedComm);
			mergedComm.setIndex(mergerComm.getIndex());
			return mergedComm;
		}
	}

	public void graphBasedMerge(double mergeFactor) {
		constructInvertedIndex();
		// System.out.println("Merging---> Started.");

		int n = pool.getCommunities().size();
		int[] temporaryPool = new int[n];

		int i = n - 2;
		boolean merged = false;
		while (i >= 0) {
			Community<Integer> mergerComm = pool.getCommunities().get(i);
			if (mergerComm == null) {
				i = i - 1;
				continue;
			}
			do {
				int temporaryPoolSize = 0;
				for (Community<Integer> dependecy : mergerComm.getDependencyList()) {
					int indexOfCommunity = dependecy.getIndex();
					if (indexOfCommunity > mergerComm.getIndex()) {
						temporaryPool[temporaryPoolSize++] = indexOfCommunity;
					}
				}
				merged = false;
				for (int k = 0; k < temporaryPoolSize; ++k) {
					int index = temporaryPool[k];
					Community<Integer> mergedComm = pool.getCommunities().get(index);
					if (isMergible(mergerComm, mergedComm, mergeFactor)) {
						merged = true;
						mergerComm = merge(mergerComm, mergedComm, true);
					}
				}
			} while (merged);
			i = i - 1;
		}
	}

	/**
	 * This method performs merging. This is the brute force approach.
	 * Complexity is very high.
	 * 
	 * @param mergeFactor
	 */
	public void quadraticMerge(double mergeFactor) {

		/* merging part pooling approach */
		int j;
		int n = pool.getCommunities().size();
		int i = n - 2;
		boolean merged = false;
		while (i >= 0) {
			j = i + 1;
			Community<Integer> mergerComm = pool.getCommunities().get(i);
			if (mergerComm == null)
				continue;
			do {
				merged = false;
				while (j < n) {
					Community<Integer> mergedComm = pool.getCommunities().get(j);
					if (mergedComm != null && isMergible(mergerComm, mergedComm, mergeFactor)) {
						mergerComm = merge(mergerComm, mergedComm, false);
						merged = true;
					}
					j = j + 1;
				}
			} while (merged);
			i = i - 1;
		}
	}

	/**
	 * This method removes null values from community pool.
	 */
	public CommunityList<Integer> cleanPool(CommunityList<Integer> pool) {
		Iterator<Community<Integer>> iter = pool.getCommunities().iterator();
		CommunityList<Integer> cleanedPool = new CommunityList<Integer>();
		while (iter.hasNext()) {
			Community<Integer> community = iter.next();
			if (community != null)
				cleanedPool.getCommunities().add(community);
		}
		return cleanedPool;
	}

	/**
	 * This method constructs inverted index list then it builds community
	 * dependency graph.
	 */
	private void constructInvertedIndex() {
		HashMap<Integer, ArrayList<Community<Integer>>> invertedIndex = null;

		/* inverted index extraction */
		invertedIndex = new HashMap<Integer, ArrayList<Community<Integer>>>();
		for (Community<Integer> community : pool.getCommunities()) {
			for (Integer member : community.getMembers()) {
				ArrayList<Community<Integer>> ii = invertedIndex.get((member));
				if (ii == null) {
					ii = new ArrayList<Community<Integer>>();
					invertedIndex.put(member, ii);
				}

				ii.add(community);
			}

		}

		// System.out.println("Inverted index-->done.");

		/* dependency construction */
		for (ArrayList<Community<Integer>> list : invertedIndex.values()) {
			for (int i = 0; i < list.size(); i++) {
				list.get(i).getDependencyList().addAll(list);
				list.get(i).getDependencyList().remove(list.get(i));
			}
		}
		// System.out.println("dependency construction--> done.");

	}
	/**
	 * This method checks whether two communities can be merged or not.
	 * 
	 * @param community1
	 *            first community
	 * @param community2
	 *            second community
	 * @param mergeFactor
	 *            �?² is the threshold. If it is 1 we merge iff fully
	 *            containment is achieved. If it is 0 two communities are merged
	 *            anyway.
	 * @return returns true if at least �?² fraction of the smaller community
	 *         resides in their intersection.
	 */
	private boolean isMergible(Community<Integer> community1, Community<Integer> community2, double mergeFactor) {
		
		if (community1 == null || community2 == null)
			return false;

		double intersection = 0;
		double size1 = community1.getMembers().size();
		double size2 = community2.getMembers().size();
		double min = size2;
		if (size1 < size2) {
			min = size1;
		}
		if (min == size2) {
			for (Integer value : community2.getMembers()) {
				if (community1.getMembers().contains(value))
					intersection++;
			}
		} else {
			for (Integer value : community1.getMembers()) {
				if (community2.getMembers().contains(value))
					intersection++;
			}
		}
		if (intersection / min >= mergeFactor) {

			// them
			return true;
		}
		return false;
	}

	
}
