<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="rcTitle" value="Đơn đăng ký của tôi" scope="request"/>
<c:set var="rcHero" value="Đơn đăng ký của tôi" scope="request"/>
<c:set var="rcEyebrow" value="EAMS / ĐƠN ĐĂNG KÝ" scope="request"/>
<c:set var="rcSubtitle" value="Theo dõi đơn tham gia và hồ sơ ứng tuyển ban tổ chức." scope="request"/>
<%@ include file="_top.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<section class="card rc-card" data-page-container data-role-filter>
    <h2>Đơn của bạn</h2>
    <div class="rc-tabs" style="margin-top:10px">
        <button type="button" class="rc-tab active" data-filter="all">Tất cả (${rows.size()})</button>
        <button type="button" class="rc-tab" data-filter="JOIN">Người tham gia (${joinCount})</button>
        <button type="button" class="rc-tab" data-filter="BTC">Ban tổ chức (${organizerCount})</button>
    </div>
    <div class="table-wrap">
        <table class="rc-table">
            <thead>
            <tr><th>Hoạt động</th><th>Vai trò</th><th>Ngày gửi</th><th>Trạng thái</th><th>Thao tác</th></tr>
            </thead>
            <tbody>
            <c:forEach items="${rows}" var="a">
                <tr data-role="${a.role}" data-page-item>
                    <td><strong><c:out value="${a.activityName}"/></strong></td>
                    <td><span class="rc-role ${a.role eq 'JOIN' ? 'join' : ''}">${a.role eq 'JOIN' ? 'Người tham gia' : 'Ban tổ chức'}</span></td>
                    <td><c:choose><c:when test="${not empty a.submittedAt}"><c:out value="${dt:format(a.submittedAt)}"/></c:when><c:otherwise>—</c:otherwise></c:choose></td>
                    <td>
                        <c:choose>
                            <c:when test="${a.role eq 'BTC'}"><span class="badge status-pill ${appBadges[a.status]}">${appLabels[a.status]}</span></c:when>
                            <c:when test="${a.status eq 'SUBMITTED'}"><span class="badge status-pill status-yellow">Chờ xử lý</span></c:when>
                            <c:when test="${a.status eq 'APPROVED'}"><span class="badge status-pill status-green">Đã duyệt</span></c:when>
                            <c:when test="${a.status eq 'REJECTED'}"><span class="badge status-pill status-red">Từ chối</span></c:when>
                            <c:when test="${a.status eq 'CANCELLED'}"><span class="badge status-pill status-gray">Đã hủy đăng ký</span></c:when>
                            <c:when test="${a.status eq 'WITHDRAWN'}"><span class="badge status-pill status-blue">Đã rút đơn</span></c:when>
                            <c:otherwise><span class="badge status-pill status-gray"><c:out value="${a.status}"/></span></c:otherwise>
                        </c:choose>
                    </td>
                    <td><a href="${a.url}">${a.role eq 'BTC' ? 'Xem tiến trình' : 'Xem đơn'}</a></td>
                </tr>
            </c:forEach>
            <c:if test="${empty rows}"><tr><td colspan="5" class="rc-sub">Bạn chưa có đơn đăng ký nào.</td></tr></c:if>
            <c:if test="${not empty rows}"><tr data-filter-empty hidden><td colspan="5" class="rc-sub">Không có đơn đăng ký nào trong vai trò này.</td></tr></c:if>
            </tbody>
        </table>
    </div>
    <div class="activity-pagination" data-pagination hidden>
        <span class="activity-pagination-summary" aria-live="polite"></span>
        <nav class="activity-pagination-nav" aria-label="Phân trang đơn đăng ký"></nav>
    </div>
</section>

