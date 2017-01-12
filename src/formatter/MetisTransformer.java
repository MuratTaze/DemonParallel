package formatter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import labelPropagation.GraphLoader;
import labelPropagation.Vertex;

public class MetisTransformer {
	HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> graph;

	public MetisTransformer() {
		super();
		// TODO Auto-generated constructor stub
	}

	public MetisTransformer(
			HashMap<Vertex<Integer>, HashSet<Vertex<Integer>>> graph) {
		this.graph = graph;
	}

	public void weightedMinCutMetis() throws FileNotFoundException {
		Integer[] vertexList = new Integer[graph.size()];
		HashMap<Integer,Integer> idMap=new HashMap<Integer, Integer>();
		int abc = 0;
		for (Vertex<Integer> vertex : graph.keySet()) {
			vertexList[abc] = vertex.getValue();
			abc++;
		}
		
		Arrays.sort(vertexList);
		int id=1;
		for(Integer i:vertexList){
			idMap.put(i,id);
			id++;
		}
		
		PrintWriter writer = new PrintWriter(new File("FormattedTraining.txt"));
		writer.print(graph.keySet().size());//number of vertices
		writer.print(" ");
		
		writer.print(GraphLoader.edgeCount/2);//number of edges
		writer.print(" ");
		writer.print(10);// the graph has vertex weights
		writer.print(" ");
		writer.print(1);
		writer.print("\n");
		Vertex<Integer> vrt=new Vertex<Integer>();
		for(Integer i:vertexList){
			vrt.setValue(i);
			writer.print(graph.get(vrt).size());
			writer.print("    ");
			for(Vertex<Integer> neighbor:graph.get(vrt)){
				writer.print(idMap.get(neighbor.getValue()));
				writer.print("  ");
			}
			writer.print("\n");
			
		}
		
		
		
		
		
		
		
		
		
		
		
		
		   writer.flush();
	        writer.close();
	}

	public void minCutMetis() throws FileNotFoundException {
		Integer[] vertexList = new Integer[graph.size()];
		HashMap<Integer,Integer> idMap=new HashMap<Integer, Integer>();
		int abc = 0;
		for (Vertex<Integer> vertex : graph.keySet()) {
			vertexList[abc] = vertex.getValue();
			abc++;
		}
		
		Arrays.sort(vertexList);
		int id=1;
		for(Integer i:vertexList){
			idMap.put(i,id);
			id++;
		}
		
		PrintWriter writer = new PrintWriter(new File("FormattedTraining.txt"));
		writer.print(graph.keySet().size());//number of vertices
		writer.print(" ");
		
		writer.print(GraphLoader.edgeCount/2);//number of edges
		
		writer.print("\n");
		Vertex<Integer> vrt=new Vertex<Integer>();
		for(Integer i:vertexList){
			vrt.setValue(i);
			for(Vertex<Integer> neighbor:graph.get(vrt)){
				writer.print(idMap.get(neighbor.getValue()));
				writer.print("  ");
			}
			writer.print("\n");
			
		}
		
		
		
		
		
		
		
		
		
		
		
		
		
		   writer.flush();
	        writer.close();
		
	}
}
