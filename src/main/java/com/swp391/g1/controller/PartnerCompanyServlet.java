package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.Enum.CommonStatus;
import com.swp391.g1.model.PartnerCompany;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IPartnerCompanyService;
import com.swp391.g1.service.impl.PartnerCompanyServiceImpl;
import com.swp391.g1.util.AuthContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "PartnerCompanyServlet", urlPatterns = {"/partner-companies"})
public class PartnerCompanyServlet extends HttpServlet {

    private final IPartnerCompanyService partnerCompanyService;

    /**
     * Khởi tạo servlet quản lý công ty đối tác cùng service nghiệp vụ tương ứng.
     */
    public PartnerCompanyServlet() {
        this.partnerCompanyService = new PartnerCompanyServiceImpl();
    }

    /**
     * Chặn và kiểm tra xác thực, phân quyền quản lý đối tác trước khi chuyển yêu cầu sang doGet/doPost.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        // Nếu chưa đăng nhập thì chuyển hướng về trang đăng nhập
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
     * Điều phối các yêu cầu GET quản lý công ty đối tác theo tham số {@code action}.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        // Nếu không truyền action thì mặc định hiển thị danh sách công ty đối tác
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }

        try {
            // Điều hướng tới phương thức xử lý tương ứng theo giá trị action
            switch (action) {
                // Hiển thị biểu mẫu tạo mới công ty đối tác
                case "create":
                    showCreateForm(request, response);
                    break;
                // Hiển thị biểu mẫu chỉnh sửa thông tin công ty đối tác
                case "edit":
                    showEditForm(request, response);
                    break;
                // Hiển thị trang chi tiết công ty đối tác
                case "detail":
                    showDetail(request, response);
                    break;
                // Thực hiện xóa công ty đối tác theo ID
                case "delete":
                    deleteCompany(request, response);
                    break;
                // Mặc định hiển thị danh sách tất cả công ty đối tác
                case "list":
                default:
                    listCompanies(request, response);
                    break;
            }
        } catch (Exception e) {
            // Chuyển tiếp sang trang báo lỗi hệ thống nếu phát sinh ngoại lệ không mong muốn
            request.setAttribute("errorMessage", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    /**
     * Điều phối các yêu cầu POST tạo mới hoặc cập nhật công ty đối tác theo tham số {@code action}.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        // Nếu thiếu action thì gán mặc định "list" để rơi vào nhánh redirect về trang danh sách
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }

        try {
            // Phân nhánh xử lý gửi biểu mẫu tạo mới hoặc cập nhật
            switch (action) {
                // Xử lý lưu mới công ty đối tác
                case "create":
                    createCompany(request, response);
                    break;
                // Xử lý lưu thay đổi công ty đối tác
                case "update":
                    updateCompany(request, response);
                    break;
                // Action không hỗ trợ thì quay về trang danh sách
                default:
                    response.sendRedirect(request.getContextPath() + "/partner-companies");
                    break;
            }
        } catch (Exception e) {
            // Bắt ngoại lệ xử lý dữ liệu và chuyển tiếp sang trang lỗi chung
            request.setAttribute("errorMessage", "Lỗi xử lý dữ liệu: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        }
    }

    // =========================================================================
    // GET ACTION HANDLERS
    // =========================================================================

    /**
     * Tải danh sách toàn bộ công ty đối tác và chuyển tiếp sang trang danh sách.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    private void listCompanies(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<PartnerCompany> list = partnerCompanyService.getAllPartnerCompanies();
        request.setAttribute("partnerCompanies", list);
        request.getRequestDispatcher("/WEB-INF/views/partner-company/list.jsp").forward(request, response);
    }

    /**
     * Chuyển tiếp sang trang biểu mẫu tạo mới công ty đối tác.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    private void showCreateForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/partner-company/create.jsp").forward(request, response);
    }

    /**
     * Hiển thị biểu mẫu chỉnh sửa công ty đối tác; ưu tiên dùng lại dữ liệu form trong request khi có lỗi validate,
     * nếu chưa có thì truy vấn theo tham số {@code id}.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PartnerCompany company = (PartnerCompany) request.getAttribute("partnerCompany");
        // Nếu chưa có đối tượng truyền lại từ POST lỗi thì đọc ID từ query string và tải từ CSDL
        if (company == null) {
            String idStr = request.getParameter("id");
            // Chỉ phân tích ID và truy vấn khi tham số id không rỗng
            if (idStr != null && !idStr.trim().isEmpty()) {
                int id = Integer.parseInt(idStr);
                company = partnerCompanyService.getPartnerCompanyById(id);
            }
        }
        // Nếu tìm thấy công ty đối tác thì chuyển sang trang edit.jsp; ngược lại quay về danh sách
        if (company != null) {
            request.setAttribute("partnerCompany", company);
            request.getRequestDispatcher("/WEB-INF/views/partner-company/edit.jsp").forward(request, response);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/partner-companies");
    }

    /**
     * Tải thông tin chi tiết một công ty đối tác theo ID và chuyển tiếp sang trang detail.jsp.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    private void showDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idStr = request.getParameter("id");
        // Kiểm tra tham số id hợp lệ trước khi truy vấn chi tiết công ty
        if (idStr != null && !idStr.trim().isEmpty()) {
            int id = Integer.parseInt(idStr);
            PartnerCompany company = partnerCompanyService.getPartnerCompanyById(id);
            // Nếu tồn tại bản ghi thì hiển thị trang chi tiết
            if (company != null) {
                request.setAttribute("partnerCompany", company);
                request.getRequestDispatcher("/WEB-INF/views/partner-company/detail.jsp").forward(request, response);
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/partner-companies");
    }

    /**
     * Xóa công ty đối tác theo tham số {@code id}, lưu thông báo kết quả vào session rồi quay lại trang danh sách.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    private void deleteCompany(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String idStr = request.getParameter("id");
        HttpSession session = request.getSession();

        // Thực hiện xóa khi có truyền ID công ty hợp lệ
        if (idStr != null && !idStr.trim().isEmpty()) {
            int id = Integer.parseInt(idStr);
            boolean success = partnerCompanyService.deletePartnerCompany(id);
            // Gắn thông báo flash thành công hoặc thất bại vào session
            if (success) {
                session.setAttribute("successMessage", "Xóa công ty đối tác thành công!");
            } else {
                // Ghi nhận thông báo lỗi khi xóa không thành công
                session.setAttribute("errorMessage", "Không thể xóa công ty đối tác. Vui lòng kiểm tra lại!");
            }
        }
        response.sendRedirect(request.getContextPath() + "/partner-companies");
    }

    // =========================================================================
    // POST ACTION HANDLERS
    // =========================================================================

    /**
     * Đọc dữ liệu biểu mẫu và gọi service tạo mới công ty đối tác;
     * chuyển hướng về danh sách nếu thành công hoặc mở lại form kèm thông báo lỗi nếu thất bại.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    private void createCompany(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PartnerCompany company = buildEntityFromRequest(request);
        HttpSession session = request.getSession();

        try {
            boolean success = partnerCompanyService.createPartnerCompany(company);
            // Nếu tạo mới thành công thì chuyển hướng về trang danh sách kèm flash message
            if (success) {
                session.setAttribute("successMessage", "Tạo mới công ty đối tác thành công!");
                response.sendRedirect(request.getContextPath() + "/partner-companies");
            } else {
                // Nếu lưu thất bại thì giữ nguyên dữ liệu đã nhập và mở lại form tạo mới
                request.setAttribute("errorMessage", "Không thể tạo công ty đối tác. Vui lòng thử lại.");
                request.setAttribute("partnerCompany", company);
                showCreateForm(request, response);
            }
        } catch (Exception e) {
            // Bắt lỗi validate hoặc lỗi nghiệp vụ để hiển thị ngay trên biểu mẫu tạo mới
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("partnerCompany", company);
            showCreateForm(request, response);
        }
    }

    /**
     * Đọc dữ liệu biểu mẫu và gọi service cập nhật công ty đối tác;
     * chuyển hướng về danh sách nếu thành công hoặc mở lại form chỉnh sửa nếu có lỗi.
     *
     * @param request  đối tượng HTTP request
     * @param response đối tượng HTTP response
     */
    private void updateCompany(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PartnerCompany company = buildEntityFromRequest(request);

        String idStr = request.getParameter("id");
        // Gán ID công ty đối tác từ tham số request nếu không rỗng
        if (idStr != null && !idStr.trim().isEmpty()) {
            company.setId(Integer.parseInt(idStr));
        }

        String statusStr = request.getParameter("status");
        // Chuyển đổi chuỗi trạng thái sang enum CommonStatus nếu người dùng có chọn
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                company.setStatus(CommonStatus.valueOf(statusStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Fallback về trạng thái ACTIVE nếu giá trị trạng thái gửi lên không hợp lệ
                company.setStatus(CommonStatus.ACTIVE);
            }
        }

        HttpSession session = request.getSession();

        try {
            boolean success = partnerCompanyService.updatePartnerCompany(company);
            // Nếu cập nhật thành công thì chuyển hướng về danh sách kèm thông báo thành công
            if (success) {
                session.setAttribute("successMessage", "Cập nhật công ty đối tác thành công!");
                response.sendRedirect(request.getContextPath() + "/partner-companies");
            } else {
                // Nếu cập nhật không ảnh hưởng bản ghi nào thì giữ lại dữ liệu và mở lại form chỉnh sửa
                request.setAttribute("errorMessage", "Cập nhật thất bại. Vui lòng kiểm tra lại.");
                request.setAttribute("partnerCompany", company);
                showEditForm(request, response);
            }
        } catch (Exception e) {
            // Bắt lỗi validate/CSDL và hiển thị lại biểu mẫu chỉnh sửa cùng thông báo lỗi
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("partnerCompany", company);
            showEditForm(request, response);
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Trích xuất các trường dữ liệu từ request để dựng đối tượng thực thể {@link PartnerCompany}.
     *
     * @param request đối tượng HTTP request chứa dữ liệu biểu mẫu
     * @return thực thể công ty đối tác đã điền các trường cơ bản
     */
    private PartnerCompany buildEntityFromRequest(HttpServletRequest request) {
        PartnerCompany company = new PartnerCompany();
        company.setCode(request.getParameter("code"));
        company.setName(request.getParameter("name"));
        company.setWebsite(request.getParameter("website"));
        company.setEmail(request.getParameter("email"));
        company.setPhoneNumber(request.getParameter("phone"));
        company.setAddress(request.getParameter("address"));
        company.setDescription(request.getParameter("description"));
        return company;
    }
}
