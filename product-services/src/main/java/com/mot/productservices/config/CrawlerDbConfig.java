package com.mot.productservices.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.mot.productservices.crawler.repository",
        entityManagerFactoryRef = "crawlerEntityManagerFactory",
        transactionManagerRef = "crawlerTransactionManager"
)
public class CrawlerDbConfig {

    @Bean(name = "crawlerDataSource")
    @ConfigurationProperties(prefix = "crawler.datasource")
    public DataSource crawlerDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "crawlerEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean crawlerEntityManagerFactory(
            @Qualifier("crawlerDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.mot.productservices.crawler.entity");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.hbm2ddl.auto", "none"); // Read-only, don't modify crawler DB
        properties.put("hibernate.show_sql", "false");
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean(name = "crawlerTransactionManager")
    public PlatformTransactionManager crawlerTransactionManager(
            @Qualifier("crawlerEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }
}
