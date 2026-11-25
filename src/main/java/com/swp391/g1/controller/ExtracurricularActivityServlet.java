package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.request.ExtracurricularActivityRequestDTO;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.dto.response.PartnerStaffResponseDTO;
import com.swp391.g1.model.Assessment;
import com.swp391.g1.model.Department;
import com.swp391.g1.model.Staff;
import com.swp391.g1.service.*;
import com.swp391.g1.service.impl.*;
import com.swp391.g1.util.AuthContext;
import com.swp391.g1.util.DateTimeUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Servlet trung tâm điều phối các chức năng quản lý hoạt động ngoại khóa:
 * danh sách có lọc/phân trang, chi tiết hoạt động, tạo/sửa/xóa, phân công nhân sự & đơn vị đồng phụ trách,
 * lời mời hợp tác đối tác và các API JSON hỗ trợ nạp danh sách nhân sự cho biểu mẫu.
 */
@WebServlet(name = "ExtracurricularActivityServlet",
        urlPatterns = {"/activities", "/activities/staffs-by-department"})
public class ExtracurricularActivityServlet extends HttpServlet {

    private static final String ACTIVITY_FORM_DATA = "activityFormData";
    private static final String ACTIVITY_FORM_ERROR = "activityFormError";

    private static final DateTimeFormatter FILTER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final IExtracurricularActivityService activityService;
    private final IActivityTypeService activityTypeService;
    private final IActivityOrganizationLevelService organizationLevelService;
    private final IActivityTargetParticipantService targetParticipantService = new ActivityTargetParticipantServiceImpl();
    private final IPartnerCompanyService partnerCompanyService;
    private final IPartnerStaffService partnerStaffService;
    private final ISemesterService semesterService;
    private final IDepartmentService departmentService;
    private final IStaffService staffService;
    private final IActivityStaffService activityStaffService;
    private final IActivityPartnerStaffService activityPartnerStaffService;
    private final IActivityCollaborationInvitationService invitationService;
    private final IRegistrationFormService registrationFormService;
    private final IActivityParticipantService participantService = new ActivityParticipantServiceImpl();
    private final IStudentService studentService = new StudentServiceImpl();
    private final IAssessmentService assessmentService = new AssessmentServiceImpl();
    private final com.swp391.g1.service.IRecruitmentService recruitmentService =
            new com.swp391.g1.service.impl.RecruitmentServiceImpl();

    /**
     * Khởi tạo servlet cùng các lớp service cài đặt mặc định để xử lý nghiệp vụ hoạt động ngoại khóa.
     */
    public ExtracurricularActivityServlet() {
        this.activityPartnerStaffService = new ActivityPartnerStaffServiceImpl();
        this.invitationService = new ActivityCollaborationInvitationServiceImpl();
        this.registrationFormService = new RegistrationFormServiceImpl();
        this.activityService = new ExtracurricularActivityServiceImpl();
        this.activityTypeService = new ActivityTypeServiceImpl();
        this.organizationLevelService = new ActivityOrganizationLevelServiceImpl();
        this.partnerCompanyService = new PartnerCompanyServiceImpl();
        this.partnerStaffService = new PartnerStaffServiceImpl();
        this.semesterService = new SemesterServiceImpl();
        this.departmentService = new DepartmentServiceImpl();
        this.staffService = new StaffServiceImpl();
        this.activityStaffService = new ActivityStaffServiceImpl();
    }

