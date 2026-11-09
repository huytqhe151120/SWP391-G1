package com.swp391.g1.service;

import com.swp391.g1.dto.response.ChecklistTaskResponseDTO;
import com.swp391.g1.dto.response.ChecklistAssigneeResponseDTO;
import com.swp391.g1.dto.response.ChecklistTaskDetailResponseDTO;
import com.swp391.g1.dto.response.ChecklistAssignmentIdsDTO;
import java.time.LocalDateTime;
import java.util.List;

public interface IChecklistTaskService {
    List<ChecklistTaskResponseDTO> getAllTasks();

    List<ChecklistTaskResponseDTO> getTasksForActivity(int activityId);

    List<ChecklistAssigneeResponseDTO> getActiveStaff();

    List<ChecklistAssigneeResponseDTO> getActiveStaff(int taskId);

    List<ChecklistAssigneeResponseDTO> getActivePartnerStaff();

    List<ChecklistAssigneeResponseDTO> getActivePartnerStaff(int taskId);

    int createTask(int activityId, String phase, String priority, String code, String name, String description,
                   LocalDateTime deadline, String status, List<Integer> staffIds,
                   List<Integer> partnerStaffIds, List<Integer> prerequisiteTaskIds);

    boolean addAssignees(int taskId, List<Integer> staffIds, List<Integer> partnerStaffIds);

    ChecklistTaskDetailResponseDTO getTaskDetail(int taskId);

    boolean isTaskAssignedToAccount(int taskId, int accountId);

    ChecklistAssignmentIdsDTO getAssigneeIds(int taskId);

    int updateTask(int taskId, int activityId, String phase, String priority, String code, String name,
                   String description, LocalDateTime deadline, String status, List<Integer> staffIds,
                   List<Integer> partnerStaffIds, List<Integer> prerequisiteTaskIds);

    boolean updateTaskStatus(int taskId, String status);

    int updateTaskStatuses(int activityId, List<Integer> taskIds, String status);

    boolean deleteTask(int taskId);

    List<ChecklistTaskResponseDTO> getTasksForStaffAccount(int accountId);

    List<ChecklistTaskResponseDTO> getTasksForPartnerStaffAccount(int accountId);
}
