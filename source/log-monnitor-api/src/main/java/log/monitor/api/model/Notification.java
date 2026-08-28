package log.monitor.api.model;

import log.monitor.api.constant.DatabaseConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
