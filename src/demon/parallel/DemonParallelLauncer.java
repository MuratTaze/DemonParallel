package demon.parallel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;

import labelPropagation.CommunityList;
import labelPropagation.GraphLoader;
import labelPropagation.NeighborList;

public class DemonParallelLauncer extends Storage implements StartPoint {
	@Shared
	ArrayList<NeighborList<Integer>>[] array;
	@Shared
	CommunityList<Integer> globalCommunities;

	@SuppressWarnings("rawtypes")
	@Shared
	ArrayList[] requestArray, responseArray, requests, responses,
			sendReceiveRequest, sendReceiveResponse;

	public void main() throws IOException {

		/*
		 * double epsilon = 0; do { runExperiment(epsilon); epsilon = epsilon +
		 * 0.1; } while (epsilon <= 1.0);
		 */
		runExperiment(1);
	}

	private void runExperiment(double epsilon) throws IOException {
		// TODO Auto-generated method stub
		requestArray = new ArrayList[PCJ.threadCount()];
		responseArray = new ArrayList[PCJ.threadCount()];
		requests = new ArrayList[PCJ.threadCount()];
		responses = new ArrayList[PCJ.threadCount()];
		sendReceiveRequest = new ArrayList[PCJ.threadCount()];
		sendReceiveResponse = new ArrayList[PCJ.threadCount()];
		GraphLoader graphLoader = new GraphLoader("traininGraph.txt");
		int numberOfVertices = GraphLoader.numberOfElements;
		Indexer<Integer> indexer = new Indexer<Integer>();

		array = indexer.index(graphLoader.getNetwork());
		graphLoader = null;
		try {

			File file = new File("input-" + PCJ.myId() + ".txt");

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("Partition indexed by " + " thread " + PCJ.myId() + "..\n");
			for (ArrayList<NeighborList<Integer>> nb : array) {
				if (nb != null)
					bw.write(nb.toString() + "\n");
			}
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		DemonParallel<Integer> demon = new DemonParallel<Integer>(requestArray,
				responseArray, requests, responses, sendReceiveRequest,
				sendReceiveResponse);
		/*
		 * change merge factor to see its effect. 1 mean s merge communities iff
		 * bigger community fully contains smaller community
		 */
		PCJ.barrier();
		demon.execute(indexer.getLocalNetwork(), epsilon, 1, numberOfVertices);

		globalCommunities = demon.getGlobalCommunities();
		PrintWriter writer2 = new PrintWriter(new File(PCJ.myId()
				+ "_ParallelOutput.txt"));
		writer2.print(demon.getGlobalCommunities());
		writer2.flush();
		writer2.close();
		// call performGlobalMerge() here

	}

	public static void main(String[] args) {
		// String[] nodes = new String[] { "localhost", "localhost" };
		String[] nodes = new String[] {"localhost", "localhost"};
		
		PCJ.deploy(DemonParallelLauncer.class, DemonParallelLauncer.class,
				nodes);
	}
}
