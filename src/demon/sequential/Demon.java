package demon.sequential;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import org.pcj.PCJ;

import labelPropagation.Community;
import labelPropagation.CommunityList;
import labelPropagation.GraphLoader;
import labelPropagation.LabelPropagation;
import labelPropagation.NeighborList;
import labelPropagation.Network;
import labelPropagation.Vertex;

public class Demon<T> {
    private CommunityList<T> globalCommunities = null;

    public CommunityList<T> getGlobalCommunities() {
        return globalCommunities;
    }

    public void setGlobalCommunities(CommunityList<T> globalCommunities) {
        this.globalCommunities = globalCommunities;
    }

    public Demon() {
        super();
        // TODO Auto-generated constructor stub
    }

    /*
     * two communities are merged if at least β fraction of the smaller
     * community resides in their intersection.
     */
    private void merge(Community<T> localCommunity, double mergeFactor) {
        if (globalCommunities == null) {
            globalCommunities = new CommunityList<T>();
            globalCommunities.getCommunities().add(localCommunity);
        } else {
            /* we check for each global community */
            for (Community<T> temp : globalCommunities.getCommunities()) {

                HashSet<T> temp2 = new HashSet<T>(temp.getMembers());
                double size1 = temp2.size();
                double size2 = localCommunity.getMembers().size();
                double min = size1 < size2 ? size1 : size2;
                temp2.retainAll(localCommunity.getMembers());
                if (temp2.size() / min >= mergeFactor) {
                    temp.getMembers().addAll(localCommunity.getMembers());// join
                                                                          // them
                    return;
                }
            }
            globalCommunities.getCommunities().add(localCommunity);// as a
                                                                   // seperate
            // community

        }
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
                 * hangi makinaya düştüğünü bul hangi indexte olduğunu bul o
                 * makinadan arraylisti getir sonra o arraylistte aradığımız
                 * elemanı bul
                 */
                NeighborList<T> nl = getRemoteNeighbors(neighbor);
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

        for (Entry<Vertex<T>, NeighborList<T>> entry : graph.getGraph()
                .entrySet()) {
            /*
             * for each vertex we get its ego minus ego network and perform
             * label propagation on it
             */
            Network<T> eMeN = egoMinusEgo(entry.getKey(), graph);
            LabelPropagation<T> lp = new LabelPropagation<T>();
            lp.initiliaze(eMeN.getGraph());
            lp.proceedLP();

            /* get local communities found by label propagation */
            CommunityList<T> localCommunities = lp.extractCommunities();

            /* merge each local communities found with global communities */
            for (Community<T> localCommunity : localCommunities
                    .getCommunities()) {

                localCommunity.getMembers().add(entry.getKey().getValue());
                merge(localCommunity, mergeFactor);

            }

        }

    }
}
