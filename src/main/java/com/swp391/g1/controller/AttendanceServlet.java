package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityParticipantResponseDTO;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.dto.response.ParticipantCandidateDTO;
import com.swp391.g1.model.Enum.ApprovalStatus;
import com.swp391.g1.model.Enum.ParticipantStatus;
import com.swp391.g1.model.Student;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IActivityCollaborationInvitationService;
import com.swp391.g1.service.IActivityParticipantService;
import com.swp391.g1.service.IActivityTargetParticipantService;
import com.swp391.g1.service.IExtracurricularActivityService;
import com.swp391.g1.service.IStudentService;
import com.swp391.g1.service.impl.ActivityCollaborationInvitationServiceImpl;
import com.swp391.g1.service.impl.ActivityParticipantServiceImpl;
import com.swp391.g1.service.impl.ActivityTargetParticipantServiceImpl;
import com.swp391.g1.service.impl.ExtracurricularActivityServiceImpl;
import com.swp391.g1.service.impl.StudentServiceImpl;
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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Servlet xử lý nghiệp vụ điểm danh, chấm điểm đánh giá tham gia và quản lý danh sách sinh viên
 * tham gia hoạt động ngoại khóa (hỗ trợ cả giao diện quản trị/cán bộ và chế độ sinh viên tự xem).
 */
@WebServlet(name = "AttendanceServlet", urlPatterns = {"/attendance"})
public class AttendanceServlet extends HttpServlet {

    private static final String STATUS_PARAM_PREFIX = "status_";
    private static final Set<String> POST_ACTIONS = Set.of(
            "save", "saveAjax", "saveStudentSession", "batchGrade", "batchGradeAjax", "saveGradeAjax",
            "addStudents", "addGroup", "remove");
    private static final Set<String> AJAX_ACTIONS = Set.of("saveAjax", "batchGradeAjax", "saveGradeAjax");
    private static final Set<String> ADD_ACTIONS = Set.of("addStudents", "addGroup");
    private static final List<Integer> PAGE_SIZES = List.of(30, 40, 50);
    private static final int DEFAULT_PAGE_SIZE = 30;

    private final IExtracurricularActivityService activityService;
    private final IActivityCollaborationInvitationService invitationService;
    private final IActivityParticipantService participantService;
    private final IStudentService studentService;
    private final IActivityTargetParticipantService targetParticipantService;

    /**
     * Khởi tạo servlet điểm danh và các service cần thiết
     * (hoạt động, lời mời phối hợp, danh sách tham gia, sinh viên và đối tượng tham gia).
     */
    public AttendanceServlet() {
        this.activityService = new ExtracurricularActivityServiceImpl();
        this.invitationService = new ActivityCollaborationInvitationServiceImpl();
        this.participantService = new ActivityParticipantServiceImpl();
        this.studentService = new StudentServiceImpl();
        this.targetParticipantService = new ActivityTargetParticipantServiceImpl();
    }

