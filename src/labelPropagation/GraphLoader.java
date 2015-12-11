package labelPropagation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.pcj.PCJ;

public class GraphLoader {
	/* vertices and neighbor lists */
	private HashMap<Vertex<Integer>, NeighborList<Integer>> map;
	private Set<Integer> vertices;
	public static int numberOfElements;

	/* loads data to a hash map */
	public GraphLoader(String filename) throws IOException {
		super();
		// TODO Auto-generated constructor stub
		vertices = new HashSet<Integer>();
		initialize(filename);
		System.err.println(vertices.size());
		map = new HashMap<Vertex<Integer>, NeighborList<Integer>>();/*
																	 * network
																	 * itself
																	 */
		FileReader inputFile = new FileReader(filename);
		// Instantiate the BufferedReader Class
		BufferedReader bufferReader = new BufferedReader(inputFile);
		int numberOfThreads = PCJ.threadCount();
		int arraySize = (numberOfElements / numberOfThreads) + 1;
		String line;

		// Read file line by line and add them to hashmap
		while ((line = bufferReader.readLine()) != null) {
			String[] values = line.split(" ");
			Integer arg1, arg2;
			arg1 = new Integer(values[0]);
			arg2 = new Integer(values[1]);

			int hashCode1 = arg1 % numberOfElements;
			int index1 = hashCode1 / arraySize;
			int hashCode2 = arg2 % numberOfElements;
			int index2 = hashCode2 / arraySize;

			Vertex<Integer> v1, v2;

			v1 = new Vertex<Integer>();

			v1.setValue(arg1);

			v2 = new Vertex<Integer>();

			v2.setValue(arg2);
			if (index1 == PCJ.myId()) {
				if (map.get(v1) == null) {

					NeighborList<Integer> neighborList = new NeighborList<Integer>(
							v1, new HashSet<Vertex<Integer>>());
					neighborList.getListOfNeighbors().add(v2);
					map.put(v1, neighborList);

				} else {
					map.get(v1).getListOfNeighbors().add(v2);

				}
				
			}
			if (index2 == PCJ.myId()) {
				if (map.get(v2) == null) {

					NeighborList<Integer> neighborList = new NeighborList<Integer>(
							v2, new HashSet<Vertex<Integer>>());
					neighborList.getListOfNeighbors().add(v1);
					map.put(v2, neighborList);

				} else {
					map.get(v2).getListOfNeighbors().add(v1);

				}
			}
		}

		bufferReader.close();

	}

	private void initialize(String filename) throws NumberFormatException,
			IOException {
		// Create object of FileReader
		FileReader inputFile = new FileReader(filename);

		// Instantiate the BufferedReader Class
		BufferedReader bufferReader = new BufferedReader(inputFile);

		String line;

		// Read file line by line and add them to hashset
		while ((line = bufferReader.readLine()) != null) {
			String[] values = line.split(" ");
			Integer arg1, arg2;
			arg1 = new Integer(values[0]);
			arg2 = new Integer(values[1]);
			vertices.add(arg1);
			vertices.add(arg2);
		}
		bufferReader.close();
		numberOfElements = vertices.size();

	}

	public Network<Integer> getNetwork() {
		return new Network<Integer>(this.map);
	}

	public void setMap(HashMap<Vertex<Integer>, NeighborList<Integer>> map) {
		this.map = map;
	}
}
