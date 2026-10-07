package logs.api.model;

import logs.api.constant.DatabaseConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = DatabaseConstant.PREFIX_TABLE + "query_template")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class QueryTemplate extends Auditable<String> {
    private String name;

    private String query;

    private Integer count;

    @Column(name = "time_frame")
    private String timeFrame;

    @Column(name = "note", columnDefinition = "longtext")
    private String note;

    @ManyToOne
    @JoinColumn(name = "application_id")
    private Applications application;

    @Column(name = "type")
    private Integer type;
}
