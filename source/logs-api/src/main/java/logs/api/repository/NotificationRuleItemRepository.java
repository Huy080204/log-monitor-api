package logs.api.repository;

import logs.api.model.NotificationRuleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface NotificationRuleItemRepository extends JpaRepository<NotificationRuleItem, Long> {

    List<NotificationRuleItem> findAllByNotificationRuleIdOrderByOrdering(Long notificationRuleId);

    @Query("SELECT i FROM NotificationRuleItem i "
            + "JOIN FETCH i.notificationRule nr "
            + "JOIN FETCH i.application "
            + "JOIN FETCH i.queryTemplate qt "
            + "JOIN FETCH qt.application "
            + "WHERE nr.id IN :notificationRuleIds "
            + "ORDER BY nr.id ASC, i.ordering ASC")
    List<NotificationRuleItem> findAllByNotificationRuleIdInFetchingRefs(@Param("notificationRuleIds") Collection<Long> notificationRuleIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationRuleItem nri WHERE nri.notificationRule.id = :notificationRuleId")
    void deleteAllByNotificationRuleId(@Param("notificationRuleId") Long notificationRuleId);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationRuleItem nri WHERE nri.queryTemplate.id = :queryTemplateId")
    void deleteAllByQueryTemplateId(@Param("queryTemplateId") Long queryTemplateId);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationRuleItem nri WHERE nri.application.id = :applicationId")
    void deleteAllByApplicationId(@Param("applicationId") Long applicationId);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationRuleItem nri WHERE nri.queryTemplate.id IN " +
           "(SELECT qt.id FROM QueryTemplate qt WHERE qt.application.id = :applicationId)")
    void deleteAllByQueryTemplateApplicationId(@Param("applicationId") Long applicationId);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationRuleItem nri WHERE nri.notificationRule.id IN " +
           "(SELECT nr.id FROM NotificationRule nr WHERE nr.notificationGroup.id = :notificationGroupId)")
    void deleteAllByNotificationRuleNotificationGroupId(@Param("notificationGroupId") Long notificationGroupId);
}
