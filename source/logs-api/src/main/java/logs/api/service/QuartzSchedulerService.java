package logs.api.service;

import logs.api.model.NotificationGroup;
import logs.api.scheduler.VictoriaLogsErrorAlertJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QuartzSchedulerService {
    private static final String JOB_GROUP = "victoria-logs-alert";

    @Autowired
    private Scheduler scheduler;

    public void scheduleGroup(NotificationGroup group) {
        try {
            upsertJobAndTrigger(group);
        } catch (SchedulerException e) {
            log.error("Error scheduling notification group {}: {}", group.getId(), e.getMessage(), e);
            throw new RuntimeException("Error scheduling notification group", e);
        }
    }

    public void rescheduleGroup(NotificationGroup group) {
        try {
            upsertJobAndTrigger(group);
        } catch (SchedulerException e) {
            log.error("Error rescheduling notification group {}: {}", group.getId(), e.getMessage(), e);
            throw new RuntimeException("Error rescheduling notification group", e);
        }
    }

    public void unscheduleGroup(Long groupId) {
        TriggerKey triggerKey = buildTriggerKey(groupId);
        try {
            scheduler.unscheduleJob(triggerKey);
        } catch (SchedulerException e) {
            log.error("Error unscheduling notification group {}: {}", groupId, e.getMessage(), e);
            throw new RuntimeException("Error unscheduling notification group", e);
        }
    }

    private void upsertJobAndTrigger(NotificationGroup group) throws SchedulerException {
        JobDetail jobDetail = buildJobDetail(group);
        scheduler.addJob(jobDetail, true);

        TriggerKey triggerKey = buildTriggerKey(group.getId());
        CronTrigger trigger = buildTrigger(group, triggerKey);
        if (scheduler.checkExists(triggerKey)) {
            scheduler.rescheduleJob(triggerKey, trigger);
        } else {
            scheduler.scheduleJob(trigger);
        }
    }

    private JobDetail buildJobDetail(NotificationGroup group) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("groupId", group.getId());
        return JobBuilder.newJob(VictoriaLogsErrorAlertJob.class)
                .withIdentity(buildJobKey(group.getId()))
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private CronTrigger buildTrigger(NotificationGroup group, TriggerKey triggerKey) {
        return TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .forJob(buildJobKey(group.getId()))
                .withSchedule(CronScheduleBuilder.cronSchedule(group.getCronExpression()))
                .build();
    }

    private JobKey buildJobKey(Long groupId) {
        return new JobKey("notification-group-" + groupId, JOB_GROUP);
    }

    private TriggerKey buildTriggerKey(Long groupId) {
        return new TriggerKey("notification-group-" + groupId, JOB_GROUP);
    }
}
