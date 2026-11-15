package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.Student;
import com.swp391.g1.service.IStudentService;
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

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
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
        
        student.setName(name);
        student.setDob(dob);
        student.setGender(gender);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);
        
        boolean updated = studentService.updateStudent(student);
        
        if (updated) {
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
