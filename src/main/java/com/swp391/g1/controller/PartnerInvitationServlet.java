package com.swp391.g1.controller;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityCollaborationInvitationDTO;
import com.swp391.g1.service.IActivityCollaborationInvitationService;
import com.swp391.g1.service.impl.ActivityCollaborationInvitationServiceImpl;
import com.swp391.g1.util.AuthContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@WebServlet(name = "PartnerInvitationServlet", urlPatterns = {"/partner-invitations"})
public class PartnerInvitationServlet extends HttpServlet {

    private static final Set<Integer> PAGE_SIZES = Set.of(5, 10, 20, 25);

    private final IActivityCollaborationInvitationService invitationService =
            new ActivityCollaborationInvitationServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser user = AuthContext.getCurrentUser(request);
        List<ActivityCollaborationInvitationDTO> all = invitationService.getInvitationsForUser(user);

        int pageSize = parseInt(request.getParameter("pageSize"), 5);
        if (!PAGE_SIZES.contains(pageSize)) {
            pageSize = 5;
        }
        int total = all.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        int currentPage = Math.max(1, Math.min(parseInt(request.getParameter("page"), 1), totalPages));
        int from = Math.min((currentPage - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);

        request.setAttribute("invitations", new ArrayList<>(all.subList(from, to)));
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("currentPage", currentPage);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", total);
        request.setAttribute("firstNumber", total == 0 ? 0 : from + 1);
        request.setAttribute("lastNumber", to);
        request.getRequestDispatcher("/WEB-INF/views/partner-invitation/list.jsp").forward(request, response);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}