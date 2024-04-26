package fr.kysio.reminderbot.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.rest.util.Color;
import fr.kysio.reminderbot.components.ButtonComponent;
import fr.kysio.reminderbot.components.DeleteReminderButton;
import fr.kysio.reminderbot.data.ExecutionDay;
import fr.kysio.reminderbot.data.Reminder;
import fr.kysio.reminderbot.data.ids.ExecutionDayId;
import fr.kysio.reminderbot.utils.HibernateUtil;
import io.sentry.Sentry;
import org.hibernate.Session;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.Optional;

public abstract class BasicReminderCommand implements SlashCommand {
    
private final String name;

    public BasicReminderCommand(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
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

    protected void deleteReminderConfirmationModal(ButtonInteractionEvent event, Reminder reminderToDelete) {
        final DeleteReminderButton deleteReminderButton = new DeleteReminderButton(reminderToDelete.getIdReminder());
        event.reply(spec -> spec.addEmbed(embed -> {
            embed.setTitle("Delete reminder");
            embed.setDescription("Do you really want to delete the reminder " + reminderToDelete.getName() + " ?");
            embed.setColor(Color.RED);
        }).setComponents(ActionRow.of(deleteReminderButton.getButton())).setEphemeral(true)).block();

        var ref = new Object() {
            Disposable eventDisposable = null;
        };
        ref.eventDisposable = event.getClient().on(ButtonInteractionEvent.class, buttonEvent -> eventHandler(buttonEvent, ref.eventDisposable)).subscribe();
    }

    private void ttlBeforeDispose(Disposable disposable) {
        new Thread(() -> {
            try {
                Thread.sleep(600000);
                if (!disposable.isDisposed())
                    disposable.dispose();
            } catch (InterruptedException e) {
                Sentry.captureException(e);
                e.printStackTrace();
            }
        }).start();
    }

    protected void switchWeekdayExecutionStatus(ButtonInteractionEvent event, int weekday) {
        final Session session = HibernateUtil.sessionFactory.openSession();
        session.beginTransaction();

        final Long reminderId = Long.parseLong(event.getCustomId().split("_")[1]);

        final Reminder reminder = session.get(Reminder.class, reminderId);
        if (reminder == null) {
            session.getTransaction().commit();
            session.close();
            event.reply(spec -> spec.addEmbed(embed -> {
                embed.setTitle("Reminder don't exist");
                embed.setDescription("Reminder " + reminder.getName() + " can't be found");
                embed.setColor(Color.ORANGE);
            }).setEphemeral(true)).block();
            return;
        }

        final Optional<ExecutionDay> executionDayOptional = reminder.getExecutionDays().stream().filter(executionDay -> executionDay.getId().getWeekday().equals(weekday)).findFirst();
        if (executionDayOptional.isPresent()) {
            reminder.getExecutionDays().remove(executionDayOptional.get());
        } else {
            final ExecutionDay executionDay = new ExecutionDay();
            executionDay.setId(new ExecutionDayId(reminderId, weekday));
            executionDay.getId().setWeekday(weekday);
            reminder.getExecutionDays().add(executionDay);
        }

        if (!reminder.getUserUuid().equals(event.getInteraction().getMember().get().getId().asString())) {
            session.getTransaction().commit();
            session.close();
            event.reply(spec -> spec.addEmbed(embed -> {
                embed.setTitle("You can't delete this reminder");
                embed.setDescription("You can't delete the reminder " + reminder.getName() + " because you are not the creator");
                embed.setColor(Color.ORANGE);
            }).setEphemeral(true)).block();
            return;
        }

        session.update(reminder);
        session.getTransaction().commit();
        session.close();
    }
}
