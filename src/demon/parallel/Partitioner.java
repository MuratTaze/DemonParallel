package demon.parallel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.pcj.PCJ;

import labelPropagation.GraphLoader;
import labelPropagation.Vertex;

public class Partitioner {
	HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> graph;

	public Partitioner() {
		super();
	}

	public Partitioner(HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> graph) {
		this.graph = graph;
	}

	@SuppressWarnings("unchecked")
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
		/**
		 * for (Iterator<Entry<Vertex<Integer>, HashSet<Vertex<Integer>>>> it =
		 * graph .entrySet().iterator(); it.hasNext();) { Entry<Vertex<Integer>,
		 * HashSet<Vertex<Integer>>> entry = it.next();
		 * 
		 * for(HashSet<Vertex<Integer>> neighbors:graph.values()){
		 * for(Vertex<Integer> v:neighbors){ v.setThreadNumber(graph.); } }
		 * 
		 * if (entry.getKey().equals("test")) {
		 * 
		 * it.remove(); } }
		 **/

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

		Iterator<Entry<Vertex<Integer>, HashSet<Vertex<Integer>>>> it = graph
				.entrySet().iterator();
		Entry<Vertex<Integer>, HashSet<Vertex<Integer>>> entry = null;
		if (it.hasNext()) {
			entry = it.next();
		}
		// Adds to end of queue
		Vertex<Integer> startingVertex = covered(lookup);

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

}
