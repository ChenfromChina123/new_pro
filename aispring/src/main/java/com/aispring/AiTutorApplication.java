package com.aispring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.scheduling.annotation.EnableAsync;
import com.aispring.service.CloudDiskService;
import com.aispring.service.UserService;
import com.aispring.entity.Admin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI智能学习助手 - Spring Boot版本启动成功！
 */
@SpringBootApplication(scanBasePackages = {"com.aispring", "com.example.aispring"})
@EnableJpaAuditing
@EnableAsync
@EnableConfigurationProperties
public class AiTutorApplication {
    private static final Logger log = LoggerFactory.getLogger(AiTutorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AiTutorApplication.class, args);
        System.out.println("""
            
            ╔═══════════════════════════════════════════════════════════╗
            ║                                                           ║
            ║   🤖 AI智能学习助手系统 - Spring Boot版本启动成功！            ║
            ║                                                           ║
            ║   📚 API文档: http://localhost:5000/swagger-ui.html        ║
            ║   💻 管理后台: http://localhost:5000/admin                 ║
            ║                                                           ║
            ╚═══════════════════════════════════════════════════════════╝
            
        """);
    }

    @Bean
    @ConditionalOnProperty(name = "app.cloud-disk.migrate-on-startup", havingValue = "true")
    public CommandLineRunner cloudDiskMigrationRunner(CloudDiskService cloudDiskService) {
        return args -> cloudDiskService.migrateToUnifiedBase();
    }

    @Bean
    public CommandLineRunner adminSetupRunner(UserService userService) {
        return args -> {
            String adminEmail = "3301767269@qq.com";
            try {
                Admin admin = userService.setAsAdmin(adminEmail);
                log.info("管理员设置成功: {} (ID: {})", adminEmail, admin.getId());
            } catch (Exception e) {
                log.warn("设置管理员失败 (可能用户尚未注册): {}", e.getMessage());
            }
        };
    }

    @Bean
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
