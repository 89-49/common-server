package org.pgsg.config.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
@EnableJpaAuditing
@ConditionalOnProperty(name = "spring.datasource.url")
@EnableJpaRepositories(basePackages = "org.pgsg")
@EntityScan(basePackages = "org.pgsg")
public class JPAConfig {

	@PersistenceContext
	private EntityManager em;

	@Bean
	@ConditionalOnMissingBean(JPAQueryFactory.class)
	public JPAQueryFactory jpaQueryFactory() {
		return new JPAQueryFactory(em);
	}
}