package fr.kysio.reminderbot;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import fr.kysio.reminderbot.listener.SlashCommandListener;
import fr.kysio.reminderbot.utils.GlobalCommandRegistrar;
import fr.kysio.reminderbot.utils.HibernateUtil;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import reactor.core.publisher.Mono;

import java.util.List;

public class Main {
    private static final String DISCORD_TOKEN = System.getenv("DISCORD_TOKEN");
    private static final List<String> COMMANDS = List.of("remember.json");

    public static void main(String[] args) {
        HibernateUtil.createSessionFactory();
        final GatewayDiscordClient client = DiscordClientBuilder.create(DISCORD_TOKEN)
                .build()
                .login()
                .block();

        // Set client status
        client.updatePresence(ClientPresence.of(Status.ONLINE, ClientActivity.watching("The world burn")))
                        .block();

        try {
            new GlobalCommandRegistrar(client.getRestClient()).registerCommands(COMMANDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        client.on(ChatInputInteractionEvent.class, SlashCommandListener::handle)
                .then(client.onDisconnect())
                .block();
    }
}