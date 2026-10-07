package logs.api.model;

import logs.api.constant.DatabaseConstant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;

@Entity
@Table(name = DatabaseConstant.PREFIX_TABLE + "notification_log")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog extends Auditable<String> {
    private Long appId;
    private String appName;
    private String errorName;

    @Column(columnDefinition = "text")
    private String link;
}
