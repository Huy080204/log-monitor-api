package logs.api.model.criteria;


import logs.api.model.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class AccountCriteria implements Serializable{
    private static final long serialVersionUID = 1L;
    private Long id;
    private String username;
    private Integer status;
    private String email;
    private String fullName;
    private String phone;
    private Integer kind;
    private Boolean isSuperAdmin;
    private Long requiredId;

    @Schema(hidden = true)
    public Specification<Account> getSpecification() {
        return new Specification<Account>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Predicate toPredicate(Root<Account> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();

                if(getId() != null){
                    predicates.add(cb.equal(root.get("id"), getId()));
                }
                if(getStatus() != null){
                    predicates.add(cb.equal(root.get("status"), getStatus()));
                }
                if(!StringUtils.isEmpty(getUsername())){
                    predicates.add(cb.like(cb.lower(root.get("username")), "%"+getUsername().toLowerCase()+"%"));
                }
                if(!StringUtils.isEmpty(getEmail())){
                    predicates.add(cb.like(cb.lower(root.get("email")), "%"+getEmail().toLowerCase()+"%"));
                }
                if(!StringUtils.isEmpty(getFullName())){
                    predicates.add(cb.like(cb.lower(root.get("fullName")), "%"+getFullName().toLowerCase()+"%"));
                }
                if (!StringUtils.isEmpty(getPhone())) {
                    predicates.add(cb.like(cb.lower(root.get("phone")), "%" + getPhone().toLowerCase() + "%"));
                }
                if(getKind() != null){
                    predicates.add(cb.equal(root.get("kind"), getKind()));
                }
                if(getIsSuperAdmin() != null){
                    predicates.add(cb.equal(root.get("isSuperAdmin"), getIsSuperAdmin()));
                }

                Predicate filters = cb.and(predicates.toArray(new Predicate[0]));
                if (getRequiredId() != null) {
                    Predicate required = cb.equal(root.get("id"), getRequiredId());
                    query.orderBy(
                            cb.desc(cb.selectCase()
                                    .when(required, 1)
                                    .otherwise(0)
                            )
                    );
                    return cb.or(filters, required);
                } else {
                    return filters;
                }
            }
        };
    }
}
