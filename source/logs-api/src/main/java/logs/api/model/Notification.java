package logs.api.model;

import logs.api.constant.DatabaseConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = DatabaseConstant.PREFIX_TABLE + "notification")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Notification extends Auditable<String> {
    @Column(columnDefinition = "longtext")
    private String message;

    private Integer state; // 0: sent, 1: read

    @ManyToOne
    @JoinColumn(name = "notification_group_id")
    private NotificationGroup notificationGroup;
}
