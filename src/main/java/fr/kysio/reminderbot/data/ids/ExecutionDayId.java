package fr.kysio.reminderbot.data.ids;

import lombok.AllArgsConstructor;

import java.io.Serializable;


public class ExecutionDayId implements Serializable {

    private Integer idReminder;
    private Integer weekday;

    public ExecutionDayId(Integer idReminder, Integer weekday) {
        this.idReminder = idReminder;
        this.weekday = weekday;
    }
}
