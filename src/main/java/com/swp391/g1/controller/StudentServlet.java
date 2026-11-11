package com.swp391.g1.controller;

import com.swp391.g1.model.Student;
import com.swp391.g1.service.IStudentService;
import com.swp391.g1.service.impl.StudentServiceImpl;
import com.swp391.g1.util.ParamUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

@WebServlet("/student")
public class StudentServlet extends HttpServlet {

    private final IStudentService studentService = new StudentServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = ParamUtil.getString(request, "action", "");
        if ("detail".equals(action) || "edit".equals(action)) {
            showStudent(request, response, action);
            return;
        }

        String keyword = ParamUtil.getString(request, "keyword", "");
        String id = ParamUtil.getString(request, "id", "");
        String code = ParamUtil.getString(request, "code", "");
        String name = ParamUtil.getString(request, "name", "");
        String classVal = ParamUtil.getString(request, "class", "");
        String department = ParamUtil.getString(request, "department", "");
        String major = ParamUtil.getString(request, "major", "");
        String dob = ParamUtil.getString(request, "dob", "");
        String email = ParamUtil.getString(request, "email", "");
        String phoneNumber = ParamUtil.getString(request, "phoneNumber", "");
        String gender = ParamUtil.getString(request, "gender", "");
        String status = ParamUtil.getString(request, "status", "");
        int page = ParamUtil.getInt(request, "page", 1);
        int pageSize = ParamUtil.getInt(request, "pageSize", 10);
        if (pageSize != 5 && pageSize != 10 && pageSize != 20 && pageSize != 25) {
            pageSize = 10;
        }

        try {
            List<Student> allStudents = studentService.searchStudents(
                    keyword, id, code, name, classVal, department, major,
                    dob, email, phoneNumber, gender, status);
            int totalStudentCount = allStudents.size();
            int totalPages = Math.max(1, (int) Math.ceil((double) totalStudentCount / pageSize));
            int currentPage = Math.max(1, Math.min(page, totalPages));
            int pageWindowStart = ((currentPage - 1) / 3) * 3 + 1;
            int pageWindowEnd = Math.min(pageWindowStart + 2, totalPages);

            int fromIndex = Math.min((currentPage - 1) * pageSize, totalStudentCount);
            int toIndex = Math.min(fromIndex + pageSize, totalStudentCount);

            List<Student> pageStudents = new ArrayList<>(allStudents.subList(fromIndex, toIndex));

            request.setAttribute("students", pageStudents);
            request.setAttribute("pageSize", pageSize);
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("pageWindowStart", pageWindowStart);
            request.setAttribute("pageWindowEnd", pageWindowEnd);
            request.setAttribute("totalStudentCount", totalStudentCount);
            request.setAttribute("firstStudentNumber", totalStudentCount == 0 ? 0 : fromIndex + 1);
            request.setAttribute("lastStudentNumber", toIndex);

            boolean filtered = !keyword.isEmpty() || !id.isEmpty() || !code.isEmpty()
                    || !name.isEmpty() || !classVal.isEmpty() || !department.isEmpty()
                    || !major.isEmpty() || !dob.isEmpty()
                    || !email.isEmpty() || !phoneNumber.isEmpty() || !gender.isEmpty() || !status.isEmpty();
            request.setAttribute("filtered", filtered);
            request.setAttribute("activeFilterCount",
                    (keyword.isEmpty() ? 0 : 1) + (id.isEmpty() ? 0 : 1)
                    + (code.isEmpty() ? 0 : 1) + (name.isEmpty() ? 0 : 1)
                    + (classVal.isEmpty() ? 0 : 1) + (department.isEmpty() ? 0 : 1)
                    + (major.isEmpty() ? 0 : 1) + (dob.isEmpty() ? 0 : 1)
                    + (email.isEmpty() ? 0 : 1) + (phoneNumber.isEmpty() ? 0 : 1)
                    + (gender.isEmpty() ? 0 : 1) + (status.isEmpty() ? 0 : 1));
            request.setAttribute("filterQuery", buildFilterQuery(
                    keyword, id, code, name, classVal, department, major,
                    dob, email, phoneNumber, gender, status));
            request.setAttribute("keyword", keyword);
            request.setAttribute("filterId", id);
            request.setAttribute("filterCode", code);
            request.setAttribute("filterName", name);
            request.setAttribute("filterClass", classVal);
            request.setAttribute("filterDepartment", department);
            request.setAttribute("filterMajor", major);
            request.setAttribute("filterDob", dob);
            request.setAttribute("filterEmail", email);
            request.setAttribute("filterPhoneNumber", phoneNumber);
            request.setAttribute("filterGender", gender);
            request.setAttribute("filterStatus", status);

            List<Student> studentsForOptions = studentService.getAllStudents();
            TreeSet<String> classes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            TreeSet<String> departments = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            Map<String, TreeSet<String>> majorsByDepartment = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (Student candidate : studentsForOptions) {
                if (candidate.getMainClassCode() != null && !candidate.getMainClassCode().isBlank()) {
                    classes.add(candidate.getMainClassCode());
                }
                String departmentName = candidate.getDepartmentName();
                String majorName = candidate.getMajorName();
                if (departmentName != null && !departmentName.isBlank()) {
                    departments.add(departmentName);
                    if (majorName != null && !majorName.isBlank()) {
                        majorsByDepartment.computeIfAbsent(
                                departmentName,
                                key -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)
                        ).add(majorName);
                    }
                }
            }
            request.setAttribute("classes", classes);
            request.setAttribute("departments", departments);
            request.setAttribute("majorsByDepartment", majorsByDepartment);