    /**
     * Cổng kiểm tra xác thực và phân quyền chung trước khi điều hướng tới doGet/doPost:
     * - Chưa đăng nhập: chuyển hướng về trang /login.
     * - Không có quyền điểm danh (ADMIN, STAFF, PARTNER_STAFF) và cũng không phải sinh viên xem điểm danh của chính mình (GET/HEAD): trả lỗi 403.
     * - Hợp lệ: gọi super.service(...) để thực thi doGet hoặc doPost tương ứng.
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        // Nếu chưa đăng nhập, chuyển hướng về trang /login.
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        boolean canTakeAttendance = AccessPolicy.canTakeAttendance(currentUser);
        boolean isGetOrHead = "GET".equalsIgnoreCase(request.getMethod())
                || "HEAD".equalsIgnoreCase(request.getMethod());
        boolean canStudentViewOwn = isGetOrHead && AccessPolicy.canViewOwnAttendance(currentUser);
        // Nếu không có quyền điểm danh và cũng không phải sinh viên xem điểm danh của chính mình, trả lỗi 403.
        if (!canTakeAttendance && !canStudentViewOwn) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ tài khoản ADMIN, STAFF hoặc PARTNER_STAFF mới được điểm danh sinh viên.");
            return;
        }
        super.service(request, response);
    }

    /**
     * Xử lý yêu cầu GET tải trang điểm danh:
     * - Nếu là sinh viên: chuyển sang handleStudentOwnDetailGet để xem chi tiết điểm danh cá nhân.
     * - Nếu là cán bộ quản lý: lấy danh sách hoạt động được phép xem; điều hướng sang xem chi tiết 1 sinh viên (action=detail)
     *   hoặc tải danh sách sinh viên của hoạt động/buổi đã chọn, lọc, phân trang, nạp danh sách ứng viên thêm mới và render list.jsp.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            CurrentUser currentUser = AuthContext.getCurrentUser(request);
            boolean canTakeAttendanceRole = AccessPolicy.canTakeAttendance(currentUser);
            boolean isStudent = !canTakeAttendanceRole && AccessPolicy.canViewOwnAttendance(currentUser);
            request.setAttribute("isStudent", isStudent);

            // Nếu người dùng là sinh viên (chỉ có quyền xem điểm danh của chính mình), điều hướng sang luồng xem chi tiết cá nhân.
            if (isStudent) {
                handleStudentOwnDetailGet(request, response, currentUser);
                return;
            }

            List<ExtracurricularActivityResponseDTO> activities = getVisibleActivities(currentUser);
            request.setAttribute("activities", activities);

            int activityId = parseId(request.getParameter("activityId"));
            String action = request.getParameter("action");
            // Nếu yêu cầu xem chi tiết điểm danh của một sinh viên cụ thể (action=detail), chuyển sang hàm xử lý riêng.
            if ("detail".equals(action)) {
                handleParticipantDetailGet(request, response, activities, activityId);
                return;
            }
            // Form thêm sinh viên nằm ngay trong trang danh sách; action=add chỉ bật sẵn panel thêm sinh viên.
            boolean openAddPanel = "add".equals(action);
            // Nếu yêu cầu mở panel thêm sinh viên nhưng chưa có activityId hợp lệ, quay về trang điểm danh mặc định.
            if (openAddPanel && activityId <= 0) {
                response.sendRedirect(request.getContextPath() + "/attendance");
                return;
            }
            // Khi đã chọn hoạt động hợp lệ: kiểm tra quyền, tải danh sách điểm danh, thống kê, lọc và phân trang.
            if (activityId > 0) {
                ExtracurricularActivityResponseDTO selectedActivity = findById(activities, activityId);
                // Nếu hoạt động không thuộc phạm vi được phép điểm danh của người dùng, trả lỗi 403.
                if (selectedActivity == null) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Bạn không có quyền điểm danh hoạt động này.");
                    return;
                }
                // Chuẩn hóa số buổi điểm danh đang chọn về phạm vi hợp lệ [1..totalSessions].
                int totalSessions = Math.max(1, selectedActivity.getAttendanceSessionCount());
                int requestedSession = parseId(request.getParameter("sessionNumber"));
                int selectedSessionNumber = requestedSession <= 0
                        ? 1
                        : Math.min(requestedSession, totalSessions);

                List<ActivityParticipantResponseDTO> allParticipants =
                        participantService.getParticipants(activityId, selectedSessionNumber);
                Map<ParticipantStatus, Long> statusCounts = countByStatus(allParticipants);
                Set<String> departments = new TreeSet<>();
                Set<String> programs = new TreeSet<>();
                Set<String> majors = new TreeSet<>();
                Set<String> classes = new TreeSet<>();
                // Duyệt toàn bộ sinh viên tham gia để thu thập các giá trị Khoa, CTĐT, Ngành, Lớp (không trùng, tự sắp xếp) cho dropdown bộ lọc.
                for (ActivityParticipantResponseDTO participant : allParticipants) {
                    addIfPresent(departments, participant.getDepartmentName());
                    addIfPresent(programs, participant.getAcademicProgramName());
                    addIfPresent(majors, participant.getMajorName());
                    addIfPresent(classes, participant.getMainClassCode());
                }

                ParticipantFilter filter = ParticipantFilter.from(request);
                List<ActivityParticipantResponseDTO> filteredParticipants = new ArrayList<>();
                // Duyệt toàn bộ sinh viên tham gia và lọc giữ lại những bản ghi khớp với bộ lọc hiện tại.
                for (ActivityParticipantResponseDTO participant : allParticipants) {
                    // Nếu sinh viên thỏa mãn toàn bộ tiêu chí tìm kiếm/bộ lọc, đưa vào danh sách kết quả.
                    if (filter.matches(participant)) {
                        filteredParticipants.add(participant);
                    }
                }

                // Tính toán tham số phân trang (tổng số trang, trang hiện tại, vị trí cắt) và lấy danh sách sinh viên của trang hiện tại.
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
                request.setAttribute("selectedSessionNumber", selectedSessionNumber);
                request.setAttribute("totalSessions", totalSessions);
                request.setAttribute("participants", participants);
                request.setAttribute("filteredParticipantCount", filteredCount);
                request.setAttribute("pageSize", pageSize);
                request.setAttribute("pageSizes", PAGE_SIZES);
                request.setAttribute("currentPage", currentPage);
                request.setAttribute("totalPages", totalPages);
                // Số thứ tự bản ghi đầu tiên trên trang (bằng 0 nếu không có kết quả sau lọc, ngược lại bắt đầu từ 1).
                request.setAttribute("firstParticipantNumber", filteredCount == 0 ? 0 : fromIndex + 1);
                request.setAttribute("lastParticipantNumber", toIndex);
                request.setAttribute("pagingQuery", pagingQuery(currentPage, pageSize));
                request.setAttribute("totalParticipantCount", allParticipants.size());
                request.setAttribute("statusCounts", statusCounts);
                request.setAttribute("canTakeAttendance", participantService.canTakeAttendance(selectedActivity));
                request.setAttribute("departments", departments);
                request.setAttribute("academicPrograms", programs);
                request.setAttribute("majors", majors);
                request.setAttribute("mainClasses", classes);
                setFilterAttributes(request, filter);
                boolean canManageParticipants = AccessPolicy.canManageParticipants(currentUser)
                        && participantService.canManageParticipants(selectedActivity);
                request.setAttribute("canManageParticipants", canManageParticipants);
                // Nếu tài khoản có quyền và hoạt động cho phép quản lý danh sách, nạp danh sách ứng viên thêm mới.
                if (canManageParticipants) {
                    setCandidateAttributes(request, participantService.getCandidates(activityId));
                    request.setAttribute("openAddPanel", openAddPanel);
                // Nếu không được phép quản lý nhưng người dùng yêu cầu mở panel thêm sinh viên (action=add).
                } else if (openAddPanel) {
                    // Trường hợp tài khoản không thuộc vai trò được phép thêm/xóa sinh viên: trả lỗi 403.
                    if (!AccessPolicy.canManageParticipants(currentUser)) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                                "Bạn không có quyền thêm hoặc xóa sinh viên trong danh sách tham gia.");
                        return;
                    }
                    // Trường hợp có quyền vai trò nhưng hoạt động đã hủy: báo lỗi vào session và redirect về trang danh sách.
                    request.getSession().setAttribute("errorMessage",
                            "Không thể thay đổi danh sách sinh viên của hoạt động đã hủy.");
                    response.sendRedirect(request.getContextPath() + "/attendance?activityId=" + activityId
                            + "&sessionNumber=" + selectedSessionNumber
                            + filter.toQuery() + pagingQuery(currentPage, pageSize));
                    return;
                }
            }
            request.setAttribute("participantStatuses", ParticipantStatus.values());
            request.getRequestDispatcher("/WEB-INF/views/attendance/list.jsp").forward(request, response);
        // Bắt các lỗi hệ thống ngoài dự kiến trong quá trình tải trang điểm danh và chuyển sang trang lỗi 500.
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("errorMessage", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    /**
     * Xử lý GET cho vai trò sinh viên khi tự xem chi tiết điểm danh của bản thân.
     * Kiểm tra quyền chỉ xem dữ liệu của chính mình, lấy danh sách hoạt động đã tham gia,
     * thông tin điểm danh theo từng buổi và lịch sử điểm danh rồi forward sang detail.jsp.
     */
    private void handleStudentOwnDetailGet(HttpServletRequest request, HttpServletResponse response,
            CurrentUser currentUser) throws ServletException, IOException {
        String action = request.getParameter("action");
        // Chỉ cho phép sinh viên truy cập mặc định hoặc action=detail; chặn mọi action quản trị khác.
        if (action != null && !action.isBlank() && !"detail".equals(action)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Sinh viên chỉ được phép xem giao diện chi tiết điểm danh của chính mình.");
            return;
        }

        Student me = studentService.getStudentByAccountId(currentUser.getId());
        // Nếu tài khoản đang đăng nhập chưa liên kết hồ sơ sinh viên, từ chối truy cập (403).
        if (me == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Không tìm thấy hồ sơ sinh viên tương ứng với tài khoản đang đăng nhập.");
            return;
        }
        int myStudentId = me.getId();
        int requestedStudentId = parseId(request.getParameter("studentId"));
        // Chặn trường hợp sinh viên cố sửa tham số studentId trên URL để xem dữ liệu của sinh viên khác.
        if (requestedStudentId > 0 && requestedStudentId != myStudentId) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Sinh viên chỉ được phép xem chi tiết điểm danh của chính mình.");
            return;
        }

        List<ExtracurricularActivityResponseDTO> activities = getStudentParticipatedActivities(myStudentId);
        request.setAttribute("activities", activities);
        request.setAttribute("studentProfile", me);
        request.setAttribute("allParticipants", List.of());
        request.setAttribute("canTakeAttendance", false);
        request.setAttribute("participantStatuses", ParticipantStatus.values());

        int activityId = parseId(request.getParameter("activityId"));
        int requestedParticipantId = parseId(request.getParameter("participantId"));

