package fr.kysio.reminderbot.listener;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import fr.kysio.reminderbot.commands.Remember;
import fr.kysio.reminderbot.commands.Reminders;
import fr.kysio.reminderbot.commands.SlashCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

public class SlashCommandListener {
    private final static List<SlashCommand> commnads = Arrays.asList(
            new Remember(),
            new Reminders()
    );

    public static Mono<Void> handle(ChatInputInteractionEvent event) {
        return Flux.fromIterable(commnads)
                .filter(command -> command.getName().equals(event.getCommandName()))
                .next()
                .flatMap(command -> command.handle(event));
    }
}
