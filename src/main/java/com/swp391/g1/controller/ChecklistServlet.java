package com.swp391.g1.controller;

import com.swp391.g1.dao.DataAccessException;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ChecklistAssignmentIdsDTO;
import com.swp391.g1.dto.response.ChecklistTaskDetailResponseDTO;
import com.swp391.g1.dto.response.ChecklistTaskResponseDTO;
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
        if (!authorize(user, response)) {
            return;
        }

        try {
            String action = request.getParameter("action");
            if ("create".equals(action)) {
                try {
                    request.setAttribute("formActivityId",
                            parseOptionalActivityId(request.getParameter("activityId")));
                } catch (IllegalArgumentException e) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Hoạt động không hợp lệ.");
                    return;
                }
                showTaskForm(request, response, null);
            } else if ("edit".equals(action) || "detail".equals(action)) {
                int taskId = parseTaskId(request.getParameter("id"));
                ChecklistTaskDetailResponseDTO detail = checklistTaskService.getTaskDetail(taskId);
                if (detail == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy công việc checklist.");
                    return;
                }
                if ("edit".equals(action)) {
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
                    showTaskForm(request, response, taskId);
                } else {
                    request.setAttribute("taskDetail", detail);
                    request.setAttribute("activeNav", "checklist");
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
                request.setAttribute("activities", activityService.getAllActivities());
                request.setAttribute("selectedActivityId", activityId);
                List<ChecklistTaskResponseDTO> checklistTasks = activityId == 0
                        ? List.of() : checklistTaskService.getTasksForActivity(activityId);
                request.setAttribute("quickAssignTaskOptions", checklistTasks);
                request.setAttribute("quickAssignStaffOptions", checklistTaskService.getActiveStaff());
                request.setAttribute("quickAssignPartnerOptions",
                        checklistTaskService.getActivePartnerStaff());
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
        if (!authorize(AuthContext.getCurrentUser(request), response)) {
            return;
        }
        String action = request.getParameter("action");
        if ("quickAssign".equals(action)) {
            quickAssign(request, response);
            return;
        }
        if ("delete".equals(action)) {
            deleteTask(request, response);
            return;
        }
        if ("updateStatus".equals(action)) {
            updateTaskStatus(request, response);
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
            staffIds = parseIds(request.getParameterValues("staffIds"));
            partnerStaffIds = parseIds(request.getParameterValues("partnerStaffIds"));
            prerequisiteTaskIds = parseIds(request.getParameterValues("prerequisiteTaskIds"));
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", "Danh sách nhân sự hoặc công việc tiên quyết không hợp lệ.");
            preserveForm(request, activityId, phase, priority, trim(request.getParameter("code")),
                    trim(request.getParameter("name")), trim(request.getParameter("description")),
                    trim(request.getParameter("deadline")), status, Set.of(), Set.of(), Set.of());
            showTaskForm(request, response, taskId == 0 ? null : taskId);
            return;
        }

        LocalDateTime deadline = null;
        String error = validate(activityId, phase, priority, code, name, deadlineValue, status,
                staffIds, partnerStaffIds, prerequisiteTaskIds);
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
            showTaskForm(request, response, taskId == 0 ? null : taskId);
            return;
        }

        try {
            if ("create".equals(action)) {
                int createdTaskId = checklistTaskService.createTask(activityId, phase, priority, code, name, description,
                        deadline, status, new ArrayList<>(staffIds), new ArrayList<>(partnerStaffIds),
                        new ArrayList<>(prerequisiteTaskIds));
                notificationService.notifyChecklistTaskAssignment(createdTaskId);
                response.sendRedirect(request.getContextPath()
                        + "/checklist?created=1&activityId=" + activityId);
            } else {
                int updated = checklistTaskService.updateTask(taskId, activityId, phase, priority,
                        code, name, description, deadline, status, new ArrayList<>(staffIds),
                        new ArrayList<>(partnerStaffIds), new ArrayList<>(prerequisiteTaskIds));
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
            showTaskForm(request, response, taskId == 0 ? null : taskId);
        } catch (DataAccessException e) {
            request.setAttribute("errorMessage", e.isDuplicateKey()
                    ? "Mã công việc đã tồn tại. Vui lòng nhập mã khác."
                    : "Không thể tạo công việc lúc này. Vui lòng kiểm tra dữ liệu và thử lại.");
            preserveForm(request, activityId, phase, priority, code, name, description, deadlineValue,
                    status, staffIds, partnerStaffIds, prerequisiteTaskIds);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showTaskForm(request, response, taskId == 0 ? null : taskId);
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

    private void showTaskForm(HttpServletRequest request, HttpServletResponse response, Integer taskId)
            throws ServletException, IOException {
        request.setAttribute("activeNav", "checklist");
        request.setAttribute("isEdit", taskId != null);
        request.setAttribute("activityOptions", activityService.getAllActivities());
        request.setAttribute("taskOptions", checklistTaskService.getAllTasks());
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
        request.setAttribute("staffOptions", checklistTaskService.getActiveStaff(assignmentTaskId));
        request.setAttribute("partnerStaffOptions",
                checklistTaskService.getActivePartnerStaff(assignmentTaskId));
        request.getRequestDispatcher("/WEB-INF/views/checklist/form.jsp").forward(request, response);
    }

    private void deleteTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int taskId;
        try {
            taskId = parseTaskId(request.getParameter("id"));
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã công việc không hợp lệ.");
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

    private void updateTaskStatus(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int taskId;
        try {
            taskId = parseTaskId(request.getParameter("id"));
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã công việc không hợp lệ.");
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

    private void quickAssign(HttpServletRequest request, HttpServletResponse response)
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

        try {
            ChecklistTaskDetailResponseDTO detail = checklistTaskService.getTaskDetail(taskId);
            if (detail == null || detail.getTask().getActivityId() != activityId) {
                response.sendRedirect(request.getContextPath()
                        + "/checklist?quickAssignError=1" + activityQuery(Integer.toString(activityId)));
                return;
            }
            boolean assigned = checklistTaskService.addAssignees(taskId,
                    new ArrayList<>(staffIds), new ArrayList<>(partnerStaffIds));
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
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền quản lý checklist.");
        return false;
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
