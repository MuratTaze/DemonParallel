import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class SingleThreadedNonOverlapping {

	/*
	 * 1: for all v in V do 
	 * 2: e <- EgoMinusEgo(v, G) 
	 * 3: C(v) <- LabelPropagation(e) 
	 * 4: 	for all C in C(v) do 
	 * 5: 		C <- C U v 
	 * 6: 		C <- Merge(C,C, delta) 
	 * 7: 	end for 
	 * 8: end for
	 * 9: return C
	 */

	/*
	 * this map stores id of each node and its community
	 */
	private ArrayList<Node> communityList;	
	
	
	public static void main(String[] args) throws IOException {
		Dataset data = new Dataset("Email-Enron.txt");
		HashMap<String, HashSet<String>> map = data.map;
		initiliaze(map);
		do {
			Collections.shuffle(communityList);
			for (Node v : communityList) {
				HashSet<String> egoMinusEgo = map.get(v.getRealId());
				labelPropagation(v, egoMinusEgo);
			}
		} while(!isTerminated(communityList, map));
	}

	private static void initiliaze(HashMap<String, HashSet<String>> map) {
		/*
		 * initially all nodes belong to themselves as community.
		 */
		communityList = new ArrayList<Node>(map.size());
		for (Entry<String, HashSet<String>> entry : map.entrySet()) {
			String key = entry.getKey();
			communityList.add(new Node(key, key));
		}

	}

	private static void labelPropagation(Node v,
			HashSet<String> egoMinusEgoNetwork) {
		v.setCommunityId(findMostCommonlyUsedId(egoMinusEgoNetwork));
	}

	private static String findMostCommonlyUsedId(
			HashSet<String> egoMinusEgoNetwork) {
		/* iterate over ego minus ego network and count labels(communities) */
		HashMap<String, Integer> countList = new HashMap<String, Integer>(
				egoMinusEgoNetwork.size());
		for (String nodeId : egoMinusEgoNetwork) {
			String communityId = getCommunityId(nodeId);
			if (countList.get(communityId) == null) {
				countList.put(communityId, 1);
			} else {
				countList.put(communityId, countList.get(communityId) + 1);
			}
		}
		/* return most commonly used id(community) of neighbors */
		for (Entry<String, Integer> entry : countList.entrySet()) {
			if (entry.getValue().equals(Collections.max(countList.values())))
				return getCommunityId(entry.getKey());
		}

		return null;
	}

	private static String getCommunityId(String nodeId) {
		/* returns the community id of node */
		for (Node v : communityList) {
			if (v.getRealId().equals(nodeId))
				return v.getCommunityId();
		}

		return null;
	}
}
