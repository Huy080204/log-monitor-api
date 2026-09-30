package logs.api.model.criteria;

import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.model.NotificationLog;
import lombok.Data;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class NotificationLogCriteria implements Serializable {

    private Long id;
    private Long appId;
    private String appName;
    private String errorName;
    private Integer status;

    @Schema(hidden = true)
    public Specification<NotificationLog> getCriteria() {
        return new Specification<NotificationLog>() {
            private static final long serialVersionUID = 1L;
            @Override
            public Predicate toPredicate(Root<NotificationLog> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (getId() != null) {
                    predicates.add(cb.equal(root.get("id"), getId()));
                }

                if (getAppId() != null) {
                    predicates.add(cb.equal(root.get("appId"), getAppId()));
                }

                if (!StringUtils.isEmpty(getAppName())) {
                    predicates.add(cb.like(cb.lower(root.get("appName")), "%" + getAppName().toLowerCase() + "%"));
                }

                if (!StringUtils.isEmpty(getErrorName())) {
                    predicates.add(cb.like(cb.lower(root.get("errorName")), "%" + getErrorName().toLowerCase() + "%"));
                }

                if (getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), getStatus()));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        };
    }
}
