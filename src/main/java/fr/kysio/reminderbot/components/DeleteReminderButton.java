package fr.kysio.reminderbot.components;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.Button;
import discord4j.rest.util.Color;
import fr.kysio.reminderbot.data.Reminder;
import fr.kysio.reminderbot.utils.HibernateUtil;
import org.hibernate.Session;

public class DeleteReminderButton extends ButtonComponent {

    public DeleteReminderButton(Long reminderId) {
        super(Button.danger("delete_reminder_" + reminderId, "Delete"));
    }

    @Override
    public void onClick(final ButtonInteractionEvent event) {
        final Session session = HibernateUtil.sessionFactory.openSession();
        session.beginTransaction();

        final Long reminderId = Long.parseLong(event.getCustomId().split("_")[2]);

        final Reminder reminderToDelete = session.get(Reminder.class, reminderId);
        if (reminderToDelete == null) {
            session.getTransaction().commit();
            session.close();
            event.reply(spec -> spec.addEmbed(embed -> {
                embed.setTitle("Reminder already deleted");
                embed.setDescription("Reminder " + reminderToDelete.getName() + " have already been deleted");
                embed.setColor(Color.ORANGE);
            }).setEphemeral(true)).block();
            return;
        }

        if (!reminderToDelete.getUserUuid().equals(event.getInteraction().getMember().get().getId().asString())) {
            session.getTransaction().commit();
            session.close();
            event.reply(spec -> spec.addEmbed(embed -> {
                embed.setTitle("You can't delete this reminder");
                embed.setDescription("You can't delete the reminder " + reminderToDelete.getName() + " because you are not the creator");
                embed.setColor(Color.ORANGE);
            }).setEphemeral(true)).block();
            return;
        }


        session.delete(reminderToDelete);
        session.getTransaction().commit();
        session.close();


        event.reply(spec -> spec.addEmbed(embed -> {
            embed.setTitle("Reminder deleted");
            embed.setDescription("Reminder " + reminderToDelete.getName() + " deleted");
            embed.setColor(Color.RED);
        }).setEphemeral(true)).block();
    }
}
