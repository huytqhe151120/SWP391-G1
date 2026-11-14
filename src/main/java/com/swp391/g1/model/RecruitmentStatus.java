package com.swp391.g1.model;

/** Hằng số trạng thái và loại dùng chung cho chức năng tuyển ban tổ chức. */
public final class RecruitmentStatus {

    public static final String ROUND_DRAFT = "DRAFT";
    public static final String ROUND_OPEN = "OPEN";
    public static final String ROUND_CLOSED = "CLOSED";

    public static final String APP_PENDING = "PENDING";
    public static final String APP_INTERVIEW_INVITED = "INTERVIEW_INVITED";
    public static final String APP_INTERVIEWED = "INTERVIEWED";
    public static final String APP_ACCEPTED = "ACCEPTED";
    public static final String APP_REJECTED = "REJECTED";
    public static final String APP_WITHDRAWN = "WITHDRAWN";

    public static final String INTERVIEW_SCHEDULED = "SCHEDULED";
    public static final String INTERVIEW_CONFIRMED = "CONFIRMED";
    public static final String INTERVIEW_CANCELLED = "CANCELLED";
    public static final String INTERVIEW_DONE = "DONE";

    public static final String FORMAT_OFFLINE = "OFFLINE";
    public static final String FORMAT_ONLINE = "ONLINE";

    public static final String QUESTION_TEXT = "TEXT";
    public static final String QUESTION_DROPDOWN = "DROPDOWN";
    public static final String QUESTION_MULTI_CHOICE = "MULTI_CHOICE";

    public static final int MAX_POSITIONS = 10;
    public static final int MAX_QUESTIONS = 10;
    public static final int MAX_CHOICES = 2;
    public static final int REMINDER_HOURS_BEFORE_DEADLINE = 24;
    public static final int WITHDRAW_ACCEPTED_HOURS_BEFORE_ACTIVITY = 48;

    private RecruitmentStatus() {
    }
}
