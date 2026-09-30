package com.swp391.g1.controller;

import com.swp391.g1.dto.request.ExtracurricularActivityRequestDTO;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.dto.response.PartnerStaffResponseDTO;
import com.swp391.g1.service.*;
import com.swp391.g1.service.impl.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet(name = "ExtracurricularActivityController", urlPatterns = {"/activities"})
public class ExtracurricularActivityController extends HttpServlet {

    private final IExtracurricularActivityService activityService;
    private final IActivityTypeService activityTypeService;
    private final IPartnerCompanyService partnerCompanyService;
    private final IPartnerStaffService partnerStaffService;
    // Bổ sung Service cho Semester, Department, Staff nếu hệ thống đã có
    private final ISemesterService semesterService;
    private final IDepartmentService departmentService;
    private final IStaffService staffService;

    public ExtracurricularActivityController() {
        this.activityService = new ExtracurricularActivityServiceImpl();
        this.activityTypeService = new ActivityTypeServiceImpl();
        this.partnerCompanyService = new PartnerCompanyServiceImpl();
        this.partnerStaffService = new PartnerStaffServiceImpl();
        this.semesterService = new SemesterServiceImpl();
        this.departmentService = new DepartmentServiceImpl();
        this.staffService = new StaffServiceImpl();
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
                    showDetail(request, response);
                    break;
                case "delete":
                    deleteActivity(request, response);
                    break;
                case "getStaffsByCompany":
                    getStaffsByCompanyJson(request, response);
                    break;
                case "list":
                default:
                    listActivities(request, response);
                    break;
            }
        } catch (Exception e) {
            request.setAttribute("errorMessage", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
            request.getRequestDispatcher("/views/common/error.jsp").forward(request, response);
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
                    createActivity(request, response);
                    break;
                case "update":
                    updateActivity(request, response);
                    break;
                default:
                    response.sendRedirect(request.getContextPath() + "/activities");
                    break;
            }
        } catch (Exception e) {
            request.setAttribute("errorMessage", "Lỗi xử lý dữ liệu: " + e.getMessage());
            request.getRequestDispatcher("/views/common/error.jsp").forward(request, response);
        }
    }

    // =========================================================================
    // GET ACTION HANDLERS
    // =========================================================================

    private void listActivities(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<ExtracurricularActivityResponseDTO> list = activityService.getAllActivities();
        request.setAttribute("activities", list);
        request.getRequestDispatcher("/views/activity/list.jsp").forward(request, response);
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        loadDropdownData(request);
        request.getRequestDispatcher("/views/activity/create.jsp").forward(request, response);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idStr = request.getParameter("id");
        if (idStr != null && !idStr.trim().isEmpty()) {
            int id = Integer.parseInt(idStr);
            ExtracurricularActivityResponseDTO activity = activityService.getActivityById(id);
            if (activity != null) {
                request.setAttribute("activity", activity);
                loadDropdownData(request);

                // Nạp trước danh sách nhân viên đối tác nếu đã chọn Công ty đối tác
                if (activity.getPartnerCompanyId() > 0) {
                    List<PartnerStaffResponseDTO> staffs = partnerStaffService.getStaffsByCompanyId(activity.getPartnerCompanyId());
                    request.setAttribute("partnerStaffs", staffs);
                }

                request.getRequestDispatcher("/views/activity/edit.jsp").forward(request, response);
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/activities");
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idStr = request.getParameter("id");
        if (idStr != null && !idStr.trim().isEmpty()) {
            int id = Integer.parseInt(idStr);
            ExtracurricularActivityResponseDTO activity = activityService.getActivityById(id);
            if (activity != null) {
                request.setAttribute("activity", activity);
                request.getRequestDispatcher("/views/activity/detail.jsp").forward(request, response);
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/activities");
    }

    private void deleteActivity(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String idStr = request.getParameter("id");
        HttpSession session = request.getSession();
        if (idStr != null && !idStr.trim().isEmpty()) {
            int id = Integer.parseInt(idStr);
            boolean success = activityService.deleteActivity(id);
            if (success) {
                session.setAttribute("successMessage", "Xóa hoạt động thành công!");
            } else {
                session.setAttribute("errorMessage", "Xóa hoạt động thất bại!");
            }
        }
        response.sendRedirect(request.getContextPath() + "/activities");
    }

    private void getStaffsByCompanyJson(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String companyIdStr = request.getParameter("companyId");
        StringBuilder json = new StringBuilder("[");

        if (companyIdStr != null && !companyIdStr.trim().isEmpty()) {
            try {
                int companyId = Integer.parseInt(companyIdStr);
                List<PartnerStaffResponseDTO> staffs = partnerStaffService.getStaffsByCompanyId(companyId);

                for (int i = 0; i < staffs.size(); i++) {
                    PartnerStaffResponseDTO staff = staffs.get(i);
                    json.append("{")
                            .append("\"id\":").append(staff.getId()).append(",")
                            .append("\"name\":\"").append(escapeJson(staff.getName())).append("\",")
                            .append("\"position\":\"").append(escapeJson(staff.getPosition() != null ? staff.getPosition() : "")).append("\"")
                            .append("}");
                    if (i < staffs.size() - 1) {
                        json.append(",");
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
        json.append("]");

        try (PrintWriter out = response.getWriter()) {
            out.print(json.toString());
            out.flush();
        }
    }

    // =========================================================================
    // POST ACTION HANDLERS
    // =========================================================================

    private void createActivity(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ExtracurricularActivityRequestDTO dto = buildRequestDTO(request);
        HttpSession session = request.getSession();

        try {
            boolean success = activityService.createActivity(dto);
            if (success) {
                session.setAttribute("successMessage", "Tạo mới hoạt động thành công!");
                response.sendRedirect(request.getContextPath() + "/activities");
            } else {
                request.setAttribute("errorMessage", "Không thể tạo hoạt động. Vui lòng thử lại.");
                request.setAttribute("formData", dto);
                showCreateForm(request, response);
            }
        } catch (Exception e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("formData", dto);
            showCreateForm(request, response);
        }
    }

    private void updateActivity(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ExtracurricularActivityRequestDTO dto = buildRequestDTO(request);
        dto.setId(request.getParameter("id"));
        HttpSession session = request.getSession();

        try {
            boolean success = activityService.updateActivity(dto);
            if (success) {
                session.setAttribute("successMessage", "Cập nhật hoạt động thành công!");
                response.sendRedirect(request.getContextPath() + "/activities");
            } else {
                request.setAttribute("errorMessage", "Cập nhật không thành công. Vui lòng kiểm tra lại.");
                request.setAttribute("activity", dto);
                showEditForm(request, response);
            }
        } catch (Exception e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("activity", dto);
            showEditForm(request, response);
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void loadDropdownData(HttpServletRequest request) {
        request.setAttribute("semesters", semesterService.getAllSemesters());
        request.setAttribute("activityTypes", activityTypeService.getActiveActivityTypes());
        request.setAttribute("departments", departmentService.getAllDepartments());
        request.setAttribute("staffs", staffService.getAllStaffs());
        request.setAttribute("partnerCompanies", partnerCompanyService.getActivePartnerCompanies());
    }

    private ExtracurricularActivityRequestDTO buildRequestDTO(HttpServletRequest request) {
        ExtracurricularActivityRequestDTO dto = new ExtracurricularActivityRequestDTO();
        dto.setCode(request.getParameter("code"));
        dto.setName(request.getParameter("name"));
        dto.setSemesterId(request.getParameter("semesterId"));
        dto.setActivityTypeId(request.getParameter("activityTypeId"));
        dto.setResponsibleDepartmentId(request.getParameter("responsibleDepartmentId"));
        dto.setResponsibleStaffId(request.getParameter("responsibleStaffId"));
        dto.setPartnerCompanyId(request.getParameter("partnerCompanyId"));
        dto.setPartnerStaffId(request.getParameter("partnerStaffId"));
        dto.setBonusPoint(request.getParameter("bonusPoint"));       // Bổ sung
        dto.setPenaltyPoint(request.getParameter("penaltyPoint"));   // Bổ sung
        dto.setAddress(request.getParameter("address"));             // Đổi location -> address
        dto.setStartTime(request.getParameter("startTime"));         // Đổi startDateTime -> startTime
        dto.setEndTime(request.getParameter("endTime"));             // Đổi endDateTime -> endTime
        dto.setDescription(request.getParameter("description"));
        return dto;
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}