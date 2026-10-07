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
import jakarta.persistence.UniqueConstraint;

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
