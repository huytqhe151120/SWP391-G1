package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.PartnerStaffResponseDTO;
import com.swp391.g1.dto.request.PartnerStaffRequestDTO;
import com.swp391.g1.model.Account;
import com.swp391.g1.model.PartnerStaff;
import com.swp391.g1.service.IAccountService;
import com.swp391.g1.service.IPartnerStaffService;
import com.swp391.g1.service.impl.AccountServiceImpl;
import com.swp391.g1.service.impl.PartnerStaffServiceImpl;
import com.swp391.g1.util.AuthContext;
import com.swp391.g1.util.ParamUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@WebServlet("/partner-staff-profile")
public class PartnerStaffProfileServlet extends HttpServlet {

    private final IPartnerStaffService partnerStaffService = new PartnerStaffServiceImpl();
    private final IAccountService accountService = new AccountServiceImpl();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        request.setAttribute("profileAccount", currentUser);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (!"PARTNER_STAFF".equals(currentUser.getType())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ nhân viên đối tác mới có quyền truy cập trang này.");
            return;
        }
        super.service(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = ParamUtil.getString(request, "action", "");
        
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        
        // Find partner staff by account ID using DAO method
        PartnerStaff staff = partnerStaffService.getPartnerStaffByAccountId(currentUser.getId());
        
        if (staff == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy thông tin nhân viên đối tác.");
            return;
        }
        
        // Convert to DTO for display
        PartnerStaffResponseDTO staffDTO = partnerStaffService.getPartnerStaffById(staff.getId());
        
        request.setAttribute("staff", staffDTO);
        
        if ("edit".equals(action)) {
            request.getRequestDispatcher("/WEB-INF/views/partner-staff-profile/edit.jsp").forward(request, response);
        } else {
            request.getRequestDispatcher("/WEB-INF/views/partner-staff-profile/profile.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = ParamUtil.getString(request, "action", "");
        
        if (!"update".equals(action)) {
            response.sendRedirect(request.getContextPath() + "/partner-staff-profile");
            return;
        }
        
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        
        // Find partner staff by account ID using DAO method
        PartnerStaff staff = partnerStaffService.getPartnerStaffByAccountId(currentUser.getId());
        
        if (staff == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy thông tin nhân viên đối tác.");
            return;
        }
        
        // Get DTO for display
        PartnerStaffResponseDTO existingStaffDTO = partnerStaffService.getPartnerStaffById(staff.getId());
        
        String name = ParamUtil.getString(request, "name", "");
        String dob = ParamUtil.getString(request, "dob", "");
        String gender = ParamUtil.getString(request, "gender", "");
        String position = ParamUtil.getString(request, "position", "");
        String newPassword = ParamUtil.getString(request, "newPassword", "");
        String confirmPassword = ParamUtil.getString(request, "confirmPassword", "");
        
        // Build DTO for update
        PartnerStaffRequestDTO dto = new PartnerStaffRequestDTO();
        dto.setId(existingStaffDTO.getId());
        dto.setCompanyId(String.valueOf(existingStaffDTO.getCompanyId()));
        dto.setCode(existingStaffDTO.getCode());
        dto.setName(name);
        dto.setDob(dob);
        dto.setGender(gender);
        dto.setPosition(position);
        dto.setStatus(existingStaffDTO.getStatus() != null ? existingStaffDTO.getStatus() : "ACTIVE");
        
        boolean profileUpdated = false;
        try {
            profileUpdated = partnerStaffService.updatePartnerStaff(dto);
        } catch (Exception e) {
            request.setAttribute("message", "Cập nhật hồ sơ thất bại: " + e.getMessage());
            request.setAttribute("messageType", "error");
            request.setAttribute("staff", existingStaffDTO);
            request.getRequestDispatcher("/WEB-INF/views/partner-staff-profile/edit.jsp").forward(request, response);
            return;
        }
        
        boolean passwordUpdated = true;
        
        if (!newPassword.isEmpty() || !confirmPassword.isEmpty()) {
            if (!newPassword.equals(confirmPassword)) {
                request.setAttribute("message", "Mật khẩu xác nhận không khớp!");
                request.setAttribute("messageType", "error");
                request.setAttribute("staff", existingStaffDTO);
                request.getRequestDispatcher("/WEB-INF/views/partner-staff-profile/edit.jsp").forward(request, response);
                return;
            }
            if (newPassword.length() < 6) {
                request.setAttribute("message", "Mật khẩu phải có ít nhất 6 ký tự!");
                request.setAttribute("messageType", "error");
                request.setAttribute("staff", existingStaffDTO);
                request.getRequestDispatcher("/WEB-INF/views/partner-staff-profile/edit.jsp").forward(request, response);
                return;
            }
            Account account = accountService.getAccountById(currentUser.getId());
            if (account != null) {
                account.setPassword(newPassword);
                passwordUpdated = accountService.updateAccount(account).isSuccess();
            }
        }
        
        if (profileUpdated && passwordUpdated) {
            request.setAttribute("message", "Cập nhật hồ sơ thành công!");
            request.setAttribute("messageType", "success");
        } else {
            request.setAttribute("message", "Cập nhật hồ sơ thất bại. Vui lòng thử lại.");
            request.setAttribute("messageType", "error");
        }
        
        // Refresh staff data
        PartnerStaffResponseDTO updatedStaffDTO = partnerStaffService.getPartnerStaffById(staff.getId());
        request.setAttribute("staff", updatedStaffDTO != null ? updatedStaffDTO : existingStaffDTO);
        request.getRequestDispatcher("/WEB-INF/views/partner-staff-profile/edit.jsp").forward(request, response);
    }
}
