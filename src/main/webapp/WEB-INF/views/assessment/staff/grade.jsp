<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="activeNav" value="assessments" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chấm bài đánh giá | EAMS</title>
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
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> CHẤM ĐIỂM BÀI ĐÁNH GIÁ</span>
                    <h1>Chi tiết &amp; chấm điểm bài làm</h1>
                    <p>
                        <c:choose>
                            <c:when test="${not empty student}">
                                <span class="activity-code"><c:out value="${student.code}"/></span>
                                <c:out value="${student.name}"/>
                            </c:when>
                            <c:otherwise>Sinh viên #<c:out value="${attempt.studentId}"/></c:otherwise>
                        </c:choose>
                        <c:if test="${not empty assessment}">
                            · <c:out value="${assessment.title}"/>
                        </c:if>
                    </p>
                </div>
            </header>

            <div class="assessment-page-nav">
                <div class="assessment-page-nav-links">
                    <c:choose>
                        <c:when test="${param.from eq 'manage'}">
                            <a class="button button-back"
                               href="${pageContext.request.contextPath}/assessment?action=manage&amp;activityId=${assessment.activityId}#tab-form-applications">
                                <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5"/><path d="m12 19-7-7 7-7"/></svg>
                                Quay lại chi tiết bài đánh giá
                            </a>
                        </c:when>
                        <c:otherwise>
                            <a class="button button-back"
                               href="${pageContext.request.contextPath}/assessment?action=submissions&amp;activityId=${assessment.activityId}">
                                <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5"/><path d="m12 19-7-7 7-7"/></svg>
                                Danh sách bài nộp
                            </a>
                            <a class="button button-outline"
                               href="${pageContext.request.contextPath}/assessment?action=manage&amp;activityId=${assessment.activityId}">
                                Chi tiết bài đánh giá
                            </a>
                        </c:otherwise>
                    </c:choose>
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

            <c:choose>
                <c:when test="${attempt.status == 'COMPLETED'}">
                    <div class="assessment-banner assessment-banner--success" role="status">
                        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg>
                        <div>
                            Bài làm này đã được chấm điểm hoàn tất. Điểm tổng kết cuối cùng là
                            <strong><fmt:formatNumber value="${attempt.finalScore}" minFractionDigits="1" maxFractionDigits="2"/> / <fmt:formatNumber value="${totalMaxScore}" minFractionDigits="1" maxFractionDigits="2"/></strong>.
                        </div>
                    </div>
                </c:when>
                <c:when test="${attempt.status == 'SUBMITTED'}">
                    <div class="assessment-banner assessment-banner--info" role="note">
                        <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
                        <div>
                            Các câu trắc nghiệm đã được tự động tính điểm. Vui lòng nhập điểm và nhận xét cho từng câu tự luận bên dưới; khi tất cả câu tự luận đều được chấm, hệ thống sẽ tự động tổng hợp điểm cuối cùng.
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="assessment-banner assessment-banner--warning" role="note">
                        <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10"/><path d="M12 8v4"/><path d="M12 16h.01"/></svg>
                        <div>Sinh viên đang trong quá trình làm bài và chưa nhấn nộp chính thức. Bạn chỉ có thể chấm điểm sau khi bài làm được nộp.</div>
                    </div>
                </c:otherwise>
            </c:choose>

            <section class="card activity-table-card">
                <div class="activity-table-heading">
                    <div>
                        <h2>Thông tin bài nộp &amp; kết quả từng câu</h2>
                        <p>
                            <c:if test="${not empty activity}">
                                Hoạt động: <strong><c:out value="${activity.code}"/> — <c:out value="${activity.name}"/></strong>
                            </c:if>
                        </p>
                    </div>
                    <c:choose>
                        <c:when test="${attempt.status == 'COMPLETED'}">
                            <span class="badge status-pill status-green">Đã hoàn thành chấm điểm</span>
                        </c:when>
                        <c:when test="${attempt.status == 'SUBMITTED'}">
                            <span class="badge status-pill status-yellow">Đang chờ chấm tự luận</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge status-pill status-blue">Đang làm bài</span>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div class="assessment-summary-cards">
                    <div class="assessment-summary-card is-muted">
                        <span class="assessment-summary-label">Sinh viên thực hiện</span>
                        <span class="assessment-summary-value" style="font-size: 1.05rem;">
                            <c:choose>
                                <c:when test="${not empty student and not empty student.name}"><c:out value="${student.name}"/></c:when>
                                <c:otherwise>Sinh viên #<c:out value="${attempt.studentId}"/></c:otherwise>
                            </c:choose>
                        </span>
                        <span class="assessment-summary-sub">
                            Mã SV: <c:out value="${not empty student ? student.code : attempt.studentId}"/>
                            <c:if test="${not empty student and not empty student.mainClassCode}"> · Lớp <c:out value="${student.mainClassCode}"/></c:if>
                        </span>
                    </div>
                    <div class="assessment-summary-card is-muted">
                        <span class="assessment-summary-label">Thời điểm nộp bài</span>
                        <span class="assessment-summary-value" style="font-size: 1.05rem;">
                            <c:out value="${not empty submittedAtDisplay ? submittedAtDisplay : 'Chưa nộp'}"/>
                        </span>
                        <span class="assessment-summary-sub">Ghi nhận trên hệ thống</span>
                    </div>
                    <div class="assessment-summary-card is-blue">
                        <span class="assessment-summary-label">Số câu hỏi</span>
                        <span class="assessment-summary-value"><c:out value="${answers.size()}"/> câu</span>
                        <span class="assessment-summary-sub">Thang điểm tối đa: <fmt:formatNumber value="${totalMaxScore}" minFractionDigits="1" maxFractionDigits="2"/></span>
                    </div>
                    <div class="assessment-summary-card ${attempt.finalScore != null ? 'is-green' : 'is-yellow'}">
                        <span class="assessment-summary-label">Điểm tổng kết</span>
                        <span class="assessment-summary-value">
                            <c:choose>
                                <c:when test="${attempt.finalScore != null}">
                                    <fmt:formatNumber value="${attempt.finalScore}" minFractionDigits="1" maxFractionDigits="2"/> / <fmt:formatNumber value="${totalMaxScore}" minFractionDigits="1" maxFractionDigits="2"/>
                                </c:when>
                                <c:otherwise>Chờ chấm xong</c:otherwise>
                            </c:choose>
                        </span>
                        <span class="assessment-summary-sub">${attempt.finalScore != null ? 'Đã đồng bộ kết quả' : 'Tự động cộng khi chấm đủ'}</span>
                    </div>
                </div>

                <div class="assessment-question-list">
                    <c:forEach var="a" items="${answers}" varStatus="loop">
                        <article class="assessment-question-card">
                            <div class="assessment-question-header">
                                <div class="assessment-question-badges">
                                    <span class="assessment-q-index">Câu <c:out value="${loop.count}"/></span>
                                    <c:choose>
                                        <c:when test="${a.question.questionType == 'MCQ'}">
                                            <span class="assessment-q-type assessment-q-type--mcq">Trắc nghiệm (Tự động chấm)</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="assessment-q-type assessment-q-type--essay">Tự luận (Chấm tay)</span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                                <div class="assessment-question-badges">
                                    <c:choose>
                                        <c:when test="${a.gradingStatus == 'GRADED' and a.awardedScore != null}">
                                            <span class="assessment-score-pill assessment-score-pill--green">
                                                Đạt: <fmt:formatNumber value="${a.awardedScore}" minFractionDigits="1" maxFractionDigits="2"/> / <fmt:formatNumber value="${a.question.maxScore}" minFractionDigits="1" maxFractionDigits="2"/> điểm
                                            </span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge status-pill status-yellow">
                                                Chờ chấm (Tối đa <fmt:formatNumber value="${a.question.maxScore}" minFractionDigits="1" maxFractionDigits="2"/> điểm)
                                            </span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>

                            <p class="assessment-question-prompt"><c:out value="${a.question.content}"/></p>

                            <c:choose>
                                <c:when test="${a.question.questionType == 'MCQ'}">
                                    <div class="assessment-choice-list">
                                        <c:forEach var="opt" items="${a.question.options}">
                                            <c:set var="isChosen" value="${a.selectedOptionId != null and a.selectedOptionId == opt.id}"/>
                                            <div class="assessment-choice-review ${opt.correct ? 'is-correct' : (isChosen ? 'is-wrong' : '')}">
                                                <span><c:out value="${opt.optionText}"/></span>
                                                <div class="assessment-question-badges">
                                                    <c:if test="${isChosen}">
                                                        <span class="badge status-pill ${opt.correct ? 'status-green' : 'status-red'}">Sinh viên chọn</span>
                                                    </c:if>
                                                    <c:if test="${opt.correct}">
                                                        <span class="badge status-pill status-green">Đáp án đúng</span>
                                                    </c:if>
                                                </div>
                                            </div>
                                        </c:forEach>
                                        <c:if test="${a.selectedOptionId == null}">
                                            <div class="assessment-essay-box assessment-essay-box--empty">Sinh viên không chọn phương án nào.</div>
                                        </c:if>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <c:choose>
                                        <c:when test="${not empty a.answerText}">
                                            <div class="assessment-essay-box"><c:out value="${a.answerText}"/></div>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="assessment-essay-box assessment-essay-box--empty">Sinh viên để trống câu trả lời tự luận này.</div>
                                        </c:otherwise>
                                    </c:choose>

                                    <c:choose>
                                        <c:when test="${attempt.status == 'SUBMITTED' and a.id > 0}">
                                            <div class="assessment-grade-panel">
                                                <form method="post" action="${pageContext.request.contextPath}/assessment">
                                                    <input type="hidden" name="action" value="gradeEssay"/>
                                                    <input type="hidden" name="answerId" value="${a.id}"/>
                                                    <input type="hidden" name="from" value="<c:out value='${param.from}'/>"/>
                                                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"/>

                                                    <div class="assessment-grade-form-grid">
                                                        <div class="field" style="margin:0;">
                                                            <label for="score_${a.id}">
                                                                Điểm chấm (0 – <fmt:formatNumber value="${a.question.maxScore}" minFractionDigits="1" maxFractionDigits="2"/>)
                                                                <span class="required-mark">*</span>
                                                            </label>
                                                            <input id="score_${a.id}" type="number" name="score" step="0.25"
                                                                   min="0" max="${a.question.maxScore}"
                                                                   value="<c:out value='${a.awardedScore}'/>" required/>
                                                        </div>
                                                        <div class="field" style="margin:0;">
                                                            <label for="fb_${a.id}">Nhận xét cho sinh viên</label>
                                                            <input id="fb_${a.id}" type="text" name="feedback" maxlength="1000"
                                                                   value="<c:out value='${a.feedback}'/>"
                                                                   placeholder="Nhập nhận xét hoặc góp ý cho câu trả lời..."/>
                                                        </div>
                                                        <button type="submit" class="button button-primary">
                                                            ${a.gradingStatus == 'GRADED' ? 'Cập nhật điểm' : 'Lưu điểm câu này'}
                                                        </button>
                                                    </div>
                                                </form>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <c:if test="${not empty a.feedback}">
                                                <div class="assessment-feedback-box">
                                                    <strong>Nhận xét của người chấm</strong>
                                                    <c:out value="${a.feedback}"/>
                                                </div>
                                            </c:if>
                                        </c:otherwise>
                                    </c:choose>
                                </c:otherwise>
                            </c:choose>
                        </article>
                    </c:forEach>
                </div>
            </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>
