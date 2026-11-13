<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="rcTitle" value="Tuyển ban tổ chức" scope="request"/>
<c:set var="rcHero" value="Tuyển ban tổ chức" scope="request"/>
<c:set var="rcEyebrow" value="EAMS / TUYỂN BAN TỔ CHỨC" scope="request"/>
<c:set var="rcSubtitle" value="Ứng tuyển vào ban tổ chức của các hoạt động ngoại khóa." scope="request"/>
<%@ include file="_top.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="tab" value="${param.tab eq 'applied' ? 'applied' : 'open'}"/>
<section class="card rc-card">
    <div class="rc-head">
        <div>
            <h2>Đợt tuyển của bạn</h2>
            <p class="rc-sub">Chỉ hiện hoạt động đang nhận hồ sơ ban tổ chức</p>
        </div>
        <span class="rc-pill">${rounds.size()} đợt đang mở</span>
    </div>

    <div class="rc-tabs" role="tablist">
        <button type="button" class="rc-tab ${tab eq 'open' ? 'active' : ''}" data-tab="open">Đang tuyển (${rounds.size()})</button>
        <button type="button" class="rc-tab ${tab eq 'applied' ? 'active' : ''}" data-tab="applied">Đã ứng tuyển (${appliedApplications.size()})</button>
    </div>

    <div class="rc-panel" data-panel="open" data-page-container ${tab eq 'open' ? '' : 'hidden'}>
        <c:choose>
            <c:when test="${empty rounds}">
                <div class="empty-state activity-empty-state">
                    <span class="empty-state-mark" aria-hidden="true">+</span>
                    <strong>Hiện chưa có đợt tuyển nào</strong>
                    <span>Chưa có đợt tuyển nào đang nhận hồ sơ ban tổ chức.</span>
                </div>
            </c:when>
            <c:otherwise>
        <c:forEach items="${rounds}" var="r">
            <c:set var="applied" value="${appliedRoundIds.contains(r.id)}"/>
            <div class="rc-item" data-page-item>
                <div>
                    <strong><c:out value="${r.activityName}"/></strong>
                    <small>
                        <c:choose>
                            <c:when test="${not empty r.closeAt}">Nhận hồ sơ đến <c:out value="${dt:format(r.closeAt)}"/></c:when>
                            <c:otherwise>Không giới hạn thời gian nhận hồ sơ</c:otherwise>
                        </c:choose>
                        · ${r.positions.size()} vị trí · còn ${r.totalRemaining} chỉ tiêu
                    </small>
                </div>
                <div class="rc-item-actions">
                    <span class="badge status-pill ${applied ? 'status-yellow' : (r.totalRemaining > 0 ? 'status-green' : 'status-gray')}">
                        ${applied ? 'Đã nộp hồ sơ' : (r.totalRemaining > 0 ? 'Đang mở' : 'Hết chỉ tiêu')}</span>
                    <c:choose>
                        <c:when test="${applied}">
                            <a class="button" href="${ctx}/organizer-recruitment?action=myApplications">Xem đơn</a>
                        </c:when>
                        <c:when test="${r.totalRemaining > 0}">
                            <a class="button button-primary" href="${ctx}/organizer-recruitment?action=apply&amp;roundId=${r.id}">Ứng tuyển</a>
                        </c:when>
                    </c:choose>
                </div>
            </div>
        </c:forEach>
            </c:otherwise>
        </c:choose>
        <div class="activity-pagination" data-pagination hidden>
            <span class="activity-pagination-summary" aria-live="polite"></span>
            <nav class="activity-pagination-nav" aria-label="Phân trang đợt đang tuyển"></nav>
        </div>
    </div>

    <div class="rc-panel" data-panel="applied" data-page-container ${tab eq 'applied' ? '' : 'hidden'}>
        <c:choose>
            <c:when test="${empty appliedApplications}">
                <div class="empty-state activity-empty-state">
                    <span class="empty-state-mark" aria-hidden="true">+</span>
                    <strong>Chưa nộp hồ sơ nào</strong>
                    <span>Bạn chưa nộp hồ sơ ứng tuyển ban tổ chức nào.</span>
                </div>
            </c:when>
            <c:otherwise>
        <c:forEach items="${appliedApplications}" var="a">
            <div class="rc-item" data-page-item>
                <div>
                    <strong><c:out value="${a.activityName}"/></strong>
                    <small>Đã nộp <c:out value="${dt:format(a.submittedAt)}"/> · Nguyện vọng 1: <c:out value="${a.choice1Name}"/></small>
                </div>
                <div class="rc-item-actions">
                    <span class="badge status-pill ${appBadges[a.status]}">${appLabels[a.status]}</span>
                    <a class="button" href="${ctx}/organizer-recruitment?action=myApplications&amp;id=${a.id}#progress">Xem đơn</a>
                </div>
            </div>
        </c:forEach>
            </c:otherwise>
        </c:choose>
        <div class="activity-pagination" data-pagination hidden>
            <span class="activity-pagination-summary" aria-live="polite"></span>
            <nav class="activity-pagination-nav" aria-label="Phân trang đợt đã ứng tuyển"></nav>
        </div>
    </div>

    <p class="rc-note">Mỗi hoạt động bạn chỉ giữ một vai trò. Nếu trúng tuyển ban tổ chức, đăng ký tham gia của bạn ở hoạt động đó (nếu có) sẽ được hủy.</p>
</section>
<script>
    document.querySelectorAll('.rc-tab').forEach(function (btn) {
        btn.addEventListener('click', function () {
            document.querySelectorAll('.rc-tab').forEach(function (b) { b.classList.toggle('active', b === btn); });
            document.querySelectorAll('.rc-panel').forEach(function (p) { p.hidden = p.dataset.panel !== btn.dataset.tab; });
        });
    });
</script>
<script src="${ctx}/assets/js/recruitment-pagination.js?v=20261113-1"></script>
<%@ include file="_bottom.jspf" %>
