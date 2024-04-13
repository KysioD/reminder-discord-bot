package fr.kysio.reminderbot.commands;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Color;
import fr.kysio.reminderbot.data.Reminder;
import fr.kysio.reminderbot.utils.HibernateUtil;
import org.hibernate.Session;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public class Reminders implements SlashCommand {
    @Override
    public String getName() {
        return "reminders";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        final User user = event.getOption("user")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asUser)
                .map(Mono::block)
                .orElse(event.getInteraction().getUser());

        // Get all reminders for the user
        Session session = HibernateUtil.sessionFactory.openSession();
        session.beginTransaction();
        List<Reminder> reminders = session.createQuery("from Reminder where userUuid = :user")
                .setParameter("user", user.getId().asString())
                .getResultList();

        if (reminders.isEmpty()) {
            event.reply(spec -> spec.addEmbed(embed -> {
                embed.setTitle("No reminders found");
                embed.setDescription("No reminders found for " + user.getUsername());
            })).block();
            return Mono.empty();
        }

        showReminderMessager(event, reminders, 0, user);

        session.getTransaction().commit();
        session.close();

        return Mono.empty();
    }

    private void showReminderMessager(final DeferrableInteractionEvent event, final List<Reminder> reminders, final int index, final User user) {
        final Reminder reminder = reminders.get(index);

        final Button deleteButton = Button.danger("delete_reminder_"+reminder.getIdReminder(), "Delete reminder");
        final Button mondayButton = Button.primary("monday", "Monday");
        final Button tuesdayButton = Button.primary("tuesday", "Tuesday");
        final Button wednesdayButton = Button.primary("wednesday", "Wednesday");
        final Button thursdayButton = Button.primary("thursday", "Thursday");
        final Button fridayButton = Button.primary("friday", "Friday");
        final Button saturdayButton = Button.primary("saturday", "Saturday");
        final Button sundayButton = Button.primary("sunday", "Sunday");

        final Button nextButton = Button.secondary("next", "Next");
        final Button previousButton = Button.secondary("previous", "Previous");

        event.getClient().on(ButtonInteractionEvent.class, buttonEvent -> {
            if (buttonEvent.getCustomId().equals("delete_reminder_"+reminder.getIdReminder())) {
                // Delete reminder
                Session session = HibernateUtil.sessionFactory.openSession();
                session.beginTransaction();
                // Get remember with id
                Reminder reminderToDelete = session.get(Reminder.class, reminder.getIdReminder());
                session.delete(reminderToDelete);
                session.getTransaction().commit();
                session.close();

                buttonEvent.reply(spec -> spec.addEmbed(embed -> {
                    embed.setTitle("Reminder deleted");
                    embed.setDescription("Reminder " + reminder.getName() + " deleted");
                    embed.setColor(Color.RED);
                }).setEphemeral(true)).block();

                return Mono.empty();
            } else if (buttonEvent.getCustomId().equals("next")) {
                showReminderMessager(buttonEvent, reminders, index + 1, user);
                return Mono.empty();
            } else if (buttonEvent.getCustomId().equals("previous")) {
                showReminderMessager(buttonEvent, reminders, index - 1, user);
                return Mono.empty();
            }

            return Mono.empty();
        }).subscribe();

        event.reply(spec -> spec.addEmbed(embed -> {
            embed.setTitle(reminder.getName());
            embed.addField("Execution time", reminder.getExecutionTime().toString(), true);
            if (reminder.getExecutionDate() != null)
                embed.addField("Execution date", reminder.getExecutionDate().toString(), true);
            embed.setFooter("By " + user.getUsername() + " | reminder " + (index + 1) + "/" + reminders.size(), user.getAvatarUrl());
            embed.setColor(Color.BLUE);
        }).setComponents(ActionRow.of(mondayButton, tuesdayButton, wednesdayButton, thursdayButton),
                ActionRow.of(fridayButton, saturdayButton, sundayButton),
                ActionRow.of(deleteButton, previousButton, nextButton))).block();
    }
}
