<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<section id="activity-qa" class="card activity-form-card activity-detail-card">
    <div class="activity-table-heading">
        <div><h2>Hỏi &amp; Đáp về hoạt động</h2>
            <p><c:out value="${activity.name}"/> — xem câu hỏi công khai và câu hỏi của bạn.</p>
        </div>
    </div>
    <c:if test="${param.msg == 'question-created'}">
        <p class="alert alert-success" role="status">Đã gửi câu hỏi cho hoạt động này.</p>
    </c:if>
    <form method="post" action="${pageContext.request.contextPath}/student-qa" class="activity-form">
        <input type="hidden" name="activityId" value="${activity.id}">
        <input type="hidden" name="returnTo" value="activity">
        <h3>Đặt câu hỏi</h3>
        <div class="form-group">
            <label for="activity-question-title">Tiêu đề</label>
            <input id="activity-question-title" name="title" maxlength="200" required>
        </div>
        <div class="form-group">
            <label for="activity-question-content">Nội dung</label>
            <textarea id="activity-question-content" name="content" maxlength="4000" rows="4" required></textarea>
        </div>
        <label><input type="checkbox" name="anonymous"> Gửi ẩn danh</label>
        <p>Câu hỏi mới ở chế độ riêng tư, chỉ bạn và bộ phận phụ trách xem được cho đến khi được công khai.</p>
        <button type="submit" class="button button-primary">Gửi câu hỏi</button>
    </form>
    <h3>Câu hỏi về hoạt động này</h3>
    <c:if test="${empty activityQuestions}"><p>Chưa có câu hỏi nào hiển thị. Bạn có thể đặt câu hỏi đầu tiên.</p></c:if>
    <c:forEach var="q" items="${activityQuestions}">
        <article class="card" style="padding: 20px; margin-top: 16px;">
            <p>${q.publicQuestion ? 'Công khai' : 'Riêng tư'} · ${q.status == 'ANSWERED' ? 'Đã trả lời' : 'Chờ trả lời'} · ${dt:format(q.createdAt)}</p>
            <h4><c:out value="${q.title}"/></h4>
            <p style="white-space: pre-wrap; overflow-wrap: anywhere;"><c:out value="${q.content}"/></p>
            <p><c:choose><c:when test="${q.anonymous}">Người hỏi ẩn danh</c:when><c:otherwise><c:out value="${q.studentName}"/></c:otherwise></c:choose></p>
            <c:forEach var="answer" items="${activityAnswers[q.id]}">
                <blockquote>
                    <strong>Phản hồi từ ${answer.responderType == 'PARTNER_STAFF' ? 'đối tác' : 'nhà trường'}</strong>
                    <p style="white-space: pre-wrap; overflow-wrap: anywhere;"><c:out value="${answer.content}"/></p>
                    <small><c:out value="${answer.staffName}"/> · ${dt:format(answer.publishedAt)}</small>
                </blockquote>
            </c:forEach>
        </article>
    </c:forEach>
</section>
