package me.emerald.finlandbot;

import me.emerald.finlandbot.commands.IDCommand;
import me.emerald.finlandbot.commands.SettingsCommand;
import me.emerald.finlandbot.listeners.AdListener;
import me.emerald.finlandbot.utils.ConfigUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.earthmc.emcapiclient.EMCAPIClient;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
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
                GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                GatewayIntent.GUILD_MEMBERS
        );

        bot = JDABuilder.create((String)configuration.get("token"), intents)
                .setActivity(Activity.listening("you from within your walls :3"))
                .addEventListeners(new Main())
                .addEventListeners(new AdListener())
                .build();

        //wait for the bot to load in
        timer.schedule(new TimerTask(){
            public void run(){
                //owner-only commands
                System.out.println("Current guilds: "+ bot.getGuilds());
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
        },3000);


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
            case "updatecommands" -> updateCommandsCommand(event);
            case "stop" -> shutDownCommand(event);
            case "restart" -> restartCommand(event);
            default -> event.reply("This command is still in development...").setEphemeral(true).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        Guild guild = event.getGuild();
        guild.loadMembers();

        Role role;
        Member member;
        switch (event.getComponentId()) {
            case "enable-vp":
                role = guild.getRoleById(ConfigUtils.getServerSettings(guild.getId()).get("vprole"));
                member = guild.getMember(event.getUser());
                guild.addRoleToMember(member, role).queue();
                event.reply("Sucessfully enabled VoteParty Alerts").setEphemeral(true).queue();
                break;
            case "disable-vp":
                role =  guild.getRoleById(ConfigUtils.getServerSettings(guild.getId()).get("vprole"));
                member = guild.getMember(event.getUser());
                if (member.getRoles().contains(role)) {
                    guild.removeRoleFromMember(member,role).queue();
                }
                event.reply("Sucessfully disabled VoteParty Alerts").setEphemeral(true).queue();
                break;
            default:
                event.reply("Not implemented yet").setEphemeral(true).queue();
                break;
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
        if (remaining>100) {
            fiftyRan = false;
            tenRan = false;
        }
    }


    //simple commands all from here on out --------------------------------------------------------
    private void pingCommand(SlashCommandInteractionEvent event) {
        event.reply("pong").setEphemeral(true).queue();
    }


    public static void updateCommandsCommand(SlashCommandInteractionEvent event) {
        if (event.getUser().getId().equals("258934231345004544")) { //verify, it's the owner running the command
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
                                                new SubcommandData("channel", "set the voteparty announcements channel")
                                                        .addOption(OptionType.CHANNEL, "channel", "channel to send the announcements in", true),
                                                new SubcommandData("role", "set the role to be notified")
                                                        .addOption(OptionType.ROLE, "role", "make sure the role is lower than the bot's role or it won't work ", true),
                                                new SubcommandData("enable", "Enable the sending of notifications"),
                                                new SubcommandData("disable", "Disable the sending of notifications")
                                                ),
                                        new SubcommandGroupData("advertisementgifs","Manage advertisement channel gif sending settings, which will send gifs if an ad is a duplicate").addSubcommands(
                                                new SubcommandData("channel","set the advertisements channel")
                                                        .addOption(OptionType.CHANNEL,"channel","advertisements channel",true),
                                                new SubcommandData("enable", "Enable the sending of gifs"),
                                                new SubcommandData("disable", "Disable the sending of gifs")
                                        )
                                )
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                );

                //owner-only commands
                if (g.getId().equals("406810397018947596")) {
                    commands.addCommands(
                            Commands.slash("restart","Restart the bot")
                                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                            Commands.slash("stop","Stops the bot")
                                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                            Commands.slash("updatecommands","Updates slash commands of the bot")
                                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                    );
                }

                commands.queue();
            }
            event.reply("Successfully updated commands!").setEphemeral(true).queue();
        }
        else {
            event.reply("You do not have permissions to run this command!").setEphemeral(true).queue();
        }
    }


    private void shutDownCommand(SlashCommandInteractionEvent event) {
        event.reply("Shutting down...").setEphemeral(true).queue();
        bot.shutdown();
        timer.schedule(new TimerTask(){
            public void run(){
                System.exit(0);
            }
        },2000);
    }


    private void restartCommand(SlashCommandInteractionEvent event) {
        event.reply("Restarting...").setEphemeral(true).queue();
        bot.shutdown();
        timer.schedule(new TimerTask(){
            public void run(){
                String currJar = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

                //TODO remove this, as the final version of the bot will be running on linux
                if (System.getProperty("os.name").contains("Windows"))
                {
                    //Java adds a slash in front of the filepath on Windows and the command won't work with the slash (or backslash) as the first character
                    currJar = currJar.substring(1);
                }
                ProcessBuilder builder = new ProcessBuilder("java", "-jar", FilenameUtils.separatorsToSystem(currJar));
                try {
                    builder.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
        },2000);
    }
}
