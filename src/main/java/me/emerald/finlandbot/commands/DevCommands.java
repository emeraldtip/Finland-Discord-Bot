package me.emerald.finlandbot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.util.TimerTask;

import static me.emerald.finlandbot.Main.*;

public class DevCommand {
    public void updateCommandsCommand(SlashCommandInteractionEvent event) {
        if (event.getUser().getId().equals("258934231345004544")) { //verify, it's the developer running the command
            //add global commands
            CommandListUpdateAction commands = bot.updateCommands();

            commands.addCommands(
                    Commands.slash("ping", "Fragmentation is a multistage process.")
                            .setDefaultPermissions(DefaultMemberPermissions.ENABLED),
                    Commands.slash("voteparty","Get the number of votes remaining until the occurrence of the next VoteParty")
                            .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
            );
            commands.queue();

            //add guild-only commands
            for (Guild g : bot.getGuilds()) {
                commands = g.updateCommands();
                commands.addCommands(
                        Commands.slash("settings", "Manage bot settings").addSubcommandGroups(
                                        new SubcommandGroupData("voteparty", "Manage voteparty announcement settings").addSubcommands(
                                                new SubcommandData("channel", "Set the voteparty announcements channel")
                                                        .addOption(OptionType.CHANNEL, "channel", "channel to send the announcements in", true),
                                                new SubcommandData("role", "Set the role to be notified")
                                                        .addOption(OptionType.ROLE, "role", "make sure the role is lower than the bot's role or it won't work ", true),
                                                new SubcommandData("enable", "Enable the sending of notifications"),
                                                new SubcommandData("disable", "Disable the sending of notifications")
                                        ),
                                        new SubcommandGroupData("advertisementgifs","Manage advertisement channel gif sending settings, which will send gifs if an ad is a duplicate").addSubcommands(
                                                new SubcommandData("channel","Set the advertisements channel")
                                                        .addOption(OptionType.CHANNEL,"channel","advertisements channel",true),
                                                new SubcommandData("enable", "Enable the sending of gifs"),
                                                new SubcommandData("disable", "Disable the sending of gifs")
                                        )
                                )
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                        Commands.slash("voterid","Get your voterID for the currently ongoing elections")
                                .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                );

                //developer-only commands
                if (g.getId().equals("406810397018947596")) {
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
                }

                commands.queue();
            }
            event.reply("Successfully updated commands!").setEphemeral(true).queue();
        }
        else {
            event.reply("You do not have permissions to run this command!").setEphemeral(true).queue();
        }
    }


    public void updateCommand(SlashCommandInteractionEvent event) {
        if (event.getUser().getId().equals("258934231345004544")) {
            try {
                event.reply("Trying to pull from git").queue();
                Process pull = Runtime.getRuntime().exec("git pull");
                printCommandOutput(pull);

                if (pull.waitFor() == 0) {
                    event.getChannel().sendMessage("Git pull successful").queue();
                }
                else {
                    event.getChannel().sendMessage("Git pull failed").queue();
                    return;
                }

                //pain
                String currJar = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                Process build = Runtime.getRuntime().exec("gradle build");
                printCommandOutput(build);

                if (build.waitFor() == 0) {
                    event.getChannel().sendMessage("Build successful").queue();
                }
                else {
                    event.getChannel().sendMessage("Build failed").queue();
                    return;
                }

            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void shutDownCommand(SlashCommandInteractionEvent event) {
        if (event.getUser().getId().equals("258934231345004544")) {
            event.reply("Shutting down...").setEphemeral(true).queue();
            bot.shutdown();
            timer.schedule(new TimerTask() {
                public void run() {
                    System.exit(0);
                }
            }, 2000);
        }
    }


    public void restartCommand(SlashCommandInteractionEvent event) {
        if (event.getUser().getId().equals("258934231345004544")) {
            event.reply("Restarting...").setEphemeral(true).queue();
            bot.shutdown();
            timer.schedule(new TimerTask() {
                public void run() {
                    String currJar = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

                    //TODO remove this, as the final version of the bot will be running on linux
                    if (System.getProperty("os.name").contains("Windows")) {
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
            }, 2000);
        }
    }
}
