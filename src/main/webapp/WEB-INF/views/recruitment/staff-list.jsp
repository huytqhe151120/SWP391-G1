<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="rcTitle" value="Tuyển ban tổ chức" scope="request"/>
<c:set var="rcHero" value="Tuyển ban tổ chức" scope="request"/>
<c:set var="rcEyebrow" value="EAMS / QUẢN LÝ TUYỂN BAN TỔ CHỨC" scope="request"/>
<c:set var="rcSubtitle" value="Chọn hoạt động để quản lý đợt tuyển và hồ sơ ban tổ chức." scope="request"/>
<c:set var="rcAttendanceStyles" value="true" scope="request"/>
<%@ include file="_top.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<section class="card activity-table-card activity-form-card activity-detail-card registration-detail-card">
    <div class="activity-table-heading">
        <div>
            <h2>Hồ sơ ứng tuyển &amp; Thông tin đợt tuyển</h2>
            <p><c:choose>
                <c:when test="${not empty activity}"><c:out value="${activity.code}"/> – <c:out value="${activity.name}"/></c:when>
                <c:otherwise>Chọn hoạt động để xem thông tin đợt tuyển và hồ sơ ứng tuyển.</c:otherwise>
            </c:choose></p>
        </div>
        <div class="activity-heading-actions">
            <c:if test="${not empty activity}">
                <a class="button button-sm" href="${ctx}/activities?action=detail&amp;id=${activity.id}">Xem hoạt động</a>
            </c:if>
            <c:if test="${not empty round}">
                <span class="activity-count"><c:out value="${round.applicationCount}"/> hồ sơ hiệu lực</span>
                <span class="badge status-pill ${roundBadges[round.status]}">${roundLabels[round.status]}</span>
                <a class="button button-sm" href="${ctx}/organizer-recruitment?action=edit&amp;id=${round.id}">Chỉnh sửa</a>
                <details class="action-menu">
                    <summary aria-label="Thao tác khác"><span aria-hidden="true">⋯</span></summary>
                    <div class="action-menu__items">
                        <c:if test="${round.status ne 'OPEN'}">
                            <form method="post" action="${ctx}/organizer-recruitment">
                                <input type="hidden" name="action" value="open"><input type="hidden" name="id" value="${round.id}">
                                <button class="action-menu-item action-menu-item--submit" type="submit">Mở nhận hồ sơ</button>
                            </form>
                        </c:if>
                        <c:if test="${round.status eq 'OPEN'}">
                            <form method="post" action="${ctx}/organizer-recruitment" onsubmit="return confirm('Đóng đợt tuyển này?');">
                                <input type="hidden" name="action" value="close"><input type="hidden" name="id" value="${round.id}">
                                <button class="action-menu-item action-menu-item--undo" type="submit">Đóng đợt</button>
                            </form>
                        </c:if>
                        <form method="post" action="${ctx}/organizer-recruitment" onsubmit="return confirm('Xóa đợt tuyển này?');">
                            <input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="${round.id}">
                            <button class="action-menu-item action-menu-item--delete" type="submit">Xóa đợt tuyển</button>
                        </form>
                    </div>
                </details>
            </c:if>
            <c:if test="${not empty activity and empty round}">
                <a class="button button-sm button-primary" href="${ctx}/organizer-recruitment?action=create&amp;activityId=${activity.id}">Tạo đợt tuyển</a>
            </c:if>
        </div>
    </div>
    <details class="activity-filter-panel" open>
        <summary class="activity-filter-toggle"><span>Chọn hoạt động</span></summary>
        <form class="activity-filter attendance-filter attendance-filter--single" action="${ctx}/organizer-recruitment" method="get">
            <div class="activity-filter-field attendance-filter-activity">
                <label id="recruitment-activity-label">Hoạt động</label>
                <div class="multi-select-combobox" data-multi-select data-single data-auto-submit
                     data-item-label="hoạt động" data-input-name="activityId">
                    <button type="button" class="multi-select-trigger" aria-expanded="false"
                            aria-controls="recruitment-activity-options" aria-labelledby="recruitment-activity-label">
                        <span data-selection-label>-- Chọn hoạt động --</span><span aria-hidden="true">▾</span>
                    </button>
                    <div class="multi-select-dropdown" id="recruitment-activity-options" hidden>
                        <input class="multi-select-search" type="search" placeholder="Tìm mã hoặc tên hoạt động..." aria-label="Tìm hoạt động">
                        <fieldset class="multi-select-options">
                            <legend class="visually-hidden">Chọn hoạt động</legend>
                            <c:forEach var="item" items="${activities}">
                                <label class="multi-select-option">
                                    <input type="radio" name="activityId" value="${item.id}" ${activity.id == item.id ? 'checked' : ''}>
                                    <span><c:out value="${item.code}"/> – <c:out value="${item.name}"/><c:if test="${not empty item.semesterName}"> (<c:out value="${item.semesterName}"/>)</c:if><c:if test="${not empty item.organizationLevelName}"> [<c:out value="${item.organizationLevelName}"/>]</c:if></span>
                                </label>
                            </c:forEach>
                        </fieldset>
                        <div class="multi-select-empty" hidden>Không tìm thấy hoạt động phù hợp.</div>
                    </div>
                    <span class="multi-select-count" aria-live="polite">Chưa chọn hoạt động</span>
                </div>
            </div>
            <noscript>
                <style>.multi-select-combobox { display: none; }</style>
                <label for="recruitment-activity-fallback">Hoạt động</label>
                <select id="recruitment-activity-fallback" name="activityIdFallback">
                    <option value="">-- Chọn hoạt động --</option>
                    <c:forEach var="item" items="${activities}">
                        <option value="${item.id}" ${activity.id == item.id ? 'selected' : ''}><c:out value="${item.code}"/> – <c:out value="${item.name}"/></option>
                    </c:forEach>
                </select>
                <button type="submit" class="button button-primary button-sm">Xem thông tin</button>
            </noscript>
        </form>
    </details>
    <c:choose>
        <c:when test="${empty activity}">
            <div class="empty-state activity-empty-state">
                <span class="empty-state-mark" aria-hidden="true"><svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M19 8v6"/><path d="M16 11h6"/></svg></span>
                <strong>Chọn hoạt động để xem đợt tuyển</strong>
                <span>Thông tin đợt tuyển và hồ sơ ứng tuyển sẽ hiển thị tại đây.</span>
            </div>
        </c:when>
        <c:when test="${empty round}">
            <div class="empty-state activity-empty-state">
                <span class="empty-state-mark" aria-hidden="true"><svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14"/><path d="M5 12h14"/></svg></span>
                <strong>Hoạt động chưa có đợt tuyển ban tổ chức</strong>
                <span>Chỉ hoạt động đã duyệt và chưa diễn ra mới được tạo đợt tuyển.</span>
                <a class="button button-primary" href="${ctx}/organizer-recruitment?action=create&amp;activityId=${activity.id}">Tạo đợt tuyển</a>
            </div>
        </c:when>
    </c:choose>
</section>
<c:if test="${not empty round}">
    <%@ include file="_applications.jspf" %>
</c:if>
<script src="${ctx}/assets/js/activity.js?v=20261107-5"></script>
<script>
    document.querySelectorAll('.multi-select-combobox[data-auto-submit]').forEach(function (combobox) {
        combobox.addEventListener('change', function (event) {
            if (event.target.type === 'radio' && event.target.checked) combobox.closest('form').submit();
        });
    });
</script>
<%@ include file="_bottom.jspf" %>
