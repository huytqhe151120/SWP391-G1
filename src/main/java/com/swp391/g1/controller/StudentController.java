package com.swp391.g1.controller;

import com.swp391.g1.service.IStudentService;
import com.swp391.g1.service.impl.StudentServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/student")
public class StudentController extends HttpServlet {

    private final IStudentService studentService = new StudentServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            request.setAttribute("students", studentService.getAllStudents());
            request.getRequestDispatcher("/WEB-INF/views/student/list-student.jsp")
                    .forward(request, response);
        } catch (IllegalStateException exception) {
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to access student data."
            );
        }
    }
}