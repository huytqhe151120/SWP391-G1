<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="activities" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chinh sua danh gia | EAMS</title>
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
                    <h1>Chinh sua danh gia</h1>
                    <p>Chinh sua thong tin danh gia cho hoat dong ngoai khoa.</p>
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
        <form action="${pageContext.request.contextPath}/assessment" method="post">
            <input type="hidden" name="action" value="update"/>
            <input type="hidden" name="id" value="${assessment.id}"/>
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"/>

            <div class="field">
                <label>Tieu de danh gia <span class="required">*</span></label>
                <input type="text" name="title" required value="<c:out value='${assessment.title}'/>" placeholder="Vd: Danh gia ket thuc hoat dong tu van nghiep vu">
            </div>

            <div class="field">
                <label>Mo ta</label>
                <textarea name="description" rows="3" placeholder="Mo ta ve noi dung va muc tieu cua danh gia"><c:out value='${assessment.description}'/></textarea>
            </div>

            <div class="field">
                <label>Thoi gian mo bai (tuong lai)</label>
                <input type="datetime-local" name="openTime" value="<c:out value='${openTimeDisplay}'/>">
            </div>

            <div class="field">
                <label>Thoi gian dong bai</label>
                <input type="datetime-local" name="closeTime" value="<c:out value='${closeTimeDisplay}'/>">
            </div>

            <div class="form-actions">
                <button type="submit" class="button button-primary">Cap nhat danh gia</button>
                <a class="button" href="${pageContext.request.contextPath}/assessment?action=manage&activityId=${assessment.activityId}">Huy</a>
            </div>
        </form>
    </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>