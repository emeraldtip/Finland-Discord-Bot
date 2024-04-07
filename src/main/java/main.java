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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;

public class main extends ListenerAdapter {

    public static void main(String[] args)
    {
        new main().init();
    }

    public static Configuration configuration;
    public void init() {
        //config loading
        try {
            configuration = YamlConfiguration.loadConfiguration(new File("config.yml"));
        } catch (IOException e) {
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
    private String generateID(String discID, String mcID) {
        String seed = configuration.getString("seed");
        String input  = seed+discID+mcID; //combine all of em together
        try {
            //hashing the data
            byte[] hashedData = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));

            //turning the hashed byte array into a string and returning it
            String output = "";
            for (int i = 0; i<hashedData.length; i++) {
                output += Integer.toString( (hashedData[i] & 0xff) + 0x100, 16).substring(1);
            }
            return output;
        }
        catch (NoSuchAlgorithmException e) { //this should never happen
            e.printStackTrace();
        }
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
        EMCAPIClient client = new EMCAPIClient();

        DiscordIdentifier discData = client.getDiscordIdentifierByString(discID);
        String playerID = discData.getUUID();
        if (playerID==null) {
            /*
            event.reply("Your minecraft account is not currently linked to your discord.\n" +
                    "To link your discord account log onto **Aurora** and run the **'/discord link'** command " +
                    "and follow the instructions provided in chat."
            ).queue();
             */
            playerID = "61fa89cf-0114-4d1d-9079-fdafecabeaf4";
        }
        //else {
            String id = generateID(discID,playerID);

            boolean dmSuccess = sendDM(event.getUser(),"Here's your voterID: "+id+"\nDo not share it with others!");
            if (dmSuccess) {
                event.reply("VoterID generation successful! Check your DMs").queue();
            }
            else {
                event.reply("Can't send voterID, please make sure you have the " +
                        "'Allow direct messages from server members' setting enabled").queue();
            }

        //}
    }
}
