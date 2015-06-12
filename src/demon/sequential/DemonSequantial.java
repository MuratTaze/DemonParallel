package demon.sequential;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import labelPropagation.Community;
import labelPropagation.GraphLoader;
import labelPropagation.Network;
import labelPropagation.Vertex;
import net.ontopia.utils.CompactHashSet;

public class DemonSequantial {

    public static void main(String[] args) throws IOException {
        GraphLoader graphLoader = new GraphLoader("Email-Enron.txt");
        System.out.println("Network construction---> Done.");
        Demon<Integer> demon = new Demon<Integer>();
        /*
         * change merge factor to see its effect. 1 means merge communities if
         * bigger community fully contains smaller community
         */
       demon.execute(graphLoader.getNetwork(), 0.5, 0);
        System.out.println("Demon execution---> Done.");
        PrintWriter writer = new PrintWriter(new File("QuadraticOutput.txt"));
        writer.print(demon.getGlobalCommunities());
        writer.flush();
        writer.close();
        System.out.println("Output is done for Quadratic method.");
        averageConductance(graphLoader, demon);
      /*   demon.execute(graphLoader.getNetwork(), 0.5, 1);
        PrintWriter writer2 = new PrintWriter(new File("SubLinearOutput.txt"));
        writer2.print(demon.getGlobalCommunities());
        writer2.flush();
        writer2.close();
        System.out.println("Output is done for Sublinear method.");
        averageConductance(graphLoader, demon);*/

    }

    private static void averageConductance(GraphLoader graphLoader,
            Demon<Integer> demon) {
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

    private static double degree(
            CompactHashSet<Vertex<Integer>> compactHashSet,
            CompactHashSet<Integer> compactHashSet2) {
        double degree = 0;
        Iterator<Integer> iter = compactHashSet2.iterator();
        while (iter.hasNext()) {
            if (compactHashSet.contains(new Vertex<Integer>(iter.next()))) {
                degree++;
            }
        }
        return degree;
    }

}