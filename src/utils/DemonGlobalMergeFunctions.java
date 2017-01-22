package utils;

import java.io.FileNotFoundException;

import org.pcj.PCJ;

import demon.parallel.DemonParallel;
import labelPropagation.Community;
import labelPropagation.CommunityList;

public class DemonGlobalMergeFunctions {
	CommunityList<Integer> globalCommunities;

	public DemonGlobalMergeFunctions(CommunityList<Integer> globalCommunities) {

		super();
		this.globalCommunities = globalCommunities;
	}

	public void performGlobalMerge(DemonParallel<Integer> demon) throws FileNotFoundException {
		// interprocess merge operation starts here
		int numberOfIterations = (int) (Math.log10(PCJ.threadCount()) / Math.log10(2));
		for (int i = 0; i < numberOfIterations; i++) {
			// wait for others to reach the same step
			PCJ.barrier();

			// determine who will merge with whom at this step
			if (myTurn(i)) {

				int target = PCJ.myId() + (int) Math.pow(2, i);
				if (target >= PCJ.threadCount())
					continue;

				CommunityList<Integer> targetCommunities = PCJ.get(target, "globalCommunities");

				System.out.println("merge!!!!!!!!!!!!!!!!!!! from " + PCJ.myId());
				globalCommunities.getCommunities().addAll(targetCommunities.getCommunities());
				merge(1);

			}
		}

	}

	private void merge(double mergeFactor) {
		/* merge part - pooling approach */
		for (int i = 0; i < globalCommunities.getCommunities().size() - 1; i++) {
			for (int j = i + 1; j < globalCommunities.getCommunities().size();) {
				if (isMergible(globalCommunities.getCommunities().get(i), globalCommunities.getCommunities().get(j),
						mergeFactor)) {
					globalCommunities.getCommunities().get(i).getMembers()
							.addAll(globalCommunities.getCommunities().get(j).getMembers());
					globalCommunities.getCommunities().remove(j);
					j = i + 1;
				} else
					j++;
			}
		}
	}

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
			return true;
		}
		return false;
	}

	private boolean myTurn(int iteration) {
		if (PCJ.myId() % (Math.pow(2, iteration + 1)) == 0)
			return true;
		else
			return false;
	}

}
