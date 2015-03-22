package labelPropagation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

public class GraphLoader {
    /*vertices and neighbor lists*/
    private HashMap<String, HashSet<String>> map;

    public HashMap<String, HashSet<String>> getMap() {
	return map;
    }

    public void setMap(HashMap<String, HashSet<String>> map) {
	this.map = map;
    }

    /* loads data to a hash map */
    public GraphLoader(String filename) throws IOException {
	super();
	// TODO Auto-generated constructor stub

	map = new HashMap<String, HashSet<String>>();/*
			                              * network itself
			                              */

	// Create object of FileReader
	FileReader inputFile = new FileReader(filename);

	// Instantiate the BufferedReader Class
	BufferedReader bufferReader = new BufferedReader(inputFile);

	String line;

	// Read file line by line and add them to hashmap
	while ((line = bufferReader.readLine()) != null) {
	    String[] values = line.split("	");

	    if (map.get(values[0]) == null) {

		map.put(values[0], new HashSet<String>());
		map.get(values[0]).add(values[1]);
	    } else {
		map.get(values[0]).add(values[1]);
	    }

	    if (map.get(values[1]) == null) {
		map.put(values[1], new HashSet<String>());
		map.get(values[1]).add(values[0]);
	    } else {
		map.get(values[1]).add(values[0]);
	    }

	}
	// Close the buffer reader
	bufferReader.close();

    }

    /*
     * this method partitions the entire network to a smaller one w.r.t total
     * thread numbers and calling thread. It returns an hashed array of Network
     * for the sake of remote access
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Network[] partition(int threadId, int numberofThreads) {
	int counter = 0;
	int capacity = (int) Math.ceil(((double) map.size())
	        / ((double) numberofThreads));
	int start = (int) (capacity * threadId);
	int end = (int) (start + capacity);

	Network[] egoNetworks = new Network[(int) capacity];

	for (Entry<String, HashSet<String>> entry : map.entrySet()) {
	    if (start <= counter && counter < end) {
		if (egoNetworks[hash(Integer.parseInt(entry.getKey()), capacity)] == null) {
		    egoNetworks[hash(Integer.parseInt(entry.getKey()), capacity)] = new Network<Object>();
		}

		NeighborList<Integer> list = new NeighborList<Integer>();
		list.setHeadVertex(new Vertex<Integer>(Integer.parseInt(entry
		        .getKey())));
		list.setListOfNeighbors(stringToVertex(entry.getValue()));

		egoNetworks[hash(Integer.parseInt(entry.getKey()), capacity)]
		        .getGraph().add(list);

	    }
	    counter++;
	}

	return egoNetworks;
    }

    /* convert HashSet<String> to HashSet<Vertex> */
    private HashSet<Vertex<Integer>> stringToVertex(HashSet<String> neighbors) {
	HashSet<Vertex<Integer>> result = new HashSet<Vertex<Integer>>();
	for (Iterator<String> iterator = neighbors.iterator(); iterator
	        .hasNext();) {
	    String string = (String) iterator.next();
	    result.add(new Vertex<Integer>(Integer.parseInt(string)));
	}
	return result;
    }

    public int hash(Integer vertexId, int capacity) {

	return vertexId.hashCode() % capacity;
    }
}
