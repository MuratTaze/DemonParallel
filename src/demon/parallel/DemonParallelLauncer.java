package demon.parallel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;

import labelPropagation.CommunityList;
import labelPropagation.GraphLoader;
import labelPropagation.LabelPropagation;
import labelPropagation.NeighborList;
import labelPropagation.Vertex;

public class DemonParallelLauncer extends Storage implements StartPoint {
	@Shared
	ArrayList<NeighborList<Integer>>[] array;
	@Shared
	CommunityList<Integer> globalCommunities;

	@SuppressWarnings("rawtypes")
	@Shared
	ArrayList[] requestArray, responseArray, requests, responses,
			sendReceiveRequest, sendReceiveResponse;
	@SuppressWarnings("rawtypes")
	@Shared
	RequestPacket[] packetRequest;
	@SuppressWarnings("rawtypes")
	@Shared
	ResponsePacket[] packetResponse;

	@Shared
	HashMap[] partitions;

	public void main() throws IOException {

		/*
		 * double epsilon = 0; do { runExperiment(epsilon); epsilon = epsilon +
		 * 0.1; } while (epsilon <= 1.0);
		 */

		runExperiment(1);

	}

	@SuppressWarnings("unchecked")
	private void runExperiment(double epsilon) throws IOException {
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
			double starttime1=System.nanoTime();
			GraphLoader graphLoader = new GraphLoader("Email-Enron.txt");
			Partitioner partitioner = new Partitioner(graphLoader.getGraph());
			partitions = partitioner.randomPartitioning();
			double estimatedTime1 = (System.nanoTime() - starttime1) / 1000000000.;
			System.out.println("Total Partitioning Time:" + estimatedTime1);
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

		DemonParallel<Integer> demon = new DemonParallel<Integer>(requestArray,
				responseArray, requests, responses, sendReceiveRequest,
				sendReceiveResponse, packetRequest, packetResponse);

		demon.execute(partition, epsilon, 1);
		globalCommunities = demon.getGlobalCommunities();
		double estimatedTime = (System.nanoTime() - startTime) / 1000000000.;
		System.out.println("Total Time Thread:" +PCJ.myId()+"   "+ estimatedTime);
		//System.out.println(globalCommunities.getCommunities());
		// call performGlobalMerge() here

	}

	public static void main(String[] args) {
	String[] nodes = new String[] { "localhost","localhost"};

		PCJ.deploy(DemonParallelLauncer.class, DemonParallelLauncer.class,
				nodes);

	}
}
