import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.simpleyaml.configuration.Configuration;
import org.simpleyaml.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

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
            System.out.println("Error loading config\n"+e.toString());
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
                .build();

        CommandListUpdateAction commands = jda.updateCommands();
    }
}
