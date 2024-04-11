package me.emerald.finlandbot.utils;

import me.emerald.finlandbot.Main;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class IDUtils {
    //for storing voterIDs in a csv file
    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    public static boolean storeID(String id, String playerID) {
        //usernames are more human-readable than UUIDs
        String username = Main.client.getPlayerDataByString(playerID).getName();

        //read and check if name is already stored
        try {
            //csv, because I want human readability
            BufferedReader reader = new BufferedReader(new FileReader("voterIDs.csv"));

            int lineCounter = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (lineCounter>1)
                {
                    String[] values = line.strip().split(",");
                    if (values[0].equals(username) && values[1].equals(id)) {
                        System.out.println(username+"'s voterID already exists in table");
                        return true;
                    }
                }
                lineCounter++;
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("voterID csv file doesn't exist yet, creating...");
            try {
                FileWriter writer = new FileWriter("voterIDs.csv");
                writer.write("sep=,\n");
                writer.write("Username,voterID\n");
                writer.close();
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        //actually writing data into file:
        try {
            PrintWriter writer = new PrintWriter(new FileOutputStream(
                    "voterIDs.csv",
                    true));
            writer.append(username+","+id+"\n");
            writer.close();
            return true;
        }
        catch (FileNotFoundException ignored) {} //again - this should never happen
        return false;
    }


    //for generating voterIDs
    @SuppressWarnings("StringConcatenationInLoop")
    public static String generateID(String discID, String mcID) {
        String seed = (String)Main.configuration.get("seed");
        String input  = seed+discID+mcID; //combine all of em together
        try {
            //hashing the data
            byte[] hashedData = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));

            //turning the hashed byte array into a string and returning it
            String output = "";
            for (byte hashedByte : hashedData) {
                output += Integer.toString((hashedByte & 0xff) + 0x100, 16).substring(1);
            }
            return output;
        }
        catch (NoSuchAlgorithmException ignored) {} //this should never happen
        return "";
    }
}