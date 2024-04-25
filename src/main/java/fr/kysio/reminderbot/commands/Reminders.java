package fr.kysio.reminderbot.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.User;
import discord4j.core.spec.legacy.LegacyInteractionApplicationCommandCallbackSpec;
import discord4j.rest.util.Color;
import fr.kysio.reminderbot.components.ButtonComponent;
import fr.kysio.reminderbot.components.CallbackButton;
import fr.kysio.reminderbot.data.Reminder;
import fr.kysio.reminderbot.utils.HibernateUtil;
import org.hibernate.Session;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.List;

public class Reminders extends BasicReminderCommand {
    public Reminders() {
        super("reminders");
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        final User user = event.getOption("user")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asUser)
                .map(Mono::block)
                .orElse(event.getInteraction().getUser());

        // Get all reminders for the user
        List<Reminder> reminders = listReminders(user);

        if (reminders.isEmpty()) {
            event.reply(spec -> spec.addEmbed(embed -> {
                embed.setTitle("No reminders found");
                embed.setDescription("No reminders found for " + user.getUsername());
            })).block();
            return Mono.empty();
        }

        showReminderMessager(event, listReminders(user), 0, user);


        return Mono.empty();
    }

    private List<Reminder> listReminders(@Nullable User user) {
        if (user == null) return List.of();
        Session session = HibernateUtil.sessionFactory.openSession();
        session.beginTransaction();
        List<Reminder> reminders = session.createQuery("from Reminder where userUuid = :user")
                .setParameter("user", user.getId().asString())
                .getResultList();

        session.getTransaction().commit();
        session.close();

        return reminders;
    }

    private void showReminderMessager(final DeferrableInteractionEvent event, final List<Reminder> reminders, final int index, final User user) {
        final Reminder reminder = reminders.get(index);

        final ButtonComponent deleteButton = new CallbackButton("delete_" + reminder.getIdReminder(), "Delete", false, true, buttonEvent ->
                deleteReminderConfirmationModal(buttonEvent, reminder)
        );

        final boolean isMonday = reminder.getExecutionDays() != null && reminder.getExecutionDays().stream().anyMatch(executionDay -> executionDay.getId().getWeekday().equals(1));
        final boolean isTuesday = reminder.getExecutionDays() != null && reminder.getExecutionDays().stream().anyMatch(executionDay -> executionDay.getId().getWeekday().equals(2));
        final boolean isWednesday = reminder.getExecutionDays() != null && reminder.getExecutionDays().stream().anyMatch(executionDay -> executionDay.getId().getWeekday().equals(3));
        final boolean isThursday = reminder.getExecutionDays() != null && reminder.getExecutionDays().stream().anyMatch(executionDay -> executionDay.getId().getWeekday().equals(4));
        final boolean isFriday = reminder.getExecutionDays() != null && reminder.getExecutionDays().stream().anyMatch(executionDay -> executionDay.getId().getWeekday().equals(5));
        final boolean isSaturday = reminder.getExecutionDays() != null && reminder.getExecutionDays().stream().anyMatch(executionDay -> executionDay.getId().getWeekday().equals(6));
        final boolean isSunday = reminder.getExecutionDays() != null && reminder.getExecutionDays().stream().anyMatch(executionDay -> executionDay.getId().getWeekday().equals(7));

        final ButtonComponent mondayButton = new CallbackButton("monday_" + reminder.getIdReminder(), "Monday", isMonday, false, buttonEvent ->
                switchWeekdayExecutionStatus(buttonEvent, 1, index)
        );
        final ButtonComponent tuesdayButton = new CallbackButton("tuesday_" + reminder.getIdReminder(), "Tuesday", isTuesday, false, buttonEvent ->
                switchWeekdayExecutionStatus(buttonEvent, 2, index)
        );
        final ButtonComponent wednesdayButton = new CallbackButton("wednesday_" + reminder.getIdReminder(), "Wednesday", isWednesday, false, buttonEvent ->
                switchWeekdayExecutionStatus(buttonEvent, 3, index)
        );
        final ButtonComponent thursdayButton = new CallbackButton("thursday_" + reminder.getIdReminder(), "Thursday", isThursday, false, buttonEvent ->
                switchWeekdayExecutionStatus(buttonEvent, 4, index)
        );
        final ButtonComponent fridayButton = new CallbackButton("friday_" + reminder.getIdReminder(), "Friday", isFriday, false, buttonEvent ->
                switchWeekdayExecutionStatus(buttonEvent, 5, index)
        );
        final ButtonComponent saturdayButton = new CallbackButton("saturday_" + reminder.getIdReminder(), "Saturday", isSaturday, false, buttonEvent ->
                switchWeekdayExecutionStatus(buttonEvent, 6, index)
        );
        final ButtonComponent sundayButton = new CallbackButton("sunday_" + reminder.getIdReminder(), "Sunday", isSunday, false, buttonEvent ->
                switchWeekdayExecutionStatus(buttonEvent, 7, index)
        );

        final ButtonComponent nextButton = new CallbackButton("next_" + reminder.getIdReminder(), "Next", false, false, buttonEvent ->
                showReminderMessager(buttonEvent, reminders, index + 1, user)
        );
        final ButtonComponent previousButton = new CallbackButton("previous_" + reminder.getIdReminder(), "Previous", false, false, buttonEvent ->
                showReminderMessager(buttonEvent, reminders, index - 1, user)
        );

        var ref = new Object() {
            Disposable eventDisposable = null;
        };
        ref.eventDisposable = event.getClient().on(ButtonInteractionEvent.class, buttonEvent -> eventHandler(buttonEvent, ref.eventDisposable)).subscribe();

        event.reply(spec -> {
            final LegacyInteractionApplicationCommandCallbackSpec reply =
                    spec.addEmbed(embed -> {
                        embed.setTitle(reminder.getName());
                        embed.addField("Execution time", reminder.getExecutionTime().toString(), true);
                        if (reminder.getExecutionDate() != null)
                            embed.addField("Execution date", reminder.getExecutionDate().toString(), true);
                        embed.setFooter("By " + user.getUsername() + " | reminder " + (index + 1) + "/" + reminders.size(), user.getAvatarUrl());
                        embed.setColor(Color.BLUE);
                    }).setEphemeral(true);

            if (reminder.getExecutionDate() == null) {
                ActionRow finalRow;
                // Display next button only if there is a next reminder
                // Display previous button only if there is a previous reminder
                if (index == 0 && reminders.size() > 1) {
                    finalRow = ActionRow.of(deleteButton.getButton(), nextButton.getButton());
                } else if (index == 0 && reminders.size() == 1) {
                    finalRow = ActionRow.of(deleteButton.getButton());
                } else if (reminders.size() > (index + 1)) {
                    finalRow = ActionRow.of(deleteButton.getButton(), previousButton.getButton(), nextButton.getButton());
                } else {
                    finalRow = ActionRow.of(deleteButton.getButton(), previousButton.getButton());
                }

                reply.setComponents(ActionRow.of(mondayButton.getButton(), tuesdayButton.getButton(), wednesdayButton.getButton(), thursdayButton.getButton()),
                        ActionRow.of(fridayButton.getButton(), saturdayButton.getButton(), sundayButton.getButton()), finalRow);
            } else {
                ActionRow finalRow;
                // Display next button only if there is a next reminder
                // Display previous button only if there is a previous reminder
                if (index == 0 && reminders.size() > 1) {
                    finalRow = ActionRow.of(deleteButton.getButton(), nextButton.getButton());
                } else if (index == 0 && reminders.size() == 1) {
                    finalRow = ActionRow.of(deleteButton.getButton());
                } else if (reminders.size() > (index + 1)) {
                    finalRow = ActionRow.of(deleteButton.getButton(), previousButton.getButton(), nextButton.getButton());
                } else {
                    finalRow = ActionRow.of(deleteButton.getButton(), previousButton.getButton());
                }
                reply.setComponents(finalRow);
            }
        }).block();
    }

    protected void switchWeekdayExecutionStatus(ButtonInteractionEvent event, int weekday, int reminderIndex) {
        super.switchWeekdayExecutionStatus(event, weekday);

        this.showReminderMessager(event, listReminders(event.getInteraction().getMember().orElse(null)), reminderIndex, event.getInteraction().getUser());
    }
}
