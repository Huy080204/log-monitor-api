package logs.api.model.criteria;

import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.model.Applications;
import logs.api.model.NotificationGroup;
import logs.api.model.NotificationQuery;
import logs.api.model.QueryTemplate;
import lombok.Data;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class NotificationQueryCriteria implements Serializable {

    private Long id;
    private Long notificationGroupId;
    private Long queryTemplateId;
    private Long applicationId;
    private Integer status;

    @Schema(hidden = true)
    public Specification<NotificationQuery> getCriteria() {
        return new Specification<NotificationQuery>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Predicate toPredicate(Root<NotificationQuery> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (getId() != null) {
                    predicates.add(cb.equal(root.get("id"), getId()));
                }

                if (getNotificationGroupId() != null) {
                    Join<NotificationQuery, NotificationGroup> notificationGroupJoin = root.join("notificationGroup", JoinType.INNER);
                    predicates.add(cb.equal(notificationGroupJoin.get("id"), getNotificationGroupId()));
                }

                if (getQueryTemplateId() != null) {
                    Join<NotificationQuery, QueryTemplate> queryTemplateJoin = root.join("queryTemplate", JoinType.INNER);
                    predicates.add(cb.equal(queryTemplateJoin.get("id"), getQueryTemplateId()));
                }

                if (getApplicationId() != null) {
                    Join<NotificationQuery, Applications> applicationJoin = root.join("application", JoinType.INNER);
                    predicates.add(cb.equal(applicationJoin.get("id"), getApplicationId()));
                }

                if (getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), getStatus()));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        };
    }
}
