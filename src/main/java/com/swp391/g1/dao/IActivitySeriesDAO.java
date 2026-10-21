package com.swp391.g1.dao;

import com.swp391.g1.model.ActivitySeries;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface IActivitySeriesDAO extends IGenericDAO<ActivitySeries, Integer> {
    ActivitySeries findBySeriesCode(String seriesCode);
    List<ActivitySeries> findByApprovalStatus(String approvalStatus);
    boolean existsBySeriesCode(String seriesCode);

    Integer insertOccurrence(Connection conn,
                             String code,
                             String name,
                             int activityTypeId,
                             int responsibleDepartmentId,
                             int responsibleStaffId,
                             String description,
                             boolean registrationFormRequired,
                             Integer semesterId,
                             java.time.LocalDateTime startTimeUtc,
                             java.time.LocalDateTime endTimeUtc,
                             int seriesId,
                             int occurrenceNo,
                             java.time.LocalDate occurrenceStartDate) throws SQLException;

    int countOccurrencesBySeriesId(int seriesId);
}
