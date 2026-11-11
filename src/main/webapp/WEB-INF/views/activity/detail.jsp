<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://swp391.g1/status" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<%
    if (request.getAttribute("totalParticipantsCount") == null && request.getAttribute("activity") != null) {
        com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO actObj =
                (com.swp391.g1.dto.response.ExtracurricularActivityResponseDTO) request.getAttribute("activity");
        int actId = actObj.getId();
        com.swp391.g1.model.RegistrationForm regFormObj =
                (com.swp391.g1.model.RegistrationForm) request.getAttribute("registrationForm");
        if (regFormObj == null) {
            try {
                regFormObj = new com.swp391.g1.service.impl.RegistrationFormServiceImpl().getByActivityId(actId);
                request.setAttribute("registrationForm", regFormObj);
            } catch (Exception ignored) {}
        }
        int totalApps = 0;
        long submittedCnt = 0, approvedCnt = 0, rejectedCnt = 0, cancelledCnt = 0;
        java.util.List<java.util.Map<String, Object>> recentAppsList = new java.util.ArrayList<>();
        Integer maxQuota = actObj.getMaxParticipants();

        int totalParts = 0;
        long attendedCnt = 0, absentCnt = 0, lateOrEarlyCnt = 0, notYetCnt = 0;
        java.util.Map<com.swp391.g1.model.Enum.ParticipantStatus, Long> stCounts =
                new java.util.EnumMap<>(com.swp391.g1.model.Enum.ParticipantStatus.class);
        for (com.swp391.g1.model.Enum.ParticipantStatus ps : com.swp391.g1.model.Enum.ParticipantStatus.values()) {
            stCounts.put(ps, 0L);
        }
        java.util.List<java.util.Map<String, Object>> recentPartsList = new java.util.ArrayList<>();

        try (java.sql.Connection conn = com.swp391.g1.common.DBContext.getConnection()) {
            if (regFormObj != null) {
                String appSql = "SELECT r.id, r.status, r.submitted_at, "
                        + "COALESCE(submitted.student_code, s.code) AS student_code, "
                        + "COALESCE(submitted.student_name, s.name) AS student_name, "
                        + "COALESCE(submitted.class_code, mc.code) AS class_code, "
                        + "p.name AS program_name, m.name AS major_name, d.name AS department_name "
                        + "FROM dbo.activity_registration r "
                        + "JOIN dbo.student s ON s.id = r.student_id "
                        + "LEFT JOIN dbo.main_class mc ON mc.id = s.main_class_id "
                        + "LEFT JOIN dbo.academic_program p ON p.id = mc.responsible_academic_program_id "
                        + "LEFT JOIN dbo.major m ON m.id = p.responsible_major_id "
                        + "LEFT JOIN dbo.department d ON d.id = m.responsible_department_id "
                        + "LEFT JOIN (SELECT registration_id, "
                        + "MAX(CASE WHEN field_key='application_student_code' THEN answer_value END) AS student_code, "
                        + "MAX(CASE WHEN field_key='application_student_name' THEN answer_value END) AS student_name, "
                        + "MAX(CASE WHEN field_key='application_class_code' THEN answer_value END) AS class_code "
                        + "FROM dbo.activity_registration_answer GROUP BY registration_id) submitted "
                        + "ON submitted.registration_id = r.id "
                        + "WHERE r.form_id = ? "
                        + "ORDER BY r.submitted_at DESC, r.id DESC";
                try (java.sql.PreparedStatement ps = conn.prepareStatement(appSql)) {
                    ps.setInt(1, regFormObj.getId());
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            totalApps++;
                            String st = rs.getString("status");
                            if ("SUBMITTED".equals(st)) submittedCnt++;
                            else if ("APPROVED".equals(st)) approvedCnt++;
                            else if ("REJECTED".equals(st)) rejectedCnt++;
                            else if ("CANCELLED".equals(st) || "WITHDRAWN".equals(st)) cancelledCnt++;
                            if (recentAppsList.size() < 5) {
                                java.util.Map<String, Object> row = new java.util.HashMap<>();
                                row.put("studentCode", rs.getString("student_code"));
                                row.put("studentName", rs.getString("student_name"));
                                row.put("departmentName", rs.getString("department_name"));
                                row.put("majorName", rs.getString("major_name"));
                                row.put("academicProgramName", rs.getString("program_name"));
                                row.put("classCode", rs.getString("class_code"));
                                java.sql.Timestamp ts = rs.getTimestamp("submitted_at");
                                row.put("submittedAt", ts != null ? ts.toLocalDateTime() : null);
                                row.put("status", st);
                                recentAppsList.add(row);
                            }
                        }
                    }
                }
            }

            String partSql = "SELECT ap.id, ap.attendance_status, "
                    + "s.code AS student_code, s.name AS student_name, mc.code AS class_code, "
                    + "p.name AS program_name, m.name AS major_name, d.name AS department_name "
                    + "FROM dbo.activity_participant ap "
                    + "JOIN dbo.student s ON s.id = ap.student_id "
                    + "LEFT JOIN dbo.main_class mc ON mc.id = s.main_class_id "
                    + "LEFT JOIN dbo.academic_program p ON p.id = mc.responsible_academic_program_id "
                    + "LEFT JOIN dbo.major m ON m.id = p.responsible_major_id "
                    + "LEFT JOIN dbo.department d ON d.id = m.responsible_department_id "
                    + "WHERE ap.activity_id = ? "
                    + "ORDER BY CASE WHEN ap.attendance_status IS NOT NULL AND ap.attendance_status <> 'NOT_YET' THEN 0 ELSE 1 END, "
                    + "s.name ASC, s.code ASC";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(partSql)) {
                ps.setInt(1, actId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        totalParts++;
                        String stStr = rs.getString("attendance_status");
                        com.swp391.g1.model.Enum.ParticipantStatus pst = com.swp391.g1.model.Enum.ParticipantStatus.NOT_YET;
                        if (stStr != null) {
                            try {
                                pst = com.swp391.g1.model.Enum.ParticipantStatus.valueOf(stStr);
                            } catch (IllegalArgumentException ignored) {}
                        }
                        stCounts.put(pst, stCounts.getOrDefault(pst, 0L) + 1L);
                        if (pst == com.swp391.g1.model.Enum.ParticipantStatus.ATTENDED) attendedCnt++;
                        else if (pst == com.swp391.g1.model.Enum.ParticipantStatus.ABSENT) absentCnt++;
                        else if (pst == com.swp391.g1.model.Enum.ParticipantStatus.IN_LATE
                                || pst == com.swp391.g1.model.Enum.ParticipantStatus.OUT_EARLY) lateOrEarlyCnt++;
                        else notYetCnt++;

                        if (recentPartsList.size() < 5) {
                            java.util.Map<String, Object> row = new java.util.HashMap<>();
                            row.put("studentCode", rs.getString("student_code"));
                            row.put("studentName", rs.getString("student_name"));
                            row.put("departmentName", rs.getString("department_name"));
                            row.put("majorName", rs.getString("major_name"));
                            row.put("academicProgramName", rs.getString("program_name"));
                            row.put("mainClassCode", rs.getString("class_code"));
                            row.put("attendanceStatus", pst);
                            recentPartsList.add(row);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        int regPct = (maxQuota != null && maxQuota > 0)
                ? Math.min(100, (int) Math.round((totalApps * 100.0) / maxQuota))
                : 0;
        long actualAttended = attendedCnt + lateOrEarlyCnt;
        int attRatePct = totalParts > 0
                ? Math.min(100, (int) Math.round((actualAttended * 100.0) / totalParts))
                : 0;

        request.setAttribute("totalApplicationsCount", totalApps);
        request.setAttribute("regSubmittedCount", submittedCnt);
        request.setAttribute("regApprovedCount", approvedCnt);
        request.setAttribute("regRejectedCount", rejectedCnt);
        request.setAttribute("regCancelledCount", cancelledCnt);
        request.setAttribute("recentApplications", recentAppsList);
        request.setAttribute("registrationMaxQuota", maxQuota);
        request.setAttribute("registrationProgressPct", regPct);

        request.setAttribute("participantStatuses", com.swp391.g1.model.Enum.ParticipantStatus.values());
        request.setAttribute("statusCounts", stCounts);
        request.setAttribute("totalParticipantsCount", totalParts);
        request.setAttribute("attendedCount", attendedCnt);
        request.setAttribute("absentCount", absentCnt);
        request.setAttribute("lateOrEarlyCount", lateOrEarlyCnt);
        request.setAttribute("notYetCount", notYetCnt);
        request.setAttribute("actualAttendedCount", actualAttended);
        request.setAttribute("attendanceRatePct", attRatePct);
        request.setAttribute("recentParticipants", recentPartsList);

        com.swp391.g1.dto.CurrentUser curUser = com.swp391.g1.util.AuthContext.getCurrentUser(request);
        boolean canAccAtt = com.swp391.g1.service.AccessPolicy.canTakeAttendance(curUser);
        request.setAttribute("canAccessAttendance", canAccAtt);
        boolean canOpenAtt = canAccAtt
                && actObj.getApprovalStatus() == com.swp391.g1.model.Enum.ApprovalStatus.APPROVED
                && actObj.getActivityStatus() != com.swp391.g1.model.Enum.ActivityStatus.CANCELLED;
        request.setAttribute("canOpenAttendancePage", canOpenAtt);
    }
%>
<c:set var="activeNav" value="activities" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chi tiết hoạt động | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261108-6">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261107-2">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-list-page activity-form-page activity-detail-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> QUẢN LÝ HOẠT ĐỘNG</span>
                    <h1>Chi tiết hoạt động</h1>
                    <p><span class="activity-code"><c:out value="${activity.code}"/></span> <c:out value="${activity.name}"/></p>
                </div>
            </header>

    <c:if test="${not empty sessionScope.successMessage}">
        <div class="alert alert-success activity-alert"><c:out value="${sessionScope.successMessage}"/></div>
        <c:remove var="successMessage" scope="session"/>
    </c:if>
    <c:if test="${not empty sessionScope.errorMessage}">
        <div class="alert alert-error activity-alert"><c:out value="${sessionScope.errorMessage}"/></div>
        <c:remove var="errorMessage" scope="session"/>
    </c:if>

    <section class="card activity-form-card activity-detail-card">
        <div class="activity-table-heading">
            <div>
                <h2>Chi tiết hoạt động</h2>
                <p>Thông tin tổ chức, thời gian và nhân sự phụ trách hoạt động.</p>
            </div>
            <div class="activity-detail-header-actions">
                <c:if test="${isStudent}">
                    <c:choose>
                        <c:when test="${activity.nature == 'MANDATORY'}">
                            <span class="badge status-pill status-green">
                                Hoạt động bắt buộc tham gia - Bạn đã được tự động ghi danh
                            </span>
                        </c:when>
                        <c:when test="${activity.nature == 'REQUIRE_REGISTRATION'}">
                            <c:choose>
                                <c:when test="${canStudentRegister and (empty myRegistration or myRegistration.status eq 'WITHDRAWN')}">
                                    <c:choose>
                                        <c:when test="${activity.registrationFormRequired and not empty registrationForm}">
                                            <a class="button button-primary"
                                               href="${pageContext.request.contextPath}/registration-forms?action=register&amp;id=${registrationForm.id}&amp;returnTo=${activityReturnUrlParameter}">
                                                Đợt đăng ký
                                            </a>
                                        </c:when>
                                        <c:otherwise>
                                            <form action="${pageContext.request.contextPath}/registration-forms" method="post" style="display:inline-flex; margin:0;">
                                                <input type="hidden" name="action" value="registerQuick">
                                                <input type="hidden" name="activityId" value="${activity.id}">
                                                <input type="hidden" name="returnTo" value="<c:out value='${activityReturnUrl}'/>">
                                                <button type="submit" class="button button-primary">Đợt đăng ký</button>
                                            </form>
                                        </c:otherwise>
                                    </c:choose>
                                </c:when>
                                <c:when test="${not empty myRegistration and myRegistration.status eq 'APPROVED'}">
                                    <span class="badge status-pill status-green">Đã đăng ký tham gia</span>
                                </c:when>
                                <c:when test="${not empty myRegistration and myRegistration.status eq 'SUBMITTED'}">
                                    <span class="badge status-pill status-yellow">Đang chờ duyệt đăng ký</span>
                                </c:when>
                            </c:choose>
                        </c:when>
                    </c:choose>
                </c:if>
                <c:if test="${canManageActivities && activity.activityStatus == 'UPCOMING'}">
                    <a class="button button-primary"
                       href="${pageContext.request.contextPath}/activities?action=edit&amp;id=${activity.id}">Chỉnh sửa</a>
                </c:if>
            </div>
        </div>

        <c:set var="safeTotalApps" value="${not empty totalApplicationsCount ? totalApplicationsCount : (not empty registrationForm ? registrationForm.registrationCount : 0)}"/>
        <c:set var="safeRegSubmitted" value="${not empty regSubmittedCount ? regSubmittedCount : 0}"/>
        <c:set var="safeRegApproved" value="${not empty regApprovedCount ? regApprovedCount : 0}"/>
        <c:set var="safeRegRejected" value="${not empty regRejectedCount ? regRejectedCount : 0}"/>
        <c:set var="safeRegCancelled" value="${not empty regCancelledCount ? regCancelledCount : 0}"/>
        <c:set var="safeRegMaxQuota" value="${not empty registrationMaxQuota ? registrationMaxQuota : (not empty registrationForm and not empty registrationForm.maxParticipants ? registrationForm.maxParticipants : activity.maxParticipants)}"/>
        <c:set var="safeRegProgressPct" value="${not empty registrationProgressPct ? registrationProgressPct : ((not empty safeRegMaxQuota and safeRegMaxQuota > 0) ? ((safeTotalApps * 100) / safeRegMaxQuota > 100 ? 100 : (safeTotalApps * 100) / safeRegMaxQuota) : 0)}"/>
        <c:set var="safeTotalParticipants" value="${not empty totalParticipantsCount ? totalParticipantsCount : 0}"/>
        <c:set var="safeActualAttended" value="${not empty actualAttendedCount ? actualAttendedCount : 0}"/>
        <c:set var="safeAttendanceRatePct" value="${not empty attendanceRatePct ? attendanceRatePct : 0}"/>

        <div class="activity-tabs-nav" role="tablist">
            <button type="button" class="activity-tab-btn active" id="btn-tab-info" data-tab="tab-info" role="tab" aria-selected="true" aria-controls="tab-info">
                Thông tin chi tiết
            </button>
            <button type="button" class="activity-tab-btn" id="btn-tab-staff" data-tab="tab-staff" role="tab" aria-selected="false" aria-controls="tab-staff">
                Phụ trách
            </button>
            <button type="button" class="activity-tab-btn" id="btn-tab-partner" data-tab="tab-partner" role="tab" aria-selected="false" aria-controls="tab-partner">
                Đối tác
            </button>
            <c:if test="${not isStudent}">
                <c:if test="${activity.registrationFormRequired}">
                    <button type="button" class="activity-tab-btn" id="btn-tab-registration" data-tab="tab-registration" role="tab" aria-selected="false" aria-controls="tab-registration">
                        Đăng ký tham gia
                        <span class="activity-tab-badge"><c:out value="${safeTotalApps}"/></span>
                    </button>
                </c:if>
                <button type="button" class="activity-tab-btn" id="btn-tab-attendance" data-tab="tab-attendance" role="tab" aria-selected="false" aria-controls="tab-attendance">
                    Điểm danh sinh viên
                    <span class="activity-tab-badge"><c:out value="${safeTotalParticipants}"/></span>
                </button>
            </c:if>
        </div>

        <div class="activity-detail-content">
            <!-- TAB 1: THÔNG TIN CHI TIẾT HOẠT ĐỘNG -->
            <div id="tab-info" class="activity-tab-pane active" role="tabpanel" aria-labelledby="btn-tab-info">
                <section class="activity-detail-section">
                    <h3>Thông tin cơ bản</h3>
                    <dl class="detail-grid activity-detail-grid">
                        <div class="detail-item"><dt>Mã hoạt động</dt><dd><c:out value="${activity.code}" default="—"/></dd></div>
                        <div class="detail-item"><dt>Tên hoạt động</dt><dd><c:out value="${activity.name}" default="—"/></dd></div>
                        <div class="detail-item"><dt>Học kỳ</dt><dd><c:out value="${activity.semesterCode}" default="—"/> <c:if test="${not empty activity.semesterName}">- <c:out value="${activity.semesterName}"/></c:if></dd></div>
                        <div class="detail-item"><dt>Loại hoạt động</dt><dd><c:out value="${activity.activityTypeCode}" default="—"/> <c:if test="${not empty activity.activityTypeName}">- <c:out value="${activity.activityTypeName}"/></c:if></dd></div>
                        <div class="detail-item">
                            <dt>Cấp tổ chức</dt>
                            <dd>
                                <c:choose>
                                    <c:when test="${not empty activity.organizationLevelName}">
                                        <c:out value="${activity.organizationLevelName}"/> (Hệ số: <c:out value="${activity.formattedOrganizationLevelCoefficient}"/>)
                                    </c:when>
                                    <c:otherwise>—</c:otherwise>
                                </c:choose>
                            </dd>
                        </div>
                        <div class="detail-item"><dt>Trạng thái hoạt động</dt><dd><span class="badge status-pill ${st:css(activity.activityStatus)}">${st:label(activity.activityStatus)}</span></dd></div>
                        <div class="detail-item"><dt>Trạng thái duyệt</dt><dd><span class="badge status-pill ${st:css(activity.approvalStatus)}">${st:label(activity.approvalStatus)}</span></dd></div>
                    </dl>
                </section>

                <section class="activity-detail-section">
                    <h3>Phụ trách chính</h3>
                    <dl class="detail-grid activity-detail-grid">
                        <div class="detail-item"><dt>Đơn vị phụ trách chính</dt><dd><c:out value="${activity.responsibleDepartmentCode}" default="—"/> <c:if test="${not empty activity.responsibleDepartmentName}">- <c:out value="${activity.responsibleDepartmentName}"/></c:if></dd></div>
                        <div class="detail-item"><dt>Nhân viên phụ trách chính</dt><dd><c:out value="${activity.responsibleStaffCode}" default="—"/> <c:if test="${not empty activity.responsibleStaffName}">- <c:out value="${activity.responsibleStaffName}"/></c:if></dd></div>
                    </dl>
                </section>

                <section class="activity-detail-section">
                    <h3>Thời gian, địa điểm và cấu hình</h3>
                    <dl class="detail-grid activity-detail-grid">
                        <div class="detail-item"><dt>Bắt đầu</dt><dd><c:out value="${dt:format(activity.startTime)}" default="Chưa xác định"/></dd></div>
                        <div class="detail-item"><dt>Kết thúc</dt><dd><c:out value="${dt:format(activity.endTime)}" default="Chưa xác định"/></dd></div>
                        <div class="detail-item"><dt>Tính chất hoạt động</dt><dd><c:choose><c:when test="${activity.nature == 'REQUIRE_REGISTRATION'}">Cần đăng ký</c:when><c:when test="${activity.nature == 'VOLUNTARY'}">Tự nguyện</c:when><c:when test="${activity.nature == 'MANDATORY'}">Bắt buộc</c:when><c:otherwise>—</c:otherwise></c:choose></dd></div>
                        <div class="detail-item"><dt>Bắt buộc điền form</dt><dd><c:out value="${activity.registrationFormRequired ? 'Có' : 'Không'}"/></dd></div>
                        <div class="detail-item"><dt>Yêu cầu bài tổng kết</dt><dd><c:out value="${activity.summaryRequired ? 'Có' : 'Không'}"/></dd></div>
                        <div class="detail-item"><dt>Số lượng tối đa</dt><dd><c:out value="${activity.maxParticipants}" default="Không giới hạn"/></dd></div>
                        <div class="detail-item"><dt>Số buổi điểm danh</dt><dd><c:out value="${activity.attendanceSessionCount > 0 ? activity.attendanceSessionCount : 1}"/> buổi</dd></div>
                        <div class="detail-item field-wide"><dt>Đối tượng tham gia</dt><dd><c:out value="${activity.targetParticipantsSummary}" default="Toàn trường"/></dd></div>
                        <div class="detail-item field-wide"><dt>Địa điểm</dt><dd><c:out value="${activity.address}" default="Chưa cập nhật"/></dd></div>
                        <div class="detail-item field-wide"><dt>Mô tả</dt><dd class="activity-detail-description"><c:out value="${activity.description}" default="Chưa có mô tả"/></dd></div>
                    </dl>
                </section>
            </div>

            <!-- TAB 2: THÔNG TIN NHÂN SỰ PHỤ TRÁCH -->
            <div id="tab-staff" class="activity-tab-pane" role="tabpanel" aria-labelledby="btn-tab-staff">
                <section class="activity-detail-section">
                    <div>
                        <h3 style="margin: 0;">Đối tượng phụ trách chính</h3>
                        <p style="margin: 0.25rem 0 0; font-size: 0.875rem; color: var(--text-muted, #666);">
                            Đơn vị phụ trách chính và nhân sự được phân công chịu trách nhiệm
                        </p>
                        <p style="margin: 0.25rem 0 0; font-size: 0.875rem; color: var(--text-muted, #666);">
                            Đơn vị: <strong><c:out value="${activity.responsibleDepartmentName}" default="Không có"/></strong>
                            - Nhân sự phụ trách chính: <strong><c:out value="${activity.responsibleStaffName}" default="Chưa có"/></strong>
                        </p>
                    </div>
                </section>

                <section class="activity-detail-section" style="grid-column: 1 / -1;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                        <div>
                            <h3 style="margin: 0;">Đơn vị đồng phụ trách</h3>
                            <p style="margin: 0.25rem 0 0; font-size: 0.875rem; color: var(--text-muted, #666);">
                                Các đơn vị cùng tham gia tổ chức hoạt động
                            </p>
                        </div>
                        <span class="activity-count"><c:out value="${empty coDepartments ? 0 : coDepartments.size()}"/> đơn vị</span>
                    </div>

                    <div class="table-wrap activity-table-wrap" style="margin-bottom: 1.5rem;">
                        <table>
                            <thead>
                            <tr>
                                <th scope="col" class="activity-index-column">STT</th>
                                <th scope="col">Mã đơn vị</th>
                                <th scope="col">Tên đơn vị</th>
                                <th scope="col" class="col-status">Trạng thái đơn vị</th>
                                <th scope="col">Thời gian thêm</th>
                                <c:if test="${canRemoveStaff}">
                                    <th scope="col" class="activity-action-column">Thao tác</th>
                                </c:if>
                            </tr>
                            </thead>
                            <tbody>
                            <c:choose>
                                <c:when test="${empty coDepartments}">
                                    <tr>
                                        <td class="empty-state activity-empty-state" colspan="${canRemoveStaff ? 6 : 5}">
                                            <strong>Chưa có đơn vị đồng phụ trách nào.</strong>
                                        </td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${coDepartments}" var="cd" varStatus="loop">
                                        <tr>
                                            <td>${loop.index + 1}</td>
                                            <td><strong><c:out value="${cd.departmentCode}"/></strong></td>
                                            <td><c:out value="${cd.departmentName}"/></td>
                                            <td class="col-status"><span class="badge status-pill ${st:css(cd.departmentStatus)}">${st:label(cd.departmentStatus)}</span></td>
                                            <td><c:out value="${cd.formattedCreatedAt}"/></td>
                                            <c:if test="${canRemoveStaff}">
                                                <td class="activity-action-column">
                                                    <div class="actions activity-actions"><form action="${pageContext.request.contextPath}/activities" method="post" style="display:inline"
                                                          data-confirm="Gỡ đơn vị này sẽ đồng thời gỡ các nhân sự chỉ thuộc đơn vị này khỏi hoạt động. Bạn có chắc chắn?">
                                                        <input type="hidden" name="action" value="removeDepartment">
                                                        <input type="hidden" name="activityId" value="${activity.id}">
                                                        <input type="hidden" name="departmentId" value="${cd.departmentId}">
                                                        <button type="submit" class="button button-sm action-btn action-btn--bulk-reject">
                                                        <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="m19 6-1 14H6L5 6"/><path d="M10 11v5"/><path d="M14 11v5"/></svg></span>
                                                            Gỡ bỏ
                                                        </button>
                                                    </form></div>
                                                </td>
                                            </c:if>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                            </tbody>
                        </table>
                    </div>

                    <c:if test="${canAddStaff}">
                        <div style="background: var(--surface-secondary, #f8fafc); border: 1px solid var(--border, #e2e8f0); border-radius: 8px; padding: 1.25rem;">
                            <h4 style="margin: 0 0 0.5rem; font-size: 1rem;">Thêm đơn vị đồng phụ trách</h4>
                            <c:choose>
                                <c:when test="${not empty availableDepartments}">
                                    <form action="${pageContext.request.contextPath}/activities" method="post" style="display: flex; gap: 0.75rem; align-items: center; flex-wrap: wrap;">
                                        <input type="hidden" name="action" value="addDepartment">
                                        <input type="hidden" name="activityId" value="${activity.id}">
                                        <div class="multi-select-combobox" data-multi-select data-item-label="đơn vị">
                                            <button type="button" class="multi-select-trigger" aria-expanded="false"
                                                    aria-controls="department-options">
                                                <span data-selection-label>Chọn đơn vị đồng phụ trách</span>
                                                <span aria-hidden="true">▾</span>
                                            </button>
                                            <div class="multi-select-dropdown" id="department-options" hidden>
                                                <input class="multi-select-search" type="search"
                                                       placeholder="Tìm đơn vị..." aria-label="Tìm đơn vị">
                                                <fieldset class="multi-select-options">
                                                    <legend class="visually-hidden">Chọn đơn vị</legend>
                                                    <c:forEach items="${availableDepartments}" var="dp">
                                                        <label class="multi-select-option">
                                                            <input type="checkbox" name="departmentIds" value="${dp.id}">
                                                            <span><c:out value="${dp.code}"/> - <c:out value="${dp.name}"/></span>
                                                        </label>
                                                    </c:forEach>
                                                </fieldset>
                                                <div class="multi-select-empty" hidden>Không tìm thấy đơn vị phù hợp.</div>
                                            </div>
                                            <span class="multi-select-count" aria-live="polite">Chưa chọn đơn vị</span>
                                        </div>
                                        <button type="submit" class="button button-primary button-sm action-btn"><span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M12 5v14"/><path d="M5 12h14"/></svg></span> Thêm đơn vị đồng phụ trách</button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <p style="margin: 0; font-style: italic; font-size: 0.875rem; color: var(--text-muted, #64748b);">
                                        Không còn đơn vị đang hoạt động nào để thêm.
                                    </p>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:if>
                </section>

                <section class="activity-detail-section" style="grid-column: 1 / -1;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                        <div>
                            <h3 style="margin: 0;">Danh sách nhân sự đồng phụ trách</h3>
                            <p style="margin: 0.25rem 0 0; font-size: 0.875rem; color: var(--text-muted, #666);">
                                Các nhân sự thuộc đơn vị đồng phụ trách tham gia điều phối hoạt động.
                            </p>
                        </div>
                        <span class="activity-count">
                            <c:choose>
                                <c:when test="${empty assignedStaffs}">0 nhân sự</c:when>
                                <c:otherwise><c:out value="${assignedStaffs.size()}"/> nhân sự</c:otherwise>
                            </c:choose>
                        </span>
                    </div>

                    <div class="table-wrap activity-table-wrap" style="margin-bottom: 1.5rem;">
                        <table>
                            <thead>
                            <tr>
                                <th scope="col" class="activity-index-column">STT</th>
                                <th scope="col">Mã nhân sự</th>
                                <th scope="col">Họ và tên</th>
                                <th scope="col" class="col-status">Trạng thái nhân sự</th>
                                <th scope="col">Chức vụ</th>
                                <th scope="col">Thời gian phân công</th>
                                <c:if test="${canRemoveStaff && activity.activityStatus == 'UPCOMING'}">
                                    <th scope="col" class="activity-action-column">Thao tác</th>
                                </c:if>
                            </tr>
                            </thead>
                            <tbody>
                            <c:choose>
                                <c:when test="${empty assignedStaffs}">
                                    <tr>
                                        <td class="empty-state activity-empty-state" colspan="${canRemoveStaff && activity.activityStatus == 'UPCOMING' ? 7 : 6}">
                                            <strong>Chưa có nhân sự nào được phân công phụ trách hoạt động này.</strong>
                                        </td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${assignedStaffs}" var="ast" varStatus="loop">
                                        <tr>
                                            <td>${loop.index + 1}</td>
                                            <td><strong><c:out value="${ast.staffCode}"/></strong></td>
                                            <td><c:out value="${ast.staffName}"/></td>
                                            <td class="col-status"><span class="badge status-pill ${st:css(ast.staffStatus)}">${st:label(ast.staffStatus)}</span></td>
                                            <td><c:out value="${ast.position}" default="Nhân viên"/></td>
                                            <td><c:out value="${ast.formattedAssignedOn}"/></td>
                                            <c:if test="${canRemoveStaff && activity.activityStatus == 'UPCOMING'}">
                                                <td class="activity-action-column">
                                                    <div class="actions activity-actions"><form action="${pageContext.request.contextPath}/activities" method="post" style="display:inline"
                                                      data-confirm="Bạn có chắc chắn muốn hủy phân công nhân sự này?">
                                                    <input type="hidden" name="action" value="unassignStaff">
                                                    <input type="hidden" name="activityId" value="${activity.id}">
                                                    <input type="hidden" name="staffId" value="${ast.staffId}">
                                                    <button type="submit" class="button button-sm action-btn action-btn--bulk-reject">
                                                        <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="m19 6-1 14H6L5 6"/><path d="M10 11v5"/><path d="M14 11v5"/></svg></span>
                                                        Gỡ bỏ
                                                    </button>
                                                    </form></div>
                                                </td>
                                            </c:if>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                            </tbody>
                        </table>
                    </div>

                    <c:if test="${canAddStaff && activity.activityStatus == 'UPCOMING'}">
                        <div style="background: var(--surface-secondary, #f8fafc); border: 1px solid var(--border, #e2e8f0); border-radius: 8px; padding: 1.25rem;">
                            <h4 style="margin: 0 0 0.5rem; font-size: 1rem;">Phân công nhân sự mới</h4>
                            <p style="margin: 0 0 1rem; font-size: 0.875rem; color: var(--text-muted, #64748b);">
                                Chỉ nhân sự đang hoạt động thuộc đơn vị phụ trách chính (<strong><c:out value="${activity.responsibleDepartmentName}" default="${activity.responsibleDepartmentCode}"/></strong>) hoặc các đơn vị đồng phụ trách mới có thể được phân công.
                            </p>
                            <c:choose>
                                <c:when test="${not empty availableStaffs}">
                                    <form action="${pageContext.request.contextPath}/activities" method="post" style="display: flex; gap: 0.75rem; align-items: center; flex-wrap: wrap;">
                                        <input type="hidden" name="action" value="assignStaff">
                                        <input type="hidden" name="activityId" value="${activity.id}">
                                        <div class="multi-select-combobox" data-multi-select data-item-label="nhân sự">
                                            <button type="button" class="multi-select-trigger" aria-expanded="false"
                                                    aria-controls="staff-options">
                                                <span data-selection-label>Chọn nhân sự để phân công</span>
                                                <span aria-hidden="true">▾</span>
                                            </button>
                                            <div class="multi-select-dropdown" id="staff-options" hidden>
                                                <input class="multi-select-search" type="search"
                                                       placeholder="Tìm nhân sự..." aria-label="Tìm nhân sự">
                                                <fieldset class="multi-select-options">
                                                    <legend class="visually-hidden">Chọn nhân sự</legend>
                                                    <c:forEach items="${availableStaffs}" var="st" >
                                                        <label class="multi-select-option">
                                                            <input type="checkbox" name="staffIds" value="${st.id}">
                                                            <span><c:if test="${not empty staffDepartmentLabels[st.id]}"><c:out value="${staffDepartmentLabels[st.id]}"/> - </c:if><c:out value="${st.code}"/> - <c:out value="${st.name}"/></span>
                                                        </label>
                                                    </c:forEach>
                                                </fieldset>
                                                <div class="multi-select-empty" hidden>Không tìm thấy nhân sự phù hợp.</div>
                                            </div>
                                            <span class="multi-select-count" aria-live="polite">Chưa chọn nhân sự</span>
                                        </div>
                                        <button type="submit" class="button button-primary button-sm action-btn"><span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M12 5v14"/><path d="M5 12h14"/></svg></span> Phân công vào hoạt động</button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <p style="margin: 0; font-style: italic; font-size: 0.875rem; color: var(--text-muted, #64748b);">
                                        Không còn nhân sự đang hoạt động nào thuộc các đơn vị phụ trách chưa được phân công.
                                    </p>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:if>
                </section>
            </div>

            <!-- TAB 3: ĐỐI TÁC HOẠT ĐỘNG -->
            <div id="tab-partner" class="activity-tab-pane" role="tabpanel" aria-labelledby="btn-tab-partner">
                <section class="activity-detail-section" style="grid-column: 1 / -1;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                        <div>
                            <h3 style="margin: 0;">Lời mời đối tác</h3>
                            <p style="margin: 0.25rem 0 0; font-size: 0.875rem; color: var(--text-muted, #666);">
                                Mời hợp tác hoặc mời ngừng hợp tác đều cần lời nhắn và chỉ có hiệu lực sau khi nhân sự được mời (hoặc trưởng/thư ký của công ty đối tác) chấp nhận.
                            </p>
                        </div>
                        <span class="activity-count"><c:out value="${empty invitations ? 0 : invitations.size()}"/> lời mời</span>
                    </div>
                    <div class="table-wrap activity-table-wrap" style="margin-bottom: 1.5rem;">
                        <table>
                            <thead>
                            <tr>
                                <th scope="col" class="activity-index-column">STT</th>
                                <th scope="col">Công ty đối tác</th>
                                <th scope="col">Phụ trách chính</th>
                                <th scope="col">Loại lời mời</th>
                                <th scope="col">Lời nhắn</th>
                                <th scope="col" class="col-status">Trạng thái</th>
                                <th scope="col">Người mời</th>
                                <th scope="col">Thời gian mời</th>
                                <th scope="col" class="activity-action-column">Thao tác</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:choose>
                                <c:when test="${empty invitations}">
                                    <tr>
                                        <td class="empty-state activity-empty-state" colspan="9">
                                            <strong>Chưa có lời mời nào được gửi cho hoạt động này.</strong>
                                        </td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${invitations}" var="inv" varStatus="loop">
                                        <tr>
                                            <td>${loop.index + 1}</td>
                                            <td><strong><c:out value="${inv.partnerCompanyCode}"/></strong> - <c:out value="${inv.partnerCompanyName}"/></td>
                                            <td><c:out value="${inv.partnerStaffName}" default="—"/></td>
                                            <td>${inv.termination ? 'Mời ngừng hợp tác' : 'Mời hợp tác'}</td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${empty inv.message}">—</c:when>
                                                    <c:otherwise>
                                                        <button type="button" class="button button-sm action-btn action-btn--view activity-message-view"
                                                                data-title="${inv.termination ? 'Lời nhắn mời ngừng hợp tác' : 'Lời nhắn mời hợp tác'}"
                                                                data-message="<c:out value='${inv.message}'/>"><span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg></span> Xem
                                                        </button>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="col-status">
                                                <span class="badge status-pill ${st:css(inv.approvalStatus)}">
                                                    <c:choose>
                                                        <c:when test="${inv.approvalStatus == 'PENDING'}">Chờ phản hồi</c:when>
                                                        <c:when test="${!inv.termination && inv.approvalStatus == 'APPROVED' && inv.collaborationStatus != 'ACTIVE'}">Đã ngừng hợp tác</c:when>
                                                        <c:otherwise>${st:label(inv.approvalStatus)}</c:otherwise>
                                                    </c:choose>
                                                </span>
                                            </td>
                                            <td><c:out value="${empty inv.createdByName ? inv.createdBy : inv.createdByName}"/></td>
                                            <td><c:out value="${inv.formattedCreatedAt}"/></td>
                                            <td class="activity-action-column">
                                                <div class="actions activity-actions">
                                                <c:if test="${inv.canRespond && activity.activityStatus == 'UPCOMING'}">
                                                    <form action="${pageContext.request.contextPath}/activities" method="post" style="display:inline">
                                                        <input type="hidden" name="action" value="respondInvitation">
                                                        <input type="hidden" name="activityId" value="${activity.id}">
                                                        <input type="hidden" name="invitationId" value="${inv.id}">
                                                        <button type="submit" name="decision" value="accept" class="button button-sm action-btn action-btn--bulk-approve"><span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/></svg></span> Chấp nhận</button>
                                                        <button type="submit" name="decision" value="reject" class="button button-sm action-btn action-btn--bulk-reject"><span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/></svg></span> Từ chối</button>
                                                    </form>
                                                </c:if>
                                                <c:if test="${inv.canRevoke}">
                                                    <form action="${pageContext.request.contextPath}/activities" method="post" style="display:inline"
                                                          data-confirm="Hủy phản hồi sẽ đưa lời mời về trạng thái chờ phản hồi${inv.approvalStatus == 'APPROVED' ? ' và gỡ các nhân sự đối tác của công ty khỏi hoạt động' : ''}. Bạn có chắc chắn?">
                                                        <input type="hidden" name="action" value="revokeInvitation">
                                                        <input type="hidden" name="activityId" value="${activity.id}">
                                                        <input type="hidden" name="invitationId" value="${inv.id}">
                                                        <button type="submit" class="button button-sm action-btn button-revoke">
                                                            <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M3 7v6h6"/><path d="M5.5 13a8 8 0 1 0 2-7L3 11"/></svg></span>
                                                            ${inv.approvalStatus == 'APPROVED' ? 'Hủy duyệt' : 'Hủy từ chối'}
                                                        </button>
                                                    </form>
                                                </c:if>
                                                <c:if test="${canManageActivities && inv.approvalStatus == 'PENDING' && activity.activityStatus == 'UPCOMING'}">
                                                    <form action="${pageContext.request.contextPath}/activities" method="post" style="display:inline"
                                                          data-confirm="Bạn có chắc chắn muốn hủy lời mời này?">
                                                        <input type="hidden" name="action" value="cancelInvitation">
                                                        <input type="hidden" name="activityId" value="${activity.id}">
                                                        <input type="hidden" name="invitationId" value="${inv.id}">
                                                        <button type="submit" class="button button-sm action-btn button-revoke"><span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/></svg></span> Hủy lời mời</button>
                                                    </form>
                                                </c:if>
                                            </div>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                            </tbody>
                        </table>
                    </div>

                    <c:if test="${canManageActivities && activity.approvalStatus != 'APPROVED'}">
                        <p class="field-hint">Chỉ có thể tạo lời mời đối tác khi hoạt động đã được duyệt.</p>
                    </c:if>
                    <c:if test="${canManageActivities && activity.approvalStatus == 'APPROVED'}">
                        <button type="button" class="button button-primary" id="invite-form-toggle"
                                aria-expanded="false" aria-controls="invite-form-panel">Tạo lời mời mới</button>
                        <div id="invite-form-panel" hidden>
                        <form class="activity-partner-form" method="post" action="${pageContext.request.contextPath}/activities">
                            <input type="hidden" id="inviteAction" name="action" value="inviteCollaborator">
                            <input type="hidden" name="activityId" value="${activity.id}">
                            <div class="field" style="margin-bottom: 0.75rem;">
                                <label for="inviteType">Loại lời mời</label>
                                <select id="inviteType">
                                    <option value="inviteCollaborator">Mời hợp tác</option>
                                    <option value="terminatePartner">Mời ngừng hợp tác</option>
                                </select>
                            </div>
                            <div id="inviteTerminateFields" class="field" hidden>
                                <label id="partnerCompanyLinkLabel">Đối tác đang hợp tác</label>
                                <div class="multi-select-combobox" id="partnerLinkCombobox" data-multi-select data-single data-required
                                     data-item-label="đối tác" data-disabled>
                                    <button type="button" class="multi-select-trigger" aria-expanded="false"
                                            aria-controls="partner-link-options" aria-labelledby="partnerCompanyLinkLabel" disabled>
                                        <span data-selection-label>Chọn đối tác</span>
                                        <span aria-hidden="true">▾</span>
                                    </button>
                                    <div class="multi-select-dropdown" id="partner-link-options" hidden>
                                        <input class="multi-select-search" type="search"
                                               placeholder="Tìm đối tác..." aria-label="Tìm đối tác">
                                        <fieldset class="multi-select-options">
                                            <legend class="visually-hidden">Chọn đối tác</legend>
                                            <c:forEach var="partner" items="${approvedPartners}">
                                                <c:if test="${partner.active}">
                                                    <label class="multi-select-option">
                                                        <input type="radio" name="partnerCompanyLinkId" value="${partner.id}" disabled>
                                                        <span><c:out value="${partner.partnerCompanyCode}"/> - <c:out value="${partner.partnerCompanyName}"/></span>
                                                    </label>
                                                </c:if>
                                            </c:forEach>
                                        </fieldset>
                                        <div class="multi-select-empty" hidden>Không tìm thấy đối tác phù hợp.</div>
                                    </div>
                                    <span class="multi-select-count" aria-live="polite">Chưa chọn đối tác</span>
                                </div>
                            </div>
                            <div id="inviteCollabFields" class="form-grid activity-form-grid">
                                <div class="field">
                                    <label id="partnerCompanyLabel">Công ty đối tác</label>
                                    <div class="multi-select-combobox" id="partnerCompanyCombobox" data-multi-select data-single data-required
                                         data-item-label="công ty đối tác">
                                        <button type="button" class="multi-select-trigger" aria-expanded="false"
                                                aria-controls="partner-company-options" aria-labelledby="partnerCompanyLabel">
                                            <span data-selection-label>Chọn công ty đối tác</span>
                                            <span aria-hidden="true">▾</span>
                                        </button>
                                        <div class="multi-select-dropdown" id="partner-company-options" hidden>
                                            <input class="multi-select-search" type="search"
                                                   placeholder="Tìm công ty..." aria-label="Tìm công ty đối tác">
                                            <fieldset class="multi-select-options">
                                                <legend class="visually-hidden">Chọn công ty đối tác</legend>
                                                <c:forEach var="company" items="${partnerCompanies}">
                                                    <label class="multi-select-option">
                                                        <input type="radio" name="partnerCompanyId" value="${company.id}">
                                                        <span><c:out value="${company.code}"/> - <c:out value="${company.name}"/></span>
                                                    </label>
                                                </c:forEach>
                                            </fieldset>
                                            <div class="multi-select-empty" hidden>Không tìm thấy công ty phù hợp.</div>
                                        </div>
                                        <span class="multi-select-count" aria-live="polite">Chưa chọn công ty đối tác</span>
                                    </div>
                                </div>
                                <div class="field">
                                    <label id="partnerStaffLabel">Nhân sự đối tác phụ trách chính</label>
                                    <div class="multi-select-combobox" id="partnerStaffCombobox" data-multi-select data-single data-required
                                         data-item-label="nhân sự phụ trách chính" data-disabled
                                         data-input-name="partnerStaffId"
                                         data-staff-endpoint="${pageContext.request.contextPath}/activities">
                                        <button type="button" class="multi-select-trigger" aria-expanded="false"
                                                aria-controls="partner-staff-options" aria-labelledby="partnerStaffLabel" disabled>
                                            <span data-selection-label>Chọn nhân sự phụ trách chính</span>
                                            <span aria-hidden="true">▾</span>
                                        </button>
                                        <div class="multi-select-dropdown" id="partner-staff-options" hidden>
                                            <input class="multi-select-search" type="search"
                                                   placeholder="Tìm nhân sự..." aria-label="Tìm nhân sự đối tác">
                                            <fieldset class="multi-select-options">
                                                <legend class="visually-hidden">Chọn nhân sự phụ trách chính</legend>
                                            </fieldset>
                                            <div class="multi-select-empty" hidden>Không tìm thấy nhân sự phù hợp.</div>
                                        </div>
                                        <span class="multi-select-count" aria-live="polite">Chưa chọn nhân sự phụ trách chính</span>
                                    </div>
                                    <span id="partnerStaffMessage" class="field-hint" aria-live="polite"></span>
                                </div>
                            </div>
                            <div class="field">
                                <label for="inviteMessage">Lời nhắn gửi đối tác</label>
                                <textarea id="inviteMessage" name="message" rows="2" maxlength="500" required
                                          placeholder="Nội dung lời mời / lý do ngừng hợp tác"></textarea>
                            </div>
                            <div class="activity-partner-form-actions">
                                <button type="submit" class="button button-primary">Gửi lời mời</button>
                            </div>
                        </form>
                        </div>
                    </c:if>
                </section>

                <section class="activity-detail-section" style="grid-column: 1 / -1;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                        <div>
                            <h3 style="margin: 0;">Đối tác tham gia hoạt động</h3>
                            <p style="margin: 0.25rem 0 0; font-size: 0.875rem; color: var(--text-muted, #666);">
                                Các đối tác của hoạt động và trạng thái tham gia; mỗi đối tác có một nhân sự phụ trách chính.
                            </p>
                        </div>
                        <span class="activity-count"><c:out value="${empty approvedPartners ? 0 : approvedPartners.size()}"/> đối tác</span>
                    </div>
                    <div class="table-wrap activity-table-wrap">
                        <table>
                            <thead>
                            <tr>
                                <th scope="col" class="activity-index-column">STT</th>
                                <th scope="col">Công ty đối tác</th>
                                <th scope="col">Phụ trách chính</th>
                                <th scope="col">Chức vụ</th>
                                <th scope="col" class="col-status">Trạng thái</th>
                                <th scope="col">Bắt đầu hợp tác</th>
`                            </tr>
                            </thead>
                            <tbody>
                            <c:choose>
                                <c:when test="${empty approvedPartners}">
                                    <tr>
                                        <td class="empty-state activity-empty-state" colspan="6">
                                            <strong>Chưa có đối tác nào được mời tham gia hoạt động này.</strong>
                                        </td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${approvedPartners}" var="partner" varStatus="loop">
                                        <tr>
                                            <td>${loop.index + 1}</td>
                                            <td><strong><c:out value="${partner.partnerCompanyCode}"/></strong> - <c:out value="${partner.partnerCompanyName}"/></td>
                                            <td><c:out value="${partner.partnerStaffName}" default="—"/></td>
                                            <td><c:out value="${partner.partnerStaffPosition}" default="—"/></td>
                                            <td class="col-status">
                                                <span class="badge status-pill ${partner.active ? 'status-green' : 'status-gray'}">
                                                    ${partner.active ? 'Đang hoạt động' : 'Không hoạt động'}
                                                </span>
                                            </td>
                                            <td><c:out value="${partner.formattedCreatedAt}"/></td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                            </tbody>
                        </table>
                    </div>
                </section>

                <section class="activity-detail-section" style="grid-column: 1 / -1;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                        <div>
                            <h3 style="margin: 0;">Danh sách nhân sự đối tác phụ trách</h3>
                            <p style="margin: 0.25rem 0 0; font-size: 0.875rem; color: var(--text-muted, #666);">
                                Các nhân sự của đối tác tham gia điều phối hoạt động.
                            </p>
                        </div>
                        <span class="activity-count"><c:out value="${empty assignedPartnerStaffs ? 0 : assignedPartnerStaffs.size()}"/> nhân sự</span>
                    </div>

                    <div class="table-wrap activity-table-wrap" style="margin-bottom: 1.5rem;">
                        <table>
                            <thead>
                            <tr>
                                <th scope="col" class="activity-index-column">STT</th>
                                <th scope="col">Mã nhân sự</th>
                                <th scope="col">Họ và tên</th>
                                <th scope="col">Công ty</th>
                                <th scope="col">Chức vụ</th>
                                <th scope="col" class="col-status">Trạng thái</th>
                                <th scope="col">Thời gian phân công</th>
                                <c:if test="${canManagePartnerStaff && activity.activityStatus == 'UPCOMING'}">
                                    <th scope="col" class="activity-action-column">Thao tác</th>
                                </c:if>
                            </tr>
                            </thead>
                            <tbody>
                            <c:choose>
                                <c:when test="${empty assignedPartnerStaffs}">
                                    <tr>
                                        <td class="empty-state activity-empty-state" colspan="${canManagePartnerStaff && activity.activityStatus == 'UPCOMING' ? 8 : 7}">
                                            <strong>Chưa có nhân sự đối tác nào được phân công phụ trách hoạt động này.</strong>
                                        </td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${assignedPartnerStaffs}" var="aps" varStatus="loop">
                                        <tr>
                                            <td>${loop.index + 1}</td>
                                            <td><strong><c:out value="${aps.partnerStaffCode}"/></strong></td>
                                            <td><c:out value="${aps.partnerStaffName}"/></td>
                                            <td><c:out value="${aps.companyName}" default="—"/></td>
                                            <td><c:out value="${aps.position}" default="—"/></td>
                                            <td class="col-status"><span class="badge status-pill ${st:css(aps.status)}">${st:label(aps.status)}</span></td>
                                            <td><c:out value="${aps.formattedAssignedOn}"/></td>
                                            <c:if test="${canManagePartnerStaff && activity.activityStatus == 'UPCOMING'}">
                                                <td class="activity-action-column">
                                                    <div class="actions activity-actions"><form action="${pageContext.request.contextPath}/activities" method="post" style="display:inline"
                                                          data-confirm="Bạn có chắc chắn muốn gỡ nhân sự đối tác này?">
                                                        <input type="hidden" name="action" value="unassignPartnerStaff">
                                                        <input type="hidden" name="activityId" value="${activity.id}">
                                                        <input type="hidden" name="partnerStaffId" value="${aps.partnerStaffId}">
                                                        <button type="submit" class="button button-sm action-btn action-btn--bulk-reject">
                                                        <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="m19 6-1 14H6L5 6"/><path d="M10 11v5"/><path d="M14 11v5"/></svg></span>
                                                            Gỡ bỏ
                                                        </button>
                                                    </form></div>
                                                </td>
                                            </c:if>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                            </tbody>
                        </table>
                    </div>

                    <c:if test="${canManagePartnerStaff && activity.activityStatus == 'UPCOMING'}">
                        <div style="background: var(--surface-secondary, #f8fafc); border: 1px solid var(--border, #e2e8f0); border-radius: 8px; padding: 1.25rem;">
                            <h4 style="margin: 0 0 0.5rem; font-size: 1rem;">Phân công nhân sự đối tác mới</h4>
                            <p style="margin: 0 0 1rem; font-size: 0.875rem; color: var(--text-muted, #64748b);">
                                Chỉ nhân sự thuộc các công ty đối tác đã chấp nhận tham gia hoạt động mới có thể được phân công; không bao gồm nhân sự đối tác phụ trách chính.
                            </p>
                            <c:choose>
                                <c:when test="${not empty availablePartnerStaffs}">
                                    <form action="${pageContext.request.contextPath}/activities" method="post" style="display: flex; gap: 0.75rem; align-items: center; flex-wrap: wrap;">
                                        <input type="hidden" name="action" value="assignPartnerStaff">
                                        <input type="hidden" name="activityId" value="${activity.id}">
                                        <div class="multi-select-combobox" data-multi-select data-item-label="nhân sự đối tác">
                                            <button type="button" class="multi-select-trigger" aria-expanded="false"
                                                    aria-controls="partner-staff-options">
                                                <span data-selection-label>Chọn nhân sự đối tác để phân công</span>
                                                <span aria-hidden="true">▾</span>
                                            </button>
                                            <div class="multi-select-dropdown" id="partner-staff-options" hidden>
                                                <input class="multi-select-search" type="search"
                                                       placeholder="Tìm nhân sự đối tác..." aria-label="Tìm nhân sự đối tác">
                                                <fieldset class="multi-select-options">
                                                    <legend class="visually-hidden">Chọn nhân sự đối tác</legend>
                                                    <c:forEach items="${availablePartnerStaffs}" var="pst">
                                                        <label class="multi-select-option">
                                                            <input type="checkbox" name="partnerStaffIds" value="${pst.id}">
                                                            <span><c:out value="${pst.code}"/> - <c:out value="${pst.name}"/></span>
                                                        </label>
                                                    </c:forEach>
                                                </fieldset>
                                                <div class="multi-select-empty" hidden>Không tìm thấy nhân sự phù hợp.</div>
                                            </div>
                                            <span class="multi-select-count" aria-live="polite">Chưa chọn nhân sự đối tác</span>
                                        </div>
                                        <button type="submit" class="button button-primary button-sm action-btn"><span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M12 5v14"/><path d="M5 12h14"/></svg></span> Phân công vào hoạt động</button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <p style="margin: 0; font-style: italic; font-size: 0.875rem; color: var(--text-muted, #64748b);">
                                        Không còn nhân sự đối tác nào có thể được phân công.
                                    </p>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:if>
                </section>
            </div>

            <c:if test="${not isStudent}">
                <c:if test="${activity.registrationFormRequired}">
                <!-- TAB 4: ĐĂNG KÝ THAM GIA -->
                <div id="tab-registration" class="activity-tab-pane hub-edge-pane" role="tabpanel" aria-labelledby="btn-tab-registration">
                    <c:if test="${canManageActivities}">
                        <div class="attendance-summary-actions">
                            <c:choose>
                                <c:when test="${not empty registrationForm}">
                                    <a class="button button-primary-outline button-sm"
                                       href="${pageContext.request.contextPath}/registration-forms?action=view&amp;id=${registrationForm.id}">
                                        Quản lý &amp; Cấu hình Đợt đăng ký
                                    </a>
                                </c:when>
                                <c:when test="${activity.approvalStatus == 'APPROVED' and activity.registrationFormRequired}">
                                    <a class="button button-primary-outline button-sm"
                                       href="${pageContext.request.contextPath}/registration-forms?action=view&amp;activityId=${activity.id}">
                                        Quản lý &amp; Cấu hình Đợt đăng ký
                                    </a>
                                </c:when>
                                <c:otherwise>
                                    <button type="button" class="button button-primary-outline button-sm" disabled
                                            title="Chỉ có thể tạo và quản lý đợt đăng ký sau khi hoạt động được duyệt">
                                        Cấu hình Đợt đăng ký
                                    </button>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:if>
                    <div class="attendance-summary">
                        <div class="attendance-summary-item">
                            <span class="attendance-summary-label">Tiêu đề form</span>
                            <strong class="activity-name">
                                <c:choose>
                                    <c:when test="${not empty registrationForm}"><c:out value="${registrationForm.title}"/></c:when>
                                    <c:otherwise><c:out value="${activity.name}"/></c:otherwise>
                                </c:choose>
                            </strong>
                        </div>
                        <div class="attendance-summary-item">
                            <span class="attendance-summary-label">Mở đăng ký</span>
                            <span class="activity-time">
                                <c:choose>
                                    <c:when test="${not empty registrationForm and not empty registrationForm.openAt}"><c:out value="${dt:format(registrationForm.openAt)}"/></c:when>
                                    <c:when test="${not empty registrationForm}">Không giới hạn</c:when>
                                    <c:otherwise>Chưa thiết lập</c:otherwise>
                                </c:choose>
                            </span>
                        </div>
                        <div class="attendance-summary-item">
                            <span class="attendance-summary-label">Đóng đăng ký</span>
                            <span class="activity-time">
                                <c:choose>
                                    <c:when test="${not empty registrationForm and not empty registrationForm.closeAt}"><c:out value="${dt:format(registrationForm.closeAt)}"/></c:when>
                                    <c:when test="${not empty registrationForm}">Không giới hạn</c:when>
                                    <c:otherwise>Chưa thiết lập</c:otherwise>
                                </c:choose>
                            </span>
                        </div>
                        <div class="attendance-summary-item">
                            <span class="attendance-summary-label">Trạng thái đợt đăng ký</span>
                            <c:choose>
                                <c:when test="${not empty registrationForm}">
                                    <span class="badge status-pill ${st:css(registrationForm.status)}">${st:label(registrationForm.status)}</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge status-pill status-gray">Chưa mở</span>
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <div class="attendance-summary-item">
                            <span class="attendance-summary-label">Tiến độ chỉ tiêu</span>
                            <c:choose>
                                <c:when test="${not empty safeRegMaxQuota and safeRegMaxQuota > 0}">
                                    <span class="activity-code"><c:out value="${safeTotalApps}"/> / <c:out value="${safeRegMaxQuota}"/> SV</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="activity-code"><c:out value="${safeTotalApps}"/> Không giới hạn SV</span>
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <div class="attendance-summary-progress">
                            <div class="hub-progress-track hub-progress-track--full" role="progressbar"
                                 aria-valuenow="${safeRegProgressPct}" aria-valuemin="0" aria-valuemax="100">
                                <div class="hub-progress-fill" style="width: ${safeRegProgressPct}%;"></div>
                            </div>
                        </div>
                    </div>

                    <ul class="attendance-counts" aria-label="Thống kê đơn đăng ký">
                        <li class="badge status-pill status-yellow">Chờ xử lý: <strong><c:out value="${safeRegSubmitted}"/></strong></li>
                        <li class="badge status-pill status-green">Đã duyệt: <strong><c:out value="${safeRegApproved}"/></strong></li>
                        <li class="badge status-pill status-red">Từ chối: <strong><c:out value="${safeRegRejected}"/></strong></li>
                        <li class="badge status-pill status-gray">Đã hủy / Rút: <strong><c:out value="${safeRegCancelled}"/></strong></li>
                    </ul>

                    <div class="activity-bulk-toolbar">
                        <span class="activity-bulk-selection">
                            Đơn đăng ký mới nhất (hiển thị tối đa 5 đơn gần nhất trong tổng số <c:out value="${safeTotalApps}"/> đơn đăng ký).
                        </span>
                        <c:if test="${canManageActivities}">
                            <div class="activity-bulk-actions">
                                <c:choose>
                                    <c:when test="${not empty registrationForm}">
                                        <a class="button button-primary button-sm"
                                           href="${pageContext.request.contextPath}/registration-forms?action=view&amp;id=${registrationForm.id}&amp;tab=applications#tab-form-applications">
                                            Xem tất cả đơn đăng ký
                                        </a>
                                    </c:when>
                                    <c:otherwise>
                                        <button type="button" class="button button-primary button-sm" disabled
                                                title="Chưa có đợt đăng ký để xem danh sách đơn">
                                            Xem tất cả đơn đăng ký
                                        </button>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </c:if>
                    </div>

                    <div class="table-wrap activity-table-wrap">
                        <table class="hub-recent-table hub-recent-table--registration">
                            <thead>
                            <tr>
                                <th scope="col" class="col-code">Mã SV</th>
                                <th scope="col" class="col-object-name">Họ tên</th>
                                <th scope="col" class="col-academic">Khoa</th>
                                <th scope="col" class="col-academic">Ngành</th>
                                <th scope="col" class="col-academic col-program">Chương trình đào tạo</th>
                                <th scope="col" class="col-academic col-class">Lớp</th>
                                <th scope="col" class="col-datetime">Thời gian gửi</th>
                                <th scope="col" class="col-status activity-status-column">Trạng thái duyệt</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:choose>
                                <c:when test="${empty recentApplications}">
                                    <tr>
                                        <td class="empty-state activity-empty-state" colspan="8">
                                            <span class="empty-state-mark empty-state-mark--icon" aria-hidden="true">
                                                <svg class="ui-icon" viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
                                            </span>
                                            <strong>Chưa có sinh viên gửi đơn</strong>
                                            <span>Đơn đăng ký sẽ xuất hiện tại đây sau khi sinh viên gửi form.</span>
                                        </td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${recentApplications}" var="appItem">
                                        <tr>
                                            <td class="col-code"><span class="activity-code"><c:out value="${appItem.studentCode}" default="—"/></span></td>
                                            <td class="col-object-name" title="<c:out value='${appItem.studentName}'/>">
                                                <strong class="activity-name" title="<c:out value='${appItem.studentName}'/>"><c:out value="${appItem.studentName}" default="—"/></strong>
                                            </td>
                                            <td class="col-academic" title="<c:out value='${appItem.departmentName}'/>"><c:out value="${appItem.departmentName}" default="—"/></td>
                                            <td class="col-academic" title="<c:out value='${appItem.majorName}'/>"><c:out value="${appItem.majorName}" default="—"/></td>
                                            <td class="col-academic col-program" title="<c:out value='${appItem.academicProgramName}'/>"><c:out value="${appItem.academicProgramName}" default="—"/></td>
                                            <td class="col-academic col-class" title="<c:out value='${appItem.classCode}'/>"><c:out value="${appItem.classCode}" default="—"/></td>
                                            <td class="col-datetime"><span class="activity-time"><c:out value="${not empty appItem.submittedAt ? dt:format(appItem.submittedAt) : '—'}"/></span></td>
                                            <td class="col-status activity-status-column">
                                                <c:choose>
                                                    <c:when test="${appItem.status eq 'APPROVED'}"><span class="badge status-pill status-green">Đã duyệt</span></c:when>
                                                    <c:when test="${appItem.status eq 'REJECTED'}"><span class="badge status-pill status-red">Từ chối</span></c:when>
                                                    <c:when test="${appItem.status eq 'CANCELLED'}"><span class="badge status-pill status-gray">Sinh viên đã hủy</span></c:when>
                                                    <c:when test="${appItem.status eq 'WITHDRAWN'}"><span class="badge status-pill status-blue">Sinh viên đã rút đơn</span></c:when>
                                                    <c:otherwise><span class="badge status-pill status-yellow">Chờ xử lý</span></c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                            </tbody>
                        </table>
                    </div>

                    <c:if test="${not empty registrationForm}">
                        <div class="activity-pagination">
                            <span class="activity-pagination-summary">
                                Hiển thị <c:out value="${not empty recentApplications ? recentApplications.size() : 0}"/> trong <c:out value="${safeTotalApps}"/> đơn đăng ký
                            </span>
                            <a class="activity-action-link"
                               href="${pageContext.request.contextPath}/registration-forms?action=view&amp;id=${registrationForm.id}&amp;tab=applications#tab-form-applications">
                                Xem tất cả <c:out value="${safeTotalApps}"/> đơn đăng ký →
                            </a>
                        </div>
                    </c:if>
                </div>
                </c:if>

                <!-- TAB 5: ĐIỂM DANH SINH VIÊN -->
                <div id="tab-attendance" class="activity-tab-pane hub-edge-pane" role="tabpanel" aria-labelledby="btn-tab-attendance">
                    <div class="attendance-summary">
                        <div class="attendance-summary-item">
                            <span class="attendance-summary-label">Thời gian bắt đầu</span>
                            <span class="activity-time">
                                <c:choose>
                                    <c:when test="${not empty activity.startTime}">${dt:format(activity.startTime)}</c:when>
                                    <c:otherwise>Chưa xác định</c:otherwise>
                                </c:choose>
                            </span>
                        </div>
                        <div class="attendance-summary-item">
                            <span class="attendance-summary-label">Thời gian kết thúc</span>
                            <span class="activity-time">
                                <c:choose>
                                    <c:when test="${not empty activity.endTime}">${dt:format(activity.endTime)}</c:when>
                                    <c:otherwise>Chưa xác định</c:otherwise>
                                </c:choose>
                            </span>
                        </div>
                        <div class="attendance-summary-item">
                            <span class="attendance-summary-label">Số buổi điểm danh</span>
                            <span class="activity-code"><c:out value="${activity.attendanceSessionCount > 0 ? activity.attendanceSessionCount : 1}"/> buổi</span>
                        </div>
                        <div class="attendance-summary-item">
                            <span class="attendance-summary-label">Trạng thái hoạt động</span>
                            <span class="badge status-pill ${st:css(activity.activityStatus)}">${st:label(activity.activityStatus)}</span>
                        </div>
                        <div class="attendance-summary-item">
                            <span class="attendance-summary-label">Tỷ lệ tham gia</span>
                            <span class="activity-code"><c:out value="${safeActualAttended}"/> / <c:out value="${safeTotalParticipants}"/> sinh viên (<c:out value="${safeAttendanceRatePct}"/>%)</span>
                        </div>
                        <div class="attendance-summary-progress">
                            <div class="hub-progress-track hub-progress-track--full" role="progressbar"
                                 aria-valuenow="${safeAttendanceRatePct}" aria-valuemin="0" aria-valuemax="100">
                                <div class="hub-progress-fill" style="width: ${safeAttendanceRatePct}%;"></div>
                            </div>
                        </div>
                    </div>

                    <ul class="attendance-counts" aria-label="Thống kê điểm danh">
                        <c:choose>
                            <c:when test="${not empty participantStatuses}">
                                <c:forEach var="status" items="${participantStatuses}">
                                    <li class="badge status-pill ${st:css(status)}">${st:label(status)}: <strong><c:out value="${not empty statusCounts[status] ? statusCounts[status] : 0}"/></strong></li>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <li class="badge status-pill status-gray">Chưa điểm danh: <strong>0</strong></li>
                                <li class="badge status-pill status-green">Có mặt: <strong>0</strong></li>
                                <li class="badge status-pill status-red">Vắng mặt: <strong>0</strong></li>
                                <li class="badge status-pill status-yellow">Vào trễ: <strong>0</strong></li>
                                <li class="badge status-pill status-yellow">Về sớm: <strong>0</strong></li>
                            </c:otherwise>
                        </c:choose>
                    </ul>

                    <div class="activity-bulk-toolbar">
                        <span class="activity-bulk-selection">
                            Lịch sử điểm danh gần nhất (hiển thị tối đa 5 sinh viên trong tổng số <c:out value="${safeTotalParticipants}"/> sinh viên tham gia).
                        </span>
                        <c:if test="${canAccessAttendance}">
                            <div class="activity-bulk-actions">
                                <c:choose>
                                    <c:when test="${canOpenAttendancePage}">
                                        <a class="button button-primary button-sm"
                                           href="${pageContext.request.contextPath}/attendance?activityId=${activity.id}">
                                            Điểm danh Sinh viên
                                        </a>
                                        <a class="button button-primary-outline button-sm"
                                           href="${pageContext.request.contextPath}/participation-grading?activityId=${activity.id}">
                                            Chấm điểm tham gia
                                        </a>
                                    </c:when>
                                    <c:otherwise>
                                        <button type="button" class="button button-primary button-sm" disabled
                                                title="Chỉ có thể điểm danh khi hoạt động Đã duyệt / Đang diễn ra">
                                            Điểm danh Sinh viên
                                        </button>
                                        <button type="button" class="button button-primary-outline button-sm" disabled
                                                title="Chỉ có thể chấm điểm tham gia khi hoạt động Đã duyệt / Đang diễn ra">
                                            Chấm điểm tham gia
                                        </button>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </c:if>
                    </div>

                    <div class="table-wrap activity-table-wrap">
                        <table class="hub-recent-table hub-recent-table--attendance">
                            <thead>
                            <tr>
                                <th scope="col" class="col-code">Mã SV</th>
                                <th scope="col" class="col-object-name">Họ tên</th>
                                <th scope="col" class="col-academic">Khoa</th>
                                <th scope="col" class="col-academic">Ngành</th>
                                <th scope="col" class="col-academic col-program">Chương trình đào tạo</th>
                                <th scope="col" class="col-academic col-class">Lớp</th>
                                <th scope="col" class="col-status activity-status-column">Trạng thái điểm danh</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:choose>
                                <c:when test="${empty recentParticipants}">
                                    <tr>
                                        <td class="empty-state activity-empty-state" colspan="7">
                                            <span class="empty-state-mark empty-state-mark--icon" aria-hidden="true">
                                                <svg class="ui-icon" viewBox="0 0 24 24"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                                            </span>
                                            <strong>Chưa có sinh viên tham gia</strong>
                                            <span>Hoạt động này chưa có sinh viên nào trong danh sách tham gia.</span>
                                        </td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${recentParticipants}" var="pItem">
                                        <tr>
                                            <td class="col-code"><span class="activity-code"><c:out value="${pItem.studentCode}" default="—"/></span></td>
                                            <td class="col-object-name" title="<c:out value='${pItem.studentName}'/>">
                                                <strong class="activity-name" title="<c:out value='${pItem.studentName}'/>"><c:out value="${pItem.studentName}" default="—"/></strong>
                                            </td>
                                            <td class="col-academic" title="<c:out value='${pItem.departmentName}'/>"><c:out value="${pItem.departmentName}" default="—"/></td>
                                            <td class="col-academic" title="<c:out value='${pItem.majorName}'/>"><c:out value="${pItem.majorName}" default="—"/></td>
                                            <td class="col-academic col-program" title="<c:out value='${pItem.academicProgramName}'/>"><c:out value="${pItem.academicProgramName}" default="—"/></td>
                                            <td class="col-academic col-class" title="<c:out value='${pItem.mainClassCode}'/>"><c:out value="${pItem.mainClassCode}" default="—"/></td>
                                            <td class="col-status activity-status-column">
                                                <span class="badge status-pill ${st:css(pItem.attendanceStatus)}">${st:label(pItem.attendanceStatus)}</span>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                            </tbody>
                        </table>
                    </div>

                    <c:if test="${canOpenAttendancePage}">
                        <div class="activity-pagination">
                            <span class="activity-pagination-summary">
                                Hiển thị <c:out value="${not empty recentParticipants ? recentParticipants.size() : 0}"/> trong <c:out value="${safeTotalParticipants}"/> sinh viên
                            </span>
                            <a class="activity-action-link"
                               href="${pageContext.request.contextPath}/attendance?activityId=${activity.id}">
                                Xem tất cả <c:out value="${safeTotalParticipants}"/> sinh viên →
                            </a>
                        </div>
                    </c:if>
                </div>
            </c:if>
        </div>

        <div class="detail-actions activity-detail-actions">
            <a class="button" href="${pageContext.request.contextPath}/activities">Đóng</a>
            <%-- DRAFT / REJECTED: gửi duyệt --%>
            <c:if test="${activity.activityStatus == 'UPCOMING' && (activity.approvalStatus == 'DRAFT' || activity.approvalStatus == 'REJECTED')}">
                <form action="${pageContext.request.contextPath}/activity/approval" method="post" style="display:inline">
                    <input type="hidden" name="id" value="${activity.id}">
                    <button type="submit" name="action" value="submit" class="button button-send-approval button-primary">Gửi duyệt</button>
                </form>
            </c:if>

            <%-- PENDING: duyệt / từ chối --%>
            <c:if test="${canApproveActivities && activity.activityStatus == 'UPCOMING' && activity.approvalStatus == 'PENDING'}">
                <form action="${pageContext.request.contextPath}/activity/approval" method="post" style="display:inline">
                    <input type="hidden" name="id" value="${activity.id}">
                    <button type="submit" name="action" value="approve" class="button button-approve button-primary">Duyệt</button>
                    <button type="submit" name="action" value="reject" class="button button-reject">Từ chối</button>
                </form>
            </c:if>

            <%-- APPROVED (chỉ khi hoạt động chưa diễn ra): hủy duyệt --%>
            <c:if test="${canApproveActivities && activity.approvalStatus == 'APPROVED' && activity.activityStatus == 'UPCOMING'}">
                <form action="${pageContext.request.contextPath}/activity/approval" method="post" style="display:inline">
                    <input type="hidden" name="id" value="${activity.id}">
                    <button type="submit" name="action" value="revokeApproval" class="button button-revoke"
                            data-confirm="Hủy duyệt và đưa hoạt động về trạng thái chờ duyệt?">Hủy duyệt</button>
                </form>
            </c:if>

            <%-- REJECTED: hủy từ chối --%>
            <c:if test="${canApproveActivities && activity.activityStatus == 'UPCOMING' && activity.approvalStatus == 'REJECTED'}">
                <form action="${pageContext.request.contextPath}/activity/approval" method="post" style="display:inline">
                    <input type="hidden" name="id" value="${activity.id}">
                    <button type="submit" name="action" value="revokeRejection" class="button button-revoke"
                            data-confirm="Hủy từ chối và đưa hoạt động về trạng thái chờ duyệt?">Hủy từ chối</button>
                </form>
            </c:if>
        </div>
    </section>
        </main>
    </div>
</div>
<dialog id="activity-message-modal" class="activity-message-modal">
    <h3 id="activity-message-modal-title" style="margin-top: 0;">Lời nhắn</h3>
    <p id="activity-message-modal-body" style="white-space: pre-wrap; word-break: break-word;"></p>
    <div style="text-align: right;"><button type="button" class="button" id="activity-message-modal-close">Đóng</button></div>
</dialog>
<script src="${pageContext.request.contextPath}/assets/js/activity.js?v=20261107-6"></script>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>
