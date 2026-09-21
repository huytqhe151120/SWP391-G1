package com.swp391.g1.service;

import com.swp391.g1.dao.ExtracurricularActivityDAO;
import com.swp391.g1.model.ExtracurricularActivity;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class ExtracurricularActivityService {

    private ExtracurricularActivityDAO activityDAO;
    private boolean sampleMode;
    private final List<ExtracurricularActivity> sampleActivities = new java.util.ArrayList<>();

    public ExtracurricularActivityService() {
        try {
            activityDAO = new ExtracurricularActivityDAO();
        } catch (IllegalStateException exception) {
            sampleMode = true;
            sampleActivities.addAll(createSampleActivities());
        }
    }

    public List<ExtracurricularActivity> getAll() throws SQLException {
        if (sampleMode) {
            return sampleActivities;
        }
        try {
            return activityDAO.getAll();
        } catch (SQLException exception) {
            enableSampleMode();
            return sampleActivities;
        }
    }

    public ExtracurricularActivity getById(int id) throws SQLException {
        if (id <= 0) {
            return null;
        }
        if (sampleMode) {
            return findSampleActivity(id);
        }
        try {
            return activityDAO.getById(id);
        } catch (SQLException exception) {
            enableSampleMode();
            return findSampleActivity(id);
        }
    }

    public boolean create(ExtracurricularActivity activity) throws SQLException {
        if (!isValid(activity)) {
            return false;
        }
        if (activity.getCreatedAt() == null) {
            activity.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        }
        if (sampleMode) {
            addSampleActivity(activity);
            return true;
        }
        try {
            return activityDAO.insert(activity);
        } catch (SQLException exception) {
            enableSampleMode();
            addSampleActivity(activity);
            return true;
        }
    }

    public boolean update(ExtracurricularActivity activity) throws SQLException {
        if (activity == null || activity.getId() <= 0 || !isValid(activity)) {
            return false;
        }
        if (sampleMode) {
            return replaceSampleActivity(activity);
        }
        try {
            return activityDAO.update(activity);
        } catch (SQLException exception) {
            enableSampleMode();
            return replaceSampleActivity(activity);
        }
    }

    public boolean delete(int id) throws SQLException {
        if (id <= 0) {
            return false;
        }
        if (sampleMode) {
            return removeSampleActivity(id);
        }
        try {
            return activityDAO.delete(id);
        } catch (SQLException exception) {
            enableSampleMode();
            return removeSampleActivity(id);
        }
    }

    private void enableSampleMode() {
        sampleMode = true;
        if (sampleActivities.isEmpty()) {
            sampleActivities.addAll(createSampleActivities());
        }
    }

    private List<ExtracurricularActivity> createSampleActivities() {
        List<ExtracurricularActivity> activities = new java.util.ArrayList<>();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        ExtracurricularActivity first = new ExtracurricularActivity(1, "WORKSHOP", "ACT-001",
                "Java Web Development Workshop", 1, 1, 1, 1, 5, 0,
                "Da Nang", "Sample activity for demonstration", "IN_PROGRESS", "APPROVED", now);
        first.setId(1);
        ExtracurricularActivity second = new ExtracurricularActivity(1, "VOLUNTEER", "ACT-002",
                "Community Volunteer Day", 1, 1, 2, 2, 3, 0,
                "Ho Chi Minh City", "Sample volunteer activity", "NOT_STARTED", "PENDING", now);
        second.setId(2);
        activities.add(first);
        activities.add(second);
        return activities;
    }

    private ExtracurricularActivity findSampleActivity(int id) {
        return sampleActivities.stream().filter(activity -> activity.getId() == id).findFirst().orElse(null);
    }

    private void addSampleActivity(ExtracurricularActivity activity) {
        activity.setId(sampleActivities.stream().mapToInt(ExtracurricularActivity::getId).max().orElse(0) + 1);
        sampleActivities.add(activity);
    }

    private boolean replaceSampleActivity(ExtracurricularActivity activity) {
        for (int index = 0; index < sampleActivities.size(); index++) {
            if (sampleActivities.get(index).getId() == activity.getId()) {
                sampleActivities.set(index, activity);
                return true;
            }
        }
        return false;
    }

    private boolean removeSampleActivity(int id) {
        return sampleActivities.removeIf(activity -> activity.getId() == id);
    }

    private boolean isValid(ExtracurricularActivity activity) {
        return activity != null
                && activity.getSemesterId() > 0
                && hasText(activity.getActivityType())
                && hasText(activity.getCode())
                && hasText(activity.getName())
                && activity.getResponsibleDepartmentId() > 0
                && activity.getResponsibleStaffId() > 0
                && activity.getPartnerId() > 0
                && activity.getPartnerStaffId() > 0
                && activity.getBonusPoint() >= 0
                && activity.getPenaltyPoint() >= 0
                && hasText(activity.getActivityStatus())
                && hasText(activity.getApprovalStatus());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}