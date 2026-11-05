package com.swp391.g1.dao;

import com.swp391.g1.dto.response.ActivityParticipantResponseDTO;
import com.swp391.g1.dto.response.ParticipantCandidateDTO;
import com.swp391.g1.model.Enum.ParticipantStatus;

import java.util.List;
import java.util.Map;

public interface IActivityParticipantDAO {
    List<ActivityParticipantResponseDTO> findByActivityId(int activityId);

    /** Active students that are not yet participants of the activity. */
    List<ParticipantCandidateDTO> findCandidates(int activityId);

    int countByActivityId(int activityId);

    /** Distinct students in the attendance roster or holding an active registration seat. */
    int countOccupiedByActivityId(int activityId);

    /**
     * Adds active students that are not yet in the activity, in one transaction. Returns the number of
     * inserted rows, or -1 (nothing inserted) when the result would exceed maxParticipants (null = no limit).
     */
    int insertParticipants(int activityId, List<Integer> studentIds, Integer maxParticipants);

    /**
     * Deletes participants that are still NOT_YET and have no attendance history, so no attendance
     * data is lost. Other ids are skipped. Returns the number of deleted rows.
     */
    int deleteRemovable(int activityId, List<Integer> participantIds);

    /**
     * Updates attendance statuses of participants in one activity and records each actual change
     * in activity_attendance_history, all in one transaction. Returns the number of changed rows.
     */
    int updateStatuses(int activityId, Map<Integer, ParticipantStatus> statusByParticipantId, String changedBy);
}
