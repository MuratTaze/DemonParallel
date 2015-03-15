package labelPropagation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class GraphLoader {
	private HashMap<String, HashSet<String>> map;

	public HashMap<String, HashSet<String>> getMap() {
		return map;
	}

	public void setMap(HashMap<String, HashSet<String>> map) {
		this.map = map;
	}

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

	public EgoNetwork[] partition(int threadId, int numberofThreads) {
		int counter = 0;
		int capacity = (int) Math.ceil(((double) map.size())
				/ ((double) numberofThreads));
		int start = (int) (capacity * threadId);
		int end = (int) (start + capacity);

		EgoNetwork[] egoNetworks = new EgoNetwork[(int) capacity];

		for (Entry<String, HashSet<String>> entry : map.entrySet()) {
			if (start <= counter && counter < end) {
				if (egoNetworks[hash(Integer.parseInt(entry.getKey()), capacity)] == null) {
					egoNetworks[hash(Integer.parseInt(entry.getKey()), capacity)] = new EgoNetwork();
				}
				HashMap<String, HashSet<String>> temp = new HashMap<String, HashSet<String>>();
				temp.put(entry.getKey(), entry.getValue());
				egoNetworks[hash(Integer.parseInt(entry.getKey()), capacity)]
						.getSubGraph().add(temp);

			}
			counter++;
		}
		return egoNetworks;
	}

	public int hash(int vertexId, int capacity) {

		return (vertexId & 0x7fffffff) % capacity;
	}
}
