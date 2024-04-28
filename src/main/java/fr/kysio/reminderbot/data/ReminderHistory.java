package fr.kysio.reminderbot.data;

import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reminder_history")
public class ReminderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReminder;

    private LocalDateTime executionDate;

    @JoinColumn(name = "id_reminder")
    @OneToOne
    private Reminder reminder;

    @Column(name = "is_checked")
    @ColumnDefault("false")
    private Boolean isChecked;

    public Boolean getChecked() {
        return isChecked;
    }

    public void setChecked(Boolean checked) {
        isChecked = checked;
    }

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
