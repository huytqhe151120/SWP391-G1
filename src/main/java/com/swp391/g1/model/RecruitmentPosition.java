package com.swp391.g1.model;

public class RecruitmentPosition {
    private int id;
    private int roundId;
    private String name;
    private int quota;
    private int sortOrder;
    private int acceptedCount;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getRoundId() { return roundId; }
    public void setRoundId(int roundId) { this.roundId = roundId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getQuota() { return quota; }
    public void setQuota(int quota) { this.quota = quota; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public int getAcceptedCount() { return acceptedCount; }
    public void setAcceptedCount(int acceptedCount) { this.acceptedCount = acceptedCount; }
    public int getRemaining() { return Math.max(0, quota - acceptedCount); }
}
