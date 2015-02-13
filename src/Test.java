import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;

public class Test extends Storage implements StartPoint {
	@Shared
	double[] array = new double[20];

	@Override
	public void main() {
		double[] arrayLocal = new double[20];
		int numberOfIterations = 1;
		while (numberOfIterations > 0) {
			PCJ.barrier();
			if (PCJ.myId() == 0) {
				for (int i = 0; i < 10; i++)
					arrayLocal[i] = i;
				PCJ.broadcast("array", arrayLocal);

			}
			if (PCJ.myId() == 1) {
				for (int i = 10; i < 15; i++)
					arrayLocal[i] = i * 2;
				PCJ.waitFor("array", 1);
				for (int i = 10; i < 15; i++)
					array[i] = arrayLocal[i];
				PCJ.broadcast("array", array);

			}
			if (PCJ.myId() == 2) {
				for (int i = 15; i < 20; i++)
					arrayLocal[i] = i * 3;
				PCJ.waitFor("array", 2);
				for (int i = 15; i < 20; i++)
					array[i] = arrayLocal[i];
				PCJ.broadcast("array", array);

				for (int i = 0; i < 20; i++)
					System.out.println(array[i]);

			}
			numberOfIterations--;
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String[] nodes = new String[] { "localhost", "localhost", "localhost" };
		PCJ.deploy(Test.class, Test.class, nodes);
	}

}
