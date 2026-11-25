package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityCollaborationInvitationDTO;
import com.swp391.g1.service.IActivityCollaborationInvitationService;
import com.swp391.g1.service.impl.ActivityCollaborationInvitationServiceImpl;
import com.swp391.g1.util.AuthContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@WebServlet(name = "PartnerInvitationServlet", urlPatterns = {"/partner-invitations"})
public class PartnerInvitationServlet extends HttpServlet {

    private static final Set<Integer> PAGE_SIZES = Set.of(5, 10, 20, 25);
    private static final List<String> INVITATION_STATUSES = List.of("PENDING", "APPROVED", "REJECTED", "CANCELLED");

    private final IActivityCollaborationInvitationService invitationService =
            new ActivityCollaborationInvitationServiceImpl();

    /**
     * Xử lý yêu cầu GET hiển thị danh sách lời mời phối hợp hoạt động theo quyền của người dùng hiện tại,
     * hỗ trợ lọc theo từ khóa/loại/trạng thái và phân trang.
     *
     * @param request  đối tượng HTTP request chứa các tham số lọc và phân trang
     * @param response đối tượng HTTP response để chuyển tiếp sang trang JSP
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Lấy thông tin người dùng hiện tại và xác định quyền hiển thị cột tên công ty đối tác
        CurrentUser user = AuthContext.getCurrentUser(request);
        // Nếu chưa có thông tin người dùng thì gán chuỗi rỗng để tránh NullPointerException
        String userType = user == null ? "" : user.getType();
        boolean showCompanyColumn = "ADMIN".equals(userType) || "STAFF".equals(userType);
        List<ActivityCollaborationInvitationDTO> source = invitationService.getInvitationsForUser(user);

        // Đọc và chuẩn hóa các tham số lọc từ query string
        String keyword = trim(request.getParameter("keyword"));
        String activityType = trim(request.getParameter("activityType"));
        String activityStatus = allowed(request.getParameter("activityStatus"),
                com.swp391.g1.model.Enum.ActivityStatus.values());
        String activityApproval = allowed(request.getParameter("activityApprovalStatus"),
                com.swp391.g1.model.Enum.ApprovalStatus.values());
        // Chỉ chấp nhận trạng thái lời mời nằm trong danh sách hợp lệ; ngược lại đặt rỗng (không lọc)
        String invitationStatus = INVITATION_STATUSES.contains(trim(request.getParameter("invitationStatus")))
                ? trim(request.getParameter("invitationStatus")) : "";
        String normalizedKeyword = normalize(keyword);

        // Tập hợp các loại hoạt động (sắp xếp theo thứ tự bảng chữ cái) và danh sách lời mời sau khi lọc
        Set<String> activityTypes = new TreeSet<>();
        List<ActivityCollaborationInvitationDTO> all = new ArrayList<>();
        // Duyệt toàn bộ danh sách lời mời nguồn để gom tùy chọn loại hoạt động và áp dụng từng tiêu chí lọc
        for (ActivityCollaborationInvitationDTO inv : source) {
            // Bổ sung tên loại hoạt động vào danh sách dropdown lọc nếu khác null
            if (inv.getActivityTypeName() != null) {
                activityTypes.add(inv.getActivityTypeName());
            }
            // Nếu có từ khóa tìm kiếm thì bỏ qua các bản ghi không khớp mã/tên hoạt động (và tên công ty đối tác khi được phép xem)
            if (!normalizedKeyword.isEmpty() && !normalize(inv.getActivityCode()).contains(normalizedKeyword)
                    && !normalize(inv.getActivityName()).contains(normalizedKeyword)
                    && !(showCompanyColumn && normalize(inv.getPartnerCompanyName()).contains(normalizedKeyword))) {
                continue;
            }
            // Bỏ qua nếu đang lọc theo loại hoạt động nhưng bản ghi không khớp
            if (!activityType.isEmpty() && !activityType.equals(inv.getActivityTypeName())) {
                continue;
            }
            // Bỏ qua nếu đang lọc theo trạng thái hoạt động nhưng bản ghi không khớp
            if (!activityStatus.isEmpty() && !activityStatus.equals(inv.getActivityStatus())) {
                continue;
            }
            // Bỏ qua nếu đang lọc theo trạng thái duyệt hoạt động nhưng bản ghi không khớp
            if (!activityApproval.isEmpty() && !activityApproval.equals(inv.getActivityApprovalStatus())) {
                continue;
            }
            // Bỏ qua nếu đang lọc theo trạng thái lời mời nhưng bản ghi không khớp
            if (!invitationStatus.isEmpty() && !invitationStatus.equals(inv.getApprovalStatus())) {
                continue;
            }
            all.add(inv);
        }

        // Tính toán thông số phân trang (kích thước trang, tổng số trang, trang hiện tại và chỉ số cắt danh sách)
        int pageSize = parseInt(request.getParameter("pageSize"), 5);
        // Nếu kích thước trang không nằm trong danh sách cho phép thì đưa về mặc định 5 bản ghi/trang
        if (!PAGE_SIZES.contains(pageSize)) {
            pageSize = 5;
        }
        int total = all.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        int currentPage = Math.max(1, Math.min(parseInt(request.getParameter("page"), 1), totalPages));
        int from = Math.min((currentPage - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);

        // Xây dựng chuỗi query string giữ nguyên các bộ lọc đang chọn cho liên kết phân trang
        StringBuilder query = new StringBuilder();
        appendParam(query, "keyword", keyword);
        appendParam(query, "activityType", activityType);
        appendParam(query, "activityStatus", activityStatus);
        appendParam(query, "activityApprovalStatus", activityApproval);
        appendParam(query, "invitationStatus", invitationStatus);

        // Gắn dữ liệu bộ lọc, danh sách lời mời của trang hiện tại và metadata phân trang vào request
        request.setAttribute("showCompanyColumn", showCompanyColumn);
        request.setAttribute("keyword", keyword);
        request.setAttribute("selectedActivityType", activityType);
        request.setAttribute("selectedActivityStatus", activityStatus);
        request.setAttribute("selectedActivityApprovalStatus", activityApproval);
        request.setAttribute("selectedInvitationStatus", invitationStatus);
        request.setAttribute("filterQuery", query.toString());
        request.setAttribute("filtered", query.length() > 0);
        // Đếm số tiêu chí lọc đang được kích hoạt để hiển thị huy hiệu trên giao diện
        request.setAttribute("activeFilterCount",
                (keyword.isEmpty() ? 0 : 1) + (activityType.isEmpty() ? 0 : 1) + (activityStatus.isEmpty() ? 0 : 1)
                        + (activityApproval.isEmpty() ? 0 : 1) + (invitationStatus.isEmpty() ? 0 : 1));
        request.setAttribute("activityTypes", activityTypes);
        request.setAttribute("activityStatuses", com.swp391.g1.model.Enum.ActivityStatus.values());
        request.setAttribute("approvalStatuses", com.swp391.g1.model.Enum.ApprovalStatus.values());
        request.setAttribute("invitationStatuses", INVITATION_STATUSES);
        request.setAttribute("invitations", new ArrayList<>(all.subList(from, to)));
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("currentPage", currentPage);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", total);
        // Nếu không có bản ghi nào thì chỉ số bản ghi đầu là 0, ngược lại tính từ 1 trở đi
        request.setAttribute("firstNumber", total == 0 ? 0 : from + 1);
        request.setAttribute("lastNumber", to);
        request.getRequestDispatcher("/WEB-INF/views/partner-invitation/list.jsp").forward(request, response);
    }

    /**
     * Cắt khoảng trắng hai đầu chuỗi một cách an toàn với giá trị null.
     *
     * @param value chuỗi đầu vào cần xử lý
     * @return chuỗi đã loại bỏ khoảng trắng hai đầu, hoặc chuỗi rỗng nếu đầu vào là null
     */
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Chuẩn hóa chuỗi tiếng Việt về dạng không dấu, chữ thường để phục vụ tìm kiếm không phân biệt dấu và hoa/thường.
     *
     * @param value chuỗi cần chuẩn hóa
     * @return chuỗi đã bỏ dấu tiếng Việt và chuyển thành chữ thường, hoặc chuỗi rỗng nếu null
     */
    private String normalize(String value) {
        // Trả về chuỗi rỗng nếu giá trị đầu vào là null
        if (value == null) {
            return "";
        }
        // Tách dấu tổ hợp Unicode (NFD), xóa toàn bộ ký tự dấu, đổi đ/Đ thành d/D rồi hạ chữ thường
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Kiểm tra giá trị tham số có khớp với tên một hằng enum hợp lệ trong danh sách cho phép hay không.
     *
     * @param value   giá trị chuỗi lấy từ request
     * @param options mảng các giá trị enum hợp lệ
     * @return tên enum hợp lệ nếu khớp, ngược lại trả về chuỗi rỗng
     */
    private String allowed(String value, java.lang.Enum<?>[] options) {
        String candidate = trim(value);
        // Duyệt danh sách các hằng enum hợp lệ để đối chiếu chính xác theo tên
        for (java.lang.Enum<?> option : options) {
            // Trả về ngay nếu tìm thấy hằng enum trùng khớp với giá trị đầu vào
            if (option.name().equals(candidate)) {
                return candidate;
            }
        }
        return "";
    }

    /**
     * Nối thêm một cặp tham số truy vấn đã mã hóa URL vào chuỗi query nếu giá trị không rỗng.
     *
     * @param query bộ đệm chuỗi query string đang xây dựng
     * @param name  tên tham số
     * @param value giá trị tham số cần nối
     */
    private void appendParam(StringBuilder query, String name, String value) {
        // Chỉ nối tham số khi giá trị khác rỗng để tránh làm rác URL
        if (!value.isEmpty()) {
            query.append('&').append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
    }

    /**
     * Chuyển đổi chuỗi sang số nguyên, trả về giá trị mặc định nếu chuỗi không hợp lệ.
     *
     * @param value    chuỗi số cần chuyển đổi
     * @param fallback giá trị mặc định khi chuyển đổi thất bại
     * @return giá trị số nguyên sau khi phân tích hoặc giá trị fallback
     */
    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Trả về giá trị mặc định khi tham số null hoặc không đúng định dạng số nguyên
            return fallback;
        }
    }
}
