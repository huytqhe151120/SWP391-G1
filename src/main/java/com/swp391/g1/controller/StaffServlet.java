package com.swp391.g1.controller;

import com.swp391.g1.model.Staff;
import com.swp391.g1.service.IStaffService;
import com.swp391.g1.service.impl.StaffServiceImpl;
import com.swp391.g1.util.ParamUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/staff")
public class StaffServlet extends HttpServlet {

    private final IStaffService staffService =
            new StaffServiceImpl();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String action = ParamUtil.getString(request, "action", "");
        if ("detail".equals(action) || "edit".equals(action)) {
            showStaff(request, response, action);
            return;
        }

        String keyword = ParamUtil.getString(request, "keyword", "");
        String id = ParamUtil.getString(request, "id", "");
        String code = ParamUtil.getString(request, "code", "");
        String name = ParamUtil.getString(request, "name", "");
        String dob = ParamUtil.getString(request, "dob", "");
        String gender = ParamUtil.getString(request, "gender", "");
        String accountId = ParamUtil.getString(request, "accountId", "");
        String status = ParamUtil.getString(request, "status", "");
        int page = ParamUtil.getInt(request, "page", 1);
        int pageSize = ParamUtil.getInt(request, "pageSize", 10);
        if (pageSize != 5 && pageSize != 10 && pageSize != 20 && pageSize != 25) {
            pageSize = 10;
        }

        try {
            List<Staff> allStaff = staffService.searchStaff(
                    keyword, id, code, name, dob, gender, accountId, status);
            int totalStaffCount = allStaff.size();
            int totalPages = Math.max(1, (int) Math.ceil((double) totalStaffCount / pageSize));
            int currentPage = Math.max(1, Math.min(page, totalPages));
            int pageWindowStart = ((currentPage - 1) / 3) * 3 + 1;
            int pageWindowEnd = Math.min(pageWindowStart + 2, totalPages);

            int fromIndex = Math.min((currentPage - 1) * pageSize, totalStaffCount);
            int toIndex = Math.min(fromIndex + pageSize, totalStaffCount);

            List<Staff> pageStaff = new ArrayList<>(allStaff.subList(fromIndex, toIndex));

            request.setAttribute("staffList", pageStaff);
            request.setAttribute("pageSize", pageSize);
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("pageWindowStart", pageWindowStart);
            request.setAttribute("pageWindowEnd", pageWindowEnd);
            request.setAttribute("totalStaffCount", totalStaffCount);
            request.setAttribute("firstStaffNumber", totalStaffCount == 0 ? 0 : fromIndex + 1);
            request.setAttribute("lastStaffNumber", toIndex);

            boolean filtered = !keyword.isEmpty() || !id.isEmpty() || !code.isEmpty()
                    || !name.isEmpty() || !dob.isEmpty() || !gender.isEmpty()
                    || !accountId.isEmpty() || !status.isEmpty();
            request.setAttribute("filtered", filtered);
            request.setAttribute("activeFilterCount",
                    (keyword.isEmpty() ? 0 : 1) + (id.isEmpty() ? 0 : 1)
                    + (code.isEmpty() ? 0 : 1) + (name.isEmpty() ? 0 : 1)
                    + (dob.isEmpty() ? 0 : 1) + (gender.isEmpty() ? 0 : 1)
                    + (accountId.isEmpty() ? 0 : 1) + (status.isEmpty() ? 0 : 1));
            request.setAttribute("filterQuery", buildFilterQuery(
                    keyword, id, code, name, dob, gender, accountId, status));
            request.setAttribute("keyword", keyword);
            request.setAttribute("filterId", id);
            request.setAttribute("filterCode", code);
            request.setAttribute("filterName", name);
            request.setAttribute("filterDob", dob);
            request.setAttribute("filterGender", gender);
            request.setAttribute("filterAccountId", accountId);
            request.setAttribute("filterStatus", status);

            request.getRequestDispatcher(
                    "/WEB-INF/views/staff/list-staff.jsp"
            ).forward(request, response);

        } catch (Exception e) {
            throw new ServletException(
                    "Không thể tải danh sách nhân viên.",
                    e
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
        Staff staff = new Staff();
        staff.setId(id);
        staff.setCode(request.getParameter("code"));
        staff.setName(request.getParameter("name"));
        staff.setDob(request.getParameter("dob"));
        staff.setGender(request.getParameter("gender"));
        staff.setStatus(request.getParameter("status"));
        try {
            staff.setAccountId(request.getParameter("accountId") == null
                    || request.getParameter("accountId").isBlank()
                    ? 0 : Integer.parseInt(request.getParameter("accountId")));
            if (!staffService.updateStaff(staff)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=notFound");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/staff?action=detail&id=" + id + "&msg=updated");
        } catch (IllegalArgumentException e) {
            request.setAttribute("staff", staff);
            request.setAttribute("formError", e instanceof NumberFormatException
                    ? "Account ID phải là số nguyên hợp lệ." : e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/staff/form.jsp").forward(request, response);
        } catch (IllegalStateException e) {
            String validationMessage = constraintViolationMessage(e);
            if (validationMessage == null) {
                throw new ServletException("Không thể cập nhật thông tin nhân viên.", e);
            }
            request.setAttribute("staff", staff);
            request.setAttribute("formError", validationMessage);
            request.getRequestDispatcher("/WEB-INF/views/staff/form.jsp").forward(request, response);
        } catch (Exception e) {
            throw new ServletException("Không thể cập nhật thông tin nhân viên.", e);
        }
    }

    private String constraintViolationMessage(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sqlException) {
                if (sqlException.getErrorCode() == 2601 || sqlException.getErrorCode() == 2627) {
                    return "Mã nhân viên hoặc Account ID đã được sử dụng.";
                }
                if (sqlException.getErrorCode() == 2628 || sqlException.getErrorCode() == 8152) {
                    return "Một hoặc nhiều thông tin vượt quá độ dài cho phép.";
                }
                if (sqlException.getErrorCode() == 547) {
                    return "Account ID không tồn tại hoặc không thể liên kết với nhân viên.";
                }
                if (sqlException.getSQLState() != null && sqlException.getSQLState().startsWith("23")) {
                    return "Thông tin nhân viên không hợp lệ hoặc đã tồn tại.";
                }
            }
        }
        return null;
    }

    private void showStaff(HttpServletRequest request, HttpServletResponse response, String action)
            throws ServletException, IOException {
        int id = ParamUtil.getInt(request, "id", 0);
        Staff staff = staffService.getStaffById(id);
        if (staff == null) {
            response.sendRedirect(request.getContextPath() + "/staff?error=notFound");
            return;
        }
        request.setAttribute("staff", staff);
        if ("edit".equals(action)) {
            request.getRequestDispatcher("/WEB-INF/views/staff/form.jsp").forward(request, response);
        } else {
            request.getRequestDispatcher("/WEB-INF/views/staff/detail.jsp").forward(request, response);
        }
    }

    private String buildFilterQuery(String keyword, String id, String code, String name, String dob,
                                    String gender, String accountId, String status) {
        StringBuilder query = new StringBuilder();
        appendParam(query, "keyword", keyword);
        appendParam(query, "id", id);
        appendParam(query, "code", code);
        appendParam(query, "name", name);
        appendParam(query, "dob", dob);
        appendParam(query, "gender", gender);
        appendParam(query, "accountId", accountId);
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