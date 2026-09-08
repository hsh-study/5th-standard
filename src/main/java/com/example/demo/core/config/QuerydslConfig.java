package com.example.demo.core.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JPA의 EntityManager를 이용하는 QueryDSL 쿼리 팩토리를 Spring Bean으로 등록.
 * Q 타입은 이 설정이 생성하지 않는다. Gradle의 annotationProcessor가 컴파일할 때 생성.
 */
@Configuration
public class QuerydslConfig {

    // JPA 영속성 컨텍스트를 통한 JPA 표준 주입 방식
//     @PersistenceContext
//     private EntityManager entityManager;

    // Bean 을 통한 매개변수 주입
    // Repository는 주입받은 factory 로 쿼리를 구성하고, JPA를 통해 SQL을 실행.
    @Bean
    JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }

}
