package com.aispring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import com.aispring.service.CloudDiskService;

/**
 * AI智能学习助手 - Spring Boot应用主类
 * 
 * @author AI Spring Team
 * @version 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"com.aispring", "com.example.aispring"})
@EnableJpaAuditing
@EnableAsync
@EnableConfigurationProperties
public class AiTutorApplication {

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
}
