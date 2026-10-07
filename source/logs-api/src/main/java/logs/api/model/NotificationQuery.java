package logs.api.model;

import logs.api.constant.DatabaseConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = DatabaseConstant.PREFIX_TABLE + "notification_query")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class NotificationQuery extends Auditable<String> {

    @ManyToOne
    @JoinColumn(name = "notification_group_id")
    private NotificationGroup notificationGroup;

    @ManyToOne
    @JoinColumn(name = "query_template_id")
    private QueryTemplate queryTemplate;

    @ManyToOne
    @JoinColumn(name = "application_id")
    private Applications application;
}
