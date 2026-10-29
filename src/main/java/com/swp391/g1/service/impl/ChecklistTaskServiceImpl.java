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
    public List<ChecklistAssigneeResponseDTO> getActiveStaff() {
        return checklistTaskDAO.findActiveStaff();
    }

    @Override
    public List<ChecklistAssigneeResponseDTO> getActivePartnerStaff() {
        return checklistTaskDAO.findActivePartnerStaff();
    }

    @Override
    public int createTask(String code, String name, String description, LocalDateTime deadline,
                          String status, List<Integer> staffIds, List<Integer> partnerStaffIds) {
        return checklistTaskDAO.create(code, name, description, deadline, status, staffIds, partnerStaffIds);
    }

    @Override
    public ChecklistTaskDetailResponseDTO getTaskDetail(int taskId) {
        return checklistTaskDAO.findDetailById(taskId);
    }

    @Override
    public ChecklistAssignmentIdsDTO getAssigneeIds(int taskId) {
        return checklistTaskDAO.findAssigneeIds(taskId);
    }

    @Override
    public int updateTask(int taskId, String code, String name, String description, LocalDateTime deadline,
                          String status, List<Integer> staffIds, List<Integer> partnerStaffIds) {
        return checklistTaskDAO.update(taskId, code, name, description, deadline,
                status, staffIds, partnerStaffIds);
    }

    @Override
    public boolean updateTaskStatus(int taskId, String status) {
        return checklistTaskDAO.updateStatus(taskId, status);
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
