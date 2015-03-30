package labelPropagation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

public class LabelPropagation<T> {

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

    public CommunityList<T> extractCommunities() {
        CommunityList<T> communities = new CommunityList<T>();
        for (Vertex<T> vertex : this.network.keySet()) {
            if (communities.hasCommunity(vertex)) {
                communities.addMember(vertex);
            } else {
                communities.createCommunity(vertex);
            }
        }

        return communities;

    }

    public T findMostCommonlyUsedId(Vertex<T> randomVertex,
            NeighborList<T> neighborList) {
        if (neighborList.getListOfNeighbors().size() != 0) {
            /* iterate over ego network and count community id's. */
            HashMap<T, Integer> countList = new HashMap<T, Integer>(
                    neighborList.getListOfNeighbors().size());
            for (Vertex<T> node : neighborList.getListOfNeighbors()) {

                if (countList.get(node.getLabel()) == null) {
                    countList.put(node.getLabel(), 1);
                } else {/*
                         * increment that label 's occurrences .
                         */
                    countList.put(node.getLabel(),
                            countList.get(node.getLabel()) + 1);
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
        return randomVertex.getLabel();
    }

    public HashMap<Vertex<T>, NeighborList<T>> getNetwork() {
        return network;
    }

    public boolean initiliaze(HashMap<Vertex<T>, NeighborList<T>> network) {
        if (network == null)
            return false;
        this.network = network;

        /*
         * initially all nodes belong to themselves as community.
         */

        for (Entry<Vertex<T>, NeighborList<T>> entry : this.network.entrySet()) {
            Vertex<T> key = entry.getKey();
            key.setLabel(key.getValue());
        }

        return true;
    }

    /* check whether all vertices have the most commonly id of its neighbors */
    public boolean isTerminated() {

        for (Entry<Vertex<T>, NeighborList<T>> entry : this.network.entrySet()) {
            Vertex<T> key = entry.getKey();

            if (findMostCommonlyUsedId(key, entry.getValue()) == key.getLabel()) {
                continue;/* do nothing */
            } else {
                return false;
            }
        }
        return true;

    }

    /* the label propagation algorithm. */
    public void proceedLP() {
        List<Vertex<T>> keysAsArray = new ArrayList<Vertex<T>>(
                this.network.keySet());
        Random r = new Random();

        do {
            Vertex<T> randomVertex = keysAsArray.get(r.nextInt(keysAsArray
                    .size()));
            randomVertex.setLabel(findMostCommonlyUsedId(randomVertex,
                    this.network.get(randomVertex)));

        } while ((!isTerminated()));/*
                                     * i cut this code && (numberOfIterations !=
                                     * t)
                                     */

    }

    public void setNetwork(HashMap<Vertex<T>, NeighborList<T>> network) {
        this.network = network;
    }
}
