package me.emerald.finlandbot.commands;

import me.emerald.finlandbot.Main;
import me.emerald.finlandbot.utils.IDUtils;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.earthmc.emcapiclient.object.identifier.DiscordIdentifier;

public class IDCommand {

    public void idCommand(SlashCommandInteractionEvent event) {
        String discID = event.getUser().getId();
        DiscordIdentifier discData = Main.client.getDiscordIdentifierByString(discID);
        String playerID = discData.getUUID();

        if (playerID==null) {
            event.reply("Your minecraft account is not currently linked to your discord.\n" +
                    "To link your discord account log onto **Aurora** and run the '**/discord link**' command " +
                    "and follow the instructions provided in chat."
            ).queue();
        }
        else {
            String id = IDUtils.generateID(discID,playerID);
            boolean writeSuccess = IDUtils.storeID(id,playerID);
            if (writeSuccess) {
                boolean dmSuccess = Main.sendDM(event.getUser(),"Here's your voterID: || "+id+" || \nDo not share it with others!");
                if (dmSuccess) {
                    event.reply("VoterID generation successful! Check your DMs").queue();
                }
                else {
                    event.reply("Can't send voterID, please make sure you have the " +
                            "'Allow direct messages from server members' setting enabled").queue();
                }
            }
            else {
                event.reply("Something went wrong with ID generation, please notify Emerald").queue();
            }
        }
    }
}