package bd.share.journal.autoconfig;

import bd.share.journal.fliter.JournalFilter;
import bd.share.journal.log.JournalWapper;
import bd.share.journal.log.LogEmitter;
import bd.share.journal.util.DicHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
@EnableConfigurationProperties({JournalProperty.class})
public class JournalAutoConfig {

    private JournalProperty property;

    public JournalAutoConfig(JournalProperty property) {
        this.property = property;
        if (property.getEnabled() == null){
            property.setEnabled(true);
        }
        if (property.getLogAnyway() == null){
            property.setLogAnyway(false);
        }
    }

    @Bean
    @ConditionalOnBean(RequestMappingHandlerMapping.class)
    public DicHolder holder(RequestMappingHandlerMapping mapping){
        return new DicHolder(mapping);
    }

    @Bean
    @ConditionalOnBean(DicHolder.class)
    @ConditionalOnMissingBean(JournalWapper.class)
    public JournalWapper journalWapper(DicHolder dicHolder){
        return new LogEmitter(dicHolder,property.getLogAnyway());
    }

    @Bean
    @ConditionalOnBean({JournalWapper.class,DicHolder.class})
    @Order(Integer.MIN_VALUE)
    public JournalFilter loggingFilter(JournalWapper wapper,DicHolder dicHolder){
        return new JournalFilter(dicHolder,property.getEnabled(),wapper);
    }

}
