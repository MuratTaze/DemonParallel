package demon;

import java.io.IOException;

import labelPropagation.GraphLoader;
import labelPropagation.Network;

public class DemonSequantial {

    @SuppressWarnings("rawtypes")
    public static void main(String[] args) throws IOException {
	GraphLoader graphLoader = new GraphLoader("traininGraph.txt");
	Network[] graph = graphLoader.partition(0, 1);
	Demon demon = new Demon();
	/*
	 * change merge factor to see its effect 1 means bigger fully contains
	 * smaller community
	 */
	demon.execute(graph, 1);
	System.out.println("Overlapping Communities = "+demon.getGlobalCommunities());
    }
}