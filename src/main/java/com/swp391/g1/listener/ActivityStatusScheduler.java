package com.swp391.g1.listener;

import com.swp391.g1.service.IExtracurricularActivityService;
import com.swp391.g1.service.impl.ExtracurricularActivityServiceImpl;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebListener
public class ActivityStatusScheduler
        implements ServletContextListener {

    private static final Logger LOGGER =
            Logger.getLogger(ActivityStatusScheduler.class.getName());

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {

        // Khởi tạo service để xử lý nghiệp vụ.
        IExtracurricularActivityService activityService =
                new ExtracurricularActivityServiceImpl();

        // Tạo một luồng riêng để chạy scheduler.
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(
                    runnable, "activity-status-scheduler");

            thread.setDaemon(true);
            return thread;
        });

        // Công việc đồng bộ trạng thái.
        Runnable syncTask = () -> {
            try {
                activityService.synchronizeActivityStatuses();

            } catch (Exception e) {
                // Ghi log nếu có lỗi, không làm scheduler ngừng hoạt động.
                LOGGER.log(
                        Level.SEVERE,
                        "Could not synchronize extracurricular activity statuses.",
                        e);
            }
        };

        // Chạy ngay khi ứng dụng khởi động.
        // Mục đích: xử lý các hoạt động bị bỏ lỡ khi Tomcat tắt.
        scheduler.execute(syncTask);

        // Sau đó tiếp tục kiểm tra định kỳ mỗi phút.
        scheduler.scheduleWithFixedDelay(
                syncTask,
                1,
                1,
                TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {

        // Dừng scheduler khi ứng dụng bị tắt hoặc undeploy.
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}