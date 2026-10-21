package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.NotificationDTO;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.INotificationService;
import com.swp391.g1.service.impl.NotificationServiceImpl;
import com.swp391.g1.util.AuthContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * JSON API: GET ?action=list|count, POST action=read(id)|readAll.
 * list: filter=all|unread|read, category=all|activities|qa|appeals|results|messages, page, size.
 */
@WebServlet("/api/notifications")
public class NotificationApiServlet extends HttpServlet {

    private final INotificationService service;

    public NotificationApiServlet() {
        this.service = new NotificationServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CurrentUser user = authorize(req, resp);
        if (user == null) return;
        try {
            if ("count".equals(req.getParameter("action"))) {
                json(resp, 200, "{\"unread\":" + service.countUnread(user.getId()) + "}");
                return;
            }
            String filter = normalizeFilter(req.getParameter("filter"));
            String category = req.getParameter("category");
            int size = clamp(intParam(req, "size", 10), 1, 50);
            int total = service.countNotifications(user.getId(), filter, category);
            int pages = Math.max(1, (total + size - 1) / size);
            int page = clamp(intParam(req, "page", 1), 1, pages);
            List<NotificationDTO> items = service.getNotifications(user.getId(), filter, category, page, size);

            StringBuilder sb = new StringBuilder("{\"unread\":").append(service.countUnread(user.getId()))
                    .append(",\"total\":").append(total).append(",\"page\":").append(page)
                    .append(",\"pages\":").append(pages).append(",\"items\":[");
            for (int i = 0; i < items.size(); i++) {
                NotificationDTO n = items.get(i);
                if (i > 0) sb.append(',');
                sb.append("{\"id\":").append(n.getId())
                        .append(",\"title\":").append(quote(n.getTitle()))
                        .append(",\"content\":").append(quote(n.getContent()))
                        .append(",\"link\":").append(quote(n.getLink()))
                        .append(",\"createdAt\":").append(quote(n.getCreatedAt().toString()))
                        .append(",\"read\":").append(n.isRead()).append('}');
            }
            json(resp, 200, sb.append("]}").toString());
        } catch (IllegalStateException e) {
            json(resp, 500, "{\"error\":\"Lỗi hệ thống.\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CurrentUser user = authorize(req, resp);
        if (user == null) return;
        try {
            String action = req.getParameter("action");
            if ("readAll".equals(action)) {
                json(resp, 200, "{\"updated\":" + service.markAllRead(user.getId()) + "}");
            } else if ("read".equals(action)) {
                int id = Integer.parseInt(req.getParameter("id"));
                json(resp, 200, "{\"updated\":" + (service.markRead(user.getId(), id) ? 1 : 0) + "}");
            } else {
                json(resp, 400, "{\"error\":\"Hành động không hợp lệ.\"}");
            }
        } catch (NumberFormatException e) {
            json(resp, 400, "{\"error\":\"ID không hợp lệ.\"}");
        } catch (IllegalStateException e) {
            json(resp, 500, "{\"error\":\"Lỗi hệ thống.\"}");
        }
    }

    private CurrentUser authorize(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CurrentUser user = AuthContext.getCurrentUser(req);
        if (!AccessPolicy.canViewNotifications(user)) {
            json(resp, 403, "{\"error\":\"Cần đăng nhập.\"}");
            return null;
        }
        return user;
    }

    private static String normalizeFilter(String f) {
        return "unread".equals(f) || "read".equals(f) ? f : "all";
    }

    private static int intParam(HttpServletRequest req, String name, int def) {
        try {
            return Integer.parseInt(req.getParameter(name));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String quote(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(ch < 0x20 ? String.format("\\u%04x", (int) ch) : String.valueOf(ch));
            }
        }
        return sb.append('"').toString();
    }

    private static void json(HttpServletResponse resp, int status, String body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(body);
    }
}