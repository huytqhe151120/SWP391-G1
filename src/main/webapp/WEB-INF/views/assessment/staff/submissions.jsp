<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="activities" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Danh sach bai nop | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261022-4">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/list-pagination.css?v=1">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-detail-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> DANH GIA SAU HOAT DONG</span>
                    <h1>Danh sach bai nop</h1>
                    <p>ID hoat dong: <c:out value="${activityId}"/></p>
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

    <section class="card activity-form-card" data-paginated-list>
        <div class="activity-table-heading">
            <div>
                <h2>Danh sach bai nho</h2>
                <p>Xem va cham bai cua sinh vien.</p>
            </div>
            <a class="button" href="${pageContext.request.contextPath}/assessment?action=manage&activityId=${activityId}">Quay lai</a>
        </div>

        <c:if test="${empty submissions}">
            <div class="empty-state">
                <p>Chua co bai nop nao.</p>
            </div>
        </c:if>

        <table class="table">
            <thead>
                <tr>
                    <th>Sinh vien</th>
                    <th>Trang thai</th>
                    <th>Diem cuoi cung</th>
                    <th>Thoi gian nop</th>
                    <th>Hanh dong</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="s" items="${submissions}">
                    <tr data-page-item>
                        <td><c:out value="${s.studentId}"/></td>
                        <td>
                            <span class="status-badge ${s.status == 'COMPLETED' ? 'active' : (s.status == 'SUBMITTED' ? 'warning' : 'inactive')}">${s.status}</span>
                        </td>
                        <td><c:choose><c:when test="${not empty s.finalScore}"><c:out value="${s.finalScore}"/></c:when><c:otherwise>--</c:otherwise></c:choose></td>
                        <td><c:choose><c:when test="${not empty submittedAtDisplay[s.id]}"><c:out value="${submittedAtDisplay[s.id]}"/></c:when><c:otherwise>--</c:otherwise></c:choose></td>
                        <td>
                            <a class="button button-sm" href="${pageContext.request.contextPath}/assessment?action=grade&attemptId=${s.id}">Cham bai</a>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
<script src="${pageContext.request.contextPath}/assets/js/list-pagination.js?v=1" defer></script>
</body>
</html>