    /**
     * Tiền xử lý mọi yêu cầu HTTP để kiểm tra quyền quản lý hoạt động (STAFF/ADMIN)
     * trước khi điều phối tiếp vào {@link #doGet} hoặc {@link #doPost}.
     *
     * @param request  đối tượng yêu cầu HTTP
     * @param response đối tượng phản hồi HTTP
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi nhập/xuất
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        // Chuẩn hóa action về chuỗi rỗng nếu tham số không được truyền
        action = action == null ? "" : action.trim();

        // Xác định các thao tác tạo/sửa/xóa hoạt động và mời/hủy/ngừng hợp tác đối tác đòi hỏi quyền quản lý
        boolean isGetRequest = "GET".equalsIgnoreCase(request.getMethod())
                || "HEAD".equalsIgnoreCase(request.getMethod());
        boolean requiresManagementPermission =
                (isGetRequest
                        && ("create".equals(action) || "edit".equals(action)))
                        || ("POST".equalsIgnoreCase(request.getMethod())
                        && ("create".equals(action) || "update".equals(action) || "delete".equals(action)))
                        || ("POST".equalsIgnoreCase(request.getMethod())
                        && ("inviteCollaborator".equals(action) || "cancelInvitation".equals(action)
                        || "terminatePartner".equals(action)));
        // Trả lỗi 403 nếu hành động yêu cầu quyền quản lý nhưng người dùng không phải STAFF/ADMIN
        if (requiresManagementPermission
                && !AccessPolicy.canManageActivities(AuthContext.getCurrentUser(request))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ tài khoản STAFF hoặc ADMIN mới được thêm, sửa, xóa hoạt động.");
            return;
        }

        super.service(request, response);
    }

    /**
     * Điều phối các yêu cầu GET: hiển thị danh sách, form tạo/sửa, trang chi tiết hoạt động
     * và trả về JSON nhân sự theo công ty đối tác hoặc phòng ban.
     *
     * @param request  đối tượng yêu cầu HTTP
     * @param response đối tượng phản hồi HTTP
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi nhập/xuất
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Hỗ trợ endpoint riêng /activities/staffs-by-department để gọi AJAX lấy nhân sự theo phòng ban
        if ("/activities/staffs-by-department".equals(request.getServletPath())) {
            try {
                getStaffsByDepartmentJson(request, response);
            } catch (Exception e) {
                // Trả mã lỗi 500 khi không thể xuất JSON nhân sự theo phòng ban
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            return;
        }

        String action = request.getParameter("action");
        // Mặc định hiển thị danh sách hoạt động khi không truyền action
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }

        try {
            // Điều hướng tới handler GET tương ứng với tham số action
            switch (action) {
                // Mở biểu mẫu tạo mới hoạt động
                case "create":
                    showCreateForm(request, response);
                    break;
                // Mở biểu mẫu chỉnh sửa hoạt động
                case "edit":
                    showEditForm(request, response);
                    break;
                // Mở trang chi tiết hoạt động
                case "detail":
                    showDetail(request, response);
                    break;
                // Trả về JSON danh sách nhân sự đối tác theo công ty
                case "getStaffsByCompany":
                    getStaffsByCompanyJson(request, response);
                    break;
                // Trả về JSON danh sách nhân sự trường theo phòng ban
                case "getStaffsByDepartment":
                    getStaffsByDepartmentJson(request, response);
                    break;
                // Hiển thị danh sách hoạt động (mặc định)
                case "list":
                default:
                    listActivities(request, response);
                    break;
            }
        } catch (Exception e) {
            // Chuyển tới trang lỗi chung nếu phát sinh ngoại lệ không lường trước
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("errorMessage", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    /**
     * Điều phối các yêu cầu POST: tạo/sửa/xóa hoạt động, phân công/hủy phân công nhân sự,
     * quản lý đơn vị đồng phụ trách, nhân sự đối tác và xử lý quy trình lời mời hợp tác.
     *
     * @param request  đối tượng yêu cầu HTTP
     * @param response đối tượng phản hồi HTTP
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi nhập/xuất
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        // Mặc định rơi về action "list" nếu thiếu tham số hành động
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }

        try {
            // Điều hướng tới handler POST tương ứng với tham số action
            switch (action) {
                // Tạo mới hoạt động
                case "create":
                    createActivity(request, response);
                    break;
                // Xóa hoạt động
                case "delete":
                    deleteActivity(request, response);
                    break;
                // Cập nhật thông tin hoạt động
                case "update":
                    updateActivity(request, response);
                    break;
                // Phân công nhân sự nội bộ vào hoạt động
                case "assignStaff":
                    assignStaff(request, response);
                    break;
                // Hủy phân công nhân sự nội bộ khỏi hoạt động
                case "unassignStaff":
                    unassignStaff(request, response);
                    break;
                // Thêm đơn vị đồng phụ trách cho hoạt động
                case "addDepartment":
                    addDepartment(request, response);
                    break;
                // Gỡ đơn vị đồng phụ trách khỏi hoạt động
                case "removeDepartment":
                    removeDepartment(request, response);
                    break;
                // Phân công nhân sự đối tác tham gia phối hợp hoạt động
                case "assignPartnerStaff":
                    assignPartnerStaff(request, response);
                    break;
                // Gửi lời mời hợp tác tới doanh nghiệp/nhân sự đối tác
                case "inviteCollaborator":
                    handleInvitation(request, response, "invite");
                    break;
                // Đối tác phản hồi (chấp nhận/từ chối) lời mời hợp tác
                case "respondInvitation":
                    handleInvitation(request, response, "respond");
                    break;
                // Hủy lời mời hợp tác đang chờ phản hồi
                case "cancelInvitation":
                    handleInvitation(request, response, "cancel");
                    break;
                // Hủy phản hồi lời mời để đưa về trạng thái chờ phản hồi
                case "revokeInvitation":
                    handleInvitation(request, response, "revoke");
                    break;
                // Gửi yêu cầu ngừng hợp tác với đối tác đã liên kết
                case "terminatePartner":
                    handleInvitation(request, response, "terminate");
                    break;
                // Gỡ phân công nhân sự đối tác khỏi hoạt động
                case "unassignPartnerStaff":
                    unassignPartnerStaff(request, response);
                    break;
                // Các action không xác định sẽ chuyển hướng về trang danh sách hoạt động
                default:
                    response.sendRedirect(request.getContextPath() + "/activities");
                    break;
            }
        } catch (Exception e) {
            // Chuyển sang trang lỗi nếu quá trình xử lý POST gặp ngoại lệ không bắt riêng
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("errorMessage", "Lỗi xử lý dữ liệu: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    // =========================================================================
    // GET ACTION HANDLERS
    // =========================================================================

    /**
     * Tải danh sách hoạt động ngoại khóa theo phạm vi hiển thị của người dùng hiện tại,
     * áp dụng các bộ lọc (từ khóa không dấu, học kỳ, loại, phòng ban, trạng thái, tính chất, khoảng ngày),
     * nạp thông tin đăng ký/BTC cho sinh viên, thực hiện phân trang và chuyển tiếp sang {@code list.jsp}.
     *
     * @param request  đối tượng yêu cầu HTTP chứa các tham số lọc và phân trang
     * @param response đối tượng phản hồi HTTP
     * @throws ServletException nếu lỗi khi forward sang JSP
     * @throws IOException      nếu lỗi nhập/xuất
     */
    private void listActivities(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Đọc và chuẩn hóa các tham số lọc từ query string
        String keyword = trimToEmpty(request.getParameter("keyword"));
        int semesterId = parseId(request.getParameter("semesterId"));
        int activityTypeId = parseId(request.getParameter("activityTypeId"));
        int departmentId = parseId(request.getParameter("departmentId"));
        String activityStatus = parseEnumName(request.getParameter("activityStatus"),
                com.swp391.g1.model.Enum.ActivityStatus.values());
        String nature = trimToEmpty(request.getParameter("nature"));
        LocalDate startDate = parseDate(request.getParameter("startDate"));
        LocalDate endDate = parseDate(request.getParameter("endDate"));
        String normalizedKeyword = normalizeText(keyword);

        // Lấy tập ID hoạt động được phép xem (áp dụng giới hạn cho đối tác) và ngữ cảnh người dùng hiện tại
        Set<Integer> visibleActivityIds =
                invitationService.getVisibleActivityIds(AuthContext.getCurrentUser(request));
        List<ExtracurricularActivityResponseDTO> allActivities = new ArrayList<>();
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        boolean studentView = currentUser != null && "STUDENT".equals(currentUser.getType());
        LocalDateTime now = DateTimeUtil.now();
        // Lưu URL hiện tại (kèm query string nếu có) để quay lại đúng bộ lọc sau khi sinh viên đăng ký
        String activityReturnUrl = request.getRequestURI()
                + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
        request.setAttribute("activityReturnUrl", activityReturnUrl);
        request.setAttribute("activityReturnUrlParameter",
                URLEncoder.encode(activityReturnUrl, StandardCharsets.UTF_8));
        Map<Integer, com.swp391.g1.model.RegistrationForm> activityFormsByActivity = new HashMap<>();
        Set<Integer> activitiesAvailableForRegistration = new LinkedHashSet<>();
        // Duyệt toàn bộ hoạt động trong phạm vi hiển thị của người dùng để lọc theo điều kiện tìm kiếm
        for (ExtracurricularActivityResponseDTO activity :
                activityService.getActivitiesVisibleTo(currentUser)) {
            // Sinh viên chỉ được xem hoạt động công khai và đã được phê duyệt
            if (studentView && (!activity.isPublic()
                    || activity.getApprovalStatus() != com.swp391.g1.model.Enum.ApprovalStatus.APPROVED)) {
                continue;
            }
            // Nếu có danh sách giới hạn theo lời mời đối tác thì bỏ qua hoạt động không thuộc danh sách
            if (visibleActivityIds != null && !visibleActivityIds.contains(activity.getId())) {
                continue;
            }
            // Lọc theo từ khóa không phân biệt dấu/chữ hoa trên mã và tên hoạt động
            if (!normalizedKeyword.isEmpty()
                    && !normalizeText(activity.getCode()).contains(normalizedKeyword)
                    && !normalizeText(activity.getName()).contains(normalizedKeyword)) {
                continue;
            }
            // Lọc theo học kỳ khi người dùng chọn bộ lọc học kỳ
            if (semesterId > 0 && activity.getSemesterId() != semesterId) {
                continue;
            }
            // Lọc theo loại hoạt động
            if (activityTypeId > 0 && activity.getActivityTypeId() != activityTypeId) {
                continue;
            }
            // Lọc theo phòng ban phụ trách chính
            if (departmentId > 0 && activity.getResponsibleDepartmentId() != departmentId) {
                continue;
            }
            // Lọc theo trạng thái diễn ra của hoạt động
            if (!activityStatus.isEmpty() && !activityStatus.equals(String.valueOf(activity.getActivityStatus()))) {
                continue;
            }
            // Lọc theo tính chất hoạt động (bắt buộc / yêu cầu đăng ký / tự do)
            if (!nature.isEmpty() && !nature.equals(String.valueOf(activity.getNature()))) {
                continue;
            }
            // Lọc các hoạt động có ngày bắt đầu từ startDate trở đi
            if (startDate != null && (activity.getStartTime() == null
                    || activity.getStartTime().toLocalDate().isBefore(startDate))) {
                continue;
            }
            // Lọc các hoạt động có ngày kết thúc không muộn hơn endDate
            if (endDate != null && (activity.getEndTime() == null
                    || activity.getEndTime().toLocalDate().isAfter(endDate))) {
                continue;
            }
            allActivities.add(activity);
            // Với giao diện sinh viên, nạp thêm form đăng ký và đánh dấu hoạt động đang mở đăng ký
            if (studentView && (activity.isRegistrationFormRequired()
                    || "REQUIRE_REGISTRATION".equals(String.valueOf(activity.getNature())))) {
                com.swp391.g1.model.RegistrationForm registrationForm = registrationFormService.getByActivityId(activity.getId());
                // Lưu ánh xạ form đăng ký theo ID hoạt động nếu đã được tạo
                if (registrationForm != null) {
                    activityFormsByActivity.put(activity.getId(), registrationForm);
                }
                // Đánh dấu hoạt động đủ điều kiện mở nút đăng ký tại thời điểm hiện tại
                if (isRegistrationAvailable(activity, registrationForm, now)) {
                    activitiesAvailableForRegistration.add(activity.getId());
                }
            }
        }

        // Nạp trạng thái đơn đăng ký tham gia và các hoạt động mà sinh viên đã trúng tuyển BTC
        Map<Integer, com.swp391.g1.model.RegistrationApplication> registrationsByActivity = new HashMap<>();
        Set<Integer> organizerActivityIds = new LinkedHashSet<>();
        // Chỉ truy vấn dữ liệu cá nhân sinh viên khi người dùng là STUDENT
        if (studentView) {
            com.swp391.g1.model.Student myStudent = studentService.getStudentByAccountId(currentUser.getId());
            // Bỏ qua nếu tài khoản sinh viên chưa có hồ sơ Student tương ứng
            if (myStudent != null) {
                // Duyệt các đơn đăng ký tham gia hoạt động của sinh viên để lập map theo activityId
                for (com.swp391.g1.model.RegistrationApplication myApp
                        : registrationFormService.getApplicationsForStudent(myStudent.getId())) {
                    registrationsByActivity.putIfAbsent(myApp.getActivityId(), myApp);
                }
                // Duyệt các đơn ứng tuyển BTC của sinh viên để đánh dấu những hoạt động đã trúng tuyển
                for (com.swp391.g1.model.RecruitmentApplication app
                        : recruitmentService.getStudentApplications(myStudent.getId())) {
                    // Sinh viên đã trúng tuyển BTC (ACCEPTED) sẽ không cần đăng ký làm người tham gia nữa
                    if ("ACCEPTED".equals(app.getStatus())) organizerActivityIds.add(app.getActivityId());
                }
            }
        }
        // Loại bỏ các hoạt động mà sinh viên đã là BTC khỏi tập hoạt động hiển thị nút đăng ký tham gia
        activitiesAvailableForRegistration.removeAll(organizerActivityIds);
        request.setAttribute("organizerActivityIds", organizerActivityIds);
        request.setAttribute("registrationsByActivity", registrationsByActivity);
        request.setAttribute("isStudent", studentView);
        request.setAttribute("activityFormsByActivity", activityFormsByActivity);
        request.setAttribute("activitiesAvailableForRegistration", activitiesAvailableForRegistration);
        // Xác định trạng thái đang áp dụng bộ lọc và đếm số bộ lọc đang bật để hiển thị trên giao diện
        boolean filtered = !normalizedKeyword.isEmpty() || semesterId > 0 || activityTypeId > 0
                || departmentId > 0 || !activityStatus.isEmpty() || !nature.isEmpty()
                || startDate != null || endDate != null;
        request.setAttribute("keyword", keyword);
        request.setAttribute("selectedActivityStatus", activityStatus);
        request.setAttribute("selectedNature", nature);
        request.setAttribute("selectedSemesterId", semesterId);
        request.setAttribute("selectedActivityTypeId", activityTypeId);
        request.setAttribute("selectedDepartmentId", departmentId);
        request.setAttribute("startDate", formatDate(startDate));
        request.setAttribute("endDate", formatDate(endDate));
        request.setAttribute("filtered", filtered);
        // Cộng dồn số lượng tiêu chí lọc đang có giá trị hợp lệ (mỗi tiêu chí đóng góp 1 nếu đang lọc)
        request.setAttribute("activeFilterCount",
                (normalizedKeyword.isEmpty() ? 0 : 1) + (semesterId > 0 ? 1 : 0)
                        + (activityTypeId > 0 ? 1 : 0) + (departmentId > 0 ? 1 : 0)
                        + (activityStatus.isEmpty() ? 0 : 1) + (nature.isEmpty() ? 0 : 1)
                        + (startDate != null ? 1 : 0) + (endDate != null ? 1 : 0));
        request.setAttribute("filterQuery", buildFilterQuery(keyword, semesterId, activityTypeId,
                departmentId, activityStatus, nature, startDate, endDate));
        request.setAttribute("activityStatuses", com.swp391.g1.model.Enum.ActivityStatus.values());
        request.setAttribute("semesters", semesterService.getAllSemesters());
        request.setAttribute("activityTypes", activityTypeService.getActiveActivityTypes());
        request.setAttribute("departments", departmentService.getAllDepartments());

        // Tính toán chỉ số phân trang trên danh sách đã lọc
        int pageSize = parsePageSize(request.getParameter("pageSize"));
        int totalActivityCount = allActivities.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalActivityCount / pageSize));
        int currentPage = parsePageNumber(request.getParameter("page"), totalPages);
        int fromIndex = Math.min((currentPage - 1) * pageSize, totalActivityCount);
        int toIndex = Math.min(fromIndex + pageSize, totalActivityCount);

        List<ExtracurricularActivityResponseDTO> pageActivities =
                new ArrayList<>(allActivities.subList(fromIndex, toIndex));
        Set<Integer> activitiesBeforeStart = new LinkedHashSet<>();
        // Duyệt các hoạt động trên trang hiện tại để đánh dấu những hoạt động chưa tới giờ bắt đầu (cho phép sửa)
        for (ExtracurricularActivityResponseDTO activity : pageActivities) {
            // Chỉ cho phép các thao tác trước khi bắt đầu nếu hoạt động còn UPCOMING và now < startTime
            if (isUpcomingBeforeStart(activity, now)) {
                activitiesBeforeStart.add(activity.getId());
            }
        }
        request.setAttribute("activities", pageActivities);
        request.setAttribute("activitiesBeforeStart", activitiesBeforeStart);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("currentPage", currentPage);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalActivityCount", totalActivityCount);
        // Nếu danh sách rỗng thì số thứ tự bắt đầu là 0, ngược lại là vị trí 1-based của phần tử đầu trang
        request.setAttribute("firstActivityNumber", totalActivityCount == 0 ? 0 : fromIndex + 1);
        request.setAttribute("lastActivityNumber", toIndex);
        setActivityManagementPermission(request);
        request.getRequestDispatcher("/WEB-INF/views/activity/list.jsp").forward(request, response);
    }

    /**
     * Cắt khoảng trắng đầu/cuối của chuỗi và trả về chuỗi rỗng nếu giá trị là {@code null}.
     *
     * @param value chuỗi đầu vào
     * @return chuỗi đã trim hoặc {@code ""} nếu null
     */
    private String trimToEmpty(String value) {
        // Tránh NullPointerException bằng cách thay null bằng chuỗi rỗng
        return value == null ? "" : value.trim();
    }

    /**
     * Chuẩn hóa chuỗi tiếng Việt để tìm kiếm không phân biệt dấu và chữ hoa/chữ thường:
     * tách dấu tổ hợp Unicode (NFD), xóa toàn bộ dấu thanh/mũ, chuyển đ/Đ thành d/D và đưa về chữ thường.
     *
     * @param value chuỗi văn bản cần chuẩn hóa
     * @return chuỗi đã bỏ dấu tiếng Việt ở dạng chữ thường; {@code ""} nếu null
     */
    private String normalizeText(String value) {
        // Trả về chuỗi rỗng nếu đầu vào null
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Phân tích chuỗi ngày lọc theo định dạng nghiêm ngặt {@code dd/MM/uuuu}.
     *
     * @param value chuỗi ngày từ bộ lọc
     * @return đối tượng {@link LocalDate} hợp lệ, hoặc {@code null} nếu rỗng/sai định dạng
     */
    private LocalDate parseDate(String value) {
        try {
            // Trả về null nếu chuỗi rỗng, ngược lại parse theo định dạng dd/MM/uuuu nghiêm ngặt
            return value == null || value.isBlank()
                    ? null : LocalDate.parse(value.trim(), FILTER_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            // Bỏ qua giá trị ngày không hợp lệ từ query string bằng cách trả về null
            return null;
        }
    }

    /**
     * Định dạng đối tượng {@link LocalDate} sang chuỗi {@code dd/MM/uuuu} để hiển thị lại trên ô lọc.
     *
     * @param date ngày cần định dạng
     * @return chuỗi ngày đã định dạng hoặc {@code ""} nếu {@code date} null
     */
    private String formatDate(LocalDate date) {
        return date == null ? "" : date.format(FILTER_DATE_FORMAT);
    }

    /**
     * Kiểm tra và trích xuất tên hằng enum hợp lệ từ tham số chuỗi dựa trên tập hằng cho phép.
     *
     * @param value   giá trị chuỗi người dùng truyền lên
     * @param allowed mảng hằng enum được chấp nhận
     * @return tên enum hợp lệ nếu khớp; ngược lại trả về {@code ""}
     */
    private String parseEnumName(String value, java.lang.Enum<?>[] allowed) {
        String candidate = trimToEmpty(value);
        // Duyệt danh sách hằng enum hợp lệ để đối chiếu chính xác tên hằng
        for (java.lang.Enum<?> option : allowed) {
            // Khớp chính xác tên hằng enum -> trả về giá trị hợp lệ
            if (option.name().equals(candidate)) {
                return candidate;
            }
        }
        return "";
    }

    /**
     * Xây dựng chuỗi query tham số lọc (bắt đầu bằng {@code &}) để giữ nguyên điều kiện lọc khi chuyển trang.
     *
     * @param keyword        từ khóa tìm kiếm
     * @param semesterId     ID học kỳ lọc
     * @param activityTypeId ID loại hoạt động lọc
     * @param departmentId   ID phòng ban phụ trách lọc
     * @param activityStatus trạng thái hoạt động lọc
     * @param nature         tính chất hoạt động lọc
     * @param startDate      ngày bắt đầu lọc
     * @param endDate        ngày kết thúc lọc
     * @return chuỗi query đã URL-encode sẵn sàng nối vào liên kết phân trang
     */
    private String buildFilterQuery(String keyword, int semesterId, int activityTypeId,
                                    int departmentId, String activityStatus, String nature,
                                    LocalDate startDate, LocalDate endDate) {
        StringBuilder query = new StringBuilder();
        appendParam(query, "keyword", keyword);
        // Chỉ nối các tham số ID khi giá trị > 0
        appendParam(query, "semesterId", semesterId > 0 ? String.valueOf(semesterId) : "");
        appendParam(query, "activityTypeId", activityTypeId > 0 ? String.valueOf(activityTypeId) : "");
        appendParam(query, "departmentId", departmentId > 0 ? String.valueOf(departmentId) : "");
        appendParam(query, "activityStatus", activityStatus);
        appendParam(query, "nature", nature);
        appendParam(query, "startDate", formatDate(startDate));
        appendParam(query, "endDate", formatDate(endDate));
        return query.toString();
    }

    /**
     * Nối một cặp tham số {@code &name=encodedValue} vào {@link StringBuilder} nếu giá trị khác rỗng.
     *
     * @param query bộ đệm xây dựng chuỗi query
     * @param name  tên tham số
     * @param value giá trị tham số chưa encode
     */
    private void appendParam(StringBuilder query, String name, String value) {
        // Chỉ thêm tham số vào query string khi giá trị không rỗng
        if (!value.isEmpty()) {
            query.append('&').append(name).append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
    }

    /**
     * Phân tích kích thước trang từ tham số request, chỉ chấp nhận các mức {5, 10, 20, 25}.
     *
     * @param value chuỗi kích thước trang
     * @return kích thước trang hợp lệ; mặc định là {@code 5} nếu không hợp lệ
     */
    private int parsePageSize(String value) {
        try {
            int pageSize = Integer.parseInt(value);
            // Chỉ chấp nhận các tùy chọn pageSize được hỗ trợ trên giao diện, mặc định 5
            return Set.of(5, 10, 20, 25).contains(pageSize) ? pageSize : 5;
        } catch (NumberFormatException e) {
            // Rơi về giá trị mặc định 5 khi tham số rỗng hoặc không phải số
            return 5;
        }
    }

    /**
     * Phân tích số trang hiện tại và kẹp giá trị nằm trong đoạn hợp lệ {@code [1, totalPages]}.
     *
     * @param value      chuỗi số trang từ request
     * @param totalPages tổng số trang hiện có
     * @return số trang hợp lệ; mặc định {@code 1} nếu lỗi định dạng
     */
    private int parsePageNumber(String value, int totalPages) {
        try {
            int page = Integer.parseInt(value);
            return Math.max(1, Math.min(page, totalPages));
        } catch (NumberFormatException e) {
            // Trả về trang đầu tiên khi tham số page thiếu hoặc không hợp lệ
            return 1;
        }
    }

    /**
     * Hiển thị biểu mẫu tạo mới hoạt động ngoại khóa, đồng thời khôi phục dữ liệu flash (nếu vừa bị lỗi validate)
     * và nạp toàn bộ dữ liệu dropdown danh mục.
     *
     * @param request  đối tượng yêu cầu HTTP
     * @param response đối tượng phản hồi HTTP
     * @throws ServletException nếu lỗi forward JSP
     * @throws IOException      nếu lỗi nhập/xuất
     */
    private void showCreateForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        restoreActivityFormFlash(request);
        ExtracurricularActivityRequestDTO formData = (ExtracurricularActivityRequestDTO)
                request.getAttribute("formData");
        // Nạp danh sách nhân sự tương ứng với phòng ban đã chọn trước đó (nếu đang khôi phục form lỗi)
        loadDropdownData(request, parseId(formData == null ? null : formData.getResponsibleDepartmentId()));
        request.getRequestDispatcher("/WEB-INF/views/activity/create.jsp").forward(request, response);
    }

    /**
     * Hiển thị biểu mẫu chỉnh sửa hoạt động ngoại khóa sau khi kiểm tra quyền truy cập của STAFF
     * và điều kiện hoạt động vẫn đang sắp diễn ra (trước thời điểm bắt đầu).
     *
     * @param request  đối tượng yêu cầu HTTP chứa tham số {@code id}
     * @param response đối tượng phản hồi HTTP
     * @throws ServletException nếu lỗi forward JSP
     * @throws IOException      nếu lỗi nhập/xuất
     */
    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        restoreActivityFormFlash(request);
        String idStr = request.getParameter("id");
        // Chỉ tải hoạt động khi có tham số id hợp lệ trên URL
        if (idStr != null && !idStr.trim().isEmpty()) {
            int id = Integer.parseInt(idStr);
            ExtracurricularActivityResponseDTO activity = activityService.getActivityById(id);
            CurrentUser currentUser = AuthContext.getCurrentUser(request);
            // Chặn nhân viên (STAFF) truy cập form sửa đối với hoạt động ngoài phạm vi phụ trách
            if (activity != null && AccessPolicy.hasType(currentUser, "STAFF")
                    && !activityService.canAccessActivity(id, currentUser)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bạn không có quyền sửa hoạt động này.");
                return;
            }
            // Không cho phép mở form sửa nếu hoạt động không còn ở trạng thái sắp diễn ra trước giờ bắt đầu
            if (activity != null && !isUpcomingBeforeStart(activity, DateTimeUtil.now())) {
                request.getSession().setAttribute("errorMessage",
                        "Chỉ có thể sửa hoạt động sắp diễn ra trước thời điểm bắt đầu.");
                response.sendRedirect(request.getContextPath() + "/activities");
                return;
            }
            // Nếu tìm thấy hoạt động hợp lệ thì nạp dữ liệu dropdown và chuyển sang edit.jsp
            if (activity != null) {
                request.setAttribute("activity", activity);
                ExtracurricularActivityRequestDTO formData =
                        (ExtracurricularActivityRequestDTO) request.getAttribute("formData");
                // Ưu tiên lấy phòng ban từ formData vừa submit lỗi; nếu mở mới thì lấy theo hoạt động trong DB
                int departmentId = formData == null
                        ? activity.getResponsibleDepartmentId()
                        : parseId(formData.getResponsibleDepartmentId());
                loadDropdownData(request, departmentId);

                request.getRequestDispatcher("/WEB-INF/views/activity/edit.jsp").forward(request, response);
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/activities");
    }

    /**
     * Khôi phục dữ liệu biểu mẫu và thông báo lỗi lưu tạm trong session (mẫu PRG - Post/Redirect/Get)
     * sang request attribute rồi xóa ngay khỏi session để tránh hiển thị lặp lại.
     *
     * @param request đối tượng yêu cầu HTTP
     */
    private void restoreActivityFormFlash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        // Bỏ qua nếu chưa có session hiện hữu
        if (session == null) {
            return;
        }
        Object formData = session.getAttribute(ACTIVITY_FORM_DATA);
        Object errorMessage = session.getAttribute(ACTIVITY_FORM_ERROR);
        // Chuyển formData từ session flash sang request và xóa khỏi session
        if (formData != null) {
            request.setAttribute("formData", formData);
            session.removeAttribute(ACTIVITY_FORM_DATA);
        }
        // Chuyển thông báo lỗi từ session flash sang request và xóa khỏi session
        if (errorMessage != null) {
            request.setAttribute("errorMessage", errorMessage);
            session.removeAttribute(ACTIVITY_FORM_ERROR);
        }
    }

    /**
     * Lưu tạm dữ liệu form và thông báo lỗi vào session rồi chuyển hướng quay lại form tạo mới hoặc chỉnh sửa.
     *
     * @param request      đối tượng yêu cầu HTTP
     * @param response     đối tượng phản hồi HTTP
     * @param session      session hiện tại để lưu flash attribute
     * @param dto          dữ liệu form người dùng vừa nhập
     * @param errorMessage thông báo lỗi cần hiển thị
     * @param action       đích chuyển hướng ({@code "create"} hoặc {@code "edit"})
     * @throws IOException nếu lỗi khi redirect
     */
    private void redirectToActivityForm(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            ExtracurricularActivityRequestDTO dto,
            String errorMessage,
            String action) throws IOException {
        session.setAttribute(ACTIVITY_FORM_DATA, dto);
        session.setAttribute(ACTIVITY_FORM_ERROR, errorMessage);
        String destination = request.getContextPath() + "/activities?action=" + action;
        // Khi quay lại form sửa cần gắn thêm tham số id của hoạt động đang chỉnh sửa
        if ("edit".equals(action)) {
            destination += "&id=" + dto.getId();
        }
        response.sendRedirect(destination);
    }

    /**
     * Hiển thị trang chi tiết một hoạt động ngoại khóa cùng toàn bộ thông tin liên quan:
     * kiểm tra quyền xem theo vai trò, thống kê đăng ký tham gia, tuyển BTC, điểm danh,
     * nhân sự phụ trách, đối tác hợp tác và bài đánh giá (assessment).
     *
     * @param request  đối tượng yêu cầu HTTP chứa tham số {@code id}
     * @param response đối tượng phản hồi HTTP
     * @throws ServletException nếu lỗi forward JSP
     * @throws IOException      nếu lỗi nhập/xuất
     */
    private void showDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idStr = request.getParameter("id");
        // Chỉ tải chi tiết khi có tham số id khác rỗng
        if (idStr != null && !idStr.trim().isEmpty()) {
            int id = Integer.parseInt(idStr);
            ExtracurricularActivityResponseDTO activity = activityService.getActivityById(id);
            CurrentUser currentUser = AuthContext.getCurrentUser(request);
            // Chặn nhân viên (STAFF) xem chi tiết hoạt động ngoài phạm vi quản lý
            if (activity != null && AccessPolicy.hasType(currentUser, "STAFF")
                    && !activityService.canAccessActivity(id, currentUser)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền xem hoạt động này.");
                return;
            }
            Set<Integer> visibleActivityIds =
                    invitationService.getVisibleActivityIds(AuthContext.getCurrentUser(request));
            // Chặn đối tác xem hoạt động không nằm trong danh sách lời mời/hợp tác của họ
            if (activity != null && visibleActivityIds != null && !visibleActivityIds.contains(id)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền xem hoạt động này.");
                return;
            }
            boolean studentView = AccessPolicy.hasType(currentUser, "STUDENT");
            com.swp391.g1.model.Student myStudent = null;
            // Xử lý kiểm tra điều kiện và nạp thông tin cá nhân khi người xem là sinh viên
            if (activity != null && studentView) {
                myStudent = studentService.getStudentByAccountId(currentUser.getId());
                // Trả 404 nếu hoạt động chưa công khai, chưa duyệt hoặc sinh viên không thuộc đối tượng mục tiêu
                if (!activity.isPublic()
                        || activity.getApprovalStatus() != com.swp391.g1.model.Enum.ApprovalStatus.APPROVED
                        || myStudent == null
                        || !targetParticipantService.isStudentEligible(id, myStudent.getId())) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                request.setAttribute("isStudent", true);
                // Lưu lại URL trang chi tiết để điều hướng quay về sau khi sinh viên thực hiện đăng ký
                String detailReturnUrl = request.getRequestURI()
                        + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
                request.setAttribute("activityReturnUrl", detailReturnUrl);
                request.setAttribute("activityReturnUrlParameter",
                        URLEncoder.encode(detailReturnUrl, StandardCharsets.UTF_8));
                com.swp391.g1.model.RegistrationForm studentRegForm = registrationFormService.getByActivityId(id);
                request.setAttribute("registrationForm", studentRegForm);
                boolean acceptedOrganizer = recruitmentService.hasAcceptedApplication(id, myStudent.getId());
                request.setAttribute("canStudentRegister",
                        isRegistrationAvailable(activity, studentRegForm, DateTimeUtil.now()) && !acceptedOrganizer);
                request.setAttribute("isAcceptedOrganizer", acceptedOrganizer);
                // Duyệt danh sách đơn đăng ký của sinh viên để tìm đơn ứng với hoạt động hiện tại
                for (com.swp391.g1.model.RegistrationApplication myApp
                        : registrationFormService.getApplicationsForStudent(myStudent.getId())) {
                    // Tìm thấy đơn đăng ký của hoạt động hiện tại -> gán attribute rồi dừng vòng lặp
                    if (myApp.getActivityId() == id) {
                        request.setAttribute("myRegistration", myApp);
                        break;
                    }
                }
            }
            // Tổng hợp toàn bộ dữ liệu các tab trên trang chi tiết khi hoạt động tồn tại
            if (activity != null) {
                request.setAttribute("activity", activity);
                // Nạp biểu mẫu đăng ký và thống kê số lượng đơn theo từng trạng thái
                com.swp391.g1.model.RegistrationForm regForm = registrationFormService.getByActivityId(id);
                request.setAttribute("registrationForm", regForm);
                int totalApplicationsCount = 0;
                long regSubmittedCount = 0;
                long regApprovedCount = 0;
                long regRejectedCount = 0;
                long regCancelledCount = 0;
                List<com.swp391.g1.model.RegistrationApplication> recentApplications = List.of();
                // Ưu tiên lấy chỉ tiêu tối đa cấu hình trên form đăng ký; nếu không có thì fallback về chỉ tiêu của hoạt động
                Integer registrationMaxQuota = regForm != null && regForm.getMaxParticipants() != null
                        ? regForm.getMaxParticipants()
                        : activity.getMaxParticipants();
                // Chỉ thống kê danh sách đơn đăng ký khi hoạt động đã có biểu mẫu đăng ký
                if (regForm != null) {
                    List<com.swp391.g1.model.RegistrationApplication> allApps =
                            new ArrayList<>(registrationFormService.getApplications(regForm.getId()));
                    totalApplicationsCount = allApps.size();
                    // Duyệt toàn bộ đơn đăng ký để đếm số lượng theo từng trạng thái xử lý
                    for (com.swp391.g1.model.RegistrationApplication app : allApps) {
                        String appStatus = app.getStatus();
                        // Phân loại trạng thái đơn: chờ duyệt / đã duyệt / từ chối / đã hủy hoặc rút
                        if ("SUBMITTED".equals(appStatus)) {
                            regSubmittedCount++;
                        } else if ("APPROVED".equals(appStatus)) {
                            regApprovedCount++;
                        } else if ("REJECTED".equals(appStatus)) {
                            regRejectedCount++;
                        } else if ("CANCELLED".equals(appStatus) || "WITHDRAWN".equals(appStatus)) {
                            regCancelledCount++;
                        }
                    }
                    // Sắp xếp danh sách đơn theo thời gian nộp giảm dần (mới nhất lên đầu; cùng thời gian thì ID lớn hơn lên trước)
                    allApps.sort((a, b) -> {
                        // Cả hai cùng thiếu submittedAt thì so sánh theo ID giảm dần
                        if (a.getSubmittedAt() == null && b.getSubmittedAt() == null) {
                            return Integer.compare(b.getId(), a.getId());
                        }
                        // Đưa đơn không có thời gian nộp xuống cuối danh sách
                        if (a.getSubmittedAt() == null) {
                            return 1;
                        }
                        // Đưa đơn không có thời gian nộp xuống cuối danh sách
                        if (b.getSubmittedAt() == null) {
                            return -1;
                        }
                        int cmp = b.getSubmittedAt().compareTo(a.getSubmittedAt());
                        // Nếu cùng thời điểm nộp thì ưu tiên bản ghi có ID lớn hơn
                        return cmp != 0 ? cmp : Integer.compare(b.getId(), a.getId());
                    });
                    recentApplications = allApps.subList(0, Math.min(5, allApps.size()));
                }
                // Tính tỷ lệ phần trăm lấp đầy chỉ tiêu đăng ký (giới hạn tối đa 100%, bằng 0 nếu không giới hạn chỉ tiêu)
                int registrationProgressPct = (registrationMaxQuota != null && registrationMaxQuota > 0)
                        ? Math.min(100, (int) Math.round((totalApplicationsCount * 100.0) / registrationMaxQuota))
                        : 0;
                request.setAttribute("totalApplicationsCount", totalApplicationsCount);
                request.setAttribute("regSubmittedCount", regSubmittedCount);
                request.setAttribute("regApprovedCount", regApprovedCount);
                request.setAttribute("regRejectedCount", regRejectedCount);
                request.setAttribute("regCancelledCount", regCancelledCount);
                request.setAttribute("recentApplications", recentApplications);
                request.setAttribute("registrationMaxQuota", registrationMaxQuota);
                request.setAttribute("registrationProgressPct", registrationProgressPct);

                // Thiết lập cờ quyền quản lý, thông tin tuyển dụng BTC mặc định và nhãn/badge trạng thái
                request.setAttribute("activityBeforeStart",
                        isUpcomingBeforeStart(activity, DateTimeUtil.now()));
                setActivityManagementPermission(request);
                boolean canManageRecruitment = AccessPolicy.canManageActivities(currentUser);
                request.setAttribute("canManageRecruitment", canManageRecruitment);
                request.setAttribute("recruitmentRound", null);
                request.setAttribute("recruitmentApplications", List.of());
                request.setAttribute("recruitmentCounts", Map.of());
                request.setAttribute("recruitmentAppLabels", Map.of(
                        "PENDING", "Chờ xét",
                        "INTERVIEW_INVITED", "Mời phỏng vấn",
                        "INTERVIEWED", "Đã phỏng vấn",
                        "ACCEPTED", "Trúng tuyển",
                        "REJECTED", "Không đạt",
                        "WITHDRAWN", "Đã rút"));
                request.setAttribute("recruitmentAppBadges", Map.of(
                        "PENDING", "status-yellow",
                        "INTERVIEW_INVITED", "status-yellow",
                        "INTERVIEWED", "status-blue",
                        "ACCEPTED", "status-green",
                        "REJECTED", "status-red",
                        "WITHDRAWN", "status-gray"));
                // Chỉ nạp thống kê đợt tuyển BTC khi người dùng có quyền quản lý hoạt động
                if (canManageRecruitment) {
                    com.swp391.g1.model.RecruitmentRound recruitmentRound =
                            recruitmentService.getRoundByActivity(id);
                    request.setAttribute("recruitmentRound", recruitmentRound);
                    // Nếu hoạt động có đợt tuyển BTC thì tổng hợp số đơn theo trạng thái và lấy 5 đơn mới nhất
                    if (recruitmentRound != null) {
                        List<com.swp391.g1.model.RecruitmentApplication> applications =
                                new ArrayList<>(recruitmentService.getApplications(recruitmentRound.getId(), null, null));
                        Map<String, Integer> recruitmentCounts = new HashMap<>();
                        // Duyệt toàn bộ hồ sơ ứng tuyển BTC để cộng dồn số lượng theo từng trạng thái
                        for (com.swp391.g1.model.RecruitmentApplication application : applications) {
                            recruitmentCounts.merge(application.getStatus(), 1, Integer::sum);
                        }
                        // Sắp xếp hồ sơ ứng tuyển BTC theo thời gian nộp giảm dần, null xếp cuối
                        applications.sort((a, b) -> {
                            // Cả hai không có submittedAt -> so sánh theo ID giảm dần
                            if (a.getSubmittedAt() == null && b.getSubmittedAt() == null) {
                                return Integer.compare(b.getId(), a.getId());
                            }
                            // Hồ sơ thiếu submittedAt đẩy xuống cuối
                            if (a.getSubmittedAt() == null) return 1;
                            // Hồ sơ thiếu submittedAt đẩy xuống cuối
                            if (b.getSubmittedAt() == null) return -1;
                            int comparison = b.getSubmittedAt().compareTo(a.getSubmittedAt());
                            // Trùng thời gian nộp thì sắp theo ID giảm dần
                            return comparison != 0 ? comparison : Integer.compare(b.getId(), a.getId());
                        });
                        request.setAttribute("recruitmentApplications",
                                applications.subList(0, Math.min(5, applications.size())));
                        request.setAttribute("recruitmentCounts", recruitmentCounts);
                        request.setAttribute("recruitmentTotalApplications", applications.size());
                    } else {
                        // Chưa mở đợt tuyển BTC -> tổng số hồ sơ bằng 0
                        request.setAttribute("recruitmentTotalApplications", 0);
                    }
                }
                // Xác định quyền truy cập và mở trang điểm danh cho hoạt động hiện tại
                Set<Integer> attendanceActivityIds =
                        invitationService.getAttendanceActivityIds(currentUser);
                boolean canAccessAttendance = AccessPolicy.canTakeAttendance(currentUser)
                        && (attendanceActivityIds == null || attendanceActivityIds.contains(id));
                request.setAttribute("canAccessAttendance", canAccessAttendance);
                boolean canOpenAttendancePage = canAccessAttendance
                        && activity.getApprovalStatus() == com.swp391.g1.model.Enum.ApprovalStatus.APPROVED
                        && activity.getActivityStatus() != com.swp391.g1.model.Enum.ActivityStatus.CANCELLED;
                request.setAttribute("canOpenAttendancePage", canOpenAttendancePage);

                // Nạp danh sách người tham gia và tìm bản ghi điểm danh của riêng sinh viên đang xem (nếu có)
                List<com.swp391.g1.dto.response.ActivityParticipantResponseDTO> allParticipants =
                        participantService.getParticipants(id);
                // Nếu là sinh viên thì tìm bản ghi tham gia tương ứng để hiển thị kết quả cá nhân
                if (studentView && myStudent != null) {
                    int mySid = myStudent.getId();
                    // Duyệt danh sách người tham gia để tìm khớp studentId của sinh viên hiện tại
                    for (com.swp391.g1.dto.response.ActivityParticipantResponseDTO p : allParticipants) {
                        // Tìm thấy bản ghi của chính sinh viên -> lưu vào request rồi thoát vòng lặp
                        if (p.getStudentId() == mySid) {
                            request.setAttribute("myParticipant", p);
                            break;
                        }
                    }
                }
                // Khởi tạo bảng đếm trạng thái điểm danh và tính tỷ lệ tham gia thực tế
                long attendedCount = 0;
                long absentCount = 0;
                long lateOrEarlyCount = 0;
                long notYetCount = 0;
                java.util.Map<com.swp391.g1.model.Enum.ParticipantStatus, Long> statusCounts =
                        new java.util.EnumMap<>(com.swp391.g1.model.Enum.ParticipantStatus.class);
                // Khởi tạo bộ đếm bằng 0 cho mọi giá trị enum ParticipantStatus
                for (com.swp391.g1.model.Enum.ParticipantStatus status : com.swp391.g1.model.Enum.ParticipantStatus.values()) {
                    statusCounts.put(status, 0L);
                }
                List<com.swp391.g1.dto.response.ActivityParticipantResponseDTO> markedParticipants = new ArrayList<>();
                // Duyệt toàn bộ người tham gia để thống kê số lượng từng nhóm trạng thái điểm danh
                for (com.swp391.g1.dto.response.ActivityParticipantResponseDTO p : allParticipants) {
                    // Quy ước trạng thái null là chưa điểm danh (NOT_YET)
                    com.swp391.g1.model.Enum.ParticipantStatus st = p.getAttendanceStatus() != null
                            ? p.getAttendanceStatus()
                            : com.swp391.g1.model.Enum.ParticipantStatus.NOT_YET;
                    statusCounts.put(st, statusCounts.getOrDefault(st, 0L) + 1L);
                    // Phân nhóm trạng thái điểm danh: có mặt / vắng / đi muộn hoặc về sớm / chưa điểm danh
                    if (st == com.swp391.g1.model.Enum.ParticipantStatus.ATTENDED) {
                        attendedCount++;
                        markedParticipants.add(p);
                    } else if (st == com.swp391.g1.model.Enum.ParticipantStatus.ABSENT) {
                        absentCount++;
                        markedParticipants.add(p);
                    } else if (st == com.swp391.g1.model.Enum.ParticipantStatus.IN_LATE
                            || st == com.swp391.g1.model.Enum.ParticipantStatus.OUT_EARLY) {
                        lateOrEarlyCount++;
                        markedParticipants.add(p);
                    } else {
                        // Nhóm chưa được điểm danh (NOT_YET hoặc trạng thái khác)
                        notYetCount++;
                    }
                }
                int totalParticipantsCount = allParticipants.size();
                long actualAttendedCount = attendedCount + lateOrEarlyCount;
                // Tính phần trăm tham dự thực tế (có mặt + đi muộn/về sớm), giới hạn tối đa 100%
                int attendanceRatePct = totalParticipantsCount > 0
                        ? Math.min(100, (int) Math.round((actualAttendedCount * 100.0) / totalParticipantsCount))
                        : 0;
                List<com.swp391.g1.dto.response.ActivityParticipantResponseDTO> sortedParticipants =
                        new ArrayList<>(allParticipants);
                // Sắp xếp ưu tiên các người tham gia đã được điểm danh lên đầu danh sách xem trước
                sortedParticipants.sort((a, b) -> {
                    boolean aMarked = a.getAttendanceStatus() != null
                            && a.getAttendanceStatus() != com.swp391.g1.model.Enum.ParticipantStatus.NOT_YET;
                    boolean bMarked = b.getAttendanceStatus() != null
                            && b.getAttendanceStatus() != com.swp391.g1.model.Enum.ParticipantStatus.NOT_YET;
                    // Người đã điểm danh luôn đứng trước người chưa điểm danh
                    if (aMarked != bMarked) {
                        return aMarked ? -1 : 1;
                    }
                    return 0;
                });
                List<com.swp391.g1.dto.response.ActivityParticipantResponseDTO> recentParticipants =
                        sortedParticipants.subList(0, Math.min(5, sortedParticipants.size()));
                request.setAttribute("participantStatuses", com.swp391.g1.model.Enum.ParticipantStatus.values());
                request.setAttribute("statusCounts", statusCounts);
                request.setAttribute("totalParticipantsCount", totalParticipantsCount);
                request.setAttribute("attendedCount", attendedCount);
                request.setAttribute("absentCount", absentCount);
                request.setAttribute("lateOrEarlyCount", lateOrEarlyCount);
                request.setAttribute("notYetCount", notYetCount);
                request.setAttribute("actualAttendedCount", actualAttendedCount);
                request.setAttribute("attendanceRatePct", attendanceRatePct);
                request.setAttribute("recentParticipants", recentParticipants);
                // Chỉ nạp danh sách công ty đối tác đang hoạt động cho người có quyền quản lý (để gửi lời mời)
                if (AccessPolicy.canManageActivities(AuthContext.getCurrentUser(request))) {
                    request.setAttribute("partnerCompanies", partnerCompanyService.getActivePartnerCompanies());
                }
                // Nạp dữ liệu phân công nhân sự nội bộ, đơn vị đồng phụ trách và lời mời hợp tác đối tác
                boolean canAssignStaff = activityStaffService.canAssignStaff(id, currentUser);
                request.setAttribute("canAddStaff", canAssignStaff);
                request.setAttribute("canRemoveStaff", canAssignStaff);
                request.setAttribute("invitations", invitationService.getInvitations(id, currentUser));
                request.setAttribute("approvedPartners", invitationService.getApprovedPartners(id));
                request.setAttribute("assignedStaffs", activityStaffService.getStaffsByActivityId(id));
                request.setAttribute("coDepartments", activityStaffService.getDepartmentsByActivityId(id));
                // Chỉ nạp danh sách chọn phòng ban/nhân sự khả dụng khi người dùng được phép phân công nhân sự
                if (canAssignStaff) {
                    request.setAttribute("availableDepartments", activityStaffService.getAvailableDepartmentsForActivity(id));
                    request.setAttribute("availableStaffs", activityStaffService.getAvailableStaffsForActivity(id));
                    request.setAttribute("staffDepartmentLabels", activityStaffService.getDepartmentLabelsByStaff(id));
                }
                boolean canManagePartnerStaff = activityPartnerStaffService.canManagePartnerStaff(id, currentUser);
                request.setAttribute("canManagePartnerStaff", canManagePartnerStaff);
                request.setAttribute("assignedPartnerStaffs", activityPartnerStaffService.getPartnerStaffsByActivityId(id, currentUser));
                // Chỉ nạp danh sách nhân sự đối tác khả dụng khi người dùng có quyền quản lý nhân sự đối tác
                if (canManagePartnerStaff) {
                    request.setAttribute("availablePartnerStaffs",
                            activityPartnerStaffService.getAvailablePartnerStaffsForActivity(id, currentUser));
                }

                // Tích hợp thông tin bài đánh giá/khảo sát (chỉ đọc, không gây tác dụng phụ)
                Assessment assessment = null;
                // Quản lý/ADMIN xem bài đánh giá theo quyền quản trị; sinh viên xem bản công khai dành cho sinh viên
                if (AccessPolicy.canManageActivities(currentUser)
                        || AccessPolicy.hasType(currentUser, "ADMIN")) {
                    assessment = assessmentService.getAssessmentByActivityId(id, currentUser);
                } else if (studentView) {
                    assessment = assessmentService.getAssessmentByActivityIdForStudent(id);
                }
                request.setAttribute("assessment", assessment);

                // Kiểm tra sinh viên hiện tại có đủ điều kiện làm bài đánh giá hay không
                if (studentView && assessment != null && myStudent != null) {
                    request.setAttribute("canStudentTake",
                            assessmentService.canStudentTake(assessment.getId(), myStudent.getId()));
                }

                request.getRequestDispatcher("/WEB-INF/views/activity/detail.jsp").forward(request, response);
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/activities");
    }

    /**
     * Kiểm tra hoạt động có đang ở trạng thái UPCOMING và chưa đến thời điểm bắt đầu hay không.
     *
     * @param activity thông tin hoạt động
     * @param now      mốc thời gian hiện tại
     * @return {@code true} nếu hoạt động sắp diễn ra và {@code now < startTime}
     */
    private boolean isUpcomingBeforeStart(ExtracurricularActivityResponseDTO activity, LocalDateTime now) {
        return activity != null
                && activity.getActivityStatus() == com.swp391.g1.model.Enum.ActivityStatus.UPCOMING
                && activity.getStartTime() != null
                && now.isBefore(activity.getStartTime());
    }

    /**
     * Xác định sinh viên có thể đăng ký tham gia hoạt động tại thời điểm {@code now} hay không.
     * Hỗ trợ cả hai chế độ: đăng ký nhanh (không yêu cầu form tùy chỉnh) và đăng ký qua form tùy chỉnh đang OPENING.
     *
     * @param activity thông tin hoạt động cần kiểm tra
     * @param form     biểu mẫu đăng ký gắn với hoạt động (có thể {@code null})
     * @param now      thời điểm hiện tại để đối chiếu khung giờ mở/đóng đăng ký
     * @return {@code true} nếu đang mở đăng ký và còn chỗ trống; ngược lại {@code false}
     */
    private boolean isRegistrationAvailable(ExtracurricularActivityResponseDTO activity,
                                            com.swp391.g1.model.RegistrationForm form,
                                            LocalDateTime now) {
        // Loại ngay nếu không thuộc loại yêu cầu đăng ký, chưa duyệt, không UPCOMING, không công khai hoặc đã bắt đầu
        if (activity.getNature() != com.swp391.g1.model.Enum.ActivityNature.REQUIRE_REGISTRATION
                || activity.getApprovalStatus() != com.swp391.g1.model.Enum.ApprovalStatus.APPROVED
                || activity.getActivityStatus() != com.swp391.g1.model.Enum.ActivityStatus.UPCOMING
                || !activity.isPublic()
                || (activity.getStartTime() != null && !now.isBefore(activity.getStartTime()))) {
            return false;
        }
        // Trường hợp đăng ký nhanh (không yêu cầu form riêng): chỉ cần kiểm tra còn chỗ so với maxParticipants
        if (!activity.isRegistrationFormRequired()) {
            Integer max = activity.getMaxParticipants();
            // Nếu chưa tạo bản ghi form nhanh thì số chỗ đã giữ bằng 0
            int reserved = form != null ? form.getReservedSeatCount() : 0;
            return max == null || reserved < max;
        }
        // Trường hợp bắt buộc có form đăng ký nhưng chưa tạo form -> chưa mở đăng ký
        if (form == null) {
            return false;
        }
        return "OPENING".equals(form.getStatus())
                && (form.getOpenAt() == null || !now.isBefore(form.getOpenAt()))
                && (form.getCloseAt() == null || now.isBefore(form.getCloseAt()))
                && (form.getMaxParticipants() == null
                || form.getReservedSeatCount() < form.getMaxParticipants());
    }

    /**
     * Xử lý yêu cầu POST xóa một hoạt động sau khi xác minh quyền truy cập của người dùng hiện tại.
     *
     * @param request  đối tượng yêu cầu HTTP chứa tham số {@code id}
     * @param response đối tượng phản hồi HTTP
     * @throws IOException nếu lỗi khi trả mã lỗi hoặc redirect
     */
    private void deleteActivity(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String idStr = request.getParameter("id");
        HttpSession session = request.getSession();
        // Chỉ thực hiện xóa khi tham số id hợp lệ
        if (idStr != null && !idStr.trim().isEmpty()) {
            int id = Integer.parseInt(idStr);
            try {
                // Kiểm tra phạm vi quyền truy cập hoạt động trước khi cho phép xóa
                if (!activityService.canAccessActivity(id, AuthContext.getCurrentUser(request))) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Bạn không có quyền xóa hoạt động này.");
                    return;
                }
                boolean success = activityService.deleteActivity(id);
                // Lưu thông báo kết quả xóa tương ứng vào session
                if (success) {
                    session.setAttribute("successMessage", "Xóa hoạt động thành công!");
                } else {
                    session.setAttribute("errorMessage", "Xóa hoạt động thất bại!");
                }
            } catch (IllegalArgumentException e) {
                // Hiển thị lỗi nghiệp vụ (ví dụ hoạt động đang diễn ra hoặc đã hoàn thành)
                session.setAttribute("errorMessage", e.getMessage());
            }
        }
        response.sendRedirect(request.getContextPath() + "/activities");
    }

    /**
     * API AJAX trả về mảng JSON danh sách nhân sự đối tác đang hoạt động (ACTIVE) thuộc một công ty đối tác.
     *
     * @param request  đối tượng yêu cầu HTTP chứa tham số {@code companyId}
     * @param response đối tượng phản hồi HTTP trả về nội dung {@code application/json}
     * @throws IOException nếu lỗi khi ghi phản hồi JSON
     */
    private void getStaffsByCompanyJson(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String companyIdStr = request.getParameter("companyId");
        StringBuilder json = new StringBuilder("[");

        // Chỉ truy vấn và dựng mảng JSON khi có companyId hợp lệ
        if (companyIdStr != null && !companyIdStr.trim().isEmpty()) {
            try {
                int companyId = Integer.parseInt(companyIdStr);
                List<PartnerStaffResponseDTO> staffs = partnerStaffService.getStaffsByCompanyId(companyId);

                // Lọc chỉ giữ lại các nhân sự đối tác đang có trạng thái ACTIVE
                staffs = staffs.stream().filter(st -> "ACTIVE".equalsIgnoreCase(st.getStatus())).toList();
                // Duyệt danh sách nhân sự đối tác để ghép từng đối tượng JSON {id, name, position}
                for (int i = 0; i < staffs.size(); i++) {
                    PartnerStaffResponseDTO staff = staffs.get(i);
                    json.append("{")
                            .append("\"id\":").append(staff.getId()).append(",")
                            .append("\"name\":\"").append(escapeJson(staff.getName())).append("\",")
                            // Chuẩn hóa chức vụ về chuỗi rỗng nếu null trước khi escape JSON
                            .append("\"position\":\"").append(escapeJson(staff.getPosition() != null ? staff.getPosition() : "")).append("\"")
                            .append("}");
                    // Chèn dấu phẩy ngăn cách giữa các phần tử JSON trừ phần tử cuối cùng
                    if (i < staffs.size() - 1) {
                        json.append(",");
                    }
                }
            } catch (NumberFormatException ignored) {
                // Bỏ qua companyId sai định dạng và trả về mảng JSON rỗng []
            }
        }
        json.append("]");

        try (PrintWriter out = response.getWriter()) {
            out.print(json.toString());
            out.flush();
        }
    }

    /**
     * API AJAX trả về mảng JSON danh sách nhân sự nội bộ thuộc một phòng ban/đơn vị phụ trách.
     *
     * @param request  đối tượng yêu cầu HTTP chứa tham số {@code departmentId}
     * @param response đối tượng phản hồi HTTP trả về nội dung {@code application/json}
     * @throws IOException nếu lỗi khi trả mã lỗi hoặc ghi phản hồi JSON
     */
    private void getStaffsByDepartmentJson(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String departmentIdValue = request.getParameter("departmentId");
        int departmentId = parseId(departmentIdValue);
        // Trả lỗi 400 nếu departmentId thiếu hoặc không phải số nguyên dương
        if (departmentId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Đơn vị phụ trách không hợp lệ.");
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        StringBuilder json = new StringBuilder("[");
        // Nhân sự được lọc thông qua bảng phân công staff_assignment theo phòng ban này nên cùng thuộc phòng ban đó
        Department department = departmentService.getDepartmentById(departmentId);
        // Xử lý an toàn giá trị mã và tên phòng ban khi null
        String departmentCode = department == null || department.getCode() == null ? "" : department.getCode();
        String departmentName = department == null || department.getName() == null ? "" : department.getName();
        List<Staff> staffs = staffService.getStaffsByDepartmentId(departmentId);
        // Duyệt danh sách nhân sự thuộc phòng ban để dựng chuỗi mảng JSON phản hồi
        for (int i = 0; i < staffs.size(); i++) {
            Staff staff = staffs.get(i);
            json.append("{")
                    .append("\"id\":").append(staff.getId()).append(",")
                    .append("\"departmentCode\":\"").append(escapeJson(departmentCode)).append("\",")
                    .append("\"departmentName\":\"").append(escapeJson(departmentName)).append("\",")
                    .append("\"code\":\"").append(escapeJson(staff.getCode())).append("\",")
                    .append("\"name\":\"").append(escapeJson(staff.getName())).append("\"")
                    .append("}");
            // Thêm dấu phẩy phân cách nếu chưa phải phần tử cuối
            if (i < staffs.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");

        try (PrintWriter out = response.getWriter()) {
            out.print(json);
            out.flush();
        }
    }

    // =========================================================================
    // POST ACTION HANDLERS
    // =========================================================================

    /**
     * Xử lý yêu cầu POST tạo mới hoạt động ngoại khóa.
     * Nếu tạo thành công sẽ chuyển về danh sách; nếu thất bại hoặc lỗi validate sẽ lưu flash form và quay lại form tạo.
     *
     * @param request  đối tượng yêu cầu HTTP chứa dữ liệu biểu mẫu
     * @param response đối tượng phản hồi HTTP
     * @throws ServletException nếu lỗi servlet
     * @throws IOException      nếu lỗi khi redirect
     */
    private void createActivity(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ExtracurricularActivityRequestDTO dto = buildRequestDTO(request);
        HttpSession session = request.getSession();

        try {
            boolean success = activityService.createActivity(dto);
            // Chuyển về danh sách khi lưu thành công; ngược lại quay về form tạo kèm thông báo lỗi
            if (success) {
                session.setAttribute("successMessage", "Tạo mới hoạt động thành công!");
                response.sendRedirect(request.getContextPath() + "/activities");
            } else {
                redirectToActivityForm(request, response, session, dto,
                        "Không thể tạo hoạt động. Vui lòng thử lại.", "create");
            }
        } catch (Exception e) {
            // Bắt lỗi validate hoặc lỗi hệ thống để giữ lại dữ liệu form đã nhập và hiển thị thông báo lỗi
            redirectToActivityForm(request, response, session, dto, e.getMessage(), "create");
        }
    }

    /**
     * Xử lý yêu cầu POST cập nhật thông tin hoạt động sau khi xác minh quyền truy cập.
     *
     * @param request  đối tượng yêu cầu HTTP chứa dữ liệu chỉnh sửa và {@code id}
     * @param response đối tượng phản hồi HTTP
     * @throws ServletException nếu lỗi servlet
     * @throws IOException      nếu lỗi khi trả mã lỗi hoặc redirect
     */
    private void updateActivity(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ExtracurricularActivityRequestDTO dto = buildRequestDTO(request);
        dto.setId(request.getParameter("id"));
        HttpSession session = request.getSession();

        int activityId = parseId(dto.getId());
        // Kiểm tra phạm vi quyền sửa hoạt động của người dùng hiện tại
        if (!activityService.canAccessActivity(activityId, AuthContext.getCurrentUser(request))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không có quyền sửa hoạt động này.");
            return;
        }

        try {
            boolean success = activityService.updateActivity(dto);
            // Nếu cập nhật thành công thì về danh sách; ngược lại quay về form sửa cùng dữ liệu đã nhập
            if (success) {
                session.setAttribute("successMessage", "Cập nhật hoạt động thành công! Hoạt động đã duyệt mà có thay đổi nội dung sẽ chuyển về Bản nháp và cần Gửi duyệt lại.");
                response.sendRedirect(request.getContextPath() + "/activities");
            } else {
                redirectToActivityForm(request, response, session, dto,
                        "Cập nhật không thành công. Vui lòng kiểm tra lại.", "edit");
            }
        } catch (Exception e) {
            // Bắt ngoại lệ nghiệp vụ/validate và chuyển ngược về form sửa kèm flash message
            redirectToActivityForm(request, response, session, dto, e.getMessage(), "edit");
        }
    }

    /**
     * Xử lý POST phân công một hoặc nhiều nhân sự nội bộ vào hoạt động (chỉ thực hiện trước khi hoạt động bắt đầu).
     *
     * @param request  đối tượng yêu cầu HTTP chứa {@code activityId} và mảng {@code staffIds}
     * @param response đối tượng phản hồi HTTP
     * @throws IOException nếu lỗi khi trả mã lỗi hoặc redirect
     */
    private void assignStaff(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        HttpSession session = request.getSession();
        int activityId = 0;
        try {
            activityId = Integer.parseInt(request.getParameter("activityId"));
            // Xác minh người dùng có quyền quản lý phân công nhân sự của hoạt động này
            if (!activityStaffService.canManageStaffAssignments(activityId, currentUser)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bạn không có quyền thêm nhân sự vào hoạt động này.");
                return;
            }
            // Kiểm tra hoạt động còn ở giai đoạn cho phép thay đổi phân công (trước giờ bắt đầu)
            if (!activityStaffService.canAssignStaff(activityId, currentUser)) {
                session.setAttribute("errorMessage",
                        "Không thể thêm nhân sự vì chỉ được phân công trước thời điểm hoạt động bắt đầu.");
                response.sendRedirect(request.getContextPath()
                        + "/activities?action=detail&id=" + activityId + "#tab-staff");
                return;
            }
            List<Integer> staffIds = parseSelectedIds(request.getParameterValues("staffIds"));
            int assigned = 0;
            String firstError = null;
            // Duyệt từng ID nhân sự được chọn để phân công độc lập và ghi nhận lỗi đầu tiên (nếu có)
            for (int staffId : staffIds) {
                try {
                    // Tăng số lượng thành công nếu gán được; ngược lại lưu lỗi đầu tiên gặp phải
                    if (activityStaffService.assignStaff(activityId, staffId, currentUser)) {
                        assigned++;
                    } else if (firstError == null) {
                        firstError = "Không thể phân công nhân sự có mã ID " + staffId + ".";
                    }
                } catch (Exception e) {
                    // Lưu thông điệp ngoại lệ đầu tiên mà vẫn tiếp tục xử lý các nhân sự còn lại
                    if (firstError == null) {
                        firstError = e.getMessage();
                    }
                }
            }
            setAssignmentResult(session, assigned, staffIds.size(), "nhân sự", "phân công", firstError);
        } catch (NumberFormatException e) {
            // Lỗi khi activityId không phải số nguyên hợp lệ
            session.setAttribute("errorMessage", "Dữ liệu phân công không hợp lệ.");
        } catch (IllegalArgumentException e) {
            // Lỗi validate danh sách ID nhân sự được chọn
            session.setAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            // Bắt các lỗi hệ thống không mong muốn trong quá trình phân công
            session.setAttribute("errorMessage", "Đã xảy ra lỗi khi phân công nhân sự: " + e.getMessage());
        }

        // Quay lại tab nhân sự của hoạt động nếu có activityId hợp lệ, ngược lại về trang danh sách
        if (activityId > 0) {
            response.sendRedirect(request.getContextPath() + "/activities?action=detail&id=" + activityId + "#tab-staff");
        } else {
            response.sendRedirect(request.getContextPath() + "/activities");
        }
    }

    /**
     * Xử lý POST thêm một hoặc nhiều đơn vị đồng phụ trách vào hoạt động trước thời điểm hoạt động bắt đầu.
     *
     * @param request  đối tượng yêu cầu HTTP chứa {@code activityId} và mảng {@code departmentIds}
     * @param response đối tượng phản hồi HTTP
     * @throws IOException nếu lỗi khi trả mã lỗi hoặc redirect
     */
    private void addDepartment(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        HttpSession session = request.getSession();
        int activityId = 0;
        try {
            activityId = Integer.parseInt(request.getParameter("activityId"));
            // Kiểm tra quyền quản lý nhân sự/đơn vị của hoạt động
            if (!activityStaffService.canManageStaffAssignments(activityId, currentUser)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bạn không có quyền thêm đơn vị đồng phụ trách vào hoạt động này.");
                return;
            }
            // Chỉ cho phép thêm đơn vị đồng phụ trách trước khi hoạt động bắt đầu
            if (!activityStaffService.canAssignStaff(activityId, currentUser)) {
                session.setAttribute("errorMessage",
                        "Không thể thêm đơn vị đồng phụ trách vì chỉ được thay đổi trước thời điểm hoạt động bắt đầu.");
                redirectToStaffTab(request, response, activityId);
                return;
            }
            List<Integer> departmentIds = parseSelectedIds(request.getParameterValues("departmentIds"));
            int added = 0;
            String firstError = null;
            // Duyệt từng đơn vị được chọn để thêm lần lượt vào hoạt động
            for (int departmentId : departmentIds) {
                try {
                    // Đếm số đơn vị thêm thành công và giữ lại thông báo lỗi đầu tiên nếu có đơn vị thất bại
                    if (activityStaffService.addDepartment(activityId, departmentId, currentUser)) {
                        added++;
                    } else if (firstError == null) {
                        firstError = "Không thể thêm đơn vị có mã ID " + departmentId + ".";
                    }
                } catch (Exception e) {
                    // Ghi nhận thông báo lỗi đầu tiên và tiếp tục vòng lặp cho các đơn vị còn lại
                    if (firstError == null) {
                        firstError = e.getMessage();
                    }
                }
            }
            setAssignmentResult(session, added, departmentIds.size(), "đơn vị", "thêm", firstError);
        } catch (NumberFormatException e) {
            // Tham số activityId không hợp lệ
            session.setAttribute("errorMessage", "Dữ liệu đơn vị đồng phụ trách không hợp lệ.");
        } catch (Exception e) {
            // Bắt lỗi validate hoặc lỗi hệ thống khi thêm đơn vị đồng phụ trách
            session.setAttribute("errorMessage", "Đã xảy ra lỗi khi thêm đơn vị đồng phụ trách: " + e.getMessage());
        }
        redirectToStaffTab(request, response, activityId);
    }

    /**
     * Xử lý POST gỡ một đơn vị đồng phụ trách khỏi hoạt động (đồng thời gỡ nhân sự chỉ thuộc đơn vị đó).
     *
     * @param request  đối tượng yêu cầu HTTP chứa {@code activityId} và {@code departmentId}
     * @param response đối tượng phản hồi HTTP
     * @throws IOException nếu lỗi khi trả mã lỗi hoặc redirect
     */
    private void removeDepartment(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        HttpSession session = request.getSession();
        int activityId = 0;
        try {
            activityId = Integer.parseInt(request.getParameter("activityId"));
            // Kiểm tra quyền quản lý phân công đơn vị của hoạt động
            if (!activityStaffService.canManageStaffAssignments(activityId, currentUser)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bạn không có quyền gỡ đơn vị đồng phụ trách khỏi hoạt động này.");
                return;
            }
            // Chỉ cho phép gỡ đơn vị đồng phụ trách trước thời điểm hoạt động bắt đầu
            if (!activityStaffService.canAssignStaff(activityId, currentUser)) {
                session.setAttribute("errorMessage",
                        "Không thể gỡ đơn vị đồng phụ trách vì chỉ được thay đổi trước thời điểm hoạt động bắt đầu.");
                redirectToStaffTab(request, response, activityId);
                return;
            }
            int departmentId = Integer.parseInt(request.getParameter("departmentId"));
            // Gán flash message thành công hoặc thất bại theo kết quả gỡ đơn vị
            if (activityStaffService.removeDepartment(activityId, departmentId, currentUser)) {
                session.setAttribute("successMessage",
                        "Đã gỡ đơn vị đồng phụ trách. Nhân sự chỉ thuộc đơn vị này cũng đã được gỡ khỏi hoạt động.");
            } else {
                session.setAttribute("errorMessage", "Không thể gỡ đơn vị đồng phụ trách.");
            }
        } catch (NumberFormatException e) {
            // Lỗi định dạng số ở activityId hoặc departmentId
            session.setAttribute("errorMessage", "Dữ liệu không hợp lệ.");
        } catch (Exception e) {
            // Lỗi nghiệp vụ hoặc hệ thống khi gỡ đơn vị đồng phụ trách
            session.setAttribute("errorMessage", "Đã xảy ra lỗi khi gỡ đơn vị đồng phụ trách: " + e.getMessage());
        }
        redirectToStaffTab(request, response, activityId);
    }

    /**
     * Chuyển hướng người dùng về tab Nhân sự ({@code #tab-staff}) của trang chi tiết hoạt động nếu có ID hợp lệ,
     * ngược lại chuyển về trang danh sách hoạt động.
     *
     * @param request    đối tượng yêu cầu HTTP
     * @param response   đối tượng phản hồi HTTP
     * @param activityId mã định danh hoạt động
     * @throws IOException nếu lỗi khi redirect
     */
    private void redirectToStaffTab(HttpServletRequest request, HttpServletResponse response, int activityId)
            throws IOException {
        // Điều hướng về đúng tab nhân sự khi activityId > 0; ngược lại về danh sách chung
        if (activityId > 0) {
            response.sendRedirect(request.getContextPath() + "/activities?action=detail&id=" + activityId + "#tab-staff");
        } else {
            response.sendRedirect(request.getContextPath() + "/activities");
        }
    }

    /**
     * Xử lý POST hủy phân công một nhân sự nội bộ khỏi hoạt động trước thời điểm bắt đầu.
     *
     * @param request  đối tượng yêu cầu HTTP chứa {@code activityId} và {@code staffId}
     * @param response đối tượng phản hồi HTTP
     * @throws IOException nếu lỗi khi trả mã lỗi hoặc redirect
     */
    private void unassignStaff(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        HttpSession session = request.getSession();
        int activityId = 0;
        try {
            activityId = Integer.parseInt(request.getParameter("activityId"));
            // Kiểm tra quyền quản lý phân công nhân sự của hoạt động
            if (!activityStaffService.canManageStaffAssignments(activityId, currentUser)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bạn không có quyền hủy phân công nhân sự khỏi hoạt động này.");
                return;
            }
            // Không cho phép hủy phân công nếu hoạt động đã bắt đầu
            if (!activityStaffService.canAssignStaff(activityId, currentUser)) {
                session.setAttribute("errorMessage",
                        "Không thể hủy phân công nhân sự vì chỉ được thay đổi trước thời điểm hoạt động bắt đầu.");
                response.sendRedirect(request.getContextPath()
                        + "/activities?action=detail&id=" + activityId + "#tab-staff");
                return;
            }
            int staffId = Integer.parseInt(request.getParameter("staffId"));
            boolean success = activityStaffService.removeStaff(activityId, staffId, currentUser);
            // Cập nhật flash message tương ứng kết quả hủy phân công
            if (success) {
                session.setAttribute("successMessage", "Đã hủy phân công nhân sự khỏi hoạt động.");
            } else {
                session.setAttribute("errorMessage", "Không thể hủy phân công nhân sự.");
            }
        } catch (NumberFormatException e) {
            // Lỗi định dạng số nguyên ở activityId hoặc staffId
            session.setAttribute("errorMessage", "Dữ liệu không hợp lệ.");
        } catch (Exception e) {
            // Bắt lỗi nghiệp vụ hoặc hệ thống phát sinh khi hủy phân công
            session.setAttribute("errorMessage", "Đã xảy ra lỗi khi hủy phân công: " + e.getMessage());
        }

        // Chuyển hướng về tab nhân sự của hoạt động nếu có ID hợp lệ, ngược lại về danh sách
        if (activityId > 0) {
            response.sendRedirect(request.getContextPath() + "/activities?action=detail&id=" + activityId + "#tab-staff");
        } else {
            response.sendRedirect(request.getContextPath() + "/activities");
        }
    }

    /**
     * Xử lý POST phân công nhân sự đối tác vào hoạt động bằng cách ủy quyền cho {@link #handlePartnerStaffChange}.
     *
     * @param request  đối tượng yêu cầu HTTP
     * @param response đối tượng phản hồi HTTP
     * @throws IOException nếu lỗi khi trả mã lỗi hoặc redirect
     */
    private void assignPartnerStaff(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        handlePartnerStaffChange(request, response, true);
    }

    /**
     * Xử lý thống nhất toàn bộ luồng lời mời hợp tác đối tác (gửi mời, phản hồi, hủy phản hồi,
     * yêu cầu ngừng hợp tác và hủy lời mời), sau đó điều hướng về trang gọi tương ứng.
     *
     * @param request   đối tượng yêu cầu HTTP
     * @param response  đối tượng phản hồi HTTP
     * @param operation loại thao tác ({@code "invite"}, {@code "respond"}, {@code "revoke"}, {@code "terminate"}, mặc định {@code "cancel"})
     * @throws IOException nếu lỗi khi redirect
     */
    private void handleInvitation(HttpServletRequest request, HttpServletResponse response, String operation)
            throws IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        HttpSession session = request.getSession();
        int activityId = parseId(request.getParameter("activityId"));
        try {
            // Rẽ nhánh thực thi nghiệp vụ lời mời đối tác theo loại thao tác operation
            switch (operation) {
                // Gửi lời mời hợp tác mới tới công ty/nhân sự đối tác
                case "invite":
                    invitationService.invite(activityId, request.getParameter("partnerCompanyId"),
                            request.getParameter("partnerStaffId"), request.getParameter("message"), currentUser);
                    session.setAttribute("successMessage", "Đã gửi lời mời đối tác tham gia hoạt động.");
                    break;
                // Đối tác chấp nhận hoặc từ chối lời mời hợp tác
                case "respond":
                    boolean accept = "accept".equals(request.getParameter("decision"));
                    invitationService.respond(parseId(request.getParameter("invitationId")), accept, currentUser);
                    // Chọn nội dung thông báo tương ứng quyết định chấp nhận hay từ chối
                    session.setAttribute("successMessage",
                            accept ? "Đã chấp nhận lời mời." : "Đã từ chối lời mời.");
                    break;
                // Hủy phản hồi đã gửi để đưa lời mời về trạng thái chờ phản hồi
                case "revoke":
                    invitationService.revoke(parseId(request.getParameter("invitationId")), currentUser);
                    session.setAttribute("successMessage", "Đã hủy phản hồi lời mời. Lời mời quay lại trạng thái chờ phản hồi.");
                    break;
                // Gửi đề nghị ngừng hợp tác với đối tác đã liên kết
                case "terminate":
                    invitationService.requestTermination(activityId,
                            parseId(request.getParameter("partnerCompanyLinkId")),
                            request.getParameter("message"), currentUser);
                    session.setAttribute("successMessage",
                            "Đã gửi lời mời ngừng hợp tác. Hợp tác chỉ kết thúc sau khi đối tác chấp nhận.");
                    break;
                // Mặc định: hủy lời mời hợp tác đang chờ xử lý
                default:
                    invitationService.cancel(parseId(request.getParameter("invitationId")), currentUser);
                    session.setAttribute("successMessage", "Đã hủy lời mời.");
                    break;
            }
        } catch (SecurityException | IllegalArgumentException e) {
            // Hiển thị thông báo lỗi phân quyền hoặc vi phạm trạng thái lời mời
            session.setAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            // Bắt các lỗi hệ thống khác trong quá trình xử lý lời mời
            session.setAttribute("errorMessage", "Không thể xử lý lời mời: " + e.getMessage());
        }
        // Điều hướng quay lại đúng trang nguồn (trang lời mời đối tác, tab đối tác của hoạt động, hoặc danh sách)
        if ("partner-invitations".equals(request.getParameter("returnTo"))) {
            response.sendRedirect(request.getContextPath() + "/partner-invitations");
        } else if (activityId > 0) {
            response.sendRedirect(request.getContextPath()
                    + "/activities?action=detail&id=" + activityId + "#tab-partner");
        } else {
            response.sendRedirect(request.getContextPath() + "/activities");
        }
    }

    /**
     * Xử lý POST gỡ phân công nhân sự đối tác khỏi hoạt động bằng cách ủy quyền cho {@link #handlePartnerStaffChange}.
     *
     * @param request  đối tượng yêu cầu HTTP
     * @param response đối tượng phản hồi HTTP
     * @throws IOException nếu lỗi khi trả mã lỗi hoặc redirect
     */
    private void unassignPartnerStaff(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        handlePartnerStaffChange(request, response, false);
    }

    /**
     * Hàm dùng chung xử lý cả hai thao tác phân công ({@code assign = true}) và gỡ phân công ({@code assign = false})
     * cho nhân sự đối tác của hoạt động, kèm kiểm tra quyền, kiểm tra thời điểm trước bắt đầu và tổng hợp kết quả.
     *
     * @param request  đối tượng yêu cầu HTTP
     * @param response đối tượng phản hồi HTTP
     * @param assign   {@code true} để phân công thêm; {@code false} để gỡ phân công
     * @throws IOException nếu lỗi khi trả mã lỗi hoặc redirect
     */
    private void handlePartnerStaffChange(HttpServletRequest request, HttpServletResponse response, boolean assign)
            throws IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        HttpSession session = request.getSession();
        int activityId = 0;
        try {
            activityId = Integer.parseInt(request.getParameter("activityId"));
            // Kiểm tra quyền quản lý nhân sự đối tác của hoạt động
            if (!activityPartnerStaffService.canManagePartnerStaff(activityId, currentUser)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bạn không có quyền quản lý nhân sự đối tác của hoạt động này.");
                return;
            }
            // Chỉ cho phép thêm/gỡ nhân sự đối tác trước thời điểm hoạt động bắt đầu
            if (!activityPartnerStaffService.canAssignPartnerStaff(activityId, currentUser)) {
                // Chọn thông báo lỗi phù hợp theo chiều thao tác phân công hay gỡ phân công
                session.setAttribute("errorMessage", assign
                        ? "Không thể phân công nhân sự đối tác vì chỉ được thay đổi trước thời điểm hoạt động bắt đầu."
                        : "Không thể gỡ nhân sự đối tác vì chỉ được thay đổi trước thời điểm hoạt động bắt đầu.");
                // Quay lại trang lời mời đối tác nếu gọi từ đó, ngược lại về tab đối tác của hoạt động
                if ("partner-invitations".equals(request.getParameter("returnTo"))) {
                    response.sendRedirect(request.getContextPath() + "/partner-invitations");
                } else {
                    response.sendRedirect(request.getContextPath()
                            + "/activities?action=detail&id=" + activityId + "#tab-partner");
                }
                return;
            }
            // Khi phân công cho phép chọn nhiều ID (partnerStaffIds); khi gỡ nhận 1 ID đơn lẻ (partnerStaffId)
            String[] selectedPartnerIds = assign
                    ? request.getParameterValues("partnerStaffIds")
                    : new String[] {request.getParameter("partnerStaffId")};
            List<Integer> partnerStaffIds = parseSelectedIds(selectedPartnerIds);
            int completed = 0;
            String firstError = null;
            // Duyệt từng ID nhân sự đối tác để thực hiện phân công/gỡ phân công độc lập
            for (int partnerStaffId : partnerStaffIds) {
                try {
                    // Gọi phương thức phân công hoặc gỡ phân công tương ứng cờ assign
                    boolean success = assign
                            ? activityPartnerStaffService.assignPartnerStaff(activityId, partnerStaffId, currentUser)
                            : activityPartnerStaffService.removePartnerStaff(activityId, partnerStaffId, currentUser);
                    // Tăng bộ đếm thành công hoặc lưu lỗi đầu tiên nếu thao tác trả về false
                    if (success) {
                        completed++;
                    } else if (firstError == null) {
                        firstError = "Không thể thực hiện thao tác với nhân sự đối tác có mã ID "
                                + partnerStaffId + ".";
                    }
                } catch (Exception e) {
                    // Ghi nhận thông điệp ngoại lệ đầu tiên để báo lại cho người dùng sau vòng lặp
                    if (firstError == null) {
                        firstError = e.getMessage();
                    }
                }
            }
            // Tổng hợp kết quả toàn bộ/một phần/thất bại kèm nhãn thao tác tương ứng
            setAssignmentResult(session, completed, partnerStaffIds.size(), "nhân sự đối tác",
                    assign ? "phân công" : "gỡ phân công", firstError);
        } catch (NumberFormatException e) {
            // Lỗi định dạng số nguyên ở activityId
            session.setAttribute("errorMessage", "Dữ liệu không hợp lệ.");
        } catch (IllegalArgumentException e) {
            // Lỗi validate danh sách ID nhân sự đối tác
            session.setAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            // Bắt lỗi hệ thống phát sinh ngoài dự kiến
            session.setAttribute("errorMessage", "Đã xảy ra lỗi: " + e.getMessage());
        }

        // Chuyển hướng về trang gọi tương ứng (trang lời mời đối tác, tab đối tác, hoặc danh sách hoạt động)
        if ("partner-invitations".equals(request.getParameter("returnTo"))) {
            response.sendRedirect(request.getContextPath() + "/partner-invitations");
        } else if (activityId > 0) {
            response.sendRedirect(request.getContextPath()
                    + "/activities?action=detail&id=" + activityId + "#tab-partner");
        } else {
            response.sendRedirect(request.getContextPath() + "/activities");
        }
    }

    /**
     * Phân tích mảng chuỗi ID được chọn từ form thành danh sách số nguyên dương không trùng lặp (giữ thứ tự).
     *
     * @param values mảng chuỗi ID từ tham số request
     * @return danh sách ID nguyên dương duy nhất
     * @throws IllegalArgumentException nếu mảng rỗng hoặc chứa giá trị không phải số nguyên dương
     */
    private List<Integer> parseSelectedIds(String[] values) {
        // Yêu cầu người dùng phải chọn ít nhất một phần tử
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một nhân sự.");
        }
        Set<Integer> ids = new LinkedHashSet<>();
        // Duyệt mảng chuỗi ID để kiểm tra hợp lệ và loại bỏ phần tử trùng lặp
        for (String value : values) {
            try {
                int id = Integer.parseInt(value);
                // Chỉ chấp nhận ID lớn hơn 0
                if (id <= 0) {
                    throw new NumberFormatException();
                }
                ids.add(id);
            } catch (NumberFormatException e) {
                // Chuyển lỗi parse thành IllegalArgumentException với thông báo tiếng Việt
                throw new IllegalArgumentException("Dữ liệu nhân sự được chọn không hợp lệ.");
            }
        }
        return new ArrayList<>(ids);
    }

    /**
     * Lưu thông báo kết quả phân công/thêm/gỡ hàng loạt vào session dựa trên tỷ lệ thành công.
     *
     * @param session     session hiện tại
     * @param completed   số lượng xử lý thành công
     * @param requested   tổng số lượng yêu cầu
     * @param staffLabel  nhãn đối tượng (ví dụ "nhân sự", "đơn vị", "nhân sự đối tác")
     * @param actionLabel nhãn hành động (ví dụ "phân công", "thêm", "gỡ phân công")
     * @param firstError  thông báo lỗi đầu tiên ghi nhận được (có thể {@code null})
     */
    private void setAssignmentResult(HttpSession session, int completed, int requested,
                                     String staffLabel, String actionLabel, String firstError) {
        // Phân 3 trường hợp: thành công toàn bộ -> successMessage; thành công một phần hoặc thất bại toàn bộ -> errorMessage
        if (completed == requested) {
            session.setAttribute("successMessage",
                    "Đã " + actionLabel + " " + completed + " " + staffLabel + ".");
        } else if (completed > 0) {
            session.setAttribute("errorMessage",
                    "Đã " + actionLabel + " " + completed + "/" + requested + " " + staffLabel
                            + ". Một số lựa chọn không thành công: " + firstError);
        } else {
            // Dùng thông báo mặc định nếu không ghi nhận được thông điệp lỗi cụ thể
            session.setAttribute("errorMessage",
                    "Không thể " + actionLabel + " " + staffLabel + ": "
                            + (firstError == null ? "Vui lòng chọn nhân sự." : firstError));
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Nạp toàn bộ danh mục phục vụ các ô chọn (dropdown/multi-select) trên form tạo và chỉnh sửa hoạt động.
     *
     * @param request      đối tượng yêu cầu HTTP để gắn attribute danh mục
     * @param departmentId ID phòng ban đang được chọn nhằm nạp danh sách nhân sự tương ứng
     */
    private void loadDropdownData(HttpServletRequest request, int departmentId) {
        request.setAttribute("semesters", semesterService.getAllSemesters());
        request.setAttribute("activityTypes", activityTypeService.getActiveActivityTypes());
        request.setAttribute("organizationLevels", organizationLevelService.getAllOrganizationLevels());
        request.setAttribute("departments", departmentService.getAllDepartments());
        request.setAttribute("staffs", staffService.getStaffsByDepartmentId(departmentId));
        request.setAttribute("targetDepartments", targetParticipantService.getActiveAcademicDepartments());
        request.setAttribute("targetCohorts", targetParticipantService.getActiveCohorts());
        request.setAttribute("targetMajors", targetParticipantService.getActiveMajors());
        request.setAttribute("targetPrograms", targetParticipantService.getActiveAcademicPrograms());
        request.setAttribute("targetClasses", targetParticipantService.getActiveMainClasses());
    }

    /**
     * Chuyển đổi chuỗi tham số ID sang {@code int} một cách an toàn; trả về {@code 0} nếu rỗng hoặc lỗi định dạng.
     *
     * @param value chuỗi ID cần phân tích
     * @return giá trị số nguyên hoặc {@code 0} nếu không hợp lệ
     */
    private int parseId(String value) {
        // Trả về 0 nếu chuỗi null hoặc chỉ chứa khoảng trắng
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            // Trả về 0 khi chuỗi không thể parse sang số nguyên
            return 0;
        }
    }

    /**
     * Gắn hai cờ phân quyền {@code canManageActivities} và {@code canApproveActivities} vào request
     * để JSP ẩn/hiện các nút thao tác quản trị và phê duyệt phù hợp với vai trò người dùng.
     *
     * @param request đối tượng yêu cầu HTTP
     */
    private void setActivityManagementPermission(HttpServletRequest request) {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        request.setAttribute("canManageActivities",
                AccessPolicy.canManageActivities(currentUser));
        request.setAttribute("canApproveActivities",
                AccessPolicy.canReviewActivities(currentUser));
    }

    /**
     * Thu thập toàn bộ tham số từ biểu mẫu tạo/sửa hoạt động và đóng gói thành {@link ExtracurricularActivityRequestDTO}.
     *
     * @param request đối tượng yêu cầu HTTP chứa tham số form
     * @return đối tượng {@link ExtracurricularActivityRequestDTO}
     */
    private ExtracurricularActivityRequestDTO buildRequestDTO(HttpServletRequest request) {
        ExtracurricularActivityRequestDTO dto = new ExtracurricularActivityRequestDTO();
        dto.setCode(request.getParameter("code"));
        dto.setName(request.getParameter("name"));
        dto.setSemesterId(request.getParameter("semesterId"));
        dto.setActivityTypeId(request.getParameter("activityTypeId"));
        dto.setOrganizationLevelId(request.getParameter("organizationLevelId"));
        dto.setResponsibleDepartmentId(request.getParameter("responsibleDepartmentId"));
        dto.setResponsibleStaffId(request.getParameter("responsibleStaffId"));
        dto.setNature(request.getParameter("nature"));
        // Checkbox chỉ gửi tham số lên khi được tích chọn -> khác null tương ứng "true"
        dto.setSummaryRequired(request.getParameter("summaryRequired") != null ? "true" : "false");
        dto.setMaxParticipants(request.getParameter("maxParticipants"));
        String sessionCountParam = request.getParameter("attendanceSessionCount");
        // Mặc định 1 buổi điểm danh nếu người dùng bỏ trống tham số
        dto.setAttendanceSessionCount(sessionCountParam == null || sessionCountParam.isBlank() ? "1" : sessionCountParam.trim());
        dto.setAddress(request.getParameter("address"));             // Đổi location -> address
        dto.setStartTime(request.getParameter("startTime"));         // Đổi startDateTime -> startTime
        dto.setEndTime(request.getParameter("endTime"));             // Đổi endDateTime -> endTime
        dto.setDescription(request.getParameter("description"));
        // Chuyển trạng thái checkbox yêu cầu biểu mẫu đăng ký sang chuỗi "true"/"false"
        dto.setRegistrationFormRequired(request.getParameter("registrationFormRequired") != null ? "true" : "false");
        dto.setTargetScope(request.getParameter("targetScope"));
        dto.setTargetDepartmentIds(toList(request.getParameterValues("targetDepartmentIds")));
        dto.setTargetCohortIds(toList(request.getParameterValues("targetCohortIds")));
        dto.setTargetMajorIds(toList(request.getParameterValues("targetMajorIds")));
        dto.setTargetProgramIds(toList(request.getParameterValues("targetProgramIds")));
        dto.setTargetClassIds(toList(request.getParameterValues("targetClassIds")));
        return dto;
    }

    /**
     * Chuyển mảng chuỗi tham số nhiều giá trị sang {@link List}; trả về danh sách rỗng nếu mảng null hoặc rỗng.
     *
     * @param values mảng chuỗi đầu vào
     * @return danh sách chuỗi tương ứng
     */
    private List<String> toList(String[] values) {
        // Trả về danh sách bất biến rỗng nếu không có giá trị nào được chọn
        if (values == null || values.length == 0) {
            return List.of();
        }
        return java.util.Arrays.asList(values);
    }

    /**
     * Thoát các ký tự đặc biệt ({@code \} và {@code "}) để đảm bảo chuỗi an toàn khi nhúng vào JSON thủ công.
     *
     * @param input chuỗi gốc cần escape
     * @return chuỗi đã thoát ký tự đặc biệt JSON, hoặc {@code ""} nếu {@code input} null
     */
    private String escapeJson(String input) {
        // Trả về chuỗi rỗng nếu input null để tránh ghi chữ "null" vào JSON
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}