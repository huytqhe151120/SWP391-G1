package com.swp391.g1.controller;

import com.swp391.g1.model.Enum.CommonStatus;
import com.swp391.g1.model.PartnerCompany;
import com.swp391.g1.service.IPartnerCompanyService;
import com.swp391.g1.service.impl.PartnerCompanyServiceImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "PartnerCompanyController", urlPatterns = {"/partner-companies"})
public class PartnerCompanyController extends HttpServlet {

    private final IPartnerCompanyService partnerCompanyService;

    public PartnerCompanyController() {
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
                    deleteCompany(request, response);
                    break;
                case "list":
                default:
                    listCompanies(request, response);
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
                    createCompany(request, response);
                    break;
                case "update":
                    updateCompany(request, response);
                    break;
                default:
                    response.sendRedirect(request.getContextPath() + "/partner-companies");
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

    private void listCompanies(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<PartnerCompany> list = partnerCompanyService.getAllPartnerCompanies();
        request.setAttribute("partnerCompanies", list);
        request.getRequestDispatcher("/views/partner-company/list.jsp").forward(request, response);
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/views/partner-company/create.jsp").forward(request, response);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idStr = request.getParameter("id");
        if (idStr != null && !idStr.trim().isEmpty()) {
            int id = Integer.parseInt(idStr);
            PartnerCompany company = partnerCompanyService.getPartnerCompanyById(id);
            if (company != null) {
                request.setAttribute("partnerCompany", company);
                request.getRequestDispatcher("/views/partner-company/edit.jsp").forward(request, response);
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/partner-companies");
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idStr = request.getParameter("id");
        if (idStr != null && !idStr.trim().isEmpty()) {
            int id = Integer.parseInt(idStr);
            PartnerCompany company = partnerCompanyService.getPartnerCompanyById(id);
            if (company != null) {
                request.setAttribute("partnerCompany", company);
                request.getRequestDispatcher("/views/partner-company/detail.jsp").forward(request, response);
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/partner-companies");
    }

    private void deleteCompany(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String idStr = request.getParameter("id");
        HttpSession session = request.getSession();

        if (idStr != null && !idStr.trim().isEmpty()) {
            int id = Integer.parseInt(idStr);
            boolean success = partnerCompanyService.deletePartnerCompany(id);
            if (success) {
                session.setAttribute("successMessage", "Xóa công ty đối tác thành công!");
            } else {
                session.setAttribute("errorMessage", "Không thể xóa công ty đối tác. Vui lòng kiểm tra lại!");
            }
        }
        response.sendRedirect(request.getContextPath() + "/partner-companies");
    }

    // =========================================================================
    // POST ACTION HANDLERS
    // =========================================================================

    private void createCompany(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PartnerCompany company = buildEntityFromRequest(request);
        HttpSession session = request.getSession();

        try {
            boolean success = partnerCompanyService.createPartnerCompany(company);
            if (success) {
                session.setAttribute("successMessage", "Tạo mới công ty đối tác thành công!");
                response.sendRedirect(request.getContextPath() + "/partner-companies");
            } else {
                request.setAttribute("errorMessage", "Không thể tạo công ty đối tác. Vui lòng thử lại.");
                request.setAttribute("partnerCompany", company);
                showCreateForm(request, response);
            }
        } catch (Exception e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("partnerCompany", company);
            showCreateForm(request, response);
        }
    }

    private void updateCompany(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PartnerCompany company = buildEntityFromRequest(request);

        String idStr = request.getParameter("id");
        if (idStr != null && !idStr.trim().isEmpty()) {
            company.setId(Integer.parseInt(idStr));
        }

        String statusStr = request.getParameter("status");
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                company.setStatus(CommonStatus.valueOf(statusStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                company.setStatus(CommonStatus.ACTIVE);
            }
        }

        HttpSession session = request.getSession();

        try {
            boolean success = partnerCompanyService.updatePartnerCompany(company);
            if (success) {
                session.setAttribute("successMessage", "Cập nhật công ty đối tác thành công!");
                response.sendRedirect(request.getContextPath() + "/partner-companies");
            } else {
                request.setAttribute("errorMessage", "Cập nhật thất bại. Vui lòng kiểm tra lại.");
                request.setAttribute("partnerCompany", company);
                showEditForm(request, response);
            }
        } catch (Exception e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("partnerCompany", company);
            showEditForm(request, response);
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private PartnerCompany buildEntityFromRequest(HttpServletRequest request) {
        PartnerCompany company = new PartnerCompany();
        company.setCode(request.getParameter("code"));
        company.setName(request.getParameter("name"));
        company.setEmail(request.getParameter("email"));
        company.setPhoneNumber(request.getParameter("phone"));
        company.setAddress(request.getParameter("address"));
        company.setDescription(request.getParameter("description"));
        return company;
    }
}
