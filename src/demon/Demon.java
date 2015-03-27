package demon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import labelPropagation.LabelPropagation;
import labelPropagation.NeighborList;
import labelPropagation.Network;
import labelPropagation.Vertex;

public class Demon {
    private CopyOnWriteArrayList<HashSet<String>> globalCommunities = null;

    public CopyOnWriteArrayList<HashSet<String>> getGlobalCommunities() {
	return globalCommunities;
    }

    public void setGlobalCommunities(
	    CopyOnWriteArrayList<HashSet<String>> globalCommunities) {
	this.globalCommunities = globalCommunities;
    }

    public Demon() {
	super();
	// TODO Auto-generated constructor stub
    }

    /*
     * two communities are merged if at least β fraction of the smaller
     * community resides in their intersection.
     */
    private void merge(HashSet<String> localCommunity, double mergeFactor) {
	if (globalCommunities == null) {
	    globalCommunities = new CopyOnWriteArrayList<HashSet<String>>();
	    globalCommunities.add(localCommunity);
	} else {
	    /* we check for each global community */
	    for (Iterator<HashSet<String>> iterator = globalCommunities
		    .iterator(); iterator.hasNext();) {
		HashSet<String> temp = iterator.next();
		HashSet<String> temp2 = new HashSet<String>(temp);
		double size1 = temp2.size();
		double size2 = localCommunity.size();
		double min = size1 < size2 ? size1 : size2;
		temp2.retainAll(localCommunity);
		if (temp2.size() / min >= mergeFactor) {
		    temp.addAll(localCommunity);// join them
		    return;
		}
	    }
	    globalCommunities.add(localCommunity);// as a seperate
	    // community

	}
    }

    /*
     * for a given vertex and a network this method constructs its ego minus ego
     * network.
     */
    private HashMap<String, HashSet<String>> egoMinusEgo(String key,
	    HashMap<String, HashSet<String>> localGraph) {
	HashMap<String, HashSet<String>> result = new HashMap<String, HashSet<String>>();

	HashSet<String> neighborList = localGraph.get(key);
	for (Iterator<String> iterator = neighborList.iterator(); iterator
	        .hasNext();) {
	    String neighbor = iterator.next();
	    /*
	     * fetch the neighbors of current neighbor. if it returns null we
	     * will do remote access
	     */
	    if (localGraph.get(neighbor) != null) {
		/*
	         * remove neighbors of current neighbor which are not included
	         * in neighborlist.
	         */
		result.put(neighbor,
		        intersection(localGraph.get(neighbor), neighborList));
	    } else {
		// remote access required. it will soon be implemented.
	    }
	}
	return result;
    }

    /* returns intersection of two sets as new set */
    private HashSet<String> intersection(HashSet<String> set1,
	    HashSet<String> set2) {
	HashSet<String> intersection = new HashSet<String>(set1);
	intersection.retainAll(set2);
	return intersection;
    }

    /*
     * constructs HashMap for a given Network array. we consider that we may
     * have collisions in Network array
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public HashMap<String, HashSet<String>> constructHashMap(Network[] egos) {
	HashMap<String, HashSet<String>> result = new HashMap<String, HashSet<String>>();
	for (int i = 0; i < egos.length; i++) {
	    for (int j = 0; j < egos[i].getGraph().size(); j++) {
		result.put(((NeighborList) (egos[i].getGraph().get(j)))
		        .getHeadVertex().getValue().toString(),
		        vertexToString(((NeighborList) (egos[i].getGraph()
		                .get(j))).getListOfNeighbors()));
	    }

	}
	return result;
    }

    /* converts given set of vertices to a set of strings */
    @SuppressWarnings("rawtypes")
    private HashSet<String> vertexToString(HashSet<Vertex> neighbors) {
	HashSet<String> result = new HashSet<String>();
	for (Iterator iterator = neighbors.iterator(); iterator.hasNext();) {
	    Vertex v = (Vertex) iterator.next();
	    result.add(v.getValue().toString());
	}
	return result;
    }

    /* demon algorithm. */
    @SuppressWarnings("rawtypes")
    public void execute(Network[] graph, double mergeFactor) throws IOException {
	HashMap<String, HashSet<String>> map = constructHashMap(graph);
	for (Entry<String, HashSet<String>> entry : map.entrySet()) {
	    /*
	     * for each vertex we get its ego minus ego network and perform
	     * label propagation on it
	     */
	    HashMap<String, HashSet<String>> e = egoMinusEgo(entry.getKey(),
		    map);

	    LabelPropagation lp = new LabelPropagation();
	    lp.initiliaze(e);
	    lp.proceedLP();

	    /* get local communities found by label propagation */
	    ArrayList<HashSet<String>> localCommunities = lp
		    .constructCommunities();

	    /* merge each local communities found with global communities */
	    for (Iterator<HashSet<String>> iterator = localCommunities
		    .iterator(); iterator.hasNext();) {
		HashSet<String> localCommunity = iterator.next();
		localCommunity.add(entry.getKey());
		merge(localCommunity, mergeFactor);
	    }

	}

    }
}
