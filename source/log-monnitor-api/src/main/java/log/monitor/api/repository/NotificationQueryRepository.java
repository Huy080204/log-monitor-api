package log.monitor.api.repository;

import log.monitor.api.model.NotificationQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationQueryRepository extends JpaRepository<NotificationQuery, Long>, JpaSpecificationExecutor<NotificationQuery> {

    @Query(value = "SELECT COUNT(*) > 0 FROM db_notification_query WHERE BINARY query = :query AND notification_group_id = :notificationGroupId", nativeQuery = true)
    boolean existsByQueryAndNotificationGroupIdCaseSensitive(@Param("query") String query, @Param("notificationGroupId") Long notificationGroupId);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationQuery nq WHERE nq.notificationGroup.id = :notificationGroupId")
    void deleteAllByNotificationGroupId(@Param("notificationGroupId") Long notificationGroupId);
}
