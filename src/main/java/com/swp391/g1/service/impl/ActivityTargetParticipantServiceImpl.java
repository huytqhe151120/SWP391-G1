package com.swp391.g1.service.impl;

import com.swp391.g1.dao.IActivityTargetParticipantDAO;
import com.swp391.g1.dao.impl.ActivityTargetParticipantDAOImpl;
import com.swp391.g1.model.AcademicCohort;
import com.swp391.g1.model.AcademicProgram;
import com.swp391.g1.model.ActivityTargetParticipant;
import com.swp391.g1.model.Department;
import com.swp391.g1.model.MainClass;
import com.swp391.g1.model.Major;
import com.swp391.g1.service.IActivityTargetParticipantService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivityTargetParticipantServiceImpl implements IActivityTargetParticipantService {

    private static final ExecutorService AUTO_ENROLL_QUEUE = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "mandatory-activity-auto-enroll-worker");
        t.setDaemon(true);
        return t;
    });

    private final IActivityTargetParticipantDAO targetDAO;

    public ActivityTargetParticipantServiceImpl() {
        this.targetDAO = new ActivityTargetParticipantDAOImpl();
    }

    @Override
    public List<Department> getActiveAcademicDepartments() {
        return targetDAO.findActiveAcademicDepartments();
    }

    @Override
    public List<AcademicCohort> getActiveCohorts() {
        return targetDAO.findActiveCohorts();
    }

    @Override
    public List<Major> getActiveMajors() {
        return targetDAO.findActiveMajors();
    }

    @Override
    public List<AcademicProgram> getActiveAcademicPrograms() {
        return targetDAO.findActiveAcademicPrograms();
    }

    @Override
    public List<MainClass> getActiveMainClasses() {
        return targetDAO.findActiveMainClasses();
    }

    @Override
    public List<ActivityTargetParticipant> getTargetsByActivityId(int activityId) {
        return targetDAO.findByActivityId(activityId);
    }

    @Override
    public List<ActivityTargetParticipant> buildTargetRows(
            String targetScope,
            List<String> departmentIds,
            List<String> cohortIds,
            List<String> majorIds,
            List<String> academicProgramIds,
            List<String> mainClassIds) {

        String scope = targetScope == null || targetScope.isBlank() ? "ALL" : targetScope.trim().toUpperCase();
        if ("ALL".equals(scope)) {
            return List.of(new ActivityTargetParticipant(null, null, null, null, null));
        }
        if (!"GROUP".equals(scope)) {
            throw new IllegalArgumentException("Phạm vi đối tượng tham gia không hợp lệ.");
        }

        Set<Integer> selectedDeptIds = parsePositiveIds(departmentIds, "Khoa không hợp lệ.");
        Set<Integer> selectedCohortIds = parsePositiveIds(cohortIds, "Niên khóa không hợp lệ.");
        Set<Integer> selectedMajorIds = parsePositiveIds(majorIds, "Ngành học không hợp lệ.");
        Set<Integer> selectedProgramIds = parsePositiveIds(academicProgramIds, "Chương trình đào tạo không hợp lệ.");
        Set<Integer> selectedClassIds = parsePositiveIds(mainClassIds, "Lớp sinh hoạt không hợp lệ.");

        if (selectedDeptIds.isEmpty()
                && selectedCohortIds.isEmpty()
                && selectedMajorIds.isEmpty()
                && selectedProgramIds.isEmpty()
                && selectedClassIds.isEmpty()) {
            return List.of(new ActivityTargetParticipant(null, null, null, null, null));
        }

        Map<Integer, Major> majorById = new HashMap<>();
        for (Major m : targetDAO.findActiveMajors()) {
            majorById.put(m.getId(), m);
        }
        Map<Integer, AcademicProgram> programById = new HashMap<>();
        for (AcademicProgram p : targetDAO.findActiveAcademicPrograms()) {
            programById.put(p.getId(), p);
        }
        Map<Integer, MainClass> classById = new HashMap<>();
        for (MainClass mc : targetDAO.findActiveMainClasses()) {
            classById.put(mc.getId(), mc);
        }

        Set<Integer> consumedDeptIds = new LinkedHashSet<>();
        Set<Integer> consumedCohortIds = new LinkedHashSet<>();
        Set<Integer> consumedMajorIds = new LinkedHashSet<>();
        Set<Integer> consumedProgramIds = new LinkedHashSet<>();

        Set<ActivityTargetParticipant> result = new LinkedHashSet<>();

        // 1. Leaf level: Main Classes
        for (Integer classId : selectedClassIds) {
            MainClass mc = classById.get(classId);
            Integer progId = mc != null ? mc.getResponsibleAcademicProgramId() : null;
            AcademicProgram prog = progId != null ? programById.get(progId) : null;
            Integer majorId = prog != null ? prog.getResponsibleMajorId() : null;
            Major major = majorId != null ? majorById.get(majorId) : null;
            Integer deptId = major != null ? major.getResponsibleDepartmentId() : null;
            Integer cohortId = mc != null ? mc.getApplicableCohortId() : null;

            Integer matchedProg = (progId != null && selectedProgramIds.contains(progId)) ? progId : null;
            Integer matchedMajor = (majorId != null && selectedMajorIds.contains(majorId)) ? majorId : null;
            Integer matchedDept = (deptId != null && selectedDeptIds.contains(deptId)) ? deptId : null;
            Integer matchedCohort = (cohortId != null && selectedCohortIds.contains(cohortId)) ? cohortId : null;

            if (matchedProg != null) consumedProgramIds.add(matchedProg);
            if (matchedMajor != null) consumedMajorIds.add(matchedMajor);
            if (matchedDept != null) consumedDeptIds.add(matchedDept);
            if (matchedCohort != null) consumedCohortIds.add(matchedCohort);

            result.add(new ActivityTargetParticipant(matchedDept, matchedCohort, matchedMajor, matchedProg, classId));
        }

        // Academic branches (Department / Major / Program) to be combined with orthogonal Cohorts
        List<int[]> academicBranches = new ArrayList<>(); // [deptId, majorId, progId] (0 means null)

        // 2. Academic Programs not consumed by any selected Class
        for (Integer progId : selectedProgramIds) {
            if (consumedProgramIds.contains(progId)) {
                continue;
            }
            AcademicProgram prog = programById.get(progId);
            Integer majorId = prog != null ? prog.getResponsibleMajorId() : null;
            Major major = majorId != null ? majorById.get(majorId) : null;
            Integer deptId = major != null ? major.getResponsibleDepartmentId() : null;

            int matchedMajor = (majorId != null && selectedMajorIds.contains(majorId)) ? majorId : 0;
            int matchedDept = (deptId != null && selectedDeptIds.contains(deptId)) ? deptId : 0;

            if (matchedMajor > 0) consumedMajorIds.add(matchedMajor);
            if (matchedDept > 0) consumedDeptIds.add(matchedDept);

            academicBranches.add(new int[]{matchedDept, matchedMajor, progId});
        }

        // 3. Majors not consumed by any selected Program or Class
        for (Integer majorId : selectedMajorIds) {
            if (consumedMajorIds.contains(majorId)) {
                continue;
            }
            Major major = majorById.get(majorId);
            Integer deptId = major != null ? major.getResponsibleDepartmentId() : null;
            int matchedDept = (deptId != null && selectedDeptIds.contains(deptId)) ? deptId : 0;
            if (matchedDept > 0) consumedDeptIds.add(matchedDept);

            academicBranches.add(new int[]{matchedDept, majorId, 0});
        }

        // 4. Departments not consumed by any selected Major, Program, or Class
        for (Integer deptId : selectedDeptIds) {
            if (consumedDeptIds.contains(deptId)) {
                continue;
            }
            academicBranches.add(new int[]{deptId, 0, 0});
        }

        // 5. Combine Academic Branches with selected Cohorts (AND logic between orthogonal Cohort and Dept/Major/Program)
        for (int[] branch : academicBranches) {
            Integer dId = branch[0] > 0 ? branch[0] : null;
            Integer mId = branch[1] > 0 ? branch[1] : null;
            Integer pId = branch[2] > 0 ? branch[2] : null;
            if (selectedCohortIds.isEmpty()) {
                result.add(new ActivityTargetParticipant(dId, null, mId, pId, null));
            } else {
                for (Integer cohortId : selectedCohortIds) {
                    consumedCohortIds.add(cohortId);
                    result.add(new ActivityTargetParticipant(dId, cohortId, mId, pId, null));
                }
            }
        }

        // 6. Any remaining unconsumed Cohorts (e.g., Cohort-only selection or Cohort independent of selected Class)
        for (Integer cohortId : selectedCohortIds) {
            if (!consumedCohortIds.contains(cohortId)) {
                result.add(new ActivityTargetParticipant(null, cohortId, null, null, null));
            }
        }

        return new ArrayList<>(result);
    }

    @Override
    public void saveTargetsForActivity(int activityId, List<ActivityTargetParticipant> targets) {
        targetDAO.replaceTargetsForActivity(activityId, targets);
    }

    @Override
    public boolean isStudentEligible(int activityId, int studentId) {
        return targetDAO.isStudentEligible(activityId, studentId);
    }

    @Override
    public Set<Integer> getEligibleActivityIdsForStudent(int studentId) {
        return targetDAO.findEligibleActivityIdsForStudent(studentId);
    }

    @Override
    public void enqueueMandatoryAutoEnrollment(int activityId) {
        if (activityId <= 0) {
            return;
        }
        AUTO_ENROLL_QUEUE.submit(() -> {
            try {
                targetDAO.enrollEligibleStudentsForMandatoryActivity(activityId);
            } catch (Exception e) {
                System.err.println("Auto-enrollment failed for mandatory activity " + activityId + ": " + e.getMessage());
            }
        });
    }

    @Override
    public void removeUnattendedAutoEnrolledParticipants(int activityId) {
        targetDAO.removeUnattendedAutoEnrolledParticipants(activityId);
    }

    private Set<Integer> parsePositiveIds(List<String> rawValues, String errorMessage) {
        Set<Integer> ids = new LinkedHashSet<>();
        if (rawValues == null) {
            return ids;
        }
        for (String raw : rawValues) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                int id = Integer.parseInt(raw.trim());
                if (id <= 0) {
                    throw new IllegalArgumentException(errorMessage);
                }
                ids.add(id);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(errorMessage);
            }
        }
        return ids;
    }
}
