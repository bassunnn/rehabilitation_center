package ru.mirea.rehab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Точка входа серверного приложения Информационной системы Реабилитационного центра.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class RehabilitationCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(RehabilitationCenterApplication.class, args);
    }
}

