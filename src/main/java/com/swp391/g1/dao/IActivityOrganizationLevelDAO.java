package com.swp391.g1.dao;

import com.swp391.g1.model.ActivityOrganizationLevel;

import java.util.List;

public interface IActivityOrganizationLevelDAO {
    List<ActivityOrganizationLevel> findAll();

    ActivityOrganizationLevel findById(Integer id);

    List<ActivityOrganizationLevel> findByCode(String code);

    Integer insert(ActivityOrganizationLevel level);

    boolean update(ActivityOrganizationLevel level);

    boolean delete(Integer id);
}
