package demon.sequential;

import java.io.IOException;
import labelPropagation.GraphLoader;

public class DemonSequantial {

    public static void main(String[] args) throws IOException {
        GraphLoader graphLoader = new GraphLoader("traininGraph.txt");

        Demon<Integer> demon = new Demon<Integer>();
        /*
         * change merge factor to see its effect. 1 means merge communities if
         * bigger community fully contains smaller community
         */
        demon.execute(graphLoader.getNetwork(), 0.5);
        System.out.println("Overlapping Communities = "
                + demon.getGlobalCommunities());
    }
}