package demon.sequential;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import labelPropagation.Community;
import labelPropagation.CommunityList;
import labelPropagation.GraphLoader;
import labelPropagation.LabelPropagation;
import labelPropagation.NeighborList;
import labelPropagation.Network;
import labelPropagation.Vertex;
import net.ontopia.utils.CompactHashMap;
import net.ontopia.utils.CompactHashSet;

import org.pcj.PCJ;

public class Demon<T> {

    private CommunityList<T> pool = null;
    private CommunityList<T> cleanedPool = null;
    private LabelPropagation<T> lp;

    public CommunityList<T> getGlobalCommunities() {
        return pool;
    }

    public Demon() {

        super();
        lp = new LabelPropagation<T>();
    }

    /**
     * This method checks whether two communities can be merged or not.
     * 
     * @param community1
     *            first community
     * @param community2
     *            second community
     * @param mergeFactor
     *            β is the threshold. If it is 1 we merge iff fully containment
     *            is achieved. If it is 0 two communities are merged anyway.
     * @return returns true if at least β fraction of the smaller community
     *         resides in their intersection.
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

    /**
     * This method constructs ego minus ego network of a given vertex.
     * 
     * @param vertex
     * @param network
     * @return the network which is constructed without above vertex. It
     *         includes edges between neighbors of the above vertex.
     */
    public Network<T> egoMinusEgo(Vertex<T> vertex, Network<T> network) {
        HashMap<Vertex<T>, NeighborList<T>> result = new HashMap<Vertex<T>, NeighborList<T>>();

        NeighborList<T> neighborList = network.getGraph().get(vertex);
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
                        intersection(network.getGraph().get(neighbor),
                                neighborList));
            } else {
                NeighborList<T> nl = getRemoteNeighbors(neighbor);
                network.getGraph().put(nl.getHeadVertex(), nl);
                result.put(neighbor, intersection(nl, neighborList));

            }
        }
        return new Network<T>(result);
    }

    /**
     * This method gets the neighbor list of given vertex from the indexed
     * processor.
     * 
     * @param vertex
     * @return adjacency list of given vertex
     */
    @SuppressWarnings("unchecked")
    private NeighborList<T> getRemoteNeighbors(Vertex<T> vertex) {

        int numberOfThreads = PCJ.threadCount();
        int size = GraphLoader.numberOfElements;
        int arraySize = (size / numberOfThreads) + 1;
        int hashCode = vertex.hashCode() % (size + 1);
        int thread = hashCode / arraySize;

        ArrayList<NeighborList<T>> entry = (ArrayList<NeighborList<T>>) (PCJ
                .get(thread, "array", vertex.hashCode() % arraySize));
        for (Iterator<NeighborList<T>> iterator = entry.iterator(); iterator
                .hasNext();) {
            NeighborList<T> neighborList = (NeighborList<T>) iterator.next();
            if (neighborList.getHeadVertex().equals(vertex)) {
                return neighborList;
            }
        }
        return null;
    }

    /**
     * 
     * This method computes intersection of given two sets.
     * 
     * @param neighborList
     * @param neighborList2
     * @return returns intersection of two sets as a new set
     */
    private NeighborList<T> intersection(NeighborList<T> neighborList,
            NeighborList<T> neighborList2) {
        CompactHashSet<Vertex<T>> intersection = new CompactHashSet<Vertex<T>>(
                neighborList.getListOfNeighbors());
        intersection.retainAll(neighborList2.getListOfNeighbors());

        double size1 = neighborList.getListOfNeighbors().size();
        double size2 = neighborList2.getListOfNeighbors().size();
        double min = size2;
        if (size1 < size2) {
            min = size1;
        }
        if (min == size2) {
            for (Vertex<T> value : neighborList2.getListOfNeighbors()) {
                if (neighborList.getListOfNeighbors().contains(value))
                    intersection.add(value);
            }
        } else {
            for (Vertex<T> value : neighborList.getListOfNeighbors()) {
                if (neighborList2.getListOfNeighbors().contains(value))
                    intersection.add(value);
            }
        }

        return new NeighborList<T>(neighborList.getHeadVertex(), intersection);
    }

    /**
     * Main demon algorithm.
     * 
     * @param graph
     * @param mergeFactor
     * @param mergingType
     * @throws IOException
     */
    public void execute(Network<T> graph, double mergeFactor, int mergingType)
            throws IOException {
        int count = 0;
        pool = new CommunityList<T>();
        cleanedPool = new CommunityList<T>();
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
                localCommunity.setIndex(count);
                count++;
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
            superLinearMerge(mergeFactor);
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

    /**
     * Inverted indexed based merging. First creates inverted index list from
     * communities. For each vertex , we have corresponding communities in
     * posting list. Then this inverted index list is used to build community
     * dependency graph. By using this dependency graph we perform merging.
     * There is no unnecessary comparison.
     * 
     * @param mergeFactor
     */
    private void superLinearMerge(double mergeFactor) {
        constructInvertedIndex();
        System.out.println("Merging---> Started.");

        List<Integer> temporaryPool = null;
        int n = pool.getCommunities().size();
        int i = n - 2;
        boolean merged = false;
        while (i >= 0) {
            do {
                Community<T> mergerCommunity = pool.getCommunities().get(i);
                temporaryPool = new LinkedList<Integer>();
                Integer indexOfCommunity;
                for (Community<T> dependecy : mergerCommunity
                        .getDependencyList()) {
                    indexOfCommunity = dependecy.getIndex();
                    if (indexOfCommunity > mergerCommunity.getIndex())
                        temporaryPool.add(indexOfCommunity);
                }
                merged = false;
                for (Integer index : temporaryPool) {
                    Community<T> mergedCommunity = pool.getCommunities().get(
                            index);
                    if (isMergible(mergerCommunity, mergedCommunity,
                            mergeFactor)) {
                        mergerCommunity.getMembers().addAll(
                                mergedCommunity.getMembers());
                        merged = true;
                        mergedCommunity.getDependencyList().remove(
                                mergerCommunity);
                        for (Community<T> c : mergedCommunity
                                .getDependencyList()) {
                            c.getDependencyList().remove(mergedCommunity);
                            c.getDependencyList().add(mergerCommunity);
                            mergerCommunity.getDependencyList().add(c);
                        }
                        mergerCommunity.getDependencyList().remove(
                                mergedCommunity);
                        pool.getCommunities().set(index, null);
                    }

                }
            } while (merged);
            i = i - 1;
        }

        cleanPool();
    }


    /**
     * This method performs merging. This is the brute force approach.
     * Complexity is very high.
     * 
     * @param mergeFactor
     */
    private void quadraticMerging(double mergeFactor) {
        System.out.println("Merging---> Started.");
        /* merging part pooling approach */
        int j;
        int n = pool.getCommunities().size();
        int i = n - 2;
        boolean merged = false;
        while (i >= 0) {

            j = i + 1;
            do {
                merged = false;
                while (j < n) {

                    if (isMergible(pool.getCommunities().get(i), pool
                            .getCommunities().get(j), mergeFactor)) {
                        pool.getCommunities()
                                .get(i)
                                .getMembers()
                                .addAll(pool.getCommunities().get(j)
                                        .getMembers());
                        pool.getCommunities().set(j, null);
                        merged = true;
                    }
                    j = j + 1;
                }
            } while (merged);

            i = i - 1;
        }
        cleanPool();
    }

    /**
     * This method removes null values from community pool.
     */
    private void cleanPool() {
        Iterator<Community<T>> iter = pool.getCommunities().iterator();
        while (iter.hasNext()) {
            Community<T> community = iter.next();
            if (community != null)
                cleanedPool.getCommunities().add(community);
        }
        pool = cleanedPool;
    }

    /**
     * This method constructs inverted index list then it builds community
     * dependency graph.
     */
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
        new CompactHashMap<Community<T>, CompactHashSet<Integer>>();
        ArrayList<Integer> list;
        for (Community<T> community : pool.getCommunities()) {
            list = new ArrayList<Integer>();
            for (Community<T> dependecy : community.getDependencyList()) {
                Integer index = dependecy.getIndex();
                if (index > community.getIndex())
                    list.add(index);
            }
        }
        System.out.println("dependency construction--> done.");

    }
}
