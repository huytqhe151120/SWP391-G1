package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.StudentActivityGradeDTO;
import com.swp391.g1.dto.response.StudentGradeSummaryDTO;
import com.swp391.g1.model.Enum.CommonStatus;
import com.swp391.g1.model.Semester;
import com.swp391.g1.model.Student;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.ISemesterService;
import com.swp391.g1.service.IStudentGradeService;
import com.swp391.g1.service.IStudentService;
import com.swp391.g1.service.impl.SemesterServiceImpl;
import com.swp391.g1.service.impl.StudentGradeServiceImpl;
import com.swp391.g1.service.impl.StudentServiceImpl;
import com.swp391.g1.util.AuthContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@WebServlet(name = "StudentGradeServlet", urlPatterns = {"/student-grades"})
public class StudentGradeServlet extends HttpServlet {

    private static final List<Integer> PAGE_SIZES = List.of(30, 40, 50);
    private static final int DEFAULT_PAGE_SIZE = 30;

    private final IStudentGradeService gradeService;
    private final IStudentService studentService;
    private final ISemesterService semesterService;

    public StudentGradeServlet() {
        this.gradeService = new StudentGradeServiceImpl();
        this.studentService = new StudentServiceImpl();
        this.semesterService = new SemesterServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            CurrentUser currentUser = AuthContext.getCurrentUser(request);
            if (currentUser == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }

            boolean isStudent = AccessPolicy.hasType(currentUser, "STUDENT");
            boolean canViewAll = AccessPolicy.canTakeAttendance(currentUser)
                    || AccessPolicy.canManageActivities(currentUser);

            if (!isStudent && !canViewAll) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bạn không có quyền truy cập chức năng điểm hoạt động sinh viên.");
                return;
            }

