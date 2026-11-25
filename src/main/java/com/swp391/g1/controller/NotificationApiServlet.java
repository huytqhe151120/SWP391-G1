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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

/**
 * Servlet cung cấp JSON API cho module thông báo:
 * <ul>
 *   <li>GET {@code ?action=count}: đếm số thông báo chưa đọc của người dùng hiện tại.</li>
 *   <li>GET {@code ?action=list} (mặc định): lấy danh sách thông báo có lọc ({@code filter=all|unread|read})
 *       và phân trang ({@code page}, {@code size}).</li>
 *   <li>POST {@code action=read&id=...}: đánh dấu một thông báo là đã đọc.</li>
 *   <li>POST {@code action=readAll}: đánh dấu toàn bộ thông báo của người dùng là đã đọc.</li>
 * </ul>
 */
@WebServlet("/api/notifications")
public class NotificationApiServlet extends HttpServlet {

    private static final String CSRF_SESSION_KEY = NotificationApiServlet.class.getName() + ".csrfToken";
    private final INotificationService service;

    /**
     * Khởi tạo {@code NotificationApiServlet} với implementation mặc định của {@link INotificationService}.
     */
    public NotificationApiServlet() {
        this.service = new NotificationServiceImpl();
    }

    /**
     * Xử lý yêu cầu GET cho API thông báo: xác thực người dùng, cấp phát CSRF token qua header,
     * sau đó trả về số lượng chưa đọc hoặc danh sách thông báo phân trang dạng JSON.
     *
     * @param req  HTTP request chứa tham số {@code action}, {@code filter}, {@code page}, {@code size}
     * @param resp HTTP response trả về chuỗi JSON
     * @throws IOException nếu xảy ra lỗi ghi phản hồi JSON
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CurrentUser user = authorize(req, resp);
        // Dừng xử lý nếu người dùng chưa đăng nhập hoặc không đủ quyền (authorize đã ghi lỗi 403)
        if (user == null) return;
        // Gắn CSRF token hiện tại vào response header để client dùng cho các yêu cầu POST tiếp theo
        resp.setHeader("X-CSRF-Token", csrfToken(req));
        try {
            // Nhánh chỉ lấy tổng số thông báo chưa đọc (thường dùng cho badge chuông thông báo)
            if ("count".equals(req.getParameter("action"))) {
                json(resp, 200, "{\"unread\":" + service.countUnread(user.getId()) + "}");
                return;
            }
            // Chuẩn hóa bộ lọc và tính toán tham số phân trang hợp lệ trong giới hạn cho phép
            String filter = normalizeFilter(req.getParameter("filter"));
            int size = clamp(intParam(req, "size", 10), 1, 50);
            int total = service.countNotifications(user.getId(), filter);
            int pages = Math.max(1, (total + size - 1) / size);
            int page = clamp(intParam(req, "page", 1), 1, pages);
            List<NotificationDTO> items = service.getNotifications(user.getId(), filter, page, size);

            // Dựng thủ công chuỗi JSON phản hồi gồm metadata phân trang và mảng danh sách thông báo
            StringBuilder sb = new StringBuilder("{\"unread\":").append(service.countUnread(user.getId()))
                    .append(",\"total\":").append(total).append(",\"page\":").append(page)
                    .append(",\"pages\":").append(pages).append(",\"items\":[");
            // Duyệt từng thông báo trong trang hiện tại để serialize sang đối tượng JSON
            for (int i = 0; i < items.size(); i++) {
                NotificationDTO n = items.get(i);
                // Thêm dấu phẩy ngăn cách giữa các phần tử JSON từ phần tử thứ hai trở đi
                if (i > 0) sb.append(',');
                sb.append("{\"id\":").append(n.getId())
                        .append(",\"title\":").append(quote(n.getTitle()))
                        .append(",\"content\":").append(quote(n.getContent()))
                        .append(",\"eventType\":").append(quote(n.getEventType()))
                        .append(",\"link\":").append(quote(n.getLink()))
                        .append(",\"createdAt\":").append(quote(n.getCreatedAt().toString()))
                        .append(",\"read\":").append(n.isRead()).append('}');
            }
            json(resp, 200, sb.append("]}").toString());
        } catch (IllegalStateException e) {
            // Xử lý lỗi tầng dịch vụ/CSDL khi tải hoặc đếm thông báo
            json(resp, 500, "{\"error\":\"Lỗi hệ thống.\"}");
        }
    }

    /**
     * Xử lý yêu cầu POST để cập nhật trạng thái đã đọc của một hoặc toàn bộ thông báo,
     * có kiểm tra xác thực người dùng và xác minh CSRF token.
     *
     * @param req  HTTP request chứa tham số {@code action} ({@code read} hoặc {@code readAll}) và {@code id}
     * @param resp HTTP response trả về số bản ghi đã cập nhật hoặc thông báo lỗi dạng JSON
     * @throws IOException nếu xảy ra lỗi ghi phản hồi JSON
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CurrentUser user = authorize(req, resp);
        // Dừng xử lý nếu người dùng không vượt qua bước kiểm tra quyền
        if (user == null) return;
        // Chặn yêu cầu POST nếu thiếu hoặc sai CSRF token nhằm phòng chống tấn công CSRF
        if (!isValidCsrfToken(req)) {
            json(resp, 403, "{\"error\":\"Yêu cầu không hợp lệ.\"}");
            return;
        }
        try {
            String action = req.getParameter("action");
            // Đánh dấu toàn bộ thông báo chưa đọc của người dùng thành đã đọc
            if ("readAll".equals(action)) {
                json(resp, 200, "{\"updated\":" + service.markAllRead(user.getId()) + "}");
            // Đánh dấu một thông báo cụ thể theo ID là đã đọc
            } else if ("read".equals(action)) {
                int id = Integer.parseInt(req.getParameter("id"));
                // Chuyển kết quả boolean từ service thành số bản ghi cập nhật (1 nếu thành công, 0 nếu không đổi)
                json(resp, 200, "{\"updated\":" + (service.markRead(user.getId(), id) ? 1 : 0) + "}");
            // Trả lỗi 400 nếu tham số action không thuộc các hành động được hỗ trợ
            } else {
                json(resp, 400, "{\"error\":\"Hành động không hợp lệ.\"}");
            }
        } catch (NumberFormatException e) {
            // Xử lý trường hợp tham số id không thể chuyển sang số nguyên hợp lệ
            json(resp, 400, "{\"error\":\"ID không hợp lệ.\"}");
        } catch (IllegalStateException e) {
            // Xử lý lỗi hệ thống khi cập nhật trạng thái thông báo trong CSDL
            json(resp, 500, "{\"error\":\"Lỗi hệ thống.\"}");
        }
    }

    /**
     * Kiểm tra người dùng hiện tại trong request có quyền truy cập API thông báo hay không.
     * Nếu không hợp lệ sẽ tự động trả về HTTP 403 dạng JSON.
     *
     * @param req  HTTP request hiện tại
     * @param resp HTTP response để ghi lỗi 403 khi không đủ quyền
     * @return đối tượng {@link CurrentUser} nếu hợp lệ, hoặc {@code null} nếu bị từ chối
     * @throws IOException nếu xảy ra lỗi khi ghi phản hồi lỗi
     */
    private CurrentUser authorize(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CurrentUser user = AuthContext.getCurrentUser(req);
        // Từ chối truy cập nếu chưa đăng nhập hoặc chính sách không cho phép xem thông báo
        if (!AccessPolicy.canViewNotifications(user)) {
            json(resp, 403, "{\"error\":\"Cần đăng nhập.\"}");
            return null;
        }
        return user;
    }

