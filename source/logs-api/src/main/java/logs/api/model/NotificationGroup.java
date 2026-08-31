package logs.api.model;

import logs.api.constant.DatabaseConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;

@Entity
@Table(name = DatabaseConstant.PREFIX_TABLE + "notification_group")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class NotificationGroup extends Auditable<String> {
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "longtext")
    private String channelSetting;

    private Integer type; // 0: telegram, 1: slack
}
