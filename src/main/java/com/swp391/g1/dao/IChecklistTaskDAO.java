package com.swp391.g1.dao;

import com.swp391.g1.dto.response.ChecklistTaskResponseDTO;
import com.swp391.g1.dto.response.ChecklistAssigneeResponseDTO;
import com.swp391.g1.dto.response.ChecklistTaskDetailResponseDTO;
import com.swp391.g1.dto.response.ChecklistAssignmentIdsDTO;
import java.time.LocalDateTime;
import java.util.List;

public interface IChecklistTaskDAO {
    List<ChecklistTaskResponseDTO> findAll();

    List<ChecklistTaskResponseDTO> findForActivity(int activityId);

    List<ChecklistAssigneeResponseDTO> findActiveStaff();

    List<ChecklistAssigneeResponseDTO> findActiveStaff(int taskId);

    List<ChecklistAssigneeResponseDTO> findActivePartnerStaff();

    List<ChecklistAssigneeResponseDTO> findActivePartnerStaff(int taskId);

    int create(int activityId, String phase, String priority, String code, String name, String description,
               LocalDateTime deadline, String status, List<Integer> staffIds, List<Integer> partnerStaffIds,
               List<Integer> prerequisiteTaskIds, int assignedByAccountId);

    boolean addAssignees(int taskId, List<Integer> staffIds, List<Integer> partnerStaffIds,
                         int assignedByAccountId);

    ChecklistTaskDetailResponseDTO findDetailById(int taskId);

    boolean isStaffAssignedToAccount(int taskId, int accountId);

    boolean isPartnerStaffAssignedToAccount(int taskId, int accountId);

    int findActiveStaffIdForAccount(int accountId);

    int findActivePartnerStaffIdForAccount(int accountId);

    ChecklistAssignmentIdsDTO findAssigneeIds(int taskId);

    int update(int taskId, int activityId, String phase, String priority, String code, String name,
               String description, LocalDateTime deadline, String status, List<Integer> staffIds,
               List<Integer> partnerStaffIds, List<Integer> prerequisiteTaskIds, int assignedByAccountId);

    boolean updateStatus(int taskId, String status);

    int updateStatuses(int activityId, List<Integer> taskIds, String status);

    boolean delete(int taskId);

    List<ChecklistTaskResponseDTO> findForStaffAccount(int accountId);

    List<ChecklistTaskResponseDTO> findForPartnerStaffAccount(int accountId);
}
