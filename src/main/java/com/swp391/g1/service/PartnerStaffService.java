package com.swp391.g1.service;

import com.swp391.g1.dao.PartnerStaffDAO;
import com.swp391.g1.model.PartnerStaff;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class PartnerStaffService {

    private PartnerStaffDAO partnerStaffDAO;
    private boolean sampleMode;
    private final List<PartnerStaff> sampleStaff = new java.util.ArrayList<>();

    public PartnerStaffService() {
        try {
            partnerStaffDAO = new PartnerStaffDAO();
        } catch (IllegalStateException exception) {
            sampleMode = true;
            sampleStaff.addAll(createSampleStaff());
        }
    }

    public List<PartnerStaff> getAll() throws SQLException {
        if (sampleMode) {
            return sampleStaff;
        }
        try {
            return partnerStaffDAO.getAll();
        } catch (SQLException exception) {
            enableSampleMode();
            return sampleStaff;
        }
    }

    public List<PartnerStaff> getByPartnerId(int partnerId) throws SQLException {
        if (partnerId <= 0) {
            return List.of();
        }
        if (sampleMode) {
            return sampleStaff.stream().filter(staff -> staff.getPartnerId() == partnerId).toList();
        }
        try {
            return partnerStaffDAO.getByPartnerId(partnerId);
        } catch (SQLException exception) {
            enableSampleMode();
            return sampleStaff.stream().filter(staff -> staff.getPartnerId() == partnerId).toList();
        }
    }

    public PartnerStaff getById(int id) throws SQLException {
        if (id <= 0) {
            return null;
        }
        if (sampleMode) {
            return findSampleStaff(id);
        }
        try {
            return partnerStaffDAO.getById(id);
        } catch (SQLException exception) {
            enableSampleMode();
            return findSampleStaff(id);
        }
    }

    public boolean create(PartnerStaff staff) throws SQLException {
        if (!isValid(staff)) {
            return false;
        }
        if (staff.getCreatedAt() == null) {
            staff.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        }
        if (sampleMode) {
            addSampleStaff(staff);
            return true;
        }
        try {
            return partnerStaffDAO.insert(staff);
        } catch (SQLException exception) {
            enableSampleMode();
            addSampleStaff(staff);
            return true;
        }
    }

    public boolean update(PartnerStaff staff) throws SQLException {
        if (staff == null || staff.getId() == null || staff.getId() <= 0 || !isValid(staff)) {
            return false;
        }
        if (sampleMode) {
            return replaceSampleStaff(staff);
        }
        try {
            return partnerStaffDAO.update(staff);
        } catch (SQLException exception) {
            enableSampleMode();
            return replaceSampleStaff(staff);
        }
    }

    public boolean delete(int id) throws SQLException {
        if (id <= 0) {
            return false;
        }
        if (sampleMode) {
            return removeSampleStaff(id);
        }
        try {
            return partnerStaffDAO.delete(id);
        } catch (SQLException exception) {
            enableSampleMode();
            return removeSampleStaff(id);
        }
    }

    private void enableSampleMode() {
        sampleMode = true;
        if (sampleStaff.isEmpty()) {
            sampleStaff.addAll(createSampleStaff());
        }
    }

    private List<PartnerStaff> createSampleStaff() {
        List<PartnerStaff> staffList = new java.util.ArrayList<>();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        PartnerStaff first = new PartnerStaff("Nguyen Van An", "an@fpt.com", "0910000001",
                true, 1, now);
        first.setId(1);
        first.setPosition("Project Coordinator");
        PartnerStaff second = new PartnerStaff("Tran Thi Binh", "binh@vng.com", "0910000002",
                true, 2, now);
        second.setId(2);
        second.setPosition("Partner Representative");
        staffList.add(first);
        staffList.add(second);
        return staffList;
    }

    private PartnerStaff findSampleStaff(int id) {
        return sampleStaff.stream().filter(staff -> staff.getId() == id).findFirst().orElse(null);
    }

    private void addSampleStaff(PartnerStaff staff) {
        staff.setId(sampleStaff.stream().mapToInt(PartnerStaff::getId).max().orElse(0) + 1);
        sampleStaff.add(staff);
    }

    private boolean replaceSampleStaff(PartnerStaff staff) {
        for (int index = 0; index < sampleStaff.size(); index++) {
            if (sampleStaff.get(index).getId().equals(staff.getId())) {
                sampleStaff.set(index, staff);
                return true;
            }
        }
        return false;
    }

    private boolean removeSampleStaff(int id) {
        return sampleStaff.removeIf(staff -> staff.getId() == id);
    }

    private boolean isValid(PartnerStaff staff) {
        return staff != null
                && hasText(staff.getFullName())
                && hasText(staff.getEmail())
                && hasText(staff.getPhone())
                && staff.getIsActive() != null
                && staff.getPartnerId() != null
                && staff.getPartnerId() > 0;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}