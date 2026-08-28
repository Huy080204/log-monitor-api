package log.monitor.api.model.criteria;

import io.swagger.v3.oas.annotations.media.Schema;
import log.monitor.api.model.Notification;
import log.monitor.api.model.NotificationGroup;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
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
public class NotificationCriteria implements Serializable {

    private Long id;
    private String message;
    private Integer state;
    private Long notificationGroupId;
    private Integer status;

    @Schema(hidden = true)
    public Specification<Notification> getCriteria() {
        return new Specification<Notification>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Predicate toPredicate(Root<Notification> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (getId() != null) {
                    predicates.add(cb.equal(root.get("id"), getId()));
                }

                if (StringUtils.isNotBlank(getMessage())) {
                    predicates.add(cb.like(cb.lower(root.get("message")), "%" + getMessage().toLowerCase() + "%"));
                }

                if (getState() != null) {
                    predicates.add(cb.equal(root.get("state"), getState()));
                }

                if (getNotificationGroupId() != null) {
                    Join<Notification, NotificationGroup> notificationGroupJoin = root.join("notificationGroup", JoinType.INNER);
                    predicates.add(cb.equal(notificationGroupJoin.get("id"), getNotificationGroupId()));
                }

                if (getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), getStatus()));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        };
    }
}
