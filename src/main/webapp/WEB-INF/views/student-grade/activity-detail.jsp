<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://swp391.g1/status" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="student-grades" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>EAMS | Chi tiết điểm một hoạt động</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261109-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/student-grade.css?v=20261111-2">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-list-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> CHI TIẾT ĐẦU ĐIỂM HOẠT ĐỘNG</span>
                    <h1>Chi tiết điểm một hoạt động</h1>
                    <p>Xem cụ thể căn cứ tính điểm, nhận xét/feedback và người chấm của từng đầu điểm đối với sinh viên trong một hoạt động.</p>
                </div>
            </header>

            <div class="activity-page-back">
                <a class="button button-back"
                   href="${pageContext.request.contextPath}/student-grades?action=studentDetail<c:if test='${not empty selectedStudent}'>&amp;studentId=${selectedStudent.studentId}</c:if>">
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5"/><path d="m12 19-7-7 7-7"/></svg>
                    Quay lại lịch sử điểm của sinh viên
                </a>
                <c:if test="${not isStudent and not empty gradeDetail}">
                    <div style="display:flex; gap:8px; flex-wrap:wrap;">
                        <a class="button button-outline button-sm"
                           href="${pageContext.request.contextPath}/attendance?activityId=${gradeDetail.activityId}">
                            Đi tới trang Điểm danh HĐ này →
                        </a>
                        <a class="button button-outline button-sm"
                           href="${pageContext.request.contextPath}/participation-grading?activityId=${gradeDetail.activityId}">
                            Đi tới trang Chấm điểm tham gia HĐ này →
                        </a>
                    </div>
                </c:if>
            </div>

            <section class="card activity-table-card" aria-labelledby="activity-grade-detail-title">
                <div class="activity-table-heading">
                    <div>
                        <h2 id="activity-grade-detail-title">Chi tiết các đầu điểm hoạt động</h2>
                        <p>
                            <c:choose>
                                <c:when test="${not empty gradeDetail}">
                                    <c:out value="${gradeDetail.activityCode}"/> – <c:out value="${gradeDetail.activityName}"/>
                                </c:when>
                                <c:otherwise>Chọn sinh viên và hoạt động để xem chi tiết đầu điểm</c:otherwise>
                            </c:choose>
                        </p>
                    </div>
                </div>

                <%-- 1. Bộ chọn Sinh viên & Hoạt động --%>
                <form class="grade-selector-bar" action="${pageContext.request.contextPath}/student-grades" method="get">
                    <input type="hidden" name="action" value="activityDetail">
                    <c:choose>
                        <c:when test="${not isStudent}">
                            <div class="grade-selector-field">
                                <label for="detail-student-selector">Chọn Sinh viên</label>
                                <select id="detail-student-selector" name="studentId" onchange="this.form.activityId.value=''; this.form.submit();">
                                    <c:forEach var="st" items="${allStudents}">
                                        <option value="${st.studentId}" ${selectedStudentId == st.studentId ? 'selected' : ''}>
                                            <c:out value="${st.studentCode}"/> – <c:out value="${st.studentName}"/><c:if test="${not empty st.mainClassCode}"> (<c:out value="${st.mainClassCode}"/>)</c:if>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <input type="hidden" name="studentId" value="${selectedStudentId}">
                        </c:otherwise>
                    </c:choose>

                    <div class="grade-selector-field">
                        <label for="detail-activity-selector">Chọn Hoạt động (các hoạt động sinh viên có tên)</label>
                        <select id="detail-activity-selector" name="activityId" onchange="this.form.submit()">
                            <c:choose>
                                <c:when test="${empty studentActivities}">
                                    <option value="">-- Sinh viên chưa tham gia hoạt động nào --</option>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach var="act" items="${studentActivities}">
                                        <option value="${act.activityId}" ${selectedActivityId == act.activityId ? 'selected' : ''}>
                                            <c:out value="${act.activityCode}"/> – <c:out value="${act.activityName}"/> (${st:label(act.attendanceStatus)})
                                        </option>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </select>
                    </div>
                </form>

                <%-- 2. Thông tin tổng quan --%>
                <c:if test="${not empty selectedStudent}">
                    <div class="grade-detail-overview">
                        <div class="grade-detail-overview-item">
                            <span>Mã sinh viên</span>
                            <strong><c:out value="${selectedStudent.studentCode}" default="—"/></strong>
                        </div>
                        <div class="grade-detail-overview-item">
                            <span>Họ tên sinh viên</span>
                            <strong><c:out value="${selectedStudent.studentName}" default="—"/></strong>
                        </div>
                        <div class="grade-detail-overview-item">
                            <span>Khoa / Lớp</span>
                            <strong><c:out value="${selectedStudent.departmentName}" default="—"/> – <c:out value="${selectedStudent.mainClassCode}" default="—"/></strong>
                        </div>
                        <c:if test="${not empty gradeDetail}">
                            <div class="grade-detail-overview-item">
                                <span>Cấp tổ chức (H<sub>cấp tổ chức</sub>)</span>
                                <strong><c:out value="${gradeDetail.organizationLevelName}" default="Mặc định"/> (x<c:out value="${gradeDetail.formattedOrgLevelCoefficient}"/>)</strong>
                            </div>
                            <div class="grade-detail-overview-item">
                                <span>Hệ số đăng ký (H<sub>đăng ký trước</sub>)</span>
                                <strong><c:out value="${gradeDetail.registrationTypeLabel}"/></strong>
                            </div>
                            <div class="grade-detail-overview-item">
                                <span>Điểm tích lũy HĐ (D<sub>TL</sub>)</span>
                                <strong>
                                    <span class="grade-pill ${gradeDetail.accumulatedScore < 0 ? 'grade-pill--minus' : (gradeDetail.accumulatedScore > 0 ? 'grade-pill--plus' : 'grade-pill--neutral')}">
                                        <c:out value="${gradeDetail.accumulatedScore > 0 ? '+' : ''}${gradeDetail.formattedAccumulatedScore}"/> điểm
                                    </span>
                                </strong>
                            </div>
                        </c:if>
                    </div>
                </c:if>

                <c:if test="${not empty gradeDetail}">
                    <div class="grade-formula-banner">
                        <span>
                            <strong>Công thức áp dụng:</strong>
                            <code>D_TG = D_điểm_danh + D_đánh_giá = <c:out value="${gradeDetail.formattedParticipationScore}"/></code>
                            •
                            <code>ĐTB = <c:out value="${gradeDetail.formattedAverageScore}"/></code>
                            •
                            <code>D_TL = ĐTB × <c:out value="${gradeDetail.formattedOrgLevelCoefficient}"/> × <c:out value="${gradeDetail.formattedEarlyRegCoefficient}"/> = <c:out value="${gradeDetail.formattedAccumulatedScore}"/></code>
                        </span>
                        <span class="badge status-pill ${st:css(gradeDetail.attendanceStatus)}">${st:label(gradeDetail.attendanceStatus)}</span>
                    </div>
                </c:if>

                <%-- 3. Bảng chi tiết đầu điểm --%>
                <div class="table-wrap activity-table-wrap">
                    <table>
                        <thead>
                        <tr>
                            <th scope="col" style="width: 240px;">Đầu điểm</th>
                            <th scope="col" style="width: 160px; text-align: center;">Số điểm</th>
                            <th scope="col">Nhận xét / Feedback</th>
                            <th scope="col" style="width: 240px;">Người nhận xét</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:choose>
                            <c:when test="${empty gradeDetail}">
                                <tr>
                                    <td class="empty-state activity-empty-state" colspan="4">
                                        <span class="empty-state-mark" aria-hidden="true">✓</span>
                                        <strong>Chưa có dữ liệu đầu điểm hoạt động</strong>
                                        <span>Hãy chọn sinh viên và hoạt động từ bộ lọc phía trên.</span>
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <%-- Đầu điểm 1: Điểm danh --%>
                                <tr>
                                    <td>
                                        <strong>Điểm danh chuyên cần</strong>
                                        <div class="text-muted" style="font-size: .78rem;">
                                            Có mặt: <strong><c:out value="${gradeDetail.attendedSessionCount}"/>/<c:out value="${gradeDetail.totalSessions}"/></strong> buổi
                                        </div>
                                    </td>
                                    <td style="text-align: center;">
                                        <c:choose>
                                            <c:when test="${gradeDetail.attendanceScore > 0}">
                                                <span class="grade-pill grade-pill--plus"><c:out value="${gradeDetail.formattedAttendanceScore}"/> / 5.0</span>
                                            </c:when>
                                            <c:when test="${gradeDetail.attendanceScore < 0}">
                                                <span class="grade-pill grade-pill--minus"><c:out value="${gradeDetail.formattedAttendanceScore}"/></span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="grade-pill grade-pill--neutral">0.0 / 5.0</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${gradeDetail.present}">
                                                Sinh viên có mặt <strong><c:out value="${gradeDetail.attendedSessionCount}"/>/<c:out value="${gradeDetail.totalSessions}"/></strong> buổi điểm danh. Điểm chuyên cần trung bình các buổi: <strong><c:out value="${gradeDetail.formattedAttendanceScore}"/> điểm</strong>.
                                            </c:when>
                                            <c:when test="${gradeDetail.attendanceStatus == 'ABSENT'}">
                                                Sinh viên vắng mặt (0/<c:out value="${gradeDetail.totalSessions}"/> buổi có mặt). Điểm chuyên cần trung bình: <strong><c:out value="${gradeDetail.formattedAttendanceScore}"/> điểm</strong>.
                                            </c:when>
                                            <c:otherwise>
                                                Chưa thực hiện điểm danh (<c:out value="${gradeDetail.totalSessions}"/> buổi).
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:out value="${not empty gradeDetail.attendanceMarkedBy ? gradeDetail.attendanceMarkedBy : 'Hệ thống / NV Điểm danh'}"/>
                                        <c:if test="${not empty gradeDetail.attendanceMarkedAt}">
                                            <div class="text-muted" style="font-size: .75rem;">${dt:format(gradeDetail.attendanceMarkedAt)}</div>
                                        </c:if>
                                    </td>
                                </tr>

                                <%-- Đầu điểm 2: Đánh giá tham gia --%>
                                <tr>
                                    <td><strong>Đánh giá tham gia</strong></td>
                                    <td style="text-align: center;">
                                        <c:choose>
                                            <c:when test="${not gradeDetail.present}">
                                                <span class="grade-pill grade-pill--neutral">0.0 / 5.0</span>
                                            </c:when>
                                            <c:when test="${not empty gradeDetail.participationEvalScore}">
                                                <span class="grade-pill grade-pill--plus"><c:out value="${gradeDetail.formattedEvalScore}"/> / 5.0</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="grade-pill grade-pill--neutral">Chưa chấm / 5.0</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty gradeDetail.participationFeedback}">
                                                <c:out value="${gradeDetail.participationFeedback}"/>
                                            </c:when>
                                            <c:when test="${not gradeDetail.present}">
                                                <span class="text-muted">Không áp dụng do sinh viên chưa có mặt tại hoạt động.</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="text-muted">Chưa có nhận xét đánh giá tham gia.</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty gradeDetail.participationGradedBy}">
                                                <c:out value="${gradeDetail.participationGradedBy}"/>
                                                <c:if test="${not empty gradeDetail.participationGradedAt}">
                                                    <div class="text-muted" style="font-size: .75rem;">${dt:format(gradeDetail.participationGradedAt)}</div>
                                                </c:if>
                                            </c:when>
                                            <c:otherwise><span class="text-muted">—</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>

                                <%-- Đầu điểm 3: Bài thu hoạch --%>
                                <tr>
                                    <td><strong>Bài thu hoạch</strong></td>
                                    <td style="text-align: center;">
                                        <c:choose>
                                            <c:when test="${not gradeDetail.summaryRequired}">
                                                <span class="grade-pill grade-pill--neutral">-</span>
                                            </c:when>
                                            <c:when test="${not empty gradeDetail.summaryScore}">
                                                <span class="grade-pill grade-pill--plus"><c:out value="${gradeDetail.formattedSummaryScore}"/> / 10.0</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="grade-pill grade-pill--neutral">Chưa chấm / 10.0</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not gradeDetail.summaryRequired}">
                                                <span class="text-muted">Hoạt động không yêu cầu nộp bài thu hoạch.</span>
                                            </c:when>
                                            <c:when test="${not empty gradeDetail.summaryFeedback}">
                                                <c:out value="${gradeDetail.summaryFeedback}"/>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="text-muted">Chưa có điểm / nhận xét bài thu hoạch.</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:out value="${not empty gradeDetail.summaryGradedBy ? gradeDetail.summaryGradedBy : '—'}"/>
                                    </td>
                                </tr>
                            </c:otherwise>
                        </c:choose>
                        </tbody>
                    </table>
                </div>
            </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>
