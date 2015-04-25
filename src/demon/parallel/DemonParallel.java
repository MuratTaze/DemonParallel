package demon.parallel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import labelPropagation.Community;
import labelPropagation.CommunityList;
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
    @Shared
    CommunityList<Integer> globalCommunities;

    @Override
    public void main() throws IOException {
        GraphLoader graphLoader = new GraphLoader("traininGraph.txt");
        Indexer<Integer> indexer = new Indexer<Integer>();
        array = indexer.index(graphLoader.getNetwork());
        graphLoader = null;

        Demon<Integer> demon = new Demon<Integer>();
        /*
         * change merge factor to see its effect. 1 means merge communities iff
         * bigger community fully contains smaller community
         */
        PCJ.barrier();
        demon.execute(indexer.getLocalNetwork(), 0.6);
        globalCommunities = demon.getGlobalCommunities();
        int numberOfIterations = (int) (Math.log10(PCJ.threadCount()) / Math
                .log10(2));
        for (int i = 0; i < numberOfIterations; i++) {
            /* wait for others to reach the same step */
            PCJ.barrier();

            /* determine who will merge with whom at this step */
            if (myTurn(i)) {
                int target = PCJ.myId() + (int) Math.pow(2, i);
                CommunityList<Integer> targetCommunities = PCJ.get(target,
                        "globalCommunities");
                for (Community<Integer> community : targetCommunities
                        .getCommunities()) {
                    merge(community, 0.6);
                }

            }
        }
        System.out
                .println("Found by " + PCJ.myId() + " \n" + globalCommunities);
    }

    private void merge(Community<Integer> toBeMerged, double mergeFactor) {
        if (globalCommunities == null) {
            globalCommunities = new CommunityList<Integer>();
            globalCommunities.getCommunities().add(toBeMerged);
        } else {
            /* we check for each global community */
            for (Community<Integer> temp : globalCommunities.getCommunities()) {

                HashSet<Integer> temp2 = new HashSet<Integer>(temp.getMembers());
                double size1 = temp2.size();
                double size2 = toBeMerged.getMembers().size();
                double min = size1 < size2 ? size1 : size2;
                temp2.retainAll(toBeMerged.getMembers());
                if (temp2.size() / min >= mergeFactor) {
                    temp.getMembers().addAll(toBeMerged.getMembers());// join
                                                                      // them
                    return;
                }
            }
            globalCommunities.getCommunities().add(toBeMerged);// as a
                                                               // seperate
            // community

        }
    }

    private boolean myTurn(int iteration) {
        if (PCJ.myId() % (Math.pow(2, iteration + 1)) == 0)
            return true;
        else
            return false;
    }

    public static void main(String[] args) {
        String[] nodes = new String[] { "localhost", "localhost", "localhost",
                "localhost" };
        PCJ.deploy(DemonParallel.class, DemonParallel.class, nodes);
    }
}
