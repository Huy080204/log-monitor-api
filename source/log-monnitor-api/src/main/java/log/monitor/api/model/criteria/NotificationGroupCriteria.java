package log.monitor.api.model.criteria;

import io.swagger.v3.oas.annotations.media.Schema;
import log.monitor.api.model.NotificationGroup;
import lombok.Data;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class NotificationGroupCriteria implements Serializable {

    private Long id;
    private String name;
    private Integer status;
    private Integer sortDate; // 1: created date asc, 2: created date desc

    @Schema(hidden = true)
    public Specification<NotificationGroup> getCriteria() {
        return new Specification<NotificationGroup>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Predicate toPredicate(Root<NotificationGroup> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (getId() != null) {
                    predicates.add(cb.equal(root.get("id"), getId()));
                }

                if (getName() != null && !getName().isEmpty()) {
                    predicates.add(cb.like(cb.lower(root.get("name")), "%" + getName().toLowerCase() + "%"));
                }

                if (getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), getStatus()));
                }

                if (getSortDate() != null) {
                    if (getSortDate().equals(1)) {
                        query.orderBy(cb.asc(root.get("createdDate")));
                    } else {
                        query.orderBy(cb.desc(root.get("createdDate")));
                    }
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        };
    }
}
