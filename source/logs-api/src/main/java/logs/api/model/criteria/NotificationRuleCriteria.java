package logs.api.model.criteria;

import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.model.NotificationRule;
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
public class NotificationRuleCriteria implements Serializable {

    private Long id;
    private Integer status;
    private Long notificationGroupId;
    private String name;

    @Schema(hidden = true)
    public Specification<NotificationRule> getCriteria() {
        return new Specification<NotificationRule>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Predicate toPredicate(Root<NotificationRule> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (getId() != null) {
                    predicates.add(cb.equal(root.get("id"), getId()));
                }

                if (getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), getStatus()));
                }

                if (getNotificationGroupId() != null) {
                    predicates.add(cb.equal(root.get("notificationGroup").get("id"), getNotificationGroupId()));
                }

                if (getName() != null && !getName().isEmpty()) {
                    predicates.add(cb.like(cb.lower(root.get("name")), "%" + getName().toLowerCase() + "%"));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        };
    }
}
