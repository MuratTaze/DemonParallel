package labelPropagation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

public class LabelPropagation {
    private ArrayList<Node> vertexList;
    private ArrayList<HashSet<String>> communities;
    private HashMap<String, HashSet<String>> map;/* due to speed issues */
    private int t;/* number of iterations */

    public LabelPropagation() {
	super();
	t = 100;/* max # of iterations as default */
    }

    public LabelPropagation(HashMap<String, HashSet<String>> map, int t) {
	super();
	this.map = map;
	this.t = t;
	initiliaze(map);
    }

    public String findMostCommonlyUsedId(String vertex,
	    HashSet<String> neighbors) {
	if (neighbors.size() != 0) {
	    /* iterate over ego network and count community id's. */
	    HashMap<String, Integer> countList = new HashMap<String, Integer>(
		    neighbors.size());
	    for (String nodeId : neighbors) {
		String communityId = getCommunityId(nodeId);
		if (countList.get(communityId) == null) {
		    countList.put(communityId, 1);
		} else {/*
		         * increment that label 's occurrences .
		         */
		    countList.put(communityId, countList.get(communityId) + 1);
		}
	    }
	    /* ties are broken randomly */
	    int max = Collections.max(countList.values());
	    if (max == 1) {
		List<String> valuesList = new ArrayList<String>(
		        countList.keySet());
		int randomIndex = new Random().nextInt(valuesList.size());
		String randomValue = valuesList.get(randomIndex);
		return randomValue;
	    }
	    /* return most commonly used id(community) of neighbors */
	    for (Entry<String, Integer> entry : countList.entrySet()) {

		if (entry.getValue().equals(max)) {
		    return entry.getKey();
		}
	    }
	}
	return vertex;
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

	return true;
    }

    /* check whether all vertices have the most commonly id of its neighbors */
    public boolean isTerminated() {
	for (Node v : vertexList) {
	    if (findMostCommonlyUsedId(v.getRealId(), map.get(v.getRealId())) == v
		    .getCommunityId()) {
		continue;/* do nothing */
	    } else {
		return false;
	    }
	}
	return true;
    }

    /* returns found communities as an arraylist of hashsets. */
    public ArrayList<HashSet<String>> constructCommunities() {

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

    }

    /* the label propagation algorithm. */
    public void proceedLP() {
	int numberOfIterations = 0;
	do {
	    Collections.shuffle(vertexList);
	    for (Node v : vertexList) {
		HashSet<String> egoNetwork = map.get(v.getRealId());
		v.setCommunityId(findMostCommonlyUsedId(v.getRealId(),
		        egoNetwork));
	    }
	    numberOfIterations++;
	} while ((!isTerminated()) && (numberOfIterations != t));

    }

    public void setCommunityList(ArrayList<Node> communityList) {
	this.vertexList = communityList;
    }

    public void setMap(HashMap<String, HashSet<String>> map) {
	this.map = map;
    }

    class Node implements Serializable, Comparable<Node> {
	/**
		 * 
		 */
	private static final long serialVersionUID = 4010598725887800685L;
	private String realId;
	private String communityId;

	public Node() {
	    super();
	}

	public Node(String realId, String communityId) {
	    super();
	    this.realId = realId;
	    this.communityId = communityId;
	}

	@Override
	public String toString() {
	    return "Node [realId=" + realId + ", communityId=" + communityId
		    + "]";
	}

	public String getCommunityId() {
	    return communityId;
	}

	public String getRealId() {
	    return realId;
	}

	public void setCommunityId(String communityId) {
	    this.communityId = communityId;
	}

	public void setRealId(String realId) {
	    this.realId = realId;
	}

	@Override
	public int compareTo(Node o) {
	    return this.getCommunityId().compareTo(o.getCommunityId());
	}
    }

}
