package com.swp391.g1.dao;

import com.swp391.g1.model.Appeal;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data access contract for the Appeal entity.
 */
public interface IAppealDAO {

    /** Student: insert a new appeal request (status = PENDING). */
    boolean insert(Appeal appeal);

    /** Get all appeals (staff/admin dashboard). */
    List<Appeal> getAll();

    /** Get only PENDING appeals for staff review. */
    List<Appeal> getPending();

    /** Get all appeals submitted by a specific student. */
    List<Appeal> getByStudentId(int studentId);

    /** Get appeals for a specific activity (staff view). */
    List<Appeal> getByActivityId(int activityId);

    /** Completed activities for which this student has an official final point. */
    List<Appeal> getEligibleActivities(int studentId);

    /** Get a single appeal by its primary key. */
    Appeal getById(int id);

    /**
     * Approve an appeal and update the student's official final point atomically.
     *
     * @param appealId   ID of the appeal
     * @param staffId    ID of the staff member who approved
     * @param newPoint   required new final point (0..10)
     * @param reviewNote optional internal note
     */
    boolean approve(int appealId, int staffId, BigDecimal newPoint, String reviewNote);

    /**
     * Reject an appeal.
     *
     * @param appealId   ID of the appeal
     * @param staffId    ID of the staff member who rejected
     * @param reviewNote reason for rejection (shown to student)
     */
    boolean reject(int appealId, int staffId, String reviewNote);

    /** Check whether a student already has a PENDING appeal for the given activity. */
    boolean hasPendingAppeal(int studentId, int activityId);
}
