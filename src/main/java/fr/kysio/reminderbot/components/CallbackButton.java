package fr.kysio.reminderbot.components;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.Button;

import java.util.function.Consumer;
import java.util.function.Function;

public class CallbackButton extends ButtonComponent {

        final boolean active;
        final Consumer<ButtonInteractionEvent> callback;

        public CallbackButton(String customId, String label, boolean active, boolean red, Consumer<ButtonInteractionEvent> callback) {
            super(red ? Button.danger(customId, label) : (active ? Button.success(customId, label) : Button.secondary(customId, label)));
            this.active = active;
            this.callback = callback;
        }

        @Override
        public void onClick(final ButtonInteractionEvent event) {
            callback.accept(event);
        }

        public boolean isActive() {
            return active;
        }

        public Consumer<ButtonInteractionEvent> getCallback() {
            return callback;
        }
}
