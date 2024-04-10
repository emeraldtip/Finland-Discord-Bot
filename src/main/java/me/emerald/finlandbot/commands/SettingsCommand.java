package me.emerald.finlandbot.commands;

import me.emerald.finlandbot.utils.ConfigUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class SettingsCommand {
    @SuppressWarnings("DataFlowIssue")
    public void settingsCommand(SlashCommandInteractionEvent event) {
        System.out.println(event.getCommandString());
        System.out.println(event.getSubcommandGroup());

        boolean success = false;
        switch (event.getSubcommandGroup()) {
            case "voteparty":
                switch (event.getSubcommandName()) {
                    case "channel" ->
                            success = ConfigUtils.setServerSetting(event.getGuild().getId(), "channel", event.getOption("channel").getAsString());
                    case "role" ->
                            success = ConfigUtils.setServerSetting(event.getGuild().getId(), "role", event.getOption("role").getAsString());
                    default ->
                            event.reply("Not implemented yet").queue();
                }
                break;
            default:
                event.reply("Not implemented yet").queue();
                break;
        }

        if (success) {
            event.reply("Setting updated successfully!").queue();
        }
        else {
            event.reply("Updating setting failed!").queue();
        }
    }
}
