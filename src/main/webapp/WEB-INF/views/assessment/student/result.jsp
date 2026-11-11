<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="activities" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Ket qua danh gia | EAMS</title>
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
                    <h1>Ket qua danh gia</h1>
                    <p>ID danh gia: <c:out value="${param.assessmentId}"/></p>
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
                <h2>Trang thai bai lam</h2>
                <p>Xem ket qua va nhan xet cua cac cau hoi tu luan.</p>
            </div>
            <a class="button" href="${pageContext.request.contextPath}/activities?action=detail&id=${activityId}">Quay lai</a>
        </div>

        <div class="field">
            <label>Trang thai</label>
            <span class="status-badge ${attempt.status == 'COMPLETED' ? 'active' : (attempt.status == 'SUBMITTED' ? 'warning' : 'inactive')}">${attempt.status}</span>
        </div>

        <c:if test="${attempt.status == 'COMPLETED'}">
            <div class="field">
                <label>Diem cuoi cung</label>
                <h3><c:out value="${attempt.finalScore}"/></h3>
            </div>
        </c:if>
        <c:if test="${attempt.status == 'SUBMITTED'}">
            <div class="alert alert-warning">
                <strong>Dang cho cham:</strong> Cac cau hoi tu luan chua duoc cham. Khong co diem cuoi cung.
            </div>
        </c:if>

        <h3>Chi tiet cau tra loi</h3>
        <c:if test="${empty answers}">
            <p>Chua co cau tra loi nao.</p>
        </c:if>
        <c:forEach var="a" items="${answers}">
            <div class="field" style="border:1px solid var(--border); padding:1rem; margin-bottom:1rem; border-radius:8px;">
                <div style="margin-bottom:0.5rem;">
                    <strong>Cau hoi ${a.question.displayOrder}:</strong>
                    <p style="margin:0.25rem 0;"><c:out value="${a.question.content}"/></p>
                </div>

                <c:if test="${a.question.questionType == 'MCQ'}">
                    <div class="field">
                        <label>Cau tra loi</label>
                        <p><c:out value="${a.selectedOptionId}"/></p>
                    </div>
                    <c:if test="${a.awardedScore != null}">
                        <div class="field">
                            <label>Diem</label>
                            <p><c:out value="${a.awardedScore}"/> / <c:out value="${a.question.maxScore}"/></p>
                        </div>
                    </c:if>
                </c:if>

                <c:if test="${a.question.questionType == 'ESSAY'}">
                    <div class="field">
                        <label>Cau tra loi</label>
                        <p style="white-space:pre-wrap;"><c:out value="${a.answerText}"/></p>
                    </div>
                    <c:if test="${a.awardedScore != null}">
                        <div class="field">
                            <label>Diem</label>
                            <p><c:out value="${a.awardedScore}"/> / <c:out value="${a.question.maxScore}"/></p>
                        </div>
                    </c:if>
                    <c:if test="${not empty a.feedback}">
                        <div class="field">
                            <label>Nhan xet cua nguoi cham</label>
                            <p><c:out value="${a.feedback}"/></p>
                        </div>
                    </c:if>
                    <c:if test="${a.awardedScore == null && a.gradingStatus == 'PENDING_GRADING'}">
                        <div class="alert alert-info">
                            <strong>Dang cho cham:</strong> Cau hoi nay chua duoc nguoi quan ly cham.
                        </div>
                    </c:if>
                </c:if>
            </div>
        </c:forEach>
    </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>