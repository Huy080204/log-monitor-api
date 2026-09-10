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
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = DatabaseConstant.PREFIX_TABLE + "notification_rule_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"notification_rule_id", "ordering"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class NotificationRuleItem extends Auditable<String> {

    @ManyToOne
    @JoinColumn(name = "notification_rule_id")
    private NotificationRule notificationRule;

    @ManyToOne
    @JoinColumn(name = "query_template_id")
    private QueryTemplate queryTemplate;

    @ManyToOne
    @JoinColumn(name = "application_id")
    private Applications application;

    private Integer ordering;

    private Integer operator;
}
