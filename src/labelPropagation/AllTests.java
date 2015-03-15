package labelPropagation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.junit.Test;

public class AllTests {

	

	@Test
	public void testFindMostCommonlyUsedId() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitiliaze() {
		GraphLoader graph = null;
		try {
			graph = new GraphLoader("traininGraph.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		EgoNetwork[] egos=graph.partition(0,1);
		LocalNetwork demon=new LocalNetwork(egos);
		LabelPropagation lp = new LabelPropagation();
		lp.initiliaze(demon.getLocalNetwork());
		lp.proceedLP();
		ArrayList<HashSet<String>> kominiteler=lp.constructCommunities();
		for (Iterator<HashSet<String>> iterator = kominiteler.iterator(); iterator.hasNext();) {
			HashSet<String> hashSet = (HashSet<String>) iterator.next();
			System.out.println(hashSet);
		}
		
		
	}

	@Test
	public void testGetCommunityId() {
		GraphLoader data = null;
		try {
			data = new GraphLoader("traininGraph.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		HashMap<String, HashSet<String>> map = data.getMap();
		LabelPropagation lp = new LabelPropagation();
		lp.initiliaze(map);
		/*initially each node itself is a community*/
		assertEquals(lp.getCommunityId("11"), "11");

		/* after communities are constructed */
		lp.proceedLP();
		assertEquals(lp.getCommunityId("11"), lp.getCommunityId("12"));
		assertEquals(lp.getCommunityId("3"), lp.getCommunityId("2"));
		assertEquals(lp.getCommunityId("8"), lp.getCommunityId("7"));
		//Collections.sort(lp.getCommunityList());
		ArrayList<HashSet<String>> kominiteler=lp.constructCommunities();
		for (Iterator<HashSet<String>> iterator = kominiteler.iterator(); iterator.hasNext();) {
			HashSet<String> hashSet = (HashSet<String>) iterator.next();
		//	System.out.println(hashSet);
		}
	}

	@Test
	public void testProceedLP() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsTerminated() {
		fail("Not yet implemented");
	}

}
