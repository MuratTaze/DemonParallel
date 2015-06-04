package demon.sequential;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import labelPropagation.Community;
import labelPropagation.CommunityList;
import labelPropagation.GraphLoader;
import labelPropagation.LabelPropagation;
import labelPropagation.NeighborList;
import labelPropagation.Network;
import labelPropagation.Vertex;
import net.ontopia.utils.CompactHashSet;

import org.pcj.PCJ;

public class Demon<T> {

    private CommunityList<T> pool = null;
    private LabelPropagation<T> lp;

    public CommunityList<T> getGlobalCommunities() {
        return pool;
    }

    public Demon() {

        super();
        lp = new LabelPropagation<T>();
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
        CompactHashSet<Vertex<T>> intersection = new CompactHashSet<Vertex<T>>(
                neighborList.getListOfNeighbors());
        intersection.retainAll(neighborList2.getListOfNeighbors());
        return new NeighborList<T>(headVertex, intersection);
    }

    /* demon algorithm. */
    public void execute(Network<T> graph, double mergeFactor, int mergingType)
            throws IOException {
        pool = new CommunityList<T>();
        @SuppressWarnings("unchecked")
        Vertex<T>[] vertices = new Vertex[graph.getGraph().size()];
        graph.getGraph().keySet().toArray(vertices);

        for (Vertex<T> vertex : vertices) {
            Network<T> eMeN = egoMinusEgo(vertex, graph);

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

        if (mergingType == 1) {
            System.out.println("Number of communities found by LP is "
                    + pool.getCommunities().size());
            long startTime = System.nanoTime();
            subLinearMerging(mergeFactor);
            long estimatedTime = System.nanoTime() - startTime;
            System.out.println("Time: " + estimatedTime
                    + " with Sublinear Merge ");
        } else {
            System.out.println("Number of communities found by LP is "
                    + pool.getCommunities().size());
            long startTime = System.nanoTime();
            quadraticMerging(mergeFactor);
            long estimatedTime = System.nanoTime() - startTime;
            System.out.println("Time: " + estimatedTime
                    + " with Quadratic Merge ");
        }

    }

    private void subLinearMerging(double mergeFactor) {
        constructInvertedIndex();
        for (Community<T> community : pool.getCommunities()) {

            while (true) {

                if (community == null
                        || community.getDependencyList().size() == 0)
                    break;
                boolean merged = false;
                for (Community<T> c : community.getDependencyList()) {
                    if (isMergible(community, c, mergeFactor)) {
                        community.getMembers().addAll(c.getMembers());
                        c.getDependencyList().remove(community);
                        community.getDependencyList().addAll(
                                c.getDependencyList());

                        community.getDependencyList().remove(c);

                        for (Community<T> t : c.getDependencyList()) {
                            t.getDependencyList().remove(c);
                            t.getDependencyList().add(community);
                        }
                        pool.getCommunities().set(
                                pool.getCommunities().indexOf(c), null);
                        merged = true;
                        break;
                    }
                }
                if (!merged)
                    break;
            }

        }
        cleanPool();
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

    private void cleanPool() {
        Iterator<Community<T>> iter = pool.getCommunities().iterator();
        while (iter.hasNext()) {
            Community<T> community = iter.next();
            if (community == null)
                iter.remove();
        }
    }

    private void constructInvertedIndex() {
        HashMap<T, ArrayList<Community<T>>> invertedIndex = null;

        /* inverted index extraction */
        invertedIndex = new HashMap<T, ArrayList<Community<T>>>();
        for (Community<T> community : pool.getCommunities()) {
            for (T member : community.getMembers()) {
                ArrayList<Community<T>> ii = invertedIndex.get((member));
                if (ii == null) {
                    ii = new ArrayList<Community<T>>();
                    invertedIndex.put(member, ii);
                }

                ii.add(community);
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
