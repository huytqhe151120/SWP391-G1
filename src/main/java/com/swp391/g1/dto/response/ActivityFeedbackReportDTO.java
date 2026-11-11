package com.swp391.g1.dto.response;

import java.util.ArrayList;
import java.util.List;

public class ActivityFeedbackReportDTO {
    private ActivityFeedbackSummaryDTO summary;
    private final List<ActivityFeedbackAnswerDTO> answers = new ArrayList<>();
    private final List<FeedbackStudentDTO> pendingStudents = new ArrayList<>();
    private final int[] ratingDistribution = new int[5];
    private double averageContentRating;
    private double averageOrganizationRating;
    private double averageVenueRating;
    private int noteCount;

    public ActivityFeedbackSummaryDTO getSummary() { return summary; }
    public void setSummary(ActivityFeedbackSummaryDTO value) { summary = value; }
    public List<ActivityFeedbackAnswerDTO> getAnswers() { return answers; }
    public List<FeedbackStudentDTO> getPendingStudents() { return pendingStudents; }
    public int[] getRatingDistribution() { return ratingDistribution; }
    public double getAverageContentRating() { return averageContentRating; }
    public void setAverageContentRating(double value) { averageContentRating = value; }
    public double getAverageOrganizationRating() { return averageOrganizationRating; }
    public void setAverageOrganizationRating(double value) { averageOrganizationRating = value; }
    public double getAverageVenueRating() { return averageVenueRating; }
    public void setAverageVenueRating(double value) { averageVenueRating = value; }
    public int getNoteCount() { return noteCount; }
    public void setNoteCount(int value) { noteCount = value; }
}
