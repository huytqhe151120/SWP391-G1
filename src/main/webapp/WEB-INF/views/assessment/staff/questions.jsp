<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="activities" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quan ly cau hoi | EAMS</title>
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
                    <h1>Quan ly cau hoi</h1>
                    <p>ID danh gia: <c:out value="${assessmentId}"/></p>
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
                <h2>Danh sach cau hoi</h2>
                <p>Them, chinh sua hoac xoa cau hoi trong danh gia.</p>
            </div>
            <a class="button button-primary" href="${pageContext.request.contextPath}/assessment?action=edit&id=${assessmentId}">Chinh sua thong tin danh gia</a>
            <a class="button" href="${pageContext.request.contextPath}/assessment?action=manage&activityId=${assessmentId}">Quay lai</a>
        </div>

        <c:if test="${empty questions}">
            <div class="empty-state">
                <p>Chua co cau hoi nao. Them cau hoi dau tien.</p>
            </div>
        </c:if>

        <c:forEach var="q" items="${questions}">
            <div class="field" style="border:1px solid var(--border); padding:1rem; margin-bottom:1rem; border-radius:8px;">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.5rem;">
                    <strong>Cau hoi ${q.displayOrder}</strong>
                    <span class="status-badge ${q.questionType == 'MCQ' ? 'active' : 'inactive'}">${q.questionType}</span>
                </div>
                <p><c:out value="${q.content}"/></p>
                <p style="font-size:0.875rem; color:var(--text-muted);">Diem toi da: <c:out value="${q.maxScore}"/></p>
                <c:if test="${q.questionType == 'MCQ'}">
                    <a class="button button-sm" href="${pageContext.request.contextPath}/assessment?action=options&questionId=${q.id}">Quan ly phuong an</a>
                </c:if>
                <form action="${pageContext.request.contextPath}/assessment" method="post" style="display:inline">
                    <input type="hidden" name="action" value="deleteQuestion"/>
                    <input type="hidden" name="questionId" value="${q.id}"/>
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"/>
                    <button type="submit" class="button button-sm button-revoke" onclick="return confirm('Xoa cau hoi nay?')">Xoa</button>
                </form>
            </div>
        </c:forEach>

        <div class="activity-table-heading" style="margin-top:2rem;">
            <div>
                <h3>Them cau hoi moi</h3>
            </div>
        </div>
        <form action="${pageContext.request.contextPath}/assessment" method="post">
            <input type="hidden" name="action" value="addQuestion"/>
            <input type="hidden" name="assessmentId" value="${assessmentId}"/>
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"/>

            <div class="field">
                <label>Loai cau hoi</label>
                <select name="questionType">
                    <option value="MCQ">Tra loi chon nhieu (MCQ)</option>
                    <option value="ESSAY">Tu luan (Essay)</option>
                </select>
            </div>

            <div class="field">
                <label>Noi dung cau hoi</label>
                <textarea name="content" rows="3" required></textarea>
            </div>

            <div class="field">
                <label>Diem toi da</label>
                <input type="number" name="maxScore" step="0.01" min="0" required value="10">
            </div>

            <div class="field">
                <label>Thu tu hien thi</label>
                <input type="number" name="displayOrder" value="0">
            </div>

            <div class="form-actions">
                <button type="submit" class="button button-primary">Them cau hoi</button>
            </div>
        </form>
    </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>