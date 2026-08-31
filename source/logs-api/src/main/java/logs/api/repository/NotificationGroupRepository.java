package logs.api.repository;

import logs.api.model.NotificationGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface NotificationGroupRepository extends JpaRepository<NotificationGroup, Long>, JpaSpecificationExecutor<NotificationGroup> {
    boolean existsByName(String name);
    Optional<NotificationGroup> findFirstByStatus(Integer status);
}
