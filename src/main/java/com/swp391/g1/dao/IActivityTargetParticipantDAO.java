package com.swp391.g1.dao;

import com.swp391.g1.model.AcademicCohort;
import com.swp391.g1.model.AcademicProgram;
import com.swp391.g1.model.ActivityTargetParticipant;
import com.swp391.g1.model.Department;
import com.swp391.g1.model.MainClass;
import com.swp391.g1.model.Major;

import java.util.List;
import java.util.Set;

public interface IActivityTargetParticipantDAO {
    List<Department> findActiveAcademicDepartments();

    List<AcademicCohort> findActiveCohorts();

    List<Major> findActiveMajors();

    List<AcademicProgram> findActiveAcademicPrograms();

    List<MainClass> findActiveMainClasses();

    List<ActivityTargetParticipant> findByActivityId(int activityId);

    void replaceTargetsForActivity(int activityId, List<ActivityTargetParticipant> targets);

    boolean isStudentEligible(int activityId, int studentId);

    Set<Integer> findEligibleActivityIdsForStudent(int studentId);

    int enrollEligibleStudentsForMandatoryActivity(int activityId);

    void removeUnattendedAutoEnrolledParticipants(int activityId);
}
