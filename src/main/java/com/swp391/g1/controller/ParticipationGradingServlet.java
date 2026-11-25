package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityParticipantResponseDTO;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.model.Enum.ParticipantStatus;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IActivityCollaborationInvitationService;
import com.swp391.g1.service.IActivityParticipantService;
import com.swp391.g1.service.IExtracurricularActivityService;
import com.swp391.g1.service.impl.ActivityCollaborationInvitationServiceImpl;
import com.swp391.g1.service.impl.ActivityParticipantServiceImpl;
import com.swp391.g1.service.impl.ExtracurricularActivityServiceImpl;
import com.swp391.g1.util.AuthContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Servlet xử lý màn hình và thao tác chấm điểm đánh giá tham gia hoạt động ngoại khóa
 * (hỗ trợ cả submit form truyền thống lẫn AJAX chấm đơn lẻ/hàng loạt).
 */
@WebServlet(name = "ParticipationGradingServlet", urlPatterns = {"/participation-grading"})
public class ParticipationGradingServlet extends HttpServlet {

    private static final Set<String> POST_ACTIONS = Set.of("batchGrade", "batchGradeAjax", "saveGradeAjax");
    private static final Set<String> AJAX_ACTIONS = Set.of("batchGradeAjax", "saveGradeAjax");
    private static final List<Integer> PAGE_SIZES = List.of(30, 40, 50);
    private static final int DEFAULT_PAGE_SIZE = 30;

    private final IExtracurricularActivityService activityService;
    private final IActivityCollaborationInvitationService invitationService;
    private final IActivityParticipantService participantService;

    /**
     * Khởi tạo servlet cùng các service quản lý hoạt động, lời mời phối hợp và người tham gia.
     */
    public ParticipationGradingServlet() {
        this.activityService = new ExtracurricularActivityServiceImpl();
        this.invitationService = new ActivityCollaborationInvitationServiceImpl();
        this.participantService = new ActivityParticipantServiceImpl();
    }