            request.getRequestDispatcher("/WEB-INF/views/student/list-student.jsp")
                    .forward(request, response);
        } catch (IllegalStateException exception) {
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không thể truy cập dữ liệu sinh viên."
            );
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!"update".equals(ParamUtil.getString(request, "action", ""))) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        int id = ParamUtil.getInt(request, "id", 0);
        Student original = studentService.getStudentById(id);
        if (original == null) {
            response.sendRedirect(request.getContextPath() + "/student?error=notFound");
            return;
        }

        Student student = new Student();
        student.setId(id);
        student.setMainClassId(original.getMainClassId());
        student.setMainClassCode(original.getMainClassCode());
        student.setDepartmentName(original.getDepartmentName());
        student.setMajorName(original.getMajorName());
        student.setAccountId(original.getAccountId());
        student.setCode(request.getParameter("code"));
        student.setName(request.getParameter("name"));
        student.setDob(request.getParameter("dob"));
        student.setGender(request.getParameter("gender"));
        student.setEmail(request.getParameter("email"));
        student.setPhoneNumber(request.getParameter("phoneNumber"));
        student.setStatus(request.getParameter("status"));
        try {
            if (!studentService.updateStudent(student)) {
                response.sendRedirect(request.getContextPath() + "/student?error=notFound");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/student?action=detail&id=" + id + "&msg=updated");
        } catch (IllegalArgumentException e) {
            request.setAttribute("student", student);
            request.setAttribute("formError", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/student/form.jsp").forward(request, response);
        } catch (IllegalStateException e) {
            String validationMessage = constraintViolationMessage(e);
            if (validationMessage == null) {
                throw new ServletException("Không thể cập nhật thông tin sinh viên.", e);
            }
            request.setAttribute("student", student);
            request.setAttribute("formError", validationMessage);
            request.getRequestDispatcher("/WEB-INF/views/student/form.jsp").forward(request, response);
        }
    }

    private String constraintViolationMessage(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sqlException) {
                if (sqlException.getErrorCode() == 2601 || sqlException.getErrorCode() == 2627) {
                    return "Mã sinh viên đã tồn tại.";
                }
                if (sqlException.getErrorCode() == 2628 || sqlException.getErrorCode() == 8152) {
                    return "Một hoặc nhiều thông tin vượt quá độ dài cho phép.";
                }
                if (sqlException.getErrorCode() == 547) {
                    return "Thông tin liên kết không hợp lệ.";
                }
                if (sqlException.getSQLState() != null && sqlException.getSQLState().startsWith("23")) {
                    return "Thông tin sinh viên không hợp lệ hoặc đã tồn tại.";
                }
            }
        }
        return null;
    }

    private void showStudent(HttpServletRequest request, HttpServletResponse response, String action)
            throws ServletException, IOException {
        int id = ParamUtil.getInt(request, "id", 0);
        Student student = studentService.getStudentById(id);
        if (student == null) {
            response.sendRedirect(request.getContextPath() + "/student?error=notFound");
            return;
        }
        request.setAttribute("student", student);
        if ("edit".equals(action)) {
            request.getRequestDispatcher("/WEB-INF/views/student/form.jsp").forward(request, response);
        } else {
            request.getRequestDispatcher("/WEB-INF/views/student/detail.jsp").forward(request, response);
        }
    }

    private String buildFilterQuery(String keyword, String id, String code, String name, String classVal,
                                    String department, String major, String dob, String email,
                                    String phoneNumber, String gender, String status) {
        StringBuilder query = new StringBuilder();
        appendParam(query, "keyword", keyword);
        appendParam(query, "id", id);
        appendParam(query, "code", code);
        appendParam(query, "name", name);
        appendParam(query, "class", classVal);
        appendParam(query, "department", department);
        appendParam(query, "major", major);
        appendParam(query, "dob", dob);
        appendParam(query, "email", email);
        appendParam(query, "phoneNumber", phoneNumber);
        appendParam(query, "gender", gender);
        appendParam(query, "status", status);
        return query.toString();
    }

    private void appendParam(StringBuilder query, String name, String value) {
        if (value != null && !value.isEmpty()) {
            query.append('&').append(name).append('=')
                    .append(java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}