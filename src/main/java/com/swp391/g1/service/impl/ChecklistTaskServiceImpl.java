package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IChecklistTaskDAO;
import com.swp391.g1.dao.impl.ChecklistTaskDAOImpl;
import com.swp391.g1.dto.response.ChecklistTaskResponseDTO;
import com.swp391.g1.dto.response.ChecklistAssigneeResponseDTO;
import com.swp391.g1.dto.response.ChecklistTaskDetailResponseDTO;
import com.swp391.g1.dto.response.ChecklistAssignmentIdsDTO;
import com.swp391.g1.service.IChecklistTaskService;

import java.time.LocalDateTime;
import java.util.List;

public class ChecklistTaskServiceImpl implements IChecklistTaskService {

    private final IChecklistTaskDAO checklistTaskDAO;

    public ChecklistTaskServiceImpl() {
        this(new ChecklistTaskDAOImpl());
    }

    ChecklistTaskServiceImpl(IChecklistTaskDAO checklistTaskDAO) {
        this.checklistTaskDAO = checklistTaskDAO;
    }

    @Override
    public List<ChecklistTaskResponseDTO> getAllTasks() {
        return checklistTaskDAO.findAll();
    }

    @Override
    public List<ChecklistTaskResponseDTO> getTasksForActivity(int activityId) {
        return checklistTaskDAO.findForActivity(activityId);
    }

    @Override
    public List<ChecklistAssigneeResponseDTO> getActiveStaff() {
        return checklistTaskDAO.findActiveStaff();
    }

    @Override
    public List<ChecklistAssigneeResponseDTO> getActiveStaff(int taskId) {
        return checklistTaskDAO.findActiveStaff(taskId);
    }

    @Override
    public List<ChecklistAssigneeResponseDTO> getActivePartnerStaff() {
        return checklistTaskDAO.findActivePartnerStaff();
    }

    @Override
    public List<ChecklistAssigneeResponseDTO> getActivePartnerStaff(int taskId) {
        return checklistTaskDAO.findActivePartnerStaff(taskId);
    }

    @Override
    public int createTask(int activityId, String phase, String priority, String code, String name,
                          String description, LocalDateTime deadline, String status,
                          List<Integer> staffIds, List<Integer> partnerStaffIds,
                          List<Integer> prerequisiteTaskIds, int assignedByAccountId) {
        return checklistTaskDAO.create(activityId, phase, priority, code, name, description, deadline,
                status, staffIds, partnerStaffIds, prerequisiteTaskIds, assignedByAccountId);
    }

    @Override
    public boolean addAssignees(int taskId, List<Integer> staffIds, List<Integer> partnerStaffIds,
                                int assignedByAccountId) {
        return checklistTaskDAO.addAssignees(taskId, staffIds, partnerStaffIds, assignedByAccountId);
    }

    @Override
    public ChecklistTaskDetailResponseDTO getTaskDetail(int taskId) {
        return checklistTaskDAO.findDetailById(taskId);
    }

    @Override
    public boolean isTaskAssignedToStaffAccount(int taskId, int accountId) {
        return checklistTaskDAO.isStaffAssignedToAccount(taskId, accountId);
    }

    @Override
    public boolean isTaskAssignedToPartnerStaffAccount(int taskId, int accountId) {
        return checklistTaskDAO.isPartnerStaffAssignedToAccount(taskId, accountId);
    }

    @Override
    public int getActiveStaffIdForAccount(int accountId) {
        return checklistTaskDAO.findActiveStaffIdForAccount(accountId);
    }

    @Override
    public int getActivePartnerStaffIdForAccount(int accountId) {
        return checklistTaskDAO.findActivePartnerStaffIdForAccount(accountId);
    }

    @Override
    public ChecklistAssignmentIdsDTO getAssigneeIds(int taskId) {
        return checklistTaskDAO.findAssigneeIds(taskId);
    }

    @Override
    public int updateTask(int taskId, int activityId, String phase, String priority, String code,
                          String name, String description, LocalDateTime deadline, String status,
                          List<Integer> staffIds, List<Integer> partnerStaffIds,
                          List<Integer> prerequisiteTaskIds, int assignedByAccountId) {
        return checklistTaskDAO.update(taskId, activityId, phase, priority, code, name, description, deadline,
                status, staffIds, partnerStaffIds, prerequisiteTaskIds, assignedByAccountId);
    }

    @Override
    public boolean updateTaskStatus(int taskId, String status) {
        return checklistTaskDAO.updateStatus(taskId, status);
    }

    @Override
    public int updateTaskStatuses(int activityId, List<Integer> taskIds, String status) {
        return checklistTaskDAO.updateStatuses(activityId, taskIds, status);
    }

    @Override
    public boolean deleteTask(int taskId) {
        return checklistTaskDAO.delete(taskId);
    }

    @Override
    public List<ChecklistTaskResponseDTO> getTasksForStaffAccount(int accountId) {
        return checklistTaskDAO.findForStaffAccount(accountId);
    }

    @Override
    public List<ChecklistTaskResponseDTO> getTasksForPartnerStaffAccount(int accountId) {
        return checklistTaskDAO.findForPartnerStaffAccount(accountId);
    }
}
