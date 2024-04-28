package fr.kysio.reminderbot;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import fr.kysio.reminderbot.listener.SlashCommandListener;
import fr.kysio.reminderbot.schedulers.RemindScheduler;
import fr.kysio.reminderbot.schedulers.RemindVerificationScheduler;
import fr.kysio.reminderbot.utils.GlobalCommandRegistrar;
import fr.kysio.reminderbot.utils.HibernateUtil;
import fr.kysio.reminderbot.utils.QuoteRandomizer;
import io.sentry.Sentry;

import java.net.URISyntaxException;
import java.util.List;

public class Main {
    private static final String DISCORD_TOKEN = System.getenv("DISCORD_TOKEN");
    private static final List<String> COMMANDS = List.of("remember.json", "reminders.json");

    public static void main(String[] args) {
        setupSentry();

        try {
            HibernateUtil.createSessionFactory();
        } catch (URISyntaxException e) {
            Sentry.captureException(e);
            throw new RuntimeException(e);
        }
        final GatewayDiscordClient client = DiscordClientBuilder.create(DISCORD_TOKEN)
                .build()
                .login()
                .block();

        // Set client status
        client.updatePresence(ClientPresence.of(Status.ONLINE, ClientActivity.custom(QuoteRandomizer.getRandomQuote())))
                .block();

        try {
            new GlobalCommandRegistrar(client.getRestClient()).registerCommands(COMMANDS);
        } catch (Exception e) {
            Sentry.captureException(e);
            e.printStackTrace();
        }

        startSchedulers(client);

        client.on(ChatInputInteractionEvent.class, SlashCommandListener::handle)
                .then(client.onDisconnect())
                .block();

    }

    private static void setupSentry() {
        Sentry.init(options -> {
            options.setDsn(System.getenv("SENTRY_DSN"));
            options.setTracesSampleRate(1.0);
            options.setEnableMetrics(true);
        });

        Sentry.configureScope(scope -> {
            scope.setTag("environment", System.getenv("ENVIRONMENT"));
            scope.setTag("app", "reminder-bot");
        });

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Sentry.captureException(e);
            e.printStackTrace();
        });
    }

    private static void startSchedulers(GatewayDiscordClient client) {
        new RemindScheduler("RemindScheduler", client).start();
        new RemindVerificationScheduler("RemindVerificationScheduler", client).start();
    }
}