package labelPropagation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class LocalNetwork {
	private HashMap<String, HashSet<String>> localNetwork;


	public HashMap<String, HashSet<String>> getLocalNetwork() {
		return localNetwork;
	}

	public void setLocalNetwork(HashMap<String, HashSet<String>> localNetwork) {
		this.localNetwork = localNetwork;
	}

	public LocalNetwork() {
		super();
		// TODO Auto-generated constructor stub
	}

	public LocalNetwork(EgoNetwork[] egos) {
		constructHashMap(egos);
	}

	private void constructHashMap(EgoNetwork[] egos){
	localNetwork=new HashMap<String, HashSet<String>>(); 
	for (int i = 0; i < egos.length; i++) {
		for(int j=0;j<egos[i].getSubGraph().size();j++){
			for(Entry<String, HashSet<String>> entry : egos[i].getSubGraph().get(j).entrySet())
				localNetwork.put(entry.getKey(),entry.getValue());
		}
	}
}
}
