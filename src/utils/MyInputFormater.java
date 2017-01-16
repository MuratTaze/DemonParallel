package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;



public class MyInputFormater {
    private HashMap<String, HashSet<String>> map;

    public HashMap<String, HashSet<String>> getMap() {
        return map;
    }

    public void setMap(HashMap<String, HashSet<String>> map) {
        this.map = map;
    }

    public MyInputFormater(String filename) throws IOException {
        super();
        // TODO Auto-generated constructor stub

        map = new HashMap<String, HashSet<String>>();/*
                                                      * network itself
                                                      */

        // Create object of FileReader
        FileReader inputFile = new FileReader(filename);

        // Instantiate the BufferedReader Class
        BufferedReader bufferReader = new BufferedReader(inputFile);
        PrintWriter writer = new PrintWriter(new File("Formatted Zachary Karate.txt"));
        String line;

        // Read file line by line and add them to hashmap
        while ((line = bufferReader.readLine()) != null) {
            line = bufferReader.readLine();
            line = bufferReader.readLine();
            String[] values = new String[2];
            if (line == null)
                break;
            values[0] = line.split(" ")[2];
            line = bufferReader.readLine();
            values[1] = line.split(" ")[2];
            line = bufferReader.readLine();
            writer.println(values[0] + " " + values[1]);
        }
        // Close the buffer reader
        bufferReader.close();
        writer.flush();
        writer.close();
    }

    public static void main(String[] args) {
System.out.println((int)(Math.log10(7)/Math.log10(2)));
    }

}
