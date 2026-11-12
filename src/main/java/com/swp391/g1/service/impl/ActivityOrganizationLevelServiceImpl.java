package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IActivityOrganizationLevelDAO;
import com.swp391.g1.dao.impl.ActivityOrganizationLevelDAOImpl;
import com.swp391.g1.model.ActivityOrganizationLevel;
import com.swp391.g1.service.IActivityOrganizationLevelService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class ActivityOrganizationLevelServiceImpl implements IActivityOrganizationLevelService {

    private static final BigDecimal MIN_COEFFICIENT = new BigDecimal("0.01");
    private static final BigDecimal MAX_COEFFICIENT = new BigDecimal("10.00");

    private final IActivityOrganizationLevelDAO organizationLevelDAO;

    public ActivityOrganizationLevelServiceImpl() {
        this.organizationLevelDAO = new ActivityOrganizationLevelDAOImpl();
    }

    @Override
    public List<ActivityOrganizationLevel> getAllOrganizationLevels() {
        return organizationLevelDAO.findAll();
    }

    @Override
    public ActivityOrganizationLevel getOrganizationLevelById(int id) {
        if (id <= 0) {
            return null;
        }
        return organizationLevelDAO.findById(id);
    }

    @Override
    public boolean createOrganizationLevel(ActivityOrganizationLevel level) {
        validateOrganizationLevel(level, false);
        Integer id = organizationLevelDAO.insert(level);
        return id != null && id > 0;
    }

    @Override
    public boolean updateOrganizationLevel(ActivityOrganizationLevel level) {
        validateOrganizationLevel(level, true);
        return organizationLevelDAO.update(level);
    }

    @Override
    public boolean deleteOrganizationLevel(int id) {
        if (id <= 0) {
            return false;
        }
        return organizationLevelDAO.delete(id);
    }

    private void validateOrganizationLevel(ActivityOrganizationLevel level, boolean isUpdate) {
        if (level == null) {
            throw new IllegalArgumentException("Dữ liệu cấp tổ chức không hợp lệ.");
        }
        if (isUpdate && level.getId() <= 0) {
            throw new IllegalArgumentException("ID cấp tổ chức không hợp lệ.");
        }
        if (level.getCode() == null || level.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã cấp tổ chức không được để trống.");
        }
        if (level.getName() == null || level.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên cấp tổ chức không được để trống.");
        }
        BigDecimal coeff = level.getCoefficient();
        if (coeff == null) {
            throw new IllegalArgumentException("Hệ số nhân không được để trống.");
        }
        coeff = coeff.setScale(2, RoundingMode.HALF_UP);
        if (coeff.compareTo(MIN_COEFFICIENT) < 0 || coeff.compareTo(MAX_COEFFICIENT) > 0) {
            throw new IllegalArgumentException("Hệ số nhân phải nằm trong khoảng từ 0.01 đến 10.00.");
        }
        level.setCoefficient(coeff);
        String code = level.getCode().trim().toUpperCase();
        level.setCode(code);
        level.setName(level.getName().trim());
        int excludedId = isUpdate ? level.getId() : 0;
        boolean duplicate = organizationLevelDAO.findByCode(code).stream()
                .anyMatch(existing -> existing.getId() != excludedId);
        if (duplicate) {
            throw new IllegalArgumentException("Mã cấp tổ chức đã tồn tại.");
        }
    }
}
