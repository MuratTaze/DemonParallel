package labelPropagation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

public class LabelPropagation<T> {
    private HashMap<T, T> communites = new HashMap<T, T>();
    private HashMap<Vertex<T>, NeighborList<T>> network;/*
                                                         * due to speed issues
                                                         */

    public LabelPropagation() {
        super();
    }

    public LabelPropagation(Network<T> network) {
        super();
        this.network = network.getGraph();
        initiliaze(this.network);
    }

    /**
     * This method creates communities by using label information of each
     * vertex.
     * 
     * @return CommunityList object which contains a set of communities
     */
    public CommunityList<T> extractCommunities() {
        CommunityList<T> communities = new CommunityList<T>();
        for (Vertex<T> vertex : this.network.keySet()) {
            if (communities
                    .hasCommunity(this.communites.get(vertex.getValue()))) {
                communities.addMember(vertex,
                        this.communites.get(vertex.getValue()));
            } else {
                communities.createCommunity(vertex.getValue(),
                        this.communites.get(vertex.getValue()));
            }
        }

        return communities;

    }

    /**
     * For a given vertex and its neighbors, this method finds label of the most
     * frequent neighbor.
     * 
     * @param randomVertex
     *            vertex chosen randomly from the network
     * @param neighborList
     *            neighbors of the vertex
     * @return label of the most frequent neighbor
     */
    public T findMostCommonlyUsedId(Vertex<T> randomVertex,
            NeighborList<T> neighborList) {

        if (neighborList.getListOfNeighbors().size() != 0) {
            /* iterate over ego network and count community id's. */
            HashMap<T, Integer> countList = new HashMap<T, Integer>(
                    neighborList.getListOfNeighbors().size());
            for (Vertex<T> node : neighborList.getListOfNeighbors()) {

                if (countList.get(communites.get(node.getValue())) == null) {
                    countList.put(communites.get(node.getValue()), 1);
                } else {/*
                         * increment that label 's occurrences .
                         */
                    countList.put(communites.get(node.getValue()),
                            countList.get(communites.get(node.getValue())) + 1);
                }
            }
            /* ties are broken randomly */
            int max = Collections.max(countList.values());
            if (max == 1) {
                List<T> valuesList = new ArrayList<T>(countList.keySet());
                int randomIndex = new Random().nextInt(valuesList.size());
                T randomValue = valuesList.get(randomIndex);
                return randomValue;
            }
            /* return most commonly used id(community) of neighbors */
            for (Entry<T, Integer> entry : countList.entrySet()) {

                if (entry.getValue().equals(max)) {
                    return entry.getKey();
                }
            }
        }
        return communites.get(randomVertex.getValue());
    }

    public HashMap<Vertex<T>, NeighborList<T>> getNetwork() {
        return network;
    }

    /**
     * This method initializes all vertices with a unique label(id of each
     * vertex).
     * 
     * @param network
     *            graph
     * @return returns false if the network is null.
     */
    public boolean initiliaze(HashMap<Vertex<T>, NeighborList<T>> network) {
        if (network == null)
            return false;
        this.network = network;

        /*
         * initially all nodes belong to themselves as community.
         */

        for (Entry<Vertex<T>, NeighborList<T>> entry : this.network.entrySet()) {
            communites
                    .put(entry.getKey().getValue(), entry.getKey().getValue());
        }

        return true;
    }

    /**
     * This methods checks whether all vertices have the most commonly id of its
     * neighbors
     * 
     * @return returns true if all the vertices have label of its most frequent
     *         neighbor.
     */
    public boolean isTerminated() {

        for (Entry<Vertex<T>, NeighborList<T>> entry : this.network.entrySet()) {

            if (findMostCommonlyUsedId(entry.getKey(), entry.getValue()) == communites
                    .get(entry.getKey().getValue())) {
                continue;/* do nothing */
            } else {
                return false;
            }
        }
        return true;

    }

    /* the label propagation algorithm. */
    /**
     * This method works as the following: -first shuffles vertices. -then it
     * assigns label of corresponding the most frequent neighbor to label of
     * each vertex. -after all vertices are processed it checks termination
     * condition. If it satisfies then we are done otherwise we go back to
     * shuffling step and do the same things.
     */
    public void proceedLP() {
        List<Vertex<T>> vertices = new ArrayList<Vertex<T>>(
                this.network.keySet());

        do {
            shuffle(vertices);
            for (Vertex<T> vertex : vertices) {
                communites
                        .put(vertex.getValue(),
                                findMostCommonlyUsedId(vertex,
                                        this.network.get(vertex)));
            }
        } while (!isTerminated());
    }

    /**
     * This method places each vertex to a random location in list. In-place
     * shuffling.
     * 
     * @param vertices
     *            list of vertices
     */
    private void shuffle(List<Vertex<T>> vertices) {
        Random r = new Random();
        int i = vertices.size() - 1;
        while (i != 0) {
            int j = r.nextInt(i);
            Vertex<T> temp = vertices.get(j);
            vertices.set(j, vertices.get(i));
            vertices.set(i, temp);
            i--;
        }

    }

    public void setNetwork(HashMap<Vertex<T>, NeighborList<T>> network) {
        this.network = network;
    }
}
