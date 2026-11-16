<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="activeNav" value="" scope="request"/>
<c:set var="rcTitle" value="Hỏi & Đáp" scope="request"/>
<c:set var="rcHero" value="Kênh hỏi đáp" scope="request"/>
<c:set var="rcEyebrow" value="EAMS / KÊNH HỖ TRỢ" scope="request"/>
<c:set var="rcSubtitle" value="Đặt câu hỏi và nhận phản hồi từ bộ phận phụ trách." scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${rcTitle}"/> | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/student-qa.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/support-theme.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/support-pagination.css?v=20261113">
</head>
<body class="support-page student-qa-page">
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-list-page">
            <c:if test="${not empty rcHero}">
                <header class="activity-hero">
                    <div class="activity-hero-copy">
                        <span class="eyebrow"><c:out value="${rcEyebrow}"/></span>
                        <h1><c:out value="${rcHero}"/></h1>
                        <p><c:out value="${rcSubtitle}"/></p>
                    </div>
                    <div class="student-chip">
                        <span class="student-avatar">SV</span>
                        <span><strong><c:out value="${student.name}"/></strong><small><c:out value="${student.code}"/></small></span>
                    </div>
                </header>
            </c:if>

    <c:if test="${param.msg == 'created'}">
        <div class="notice notice-success" role="status">Câu hỏi đã được gửi và đang chờ phản hồi.</div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="notice notice-error" role="alert"><c:out value="${error}"/></div>
    </c:if>

    <div class="content-grid">
        <section class="question-form-panel" aria-labelledby="ask-title">
            <p class="section-index">01</p>
            <h2 id="ask-title">Đặt câu hỏi mới</h2>
            <p class="section-note">Mô tả đủ bối cảnh để staff có thể trả lời chính xác.</p>

            <form method="post" action="${pageContext.request.contextPath}/student-qa">
                <label for="title">Tiêu đề</label>
                <input id="title" name="title" maxlength="200" required
                       value="<c:out value='${submittedTitle}'/>"
                       placeholder="Ví dụ: Thời hạn đăng ký hoạt động">

                <label for="content">Nội dung câu hỏi</label>
                <textarea id="content" name="content" maxlength="4000" required
                          placeholder="Nhập nội dung cụ thể..."><c:out value="${submittedContent}"/></textarea>

                <label class="check-row" for="anonymous">
                    <input id="anonymous" type="checkbox" name="anonymous" ${submittedAnonymous ? 'checked' : ''}>
                    <span><strong>Gửi ẩn danh</strong><small>Tên của bạn sẽ không xuất hiện khi câu hỏi được công khai.</small></span>
                </label>

                <button class="submit-button" type="submit">Gửi câu hỏi</button>
            </form>
        </section>

        <section class="feed" aria-labelledby="feed-title">
            <div class="feed-heading">
                <div>
                    <p class="section-index">02</p>
                    <h2 id="feed-title">Các câu hỏi</h2>
                </div>
                <span class="count"><c:out value="${questions.size()}"/> câu</span>
            </div>

            <c:choose>
                <c:when test="${empty questions}">
                    <div class="empty-state">
                        <strong>Chưa có câu hỏi nào</strong>
                        <span>Hãy gửi câu hỏi đầu tiên bằng biểu mẫu bên cạnh.</span>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="question-list" data-support-pages="10" data-support-item=".question-item" data-support-label="câu hỏi">
                        <c:forEach var="q" items="${questions}">
                            <article class="question-item">
                                <div class="question-topline">
                                    <c:choose>
                                        <c:when test="${q.status == 'ANSWERED'}">
                                            <span class="status answered">Đã trả lời</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="status pending">Đang chờ</span>
                                        </c:otherwise>
                                    </c:choose>
                                    <time>${dt:format(q.createdAt)}</time>
                                </div>
                                <h3><c:out value="${q.title}"/></h3>
                                <p class="question-content"><c:out value="${q.content}"/></p>
                                <p class="author">
                                    <c:choose>
                                        <c:when test="${q.anonymous}">Người hỏi ẩn danh</c:when>
                                        <c:otherwise><c:out value="${q.studentName}"/> · <c:out value="${q.studentCode}"/></c:otherwise>
                                    </c:choose>
                                </p>

                                <c:forEach var="answer" items="${answersByQuestion[q.id]}">
                                    <div class="answer">
                                        <span class="answer-label">PHẢN HỒI TỪ STAFF</span>
                                        <p><c:out value="${answer.content}"/></p>
                                        <small><c:out value="${answer.staffName}"/> · ${dt:format(answer.publishedAt)}</small>
                                    </div>
                                </c:forEach>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/action-menu.js?v=5"></script>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
<script src="${pageContext.request.contextPath}/assets/js/support-pagination.js?v=20261113"></script>
</body>
</html>
