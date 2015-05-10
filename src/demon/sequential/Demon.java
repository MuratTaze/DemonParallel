package demon.sequential;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import labelPropagation.Community;
import labelPropagation.CommunityList;
import labelPropagation.GraphLoader;
import labelPropagation.LabelPropagation;
import labelPropagation.NeighborList;
import labelPropagation.Network;
import labelPropagation.Vertex;

import org.pcj.PCJ;

public class Demon<T> {

    private CommunityList<T> pool = null;

    public CommunityList<T> getGlobalCommunities() {
        return pool;
    }

    public Demon() {
        super();
        // TODO Auto-generated constructor stub
    }

    /*
     * two communities are merged if at least β fraction of the smaller
     * community resides in their intersection.
     */
    private boolean isMergible(Community<T> community1, Community<T> community2,
            double mergeFactor) {
        if(community1==null||community2==null)
            return false;
        
        double intersection = 0;
        double size1 = community1.getMembers().size();
        double size2 = community2.getMembers().size();
        double min = size2;
        if (size1 < size2) {
            min = size1;
        }
        if (min == size2) {
            for (T value : community2.getMembers()) {
                if (community1.getMembers().contains(value))
                    intersection++;
            }
        } else {
            for (T value : community1.getMembers()) {
                if (community2.getMembers().contains(value))
                    intersection++;
            }
        }
        if (intersection / min >= mergeFactor) {

            // them
            return true;
        }
        return false;
    }

    /*
     * for a given vertex and a network this method constructs its ego minus ego
     * network.
     */
    public Network<T> egoMinusEgo(Vertex<T> key, Network<T> network) {
        HashMap<Vertex<T>, NeighborList<T>> result = new HashMap<Vertex<T>, NeighborList<T>>();

        NeighborList<T> neighborList = network.getGraph().get(key);
        for (Vertex<T> neighbor : neighborList.getListOfNeighbors()) {

            /*
             * fetch the neighbors of current neighbor. if it returns null we
             * will do remote access
             */
            if (network.getGraph().get(neighbor) != null) {
                /*
                 * remove neighbors of current neighbor which are not included
                 * in neighborlist.
                 */
                result.put(
                        neighbor,
                        intersection(neighbor,
                                network.getGraph().get(neighbor), neighborList));
            } else {
                // remote access required. it will soon be implemented.
                /*
                 * hangi makinaya düştüğünü bul hangi indexte olduğunu
                 * bul o makinadan arraylisti getir sonra o arraylistte
                 * aradığımız elemanı bul
                 */
                NeighborList<T> nl = getRemoteNeighbors(neighbor);
                network.getGraph().put(nl.getHeadVertex(), nl);
                result.put(neighbor, intersection(neighbor, nl, neighborList));

            }
        }
        return new Network<T>(result);
    }

    @SuppressWarnings("unchecked")
    private NeighborList<T> getRemoteNeighbors(Vertex<T> neighbor) {

        int numberOfThreads = PCJ.threadCount();
        int size = GraphLoader.numberOfElements;
        int arraySize = (size / numberOfThreads) + 1;
        int hashCode = neighbor.hashCode() % (size + 1);
        int thread = hashCode / arraySize;

        ArrayList<NeighborList<T>> entry = (ArrayList<NeighborList<T>>) (PCJ
                .get(thread, "array", neighbor.hashCode() % arraySize));
        for (Iterator<NeighborList<T>> iterator = entry.iterator(); iterator
                .hasNext();) {
            NeighborList<T> neighborList = (NeighborList<T>) iterator.next();
            if (neighborList.getHeadVertex().equals(neighbor)) {
                return neighborList;
            }
        }
        return null;
    }

    /* returns intersection of two sets as new set */
    private NeighborList<T> intersection(Vertex<T> headVertex,
            NeighborList<T> neighborList, NeighborList<T> neighborList2) {
        HashSet<Vertex<T>> intersection = new HashSet<Vertex<T>>(
                neighborList.getListOfNeighbors());
        intersection.retainAll(neighborList2.getListOfNeighbors());
        return new NeighborList<T>(headVertex, intersection);
    }

    /* demon algorithm. */
    public void execute(Network<T> graph, double mergeFactor)
            throws IOException {
        pool = new CommunityList<T>();
        @SuppressWarnings("unchecked")
        Vertex<T>[] vertices = new Vertex[graph.getGraph().size()];
        graph.getGraph().keySet().toArray(vertices);
        for (Vertex<T> vertex : vertices) {

            Network<T> eMeN = egoMinusEgo(vertex, graph);
            LabelPropagation<T> lp = new LabelPropagation<T>();
            lp.initiliaze(eMeN.getGraph());
            lp.proceedLP();
            /* get local communities found by label propagation */
            CommunityList<T> localCommunities = lp.extractCommunities();

            /* merge each local communities found with global communities */
            for (Community<T> localCommunity : localCommunities
                    .getCommunities()) {

                localCommunity.getMembers().add(vertex.getValue());
                pool.getCommunities().add(localCommunity);/*
                                                           * add all communities
                                                           * to community pool
                                                           */

            }
        }
        /* merge part - pooling approach */
        for (int i = 0; i < pool.getCommunities().size() - 1; i++) {
            for (int j = i+1; j < pool.getCommunities().size();) {
                if (isMergible(pool.getCommunities().get(i), pool.getCommunities()
                        .get(j), mergeFactor)) {
                    pool.getCommunities().get(i).getMembers()
                            .addAll(pool.getCommunities().get(j).getMembers());
                    pool.getCommunities().remove(j);
                    j = i+1;
                } else
                    j++;
            }
        }

    }
}
