package fr.kysio.reminderbot.data;

import fr.kysio.reminderbot.data.ids.ExecutionDayId;

import javax.persistence.*;

@Entity
@Table(name = "execution_day")
@IdClass(ExecutionDayId.class)
public class ExecutionDay {

    @Id
    @Column(name = "id_reminder")
    private Long idReminder;

    @Id
    private Integer weekday;

    public Long getIdReminder() {
        return idReminder;
    }

    public void setIdReminder(Long idReminder) {
        this.idReminder = idReminder;
    }

    public Integer getWeekday() {
        return weekday;
    }

    public void setWeekday(Integer weekday) {
        this.weekday = weekday;
    }
}
