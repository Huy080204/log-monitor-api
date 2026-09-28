package logs.api.config.quartz;

import logs.api.scheduler.PendingNotificationDispatchJob;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;

@Configuration
public class QuartzConfig {

    @Autowired
    private AutowiringSpringBeanJobFactory jobFactory;

    @Value("${notification.dispatch.interval-seconds}")
    private int notificationDispatchIntervalSeconds;

    @Bean
    public SchedulerFactoryBeanCustomizer jobFactoryCustomizer() {
        return schedulerFactoryBean -> schedulerFactoryBean.setJobFactory(jobFactory);
    }

    @Bean
    public JobDetailFactoryBean pendingNotificationDispatchJobDetail() {
        JobDetailFactoryBean jobDetailFactoryBean = new JobDetailFactoryBean();
        jobDetailFactoryBean.setJobClass(PendingNotificationDispatchJob.class);
        jobDetailFactoryBean.setDurability(true);
        return jobDetailFactoryBean;
    }

    @Bean
    public Trigger pendingNotificationDispatchTrigger(JobDetail pendingNotificationDispatchJobDetail) {
        return TriggerBuilder.newTrigger()
                .withIdentity("pending-notification-dispatch-trigger", "system")
                .forJob(pendingNotificationDispatchJobDetail)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(notificationDispatchIntervalSeconds)
                        .repeatForever())
                .build();
    }
}
