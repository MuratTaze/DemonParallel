package labelPropagation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;




public class GraphLoader {
	/* vertices and neighbor lists */
	private HashMap<Vertex<Integer>, NeighborList<Integer>> map;

	public static int numberOfElements;

	/* loads data to a hash map */
	public GraphLoader(String filename) throws IOException {
		super();
		// TODO Auto-generated constructor stub

		map = new HashMap<Vertex<Integer>, NeighborList<Integer>>();/*
																	 * network
																	 * itself
																	 */

		// Create object of FileReader
		FileReader inputFile = new FileReader(filename);

		// Instantiate the BufferedReader Class
		BufferedReader bufferReader = new BufferedReader(inputFile);

		String line;

		// Read file line by line and add them to hashmap
		while ((line = bufferReader.readLine()) != null) {
			String[] values = line.split(" ");
			Vertex<Integer> arg1, arg2;

			arg1 = new Vertex<Integer>();

			arg1.setValue(new Integer(values[0]));

			arg2 = new Vertex<Integer>();

			arg2.setValue(new Integer(values[1]));

			if (map.get(arg1) == null) {

				NeighborList<Integer> neighborList = new NeighborList<Integer>(
						arg1, new HashSet<Vertex<Integer>>());
				neighborList.getListOfNeighbors().add(arg2);
				map.put(arg1, neighborList);

			} else {
				map.get(arg1).getListOfNeighbors().add(arg2);

			}
			if (map.get(arg2) == null) {

				NeighborList<Integer> neighborList = new NeighborList<Integer>(
						arg2, new HashSet<Vertex<Integer>>());
				neighborList.getListOfNeighbors().add(arg1);
				map.put(arg2, neighborList);

			} else {
				map.get(arg2).getListOfNeighbors().add(arg1);

			}

		}
		// Close the buffer reader
		bufferReader.close();
		numberOfElements = map.size();
	}

	

	public Network<Integer> getNetwork() {
		return new Network<Integer>(this.map);
	}

	public void setMap(HashMap<Vertex<Integer>, NeighborList<Integer>> map) {
		this.map = map;
	}
}