<c:if test="${not empty selected}">
    <c:set var="iv" value="${selected.interview}"/>
    <section class="card rc-card rc-progress-card" id="progress">
        <div class="rc-head">
            <div>
                <span class="rc-sub" style="text-transform:uppercase;font-size:.7rem;font-weight:700;letter-spacing:.05em">Tiến trình ứng tuyển</span>
                <h2><c:out value="${selected.activityName}"/> · <c:out value="${empty selected.acceptedPositionName ? selected.choice1Name : selected.acceptedPositionName}"/></h2>
            </div>
            <span class="badge status-pill ${appBadges[selected.status]}">${appLabels[selected.status]}</span>
        </div>
        <div class="rc-stepper">
            <c:forEach items="${steps}" var="s">
                <div><span class="rc-dot ${s.state}">${s.state eq 'done' ? '✓' : (s.state eq 'fail' ? '✕' : s.no)}</span>${s.label}<small><c:out value="${s.sub}"/></small></div>
            </c:forEach>
        </div>

        <c:if test="${selected.status eq 'INTERVIEW_INVITED' and not empty iv}">
            <div class="rc-interview">
                <div>
                    <span><b>Thời gian:</b> <c:out value="${dt:format(iv.scheduledAt)}"/></span>
                    <span><b>Hình thức:</b> ${iv.format eq 'ONLINE' ? 'Trực tuyến' : 'Trực tiếp'}</span>
                    <span><b>${iv.format eq 'ONLINE' ? 'Liên kết' : 'Địa điểm'}:</b> <c:out value="${iv.location}"/></span>
                    <span><b>Hạn xác nhận:</b> <c:out value="${dt:format(iv.confirmDeadline)}"/></span>
                    <c:if test="${not empty iv.note}"><span><b>Ghi chú:</b> <c:out value="${iv.note}"/></span></c:if>
                </div>
                <div style="display:flex;gap:10px">
                    <c:choose>
                        <c:when test="${iv.status eq 'SCHEDULED' and not iv.confirmOverdue}">
                            <form method="post" action="${ctx}/organizer-recruitment">
                                <input type="hidden" name="action" value="confirm">
                                <input type="hidden" name="applicationId" value="${selected.id}">
                                <button type="submit" class="button button-primary">Xác nhận tham dự</button>
                            </form>
                        </c:when>
                        <c:when test="${iv.status eq 'CONFIRMED'}"><span class="badge status-pill status-green">Đã xác nhận tham dự</span></c:when>
                        <c:when test="${iv.status eq 'SCHEDULED'}"><span class="badge status-pill status-red">Quá hạn xác nhận, vui lòng liên hệ staff</span></c:when>
                    </c:choose>
                </div>
            </div>
            <p class="rc-note">Hồ sơ đã được mời phỏng vấn. Nếu muốn rút, vui lòng báo staff.</p>
        </c:if>

        <c:if test="${selected.status eq 'PENDING'}">
            <form class="rc-interview" method="post" action="${ctx}/organizer-recruitment"
                  onsubmit="return confirm('Bạn chắc chắn muốn rút hồ sơ?');">
                <input type="hidden" name="action" value="withdraw">
                <input type="hidden" name="applicationId" value="${selected.id}">
                <div><span>Bạn có thể rút hồ sơ cho đến khi được mời phỏng vấn.</span></div>
                <button type="submit" class="button">Rút hồ sơ</button>
            </form>
        </c:if>

        <c:if test="${selected.status eq 'ACCEPTED'}">
            <c:choose>
                <c:when test="${not selectedAcceptedLocked}">
                    <form class="rc-interview" method="post" action="${ctx}/organizer-recruitment"
                          onsubmit="return confirm('Bạn chắc chắn muốn rút khỏi ban tổ chức?');">
                        <input type="hidden" name="action" value="withdraw">
                        <input type="hidden" name="applicationId" value="${selected.id}">
                        <div class="rc-field" style="flex:1"><label for="reason">Lý do rút khỏi ban tổ chức <em>*</em></label>
                            <input id="reason" name="reason" maxlength="500" required></div>
                        <button type="submit" class="button">Rút khỏi ban tổ chức</button>
                    </form>
                    <p class="rc-note">Bạn có thể rút đến 48 giờ trước hoạt động. Chỉ tiêu sẽ được trả lại và staff sẽ tìm người thay thế.</p>
                </c:when>
                <c:otherwise>
                    <p class="rc-note">Đã quá mốc 48 giờ trước hoạt động. Nếu không thể tham gia, vui lòng liên hệ staff.</p>
                </c:otherwise>
            </c:choose>
        </c:if>
        <c:if test="${selected.status eq 'REJECTED' and not empty selected.decisionReason and false}"></c:if>
    </section>
</c:if>
<script src="${ctx}/assets/js/recruitment-pagination.js?v=20261113-1"></script>
<%@ include file="_bottom.jspf" %>
