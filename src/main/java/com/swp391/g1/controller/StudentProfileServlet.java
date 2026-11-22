package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.Account;
import com.swp391.g1.model.Student;
import com.swp391.g1.service.IAccountService;
import com.swp391.g1.service.IStudentService;
import com.swp391.g1.service.impl.AccountServiceImpl;
import com.swp391.g1.service.impl.StudentServiceImpl;
import com.swp391.g1.util.AuthContext;
import com.swp391.g1.util.ParamUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/student-profile")
public class StudentProfileServlet extends HttpServlet {

    private final IStudentService studentService = new StudentServiceImpl();
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
        if (!"STUDENT".equals(currentUser.getType())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ sinh viên mới có quyền truy cập trang này.");
            return;
        }
        super.service(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = ParamUtil.getString(request, "action", "");
        
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        Student student = studentService.getStudentByAccountId(currentUser.getId());
        
        if (student == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy thông tin sinh viên.");
            return;
        }
        
        request.setAttribute("student", student);
        
        if ("edit".equals(action)) {
            request.getRequestDispatcher("/WEB-INF/views/student-profile/edit.jsp").forward(request, response);
        } else {
            request.getRequestDispatcher("/WEB-INF/views/student-profile/profile.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = ParamUtil.getString(request, "action", "");
        
        if (!"update".equals(action)) {
            response.sendRedirect(request.getContextPath() + "/student-profile");
            return;
        }
        
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        Student student = studentService.getStudentByAccountId(currentUser.getId());
        
        if (student == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy thông tin sinh viên.");
            return;
        }
        
        String name = ParamUtil.getString(request, "name", "");
        String dob = ParamUtil.getString(request, "dob", "");
        String gender = ParamUtil.getString(request, "gender", "");
        String email = ParamUtil.getString(request, "email", "");
        String phoneNumber = ParamUtil.getString(request, "phoneNumber", "");
        String newPassword = ParamUtil.getString(request, "newPassword", "");
        String confirmPassword = ParamUtil.getString(request, "confirmPassword", "");
        
        student.setName(name);
        student.setDob(dob);
        student.setGender(gender);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);
        
        boolean profileUpdated = studentService.updateStudent(student);
        boolean passwordUpdated = true;
        
        if (!newPassword.isEmpty() || !confirmPassword.isEmpty()) {
            if (!newPassword.equals(confirmPassword)) {
                request.setAttribute("message", "Mật khẩu xác nhận không khớp!");
                request.setAttribute("messageType", "error");
                request.setAttribute("student", student);
                request.getRequestDispatcher("/WEB-INF/views/student-profile/edit.jsp").forward(request, response);
                return;
            }
            if (newPassword.length() < 6) {
                request.setAttribute("message", "Mật khẩu phải có ít nhất 6 ký tự!");
                request.setAttribute("messageType", "error");
                request.setAttribute("student", student);
                request.getRequestDispatcher("/WEB-INF/views/student-profile/edit.jsp").forward(request, response);
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
        
        request.setAttribute("student", student);
        request.getRequestDispatcher("/WEB-INF/views/student-profile/edit.jsp").forward(request, response);
    }
}
