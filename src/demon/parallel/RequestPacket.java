package demon.parallel;

import java.io.Serializable;
import java.util.ArrayList;

import labelPropagation.NeighborList;
import labelPropagation.Vertex;

public class RequestPacket<T> implements Serializable{
	private ArrayList<Vertex<T>> neighborlistQuery;
	private ArrayList<NeighborList<T>> connectionListQuery;

	public RequestPacket() {
		super();
		connectionListQuery = new ArrayList<NeighborList<T>>();
		neighborlistQuery = new ArrayList<Vertex<T>>();
	}

	public ArrayList<Vertex<T>> getNeighborlistQuery() {
		return neighborlistQuery;
	}

	public void setNeighborlistQuery(ArrayList<Vertex<T>> neighborlistQuery) {
		this.neighborlistQuery = neighborlistQuery;
	}

	public ArrayList<NeighborList<T>> getConnectionListQuery() {
		return connectionListQuery;
	}

	public void setConnectionListQuery(ArrayList<NeighborList<T>> connectionListQuery) {
		this.connectionListQuery = connectionListQuery;
	}

}