            Integer myStudentId = null;
            if (isStudent) {
                Student me = studentService.getStudentByAccountId(currentUser.getId());
                if (me == null) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Không tìm thấy hồ sơ sinh viên tương ứng với tài khoản đang đăng nhập.");
                    return;
                }
                myStudentId = me.getId();
            }

            request.setAttribute("isStudent", isStudent);
            request.setAttribute("canViewAllGrades", canViewAll);

            String action = request.getParameter("action");
            if (action == null || action.isBlank()) {
                action = isStudent ? "studentDetail" : "list";
            }

            switch (action) {
                case "studentDetail" -> handleStudentDetail(request, response, isStudent, myStudentId);
                case "activityDetail" -> handleActivityDetail(request, response, isStudent, myStudentId);
                default -> {
                    if (isStudent) {
                        response.sendRedirect(request.getContextPath()
                                + "/student-grades?action=studentDetail&studentId=" + myStudentId);
                        return;
                    }
                    handleList(request, response);
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("errorMessage", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Semester> semesters = semesterService.getAllSemesters();
        int activeSemesterId = findActiveSemesterId(semesters);
        int selectedSemesterId = resolveSelectedSemesterId(request.getParameter("semesterId"), activeSemesterId);
        Semester selectedSemester = findSemesterById(semesters, selectedSemesterId);

        List<StudentGradeSummaryDTO> allSummaries = gradeService.getAllStudentGradeSummaries(selectedSemesterId);

        Set<String> departments = new TreeSet<>();
        Set<String> majors = new TreeSet<>();
        Set<String> programs = new TreeSet<>();
        Set<String> classes = new TreeSet<>();
        for (StudentGradeSummaryDTO item : allSummaries) {
            addIfPresent(departments, item.getDepartmentName());
            addIfPresent(majors, item.getMajorName());
            addIfPresent(programs, item.getAcademicProgramName());
            addIfPresent(classes, item.getMainClassCode());
        }

        GradeListFilter filter = GradeListFilter.from(request, selectedSemesterId, activeSemesterId);
        List<StudentGradeSummaryDTO> filteredList = new ArrayList<>();
        for (StudentGradeSummaryDTO item : allSummaries) {
            if (filter.matches(item)) {
                filteredList.add(item);
            }
        }

        int pageSize = parsePageSize(request.getParameter("pageSize"));
        int filteredCount = filteredList.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredCount / pageSize));
        int currentPage = Math.min(Math.max(parseId(request.getParameter("page")), 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * pageSize, filteredCount);
        int toIndex = Math.min(fromIndex + pageSize, filteredCount);
        List<StudentGradeSummaryDTO> pageItems = new ArrayList<>(filteredList.subList(fromIndex, toIndex));

        request.setAttribute("semesters", semesters);
        request.setAttribute("activeSemesterId", activeSemesterId);
        request.setAttribute("selectedSemesterId", selectedSemesterId);
        request.setAttribute("selectedSemester", selectedSemester);

        request.setAttribute("summaries", pageItems);
        request.setAttribute("totalStudentsCount", allSummaries.size());
        request.setAttribute("filteredStudentsCount", filteredCount);
        request.setAttribute("departments", departments);
        request.setAttribute("majors", majors);
        request.setAttribute("academicPrograms", programs);
        request.setAttribute("mainClasses", classes);

        request.setAttribute("keyword", filter.keyword);
        request.setAttribute("selectedDepartment", filter.department);
        request.setAttribute("selectedMajor", filter.major);
        request.setAttribute("selectedAcademicProgram", filter.academicProgram);
        request.setAttribute("selectedMainClass", filter.mainClass);
        request.setAttribute("filtered", filter.activeCount() > 0);
        request.setAttribute("activeFilterCount", filter.activeCount());
        request.setAttribute("filterQuery", filter.toQuery());

        request.setAttribute("pageSize", pageSize);
        request.setAttribute("pageSizes", PAGE_SIZES);
        request.setAttribute("currentPage", currentPage);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("firstItemNumber", filteredCount == 0 ? 0 : fromIndex + 1);
        request.setAttribute("lastItemNumber", toIndex);

        request.getRequestDispatcher("/WEB-INF/views/student-grade/list.jsp").forward(request, response);
    }

    private void handleStudentDetail(HttpServletRequest request, HttpServletResponse response,
            boolean isStudent, Integer myStudentId) throws ServletException, IOException {
        int requestedStudentId = parseId(request.getParameter("studentId"));
        if (isStudent) {
            if (requestedStudentId > 0 && requestedStudentId != myStudentId) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Sinh viên chỉ được phép xem điểm hoạt động của chính mình.");
                return;
            }
            requestedStudentId = myStudentId;
        }

        List<Semester> semesters = semesterService.getAllSemesters();
        int activeSemesterId = findActiveSemesterId(semesters);
        int selectedSemesterId = resolveSelectedSemesterId(request.getParameter("semesterId"), activeSemesterId);
        Semester selectedSemester = findSemesterById(semesters, selectedSemesterId);

        List<StudentGradeSummaryDTO> allStudents = isStudent
                ? List.of()
                : gradeService.getAllStudentGradeSummaries(selectedSemesterId);
        if (!isStudent && requestedStudentId <= 0 && !allStudents.isEmpty()) {
            requestedStudentId = allStudents.get(0).getStudentId();
        }

        StudentGradeSummaryDTO selectedStudent = requestedStudentId > 0
                ? gradeService.getStudentGradeSummaryById(requestedStudentId, selectedSemesterId)
                : null;
        List<StudentActivityGradeDTO> activityGrades = selectedStudent != null
                ? gradeService.getActivityGradesByStudentId(selectedStudent.getStudentId(), selectedSemesterId)
                : List.of();

        request.setAttribute("semesters", semesters);
        request.setAttribute("activeSemesterId", activeSemesterId);
        request.setAttribute("selectedSemesterId", selectedSemesterId);
        request.setAttribute("selectedSemester", selectedSemester);
        request.setAttribute("allStudents", allStudents);
        request.setAttribute("selectedStudent", selectedStudent);
        request.setAttribute("selectedStudentId", selectedStudent == null ? 0 : selectedStudent.getStudentId());
        request.setAttribute("activityGrades", activityGrades);
        request.getRequestDispatcher("/WEB-INF/views/student-grade/student-detail.jsp").forward(request, response);
    }

    private void handleActivityDetail(HttpServletRequest request, HttpServletResponse response,
            boolean isStudent, Integer myStudentId) throws ServletException, IOException {
        int requestedStudentId = parseId(request.getParameter("studentId"));
        int requestedActivityId = parseId(request.getParameter("activityId"));

        if (isStudent) {
            if (requestedStudentId > 0 && requestedStudentId != myStudentId) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Sinh viên chỉ được phép xem chi tiết điểm hoạt động của chính mình.");
                return;
            }
            requestedStudentId = myStudentId;
        }

        List<StudentGradeSummaryDTO> allStudents = isStudent
                ? List.of()
                : gradeService.getAllStudentGradeSummaries();
        if (!isStudent && requestedStudentId <= 0 && !allStudents.isEmpty()) {
            requestedStudentId = allStudents.get(0).getStudentId();
        }

        StudentGradeSummaryDTO selectedStudent = requestedStudentId > 0
                ? gradeService.getStudentGradeSummaryById(requestedStudentId)
                : null;
        List<StudentActivityGradeDTO> studentActivities = selectedStudent != null
                ? gradeService.getActivityGradesByStudentId(selectedStudent.getStudentId())
                : List.of();

        if (requestedActivityId <= 0 && !studentActivities.isEmpty()) {
            requestedActivityId = studentActivities.get(0).getActivityId();
        }

        StudentActivityGradeDTO gradeDetail = (selectedStudent != null && requestedActivityId > 0)
                ? gradeService.getActivityGradeDetail(selectedStudent.getStudentId(), requestedActivityId)
                : null;

        if (gradeDetail == null && !studentActivities.isEmpty()) {
            gradeDetail = studentActivities.get(0);
            requestedActivityId = gradeDetail.getActivityId();
        }

        request.setAttribute("allStudents", allStudents);
        request.setAttribute("selectedStudent", selectedStudent);
        request.setAttribute("selectedStudentId", selectedStudent == null ? 0 : selectedStudent.getStudentId());
        request.setAttribute("studentActivities", studentActivities);
        request.setAttribute("selectedActivityId", requestedActivityId);
        request.setAttribute("gradeDetail", gradeDetail);
        request.getRequestDispatcher("/WEB-INF/views/student-grade/activity-detail.jsp").forward(request, response);
    }

    private int findActiveSemesterId(List<Semester> semesters) {
        if (semesters == null || semesters.isEmpty()) {
            return 0;
        }
        for (Semester s : semesters) {
            if (s.getStatus() == CommonStatus.ACTIVE) {
                return s.getId();
            }
        }
        return semesters.get(0).getId();
    }

    private int resolveSelectedSemesterId(String rawParam, int activeSemesterId) {
        if (rawParam == null || rawParam.trim().isEmpty()) {
            return activeSemesterId;
        }
        String trimmed = rawParam.trim();
        if ("0".equals(trimmed) || "ALL".equalsIgnoreCase(trimmed)) {
            return 0;
        }
        int parsed = parseId(trimmed);
        return parsed > 0 ? parsed : activeSemesterId;
    }

    private Semester findSemesterById(List<Semester> semesters, int semesterId) {
        if (semesters == null || semesterId <= 0) {
            return null;
        }
        for (Semester s : semesters) {
            if (s.getId() == semesterId) {
                return s;
            }
        }
        return null;
    }

    private void addIfPresent(Set<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }

    private int parsePageSize(String value) {
        int pageSize = parseId(value);
        return PAGE_SIZES.contains(pageSize) ? pageSize : DEFAULT_PAGE_SIZE;
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

    private static final class GradeListFilter {
        private final int semesterId;
        private final int defaultSemesterId;
        private final String keyword;
        private final String department;
        private final String major;
        private final String academicProgram;
        private final String mainClass;
        private final String normalizedKeyword;

        private GradeListFilter(HttpServletRequest request, int semesterId, int defaultSemesterId) {
            this.semesterId = semesterId;
            this.defaultSemesterId = defaultSemesterId;
            this.keyword = trimToEmpty(request.getParameter("keyword"));
            this.department = trimToEmpty(request.getParameter("department"));
            this.major = trimToEmpty(request.getParameter("major"));
            this.academicProgram = trimToEmpty(request.getParameter("academicProgram"));
            this.mainClass = trimToEmpty(request.getParameter("mainClass"));
            this.normalizedKeyword = normalizeText(keyword);
        }

        static GradeListFilter from(HttpServletRequest request, int semesterId, int defaultSemesterId) {
            return new GradeListFilter(request, semesterId, defaultSemesterId);
        }

        boolean matches(StudentGradeSummaryDTO item) {
            if (!normalizedKeyword.isEmpty()
                    && !normalizeText(item.getStudentCode()).contains(normalizedKeyword)
                    && !normalizeText(item.getStudentName()).contains(normalizedKeyword)) {
                return false;
            }
            return matchesValue(department, item.getDepartmentName())
                    && matchesValue(major, item.getMajorName())
                    && matchesValue(academicProgram, item.getAcademicProgramName())
                    && matchesValue(mainClass, item.getMainClassCode());
        }

        int activeCount() {
            return (semesterId != defaultSemesterId ? 1 : 0)
                    + (normalizedKeyword.isEmpty() ? 0 : 1)
                    + (department.isEmpty() ? 0 : 1)
                    + (major.isEmpty() ? 0 : 1)
                    + (academicProgram.isEmpty() ? 0 : 1)
                    + (mainClass.isEmpty() ? 0 : 1);
        }

        String toQuery() {
            StringBuilder query = new StringBuilder();
            appendParam(query, "semesterId", String.valueOf(semesterId));
            appendParam(query, "keyword", keyword);
            appendParam(query, "department", department);
            appendParam(query, "major", major);
            appendParam(query, "academicProgram", academicProgram);
            appendParam(query, "mainClass", mainClass);
            return query.toString();
        }

        private static boolean matchesValue(String selected, String value) {
            return selected.isEmpty() || (value != null && selected.equals(value.trim()));
        }

        private static void appendParam(StringBuilder query, String name, String value) {
            if (!value.isEmpty()) {
                query.append('&').append(name).append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }

        private static String trimToEmpty(String value) {
            return value == null ? "" : value.trim();
        }

        private static String normalizeText(String value) {
            if (value == null) {
                return "";
            }
            return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .replace('đ', 'd').replace('Đ', 'D')
                    .toLowerCase(Locale.ROOT);
        }
    }
}
