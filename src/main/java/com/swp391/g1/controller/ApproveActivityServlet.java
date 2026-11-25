package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IExtracurricularActivityService;
import com.swp391.g1.util.AuthContext;
import com.swp391.g1.service.impl.ExtracurricularActivityServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Servlet xử lý quy trình phê duyệt hoạt động ngoại khóa (gửi duyệt, duyệt, từ chối,
 * hủy duyệt, hủy từ chối và duyệt/từ chối hàng loạt).
 */
@WebServlet("/activity/approval")
public class ApproveActivityServlet extends HttpServlet {

    private final IExtracurricularActivityService activityService =
            new ExtracurricularActivityServiceImpl();

    /**
     * Xử lý các yêu cầu POST liên quan đến thay đổi trạng thái phê duyệt của hoạt động.
     * Kiểm tra tính hợp lệ của hành động, phân quyền theo vai trò (ADMIN/STAFF) và điều phối
     * sang xử lý đơn lẻ hoặc xử lý hàng loạt.
     *
     * @param request  đối tượng yêu cầu HTTP chứa tham số {@code action} và {@code id}
     * @param response đối tượng phản hồi HTTP để chuyển hướng hoặc trả mã lỗi
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi nhập/xuất khi phản hồi
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String action = request.getParameter("action");
            CurrentUser currentUser = AuthContext.getCurrentUser(request);

            // Từ chối yêu cầu nếu thiếu tham số hành động
            if (action == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Hành động không hợp lệ.");
                return;
            }

            // Xác định các hành động ra quyết định phê duyệt vốn chỉ dành cho quản trị viên (ADMIN)
            boolean decisionAction = "approve".equals(action) || "reject".equals(action)
                    || "revokeApproval".equals(action) || "revokeRejection".equals(action)
                    || "bulkApprove".equals(action) || "bulkReject".equals(action);
            // Chặn người dùng không có quyền duyệt khi thực hiện các hành động phê duyệt
            if (decisionAction && !AccessPolicy.canReviewActivities(currentUser)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Chỉ tài khoản ADMIN mới được duyệt, từ chối hoặc hủy duyệt hoạt động.");
                return;
            }

            // Rẽ nhánh sang luồng duyệt hoặc từ chối nhiều hoạt động cùng lúc
            if ("bulkApprove".equals(action) || "bulkReject".equals(action)) {
                handleBulkDecision(request, response, action);
                return;
            }

            int id = Integer.parseInt(request.getParameter("id"));
            // Kiểm tra quyền quản lý và phạm vi truy cập hoạt động trước khi cho phép gửi duyệt
            if ("submit".equals(action) && (!AccessPolicy.canManageActivities(currentUser)
                    || !activityService.canAccessActivity(id, currentUser))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Chỉ tài khoản STAFF hoặc ADMIN mới được gửi duyệt hoạt động.");
                return;
            }

            boolean success;
            String message;

            // Điều phối nghiệp vụ chuyển trạng thái duyệt tương ứng với tham số action
            switch (action) {
                // Chuyển hoạt động từ bản nháp/bị từ chối sang chờ duyệt
                case "submit":
                    success = activityService.submitForApproval(id);
                    message = "Đã gửi hoạt động đi duyệt.";
                    break;
                // Phê duyệt hoạt động đang chờ duyệt
                case "approve":
                    success = activityService.approveActivity(id);
                    message = "Đã duyệt hoạt động.";
                    break;
                // Từ chối hoạt động đang chờ duyệt
                case "reject":
                    success = activityService.rejectActivity(id);
                    message = "Đã từ chối hoạt động.";
                    break;
                // Hủy quyết định duyệt để đưa hoạt động quay lại trạng thái chờ duyệt
                case "revokeApproval":
                    success = activityService.revokeApproval(id);
                    message = "Đã hủy duyệt, hoạt động quay về trạng thái Chờ duyệt.";
                    break;
                // Hủy quyết định từ chối để đưa hoạt động quay lại trạng thái chờ duyệt
                case "revokeRejection":
                    success = activityService.revokeRejection(id);
                    message = "Đã hủy từ chối, hoạt động quay về trạng thái Chờ duyệt.";
                    break;
                // Trả lỗi 400 nếu action không nằm trong danh sách hỗ trợ
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Hành động không hợp lệ.");
                    return;
            }

            // Báo lỗi 404 nếu hoạt động không tồn tại hoặc không tìm thấy bản ghi để cập nhật
            if (!success) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy hoạt động.");
                return;
            }

            request.getSession().setAttribute("successMessage", message);
            response.sendRedirect(request.getContextPath() + "/activities");
        } catch (NumberFormatException e) {
            // Lỗi khi tham số id không thể chuyển đổi sang số nguyên
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID hoạt động không hợp lệ.");
        } catch (IllegalArgumentException e) {
            // Lỗi nghiệp vụ (trạng thái không hợp lệ, hoạt động đã bắt đầu,...) -> hiển thị flash message
            request.getSession().setAttribute("errorMessage", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/activities");
        } catch (IllegalStateException e) {
            // Lỗi trạng thái hệ thống khi không thể lưu cập nhật phê duyệt
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không thể cập nhật trạng thái duyệt.");
        }
    }

    /**
     * Xử lý duyệt hoặc từ chối hàng loạt cho danh sách ID hoạt động được chọn (từ 1 đến 100 hoạt động).
     * Các hoạt động hợp lệ sẽ được cập nhật độc lập; những hoạt động bị thay đổi trạng thái đồng thời
     * sẽ được bỏ qua và tổng hợp số lượng vào thông báo kết quả.
     *
     * @param request  đối tượng yêu cầu HTTP chứa mảng tham số {@code id}
     * @param response đối tượng phản hồi HTTP
     * @param action   hành động hàng loạt ({@code "bulkApprove"} hoặc {@code "bulkReject"})
     * @throws IOException nếu xảy ra lỗi khi chuyển hướng hoặc trả lỗi HTTP
     */
    private void handleBulkDecision(HttpServletRequest request, HttpServletResponse response, String action)
            throws IOException {
        String[] rawIds = request.getParameterValues("id");
        // Ràng buộc số lượng hoạt động được chọn phải nằm trong khoảng 1..100
        if (rawIds == null || rawIds.length == 0 || rawIds.length > 100) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Hãy chọn từ 1 đến 100 hoạt động.");
            return;
        }

