package logs.api.repository;

import logs.api.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    Notification findFirstByNotificationGroupType(Integer type);

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.notificationGroup.id = :notificationGroupId")
    void deleteAllByNotificationGroupId(@Param("notificationGroupId") Long notificationGroupId);
}
