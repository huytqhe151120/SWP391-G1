<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="activity-series" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chi tiết chuỗi hoạt động | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261022-4">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=1">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-list-page activity-form-page activity-detail-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> QUẢN LÝ CHUỖI HOẠT ĐỘNG</span>
                    <h1>Chi tiết chuỗi hoạt động</h1>
                    <p><span class="activity-code"><c:out value="${series.seriesCode}"/></span> <c:out value="${series.name}"/></p>
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
                <h2>Chi tiết chuỗi hoạt động</h2>
                <p>Thông tin cấu hình lặp lại và trạng thái duyệt.</p>
            </div>
            <c:if test="${canManageActivities && series.approvalStatus != 'APPROVED'}">
                <a class="button button-primary"
                   href="${pageContext.request.contextPath}/activity-series?action=edit&id=${series.id}">Chỉnh sửa</a>
            </c:if>
        </div>

        <dl class="detail-grid activity-detail-grid">
            <div class="detail-item"><dt>Mã chuỗi</dt><dd><c:out value="${series.seriesCode}"/></dd></div>
            <div class="detail-item"><dt>Tên chuỗi</dt><dd><c:out value="${series.name}"/></dd></div>
            <div class="detail-item"><dt>Chế độ lặp</dt><dd><c:out value="${series.recurrenceMode}"/></dd></div>
            <div class="detail-item"><dt>Khoảng cách lặp</dt><dd><c:out value="${series.recurrenceInterval}"/> tháng/năm</dd></div>
            <div class="detail-item"><dt>Ngày bắt đầu</dt><dd><c:out value="${series.seriesStartDate}"/></dd></div>
            <div class="detail-item"><dt>Ngày kết thúc</dt><dd><c:out value="${series.seriesEndDate}"/></dd></div>
            <div class="detail-item"><dt>Số sự kiện tối đa</dt><dd><c:out value="${series.maxOccurrences}"/></dd></div>
            <div class="detail-item"><dt>Thời lượng sự kiện (phút)</dt><dd><c:out value="${series.occurrenceDurationMinutes}"/></dd></div>
            <div class="detail-item"><dt>Giờ trong ngày</dt><dd><c:out value="${series.timeOfDay}"/></dd></div>
            <div class="detail-item"><dt>Học kỳ</dt><dd><c:out value="${series.semesterId}"/></dd></div>
            <div class="detail-item"><dt>Mô tả</dt><dd><c:out value="${series.description}"/></dd></div>
            <div class="detail-item"><dt>Trạng thái duyệt</dt><dd><span class="badge ${series.approvalStatus == 'APPROVED' ? 'status-green' : series.approvalStatus == 'REJECTED' ? 'status-red' : series.approvalStatus == 'PENDING' ? 'status-yellow' : 'status-gray'}"><c:out value="${series.approvalStatus}"/></span></dd></div>
            <div class="detail-item"><dt>Tạo lúc</dt><dd><c:out value="${series.createdAt}"/></dd></div>
            <div class="detail-item"><dt>Cập nhật lúc</dt><dd><c:out value="${series.updatedAt}"/></dd></div>
        </dl>

        <section class="activity-detail-section">
            <h3>Danh sách sự kiện lặp lại</h3>
            <c:if test="${not empty occurrences}">
                <div class="table-wrap">
                    <table class="activity-table occurrence-table">
                        <thead>
                            <tr>
                                <th>Mã hoạt động</th>
                                <th>Tên hoạt động</th>
                                <th>Thời gian bắt đầu</th>
                                <th>Thời gian kết thúc</th>
                                <th>Trạng thái hoạt động</th>
                                <th>Trạng thái duyệt</th>
                                <th class="activity-table-actions">Hành động</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="occ" items="${occurrences}">
                                <tr>
                                    <td><c:out value="${occ.code}"/></td>
                                    <td><a href="${pageContext.request.contextPath}/activities?action=detail&id=${occ.id}"><c:out value="${occ.name}"/></a></td>
                                    <td><c:out value="${occ.startTime}"/></td>
                                    <td><c:out value="${occ.endTime}"/></td>
                                    <td><span class="badge status-${occ.activityStatus == null ? 'gray' : occ.activityStatus.toLowerCase()}"><c:out value="${occ.activityStatus}"/></span></td>
                                    <td><span class="badge ${occ.approvalStatus == 'APPROVED' ? 'status-green' : occ.approvalStatus == 'REJECTED' ? 'status-red' : occ.approvalStatus == 'PENDING' ? 'status-yellow' : 'status-gray'}"><c:out value="${occ.approvalStatus}"/></span></td>
                                    <td class="activity-table-actions"><a class="button button-outline" href="${pageContext.request.contextPath}/activities?action=detail&id=${occ.id}">Chi tiết</a></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:if>
            <c:if test="${empty occurrences}">
                <div class="empty-state"><p>Chưa có sự kiện lặp lại nào được tạo cho chuỗi này.</p></div>
            </c:if>
        </section>

        <div class="activity-form-actions">
            <c:if test="${canManageActivities}">
                <form method="post"
                      action="${pageContext.request.contextPath}/activity-series"
                      style="display:inline-block; margin-right: 0.5rem;"
                      onsubmit="return confirm('Bạn có chắc chắn muốn tạo sự kiện lặp lại cho chuỗi này?');">
                    <input type="hidden" name="action" value="generate"/>
                    <input type="hidden" name="seriesId" value="${series.id}"/>
                    <button class="button button-primary" type="submit">Tạo sự kiện lặp lại</button>
                </form>
                <form method="post"
                      action="${pageContext.request.contextPath}/activity-series"
                      style="display:inline-block; margin-right: 0.5rem;"
                      onsubmit="return confirm('Bạn có chắc chắn muốn xóa chuỗi hoạt động này?');">
                    <input type="hidden" name="action" value="delete"/>
                    <input type="hidden" name="id" value="${series.id}"/>
                    <button class="button button-danger-light" type="submit">Xóa chuỗi</button>
                </form>
            </c:if>
            <a class="button button-outline"
               href="${pageContext.request.contextPath}/activity-series">Quay về danh sách</a>
        </div>
    </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/activity.js?v=20261016-1"></script>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>
