package labelPropagation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class EgoNetwork {
	private ArrayList<HashMap<String,HashSet<String>>> subGraph;

	public EgoNetwork() {
		super();
		// TODO Auto-generated constructor stub
		subGraph=new ArrayList<HashMap<String,HashSet<String>>>();
	}

	public ArrayList<HashMap<String, HashSet<String>>> getSubGraph() {
		return subGraph;
	}

	public void setSubGraph(ArrayList<HashMap<String, HashSet<String>>> subGraph) {
		this.subGraph = subGraph;
	}

	@Override
	public String toString() {
		return "EgoNetwork [subGraph=" + subGraph + "]";
	} 
}
