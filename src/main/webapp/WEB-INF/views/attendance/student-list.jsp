<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://swp391.g1/status" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="attendance" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>EAMS | Kết quả điểm danh</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261022-4">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css?v=3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/attendance.css?v=10">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-list-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> KẾT QUẢ ĐIỂM DANH</span>
                    <h1>Kết quả điểm danh của tôi</h1>
                    <p>Theo dõi danh sách hoạt động ngoại khóa tham gia và kết quả điểm danh của bạn.</p>
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

            <c:if test="${not empty notParticipatingActivity}">
                <div class="alert alert-error activity-alert">
                    Bạn hiện không có trong danh sách tham gia của hoạt động
                    <strong><c:out value="${notParticipatingActivity.code}"/> – <c:out value="${notParticipatingActivity.name}"/></strong>.
                </div>
            </c:if>

            <section class="card activity-table-card" aria-labelledby="student-attendance-table-title">
                <div class="activity-table-heading">
                    <div>
                        <h2 id="student-attendance-table-title">Hoạt động tham gia &amp; Kết quả điểm danh</h2>
                        <p>
                            Sinh viên: <strong><c:out value="${student.code}"/> – <c:out value="${student.name}"/></strong>
                            <c:if test="${not empty student.mainClassCode}">
                                (<c:out value="${student.mainClassCode}"/>)
                            </c:if>
                        </p>
                    </div>
                    <div class="activity-heading-actions">
                        <span class="activity-count">
                            <c:choose>
                                <c:when test="${filtered}"><c:out value="${filteredRecordCount}"/> / <c:out value="${totalRecordCount}"/> hoạt động</c:when>
                                <c:otherwise><c:out value="${totalRecordCount}"/> hoạt động</c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                </div>

                <div class="attendance-summary">
                    <div class="attendance-summary-item">
                        <span class="attendance-summary-label">Mã sinh viên</span>
                        <span class="activity-code"><c:out value="${student.code}" default="—"/></span>
                    </div>
                    <div class="attendance-summary-item">
                        <span class="attendance-summary-label">Họ và tên</span>
                        <strong><c:out value="${student.name}" default="—"/></strong>
                    </div>
                    <c:if test="${not empty student.mainClassCode}">
                        <div class="attendance-summary-item">
                            <span class="attendance-summary-label">Lớp hành chính</span>
                            <span><c:out value="${student.mainClassCode}"/></span>
                        </div>
                    </c:if>
                    <ul class="attendance-counts" aria-label="Thống kê kết quả điểm danh">
                        <c:forEach var="status" items="${participantStatuses}">
                            <li class="badge status-pill ${st:css(status)}">
                                ${st:label(status)}: <strong><c:out value="${statusCounts[status]}"/></strong>
                            </li>
                        </c:forEach>
                    </ul>
                </div>

                <c:if test="${not empty selectedRecord}">
                    <div class="attendance-selected-banner">
                        <div class="attendance-selected-banner-main">
                            <span class="attendance-summary-label">Kết quả điểm danh trong hoạt động đang chọn</span>
                            <div class="attendance-selected-banner-title">
                                <span class="activity-code"><c:out value="${selectedRecord.activityCode}"/></span>
                                <strong><c:out value="${selectedRecord.activityName}"/></strong>
                                <span class="badge status-pill ${st:css(selectedRecord.activityStatus)}">${st:label(selectedRecord.activityStatus)}</span>
                            </div>
                            <span class="attendance-selected-banner-meta">
                                Thời gian:
                                <c:choose>
                                    <c:when test="${not empty selectedRecord.startTime}">
                                        ${dt:format(selectedRecord.startTime)}
                                        <c:if test="${not empty selectedRecord.endTime}"> đến ${dt:format(selectedRecord.endTime)}</c:if>
                                    </c:when>
                                    <c:otherwise>Chưa xác định</c:otherwise>
                                </c:choose>
                                <c:if test="${not empty selectedRecord.address}">
                                    • Địa điểm: <c:out value="${selectedRecord.address}"/>
                                </c:if>
                            </span>
                        </div>
                        <div class="attendance-selected-banner-status">
                            <span class="attendance-summary-label">Trạng thái điểm danh</span>
                            <span class="badge status-pill ${st:css(selectedRecord.attendanceStatus)}">${st:label(selectedRecord.attendanceStatus)}</span>
                        </div>
                    </div>
                </c:if>

                <details class="activity-filter-panel" ${filtered ? 'open' : ''}>
                    <summary class="activity-filter-toggle">
                        <span>Tìm kiếm &amp; Bộ lọc</span>
                        <c:if test="${activeFilterCount > 0}">
                            <span class="activity-filter-badge"><c:out value="${activeFilterCount}"/></span>
                        </c:if>
                    </summary>
                    <form class="activity-filter" action="${pageContext.request.contextPath}/attendance" method="get">
                        <input type="hidden" name="pageSize" value="${pageSize}">
                        <div class="activity-filter-field activity-filter-search">
                            <label for="filter-keyword">Tìm kiếm</label>
                            <input id="filter-keyword" type="search" name="keyword" value="<c:out value='${keyword}'/>"
                                   placeholder="Mã hoặc tên hoạt động">
                        </div>
                        <div class="activity-filter-field">
                            <label for="filter-semester">Học kỳ</label>
                            <select id="filter-semester" name="semesterId">
                                <option value="">Tất cả</option>
                                <c:forEach var="semester" items="${semesters}">
                                    <option value="${semester.id}" ${selectedSemesterId == semester.id ? 'selected' : ''}>
                                        <c:out value="${semester.name}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="activity-filter-field">
                            <label for="filter-type">Loại hoạt động</label>
                            <select id="filter-type" name="activityTypeId">
                                <option value="">Tất cả</option>
                                <c:forEach var="type" items="${activityTypes}">
                                    <option value="${type.id}" ${selectedActivityTypeId == type.id ? 'selected' : ''}>
                                        <c:out value="${type.name}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="activity-filter-field">
                            <label for="filter-activity-status">Trạng thái hoạt động</label>
                            <select id="filter-activity-status" name="activityStatus">
                                <option value="">Tất cả</option>
                                <c:forEach var="status" items="${activityStatuses}">
                                    <option value="${status}" ${selectedActivityStatus == status.name() ? 'selected' : ''}>
                                        ${st:label(status)}
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="activity-filter-field">
                            <label for="filter-attendance-status">Trạng thái điểm danh</label>
                            <select id="filter-attendance-status" name="attendanceStatus">
                                <option value="">Tất cả</option>
                                <c:forEach var="status" items="${participantStatuses}">
                                    <option value="${status}" ${selectedAttendanceStatus == status.name() ? 'selected' : ''}>
                                        ${st:label(status)}
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="activity-filter-buttons">
                            <button type="submit" class="button button-primary button-sm">Tìm kiếm &amp; Lọc</button>
                            <c:if test="${filtered}">
                                <a class="button button-sm" href="${pageContext.request.contextPath}/attendance?pageSize=${pageSize}">Xóa Bộ lọc</a>
                            </c:if>
                        </div>
                    </form>
                </details>

                <div class="table-wrap activity-table-wrap">
                    <table>
                        <thead>
                        <tr>
                            <th scope="col">Mã hoạt động</th>
                            <th scope="col">Tên hoạt động</th>
                            <th scope="col">Học kỳ</th>
                            <th scope="col">Loại hoạt động</th>
                            <th scope="col">Đơn vị phụ trách chính</th>
                            <th scope="col">Thời gian</th>
                            <th scope="col">Địa điểm</th>
                            <th scope="col" class="activity-run-status"><span>Trạng thái</span><span>hoạt động</span></th>
                            <th scope="col" class="activity-status-column"><span>Kết quả</span><span>điểm danh</span></th>
                            <th scope="col" class="activity-action-column">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:choose>
                            <c:when test="${empty records && filtered}">
                                <tr>
                                    <td class="empty-state activity-empty-state" colspan="10">
                                        <span class="empty-state-mark" aria-hidden="true">+</span>
                                        <strong>Không tìm thấy hoạt động phù hợp</strong>
                                        <span>Hãy thử thay đổi từ khóa hoặc bộ lọc.</span>
                                    </td>
                                </tr>
                            </c:when>
                            <c:when test="${empty records}">
                                <tr>
                                    <td class="empty-state activity-empty-state" colspan="10">
                                        <span class="empty-state-mark" aria-hidden="true">✓</span>
                                        <strong>Chưa có hoạt động tham gia</strong>
                                        <span>Bạn hiện chưa có trong danh sách tham gia của hoạt động ngoại khóa nào.</span>
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="item" items="${records}">
                                    <tr class="${selectedActivityId == item.activityId ? 'attendance-row-selected' : ''}">
                                        <td><span class="activity-code"><c:out value="${item.activityCode}"/></span></td>
                                        <td><strong class="activity-name"><c:out value="${item.activityName}"/></strong></td>
                                        <td><span class="table-primary-text"><c:out value="${item.semesterName}" default="—"/></span></td>
                                        <td><c:out value="${item.activityTypeName}" default="—"/></td>
                                        <td><c:out value="${item.responsibleDepartmentName}" default="—"/></td>
                                        <td class="activity-time">
                                            <c:choose>
                                                <c:when test="${not empty item.startTime}">
                                                    ${dt:format(item.startTime)}
                                                    <c:if test="${not empty item.endTime}"><span>đến</span>${dt:format(item.endTime)}</c:if>
                                                </c:when>
                                                <c:otherwise>Chưa xác định</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td><c:out value="${item.address}" default="Chưa cập nhật"/></td>
                                        <td class="activity-run-status">
                                            <span class="badge status-pill ${st:css(item.activityStatus)}">${st:label(item.activityStatus)}</span>
                                        </td>
                                        <td class="activity-status-column">
                                            <span class="badge status-pill ${st:css(item.attendanceStatus)}">${st:label(item.attendanceStatus)}</span>
                                        </td>
                                        <td class="activity-action-column">
                                            <div class="actions activity-actions">
                                                <a class="activity-action-link action-btn action-btn--view"
                                                   href="${pageContext.request.contextPath}/activities?action=detail&amp;id=${item.activityId}">
                                                    <span class="action-btn__icon" aria-hidden="true">
                                                        <svg viewBox="0 0 24 24"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg>
                                                    </span>
                                                    Chi tiết
                                                </a>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                        </tbody>
                    </table>
                </div>

                <c:if test="${filteredRecordCount > 0}">
                    <c:set var="pageBaseUrl" value="${pageContext.request.contextPath}/attendance?pageSize=${pageSize}${filterQuery}"/>
                    <div class="activity-pagination">
                        <form class="activity-page-size" action="${pageContext.request.contextPath}/attendance" method="get">
                            <input type="hidden" name="page" value="1">
                            <input type="hidden" name="keyword" value="<c:out value='${keyword}'/>">
                            <input type="hidden" name="semesterId" value="${selectedSemesterId > 0 ? selectedSemesterId : ''}">
                            <input type="hidden" name="activityTypeId" value="${selectedActivityTypeId > 0 ? selectedActivityTypeId : ''}">
                            <input type="hidden" name="activityStatus" value="<c:out value='${selectedActivityStatus}'/>">
                            <input type="hidden" name="attendanceStatus" value="<c:out value='${selectedAttendanceStatus}'/>">
                            <label for="student-attendance-page-size">Hiển thị</label>
                            <select id="student-attendance-page-size" name="pageSize" onchange="this.form.submit()">
                                <c:forEach var="size" items="${pageSizes}">
                                    <option value="${size}" ${pageSize == size ? 'selected' : ''}>${size}</option>
                                </c:forEach>
                            </select>
                            <span>bản ghi mỗi trang</span>
                        </form>
                        <span class="activity-pagination-summary">
                            Hiển thị <c:out value="${firstRecordNumber}"/>–<c:out value="${lastRecordNumber}"/>
                            trong <c:out value="${filteredRecordCount}"/> hoạt động
                        </span>
                        <c:if test="${totalPages > 1}">
                            <nav class="activity-pagination-nav" aria-label="Phân trang kết quả điểm danh">
                                <c:choose>
                                    <c:when test="${currentPage > 1}">
                                        <a class="activity-page-link" href="<c:out value='${pageBaseUrl}&page=${currentPage - 1}'/>"
                                           aria-label="Trang trước">‹</a>
                                    </c:when>
                                    <c:otherwise><span class="activity-page-link is-disabled" aria-hidden="true">‹</span></c:otherwise>
                                </c:choose>
                                <c:forEach var="pageNumber" begin="1" end="${totalPages}">
                                    <a class="activity-page-link${pageNumber == currentPage ? ' is-active' : ''}"
                                       href="<c:out value='${pageBaseUrl}&page=${pageNumber}'/>"
                                       aria-current="${pageNumber == currentPage ? 'page' : ''}"><c:out value="${pageNumber}"/></a>
                                </c:forEach>
                                <c:choose>
                                    <c:when test="${currentPage < totalPages}">
                                        <a class="activity-page-link" href="<c:out value='${pageBaseUrl}&page=${currentPage + 1}'/>"
                                           aria-label="Trang tiếp theo">›</a>
                                    </c:when>
                                    <c:otherwise><span class="activity-page-link is-disabled" aria-hidden="true">›</span></c:otherwise>
                                </c:choose>
                            </nav>
                        </c:if>
                    </div>
                </c:if>
            </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>
