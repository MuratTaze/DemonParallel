package demon.parallel;

import java.io.Serializable;
import java.util.ArrayList;

import labelPropagation.NeighborList;
import labelPropagation.Vertex;

public class ResponsePacket<T> implements Serializable{
	private ArrayList<NeighborList<T>>  neighborLists;
	private ArrayList<NeighborList<T>> connections;

	public ResponsePacket() {
		super();
		neighborLists=new ArrayList<NeighborList<T>> ();
		connections=new ArrayList<NeighborList<T>> ();
	}

	public ResponsePacket(ArrayList<NeighborList<T>>  neighborLists, ArrayList<NeighborList<T>> connections) {
		super();
		this.neighborLists = neighborLists;
		this.connections = connections;
	}

	public ArrayList<NeighborList<T>> getNeighborLists() {
		return neighborLists;
	}

	public void setNeighborLists(ArrayList<NeighborList<T>>  neighborLists) {
		this.neighborLists = neighborLists;
	}

	public ArrayList<NeighborList<T>> getConnections() {
		return connections;
	}

	public void setConnections(ArrayList<NeighborList<T>> connections) {
		this.connections = connections;
	}

}
