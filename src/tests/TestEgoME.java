package tests;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class TestEgoME {

	@Test
	public void test() throws IOException {

		Set<String> egos = new HashSet<String>();
		FileReader inputFile;
		BufferedReader bufferReader;
		int numberOfThreads = 2;
		int i = 0;
		// read and combine ego networks for parallel execution
		while (numberOfThreads > i) {
			inputFile = new FileReader(i + "_parallelEgo.txt");

			// Instantiate the BufferedReader Class
			bufferReader = new BufferedReader(inputFile);

			String line;

			// Read file line by line and add them to hashset
			while ((line = bufferReader.readLine()) != null) {

				egos.add(line);

			}
			bufferReader.close();
			i++;
		}

		inputFile = new FileReader("SerialEgo.txt");

		// Instantiate the BufferedReader Class
		bufferReader = new BufferedReader(inputFile);

		String line;
		int hata = 0;
		// read ego networks for serial execution
		while ((line = bufferReader.readLine()) != null) {

			if (true == egos.add(line))
				{
				hata++;
				System.out.println(line);
			}

		}
		bufferReader.close();
System.out.println(hata);
	}

}
