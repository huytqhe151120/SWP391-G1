<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="activeNav" value="assessments" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Kết quả bài đánh giá | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261113-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261113-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/assessment.css?v=20261113-1">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-detail-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> KẾT QUẢ BÀI ĐÁNH GIÁ</span>
                    <h1><c:out value="${not empty assessment ? assessment.title : 'Kết quả bài đánh giá'}"/></h1>
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
                    <c:if test="${activityId > 0}">
                        <a class="button button-outline" href="${pageContext.request.contextPath}/activities?action=detail&amp;id=${activityId}">
                            Xem hoạt động ngoại khóa
                        </a>
                    </c:if>
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
                            Bài đánh giá của bạn đã được chấm hoàn tất. Điểm tổng kết đạt được là
                            <strong><fmt:formatNumber value="${attempt.finalScore}" minFractionDigits="1" maxFractionDigits="2"/> / <fmt:formatNumber value="${totalMaxScore}" minFractionDigits="1" maxFractionDigits="2"/></strong>.
                        </div>
                    </div>
                </c:when>
                <c:when test="${attempt.status == 'SUBMITTED'}">
                    <div class="assessment-banner assessment-banner--warning" role="status">
                        <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
                        <div>
                            <strong>Đang chờ chấm điểm tự luận:</strong> Bài làm của bạn đã được nộp thành công. Điểm tổng kết cuối cùng sẽ được hiển thị ngay sau khi cán bộ phụ trách chấm xong các câu tự luận.
                        </div>
                    </div>
                </c:when>
            </c:choose>

            <section class="card activity-table-card">
                <div class="activity-table-heading">
                    <div>
                        <h2>Tổng hợp bài làm &amp; nhận xét</h2>
                        <p>Chi tiết câu trả lời bạn đã nộp và phản hồi chấm điểm từng câu hỏi.</p>
                    </div>
                    <c:choose>
                        <c:when test="${attempt.status == 'COMPLETED'}">
                            <span class="badge status-pill status-green">Đã hoàn thành</span>
                        </c:when>
                        <c:when test="${attempt.status == 'SUBMITTED'}">
                            <span class="badge status-pill status-yellow">Đang chờ chấm</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge status-pill status-blue">Đang làm bài</span>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div class="assessment-summary-cards">
                    <div class="assessment-summary-card is-muted">
                        <span class="assessment-summary-label">Hoạt động ngoại khóa</span>
                        <span class="assessment-summary-value" style="font-size: 1rem;">
                            <c:choose>
                                <c:when test="${not empty activity}"><c:out value="${activity.code}"/></c:when>
                                <c:otherwise>#<c:out value="${activityId}"/></c:otherwise>
                            </c:choose>
                        </span>
                        <span class="assessment-summary-sub"><c:out value="${not empty activity ? activity.name : ''}"/></span>
                    </div>
                    <div class="assessment-summary-card is-muted">
                        <span class="assessment-summary-label">Thời điểm nộp bài</span>
                        <span class="assessment-summary-value" style="font-size: 1.05rem;">
                            <c:out value="${not empty submittedAtDisplay ? submittedAtDisplay : 'Chưa nộp'}"/>
                        </span>
                        <span class="assessment-summary-sub">Ghi nhận trên hệ thống</span>
                    </div>
                    <div class="assessment-summary-card is-blue">
                        <span class="assessment-summary-label">Tổng số câu hỏi</span>
                        <span class="assessment-summary-value"><c:out value="${answers.size()}"/> câu</span>
                        <span class="assessment-summary-sub">Thang điểm tối đa: <fmt:formatNumber value="${totalMaxScore}" minFractionDigits="1" maxFractionDigits="2"/></span>
                    </div>
                    <div class="assessment-summary-card ${attempt.finalScore != null ? 'is-green' : 'is-yellow'}">
                        <span class="assessment-summary-label">Điểm cuối cùng</span>
                        <span class="assessment-summary-value">
                            <c:choose>
                                <c:when test="${attempt.finalScore != null}">
                                    <fmt:formatNumber value="${attempt.finalScore}" minFractionDigits="1" maxFractionDigits="2"/> / <fmt:formatNumber value="${totalMaxScore}" minFractionDigits="1" maxFractionDigits="2"/>
                                </c:when>
                                <c:otherwise>Đang chờ chấm</c:otherwise>
                            </c:choose>
                        </span>
                        <span class="assessment-summary-sub">${attempt.finalScore != null ? 'Kết quả chính thức' : 'Chờ hoàn tất câu tự luận'}</span>
                    </div>
                </div>

                <c:choose>
                    <c:when test="${empty answers}">
                        <div class="assessment-empty-state">
                            <strong>Chưa ghi nhận câu trả lời nào</strong>
                            <p>Không tìm thấy dữ liệu câu trả lời cho lượt làm bài này.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="assessment-question-list">
                            <c:forEach var="a" items="${answers}" varStatus="loop">
                                <article class="assessment-question-card">
                                    <div class="assessment-question-header">
                                        <div class="assessment-question-badges">
                                            <span class="assessment-q-index">Câu <c:out value="${loop.count}"/></span>
                                            <c:choose>
                                                <c:when test="${a.question.questionType == 'MCQ'}">
                                                    <span class="assessment-q-type assessment-q-type--mcq">Trắc nghiệm</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="assessment-q-type assessment-q-type--essay">Tự luận</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>

                                        <c:choose>
                                            <c:when test="${a.gradingStatus == 'GRADED' and a.awardedScore != null}">
                                                <span class="assessment-score-pill ${a.awardedScore > 0 ? 'assessment-score-pill--green' : 'assessment-score-pill--muted'}">
                                                    Điểm: <fmt:formatNumber value="${a.awardedScore}" minFractionDigits="1" maxFractionDigits="2"/> / <fmt:formatNumber value="${a.question.maxScore}" minFractionDigits="1" maxFractionDigits="2"/>
                                                </span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge status-pill status-yellow">
                                                    Đang chờ chấm (Tối đa <fmt:formatNumber value="${a.question.maxScore}" minFractionDigits="1" maxFractionDigits="2"/> điểm)
                                                </span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>

                                    <p class="assessment-question-prompt"><c:out value="${a.question.content}"/></p>

                                    <c:choose>
                                        <c:when test="${a.question.questionType == 'MCQ'}">
                                            <c:choose>
                                                <c:when test="${not empty a.selectedOption}">
                                                    <div class="assessment-choice-review ${a.awardedScore != null and a.awardedScore > 0 ? 'is-correct' : 'is-wrong'}">
                                                        <span>
                                                            <strong>Phương án bạn đã chọn:</strong>
                                                            <c:out value="${a.selectedOption.optionText}"/>
                                                        </span>
                                                        <span class="badge status-pill ${a.awardedScore != null and a.awardedScore > 0 ? 'status-green' : 'status-red'}">
                                                            ${a.awardedScore != null and a.awardedScore > 0 ? 'Chính xác' : 'Chưa chính xác'}
                                                        </span>
                                                    </div>
                                                </c:when>
                                                <c:otherwise>
                                                    <div class="assessment-essay-box assessment-essay-box--empty">Bạn chưa chọn phương án cho câu hỏi này.</div>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:when>
                                        <c:otherwise>
                                            <c:choose>
                                                <c:when test="${not empty a.answerText}">
                                                    <div class="assessment-essay-box"><c:out value="${a.answerText}"/></div>
                                                </c:when>
                                                <c:otherwise>
                                                    <div class="assessment-essay-box assessment-essay-box--empty">Bạn không nhập câu trả lời cho câu hỏi này.</div>
                                                </c:otherwise>
                                            </c:choose>

                                            <c:if test="${not empty a.feedback}">
                                                <div class="assessment-feedback-box">
                                                    <strong>Nhận xét của người chấm</strong>
                                                    <c:out value="${a.feedback}"/>
                                                </div>
                                            </c:if>
                                        </c:otherwise>
                                    </c:choose>
                                </article>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>
