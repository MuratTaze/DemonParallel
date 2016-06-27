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
	private CommunityList<T> pool = null;
	private LabelPropagation<T> lp;
	private int numberOfComparison = 0;
	public ArrayList[] requests;
	public ArrayList[] responses;
	HashMap<Vertex<T>, HashSet<Vertex<T>>> connections;
	public RequestPacket[] packetRequest;
	public ResponsePacket[] packetResponse;

	public int getNumberOfComparison() {
		return numberOfComparison;
	}

	public void setNumberOfComparison(int numberOfComparison) {
		this.numberOfComparison = numberOfComparison;
	}

	public CommunityList<T> getGlobalCommunities() {
		return pool;
	}

	public DemonParallel() {

		super();
		lp = new LabelPropagation<T>();

	}

	public DemonParallel(ArrayList[] requestArray, ArrayList[] responseArray, ArrayList[] requests,
			ArrayList[] responses, ArrayList[] sendReceiveRequest, ArrayList[] sendReceiveResponse,
			RequestPacket[] packetRequest, ResponsePacket[] packetResponse) {
		super();
		lp = new LabelPropagation<T>();
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
	private void connectionBasedRemoteAccess(Network<T> graph, int numberOfVertices) {
		HashMap<Vertex<T>, NeighborList<T>> map = graph.getGraph();

		connections = findExternalNodes(map);
		/* assign each boundry vertex to corresponding index(processor) */
		// assignNodesToProcs(connections, numberOfVertices);
		ArrayList[] toBeSent = new ArrayList[PCJ.threadCount()];
		PCJ.barrier();
		double startTime = System.nanoTime();

		ArrayList[] remoteNeighborsSent = getConnections(toBeSent, map);
		double estimatedTime = (System.nanoTime() - startTime) / 1000000000.;
		// System.out.println("Time for asking connections: " + estimatedTime +
		// " seconds");

		map = null;
		startTime = System.nanoTime();
		/* keep all valuable information in auxiliary hash map */
		fillAuxGraph(null, remoteNeighborsSent);
		estimatedTime = (System.nanoTime() - startTime) / 1000000000.;
		// System.out.println("Time for building aux. graph: " + estimatedTime +
		// " seconds");

	}

	/**
	 * This method is used to fetch the neighbor list of boundary vertices.
	 * 
	 * @param graph
	 * @param numberOfVertices
	 */
	private HashMap<Vertex<T>, HashSet<Vertex<T>>> findExternalNodes(HashMap<Vertex<T>, NeighborList<T>> map) {
		HashMap<Vertex<T>, HashSet<Vertex<T>>> conns = new HashMap<Vertex<T>, HashSet<Vertex<T>>>();

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
				if (!conns.containsKey(v)) {
					if (iter.hasNext())
						conns.put(v, new HashSet<Vertex<T>>());
					else
						break;
				}
				iter.remove();
				conns.get(v).addAll(friends);
			}
		}

		return conns;
	}

	private void assignNodesToProcs(int numberOfVertices) {
		int numberOfThreads = PCJ.threadCount();
		int arraySize = (numberOfVertices / numberOfThreads) + 1;
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
	}

	private ArrayList[] performDegreeComm(Network<T> graph) {
		PCJ.barrier();
		for (int i = 0; i < PCJ.threadCount(); i++) {
			/*
			 * each thread fetches requests and put the result to requester
			 * threads response array.
			 */
			if (PCJ.myId() == i)
				continue;
			ArrayList<Vertex<T>> currentRequest = PCJ.get(i, "requestArray", PCJ.myId());
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

	private void categorizeBoundries(RequestPacket[] requestPackets, ArrayList[] computedDegrees) {
		int i = 0;
		int j = 0;

		for (ArrayList<Vertex<T>> l : requestArray) {
			if (l != null) {
				j = 0;
				for (Vertex<T> v : l) {
					/* bu liste neighborlarý getirilecekleri tutuyor */
					if (connections.get(v).size() > ((Integer) (computedDegrees[i].get(j)))) {
						if (requestPackets[i] == null) {
							requestPackets[i] = new RequestPacket<T>();
						}
						requestPackets[i].getNeighborlistQuery().add(v);

					} else {
						/*
						 * bu liste connecitoionslarý tutuyor connections
						 * arraylist yokki!!!
						 */
						if (requestPackets[i] == null) {
							requestPackets[i] = new RequestPacket<T>();
						}
						requestPackets[i].getConnectionListQuery().add(new NeighborList<>(v, connections.get(v)));

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
			ArrayList<Vertex<T>> currentRequest = PCJ.get(i1, "sendReceiveRequest", PCJ.myId());
			if (currentRequest != null && currentRequest.size() != 0) {
				ArrayList<NeighborList<T>> neighbors = getNeighbors(currentRequest, graph);
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
			ArrayList<NeighborList<T>> currentResponse = PCJ.get(j1, "sendReceiveResponse", PCJ.myId());
			if (currentResponse != null && currentResponse.size() != 0) {
				remoteNeighborsFetched[j1] = currentResponse;
			}
		}
		return remoteNeighborsFetched;
	}

	private ArrayList[] getNeighborlists(Network<T> graph, ArrayList[] toBeFetched) {
		int numberOfThreads = PCJ.threadCount();
		int arraySize = (GraphLoader.numberOfElements / numberOfThreads) + 1;
		for (Vertex<T> v : connections.keySet()) {
			int hashCode = v.hashCode() % GraphLoader.numberOfElements;
			int index = hashCode / arraySize;
			if (toBeFetched[index] == null)
				toBeFetched[index] = new ArrayList<Vertex<T>>();
			toBeFetched[index].add(v);
		}

		int k = 0;
		for (Object obj : toBeFetched) {

			sendReceiveRequest[k] = (ArrayList<Vertex<T>>) obj;
			k++;
		}
		PCJ.barrier();
		for (int i1 = 0; i1 < PCJ.threadCount(); i1++) {

			if (PCJ.myId() == i1)
				continue;
			ArrayList<Vertex<T>> currentRequest = PCJ.get(i1, "sendReceiveRequest", PCJ.myId());
			if (currentRequest != null && currentRequest.size() != 0) {
				ArrayList<NeighborList<T>> neighbors = getNeighbors(currentRequest, graph);
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
			ArrayList<NeighborList<T>> currentResponse = PCJ.get(j1, "sendReceiveResponse", PCJ.myId());
			if (currentResponse != null && currentResponse.size() != 0) {
				remoteNeighborsFetched[j1] = currentResponse;
			}
		}
		return remoteNeighborsFetched;
	}

	private void neigborlistBasedRemoteAccess(Network<T> graph, int numberOfVertices) {

		HashMap<Vertex<T>, NeighborList<T>> map = graph.getGraph();
		connections = findExternalNodes(map);
		/* assign each boundry vertex to corresponding index(processor) */
		// assignNodesToProcs(connections, numberOfVertices);
		/* lets fetch computed related degrees */

		ArrayList[] toBeFetched = new ArrayList[PCJ.threadCount()];

		ArrayList[] remoteNeighborsFetched = getNeighborlists(graph, toBeFetched);

		PCJ.barrier();

		map = null;
		/* keep all valuable information in auxiliary hash map */
		fillAuxGraph(remoteNeighborsFetched, null);

	}

	private void fillAuxGraph(ArrayList[] remoteNeighborsFetched, ArrayList[] remoteNeighborsSent) {

		int counter = 0;
		double startTime = System.nanoTime();
		if (remoteNeighborsFetched != null)
			for (ArrayList<NeighborList<T>> arrayList : remoteNeighborsFetched) {
				if (arrayList != null)
					for (NeighborList<T> nl : arrayList) {
						{

							/*
							 * 4->1 , 2, 5, 6 4->2, 5, 8, 9, 7,
							 */
							connections.get(nl.getHeadVertex()).retainAll(nl.getListOfNeighbors());
						}
					}
			}
		double estimatedTime = (System.nanoTime() - startTime) / 1000000000.;
		counter = 0;
		startTime = System.nanoTime();
		if (remoteNeighborsSent != null)
			for (ArrayList<NeighborList<T>> arrayList : remoteNeighborsSent) {
				if (arrayList != null)
					for (NeighborList<T> nl : arrayList) {
						{
							connections.get(nl.getHeadVertex()).retainAll(nl.getListOfNeighbors());
						}
					}
			}
		estimatedTime = (System.nanoTime() - startTime) / 1000000000.;
	}

	private ArrayList[] getConnections(ArrayList[] toBeSent, HashMap<Vertex<T>, NeighborList<T>> map) {

		ArrayList[] remoteNeighborsSent = new ArrayList[PCJ.threadCount()];

		convertToNeighborList(toBeSent);
		int i3 = 0;
		for (Object obj : toBeSent) {

			requests[i3] = (ArrayList<NeighborList<T>>) obj;
			i3++;
		}
		PCJ.barrier();
		for (int i1 = 0; i1 < PCJ.threadCount(); i1++) {

			if (PCJ.myId() == i1)
				continue;
			ArrayList<NeighborList<T>> currentRequest = PCJ.get(i1, "requests", PCJ.myId());
			if (currentRequest != null && currentRequest.size() != 0) {
				responses[i1] = findEdges(currentRequest, map);
			}
		}
		PCJ.barrier();
		ArrayList[] edges = new ArrayList[PCJ.threadCount()];
		for (int j1 = 0; j1 < PCJ.threadCount(); j1++) {
			if (PCJ.myId() == j1)
				continue;
			ArrayList<NeighborList<T>> currentResponse = PCJ.get(j1, "responses", PCJ.myId());
			if (currentResponse != null && currentResponse.size() != 0) {
				remoteNeighborsSent[j1] = currentResponse;
			}
		}

		return remoteNeighborsSent;
	}

	private ArrayList[] getConnectionsForDegreeBased(ArrayList[] toBeSent, HashMap<Vertex<T>, NeighborList<T>> map) {

		ArrayList[] remoteNeighborsSent = new ArrayList[PCJ.threadCount()];
		ArrayList[] convertedArray = convertToNeighborListWithDegreeInfo(toBeSent);
		int i3 = 0;
		for (Object obj : convertedArray) {

			requests[i3] = (ArrayList<NeighborList<T>>) obj;
			i3++;
		}
		PCJ.barrier();
		for (int i1 = 0; i1 < PCJ.threadCount(); i1++) {

			if (PCJ.myId() == i1)
				continue;
			ArrayList<NeighborList<T>> currentRequest = PCJ.get(i1, "requests", PCJ.myId());
			if (currentRequest != null && currentRequest.size() != 0) {
				responses[i1] = findEdges(currentRequest, map);
			}
		}
		PCJ.barrier();
		ArrayList[] edges = new ArrayList[PCJ.threadCount()];
		for (int j1 = 0; j1 < PCJ.threadCount(); j1++) {
			if (PCJ.myId() == j1)
				continue;
			ArrayList<NeighborList<T>> currentResponse = PCJ.get(j1, "responses", PCJ.myId());
			if (currentResponse != null && currentResponse.size() != 0) {
				remoteNeighborsSent[j1] = currentResponse;
			}
		}

		return remoteNeighborsSent;
	}

	@SuppressWarnings({ "unchecked" })
	private void degreeBasedRemoteAccess(Network<T> graph, int numberOfVertices) {
		HashMap<Vertex<T>, NeighborList<T>> map = graph.getGraph();

		connections = findExternalNodes(map);
		/* assign each boundry vertex to corresponding index(processor) */
		double startTime = System.nanoTime();
		assignNodesToProcs(numberOfVertices);
		double estimatedTime = (System.nanoTime() - startTime) / 1000000000.;

		startTime = System.nanoTime();
		/* lets fetch computed related degrees */
		ArrayList[] computedDegrees = performDegreeComm(graph);
		estimatedTime = (System.nanoTime() - startTime) / 1000000000.;
		// System.out.println("Time for degree comm.: " + estimatedTime + "
		// seconds");

		/* compare degrees with the list of each key */
		RequestPacket[] requestPackets = new RequestPacket[PCJ.threadCount()];
		ResponsePacket[] responsePackets = new ResponsePacket[PCJ.threadCount()];
		startTime = System.nanoTime();
		categorizeBoundries(requestPackets, computedDegrees);
		estimatedTime = (System.nanoTime() - startTime) / 1000000000.;

		performComm(requestPackets, responsePackets, map);

		map = null;
		startTime = System.nanoTime();
		/* keep all valuable information in auxiliary hash map */
		for (ResponsePacket packet : responsePackets)
			fillAuxGraph(packet);
		estimatedTime = (System.nanoTime() - startTime) / 1000000000.;

	}

	private void fillAuxGraph(ResponsePacket packet) {
		if (packet != null) {
			for (NeighborList<T> nl : (ArrayList<NeighborList<T>>) packet.getConnections()) {
				connections.get(nl.getHeadVertex()).retainAll(nl.getListOfNeighbors());
			}
			for (NeighborList<T> nl : (ArrayList<NeighborList<T>>) packet.getNeighborLists()) {
				connections.get(nl.getHeadVertex()).retainAll(nl.getListOfNeighbors());
			}
		}
	}

	private void performComm(RequestPacket[] requestPackets, ResponsePacket[] responsePackets,
			HashMap<Vertex<T>, NeighborList<T>> map) {
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
				packetResponse[i1] = processPacket(currentRequest, map);
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

	private ResponsePacket processPacket(RequestPacket currentRequest, HashMap<Vertex<T>, NeighborList<T>> map) {
		/*
		 * first prepare neighbor lists for the requested vertices second check
		 * the connections lastly return response object
		 */
		ResponsePacket response = new ResponsePacket<T>();
		for (Object obj : currentRequest.getNeighborlistQuery()) {
			Vertex<T> v = (Vertex<T>) obj;
			response.getNeighborLists().add(map.get(v));

		}

		response.setConnections(findEdges(currentRequest.getConnectionListQuery(), map));
		return response;
	}

	private ArrayList<NeighborList> findEdges(ArrayList<NeighborList<T>> currentRequest,
			HashMap<Vertex<T>, NeighborList<T>> map) {
		// TODO Auto-generated method stub

		ArrayList<NeighborList> list = new ArrayList<NeighborList>();
		for (NeighborList<T> neighborList : currentRequest) {
			NeighborList<T> nb = new NeighborList<T>(neighborList.getHeadVertex(), new HashSet<Vertex<T>>());
			for (Vertex<T> v : neighborList.getListOfNeighbors()) {
				if (map.get(neighborList.getHeadVertex()).getListOfNeighbors().contains(v)) {
					nb.getListOfNeighbors().add(v);

				}
			}
			list.add(nb);
		}
		return list;
	}

	/*
	 * this method returns a modified array storing vertices and their
	 * dependencies
	 */
	private void convertToNeighborList(ArrayList[] toBeSent) {
		int numberOfThreads = PCJ.threadCount();
		int arraySize = (GraphLoader.numberOfElements / numberOfThreads) + 1;
		for (Vertex<T> v : connections.keySet()) {
			int hashCode = v.hashCode() % GraphLoader.numberOfElements;
			int index = hashCode / arraySize;
			if (toBeSent[index] == null)
				toBeSent[index] = new ArrayList<NeighborList<T>>();
			toBeSent[index].add(new NeighborList<T>(v, connections.get(v)));
		}

	}

	private ArrayList[] convertToNeighborListWithDegreeInfo(ArrayList[] toBeSent) {
		ArrayList[] result = new ArrayList[PCJ.threadCount()];
		int numberOfThreads = PCJ.threadCount();
		int arraySize = (GraphLoader.numberOfElements / numberOfThreads) + 1;
		for (int i = 0; i < PCJ.threadCount(); i++) {
			if (toBeSent[i] != null) {
				for (Object o : toBeSent[i]) {
					Vertex<T> v = (Vertex<T>) o;
					if (result[i] == null)
						result[i] = new ArrayList<NeighborList<T>>();
					result[i].add(new NeighborList<T>(v, connections.get(v)));
				}
			}
		}

		return result;
	}

	private ArrayList<NeighborList<T>> getNeighbors(ArrayList<Vertex<T>> currentRequest, Network<T> graph) {
		ArrayList<NeighborList<T>> result = new ArrayList<NeighborList<T>>();
		for (Vertex<T> v : currentRequest) {
			result.add((graph.getGraph().get(v)));
		}

		return result;
	}

	private ArrayList<Integer> calculateDegrees(ArrayList<Vertex<T>> currentRequest, Network<T> graph) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		for (Vertex<T> v : currentRequest) {
			result.add(new Integer(graph.getGraph().get(v).getListOfNeighbors().size()));
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
	 *            �?² is the threshold. If it is 1 we merge iff fully
	 *            containment is achieved. If it is 0 two communities are merged
	 *            anyway.
	 * @return returns true if at least �?² fraction of the smaller community
	 *         resides in their intersection.
	 */
	private boolean isMergible(Community<T> community1, Community<T> community2, double mergeFactor) {
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
				NeighborList<T> ego = intersection(network.getGraph().get(neighbor), neighborList);
				if (ego.getListOfNeighbors().size() != 0 && ego != null) {
					for (Vertex<T> v : ego.getListOfNeighbors()) {
						if (result.get(v) == null)
							result.put(v, new NeighborList<T>(v, new HashSet<Vertex<T>>()));
						result.get(v).getListOfNeighbors().add(ego.getHeadVertex());
					}
				}

				if (ego.getListOfNeighbors().size() != 0 && ego != null) {
					if (result.get(neighbor) == null)
						result.put(neighbor, ego);
					else
						result.get(neighbor).getListOfNeighbors().addAll(ego.getListOfNeighbors());
				}

			} else {

				NeighborList<T> nl = new NeighborList<>(neighbor, connections.get(neighbor));

				if (nl != null && nl.getListOfNeighbors() != null && nl.getListOfNeighbors().size() != 0) {
					NeighborList<T> ego = intersection(nl, neighborList);
					if (ego.getListOfNeighbors().size() != 0 && ego != null) {
						for (Vertex<T> v : ego.getListOfNeighbors()) {
							if (result.get(v) == null)
								result.put(v, new NeighborList<T>(v, new HashSet<Vertex<T>>()));
							result.get(v).getListOfNeighbors().add(ego.getHeadVertex());
						}
					}
					if (ego.getListOfNeighbors().size() != 0 && ego != null) {
						if (result.get(neighbor) == null)
							result.put(neighbor, ego);
						else
							result.get(neighbor).getListOfNeighbors().addAll(ego.getListOfNeighbors());
					}

				}

			}
		}

		return new Network<T>(result);
	}

	/**
	 * 
	 * This method computes intersection of given two sets.
	 * 
	 * @param neighborList
	 * @param neighborList2
	 * @return returns intersection of two sets as a new set
	 */
	private NeighborList<T> intersection(NeighborList<T> neighborList, NeighborList<T> neighborList2) {
		HashSet<Vertex<T>> intersection = new HashSet<Vertex<T>>();

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
	public void execute(Network<T> graph, double mergeFactor, int mergingType, int numberOfVertices)
			throws IOException {

		double startTime = System.nanoTime();

		// degreeBasedRemoteAccess(graph, numberOfVertices);
		// neigborlistBasedRemoteAccess(graph, numberOfVertices);
		connectionBasedRemoteAccess(graph, numberOfVertices);

		double estimatedTime = (System.nanoTime() - startTime) / 1000000000.;
		if (PCJ.myId() == 0)
			System.out.println("Total Time: " + estimatedTime + " seconds");

		File file = new File(PCJ.myId() + "_parallelEgo.txt");

		// if file doesnt exists, then create it
		if (!file.exists()) {
			file.createNewFile();
		}

		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		int count = 0;
		pool = new CommunityList<T>();

		@SuppressWarnings("unchecked")
		Vertex<T>[] vertices = new Vertex[graph.getGraph().size()];
		graph.getGraph().keySet().toArray(vertices);

		for (Vertex<T> vertex : vertices) {
			Network<T> eMeN = egoMinusEgo(vertex, graph);
			if (eMeN.getGraph().size() == 0)
				continue;
			bw.write("Ego Network for " + vertex.toString());
			bw.write(eMeN.toString());
			bw.write("\n");
			lp.initiliaze(eMeN.getGraph());
			lp.proceedLP();
			CommunityList<T> localCommunities = lp.extractCommunities();

			// merge each local communities found with global communities
			for (Community<T> localCommunity : localCommunities.getCommunities()) {
				localCommunity.getMembers().add(vertex.getValue());
				localCommunity.setIndex(count);
				count++;
				pool.getCommunities().add(localCommunity);
			}
		}
		bw.close();
		// long startTime = System.nanoTime();
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

		pool = cleanPool(pool);
		// System.out.println("Number of communities after merge is " +
		// pool.getCommunities().size());

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

	private Community<T> merge(Community<T> mergerComm, Community<T> mergedComm, boolean withDependencies) {
		// members
		int size1 = mergerComm.getMembers().size();
		int size2 = mergedComm.getMembers().size();
		if (size1 > size2 || (size1 == size2 && mergerComm.getIndex() < mergedComm.getIndex())) {
			mergerComm.getMembers().addAll(mergedComm.getMembers());
			if (withDependencies) {
				mergerComm.getDependencyList().remove(mergedComm);
				mergedComm.getDependencyList().remove(mergerComm);
				for (Community<T> c : mergedComm.getDependencyList()) {
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

		// System.out.println("Inverted index-->done.");

		/* dependency construction */
		for (ArrayList<Community<T>> list : invertedIndex.values()) {
			for (int i = 0; i < list.size(); i++) {
				list.get(i).getDependencyList().addAll(list);
				list.get(i).getDependencyList().remove(list.get(i));
			}
		}
		// System.out.println("dependency construction--> done.");

	}

}
