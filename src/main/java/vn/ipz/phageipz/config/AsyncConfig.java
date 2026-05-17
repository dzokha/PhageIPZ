package vn.ipz.phageipz.config;

// import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor; // Thêm dòng này
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import vn.ipz.phageipz.config.PhageIPZProperties;

@Configuration
@EnableAsync
public class AsyncConfig {

    private final PhageIPZProperties phageipzProperties;
    public AsyncConfig(PhageIPZProperties phageipzProperties) {
        this.phageipzProperties = phageipzProperties;
    }

    @Bean(name = "analysisTaskExecutor")
    public Executor analysisTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Tối ưu số luồng dựa trên CPU hoặc cấu hình
        int coreThreads = phageipzProperties.getThreadCount();
        if (coreThreads <= 0) {
            coreThreads = Runtime.getRuntime().availableProcessors(); // Dùng số nhân CPU thực tế
            System.out.println("Using default thread count: " + coreThreads); // Gộp lại cho gọn và đúng chuẩn
        }
        executor.setCorePoolSize(coreThreads);
        executor.setMaxPoolSize(coreThreads * 2);
        executor.setQueueCapacity(500); // Tăng lên một chút nếu file nhiều
        executor.setThreadNamePrefix("SOAP-Analysis-");

        // CHẾ ĐỘ QUAN TRỌNG:
        // 1. Giúp hệ thống không bị sập khi hàng đợi đầy
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 2. Đảm bảo các phân tích đang chạy được hoàn tất khi tắt Server
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60); // Đợi tối đa 60s

        executor.initialize();
        return executor;
    }
}