package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.ActivitySeries;
import com.swp391.g1.model.ActivityType;
import com.swp391.g1.model.Department;
import com.swp391.g1.model.Enum;
import com.swp391.g1.model.Semester;
import com.swp391.g1.model.Staff;
import com.swp391.g1.model.ExtracurricularActivity;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IDepartmentService;
import com.swp391.g1.service.IActivitySeriesService;
import com.swp391.g1.service.IActivityTypeService;
import com.swp391.g1.service.IStaffService;
import com.swp391.g1.service.ISemesterService;
import com.swp391.g1.service.impl.ActivitySeriesServiceImpl;
import com.swp391.g1.service.impl.ActivityTypeServiceImpl;
import com.swp391.g1.service.impl.DepartmentServiceImpl;
import com.swp391.g1.service.impl.SemesterServiceImpl;
import com.swp391.g1.service.impl.StaffServiceImpl;
import com.swp391.g1.util.AuthContext;

import com.google.gson.Gson;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@WebServlet(name = "ActivitySeriesServlet",
        urlPatterns = {"/activity-series"})
public class ActivitySeriesServlet extends HttpServlet {

    private final IActivitySeriesService seriesService = new ActivitySeriesServiceImpl();
    private final IActivityTypeService activityTypeService = new ActivityTypeServiceImpl();
    private final IDepartmentService departmentService = new DepartmentServiceImpl();
    private final ISemesterService semesterService = new SemesterServiceImpl();
    private final IStaffService staffService = new StaffServiceImpl();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        action = action == null ? "" : action.trim();

        boolean isGetRequest = "GET".equalsIgnoreCase(request.getMethod())
                || "HEAD".equalsIgnoreCase(request.getMethod());
        boolean requiresManagementPermission =
                (isGetRequest && ("create".equals(action) || "edit".equals(action)
                        || "list".equals(action) || "detail".equals(action)
                        || "listOccurrences".equals(action)))
                || ("POST".equalsIgnoreCase(request.getMethod())
                        && ("create".equals(action) || "update".equals(action)
                                || "delete".equals(action) || "generate".equals(action)));

        if (requiresManagementPermission
                && !AccessPolicy.canManageActivities(AuthContext.getCurrentUser(request))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ tài khoản STAFF hoặc ADMIN mới được thêm, sửa, xóa chuỗi hoạt động.");
            return;
        }

        super.service(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }

