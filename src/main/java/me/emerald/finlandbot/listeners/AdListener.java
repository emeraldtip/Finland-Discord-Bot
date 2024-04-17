package me.emerald.finlandbot.listeners;

import me.emerald.finlandbot.utils.ConfigUtils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;

public class AdListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().equals(event.getJDA().getSelfUser())) return;

        for (String server : ConfigUtils.getServers()) {
            HashMap<String,String> settings = ConfigUtils.getServerSettings(server);
            if (settings.containsKey("adenabled")) {
                if (settings.get("adenabled").equals("true"))
                {
                    //add logic to generate hash for the message and check whether it exists or not
                    //if ad hash doesn't exist, add it into the config file
                    String msg = generateHash(event.getMessage().toString());
                }
            }
        }
    }

    private static String generateHash(String input)
    {
        try {
            byte[] hashedData = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));

            //turning the hashed byte array into a string and returning it
            StringBuilder output = new StringBuilder();
            for (byte hashedByte : hashedData) {
                output.append(Integer.toString((hashedByte & 0xff) + 0x100, 16).substring(1));
            }

            return output.toString();
        }
        catch (Exception e) {
            return "";
        }
    }
}
