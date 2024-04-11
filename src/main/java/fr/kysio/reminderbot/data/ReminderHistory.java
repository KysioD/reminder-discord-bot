package fr.kysio.reminderbot.data;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reminder_history")
public class ReminderHistory {

    @Id
    private Long idReminder;

    private LocalDateTime executionDate;

    @JoinColumn(name = "id_reminder")
    @OneToOne
    private Reminder reminder;

    public Long getIdReminder() {
        return idReminder;
    }

    public void setIdReminder(Long idReminder) {
        this.idReminder = idReminder;
    }

    public LocalDateTime getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(LocalDateTime executionDate) {
        this.executionDate = executionDate;
    }

    public Reminder getReminder() {
        return reminder;
    }

    public void setReminder(Reminder reminder) {
        this.reminder = reminder;
    }
}
