package demon.sequential;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;

import labelPropagation.Community;
import labelPropagation.GraphLoader;
import labelPropagation.Network;
import labelPropagation.Vertex;

public class DemonSequantial {

    public static void main(String[] args) throws IOException {
        GraphLoader graphLoader = new GraphLoader("ACM_Formatted .txt");
        System.out.println("Network construction---> Done.");
        Demon<Integer> demon = new Demon<Integer>();
        /*
         * change merge factor to see its effect. 1 means merge communities if
         * bigger community fully contains smaller community
         */
        demon.execute(graphLoader.getNetwork(), 0.5);
        System.out.println("Demon execution---> Done.");
        PrintWriter writer = new PrintWriter(new File("EnronOutput.txt"));
        writer.print(demon.getGlobalCommunities());
        writer.flush();
        writer.close();
        System.out.println("Output ---> Done.");
        double total_conductance = 0;
        for (int i = 0; i < demon.getGlobalCommunities().getCommunities()
                .size(); i++) {
            total_conductance += conductance(demon.getGlobalCommunities()
                    .getCommunities().get(i), graphLoader.getNetwork());
        }
        double average_conductance = total_conductance
                / demon.getGlobalCommunities().getCommunities().size();
        System.out.println("Average conductance value is "
                + average_conductance);

    }

    private static double conductance(Community<Integer> community,
            Network<Integer> network) {
        double in_degree = 0;
        double out_degree = 0;
        Iterator<Integer> iter = community.getMembers().iterator();
        while (iter.hasNext()) {
            Integer element = iter.next();
            double current_degree = degree(
                    network.getGraph().get(new Vertex<Integer>(element))
                            .getListOfNeighbors(), community.getMembers());
            out_degree += network.getGraph().get(new Vertex<Integer>(element))
                    .getListOfNeighbors().size()
                    - current_degree;
            in_degree += current_degree;
        }
        return out_degree / (in_degree / 2);

    }

    private static double degree(HashSet<Vertex<Integer>> all_neigbors,
            HashSet<Integer> community_neigbors) {
        double degree = 0;
        Iterator<Integer> iter = community_neigbors.iterator();
        while (iter.hasNext()) {
            if (all_neigbors.contains(new Vertex<Integer>(iter.next()))) {
                degree++;
            }
        }
        return degree;
    }

}