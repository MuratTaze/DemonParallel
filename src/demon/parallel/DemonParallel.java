package demon.parallel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

@SuppressWarnings("rawtypes")
public class DemonParallel<T> {

	public ArrayList[] requestArray, responseArray, sendReceiveRequest, sendReceiveResponse;
	private CommunityList<Integer> pool = null;
	private LabelPropagation<Integer> lp;
	private int numberOfComparison = 0;
	public ArrayList[] requests;
	public ArrayList[] responses;
	HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> connections;
	HashMap<Integer, HashSet<Integer>> map;

	public RequestPacket[] packetRequest;
	public ResponsePacket[] packetResponse;

	public int getNumberOfComparison() {
		return numberOfComparison;
	}

	public void setNumberOfComparison(int numberOfComparison) {
		this.numberOfComparison = numberOfComparison;
	}

	public CommunityList<Integer> getGlobalCommunities() {
		return pool;
	}

	public DemonParallel() {

		super();
		lp = new LabelPropagation<Integer>();

	}

	public DemonParallel(ArrayList[] requestArray, ArrayList[] responseArray, ArrayList[] requests,
			ArrayList[] responses, ArrayList[] sendReceiveRequest, ArrayList[] sendReceiveResponse,
			RequestPacket[] packetRequest, ResponsePacket[] packetResponse) {
		super();
		lp = new LabelPropagation<Integer>();
		this.requestArray = requestArray;
		this.responseArray = responseArray;
		this.requests = requests;
		this.responses = responses;
		this.sendReceiveRequest = sendReceiveRequest;
		this.sendReceiveResponse = sendReceiveResponse;
		this.packetRequest = packetRequest;
		this.packetResponse = packetResponse;
	}

	/**
	 * This method is used to learn if there is a connection among given nodes.
	 * 
	 * @param graph
	 * @param numberOfVertices
	 */
	private void connectionBasedRemoteAccess(HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> map) {

		connections = findExternalNodes(map);
		/* assign each boundry vertex to corresponding index(processor) */
		// assignNodesToProcs(connections, numberOfVertices);
		ArrayList[] toBeSent = new ArrayList[PCJ.threadCount()];
		PCJ.barrier();
		transformGraph(map);
		getConnections(toBeSent, map);

	}

