package me.emerald.finlandbot.commands;

import me.emerald.finlandbot.utils.ConfigUtils;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class SettingsCommand {

    @SuppressWarnings("DataFlowIssue")
    public void settingsCommand(SlashCommandInteractionEvent event) {
        System.out.println(event.getCommandString());
        System.out.println(event.getSubcommandGroup());

        boolean success = false;
        String guildID = event.getGuild().getId();
        switch (event.getSubcommandGroup()) {
            case "voteparty":
                switch (event.getSubcommandName()) {
                    case "channel":
                        if (event.getOption("channel").getAsChannel().getType().equals(ChannelType.TEXT)) { //only text channels allowed
                            success = ConfigUtils.setServerSetting(guildID, "vpchannel", event.getOption("channel").getAsString());
                        }
                        else {
                            event.reply("Please select a text channel").queue();
                        }
                        break;
                    case "role":
                        success = ConfigUtils.setServerSetting(guildID, "vprole", event.getOption("role").getAsString());
                        break;
                    case "enable":
                        if (!ConfigUtils.getServerSettings(guildID).containsKey("vpchannel")) {
                            event.reply("You do not have the voteparty alerts channel set").queue();
                            return;
                        }
                        if(!ConfigUtils.getServerSettings(guildID).containsKey("vprole")) {
                            event.reply("You do not have the voteparty alerted role set").queue();
                            return;
                        }

                        event.getGuild().getTextChannelById(ConfigUtils.getServerSettings(guildID).get("vpchannel"))
                                .sendMessage("Click the buttons below to manage your voteparty alerts settings")
                                .addActionRow(
                                        Button.success("enable-vp","Enable VoteParty alerts"),
                                        Button.danger("disable-vp","Disable VoteParty alerts")
                                ).queue();
                        success = ConfigUtils.setServerSetting(guildID,"vpenabled","true");
                        break;
                    case "disable":
                        success = ConfigUtils.setServerSetting(guildID,"vpenabled","false");
                        break;
                    default:
                        event.reply("Not implemented yet").queue();
                        break;
                }
                break;
            case "advertisementgifs":
                switch (event.getSubcommandName()) {
                    case "channel":
                        if (event.getOption("channel").getAsChannel().getType().equals(ChannelType.TEXT)) { //only text channels allowed
                            success = ConfigUtils.setServerSetting(guildID, "adchannel", event.getOption("channel").getAsString());
                        }
                        else {
                            event.reply("Please select a text channel").queue();
                        }
                        break;
                    case "enable":
                        if (!ConfigUtils.getServerSettings(guildID).containsKey("adchannel")) {
                            event.reply("You do not have the advertisements channel set").queue();
                            return;
                        }

                        success = ConfigUtils.setServerSetting(guildID,"adenabled","true");
                        break;
                    case "disable":
                        success = ConfigUtils.setServerSetting(guildID,"adenabled","false");
                        break;
                    default:
                        event.reply("Not implemented yet").queue();
                        break;
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
