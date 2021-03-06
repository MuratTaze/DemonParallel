package utils;

import java.io.FileNotFoundException;
import java.util.Collections;

import org.pcj.PCJ;

import labelPropagation.Community;
import labelPropagation.CommunityList;

public class DemonGlobalMergeFunctions {

	CommunityList<Integer> globalCommunities;

	public DemonGlobalMergeFunctions(CommunityList<Integer> globalCommunities) {

		super();
		this.globalCommunities = globalCommunities;
	}

	/**
	 * It is like merge sort. At each level number of active threads decrease to
	 * half. It has some load balance problems.
	 * 
	 * @return
	 * 
	 * @throws FileNotFoundException
	 */
	public CommunityList<Integer> naiveGlobalMerge() throws FileNotFoundException {
	

		// interprocess merge operation starts here
		int numberOfIterations = (int) (Math.log10(PCJ.threadCount()) / Math.log10(2));
		/* create CM object to merge communities found */
		CommunityMerger merger = new CommunityMerger(globalCommunities);
		for (int i = 0; i < numberOfIterations; i++) {
			// wait for others to reach the same step
			PCJ.barrier();

			// determine who will merge with whom at this step
			if (myTurn(i)) {

				int target = PCJ.myId() + (int) Math.pow(2, i);
				if (target >= PCJ.threadCount())
					continue;

				CommunityList<Integer> targetCommunities = PCJ.get(target, "globalCommunities");

				globalCommunities.getCommunities().addAll(targetCommunities.getCommunities());
				globalCommunities = merger.cleanPool(globalCommunities);
				Collections.sort(globalCommunities.getCommunities(), Collections.reverseOrder());
				merger.setPool(globalCommunities);
				int a = 0;

				/* update index attributes */
				for (Community<Integer> c : globalCommunities.getCommunities()) {
					c.setIndex(a);
					a++;
				}
				merger.improvedGraphBasedMerge(0.8);

			} 

			
		}
		return globalCommunities;
	}

	/**
	 * Checks whether it is right time to do communication or not according to
	 * the iteration variable which is used represent current merging level
	 * 
	 * @param iteration
	 * @return returns true if the running thread is to do merge with another
	 *         thread
	 */
	private boolean myTurn(int iteration) {
		if (PCJ.myId() % (Math.pow(2, iteration + 1)) == 0)
			return true;
		else
			return false;
	}

}
