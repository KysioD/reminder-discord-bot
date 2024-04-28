package fr.kysio.reminderbot.schedulers;

import discord4j.core.GatewayDiscordClient;
import fr.kysio.reminderbot.data.Reminder;
import org.hibernate.Session;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class RemindScheduler extends RemindSchedulerBase {

    public RemindScheduler(String name, GatewayDiscordClient client) {
        super(name, 0, 60000, client);
    }

    @Override
    protected List<Reminder> getReminders(Session session) {
        final LocalTime now = LocalTime.now().withSecond(0).withNano(0);

        return session.createNativeQuery("select * from reminders r " + " left join execution_day ed ON ed.idreminder = r.id_reminder " + "where (r.execution_date is not null and r.execution_date = :executionDate and r.execution_time = :executionTime) or (r.execution_date is null and ed.weekday = :executionDay and r.execution_time = :executionTime)", Reminder.class).setParameter("executionDate", LocalDate.now()).setParameter("executionTime", now).setParameter("executionDay", LocalDate.now().getDayOfWeek().getValue()).list();
    }


}
