package me.emerald.finlandbot;

import me.emerald.finlandbot.commands.DevCommands;
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
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.earthmc.emcapiclient.EMCAPIClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class Main extends ListenerAdapter {

    public static String jarLoc;
    public static LinkedHashMap<String,Object> configuration;

    public final static EMCAPIClient client = new EMCAPIClient();
    public static JDA bot;

    public static final Timer timer = new Timer();

    public static void main(String[] args) {new Main().init();}


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
                //developer-only commands
                System.out.println("Current guilds: "+ bot.getGuilds());
                CommandListUpdateAction commands = Objects.requireNonNull(bot.getGuildById("406810397018947596")).updateCommands();

                commands.addCommands(
                        Commands.slash("restart","Restart the bot")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                        Commands.slash("stop","Stops the bot")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                        Commands.slash("updatecommands","Updates slash commands of the bot")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                        Commands.slash("update","Pulls changes from the master branch, removes the old executables and builds new ones")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                );
                commands.queue();
            }
        },10000);


        timer.scheduleAtFixedRate(new TimerTask(){
            public void run(){
                checkVoteParty();
            }
        },11000,5000);
    }


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping" -> event.reply("pong").setEphemeral(true).queue();
            case "voterid" -> new IDCommand().idCommand(event);
            case "voteparty" -> event.reply("**"+remaining+"** votes remain until the next VoteParty").queue();
            case "settings" -> new SettingsCommand().settingsCommand(event);
            case "updatecommands" -> new DevCommands().updateCommandsCommand(event);
            case "update" -> new DevCommands().updateCommand(event);
            case "stop" -> new DevCommands().shutDownCommand(event);
            case "restart" -> new DevCommands().restartCommand(event);
            default -> event.reply("This command is still in development...").setEphemeral(true).queue();
        }
    }


    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        Guild guild = event.getGuild();
        assert guild != null;
        guild.loadMembers();

        Role role;
        Member member;
        switch (event.getComponentId()) {
            case "enable-vp" -> {
                role = guild.getRoleById(ConfigUtils.getServerSettings(guild.getId()).get("vprole"));
                if (role!=null) {
                    member = guild.getMember(event.getUser());
                    assert member != null;
                    guild.addRoleToMember(member, role).queue();
                    event.reply("Successfully enabled VoteParty Alerts").setEphemeral(true).queue();
                }
                else {
                    event.reply("Failed to enable VoteParty Alerts").setEphemeral(true).queue();
                }
            }
            case "disable-vp" -> {
                role = guild.getRoleById(ConfigUtils.getServerSettings(guild.getId()).get("vprole"));
                if (role!=null) {
                    member = guild.getMember(event.getUser());
                    assert member != null;
                    if (member.getRoles().contains(role)) {
                        guild.removeRoleFromMember(member, role).queue();
                    }
                    event.reply("Successfully disabled VoteParty Alerts").setEphemeral(true).queue();
                }
                else {
                    event.reply("Failed to disable VoteParty Alerts").setEphemeral(true).queue();
                }
            }
            default -> event.reply("Not implemented yet").setEphemeral(true).queue();
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


    public static void printCommandOutput(Process proc) {
        try {
            BufferedReader out = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String s;
            while ((s = out.readLine()) != null) {
                System.out.println(s);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    private static boolean fiftyRan = false;
    private static boolean tenRan = false;
    public static int remaining = 5000;
    public static void checkVoteParty() {
        try {
            remaining = client.getServerData().getNumVotesRemaining();
        }
        catch(Exception e) {
            System.out.println("Failed to get voteParty data: "+e.getMessage());
            return;
        }

        try {
            if ((remaining < 50 && !fiftyRan) || (remaining < 10 && !tenRan)) {
                for (String s : ConfigUtils.getServers()) {
                    System.out.println(s);
                    HashMap<String, String> settings = ConfigUtils.getServerSettings(s);
                    if (settings.containsKey("vpenabled")) {
                        if (settings.get("vpenabled").equals("true")) { //enabled shouldn't be true if other settings are not set
                            Guild guild = bot.getGuildById(s);
                            if (guild != null) {
                                TextChannel channel = guild.getTextChannelById(settings.get("vpchannel"));
                                if (channel != null) {
                                    Role role = guild.getRoleById(settings.get("vprole"));
                                    if (role != null) {
                                        channel.sendMessage("VoteParty is happening in **" + remaining + "** votes \n" + role.getAsMention() + " get yo ass on")
                                                .queue();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (remaining < 50) {
                fiftyRan = true;
            }
            if (remaining < 10) {
                tenRan = true;
            }
            if (remaining > 100) {
                fiftyRan = false;
                tenRan = false;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
