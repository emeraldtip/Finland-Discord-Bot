package me.emerald.finlandbot;

import me.emerald.finlandbot.commands.IDCommand;
import me.emerald.finlandbot.commands.SettingsCommand;
import me.emerald.finlandbot.utils.ConfigUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.earthmc.emcapiclient.EMCAPIClient;

import java.util.*;

public class Main extends ListenerAdapter {

    public static String jarLoc;
    public static LinkedHashMap<String,Object> configuration;

    public final static EMCAPIClient client = new EMCAPIClient();
    public static JDA bot;

    public static Timer timer = new Timer();
    public static TimerTask votePartyChecker;



    public static void main(String[] args)
    {
        new Main().init();
    }


    public void init() {
        String jarPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        //cause the jarPath contains the full path to the jar, but we want the folder the jar is in
        jarLoc = jarPath.substring(0,jarPath.lastIndexOf("/")+1);

        //config loading
        if (!ConfigUtils.loadConfig()) { return; } //will exit the program if config doesn't load properly

        //JDA setup stuff
        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.DIRECT_MESSAGE_REACTIONS
        );

        bot = JDABuilder.create((String)configuration.get("token"), intents)
                .setActivity(Activity.listening("you from within your walls :3"))
                .addEventListeners(new Main())
                .build();


        //Commands here
        CommandListUpdateAction commands = bot.updateCommands();

        commands.addCommands(
                Commands.slash("ping", "Fragmentation is a multistage process."),
                Commands.slash("voterid","Get your voterID for the currently ongoing elections"),
                Commands.slash("settings", "Manage bot settings").addSubcommandGroups(
                        new SubcommandGroupData("voteparty", "Manage voteparty announcement settings").addSubcommands(
                                new SubcommandData("channel","set the voteparty channel")
                                        .addOption(OptionType.CHANNEL,"channel","channel to send the announcements in", true),
                                new SubcommandData("role","set the role to be notified")
                                        .addOption(OptionType.ROLE,"role","role to be notified", true),
                                new SubcommandData("enable","Enable the sending of notifications"),
                                new SubcommandData("disable","Disable the sending of notifications")
                        )
                )
        );

        commands.queue();

        votePartyChecker = new TimerTask(){
            public void run(){
                checkVoteParty();
            }
        };

        timer.schedule(votePartyChecker, 5000L);
    }


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping" -> pingCommand(event);
            case "voterid" -> new IDCommand().idCommand(event);
            case "settings" -> new SettingsCommand().settingsCommand(event);
            default -> event.reply("This command is still in development...").queue();
        }
    }


    //the simplest utils ----------------------------------------------------------------------

    public static boolean sendDM(User user, String content) {
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

    private static boolean fiftyRan = false;
    private static boolean tenRan = false;
    public static void checkVoteParty() {
        int remaining = client.getServerData().getNumVotesRemaining();
        if (remaining<50 && !fiftyRan) {
            for (String s :ConfigUtils.getServers()) {
                HashMap<String,String> settings = ConfigUtils.getServerSettings(s);
                if (settings.containsKey("enabled")) {
                    if (settings.get("enabled").equals("true")) {
                        bot.getGuildById(s)
                                .getTextChannelById(settings.get("channel"))
                                .sendMessage("VoteParty is happening in 50 votes <@"+settings.get("role")+">")
                                .queue();
                    }
                }
            }
            fiftyRan = true;
            tenRan = false;
        }
        if (remaining<10 && !tenRan) {
            for (String s :ConfigUtils.getServers()) {
                HashMap<String,String> settings = ConfigUtils.getServerSettings(s);
                if (settings.containsKey("enabled")) {
                    if (settings.get("enabled").equals("true")) { //enabled shouldn't be true if other settings are not set
                        bot.getGuildById(s)
                                .getTextChannelById(settings.get("channel"))
                                .sendMessage("VoteParty is happening in 10 votes <@"+settings.get("role")+">, get yo ass on")
                                .queue();
                    }
                }
            }
            fiftyRan = false;
            tenRan = true;
        }
    }


    //simple commands all from here on out --------------------------------------------------------
    private void pingCommand(SlashCommandInteractionEvent event) {
        event.reply("pong").setEphemeral(true).queue();
    }
}
