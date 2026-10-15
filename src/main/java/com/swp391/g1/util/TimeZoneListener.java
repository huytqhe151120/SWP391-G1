package com.swp391.g1.util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.TimeZone;

/** Makes JDBC Timestamp conversions use GMT+7. */
@WebListener
public class TimeZoneListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        TimeZone.setDefault(TimeZone.getTimeZone(DateTimeUtil.ZONE));
    }
}
