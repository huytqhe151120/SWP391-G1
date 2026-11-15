<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="rcTitle" value="Tuyển ban tổ chức" scope="request"/>
<c:set var="rcHero" value="Tuyển ban tổ chức" scope="request"/>
<c:set var="rcEyebrow" value="EAMS / QUẢN LÝ TUYỂN BAN TỔ CHỨC" scope="request"/>
<c:set var="rcSubtitle" value="Chọn hoạt động để xem thông tin đợt tuyển và quản lý hồ sơ ứng tuyển." scope="request"/>
<c:set var="rcAttendanceStyles" value="true" scope="request"/>
<%@ include file="_top.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<section class="rc-activity-picker card">
    <div class="activity-table-heading rc-page-heading">
        <div>
            <h2>Danh sách hồ sơ ứng tuyển &amp; Thông tin đợt tuyển</h2>
            <p>
                <c:choose>
                    <c:when test="${not empty activity}"><c:out value="${activity.code}"/> – <c:out value="${activity.name}"/></c:when>
                    <c:otherwise>Chọn hoạt động để xem thông tin đợt tuyển và danh sách hồ sơ ứng tuyển.</c:otherwise>
                </c:choose>
            </p>
        </div>
        <c:if test="${not empty activity and empty round}">
            <div class="activity-heading-actions">
                <a class="button button-sm button-primary" href="${ctx}/organizer-recruitment?action=create&amp;activityId=${activity.id}">Tạo mới đợt tuyển</a>
            </div>
        </c:if>
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
            <div class="rc-picker-empty">
                <span class="rc-picker-empty-icon" aria-hidden="true">
                    <svg viewBox="0 0 32 32" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="13" cy="10" r="5"/>
                        <path d="M3.5 27v-2a9.5 9.5 0 0 1 19 0v2"/>
                        <path d="M25 10v10M20 15h10"/>
                    </svg>
                </span>
                <strong>Chọn hoạt động để xem đợt tuyển</strong>
                <span>Thông tin đợt tuyển và hồ sơ ứng tuyển sẽ hiển thị tại đây.</span>
            </div>
        </c:when>
        <c:when test="${empty round}">
            <div class="empty-state activity-empty-state registration-empty-state registration-detail-empty-state">
                <span class="empty-state-mark" aria-hidden="true">+</span>
                <strong>Chưa có thông tin đợt tuyển</strong>
                <span>Hoạt động này chưa tạo đợt tuyển ban tổ chức. Bạn có thể tạo mới đợt tuyển cho hoạt động này.</span>
                <a class="button button-primary" href="${ctx}/organizer-recruitment?action=create&amp;activityId=${activity.id}">Tạo mới đợt tuyển</a>
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
