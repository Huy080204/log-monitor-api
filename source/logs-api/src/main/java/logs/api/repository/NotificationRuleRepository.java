package logs.api.repository;

import logs.api.model.NotificationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationRuleRepository extends JpaRepository<NotificationRule, Long>, JpaSpecificationExecutor<NotificationRule> {

    boolean existsByNotificationGroupIdAndName(Long notificationGroupId, String name);

    boolean existsByNotificationGroupIdAndNameAndIdNot(Long notificationGroupId, String name, Long id);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationRule nr WHERE nr.notificationGroup.id = :notificationGroupId")
    void deleteAllByNotificationGroupId(@Param("notificationGroupId") Long notificationGroupId);
}
