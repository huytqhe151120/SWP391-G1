package com.swp391.g1.dao;
import com.swp391.g1.model.ActivityType;
import com.swp391.g1.model.Enum;
import java.util.List;

public interface IActivityType extends IGenericDAO {
    ActivityType findByCode(String code);
    List<ActivityType> findByCommonStatus(Enum.CommonStatus status);
}
