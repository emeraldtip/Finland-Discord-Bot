import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.earthmc.emcapiclient.EMCAPIClient;
import net.earthmc.emcapiclient.object.identifier.DiscordIdentifier;
import net.earthmc.emcapiclient.object.identifier.PlayerIdentifier;
import org.simpleyaml.configuration.Configuration;
import org.simpleyaml.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.EnumSet;
import java.util.List;

public class main extends ListenerAdapter {

    public static void main(String[] args)
    {
        new main().init();
    }

    public void init() {
        //token is inside the config.yml file
        Configuration configuration;

        try {
            configuration = YamlConfiguration.loadConfiguration(new File("config.yml"));
        } catch (IOException e) {
            System.out.println("Error loading config\n"+e);
            return;
        }

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
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        switch (event.getName()) {
            case "ping" -> pingCommand(event);
            case "voterid" -> idCommand(event);
            default -> event.reply("This command is still in development...").queue();
        }
    }


    //commands all from here on out --------------------------------------------------------
    private void pingCommand(SlashCommandInteractionEvent event)
    {
        event.reply("pong").queue();
    }

    private void idCommand(SlashCommandInteractionEvent event)
    {
        String discID = event.getUser().getId();
        EMCAPIClient client = new EMCAPIClient();

        DiscordIdentifier discData = client.getDiscordIdentifierByString(discID);
        String playerID = discData.getUUID();
        if (playerID==null)
        {
            event.reply("Your minecraft account is not currently linked to your discord.\n" +
                    "To link your discord account log onto **Aurora** and run the **'/discord link'** command " +
                    "and follow the instructions provided in chat."
            ).queue();
        }
        else
        {
            event.reply("Your UUID is: "+playerID).queue();
        }
    }
}
