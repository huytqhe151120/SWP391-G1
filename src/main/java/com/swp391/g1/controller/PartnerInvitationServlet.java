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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@WebServlet(name = "PartnerInvitationServlet", urlPatterns = {"/partner-invitations"})
public class PartnerInvitationServlet extends HttpServlet {

    private static final Set<Integer> PAGE_SIZES = Set.of(5, 10, 20, 25);
    private static final List<String> INVITATION_STATUSES = List.of("PENDING", "APPROVED", "REJECTED", "CANCELLED");

    private final IActivityCollaborationInvitationService invitationService =
            new ActivityCollaborationInvitationServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser user = AuthContext.getCurrentUser(request);
        List<ActivityCollaborationInvitationDTO> source = invitationService.getInvitationsForUser(user);

        String keyword = trim(request.getParameter("keyword"));
        String activityType = trim(request.getParameter("activityType"));
        String activityStatus = allowed(request.getParameter("activityStatus"),
                com.swp391.g1.model.Enum.ActivityStatus.values());
        String activityApproval = allowed(request.getParameter("activityApprovalStatus"),
                com.swp391.g1.model.Enum.ApprovalStatus.values());
        String invitationStatus = INVITATION_STATUSES.contains(trim(request.getParameter("invitationStatus")))
                ? trim(request.getParameter("invitationStatus")) : "";
        String normalizedKeyword = normalize(keyword);

        Set<String> activityTypes = new TreeSet<>();
        List<ActivityCollaborationInvitationDTO> all = new ArrayList<>();
        for (ActivityCollaborationInvitationDTO inv : source) {
            if (inv.getActivityTypeName() != null) {
                activityTypes.add(inv.getActivityTypeName());
            }
            if (!normalizedKeyword.isEmpty() && !normalize(inv.getActivityCode()).contains(normalizedKeyword)
                    && !normalize(inv.getActivityName()).contains(normalizedKeyword)) {
                continue;
            }
            if (!activityType.isEmpty() && !activityType.equals(inv.getActivityTypeName())) {
                continue;
            }
            if (!activityStatus.isEmpty() && !activityStatus.equals(inv.getActivityStatus())) {
                continue;
            }
            if (!activityApproval.isEmpty() && !activityApproval.equals(inv.getActivityApprovalStatus())) {
                continue;
            }
            if (!invitationStatus.isEmpty() && !invitationStatus.equals(inv.getApprovalStatus())) {
                continue;
            }
            all.add(inv);
        }

        int pageSize = parseInt(request.getParameter("pageSize"), 5);
        if (!PAGE_SIZES.contains(pageSize)) {
            pageSize = 5;
        }
        int total = all.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        int currentPage = Math.max(1, Math.min(parseInt(request.getParameter("page"), 1), totalPages));
        int from = Math.min((currentPage - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);

        StringBuilder query = new StringBuilder();
        appendParam(query, "keyword", keyword);
        appendParam(query, "activityType", activityType);
        appendParam(query, "activityStatus", activityStatus);
        appendParam(query, "activityApprovalStatus", activityApproval);
        appendParam(query, "invitationStatus", invitationStatus);

        request.setAttribute("keyword", keyword);
        request.setAttribute("selectedActivityType", activityType);
        request.setAttribute("selectedActivityStatus", activityStatus);
        request.setAttribute("selectedActivityApprovalStatus", activityApproval);
        request.setAttribute("selectedInvitationStatus", invitationStatus);
        request.setAttribute("filterQuery", query.toString());
        request.setAttribute("filtered", query.length() > 0);
        request.setAttribute("activeFilterCount",
                (keyword.isEmpty() ? 0 : 1) + (activityType.isEmpty() ? 0 : 1) + (activityStatus.isEmpty() ? 0 : 1)
                        + (activityApproval.isEmpty() ? 0 : 1) + (invitationStatus.isEmpty() ? 0 : 1));
        request.setAttribute("activityTypes", activityTypes);
        request.setAttribute("activityStatuses", com.swp391.g1.model.Enum.ActivityStatus.values());
        request.setAttribute("approvalStatuses", com.swp391.g1.model.Enum.ApprovalStatus.values());
        request.setAttribute("invitationStatuses", INVITATION_STATUSES);
        request.setAttribute("invitations", new ArrayList<>(all.subList(from, to)));
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("currentPage", currentPage);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", total);
        request.setAttribute("firstNumber", total == 0 ? 0 : from + 1);
        request.setAttribute("lastNumber", to);
        request.getRequestDispatcher("/WEB-INF/views/partner-invitation/list.jsp").forward(request, response);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private String allowed(String value, java.lang.Enum<?>[] options) {
        String candidate = trim(value);
        for (java.lang.Enum<?> option : options) {
            if (option.name().equals(candidate)) {
                return candidate;
            }
        }
        return "";
    }

    private void appendParam(StringBuilder query, String name, String value) {
        if (!value.isEmpty()) {
            query.append('&').append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}