package fr.kysio.reminderbot.data;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Table(name = "reminders")
@Entity
public class Reminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reminder")
    private Long idReminder;

    private String name;

    @Column(name = "user_uuid")
    private String userUuid;

    @Column(name = "execution_time")
    private LocalTime executionTime;

    @Column(name = "execution_date")
    private LocalDate executionDate;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "channel_id")
    private Long channelId;

    @Column(name = "guild_id")
    private Long guildId;

    @Column(name = "creator_uuid")
    private String creatorUuid;

    @JoinColumn(name = "id_reminder")
    @OneToMany(cascade = CascadeType.ALL)
    private List<ExecutionDay> executionDays;

    public Long getIdReminder() {
        return idReminder;
    }

    public void setIdReminder(Long idReminder) {
        this.idReminder = idReminder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public LocalTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(LocalTime executionTime) {
        this.executionTime = executionTime;
    }

    public LocalDate getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(LocalDate executionDate) {
        this.executionDate = executionDate;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getGuildId() {
        return guildId;
    }

    public void setGuildId(Long guildId) {
        this.guildId = guildId;
    }

    public String getCreatorUuid() {
        return creatorUuid;
    }

    public void setCreatorUuid(String creatorUuid) {
        this.creatorUuid = creatorUuid;
    }

    public List<ExecutionDay> getExecutionDays() {
        return executionDays;
    }

    public void setExecutionDays(List<ExecutionDay> executionDays) {
        this.executionDays = executionDays;
    }
}
