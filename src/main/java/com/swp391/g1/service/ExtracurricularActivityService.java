package com.swp391.g1.service;

import com.swp391.g1.dao.ExtracurricularActivityDAO;
import com.swp391.g1.model.ExtracurricularActivity;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class ExtracurricularActivityService {

    private final ExtracurricularActivityDAO activityDAO = new ExtracurricularActivityDAO();

    public List<ExtracurricularActivity> getAll() throws SQLException {
        return activityDAO.getAll();
    }

    public ExtracurricularActivity getById(int id) throws SQLException {
        if (id <= 0) {
            return null;
        }
        return activityDAO.getById(id);
    }

    public boolean create(ExtracurricularActivity activity) throws SQLException {
        if (!isValid(activity)) {
            return false;
        }

        if (activity.getCreatedAt() == null) {
            activity.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        }

        return activityDAO.insert(activity);
    }

    public boolean update(ExtracurricularActivity activity) throws SQLException {
        if (activity == null || activity.getId() <= 0 || !isValid(activity)) {
            return false;
        }

        return activityDAO.update(activity);
    }

    public boolean delete(int id) throws SQLException {
        if (id <= 0) {
            return false;
        }

        return activityDAO.delete(id);
    }

    public boolean approve(int activityId, Integer actorAccountId, String note)
            throws SQLException {
        if (activityId <= 0) {
            return false;
        }

        return activityDAO.approve(activityId, actorAccountId, note);
    }

    public boolean reject(int activityId, Integer actorAccountId, String note)
            throws SQLException {
        if (activityId <= 0) {
            return false;
        }

        return activityDAO.reject(activityId, actorAccountId, note);
    }

    private boolean isValid(ExtracurricularActivity activity) {
        if (activity == null
                || activity.getSemesterId() <= 0
                || !hasText(activity.getActivityType())
                || !hasText(activity.getCode())
                || !hasText(activity.getName())
                || activity.getResponsibleDepartmentId() <= 0
                || activity.getResponsibleStaffId() <= 0
                || activity.getBonusPoint() < 0
                || activity.getPenaltyPoint() < 0
                || !hasText(activity.getActivityStatus())
                || !hasText(activity.getApprovalStatus())) {
            return false;
        }

        if (activity.getPartnerStaffId() > 0 && activity.getPartnerId() <= 0) {
            return false;
        }

        return true;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
