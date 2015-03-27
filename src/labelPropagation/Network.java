package labelPropagation;

import java.util.ArrayList;

public class Network<T> {
    /*here we use array list of neighbor list due to the collision which may occur during hashing.*/
    private ArrayList<NeighborList<T>> graph;

    public Network() {
	super();
	// TODO Auto-generated constructor stub
	graph = new ArrayList<NeighborList<T>>();
    }

    public ArrayList<NeighborList<T>> getGraph() {
	return graph;
    }

    public void setGraph(ArrayList<NeighborList<T>> subGraph) {
	this.graph = subGraph;
    }

    
    @Override
    public String toString() {
	return "EgoNetwork [subGraph=" + graph + "]";
    }
}
