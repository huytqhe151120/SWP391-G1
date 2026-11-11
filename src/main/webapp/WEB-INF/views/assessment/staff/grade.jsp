<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="activities" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Cham bai | EAMS</title>
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
                    <h1>Cham bai</h1>
                    <p>ID bai lam: <c:out value="${attempt.id}"/></p>
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
                <p>Cham cac cau hoi tu luan (Essay).</p>
            </div>
            <a class="button" href="${pageContext.request.contextPath}/assessment?action=submissions&activityId=${param.activityId}">Quay lai danh sach bai nop</a>
        </div>

        <c:if test="${empty answers}">
            <div class="empty-state">
                <p>Chua co cau tra loi nao.</p>
            </div>
        </c:if>

        <c:forEach var="a" items="${answers}">
            <c:if test="${a.question.questionType == 'ESSAY'}">
                <div class="field" style="border:1px solid var(--border); padding:1rem; margin-bottom:1rem; border-radius:8px;">
                    <div style="margin-bottom:0.5rem;">
                        <strong>Cau hoi ${a.question.displayOrder}:</strong>
                        <p style="margin:0.25rem 0;"><c:out value="${a.question.content}"/></p>
                        <p style="font-size:0.875rem; color:var(--text-muted);">Diem toi da: <c:out value="${a.question.maxScore}"/></p>
                    </div>
                    <div style="background:var(--surface-secondary); padding:1rem; border-radius:6px; margin-bottom:1rem;">
                        <strong>Cau tra loi cua sinh vien:</strong>
                        <p style="margin:0.5rem 0 0; white-space:pre-wrap;"><c:out value="${a.answerText}"/></p>
                    </div>
                <form action="${pageContext.request.contextPath}/assessment" method="post" style="display:inline">
                    <input type="hidden" name="action" value="gradeEssay"/>
                    <input type="hidden" name="answerId" value="${a.id}"/>
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"/>
                        <div class="field">
                            <label>Diem (0 - <c:out value="${a.question.maxScore}"/>)</label>
                            <input type="number" name="score" step="0.01" min="0" max="<c:out value="${a.question.maxScore}"/>" required value="${a.awardedScore}">
                        </div>
                        <div class="field">
                            <label>Nhan xet (tuychon)</label>
                            <textarea name="feedback" rows="3"><c:out value="${a.feedback}"/></textarea>
                        </div>
                        <div class="form-actions">
                            <button type="submit" class="button button-primary">Luu diem</button>
                            <a class="button" href="${pageContext.request.contextPath}/assessment?action=grade&attemptId=${attempt.id}">Huy</a>
                        </div>
                    </form>
                </div>
            </c:if>
        </c:forEach>

        <c:if test="${attempt.status == 'COMPLETED'}">
            <div class="alert alert-success">
                <strong>Da hoan thanh:</strong> Tat ca cac cau hoi da duoc cham. Diem cuoi cung: <c:out value="${attempt.finalScore}"/>
            </div>
        </c:if>
        <c:if test="${attempt.status == 'SUBMITTED'}">
            <div class="alert alert-warning">
                <strong>Dang cho cham:</strong> Con lai cac cau hoi chua duoc cham.
            </div>
        </c:if>
    </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>