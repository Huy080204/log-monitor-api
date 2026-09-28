package logs.api.repository;

import logs.api.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long>, JpaSpecificationExecutor<NotificationLog> {

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationLog n WHERE n.createdDate < :date")
    void deleteByCreatedDateBefore(@Param("date") Date date);
}
