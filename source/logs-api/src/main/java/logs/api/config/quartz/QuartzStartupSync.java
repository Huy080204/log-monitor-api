package logs.api.config.quartz;

import logs.api.constant.BaseConstant;
import logs.api.model.NotificationGroup;
import logs.api.repository.NotificationGroupRepository;
import logs.api.service.QuartzSchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//config when start app, and reconciled periodically in case a group drifts out of sync while the app is running
@Component
@Slf4j
public class QuartzStartupSync implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private NotificationGroupRepository notificationGroupRepository;

    @Autowired
    private QuartzSchedulerService quartzSchedulerService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("QuartzStartupSync begin — syncing active notification groups");
        syncActiveGroups();
        log.info("QuartzStartupSync finished");
    }

    @Scheduled(fixedDelay = 1200000, zone = "UTC")
    public void reconcileActiveGroups() {
        log.info("QuartzStartupSync reconciler begin — periodic re-sync of active notification groups");
        syncActiveGroups();
        log.info("QuartzStartupSync reconciler finished");
    }

    private void syncActiveGroups() {
        for (NotificationGroup group : notificationGroupRepository.findAllByStatus(BaseConstant.STATUS_ACTIVE)) {
            try {
                quartzSchedulerService.scheduleGroup(group);
            } catch (RuntimeException e) {
                log.error("Failed to schedule notification group {} on sync, skipping: {}",
                        group.getId(), e.getMessage(), e);
            }
        }
    }
}
