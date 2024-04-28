package fr.kysio.reminderbot.schedulers;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Color;
import fr.kysio.reminderbot.components.ButtonComponent;
import fr.kysio.reminderbot.components.CallbackButton;
import fr.kysio.reminderbot.data.Reminder;
import fr.kysio.reminderbot.data.ReminderHistory;
import fr.kysio.reminderbot.utils.HibernateUtil;
import io.sentry.Sentry;
import org.hibernate.Session;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public abstract class RemindSchedulerBase extends Scheduler {
    private final GatewayDiscordClient client;

    public RemindSchedulerBase(String name, long startingDelay, long delay, GatewayDiscordClient client) {
        super(name, startingDelay, delay);
        this.client = client;
    }

    protected abstract List<Reminder> getReminders(Session session);

    @Override
    public void process() {
        System.out.println("Processing reminders "+this.getName());
        // Get all reminders that need to be sent
        Session session = HibernateUtil.sessionFactory.openSession();
        session.beginTransaction();

        List<Reminder> reminders = getReminders(session);

        // Send reminders
        reminders.forEach(reminder -> {
            final ButtonComponent validateButton = new CallbackButton("validate_" + reminder.getIdReminder(), "Validate", true, false, buttonEvent -> {
                Session session1 = HibernateUtil.sessionFactory.openSession();
                session1.beginTransaction();

                // Get reminderhistory from reminder id
                ReminderHistory reminderHistory = session1.createNativeQuery("select * from reminder_history where id_reminder = :idReminder and CAST(executionDate AS DATE) = current_date", ReminderHistory.class)
                        .setParameter("idReminder", reminder.getIdReminder())
                        .uniqueResult();

                reminderHistory.setChecked(true);
                session1.update(reminderHistory);

                session1.getTransaction().commit();
                session1.close();

                buttonEvent.reply(spec -> spec.addEmbed(embed -> {
                    embed.setTitle("Reminder validated");
                    embed.setDescription("Reminder : " + reminder.getName() + " validated !");
                    embed.setColor(Color.GREEN);
                })).block();
            });

            final Snowflake userId = Snowflake.of(reminder.getUserUuid());
            final Snowflake creatorId = Snowflake.of(reminder.getCreatorUuid());
            User creator = client.getUserById(creatorId).block();
            client.getUserById(userId).flatMap(user -> user.getPrivateChannel()).flatMap(channel -> channel.createMessage(spec -> spec.addEmbed(embed -> {
                embed.setTitle("Reminder");
                embed.setDescription("It's time for " + reminder.getName() + " !");
                embed.setFooter("Reminder created by " + creator.getUsername() + " on " + reminder.getCreationDate(), creator.getAvatarUrl());
                embed.setTimestamp(LocalDateTime.now().toInstant(ZoneOffset.ofHours(2)));
                embed.setColor(Color.JAZZBERRY_JAM);
            }).setComponents(ActionRow.of(validateButton.getButton())))).block();

            // Check if reminder already exist for this datetime

            ReminderHistory existingReminderHistory = session.createNativeQuery("select * from reminder_history where id_reminder = :idReminder and CAST(executionDate AS DATE) = current_date", ReminderHistory.class)
                    .setParameter("idReminder", reminder.getIdReminder())
                    .uniqueResult();

            System.out.println("existing reminder history");

            if (existingReminderHistory == null) {
                ReminderHistory reminderHistory = new ReminderHistory();
                reminderHistory.setReminder(reminder);
                reminderHistory.setExecutionDate(LocalDateTime.now());
                reminderHistory.setIdReminder(reminder.getIdReminder());
                reminderHistory.setChecked(false);
                session.save(reminderHistory);
            }

            var ref = new Object() {
                Disposable eventDisposable = null;
            };
            ref.eventDisposable = client.on(ButtonInteractionEvent.class, buttonEvent -> eventHandler(buttonEvent, ref.eventDisposable)).subscribe();
        });

        session.getTransaction().commit();
        session.close();
    }

    private void ttlBeforeDispose(Disposable disposable) {
        new Thread(() -> {
            try {
                Thread.sleep(600000);
                if (!disposable.isDisposed()) disposable.dispose();
            } catch (InterruptedException e) {
                Sentry.captureException(e);
                e.printStackTrace();
            }
        }).start();
    }

    protected Mono<Void> eventHandler(ButtonInteractionEvent event, Disposable disposable) {
        ttlBeforeDispose(disposable);
        final String customId = event.getCustomId();
        if (ButtonComponent.buttonComponents.containsKey(customId)) {
            disposable.dispose();
            ButtonComponent.buttonComponents.get(customId).onClick(event);
        }
        return Mono.empty();
    }
}
