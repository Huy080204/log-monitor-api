package logs.api.config.quartz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Autowired
    private AutowiringSpringBeanJobFactory jobFactory;

    @Bean
    public SchedulerFactoryBeanCustomizer jobFactoryCustomizer() {
        return schedulerFactoryBean -> schedulerFactoryBean.setJobFactory(jobFactory);
    }
}