	/**
	 * This method is used to fetch the neighbor list of boundary vertices.
	 * 
	 * @param graph
	 * @param numberOfVertices
	 */
	private HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> findExternalNodes(
			HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> map) {

		HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> conns = new HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>>();

		/* this list contains lists of possibly redundant boundaries. */
		ArrayList<ArrayList<Vertex<Integer>>> list = new ArrayList<ArrayList<Vertex<Integer>>>();
		for (HashSet<Vertex<Integer>> nl : map.values()) {
			ArrayList<Vertex<Integer>> friends = new ArrayList<Vertex<Integer>>();
			for (Vertex<Integer> neighbor : nl) {
				if (neighbor.getThreadNumber() == PCJ.myId()) {
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

		for (ArrayList<Vertex<Integer>> friends : list) {
			Iterator<Vertex<Integer>> iter = friends.iterator();
			while (iter.hasNext()) {
				Vertex<Integer> v = iter.next();
				if (!conns.containsKey(v)) {
					if (iter.hasNext())
						conns.put(v, new HashSet<Vertex<Integer>>());
					else
						break;
				}
				iter.remove();
				conns.get(v).addAll(friends);
			}
		}

		return conns;
	}

	@SuppressWarnings("unchecked")
	private void assignNodesToProcs() {

		/*
		 * write only keys which are wanted to know degrees of to corresponding
		 * processor index
		 */
		for (Vertex<Integer> v : connections.keySet()) {

			int index = v.getThreadNumber();
			if (requestArray[index] == null)
				requestArray[index] = new ArrayList();
			requestArray[index].add(v.getValue());

		}
	}

	private ArrayList[] performDegreeComm(HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> graph) {
		PCJ.barrier();
		for (int i = 0; i < PCJ.threadCount(); i++) {
			/*
			 * each thread fetches requests and put the result to requester
			 * threads response array.
			 */
			if (PCJ.myId() == i)
				continue;
			ArrayList<Integer> currentRequest = PCJ.get(i, "requestArray", PCJ.myId());
			if (currentRequest != null && currentRequest.size() != 0) {
				ArrayList<Integer> currentDegrees = calculateDegrees(currentRequest, graph);
				responseArray[i] = currentDegrees;
			}
		}
		PCJ.barrier();

		/* lets fetch computed related degrees */
		ArrayList[] computedDegrees = new ArrayList[PCJ.threadCount()];
		for (int j = 0; j < PCJ.threadCount(); j++) {
			if (PCJ.myId() == j)
				continue;
			ArrayList<Integer> currentResponse = PCJ.get(j, "responseArray", PCJ.myId());
			if (currentResponse != null && currentResponse.size() != 0) {
				computedDegrees[j] = currentResponse;
			}
		}
		return computedDegrees;
	}

	@SuppressWarnings("unchecked")
	private void categorizeBoundries(RequestPacket[] requestPackets, ArrayList[] computedDegrees) {
		int i = 0;
		int j = 0;
		Vertex<Integer> vtx = new Vertex<Integer>();
		for (ArrayList<Integer> l : requestArray) {
			if (l != null) {
				j = 0;
				for (Integer v : l) {
					/* bu liste neighborlarÃ½ getirilecekleri tutuyor */
					vtx.setValue(v);
					if (connections.get(vtx).size() > ((Integer) (computedDegrees[i].get(j)))) {
						if (requestPackets[i] == null) {
							requestPackets[i] = new RequestPacket<Integer>();
						}
						requestPackets[i].getNeighborlistQuery().add(v);

					} else {
						/*
						 * bu liste connecitoionslarÃ½ tutuyor connections
						 * arraylist yokki!!!
						 */
						if (requestPackets[i] == null) {
							requestPackets[i] = new RequestPacket<Integer>();
						}
						/* burada connectionlarý al sýrayla integer */

						vtx.setValue(v);
						requestPackets[i].getConnectionListQuery().add(v);
						for (Vertex<Integer> ss : connections.get(vtx)) {
							requestPackets[i].getConnectionListQuery().add(ss.getValue());
						}
						requestPackets[i].getConnectionListQuery().add(null);

					}
					j = j + 1;
				}
			}
			i = i + 1;
		}
		// System.out.println("Size of sent: " + sent + " Size of fetched: " +
		// fetched);
	}

	private ArrayList[] getNeighborlistsWithDegree(Network<T> graph, ArrayList[] toBeFetched) {

		int k = 0;
		for (Object obj : toBeFetched) {

			sendReceiveRequest[k] = (ArrayList<Vertex<T>>) obj;
			k++;
		}
		PCJ.barrier();
		for (int i1 = 0; i1 < PCJ.threadCount(); i1++) {

			if (PCJ.myId() == i1)
				continue;
			ArrayList<T> currentRequest = PCJ.get(i1, "sendReceiveRequest", PCJ.myId());
			if (currentRequest != null && currentRequest.size() != 0) {
				// ArrayList<NeighborList<T>> neighbors =
				// getNeighbors(currentRequest, graph);
				// sendReceiveResponse[i1] = neighbors;
			}
		}
		PCJ.barrier();
		ArrayList[] remoteNeighborsFetched = new ArrayList[PCJ
				.threadCount()];/*
								 * these are the required answers for the first
								 * part
								 */
		for (int j1 = 0; j1 < PCJ.threadCount(); j1++) {
			if (PCJ.myId() == j1)
				continue;
			ArrayList<NeighborList<T>> currentResponse = PCJ.get(j1, "sendReceiveResponse", PCJ.myId());
			if (currentResponse != null && currentResponse.size() != 0) {
				remoteNeighborsFetched[j1] = currentResponse;
			}
		}
		return remoteNeighborsFetched;
	}

	private ArrayList[] getNeighborlists(HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> graph) {
		ArrayList[] toBeFetched = new ArrayList[PCJ.threadCount()];
		int numberOfThreads = PCJ.threadCount();

		for (Vertex<Integer> v : connections.keySet()) {

			int index = v.getThreadNumber();
			if (toBeFetched[index] == null)
				toBeFetched[index] = new ArrayList<Integer>();
			toBeFetched[index].add(v.getValue());
		}

		int k = 0;
		for (Object obj : toBeFetched) {

			sendReceiveRequest[k] = (ArrayList<Integer>) obj;
			k++;
		}
		PCJ.barrier();
		for (int i1 = 0; i1 < PCJ.threadCount(); i1++) {

			if (PCJ.myId() == i1)
				continue;
			ArrayList<Integer> currentRequest = PCJ.get(i1, "sendReceiveRequest", PCJ.myId());
			if (currentRequest != null && currentRequest.size() != 0) {
				ArrayList<Integer> neighbors = getNeighbors(currentRequest, graph);
				sendReceiveResponse[i1] = neighbors;
			}
		}

		PCJ.barrier();
		ArrayList[] remoteNeighborsFetched = new ArrayList[PCJ
				.threadCount()];/*
								 * these are the required answers for the first
								 * part
								 */
		for (int j1 = 0; j1 < PCJ.threadCount(); j1++) {
			if (PCJ.myId() == j1)
				continue;
			ArrayList<Integer> currentResponse = PCJ.get(j1, "sendReceiveResponse", PCJ.myId());

			if (currentResponse != null && currentResponse.size() != 0) {

				remoteNeighborsFetched[j1] = currentResponse;
			}
		}
		return remoteNeighborsFetched;
	}

	private void neigborlistBasedRemoteAccess(HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> graph) {

		connections = findExternalNodes(graph);
		/* assign each boundry vertex to corresponding index(processor) */
		// assignNodesToProcs(connections, numberOfVertices);
		/* lets fetch computed related degrees */

		ArrayList[] remoteNeighborsFetched = getNeighborlists(graph);

		transformGraph(graph);// convert vertex to integer. we don't need
								// objects
								// anymore
		PCJ.barrier();
		/* keep all valuable information in auxiliary hash map */
		finalizeComm(remoteNeighborsFetched);
	}

	private void transformGraph(HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> graph) {

		for (Entry<Vertex<Integer>, HashSet<Vertex<Integer>>> entry : graph.entrySet()) {
			map.put(entry.getKey().getValue(), new HashSet<Integer>());
			for (Vertex<Integer> neighbor : entry.getValue()) {
				map.get(entry.getKey().getValue()).add(neighbor.getValue());
			}

		}

	}

	@SuppressWarnings("unchecked")
	private void finalizeComm(ArrayList[] remoteNeighborsFetched) {

		for (ArrayList<Integer> list : remoteNeighborsFetched) {
			convertListToHashMap(list);

		}
		keepCommonBoundry();

	}

	/* take intersection of neighbors and boundries */
	private void keepCommonBoundry() {

		HashMap<Integer, HashSet<Integer>> tSet = new HashMap<Integer, HashSet<Integer>>();
		for (Entry<Vertex<Integer>, HashSet<Vertex<Integer>>> entry : connections.entrySet()) {
			tSet.put(entry.getKey().getValue(), new HashSet<Integer>());
			for (Vertex<Integer> v : entry.getValue())
				tSet.get(entry.getKey().getValue()).add(v.getValue());
			if (map.containsKey(entry.getKey().getValue()))
				map.get(entry.getKey().getValue()).retainAll(tSet.get(entry.getKey().getValue()));
		}

	}

	/* given list of integers we are building hashmap */
	private void convertListToHashMap(ArrayList<Integer> list) {
		boolean tail = false;
		boolean head = false;
		Integer tempKey = null;
		HashSet<Integer> tempValue = null;
		if (list != null) {
			for (Integer value : list) {

				if (tail && value != null) {
					map.get(tempKey).add(value);
					head = false;
				}
				if (head && value != null) {
					if (!map.containsKey(value)) {
						map.put(value, tempValue);
					}

					tempKey = value;
					tail = true;

				}
				// burasý acil düzenlenecek
				if (value == null) {
					tempValue = new HashSet<Integer>();
					head = true;
					tail = false;
				}
			}
		}

	}

	private ArrayList[] getConnections(ArrayList[] toBeSent, HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> graph) {

		int numberOfThreads = PCJ.threadCount();

		for (Vertex<Integer> v : connections.keySet()) {

			int index = v.getThreadNumber();
			if (toBeSent[index] == null)
				toBeSent[index] = new ArrayList<Integer>();
			toBeSent[index].add(v.getValue());
			for (Vertex<Integer> n : connections.get(v)) {
				toBeSent[index].add(n.getValue());
			}

			toBeSent[index].add(null);
		}

		int k = 0;
		for (Object obj : toBeSent) {

			sendReceiveRequest[k] = (ArrayList<Integer>) obj;
			k++;
		}
		PCJ.barrier();
		for (int i1 = 0; i1 < PCJ.threadCount(); i1++) {

			if (PCJ.myId() == i1)
				continue;
			ArrayList<Integer> currentRequest = PCJ.get(i1, "sendReceiveRequest", PCJ.myId());
			if (currentRequest != null && currentRequest.size() != 0) {
				ArrayList<Boolean> neighbors = findFriends(currentRequest, graph);
				sendReceiveResponse[i1] = neighbors;
			}
		}

		PCJ.barrier();
		ArrayList[] remoteNeighborsFetched = new ArrayList[PCJ
				.threadCount()];/*
								 * these are the required answers for the first
								 * part
								 */
		for (int j1 = 0; j1 < PCJ.threadCount(); j1++) {
			if (PCJ.myId() == j1)
				continue;
			ArrayList<Boolean> currentResponse = PCJ.get(j1, "sendReceiveResponse", PCJ.myId());

			if (currentResponse != null && currentResponse.size() != 0) {

				remoteNeighborsFetched[j1] = currentResponse;
			}
		}

		/*
		 * gönderdiðin sýrayla(toBeSent) gelen sýra ayný oradan bak ekle map'a
		 */
		for (int i = 0; i < PCJ.threadCount(); i++) {
			if (i != PCJ.myId())
				compare(toBeSent[i], remoteNeighborsFetched[i]);
		}

		return remoteNeighborsFetched;
	}

	private void compare(ArrayList<Integer> list1, ArrayList<Boolean> list2) {

		int key = 0;
		int value = 1;
		int k = 0;
		for (k = 0; k < list2.size(); k++) {
			if (list1.get(key) != null) {
				if (list2.get(k) != null) {
					if (list2.get(k)) {
						if (map.get(list1.get(key)) == null) {
							map.put(list1.get(key), new HashSet<Integer>());
						}
						map.get(list1.get(key)).add(list1.get(value));

						value++;
					} else {

						value++;
					}
				} else {
					key = value + 1;
					value = key + 1;

				}
			}
		}
	}

	private ArrayList<Boolean> findFriends(ArrayList<Integer> currentRequest,
			HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> graph) {
		boolean head = true;
		boolean tail = false;
		ArrayList<Boolean> realResult = new ArrayList<Boolean>();
		Vertex<Integer> v = new Vertex<Integer>();
		Vertex<Integer> x = new Vertex<Integer>();
		for (Integer value : currentRequest) {
			if (head && value != null) {
				v.setValue(value);
				tail = true;
				head = false;
			} else if (tail && value != null) {
				head = false;
				x.setValue(value);
				if (graph.get(v).contains(x))
					realResult.add(true);
				else
					realResult.add(false);
			} else if (value == null) {
				head = true;
				realResult.add(null);
			}

			// result.add((graph.getGraph().get(v)));
		}

		return realResult;
	}

	@SuppressWarnings({ "unchecked" })
	private void degreeBasedRemoteAccess(HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> partition) {

		connections = findExternalNodes(partition);
		/* assign each boundry vertex to corresponding index(processor) */

		assignNodesToProcs();

		/* lets fetch computed related degrees */
		ArrayList[] computedDegrees = performDegreeComm(partition);

		// System.out.println("Time for degree comm.: " + estimatedTime + "
		// seconds");

		/* compare degrees with the list of each key */
		RequestPacket[] requestPackets = new RequestPacket[PCJ.threadCount()];
		ResponsePacket[] responsePackets = new ResponsePacket[PCJ.threadCount()];

		categorizeBoundries(requestPackets, computedDegrees);

		performComm(requestPackets, responsePackets, partition);

		transformGraph(partition);
		boolean required = false;
		/* keep all valuable information in auxiliary hash map */
		for (ResponsePacket packet : responsePackets) {
			if (packet != null && packet.getNeighborLists().size() != 0) {
				required = true;
				convertListToHashMap(packet.getNeighborLists());
			}

		}
		if (required)
			keepCommonBoundry();

		/*
		 * gönderdiðin sýrayla(toBeSent) gelen sýra ayný oradan bak ekle map'a
		 */
		for (int i = 0; i < PCJ.threadCount(); i++) {
			if (i != PCJ.myId()&&requestPackets[i]!=null)
				compare(requestPackets[i].getConnectionListQuery(), responsePackets[i].getConnections());
		}

	}

	private void performComm(RequestPacket[] requestPackets, ResponsePacket[] responsePackets,
			HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> partition) {
		int i = 0;
		for (Object obj : requestPackets) {

			packetRequest[i] = (RequestPacket) obj;
			i++;
		}

		PCJ.barrier();
		for (int i1 = 0; i1 < PCJ.threadCount(); i1++) {

			if (PCJ.myId() == i1)
				continue;
			RequestPacket currentRequest = PCJ.get(i1, "packetRequest", PCJ.myId());
			if (currentRequest != null) {
				packetResponse[i1] = processPacket(currentRequest, partition);
			}
		}
		PCJ.barrier();
		for (int j1 = 0; j1 < PCJ.threadCount(); j1++) {
			if (PCJ.myId() == j1)
				continue;
			ResponsePacket currentResponse = PCJ.get(j1, "packetResponse", PCJ.myId());
			responsePackets[j1] = currentResponse;

		}
	}

	private ResponsePacket processPacket(RequestPacket currentRequest,
			HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> map) {
		/*
		 * first prepare neighbor lists for the requested vertices second check
		 * the connections lastly return response object
		 */
		Vertex<Integer> temp = new Vertex<Integer>();
		ResponsePacket response = new ResponsePacket();
		for (Object obj : currentRequest.getNeighborlistQuery()) {
			Integer v = (Integer) obj;
			temp.setValue(v);
			response.getNeighborLists().add(null);
			response.getNeighborLists().add(v);
			for (Vertex<Integer> neighbor : map.get(temp)) {
				response.getNeighborLists().add(neighbor.getValue());

			}
			response.getNeighborLists().add(null);
		}

		response.setConnections(findFriends(currentRequest.getConnectionListQuery(), map));
		return response;
	}

	private ArrayList<Integer> getNeighbors(ArrayList<Integer> currentRequest,
			HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> graph) {
		// ArrayList<NeighborList<T>> result = new ArrayList<NeighborList<T>>();
		ArrayList<Integer> realResult = new ArrayList<Integer>();
		Vertex<Integer> v = new Vertex<Integer>();
		for (Integer value : currentRequest) {
			v.setValue(value);
			realResult.add(null);
			realResult.add(value);
			for (Vertex<Integer> vertex : graph.get(v))
				realResult.add(vertex.getValue());
		}

		return realResult;
	}

	private ArrayList<Integer> calculateDegrees(ArrayList<Integer> currentRequest,
			HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> graph) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		Vertex<Integer> x = new Vertex<Integer>();
		for (Integer v : currentRequest) {
			x.setValue(v);
			result.add(new Integer(graph.get(x).size()));
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
	 *            ï¿½?Â² is the threshold. If it is 1 we merge iff fully
	 *            containment is achieved. If it is 0 two communities are merged
	 *            anyway.
	 * @return returns true if at least ï¿½?Â² fraction of the smaller community
	 *         resides in their intersection.
	 */
	private boolean isMergible(Community<Integer> community1, Community<Integer> community2, double mergeFactor) {
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
			for (Integer value : community2.getMembers()) {
				if (community1.getMembers().contains(value))
					intersection++;
			}
		} else {
			for (Integer value : community1.getMembers()) {
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
	 * 
	 * This method computes intersection of given two sets.
	 * 
	 * @param neighborList
	 * @param neighborList2
	 * @return returns intersection of two sets as a new set
	 */
	private HashSet<Integer> intersection(HashSet<Integer> neighborList, HashSet<Integer> neighborList2) {
		HashSet<Integer> intersection = new HashSet<Integer>();

		double size1 = neighborList.size();
		double size2 = neighborList2.size();
		double min = size2;
		if (size1 < size2) {
			min = size1;
		}
		if (min == size2) {
			for (Integer value : neighborList2) {
				if (neighborList.contains(value))
					intersection.add(value);
			}
		} else {
			for (Integer value : neighborList) {
				if (neighborList2.contains(value))
					intersection.add(value);
			}
		}

		return intersection;
	}

	/**
	 * Main demon algorithm.
	 * 
	 * @param partition
	 * @param mergeFactor
	 * @param mergingType
	 * @throws IOException
	 */
	public void execute(HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> partition, double mergeFactor,
			int mergingType) throws IOException {
		map = new HashMap<Integer, HashSet<Integer>>();// actual graph used in
														// label propagation

		/* keep original partition as a vertex list */
		Integer[] vertexList = new Integer[partition.size()];
		int abc = 0;
		for (Vertex<Integer> vertex : partition.keySet()) {
			vertexList[abc] = vertex.getValue();
			abc++;
		}

		System.out.println("Thread:" + PCJ.myId() + " " + partition.size());
		double startTime = System.nanoTime();
	 degreeBasedRemoteAccess(partition);
		//neigborlistBasedRemoteAccess(partition);
			// connectionBasedRemoteAccess(partition);
		partition = null;
		double estimatedTime = (System.nanoTime() - startTime) / 1000000000.;
		// if (PCJ.myId() == 0)
		System.out.println("Total Time for Remote Access: " + estimatedTime + " seconds");

		int count = 0;
		pool = new CommunityList<Integer>();

		estimatedTime = 0;
		startTime = System.nanoTime();

		for (Integer vertex : vertexList) {

			// find the ego minus ego network of given vertex
			HashMap<Integer, HashSet<Integer>> egoNetwork = egoMinusEgo(vertex);

			if (egoNetwork.size() == 0)
				continue;

			lp.initiliaze(egoNetwork);
			lp.proceedLP();
			CommunityList<Integer> localCommunities = lp.extractCommunities();

			// merge each local communities found with global communities
			for (Community<Integer> localCommunity : localCommunities.getCommunities()) {
				localCommunity.getMembers().add(vertex);
				localCommunity.setIndex(count);
				count++;
				pool.getCommunities().add(localCommunity);
			}
		}
		estimatedTime = (System.nanoTime() - startTime) / 1000000000.;
		// if (PCJ.myId() == 0)
		System.out.println("EGO + Label Propagation: " + " Thread:" + PCJ.myId() + "  " + estimatedTime
				+ " seconds"+ vertexList.length+" vertices.");

		// long startTime = System.nanoTime();
		startTime = System.nanoTime();
		Collections.sort(pool.getCommunities(), Collections.reverseOrder());
		int a = 0;
		for (Community<Integer> c : pool.getCommunities()) {
			c.setIndex(a);
			a++;
		}
		if (mergingType == 1) {
			improvedGraphBasedMerge(mergeFactor);
		} else {
			quadraticMerge(pool, mergeFactor);
		}

		pool = cleanPool(pool);
		// System.out.println("Number of communities after merge is " +
		// pool.getCommunities().size());
		estimatedTime = (System.nanoTime() - startTime) / 1000000000.;
		// if (PCJ.myId() == 0)
		System.out.println("Total Time for Merge: " + " Thread:" + PCJ.myId() + "  " + estimatedTime + " seconds");
	}

	/**
	 * This method constructs ego minus ego network of a given vertex.
	 * 
	 * @param vertex
	 * @return the network which is constructed without above vertex. It
	 *         includes edges between neighbors of the above vertex.
	 */
	private HashMap<Integer, HashSet<Integer>> egoMinusEgo(Integer vertex) {

		HashMap<Integer, HashSet<Integer>> result = new HashMap<Integer, HashSet<Integer>>();
		HashSet<Integer> neighborList = map.get(vertex);

		for (Integer neighbor : neighborList) {
			/*
			 * fetch the neighbors of current neighbor. if it returns null we
			 * will do remote access
			 */
			if (map.get(neighbor) != null) {
				/*
				 * remove neighbors of current neighbor which are not included
				 * in neighbor list.
				 */
				HashSet<Integer> ego = intersection(map.get(neighbor), map.get(vertex));
				for (Integer x : ego) {
					if (result.get(x) == null)
						result.put(x, new HashSet<Integer>());
					result.get(x).add(neighbor);
				}
				result.put(neighbor, ego);
			}
		}
		return result;
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
		// System.out.println("Merging---> Started.");
		constructInvertedIndex();
		int n = pool.getCommunities().size();
		int[] temporaryPool = new int[n];
		boolean[] needsMergeCheck = new boolean[n];
		int i = n - 2;
		boolean merged = false;
		while (i >= 0) {
			Community<Integer> mergerComm = pool.getCommunities().get(i);
			if (mergerComm == null) {
				i = i - 1;
				continue;
			}
			do {
				int temporaryPoolSize = 0;
				for (Community<Integer> dependecy : mergerComm.getDependencyList()) {
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
					Community<Integer> mergedComm = pool.getCommunities().get(index);
					if (!needsMergeCheck[index])
						continue;
					if (isMergible(mergerComm, mergedComm, mergeFactor)) {
						merged = true;
						for (Community<Integer> c : mergedComm.getDependencyList()) {
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

	private Community<Integer> merge(Community<Integer> mergerComm, Community<Integer> mergedComm,
			boolean withDependencies) {
		// members
		int size1 = mergerComm.getMembers().size();
		int size2 = mergedComm.getMembers().size();
		if (size1 > size2 || (size1 == size2 && mergerComm.getIndex() < mergedComm.getIndex())) {
			mergerComm.getMembers().addAll(mergedComm.getMembers());
			if (withDependencies) {
				mergerComm.getDependencyList().remove(mergedComm);
				mergedComm.getDependencyList().remove(mergerComm);
				for (Community<Integer> c : mergedComm.getDependencyList()) {
					c.getDependencyList().add(mergerComm);
					c.getDependencyList().remove(mergedComm);
				}
				mergerComm.getDependencyList().addAll(mergedComm.getDependencyList());
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
		// System.out.println("Merging---> Started.");

		int n = pool.getCommunities().size();
		int[] temporaryPool = new int[n];

		int i = n - 2;
		boolean merged = false;
		while (i >= 0) {
			Community<Integer> mergerComm = pool.getCommunities().get(i);
			if (mergerComm == null) {
				i = i - 1;
				continue;
			}
			do {
				int temporaryPoolSize = 0;
				for (Community<Integer> dependecy : mergerComm.getDependencyList()) {
					int indexOfCommunity = dependecy.getIndex();
					if (indexOfCommunity > mergerComm.getIndex()) {
						temporaryPool[temporaryPoolSize++] = indexOfCommunity;
					}
				}
				merged = false;
				for (int k = 0; k < temporaryPoolSize; ++k) {
					int index = temporaryPool[k];
					Community<Integer> mergedComm = pool.getCommunities().get(index);
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
	private void quadraticMerge(CommunityList<Integer> pool, double mergeFactor) {

		/* merging part pooling approach */
		int j;
		int n = pool.getCommunities().size();
		int i = n - 2;
		boolean merged = false;
		while (i >= 0) {
			j = i + 1;
			Community<Integer> mergerComm = pool.getCommunities().get(i);
			if (mergerComm == null)
				continue;
			do {
				merged = false;
				while (j < n) {
					Community<Integer> mergedComm = pool.getCommunities().get(j);
					if (mergedComm != null && isMergible(mergerComm, mergedComm, mergeFactor)) {
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
	private CommunityList<Integer> cleanPool(CommunityList<Integer> pool) {
		Iterator<Community<Integer>> iter = pool.getCommunities().iterator();
		CommunityList<Integer> cleanedPool = new CommunityList<Integer>();
		while (iter.hasNext()) {
			Community<Integer> community = iter.next();
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
		HashMap<Integer, ArrayList<Community<Integer>>> invertedIndex = null;

		/* inverted index extraction */
		invertedIndex = new HashMap<Integer, ArrayList<Community<Integer>>>();
		for (Community<Integer> community : pool.getCommunities()) {
			for (Integer member : community.getMembers()) {
				ArrayList<Community<Integer>> ii = invertedIndex.get((member));
				if (ii == null) {
					ii = new ArrayList<Community<Integer>>();
					invertedIndex.put(member, ii);
				}

				ii.add(community);
			}

		}

		// System.out.println("Inverted index-->done.");

		/* dependency construction */
		for (ArrayList<Community<Integer>> list : invertedIndex.values()) {
			for (int i = 0; i < list.size(); i++) {
				list.get(i).getDependencyList().addAll(list);
				list.get(i).getDependencyList().remove(list.get(i));
			}
		}
		// System.out.println("dependency construction--> done.");

	}

}
