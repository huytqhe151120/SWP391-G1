package com.swp391.g1.util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.TimeZone;

/**
 * Listener khởi tạo cùng ứng dụng web nhằm thiết lập múi giờ mặc định của JVM về GMT+7 ({@code Asia/Ho_Chi_Minh}),
 * giúp các phép chuyển đổi {@code java.sql.Timestamp} qua JDBC hoạt động nhất quán theo giờ Việt Nam.
 */
@WebListener
public class TimeZoneListener implements ServletContextListener {

    /**
     * Được Servlet container tự động gọi khi ứng dụng khởi động để gán múi giờ mặc định của JVM.
     *
     * @param sce Sự kiện vòng đời {@link ServletContextEvent} khi context được khởi tạo
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        TimeZone.setDefault(TimeZone.getTimeZone(DateTimeUtil.ZONE));
    }
}
