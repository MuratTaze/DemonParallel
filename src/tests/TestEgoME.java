package tests;

import java.io.IOException;

import labelPropagation.GraphLoader;
import labelPropagation.Network;
import labelPropagation.Vertex;

import org.junit.Test;

import demon.parallel.DemonParallel;

public class TestEgoME {

    @Test
    public void test() throws IOException {
        GraphLoader graphLoader = new GraphLoader("traininGraph.txt");
        DemonParallel<Integer> demon = new DemonParallel<Integer>();
        Network<Integer> network= graphLoader.getNetwork();
        for(Vertex<Integer> key: network.getGraph().keySet()){
            System.out.println(key);
            Network<Integer> emen= demon.egoMinusEgo(key, network);
            System.out.println(emen);
            
    }}

}
