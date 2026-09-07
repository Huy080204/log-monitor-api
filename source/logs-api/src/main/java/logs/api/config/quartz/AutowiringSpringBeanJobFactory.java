package logs.api.config.quartz;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.stereotype.Component;

// Config create new instance Job when trigger fire
@Component
public class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
        Object job = super.createJobInstance(bundle); // Quartz automatically creates a new instance using
        AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
        beanFactory.autowireBean(job); // force Spring to inject @Autowired into that instance
        return job;
    }
}
