package bd.share.autoconfig;

import bd.share.fliter.JournalFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
@EnableConfigurationProperties({JournalProperty.class})
public class JournalAutoConfig {

    @Bean
    @ConditionalOnBean(RequestMappingHandlerMapping.class)
    @Order(Integer.MIN_VALUE)
    public JournalFilter loggingFilter(RequestMappingHandlerMapping mapping){
        return new JournalFilter(mapping);
    }

}
