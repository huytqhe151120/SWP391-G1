package com.swp391.g1.listener;

import com.swp391.g1.service.INotificationService;
import com.swp391.g1.service.impl.NotificationServiceImpl;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebListener
public class ChecklistOverdueAlertScheduler implements ServletContextListener {

    private static final Logger LOGGER =
            Logger.getLogger(ChecklistOverdueAlertScheduler.class.getName());

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        INotificationService notificationService = new NotificationServiceImpl();
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "checklist-overdue-alert-scheduler");
            thread.setDaemon(true);
            return thread;
        });

        Runnable alertTask = () -> {
            try {
                int sent = notificationService.notifyOverdueChecklistTasks();
                if (sent > 0) {
                    LOGGER.info("Created overdue checklist alerts for " + sent + " tasks.");
                }
            } catch (RuntimeException e) {
                LOGGER.log(Level.SEVERE, "Could not create overdue checklist alerts.", e);
            }
        };

        scheduler.execute(alertTask);
        scheduler.scheduleWithFixedDelay(alertTask, 1, 1, TimeUnit.HOURS);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