    /**
     * Kiểm tra quyền truy cập chung trước khi điều phối request sang {@code doGet}/{@code doPost}.
     * Chỉ cho phép tài khoản có quyền điểm danh (ADMIN, STAFF hoặc PARTNER_STAFF).
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Từ chối truy cập (403) nếu người dùng hiện tại không có quyền điểm danh/chấm điểm tham gia
        if (!AccessPolicy.canTakeAttendance(AuthContext.getCurrentUser(request))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ tài khoản ADMIN, STAFF hoặc PARTNER_STAFF mới được chấm điểm tham gia.");
            return;
        }
        super.service(request, response);
    }

    /**
     * Hiển thị giao diện chấm điểm tham gia: tải danh sách hoạt động được phép xem,
     * thống kê trạng thái chấm điểm, bộ lọc động và phân trang danh sách sinh viên tham gia.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            CurrentUser currentUser = AuthContext.getCurrentUser(request);
            List<ExtracurricularActivityResponseDTO> activities = getVisibleActivities(currentUser);
            request.setAttribute("activities", activities);

            int activityId = parseId(request.getParameter("activityId"));
            // Nếu người dùng đã chọn một hoạt động cụ thể thì tải chi tiết danh sách sinh viên và thống kê
            if (activityId > 0) {
                ExtracurricularActivityResponseDTO selectedActivity = findById(activities, activityId);
                // Kiểm tra quyền chấm điểm đối với hoạt động được chọn; chặn 403 nếu không nằm trong danh sách được phép
                if (selectedActivity == null) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Bạn không có quyền chấm điểm tham gia hoạt động này.");
                    return;
                }
                List<ActivityParticipantResponseDTO> allParticipants =
                        participantService.getParticipants(activityId, 1);

                // Các biến đếm thống kê tổng quan (đủ điều kiện, đã chấm, chưa chấm, không đủ điều kiện)
                long eligibleCount = 0;
                long gradedCount = 0;
                long ungradedCount = 0;
                long ineligibleCount = 0;

                // Tập hợp giá trị duy nhất (sắp xếp theo thứ tự bảng chữ cái) để hiển thị dropdown bộ lọc
                Set<String> departments = new TreeSet<>();
                Set<String> programs = new TreeSet<>();
                Set<String> majors = new TreeSet<>();
                Set<String> classes = new TreeSet<>();

                // Duyệt toàn bộ người tham gia để gom dữ liệu bộ lọc và tính số liệu thống kê chấm điểm
                for (ActivityParticipantResponseDTO participant : allParticipants) {
                    addIfPresent(departments, participant.getDepartmentName());
                    addIfPresent(programs, participant.getAcademicProgramName());
                    addIfPresent(majors, participant.getMajorName());
                    addIfPresent(classes, participant.getMainClassCode());

                    // Sinh viên có mặt (đủ điều kiện chấm điểm tham gia) -> phân loại tiếp đã chấm hay chưa chấm
                    if (participant.isPresentStatus()) {
                        eligibleCount++;
                        // Đã có điểm đánh giá tham gia
                        if (participant.isGraded()) {
                            gradedCount++;
                        // Có mặt nhưng chưa được chấm điểm đánh giá
                        } else {
                            ungradedCount++;
                        }
                    // Sinh viên vắng mặt hoặc chưa điểm danh -> không đủ điều kiện chấm điểm tham gia
                    } else {
                        ineligibleCount++;
                    }
                }

                // Khởi tạo bộ lọc từ tham số request và lọc danh sách sinh viên khớp tiêu chí
                GradingFilter filter = GradingFilter.from(request);
                List<ActivityParticipantResponseDTO> filteredParticipants = new ArrayList<>();
                // Duyệt danh sách gốc và giữ lại những người tham gia thỏa mãn toàn bộ điều kiện lọc
                for (ActivityParticipantResponseDTO participant : allParticipants) {
                    // Chỉ thêm sinh viên khớp điều kiện tìm kiếm, đơn vị học thuật và trạng thái chấm điểm
                    if (filter.matches(participant)) {
                        filteredParticipants.add(participant);
                    }
                }

                // Tính toán thông số phân trang trên tập kết quả sau khi đã lọc (giới hạn trang hiện tại trong [1..totalPages])
                int pageSize = parsePageSize(request.getParameter("pageSize"));
                int filteredCount = filteredParticipants.size();
                int totalPages = Math.max(1, (int) Math.ceil((double) filteredCount / pageSize));
                int currentPage = Math.min(Math.max(parseId(request.getParameter("page")), 1), totalPages);
                int fromIndex = Math.min((currentPage - 1) * pageSize, filteredCount);
                int toIndex = Math.min(fromIndex + pageSize, filteredCount);
                List<ActivityParticipantResponseDTO> participants =
                        new ArrayList<>(filteredParticipants.subList(fromIndex, toIndex));

                request.setAttribute("selectedActivity", selectedActivity);
                request.setAttribute("selectedActivityId", activityId);
                request.setAttribute("participants", participants);
                request.setAttribute("filteredParticipantCount", filteredCount);
                request.setAttribute("pageSize", pageSize);
                request.setAttribute("pageSizes", PAGE_SIZES);
                request.setAttribute("currentPage", currentPage);
                request.setAttribute("totalPages", totalPages);
                // Số thứ tự bản ghi đầu tiên trên trang (trả về 0 nếu không có kết quả sau lọc)
                request.setAttribute("firstParticipantNumber", filteredCount == 0 ? 0 : fromIndex + 1);
                request.setAttribute("lastParticipantNumber", toIndex);
                request.setAttribute("pagingQuery", pagingQuery(currentPage, pageSize));
                request.setAttribute("totalParticipantCount", allParticipants.size());
                request.setAttribute("eligibleCount", eligibleCount);
                request.setAttribute("gradedCount", gradedCount);
                request.setAttribute("ungradedCount", ungradedCount);
                request.setAttribute("ineligibleCount", ineligibleCount);
                request.setAttribute("canTakeAttendance", participantService.canTakeAttendance(selectedActivity));
                request.setAttribute("departments", departments);
                request.setAttribute("academicPrograms", programs);
                request.setAttribute("majors", majors);
                request.setAttribute("mainClasses", classes);
                setFilterAttributes(request, filter);
            }
            request.setAttribute("participantStatuses", ParticipantStatus.values());
            request.getRequestDispatcher("/WEB-INF/views/participation-grading/list.jsp").forward(request, response);
        // Bắt lỗi hệ thống ngoài dự kiến và chuyển tiếp sang trang báo lỗi 500
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("errorMessage", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    /**
     * Xử lý yêu cầu lưu điểm đánh giá tham gia (hỗ trợ cả chấm hàng loạt qua form và lưu qua AJAX).
     * Xác thực hành động, kiểm tra quyền trên hoạt động, lưu điểm và phản hồi JSON hoặc redirect giữ nguyên bộ lọc.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        int activityId = parseId(request.getParameter("activityId"));
        String action = request.getParameter("action");
        boolean isAjax = action != null && AJAX_ACTIONS.contains(action);
        // Kiểm tra tính hợp lệ của action và activityId trước khi xử lý nghiệp vụ
        if (action == null || !POST_ACTIONS.contains(action) || activityId <= 0) {
            // Nếu là AJAX thì trả về JSON lỗi 400
            if (isAjax) {
                json(response, HttpServletResponse.SC_BAD_REQUEST,
                        "{\"ok\":false,\"error\":\"Yêu cầu chấm điểm không hợp lệ.\"}");
            // Nếu là form thường thì chuyển hướng về trang danh sách chấm điểm
            } else {
                response.sendRedirect(request.getContextPath() + "/participation-grading");
            }
            return;
        }

        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        // Xác minh người dùng có quyền chấm điểm đối với hoạt động có activityId này hay không
        if (findById(getVisibleActivities(currentUser), activityId) == null) {
            // Trả về JSON 403 cho request AJAX
            if (isAjax) {
                json(response, HttpServletResponse.SC_FORBIDDEN,
                        "{\"ok\":false,\"error\":\"Bạn không có quyền chấm điểm tham gia hoạt động này.\"}");
            // Trả về trang lỗi 403 cho request form thông thường
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bạn không có quyền chấm điểm tham gia hoạt động này.");
            }
            return;
        }

        // Điều hướng riêng cho các yêu cầu AJAX (trả về JSON kết quả cập nhật)
        if (isAjax) {
            saveGradesAjax(request, response, activityId, currentUser);
            return;
        }

        // Luồng submit form chấm điểm hàng loạt (lưu thông báo vào session và redirect giữ nguyên trạng thái lọc/trang)
        HttpSession session = request.getSession();
        try {
            List<Integer> participantIds = parseIds(request.getParameterValues("participantId"));
            BigDecimal evalScore = parseScore(request.getParameter("evalScore"));
            String feedback = request.getParameter("feedback");
            int graded = participantService.saveParticipationGrades(
                    activityId, participantIds, evalScore, feedback, currentUser);
            // Tạo thông báo kết quả tùy theo có sinh viên đủ điều kiện nào được cập nhật điểm hay không
            session.setAttribute("successMessage", graded == 0
                    ? "Không có sinh viên có mặt nào trong danh sách chọn để chấm điểm tham gia."
                    : "Đã chấm điểm đánh giá tham gia cho " + graded + " sinh viên.");
        // Bắt lỗi dữ liệu đầu vào không hợp lệ (ví dụ: điểm ngoài khoảng 0 - 5)
        } catch (IllegalArgumentException e) {
            session.setAttribute("errorMessage", e.getMessage());
        // Bắt các lỗi hệ thống khác trong quá trình lưu điểm
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Đã xảy ra lỗi khi chấm điểm tham gia: " + e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/participation-grading?activityId=" + activityId
                + GradingFilter.from(request).toQuery()
                + pagingQuery(parseId(request.getParameter("page")), parsePageSize(request.getParameter("pageSize"))));
    }

    /**
     * Xử lý lưu điểm đánh giá tham gia theo cơ chế AJAX và trả về JSON chứa số lượng bản ghi
     * đã cập nhật cùng chi tiết điểm mới nhất của các sinh viên bị tác động để cập nhật UI tức thì.
     */
    private void saveGradesAjax(HttpServletRequest request, HttpServletResponse response,
            int activityId, CurrentUser currentUser) throws IOException {
        try {
            List<Integer> participantIds = parseIds(request.getParameterValues("participantId"));
            BigDecimal evalScore = parseScore(request.getParameter("evalScore"));
            String feedback = request.getParameter("feedback");
            int updated = participantService.saveParticipationGrades(
                    activityId, participantIds, evalScore, feedback, currentUser);
            // Tải lại danh sách người tham gia sau khi lưu để lấy điểm tổng hợp mới tính lại từ DB
            List<ActivityParticipantResponseDTO> allParticipants = participantService.getParticipants(activityId, 1);
            // Dựng JSON phản hồi gồm cờ ok, số lượng đã cập nhật và map điểm theo participantId
            StringBuilder body = new StringBuilder("{\"ok\":true,\"updated\":").append(updated)
                    .append(",\"participants\":{");
            appendParticipantScoresJson(body, allParticipants, Set.copyOf(participantIds));
            body.append("}}");
            json(response, HttpServletResponse.SC_OK, body.toString());
        // Bắt lỗi validate dữ liệu và trả về HTTP 400 kèm thông điệp lỗi dạng JSON
        } catch (IllegalArgumentException e) {
            json(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"ok\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        // Bắt lỗi hệ thống ngoài dự kiến và trả về HTTP 500 dạng JSON
        } catch (Exception e) {
            json(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"ok\":false,\"error\":\"Đã xảy ra lỗi khi lưu điểm tham gia.\"}");
        }
    }

    /**
     * Nối các cặp {@code "participantId": {...}} chứa thông tin điểm mới nhất vào chuỗi JSON trả về AJAX.
     *
     * @param body            buffer JSON đang dựng
     * @param allParticipants danh sách đầy đủ người tham gia của hoạt động
     * @param targetIds       tập ID người tham gia cần lọc để xuất (nếu rỗng/null sẽ xuất tất cả)
     */
    private void appendParticipantScoresJson(StringBuilder body,
            List<ActivityParticipantResponseDTO> allParticipants, Set<Integer> targetIds) {
        boolean first = true;
        // Duyệt danh sách người tham gia để serialize dữ liệu điểm của các mục tiêu vừa chấm
        for (ActivityParticipantResponseDTO p : allParticipants) {
            // Bỏ qua sinh viên không nằm trong danh sách ID mục tiêu (khi targetIds có dữ liệu)
            if (targetIds != null && !targetIds.isEmpty() && !targetIds.contains(p.getParticipantId())) {
                continue;
            }
            // Thêm dấu phẩy ngăn cách giữa các thuộc tính JSON kể từ phần tử thứ hai
            if (!first) {
                body.append(',');
            }
            first = false;
            // Ghi đối tượng JSON chi tiết trạng thái chuyên cần, điểm đánh giá, nhận xét và điểm tổng kết
            body.append('"').append(p.getParticipantId()).append("\":{")
                    .append("\"present\":").append(p.isPresentStatus())
                    .append(",\"attendedSessionCount\":").append(p.getAttendedSessionCount())
                    .append(",\"totalSessions\":").append(p.getTotalSessions())
                    .append(",\"attendanceScore\":\"").append(escapeJson(p.getFormattedAttendanceScore())).append('"')
                    .append(",\"evalScore\":\"").append(escapeJson(p.getFormattedEvalScore())).append('"')
                    .append(",\"feedback\":\"").append(escapeJson(p.getParticipationFeedback())).append('"')
                    .append(",\"gradedBy\":\"").append(escapeJson(p.getParticipationGradedBy())).append('"')
                    .append(",\"participationScore\":\"").append(escapeJson(p.getFormattedParticipationScore())).append('"')
                    .append(",\"finalPoint\":\"").append(escapeJson(p.getFormattedFinalPoint())).append('"')
                    .append('}');
        }
    }

    /**
     * Chuyển đổi chuỗi điểm đánh giá tham gia sang {@link BigDecimal} (hỗ trợ cả dấu phẩy thập phân).
     *
     * @param value chuỗi điểm người dùng nhập
     * @return giá trị điểm dạng {@link BigDecimal}
     * @throws IllegalArgumentException nếu chuỗi rỗng hoặc sai định dạng số
     */
    private BigDecimal parseScore(String value) {
        // Báo lỗi nếu người dùng chưa nhập điểm đánh giá
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập điểm đánh giá tham gia (0 - 5).");
        }
        try {
            // Cho phép nhập số thập phân bằng dấu phẩy bằng cách chuẩn hóa sang dấu chấm
            return new BigDecimal(value.trim().replace(',', '.'));
        // Bắt lỗi định dạng số không hợp lệ và chuyển sang IllegalArgumentException thân thiện với người dùng
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Điểm đánh giá tham gia không hợp lệ.");
        }
    }

