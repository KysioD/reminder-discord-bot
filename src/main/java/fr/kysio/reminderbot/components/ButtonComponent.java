package fr.kysio.reminderbot.components;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.Button;

import java.util.HashMap;

public abstract class ButtonComponent {

    public static HashMap<String, ButtonComponent> buttonComponents = new HashMap<>();

    private final Button button;

    public ButtonComponent(Button button) {
        this.button = button;
        this.buttonComponents.put(button.getCustomId().orElseThrow(), this);
    }

    public abstract void onClick(final ButtonInteractionEvent event);

    public Button getButton() {
        return button;
    }
}
