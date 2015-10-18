package demon.sequential;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;

public class Demon<T> {
    public ArrayList[] requestArray, responseArray;
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

    public Demon(ArrayList[] requestArray, ArrayList[] responseArray) {
        super();
        lp = new LabelPropagation<T>();
        this.requestArray = requestArray;
        this.responseArray = responseArray;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void constructBoundries(Network<T> graph, int numberOfVertices) {

        HashMap<Vertex<T>, HashSet<Vertex<T>>> connections = new HashMap<Vertex<T>, HashSet<Vertex<T>>>();
        HashMap<Vertex<T>, NeighborList<T>> map = graph.getGraph();

        /* this list contains lists of possibly redundant boundaries. */
        ArrayList<ArrayList<Vertex<T>>> list = new ArrayList<ArrayList<Vertex<T>>>();
        for (NeighborList<T> nl : map.values()) {
            ArrayList<Vertex<T>> friends = new ArrayList<Vertex<T>>();
            for (Vertex<T> neighbor : nl.getListOfNeighbors()) {
                if (map.containsKey(neighbor)) {
                    /* no problem */
                } else {
                    /* add these vertices to boundary list */
                    friends.add(neighbor);
                }
            }
            if (friends.size() != 0 && friends.size() != 1)
                list.add(friends);
        }

        /*
         * connections hashmap contains formatted boundaries with no unnecessary
         * computation
         */
        for (ArrayList<Vertex<T>> friends : list) {
            Iterator<Vertex<T>> iter = friends.iterator();
            while (iter.hasNext()) {
                Vertex<T> v = iter.next();
                if (!connections.containsKey(v)) {
                    if (iter.hasNext())
                        connections.put(v, new HashSet<Vertex<T>>());
                    else
                        break;
                }
                iter.remove();
                connections.get(v).addAll(friends);
            }
        }

        int numberOfThreads = PCJ.threadCount();
        int arraySize = (numberOfVertices / numberOfThreads) + 1;
        /* assign each boundry vertex to corresponding index(processor) */

        /*
         * write only keys which are wanted to know degrees of to corresponding
         * processor index
         */
        for (Vertex<T> v : connections.keySet()) {
            int hashCode = v.hashCode() % numberOfVertices;
            int index = hashCode / arraySize;
            if (requestArray[index] == null)
                requestArray[index] = new ArrayList();
            requestArray[index].add(v);

        }

        /* make sure that every processor filled request lists */
        PCJ.barrier();
        for (int i = 0; i < PCJ.threadCount(); i++) {
            /*
             * each thread fetches requests and put the result to requester
             * threads response array.
             */
            if (PCJ.myId() == i)
                continue;
            ArrayList<Vertex<T>> currentRequest = PCJ.get(i, "requestArray",
                    PCJ.myId());
            if (currentRequest != null && currentRequest.size() != 0) {
                ArrayList<Integer> currentDegrees = calculateDegrees(
                        currentRequest, graph);
                responseArray[i] = currentDegrees;
            }
        }
        PCJ.barrier();

        /* lets fetch computed related degrees */
        ArrayList[] computedDegrees = new ArrayList[PCJ.threadCount()];
        for (int j = 0; j < PCJ.threadCount(); j++) {
            if (PCJ.myId() == j)
                continue;
            ArrayList<Vertex<T>> currentResponse = PCJ.get(j, "responseArray",
                    PCJ.myId());
            if (currentResponse != null && currentResponse.size() != 0) {
                computedDegrees[j] = currentResponse;
            }
        }

        /* compare degrees with the list of each key */
        ArrayList[] toBeSent = new ArrayList[PCJ.threadCount()];
        ArrayList[] toBeFetched = new ArrayList[PCJ.threadCount()];
        int i = 0;
        int j = 0;
        for (ArrayList<Vertex<T>> l : requestArray) {
            if (l != null) {
                j = 0;
                for (Vertex<T> v : l) {
                    /* bu liste neighborlar� getirilecekleri tutuyor */
                    if (connections.get(v).size() > ((Integer) (computedDegrees[i]
                            .get(j)))) {
                        if (toBeFetched[i] == null) {
                            toBeFetched[i] = new ArrayList<Vertex<T>>();
                        }
                        toBeFetched[i].add(v);
                    } else {
                        /*
                         * bu liste connecitoionslar� tutuyor connections
                         * arraylist yokki!!!
                         */
                        if (toBeSent[i] == null) {
                            toBeSent[i] = new ArrayList<Vertex<T>>();
                        }
                        toBeSent[i].add(v);
                    }
                    j = j + 1;
                }
            }
            i = i + 1;
        }

        /*
         * ilk toBeFetched yap�lacak. request array a at bunu. sonra barrieri
         * koy. sonra her processor �eksin kendi indexini ve response arrayine
         * de �ekti�i indexe neighborlistleri yazs�n. sonra her processor bu
         * neighborlistleri localine �eksin
         */

        requestArray = toBeFetched;
        PCJ.barrier();
        for (int i1 = 0; i1 < PCJ.threadCount(); i1++) {

            if (PCJ.myId() == i1)
                continue;
            ArrayList<Vertex<T>> currentRequest = PCJ.get(i1, "requestArray",
                    PCJ.myId());
            if (currentRequest != null && currentRequest.size() != 0) {
                ArrayList<NeighborList<T>> neighbors = getNeighbors(
                        currentRequest, graph);
                responseArray[i1] = neighbors;
            }
        }
        PCJ.barrier();
        ArrayList[] remoteNeighbors = new ArrayList[PCJ.threadCount()];
        for (int j1 = 0; j1 < PCJ.threadCount(); j1++) {
            if (PCJ.myId() == j1)
                continue;
            ArrayList<NeighborList<T>> currentResponse = PCJ.get(j1,
                    "responseArray", PCJ.myId());
            if (currentResponse != null && currentResponse.size() != 0) {
                remoteNeighbors[j1] = currentResponse;
            }
        }
        PCJ.barrier();

        /*
         * burada ise bizim dependencyleri yollayaca��z ve kar��l�k olarak
         * aralar�nda edge var m� yok mu onun bilgisinin gelmesini bekliyoruz
         */
        /* �ncesinde request arrayi d�zenle bir bakal�m */
        for (int i1 = 0; i1 < PCJ.threadCount(); i1++) {

            if (PCJ.myId() == i1)
                continue;
            
        
        }
        for (int i1 = 0; i1 < PCJ.threadCount(); i1++) {

            if (PCJ.myId() == i1)
                continue;
            ArrayList<Vertex<T>> currentRequest = PCJ.get(i1, "requestArray",
                    PCJ.myId());
            if (currentRequest != null && currentRequest.size() != 0) {
                ArrayList<NeighborList<T>> neighbors = getNeighbors(
                        currentRequest, graph);
                responseArray[i1] = neighbors;
            }
        }
        PCJ.barrier();
        ArrayList[] edges = new ArrayList[PCJ.threadCount()];
        for (int j1 = 0; j1 < PCJ.threadCount(); j1++) {
            if (PCJ.myId() == j1)
                continue;
            ArrayList<NeighborList<T>> currentResponse = PCJ.get(j1,
                    "responseArray", PCJ.myId());
            if (currentResponse != null && currentResponse.size() != 0) {
                remoteNeighbors[j1] = currentResponse;
            }
        }

    }

    private ArrayList<NeighborList<T>> getNeighbors(
            ArrayList<Vertex<T>> currentRequest, Network<T> graph) {
        ArrayList<NeighborList<T>> result = new ArrayList<NeighborList<T>>();
        for (Vertex<T> v : currentRequest) {
            result.add((graph.getGraph().get(v)));
        }

        return result;
    }

    private ArrayList<Integer> calculateDegrees(
            ArrayList<Vertex<T>> currentRequest, Network<T> graph) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (Vertex<T> v : currentRequest) {
            result.add(new Integer(graph.getGraph().get(v).getListOfNeighbors()
                    .size()));
        }
        return result;
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
                System.out.println("tamam");
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

        int hashCode = vertex.hashCode() % size;
        int thread = hashCode / arraySize;
        int localIndex = hashCode % (arraySize);
        int myId = PCJ.myId();
        /* thread -1 çıkıyor bu nasıl iş? */
        ArrayList<NeighborList<T>> entry = (ArrayList<NeighborList<T>>) (PCJ
                .get(thread, "array", localIndex));
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
        HashSet<Vertex<T>> intersection = new HashSet<Vertex<T>>(
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
     * @param numberOfVertices
     * @throws IOException
     */
    public void execute(Network<T> graph, double mergeFactor, int mergingType,
            int numberOfVertices) throws IOException {
        constructBoundries(graph, numberOfVertices);
        System.out.println("sağlam");
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
        Collections.sort(pool.getCommunities(), Collections.reverseOrder());
        int a = 0;
        for (Community<T> c : pool.getCommunities()) {
            c.setIndex(a);
            a++;
        }
        if (mergingType == 1) {
            improvedGraphBasedMerge(mergeFactor);
        } else {
            quadraticMerge(pool, mergeFactor);
        }
        double estimatedTime = (System.nanoTime() - startTime) / 1000000000.;
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
    private void improvedGraphBasedMerge(double mergeFactor) {
        System.out.println("Merging---> Started.");
        constructInvertedIndex();
        int n = pool.getCommunities().size();
        int[] temporaryPool = new int[n];
        boolean[] needsMergeCheck = new boolean[n];
        int i = n - 2;
        boolean merged = false;
        while (i >= 0) {
            Community<T> mergerComm = pool.getCommunities().get(i);
            if (mergerComm == null) {
                i = i - 1;
                continue;
            }
            do {
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
                    Community<T> mergedComm = pool.getCommunities().get(index);
                    if (!needsMergeCheck[index])
                        continue;
                    if (isMergible(mergerComm, mergedComm, mergeFactor)) {
                        merged = true;
                        for (Community<T> c : mergedComm.getDependencyList()) {
                            if (c.getIndex() > mergerComm.getIndex())
                                needsMergeCheck[c.getIndex()] = true;
                        }
                        mergerComm = merge(mergerComm, mergedComm, true);
                    } else {
                        needsMergeCheck[index] = false;
                    }
                }
            } while (merged);
            i = i - 1;
        }
    }

    private Community<T> merge(Community<T> mergerComm,
            Community<T> mergedComm, boolean withDependencies) {
        // members
        int size1 = mergerComm.getMembers().size();
        int size2 = mergedComm.getMembers().size();
        if (size1 > size2
                || (size1 == size2 && mergerComm.getIndex() < mergedComm
                        .getIndex())) {
            mergerComm.getMembers().addAll(mergedComm.getMembers());
            if (withDependencies) {
                mergerComm.getDependencyList().remove(mergedComm);
                mergedComm.getDependencyList().remove(mergerComm);
                for (Community<T> c : mergedComm.getDependencyList()) {
                    c.getDependencyList().add(mergerComm);
                    c.getDependencyList().remove(mergedComm);
                }
                mergerComm.getDependencyList().addAll(
                        mergedComm.getDependencyList());
            }
            pool.getCommunities().set(mergedComm.getIndex(), null);
            return mergerComm;
        } else {
            merge(mergedComm, mergerComm, withDependencies);
            pool.getCommunities().set(mergedComm.getIndex(), null);
            pool.getCommunities().set(mergerComm.getIndex(), mergedComm);
            mergedComm.setIndex(mergerComm.getIndex());
            return mergedComm;
        }
    }

    private void graphBasedMerge(double mergeFactor) {
        constructInvertedIndex();
        System.out.println("Merging---> Started.");

        int n = pool.getCommunities().size();
        int[] temporaryPool = new int[n];

        int i = n - 2;
        boolean merged = false;
        while (i >= 0) {
            Community<T> mergerComm = pool.getCommunities().get(i);
            if (mergerComm == null) {
                i = i - 1;
                continue;
            }
            do {
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
                        mergerComm = merge(mergerComm, mergedComm, true);
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
    private void quadraticMerge(CommunityList<T> pool, double mergeFactor) {

        /* merging part pooling approach */
        int j;
        int n = pool.getCommunities().size();
        int i = n - 2;
        boolean merged = false;
        while (i >= 0) {
            j = i + 1;
            Community<T> mergerComm = pool.getCommunities().get(i);
            if (mergerComm == null)
                continue;
            do {
                merged = false;
                while (j < n) {
                    Community<T> mergedComm = pool.getCommunities().get(j);
                    if (mergedComm != null
                            && isMergible(mergerComm, mergedComm, mergeFactor)) {
                        mergerComm = merge(mergerComm, mergedComm, false);
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
        System.out.println("dependency construction--> done.");

    }

}
