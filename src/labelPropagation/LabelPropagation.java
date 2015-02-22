package labelPropagation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class LabelPropagation {
	private ArrayList<Node> communityList;
	private HashMap<String, HashSet<String>> map;

	public LabelPropagation() {
		super();
		// TODO Auto-generated constructor stub
	}

	public String findMostCommonlyUsedId(HashSet<String> egoMinusEgoNetwork) {
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

	public String getCommunityId(String nodeId) {
		/* returns the community id of node */
		for (Node v : communityList) {
			if (v.getRealId().equals(nodeId))
				return v.getCommunityId();
		}

		return null;
	}

	public ArrayList<Node> getCommunityList() {
		return communityList;
	}

	public HashMap<String, HashSet<String>> getMap() {
		return map;
	}

	public boolean initiliaze(HashMap<String, HashSet<String>> map) {
		if (map == null)
			return false;
		this.map = map;

		/*
		 * initially all nodes belong to themselves as community.
		 */
		communityList = new ArrayList<Node>(map.size());
		for (Entry<String, HashSet<String>> entry : map.entrySet()) {
			String key = entry.getKey();
			communityList.add(new Node(key, key));
		}
		return true;
	}

	public boolean isTerminated() {
		for (Node v : communityList) {
			if (findMostCommonlyUsedId(map.get(v.getRealId())) == v
					.getCommunityId()) {
				continue;/* do nothing */
			} else {
				return false;
			}
		}
		return true;
	}

	public void labelPropagation(Node v, HashSet<String> egoMinusEgoNetwork) {
		v.setCommunityId(findMostCommonlyUsedId(egoMinusEgoNetwork));
	}

	public void proceedLP() {
		do {
			Collections.shuffle(communityList);
			for (Node v : communityList) {
				HashSet<String> egoMinusEgo = map.get(v.getRealId());
				labelPropagation(v, egoMinusEgo);
			}
		} while (!isTerminated());

	}

	public void setCommunityList(ArrayList<Node> communityList) {
		this.communityList = communityList;
	}

	public void setMap(HashMap<String, HashSet<String>> map) {
		this.map = map;
	}
}
