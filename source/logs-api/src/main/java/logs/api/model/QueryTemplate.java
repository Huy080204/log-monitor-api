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
@Table(name = DatabaseConstant.PREFIX_TABLE + "query_template")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class QueryTemplate extends Auditable<String> {
    private String name;

    private String query;

    private Integer count;

    @ManyToOne
    @JoinColumn(name = "application_id")
    private Applications application;
}
