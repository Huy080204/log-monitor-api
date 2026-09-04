package logs.api.model;

import logs.api.constant.DatabaseConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
