<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="activities" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Lam bai danh gia | EAMS</title>
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
                    <h1>Lam bai danh gia</h1>
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
        <form action="${pageContext.request.contextPath}/assessment" method="post">
            <input type="hidden" name="action" value="submitAttempt"/>
            <input type="hidden" name="attemptId" value="${attempt.id}"/>
            <input type="hidden" name="studentId" value="${attempt.studentId}"/>
            <input type="hidden" name="assessmentId" value="${assessmentId}"/>

            <c:forEach var="q" items="${questions}">
                <div class="field" style="border:1px solid var(--border); padding:1rem; margin-bottom:1rem; border-radius:8px;">
                    <div style="margin-bottom:0.5rem;">
                        <strong>Cau hoi ${q.displayOrder}:</strong>
                        <p style="margin:0.25rem 0;"><c:out value="${q.content}"/></p>
                        <p style="font-size:0.875rem; color:var(--text-muted);">Diem toi da: <c:out value="${q.maxScore}"/></p>
                    </div>

                    <c:if test="${q.questionType == 'MCQ'}">
                        <c:forEach var="opt" items="${q.options}">
                            <label style="display:block; margin-bottom:0.25rem;">
                                <input type="radio" name="q_${q.id}" value="${opt.id}">
                                <c:out value="${opt.optionText}"/>
                            </label>
                        </c:forEach>
                    </c:if>

                    <c:if test="${q.questionType == 'ESSAY'}">
                        <div class="field">
                            <label>Cau tra loi (van ban)</label>
                            <textarea name="q_${q.id}" rows="4" placeholder="Nhap cau tra loi cua ban o day..."></textarea>
                            <p style="font-size:0.75rem; color:var(--text-muted);">Cau tra loi hien la van bat. Khong ho tro tai tap tin.</p>
                        </div>
                    </c:if>
                </div>
            </c:forEach>

            <div class="form-actions">
                <button type="submit" class="button button-primary" onclick="return confirm('Ban chac chan muon nop bai? Sau khi nop khong the sua doi.')">Nop bai</button>
                <a class="button" href="${pageContext.request.contextPath}/activities?action=detail&id=${param.activityId}">Huy</a>
            </div>
        </form>
    </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>