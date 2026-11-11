package com.swp391.g1.dto.response;

import java.time.LocalDateTime;

public class ActivityFeedbackAnswerDTO {
    private int generalRating;
    private int contentRating;
    private int organizationRating;
    private int venueRating;
    private String bestPart;
    private String improvement;
    private String favorite;
    private String suggestions;
    private String note;
    private LocalDateTime submittedAt;

    public int getGeneralRating() { return generalRating; }
    public void setGeneralRating(int value) { generalRating = value; }
    public int getContentRating() { return contentRating; }
    public void setContentRating(int value) { contentRating = value; }
    public int getOrganizationRating() { return organizationRating; }
    public void setOrganizationRating(int value) { organizationRating = value; }
    public int getVenueRating() { return venueRating; }
    public void setVenueRating(int value) { venueRating = value; }
    public String getBestPart() { return bestPart; }
    public void setBestPart(String value) { bestPart = value; }
    public String getImprovement() { return improvement; }
    public void setImprovement(String value) { improvement = value; }
    public String getFavorite() { return favorite; }
    public void setFavorite(String value) { favorite = value; }
    public String getSuggestions() { return suggestions; }
    public void setSuggestions(String value) { suggestions = value; }
    public String getNote() { return note; }
    public void setNote(String value) { note = value; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime value) { submittedAt = value; }
}
