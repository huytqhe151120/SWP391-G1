package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.Account;
import com.swp391.g1.model.Staff;
import com.swp391.g1.service.IAccountService;
import com.swp391.g1.service.IStaffService;
import com.swp391.g1.service.impl.AccountServiceImpl;
import com.swp391.g1.service.impl.StaffServiceImpl;
import com.swp391.g1.util.AuthContext;
import com.swp391.g1.util.ParamUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/staff-profile")
public class StaffProfileServlet extends HttpServlet {

    private final IStaffService staffService = new StaffServiceImpl();
    private final IAccountService accountService = new AccountServiceImpl();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        request.setAttribute("profileAccount", currentUser);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (!"STAFF".equals(currentUser.getType())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ nhân viên mới có quyền truy cập trang này.");
            return;
        }
        super.service(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = ParamUtil.getString(request, "action", "");
        
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        Staff staff = staffService.getStaffByAccountId(currentUser.getId());
        
        if (staff == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy thông tin nhân viên.");
            return;
        }

        request.setAttribute(
                "staffDepartmentNames",
                staffService.getDepartmentNamesByStaffId(staff.getId())
        );
        
        request.setAttribute("staff", staff);
        
        if ("edit".equals(action)) {
            request.getRequestDispatcher("/WEB-INF/views/staff-profile/edit.jsp").forward(request, response);
        } else {
            request.getRequestDispatcher("/WEB-INF/views/staff-profile/profile.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = ParamUtil.getString(request, "action", "");
        
        if (!"update".equals(action)) {
            response.sendRedirect(request.getContextPath() + "/staff-profile");
            return;
        }
        
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        Staff staff = staffService.getStaffByAccountId(currentUser.getId());
        
        if (staff == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy thông tin nhân viên.");
            return;
        }
        
        String name = ParamUtil.getString(request, "name", "");
        String dob = ParamUtil.getString(request, "dob", "");
        String gender = ParamUtil.getString(request, "gender", "");
        String newPassword = ParamUtil.getString(request, "newPassword", "");
        String confirmPassword = ParamUtil.getString(request, "confirmPassword", "");
        
        staff.setName(name);
        staff.setDob(dob);
        staff.setGender(gender);
        
        boolean profileUpdated = false;
        try {
            profileUpdated = staffService.updateStaff(staff);
        } catch (Exception e) {
            request.setAttribute("message", "Cập nhật hồ sơ thất bại: " + e.getMessage());
            request.setAttribute("messageType", "error");
            request.setAttribute("staff", staff);
            request.getRequestDispatcher("/WEB-INF/views/staff-profile/edit.jsp").forward(request, response);
            return;
        }
        
        boolean passwordUpdated = true;
        
        if (!newPassword.isEmpty() || !confirmPassword.isEmpty()) {
            if (!newPassword.equals(confirmPassword)) {
                request.setAttribute("message", "Mật khẩu xác nhận không khớp!");
                request.setAttribute("messageType", "error");
                request.setAttribute("staff", staff);
                request.getRequestDispatcher("/WEB-INF/views/staff-profile/edit.jsp").forward(request, response);
                return;
            }
            if (newPassword.length() < 6) {
                request.setAttribute("message", "Mật khẩu phải có ít nhất 6 ký tự!");
                request.setAttribute("messageType", "error");
                request.setAttribute("staff", staff);
                request.getRequestDispatcher("/WEB-INF/views/staff-profile/edit.jsp").forward(request, response);
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
        
        request.setAttribute("staff", staff);
        request.getRequestDispatcher("/WEB-INF/views/staff-profile/edit.jsp").forward(request, response);
    }
}
