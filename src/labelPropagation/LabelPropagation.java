package labelPropagation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

public class LabelPropagation<T> {
    private HashMap<T, T> communites = new HashMap<T, T>();
    private HashMap<T, HashSet<T>> network;/*
                                                         * due to speed issues
                                                         */

    public LabelPropagation() {
        super();
    }

    public LabelPropagation(HashMap<T, HashSet<T>> network) {
        super();
        this.network = network;
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
        for (T vertex : this.network.keySet()) {
            if (communities.hasCommunity(this.communites.get(vertex))) {
                communities.addMember(vertex, this.communites.get(vertex));
            } else {
                communities.createCommunity(vertex, this.communites.get(vertex));
            }
        }
        return communities;

    }

    /**
     * For a given vertex and its neighbors, this method finds label of the most
     * frequent neighbor.
     * 
     * @param t
     *            vertex chosen randomly from the network
     * @param hashSet
     *            neighbors of the vertex
     * @return label of the most frequent neighbor
     */
    public T findMostCommonlyUsedId(T t, HashSet<T> hashSet) {

        if (hashSet.size() != 0) {
            /* iterate over ego network and count community id's. */
            HashMap<T, Integer> countList = new HashMap<T, Integer>(hashSet.size());
            for (T node : hashSet) {
                if (communites.get(node) == null)
                    communites.put(node, node);
                if (countList.get(communites.get(node)) == null) {
                    countList.put(communites.get(node), 1);
                } else {/*
                         * increment that label 's occurrences .
                         */
                    countList.put(communites.get(node), countList.get(communites.get(node)) + 1);
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
        return communites.get(t);
    }

    public HashMap<T, HashSet<T>> getNetwork() {
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
    public boolean initiliaze(HashMap<T, HashSet<T>> network) {
        if (network == null)
            return false;
        this.network = network;

        /*
         * initially all nodes belong to themselves as community.
         */

        for (Entry<T, HashSet<T>> entry : this.network.entrySet()) {
            communites.put(entry.getKey(), entry.getKey());
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

        for (Entry<T, HashSet<T>> entry : this.network.entrySet()) {

            if (findMostCommonlyUsedId(entry.getKey(), entry.getValue()) == communites.get(entry.getKey())) {
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
        List<T> vertices = new ArrayList<T>(this.network.keySet());

        do {
            shuffle(vertices); 
            for (T vertex : vertices) {
                communites.put(vertex, findMostCommonlyUsedId(vertex, this.network.get(vertex)));
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
    private void shuffle(List<T> vertices) {
        Random r = new Random();
        int i = vertices.size() - 1;
        while (i != 0) {
            int j = r.nextInt(i);
            T temp = vertices.get(j);
            vertices.set(j, vertices.get(i));
            vertices.set(i, temp);
            i--;
        }
    }

    public void setNetwork(HashMap<T, HashSet<T>> network) {
        this.network = network;
    }
}
