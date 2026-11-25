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
import com.swp391.g1.util.ParamUtil;

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

/**
 * Servlet điều phối các màn hình tra cứu điểm hoạt động ngoại khóa của sinh viên:
 * danh sách tổng hợp theo học kỳ ({@code list}), chi tiết điểm theo sinh viên ({@code studentDetail})
 * và chi tiết điểm của sinh viên trong từng hoạt động ({@code activityDetail}).
 */
@WebServlet(name = "StudentGradeServlet", urlPatterns = {"/student-grades"})
public class StudentGradeServlet extends HttpServlet {

    private static final List<Integer> PAGE_SIZES = List.of(30, 40, 50);
    private static final int DEFAULT_PAGE_SIZE = 30;

    private final IStudentGradeService gradeService;
    private final IStudentService studentService;
    private final ISemesterService semesterService;

    /**
     * Khởi tạo servlet cùng các service tra cứu điểm, hồ sơ sinh viên và học kỳ.
     */
    public StudentGradeServlet() {
        this.gradeService = new StudentGradeServiceImpl();
        this.studentService = new StudentServiceImpl();
        this.semesterService = new SemesterServiceImpl();
    }

    /**
     * Xử lý các yêu cầu GET tới {@code /student-grades}: xác thực đăng nhập, kiểm tra phân quyền
     * giữa sinh viên (chỉ xem điểm của bản thân) và cán bộ quản lý (xem toàn bộ), sau đó định tuyến theo {@code action}.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            CurrentUser currentUser = AuthContext.getCurrentUser(request);
            // Chưa đăng nhập -> chuyển hướng về trang đăng nhập
            if (currentUser == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }

            // Phân biệt tài khoản sinh viên với tài khoản cán bộ có quyền xem bảng điểm toàn trường
            boolean isStudent = AccessPolicy.hasType(currentUser, "STUDENT");
            boolean canViewAll = AccessPolicy.canTakeAttendance(currentUser)
                    || AccessPolicy.canManageActivities(currentUser);

            // Chặn truy cập (403) nếu tài khoản không phải sinh viên và cũng không có quyền quản lý/điểm danh
            if (!isStudent && !canViewAll) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bạn không có quyền truy cập chức năng điểm hoạt động sinh viên.");
                return;
            }

            Integer myStudentId = null;
            // Nếu là sinh viên thì tra cứu hồ sơ sinh viên gắn với tài khoản để ràng buộc phạm vi dữ liệu
            if (isStudent) {
                Student me = studentService.getStudentByAccountId(currentUser.getId());
                // Báo lỗi 403 nếu tài khoản sinh viên chưa được liên kết với hồ sơ sinh viên trong DB
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
            // Nếu không truyền action thì mặc định sinh viên vào trang chi tiết của mình, cán bộ vào trang danh sách
            if (action == null || action.isBlank()) {
                action = isStudent ? "studentDetail" : "list";
            }

            // Điều phối request theo hành động yêu cầu
            switch (action) {
                // Xem tổng hợp điểm và danh sách hoạt động của một sinh viên theo học kỳ
                case "studentDetail" -> handleStudentDetail(request, response, isStudent, myStudentId);
                // Xem bảng phân tích điểm chi tiết của một sinh viên trong một hoạt động cụ thể
                case "activityDetail" -> handleActivityDetail(request, response, isStudent, myStudentId);
                // Mặc định (bao gồm "list"): hiển thị danh sách tổng hợp toàn bộ sinh viên
                default -> {
                    // Sinh viên không được xem danh sách toàn trường -> điều hướng về trang điểm của chính mình
                    if (isStudent) {
                        response.sendRedirect(request.getContextPath()
                                + "/student-grades?action=studentDetail&studentId=" + myStudentId);
                        return;
                    }
                    handleList(request, response);
                }
            }
        // Bắt lỗi hệ thống ngoài dự kiến và chuyển tiếp sang trang lỗi 500
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("errorMessage", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    /**
     * Xử lý màn hình danh sách điểm tổng hợp sinh viên (dành cho cán bộ):
     * xác định học kỳ, nạp tổng hợp điểm, lọc theo tiêu chí, phân trang và forward sang {@code list.jsp}.
     */
    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Xác định học kỳ đang chọn (mặc định là học kỳ ACTIVE; 0 tương ứng xem tất cả học kỳ)
        SemesterSelection semesterSelection = resolveSemesterSelection(request);
        List<StudentGradeSummaryDTO> allSummaries =
                gradeService.getAllStudentGradeSummaries(semesterSelection.selectedSemesterId());

