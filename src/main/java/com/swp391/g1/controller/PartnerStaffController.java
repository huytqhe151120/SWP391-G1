package com.swp391.g1.controller;

import com.swp391.g1.dto.request.PartnerStaffRequestDTO;
import com.swp391.g1.dto.response.PartnerStaffResponseDTO;
import com.swp391.g1.service.IPartnerCompanyService;
import com.swp391.g1.service.IPartnerStaffService;
import com.swp391.g1.service.impl.PartnerCompanyServiceImpl;
import com.swp391.g1.service.impl.PartnerStaffServiceImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "PartnerStaffController", urlPatterns = {"/partner-staffs"})
public class PartnerStaffController extends HttpServlet {

    private final IPartnerStaffService partnerStaffService;
    private final IPartnerCompanyService partnerCompanyService;

    public PartnerStaffController() {
        this.partnerStaffService = new PartnerStaffServiceImpl();
        this.partnerCompanyService = new PartnerCompanyServiceImpl();
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
                    deleteStaff(request, response);
                    break;
                case "list":
                default:
                    listStaffs(request, response);
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
                    createStaff(request, response);
                    break;
                case "update":
                    updateStaff(request, response);
                    break;
                default:
                    response.sendRedirect(request.getContextPath() + "/partner-staffs");
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

    private void listStaffs(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String companyIdStr = request.getParameter("companyId");
        List<PartnerStaffResponseDTO> list;

        // Cho phép lọc danh sách nhân viên theo công ty nếu có truyền parameter companyId
        if (companyIdStr != null && !companyIdStr.trim().isEmpty()) {
            int companyId = Integer.parseInt(companyIdStr);
            list = partnerStaffService.getStaffsByCompanyId(companyId);
            request.setAttribute("selectedCompanyId", companyId);
        } else {
            list = partnerStaffService.getAllPartnerStaffs();
        }

        request.setAttribute("partnerStaffs", list);
        request.setAttribute("partnerCompanies", partnerCompanyService.getAllPartnerCompanies());
        request.getRequestDispatcher("/views/partner-staff/list.jsp").forward(request, response);
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        loadDropdownData(request);
        request.getRequestDispatcher("/views/partner-staff/create.jsp").forward(request, response);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idStr = request.getParameter("id");
        if (idStr != null && !idStr.trim().isEmpty()) {
            int id = Integer.parseInt(idStr);
            PartnerStaffResponseDTO staff = partnerStaffService.getPartnerStaffById(id);
            if (staff != null) {
                request.setAttribute("partnerStaff", staff);
                loadDropdownData(request);
                request.getRequestDispatcher("/views/partner-staff/edit.jsp").forward(request, response);
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/partner-staffs");
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idStr = request.getParameter("id");
        if (idStr != null && !idStr.trim().isEmpty()) {
            int id = Integer.parseInt(idStr);
            PartnerStaffResponseDTO staff = partnerStaffService.getPartnerStaffById(id);
            if (staff != null) {
                request.setAttribute("partnerStaff", staff);
                request.getRequestDispatcher("/views/partner-staff/detail.jsp").forward(request, response);
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/partner-staffs");
    }

    private void deleteStaff(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String idStr = request.getParameter("id");
        HttpSession session = request.getSession();

        if (idStr != null && !idStr.trim().isEmpty()) {
            int id = Integer.parseInt(idStr);
            boolean success = partnerStaffService.deletePartnerStaff(id);
            if (success) {
                session.setAttribute("successMessage", "Xóa nhân viên đối tác thành công!");
            } else {
                session.setAttribute("errorMessage", "Không thể xóa nhân viên đối tác. Vui lòng kiểm tra lại!");
            }
        }
        response.sendRedirect(request.getContextPath() + "/partner-staffs");
    }

    // =========================================================================
    // POST ACTION HANDLERS
    // =========================================================================

    private void createStaff(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PartnerStaffRequestDTO dto = buildRequestDTO(request);
        HttpSession session = request.getSession();

        try {
            boolean success = partnerStaffService.createPartnerStaff(dto);
            if (success) {
                session.setAttribute("successMessage", "Tạo mới nhân viên đối tác thành công!");
                response.sendRedirect(request.getContextPath() + "/partner-staffs");
            } else {
                request.setAttribute("errorMessage", "Không thể tạo nhân viên đối tác. Vui lòng thử lại.");
                request.setAttribute("formData", dto);
                showCreateForm(request, response);
            }
        } catch (Exception e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("formData", dto);
            showCreateForm(request, response);
        }
    }

    private void updateStaff(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PartnerStaffRequestDTO dto = buildRequestDTO(request);
        dto.setId(Integer.parseInt(request.getParameter("id")));

        HttpSession session = request.getSession();

        try {
            boolean success = partnerStaffService.updatePartnerStaff(dto);
            if (success) {
                session.setAttribute("successMessage", "Cập nhật nhân viên đối tác thành công!");
                response.sendRedirect(request.getContextPath() + "/partner-staffs");
            } else {
                request.setAttribute("errorMessage", "Cập nhật thất bại. Vui lòng kiểm tra lại.");
                request.setAttribute("formData", dto);
                showEditForm(request, response);
            }
        } catch (Exception e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("formData", dto);
            showEditForm(request, response);
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void loadDropdownData(HttpServletRequest request) {
        // Nạp danh sách các công ty ACTIVE để chọn khi tạo/sửa nhân viên
        request.setAttribute("partnerCompanies", partnerCompanyService.getActivePartnerCompanies());
    }

    private PartnerStaffRequestDTO buildRequestDTO(HttpServletRequest request) {
        PartnerStaffRequestDTO dto = new PartnerStaffRequestDTO();
        dto.setCompanyId(request.getParameter("companyId"));
        dto.setCode(request.getParameter("code"));
        dto.setName(request.getParameter("name"));
        dto.setGender(request.getParameter("gender"));
        dto.setDob(request.getParameter("dob"));
        dto.setPosition(request.getParameter("position"));
        dto.setStatus(request.getParameter("status"));
        return dto;
    }
}