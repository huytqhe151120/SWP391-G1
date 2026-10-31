<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="activity-series" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chuỗi hoạt động | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261022-4">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity-series-list.css?v=1">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
        <main class="activity-app-main page activity-list-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> QUẢN LÝ CHUỖI HOẠT ĐỘNG</span>
                    <h1>Chuỗi hoạt động</h1>
                    <p>Quản lý chuỗi hoạt động lặp lại theo tháng/năm và tạo các sự kiện lặp lại.</p>
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

    <section class="card activity-table-card" aria-labelledby="series-table-title">
        <div class="activity-table-heading">
            <div>
                <h2 id="series-table-title">Danh sách chuỗi hoạt động</h2>
                <p>Quản lý chuỗi hoạt động và tạo sự kiện lặp lại.</p>
            </div>
            <div class="activity-heading-actions">
                <c:if test="${canManageActivities}">
                    <a class="btn-create-activity"
                       onclick="location.href='${pageContext.request.contextPath}/activity-series?action=create'">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M8 3v10M3 8h10"/></svg>
                        Tạo chuỗi hoạt động
                    </a>
                </c:if>
            </div>
        </div>

        <table class="activity-table series-parent-table">
            <thead>
                <tr>
                    <th class="series-expand-col"></th>
                    <th>Mã chuỗi</th>
                    <th>Tên chuỗi</th>
                    <th>Chế độ lặp</th>
                    <th>Khoảng cách</th>
                    <th>Bắt đầu</th>
                    <th>Kết thúc</th>
                    <th>Trạng thái duyệt</th>
                    <th class="activity-table-actions">Hành động</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="s" items="${seriesList}">
                    <tr class="series-parent-row" data-series-id="${s.id}" data-expanded="false">
                        <td class="series-expand-cell"><button type="button" class="series-expand-btn" aria-expanded="false" aria-label="M? r?ng chi ti?t chu?i ${s.seriesCode}" data-series-id="${s.id}"><span class="chevron">?</span></button></td>
                        <td><c:out value="${s.seriesCode}"/></td>
                        <td><c:out value="${s.name}"/></td>
                        <td><c:out value="${s.recurrenceMode}"/></td>
                        <td><c:out value="${s.recurrenceInterval}"/></td>
                        <td><c:out value="${s.seriesStartDate}"/></td>
                        <td><c:out value="${s.seriesEndDate}"/></td>
                        <td><span class="badge ${s.approvalStatus == 'APPROVED' ? 'status-green' : s.approvalStatus == 'REJECTED' ? 'status-red' : s.approvalStatus == 'PENDING' ? 'status-yellow' : 'status-gray'}"><c:out value="${s.approvalStatus}"/></span></td>
                        <td class="activity-table-actions">
                            <a class="button button-outline"
                               href="${pageContext.request.contextPath}/activity-series?action=detail&id=${s.id}">Chi tiết</a>
                            <c:if test="${canManageActivities}">
                                <a class="button button-outline"
                                   href="${pageContext.request.contextPath}/activity-series?action=edit&id=${s.id}">Sửa</a>
                                <form method="post"
                                      action="${pageContext.request.contextPath}/activity-series"
                                      style="display:inline;"
                                      onsubmit="return confirm('Bạn có chắc chắn muốn xóa chuỗi hoạt động này?');">
                                    <input type="hidden" name="action" value="delete"/>
                                    <input type="hidden" name="id" value="${s.id}"/>
                                    <button class="button button-danger-light" type="submit">Xóa</button>
                                </form>
                                <form method="post"
                                      action="${pageContext.request.contextPath}/activity-series"
                                      style="display:inline;"
                                      onsubmit="return confirm('Bạn có chắc chắn muốn tạo sự kiện lặp lại cho chuỗi này?');">
                                    <input type="hidden" name="action" value="generate"/>
                                    <input type="hidden" name="seriesId" value="${s.id}"/>
                                    <button class="button button-primary" type="submit">Tạo sự kiện lặp</button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                    <tr class="series-child-row" data-series-id="${s.id}" style="display:none;">
                        <td colspan="9">
                            <div class="series-child-container" id="series-child-${s.id}">
                                <div class="series-loading" id="series-loading-${s.id}" style="display:none;"><span class="loading-spinner"></span><span>�ang t?i...</span></div>
                                <div class="series-error" id="series-error-${s.id}" style="display:none;"><span class="error-text"></span><button type="button" class="button button-outline series-retry-btn" data-series-id="${s.id}">Th? l?i</button></div>
                                <div class="series-empty" id="series-empty-${s.id}" style="display:none;"><p>Chua c� s? ki?n l?p l?i n�o du?c t?o.</p></div>
                                <table class="activity-table occurrence-table" id="occurrence-table-${s.id}">
                                    <thead>
                                        <tr>
                                            <th>M�</th>
                                            <th>T�n</th>
                                            <th>Th?i gian b?t d?u</th>
                                            <th>Th?i gian k?t th�c</th>
                                            <th>Tr?ng th�i ho?t d?ng</th>
                                            <th>Tr?ng th�i duy?t</th>
                                            <th class="activity-table-actions">H�nh d?ng</th>
                                        </tr>
                                    </thead>
                                    <tbody id="occurrence-body-${s.id}">
                                    </tbody>
                                </table>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/activity.js?v=20261016-1"></script>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
<script src="${pageContext.request.contextPath}/assets/js/activity-series-list.js?v=1"></script>
</body>
</html>