package demon.parallel;

import java.io.IOException;
import java.util.ArrayList;

import labelPropagation.GraphLoader;
import labelPropagation.NeighborList;

import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;

import demon.sequential.Demon;

public class DemonParallel extends Storage implements StartPoint {
    @Shared
    ArrayList<NeighborList<Integer>>[] array;

    @Override
    public void main() throws IOException {
        GraphLoader graphLoader = new GraphLoader("traininGraph.txt");
        Indexer<Integer> indexer = new Indexer<Integer>();
        array = indexer.index(graphLoader.getNetwork());
        graphLoader = null;

        Demon<Integer> demon = new Demon<Integer>();
        /*
         * change merge factor to see its effect. 1 means merge communities if
         * bigger community fully contains smaller community
         */
        demon.execute(indexer.getLocalNetwork(), 0.6);
        System.out.println("Found by " + PCJ.myId() + " \n"
                + demon.getGlobalCommunities());
        indexer = null;
    }

    public static void main(String[] args) {
        String[] nodes = new String[] { "localhost", "localhost"};
        PCJ.deploy(DemonParallel.class, DemonParallel.class, nodes);
    }
}
