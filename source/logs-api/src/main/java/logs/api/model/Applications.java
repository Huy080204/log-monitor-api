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
@Table(name = DatabaseConstant.PREFIX_TABLE + "application")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Applications extends Auditable<String> {
    private String name;
    private String victoriaAppId;

    @Column(columnDefinition = "text")
    private String description;
}
