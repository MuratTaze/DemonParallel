package demon.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;

import org.pcj.PCJ;

import labelPropagation.Community;
import labelPropagation.CommunityList;
import labelPropagation.GraphLoader;
import labelPropagation.Network;
import labelPropagation.Vertex;

public class DemonGlobalMergeFunctions {
	CommunityList<Integer> globalCommunities;

private void performGlobalMerge(DemonParallel<Integer> demon,GraphLoader graphLoader) throws FileNotFoundException{
	//interprocess merge operation starts here
	int numberOfIterations = (int) (Math.log10(PCJ.threadCount()) / Math.log10(2));
	for (int i = 0; i < numberOfIterations; i++) {
		// wait for others to reach the same step 
		PCJ.barrier();

		 //determine who will merge with whom at this step 
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
	if (PCJ.myId() == 0) {
		double average_conductance = 0;
		double total_conductance = 0;
		// calculate conductance values 
		for (int i = 0; i < globalCommunities.getCommunities().size(); i++) {
			total_conductance += conductance(demon.getGlobalCommunities().getCommunities().get(i),
					graphLoader.getNetwork());

		}
		average_conductance = total_conductance / globalCommunities.getCommunities().size();
		System.out.println("Average conductance value is " + average_conductance);
		PrintWriter writer = new PrintWriter(new File("ACM_Communities_Found.txt"));
		writer.print(globalCommunities);
		writer.flush();
		writer.close();
	}
}
	private double conductance(Community<Integer> community, Network<Integer> network) {
		double in_degree = 0;
		double out_degree = 0;
		Iterator<Integer> iter = community.getMembers().iterator();
		while (iter.hasNext()) {
			Integer element = iter.next();
			double current_degree = degree(network.getGraph().get(new Vertex<Integer>(element)).getListOfNeighbors(),
					community.getMembers());
			out_degree += network.getGraph().get(new Vertex<Integer>(element)).getListOfNeighbors().size()
					- current_degree;
			in_degree += current_degree;
		}
		return out_degree / (in_degree / 2);

	}

	private double degree(HashSet<Vertex<Integer>> HashSet, HashSet<Integer> HashSet2) {
		double degree = 0;
		Iterator<Integer> iter = HashSet2.iterator();
		while (iter.hasNext()) {
			if (HashSet.contains(new Vertex<Integer>(iter.next()))) {
				degree++;
			}
		}
		return degree;
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