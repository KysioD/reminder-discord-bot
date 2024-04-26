package fr.kysio.reminderbot.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Color;
import fr.kysio.reminderbot.components.ButtonComponent;
import fr.kysio.reminderbot.components.CallbackButton;
import fr.kysio.reminderbot.data.Reminder;
import fr.kysio.reminderbot.utils.HibernateUtil;
import org.hibernate.Session;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

public class Remember extends BasicReminderCommand {
    public Remember() {
        super("remember");
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        final String title = event.getOption("title")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get();

        final LocalTime time = event.getOption("time")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(this::parseTime)
                .get();

        final Optional<LocalDate> date = event.getOption("date")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(this::parseDate);

        final Optional<User> user = event.getOption("relateduser")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asUser)
                .map(Mono::block);

        if (date.isEmpty()) {
            remindUserWithouDate(title, time, user.orElse(event.getInteraction().getUser()), event);
        } else {
            rememberWithDate(title, time, date.get(), user.orElse(event.getInteraction().getUser()), event);
        }

        return Mono.empty();
    }

    private void remindUserWithouDate(String title, LocalTime time, User user, ChatInputInteractionEvent event) {
        // Create default reminder
        final Reminder reminder = new Reminder();
        reminder.setName(title);
        reminder.setExecutionTime(time);
        reminder.setUserUuid(user.getId().asString());
        reminder.setChannelId(event.getInteraction().getChannelId().asLong());
        reminder.setGuildId(event.getInteraction().getGuildId().get().asLong());
        reminder.setCreatorUuid(event.getInteraction().getUser().getId().asString());
        reminder.setCreationDate(LocalDateTime.now());

        Session session = HibernateUtil.sessionFactory.openSession();
        session.beginTransaction();
        session.save(reminder);
        session.getTransaction().commit();
        session.close();

        displayRemindUserWithoutDate(title, time, user, event, reminder);
    }

    private void displayRemindUserWithoutDate(String title, LocalTime time, User user, DeferrableInteractionEvent event, Reminder reminder) {
        final boolean isMonday = reminder.getExecutionDays() != null && reminder.getExecutionDays().stream().anyMatch(executionDay -> executionDay.getId().getWeekday().equals(1));
        final boolean isTuesday = reminder.getExecutionDays() != null && reminder.getExecutionDays().stream().anyMatch(executionDay -> executionDay.getId().getWeekday().equals(2));
        final boolean isWednesday = reminder.getExecutionDays() != null && reminder.getExecutionDays().stream().anyMatch(executionDay -> executionDay.getId().getWeekday().equals(3));
        final boolean isThursday = reminder.getExecutionDays() != null && reminder.getExecutionDays().stream().anyMatch(executionDay -> executionDay.getId().getWeekday().equals(4));
        final boolean isFriday = reminder.getExecutionDays() != null && reminder.getExecutionDays().stream().anyMatch(executionDay -> executionDay.getId().getWeekday().equals(5));
        final boolean isSaturday = reminder.getExecutionDays() != null && reminder.getExecutionDays().stream().anyMatch(executionDay -> executionDay.getId().getWeekday().equals(6));
        final boolean isSunday = reminder.getExecutionDays() != null && reminder.getExecutionDays().stream().anyMatch(executionDay -> executionDay.getId().getWeekday().equals(7));

        final ButtonComponent mondayButton = new CallbackButton("monday_" + reminder.getIdReminder(), "Monday", isMonday, false, buttonEvent ->
                switchWeekdayExecutionStatus(buttonEvent, 1)
        );
        final ButtonComponent tuesdayButton = new CallbackButton("tuesday_" + reminder.getIdReminder(), "Tuesday", isTuesday, false, buttonEvent ->
                switchWeekdayExecutionStatus(buttonEvent, 2)
        );
        final ButtonComponent wednesdayButton = new CallbackButton("wednesday_" + reminder.getIdReminder(), "Wednesday", isWednesday, false, buttonEvent ->
                switchWeekdayExecutionStatus(buttonEvent, 3)
        );
        final ButtonComponent thursdayButton = new CallbackButton("thursday_" + reminder.getIdReminder(), "Thursday", isThursday, false, buttonEvent ->
                switchWeekdayExecutionStatus(buttonEvent, 4)
        );
        final ButtonComponent fridayButton = new CallbackButton("friday_" + reminder.getIdReminder(), "Friday", isFriday, false, buttonEvent ->
                switchWeekdayExecutionStatus(buttonEvent, 5)
        );
        final ButtonComponent saturdayButton = new CallbackButton("saturday_" + reminder.getIdReminder(), "Saturday", isSaturday, false, buttonEvent ->
                switchWeekdayExecutionStatus(buttonEvent, 6)
        );
        final ButtonComponent sundayButton = new CallbackButton("sunday_" + reminder.getIdReminder(), "Sunday", isSunday, false, buttonEvent ->
                switchWeekdayExecutionStatus(buttonEvent, 7)
        );

        final ButtonComponent deleteButton = new CallbackButton("delete_" + reminder.getIdReminder(), "Delete", false, true, buttonEvent ->
                deleteReminderConfirmationModal(buttonEvent, reminder)
        );

        var ref = new Object() {
            Disposable eventDisposable = null;
        };
        ref.eventDisposable = event.getClient().on(ButtonInteractionEvent.class, buttonEvent -> eventHandler(buttonEvent, ref.eventDisposable)).subscribe();


        // Send message with buttons
        event.reply(spec -> spec.addEmbed(embed -> {
                    embed.setTitle("Reminder : " + title);
                    embed.setDescription("Reminded at : " + time + "\nSelect the days of the week you want to be reminded");
                    embed.setFooter("By : " + user.getUsername(), null);
                    embed.setColor(Color.GREEN);
                }).setComponents(ActionRow.of(mondayButton.getButton(), tuesdayButton.getButton(), wednesdayButton.getButton(), thursdayButton.getButton()), ActionRow.of(fridayButton.getButton(), saturdayButton.getButton(), sundayButton.getButton()), ActionRow.of(deleteButton.getButton())).setEphemeral(true))
                .block();
    }

    private void rememberWithDate(String title, LocalTime time, LocalDate date, User user, ChatInputInteractionEvent event) {
        // Create default reminder
        final Reminder reminder = new Reminder();
        reminder.setName(title);
        reminder.setExecutionTime(time);
        reminder.setExecutionDate(date);
        reminder.setUserUuid(user.getId().asString());
        reminder.setChannelId(event.getInteraction().getChannelId().asLong());
        reminder.setGuildId(event.getInteraction().getGuildId().get().asLong());
        reminder.setCreatorUuid(event.getInteraction().getUser().getId().asString());
        reminder.setCreationDate(LocalDateTime.now());

        Session session = HibernateUtil.sessionFactory.openSession();
        session.beginTransaction();
        session.save(reminder);
        session.getTransaction().commit();
        session.close();


        displayRemindUserWithDate(title, time, date, user, event, reminder);
    }

    private void displayRemindUserWithDate(String title, LocalTime time, LocalDate date, User user, DeferrableInteractionEvent event, Reminder reminder) {
        final ButtonComponent deleteButton = new CallbackButton("delete_" + reminder.getIdReminder(), "Delete", false, true, buttonEvent ->
                deleteReminderConfirmationModal(buttonEvent, reminder)
        );

        var ref = new Object() {
            Disposable eventDisposable = null;
        };
        ref.eventDisposable = event.getClient().on(ButtonInteractionEvent.class, buttonEvent -> eventHandler(buttonEvent, ref.eventDisposable)).subscribe();


        event.reply(spec -> spec.addEmbed(embed -> {
            embed.setTitle("Reminder : " + title);
            embed.setDescription("Reminded at : " + time + " on " + date);
            embed.setFooter("By : " + user.getUsername(), null);
        }).setComponents(ActionRow.of(deleteButton.getButton())).setEphemeral(true)).block();
    }

    private LocalTime parseTime(String time) {
        final String[] split = time.split(":");
        return LocalTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
    }

    /**
     * Date format is dd/MM/yyyy
     * @param date
     * @return
     */
    private LocalDate parseDate(String date) {
        final String[] split = date.split("/");
        return LocalDate.of(Integer.parseInt(split[2]), Integer.parseInt(split[1]), Integer.parseInt(split[0]));
    }

    @Override
    protected void switchWeekdayExecutionStatus(ButtonInteractionEvent event, int weekday) {
        super.switchWeekdayExecutionStatus(event, weekday);

        final Long reminderId = Long.parseLong(event.getCustomId().split("_")[1]);
        final Session session = HibernateUtil.sessionFactory.openSession();
        session.beginTransaction();

        final Reminder reminder = session.get(Reminder.class, reminderId);
        if (reminder.getExecutionDate() == null) {
            displayRemindUserWithoutDate(reminder.getName(), reminder.getExecutionTime(), event.getInteraction().getUser(), event, reminder);
        } else {
            displayRemindUserWithDate(reminder.getName(), reminder.getExecutionTime(), LocalDate.now(), event.getInteraction().getUser(), event, reminder);
        }

        session.getTransaction().commit();
        session.close();
    }
}
