package demon;
import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;

public class DemonParallel extends Storage implements StartPoint {
	@Shared
	double[] array = new double[3];/*this will be the graph, later.*/

	@Override
	public void main() {
		double[] arrayLocal = new double[3];/*this will be the duplication of network*/
		int numberOfIterations = 1;
		while (numberOfIterations > 0) {
			PCJ.barrier();/*Synchronize threads*/

			/* master node starts the process */
			if (PCJ.myId() == 0) {
				arrayLocal[0] = 1;/*
								 * here we will perform label propagation
								 * algorithm
								 */
				PCJ.broadcast("array", arrayLocal);
			}

			/* slaves */
			for (int j = 1; j < PCJ.threadCount(); j++) {
				if (PCJ.myId() == j) {
					arrayLocal[j] = 9 * j;/*
										 * here we will perform label
										 * propagation algorithm
										 */
					PCJ.waitFor("array", PCJ.myId());/* wait for updated network */
					array[j] = arrayLocal[j];/*transfer data from local network to global network*/
					PCJ.broadcast("array", array);/* commit modifications */
				}
			}

			numberOfIterations--;
		}
		PCJ.barrier();
		if (PCJ.myId() == 0) {
			for (int i = 0; i < array.length; i++) {
				System.out.println(array[i]);
			}
		}
	}

	public static void main(String[] args) {
		String[] nodes = new String[] { "localhost", "localhost", "localhost" };
		PCJ.deploy(DemonParallel.class, DemonParallel.class, nodes);

	}

}
