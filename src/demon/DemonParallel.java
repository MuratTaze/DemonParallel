package demon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import labelPropagation.EgoNetwork;
import labelPropagation.GraphLoader;
import labelPropagation.LabelPropagation;
import labelPropagation.LocalNetwork;

import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;

public class DemonParallel extends Storage implements StartPoint {
	private CopyOnWriteArrayList<HashSet<String>> globalCommunities = null;
	@Shared
	private EgoNetwork[] localGraph;
	@Override
	public void main() throws IOException {
		GraphLoader graph = null;
		graph = new GraphLoader("traininGraph.txt");
		localGraph=graph.partition(
				PCJ.myId(), PCJ.threadCount());
		LocalNetwork localNetwork = new LocalNetwork(localGraph);
		for (Entry<String, HashSet<String>> entry : localNetwork
				.getLocalNetwork().entrySet()) {
			HashMap<String, HashSet<String>> e = egoMinusEgo(entry.getKey(),
					localNetwork.getLocalNetwork());
			LabelPropagation lp = new LabelPropagation();
			lp.initiliaze(e);
			lp.proceedLP();
			ArrayList<HashSet<String>> localCommunities = lp
					.constructCommunities();
			for (Iterator<HashSet<String>> iterator = localCommunities.iterator(); iterator
					.hasNext();) {
				HashSet<String> localCommunity = (HashSet<String>) iterator.next();
				localCommunity.add(entry.getKey());
				merge( localCommunity,0.30);
			}
			
		}
System.out.println(globalCommunities);
	}

	private void merge(
		HashSet<String> localCommunity, double percantage) {
		if (globalCommunities == null) {
			globalCommunities = new CopyOnWriteArrayList<HashSet<String>>();
			globalCommunities.add(localCommunity);
		} else {
			for (Iterator<HashSet<String>> iterator = globalCommunities
					.iterator(); iterator.hasNext();) {
				HashSet<String> temp = (HashSet<String>) iterator.next();
				HashSet<String> temp2 = new HashSet<String>(temp);
				double size1 = temp2.size();
				temp2.retainAll(localCommunity);
				if (temp2.size() / size1 >= percantage) {
					temp.addAll(localCommunity);// join them
					return;
				} else {
					
				}
			}globalCommunities.add(localCommunity);// as a seperate
			// community
		}
	}

	private HashMap<String, HashSet<String>> egoMinusEgo(String key,
			HashMap<String, HashSet<String>> localGraph) {
		HashMap<String, HashSet<String>> result = new HashMap<String, HashSet<String>>();

		HashSet<String> neighborList = localGraph.get(key);
		for (Iterator<String> iterator = neighborList.iterator(); iterator
				.hasNext();) {
			String neighbor = (String) iterator.next();
			if (localGraph.get(neighbor) != null) {
				result.put(neighbor,
						intersection(localGraph.get(neighbor), neighborList));
			} else {
				// remote access required
			}
		}
		return result;
	}

	private HashSet<String> intersection(HashSet<String> set1,
			HashSet<String> set2) {
		HashSet<String> intersection = new HashSet<String>(set1);
		intersection.retainAll(set2);
		return intersection;
	}

	public static void main(String[] args) {
		String[] nodes = new String[] { "localhost" };
		PCJ.deploy(DemonParallel.class, DemonParallel.class, nodes);

	}

}
