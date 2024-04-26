package fr.kysio.reminderbot.data;

import fr.kysio.reminderbot.data.ids.ExecutionDayId;

import javax.persistence.*;

@Entity
@Table(name = "execution_day")
public class ExecutionDay {
    @EmbeddedId
    private ExecutionDayId id;

    public ExecutionDayId getId() {
        return id;
    }

    public void setId(ExecutionDayId id) {
        this.id = id;
    }
}
