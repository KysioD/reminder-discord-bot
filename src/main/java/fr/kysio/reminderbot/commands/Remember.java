package fr.kysio.reminderbot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import fr.kysio.reminderbot.data.Reminder;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public class Remember implements SlashCommand {
    @Override
    public String getName() {
        return "remember";
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
                .map(LocalDate::parse);

        final Optional<User> user = event.getOption("relateduser")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asUser)
                .map(Mono::block);

        if (date.isEmpty()) {
            remindUserWithouDate(title, time, user.orElse(event.getInteraction().getUser()), event);
        } else {
            event.reply(spec -> spec.addEmbed(embed -> {
                embed.setTitle("Error");
                embed.setDescription("This feature is not yet implemented");
            }));
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

        //TODO: Save reminder in database

        // Send embed with button for each weekday

        Button mondayButton = Button.primary("monday", "Monday");
        Button tuesdayButton = Button.primary("tuesday", "Tuesday");
        Button wednesdayButton = Button.primary("wednesday", "Wednesday");
        Button thursdayButton = Button.primary("thursday", "Thursday");
        Button fridayButton = Button.primary("friday", "Friday");
        Button saturdayButton = Button.primary("saturday", "Saturday");
        Button sundayButton = Button.primary("sunday", "Sunday");

        // Send message with buttons
        event.reply(spec -> spec.addEmbed(embed -> {
            embed.setTitle("Reminder : " + title);
            embed.setDescription("Reminded at : "+time+"\nSelect the days of the week you want to be reminded");
        }).setComponents(ActionRow.of(mondayButton, tuesdayButton, wednesdayButton), ActionRow.of(thursdayButton, fridayButton, saturdayButton, sundayButton)))
                .block();
    }

    private LocalTime parseTime(String time) {
        final String[] split = time.split(":");
        return LocalTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
    }
}
