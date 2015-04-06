package tests;

import java.io.IOException;
import java.util.ArrayList;

import labelPropagation.GraphLoader;
import labelPropagation.NeighborList;

import org.junit.Test;

import demon.parallel.Indexer;

public class TestIndexer {

    @Test
    public void test() throws IOException {
        GraphLoader loader = null;
        loader = new GraphLoader("traininGraph.txt");
        Indexer<Integer> indexer = new Indexer<Integer>();
        ArrayList<NeighborList<Integer>>[] array = indexer.index(loader
                .getNetwork());
        for (int i = 0; i < array.length; i++) {
            System.out.println(array[i]);
        }
    }
}
