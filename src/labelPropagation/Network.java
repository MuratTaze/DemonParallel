package labelPropagation;

import java.util.HashMap;

public class Network<T> {

    private HashMap<Vertex<T>, NeighborList<T>> graph;

    public Network() {
        super();
    }

    public Network(HashMap<Vertex<T>, NeighborList<T>> network) {
        super();
        this.graph = network;
    }

    public HashMap<Vertex<T>, NeighborList<T>> getGraph() {
        return graph;
    }

    public void setGraph(HashMap<Vertex<T>, NeighborList<T>> network) {
        this.graph = network;
    }

    @Override
    public String toString() {
        return "Network [network=" + graph + "]";
    }

}