        // Trường hợp sinh viên chưa tham gia hoạt động ngoại khóa nào.
        if (activities.isEmpty()) {
            // Nếu chưa tham gia hoạt động nào nhưng URL lại truyền activityId hoặc participantId cụ thể -> trả lỗi 403.
            if (activityId > 0 || requestedParticipantId > 0) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bạn không có quyền xem chi tiết điểm danh của hoạt động này.");
                return;
            }
            request.setAttribute("sessionAttendances", List.of());
            request.setAttribute("attendanceHistory", List.of());
            request.getRequestDispatcher("/WEB-INF/views/attendance/detail.jsp").forward(request, response);
            return;
        }

        // Nếu chưa truyền activityId trên URL: tự suy ra từ participantId hoặc chọn hoạt động đầu tiên.
        if (activityId <= 0) {
            // Khi có truyền participantId, dò trong các hoạt động sinh viên tham gia để tìm đúng hoạt động tương ứng.
            if (requestedParticipantId > 0) {
                // Duyệt qua từng hoạt động mà sinh viên tham gia.
                for (ExtracurricularActivityResponseDTO act : activities) {
                    // Duyệt danh sách người tham gia của từng hoạt động để đối chiếu studentId và participantId.
                    for (ActivityParticipantResponseDTO p : participantService.getParticipants(act.getId(), 1)) {
                        // Khớp đúng bản ghi của chính sinh viên này -> ghi nhận activityId và thoát vòng lặp trong.
                        if (p.getStudentId() == myStudentId && p.getParticipantId() == requestedParticipantId) {
                            activityId = act.getId();
                            break;
                        }
                    }
                    // Đã xác định được hoạt động tương ứng thì dừng luôn vòng lặp ngoài.
                    if (activityId > 0) {
                        break;
                    }
                }
                // Không tìm thấy bản ghi participantId nào thuộc về sinh viên đang đăng nhập -> trả lỗi 403.
                if (activityId <= 0) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Sinh viên chỉ được phép xem chi tiết điểm danh của chính mình.");
                    return;
                }
            // Trường hợp không truyền cả activityId lẫn participantId -> chọn mặc định hoạt động đầu tiên.
            } else {
                activityId = activities.get(0).getId();
            }
        }

        ExtracurricularActivityResponseDTO selectedActivity = findById(activities, activityId);
        // Nếu activityId yêu cầu không nằm trong danh sách hoạt động sinh viên tham gia -> trả lỗi 403.
        if (selectedActivity == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không có quyền xem chi tiết điểm danh của hoạt động này.");
            return;
        }

        // Chuẩn hóa số buổi điểm danh đang chọn về phạm vi hợp lệ [1..totalSessions].
        int totalSessions = Math.max(1, selectedActivity.getAttendanceSessionCount());
        int requestedSession = parseId(request.getParameter("sessionNumber"));
        int selectedSessionNumber = requestedSession <= 0
                ? 1
                : Math.min(requestedSession, totalSessions);

        ActivityParticipantResponseDTO myParticipant = null;
        // Duyệt danh sách người tham gia của hoạt động ở buổi hiện tại để lấy bản ghi của chính sinh viên.
        for (ActivityParticipantResponseDTO p : participantService.getParticipants(activityId, selectedSessionNumber)) {
            // Tìm thấy bản ghi có studentId trùng với sinh viên đang đăng nhập -> lưu lại và dừng duyệt.
            if (p.getStudentId() == myStudentId) {
                myParticipant = p;
                break;
            }
        }
        // Nếu không tồn tại bản ghi của sinh viên trong danh sách điểm danh của hoạt động -> trả lỗi 403.
        if (myParticipant == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không nằm trong danh sách điểm danh của hoạt động này.");
            return;
        }
        // Nếu có truyền participantId nhưng không trùng với bản ghi của chính sinh viên -> từ chối truy cập (403).
        if (requestedParticipantId > 0 && myParticipant.getParticipantId() != requestedParticipantId) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Sinh viên chỉ được phép xem chi tiết điểm danh của chính mình.");
            return;
        }

        // Xác định sinh viên có được phép mở liên kết sang trang chi tiết hoạt động hay không (công khai, đã duyệt và đúng đối tượng).
        boolean canOpenActivityDetail = selectedActivity.isPublic()
                && selectedActivity.getApprovalStatus() == ApprovalStatus.APPROVED
                && targetParticipantService.isStudentEligible(activityId, myStudentId);

        request.setAttribute("selectedActivity", selectedActivity);
        request.setAttribute("selectedActivityId", activityId);
        request.setAttribute("selectedSessionNumber", selectedSessionNumber);
        request.setAttribute("totalSessions", totalSessions);
        request.setAttribute("participant", myParticipant);
        request.setAttribute("canOpenActivityDetail", canOpenActivityDetail);
        request.setAttribute("sessionAttendances",
                participantService.getSessionAttendances(activityId, myParticipant.getParticipantId(), totalSessions));
        request.setAttribute("attendanceHistory",
                participantService.getAttendanceHistory(activityId, myParticipant.getParticipantId()));
        request.getRequestDispatcher("/WEB-INF/views/attendance/detail.jsp").forward(request, response);
    }

    /**
     * Xử lý GET xem chi tiết điểm danh của một sinh viên cụ thể (action=detail) dành cho cán bộ/quản trị viên.
     * Nạp thông tin hoạt động, danh sách sinh viên để chuyển nhanh, trạng thái từng buổi và lịch sử điểm danh rồi forward sang detail.jsp.
     */
    private void handleParticipantDetailGet(HttpServletRequest request, HttpServletResponse response,
            List<ExtracurricularActivityResponseDTO> activities, int activityId)
            throws ServletException, IOException {
        // Nếu thiếu hoặc sai activityId, chuyển hướng về trang danh sách điểm danh chính.
        if (activityId <= 0) {
            response.sendRedirect(request.getContextPath() + "/attendance");
            return;
        }
        ExtracurricularActivityResponseDTO selectedActivity = findById(activities, activityId);
        // Nếu cán bộ không có quyền xem/điểm danh hoạt động này -> trả lỗi 403.
        if (selectedActivity == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không có quyền xem chi tiết điểm danh của hoạt động này.");
            return;
        }
        // Chuẩn hóa số buổi điểm danh đang chọn về phạm vi hợp lệ [1..totalSessions].
        int totalSessions = Math.max(1, selectedActivity.getAttendanceSessionCount());
        int requestedSession = parseId(request.getParameter("sessionNumber"));
        int selectedSessionNumber = requestedSession <= 0
                ? 1
                : Math.min(requestedSession, totalSessions);

        List<ActivityParticipantResponseDTO> allParticipants =
                participantService.getParticipants(activityId, selectedSessionNumber);
        int participantId = parseId(request.getParameter("participantId"));
        int studentId = parseId(request.getParameter("studentId"));

        ActivityParticipantResponseDTO selectedParticipant = null;
        // Duyệt danh sách người tham gia để tìm đúng sinh viên cần xem (ưu tiên theo participantId, sau đó theo studentId).
        for (ActivityParticipantResponseDTO p : allParticipants) {
            // Khớp participantId hoặc (không truyền participantId nhưng khớp studentId) -> chọn và dừng vòng lặp.
            if ((participantId > 0 && p.getParticipantId() == participantId)
                    || (participantId <= 0 && studentId > 0 && p.getStudentId() == studentId)) {
                selectedParticipant = p;
                break;
            }
        }
        // Nếu không tìm thấy theo ID yêu cầu nhưng hoạt động có sinh viên, chọn mặc định sinh viên đầu tiên.
        if (selectedParticipant == null && !allParticipants.isEmpty()) {
            selectedParticipant = allParticipants.get(0);
        }
        // Nếu hoạt động chưa có sinh viên nào trong danh sách, lưu thông báo lỗi và redirect về trang danh sách.
        if (selectedParticipant == null) {
            request.getSession().setAttribute("errorMessage",
                    "Không tìm thấy sinh viên trong danh sách điểm danh của hoạt động này.");
            response.sendRedirect(request.getContextPath() + "/attendance?activityId=" + activityId
                    + "&sessionNumber=" + selectedSessionNumber);
            return;
        }

        request.setAttribute("selectedActivity", selectedActivity);
        request.setAttribute("selectedActivityId", activityId);
        request.setAttribute("selectedSessionNumber", selectedSessionNumber);
        request.setAttribute("totalSessions", totalSessions);
        request.setAttribute("participant", selectedParticipant);
        request.setAttribute("allParticipants", allParticipants);
        request.setAttribute("sessionAttendances",
                participantService.getSessionAttendances(activityId, selectedParticipant.getParticipantId(), totalSessions));
        request.setAttribute("attendanceHistory",
                participantService.getAttendanceHistory(activityId, selectedParticipant.getParticipantId()));
        request.setAttribute("canTakeAttendance", participantService.canTakeAttendance(selectedActivity));
        request.setAttribute("participantStatuses", ParticipantStatus.values());
        request.getRequestDispatcher("/WEB-INF/views/attendance/detail.jsp").forward(request, response);
    }

    /**
     * Xử lý các yêu cầu POST từ trang điểm danh:
     * - Kiểm tra action hợp lệ, quyền điểm danh, quyền quản lý sinh viên và quyền truy cập hoạt động.
     * - Nếu là AJAX (saveAjax, batchGradeAjax, saveGradeAjax): gọi hàm trả JSON tương ứng.
     * - Nếu là submit form (save, saveStudentSession, batchGrade, addStudents, addGroup, remove):
     *   thực hiện thao tác qua service, lưu thông báo vào session và redirect về trang phù hợp (giữ nguyên bộ lọc & phân trang).
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        int activityId = parseId(request.getParameter("activityId"));
        int sessionNumber = Math.max(1, parseId(request.getParameter("sessionNumber")));
        String action = request.getParameter("action");
        boolean isAjax = action != null && AJAX_ACTIONS.contains(action);
        // Set.of(...).contains(null) sẽ ném ngoại lệ nên cần kiểm tra action == null trước khi đối chiếu POST_ACTIONS và activityId.
        if (action == null || !POST_ACTIONS.contains(action) || activityId <= 0) {
            // Nếu là yêu cầu AJAX, trả JSON lỗi 400.
            if (isAjax) {
                json(response, HttpServletResponse.SC_BAD_REQUEST,
                        "{\"ok\":false,\"error\":\"Yêu cầu điểm danh không hợp lệ.\"}");
            // Nếu là submit form thường, chuyển hướng về trang /attendance mặc định.
            } else {
                response.sendRedirect(request.getContextPath() + "/attendance");
            }
            return;
        }

        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        // Kiểm tra người dùng có thuộc vai trò được phép điểm danh (ADMIN, STAFF, PARTNER_STAFF) hay không.
        if (!AccessPolicy.canTakeAttendance(currentUser)) {
            // Phản hồi lỗi 403 dạng JSON cho AJAX.
            if (isAjax) {
                json(response, HttpServletResponse.SC_FORBIDDEN,
                        "{\"ok\":false,\"error\":\"Chỉ tài khoản ADMIN, STAFF hoặc PARTNER_STAFF mới được điểm danh sinh viên.\"}");
            // Phản hồi mã lỗi HTTP 403 cho request submit form thường.
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Chỉ tài khoản ADMIN, STAFF hoặc PARTNER_STAFF mới được điểm danh sinh viên.");
            }
            return;
        }
        // Kiểm tra riêng quyền thêm/xóa sinh viên khỏi danh sách tham gia với các action quản lý danh sách.
        if ((ADD_ACTIONS.contains(action) || "remove".equals(action))
                && !AccessPolicy.canManageParticipants(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không có quyền thêm hoặc xóa sinh viên trong danh sách tham gia.");
            return;
        }
        // Kiểm tra hoạt động mục tiêu có nằm trong phạm vi quản lý/điểm danh của tài khoản hiện tại hay không.
        if (findById(getVisibleActivities(currentUser), activityId) == null) {
            // Nếu là AJAX, trả JSON lỗi 403.
            if (isAjax) {
                json(response, HttpServletResponse.SC_FORBIDDEN,
                        "{\"ok\":false,\"error\":\"Bạn không có quyền điểm danh hoạt động này.\"}");
            // Nếu là submit form thường, trả lỗi HTTP 403.
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bạn không có quyền điểm danh hoạt động này.");
            }
            return;
        }

        // Điều hướng nhánh AJAX lưu trạng thái điểm danh tức thời trên bảng.
        if ("saveAjax".equals(action)) {
            saveAttendanceAjax(request, response, activityId, sessionNumber, currentUser);
            return;
        }
        // Điều hướng nhánh AJAX chấm điểm đánh giá tham gia (hàng loạt hoặc từng sinh viên).
        if ("batchGradeAjax".equals(action) || "saveGradeAjax".equals(action)) {
            saveGradesAjax(request, response, activityId, sessionNumber, currentUser);
            return;
        }

        HttpSession session = request.getSession();
        boolean failed = false;
        try {
            // Điều phối xử lý theo loại thao tác form POST được gửi lên.
            switch (action) {
                // Lưu điểm danh hàng loạt cho buổi hiện tại dựa trên các tham số status_<participantId>.
                case "save" -> {
                    int changed = participantService.saveAttendance(activityId, sessionNumber, readStatuses(request), currentUser);
                    // Phân nhánh thông báo giữa trường hợp không có thay đổi và trường hợp có bản ghi cập nhật.
                    session.setAttribute("successMessage", changed == 0
                            ? "Không có thay đổi nào về trạng thái điểm danh buổi " + sessionNumber + "."
                            : "Đã cập nhật điểm danh buổi " + sessionNumber + " cho " + changed + " sinh viên.");
                }
                // Cập nhật trạng thái điểm danh cho một buổi cụ thể từ màn hình chi tiết của một sinh viên.
                case "saveStudentSession" -> {
                    int participantId = parseId(request.getParameter("participantId"));
                    int targetSession = Math.max(1, parseId(request.getParameter("targetSessionNumber")));
                    String newStatus = request.getParameter("attendanceStatusValue");
                    Map<Integer, String> statusMap = new LinkedHashMap<>();
                    // Chỉ đưa vào map cập nhật khi có participantId hợp lệ và giá trị trạng thái mới khác null.
                    if (participantId > 0 && newStatus != null) {
                        statusMap.put(participantId, newStatus);
                    }
                    int changed = participantService.saveAttendance(activityId, targetSession, statusMap, currentUser);
                    // Ghi thông báo tương ứng xem buổi điểm danh đó có thay đổi trạng thái hay không.
                    session.setAttribute("successMessage", changed == 0
                            ? "Trạng thái điểm danh buổi " + targetSession + " không thay đổi."
                            : "Đã cập nhật điểm danh buổi " + targetSession + " của sinh viên.");
                    response.sendRedirect(request.getContextPath() + "/attendance?action=detail&activityId=" + activityId
                            + "&participantId=" + participantId + "&sessionNumber=" + sessionNumber);
                    return;
                }
                // Chấm điểm đánh giá tham gia và lưu nhận xét hàng loạt cho các sinh viên đã chọn.
                case "batchGrade" -> {
                    List<Integer> participantIds = parseIds(request.getParameterValues("participantId"));
                    BigDecimal evalScore = parseScore(request.getParameter("evalScore"));
                    String feedback = request.getParameter("feedback");
                    int graded = participantService.saveParticipationGrades(
                            activityId, participantIds, evalScore, feedback, currentUser);
                    // Thông báo số sinh viên có mặt được chấm điểm (hoặc cảnh báo nếu không có sinh viên đủ điều kiện).
                    session.setAttribute("successMessage", graded == 0
                            ? "Không có sinh viên có mặt nào trong danh sách chọn để chấm điểm tham gia."
                            : "Đã chấm điểm đánh giá tham gia cho " + graded + " sinh viên.");
                }
                // Thêm danh sách sinh viên được chọn thủ công vào hoạt động (mặc định registrationBy=1 nếu thiếu currentUser).
                case "addStudents" -> setAddedMessage(session, participantService.addStudents(activityId,
                        parseIds(request.getParameterValues("studentId")),
                        currentUser == null ? 1 : currentUser.getId()));
                // Thêm hàng loạt sinh viên theo nhóm Khoa / Ngành / CTĐT / Lớp (mặc định registrationBy=1 nếu thiếu currentUser).
                case "addGroup" -> setAddedMessage(session, participantService.addStudentsByGroup(activityId,
                        parseId(request.getParameter("groupDepartmentId")),
                        parseId(request.getParameter("groupMajorId")),
                        parseId(request.getParameter("groupProgramId")),
                        parseId(request.getParameter("groupClassId")),
                        currentUser == null ? 1 : currentUser.getId()));
                // Xóa các sinh viên được chọn khỏi danh sách tham gia (chỉ xóa được những sinh viên chưa từng điểm danh).
                case "remove" -> {
                    List<Integer> participantIds = parseIds(request.getParameterValues("participantId"));
                    int removed = participantService.removeParticipants(activityId, participantIds);
                    String message = "Đã xóa " + removed + " sinh viên khỏi danh sách.";
                    // Nếu số lượng xóa ít hơn số lượng yêu cầu, bổ sung thông báo về số sinh viên đã điểm danh bị bỏ qua.
                    if (removed < participantIds.size()) {
                        message += " Bỏ qua " + (participantIds.size() - removed)
                                + " sinh viên đã được điểm danh.";
                    }
                    session.setAttribute("successMessage", message);
                }
                // Phòng trường hợp action không khớp bất kỳ thao tác hợp lệ nào.
                default -> throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
        // Bắt lỗi nghiệp vụ hoặc dữ liệu đầu vào không hợp lệ từ tầng service để hiển thị thông báo rõ ràng.
        } catch (IllegalArgumentException e) {
            session.setAttribute("errorMessage", e.getMessage());
            failed = true;
        // Bắt các lỗi hệ thống ngoài dự kiến khi cập nhật danh sách hoặc điểm danh.
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Đã xảy ra lỗi khi cập nhật danh sách: " + e.getMessage());
            failed = true;
        }
        // Nếu thao tác lưu từ màn hình chi tiết sinh viên bị lỗi (chưa return ở case trên), redirect ngược về đúng trang detail.
        if ("saveStudentSession".equals(action)) {
            int participantId = parseId(request.getParameter("participantId"));
            response.sendRedirect(request.getContextPath() + "/attendance?action=detail&activityId=" + activityId
                    + "&participantId=" + participantId + "&sessionNumber=" + sessionNumber);
            return;
        }
        // Nếu thao tác thêm sinh viên thất bại thì mở lại panel thêm sinh viên (action=add); các trường hợp khác quay về trang danh sách.
        // Cả hai nhánh đều giữ nguyên các tham số bộ lọc và phân trang người dùng đang thao tác.
        boolean backToAddPage = failed && ADD_ACTIONS.contains(action);
        response.sendRedirect(request.getContextPath() + "/attendance?"
                + (backToAddPage ? "action=add&" : "") + "activityId=" + activityId
                + "&sessionNumber=" + sessionNumber
                + ParticipantFilter.from(request).toQuery()
                + pagingQuery(parseId(request.getParameter("page")), parsePageSize(request.getParameter("pageSize"))));
    }

    /**
     * Xử lý lưu trạng thái điểm danh bằng AJAX (action=saveAjax).
     * Lưu thay đổi vào DB, đếm lại số lượng theo từng trạng thái và trả về JSON chứa thống kê cùng điểm số cập nhật.
     */
    private void saveAttendanceAjax(HttpServletRequest request, HttpServletResponse response,
            int activityId, int sessionNumber, CurrentUser currentUser) throws IOException {
        try {
            Map<Integer, String> statuses = readStatuses(request);
            int changed = participantService.saveAttendance(activityId, sessionNumber, statuses, currentUser);
            List<ActivityParticipantResponseDTO> allParticipants = participantService.getParticipants(activityId, sessionNumber);
            Map<ParticipantStatus, Long> counts = countByStatus(allParticipants);
            // Dựng JSON trả về gồm: số bản ghi thay đổi, thống kê đếm theo trạng thái và điểm mới của từng sinh viên cập nhật.
            StringBuilder body = new StringBuilder("{\"ok\":true,\"changed\":").append(changed).append(",\"counts\":{");
            boolean first = true;
            // Duyệt tất cả trạng thái điểm danh để xuất cặp "STATUS":count vào object JSON counts.
            for (ParticipantStatus status : ParticipantStatus.values()) {
                // Thêm dấu phẩy ngăn cách từ phần tử thứ hai trở đi.
                if (!first) {
                    body.append(',');
                }
                first = false;
                body.append('"').append(status.name()).append("\":").append(counts.getOrDefault(status, 0L));
            }
            body.append("},\"participants\":{");
            appendParticipantScoresJson(body, allParticipants, statuses.keySet());
            body.append("}}");
            json(response, HttpServletResponse.SC_OK, body.toString());
        // Lỗi dữ liệu đầu vào hoặc quy tắc nghiệp vụ: trả mã HTTP 400 kèm nội dung lỗi dạng JSON.
        } catch (IllegalArgumentException e) {
            json(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"ok\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        // Lỗi hệ thống không mong muốn khi lưu điểm danh: trả mã HTTP 500 dạng JSON.
        } catch (Exception e) {
            json(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"ok\":false,\"error\":\"Đã xảy ra lỗi khi lưu điểm danh.\"}");
        }
    }

    /**
     * Xử lý chấm điểm đánh giá tham gia và nhận xét bằng AJAX (action=batchGradeAjax / saveGradeAjax).
     * Lưu điểm qua service rồi trả về JSON chứa số bản ghi đã cập nhật và điểm mới nhất của các sinh viên liên quan.
     */
    private void saveGradesAjax(HttpServletRequest request, HttpServletResponse response,
            int activityId, int sessionNumber, CurrentUser currentUser) throws IOException {
        try {
            List<Integer> participantIds = parseIds(request.getParameterValues("participantId"));
            BigDecimal evalScore = parseScore(request.getParameter("evalScore"));
            String feedback = request.getParameter("feedback");
            int updated = participantService.saveParticipationGrades(
                    activityId, participantIds, evalScore, feedback, currentUser);
            // Tải lại danh sách người tham gia để lấy điểm đã tính lại và dựng JSON trả về cho client cập nhật DOM.
            List<ActivityParticipantResponseDTO> allParticipants = participantService.getParticipants(activityId, sessionNumber);
            StringBuilder body = new StringBuilder("{\"ok\":true,\"updated\":").append(updated)
                    .append(",\"participants\":{");
            appendParticipantScoresJson(body, allParticipants, Set.copyOf(participantIds));
            body.append("}}");
            json(response, HttpServletResponse.SC_OK, body.toString());
        // Lỗi dữ liệu hoặc quy tắc chấm điểm (điểm ngoài khoảng 0-5, feedback quá dài...): trả HTTP 400 dạng JSON.
        } catch (IllegalArgumentException e) {
            json(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"ok\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        // Lỗi hệ thống không mong muốn khi lưu điểm tham gia: trả HTTP 500 dạng JSON.
        } catch (Exception e) {
            json(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"ok\":false,\"error\":\"Đã xảy ra lỗi khi lưu điểm tham gia.\"}");
        }
    }

    /**
     * Nối thông tin điểm số, trạng thái có mặt, quyền xóa và nhận xét của các sinh viên mục tiêu vào chuỗi JSON trả về cho AJAX.
     */
    private void appendParticipantScoresJson(StringBuilder body,
            List<ActivityParticipantResponseDTO> allParticipants, Set<Integer> targetIds) {
        boolean first = true;
        // Duyệt danh sách người tham gia để serialize các trường điểm/trạng thái của những sinh viên thuộc targetIds.
        for (ActivityParticipantResponseDTO p : allParticipants) {
            // Bỏ qua các sinh viên không nằm trong tập ID mục tiêu (khi targetIds có giới hạn cụ thể).
            if (targetIds != null && !targetIds.isEmpty() && !targetIds.contains(p.getParticipantId())) {
                continue;
            }
            // Thêm dấu phẩy ngăn cách giữa các object sinh viên trong JSON.
            if (!first) {
                body.append(',');
            }
            first = false;
            body.append('"').append(p.getParticipantId()).append("\":{")
                    .append("\"present\":").append(p.isPresentStatus())
                    .append(",\"removable\":").append(p.isRemovable())
                    .append(",\"sessionAttendanceScore\":\"").append(escapeJson(p.getFormattedSessionAttendanceScore())).append('"')
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
     * Chuyển chuỗi điểm đánh giá tham gia sang BigDecimal (hỗ trợ cả dấu phẩy và dấu chấm).
     * Ném IllegalArgumentException nếu giá trị rỗng hoặc không đúng định dạng số.
     */
    private BigDecimal parseScore(String value) {
        // Từ chối khi người dùng để trống giá trị điểm đánh giá.
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập điểm đánh giá tham gia (0 - 5).");
        }
        try {
            // Chuẩn hóa dấu phẩy thập phân thành dấu chấm trước khi chuyển sang BigDecimal.
            return new BigDecimal(value.trim().replace(',', '.'));
        // Chuỗi không đúng định dạng số thực -> ném lỗi nghiệp vụ để hiển thị thông báo thân thiện.
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Điểm đánh giá tham gia không hợp lệ.");
        }
    }

    /**
     * Thống kê số lượng sinh viên theo từng trạng thái điểm danh (ParticipantStatus) trong buổi hiện tại.
     */
    private Map<ParticipantStatus, Long> countByStatus(List<ActivityParticipantResponseDTO> participants) {
        Map<ParticipantStatus, Long> statusCounts = new EnumMap<>(ParticipantStatus.class);
        // Khởi tạo trước bộ đếm bằng 0 cho mọi trạng thái enum để bảo đảm đầy đủ khóa khi xuất ra giao diện/JSON.
        for (ParticipantStatus status : ParticipantStatus.values()) {
            statusCounts.put(status, 0L);
        }
        // Duyệt danh sách sinh viên và cộng dồn số lượng theo trạng thái điểm danh của buổi hiện tại.
        for (ActivityParticipantResponseDTO participant : participants) {
            statusCounts.merge(participant.getSessionAttendanceStatus(), 1L, Long::sum);
        }
        return statusCounts;
    }

    /**
     * Ghi phản hồi HTTP dạng JSON với mã trạng thái HTTP và mã hóa UTF-8.
     */
    private static void json(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body);
    }

    /**
     * Thoát các ký tự đặc biệt (\, ", \r, \n) để chèn chuỗi an toàn vào nội dung JSON.
     */
    private static String escapeJson(String value) {
        // Nếu chuỗi null thì trả về chuỗi rỗng để tránh xuất chữ "null" vào JSON.
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    /**
     * Tạo đoạn query string phân trang (&page=...&pageSize=...) để giữ nguyên trang hiện tại khi điều hướng hoặc redirect.
     */
    private String pagingQuery(int page, int pageSize) {
        return "&page=" + Math.max(page, 1) + "&pageSize=" + pageSize;
    }

    /**
     * Phân tích kích thước trang (pageSize) từ tham số request; nếu không thuộc danh sách hợp lệ thì dùng mặc định (30).
     */
    private int parsePageSize(String value) {
        int pageSize = parseId(value);
        // Nếu kích thước trang thuộc tập cho phép (30, 40, 50) thì sử dụng; ngược lại quay về giá trị mặc định 30.
        return PAGE_SIZES.contains(pageSize) ? pageSize : DEFAULT_PAGE_SIZE;
    }

    /**
     * Đẩy toàn bộ giá trị bộ lọc hiện tại, số bộ lọc đang bật và query string đã mã hóa lên request attributes để JSP hiển thị.
     */
    private void setFilterAttributes(HttpServletRequest request, ParticipantFilter filter) {
        request.setAttribute("keyword", filter.keyword);
        request.setAttribute("selectedDepartment", filter.department);
        request.setAttribute("selectedAcademicProgram", filter.academicProgram);
        request.setAttribute("selectedMajor", filter.major);
        request.setAttribute("selectedMainClass", filter.mainClass);
        request.setAttribute("selectedAttendanceStatus", filter.attendanceStatus);
        request.setAttribute("filtered", filter.activeCount() > 0);
        request.setAttribute("activeFilterCount", filter.activeCount());
        // Query string đã được URL-encode sẵn nên JSP có thể nối trực tiếp vào các đường dẫn.
        request.setAttribute("filterQuery", filter.toQuery());
    }

    /**
     * Lưu thông báo kết quả sau khi thêm sinh viên vào danh sách tham gia vào HttpSession.
     */
    private void setAddedMessage(HttpSession session, int added) {
        // Phân nhánh nội dung thông báo giữa trường hợp không có sinh viên mới và trường hợp thêm thành công.
        session.setAttribute("successMessage", added == 0
                ? "Không có sinh viên mới nào được thêm."
                : "Đã thêm " + added + " sinh viên vào danh sách.");
    }

    /**
     * Chuẩn bị danh sách sinh viên ứng viên và gom nhóm tùy chọn Khoa / Ngành / CTĐT / Lớp (đã sắp xếp)
     * cho form thêm sinh viên theo nhóm trên giao diện.
     */
    private void setCandidateAttributes(HttpServletRequest request, List<ParticipantCandidateDTO> candidates) {
        Map<Integer, GroupOption> departments = new HashMap<>();
        Map<Integer, GroupOption> majors = new HashMap<>();
        Map<Integer, GroupOption> programs = new HashMap<>();
        Map<Integer, GroupOption> classes = new HashMap<>();
        // Duyệt danh sách ứng viên để gom nhóm tùy chọn Khoa, Ngành, CTĐT, Lớp và đếm số lượng ứng viên trong từng nhóm.
        for (ParticipantCandidateDTO c : candidates) {
            // Nếu ứng viên thuộc một Khoa hợp lệ, tạo/lấy GroupOption của Khoa đó và tăng bộ đếm.
            if (c.getDepartmentId() > 0) {
                departments.computeIfAbsent(c.getDepartmentId(), id -> new GroupOption(id,
                        c.getDepartmentName(), id, 0, 0)).count++;
            }
            // Nếu ứng viên thuộc một Ngành hợp lệ, gom nhóm theo Ngành (lưu kèm ID Khoa cha) và tăng bộ đếm.
            if (c.getMajorId() > 0) {
                majors.computeIfAbsent(c.getMajorId(), id -> new GroupOption(id,
                        c.getMajorName(), c.getDepartmentId(), id, 0)).count++;
            }
            // Nếu ứng viên thuộc một CTĐT hợp lệ, gom nhóm theo CTĐT (lưu kèm ID Khoa và Ngành cha) và tăng bộ đếm.
            if (c.getAcademicProgramId() > 0) {
                programs.computeIfAbsent(c.getAcademicProgramId(), id -> new GroupOption(id,
                        c.getAcademicProgramName(), c.getDepartmentId(), c.getMajorId(), id)).count++;
            }
            // Nếu ứng viên thuộc một Lớp sinh hoạt hợp lệ, gom nhóm theo Lớp (lưu đầy đủ ID các cấp cha) và tăng bộ đếm.
            if (c.getMainClassId() > 0) {
                classes.computeIfAbsent(c.getMainClassId(), id -> new GroupOption(id,
                        c.getMainClassCode(), c.getDepartmentId(), c.getMajorId(),
                        c.getAcademicProgramId())).count++;
            }
        }
        request.setAttribute("candidates", candidates);
        request.setAttribute("groupDepartments", sortedByName(departments));
        request.setAttribute("groupMajors", sortedByName(majors));
        request.setAttribute("groupPrograms", sortedByName(programs));
        request.setAttribute("groupClasses", sortedByName(classes));
    }

    /**
     * Chuyển Map các tùy chọn nhóm sang List và sắp xếp theo tên không phân biệt chữ hoa/thường.
     */
    private List<GroupOption> sortedByName(Map<Integer, GroupOption> options) {
        List<GroupOption> list = new ArrayList<>(options.values());
        list.sort(Comparator.comparing(GroupOption::getName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    /**
     * Thêm chuỗi đã cắt khoảng trắng vào tập giá trị nếu chuỗi khác null và không rỗng (dùng gom danh sách lọc).
     */
    private void addIfPresent(Set<String> values, String value) {
        // Chỉ thêm vào tập khi chuỗi có nội dung thực (không null và không toàn khoảng trắng).
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }

    /**
     * Lấy danh sách các hoạt động ngoại khóa mà một sinh viên cụ thể đã tham gia.
     */
    private List<ExtracurricularActivityResponseDTO> getStudentParticipatedActivities(int studentId) {
        // Trả về danh sách rỗng ngay nếu studentId không hợp lệ.
        if (studentId <= 0) {
            return List.of();
        }
        Set<Integer> participatedActivityIds = participantService.getActivityIdsByStudentId(studentId);
        // Nếu sinh viên chưa tham gia hoạt động nào thì kết thúc sớm để tránh tải toàn bộ danh sách hoạt động.
        if (participatedActivityIds.isEmpty()) {
            return List.of();
        }
        List<ExtracurricularActivityResponseDTO> activities = new ArrayList<>();
        // Duyệt toàn bộ hoạt động trong hệ thống và giữ lại những hoạt động mà sinh viên có tham gia.
        for (ExtracurricularActivityResponseDTO activity : activityService.getAllActivities()) {
            // Nếu ID hoạt động thuộc tập hoạt động đã tham gia của sinh viên, đưa vào danh sách kết quả.
            if (participatedActivityIds.contains(activity.getId())) {
                activities.add(activity);
            }
        }
        return activities;
    }

    /**
     * Lấy danh sách các hoạt động mà người dùng hiện tại có quyền xem và điểm danh
     * (kết hợp quyền xem hoạt động và quyền phối hợp từ invitationService).
     */
    private List<ExtracurricularActivityResponseDTO> getVisibleActivities(CurrentUser user) {
        Set<Integer> visibleActivityIds = invitationService.getAttendanceActivityIds(user);
        List<ExtracurricularActivityResponseDTO> activities = new ArrayList<>();
        // Duyệt các hoạt động người dùng được phép xem và lọc theo tập ID được phép điểm danh.
        for (ExtracurricularActivityResponseDTO activity : activityService.getActivitiesVisibleTo(user)) {
            // Nếu không bị giới hạn danh sách (visibleActivityIds == null) hoặc ID nằm trong tập cho phép, thêm vào kết quả.
            if (visibleActivityIds == null || visibleActivityIds.contains(activity.getId())) {
                activities.add(activity);
            }
        }
        return activities;
    }

    /**
     * Tìm hoạt động theo ID trong danh sách hoạt động cho trước; trả về null nếu không tìm thấy.
     */
    private ExtracurricularActivityResponseDTO findById(List<ExtracurricularActivityResponseDTO> activities,
            int activityId) {
        // Duyệt tuần tự danh sách hoạt động để đối chiếu ID.
        for (ExtracurricularActivityResponseDTO activity : activities) {
            // Tìm thấy hoạt động trùng khớp activityId -> trả về ngay.
            if (activity.getId() == activityId) {
                return activity;
            }
        }
        return null;
    }

    /**
     * Thu thập các tham số có tiền tố "status_<participantId>" từ request thành Map (participantId -> tên trạng thái).
     */
    private Map<Integer, String> readStatuses(HttpServletRequest request) {
        Map<Integer, String> statuses = new LinkedHashMap<>();
        Enumeration<String> names = request.getParameterNames();
        // Duyệt toàn bộ tên tham số HTTP request để trích xuất các cặp trạng thái điểm danh.
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            // Chỉ xử lý các tham số có tiền tố "status_".
            if (name.startsWith(STATUS_PARAM_PREFIX)) {
                int participantId = parseId(name.substring(STATUS_PARAM_PREFIX.length()));
                // Nếu phần hậu tố tách ra là mã participantId dương hợp lệ, đưa vào map trạng thái.
                if (participantId > 0) {
                    statuses.put(participantId, request.getParameter(name));
                }
            }
        }
        return statuses;
    }

    /**
     * Chuyển mảng chuỗi ID từ request sang danh sách số nguyên dương không trùng lặp.
     */
    private List<Integer> parseIds(String[] values) {
        List<Integer> ids = new ArrayList<>();
        // Chỉ duyệt mảng khi request có gửi lên danh sách giá trị khác null.
        if (values != null) {
            // Duyệt từng chuỗi ID trong mảng để chuyển sang số nguyên dương và loại trùng.
            for (String value : values) {
                int id = parseId(value);
                // Chỉ thêm các ID dương và chưa xuất hiện trong danh sách kết quả.
                if (id > 0 && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    /**
     * Chuyển một chuỗi sang số nguyên ID; trả về 0 nếu chuỗi null, rỗng hoặc không phải số hợp lệ.
     */
    private int parseId(String value) {
        // Trả về 0 nếu chuỗi đầu vào là null hoặc chỉ gồm khoảng trắng.
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        // Chuỗi không đúng định dạng số nguyên -> trả về 0 thay vì ném ngoại lệ.
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Đại diện cho một tùy chọn Khoa / Ngành / CTĐT / Lớp trong form thêm sinh viên theo nhóm.
     * Lưu kèm ID cấp cha để lọc liên hoàn (cascading select) và số lượng sinh viên ứng viên trong nhóm.
     */
    public static final class GroupOption {
        private final int id;
        private final String name;
        private final int departmentId;
        private final int majorId;
        private final int programId;
        private int count;

        /** Khởi tạo một tùy chọn nhóm với ID, tên hiển thị và các ID cấp cha tương ứng. */
        private GroupOption(int id, String name, int departmentId, int majorId, int programId) {
            this.id = id;
            // Nếu tên nhóm bị thiếu hoặc rỗng, dùng tên dự phòng dạng "#<id>".
            this.name = name == null || name.isBlank() ? "#" + id : name.trim();
            this.departmentId = departmentId;
            this.majorId = majorId;
            this.programId = programId;
        }

        /** Trả về ID của tùy chọn nhóm. */
        public int getId() {
            return id;
        }

        /** Trả về tên hiển thị của tùy chọn nhóm. */
        public String getName() {
            return name;
        }

        /** Trả về ID khoa cha của tùy chọn nhóm. */
        public int getDepartmentId() {
            return departmentId;
        }

        /** Trả về ID ngành cha của tùy chọn nhóm. */
        public int getMajorId() {
            return majorId;
        }

        /** Trả về ID chương trình đào tạo cha của tùy chọn nhóm. */
        public int getProgramId() {
            return programId;
        }

        /** Trả về số lượng sinh viên ứng viên thuộc nhóm này. */
        public int getCount() {
            return count;
        }
    }

    /**
     * Đóng gói tiêu chí tìm kiếm và lọc danh sách sinh viên tham gia, đọc trực tiếp từ tham số HTTP request.
     */
    private static final class ParticipantFilter {
        private final String keyword;
        private final String department;
        private final String academicProgram;
        private final String major;
        private final String mainClass;
        private final String attendanceStatus;
        private final String normalizedKeyword;

        /** Đọc và chuẩn hóa các tham số lọc (từ khóa, khoa, CTĐT, ngành, lớp, trạng thái) từ request. */
        private ParticipantFilter(HttpServletRequest request) {
            this.keyword = trimToEmpty(request.getParameter("keyword"));
            this.department = trimToEmpty(request.getParameter("department"));
            this.academicProgram = trimToEmpty(request.getParameter("academicProgram"));
            this.major = trimToEmpty(request.getParameter("major"));
            this.mainClass = trimToEmpty(request.getParameter("mainClass"));
            this.attendanceStatus = parseStatusName(request.getParameter("attendanceStatus"));
            this.normalizedKeyword = normalizeText(keyword);
        }

        /** Tạo đối tượng ParticipantFilter từ HttpServletRequest. */
        static ParticipantFilter from(HttpServletRequest request) {
            return new ParticipantFilter(request);
        }

        /** Kiểm tra một sinh viên có thỏa mãn đồng thời tất cả điều kiện tìm kiếm và bộ lọc hiện tại hay không. */
        boolean matches(ActivityParticipantResponseDTO participant) {
            // Nếu người dùng nhập từ khóa, so khớp không dấu với cả mã sinh viên lẫn họ tên; không khớp thì loại ngay.
            if (!normalizedKeyword.isEmpty()
                    && !normalizeText(participant.getStudentCode()).contains(normalizedKeyword)
                    && !normalizeText(participant.getStudentName()).contains(normalizedKeyword)) {
                return false;
            }
            // Đối chiếu đồng thời các bộ lọc Khoa, CTĐT, Ngành, Lớp và trạng thái điểm danh của buổi hiện tại.
            return matchesValue(department, participant.getDepartmentName())
                    && matchesValue(academicProgram, participant.getAcademicProgramName())
                    && matchesValue(major, participant.getMajorName())
                    && matchesValue(mainClass, participant.getMainClassCode())
                    && (attendanceStatus.isEmpty()
                            || attendanceStatus.equals(String.valueOf(participant.getSessionAttendanceStatus())));
        }

        /** Đếm số lượng tiêu chí lọc đang được người dùng kích hoạt. */
        int activeCount() {
            // Cộng 1 cho mỗi tiêu chí lọc đang có giá trị khác rỗng.
            return (normalizedKeyword.isEmpty() ? 0 : 1) + (department.isEmpty() ? 0 : 1)
                    + (academicProgram.isEmpty() ? 0 : 1) + (major.isEmpty() ? 0 : 1)
                    + (mainClass.isEmpty() ? 0 : 1) + (attendanceStatus.isEmpty() ? 0 : 1);
        }

        /** Chuyển các bộ lọc đang bật thành chuỗi query string (đã URL-encode) để giữ trạng thái lọc khi chuyển trang/redirect. */
        String toQuery() {
            StringBuilder query = new StringBuilder();
            appendParam(query, "keyword", keyword);
            appendParam(query, "department", department);
            appendParam(query, "academicProgram", academicProgram);
            appendParam(query, "major", major);
            appendParam(query, "mainClass", mainClass);
            appendParam(query, "attendanceStatus", attendanceStatus);
            return query.toString();
        }

        /** So khớp chính xác giá trị bộ lọc đã chọn với thuộc tính của sinh viên (trả true nếu chưa chọn bộ lọc). */
        private static boolean matchesValue(String selected, String value) {
            return selected.isEmpty() || (value != null && selected.equals(value.trim()));
        }

        /** Nối một cặp tham số &name=value (đã mã hóa UTF-8) vào query string nếu value không rỗng. */
        private static void appendParam(StringBuilder query, String name, String value) {
            // Chỉ nối tham số vào chuỗi query khi giá trị lọc đang được nhập/chọn.
            if (!value.isEmpty()) {
                query.append('&').append(name).append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }

        /** Cắt khoảng trắng hai đầu chuỗi; trả về chuỗi rỗng nếu null. */
        private static String trimToEmpty(String value) {
            return value == null ? "" : value.trim();
        }

        /** Chuẩn hóa chuỗi tiếng Việt về chữ thường không dấu (gồm cả đ/Đ -> d) để phục vụ tìm kiếm không phân biệt dấu. */
        private static String normalizeText(String value) {
            // Trả về chuỗi rỗng nếu đầu vào là null.
            if (value == null) {
                return "";
            }
            // Tách ký tự có dấu theo chuẩn NFD, loại bỏ toàn bộ dấu thanh/mũ, đổi đ/Đ thành d và hạ chữ thường.
            return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .replace('đ', 'd').replace('Đ', 'D')
                    .toLowerCase(Locale.ROOT);
        }

        /** Kiểm tra chuỗi trạng thái có khớp với enum ParticipantStatus hay không; trả về chuỗi rỗng nếu không hợp lệ. */
        private static String parseStatusName(String value) {
            String candidate = trimToEmpty(value);
            // Duyệt toàn bộ hằng số enum ParticipantStatus để xác thực tên trạng thái lọc gửi lên.
            for (ParticipantStatus status : ParticipantStatus.values()) {
                // Khớp chính xác tên trạng thái hợp lệ -> trả về chuỗi đó.
                if (status.name().equals(candidate)) {
                    return candidate;
                }
            }
            return "";
        }
    }
}
