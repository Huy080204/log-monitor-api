package logs.api.model.criteria;

import io.swagger.v3.oas.annotations.media.Schema;
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

import logs.api.model.NotificationChannel;

@Data
public class NotificationChannelCriteria implements Serializable {

    private Long id;
    private Integer status;
    private String name;
    private Integer type;

    @Schema(hidden = true)
    public Specification<NotificationChannel> getCriteria() {
        return new Specification<NotificationChannel>() {
            private static final long serialVersionUID = 1L;
            @Override
            public Predicate toPredicate(Root<NotificationChannel> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (getId() != null) {
                    predicates.add(cb.equal(root.get("id"), getId()));
                }

                if (getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), getStatus()));
                }

                if (!StringUtils.isEmpty(getName())) {
                    predicates.add(cb.like(cb.lower(root.get("name")), "%" + getName().toLowerCase() + "%"));
                }

                if (getType() != null) {
                    predicates.add(cb.equal(root.get("type"), getType()));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        };
    }
}
