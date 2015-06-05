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
    }

    /*
     * two communities are merged if at least β fraction of the smaller
     * community resides in their intersection.
     */
    private boolean isMergible(Community<T> community1,
            Community<T> community2, double mergeFactor) {
        if (community1 == null || community2 == null)
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
                 * hangi makinaya düştüğünü bul hangi indexte olduğunu bul o
                 * makinadan arraylisti getir sonra o arraylistte aradığımız
                 * elemanı bul
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
        System.out.println("Number of vertices to be passed :"
                + vertices.length);
        int count = 0;
        for (Vertex<T> vertex : vertices) {
            System.out.println(" communities found for " + count
                    + " vertices out of " + vertices.length);
            Network<T> eMeN = egoMinusEgo(vertex, graph);
            System.out.println("waiting for LP for " + count + " vertices");
            LabelPropagation<T> lp = new LabelPropagation<T>();

            lp.initiliaze(eMeN.getGraph());
            lp.proceedLP();
            /* get local communities found by label propagation */
            CommunityList<T> localCommunities = lp.extractCommunities();
            System.out.println("lp executed for " + count + " vertices");
            /* merge each local communities found with global communities */
            for (Community<T> localCommunity : localCommunities
                    .getCommunities()) {

                localCommunity.getMembers().add(vertex.getValue());
                pool.getCommunities().add(localCommunity);/*
                                                           * add all communities
                                                           * to community pool
                                                           */
            }
            count++;
        }
        long startTime = System.nanoTime();
        subLinearMerging(mergeFactor);
        // quadraticMerging(mergeFactor);
        long estimatedTime = System.nanoTime() - startTime;
        System.out.println(estimatedTime);

    }

    private void subLinearMerging(double mergeFactor) {
        constructInvertedIndex();
        int i = 0;
        boolean cont = true;
        while (true) {
            if (pool.getCommunities().size() <= i)
                break;
            Community<T> community = pool.getCommunities().get(i);
            cont = true;
            while (cont) {
                for (Community<T> c : community.getDependencyList()) {
                    if (isMergible(community, c, mergeFactor)) {
                        cont = true;
                        community.getMembers().addAll(c.getMembers());
                        community.getDependencyList().addAll(
                                c.getDependencyList());
                        community.getDependencyList().remove(community);
                        community.getDependencyList().remove(c);
                        c.getDependencyList().remove(community);
                        for (Community<T> t : c.getDependencyList()) {
                            t.getDependencyList().remove(c);
                            t.getDependencyList().add(community);
                        }
                        pool.getCommunities().remove(c);
                        break;
                    } else {
                        cont = false;
                    }
                }
            }
            i++;
        }
    }

    
    private void quadraticMerging(double mergeFactor) {
        System.out.println("Merging---> Started.");
        /* merging part pooling approach */
        int j;
        int n = pool.getCommunities().size();
        int i = n - 2;
        while (i >= 0) {
            if (pool.getCommunities().get(i) != null) {
                j = i + 1;
                while (j < n) {
                    if (pool.getCommunities().get(j) == null) {
                        j = j + 1;
                    } else if (!(isMergible(pool.getCommunities().get(i), pool
                            .getCommunities().get(j), mergeFactor))) {
                        j = j + 1;
                    } else {
                        pool.getCommunities()
                                .get(i)
                                .getMembers()
                                .addAll(pool.getCommunities().get(j)
                                        .getMembers());
                        pool.getCommunities().set(j, null);
                        j = i + 1;
                    }
                }
            }
            i = i - 1;
        }
        cleanPool();
    }

    private void improvedQuadraticMerging(double mergeFactor) {
        System.out.println("Merging---> Started.");
        /* merging part pooling approach */
        int j;
        int n = pool.getCommunities().size();
        int i = n - 2;
        while (i >= 0) {
            if (pool.getCommunities().get(i) != null) {
                j = i + 1;
                boolean merged = false;
                do {
                    merged = false;
                    while (j < n) {
                        if (pool.getCommunities().get(j) != null) {
                            if (!(isMergible(pool.getCommunities().get(i), pool
                                    .getCommunities().get(j), mergeFactor))) {
                            } else {
                                merged = true;
                                pool.getCommunities()
                                        .get(i)
                                        .getMembers()
                                        .addAll(pool.getCommunities().get(j)
                                                .getMembers());
                                pool.getCommunities().set(j, null);
                            }
                        }
                        j = j + 1;
                    }
                } while(merged);
            }
            i = i - 1;
        }
        cleanPool();
    }
    
    private void cleanPool() {
        // TODO: improve this
        for (Community<T> community : pool.getCommunities()) {
            if (community == null)
                pool.getCommunities().remove(community);
        }
    }

    private void constructInvertedIndex() {
        HashMap<T, ArrayList<Community<T>>> invertedIndex = null;

        /* inverted index extraction */
        invertedIndex = new HashMap<T, ArrayList<Community<T>>>();
        for (Community<T> community : pool.getCommunities()) {
            for (T member : community.getMembers()) {
                ArrayList<Community<T>> set = invertedIndex.get((member));
                if (set == null) {
                    set = new ArrayList<Community<T>>();
                    invertedIndex.put((member), set);
                }

                set.add(community);
            }

        }

        System.out.println("Inverted index-->done.");
        /* dependency construction */

        for (ArrayList<Community<T>> list : invertedIndex.values()) {
            for (int i = 0; i < list.size(); i++) {
                list.get(i).getDependencyList().addAll(list);
                list.get(i).getDependencyList().remove(list.get(i));
            }
        }

        System.out.println("dependency construction--> done.");
    }
}
