package com.swp391.g1.controller;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.swp391.g1.model.Account;
import com.swp391.g1.model.AccountProfile;
import com.swp391.g1.service.AccountOperationResult;
import com.swp391.g1.service.AccountService;
import com.swp391.g1.service.AccountServiceException;
import com.swp391.g1.util.ParamUtil;

/**
 * Account Management controller.
 *
 * <p>Routes (relative to the application context path):
 * <pre>
 * GET  /accounts               account list with search/filter
 * GET  /accounts/create        creation form
 * POST /accounts/create        create submit
 * GET  /accounts/{id}          account detail
 * GET  /accounts/{id}/edit     edit form
 * POST /accounts/{id}/edit     update submit
 * POST /accounts/{id}/status   change account status
 * </pre>
 *
 * <p>This feature is intentionally independent from Authentication; no
 * session or authorization logic exists here.
 */
@WebServlet("/accounts/*")
public class AccountServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final AccountService accountService = new AccountService();

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
        String search = ParamUtil.getString(request, "search", "");
        String type = ParamUtil.getString(request, "type", "");
        String role = ParamUtil.getString(request, "role", "");
        String status = ParamUtil.getString(request, "status", "");
        try {
            List<Account> accounts = accountService.searchAccounts(search, type, role, status);
            request.setAttribute("accounts", accounts);
            request.setAttribute("accountTypes", accountService.getAccountTypes());
            request.setAttribute("accountRoles", accountService.getAccountRoles());
        } catch (AccountServiceException e) {
            request.setAttribute("generalError", e.getMessage());
            request.setAttribute("accounts", List.of());
            request.setAttribute("accountTypes", List.of());
            request.setAttribute("accountRoles", List.of());
        }
        request.setAttribute("search", search);
        request.setAttribute("filterType", type);
        request.setAttribute("filterRole", role);
        request.setAttribute("filterStatus", status);
        request.setAttribute("accountStatuses", AccountService.ACCOUNT_STATUSES);
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
            request.setAttribute("account", account);
            safePrepareFormData(request);
            forward(request, response, "account/create.jsp");
            return;
        }

        if (result.isSuccess()) {
            response.sendRedirect(contextPath(request) + "/accounts/" + result.getAccountId() + "?msg=created");
            return;
        }
        request.setAttribute("account", account);
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
            request.setAttribute("account", account);
            safePrepareFormData(request);
            forward(request, response, "account/edit.jsp");
            return;
        }

        if (result.isSuccess()) {
            response.sendRedirect(contextPath(request) + "/accounts/" + id + "?msg=updated");
            return;
        }
        request.setAttribute("account", account);
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
        request.setAttribute("accountStatuses", AccountService.ACCOUNT_STATUSES);
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
        request.setAttribute("accountStatuses", AccountService.ACCOUNT_STATUSES);
    }

    /** Domain data for POST error re-renders; degrades gracefully on DB failure. */
    private void safePrepareFormData(HttpServletRequest request) {
        try {
            prepareFormData(request);
        } catch (AccountServiceException e) {
            request.setAttribute("generalError", e.getMessage());
        }
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
}