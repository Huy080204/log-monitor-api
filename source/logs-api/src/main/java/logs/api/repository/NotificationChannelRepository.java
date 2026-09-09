package logs.api.repository;

import logs.api.model.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, Long>, JpaSpecificationExecutor<NotificationChannel> {
    boolean existsByName(String name);
}
