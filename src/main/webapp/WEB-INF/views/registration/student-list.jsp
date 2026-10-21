<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeNav" value="registration-forms" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng ký hoạt động | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261020-11">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/registration.css?v=20261021-10">
</head>
<body>
<div class="activity-app-shell">
    <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
    <div class="activity-app-layout">
        <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
        <div class="activity-app-main">
            <main class="page activity-list-page student-registration-page">
                <header class="activity-hero">
                    <div class="activity-hero-copy">
                        <span class="eyebrow">SWP391-G1 <span aria-hidden="true">/</span> SINH VIÊN</span>
                        <h1>Đăng ký hoạt động</h1>
                        <p>Khám phá các hoạt động đang mở đăng ký và gửi thông tin tham gia.</p>
                    </div>
                </header>

                <c:if test="${param.msg eq 'registered'}">
                    <div class="alert alert-success activity-alert">Gửi đăng ký thành công.</div>
                </c:if>

                <c:choose>
                    <c:when test="${not empty forms}">
                        <div class="registration-card-grid">
                            <c:forEach items="${forms}" var="f">
                                <article class="card student-registration-card">
                                    <div class="student-registration-card-top">
                                        <span class="badge status-pill status-green">Đang mở</span>
                                    </div>
                                    <h2><c:out value="${f.title}"/></h2>
                                    <h3><c:out value="${f.activityName}"/></h3>
                                    <p class="student-registration-description"><c:out value="${f.description}"/></p>
                                    <p class="student-registration-deadline">
                                        <strong>Hạn đăng ký:</strong>
                                        <c:choose><c:when test="${not empty f.closeAt}"><c:out value="${f.closeAt}"/></c:when><c:otherwise>Không giới hạn</c:otherwise></c:choose>
                                    </p>
                                    <a class="button button-primary student-registration-cta"
                                       href="${pageContext.request.contextPath}/registration-forms?action=register&amp;id=${f.id}">
                                        Xem và đăng ký
                                    </a>
                                </article>
                            </c:forEach>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <section class="card student-registration-empty">
                            <span class="empty-state-mark" aria-hidden="true">✓</span>
                            <h2>Chưa có hoạt động mở đăng ký</h2>
                            <p>Các form đăng ký sẽ xuất hiện tại đây khi ban tổ chức mở đăng ký cho hoạt động đã được duyệt.</p>
                        </section>
                    </c:otherwise>
                </c:choose>
            </main>
        </div>
    </div>
</div>
</body>
</html>
