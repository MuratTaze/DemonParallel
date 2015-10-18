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
        double epsilon = 0.0;
        do {
            runExperiment(epsilon);
            epsilon = epsilon + 0.1;
        } while (epsilon <= 1.0);
    }

    private static void runExperiment(double epsilon) throws IOException {
        System.out.println();
        System.out.println();
        System.out.println("Epsilon="+epsilon);
        GraphLoader graphLoader = new GraphLoader("traininGRaph.txt");
        Demon<Integer> demon = new Demon<Integer>();
        demon.execute(graphLoader.getNetwork(), epsilon, 0,graphLoader.getNetwork().getGraph().size());
        PrintWriter writer = new PrintWriter(new File("QuadraticOutput.txt"));
        writer.print(demon.getGlobalCommunities());
        writer.flush();
        writer.close();
        System.out.println("Output is done for Quadratic method.");
        System.out.println("Total number of comparison is "
                + demon.getNumberOfComparison());
        demon.setNumberOfComparison(0);
        averageConductance(graphLoader, demon);
        System.out.println();
        System.out.println();
        demon.execute(graphLoader.getNetwork(), epsilon, 1,graphLoader.getNetwork().getGraph().size());

        PrintWriter writer2 = new PrintWriter(new File("SubLinearOutput.txt"));
        writer2.print(demon.getGlobalCommunities());
        writer2.flush();
        writer2.close();
        System.out.println("Output is done for Sublinear method.");
        System.out.println("Total number of comparison is "
                + demon.getNumberOfComparison());
        averageConductance(graphLoader, demon);
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
            HashSet<Vertex<Integer>> HashSet,
            HashSet<Integer> HashSet2) {
        double degree = 0;
        Iterator<Integer> iter = HashSet2.iterator();
        while (iter.hasNext()) {
            if (HashSet.contains(new Vertex<Integer>(iter.next()))) {
                degree++;
            }
        }
        return degree;
    }

}
