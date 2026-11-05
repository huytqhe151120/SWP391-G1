package com.swp391.g1.listener;

import com.swp391.g1.service.IExtracurricularActivityService;
import com.swp391.g1.service.IRegistrationFormService;
import com.swp391.g1.service.impl.ExtracurricularActivityServiceImpl;
import com.swp391.g1.service.impl.RegistrationFormServiceImpl;

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
        IRegistrationFormService registrationFormService =
                new RegistrationFormServiceImpl();

        // Tạo một luồng riêng để chạy scheduler.
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(
                    runnable, "activity-registration-status-scheduler");

            thread.setDaemon(true);
            return thread;
        });

        // Synchronize both activity and registration-form time windows.
        Runnable syncTask = () -> {
            try {
                activityService.synchronizeActivityStatuses();
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Could not synchronize extracurricular activity statuses.",
                        e);
            }
            try {
                registrationFormService.synchronizeFormStatuses();
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Could not synchronize registration form statuses.",
                        e);
            }
        };

        // Reconcile transitions missed while the application was stopped.
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