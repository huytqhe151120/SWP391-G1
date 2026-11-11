package com.swp391.g1.controller;

import com.swp391.g1.dao.DataAccessException;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ChecklistAssignmentIdsDTO;
import com.swp391.g1.dto.response.ChecklistTaskDetailResponseDTO;
import com.swp391.g1.dto.response.ChecklistTaskResponseDTO;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.service.IExtracurricularActivityService;
import com.swp391.g1.service.INotificationService;
import com.swp391.g1.model.ChecklistTaskStatus;
import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.service.IChecklistTaskService;
import com.swp391.g1.service.impl.ChecklistTaskServiceImpl;
import com.swp391.g1.service.impl.ExtracurricularActivityServiceImpl;
import com.swp391.g1.service.impl.NotificationServiceImpl;
import com.swp391.g1.util.AuthContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@WebServlet("/checklist")
public class ChecklistServlet extends HttpServlet {

    private static final DateTimeFormatter DATETIME_INPUT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final IChecklistTaskService checklistTaskService;
    private final IExtracurricularActivityService activityService;
    private final INotificationService notificationService;

    public ChecklistServlet() {
        this(new ChecklistTaskServiceImpl(), new ExtracurricularActivityServiceImpl(),
                new NotificationServiceImpl());
    }

    ChecklistServlet(IChecklistTaskService checklistTaskService,
                     IExtracurricularActivityService activityService) {
        this(checklistTaskService, activityService, new NotificationServiceImpl());
    }

