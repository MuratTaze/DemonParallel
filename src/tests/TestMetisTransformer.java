package tests;

import java.io.IOException;

import utils.GraphLoader;
import utils.MetisTransformer;

public class TestMetisTransformer {

	public static void main(String[] args) throws IOException {
		
		GraphLoader graphLoader = new GraphLoader("com-amazon.ungraph.txt");
		 MetisTransformer mt=new MetisTransformer(graphLoader.getGraph());
		 mt.weightedMinCutMetis();
		// mt.minCutMetis();
		 
	}

}
