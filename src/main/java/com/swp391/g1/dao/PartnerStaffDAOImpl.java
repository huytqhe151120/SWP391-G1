package com.swp391.g1.dao;

import com.swp391.g1.model.Enum;
import com.swp391.g1.model.PartnerStaff;

import java.util.List;

public class PartnerStaffDAOImpl implements IPartnerStaffDAO {
    @Override
    public List<PartnerStaff> findByCode(String code) {
        return List.of();
    }

    @Override
    public List<PartnerStaff> findByCommonStatus(Enum.CommonStatus status) {
        return List.of();
    }

    @Override
    public List<PartnerStaff> findByCompany(int companyId) {
        return List.of();
    }

    @Override
    public List<PartnerStaff> findAll() {
        return List.of();
    }

    @Override
    public PartnerStaff findById(Integer id) {
        return null;
    }

    @Override
    public Integer insert(PartnerStaff entity) {
        return 0;
    }

    @Override
    public boolean update(PartnerStaff entity) {
        return false;
    }

    @Override
    public boolean delete(Integer id) {
        return false;
    }
}
