package fr.kysio.reminderbot.data.ids;

import fr.kysio.reminderbot.data.ExecutionDay;

import java.io.Serializable;


public class ExecutionDayId implements Serializable {

    private Long idReminder;
    private Integer weekday;

    public ExecutionDayId(Long idReminder, Integer weekday) {
        this.idReminder = idReminder;
        this.weekday = weekday;
    }

    public ExecutionDayId() {

    }

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
