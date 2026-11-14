package com.swp391.g1.model;

import java.util.List;

public class RecruitmentQuestion {
    private int id;
    private int roundId;
    private String questionText;
    private String questionType;
    private boolean required = true;
    private String options;
    private int sortOrder;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getRoundId() { return roundId; }
    public void setRoundId(int roundId) { this.roundId = roundId; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    /** Mỗi dòng của options là một lựa chọn. */
    public List<String> getOptionList() {
        return options == null || options.isBlank()
                ? List.of()
                : options.lines().map(String::trim).filter(o -> !o.isEmpty()).toList();
    }
}
