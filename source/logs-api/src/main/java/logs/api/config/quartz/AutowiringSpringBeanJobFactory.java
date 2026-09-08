package logs.api.config.quartz;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.stereotype.Component;

// Config create new instance Job when trigger fire
@Component
public class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
        return super.createJobInstance(bundle);
    }
}
