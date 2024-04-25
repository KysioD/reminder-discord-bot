package fr.kysio.reminderbot.data;

import fr.kysio.reminderbot.data.ids.ExecutionDayId;

import javax.persistence.*;

@Entity
@Table(name = "execution_day")
//@IdClass(ExecutionDayId.class)
public class ExecutionDay {
/*
    @Id
    @Column(name = "id_reminder")
    private Long idReminder;

    @Id
    private Integer weekday;*/
    @EmbeddedId
    private ExecutionDayId id;

    public ExecutionDayId getId() {
        return id;
    }

    public void setId(ExecutionDayId id) {
        this.id = id;
    }
}
