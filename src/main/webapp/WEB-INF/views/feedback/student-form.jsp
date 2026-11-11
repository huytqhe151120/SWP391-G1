<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="activity-feedback" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Gửi feedback hoạt động | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261111-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity-feedback.css?v=20261111-8">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page feedback-page feedback-form-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> FEEDBACK HOẠT ĐỘNG</span>
                    <h1><c:out value="${selectedActivity.activityName}"/></h1>
                    <p>Chia sẻ trải nghiệm và góp ý của bạn về hoạt động.</p>
                </div>
            </header>
            <div class="feedback-back-row">
                <a class="button button-secondary feedback-back-link" href="${pageContext.request.contextPath}/activity-feedback">← Quay lại danh sách</a>
            </div>

            <section class="feedback-activity-meta">
                <div>
                    <span class="feedback-status is-submitted">Đã hoàn thành</span>
                    <span class="feedback-status is-eligible">Bạn đã tham gia hoạt động này</span>
                </div>
                <dl>
                    <div><dt>Kết thúc</dt><dd><c:out value="${dt:format(selectedActivity.endTime)}"/></dd></div>
                    <div><dt>Địa điểm</dt><dd><c:out value="${selectedActivity.address}" default="Chưa cập nhật"/></dd></div>
                </dl>
            </section>

            <c:if test="${not empty formError}">
                <div class="alert alert-error" role="alert"><c:out value="${formError}"/></div>
            </c:if>

            <c:if test="${not empty selectedFeedback}">
                <div class="feedback-readonly-note">Feedback đã gửi và không thể chỉnh sửa.</div>
            </c:if>
            <form class="feedback-form" action="${pageContext.request.contextPath}/activity-feedback"
                  method="${not empty selectedFeedback ? 'get' : 'post'}">
                <c:if test="${empty selectedFeedback}">
                    <input type="hidden" name="activityId" value="${selectedActivity.activityId}">
                </c:if>
                <p class="feedback-form-intro">Mọi câu hỏi có dấu <span class="required-mark">*</span> đều bắt buộc. Feedback được ẩn danh với người xem báo cáo; hệ thống vẫn ghi nhận trạng thái đã gửi để quản lý nhắc nhở.</p>

                <section class="feedback-form-section">
                    <div class="feedback-section-heading"><span>1</span><div><h2>Chia sẻ của bạn</h2><p>Trả lời ngắn gọn để ban tổ chức hiểu trải nghiệm của bạn.</p></div></div>
                    <div class="feedback-field">
                        <label for="bestPart">Câu 1. Điều bạn hài lòng nhất ở hoạt động này là gì? <span class="required-mark">*</span></label>
                        <textarea id="bestPart" name="bestPart" maxlength="500" minlength="5" required placeholder="Chia sẻ điều bạn thấy hài lòng..." ${not empty selectedFeedback ? 'readonly' : ''}><c:out value="${not empty selectedFeedback ? selectedFeedback.bestPart : feedbackForm.bestPart}"/></textarea>
                        <small class="feedback-character-count" data-character-count-for="bestPart" aria-live="polite"></small>
                    </div>
                    <div class="feedback-field">
                        <label for="improvement">Câu 2. Điều gì cần được cải thiện? <span class="required-mark">*</span></label>
                        <textarea id="improvement" name="improvement" maxlength="500" minlength="5" required placeholder="Góp ý điều ban tổ chức có thể cải thiện..." ${not empty selectedFeedback ? 'readonly' : ''}><c:out value="${not empty selectedFeedback ? selectedFeedback.improvement : feedbackForm.improvement}"/></textarea>
                        <small class="feedback-character-count" data-character-count-for="improvement" aria-live="polite"></small>
                    </div>
                    <div class="feedback-field">
                        <label for="favorite">Câu 3. Bạn học hỏi hoặc nhận được gì từ hoạt động? <span class="required-mark">*</span></label>
                        <textarea id="favorite" name="favorite" maxlength="500" minlength="5" required placeholder="Ví dụ: Tôi học được cách phối hợp và giao tiếp trong nhóm." ${not empty selectedFeedback ? 'readonly' : ''}><c:out value="${not empty selectedFeedback ? selectedFeedback.favorite : feedbackForm.favorite}"/></textarea>
                        <small class="feedback-character-count" data-character-count-for="favorite" aria-live="polite"></small>
                    </div>
                    <div class="feedback-field">
                        <label for="suggestions">Câu 4. Bạn có đề xuất gì cho các hoạt động sau? <span class="required-mark">*</span></label>
                        <textarea id="suggestions" name="suggestions" maxlength="500" minlength="5" required placeholder="Ví dụ: Nên bổ sung thời gian hỏi đáp với diễn giả." ${not empty selectedFeedback ? 'readonly' : ''}><c:out value="${not empty selectedFeedback ? selectedFeedback.suggestions : feedbackForm.suggestions}"/></textarea>
                        <small class="feedback-character-count" data-character-count-for="suggestions" aria-live="polite"></small>
                    </div>
                </section>

                <section class="feedback-form-section">
                    <div class="feedback-section-heading"><span>2</span><div><h2>Đánh giá hoạt động</h2><p>Chọn từ 1 đến 5 sao cho từng mục.</p></div></div>
                    <div class="feedback-rating-list">
                        <div class="feedback-rating-row">
                            <span>Đánh giá chung <span class="required-mark">*</span></span>
                            <div class="feedback-stars">
                                <c:forEach begin="1" end="5" var="star">
                                    <label class="feedback-star">
                                        <input type="radio" name="generalRating" value="${star}" required
                                            ${(not empty selectedFeedback and selectedFeedback.generalRating == star) or (empty selectedFeedback and feedbackForm.generalRating == star) ? 'checked' : ''}
                                            ${not empty selectedFeedback ? 'disabled' : ''}>
                                        <span aria-label="${star} sao">★</span>
                                    </label>
                                </c:forEach>
                            </div>
                        </div>
                        <div class="feedback-rating-row">
                            <span>Nội dung hoạt động <span class="required-mark">*</span></span>
                            <div class="feedback-stars">
                                <c:forEach begin="1" end="5" var="star">
                                    <label class="feedback-star"><input type="radio" name="contentRating" value="${star}" required
                                        ${(not empty selectedFeedback and selectedFeedback.contentRating == star) or (empty selectedFeedback and feedbackForm.contentRating == star) ? 'checked' : ''}
                                        ${not empty selectedFeedback ? 'disabled' : ''}><span aria-label="${star} sao">★</span></label>
                                </c:forEach>
                            </div>
                        </div>
                        <div class="feedback-rating-row">
                            <span>Công tác tổ chức <span class="required-mark">*</span></span>
                            <div class="feedback-stars">
                                <c:forEach begin="1" end="5" var="star">
                                    <label class="feedback-star"><input type="radio" name="organizationRating" value="${star}" required
                                        ${(not empty selectedFeedback and selectedFeedback.organizationRating == star) or (empty selectedFeedback and feedbackForm.organizationRating == star) ? 'checked' : ''}
                                        ${not empty selectedFeedback ? 'disabled' : ''}><span aria-label="${star} sao">★</span></label>
                                </c:forEach>
                            </div>
                        </div>
                        <div class="feedback-rating-row">
                            <span>Địa điểm và thời gian <span class="required-mark">*</span></span>
                            <div class="feedback-stars">
                                <c:forEach begin="1" end="5" var="star">
                                    <label class="feedback-star"><input type="radio" name="venueRating" value="${star}" required
                                        ${(not empty selectedFeedback and selectedFeedback.venueRating == star) or (empty selectedFeedback and feedbackForm.venueRating == star) ? 'checked' : ''}
                                        ${not empty selectedFeedback ? 'disabled' : ''}><span aria-label="${star} sao">★</span></label>
                                </c:forEach>
                            </div>
                        </div>
                    </div>
                </section>

                <section class="feedback-form-section feedback-note-section">
                    <div class="feedback-field">
                        <label for="note">Ghi chú thêm</label>
                        <textarea id="note" name="note" maxlength="500" placeholder="Điều bạn muốn nhắn thêm tới ban tổ chức..." ${not empty selectedFeedback ? 'readonly' : ''}><c:out value="${not empty selectedFeedback ? selectedFeedback.note : feedbackForm.note}"/></textarea>
                        <small class="feedback-character-count" data-character-count-for="note" aria-live="polite"></small>
                    </div>
                </section>

                <div class="feedback-form-actions">
                    <a class="button button-secondary" href="${pageContext.request.contextPath}/activity-feedback">Quay lại</a>
                    <c:if test="${empty selectedFeedback}">
                        <button class="button button-primary" type="submit">Gửi feedback</button>
                    </c:if>
                </div>
            </form>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/activity-feedback.js?v=20261111-11" defer></script>
</body>
</html>
