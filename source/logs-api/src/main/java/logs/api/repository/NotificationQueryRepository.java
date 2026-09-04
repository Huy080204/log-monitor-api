package logs.api.repository;

import logs.api.model.NotificationQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface NotificationQueryRepository extends JpaRepository<NotificationQuery, Long>, JpaSpecificationExecutor<NotificationQuery> {

    List<NotificationQuery> findAllByNotificationGroupIdAndStatus(Long notificationGroupId, Integer status);

    List<NotificationQuery> findAllByNotificationGroupId(Long notificationGroupId);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationQuery nq WHERE nq.notificationGroup.id = :notificationGroupId")
    void deleteAllByNotificationGroupId(@Param("notificationGroupId") Long notificationGroupId);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationQuery nq WHERE nq.notificationGroup.id = :notificationGroupId AND nq.queryTemplate.id NOT IN :queryTemplateIds")
    void deleteAllByNotificationGroupIdAndQueryTemplateIdNotIn(@Param("notificationGroupId") Long notificationGroupId, @Param("queryTemplateIds") Collection<Long> queryTemplateIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationQuery nq WHERE nq.queryTemplate.id = :queryTemplateId")
    void deleteAllByQueryTemplateId(@Param("queryTemplateId") Long queryTemplateId);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationQuery nq WHERE nq.queryTemplate.id IN (SELECT qt.id FROM QueryTemplate qt WHERE qt.application.id = :applicationId)")
    void deleteAllByQueryTemplateApplicationId(@Param("applicationId") Long applicationId);
}