        // Dùng LinkedHashSet để loại bỏ ID trùng lặp nhưng vẫn giữ thứ tự chọn ban đầu
        Set<Integer> ids = new LinkedHashSet<>();
        // Duyệt danh sách ID dạng chuỗi từ request để kiểm tra và chuyển đổi sang số nguyên dương
        for (String rawId : rawIds) {
            try {
                int id = Integer.parseInt(rawId);
                // Không chấp nhận ID <= 0
                if (id <= 0) {
                    throw new NumberFormatException();
                }
                ids.add(id);
            } catch (NumberFormatException e) {
                // Dừng ngay và trả lỗi 400 nếu có bất kỳ ID nào không hợp lệ
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID hoạt động không hợp lệ.");
                return;
            }
        }

        int updated = 0;
        // Duyệt từng ID hoạt động hợp lệ để thực hiện duyệt hoặc từ chối
        for (int id : ids) {
            try {
                // Gọi hàm duyệt hoặc từ chối tương ứng với hành động hàng loạt đã chọn
                boolean success = "bulkApprove".equals(action)
                        ? activityService.approveActivity(id)
                        : activityService.rejectActivity(id);
                // Tăng bộ đếm nếu hoạt động được cập nhật thành công
                if (success) {
                    updated++;
                }
            } catch (IllegalArgumentException e) {
                // Bỏ qua hoạt động không còn đủ điều kiện (ví dụ thay đổi đồng thời) để tiếp tục xử lý các dòng còn lại
            }
        }

        // Tổng hợp số lượng thành công/bỏ qua và gắn loại thông báo phù hợp vào session
        int skipped = ids.size() - updated;
        // Chọn nhãn hành động tiếng Việt hiển thị trên thông báo kết quả
        String decision = "bulkApprove".equals(action) ? "duyệt" : "từ chối";
        String message = "Đã " + decision + " " + updated + "/" + ids.size() + " hoạt động.";
        // Nối thêm chi tiết số hoạt động bị bỏ qua nếu không phải tất cả đều cập nhật thành công
        if (skipped > 0) {
            message += " Bỏ qua " + skipped + " hoạt động không còn ở trạng thái chờ duyệt.";
        }
        // Dùng successMessage khi thành công toàn bộ, ngược lại dùng errorMessage để cảnh báo có dòng bị bỏ qua
        request.getSession().setAttribute(updated == ids.size() ? "successMessage" : "errorMessage", message);
        response.sendRedirect(request.getContextPath() + "/activities");
    }
}
