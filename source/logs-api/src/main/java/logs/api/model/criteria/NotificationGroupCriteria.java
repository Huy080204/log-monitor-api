package logs.api.model.criteria;

import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.model.NotificationGroup;
import lombok.Data;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class NotificationGroupCriteria implements Serializable {

    private Long id;
    private String name;
    private Integer status;
    private Integer sortDate; // 1: created date asc, 2: created date desc
    private Integer checkType;

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

                if (getCheckType() != null) {
                    predicates.add(cb.equal(root.get("checkType"), getCheckType()));
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