    /**
     * Lấy CSRF token gắn với HTTP session hiện tại; nếu chưa tồn tại thì sinh mới UUID một cách an toàn luồng.
     *
     * @param req HTTP request dùng để truy xuất hoặc khởi tạo session
     * @return chuỗi CSRF token của session hiện tại
     */
    private static String csrfToken(HttpServletRequest req) {
        var session = req.getSession();
        // Đồng bộ hóa theo session để tránh tạo trùng nhiều token khi có nhiều request song song
        synchronized (session) {
            String token = (String) session.getAttribute(CSRF_SESSION_KEY);
            // Khởi tạo token ngẫu nhiên nếu session chưa từng được cấp CSRF token
            if (token == null) {
                token = UUID.randomUUID().toString();
                session.setAttribute(CSRF_SESSION_KEY, token);
            }
            return token;
        }
    }

    /**
     * Xác minh CSRF token gửi lên trong header {@code X-CSRF-Token} có khớp với token lưu trong session hay không.
     *
     * @param req HTTP request cần kiểm tra CSRF token
     * @return {@code true} nếu session tồn tại và token khớp hoàn toàn, ngược lại {@code false}
     */
    private static boolean isValidCsrfToken(HttpServletRequest req) {
        var session = req.getSession(false);
        // Không có session đồng nghĩa chưa từng được cấp token hợp lệ
        if (session == null) {
            return false;
        }
        Object expected = session.getAttribute(CSRF_SESSION_KEY);
        String supplied = req.getHeader("X-CSRF-Token");
        // So sánh theo thời gian hằng (constant-time) để tránh rò rỉ thông tin qua timing attack
        return expected instanceof String token && supplied != null
                && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                        supplied.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Chuẩn hóa giá trị bộ lọc trạng thái thông báo về một trong ba giá trị hợp lệ:
     * {@code "unread"}, {@code "read"} hoặc mặc định {@code "all"}.
     *
     * @param f giá trị tham số {@code filter} lấy từ request
     * @return chuỗi bộ lọc đã chuẩn hóa
     */
    private static String normalizeFilter(String f) {
        // Chỉ chấp nhận "unread" hoặc "read"; mọi giá trị khác (kể cả null) đều quy về "all"
        return "unread".equals(f) || "read".equals(f) ? f : "all";
    }

    /**
     * Đọc tham số kiểu số nguyên từ HTTP request, trả về giá trị mặc định nếu thiếu hoặc sai định dạng.
     *
     * @param req  HTTP request chứa tham số
     * @param name tên tham số cần đọc
     * @param def  giá trị mặc định khi không phân tích được số nguyên
     * @return giá trị nguyên đọc được hoặc {@code def}
     */
    private static int intParam(HttpServletRequest req, String name, int def) {
        try {
            return Integer.parseInt(req.getParameter(name));
        } catch (NumberFormatException e) {
            // Bỏ qua lỗi parse khi tham số null hoặc không phải số, sử dụng giá trị mặc định
            return def;
        }
    }

    /**
     * Giới hạn một số nguyên nằm trong đoạn đóng {@code [min, max]}.
     *
     * @param v   giá trị đầu vào
     * @param min cận dưới cho phép
     * @param max cận trên cho phép
     * @return giá trị đã được kẹp trong đoạn {@code [min, max]}
     */
    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Chuyển đổi chuỗi Java thành chuỗi JSON hợp lệ có dấu ngoặc kép bao quanh và escape các ký tự đặc biệt.
     *
     * @param s chuỗi cần mã hóa JSON (có thể {@code null})
     * @return chuỗi JSON literal, hoặc {@code "null"} nếu đầu vào là {@code null}
     */
    private static String quote(String s) {
        // Trả về literal null của JSON nếu chuỗi đầu vào là null
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        // Duyệt từng ký tự trong chuỗi để escape các ký tự đặc biệt theo chuẩn JSON (RFC 8259)
        for (char ch : s.toCharArray()) {
            // Phân loại ký tự cần escape hoặc giữ nguyên
            switch (ch) {
                // Escape dấu ngoặc kép
                case '"' -> sb.append("\\\"");
                // Escape dấu gạch chéo ngược
                case '\\' -> sb.append("\\\\");
                // Escape ký tự xuống dòng
                case '\n' -> sb.append("\\n");
                // Escape ký tự về đầu dòng
                case '\r' -> sb.append("\\r");
                // Escape ký tự tab
                case '\t' -> sb.append("\\t");
                // Escape các ký tự điều khiển ASCII (< 0x20) sang dạng XXXX, các ký tự còn lại giữ nguyên
                default -> sb.append(ch < 0x20 ? String.format("\\u%04x", (int) ch) : String.valueOf(ch));
            }
        }
        return sb.append('"').toString();
    }

    /**
     * Ghi phản hồi HTTP dạng JSON với mã trạng thái, mã hóa UTF-8 và tắt lưu cache trình duyệt.
     *
     * @param resp   HTTP response cần ghi nội dung
     * @param status mã trạng thái HTTP (ví dụ 200, 400, 403, 500)
     * @param body   chuỗi JSON nội dung phản hồi
     * @throws IOException nếu xảy ra lỗi khi ghi dữ liệu ra output writer
     */
    private static void json(HttpServletResponse resp, int status, String body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(body);
    }
}