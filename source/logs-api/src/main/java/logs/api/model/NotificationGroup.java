package logs.api.model;

import logs.api.constant.DatabaseConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Entity
@Table(name = DatabaseConstant.PREFIX_TABLE + "notification_group")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class NotificationGroup extends Auditable<String> {
    private String name;

    private Integer checkType; // 1: threshold, 2: comparison

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne
    @JoinColumn(name = "notification_channel_id")
    private NotificationChannel notificationChannel;

    private Integer timeFrame; // minute


    private String filterQuery;
}
