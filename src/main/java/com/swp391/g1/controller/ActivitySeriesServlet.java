package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.ActivitySeries;
import com.swp391.g1.model.Enum;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IActivitySeriesService;
import com.swp391.g1.service.impl.ActivitySeriesServiceImpl;
import com.swp391.g1.util.AuthContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@WebServlet(name = "ActivitySeriesServlet",
        urlPatterns = {"/activity-series"})
public class ActivitySeriesServlet extends HttpServlet {

    private final IActivitySeriesService seriesService = new ActivitySeriesServiceImpl();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        action = action == null ? "" : action.trim();

        boolean isGetRequest = "GET".equalsIgnoreCase(request.getMethod())
                || "HEAD".equalsIgnoreCase(request.getMethod());
        boolean requiresManagementPermission =
                (isGetRequest && ("create".equals(action) || "edit".equals(action)
                        || "list".equals(action) || "detail".equals(action)))
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
            request.setAttribute("errorMessage", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
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
            request.setAttribute("errorMessage", "Lỗi xử lý dữ liệu: " + e.getMessage());
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
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID chuỗi hoạt động không hợp lệ.");
            return;
        }
        ActivitySeries series = seriesService.getSeriesById(id);
        if (series == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy chuỗi hoạt động.");
            return;
        }
        request.setAttribute("series", series);
        request.getRequestDispatcher("/WEB-INF/views/activity-series/detail.jsp")
                .forward(request, response);
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/activity-series/create.jsp")
                .forward(request, response);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int id = parseId(request.getParameter("id"));
        if (id <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID chuỗi hoạt động không hợp lệ.");
            return;
        }
        ActivitySeries series = seriesService.getSeriesById(id);
        if (series == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy chuỗi hoạt động.");
            return;
        }
        request.setAttribute("series", series);
        request.getRequestDispatcher("/WEB-INF/views/activity-series/edit.jsp")
                .forward(request, response);
    }

    // =========================================================================
    // POST ACTION HANDLERS
    // =========================================================================

    private void createSeries(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        HttpSession session = request.getSession();
        try {
            ActivitySeries series = buildSeriesFromRequest(request);
            boolean success = seriesService.createSeries(series, currentUser);
            if (success) {
                session.setAttribute("successMessage", "Tạo chuỗi hoạt động thành công!");
                response.sendRedirect(request.getContextPath() + "/activity-series");
            } else {
                session.setAttribute("errorMessage", "Không thể tạo chuỗi hoạt động. Vui lòng thử lại.");
                response.sendRedirect(request.getContextPath() + "/activity-series?action=create");
            }
        } catch (IllegalArgumentException e) {
            session.setAttribute("errorMessage", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/activity-series?action=create");
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Lỗi tạo chuỗi hoạt động: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/activity-series?action=create");
        }
    }

    private void updateSeries(HttpServletRequest request, HttpServletResponse response)
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
            ActivitySeries series = buildSeriesFromRequest(request);
            series.setId(id);
            boolean success = seriesService.updateSeries(series, currentUser);
            if (success) {
                session.setAttribute("successMessage", "Cập nhật chuỗi hoạt động thành công!");
                response.sendRedirect(request.getContextPath() + "/activity-series");
            } else {
                session.setAttribute("errorMessage", "Cập nhật không thành công. Vui lòng thử lại.");
                response.sendRedirect(request.getContextPath() + "/activity-series?action=edit&id=" + id);
            }
        } catch (IllegalArgumentException e) {
            session.setAttribute("errorMessage", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/activity-series?action=edit&id=" + id);
        } catch (IllegalStateException e) {
            session.setAttribute("errorMessage", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/activity-series?action=edit&id=" + id);
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Lỗi cập nhật chuỗi hoạt động: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/activity-series?action=edit&id=" + id);
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
            session.setAttribute("errorMessage", "ID chuỗi hoạt động không hợp lệ.");
            response.sendRedirect(request.getContextPath() + "/activity-series");
            return;
        }
        try {
            boolean success = seriesService.generateOccurrences(seriesId, currentUser);
            if (success) {
                session.setAttribute("successMessage", "Đã tạo các sự kiện lặp lại thành công!");
            } else {
                session.setAttribute("errorMessage", "Không thể tạo sự kiện lặp lại.");
            }
        } catch (IllegalArgumentException e) {
            session.setAttribute("errorMessage", e.getMessage());
        } catch (IllegalStateException e) {
            session.setAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Lỗi tạo sự kiện lặp lại: " + e.getMessage());
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
}