package log.monitor.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import log.monitor.api.constant.DatabaseConstant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;

@Entity
@Table(name = DatabaseConstant.PREFIX_TABLE + "account")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Account extends Auditable<String> {
    private Integer kind;
    private String username;
    private String phone;
    private String email;
    @Column(name = "full_name")
    private String fullName;

    @Column(name = "avatar_path")
    private String avatarPath;

    @Column(name = "is_super_admin")
    private Boolean isSuperAdmin = false;
}
