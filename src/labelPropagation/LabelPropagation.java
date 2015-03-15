package labelPropagation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class LabelPropagation {
	private ArrayList<Node> vertexList;
	private ArrayList<HashSet<String>> communities;
	private HashMap<String, HashSet<String>> map;
	private int t;
	private int numberOfIterations;

	public LabelPropagation() {
		super();
		t = 100;/* max # of iterations as default */
	}

	public LabelPropagation(HashMap<String, HashSet<String>> map, int t) {
		super();
		this.map = map;
		this.t = t;
	}

	public String findMostCommonlyUsedId(String id,HashSet<String> egoMinusEgoNetwork) {
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

		return id;
	}

	public String getCommunityId(String nodeId) {
		/* returns the community id of node */
		for (Node v : vertexList) {
			if (v.getRealId().equals(nodeId))
				return v.getCommunityId();
		}

		return null;
	}

	public ArrayList<Node> getCommunityList() {
		return vertexList;
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
		vertexList = new ArrayList<Node>(map.size());
		for (Entry<String, HashSet<String>> entry : map.entrySet()) {
			String key = entry.getKey();
			vertexList.add(new Node(key, key));
		}
		numberOfIterations = 0;
		return true;
	}

	public boolean isTerminated() {
		for (Node v : vertexList) {
			if (findMostCommonlyUsedId(v.getRealId(),map.get(v.getRealId())) == v
					.getCommunityId()) {
				continue;/* do nothing */
			} else {
				return false;
			}
		}
		return true;
	}

	public ArrayList<HashSet<String>> constructCommunities() {
		if (isTerminated()) {
			communities = new ArrayList<HashSet<String>>();
			Collections.sort(getCommunityList());
			String previousCommunity = null;
			HashSet<String> community = new HashSet<String>();
			for (Node v : getCommunityList()) {
				if (previousCommunity == null) {
					community.add(v.getRealId());
				} else {
					if (v.getCommunityId() == previousCommunity) {
						community.add(v.getRealId());
					} else {
						communities.add(community);
						community = new HashSet<String>();
						community.add(v.getRealId());
					}
				}
				previousCommunity = v.getCommunityId();
			}
			communities.add(community);
			return communities;

		} else {
			return null;
		}
	}

	public void labelPropagation(Node v, HashSet<String> egoMinusEgoNetwork) {
	
		v.setCommunityId(findMostCommonlyUsedId(v.getRealId(),egoMinusEgoNetwork));
	}

	public void proceedLP() {
		do {
			Collections.shuffle(vertexList);
			for (Node v : vertexList) {
				HashSet<String> egoMinusEgo = map.get(v.getRealId());
				labelPropagation(v, egoMinusEgo);
			}
			numberOfIterations++;
		} while ((!isTerminated()) || (numberOfIterations != t));

	}

	public void setCommunityList(ArrayList<Node> communityList) {
		this.vertexList = communityList;
	}

	public void setMap(HashMap<String, HashSet<String>> map) {
		this.map = map;
	}
}