    /**
     * Ghi phản hồi HTTP dạng JSON UTF-8 với mã trạng thái chỉ định.
     */
    private static void json(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body);
    }

    /**
     * Escape các ký tự đặc biệt ({@code \}, {@code "}, {@code \r}, {@code \n}) để nhúng an toàn vào chuỗi JSON.
     */
    private static String escapeJson(String value) {
        // Trả về chuỗi rỗng nếu giá trị đầu vào là null để tránh in chữ "null" vào JSON
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    /**
     * Tạo đoạn query string phân trang ({@code &page=...&pageSize=...}), đảm bảo số trang tối thiểu là 1.
     */
    private String pagingQuery(int page, int pageSize) {
        return "&page=" + Math.max(page, 1) + "&pageSize=" + pageSize;
    }

    /**
     * Phân tích kích thước trang từ request; chỉ chấp nhận các giá trị hợp lệ trong {@link #PAGE_SIZES},
     * ngược lại dùng {@link #DEFAULT_PAGE_SIZE}.
     */
    private int parsePageSize(String value) {
        int pageSize = parseId(value);
        // Nếu kích thước trang nằm trong danh sách hỗ trợ thì dùng, ngược lại trả về mặc định
        return PAGE_SIZES.contains(pageSize) ? pageSize : DEFAULT_PAGE_SIZE;
    }

    /**
     * Gán toàn bộ giá trị bộ lọc hiện tại và số tiêu chí đang kích hoạt vào request attribute cho view JSP.
     */
    private void setFilterAttributes(HttpServletRequest request, GradingFilter filter) {
        request.setAttribute("keyword", filter.keyword);
        request.setAttribute("selectedDepartment", filter.department);
        request.setAttribute("selectedAcademicProgram", filter.academicProgram);
        request.setAttribute("selectedMajor", filter.major);
        request.setAttribute("selectedMainClass", filter.mainClass);
        request.setAttribute("selectedGradingStatus", filter.gradingStatus);
        request.setAttribute("filtered", filter.activeCount() > 0);
        request.setAttribute("activeFilterCount", filter.activeCount());
        request.setAttribute("filterQuery", filter.toQuery());
    }

    /**
     * Thêm chuỗi đã loại bỏ khoảng trắng thừa vào tập hợp nếu chuỗi không null và không rỗng.
     */
    private void addIfPresent(Set<String> values, String value) {
        // Bỏ qua giá trị null hoặc toàn khoảng trắng để không xuất hiện mục rỗng trong dropdown lọc
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }

    /**
     * Lấy danh sách hoạt động mà người dùng hiện tại có quyền xem và chấm điểm tham gia
     * (giao giữa danh sách hoạt động được thấy và danh sách hoạt động được phân quyền điểm danh).
     */
    private List<ExtracurricularActivityResponseDTO> getVisibleActivities(CurrentUser user) {
        Set<Integer> visibleActivityIds = invitationService.getAttendanceActivityIds(user);
        List<ExtracurricularActivityResponseDTO> activities = new ArrayList<>();
        // Duyệt các hoạt động người dùng có thể thấy và chỉ giữ lại hoạt động thuộc phạm vi quyền điểm danh
        for (ExtracurricularActivityResponseDTO activity : activityService.getActivitiesVisibleTo(user)) {
            // visibleActivityIds == null áp dụng cho vai trò toàn quyền (như ADMIN/STAFF); ngược lại lọc theo tập ID được mời
            if (visibleActivityIds == null || visibleActivityIds.contains(activity.getId())) {
                activities.add(activity);
            }
        }
        return activities;
    }

    /**
     * Tìm hoạt động theo ID trong danh sách hoạt động đã tải; trả về {@code null} nếu không tồn tại.
     */
    private ExtracurricularActivityResponseDTO findById(List<ExtracurricularActivityResponseDTO> activities,
            int activityId) {
        // Duyệt tuần tự danh sách hoạt động để tìm bản ghi có ID trùng khớp
        for (ExtracurricularActivityResponseDTO activity : activities) {
            // Trả về ngay khi tìm thấy hoạt động khớp ID
            if (activity.getId() == activityId) {
                return activity;
            }
        }
        return null;
    }

    /**
     * Chuyển mảng chuỗi ID từ request thành danh sách số nguyên dương không trùng lặp, giữ nguyên thứ tự.
     */
    private List<Integer> parseIds(String[] values) {
        List<Integer> ids = new ArrayList<>();
        // Chỉ duyệt khi tham số mảng được gửi lên trong request
        if (values != null) {
            // Chuyển từng phần tử sang số nguyên và lọc bỏ giá trị không hợp lệ hoặc trùng lặp
            for (String value : values) {
                int id = parseId(value);
                // Chỉ nhận ID dương và chưa xuất hiện trong danh sách kết quả
                if (id > 0 && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    /**
     * Chuyển chuỗi tham số sang số nguyên ID; trả về {@code 0} nếu null, rỗng hoặc sai định dạng.
     */
    private int parseId(String value) {
        // Trả về 0 nếu chuỗi đầu vào rỗng hoặc null
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        // Trả về 0 khi chuỗi không thể ép kiểu sang số nguyên
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Lớp đóng gói các tiêu chí lọc danh sách sinh viên ở màn hình chấm điểm tham gia,
     * hỗ trợ tìm kiếm tiếng Việt không dấu và dựng lại query string khi chuyển hướng.
     */
    private static final class GradingFilter {
        private final String keyword;
        private final String department;
        private final String academicProgram;
        private final String major;
        private final String mainClass;
        private final String gradingStatus;
        private final String normalizedKeyword;

        /**
         * Khởi tạo bộ lọc từ các tham số truy vấn trong {@link HttpServletRequest}.
         */
        private GradingFilter(HttpServletRequest request) {
            this.keyword = trimToEmpty(request.getParameter("keyword"));
            this.department = trimToEmpty(request.getParameter("department"));
            this.academicProgram = trimToEmpty(request.getParameter("academicProgram"));
            this.major = trimToEmpty(request.getParameter("major"));
            this.mainClass = trimToEmpty(request.getParameter("mainClass"));
            this.gradingStatus = parseGradingStatus(request.getParameter("gradingStatus"));
            this.normalizedKeyword = normalizeText(keyword);
        }

        /**
         * Factory method tạo đối tượng {@link GradingFilter} từ request hiện tại.
         */
        static GradingFilter from(HttpServletRequest request) {
            return new GradingFilter(request);
        }

        /**
         * Kiểm tra một người tham gia có thỏa mãn đồng thời tất cả điều kiện lọc hiện tại hay không
         * (từ khóa MSSV/họ tên, khoa, chương trình đào tạo, ngành, lớp và trạng thái chấm điểm).
         */
        boolean matches(ActivityParticipantResponseDTO participant) {
            // Kiểm tra từ khóa (không dấu, không phân biệt hoa thường) trên mã sinh viên hoặc họ tên
            if (!normalizedKeyword.isEmpty()
                    && !normalizeText(participant.getStudentCode()).contains(normalizedKeyword)
                    && !normalizeText(participant.getStudentName()).contains(normalizedKeyword)) {
                return false;
            }
            // Kiểm tra khớp chính xác các bộ lọc đơn vị học thuật (khoa, chương trình, ngành, lớp)
            if (!matchesValue(department, participant.getDepartmentName())
                    || !matchesValue(academicProgram, participant.getAcademicProgramName())
                    || !matchesValue(major, participant.getMajorName())
                    || !matchesValue(mainClass, participant.getMainClassCode())) {
                return false;
            }
            // Kiểm tra điều kiện lọc theo trạng thái chấm điểm tham gia
            return switch (gradingStatus) {
                // Đủ điều kiện chấm điểm (sinh viên có mặt)
                case "ELIGIBLE" -> participant.isPresentStatus();
                // Đã có mặt và đã được chấm điểm đánh giá
                case "GRADED" -> participant.isPresentStatus() && participant.isGraded();
                // Đã có mặt nhưng chưa được chấm điểm đánh giá
                case "UNGRADED" -> participant.isPresentStatus() && !participant.isGraded();
                // Không đủ điều kiện chấm điểm (vắng mặt hoặc chưa điểm danh)
                case "INELIGIBLE" -> !participant.isPresentStatus();
                // Không chọn lọc theo trạng thái -> chấp nhận tất cả
                default -> true;
            };
        }

        /**
         * Đếm tổng số tiêu chí lọc đang có giá trị (khác rỗng) để hiển thị badge trên giao diện.
         */
        int activeCount() {
            // Cộng dồn 1 cho mỗi trường lọc khác rỗng (tối đa 6 tiêu chí)
            return (normalizedKeyword.isEmpty() ? 0 : 1) + (department.isEmpty() ? 0 : 1)
                    + (academicProgram.isEmpty() ? 0 : 1) + (major.isEmpty() ? 0 : 1)
                    + (mainClass.isEmpty() ? 0 : 1) + (gradingStatus.isEmpty() ? 0 : 1);
        }

        /**
         * Chuyển các tiêu chí lọc đang kích hoạt thành đoạn query string đã URL-encode để giữ trạng thái sau redirect.
         */
        String toQuery() {
            StringBuilder query = new StringBuilder();
            appendParam(query, "keyword", keyword);
            appendParam(query, "department", department);
            appendParam(query, "academicProgram", academicProgram);
            appendParam(query, "major", major);
            appendParam(query, "mainClass", mainClass);
            appendParam(query, "gradingStatus", gradingStatus);
            return query.toString();
        }

        /**
         * So khớp giá trị được chọn trong dropdown với giá trị thực tế của bản ghi
         * (trả về {@code true} nếu không chọn lọc).
         */
        private static boolean matchesValue(String selected, String value) {
            return selected.isEmpty() || (value != null && selected.equals(value.trim()));
        }

        /**
         * Nối cặp {@code &name=encodedValue} vào query string nếu {@code value} không rỗng.
         */
        private static void appendParam(StringBuilder query, String name, String value) {
            // Chỉ đưa tham số lên URL khi có giá trị lọc thực sự
            if (!value.isEmpty()) {
                query.append('&').append(name).append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }

        /**
         * Cắt khoảng trắng hai đầu chuỗi; trả về chuỗi rỗng nếu đầu vào là {@code null}.
         */
        private static String trimToEmpty(String value) {
            // Quy đổi null sang chuỗi rỗng để tránh NullPointerException khi so sánh
            return value == null ? "" : value.trim();
        }

        /**
         * Chuẩn hóa chuỗi tiếng Việt về chữ thường không dấu (chuyển cả {@code đ/Đ} thành {@code d})
         * phục vụ tìm kiếm gần đúng theo tên hoặc MSSV.
         */
        private static String normalizeText(String value) {
            // Trả về chuỗi rỗng nếu giá trị cần chuẩn hóa là null
            if (value == null) {
                return "";
            }
            // Tách tổ hợp dấu Unicode (NFD), xóa toàn bộ dấu thanh/mũ, đổi đ/Đ và chuyển về chữ thường
            return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .replace('đ', 'd').replace('Đ', 'D')
                    .toLowerCase(Locale.ROOT);
        }

        /**
         * Xác thực giá trị tham số {@code gradingStatus} theo whitelist; trả về chuỗi rỗng nếu không hợp lệ.
         */
        private static String parseGradingStatus(String value) {
            String candidate = trimToEmpty(value);
            // Chỉ chấp nhận 4 mã trạng thái chấm điểm hợp lệ; các giá trị khác quy về không lọc ("")
            return switch (candidate) {
                case "ELIGIBLE", "GRADED", "UNGRADED", "INELIGIBLE" -> candidate;
                default -> "";
            };
        }
    }
}
