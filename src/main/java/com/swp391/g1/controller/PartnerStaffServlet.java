package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.request.PartnerStaffRequestDTO;
import com.swp391.g1.dto.response.PartnerStaffResponseDTO;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IPartnerCompanyService;
import com.swp391.g1.service.IPartnerStaffService;
import com.swp391.g1.service.impl.PartnerCompanyServiceImpl;
import com.swp391.g1.service.impl.PartnerStaffServiceImpl;
import com.swp391.g1.util.AuthContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "PartnerStaffServlet", urlPatterns = {"/partner-staffs"})
public class PartnerStaffServlet extends HttpServlet {

    private final IPartnerStaffService partnerStaffService;
    private final IPartnerCompanyService partnerCompanyService;

    /**
     * Khởi tạo servlet quản lý nhân viên đối tác cùng các service nghiệp vụ liên quan.
     */
    public PartnerStaffServlet() {
        this.partnerStaffService = new PartnerStaffServiceImpl();
        this.partnerCompanyService = new PartnerCompanyServiceImpl();
    }

    /**
     * Kiểm tra trạng thái đăng nhập và quyền quản lý đối tác trước khi cho phép thực thi doGet/doPost.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        // Nếu người dùng chưa đăng nhập thì chuyển hướng về trang đăng nhập
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        // Nếu người dùng không có quyền quản lý đối tác thì trả về lỗi 403 Forbidden
        if (!AccessPolicy.canManagePartners(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập trang này.");
            return;
        }
        super.service(request, response);
    }

    /**
     * Điều phối các yêu cầu GET quản lý nhân viên đối tác theo tham số {@code action}.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        // Mặc định hiển thị danh sách nhân viên đối tác nếu không có tham số action
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }

        try {
            // Điều hướng tới handler phù hợp theo giá trị action
            switch (action) {
                // Hiển thị form thêm mới nhân viên đối tác
                case "create":
                    showCreateForm(request, response);
                    break;
                // Hiển thị form cập nhật nhân viên đối tác
                case "edit":
                    showEditForm(request, response);
                    break;
                // Hiển thị trang chi tiết nhân viên đối tác
                case "detail":
                    showDetail(request, response);
                    break;
                // Thực hiện xóa nhân viên đối tác theo ID
                case "delete":
                    deleteStaff(request, response);
                    break;
                // Mặc định hiển thị danh sách nhân viên đối tác
                case "list":
                default:
                    listStaffs(request, response);
                    break;
            }
        } catch (Exception e) {
            // Chuyển tiếp sang trang báo lỗi hệ thống nếu phát sinh ngoại lệ không mong muốn
            request.setAttribute("errorMessage", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    /**
     * Điều phối các yêu cầu POST tạo mới hoặc cập nhật nhân viên đối tác theo tham số {@code action}.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        // Nếu thiếu tham số action thì đặt mặc định "list" để chuyển hướng về danh sách
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }

        try {
            // Phân nhánh xử lý gửi biểu mẫu tạo mới hoặc cập nhật nhân viên đối tác
            switch (action) {
                // Lưu nhân viên đối tác mới
                case "create":
                    createStaff(request, response);
                    break;
                // Lưu thay đổi thông tin nhân viên đối tác
                case "update":
                    updateStaff(request, response);
                    break;
                // Action không hỗ trợ thì quay về danh sách nhân viên đối tác
                default:
                    response.sendRedirect(request.getContextPath() + "/partner-staffs");
                    break;
            }
        } catch (Exception e) {
            // Bắt lỗi xử lý dữ liệu và chuyển tiếp sang trang lỗi chung
            request.setAttribute("errorMessage", "Lỗi xử lý dữ liệu: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    // =========================================================================
    // GET ACTION HANDLERS
    // =========================================================================

    /**
     * Tải danh sách nhân viên đối tác (tất cả hoặc lọc theo {@code companyId}) cùng danh sách công ty đối tác cho bộ lọc.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
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
            // Nếu không chọn công ty thì lấy toàn bộ danh sách nhân viên đối tác
            list = partnerStaffService.getAllPartnerStaffs();
        }

        request.setAttribute("partnerStaffs", list);
        request.setAttribute("partnerCompanies", partnerCompanyService.getAllPartnerCompanies());
        request.getRequestDispatcher("/WEB-INF/views/partner-staff/list.jsp").forward(request, response);
    }

    /**
     * Nạp dữ liệu dropdown công ty đối tác và chuyển tiếp sang trang tạo mới nhân viên đối tác.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    private void showCreateForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        loadDropdownData(request);
        request.getRequestDispatcher("/WEB-INF/views/partner-staff/create.jsp").forward(request, response);
    }

    /**
     * Hiển thị biểu mẫu chỉnh sửa nhân viên đối tác; tải dữ liệu từ CSDL theo {@code id} nếu chưa có sẵn trong request.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PartnerStaffResponseDTO staff = (PartnerStaffResponseDTO) request.getAttribute("partnerStaff");
        // Nếu chưa có đối tượng trong request thì đọc ID từ tham số và truy vấn từ service
        if (staff == null) {
            String idStr = request.getParameter("id");
            // Chỉ phân tích và truy vấn khi tham số id không rỗng
            if (idStr != null && !idStr.trim().isEmpty()) {
                int id = Integer.parseInt(idStr);
                staff = partnerStaffService.getPartnerStaffById(id);
            }
        }
        // Nếu tìm thấy nhân viên đối tác thì nạp dropdown và chuyển sang edit.jsp; ngược lại quay về danh sách
        if (staff != null) {
            request.setAttribute("partnerStaff", staff);
            loadDropdownData(request);
            request.getRequestDispatcher("/WEB-INF/views/partner-staff/edit.jsp").forward(request, response);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/partner-staffs");
    }

    /**
     * Tải thông tin chi tiết một nhân viên đối tác theo {@code id} và chuyển tiếp sang trang detail.jsp.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    private void showDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idStr = request.getParameter("id");
        // Kiểm tra tham số id không rỗng trước khi truy vấn chi tiết nhân viên
        if (idStr != null && !idStr.trim().isEmpty()) {
            int id = Integer.parseInt(idStr);
            PartnerStaffResponseDTO staff = partnerStaffService.getPartnerStaffById(id);
            // Nếu tồn tại nhân viên thì hiển thị trang chi tiết
            if (staff != null) {
                request.setAttribute("partnerStaff", staff);
                request.getRequestDispatcher("/WEB-INF/views/partner-staff/detail.jsp").forward(request, response);
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/partner-staffs");
    }

    /**
     * Xóa nhân viên đối tác theo {@code id}, lưu thông báo kết quả vào session rồi chuyển hướng về danh sách.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    private void deleteStaff(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String idStr = request.getParameter("id");
        HttpSession session = request.getSession();

        // Thực hiện xóa khi có truyền ID nhân viên hợp lệ
        if (idStr != null && !idStr.trim().isEmpty()) {
            int id = Integer.parseInt(idStr);
            boolean success = partnerStaffService.deletePartnerStaff(id);
            // Gắn flash message thành công hoặc thất bại vào session
            if (success) {
                session.setAttribute("successMessage", "Xóa nhân viên đối tác thành công!");
            } else {
                // Ghi nhận thông báo lỗi khi không xóa được nhân viên đối tác
                session.setAttribute("errorMessage", "Không thể xóa nhân viên đối tác. Vui lòng kiểm tra lại!");
            }
        }
        response.sendRedirect(request.getContextPath() + "/partner-staffs");
    }

    // =========================================================================
    // POST ACTION HANDLERS
    // =========================================================================

    /**
     * Đọc dữ liệu form, gọi service tạo mới nhân viên đối tác và xử lý chuyển hướng hoặc hiển thị lỗi validate.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    private void createStaff(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PartnerStaffRequestDTO dto = buildRequestDTO(request);
        HttpSession session = request.getSession();

        try {
            boolean success = partnerStaffService.createPartnerStaff(dto);
            // Nếu tạo mới thành công thì lưu thông báo vào session và quay về trang danh sách
            if (success) {
                session.setAttribute("successMessage", "Tạo mới nhân viên đối tác thành công!");
                response.sendRedirect(request.getContextPath() + "/partner-staffs");
            } else {
                // Nếu tạo mới thất bại thì giữ nguyên dữ liệu đã nhập và mở lại biểu mẫu tạo mới
                request.setAttribute("errorMessage", "Không thể tạo nhân viên đối tác. Vui lòng thử lại.");
                request.setAttribute("formData", dto);
                showCreateForm(request, response);
            }
        } catch (Exception e) {
            // Bắt lỗi validate/nghiệp vụ để hiển thị trực tiếp trên biểu mẫu tạo mới
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("formData", dto);
            showCreateForm(request, response);
        }
    }

    /**
     * Đọc dữ liệu form cùng ID nhân viên, gọi service cập nhật và xử lý chuyển hướng hoặc hiển thị lỗi trên form edit.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    private void updateStaff(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PartnerStaffRequestDTO dto = buildRequestDTO(request);
        dto.setId(Integer.parseInt(request.getParameter("id")));

        HttpSession session = request.getSession();

        try {
            boolean success = partnerStaffService.updatePartnerStaff(dto);
            // Nếu cập nhật thành công thì lưu flash message và chuyển hướng về danh sách
            if (success) {
                session.setAttribute("successMessage", "Cập nhật nhân viên đối tác thành công!");
                response.sendRedirect(request.getContextPath() + "/partner-staffs");
            } else {
                // Nếu không cập nhật được thì giữ lại dữ liệu đã nhập và mở lại form chỉnh sửa
                request.setAttribute("errorMessage", "Cập nhật thất bại. Vui lòng kiểm tra lại.");
                request.setAttribute("formData", dto);
                showEditForm(request, response);
            }
        } catch (Exception e) {
            // Bắt lỗi validate hoặc lỗi CSDL và hiển thị lại biểu mẫu chỉnh sửa cùng thông báo lỗi
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("formData", dto);
            showEditForm(request, response);
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Nạp danh sách tất cả công ty đối tác vào request để hiển thị trong dropdown chọn công ty.
     *
     * @param request đối tượng HTTP request
     */
    private void loadDropdownData(HttpServletRequest request) {
        request.setAttribute("partnerCompanies", partnerCompanyService.getAllPartnerCompanies());
    }

    /**
     * Gom các tham số từ biểu mẫu HTTP thành đối tượng {@link PartnerStaffRequestDTO}.
     *
     * @param request đối tượng HTTP request chứa dữ liệu biểu mẫu nhân viên đối tác
     * @return DTO yêu cầu chứa dữ liệu thô từ form
     */
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