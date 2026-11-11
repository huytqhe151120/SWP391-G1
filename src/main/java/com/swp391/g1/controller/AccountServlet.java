package com.swp391.g1.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.Account;
import com.swp391.g1.model.AccountProfile;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.AccountOperationResult;
import com.swp391.g1.service.AccountServiceException;
import com.swp391.g1.service.IAccountService;
import com.swp391.g1.service.impl.AccountServiceImpl;
import com.swp391.g1.util.AuthContext;
import com.swp391.g1.util.ParamUtil;

/**
 * Controller for the Account Management screens.
 * Routes: GET /accounts, GET|POST /accounts/create, GET /accounts/{id},
 * GET|POST /accounts/{id}/edit, POST /accounts/{id}/status.
 * Every route requires an authenticated ADMIN account; the single check in
 * service() keeps the rule in AccessPolicy instead of duplicating it per route.
 */
@WebServlet("/accounts/*")
public class AccountServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final IAccountService accountService = new AccountServiceImpl();

    /**
     * Authorization gate for the whole feature. It overrides the method-dispatch
     * entry point, so every HTTP method is checked before any doXxx handler runs.
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        if (currentUser == null) {
            response.sendRedirect(contextPath(request) + "/login");
            return;
        }
        if (!AccessPolicy.canManageAccounts(currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        super.service(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String path = request.getPathInfo();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            showList(request, response);
        } else if ("/create".equals(path)) {
            showCreateForm(request, response);
        } else if (isPath(path, "/\\d{1,9}")) {
            showDetail(request, response, parseIdFromPath(path));
        } else if (isPath(path, "/\\d{1,9}/edit")) {
            showEditForm(request, response, parseIdFromPath(path));
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String path = request.getPathInfo();
        if ("/create".equals(path)) {
            createAccount(request, response);
        } else if (isPath(path, "/\\d{1,9}/edit")) {
            updateAccount(request, response, parseIdFromPath(path));
        } else if (isPath(path, "/\\d{1,9}/status")) {
            changeStatus(request, response, parseIdFromPath(path));
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String id = ParamUtil.getString(request, "id", "");
        String search = ParamUtil.getString(request, "search", "");
        String type = ParamUtil.getString(request, "type", "");
        String role = ParamUtil.getString(request, "role", "");
        String status = ParamUtil.getString(request, "status", "");
        int page = ParamUtil.getInt(request, "page", 1);
        int pageSize = ParamUtil.getInt(request, "pageSize", 10);
        if (pageSize != 5 && pageSize != 10 && pageSize != 20 && pageSize != 25) {
            pageSize = 10;
        }

        try {
            List<Account> allAccounts = accountService.searchAccounts(id, search, type, role, status);
            int totalAccountCount = allAccounts.size();
            int totalPages = Math.max(1, (int) Math.ceil((double) totalAccountCount / pageSize));
            int currentPage = Math.max(1, Math.min(page, totalPages));
            int pageWindowStart = ((currentPage - 1) / 3) * 3 + 1;
            int pageWindowEnd = Math.min(pageWindowStart + 2, totalPages);

            int fromIndex = Math.min((currentPage - 1) * pageSize, totalAccountCount);
            int toIndex = Math.min(fromIndex + pageSize, totalAccountCount);

            List<Account> pageAccounts = new ArrayList<>(allAccounts.subList(fromIndex, toIndex));

            request.setAttribute("accounts", pageAccounts);
            request.setAttribute("accountTypes", accountService.getAccountTypes());
            request.setAttribute("accountRoles", accountService.getAccountRoles());
            request.setAttribute("pageSize", pageSize);
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("pageWindowStart", pageWindowStart);
            request.setAttribute("pageWindowEnd", pageWindowEnd);
            request.setAttribute("totalAccountCount", totalAccountCount);
            request.setAttribute("firstAccountNumber", totalAccountCount == 0 ? 0 : fromIndex + 1);
            request.setAttribute("lastAccountNumber", toIndex);

            boolean filtered = !id.isEmpty() || !search.isEmpty() || !type.isEmpty() || !role.isEmpty() || !status.isEmpty();
            request.setAttribute("filtered", filtered);
            request.setAttribute("activeFilterCount",
                    (id.isEmpty() ? 0 : 1) + (search.isEmpty() ? 0 : 1) + (type.isEmpty() ? 0 : 1)
                    + (role.isEmpty() ? 0 : 1) + (status.isEmpty() ? 0 : 1));
            request.setAttribute("filterQuery", buildFilterQuery(id, search, type, role, status));
        } catch (AccountServiceException e) {
            request.setAttribute("generalError", e.getMessage());
            request.setAttribute("accounts", List.of());
            request.setAttribute("accountTypes", List.of());
            request.setAttribute("accountRoles", List.of());
            request.setAttribute("pageSize", pageSize);
            request.setAttribute("currentPage", 1);
            request.setAttribute("totalPages", 1);
            request.setAttribute("pageWindowStart", 1);
            request.setAttribute("pageWindowEnd", 1);
            request.setAttribute("totalAccountCount", 0);
            request.setAttribute("firstAccountNumber", 0);
            request.setAttribute("lastAccountNumber", 0);
            request.setAttribute("filtered", false);
            request.setAttribute("activeFilterCount", 0);
            request.setAttribute("filterQuery", "");
        }
        request.setAttribute("filterId", id);
        request.setAttribute("search", search);
        request.setAttribute("filterType", type);
        request.setAttribute("filterRole", role);
        request.setAttribute("filterStatus", status);
        request.setAttribute("accountStatuses", IAccountService.ACCOUNT_STATUSES);
        forward(request, response, "account/list.jsp");
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            prepareFormData(request);
        } catch (AccountServiceException e) {
            response.sendRedirect(contextPath(request) + "/accounts?error=db");
            return;
        }
        request.setAttribute("account", new Account());
        forward(request, response, "account/create.jsp");
    }

    private void createAccount(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Account account = new Account(
                0,
                request.getParameter("username"),
                request.getParameter("password"),
                request.getParameter("type"),
                request.getParameter("role"),
                request.getParameter("status"));

        AccountOperationResult result;
        try {
            result = accountService.createAccount(account);
        } catch (AccountServiceException e) {
            request.setAttribute("generalError", e.getMessage());
            setAccountAttribute(request, account);
            safePrepareFormData(request);
            forward(request, response, "account/create.jsp");
            return;
        }

        if (result.isSuccess()) {
            response.sendRedirect(contextPath(request) + "/accounts/" + result.getAccountId() + "?msg=created");
            return;
        }
        setAccountAttribute(request, account);
        request.setAttribute("fieldErrors", result.getFieldErrors());
        safePrepareFormData(request);
        forward(request, response, "account/create.jsp");
    }
    private void updateAccount(HttpServletRequest request, HttpServletResponse response, int id)
            throws ServletException, IOException {
        Account account = new Account(
                id,
                request.getParameter("username"),
                request.getParameter("password"),
                request.getParameter("type"),
                request.getParameter("role"),
                request.getParameter("status"));

        AccountOperationResult result;
        try {
            result = accountService.updateAccount(account);
        } catch (AccountServiceException e) {
            request.setAttribute("generalError", e.getMessage());
            setAccountAttribute(request, account);
            safePrepareFormData(request);
            forward(request, response, "account/edit.jsp");
            return;
        }

        if (result.isSuccess()) {
            response.sendRedirect(contextPath(request) + "/accounts/" + id + "?msg=updated");
            return;
        }
        setAccountAttribute(request, account);
        request.setAttribute("fieldErrors", result.getFieldErrors());
        safePrepareFormData(request);
        forward(request, response, "account/edit.jsp");
    }

    private void changeStatus(HttpServletRequest request, HttpServletResponse response, int id)
            throws IOException {
        String status = ParamUtil.getString(request, "status", "");
        AccountOperationResult result;
        try {
            result = accountService.changeStatus(id, status);
        } catch (AccountServiceException e) {
            response.sendRedirect(contextPath(request) + "/accounts?error=db");
            return;
        }
        if (result.isSuccess()) {
            response.sendRedirect(contextPath(request) + "/accounts/" + id + "?msg=statusChanged");
            return;
        }
        if ("notFound".equals(result.getMessage())) {
            response.sendRedirect(contextPath(request) + "/accounts?error=notFound");
        } else if ("invalidStatus".equals(result.getMessage())) {
            response.sendRedirect(contextPath(request) + "/accounts/" + id + "?error=invalidStatus");
        } else {
            response.sendRedirect(contextPath(request) + "/accounts?error=db");
        }
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, int id)
            throws ServletException, IOException {
        Account account;
        List<AccountProfile> profiles;
        try {
            account = accountService.getAccountById(id);
            if (account == null) {
                response.sendRedirect(contextPath(request) + "/accounts?error=notFound");
                return;
            }
            profiles = accountService.findProfiles(id);
        } catch (AccountServiceException e) {
            response.sendRedirect(contextPath(request) + "/accounts?error=db");
            return;
        }
        request.setAttribute("account", account);
        request.setAttribute("profiles", profiles);
        request.setAttribute("accountStatuses", IAccountService.ACCOUNT_STATUSES);
        forward(request, response, "account/detail.jsp");
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response, int id)
            throws ServletException, IOException {
        Account account;
        try {
            account = accountService.getAccountById(id);
            if (account == null) {
                response.sendRedirect(contextPath(request) + "/accounts?error=notFound");
                return;
            }
            prepareFormData(request);
        } catch (AccountServiceException e) {
            response.sendRedirect(contextPath(request) + "/accounts?error=db");
            return;
        }
        request.setAttribute("account", account);
        forward(request, response, "account/edit.jsp");
    }

    /** Domain data for the create/edit forms (types, role mapping, statuses). */
    private void prepareFormData(HttpServletRequest request) {
        request.setAttribute("accountTypes", accountService.getAccountTypes());
        request.setAttribute("accountRoles", accountService.getAccountRoles());
        request.setAttribute("rolesByType", accountService.getRolesByType());
        request.setAttribute("accountStatuses", IAccountService.ACCOUNT_STATUSES);
    }

    /** Domain data for POST error re-renders; degrades gracefully on DB failure. */
    private void safePrepareFormData(HttpServletRequest request) {
        try {
            prepareFormData(request);
        } catch (AccountServiceException e) {
            request.setAttribute("generalError", e.getMessage());
        }
    }

    /** Clears the password before the account reaches the view. */
    private static void setAccountAttribute(HttpServletRequest request, Account account) {
        account.setPassword("");
        request.setAttribute("account", account);
    }

    private static boolean isPath(String path, String regex) {
        return path != null && path.matches(regex);
    }

    private static String contextPath(HttpServletRequest request) {
        String ctx = request.getContextPath();
        return ctx == null ? "" : ctx;
    }

    private Integer parseIdFromPath(String path) {
        if (path == null || path.length() < 2) {
            return null;
        }
        String segment = path.startsWith("/") ? path.substring(1) : path;
        int slash = segment.indexOf('/');
        if (slash >= 0) {
            segment = segment.substring(0, slash);
        }
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void forward(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/" + view);
        dispatcher.forward(request, response);
    }

    private String buildFilterQuery(String id, String search, String type, String role, String status) {
        StringBuilder query = new StringBuilder();
        appendParam(query, "id", id);
        appendParam(query, "search", search);
        appendParam(query, "type", type);
        appendParam(query, "role", role);
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