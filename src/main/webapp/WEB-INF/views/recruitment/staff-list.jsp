<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="rcTitle" value="Tuyển ban tổ chức" scope="request"/>
<c:set var="rcHero" value="Tuyển ban tổ chức" scope="request"/>
<c:set var="rcEyebrow" value="EAMS / QUẢN LÝ TUYỂN BAN TỔ CHỨC" scope="request"/>
<c:set var="rcSubtitle" value="Chọn hoạt động để xem thông tin đợt tuyển và quản lý hồ sơ ứng tuyển." scope="request"/>
<c:set var="rcAttendanceStyles" value="true" scope="request"/>
<c:set var="activeNav" value="recruitment" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${rcTitle}"/> | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261107-2">
    <c:if test="${rcAttendanceStyles}">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/attendance.css?v=20261110-1">
    </c:if>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/registration.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/recruitment.css?v=20261115-6">
</head>
<body>
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
                </header>
            </c:if>
            <c:if test="${not empty sessionScope.successMessage}">
                <div class="alert alert-success activity-alert"><c:out value="${sessionScope.successMessage}"/></div>
                <c:remove var="successMessage" scope="session"/>
            </c:if>
            <c:if test="${not empty sessionScope.errorMessage}">
                <div class="alert alert-error activity-alert"><c:out value="${sessionScope.errorMessage}"/></div>
                <c:remove var="errorMessage" scope="session"/>
            </c:if>
            <c:if test="${not empty error}">
                <div class="alert alert-error activity-alert"><c:out value="${error}"/></div>
            </c:if>
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
<c:set var="base" value="${ctx}/organizer-recruitment?action=applications&id=${round.id}"/>
<c:set var="pq">${positionFilter != null ? '&positionId=' : ''}${positionFilter}</c:set>
<section class="card rc-round-summary">
    <div class="rc-round-summary-top">
        <div>
            <div class="rc-round-title-line">
                <h2><c:out value="${round.title}"/></h2>
                <span class="badge status-pill ${roundBadges[round.status]}">${roundLabels[round.status]}</span>
            </div>
            <p class="rc-round-activity"><c:out value="${activity.code}"/> – <c:out value="${activity.name}"/></p>
        </div>
        <div class="rc-round-actions">
            <a class="button button-sm" href="${ctx}/activities?action=detail&amp;id=${activity.id}">Xem hoạt động</a>
            <a class="button button-sm" href="${ctx}/organizer-recruitment?action=edit&amp;id=${round.id}">Chỉnh sửa</a>
            <details class="action-menu">
                <summary aria-label="Thao tác khác"><span aria-hidden="true">⋯</span></summary>
                <div class="action-menu__items">
                    <form method="post" action="${ctx}/organizer-recruitment" onsubmit="return confirm('Xóa đợt tuyển này?');">
                        <input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="${round.id}">
                        <button class="action-menu-item action-menu-item--delete" type="submit">Xóa đợt tuyển</button>
                    </form>
                </div>
            </details>
        </div>
    </div>
    <div class="rc-round-meta">
        <div><span>Mở nhận hồ sơ</span><strong><c:out value="${not empty round.openAt ? dt:format(round.openAt) : 'Chưa thiết lập'}"/></strong></div>
        <div><span>Đóng nhận hồ sơ</span><strong><c:out value="${not empty round.closeAt ? dt:format(round.closeAt) : 'Chưa thiết lập'}"/></strong></div>
        <div><span>Phỏng vấn</span><strong><span class="rc-meta-badge">${round.interviewRequired ? 'Có phỏng vấn' : 'Không phỏng vấn'}</span></strong></div>
        <div><span>Hồ sơ hiệu lực</span><strong><c:out value="${round.applicationCount}"/></strong></div>
    </div>
    <details class="rc-round-description">
        <summary>Mô tả, câu hỏi bổ sung và lịch gợi ý</summary>
        <div>
            <c:if test="${not empty round.description}"><p><c:out value="${round.description}"/></p></c:if>
            <c:if test="${not empty round.questions}">
                <ol>
                    <c:forEach items="${round.questions}" var="question">
                        <li><c:out value="${question.questionText}"/>
                            <small><c:out value="${question.questionType}"/>${question.required ? ' · Bắt buộc' : ' · Không bắt buộc'}</small>
                        </li>
                    </c:forEach>
                </ol>
            </c:if>
            <c:if test="${empty round.description and empty round.questions}"><p>Đợt tuyển chưa có mô tả hoặc câu hỏi bổ sung.</p></c:if>
        </div>
    </details>
