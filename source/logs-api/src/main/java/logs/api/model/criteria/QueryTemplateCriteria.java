package logs.api.model.criteria;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import logs.api.model.Applications;
import logs.api.model.NotificationQuery;
import logs.api.model.QueryTemplate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class QueryTemplateCriteria implements Serializable {

    private Long id;
    private String name;
    private Integer status;
    private Long applicationId;
    private Long ignoreNotificationGroupId;
    private Boolean global;

    @Schema(hidden = true)
    public Specification<QueryTemplate> getCriteria() {
        return new Specification<QueryTemplate>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Predicate toPredicate(Root<QueryTemplate> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (getId() != null) {
                    predicates.add(cb.equal(root.get("id"), getId()));
                }

                if (!StringUtils.isEmpty(getName())) {
                    predicates.add(cb.like(cb.lower(root.get("name")), "%" + getName().toLowerCase() + "%"));
                }

                if (getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), getStatus()));
                }

                if (getApplicationId() != null) {
                    Join<QueryTemplate, Applications> joinApplication = root.join("application", JoinType.INNER);
                    predicates.add(cb.equal(joinApplication.get("id"), getApplicationId()));
                }

                if (Boolean.TRUE.equals(getGlobal())) {
                    predicates.add(cb.isNull(root.get("application")));
                }

                if (getIgnoreNotificationGroupId() != null) {
                    Subquery<Long> ignoreSubquery = query.subquery(Long.class);
                    Root<NotificationQuery> notificationQueryRoot = ignoreSubquery.from(NotificationQuery.class);
                    ignoreSubquery.select(notificationQueryRoot.get("queryTemplate").get("id"));
                    ignoreSubquery.where(cb.equal(notificationQueryRoot.get("notificationGroup").get("id"), getIgnoreNotificationGroupId()));
                    predicates.add(cb.not(root.get("id").in(ignoreSubquery)));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        };
    }
}
