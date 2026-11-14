<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="activeNav" value="assessments" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Làm bài đánh giá | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261113-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261113-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/assessment.css?v=20261113-1">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-form-page activity-detail-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> LÀM BÀI ĐÁNH GIÁ</span>
                    <h1><c:out value="${assessment.title}"/></h1>
                    <p>
                        <c:if test="${not empty activity}">
                            <span class="activity-code"><c:out value="${activity.code}"/></span>
                            <c:out value="${activity.name}"/>
                        </c:if>
                    </p>
                </div>
            </header>

            <div class="assessment-page-nav">
                <div class="assessment-page-nav-links">
                    <a class="button button-back" href="${pageContext.request.contextPath}/assessment?action=list">
                        <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5"/><path d="m12 19-7-7 7-7"/></svg>
                        Danh sách bài đánh giá của tôi
                    </a>
                    <a class="button button-outline" href="${pageContext.request.contextPath}/activities?action=detail&amp;id=${activityId}">
                        Chi tiết hoạt động
                    </a>
                </div>
            </div>

            <c:if test="${not empty sessionScope.successMessage}">
                <div class="alert alert-success activity-alert" role="status"><c:out value="${sessionScope.successMessage}"/></div>
                <c:remove var="successMessage" scope="session"/>
            </c:if>
            <c:if test="${not empty sessionScope.errorMessage}">
                <div class="alert alert-error activity-alert" role="alert"><c:out value="${sessionScope.errorMessage}"/></div>
                <c:remove var="errorMessage" scope="session"/>
            </c:if>

            <section class="card activity-form-card">
                <div class="activity-table-heading">
                    <div>
                        <h2>Nội dung đề bài đánh giá</h2>
                        <p>
                            <c:choose>
                                <c:when test="${not empty assessment.description}">
                                    <c:out value="${assessment.description}"/>
                                </c:when>
                                <c:otherwise>
                                    Hoàn thành các câu hỏi bên dưới. Bạn có thể lưu nháp trước khi nhấn nộp bài chính thức.
                                </c:otherwise>
                            </c:choose>
                        </p>
                    </div>
                    <div class="activity-heading-actions">
                        <span class="activity-count"><c:out value="${questions.size()}"/> câu hỏi</span>
                        <span class="assessment-score-pill">
                            Tổng điểm: <fmt:formatNumber value="${totalMaxScore}" minFractionDigits="1" maxFractionDigits="2"/>
                        </span>
                    </div>
                </div>

                <c:if test="${not empty closeTimeDisplay}">
                    <div style="padding: 12px 26px 0; border-top: 1px solid #edf0f5;">
                        <div class="assessment-banner assessment-banner--info" style="margin-bottom: 0;">
                            <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
                            <div>
                                Hạn chót nộp bài: <strong><c:out value="${closeTimeDisplay}"/></strong>.
                                Lưu ý: mỗi sinh viên chỉ được nộp bài chính thức <strong>01 lần duy nhất</strong>.
                            </div>
                        </div>
                    </div>
                </c:if>

                <form action="${pageContext.request.contextPath}/assessment" method="post">
                    <input type="hidden" name="attemptId" value="${attempt.id}"/>
                    <input type="hidden" name="assessmentId" value="${assessmentId}"/>
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"/>

                    <div class="assessment-question-list">
                        <c:forEach var="q" items="${questions}" varStatus="loop">
                            <c:set var="savedAns" value="${savedAnswerMap[q.id]}"/>
                            <article class="assessment-question-card">
                                <div class="assessment-question-header">
                                    <div class="assessment-question-badges">
                                        <span class="assessment-q-index">Câu <c:out value="${loop.count}"/></span>
                                        <c:choose>
                                            <c:when test="${q.questionType == 'MCQ'}">
                                                <span class="assessment-q-type assessment-q-type--mcq">Chọn 1 đáp án đúng</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="assessment-q-type assessment-q-type--essay">Câu hỏi tự luận</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                    <span class="assessment-score-pill">
                                        <fmt:formatNumber value="${q.maxScore}" minFractionDigits="1" maxFractionDigits="2"/> điểm
                                    </span>
                                </div>

                                <p class="assessment-question-prompt"><c:out value="${q.content}"/></p>

                                <c:choose>
                                    <c:when test="${q.questionType == 'MCQ'}">
                                        <div class="assessment-choice-list">
                                            <c:forEach var="opt" items="${q.options}">
                                                <label class="assessment-choice-option">
                                                    <input type="radio" name="q_${q.id}" value="${opt.id}"
                                                           ${savedAns != null and savedAns.selectedOptionId != null and savedAns.selectedOptionId == opt.id ? 'checked' : ''}/>
                                                    <span><c:out value="${opt.optionText}"/></span>
                                                </label>
                                            </c:forEach>
                                        </div>
                                    </c:when>
                                    <c:otherwise>
                                        <div class="field" style="margin:0;">
                                            <label for="essay_${q.id}">Bài trả lời của bạn (tối đa 10.000 ký tự)</label>
                                            <textarea id="essay_${q.id}" name="q_${q.id}" rows="5" maxlength="10000"
                                                      placeholder="Nhập nội dung bài thu hoạch / câu trả lời của bạn tại đây..."><c:out value="${savedAns != null ? savedAns.answerText : ''}"/></textarea>
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                            </article>
                        </c:forEach>
                    </div>

                    <div class="activity-form-actions">
                        <a class="button button-secondary" href="${pageContext.request.contextPath}/assessment?action=list">Quay lại</a>
                        <button type="submit" name="action" value="saveAnswers" formnovalidate class="button button-outline">
                            Lưu nháp
                        </button>
                        <button type="submit" name="action" value="submitAttempt" class="button button-primary"
                                onclick="return confirm('Bạn có chắc chắn muốn nộp bài đánh giá? Sau khi nộp sẽ không thể chỉnh sửa câu trả lời.');">
                            Nộp bài đánh giá
                        </button>
                    </div>
                </form>
            </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>
