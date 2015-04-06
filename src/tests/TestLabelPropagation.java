package tests;

import java.io.IOException;

import labelPropagation.GraphLoader;
import labelPropagation.LabelPropagation;

import org.junit.Test;

public class TestLabelPropagation {
    @Test
    public void testInitiliaze() throws IOException {

        GraphLoader loader = null;

        loader = new GraphLoader("traininGraph.txt");

        LabelPropagation<Integer> lp = new LabelPropagation<Integer>(
                loader.getNetwork());
        lp.proceedLP();
        System.out.println("***\n" + lp.extractCommunities());
    }

}
