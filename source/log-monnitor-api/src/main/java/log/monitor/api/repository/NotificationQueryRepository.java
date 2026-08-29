package log.monitor.api.repository;

import log.monitor.api.model.NotificationQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationQueryRepository extends JpaRepository<NotificationQuery, Long>, JpaSpecificationExecutor<NotificationQuery> {

    boolean existsByQueryAndNotificationGroupId(String query, Long notificationGroupId);

    boolean existsByNameAndNotificationGroupId(String name, Long notificationGroupId);

    List<NotificationQuery> findAllByNotificationGroupIdAndStatus(Long notificationGroupId, Integer status);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationQuery nq WHERE nq.notificationGroup.id = :notificationGroupId")
    void deleteAllByNotificationGroupId(@Param("notificationGroupId") Long notificationGroupId);
}
