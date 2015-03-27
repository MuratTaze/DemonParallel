package labelPropagation;

import java.util.HashSet;

public class NeighborList<T> {
    private Vertex<T> headVertex;
    private HashSet<Vertex<T>> listOfNeighbors;

    @Override
    public String toString() {
	return "NeighborList [headVertex=" + headVertex + ", listOfNeighbors="
	        + listOfNeighbors + "]";
    }

    public Vertex<T> getHeadVertex() {
	return headVertex;
    }

    public void setHeadVertex(Vertex<T> headVertex) {
	this.headVertex = headVertex;
    }

    public HashSet<Vertex<T>> getListOfNeighbors() {
	return listOfNeighbors;
    }

    public void setListOfNeighbors(HashSet<Vertex<T>> listOfNeighbors) {
	this.listOfNeighbors = listOfNeighbors;
    }

    public NeighborList(Vertex<T> headVertex, HashSet<Vertex<T>> listOfNeighbors) {
	super();
	this.headVertex = headVertex;
	this.listOfNeighbors = listOfNeighbors;
    }

    public NeighborList() {
	super();
	// TODO Auto-generated constructor stub
    }
}