    ChecklistServlet(IChecklistTaskService checklistTaskService,
                     IExtracurricularActivityService activityService,
                     INotificationService notificationService) {
        this.checklistTaskService = checklistTaskService;
        this.activityService = activityService;
        this.notificationService = notificationService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        CurrentUser user = AuthContext.getCurrentUser(request);
        String action = request.getParameter("action");
        boolean checklistManager = AccessPolicy.canViewChecklist(user);
        boolean checklistAdmin = AccessPolicy.hasType(user, "ADMIN");
        if (!checklistManager && !"detail".equals(action)) {
            authorize(user, response);
            return;
        }

        try {
            if ("create".equals(action)) {
                try {
                    int activityId = parseOptionalActivityId(request.getParameter("activityId"));
                    if (activityId > 0 && !canCreateChecklistInActivity(activityId, user)) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                                "Bạn không được tạo công việc trong hoạt động này.");
                        return;
                    }
                    request.setAttribute("formActivityId", activityId);
                } catch (IllegalArgumentException e) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Hoạt động không hợp lệ.");
                    return;
                }
                showTaskForm(request, response, null, user);
            } else if ("edit".equals(action) || "detail".equals(action)) {
                int taskId = parseTaskId(request.getParameter("id"));
                ChecklistTaskDetailResponseDTO detail = checklistTaskService.getTaskDetail(taskId);
                if (detail == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy công việc checklist.");
                    return;
                }
                if (!hasTaskAccess(user, taskId)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Bạn không được phân công công việc checklist này.");
                    return;
                }
                if ("edit".equals(action)) {
                    if (!checklistAdmin) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                                "Chỉ quản trị viên được chỉnh sửa công việc checklist.");
                        return;
                    }
                    ChecklistAssignmentIdsDTO assignees = checklistTaskService.getAssigneeIds(taskId);
                    request.setAttribute("formCode", detail.getTask().getCode());
                    request.setAttribute("formName", detail.getTask().getName());
                    request.setAttribute("formDescription", detail.getTask().getDescription());
                    request.setAttribute("formStatus", detail.getTask().getStatusValue());
                    request.setAttribute("formActivityId", detail.getTask().getActivityId());
                    request.setAttribute("formPhase", detail.getTask().getPhase());
                    request.setAttribute("formPriority", detail.getTask().getPriority());
                    request.setAttribute("formDeadline", detail.getTask().getDeadline() == null
                            ? "" : detail.getTask().getDeadline().format(DATETIME_INPUT_FORMAT));
                    request.setAttribute("selectedStaffIds", new LinkedHashSet<>(assignees.getStaffIds()));
                    request.setAttribute("selectedPartnerStaffIds",
                            new LinkedHashSet<>(assignees.getPartnerStaffIds()));
                    request.setAttribute("selectedPrerequisiteTaskIds",
                            new LinkedHashSet<>(assignees.getPrerequisiteTaskIds()));
                    request.setAttribute("taskId", taskId);
                    showTaskForm(request, response, taskId, user);
                } else {
                    request.setAttribute("taskDetail", detail);
                    request.setAttribute("visiblePrerequisiteTasks", detail.getPrerequisiteTasks().stream()
                            .filter(task -> hasTaskAccess(user, task.getId()))
                            .toList());
                    request.setAttribute("visibleNextTasks", detail.getNextTasks().stream()
                            .filter(task -> hasTaskAccess(user, task.getId()))
                            .toList());
                    request.setAttribute("activeNav", "checklist");
                    request.setAttribute("canManageChecklist", checklistAdmin);
                    request.setAttribute("isChecklistAdmin", checklistAdmin);
                    request.setAttribute("isPersonalChecklist", !checklistAdmin);
                    request.setAttribute("checklistPersonalName",
                            user == null ? "" : user.getDisplayName());
                    request.getRequestDispatcher("/WEB-INF/views/checklist/detail.jsp").forward(request, response);
                }
            } else {
                int activityId;
                try {
                    activityId = parseOptionalActivityId(request.getParameter("activityId"));
                } catch (IllegalArgumentException e) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Hoạt động không hợp lệ.");
                    return;
                }
                request.setAttribute("selectedActivityId", activityId);
                request.setAttribute("isChecklistAdmin", checklistAdmin);
                request.setAttribute("isPersonalChecklist", !checklistAdmin);
                request.setAttribute("checklistPersonalName",
                        user == null ? "" : user.getDisplayName());
                if (AccessPolicy.hasType(user, "ADMIN")) {
                    request.setAttribute("checklistScopeLabel", "Checklist hoạt động");
                } else if (AccessPolicy.hasType(user, "STAFF")) {
                    request.setAttribute("checklistScopeLabel", "Checklist của tôi");
                } else if (AccessPolicy.hasType(user, "PARTNER_STAFF")) {
                    request.setAttribute("checklistScopeLabel", "Checklist của tôi");
                }
                List<ChecklistTaskResponseDTO> checklistTasks;
                if (AccessPolicy.hasType(user, "ADMIN")) {
                    checklistTasks = activityId == 0 ? List.of() : checklistTaskService.getTasksForActivity(activityId);
                } else if (AccessPolicy.hasType(user, "STAFF")) {
                    checklistTasks = checklistTaskService.getTasksForStaffAccount(user.getId());
                    if (activityId > 0) {
                        checklistTasks = checklistTasks.stream()
                                .filter(task -> task.getActivityId() == activityId)
                                .toList();
                    }
                } else if (AccessPolicy.hasType(user, "PARTNER_STAFF")) {
                    checklistTasks = checklistTaskService.getTasksForPartnerStaffAccount(user.getId());
                    if (activityId > 0) {
                        checklistTasks = checklistTasks.stream()
                                .filter(task -> task.getActivityId() == activityId)
                                .toList();
                    }
                } else {
                    checklistTasks = List.of();
                }
                request.setAttribute("activities", getChecklistActivities(user));
                if (checklistAdmin) {
                    request.setAttribute("quickAssignTaskOptions", checklistTasks);
                    request.setAttribute("quickAssignStaffOptions", checklistTaskService.getActiveStaff());
                    request.setAttribute("quickAssignPartnerOptions",
                            checklistTaskService.getActivePartnerStaff());
                } else {
                    request.setAttribute("quickAssignTaskOptions", List.of());
                    request.setAttribute("quickAssignStaffOptions", List.of());
                    request.setAttribute("quickAssignPartnerOptions", List.of());
                }
                long completedTaskCount = checklistTasks.stream()
                        .filter(task -> ChecklistTaskStatus.COMPLETED.equals(task.getStatusValue()))
                        .count();
                request.setAttribute("checklistTasks", checklistTasks);
                request.setAttribute("completedTaskCount", completedTaskCount);
                request.setAttribute("totalTaskCount", checklistTasks.size());
                request.setAttribute("completionPercent", checklistTasks.isEmpty()
                        ? 0 : (int) Math.round(completedTaskCount * 100.0 / checklistTasks.size()));
                request.setAttribute("activeNav", "checklist");
                request.getRequestDispatcher("/WEB-INF/views/checklist/list.jsp").forward(request, response);
            }
        } catch (DataAccessException e) {
            request.setAttribute("errorMessage", "Không thể tải checklist. Vui lòng thử lại sau.");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.getRequestDispatcher("/WEB-INF/views/error/error.jsp").forward(request, response);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã công việc không hợp lệ.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        CurrentUser currentUser = AuthContext.getCurrentUser(request);
        if (!authorize(currentUser, response)) {
            return;
        }
        String action = request.getParameter("action");
        if ("quickAssign".equals(action)) {
            if (!requireChecklistAdmin(currentUser, response)) {
                return;
            }
            quickAssign(request, response, currentUser);
            return;
        }
        if ("delete".equals(action)) {
            if (!requireChecklistAdmin(currentUser, response)) {
                return;
            }
            deleteTask(request, response, currentUser);
            return;
        }
        if ("updateStatus".equals(action)) {
            updateTaskStatus(request, response, currentUser);
            return;
        }
        if ("updateStatuses".equals(action)) {
            if (!requireChecklistAdmin(currentUser, response)) {
                return;
            }
            updateTaskStatuses(request, response, currentUser);
            return;
        }
        if (!"create".equals(action) && !"update".equals(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thao tác checklist không hợp lệ.");
            return;
        }

        int taskId = 0;
        int activityId;
        if ("update".equals(action)) {
            try {
                taskId = parseTaskId(request.getParameter("id"));
            } catch (IllegalArgumentException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã công việc không hợp lệ.");
                return;
            }
            if (!hasTaskAccess(currentUser, taskId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bạn không được chỉnh sửa công việc checklist này.");
                return;
            }
            if (!AccessPolicy.hasType(currentUser, "ADMIN")) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Chỉ quản trị viên được chỉnh sửa công việc checklist.");
                return;
            }
        }
        try {
            activityId = parseActivityId(request.getParameter("activityId"));
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Hoạt động không hợp lệ.");
            return;
        }

        String phase = trim(request.getParameter("phase"));
        String priority = trim(request.getParameter("priority"));
        String code = trim(request.getParameter("code"));
        String name = trim(request.getParameter("name"));
        String description = trim(request.getParameter("description"));
        String deadlineValue = trim(request.getParameter("deadline"));
        String status = trim(request.getParameter("status"));
        Set<Integer> staffIds;
        Set<Integer> partnerStaffIds;
        Set<Integer> prerequisiteTaskIds;

        try {
            if (AccessPolicy.hasType(currentUser, "ADMIN")) {
                staffIds = parseIds(request.getParameterValues("staffIds"));
                partnerStaffIds = parseIds(request.getParameterValues("partnerStaffIds"));
            } else if (AccessPolicy.hasType(currentUser, "STAFF")) {
                int staffId = checklistTaskService.getActiveStaffIdForAccount(currentUser.getId());
                if (staffId <= 0) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Tài khoản chưa được liên kết với hồ sơ nhân viên đang hoạt động.");
                    return;
                }
                staffIds = new LinkedHashSet<>(Set.of(staffId));
                partnerStaffIds = new LinkedHashSet<>();
            } else {
                int partnerStaffId = checklistTaskService.getActivePartnerStaffIdForAccount(currentUser.getId());
                if (partnerStaffId <= 0) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Tài khoản chưa được liên kết với hồ sơ nhân viên đối tác đang hoạt động.");
                    return;
                }
                staffIds = new LinkedHashSet<>();
                partnerStaffIds = new LinkedHashSet<>(Set.of(partnerStaffId));
            }
            prerequisiteTaskIds = parseIds(request.getParameterValues("prerequisiteTaskIds"));
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", "Danh sách nhân sự hoặc công việc tiên quyết không hợp lệ.");
            preserveForm(request, activityId, phase, priority, trim(request.getParameter("code")),
                    trim(request.getParameter("name")), trim(request.getParameter("description")),
                    trim(request.getParameter("deadline")), status, Set.of(), Set.of(), Set.of());
            showTaskForm(request, response, taskId == 0 ? null : taskId, currentUser);
            return;
        }

        LocalDateTime deadline = null;
        if ("create".equals(action) && !canCreateChecklistInActivity(activityId, currentUser)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn chỉ được tạo công việc trong hoạt động mình được phép tham gia.");
            return;
        }
        if (!AccessPolicy.hasType(currentUser, "ADMIN")) {
            status = ChecklistTaskStatus.NOT_COMPLETED;
        }
        String error = validate(activityId, phase, priority, code, name, deadlineValue, status,
                staffIds, partnerStaffIds, prerequisiteTaskIds);
        if (error == null && !AccessPolicy.hasType(currentUser, "ADMIN")
                && !hasPrerequisiteScope(currentUser, activityId, prerequisiteTaskIds)) {
            error = "Chỉ có thể chọn công việc tiên quyết được giao cho mình trong cùng hoạt động.";
        }
        if (error == null) {
            try {
                deadline = LocalDateTime.parse(deadlineValue);
            } catch (DateTimeParseException e) {
                error = "Hạn hoàn thành không hợp lệ.";
            }
        }
        if (error != null) {
            request.setAttribute("errorMessage", error);
            request.setAttribute("formCode", code);
            request.setAttribute("formName", name);
            request.setAttribute("formDescription", description);
            request.setAttribute("formActivityId", activityId);
            request.setAttribute("formPhase", phase);
            request.setAttribute("formPriority", priority);
            request.setAttribute("formDeadline", deadlineValue);
            request.setAttribute("formStatus", status);
            request.setAttribute("selectedStaffIds", staffIds);
            request.setAttribute("selectedPartnerStaffIds", partnerStaffIds);
            request.setAttribute("selectedPrerequisiteTaskIds", prerequisiteTaskIds);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showTaskForm(request, response, taskId == 0 ? null : taskId, currentUser);
            return;
        }

        try {
            if ("create".equals(action)) {
                int createdTaskId = checklistTaskService.createTask(activityId, phase, priority, code, name, description,
                        deadline, status, new ArrayList<>(staffIds), new ArrayList<>(partnerStaffIds),
                        new ArrayList<>(prerequisiteTaskIds), currentUser.getId());
                notificationService.notifyChecklistTaskAssignment(createdTaskId);
                response.sendRedirect(request.getContextPath()
                        + "/checklist?created=1&activityId=" + activityId);
            } else {
                int updated = checklistTaskService.updateTask(taskId, activityId, phase, priority,
                        code, name, description, deadline, status, new ArrayList<>(staffIds),
                        new ArrayList<>(partnerStaffIds), new ArrayList<>(prerequisiteTaskIds),
                        currentUser.getId());
                if (updated > 0) {
                    notificationService.notifyChecklistTaskAssignment(taskId);
                }
                response.sendRedirect(request.getContextPath()
                        + (updated == 0 ? "/checklist?notFound=1" : "/checklist?updated=1")
                        + "&activityId=" + activityId);
            }
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", "PREREQUISITES_INCOMPLETE".equals(e.getMessage())
                    ? "Chỉ có thể bắt đầu công việc sau khi tất cả công việc tiên quyết hoàn thành."
                    : "Nhân sự hoặc công việc tiên quyết không hợp lệ. Hãy chọn lại các mục cùng hoạt động.");
            preserveForm(request, activityId, phase, priority, code, name, description, deadlineValue,
                    status, staffIds, partnerStaffIds, prerequisiteTaskIds);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showTaskForm(request, response, taskId == 0 ? null : taskId, currentUser);
        } catch (DataAccessException e) {
            request.setAttribute("errorMessage", e.isDuplicateKey()
                    ? "Mã công việc đã tồn tại. Vui lòng nhập mã khác."
                    : "Không thể tạo công việc lúc này. Vui lòng kiểm tra dữ liệu và thử lại.");
            preserveForm(request, activityId, phase, priority, code, name, description, deadlineValue,
                    status, staffIds, partnerStaffIds, prerequisiteTaskIds);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showTaskForm(request, response, taskId == 0 ? null : taskId, currentUser);
        }
    }

    private void preserveForm(HttpServletRequest request, int activityId, String phase, String priority,
                              String code, String name, String description, String deadline, String status,
                              Set<Integer> staffIds, Set<Integer> partnerStaffIds,
                              Set<Integer> prerequisiteTaskIds) {
        request.setAttribute("formActivityId", activityId);
        request.setAttribute("formPhase", phase);
        request.setAttribute("formPriority", priority);
        request.setAttribute("formCode", code);
        request.setAttribute("formName", name);
        request.setAttribute("formDescription", description);
        request.setAttribute("formDeadline", deadline);
        request.setAttribute("formStatus", status);
        request.setAttribute("selectedStaffIds", staffIds);
        request.setAttribute("selectedPartnerStaffIds", partnerStaffIds);
        request.setAttribute("selectedPrerequisiteTaskIds", prerequisiteTaskIds);
    }

    private void showTaskForm(HttpServletRequest request, HttpServletResponse response, Integer taskId,
                              CurrentUser user)
            throws ServletException, IOException {
        request.setAttribute("activeNav", "checklist");
        request.setAttribute("isEdit", taskId != null);
        request.setAttribute("activityOptions", getChecklistActivities(user));
        request.setAttribute("isChecklistAdmin", AccessPolicy.hasType(user, "ADMIN"));
        List<ChecklistTaskResponseDTO> taskOptions;
        if (AccessPolicy.hasType(user, "ADMIN")) {
            taskOptions = checklistTaskService.getAllTasks();
        } else if (AccessPolicy.hasType(user, "STAFF")) {
            taskOptions = checklistTaskService.getTasksForStaffAccount(user.getId());
        } else if (AccessPolicy.hasType(user, "PARTNER_STAFF")) {
            taskOptions = checklistTaskService.getTasksForPartnerStaffAccount(user.getId());
        } else {
            taskOptions = List.of();
        }
        request.setAttribute("taskOptions", taskOptions);
        if (taskId != null) {
            request.setAttribute("taskId", taskId);
        }
        if (request.getAttribute("formStatus") == null) {
            request.setAttribute("formStatus", ChecklistTaskStatus.NOT_COMPLETED);
        }
        if (request.getAttribute("formPriority") == null) {
            request.setAttribute("formPriority", "MEDIUM");
        }
        if (request.getAttribute("selectedStaffIds") == null) {
            request.setAttribute("selectedStaffIds", Set.of());
        }
        if (request.getAttribute("selectedPartnerStaffIds") == null) {
            request.setAttribute("selectedPartnerStaffIds", Set.of());
        }
        if (request.getAttribute("selectedPrerequisiteTaskIds") == null) {
            request.setAttribute("selectedPrerequisiteTaskIds", Set.of());
        }
        int assignmentTaskId = taskId == null ? 0 : taskId;
        if (AccessPolicy.hasType(user, "ADMIN")) {
            request.setAttribute("staffOptions", checklistTaskService.getActiveStaff(assignmentTaskId));
            request.setAttribute("partnerStaffOptions",
                    checklistTaskService.getActivePartnerStaff(assignmentTaskId));
        } else {
            request.setAttribute("staffOptions", List.of());
            request.setAttribute("partnerStaffOptions", List.of());
        }
        request.getRequestDispatcher("/WEB-INF/views/checklist/form.jsp").forward(request, response);
    }

    private void deleteTask(HttpServletRequest request, HttpServletResponse response, CurrentUser user)
            throws IOException {
        int taskId;
        try {
            taskId = parseTaskId(request.getParameter("id"));
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã công việc không hợp lệ.");
            return;
        }
        if (!hasTaskAccess(user, taskId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không được xóa công việc checklist này.");
            return;
        }
        try {
            boolean deleted = checklistTaskService.deleteTask(taskId);
            response.sendRedirect(request.getContextPath()
                    + (deleted ? "/checklist?deleted=1" : "/checklist?notFound=1")
                    + activityQuery(request.getParameter("activityId")));
        } catch (DataAccessException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không thể xóa công việc checklist.");
        }
    }

    private void updateTaskStatus(HttpServletRequest request, HttpServletResponse response, CurrentUser user)
            throws IOException {
        int taskId;
        try {
            taskId = parseTaskId(request.getParameter("id"));
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã công việc không hợp lệ.");
            return;
        }
        if (!hasTaskAccess(user, taskId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không được cập nhật công việc checklist này.");
            return;
        }
        String status = trim(request.getParameter("status"));
        if (!ChecklistTaskStatus.isValid(status)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Trạng thái công việc không hợp lệ.");
            return;
        }
        try {
            boolean updated = checklistTaskService.updateTaskStatus(taskId, status);
            response.sendRedirect(request.getContextPath()
                    + (updated ? "/checklist?statusUpdated=1" : "/checklist?notFound=1")
                    + activityQuery(request.getParameter("activityId")));
        } catch (IllegalStateException e) {
            response.sendRedirect(request.getContextPath()
                    + "/checklist?statusBlocked=1"
                    + activityQuery(request.getParameter("activityId")));
        } catch (DataAccessException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không thể cập nhật trạng thái công việc.");
        }
    }

    private void updateTaskStatuses(HttpServletRequest request, HttpServletResponse response, CurrentUser user)
            throws IOException {
        int activityId;
        Set<Integer> taskIds;
        String status = trim(request.getParameter("status"));
        try {
            activityId = parseActivityId(request.getParameter("activityId"));
            taskIds = parseIds(request.getParameterValues("taskIds"));
            if (taskIds.isEmpty() || !ChecklistTaskStatus.isValid(status)) {
                throw new IllegalArgumentException("Invalid bulk checklist status request.");
            }
        } catch (IllegalArgumentException e) {
            response.sendRedirect(request.getContextPath()
                    + "/checklist?bulkStatusError=1" + activityQuery(request.getParameter("activityId")));
            return;
        }

        if (taskIds.stream().anyMatch(taskId -> !hasTaskAccess(user, taskId))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn chỉ được cập nhật công việc checklist được phân công cho mình.");
            return;
        }

        try {
            int updated = checklistTaskService.updateTaskStatuses(
                    activityId, new ArrayList<>(taskIds), status);
            response.sendRedirect(request.getContextPath()
                    + "/checklist?bulkStatusUpdated=" + updated
                    + activityQuery(Integer.toString(activityId)));
        } catch (IllegalArgumentException e) {
            String result = "PREREQUISITES_INCOMPLETE".equals(e.getMessage())
                    ? "?bulkStatusBlocked=1" : "?bulkStatusError=1";
            response.sendRedirect(request.getContextPath()
                    + "/checklist" + result + activityQuery(Integer.toString(activityId)));
        } catch (DataAccessException | IllegalStateException e) {
            response.sendRedirect(request.getContextPath()
                    + "/checklist?bulkStatusError=1" + activityQuery(Integer.toString(activityId)));
        }
    }

    private void quickAssign(HttpServletRequest request, HttpServletResponse response, CurrentUser user)
            throws IOException {
        int activityId;
        int taskId;
        Set<Integer> staffIds;
        Set<Integer> partnerStaffIds;
        try {
            activityId = parseActivityId(request.getParameter("activityId"));
            taskId = parseTaskId(request.getParameter("taskId"));
            staffIds = parseIds(request.getParameterValues("staffIds"));
            partnerStaffIds = parseIds(request.getParameterValues("partnerStaffIds"));
        } catch (IllegalArgumentException e) {
            response.sendRedirect(request.getContextPath()
                    + "/checklist?quickAssignError=1" + activityQuery(request.getParameter("activityId")));
            return;
        }

        if (staffIds.isEmpty() && partnerStaffIds.isEmpty()) {
            response.sendRedirect(request.getContextPath()
                    + "/checklist?quickAssignError=1&reason=empty"
                    + activityQuery(Integer.toString(activityId)));
            return;
        }
        if (!hasTaskAccess(user, taskId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không được phân công thêm người cho công việc checklist này.");
            return;
        }

        try {
            ChecklistTaskDetailResponseDTO detail = checklistTaskService.getTaskDetail(taskId);
            if (detail == null || detail.getTask().getActivityId() != activityId) {
                response.sendRedirect(request.getContextPath()
                        + "/checklist?quickAssignError=1" + activityQuery(Integer.toString(activityId)));
                return;
            }
            boolean assigned = checklistTaskService.addAssignees(taskId,
                    new ArrayList<>(staffIds), new ArrayList<>(partnerStaffIds), user.getId());
            if (assigned) {
                notificationService.notifyChecklistTaskAssignment(taskId);
            }
            response.sendRedirect(request.getContextPath()
                    + (assigned ? "/checklist?assigned=1" : "/checklist?quickAssignError=1")
                    + activityQuery(Integer.toString(activityId)));
        } catch (IllegalArgumentException e) {
            response.sendRedirect(request.getContextPath()
                    + "/checklist?quickAssignError=1&reason=assignee"
                    + activityQuery(Integer.toString(activityId)));
        } catch (DataAccessException e) {
            response.sendRedirect(request.getContextPath()
                    + "/checklist?quickAssignError=1&reason=server"
                    + activityQuery(Integer.toString(activityId)));
        }
    }

    private int parseTaskId(String value) {
        try {
            int id = Integer.parseInt(value);
            if (id <= 0) {
                throw new IllegalArgumentException("Invalid task id.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid task id.", e);
        }
    }

    private int parseOptionalActivityId(String value) {
        return value == null || value.isBlank() ? 0 : parseActivityId(value);
    }

    private int parseActivityId(String value) {
        try {
            int id = Integer.parseInt(value);
            if (id <= 0) {
                throw new IllegalArgumentException("Invalid activity id.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid activity id.", e);
        }
    }

    private String activityQuery(String value) {
        String activityId = trim(value);
        return activityId.matches("[1-9][0-9]*") ? "&activityId=" + activityId : "";
    }

    private boolean authorize(CurrentUser user, HttpServletResponse response) throws IOException {
        if (AccessPolicy.canViewChecklist(user)) {
            return true;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền xem hoặc tạo checklist.");
        return false;
    }

    private boolean requireChecklistAdmin(CurrentUser user, HttpServletResponse response) throws IOException {
        if (AccessPolicy.hasType(user, "ADMIN")) {
            return true;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                "Thao tác này chỉ dành cho quản trị viên.");
        return false;
    }

    private boolean hasTaskAccess(CurrentUser user, int taskId) {
        if (AccessPolicy.hasType(user, "ADMIN")) {
            return true;
        }
        if (AccessPolicy.hasType(user, "STAFF")) {
            return checklistTaskService.isTaskAssignedToStaffAccount(taskId, user.getId());
        }
        if (AccessPolicy.hasType(user, "PARTNER_STAFF")) {
            return checklistTaskService.isTaskAssignedToPartnerStaffAccount(taskId, user.getId());
        }
        return false;
    }

    private boolean hasPrerequisiteScope(CurrentUser user, int activityId, Set<Integer> prerequisiteTaskIds) {
        if (prerequisiteTaskIds.isEmpty()) {
            return true;
        }
        List<ChecklistTaskResponseDTO> ownTasks;
        if (AccessPolicy.hasType(user, "STAFF")) {
            ownTasks = checklistTaskService.getTasksForStaffAccount(user.getId());
        } else if (AccessPolicy.hasType(user, "PARTNER_STAFF")) {
            ownTasks = checklistTaskService.getTasksForPartnerStaffAccount(user.getId());
        } else {
            return false;
        }
        Set<Integer> ownTaskIds = ownTasks.stream()
                .filter(task -> task.getActivityId() == activityId)
                .map(ChecklistTaskResponseDTO::getId)
                .collect(java.util.stream.Collectors.toSet());
        return ownTaskIds.containsAll(prerequisiteTaskIds);
    }

    private List<ExtracurricularActivityResponseDTO> getChecklistActivities(CurrentUser user) {
        if (AccessPolicy.hasType(user, "ADMIN")) {
            return activityService.getAllActivities();
        }

        List<ChecklistTaskResponseDTO> ownTasks = getTasksForUser(user);
        Set<Integer> ownActivityIds = ownTasks.stream()
                .map(ChecklistTaskResponseDTO::getActivityId)
                .collect(java.util.stream.Collectors.toSet());
        List<ExtracurricularActivityResponseDTO> activities =
                new ArrayList<>(activityService.getActivitiesVisibleTo(user));
        Set<Integer> includedIds = activities.stream()
                .map(ExtracurricularActivityResponseDTO::getId)
                .collect(java.util.stream.Collectors.toSet());
        for (ExtracurricularActivityResponseDTO activity : activityService.getAllActivities()) {
            if (ownActivityIds.contains(activity.getId()) && includedIds.add(activity.getId())) {
                activities.add(activity);
            }
        }
        return activities;
    }

    private boolean canCreateChecklistInActivity(int activityId, CurrentUser user) {
        if (activityService.canAccessActivity(activityId, user)) {
            return true;
        }
        if (!AccessPolicy.hasType(user, "STAFF") && !AccessPolicy.hasType(user, "PARTNER_STAFF")) {
            return false;
        }
        return getTasksForUser(user).stream()
                .anyMatch(task -> task.getActivityId() == activityId);
    }

    private List<ChecklistTaskResponseDTO> getTasksForUser(CurrentUser user) {
        if (AccessPolicy.hasType(user, "STAFF")) {
            return checklistTaskService.getTasksForStaffAccount(user.getId());
        }
        if (AccessPolicy.hasType(user, "PARTNER_STAFF")) {
            return checklistTaskService.getTasksForPartnerStaffAccount(user.getId());
        }
        return List.of();
    }

    private String validate(int activityId, String phase, String priority, String code, String name,
                            String deadline, String status,
                            Set<Integer> staffIds, Set<Integer> partnerStaffIds,
                            Set<Integer> prerequisiteTaskIds) {
        if (activityId <= 0 || activityService.getActivityById(activityId) == null) {
            return "Vui lòng chọn hoạt động hợp lệ.";
        }
        if (!Set.of("BEFORE_EVENT", "DURING_EVENT", "AFTER_EVENT").contains(phase)) {
            return "Vui lòng chọn giai đoạn công việc hợp lệ.";
        }
        if (!Set.of("LOW", "MEDIUM", "HIGH").contains(priority)) {
            return "Vui lòng chọn mức độ ưu tiên hợp lệ.";
        }
        if (code.isBlank() || code.length() > 50) {
            return "Mã công việc là bắt buộc và không được vượt quá 50 ký tự.";
        }
        if (name.isBlank() || name.length() > 255) {
            return "Tên công việc là bắt buộc và không được vượt quá 255 ký tự.";
        }
        if (deadline.isBlank()) {
            return "Vui lòng chọn hạn hoàn thành.";
        }
        if (!ChecklistTaskStatus.isValid(status)) {
            return "Vui lòng chọn trạng thái công việc hợp lệ.";
        }
        if (staffIds.isEmpty() && partnerStaffIds.isEmpty()) {
            return "Vui lòng phân công ít nhất một nhân viên hoặc nhân viên đối tác.";
        }
        return null;
    }

    private Set<Integer> parseIds(String[] values) {
        Set<Integer> ids = new LinkedHashSet<>();
        if (values == null) {
            return ids;
        }
        for (String value : values) {
            try {
                int id = Integer.parseInt(value);
                if (id <= 0) {
                    throw new IllegalArgumentException("Invalid assignee id.");
                }
                ids.add(id);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid assignee id.", e);
            }
        }
        return ids;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
