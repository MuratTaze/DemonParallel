package demon.parallel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import labelPropagation.Vertex;
import utils.GraphLoader;

import org.pcj.PCJ;

public class Partitioner {
	HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> graph;

	public Partitioner() {
		super();
	}

	public Partitioner(HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> graph) {
		this.graph = graph;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public HashMap[] randomPartitioning() {
		HashMap[] partitions = new HashMap[PCJ.threadCount()];

		/* create empty partitions */
		int capacity = GraphLoader.numberOfElements / PCJ.threadCount();
		int remaining = GraphLoader.numberOfElements % PCJ.threadCount();
		for (int i = 0; i < PCJ.threadCount() - 1; i++) {

			partitions[i] = new HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>>(
					capacity);

		}
		partitions[PCJ.threadCount() - 1] = new HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>>(
				capacity + remaining);

		List<Map.Entry<Vertex<Integer>, HashSet<Vertex<Integer>>>> list = new ArrayList<Map.Entry<Vertex<Integer>, HashSet<Vertex<Integer>>>>(
				graph.entrySet());
		graph.clear();
		// each time you want a different order.
		Collections.shuffle(list);
		int load = 0;
		int thread = 0;

		/*
		 * bu kýsmý yani partitionlarý threadlere assign etme kýsmýný bir
		 * fonksiyona yaptýralým.
		 */
		for (Map.Entry<Vertex<Integer>, HashSet<Vertex<Integer>>> entry : list) {

			entry.getKey().setThreadNumber(thread);

			load++;
			graph.put(entry.getKey(), entry.getValue());
			partitions[thread].put(entry.getKey(), entry.getValue());
			if (load % capacity == 0) {

				if ((PCJ.threadCount() - 1) == thread) {
					;
				} else
					thread++;
			}
		}
		return partitions;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public HashMap[] bfsPartitioning() {

		// Since queue is a interface
		Queue<Vertex<Integer>> queue = new LinkedList<Vertex<Integer>>();

		ArrayList<Vertex<Integer>> partitionList = new ArrayList<Vertex<Integer>>();

		HashMap<Vertex<Integer>, Boolean> lookup = new HashMap<Vertex<Integer>, Boolean>();// visited
																							// or
																							// not?

		for (Vertex<Integer> v : graph.keySet()) {
			lookup.put(v, false);
		}

		// Adds to end of queue
		Vertex<Integer> startingVertex = mostPopularVertex();

		while (startingVertex != null) {
			applyBFS(startingVertex, queue, lookup, partitionList);// start from
																	// a given
																	// vertex
																	// apply
																	// breadth
																	// first
																	// search
			startingVertex = covered(lookup);
		}
		HashMap[] partitions = new HashMap[PCJ.threadCount()];

		/* create empty partitions */
		int capacity = GraphLoader.numberOfElements / PCJ.threadCount();
		int remaining = GraphLoader.numberOfElements % PCJ.threadCount();
		for (int i = 0; i < PCJ.threadCount() - 1; i++) {

			partitions[i] = new HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>>(
					capacity);

		}
		partitions[PCJ.threadCount() - 1] = new HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>>(
				capacity + remaining);
		int load = 0;
		int thread = 0;
		for (Vertex<Integer> vrtx : partitionList) {

			vrtx.setThreadNumber(thread);

			load++;

			partitions[thread].put(vrtx, graph.get(vrtx));
			if (load % capacity == 0) {

				if ((PCJ.threadCount() - 1) == thread) {
					;
				}

				else
					thread++;
			}
		}

		return partitions;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public HashMap[] degreeBalancedBfsPartitioning() {

		// Since queue is a interface
		Queue<Vertex<Integer>> queue = new LinkedList<Vertex<Integer>>();

		ArrayList<Vertex<Integer>> partitionList = new ArrayList<Vertex<Integer>>();

		HashMap<Vertex<Integer>, Boolean> lookup = new HashMap<Vertex<Integer>, Boolean>();// visited
		int totalDegree = 0; // or
		// not?

		for (Vertex<Integer> v : graph.keySet()) {
			lookup.put(v, false);
			totalDegree += graph.get(v).size();
		}
		int averageDegree = totalDegree / PCJ.threadCount();

		// Adds to end of queue
		Vertex<Integer> startingVertex = mostPopularVertex();

		while (startingVertex != null) {
			applyBFS(startingVertex, queue, lookup, partitionList);// start from
																	// a given
																	// vertex
																	// apply
																	// breadth
																	// first
																	// search
			startingVertex = covered(lookup);
		}
		HashMap[] partitions = new HashMap[PCJ.threadCount()];

		/* create empty partitions */
		int capacity = GraphLoader.numberOfElements / PCJ.threadCount();
		int remaining = GraphLoader.numberOfElements % PCJ.threadCount();
		for (int i = 0; i < PCJ.threadCount() - 1; i++) {

			partitions[i] = new HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>>(
					capacity);

		}
		partitions[PCJ.threadCount() - 1] = new HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>>(
				capacity + remaining);

		int thread = 0;
		int threadDegree = 0;
		int vertexDegree = 0;
		for (Vertex<Integer> vrtx : partitionList) {

			vertexDegree = graph.get(vrtx).size();
			threadDegree += vertexDegree;

			vrtx.setThreadNumber(thread);

			partitions[thread].put(vrtx, graph.get(vrtx));
			if (threadDegree >= averageDegree) {
				threadDegree = 0;
				if ((PCJ.threadCount() - 1) == thread) {
					;
				}

				else {
					thread++;

				}
			}
		}

		return partitions;

	}

	private Vertex<Integer> mostPopularVertex() {
		int highestDegreee = 0;
		Vertex<Integer> popularVertex = null;
		for (Vertex<Integer> v : graph.keySet()) {
			if (graph.get(v).size() > highestDegreee) {
				highestDegreee = graph.get(v).size();
				popularVertex = v;
			}
		}
		return popularVertex;
	}

	// starting vertex yani root en baþta nasýl seçiliyor?
	private Vertex<Integer> covered(HashMap<Vertex<Integer>, Boolean> lookup) {
		Vertex<Integer> uncovered = null;

		for (Vertex<Integer> v : lookup.keySet()) {
			if (lookup.get(v) == false) {
				uncovered = v;
				break;
			}

		}
		return uncovered;
	}

	private void applyBFS(Vertex<Integer> key, Queue<Vertex<Integer>> queue,
			HashMap<Vertex<Integer>, Boolean> lookup,
			ArrayList<Vertex<Integer>> partitionList) {
		queue.add(key);
		lookup.put(key, true);
		while (!queue.isEmpty()) {
			// removes from front of queue
			Vertex<Integer> r = queue.remove();
			partitionList.add(r);

			// Visit child first before grandchild
			for (Vertex<Integer> n : graph.get(r)) {
				if (lookup.get(n) == false) {
					queue.add(n);
					lookup.put(n, true);
				}
			}
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public HashMap[] metisPartitioning(String filename,
			HashMap<Integer, Vertex<Integer>> vertexExistenceTable)
			throws NumberFormatException, IOException {
		HashMap[] partitions = new HashMap[PCJ.threadCount()];

		/* create empty partitions */
		int capacity = GraphLoader.numberOfElements / PCJ.threadCount();
		int remaining = GraphLoader.numberOfElements % PCJ.threadCount();
		for (int i = 0; i < PCJ.threadCount() - 1; i++) {

			partitions[i] = new HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>>(
					capacity);

		}
		partitions[PCJ.threadCount() - 1] = new HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>>(
				capacity + remaining);

		Integer[] vertexList = new Integer[graph.size()];
		int abc = 0;
		for (Vertex<Integer> vertex : graph.keySet()) {
			vertexList[abc] = vertex.getValue();
			abc++;
		}

		Arrays.sort(vertexList);

		int thread;
		FileReader inputFile = new FileReader(filename);
		BufferedReader bufferReader = new BufferedReader(inputFile);

		for (Integer key : vertexList) {
			thread = Integer.parseInt(bufferReader.readLine());
			vertexExistenceTable.get(key).setThreadNumber(thread);

			partitions[thread].put(vertexExistenceTable.get(key),
					graph.get(vertexExistenceTable.get(key)));

		}
		bufferReader.close();
		return partitions;
	}

}