        try {
            switch (action) {
                case "create":
                    showCreateForm(request, response);
                    break;
                case "edit":
                    showEditForm(request, response);
                    break;
                case "detail":
                    showSeries(request, response);
                    break;
                case "list":
                default:
                    listSeries(request, response);
                    break;
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("errorMessage", "ÄÃ£ xáº£y ra lá»—i há»‡ thá»‘ng: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }

        try {
            switch (action) {
                case "create":
                    createSeries(request, response);
                    break;
                case "update":
                    updateSeries(request, response);
                    break;
                case "delete":
                    deleteSeries(request, response);
                    break;
                case "generate":
                    generateOccurrences(request, response);
                    break;
                default:
                    response.sendRedirect(request.getContextPath() + "/activity-series");
                    break;
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("errorMessage", "Lá»—i xá»­ lÃ½ dá»¯ liá»‡u: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    // =========================================================================
    // GET ACTION HANDLERS
    // =========================================================================

    private void listSeries(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        List<ActivitySeries> seriesList = seriesService.getAllSeries();
        request.setAttribute("seriesList", seriesList);
        request.setAttribute("canManageActivities",
                AccessPolicy.canManageActivities(currentUser));
        request.getRequestDispatcher("/WEB-INF/views/activity-series/list.jsp")
                .forward(request, response);
    }

    private void showSeries(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int id = parseId(request.getParameter("id"));
        if (id <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID chuá»—i hoáº¡t Ä‘á»™ng khÃ´ng há»£p lá»‡.");
            return;
        }
        ActivitySeries series = seriesService.getSeriesById(id);
        if (series == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y chuá»—i hoáº¡t Ä‘á»™ng.");
            return;
        }
        request.setAttribute("series", series);
        request.getRequestDispatcher("/WEB-INF/views/activity-series/detail.jsp")
                .forward(request, response);
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        loadDropdownData(request, 0);
        request.getRequestDispatcher("/WEB-INF/views/activity-series/create.jsp")
                .forward(request, response);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int id = parseId(request.getParameter("id"));
        if (id <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID chuá»—i hoáº¡t Ä‘á»™ng khÃ´ng há»£p lá»‡.");
            return;
        }
        ActivitySeries series = seriesService.getSeriesById(id);
        if (series == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y chuá»—i hoáº¡t Ä‘á»™ng.");
            return;
        }
        request.setAttribute("series", series);
        loadDropdownData(request, series.getResponsibleDepartmentId());
        request.getRequestDispatcher("/WEB-INF/views/activity-series/edit.jsp")
                .forward(request, response);
    }

    private void loadDropdownData(HttpServletRequest request, int departmentId) {
        request.setAttribute("semesters", semesterService.getAllSemesters());
        request.setAttribute("activityTypes", activityTypeService.getActiveActivityTypes());
        request.setAttribute("departments", departmentService.getAllDepartments());
        List<Staff> staffs;
        if (departmentId > 0) {
            staffs = staffService.getStaffsByDepartmentId(departmentId);
        } else {
            staffs = staffService.getAllStaffs();
        }
        request.setAttribute("staffs", staffs);
    }

    // =========================================================================
    // POST ACTION HANDLERS
    // =========================================================================

    private void createSeries(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        HttpSession session = request.getSession();
        try {
            ActivitySeries series = buildSeriesFromRequest(request);
            if (series.getResponsibleDepartmentId() > 0 && series.getResponsibleStaffId() > 0) {
                Staff staff = staffService.getStaffById(series.getResponsibleStaffId());
                Department dept = departmentService.getDepartmentById(series.getResponsibleDepartmentId());
                if (staff == null || dept == null) {
                    throw new IllegalArgumentException("Phòng ban hoặc nhân viên phụ trách không tồn tại.");
                }
                List<Staff> staffsInDept = staffService.getStaffsByDepartmentId(series.getResponsibleDepartmentId());
                boolean staffBelongsToDept = staffsInDept.stream().anyMatch(s -> s.getId() == series.getResponsibleStaffId());
                if (!staffBelongsToDept) {
                    throw new IllegalArgumentException("Nhân viên phụ trách không thuộc phòng ban đã chọn.");
                }
            }
            boolean success = seriesService.createSeries(series, currentUser);
            if (success) {
                session.setAttribute("successMessage", "Tạo chuỗi hoạt động thành công!");
                response.sendRedirect(request.getContextPath() + "/activity-series");
            } else {
                session.setAttribute("errorMessage", "Không thể tạo chuỗi hoạt động. Vui lòng thử lại.");
                response.sendRedirect(request.getContextPath() + "/activity-series?action=create");
            }
        } catch (IllegalArgumentException e) {
            loadDropdownData(request, parseId(request.getParameter("responsibleDepartmentId")));
            request.setAttribute("errorMessage", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/activity-series/create.jsp")
                    .forward(request, response);
            return;
        } catch (Exception e) {
            loadDropdownData(request, parseId(request.getParameter("responsibleDepartmentId")));
            request.setAttribute("errorMessage", "Lỗi tạo chuỗi hoạt động: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/activity-series/create.jsp")
                    .forward(request, response);
            return;
        }
    }

    private void updateSeries(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        HttpSession session = request.getSession();
        int id = parseId(request.getParameter("id"));
        if (id <= 0) {
            session.setAttribute("errorMessage", "ID chuỗi hoạt động không hợp lệ.");
            response.sendRedirect(request.getContextPath() + "/activity-series");
            return;
        }
        try {
            ActivitySeries series = buildSeriesFromRequest(request);
            series.setId(id);
            if (series.getResponsibleDepartmentId() > 0 && series.getResponsibleStaffId() > 0) {
                Staff staff = staffService.getStaffById(series.getResponsibleStaffId());
                Department dept = departmentService.getDepartmentById(series.getResponsibleDepartmentId());
                if (staff == null || dept == null) {
                    throw new IllegalArgumentException("Phòng ban hoặc nhân viên phụ trách không tồn tại.");
                }
                List<Staff> staffsInDept = staffService.getStaffsByDepartmentId(series.getResponsibleDepartmentId());
                boolean staffBelongsToDept = staffsInDept.stream().anyMatch(s -> s.getId() == series.getResponsibleStaffId());
                if (!staffBelongsToDept) {
                    throw new IllegalArgumentException("Nhân viên phụ trách không thuộc phòng ban đã chọn.");
                }
            }
            boolean success = seriesService.updateSeries(series, currentUser);
            if (success) {
                session.setAttribute("successMessage", "Cập nhật chuỗi hoạt động thành công!");
                response.sendRedirect(request.getContextPath() + "/activity-series");
            } else {
                session.setAttribute("errorMessage", "Cập nhật không thành công. Vui lòng thử lại.");
                response.sendRedirect(request.getContextPath() + "/activity-series?action=edit&id=" + id);
            }
        } catch (IllegalArgumentException e) {
            loadDropdownData(request, parseId(request.getParameter("responsibleDepartmentId")));
            request.setAttribute("errorMessage", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/activity-series/edit.jsp")
                    .forward(request, response);
            return;
        } catch (IllegalStateException e) {
            loadDropdownData(request, parseId(request.getParameter("responsibleDepartmentId")));
            request.setAttribute("errorMessage", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/activity-series/edit.jsp")
                    .forward(request, response);
            return;
        } catch (Exception e) {
            loadDropdownData(request, parseId(request.getParameter("responsibleDepartmentId")));
            request.setAttribute("errorMessage", "Lỗi cập nhật chuỗi hoạt động: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/activity-series/edit.jsp")
                    .forward(request, response);
            return;
        }
    }

    private void deleteSeries(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        HttpSession session = request.getSession();
        int id = parseId(request.getParameter("id"));
        if (id <= 0) {
            session.setAttribute("errorMessage", "ID chuỗi hoạt động không hợp lệ.");
            response.sendRedirect(request.getContextPath() + "/activity-series");
            return;
        }
        try {
            boolean success = seriesService.deleteSeries(id, currentUser);
            if (success) {
                session.setAttribute("successMessage", "Xóa chuỗi hoạt động thành công!");
            } else {
                session.setAttribute("errorMessage", "Không thể xóa chuỗi hoạt động.");
            }
        } catch (IllegalArgumentException e) {
            session.setAttribute("errorMessage", e.getMessage());
        } catch (IllegalStateException e) {
            session.setAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Lỗi xóa chuỗi hoạt động: " + e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/activity-series");
    }

    private void generateOccurrences(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        HttpSession session = request.getSession();
        int seriesId = parseId(request.getParameter("seriesId"));
        if (seriesId <= 0) {
            session.setAttribute("errorMessage", "ID chuá»—i hoáº¡t Ä‘á»™ng khÃ´ng há»£p lá»‡.");
            response.sendRedirect(request.getContextPath() + "/activity-series");
            return;
        }
        try {
            boolean success = seriesService.generateOccurrences(seriesId, currentUser);
            if (success) {
                session.setAttribute("successMessage", "ÄÃ£ táº¡o cÃ¡c sá»± kiá»‡n láº·p láº¡i thÃ nh cÃ´ng!");
            } else {
                session.setAttribute("errorMessage", "KhÃ´ng thá»ƒ táº¡o sá»± kiá»‡n láº·p láº¡i.");
            }
        } catch (IllegalArgumentException e) {
            session.setAttribute("errorMessage", e.getMessage());
        } catch (IllegalStateException e) {
            session.setAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Lá»—i táº¡o sá»± kiá»‡n láº·p láº¡i: " + e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/activity-series?action=detail&id=" + seriesId);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private ActivitySeries buildSeriesFromRequest(HttpServletRequest request) {
        ActivitySeries series = new ActivitySeries();
        series.setSeriesCode(request.getParameter("seriesCode"));
        series.setName(request.getParameter("name"));
        String activityTypeId = request.getParameter("activityTypeId");
        if (activityTypeId != null && !activityTypeId.trim().isEmpty()) {
            series.setActivityTypeId(Integer.parseInt(activityTypeId.trim()));
        }
        String deptId = request.getParameter("responsibleDepartmentId");
        if (deptId != null && !deptId.trim().isEmpty()) {
            series.setResponsibleDepartmentId(Integer.parseInt(deptId.trim()));
        }
        String staffId = request.getParameter("responsibleStaffId");
        if (staffId != null && !staffId.trim().isEmpty()) {
            series.setResponsibleStaffId(Integer.parseInt(staffId.trim()));
        }
        series.setDescription(request.getParameter("description"));
        series.setRegistrationFormRequired(
                "true".equalsIgnoreCase(request.getParameter("registrationFormRequired")));
        String mode = request.getParameter("recurrenceMode");
        if (mode != null && !mode.trim().isEmpty()) {
            series.setRecurrenceMode(Enum.RecurrenceMode.valueOf(mode.trim()));
        }
        String interval = request.getParameter("recurrenceInterval");
        if (interval != null && !interval.trim().isEmpty()) {
            series.setRecurrenceInterval(Integer.parseInt(interval.trim()));
        }
        String startDate = request.getParameter("seriesStartDate");
        if (startDate != null && !startDate.trim().isEmpty()) {
            series.setSeriesStartDate(LocalDate.parse(startDate.trim()));
        }
        String endDate = request.getParameter("seriesEndDate");
        if (endDate != null && !endDate.trim().isEmpty()) {
            series.setSeriesEndDate(LocalDate.parse(endDate.trim()));
        }
        String maxOcc = request.getParameter("maxOccurrences");
        if (maxOcc != null && !maxOcc.trim().isEmpty()) {
            series.setMaxOccurrences(Integer.parseInt(maxOcc.trim()));
        }
        String duration = request.getParameter("occurrenceDurationMinutes");
        if (duration != null && !duration.trim().isEmpty()) {
            series.setOccurrenceDurationMinutes(Integer.parseInt(duration.trim()));
        }
        String timeOfDay = request.getParameter("timeOfDay");
        if (timeOfDay != null && !timeOfDay.trim().isEmpty()) {
            series.setTimeOfDay(LocalTime.parse(timeOfDay.trim()));
        }
        String semesterId = request.getParameter("semesterId");
        if (semesterId != null && !semesterId.trim().isEmpty()) {
            series.setSemesterId(Integer.parseInt(semesterId.trim()));
        }
        return series;
    }

    private int parseId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void listOccurrences(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        if (currentUser == null || !AccessPolicy.canManageActivities(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ tài khoản STAFF hoặc ADMIN mới được xem sự kiện lặp lại.");
            return;
        }

        int seriesId = parseId(request.getParameter("seriesId"));
        if (seriesId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID chuỗi hoạt động không hợp lệ.");
            return;
        }

        try {
            List<ExtracurricularActivity> occurrences = seriesService.getOccurrencesBySeriesId(seriesId);
            List<Map<String, Object>> data = occurrences.stream().map(a -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", a.getId());
                item.put("code", a.getCode());
                item.put("name", a.getName());
                item.put("activityTypeId", a.getActivityTypeId());
                item.put("responsibleDepartmentId", a.getResponsibleDepartmentId());
                item.put("responsibleStaffId", a.getResponsibleStaffId());
                item.put("activityStatus", a.getActivityStatus() == null ? null : a.getActivityStatus().name());
                item.put("approvalStatus", a.getApprovalStatus() == null ? null : a.getApprovalStatus().name());
                item.put("startTime", a.getStartTime() == null ? null
                        : a.getStartTime().toString());
                item.put("endTime", a.getEndTime() == null ? null
                        : a.getEndTime().toString());
                item.put("description", a.getDescription());
                item.put("address", a.getAddress());
                item.put("createdAt", a.getCreatedAt() == null ? null
                        : a.getCreatedAt().toString());
                return item;
            }).collect(Collectors.toList());

            writeJsonResponse(response, Map.of("success", true, "data", data));
        } catch (Exception e) {
            writeJsonResponse(response, Map.of("success", false,
                    "message", "Không thể tải danh sách sự kiện lặp lại: " + e.getMessage()));
        }
    }

    private void writeJsonResponse(HttpServletResponse response, Object data) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.write(new Gson().toJson(data));
        }
    }
}
