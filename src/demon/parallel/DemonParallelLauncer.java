package demon.parallel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import labelPropagation.CommunityList;
import labelPropagation.NeighborList;
import labelPropagation.Vertex;
import utils.DemonGlobalMergeFunctions;
import utils.GraphLoader;

import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;

public class DemonParallelLauncer extends Storage implements StartPoint {
	@Shared
	ArrayList<NeighborList<Integer>>[] array;
	@Shared
	CommunityList<Integer> globalCommunities;

	@SuppressWarnings("rawtypes")
	@Shared
	ArrayList[] requestArray, responseArray, requests, responses, sendReceiveRequest, sendReceiveResponse;
	@SuppressWarnings("rawtypes")
	@Shared
	RequestPacket[] packetRequest;
	@SuppressWarnings("rawtypes")
	@Shared
	ResponsePacket[] packetResponse;

	@SuppressWarnings("rawtypes")
	@Shared
	HashMap[] partitions;
	@Shared
	static String[] arg;

	public void main() throws IOException {

		/*
		 * double epsilon = 0; do { runExperiment(epsilon); epsilon = epsilon +
		 * 0.1; } while (epsilon <= 1.0);
		 */
		runExperiment();

	}

	@SuppressWarnings("unchecked")
	private void runExperiment() throws IOException {
		// TODO Auto-generated method stub
		double startTime = System.nanoTime();
		requestArray = new ArrayList[PCJ.threadCount()];
		responseArray = new ArrayList[PCJ.threadCount()];
		requests = new ArrayList[PCJ.threadCount()];
		responses = new ArrayList[PCJ.threadCount()];
		sendReceiveRequest = new ArrayList[PCJ.threadCount()];
		sendReceiveResponse = new ArrayList[PCJ.threadCount()];
		packetRequest = new RequestPacket[PCJ.threadCount()];
		packetResponse = new ResponsePacket[PCJ.threadCount()];

		/* master thread partitions the graph */
		if (PCJ.myId() == 0) {

			GraphLoader graphLoader = new GraphLoader(arg[0]);
			Partitioner partitioner = new Partitioner(graphLoader.getGraph());

			switch (Integer.parseInt(arg[1])) {
			case 1:
				partitions = partitioner.metisPartitioning(arg[2], graphLoader.getVertexExistenceTable());
				break;
			case 2:
				partitions = partitioner.degreeBalancedBfsPartitioning();
				break;
			case 3:
				partitions = partitioner.bfsPartitioning();
				break;
			case 4:
				partitions = partitioner.randomPartitioning();
				break;
			default:
				partitions = partitioner.degreeBalancedBfsPartitioning();
				break;
			}

			/*
			 * 
			 */

		}
		PCJ.barrier();

		/* each threads takes its own partition */
		HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> partition;

		if (PCJ.myId() != 0) {

			partition = PCJ.get(0, "partitions", PCJ.myId());

		} else {
			partition = partitions[0];
		}

		PCJ.barrier();
		partitions = null;

		DemonParallel<Integer> demon = new DemonParallel<Integer>(requestArray, responseArray, requests, responses,
				sendReceiveRequest, sendReceiveResponse, packetRequest, packetResponse);

		demon.execute(partition, Double.parseDouble(arg[3]), Integer.parseInt(arg[4]), Integer.parseInt(arg[5]));
		globalCommunities = demon.getGlobalCommunities();

		//
		// global merging
		DemonGlobalMergeFunctions func = new DemonGlobalMergeFunctions(globalCommunities);
		globalCommunities = func.naiveGlobalMerge();

		double estimatedTime = (System.nanoTime() - startTime) / 1000000000.;
		if (PCJ.myId() == 0) {
			System.out.println("Total Time :" + estimatedTime);
		}
		// System.out.println(globalCommunities.getCommunities());
	}

	/**
	 * 
	 * @param filename,
	 *            partitioning type, metis filename, merge factor, merging type,
	 *            communication type, number of processors
	 */
	public static void main(String[] args) {

		String[] nodes = new String[Integer.parseInt(args[6])];
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = "trabzon";
		}
		arg = args;
		PCJ.deploy(DemonParallelLauncer.class, DemonParallelLauncer.class, nodes);

	}
}
