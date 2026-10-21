package com.swp391.g1.controller;

import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.util.AuthContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/notifications")
public class NotificationServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!AccessPolicy.canViewNotifications(AuthContext.getCurrentUser(req))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn cần đăng nhập để xem thông báo.");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/notification/notifications.jsp").forward(req, resp);
    }
}