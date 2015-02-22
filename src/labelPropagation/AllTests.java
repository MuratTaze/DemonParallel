package labelPropagation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Test;

public class AllTests {

	

	@Test
	public void testFindMostCommonlyUsedId() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitiliaze() {
		Dataset data = null;
		try {
			data = new Dataset("traininGraph.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		HashMap<String, HashSet<String>> map = data.getMap();
		LabelPropagation lp = new LabelPropagation();
		assertEquals(lp.initiliaze(map), true);
		for (Node n : lp.getCommunityList())
			assertEquals(n.getRealId(), n.getCommunityId());
	}

	@Test
	public void testGetCommunityId() {
		Dataset data = null;
		try {
			data = new Dataset("traininGraph.txt");
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
		Collections.sort(lp.getCommunityList());
		for(Node v:lp.getCommunityList()){
			System.out.println(v);
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
