package logs.api.repository;

import logs.api.model.Applications;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApplicationsRepository extends JpaRepository<Applications, Long>, JpaSpecificationExecutor<Applications> {
    boolean existsByName(String name);
}
