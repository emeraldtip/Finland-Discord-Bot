package me.emerald.finlandbot;

import me.emerald.finlandbot.commands.IDCommand;
import me.emerald.finlandbot.commands.SettingsCommand;
import me.emerald.finlandbot.utils.ConfigUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
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

    public static void main(String[] args)
    {
        new Main().init();
    }


    @SuppressWarnings("DataFlowIssue")
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

        //wait for the bot to load in
        timer.schedule(new TimerTask(){
            public void run(){
                //owner-only commands
                System.out.println("Current guilds: "+bot.getGuilds().toString());
                CommandListUpdateAction commands = bot.getGuildById("406810397018947596").updateCommands();

                commands.addCommands(
                        Commands.slash("restart","Restart the bot")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                        Commands.slash("stop","Stops the bot")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                        Commands.slash("updatecommands","Updates slash commands of the bot")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                );
                commands.queue();
            }
        },5000);


        timer.scheduleAtFixedRate(new TimerTask(){
            public void run(){
                checkVoteParty();
            }
        },0,5000);
    }


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping" -> pingCommand(event);
            case "voterid" -> new IDCommand().idCommand(event);
            case "settings" -> new SettingsCommand().settingsCommand(event);
            case "updatecommands" -> updateCommands(event);
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


    @SuppressWarnings("unchecked")
    public static void updateCommands(SlashCommandInteractionEvent event) {
        if (event.getUser().getId().equals("258934231345004544")) { //verify, it's the owner running the command
            //remove all commands the bot has
            for (Command c: (List<Command>) bot.retrieveCommands()) {
                c.delete();
            }

            //add global commands
            CommandListUpdateAction commands = bot.updateCommands();

            commands.addCommands(
                    Commands.slash("ping", "Fragmentation is a multistage process.")
                            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),
                    Commands.slash("voterid","Get your voterID for the currently ongoing elections")
                            .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
            );
            commands.queue();

            //add guild-only commands
            for (Guild g : bot.getGuilds()) {
                commands = g.updateCommands();
                commands.addCommands(
                        Commands.slash("settings", "Manage bot settings").addSubcommandGroups(
                                        new SubcommandGroupData("voteparty", "Manage voteparty announcement settings").addSubcommands(
                                                new SubcommandData("channel", "set the voteparty channel")
                                                        .addOption(OptionType.CHANNEL, "channel", "channel to send the announcements in", true),
                                                new SubcommandData("role", "set the role to be notified")
                                                        .addOption(OptionType.ROLE, "role", "role to be notified", true),
                                                new SubcommandData("enable", "Enable the sending of notifications"),
                                                new SubcommandData("disable", "Disable the sending of notifications"),
                                                new SubcommandData("number", "aa")
                                                        .addOption(OptionType.NUMBER, "number", "num")
                                        )
                                )
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                );
                commands.queue();
            }
            event.reply("Successfully updated commands!").setEphemeral(true).queue();
        }
        else {
            event.reply("You do not have permissions to run this command!").setEphemeral(true).queue();
        }
    }


    private static boolean fiftyRan = false;
    private static boolean tenRan = false;
    public static void checkVoteParty() {
        int remaining = client.getServerData().getNumVotesRemaining();
        //System.out.println("checking "+ remaining);
        if ((remaining<50 && !fiftyRan) || (remaining<10 && !tenRan)) {
            for (String s :ConfigUtils.getServers()) {
                HashMap<String,String> settings = ConfigUtils.getServerSettings(s);
                if (settings.containsKey("enabled")) {
                    if (settings.get("enabled").equals("true")) { //enabled shouldn't be true if other settings are not set
                        Guild guild = bot.getGuildById(s);
                        if (guild!=null) {
                            TextChannel channel = guild.getTextChannelById(settings.get("channel"));
                            if (channel!=null){
                                Role role = guild.getRoleById(settings.get("role"));
                                if (role!=null)
                                {
                                    channel.sendMessage("VoteParty is happening in "+remaining +" votes "+ role.getAsMention() +" get yo ass on")
                                            .queue();
                                }
                            }
                        }
                    }
                }
            }
        }
        if (remaining<50) {
            fiftyRan=true;
        }
        if (remaining<10) {
            tenRan = true;
        }
        if (remaining>100)
        {
            fiftyRan = false;
            tenRan = false;
        }
    }


    //simple commands all from here on out --------------------------------------------------------
    private void pingCommand(SlashCommandInteractionEvent event) {
        event.reply("pong").setEphemeral(true).queue();
    }
}
