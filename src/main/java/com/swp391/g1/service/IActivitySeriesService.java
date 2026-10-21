package com.swp391.g1.service;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.ActivitySeries;

import java.util.List;

public interface IActivitySeriesService {
    List<ActivitySeries> getAllSeries();
    ActivitySeries getSeriesById(int id);
    ActivitySeries getSeriesBySeriesCode(String seriesCode);
    List<ActivitySeries> findSeriesByApprovalStatus(String approvalStatus);
    boolean createSeries(ActivitySeries series, CurrentUser user) throws Exception;
    boolean updateSeries(ActivitySeries series, CurrentUser user) throws Exception;
    boolean deleteSeries(int id, CurrentUser user) throws Exception;
    boolean generateOccurrences(int seriesId, CurrentUser user) throws Exception;
}