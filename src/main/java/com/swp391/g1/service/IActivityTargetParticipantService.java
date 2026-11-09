package com.swp391.g1.service;

import com.swp391.g1.model.AcademicCohort;
import com.swp391.g1.model.AcademicProgram;
import com.swp391.g1.model.ActivityTargetParticipant;
import com.swp391.g1.model.Department;
import com.swp391.g1.model.MainClass;
import com.swp391.g1.model.Major;

import java.util.List;
import java.util.Set;

public interface IActivityTargetParticipantService {
    List<Department> getActiveAcademicDepartments();

    List<AcademicCohort> getActiveCohorts();

    List<Major> getActiveMajors();

    List<AcademicProgram> getActiveAcademicPrograms();

    List<MainClass> getActiveMainClasses();

    List<ActivityTargetParticipant> getTargetsByActivityId(int activityId);

    List<ActivityTargetParticipant> buildTargetRows(
            String targetScope,
            List<String> departmentIds,
            List<String> cohortIds,
            List<String> majorIds,
            List<String> academicProgramIds,
            List<String> mainClassIds);

    void saveTargetsForActivity(int activityId, List<ActivityTargetParticipant> targets);

    boolean isStudentEligible(int activityId, int studentId);

    Set<Integer> getEligibleActivityIdsForStudent(int studentId);

    void enqueueMandatoryAutoEnrollment(int activityId);

    void removeUnattendedAutoEnrolledParticipants(int activityId);
}
