package demon.parallel;

import java.io.IOException;
import java.util.ArrayList;

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
        demon.execute(indexer.getLocalNetwork(), 0.5);
        globalCommunities = demon.getGlobalCommunities();
        int numberOfIterations = (int) (Math.log10(PCJ.threadCount()) / Math
                .log10(2));
        for (int i = 0; i < numberOfIterations; i++) {
            /* wait for others to reach the same step */
            PCJ.barrier();

            /* determine who will merge with whom at this step */
            if (myTurn(i)) {
                System.out.println("merge!!!!!!!!!!!!!!!!!!!");
                int target = PCJ.myId() + (int) Math.pow(2, i);
                if (target >= PCJ.threadCount())
                    continue;
                CommunityList<Integer> targetCommunities = PCJ.get(target,
                        "globalCommunities");
                globalCommunities.getCommunities().addAll(
                        targetCommunities.getCommunities());
                merge(0.6);
            }
        }
        System.out
                .println("Found by " + PCJ.myId() + " \n" + globalCommunities);
    }

    private void merge(double mergeFactor) {
        /* merge part - pooling approach */
        for (int i = 0; i < globalCommunities.getCommunities().size() - 1; i++) {
            for (int j = i + 1; j < globalCommunities.getCommunities().size();) {
                if (isMergible(globalCommunities.getCommunities().get(i),
                        globalCommunities.getCommunities().get(j), mergeFactor)) {
                    globalCommunities
                            .getCommunities()
                            .get(i)
                            .getMembers()
                            .addAll(globalCommunities.getCommunities().get(j)
                                    .getMembers());
                    globalCommunities.getCommunities().remove(j);
                    j = i + 1;
                } else
                    j++;
            }
        }
    }

    private boolean isMergible(Community<Integer> community1,
            Community<Integer> community2, double mergeFactor) {
        if (community1 == null || community2 == null)
            return false;
        double intersection = 0;
        double size1 = community1.getMembers().size();
        double size2 = community2.getMembers().size();
        double min = size2;
        if (size1 < size2) {
            min = size1;
        }
        if (min == size2) {
            for (Integer value : community2.getMembers()) {
                if (community1.getMembers().contains(value))
                    intersection++;
            }
        } else {
            for (Integer value : community1.getMembers()) {
                if (community2.getMembers().contains(value))
                    intersection++;
            }
        }
        if (intersection / min >= mergeFactor) {
            return true;
        }
        return false;
    }

    private boolean myTurn(int iteration) {
        if (PCJ.myId() % (Math.pow(2, iteration + 1)) == 0)
            return true;
        else
            return false;
    }

    public static void main(String[] args) {
        String[] nodes = new String[] { "localhost", "localhost" };
        PCJ.deploy(DemonParallel.class, DemonParallel.class, nodes);
    }
}
