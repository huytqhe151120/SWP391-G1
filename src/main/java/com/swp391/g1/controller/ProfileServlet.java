package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dao.DataAccessException;
import com.swp391.g1.model.Account;
import com.swp391.g1.service.AccountServiceException;
import com.swp391.g1.service.IAccountService;
import com.swp391.g1.service.impl.AccountServiceImpl;
import com.swp391.g1.util.AuthContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {
    private final IAccountService service = new AccountServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser user = AuthContext.getCurrentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        response.setHeader("Cache-Control", "no-store");
        try {
            // Always use the session identity, never an account ID from the URL.
            Account account = service.getAccountById(user.getId());
            if (account == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("account", account);
            request.setAttribute("profiles", service.findProfiles(user.getId()));
            request.getRequestDispatcher("/WEB-INF/views/account/profile.jsp").forward(request, response);
        } catch (AccountServiceException | DataAccessException e) {
            getServletContext().log("Unable to load personal profile", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
