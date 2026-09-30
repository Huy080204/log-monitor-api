package logs.api.scheduler;

import logs.api.repository.NotificationLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@DisallowConcurrentExecution
@Slf4j
public class NotificationLogCleanupJob implements Job {

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Value("${notification.log.cleanup.retention-days}")
    private int retentionDays;

    @Override
    public void execute(JobExecutionContext context) {
        Date threshold = Date.from(Instant.now().minus(retentionDays, ChronoUnit.DAYS));
        notificationLogRepository.deleteByCreatedDateBefore(threshold);
        log.info("Deleted notification logs older than {} days (before {})", retentionDays, threshold);
    }
}
