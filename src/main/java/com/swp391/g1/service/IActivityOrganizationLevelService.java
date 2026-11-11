package com.swp391.g1.service;

import com.swp391.g1.model.ActivityOrganizationLevel;

import java.util.List;

public interface IActivityOrganizationLevelService {
    List<ActivityOrganizationLevel> getAllOrganizationLevels();

    ActivityOrganizationLevel getOrganizationLevelById(int id);

    boolean createOrganizationLevel(ActivityOrganizationLevel level);

    boolean updateOrganizationLevel(ActivityOrganizationLevel level);

    boolean deleteOrganizationLevel(int id);
}
