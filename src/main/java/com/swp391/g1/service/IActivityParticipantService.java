package com.swp391.g1.service;

import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.dto.response.ActivityParticipantResponseDTO;
import com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO;
import com.swp391.g1.dto.response.ParticipantCandidateDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IActivityParticipantService {
    List<ActivityParticipantResponseDTO> getParticipants(int activityId);

    /** Attendance can only be taken while the activity is ongoing or after it has completed. */
    boolean canTakeAttendance(ExtracurricularActivityResponseDTO activity);

    /** statusByParticipantId: participantId -> ParticipantStatus name. Returns the number of changed rows. */
    int saveAttendance(int activityId, Map<Integer, String> statusByParticipantId, CurrentUser user);

    /**
     * Grades participation evaluation (0-5) and feedback for one or many participants (batch grading).
     * Returns the number of updated rows.
     */
    int saveParticipationGrades(int activityId, List<Integer> participantIds, BigDecimal evalScore,
            String feedback, CurrentUser user);

    /** Students can be added or removed in every activity status except CANCELLED. */
    boolean canManageParticipants(ExtracurricularActivityResponseDTO activity);

    /** Active students that are not yet in the activity. */
    List<ParticipantCandidateDTO> getCandidates(int activityId);

    /** Adds the given students. Returns the number of added students. */
    int addStudents(int activityId, List<Integer> studentIds);

    default int addStudents(int activityId, List<Integer> studentIds, Integer registrationBy) {
        return addStudents(activityId, studentIds);
    }

    /**
     * Adds every candidate matching all given criteria (0 = any). At least one criterion is required.
     * Returns the number of added students.
     */
    int addStudentsByGroup(int activityId, int departmentId, int majorId, int programId, int classId);

    default int addStudentsByGroup(int activityId, int departmentId, int majorId, int programId, int classId, Integer registrationBy) {
        return addStudentsByGroup(activityId, departmentId, majorId, programId, classId);
    }

    /** Removes participants that have not been marked yet. Returns the number of removed rows. */
    int removeParticipants(int activityId, List<Integer> participantIds);
}
