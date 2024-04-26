package fr.kysio.reminderbot;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.rest.util.Color;
import fr.kysio.reminderbot.data.Reminder;
import fr.kysio.reminderbot.data.ReminderHistory;
import fr.kysio.reminderbot.listener.SlashCommandListener;
import fr.kysio.reminderbot.utils.GlobalCommandRegistrar;
import fr.kysio.reminderbot.utils.HibernateUtil;
import fr.kysio.reminderbot.utils.QuoteRandomizer;
import io.sentry.Sentry;
import org.hibernate.Session;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
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

        startReminderThread(client);

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
        });
    }

    /**
     * Task that run every minute to check if there is any reminder to send
     */
    private static void startReminderThread(GatewayDiscordClient client) {
        new Thread(() -> {
            while (true) {
                try {
                    // Get all reminders that need to be sent
                    Session session = HibernateUtil.sessionFactory.openSession();
                    session.beginTransaction();

                    // Get current time without seconds
                    final LocalTime now = LocalTime.now().withSecond(0).withNano(0);

                    List<Reminder> reminders = session.createNativeQuery("select * from reminders r\n" +
                                    "left join execution_day ed ON ed.idreminder = r.id_reminder\n" +
                                    "where (r.execution_date is not null and r.execution_date = :executionDate and r.execution_time = :executionTime) or (r.execution_date is null and ed.weekday = :executionDay and r.execution_time = :executionTime)", Reminder.class)
                            .setParameter("executionDate", LocalDate.now())
                            .setParameter("executionTime", now)
                            .setParameter("executionDay", LocalDate.now().getDayOfWeek().getValue()
                            ).list();

                    // Send reminders
                    reminders.forEach(reminder -> {
                        final Snowflake userId = Snowflake.of(reminder.getUserUuid());
                        final Snowflake creatorId = Snowflake.of(reminder.getCreatorUuid());
                        User creator = client.getUserById(creatorId).block();
                        client.getUserById(userId)
                                .flatMap(user -> user.getPrivateChannel())
                                .flatMap(channel -> channel.createMessage(spec -> spec.addEmbed(embed -> {
                                            embed.setTitle("Reminder");
                                            embed.setDescription("It's time for " + reminder.getName() + " !");
                                            embed.setFooter("Reminder created by " + creator.getUsername() + " on " + reminder.getCreationDate(), creator.getAvatarUrl());
                                            embed.setTimestamp(LocalDateTime.now().toInstant(ZoneOffset.ofHours(2)));
                                            embed.setColor(Color.JAZZBERRY_JAM);
                                        })
                                ))
                                .block();

                        ReminderHistory reminderHistory = new ReminderHistory();
                        reminderHistory.setReminder(reminder);
                        reminderHistory.setExecutionDate(LocalDateTime.now());
                        reminderHistory.setIdReminder(reminder.getIdReminder());
                        session.save(reminderHistory);
                    });

                    session.getTransaction().commit();
                    session.close();
                    Thread.sleep(60000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Sentry.captureException(e);
                }
            }
        }).start();
    }
}