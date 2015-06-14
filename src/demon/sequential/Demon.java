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
import net.ontopia.utils.CompactHashMap;
import net.ontopia.utils.CompactHashSet;

import org.pcj.PCJ;


public class Demon<T> {

    private CommunityList<T> pool = null;
    private LabelPropagation<T> lp;
    private int numberOfComparison = 0;

    public int getNumberOfComparison() {
        return numberOfComparison;
    }

    public void setNumberOfComparison(int numberOfComparison) {
        this.numberOfComparison = numberOfComparison;
    }

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
        numberOfComparison++;
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
                localCommunity.setIndex(count);
                count++;
                pool.getCommunities().add(localCommunity);
            }
        }

        System.out.println("Number of communities found by LP is "
                           + pool.getCommunities().size());
        long startTime = System.nanoTime();
        if (mergingType == 1) {
            improvedSuperLinearMerge(mergeFactor);
        } else {
            quadraticMerging(pool, mergeFactor);
        }
        long estimatedTime = (System.nanoTime() - startTime) / 1000000000L;
        System.out.println("Time: " + estimatedTime + " seconds");
        pool = cleanPool(pool);
        System.out.println("Number of communities after merge is "
            + pool.getCommunities().size());
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
    private void improvedSuperLinearMerge(double mergeFactor) {
        System.out.println("Merging---> Started.");
        constructInvertedIndex();
        int n = pool.getCommunities().size();
        int[] temporaryPool = new int[n];
        boolean[] needsMergeCheck = new boolean[n];
        int i = n - 2;
        boolean merged = false;
        while (i >= 0) {
            do {
                Community<T> mergerComm = pool.getCommunities().get(i);
                // System.out.println("i " + i + " out of " + n);
                if (mergerComm == null)
                    continue;
                int temporaryPoolSize = 0;
                for (Community<T> dependecy : mergerComm.getDependencyList()) {
                    int indexOfCommunity = dependecy.getIndex();
                    if (indexOfCommunity > mergerComm.getIndex()) {
                        if (!merged) 
                            needsMergeCheck[indexOfCommunity] = true;
                        temporaryPool[temporaryPoolSize++] = indexOfCommunity;
                    }
                }
                merged = false;
                for (int k = 0; k < temporaryPoolSize; ++k) {
                    int index = temporaryPool[k];
                    Community<T> mergedComm =  pool.getCommunities().get(index);
                    if (!needsMergeCheck[index])
                        continue;
                    if (isMergible(mergerComm, mergedComm, mergeFactor)) {
                        merged = true;
                        for (Community<T> c : mergedComm.getDependencyList()) {
                            if (c.getIndex() > mergerComm.getIndex())
                                needsMergeCheck[c.getIndex()] = true;
                        }
                        merge(mergerComm, mergedComm);
                        pool.getCommunities().set(index, null);
                    } else {
                        needsMergeCheck[index] = false;
                    }
                }
            } while (merged);
            i = i - 1;
        }
    }
    
    private void merge(Community<T> mergerComm, Community<T> mergedComm)
    {
        // members
        int size1 = mergerComm.getMembers().size();
        int size2 = mergedComm.getMembers().size();
        int max = Math.max(size1, size2);
        if (size1 == max) {
            mergerComm.getMembers().addAll(mergedComm.getMembers());
        } else {
            mergedComm.getMembers().addAll(mergerComm.getMembers());
            mergerComm.setMembers(mergedComm.getMembers());
        }
        // dependency lists
        mergerComm.getDependencyList().remove(mergedComm);
        mergedComm.getDependencyList().remove(mergerComm);
        for (Community<T> c : mergedComm.getDependencyList()) {
            c.getDependencyList().remove(mergedComm);
            c.getDependencyList().add(mergerComm);
        }
        size1 = mergerComm.getDependencyList().size();
        size2 = mergedComm.getDependencyList().size();
        max = Math.max(size1, size2);
        if (size1 == max)
        {
            mergerComm.getDependencyList().addAll(
                mergedComm.getDependencyList());
        } else {
            mergedComm.getDependencyList().addAll(
                mergerComm.getDependencyList());
            mergerComm.setDependencyList(
                mergedComm.getDependencyList());
        }    
    }

    private void superLinearMerge(double mergeFactor) {
        constructInvertedIndex();
        System.out.println("Merging---> Started.");

        int n = pool.getCommunities().size();
        int[] temporaryPool = new int[n];
       
        int i = n - 2;
        boolean merged = false;
        while (i >= 0) {
            do {
                Community<T> mergerComm = pool.getCommunities().get(i);
                // System.out.println("i " + i + " out of " + n);
                if (mergerComm == null)
                    continue;
                int temporaryPoolSize = 0;
                for (Community<T> dependecy : mergerComm.getDependencyList()) {
                    int indexOfCommunity = dependecy.getIndex();
                    if (indexOfCommunity > mergerComm.getIndex()) {
                        temporaryPool[temporaryPoolSize++] = indexOfCommunity;
                    }
                }
                merged = false;
                for (int k = 0; k < temporaryPoolSize; ++k) {
                    int index = temporaryPool[k];
                    Community<T> mergedComm = pool.getCommunities().get(index);
                    if (isMergible(mergerComm, mergedComm, mergeFactor)) {
                        merged = true;
                        merge(mergerComm, mergedComm);
                        pool.getCommunities().set(index, null);
                    } 
                }
            } while (merged);
            i = i - 1;
        }
    }

    /**
     * This method performs merging. This is the brute force approach.
     * Complexity is very high.
     * 
     * @param mergeFactor
     */
    private void quadraticMerging(CommunityList<T> pool, double mergeFactor) {

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
    }

    /**
     * This method removes null values from community pool.
     */
    private CommunityList<T> cleanPool(CommunityList<T> pool) {
        Iterator<Community<T>> iter = pool.getCommunities().iterator();
        CommunityList<T> cleanedPool = new CommunityList<T>();
        while (iter.hasNext()) {
            Community<T> community = iter.next();
            if (community != null)
                cleanedPool.getCommunities().add(community);
        }
        return cleanedPool;
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
