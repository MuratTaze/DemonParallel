package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import labelPropagation.Vertex;

public class GraphLoader {
	/* vertices and neighbor lists */
	private HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> map;
	public static int numberOfElements;
	public static int edgeCount = 0;
	private HashMap<Integer, Vertex<Integer>> vertexExistenceTable;

	/* loads data to a hash map */
	public GraphLoader(String filename) throws IOException {
		super();

		map = new HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>>();/*
																		 * network
																		 * itself
																		 */
		vertexExistenceTable = new HashMap<Integer, Vertex<Integer>>();
		FileReader inputFile = new FileReader(filename);
		BufferedReader bufferReader = new BufferedReader(inputFile);
		String line;

		// Read file line by line and add them to hash map
		while ((line = bufferReader.readLine()) != null) {
			
			String[] values = line.split(" ");
			Integer arg1, arg2;
			arg1 = new Integer(values[0]);
			arg2 = new Integer(values[1]);
			Vertex<Integer> v1 = null, v2 = null;

			if (vertexExistenceTable.containsKey(arg1)) {
				v1 = vertexExistenceTable.get(arg1);
			} else {
				v1 = new Vertex<Integer>();
				v1.setValue(arg1);
				vertexExistenceTable.put(arg1, v1);
			}
			if (vertexExistenceTable.containsKey(arg2)) {
				v2 = vertexExistenceTable.get(arg2);
			} else {
				v2 = new Vertex<Integer>();
				v2.setValue(arg2);
				vertexExistenceTable.put(arg2, v2);
			}

			{
				if (map.get(v1) == null) {

					HashSet<Vertex<Integer>> neighborList = new HashSet<Vertex<Integer>>();
					neighborList.add(v2);edgeCount++;
					map.put(v1, neighborList);

				} else {
					map.get(v1).add(v2);edgeCount++;
				}

			}
			{
				if (map.get(v2) == null) {

					HashSet<Vertex<Integer>> neighborList = new HashSet<Vertex<Integer>>();
					neighborList.add(v1);
					map.put(v2, neighborList);

				} else {
					map.get(v2).add(v1);

				}
			}
		}
		numberOfElements = map.size();
		bufferReader.close();

	}

	public HashMap<Integer, Vertex<Integer>> getVertexExistenceTable() {
		return vertexExistenceTable;
	}

	public void setVertexExistenceTable(
			HashMap<Integer, Vertex<Integer>> vertexExistenceTable) {
		this.vertexExistenceTable = vertexExistenceTable;
	}

	
	public HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> getGraph() {
		return this.map;
	}

	public void setMap(HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> map) {
		this.map = map;
	}
}
