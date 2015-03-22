package labelPropagation;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.junit.Test;

public class AllTests {

    @SuppressWarnings("rawtypes")
    @Test
    public void testInitiliaze() {

	GraphLoader graph = null;
	try {
	    graph = new GraphLoader("traininGraph.txt");
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	Network[] egos = graph.partition(0, 1);
	LabelPropagation lp = new LabelPropagation(constructHashMap(egos), 100);
	lp.proceedLP();
	System.out.println("Label Propagation = " + lp.constructCommunities());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public HashMap<String, HashSet<String>> constructHashMap(Network[] egos) {
	HashMap<String, HashSet<String>> result = new HashMap<String, HashSet<String>>();
	for (int i = 0; i < egos.length; i++) {
	    if (egos[i] != null) {
		for (int j = 0; j < egos[i].getGraph().size(); j++) {

		    result.put(((NeighborList) (egos[i].getGraph().get(j)))
			    .getHeadVertex().getValue().toString(),
			    vertexToString(((NeighborList) (egos[i].getGraph()
			            .get(j))).getListOfNeighbors()));
		}
	    }

	}
	return result;
    }

    @SuppressWarnings("rawtypes")
    private HashSet<String> vertexToString(HashSet<Vertex> neighbors) {
	HashSet<String> result = new HashSet<String>();
	for (Iterator iterator = neighbors.iterator(); iterator.hasNext();) {
	    Vertex v = (Vertex) iterator.next();
	    result.add(v.getValue().toString());
	}
	return result;
    }
}