        // Trích xuất danh sách tùy chọn dropdown lọc từ tập dữ liệu gốc và khởi tạo bộ lọc từ request
        FilterOptions filterOptions = FilterOptions.from(allSummaries);
        GradeListFilter filter = GradeListFilter.from(
                request, semesterSelection.selectedSemesterId(), semesterSelection.activeSemesterId());
        // Lọc danh sách sinh viên thỏa mãn từ khóa và các tiêu chí khoa/ngành/chương trình/lớp bằng Stream
        List<StudentGradeSummaryDTO> filteredList = allSummaries.stream()
                .filter(filter::matches)
                .toList();

        // Cắt lát phân trang trên danh sách đã lọc
        int pageSize = resolvePageSize(request);
        int requestedPage = ParamUtil.getInt(request, "page", 0);
        PageSlice<StudentGradeSummaryDTO> page = PageSlice.of(filteredList, requestedPage, pageSize);

        semesterSelection.applyTo(request);
        request.setAttribute("summaries", page.items());
        request.setAttribute("totalStudentsCount", allSummaries.size());
        request.setAttribute("filteredStudentsCount", page.totalFiltered());
        filterOptions.applyTo(request);
        filter.applyTo(request);
        page.applyTo(request);

        request.getRequestDispatcher("/WEB-INF/views/student-grade/list.jsp").forward(request, response);
    }

    /**
     * Xử lý màn hình chi tiết điểm hoạt động của một sinh viên theo học kỳ.
     * Nếu là cán bộ sẽ nạp thêm danh sách sinh viên để chuyển nhanh trên giao diện.
     */
    private void handleStudentDetail(HttpServletRequest request, HttpServletResponse response,
            boolean isStudent, Integer myStudentId) throws ServletException, IOException {
        Integer authorizedStudentId = resolveAuthorizedStudentId(request, response, isStudent, myStudentId,
                "Sinh viên chỉ được phép xem điểm hoạt động của chính mình.");
        // Đã trả về lỗi 403 trong resolveAuthorizedStudentId nếu sinh viên cố xem người khác
        if (authorizedStudentId == null) {
            return;
        }

        SemesterSelection semesterSelection = resolveSemesterSelection(request);
        int selectedSemesterId = semesterSelection.selectedSemesterId();

        // Sinh viên không cần tải danh sách sinh viên toàn trường; cán bộ tải để chọn sinh viên khác
        List<StudentGradeSummaryDTO> allStudents = isStudent
                ? List.of()
                : gradeService.getAllStudentGradeSummaries(selectedSemesterId);
        int targetStudentId = resolveTargetStudentId(authorizedStudentId, isStudent, allStudents);

        // Chỉ truy vấn tổng hợp điểm khi đã xác định được ID sinh viên hợp lệ
        StudentGradeSummaryDTO selectedStudent = targetStudentId > 0
                ? gradeService.getStudentGradeSummaryById(targetStudentId, selectedSemesterId)
                : null;
        // Tải danh sách điểm từng hoạt động của sinh viên trong học kỳ đang chọn (nếu tìm thấy sinh viên)
        List<StudentActivityGradeDTO> activityGrades = selectedStudent != null
                ? gradeService.getActivityGradesByStudentId(selectedStudent.getStudentId(), selectedSemesterId)
                : List.of();

        semesterSelection.applyTo(request);
        setStudentSelectionAttributes(request, allStudents, selectedStudent);
        request.setAttribute("activityGrades", activityGrades);
        request.getRequestDispatcher("/WEB-INF/views/student-grade/student-detail.jsp").forward(request, response);
    }

    /**
     * Xử lý màn hình phân tích điểm chi tiết của một sinh viên trong một hoạt động cụ thể
     * (điểm chuyên cần, điểm đánh giá, điểm bài thu hoạch, các hệ số và điểm tích lũy).
     */
    private void handleActivityDetail(HttpServletRequest request, HttpServletResponse response,
            boolean isStudent, Integer myStudentId) throws ServletException, IOException {
        Integer authorizedStudentId = resolveAuthorizedStudentId(request, response, isStudent, myStudentId,
                "Sinh viên chỉ được phép xem chi tiết điểm hoạt động của chính mình.");
        // Dừng xử lý nếu kiểm tra quyền thất bại (đã gửi mã lỗi 403)
        if (authorizedStudentId == null) {
            return;
        }
        int requestedActivityId = ParamUtil.getInt(request, "activityId", 0);

        // Sinh viên không cần danh sách sinh viên toàn trường; cán bộ nạp toàn bộ để hỗ trợ chuyển đổi
        List<StudentGradeSummaryDTO> allStudents = isStudent
                ? List.of()
                : gradeService.getAllStudentGradeSummaries();
        int targetStudentId = resolveTargetStudentId(authorizedStudentId, isStudent, allStudents);

        // Truy vấn hồ sơ tổng hợp điểm của sinh viên mục tiêu (xuyên suốt tất cả học kỳ)
        StudentGradeSummaryDTO selectedStudent = targetStudentId > 0
                ? gradeService.getStudentGradeSummaryById(targetStudentId)
                : null;
        // Truy vấn toàn bộ hoạt động mà sinh viên đã tham gia để hiển thị danh sách chọn hoạt động
        List<StudentActivityGradeDTO> studentActivities = selectedStudent != null
                ? gradeService.getActivityGradesByStudentId(selectedStudent.getStudentId())
                : List.of();

        // Nếu không truyền activityId hợp lệ thì mặc định chọn hoạt động gần nhất (đầu danh sách)
        if (requestedActivityId <= 0 && !studentActivities.isEmpty()) {
            requestedActivityId = studentActivities.getFirst().getActivityId();
        }

        // Truy vấn bảng điểm chi tiết cho cặp (sinh viên, hoạt động) nếu cả hai đều hợp lệ
        StudentActivityGradeDTO gradeDetail = (selectedStudent != null && requestedActivityId > 0)
                ? gradeService.getActivityGradeDetail(selectedStudent.getStudentId(), requestedActivityId)
                : null;

        // Fallback: nếu activityId yêu cầu không thuộc lịch sử của sinh viên thì tự động hiển thị hoạt động đầu tiên
        if (gradeDetail == null && !studentActivities.isEmpty()) {
            gradeDetail = studentActivities.getFirst();
            requestedActivityId = gradeDetail.getActivityId();
        }

        setStudentSelectionAttributes(request, allStudents, selectedStudent);
        request.setAttribute("studentActivities", studentActivities);
        request.setAttribute("selectedActivityId", requestedActivityId);
        request.setAttribute("gradeDetail", gradeDetail);
        request.getRequestDispatcher("/WEB-INF/views/student-grade/activity-detail.jsp").forward(request, response);
    }

    /**
     * Xác định {@code studentId} được phép truy cập dựa trên vai trò người dùng.
     * Nếu là sinh viên và cố truy cập ID của sinh viên khác sẽ trả về HTTP 403 và trả {@code null}.
     */
    private Integer resolveAuthorizedStudentId(HttpServletRequest request, HttpServletResponse response,
            boolean isStudent, Integer myStudentId, String forbiddenMessage) throws IOException {
        int requestedStudentId = ParamUtil.getInt(request, "studentId", 0);
        // Cán bộ quản lý được phép xem bất kỳ studentId nào yêu cầu
        if (!isStudent) {
            return requestedStudentId;
        }
        // Sinh viên cố tình truyền studentId khác với ID của bản thân -> từ chối 403
        if (requestedStudentId > 0 && requestedStudentId != myStudentId) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, forbiddenMessage);
            return null;
        }
        return myStudentId;
    }

    /**
     * Chọn ID sinh viên mục tiêu để hiển thị; với cán bộ nếu chưa chọn {@code studentId} cụ thể
     * thì tự động chọn sinh viên đầu tiên trong danh sách.
     */
    private int resolveTargetStudentId(int requestedStudentId, boolean isStudent,
            List<StudentGradeSummaryDTO> allStudents) {
        // Trường hợp cán bộ mở thẳng trang chi tiết mà không truyền studentId -> lấy sinh viên đầu danh sách
        if (!isStudent && requestedStudentId <= 0 && !allStudents.isEmpty()) {
            return allStudents.getFirst().getStudentId();
        }
        return requestedStudentId;
    }

    /**
     * Đẩy các thuộc tính danh sách sinh viên và sinh viên đang được chọn vào request cho tầng view.
     */
    private void setStudentSelectionAttributes(HttpServletRequest request,
            List<StudentGradeSummaryDTO> allStudents, StudentGradeSummaryDTO selectedStudent) {
        request.setAttribute("allStudents", allStudents);
        request.setAttribute("selectedStudent", selectedStudent);
        // Trả về 0 nếu không có sinh viên nào được chọn để view không bị null
        request.setAttribute("selectedStudentId", selectedStudent == null ? 0 : selectedStudent.getStudentId());
    }

    /**
     * Tải danh sách học kỳ và xác định học kỳ đang hoạt động (ACTIVE) cũng như học kỳ người dùng đang chọn.
     */
    private SemesterSelection resolveSemesterSelection(HttpServletRequest request) {
        List<Semester> semesters = semesterService.getAllSemesters();
        int activeSemesterId = findActiveSemesterId(semesters);
        int selectedSemesterId = resolveSelectedSemesterId(request, activeSemesterId);
        Semester selectedSemester = findSemesterById(semesters, selectedSemesterId);
        return new SemesterSelection(semesters, activeSemesterId, selectedSemesterId, selectedSemester);
    }

    /**
     * Tìm ID học kỳ đang có trạng thái {@link CommonStatus#ACTIVE}; nếu không có học kỳ ACTIVE nào
     * sẽ fallback về học kỳ đầu tiên trong danh sách (hoặc {@code 0} nếu danh sách rỗng).
     */
    private int findActiveSemesterId(List<Semester> semesters) {
        // Không có dữ liệu học kỳ -> trả về 0 (tất cả học kỳ)
        if (semesters == null || semesters.isEmpty()) {
            return 0;
        }
        // Duyệt danh sách học kỳ để ưu tiên lấy học kỳ đang mở (ACTIVE)
        for (Semester s : semesters) {
            // Trả về ngay học kỳ đầu tiên có trạng thái ACTIVE
            if (s.getStatus() == CommonStatus.ACTIVE) {
                return s.getId();
            }
        }
        return semesters.getFirst().getId();
    }

    /**
     * Đọc tham số {@code semesterId} từ request: rỗng thì dùng học kỳ ACTIVE mặc định,
     * {@code "0"} hoặc {@code "ALL"} trả về {@code 0} (tất cả học kỳ), số dương thì dùng giá trị đó.
     */
    private int resolveSelectedSemesterId(HttpServletRequest request, int activeSemesterId) {
        String rawParam = ParamUtil.getString(request, "semesterId", "");
        // Không truyền tham số học kỳ -> mặc định lọc theo học kỳ đang hoạt động
        if (rawParam.isEmpty()) {
            return activeSemesterId;
        }
        // Người dùng chủ động chọn xem toàn bộ các học kỳ
        if ("0".equals(rawParam) || "ALL".equalsIgnoreCase(rawParam)) {
            return 0;
        }
        int parsed = ParamUtil.getInt(request, "semesterId", 0);
        // Chỉ chấp nhận semesterId dương; nếu sai định dạng thì fallback về học kỳ ACTIVE
        return parsed > 0 ? parsed : activeSemesterId;
    }

    /**
     * Tìm đối tượng {@link Semester} theo ID trong danh sách đã tải; trả về {@code null} nếu không khớp.
     */
    private Semester findSemesterById(List<Semester> semesters, int semesterId) {
        // Bỏ qua tìm kiếm nếu danh sách học kỳ null hoặc semesterId <= 0 (chế độ tất cả học kỳ)
        if (semesters == null || semesterId <= 0) {
            return null;
        }
        // Duyệt danh sách học kỳ để tìm bản ghi có ID trùng khớp
        for (Semester s : semesters) {
            // Trả về học kỳ khi khớp ID
            if (s.getId() == semesterId) {
                return s;
            }
        }
        return null;
    }

    /**
     * Thêm chuỗi đã cắt khoảng trắng vào tập hợp nếu không null và không rỗng.
     */
    private static void addIfPresent(Set<String> values, String value) {
        // Chỉ thêm giá trị có nội dung thực để tránh mục trống trong dropdown bộ lọc
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }

    /**
     * Đọc kích thước trang từ request; chỉ chấp nhận các giá trị trong {@link #PAGE_SIZES},
     * ngược lại dùng {@link #DEFAULT_PAGE_SIZE}.
     */
    private int resolvePageSize(HttpServletRequest request) {
        int pageSize = ParamUtil.getInt(request, "pageSize", DEFAULT_PAGE_SIZE);
        // Nếu pageSize hợp lệ theo danh sách hỗ trợ thì dùng, ngược lại quay về mặc định
        return PAGE_SIZES.contains(pageSize) ? pageSize : DEFAULT_PAGE_SIZE;
    }

    /**
     * Bản ghi gói gọn trạng thái chọn học kỳ (toàn bộ học kỳ, học kỳ ACTIVE và học kỳ đang chọn).
     */
    private record SemesterSelection(
            List<Semester> semesters,
            int activeSemesterId,
            int selectedSemesterId,
            Semester selectedSemester) {

        /**
         * Gán các thuộc tính thông tin học kỳ vào request để view JSP hiển thị bộ chọn học kỳ.
         */
        void applyTo(HttpServletRequest request) {
            request.setAttribute("semesters", semesters);
            request.setAttribute("activeSemesterId", activeSemesterId);
            request.setAttribute("selectedSemesterId", selectedSemesterId);
            request.setAttribute("selectedSemester", selectedSemester);
        }
    }

    /**
     * Bản ghi chứa các tập giá trị duy nhất (đã sắp xếp) cho dropdown bộ lọc khoa, ngành, chương trình và lớp.
     */
    private record FilterOptions(
            Set<String> departments,
            Set<String> majors,
            Set<String> academicPrograms,
            Set<String> mainClasses) {

        /**
         * Thu thập toàn bộ giá trị khoa, ngành, chương trình đào tạo và lớp hành chính từ danh sách tổng hợp.
         */
        static FilterOptions from(List<StudentGradeSummaryDTO> summaries) {
            FilterOptions options = new FilterOptions(
                    new TreeSet<>(), new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
            // Duyệt từng sinh viên để gom các giá trị đơn vị học thuật duy nhất vào các TreeSet
            for (StudentGradeSummaryDTO item : summaries) {
                addIfPresent(options.departments, item.getDepartmentName());
                addIfPresent(options.majors, item.getMajorName());
                addIfPresent(options.academicPrograms, item.getAcademicProgramName());
                addIfPresent(options.mainClasses, item.getMainClassCode());
            }
            return options;
        }

        /**
         * Đẩy các tập tùy chọn bộ lọc vào request attribute cho view JSP.
         */
        void applyTo(HttpServletRequest request) {
            request.setAttribute("departments", departments);
            request.setAttribute("majors", majors);
            request.setAttribute("academicPrograms", academicPrograms);
            request.setAttribute("mainClasses", mainClasses);
        }
    }

    /**
     * Bản ghi đại diện cho một trang dữ liệu sau khi cắt lát phân trang cùng các chỉ số hiển thị.
     */
    private record PageSlice<T>(
            List<T> items,
            int pageSize,
            int currentPage,
            int totalPages,
            int totalFiltered,
            int firstItemNumber,
            int lastItemNumber) {

        /**
         * Cắt danh sách nguồn theo trang yêu cầu và kích thước trang, tự động kẹp số trang hợp lệ trong {@code [1..pages]}.
         */
        static <T> PageSlice<T> of(List<T> source, int requestedPage, int pageSize) {
            // Tính tổng số trang (tối thiểu 1), kẹp trang hiện tại trong biên hợp lệ và xác định chỉ số [from..to)
            int total = source.size();
            int pages = Math.max(1, Math.ceilDiv(total, pageSize));
            int page = Math.clamp(requestedPage, 1, pages);
            int from = Math.min((page - 1) * pageSize, total);
            int to = Math.min(from + pageSize, total);
            List<T> slice = new ArrayList<>(source.subList(from, to));
            // Số thứ tự bản ghi đầu tiên là 0 khi tập kết quả rỗng, ngược lại là from + 1 (đánh số từ 1)
            return new PageSlice<>(slice, pageSize, page, pages, total, total == 0 ? 0 : from + 1, to);
        }

        /**
         * Đẩy toàn bộ thông số phân trang vào request attribute cho component phân trang trên JSP.
         */
        void applyTo(HttpServletRequest request) {
            request.setAttribute("pageSize", pageSize);
            request.setAttribute("pageSizes", PAGE_SIZES);
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("firstItemNumber", firstItemNumber);
            request.setAttribute("lastItemNumber", lastItemNumber);
        }
    }

    /**
     * Lớp đóng gói các tiêu chí lọc trên màn hình danh sách điểm sinh viên (học kỳ, từ khóa không dấu,
     * khoa, ngành, chương trình đào tạo và lớp hành chính).
     */
    private static final class GradeListFilter {
        private final int semesterId;
        private final int defaultSemesterId;
        private final String keyword;
        private final String department;
        private final String major;
        private final String academicProgram;
        private final String mainClass;
        private final String normalizedKeyword;

        /**
         * Khởi tạo bộ lọc bằng cách đọc các tham số lọc từ {@link HttpServletRequest} và chuẩn hóa từ khóa.
         */
        private GradeListFilter(HttpServletRequest request, int semesterId, int defaultSemesterId) {
            this.semesterId = semesterId;
            this.defaultSemesterId = defaultSemesterId;
            this.keyword = ParamUtil.getString(request, "keyword", "");
            this.department = ParamUtil.getString(request, "department", "");
            this.major = ParamUtil.getString(request, "major", "");
            this.academicProgram = ParamUtil.getString(request, "academicProgram", "");
            this.mainClass = ParamUtil.getString(request, "mainClass", "");
            this.normalizedKeyword = normalizeVietnamese(keyword);
        }

        /**
         * Factory method tạo {@link GradeListFilter} gắn với học kỳ đang chọn và học kỳ mặc định.
         */
        static GradeListFilter from(HttpServletRequest request, int semesterId, int defaultSemesterId) {
            return new GradeListFilter(request, semesterId, defaultSemesterId);
        }

        /**
         * Kiểm tra một bản ghi tổng hợp điểm sinh viên có khớp đồng thời toàn bộ điều kiện lọc hay không.
         */
        boolean matches(StudentGradeSummaryDTO item) {
            return matchesKeyword(item)
                    && matchesExact(department, item.getDepartmentName())
                    && matchesExact(major, item.getMajorName())
                    && matchesExact(academicProgram, item.getAcademicProgramName())
                    && matchesExact(mainClass, item.getMainClassCode());
        }

        /**
         * Đẩy giá trị các tiêu chí lọc, số lượng bộ lọc đang bật và query string tương ứng vào request.
         */
        void applyTo(HttpServletRequest request) {
            request.setAttribute("keyword", keyword);
            request.setAttribute("selectedDepartment", department);
            request.setAttribute("selectedMajor", major);
            request.setAttribute("selectedAcademicProgram", academicProgram);
            request.setAttribute("selectedMainClass", mainClass);
            int count = activeCount();
            request.setAttribute("filtered", count > 0);
            request.setAttribute("activeFilterCount", count);
            request.setAttribute("filterQuery", toQuery());
        }

        /**
         * Kiểm tra từ khóa tìm kiếm khớp với mã sinh viên hoặc họ tên (trả về {@code true} nếu không nhập từ khóa).
         */
        private boolean matchesKeyword(StudentGradeSummaryDTO item) {
            return normalizedKeyword.isEmpty()
                    || containsKeyword(item.getStudentCode())
                    || containsKeyword(item.getStudentName());
        }

        /**
         * Kiểm tra chuỗi ứng viên sau khi chuẩn hóa tiếng Việt không dấu có chứa {@link #normalizedKeyword} hay không.
         */
        private boolean containsKeyword(String candidate) {
            return candidate != null && normalizeVietnamese(candidate).contains(normalizedKeyword);
        }

        /**
         * Đếm số lượng tiêu chí lọc đang khác trạng thái mặc định (bao gồm cả việc đổi học kỳ khác học kỳ ACTIVE).
         */
        private int activeCount() {
            // Tính 1 bộ lọc nếu người dùng chọn học kỳ khác với học kỳ mặc định
            int count = semesterId != defaultSemesterId ? 1 : 0;
            // Duyệt 5 tiêu chí lọc dạng chuỗi để cộng thêm vào tổng số bộ lọc đang áp dụng
            for (String field : List.of(normalizedKeyword, department, major, academicProgram, mainClass)) {
                // Tăng biến đếm nếu trường lọc có giá trị khác rỗng
                if (!field.isEmpty()) {
                    count++;
                }
            }
            return count;
        }

        /**
         * Dựng chuỗi query string đã URL-encode chứa học kỳ và các tiêu chí lọc đang dùng để nối vào link phân trang.
         */
        private String toQuery() {
            StringBuilder query = new StringBuilder();
            appendEncodedParam(query, "semesterId", String.valueOf(semesterId));
            appendEncodedParam(query, "keyword", keyword);
            appendEncodedParam(query, "department", department);
            appendEncodedParam(query, "major", major);
            appendEncodedParam(query, "academicProgram", academicProgram);
            appendEncodedParam(query, "mainClass", mainClass);
            return query.toString();
        }

        /**
         * So khớp chính xác giá trị được chọn với giá trị của bản ghi (trả về {@code true} nếu không lọc trường này).
         */
        private static boolean matchesExact(String selected, String value) {
            return selected.isEmpty() || (value != null && selected.equals(value.trim()));
        }

        /**
         * Nối cặp {@code &name=encodedValue} vào query string nếu {@code value} không rỗng.
         */
        private static void appendEncodedParam(StringBuilder query, String name, String value) {
            // Bỏ qua tham số rỗng để giữ URL phân trang gọn gàng
            if (!value.isEmpty()) {
                query.append('&').append(name).append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }

        /**
         * Chuẩn hóa chuỗi tiếng Việt về chữ thường không dấu (đổi {@code đ/Đ} sang {@code d})
         * giúp tìm kiếm họ tên và MSSV không phụ thuộc dấu hay chữ hoa/thường.
         */
        private static String normalizeVietnamese(String value) {
            // Trả về chuỗi rỗng nếu đầu vào null hoặc toàn khoảng trắng
            if (value == null || value.isBlank()) {
                return "";
            }
            // Phân rã ký tự Unicode theo chuẩn NFD, loại bỏ toàn bộ dấu thanh/mũ rồi quy về chữ thường
            String decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
            return decomposed.replaceAll("\\p{M}", "")
                    .replace('đ', 'd')
                    .replace('Đ', 'D')
                    .toLowerCase(Locale.ROOT);
        }
    }
}
