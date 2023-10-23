package com.carupdateprovider.process.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * JPA related configuration.
 * Only active in the cup-history and ext-request-proxy service.
 */
@ConditionalOnExpression("${application.history} or ${application.ext-request-proxy}")
@Configuration()
@Import(
        {
                DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class
        }
)
public class JPAConfig {

}
