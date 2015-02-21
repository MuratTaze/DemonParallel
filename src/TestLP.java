import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;


public class TestLP {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Dataset data = new Dataset("Email-Enron.txt");
		HashMap<String, HashSet<String>> map = data.getMap();
		LabelPropagation lp=new LabelPropagation();
		lp.initiliaze(map);
		lp.proceedLP();
		
	}

}
