<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="activities" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quan ly danh gia | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261022-4">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=1">
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
                    <h1>Quan ly danh gia</h1>
                    <p><c:out value="${assessment.title}"/></p>
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

    <section class="card activity-form-card">
        <div class="activity-table-heading">
            <div>
                <h2>Thong tin danh gia</h2>
                <p>Quan ly cau hinh va trang thai cong bo cua danh gia.</p>
            </div>
        </div>

        <div class="field">
            <label>Tieu de</label>
            <p><c:out value="${assessment.title}"/></p>
        </div>
        <div class="field">
            <label>Mo ta</label>
            <p><c:out value="${assessment.description}"/></p>
        </div>
        <div class="field">
            <label>Thoi gian mo bai</label>
            <p><c:choose><c:when test="${not empty assessment.openTime}"><c:out value="${assessment.openTime}"/></c:when><c:otherwise>Chua cau hinh</c:otherwise></c:choose></p>
        </div>
        <div class="field">
            <label>Thoi gian dong bai</label>
            <p><c:choose><c:when test="${not empty assessment.closeTime}"><c:out value="${assessment.closeTime}"/></c:when><c:otherwise>Chua cau hinh</c:otherwise></c:choose></p>
        </div>
        <div class="field">
            <label>Trang thai cong bo</label>
            <c:choose>
                <c:when test="${assessment.published}">
                    <span class="status-badge active">Da cong bo</span>
                    <form action="${pageContext.request.contextPath}/assessment" method="post" style="display:inline">
                        <input type="hidden" name="action" value="publish"/>
                        <input type="hidden" name="id" value="${assessment.id}"/>
                        <input type="hidden" name="publish" value="false"/>
                        <input type="hidden" name="_csrf" value="<c:out value=${csrfToken}/>" />
                        <button type="submit" class="button button-sm button-revoke">Huy cong bo</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <span class="status-badge inactive">Chua cong bo</span>
                    <form action="${pageContext.request.contextPath}/assessment" method="post" style="display:inline">
                        <input type="hidden" name="action" value="publish"/>
                        <input type="hidden" name="id" value="${assessment.id}"/>
                        <input type="hidden" name="publish" value="true"/>
                        <input type="hidden" name="_csrf" value="<c:out value=${csrfToken}/>" />
                        <button type="submit" class="button button-sm button-primary">Cong bo</button>
                    </form>
                </c:otherwise>

        <div class="form-actions">
            <a class="button button-primary" href="${pageContext.request.contextPath}/assessment?action=questions&assessmentId=${assessment.id}">Quan ly cau hoi</a>
            <a class="button" href="${pageContext.request.contextPath}/activities?action=detail&id=${assessment.activityId}">Quay lai chi tiet hoat dong</a>
        </div>
    </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>
