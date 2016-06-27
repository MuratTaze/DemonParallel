package labelPropagation;

import java.io.Serializable;
import java.util.HashSet;

public class NeighborList<T> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Vertex<T> headVertex;
	private HashSet<Vertex<T>> listOfNeighbors;

	@Override
	public String toString() {
		String result = "";
		result = headVertex.toString();
		result += "-->";
		for (Vertex<T> v : listOfNeighbors)
			result += v.toString() + ",";
		return result.substring(0, result.length() - 1);
		
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
