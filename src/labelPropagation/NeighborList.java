package labelPropagation;

import java.io.Serializable;
import java.util.HashSet;

import net.ontopia.utils.CompactHashSet;

public class NeighborList<T> implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Vertex<T> headVertex;
    private CompactHashSet<Vertex<T>> listOfNeighbors;

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

    public CompactHashSet<Vertex<T>> getListOfNeighbors() {
        return listOfNeighbors;
    }

    public void setListOfNeighbors(CompactHashSet<Vertex<T>> listOfNeighbors) {
        this.listOfNeighbors = listOfNeighbors;
    }

    public NeighborList(Vertex<T> headVertex, CompactHashSet<Vertex<T>> listOfNeighbors) {
        super();
        this.headVertex = headVertex;
        this.listOfNeighbors = listOfNeighbors;
    }

    public NeighborList() {
        super();
        // TODO Auto-generated constructor stub
    }
}
