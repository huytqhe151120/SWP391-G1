package com.swp391.g1.dto.response;

import com.swp391.g1.model.ActivityTargetParticipant;
import com.swp391.g1.model.Enum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ExtracurricularActivityResponseDTO {
    private int id;
    private String code;
    private String name;
    private int semesterId;
    private int activityTypeId;
    private int organizationLevelId;
    private int responsibleDepartmentId;
    private int responsibleStaffId;
    private Integer partnerCompanyId;          // Nullable
    private Integer partnerStaffId;            // Nullable

    // The information has been JOIN/Mapping for display in the user interface.
    private String semesterCode;                  // Join from Semester
    private String semesterName;                  // Join from Semester
    private String activityTypeCode;              // Join from ActivityType
    private String activityTypeName;              // Join from ActivityType
    private String organizationLevelCode;         // Join from ActivityOrganizationLevel
    private String organizationLevelName;         // Join from ActivityOrganizationLevel
    private BigDecimal organizationLevelCoefficient; // Join from ActivityOrganizationLevel
    private String responsibleDepartmentCode;     // Join from Department
    private String responsibleDepartmentName;     // Join from Department
    private String responsibleStaffCode;          // Join from Staff
    private String responsibleStaffName;          // Join from Staff
    private String partnerCompanyCode;              // Join from PartnerCompany (nullable)
    private String partnerCompanyName;           // Join from PartnerCompany
    private String partnerStaffCode;                // Join from PartnerStaff (nullable)
    private String partnerStaffName;             // Join from PartnerStaff (nullable)
    private Enum.ActivityNature nature;
    private boolean summaryRequired;
    private Integer maxParticipants;
    private int attendanceSessionCount = 1;
    private String address;
    private String description;
    private Enum.ActivityStatus activityStatus;        // UPCOMING, ONGOING, COMPLETED, CANCELLED[cite: 1]
    private Enum.ApprovalStatus approvalStatus;        // DRAFT, PENDING, APPROVED, REJECTED[cite: 1]
    private LocalDateTime createdAt;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean registrationFormRequired;
    private boolean isPublic = true;
    private List<ActivityTargetParticipant> targetParticipants = new ArrayList<>();

    public List<ActivityTargetParticipant> getTargetParticipants() {
        return targetParticipants;
    }

    public void setTargetParticipants(List<ActivityTargetParticipant> targetParticipants) {
        this.targetParticipants = targetParticipants == null ? new ArrayList<>() : targetParticipants;
    }

    public boolean isAllSchoolTarget() {
        return targetParticipants == null
                || targetParticipants.isEmpty()
                || targetParticipants.stream().allMatch(ActivityTargetParticipant::isAllSchool);
    }

    public String getTargetScope() {
        return isAllSchoolTarget() ? "ALL" : "GROUP";
    }

    public String getTargetParticipantsSummary() {
        if (isAllSchoolTarget()) {
            return "Toàn trường";
        }
        return targetParticipants.stream()
                .map(ActivityTargetParticipant::getDisplayLabel)
                .collect(Collectors.joining("; "));
    }

    public Set<String> getTargetDepartmentIdStrings() {
        Set<String> set = new LinkedHashSet<>();
        if (targetParticipants != null) {
            for (ActivityTargetParticipant t : targetParticipants) {
                if (t.getDepartmentId() != null) set.add(String.valueOf(t.getDepartmentId()));
            }
        }
        return set;
    }

    public Set<String> getTargetCohortIdStrings() {
        Set<String> set = new LinkedHashSet<>();
        if (targetParticipants != null) {
            for (ActivityTargetParticipant t : targetParticipants) {
                if (t.getCohortId() != null) set.add(String.valueOf(t.getCohortId()));
            }
        }
        return set;
    }

    public Set<String> getTargetMajorIdStrings() {
        Set<String> set = new LinkedHashSet<>();
        if (targetParticipants != null) {
            for (ActivityTargetParticipant t : targetParticipants) {
                if (t.getMajorId() != null) set.add(String.valueOf(t.getMajorId()));
            }
        }
        return set;
    }

    public Set<String> getTargetProgramIdStrings() {
        Set<String> set = new LinkedHashSet<>();
        if (targetParticipants != null) {
            for (ActivityTargetParticipant t : targetParticipants) {
                if (t.getAcademicProgramId() != null) set.add(String.valueOf(t.getAcademicProgramId()));
            }
        }
        return set;
    }

    public Set<String> getTargetClassIdStrings() {
        Set<String> set = new LinkedHashSet<>();
        if (targetParticipants != null) {
            for (ActivityTargetParticipant t : targetParticipants) {
                if (t.getMainClassId() != null) set.add(String.valueOf(t.getMainClassId()));
            }
        }
        return set;
    }

    public boolean isPublic() { return isPublic; }

    public boolean getPublic() { return isPublic; }

    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public boolean isRegistrationFormRequired() {
        return registrationFormRequired;
    }

    public void setRegistrationFormRequired(boolean registrationFormRequired) {
        this.registrationFormRequired = registrationFormRequired;
    }

    // Constructor with no parameter
    public ExtracurricularActivityResponseDTO() {
    }

    // Getter and Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(int semesterId) {
        this.semesterId = semesterId;
    }

    public int getActivityTypeId() {
        return activityTypeId;
    }

    public void setActivityTypeId(int activityTypeId) {
        this.activityTypeId = activityTypeId;
    }

    public int getOrganizationLevelId() {
        return organizationLevelId;
    }

    public void setOrganizationLevelId(int organizationLevelId) {
        this.organizationLevelId = organizationLevelId;
    }

    public String getOrganizationLevelCode() {
        return organizationLevelCode;
    }

    public void setOrganizationLevelCode(String organizationLevelCode) {
        this.organizationLevelCode = organizationLevelCode;
    }

    public String getOrganizationLevelName() {
        return organizationLevelName;
    }

    public void setOrganizationLevelName(String organizationLevelName) {
        this.organizationLevelName = organizationLevelName;
    }

    public BigDecimal getOrganizationLevelCoefficient() {
        return organizationLevelCoefficient;
    }

    public void setOrganizationLevelCoefficient(BigDecimal organizationLevelCoefficient) {
        this.organizationLevelCoefficient = organizationLevelCoefficient == null
                ? null
                : organizationLevelCoefficient.setScale(2, RoundingMode.HALF_UP);
    }

    public String getFormattedOrganizationLevelCoefficient() {
        if (organizationLevelCoefficient == null) {
            return "";
        }
        return organizationLevelCoefficient.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public int getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public void setResponsibleDepartmentId(int responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
    }

    public int getResponsibleStaffId() {
        return responsibleStaffId;
    }

    public void setResponsibleStaffId(int responsibleStaffId) {
        this.responsibleStaffId = responsibleStaffId;
    }

    public Integer getPartnerStaffId() {
        return partnerStaffId;
    }

    public void setPartnerStaffId(Integer partnerStaffId) {
        this.partnerStaffId = partnerStaffId;
    }

    public String getSemesterName() {
        return semesterName;
    }

    public void setSemesterName(String semesterName) {
        this.semesterName = semesterName;
    }

    public String getActivityTypeName() {
        return activityTypeName;
    }

    public void setActivityTypeName(String activityTypeName) {
        this.activityTypeName = activityTypeName;
    }

    public String getResponsibleDepartmentName() {
        return responsibleDepartmentName;
    }

    public void setResponsibleDepartmentName(String responsibleDepartmentName) {
        this.responsibleDepartmentName = responsibleDepartmentName;
    }

    public String getResponsibleStaffName() {
        return responsibleStaffName;
    }

    public void setResponsibleStaffName(String responsibleStaffName) {
        this.responsibleStaffName = responsibleStaffName;
    }

    public String getPartnerCompanyName() {
        return partnerCompanyName;
    }

    public void setPartnerCompanyName(String partnerCompanyName) {
        this.partnerCompanyName = partnerCompanyName;
    }

    public String getPartnerStaffName() {
        return partnerStaffName;
    }

    public void setPartnerStaffName(String partnerStaffName) {
        this.partnerStaffName = partnerStaffName;
    }

    public Enum.ActivityNature getNature() {
        return nature;
    }

    public void setNature(Enum.ActivityNature nature) {
        this.nature = nature;
    }

    public boolean isSummaryRequired() {
        return summaryRequired;
    }

    public void setSummaryRequired(boolean summaryRequired) {
        this.summaryRequired = summaryRequired;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public int getAttendanceSessionCount() {
        return attendanceSessionCount > 0 ? attendanceSessionCount : 1;
    }

    public void setAttendanceSessionCount(int attendanceSessionCount) {
        this.attendanceSessionCount = attendanceSessionCount > 0 ? attendanceSessionCount : 1;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Enum.ActivityStatus getActivityStatus() {
        return activityStatus;
    }

    public void setActivityStatus(Enum.ActivityStatus activityStatus) {
        this.activityStatus = activityStatus;
    }

    public Enum.ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(Enum.ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getSemesterCode() {
        return semesterCode;
    }

    public void setSemesterCode(String semesterCode) {
        this.semesterCode = semesterCode;
    }

    public String getActivityTypeCode() {
        return activityTypeCode;
    }

    public void setActivityTypeCode(String activityTypeCode) {
        this.activityTypeCode = activityTypeCode;
    }

    public String getResponsibleDepartmentCode() {
        return responsibleDepartmentCode;
    }

    public void setResponsibleDepartmentCode(String responsibleDepartmentCode) {
        this.responsibleDepartmentCode = responsibleDepartmentCode;
    }

    public String getResponsibleStaffCode() {
        return responsibleStaffCode;
    }

    public void setResponsibleStaffCode(String responsibleStaffCode) {
        this.responsibleStaffCode = responsibleStaffCode;
    }

    public String getPartnerCompanyCode() {
        return partnerCompanyCode;
    }

    public void setPartnerCompanyCode(String partnerCompanyCode) {
        this.partnerCompanyCode = partnerCompanyCode;
    }

    public String getPartnerStaffCode() {
        return partnerStaffCode;
    }

    public void setPartnerStaffCode(String partnerStaffCode) {
        this.partnerStaffCode = partnerStaffCode;
    }

    public int getPartnerCompanyId() {
        return partnerCompanyId != null ? partnerCompanyId : 0;
    }

    public void setPartnerCompanyId(Integer partnerCompanyId) {
        this.partnerCompanyId = partnerCompanyId;
    }
}
