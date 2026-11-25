package com.swp391.g1.controller;

import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.util.AuthContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet xử lý trang giao diện danh sách thông báo của người dùng.
 */
@WebServlet("/notifications")
public class NotificationServlet extends HttpServlet {

    /**
     * Xử lý yêu cầu GET tới đường dẫn {@code /notifications}: kiểm tra quyền xem thông báo
     * của người dùng hiện tại và chuyển tiếp tới trang JSP hiển thị danh sách thông báo.
     *
     * @param req  đối tượng HTTP request từ trình duyệt
     * @param resp đối tượng HTTP response trả về cho trình duyệt
     * @throws ServletException nếu quá trình chuyển tiếp tới JSP gặp lỗi
     * @throws IOException      nếu xảy ra lỗi I/O khi gửi phản hồi hoặc chuyển tiếp
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Kiểm tra người dùng hiện tại có được phép xem thông báo hay không (yêu cầu đã đăng nhập)
        if (!AccessPolicy.canViewNotifications(AuthContext.getCurrentUser(req))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn cần đăng nhập để xem thông báo.");
            return;
        }
        // Chuyển tiếp yêu cầu tới giao diện JSP hiển thị trung tâm thông báo
        req.getRequestDispatcher("/WEB-INF/views/notification/notifications.jsp").forward(req, resp);
    }
}