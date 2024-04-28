package fr.kysio.reminderbot.schedulers;

import discord4j.core.GatewayDiscordClient;
import fr.kysio.reminderbot.data.Reminder;
import org.hibernate.Session;

import java.util.List;

public class RemindVerificationScheduler extends RemindSchedulerBase {

    public RemindVerificationScheduler(String name, GatewayDiscordClient client) {
        super(name, 60000, 600000, client);
    }

    @Override
    protected List<Reminder> getReminders(Session session) {
        return session.createNativeQuery("select * from reminder_history rh " +
                "inner join reminders r on r.id_reminder = rh.id_reminder " +
                "where CAST(rh.executiondate AS DATE) = current_date and rh.is_checked = false", Reminder.class).list();
    }

}
