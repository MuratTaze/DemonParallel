package demon.parallel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.pcj.PCJ;

import labelPropagation.NeighborList;
import labelPropagation.Network;
import labelPropagation.Vertex;

public class Indexer<T> {
    private Network<T> localNetwork = null;

    public Indexer() {
        super();

    }

    public Network<T> getLocalNetwork() {
        return localNetwork;
    }

    public ArrayList<NeighborList<T>>[] index(Network<T> network) {
        int threadId = PCJ.myId();
        int numberOfThreads = PCJ.threadCount();
        int numberOfVertices = network.getGraph().size();
        int arraySize = (numberOfVertices / numberOfThreads) + 1;
        int firstIndex = threadId * arraySize;
        int lastIndex = firstIndex + arraySize - 1;
        localNetwork = new Network<T>(new HashMap<Vertex<T>, NeighborList<T>>());
        @SuppressWarnings("unchecked")
        ArrayList<NeighborList<T>>[] array = new ArrayList[arraySize];

        for (Entry<Vertex<T>, NeighborList<T>> entry : network.getGraph()
                .entrySet()) {
            int hashCode = hash(entry.getKey().hashCode(), numberOfVertices + 1);
            if ((hashCode >= firstIndex) && (hashCode <= lastIndex)) {
                localNetwork.getGraph().put(entry.getKey(), entry.getValue());
                addElementToArray(entry.getValue(), array,
                        hash(entry.getKey().hashCode(), arraySize));
            }
        }
        return array;

    }

    private int hash(int key, int m) {
        return key % m;
    }

    private void addElementToArray(NeighborList<T> element,
            ArrayList<NeighborList<T>>[] array, int index) {
        if (array[index] != null) {
            array[index].add(element);
        } else {
            array[index] = new ArrayList<NeighborList<T>>();
            array[index].add(element);
        }
    }
}
