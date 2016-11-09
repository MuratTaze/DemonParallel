package demon.parallel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
		for (Map.Entry<Vertex<Integer>, HashSet<Vertex<Integer>>> entry : list) {

			entry.getKey().setThreadNumber(thread);

			load++;
			graph.put(entry.getKey(), entry.getValue());
			partitions[thread].put(entry.getKey(), entry.getValue());
			if (load % capacity == 0) {

				if ((PCJ.threadCount()-1 )== thread)
				thread=thread;
				else
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
}
