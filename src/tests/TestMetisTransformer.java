package tests;

import java.io.IOException;

import utils.GraphLoader;
import utils.MetisTransformer;

public class TestMetisTransformer {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		GraphLoader graphLoader = new GraphLoader("traininGraph.txt");
		 MetisTransformer mt=new MetisTransformer(graphLoader.getGraph());
		// mt.weightedMinCutMetis();
		 mt.minCutMetis();
	}

}
