<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="activities" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quan ly phuong an | EAMS</title>
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
                    <h1>Quan ly phuong an</h1>
                    <p>ID cau hoi: <c:out value="${questionId}"/></p>
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
                <h2>Danh sach phuong an</h2>
                <p>Quan ly cac phuong an tra loi cho cau hoi MCQ.</p>
            </div>
            <a class="button button-primary" href="${pageContext.request.contextPath}/assessment?action=questions&assessmentId=${param.assessmentId}">Quay lai cau hoi</a>
        </div>

        <c:if test="${empty options}">
            <div class="empty-state">
                <p>Chua co phuong an nao. Them phuong an dau tien.</p>
            </div>
        </c:if>

        <c:forEach var="o" items="${options}">
            <div class="field" style="border:1px solid var(--border); padding:1rem; margin-bottom:1rem; border-radius:8px; display:flex; justify-content:space-between; align-items:center;">
                <div>
                    <strong>Phuong an ${o.displayOrder}</strong>
                    <p style="margin:0.25rem 0 0;"><c:out value="${o.optionText}"/></p>
                    <c:if test="${o.correct}">
                        <span class="status-badge active">Dap an dung</span>
                    </c:if>
                </div>
                <form action="${pageContext.request.contextPath}/assessment" method="post" style="display:inline">
                    <input type="hidden" name="action" value="updateOption"/>
                    <input type="hidden" name="optionId" value="${o.id}"/>
                    <input type="hidden" name="questionId" value="${questionId}"/>
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"/>
                    <input type="text" name="optionText" value="<c:out value="${o.optionText}"/>" required style="margin-right:0.5rem;"/>
                    <label style="margin-right:0.5rem;">
                        <input type="checkbox" name="isCorrect" value="true" <c:if test="${o.correct}">checked</c:if>> Dung
                    </label>
                    <input type="number" name="displayOrder" value="${o.displayOrder}" style="width:60px; margin-right:0.5rem;"/>
                    <button type="submit" class="button button-sm button-primary">Cap nhat</button>
                </form>
                <form action="${pageContext.request.contextPath}/assessment" method="post" style="display:inline">
                    <input type="hidden" name="action" value="deleteOption"/>
                    <input type="hidden" name="optionId" value="${o.id}"/>
                    <input type="hidden" name="questionId" value="${questionId}"/>
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"/>
                    <button type="submit" class="button button-sm button-revoke" onclick="return confirm('Xoa phuong an nay?')">Xoa</button>
                </form>
            </div>
        </c:forEach>

        <div class="activity-table-heading" style="margin-top:2rem;">
            <div>
                <h3>Them phuong an moi</h3>
            </div>
        </div>
        <form action="${pageContext.request.contextPath}/assessment" method="post">
            <input type="hidden" name="action" value="addOption"/>
            <input type="hidden" name="questionId" value="${questionId}"/>
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'"/>"/>

            <div class="field">
                <label>Noi dung phuong an</label>
                <input type="text" name="optionText" required placeholder="Nhap noi dung phuong an">
            </div>

            <div class="field">
                <label>La dap an dung</label>
                <input type="checkbox" name="isCorrect" value="true">
            </div>

            <div class="field">
                <label>Thu tu hien thi</label>
                <input type="number" name="displayOrder" value="0">
            </div>

            <div class="form-actions">
                <button type="submit" class="button button-primary">Them phuong an</button>
            </div>
        </form>
    </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>
