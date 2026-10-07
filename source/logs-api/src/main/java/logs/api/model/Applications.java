package logs.api.model;

import logs.api.constant.DatabaseConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;

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
