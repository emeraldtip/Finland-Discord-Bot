import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.earthmc.emcapiclient.EMCAPIClient;
import net.earthmc.emcapiclient.object.identifier.DiscordIdentifier;
import org.simpleyaml.configuration.Configuration;
import org.simpleyaml.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;


public class main extends ListenerAdapter {

    public static Configuration configuration;
    private final static EMCAPIClient client = new EMCAPIClient();

    @SuppressWarnings("MethodNameSameAsClassName")
    public static void main(String[] args)
    {
        new main().init();
    }

    public void init() {
        //config loading
        try {
            configuration = YamlConfiguration.loadConfiguration(new File("config.yml"));
        }
        catch (IOException e) {
            System.out.println("Error loading config\n"+e);
            return;
        }

        //JDA setup stuff
        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.DIRECT_MESSAGE_REACTIONS
        );

        JDA jda = JDABuilder.create(configuration.getString("token"), intents)
                .setActivity(Activity.listening("you from within your walls"))
                .addEventListeners(new main())
                .build();


        //Commands here
        CommandListUpdateAction commands = jda.updateCommands();

        commands.addCommands(
                Commands.slash("ping", "Fragmentation is a multistage process."),
                Commands.slash("voterid","Get your voterID for the currently ongoing elections")
        );

        commands.queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping" -> pingCommand(event);
            case "voterid" -> idCommand(event);
            default -> event.reply("This command is still in development...").queue();
        }
    }

    //different utils ----------------------------------------------------------------------

    //for storing voterIDs in a csv file
    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    private boolean storeID(String id, String playerID) {
        //usernames are more human-readable than UUIDs
        String username = client.getPlayerDataByString(playerID).getName();

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
    private String generateID(String discID, String mcID) {
        String seed = configuration.getString("seed");
        String input  = seed+discID+mcID; //combine  all of em together
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

    private boolean sendDM(User user, String content) {
        try {
            user.openPrivateChannel()
                    .flatMap(channel -> channel.sendMessage(content))
                    .queue();
            return true;
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
        return false;
    }


    //commands all from here on out --------------------------------------------------------
    private void pingCommand(SlashCommandInteractionEvent event) {
        event.reply("pong").setEphemeral(true).queue();
    }

    private void idCommand(SlashCommandInteractionEvent event) {
        String discID = event.getUser().getId();
        DiscordIdentifier discData = client.getDiscordIdentifierByString(discID);
        String playerID = discData.getUUID();

        if (playerID==null) {

            event.reply("Your minecraft account is not currently linked to your discord.\n" +
                    "To link your discord account log onto **Aurora** and run the '**/discord link**' command " +
                    "and follow the instructions provided in chat."
            ).queue();
        }
        else {
            String id = generateID(discID,playerID);
            boolean writeSuccess = storeID(id,playerID);
            if (writeSuccess) {
                boolean dmSuccess = sendDM(event.getUser(),"Here's your voterID: || "+id+" || \nDo not share it with others!");
                if (dmSuccess) {
                    event.reply("VoterID generation successful! Check your DMs").queue();
                }
                else {
                    event.reply("Can't send voterID, please make sure you have the " +
                            "'Allow direct messages from server members' setting enabled").queue();
                }
            }
            else {
                event.reply("Something went wrong with ID generation, please notify Emerald").queue();
            }
        }
    }
}