</section>

<div class="rc-stats">
    <c:forEach items="${round.positions}" var="p">
        <div class="card rc-stat">
            <span><c:out value="${p.name}"/></span>
            <strong>${p.acceptedCount} / ${p.quota}</strong>
            <div class="rc-bar"><i style="width:${p.quota > 0 ? p.acceptedCount * 100 / p.quota : 0}%"></i></div>
            <small class="rc-sub">đã trúng tuyển · còn ${p.remaining}</small>
        </div>
    </c:forEach>
</div>

<c:set var="canInviteSelected" value="${round.interviewRequired and not empty selected and (selected.status eq 'PENDING' or selected.status eq 'INTERVIEW_INVITED')}"/>
<div class="rc-layout">
    <div>
        <section class="card rc-card rc-applications-card" style="margin:0 0 18px" data-page-container data-applications data-can-invite="${canInviteSelected}" data-position-filter="${positionFilter}">
            <nav class="rc-tabs" aria-label="Lọc hồ sơ theo trạng thái">
                <a class="rc-tab ${statusFilter eq 'all' ? 'active' : ''}" href="${base}${pq}">Tất cả (${allCount})</a>
                <a class="rc-tab ${statusFilter eq 'PENDING' ? 'active' : ''}" href="${base}&status=PENDING${pq}">Chờ xét (${counts['PENDING'] == null ? 0 : counts['PENDING']})</a>
                <a class="rc-tab ${statusFilter eq 'INTERVIEW_INVITED' ? 'active' : ''}" href="${base}&status=INTERVIEW_INVITED${pq}">Mời phỏng vấn (${counts['INTERVIEW_INVITED'] == null ? 0 : counts['INTERVIEW_INVITED']})</a>
                <a class="rc-tab ${statusFilter eq 'INTERVIEWED' ? 'active' : ''}" href="${base}&status=INTERVIEWED${pq}">Đã phỏng vấn (${counts['INTERVIEWED'] == null ? 0 : counts['INTERVIEWED']})</a>
                <a class="rc-tab ${statusFilter eq 'ACCEPTED' ? 'active' : ''}" href="${base}&status=ACCEPTED${pq}">Trúng tuyển (${counts['ACCEPTED'] == null ? 0 : counts['ACCEPTED']})</a>
                <a class="rc-tab ${statusFilter eq 'REJECTED' ? 'active' : ''}" href="${base}&status=REJECTED${pq}">Không đạt (${counts['REJECTED'] == null ? 0 : counts['REJECTED']})</a>
                <a class="rc-tab ${statusFilter eq 'WITHDRAWN' ? 'active' : ''}" href="${base}&status=WITHDRAWN${pq}">Đã rút (${counts['WITHDRAWN'] == null ? 0 : counts['WITHDRAWN']})</a>
            </nav>
            <form class="rc-filter-toolbar" method="get" action="${ctx}/organizer-recruitment">
                <input type="hidden" name="action" value="applications">
                <input type="hidden" name="id" value="${round.id}">
                <input type="hidden" name="status" value="${statusFilter}">
                <input type="search" data-application-search placeholder="Tìm theo mã hoặc tên" aria-label="Tìm hồ sơ theo mã hoặc tên">
                <select id="positionId" name="positionId" aria-label="Lọc theo vị trí" onchange="this.form.submit()">
                    <option value="">Tất cả ban</option>
                    <c:forEach items="${round.positions}" var="p"><option value="${p.id}" ${positionFilter == p.id ? 'selected' : ''}><c:out value="${p.name}"/></option></c:forEach>
                </select>
                <c:if test="${round.interviewRequired}">
                    <button class="button button-primary button-sm" type="submit" form="interviewForm" data-bulk-invite ${not canInviteSelected ? 'disabled' : ''}>Mời phỏng vấn đã chọn</button>
                </c:if>
            </form>
            <div class="table-wrap rc-applications-table-wrap">
                <table class="rc-table rc-applications-table">
                    <thead>
                    <tr>
                        <th></th>
                        <th>Ứng viên</th>
                        <th>Nguyện vọng</th>
                        <th>Ngày nộp</th>
                        <th>Trạng thái</th>
                        <th>Phỏng vấn</th>
                        <th>Thao tác</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${apps}" var="a">
                        <c:set var="iv" value="${a.interview}"/>
                        <tr data-page-item class="${selected != null and selected.id == a.id ? 'selected' : ''}">
                            <td><c:if test="${a.status eq 'PENDING' and round.interviewRequired}"><input type="checkbox" name="ids" value="${a.id}" form="interviewForm" aria-label="Chọn hồ sơ"></c:if></td>
                            <td><small class="rc-sub"><c:out value="${a.studentCode}"/></small><br><strong><c:out value="${a.studentName}"/></strong></td>
                            <td>1. <c:out value="${a.choice1Name}"/><c:if test="${not empty a.choice2Name}"><br>2. <c:out value="${a.choice2Name}"/></c:if></td>
                            <td><c:out value="${dt:format(a.submittedAt)}"/></td>
                            <td><span class="badge status-pill ${appBadges[a.status]}">${appLabels[a.status]}</span>
                                <c:if test="${a.status eq 'ACCEPTED'}"><br><small class="rc-sub"><c:out value="${a.acceptedPositionName}"/></small></c:if></td>
                            <td class="${selected != null and selected.id == a.id ? '' : 'rc-interview-disabled'}">
                                <c:choose>
                                    <c:when test="${selected != null and selected.id == a.id}">
                                        <c:choose>
                                            <c:when test="${not empty iv and iv.status ne 'CANCELLED'}">
                                                <c:out value="${dt:format(iv.scheduledAt)}"/><br>
                                                <c:choose>
                                                    <c:when test="${iv.status eq 'DONE'}"><small class="rc-sub">Đã xong</small></c:when>
                                                    <c:when test="${iv.status eq 'CONFIRMED'}"><small class="rc-sub">Đã xác nhận</small></c:when>
                                                    <c:when test="${iv.confirmOverdue}"><span class="rc-overdue">Quá hạn xác nhận</span></c:when>
                                                    <c:otherwise><small class="rc-sub">Chờ xác nhận</small></c:otherwise>
                                                </c:choose>
                                            </c:when>
                                            <c:otherwise><small class="rc-sub">Chưa mời</small></c:otherwise>
                                        </c:choose>
                                    </c:when>
                                    <c:otherwise><small class="rc-sub" aria-disabled="true">Mở hồ sơ để xem</small></c:otherwise>
                                </c:choose>
                            </td>
                            <td style="white-space:nowrap"><a href="${base}&appId=${a.id}&status=${statusFilter}${pq}#detail">Xem hồ sơ</a></td>
                        </tr>
                    </c:forEach>
                    <c:choose>
                        <c:when test="${empty apps}"><tr data-filter-empty><td colspan="7" class="rc-sub">Không có hồ sơ nào.</td></tr></c:when>
                        <c:otherwise><tr data-filter-empty hidden><td colspan="7" class="rc-sub">Không có hồ sơ nào phù hợp.</td></tr></c:otherwise>
                    </c:choose>
                    </tbody>
                </table>
            </div>
            <div class="activity-pagination rc-pagination" data-pagination hidden>
                <span class="activity-pagination-summary" aria-live="polite"></span>
                <nav class="activity-pagination-nav" aria-label="Phân trang hồ sơ ứng tuyển"></nav>
            </div>
            <c:if test="${round.interviewRequired}"><p class="rc-selection-note">Chọn hồ sơ đang chờ xét để mời phỏng vấn. <span data-selection-count aria-live="polite"></span></p></c:if>
        </section>

        <c:if test="${not empty selected}">
            <c:set var="iv" value="${selected.interview}"/>
            <section class="card rc-card" id="detail" style="margin:0">
                <div class="rc-head">
                    <div>
                        <h2><c:out value="${selected.studentName}"/> <small class="rc-sub">(<c:out value="${selected.studentCode}"/>)</small></h2>
                        <p class="rc-sub">Nộp lúc <c:out value="${dt:format(selected.submittedAt)}"/></p>
                    </div>
                    <span class="badge status-pill ${appBadges[selected.status]}">${appLabels[selected.status]}</span>
                </div>
                <p><b>Nguyện vọng 1:</b> <c:out value="${selected.choice1Name}"/> <small class="rc-sub">(còn ${remaining[selected.choice1PositionId]} chỉ tiêu)</small></p>
                <c:if test="${not empty selected.choice2Name}">
                    <p><b>Nguyện vọng 2:</b> <c:out value="${selected.choice2Name}"/> <small class="rc-sub">(còn ${remaining[selected.choice2PositionId]} chỉ tiêu)</small></p>
                </c:if>
                <c:if test="${not empty selected.note}"><p><b>Ghi chú của sinh viên:</b> <c:out value="${selected.note}"/></p></c:if>
                <c:forEach items="${selected.answers}" var="ans">
                    <p><b><c:out value="${ans.questionText}"/></b><br><c:out value="${ans.answerValue}"/></p>
                </c:forEach>

                <c:if test="${not empty iv and iv.status ne 'CANCELLED'}">
                    <div class="rc-interview" style="margin:12px 0">
                        <div>
                            <span><b>Thời gian:</b> <c:out value="${dt:format(iv.scheduledAt)}"/></span>
                            <span><b>Hình thức:</b> ${iv.format eq 'ONLINE' ? 'Trực tuyến' : 'Trực tiếp'}</span>
                            <span><b>${iv.format eq 'ONLINE' ? 'Liên kết' : 'Địa điểm'}:</b> <c:out value="${iv.location}"/></span>
                            <span><b>Hạn xác nhận:</b> <c:out value="${dt:format(iv.confirmDeadline)}"/></span>
                            <c:if test="${iv.confirmOverdue}"><span class="rc-overdue">Quá hạn xác nhận</span></c:if>
                            <c:if test="${not empty iv.evaluation}"><span><b>Nhận xét:</b> <c:out value="${iv.evaluation}"/></span></c:if>
                        </div>
                    </div>
                </c:if>

                <form method="post" action="${ctx}/organizer-recruitment" class="rc-field" style="margin:12px 0">
                    <input type="hidden" name="action" value="note">
                    <input type="hidden" name="roundId" value="${round.id}">
                    <input type="hidden" name="applicationId" value="${selected.id}">
                    <input type="hidden" name="status" value="${statusFilter}">
                    <label for="internalNote">Ghi chú nội bộ <span>(sinh viên không thấy)</span></label>
                    <textarea id="internalNote" name="internalNote" maxlength="1000"><c:out value="${selected.internalNote}"/></textarea>
                    <div><button type="submit" class="button button-sm">Lưu ghi chú</button></div>
                </form>

                <c:if test="${selected.status eq 'INTERVIEW_INVITED'}">
                    <form method="post" action="${ctx}/organizer-recruitment" class="rc-field" style="margin:12px 0">
                        <input type="hidden" name="action" value="completeInterview">
                        <input type="hidden" name="roundId" value="${round.id}">
                        <input type="hidden" name="applicationId" value="${selected.id}">
                        <input type="hidden" name="status" value="${statusFilter}">
                        <label for="evaluation">Sau buổi phỏng vấn: nhận xét</label>
                        <textarea id="evaluation" name="evaluation" maxlength="1000"></textarea>
                        <div style="display:flex;gap:10px">
                            <button type="submit" class="button button-primary button-sm" ${not interviewCanComplete ? 'disabled' : ''}>Đánh dấu đã phỏng vấn</button>
                        </div>
                        <c:if test="${not interviewCanComplete}"><small class="rc-sub">Chỉ ghi nhận sau giờ phỏng vấn.</small></c:if>
                    </form>
                    <form method="post" action="${ctx}/organizer-recruitment" onsubmit="return confirm('Hủy lịch phỏng vấn?');" style="margin-bottom:12px">
                        <input type="hidden" name="action" value="cancelInterview">
                        <input type="hidden" name="roundId" value="${round.id}">
                        <input type="hidden" name="applicationId" value="${selected.id}">
                        <input type="hidden" name="status" value="${statusFilter}">
                        <button type="submit" class="button button-sm">Hủy lịch phỏng vấn</button>
                        <span class="rc-sub">Đổi lịch: sửa thông tin ở khung bên phải rồi gửi lại.</span>
                    </form>
                    <form method="post" action="${ctx}/organizer-recruitment" style="margin-bottom:12px">
                        <input type="hidden" name="action" value="remind">
                        <input type="hidden" name="roundId" value="${round.id}">
                        <input type="hidden" name="applicationId" value="${selected.id}">
                        <input type="hidden" name="status" value="${statusFilter}">
                        <c:if test="${iv.status eq 'SCHEDULED'}"><button type="submit" class="button button-sm">Gọi lại (nhắc xác nhận)</button></c:if>
                    </form>
                </c:if>

                <c:if test="${not activityStarted}">
                    <c:if test="${(selected.status eq 'INTERVIEWED') or (selected.status eq 'PENDING' and not round.interviewRequired)}">
                        <form method="post" action="${ctx}/organizer-recruitment" class="rc-field" style="margin:12px 0">
                            <input type="hidden" name="action" value="accept">
                            <input type="hidden" name="roundId" value="${round.id}">
                            <input type="hidden" name="applicationId" value="${selected.id}">
                            <input type="hidden" name="status" value="${statusFilter}">
                            <label for="acceptPos">Ban được nhận</label>
                            <select id="acceptPos" name="positionId" required>
                                <c:forEach items="${round.positions}" var="p">
                                    <option value="${p.id}" ${p.remaining == 0 ? 'disabled' : ''} ${p.id == selected.choice1PositionId and p.remaining > 0 ? 'selected' : ''}>
                                        <c:out value="${p.name}"/> (còn ${p.remaining}/${p.quota})${p.id == selected.choice1PositionId ? ' · NV1' : (p.id == selected.choice2PositionId ? ' · NV2' : '')}</option>
                                </c:forEach>
                            </select>
                            <c:if test="${remaining[selected.choice1PositionId] == 0 and not empty selected.choice2Name and remaining[selected.choice2PositionId] > 0}">
                                <small class="rc-sub" style="color:#b54708">Nguyện vọng 1 đã đủ người. Còn chỉ tiêu ở nguyện vọng 2: <c:out value="${selected.choice2Name}"/>.</small>
                            </c:if>
                            <div><button type="submit" class="button button-primary button-sm">Trúng tuyển</button></div>
                        </form>
                    </c:if>
                    <c:if test="${selected.status eq 'PENDING' or selected.status eq 'INTERVIEW_INVITED' or selected.status eq 'INTERVIEWED'}">
                        <form method="post" action="${ctx}/organizer-recruitment" class="rc-field" style="margin:12px 0"
                              onsubmit="return confirm('Loại hồ sơ này?');">
                            <input type="hidden" name="action" value="reject">
                            <input type="hidden" name="roundId" value="${round.id}">
                            <input type="hidden" name="applicationId" value="${selected.id}">
                            <input type="hidden" name="status" value="${statusFilter}">
                            <label for="reason">Lý do loại <span>(ghi chú nội bộ)</span></label>
                            <input id="reason" name="reason" maxlength="500">
                            <div><button type="submit" class="button button-sm" style="color:#b42318">Loại</button></div>
                        </form>
                    </c:if>
                    <c:if test="${selected.status eq 'ACCEPTED' or selected.status eq 'REJECTED'}">
                        <form method="post" action="${ctx}/organizer-recruitment" onsubmit="return confirm('Thu hồi quyết định này?');">
                            <input type="hidden" name="action" value="revert">
                            <input type="hidden" name="roundId" value="${round.id}">
                            <input type="hidden" name="applicationId" value="${selected.id}">
                            <input type="hidden" name="status" value="${statusFilter}">
                            <button type="submit" class="button button-sm">Thu hồi quyết định</button>
                            <c:if test="${selected.status eq 'REJECTED' and not empty selected.decisionReason}"><span class="rc-sub">Lý do: <c:out value="${selected.decisionReason}"/></span></c:if>
                        </form>
                    </c:if>
                </c:if>
                <c:if test="${activityStarted}"><p class="rc-note">Hoạt động đã bắt đầu nên không thể thay đổi quyết định.</p></c:if>
                <c:if test="${selected.status eq 'WITHDRAWN' and not empty selected.withdrawReason}"><p class="rc-note">Lý do rút: <c:out value="${selected.withdrawReason}"/></p></c:if>
            </section>
        </c:if>
    </div>

    <c:if test="${not empty selected}">
        <aside class="card rc-card" style="margin:0">
            <h2>Mời phỏng vấn</h2>
            <c:choose>
                <c:when test="${not round.interviewRequired}"><p class="rc-sub">Đợt tuyển này không có vòng phỏng vấn. Staff cho trúng tuyển thẳng ở phần chi tiết hồ sơ.</p></c:when>
                <c:otherwise>
                    <c:if test="${not canInviteSelected}">
                        <p class="rc-sub">Hồ sơ đang chọn không ở trạng thái có thể mời hoặc đổi lịch phỏng vấn.</p>
                    </c:if>
                    <c:if test="${selected.status eq 'PENDING' or selected.status eq 'INTERVIEW_INVITED'}">
                        <p><b><c:out value="${selected.studentCode}"/> · <c:out value="${selected.studentName}"/></b></p>
                    </c:if>
                    <form id="interviewForm" method="post" action="${ctx}/organizer-recruitment" style="margin-top:14px;display:grid;gap:12px">
                        <input type="hidden" name="action" value="invite">
                        <input type="hidden" name="roundId" value="${round.id}">
                        <input type="hidden" name="status" value="${statusFilter}">
                        <c:if test="${selected.status eq 'PENDING' or selected.status eq 'INTERVIEW_INVITED'}">
                            <input type="hidden" name="applicationId" value="${selected.id}">
                        </c:if>
                        <div class="rc-field"><label>Thời gian <em>*</em>*</label><input type="datetime-local" name="scheduledAt" value="${ivInputs['scheduledAt']}" required ${not canInviteSelected ? 'disabled' : ''}></div>
                        <div class="rc-field"><label>Hình thức <em>*</em></label>
                            <select name="format" ${not canInviteSelected ? 'disabled' : ''}>
                                <option value="OFFLINE" ${not empty selected.interview and selected.interview.format eq 'OFFLINE' ? 'selected' : ''}>Trực tiếp</option>
                                <option value="ONLINE" ${not empty selected.interview and selected.interview.format eq 'ONLINE' ? 'selected' : ''}>Trực tuyến</option>
                            </select></div>
                        <div class="rc-field"><label>Địa điểm hoặc liên kết <em>*</em></label>
                            <input name="location" maxlength="300" value="<c:out value='${selected.status eq "INTERVIEW_INVITED" ? selected.interview.location : ""}'/>" placeholder="Phòng hoặc liên kết" required ${not canInviteSelected ? 'disabled' : ''}></div>
                        <div class="rc-field"><label>Hạn xác nhận <em>*</em></label><input type="datetime-local" name="confirmDeadline" value="${ivInputs['confirmDeadline']}" required ${not canInviteSelected ? 'disabled' : ''}></div>
                        <div class="rc-field"><label>Ghi chú cho ứng viên <span>(không bắt buộc)</span></label>
                            <textarea name="note" maxlength="500" ${not canInviteSelected ? 'disabled' : ''}><c:out value="${selected.status eq 'INTERVIEW_INVITED' ? selected.interview.note : ''}"/></textarea></div>
                        <div class="rc-actions" style="margin:0;padding:0;border:0">
                            <button type="submit" class="button button-primary" ${not canInviteSelected ? 'disabled' : ''}>${selected.status eq 'INTERVIEW_INVITED' ? 'Đổi lịch' : 'Gửi lời mời'}</button>
                        </div>
                    </form>
                </c:otherwise>
            </c:choose>
        </aside>
    </c:if>
</div>
<script src="${ctx}/assets/js/recruitment-pagination.js?v=20261115-2"></script>
</c:if>
<script src="${ctx}/assets/js/activity.js?v=20261107-5"></script>
<script>
    document.querySelectorAll('.multi-select-combobox[data-auto-submit]').forEach(function (combobox) {
        combobox.addEventListener('change', function (event) {
            if (event.target.type === 'radio' && event.target.checked) combobox.closest('form').submit();
        });
    });
</script>
        </main>
    </div>
</div>
<script src="${ctx}/assets/js/action-menu.js?v=5"></script>
<script src="${ctx}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>
