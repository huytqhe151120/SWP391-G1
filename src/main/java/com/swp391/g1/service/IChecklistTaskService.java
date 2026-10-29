package com.swp391.g1.service;

import com.swp391.g1.dto.response.ChecklistTaskResponseDTO;
import com.swp391.g1.dto.response.ChecklistAssigneeResponseDTO;
import com.swp391.g1.dto.response.ChecklistTaskDetailResponseDTO;
import com.swp391.g1.dto.response.ChecklistAssignmentIdsDTO;
import java.time.LocalDateTime;
import java.util.List;

public interface IChecklistTaskService {
    List<ChecklistTaskResponseDTO> getAllTasks();

    List<ChecklistAssigneeResponseDTO> getActiveStaff();

    List<ChecklistAssigneeResponseDTO> getActivePartnerStaff();

    int createTask(String code, String name, String description, LocalDateTime deadline,
                   String status, List<Integer> staffIds, List<Integer> partnerStaffIds);

    ChecklistTaskDetailResponseDTO getTaskDetail(int taskId);

    ChecklistAssignmentIdsDTO getAssigneeIds(int taskId);

    int updateTask(int taskId, String code, String name, String description, LocalDateTime deadline,
                   String status, List<Integer> staffIds, List<Integer> partnerStaffIds);

    boolean updateTaskStatus(int taskId, String status);

    boolean deleteTask(int taskId);

    List<ChecklistTaskResponseDTO> getTasksForStaffAccount(int accountId);

    List<ChecklistTaskResponseDTO> getTasksForPartnerStaffAccount(int accountId);
}
