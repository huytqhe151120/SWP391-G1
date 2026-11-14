<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="activeNav" value="assessments" scope="request"/>
<c:set var="openBuilderInitially"
       value="${(empty assessment and not empty activity and param.mode eq 'create') or (not empty assessment and param.mode eq 'edit')}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>EAMS | Quản lý bài đánh giá &amp; câu hỏi</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261113-3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css?v=20261113-3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261113-3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/attendance.css?v=20261113-3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/registration.css?v=20261113-3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/assessment.css?v=20261113-3">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-list-page activity-form-page activity-detail-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> QUẢN LÝ BÀI ĐÁNH GIÁ</span>
                    <h1>Chi tiết bài đánh giá</h1>
                    <p>
                        <c:choose>
                            <c:when test="${not empty assessment}"><c:out value="${assessment.title}"/></c:when>
                            <c:when test="${not empty activity}"><c:out value="${activity.code}"/> – <c:out value="${activity.name}"/></c:when>
                            <c:otherwise>Thiết lập đề đánh giá, câu hỏi trắc nghiệm / tự luận và chấm điểm bài nộp</c:otherwise>
                        </c:choose>
                    </p>
                </div>
            </header>

            <div class="activity-page-back">
                <c:choose>
                    <c:when test="${param.from eq 'activity' and not empty activity}">
                        <a class="button button-back" href="${pageContext.request.contextPath}/activities?action=detail&amp;id=${activity.id}">
                            <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5"/><path d="m12 19-7-7 7-7"/></svg>
                            Quay lại hoạt động
                        </a>
                    </c:when>
                    <c:otherwise>
                        <a class="button button-back" href="${pageContext.request.contextPath}/assessment?action=list">
                            <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5"/><path d="m12 19-7-7 7-7"/></svg>
                            Quay lại danh sách bài đánh giá
                        </a>
                    </c:otherwise>
                </c:choose>
            </div>

            <c:if test="${not empty requestScope.errorMessage}">
                <div class="alert alert-error activity-alert" role="alert"><c:out value="${requestScope.errorMessage}"/></div>
            </c:if>
            <c:if test="${not empty sessionScope.errorMessage}">
                <div class="alert alert-error activity-alert" role="alert"><c:out value="${sessionScope.errorMessage}"/></div>
                <c:remove var="errorMessage" scope="session"/>
            </c:if>
            <c:if test="${not empty sessionScope.successMessage}">
                <div class="alert alert-success activity-alert" role="status"><c:out value="${sessionScope.successMessage}"/></div>
                <c:remove var="successMessage" scope="session"/>
            </c:if>

            <section class="card activity-table-card activity-form-card activity-detail-card registration-detail-card assessment-detail-card"
                     aria-labelledby="assessment-detail-title">
                <div class="activity-table-heading">
                    <div>
                        <h2 id="assessment-detail-title">Danh sách bài nộp &amp; Thông tin bài đánh giá</h2>
                        <p>
                            <c:choose>
                                <c:when test="${not empty activity}">
                                    <c:out value="${activity.code}"/> – <c:out value="${activity.name}"/>
                                </c:when>
                                <c:otherwise>Chọn hoạt động để thiết lập câu hỏi, đáp án hoặc xem danh sách bài nộp của sinh viên</c:otherwise>
                            </c:choose>
                        </p>
                    </div>
                    <div class="activity-heading-actions registration-detail-actions">
                        <c:if test="${not empty assessment}">
                            <span class="activity-count" data-header-sub-count>
                                <c:out value="${fn:length(submissions)}"/> sinh viên làm bài
                            </span>
                        </c:if>

                        <c:choose>
                            <c:when test="${not empty assessment}">
                                <button type="button" class="button button-sm button-primary" data-toggle-builder>
                                    ${openBuilderInitially ? 'Xem đề bài' : 'Chỉnh sửa'}
                                </button>
                                <form method="post" action="${pageContext.request.contextPath}/assessment" style="display:inline-flex; margin:0;">
                                    <input type="hidden" name="action" value="publish"/>
                                    <input type="hidden" name="id" value="${assessment.id}"/>
                                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>"/>
                                    <c:choose>
                                        <c:when test="${assessment.published}">
                                            <input type="hidden" name="publish" value="false"/>
                                            <button type="submit" class="button button-sm button-revoke">Hủy công bố</button>
                                        </c:when>
                                        <c:otherwise>
                                            <input type="hidden" name="publish" value="true"/>
                                            <button type="submit" class="button button-sm button-approve">Công bố</button>
                                        </c:otherwise>
                                    </c:choose>
                                </form>

                                <details class="action-menu">
                                    <summary aria-label="Thao tác khác"><span aria-hidden="true">⋯</span></summary>
                                    <div class="action-menu__items">
                                        <a class="action-menu-item action-menu-item--view"
                                           href="${pageContext.request.contextPath}/activities?action=detail&amp;id=${assessment.activityId}">
                                            <span class="action-menu-item__icon" aria-hidden="true">
                                                <svg viewBox="0 0 24 24"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg>
                                            </span>
                                            Xem chi tiết hoạt động
                                        </a>
                                        <button type="button" class="action-menu-item action-menu-item--edit" data-open-builder-questions>
                                            <span class="action-menu-item__icon" aria-hidden="true">
                                                <svg viewBox="0 0 24 24"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"/></svg>
                                            </span>
                                            Thiết lập câu hỏi &amp; đáp án (<c:out value="${fn:length(questions)}"/>)
                                        </button>
                                        <c:if test="${totalAttempts == 0}">
                                            <form method="post" action="${pageContext.request.contextPath}/assessment"
                                                  onsubmit="return confirm('Bạn chắc chắn muốn xóa bài đánh giá này? Toàn bộ câu hỏi và đáp án bên trong sẽ bị xóa.')">
                                                <input type="hidden" name="action" value="delete">
                                                <input type="hidden" name="id" value="${assessment.id}">
                                                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                                <button class="action-menu-item action-menu-item--delete" type="submit">
                                                    <span class="action-menu-item__icon" aria-hidden="true">
                                                        <svg viewBox="0 0 24 24"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="m19 6-1 14H6L5 6"/><path d="M10 11v5"/><path d="M14 11v5"/></svg>
                                                    </span>
                                                    Xóa bài đánh giá
                                                </button>
                                            </form>
                                        </c:if>
                                    </div>
                                </details>
                            </c:when>
                            <c:when test="${empty assessment and not empty activity}">
                                <button type="button" class="button button-sm button-primary" data-open-builder
                                        ${openBuilderInitially ? 'hidden' : ''}>
                                    Tạo mới bài đánh giá
                                </button>
                            </c:when>
                        </c:choose>
                    </div>
                </div>

                <details class="activity-filter-panel" open>
                    <summary class="activity-filter-toggle"><span>Chọn hoạt động</span></summary>
                    <form class="activity-filter attendance-filter attendance-filter--single"
                          action="${pageContext.request.contextPath}/assessment" method="get" data-assessment-activity-form>
                        <input type="hidden" name="action" value="manage">
                        <c:if test="${param.mode eq 'create'}">
                            <input type="hidden" name="mode" value="create">
                        </c:if>
                        <div class="activity-filter-field attendance-filter-activity">
                            <label id="assessment-activity-label">Hoạt động</label>
                            <div class="multi-select-combobox" data-multi-select data-single data-auto-submit
                                 data-item-label="hoạt động" data-input-name="activityId">
                                <button type="button" class="multi-select-trigger" aria-expanded="false"
                                        aria-controls="assessment-activity-options" aria-labelledby="assessment-activity-label">
                                    <span data-selection-label>-- Chọn hoạt động --</span>
                                    <span aria-hidden="true">▾</span>
                                </button>
                                <div class="multi-select-dropdown" id="assessment-activity-options" hidden>
                                    <input class="multi-select-search" type="search"
                                           placeholder="Tìm mã hoặc tên hoạt động..." aria-label="Tìm hoạt động">
                                    <fieldset class="multi-select-options">
                                        <legend class="visually-hidden">Chọn hoạt động</legend>
                                        <c:forEach var="actOption" items="${activities}">
                                            <label class="multi-select-option">
                                                <input type="radio" name="activityId" value="${actOption.id}"
                                                       ${(not empty activity and activity.id == actOption.id) or (not empty assessment and assessment.activityId == actOption.id) ? 'checked' : ''}>
                                                <span><c:out value="${actOption.code}"/> – <c:out value="${actOption.name}"/><c:if test="${not empty actOption.semesterName}"> (<c:out value="${actOption.semesterName}"/>)</c:if><c:if test="${not empty actOption.organizationLevelName}"> [<c:out value="${actOption.organizationLevelName}"/>]</c:if><c:if test="${assessedActivityIds.contains(actOption.id)}"> · Đã có đề</c:if></span>
                                            </label>
                                        </c:forEach>
                                    </fieldset>
                                    <div class="multi-select-empty" hidden>Không tìm thấy hoạt động phù hợp.</div>
                                </div>
                                <span class="multi-select-count" aria-live="polite">Chưa chọn hoạt động</span>
                            </div>
                        </div>
                        <noscript>
                            <div class="activity-filter-buttons">
                                <button type="submit" class="button button-primary button-sm">Xem chi tiết</button>
                            </div>
                        </noscript>
                    </form>
                </details>

                <c:choose>
                    <c:when test="${empty assessment and empty activity}">
                        <div class="empty-state activity-empty-state registration-empty-state registration-detail-empty-state">
                            <span class="empty-state-mark" aria-hidden="true">${param.mode eq 'create' ? '+' : '✓'}</span>
                            <strong>
                                <c:choose>
                                    <c:when test="${param.mode eq 'create'}">Chọn hoạt động để tạo bài đánh giá mới</c:when>
                                    <c:otherwise>Chọn hoạt động để xem bài đánh giá</c:otherwise>
                                </c:choose>
                            </strong>
                            <span>
                                <c:choose>
                                    <c:when test="${param.mode eq 'create'}">Hãy chọn một hoạt động ngoại khóa ở khung trên để bắt đầu cấu hình thời gian, câu hỏi và đáp án.</c:when>
                                    <c:otherwise>Thông tin đề đánh giá, danh sách câu hỏi – đáp án và bài nộp của sinh viên sẽ hiển thị tại đây.</c:otherwise>
                                </c:choose>
                            </span>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:if test="${not empty assessment}">
                            <div class="registration-period-summary">
                                <div class="attendance-summary-item">
                                    <span class="attendance-summary-label">Tiêu đề bài đánh giá</span>
                                    <strong class="activity-name"><c:out value="${assessment.title}"/></strong>
                                </div>
                                <div class="attendance-summary-item">
                                    <span class="attendance-summary-label">Cấp tổ chức (Hệ số)</span>
                                    <span class="activity-time">
                                        <c:out value="${not empty activity and not empty activity.organizationLevelName ? activity.organizationLevelName : 'Mặc định'}"/>
                                        <c:if test="${not empty activity and not empty activity.formattedOrganizationLevelCoefficient}">
                                            (x<c:out value="${activity.formattedOrganizationLevelCoefficient}"/>)
                                        </c:if>
                                    </span>
                                </div>
                                <div class="attendance-summary-item">
                                    <span class="attendance-summary-label">Mở nhận bài</span>
                                    <span class="activity-time">
                                        <c:choose>
                                            <c:when test="${not empty openTimeDisplay}"><c:out value="${openTimeDisplay}"/></c:when>
                                            <c:otherwise>Không giới hạn</c:otherwise>
                                        </c:choose>
                                    </span>
                                </div>
                                <div class="attendance-summary-item">
                                    <span class="attendance-summary-label">Đóng nhận bài</span>
                                    <span class="activity-time">
                                        <c:choose>
                                            <c:when test="${not empty closeTimeDisplay}"><c:out value="${closeTimeDisplay}"/></c:when>
                                            <c:otherwise>Không giới hạn</c:otherwise>
                                        </c:choose>
                                    </span>
                                </div>
                                <div class="attendance-summary-item">
                                    <span class="attendance-summary-label">Tổng điểm đề bài</span>
                                    <span class="activity-code">
                                        <fmt:formatNumber value="${totalMaxScore}" minFractionDigits="1" maxFractionDigits="2"/> / 10.0
                                        (<c:out value="${fn:length(questions)}"/> câu)
                                    </span>
                                </div>
                                <div class="attendance-summary-item">
                                    <span class="attendance-summary-label">Trạng thái bài đánh giá</span>
                                    <c:choose>
                                        <c:when test="${assessment.published}">
                                            <span class="badge status-pill status-green">Đã công bố</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge status-pill status-gray">Chưa công bố</span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>

                            <div class="activity-tabs-nav registration-detail-tabs" role="tablist">
                                <button type="button" class="activity-tab-btn active" id="btn-tab-form-info"
                                        data-tab="tab-form-info" role="tab" aria-selected="true" aria-controls="tab-form-info">
                                    Câu hỏi &amp; Đáp án <span class="activity-tab-badge"><c:out value="${fn:length(questions)}"/></span>
                                </button>
                                <button type="button" class="activity-tab-btn" id="btn-tab-form-applications"
                                        data-tab="tab-form-applications" role="tab" aria-selected="false" aria-controls="tab-form-applications">
                                    Bài nộp đã gửi <span class="activity-tab-badge"><c:out value="${fn:length(submissions)}"/></span>
                                </button>
                            </div>
                        </c:if>

                        <div id="tab-form-info" class="${not empty assessment ? 'activity-tab-pane active registration-info-pane' : ''}"
                             role="tabpanel" aria-labelledby="btn-tab-form-info">

                            <c:if test="${empty assessment}">
                                <div id="assessment-empty-create-box"
                                     class="empty-state activity-empty-state registration-empty-state registration-detail-empty-state"
                                     ${openBuilderInitially ? 'hidden' : ''}>
                                    <span class="empty-state-mark" aria-hidden="true">+</span>
                                    <strong>Chưa có thông tin bài đánh giá</strong>
                                    <span>Hoạt động này chưa tạo bài đánh giá hoặc bài thu hoạch. Bạn có thể khởi tạo đề bài và thiết lập câu hỏi ngay tại đây.</span>
                                    <button type="button" class="button button-primary" data-open-builder>
                                        Tạo mới bài đánh giá
                                    </button>
                                </div>
                            </c:if>

                            <c:if test="${not empty assessment}">
                                <section id="assessment-view-pane" class="activity-detail-section"
                                         ${openBuilderInitially ? 'hidden' : ''}>
                                    <c:if test="${not assessment.published and (empty questions or totalMaxScore != 10.0)}">
                                        <div class="assessment-banner assessment-banner--warning" style="margin-top: 16px; margin-bottom: 6px;" role="note">
                                            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z"/><path d="M12 9v4"/><path d="M12 17h.01"/></svg>
                                            <div>
                                                <strong>Gợi ý trước khi công bố:</strong>
                                                Đề bài cần có ít nhất 1 câu hỏi, mọi câu trắc nghiệm có tối thiểu 2 phương án với 1 đáp án đúng.
                                                Nên cân đối tổng điểm bằng <strong>10.0 điểm</strong>
                                                (hiện tại: <strong><fmt:formatNumber value="${totalMaxScore}" minFractionDigits="1" maxFractionDigits="2"/> điểm</strong>).
                                            </div>
                                        </div>
                                    </c:if>

                                    <div class="assessment-detail-info-header">
                                        <p>
                                            <c:choose>
                                                <c:when test="${hasAttempts}">Đề đánh giá đã có sinh viên làm bài — cấu trúc câu hỏi được khóa để bảo toàn kết quả chấm điểm.</c:when>
                                                <c:otherwise>Danh sách câu hỏi trắc nghiệm (chấm tự động) và câu hỏi tự luận (cán bộ phụ trách chấm điểm).</c:otherwise>
                                            </c:choose>
                                        </p>
                                        <button type="button" class="button button-sm button-outline" data-open-builder-questions>
                                            ${hasAttempts ? 'Chỉnh sửa thông tin chung' : '+ Thiết lập câu hỏi & đáp án'}
                                        </button>
                                    </div>

                                    <dl class="detail-grid activity-detail-grid">
                                        <c:if test="${not empty assessment.description}">
                                            <div class="detail-item field-wide">
                                                <dt>Mô tả &amp; Hướng dẫn làm bài</dt>
                                                <dd style="white-space: pre-wrap;"><c:out value="${assessment.description}"/></dd>
                                            </div>
                                        </c:if>

                                        <c:forEach items="${questions}" var="q" varStatus="loop">
                                            <div class="detail-item field-wide">
                                                <dt class="assessment-detail-q-head">
                                                    <span>Câu <c:out value="${loop.count}"/>: <c:out value="${q.content}"/></span>
                                                    <span class="assessment-detail-q-meta">
                                                        <c:choose>
                                                            <c:when test="${q.questionType == 'MCQ'}">
                                                                <span class="assessment-q-type assessment-q-type--mcq">Trắc nghiệm</span>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <span class="assessment-q-type assessment-q-type--essay">Tự luận</span>
                                                            </c:otherwise>
                                                        </c:choose>
                                                        <span class="assessment-score-pill">
                                                            <fmt:formatNumber value="${q.maxScore}" minFractionDigits="1" maxFractionDigits="2"/> điểm
                                                        </span>
                                                    </span>
                                                </dt>
                                                <dd>
                                                    <c:choose>
                                                        <c:when test="${q.questionType == 'MCQ'}">
                                                            Trắc nghiệm — chọn một đáp án đúng (chấm tự động)
                                                            <c:choose>
                                                                <c:when test="${not empty q.options}">
                                                                    <ul class="registration-detail-question-options assessment-detail-options">
                                                                        <c:forEach items="${q.options}" var="opt" varStatus="optLoop">
                                                                            <li class="${opt.correct ? 'is-correct' : ''}">
                                                                                <span>
                                                                                    <span class="assessment-detail-option-letter">
                                                                                        <c:choose>
                                                                                            <c:when test="${optLoop.index < 26}"><c:out value="${fn:substring('ABCDEFGHIJKLMNOPQRSTUVWXYZ', optLoop.index, optLoop.index + 1)}"/></c:when>
                                                                                            <c:otherwise><c:out value="${optLoop.count}"/></c:otherwise>
                                                                                        </c:choose>
                                                                                    </span>
                                                                                    <c:out value="${opt.optionText}"/>
                                                                                </span>
                                                                                <c:if test="${opt.correct}">
                                                                                    <span class="badge status-pill status-green">Đáp án đúng</span>
                                                                                </c:if>
                                                                            </li>
                                                                        </c:forEach>
                                                                    </ul>
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <div class="text-muted" style="margin-top: 6px;">Chưa có phương án trả lời nào cho câu hỏi này.</div>
                                                                </c:otherwise>
                                                            </c:choose>
                                                        </c:when>
                                                        <c:otherwise>
                                                            Tự luận — sinh viên trả lời bằng văn bản, cán bộ phụ trách chấm điểm và nhận xét
                                                        </c:otherwise>
                                                    </c:choose>
                                                </dd>
                                            </div>
                                        </c:forEach>

                                        <c:if test="${empty questions}">
                                            <div class="detail-item field-wide">
                                                <dt>Câu hỏi &amp; Đáp án</dt>
                                                <dd>
                                                    Bài đánh giá này chưa có câu hỏi nào.
                                                    <button type="button" class="button button-sm button-primary" style="margin-left: 8px;" data-open-builder-questions>
                                                        + Thêm câu hỏi ngay
                                                    </button>
                                                </dd>
                                            </div>
                                        </c:if>
                                    </dl>
                                </section>
                            </c:if>

                            <div id="assessment-editor-pane" class="assessment-editor-pane"
                                 ${openBuilderInitially ? '' : 'hidden'}>
                                <div class="assessment-editor-heading">
                                    <div>
                                        <h3>${empty assessment ? 'Tạo bài đánh giá &amp; Thiết lập câu hỏi' : 'Chỉnh sửa bài đánh giá &amp; Câu hỏi'}</h3>
                                        <p>Cấu hình thông tin chung, thêm/sửa câu hỏi trắc nghiệm hoặc tự luận và chọn đáp án đúng ngay trên một biểu mẫu.</p>
                                    </div>
                                    <button type="button" class="button button-sm button-secondary" data-cancel-builder>Đóng chế độ sửa</button>
                                </div>

                                <form id="assessment-builder-form" class="form-grid activity-form-grid"
                                      method="post" action="${pageContext.request.contextPath}/assessment" novalidate>
                                    <input type="hidden" name="action" value="saveFull">
                                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                    <input type="hidden" name="id" value="${not empty assessment ? assessment.id : 0}">
                                    <input type="hidden" name="activityId" value="${not empty activity ? activity.id : selectedActivityId}">
                                    <input type="hidden" name="publishAfterSave" value="false" data-publish-after-save>

                                    <c:choose>
                                        <c:when test="${not empty assessment}">
                                            <c:set var="initialTitleValue" value="${assessment.title}"/>
                                        </c:when>
                                        <c:when test="${not empty activity and not empty activity.name}">
                                            <c:set var="initialTitleValue" value="Bài đánh giá – ${activity.name}"/>
                                        </c:when>
                                        <c:otherwise>
                                            <c:set var="initialTitleValue" value=""/>
                                        </c:otherwise>
                                    </c:choose>
                                    <div class="field field-wide">
                                        <label for="assessment-title-input">Tiêu đề bài đánh giá <span class="required-mark">*</span></label>
                                        <input id="assessment-title-input" name="title" type="text" required maxlength="255"
                                               placeholder="Ví dụ: Bài thu hoạch &amp; kiểm tra sau hội thảo..."
                                               value="<c:out value='${initialTitleValue}'/>">
                                    </div>

                                    <div class="field">
                                        <label for="assessment-open-time">Thời gian mở nhận bài</label>
                                        <input id="assessment-open-time" name="openTime" type="datetime-local"
                                               value="<c:out value='${openTimeInput}'/>">
                                        <small class="field-hint">Để trống nếu mở ngay sau khi công bố.</small>
                                    </div>

                                    <div class="field">
                                        <label for="assessment-close-time">Thời gian đóng nhận bài</label>
                                        <input id="assessment-close-time" name="closeTime" type="datetime-local"
                                               value="<c:out value='${closeTimeInput}'/>">
                                        <small class="field-hint">Để trống nếu không giới hạn hạn nộp.</small>
                                    </div>

                                    <div class="field field-wide">
                                        <label for="assessment-description-input">Mô tả &amp; Hướng dẫn làm bài</label>
                                        <textarea id="assessment-description-input" name="description" rows="3"
                                                  placeholder="Ghi chú yêu cầu làm bài, tiêu chí chấm điểm hoặc lưu ý cho sinh viên..."><c:out value="${not empty assessment ? assessment.description : ''}"/></textarea>
                                    </div>

                                    <section id="question-builder" class="field field-wide registration-builder-section assessment-builder-section"
                                             aria-labelledby="builder-section-title">
                                        <div class="activity-section-header">
                                            <div>
                                                <h3 id="builder-section-title">Câu hỏi &amp; Đáp án</h3>
                                                <p>Thêm câu hỏi trắc nghiệm (tích chọn 1 đáp án đúng) hoặc câu hỏi tự luận và phân bổ điểm số cho từng câu.</p>
                                            </div>
                                            <div class="assessment-builder-stats">
                                                <span class="registration-question-count" data-question-count>
                                                    <c:out value="${fn:length(questions)}"/> câu hỏi
                                                </span>
                                                <span class="assessment-score-pill" data-builder-total-score>
                                                    Tổng điểm: <fmt:formatNumber value="${totalMaxScore}" minFractionDigits="1" maxFractionDigits="2"/> / 10.0
                                                </span>
                                            </div>
                                        </div>

                                        <c:choose>
                                            <c:when test="${hasAttempts}">
                                                <div class="assessment-banner assessment-banner--info" style="margin-bottom: 0;" role="note">
                                                    <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
                                                    <div>
                                                        <strong>Đã khóa chỉnh sửa câu hỏi &amp; đáp án:</strong>
                                                        Hiện đã có <strong><c:out value="${totalAttempts}"/></strong> sinh viên tham gia làm bài nên không thể thêm/sửa/xóa câu hỏi. Bạn vẫn có thể cập nhật tiêu đề, mô tả và thời gian đóng/mở ở trên.
                                                    </div>
                                                </div>
                                            </c:when>
                                            <c:otherwise>
                                                <input type="hidden" name="questionCount" value="${fn:length(questions)}" data-question-count-input>

                                                <div class="registration-question-list" data-question-list>
                                                    <c:forEach items="${questions}" var="q" varStatus="qStatus">
                                                        <article class="registration-question-card assessment-builder-q-card" data-question-card>
                                                            <div class="assessment-builder-q-top">
                                                                <div class="assessment-builder-q-title-group">
                                                                    <strong class="registration-question-number" data-question-number>Câu <c:out value="${qStatus.count}"/></strong>
                                                                    <span class="assessment-q-type ${q.questionType == 'MCQ' ? 'assessment-q-type--mcq' : 'assessment-q-type--essay'}" data-question-type-badge>
                                                                        ${q.questionType == 'MCQ' ? 'Trắc nghiệm' : 'Tự luận'}
                                                                    </span>
                                                                </div>
                                                                <div class="assessment-builder-q-tools">
                                                                    <button type="button" class="assessment-tool-btn" data-move-question="up" title="Chuyển lên">↑</button>
                                                                    <button type="button" class="assessment-tool-btn" data-move-question="down" title="Chuyển xuống">↓</button>
                                                                    <button type="button" class="assessment-tool-btn" data-duplicate-question title="Nhân bản câu hỏi">Nhân bản</button>
                                                                    <button type="button" class="registration-remove-question" data-remove-question aria-label="Xóa câu hỏi" title="Xóa câu hỏi">×</button>
                                                                </div>
                                                            </div>

                                                            <div class="assessment-builder-q-fields">
                                                                <div class="field assessment-builder-q-content">
                                                                    <label>Nội dung câu hỏi <span class="required-mark">*</span></label>
                                                                    <textarea rows="2" name="questionContent_${qStatus.index}" data-question-content required
                                                                              placeholder="Nhập nội dung câu hỏi đánh giá..."><c:out value="${q.content}"/></textarea>
                                                                </div>
                                                                <div class="field">
                                                                    <label>Loại câu hỏi</label>
                                                                    <select name="questionType_${qStatus.index}" data-question-type>
                                                                        <option value="MCQ" ${q.questionType == 'MCQ' ? 'selected' : ''}>Trắc nghiệm (1 đáp án đúng)</option>
                                                                        <option value="ESSAY" ${q.questionType == 'ESSAY' ? 'selected' : ''}>Tự luận (Chấm tay)</option>
                                                                    </select>
                                                                </div>
                                                                <div class="field">
                                                                    <label>Điểm tối đa <span class="required-mark">*</span></label>
                                                                    <input type="number" step="0.25" min="0.25" max="100"
                                                                           name="questionScore_${qStatus.index}" data-question-score required
                                                                           value="${q.maxScore}">
                                                                </div>
                                                            </div>

                                                            <div class="registration-choice-editor assessment-choice-editor" data-choice-editor
                                                                 ${q.questionType == 'MCQ' ? '' : 'hidden'}>
                                                                <div class="assessment-choice-editor-head">
                                                                    <span class="assessment-choice-editor-title">Danh sách phương án trả lời</span>
                                                                    <span class="assessment-choice-editor-sub">Chọn nút tròn ở đầu dòng để đánh dấu <strong>1 đáp án đúng</strong></span>
                                                                </div>
                                                                <input type="hidden" name="choiceCount_${qStatus.index}"
                                                                       value="${not empty q.options ? fn:length(q.options) : 0}" data-choice-count>
                                                                <div class="registration-choice-list assessment-builder-choice-list" data-choice-list>
                                                                    <c:choose>
                                                                        <c:when test="${not empty q.options}">
                                                                            <c:forEach items="${q.options}" var="opt" varStatus="optStatus">
                                                                                <div class="registration-choice-row assessment-builder-choice-row ${opt.correct ? 'is-correct' : ''}" data-choice-row>
                                                                                    <label class="assessment-correct-radio" title="Đánh dấu là đáp án đúng">
                                                                                        <input type="radio" name="correctChoice_${qStatus.index}" value="${optStatus.index}"
                                                                                               data-choice-correct ${opt.correct ? 'checked' : ''}>
                                                                                        <span class="assessment-correct-letter" data-choice-letter>A</span>
                                                                                    </label>
                                                                                    <input type="text" name="choice_${qStatus.index}_${optStatus.index}"
                                                                                           value="<c:out value='${opt.optionText}'/>"
                                                                                           placeholder="Nhập nội dung phương án..." data-choice-input>
                                                                                    <span class="assessment-correct-badge">Đáp án đúng</span>
                                                                                    <button type="button" class="registration-remove-question" data-remove-choice aria-label="Xóa phương án" title="Xóa phương án">×</button>
                                                                                </div>
                                                                            </c:forEach>
                                                                        </c:when>
                                                                        <c:otherwise>
                                                                            <div class="registration-choice-row assessment-builder-choice-row is-correct" data-choice-row>
                                                                                <label class="assessment-correct-radio" title="Đánh dấu là đáp án đúng">
                                                                                    <input type="radio" name="correctChoice_${qStatus.index}" value="0" data-choice-correct checked>
                                                                                    <span class="assessment-correct-letter" data-choice-letter>A</span>
                                                                                </label>
                                                                                <input type="text" name="choice_${qStatus.index}_0" placeholder="Phương án A..." data-choice-input>
                                                                                <span class="assessment-correct-badge">Đáp án đúng</span>
                                                                                <button type="button" class="registration-remove-question" data-remove-choice aria-label="Xóa phương án">×</button>
                                                                            </div>
                                                                            <div class="registration-choice-row assessment-builder-choice-row" data-choice-row>
                                                                                <label class="assessment-correct-radio" title="Đánh dấu là đáp án đúng">
                                                                                    <input type="radio" name="correctChoice_${qStatus.index}" value="1" data-choice-correct>
                                                                                    <span class="assessment-correct-letter" data-choice-letter>B</span>
                                                                                </label>
                                                                                <input type="text" name="choice_${qStatus.index}_1" placeholder="Phương án B..." data-choice-input>
                                                                                <span class="assessment-correct-badge">Đáp án đúng</span>
                                                                                <button type="button" class="registration-remove-question" data-remove-choice aria-label="Xóa phương án">×</button>
                                                                            </div>
                                                                        </c:otherwise>
                                                                    </c:choose>
                                                                </div>
                                                                <div style="margin-top: 10px;">
                                                                    <button type="button" class="button button-secondary button-sm registration-add-choice" data-add-choice>
                                                                        + Thêm phương án
                                                                    </button>
                                                                </div>
                                                            </div>

                                                            <div class="assessment-essay-editor-hint" data-essay-hint
                                                                 ${q.questionType == 'ESSAY' ? '' : 'hidden'}>
                                                                Sinh viên nhập câu trả lời dạng văn bản tự luận. Cán bộ phụ trách sẽ chấm điểm và nhận xét tại tab <strong>Bài nộp đã gửi</strong>.
                                                            </div>
                                                        </article>
                                                    </c:forEach>
                                                </div>

                                                <div class="assessment-builder-empty" data-builder-empty ${not empty questions ? 'hidden' : ''}>
                                                    Chưa có câu hỏi nào trong đề. Nhấn <strong>+ Thêm câu trắc nghiệm</strong> hoặc <strong>+ Thêm câu tự luận</strong> bên dưới để bắt đầu.
                                                </div>

                                                <div class="registration-add-question-panel assessment-add-question-panel">
                                                    <div class="assessment-add-question-btns">
                                                        <button type="button" class="button button-secondary" data-add-question="MCQ">
                                                            + Thêm câu trắc nghiệm
                                                        </button>
                                                        <button type="button" class="button button-secondary" data-add-question="ESSAY">
                                                            + Thêm câu tự luận
                                                        </button>
                                                    </div>
                                                    <button type="button" class="button button-sm button-outline" data-distribute-score>
                                                        Chia đều thang 10 điểm
                                                    </button>
                                                </div>
                                            </c:otherwise>
                                        </c:choose>
                                    </section>

                                    <div class="form-actions activity-form-actions field-wide">
                                        <button type="button" class="button button-secondary" data-cancel-builder>Hủy</button>
                                        <button type="submit" class="button button-primary" data-save-draft-btn>
                                            ${empty assessment ? 'Tạo bài đánh giá &amp; Lưu câu hỏi' : 'Lưu thay đổi'}
                                        </button>
                                        <c:if test="${(empty assessment or not assessment.published) and not hasAttempts}">
                                            <button type="submit" class="button button-approve" data-save-publish-btn>
                                                Lưu &amp; Công bố ngay
                                            </button>
                                        </c:if>
                                    </div>
                                </form>
                            </div>
                        </div>

                        <c:if test="${not empty assessment}">
                            <div id="tab-form-applications" class="activity-tab-pane registration-applications-pane"
                                 role="tabpanel" aria-labelledby="btn-tab-form-applications">
                                <details class="activity-filter-panel" id="submission-filter-panel">
                                    <summary class="activity-filter-toggle">
                                        <span>Tìm kiếm &amp; Bộ lọc</span>
                                        <span class="activity-filter-badge" data-sub-filter-badge hidden>0</span>
                                    </summary>
                                    <form class="activity-filter" id="submission-filter-form">
                                        <div class="activity-filter-field">
                                            <label for="filter-sub-keyword">Tìm kiếm</label>
                                            <input id="filter-sub-keyword" type="search" placeholder="Mã SV, họ tên hoặc email">
                                        </div>
                                        <div class="activity-filter-field">
                                            <label for="filter-sub-department">Khoa</label>
                                            <select id="filter-sub-department">
                                                <option value="">Tất cả</option>
                                            </select>
                                        </div>
                                        <div class="activity-filter-field">
                                            <label for="filter-sub-major">Ngành</label>
                                            <select id="filter-sub-major">
                                                <option value="">Tất cả</option>
                                            </select>
                                        </div>
                                        <div class="activity-filter-field">
                                            <label for="filter-sub-program">Chương trình đào tạo</label>
                                            <select id="filter-sub-program">
                                                <option value="">Tất cả</option>
                                            </select>
                                        </div>
                                        <div class="activity-filter-field">
                                            <label for="filter-sub-class">Lớp</label>
                                            <select id="filter-sub-class">
                                                <option value="">Tất cả</option>
                                            </select>
                                        </div>
                                        <div class="activity-filter-field">
                                            <label for="filter-sub-status">Trạng thái bài làm</label>
                                            <select id="filter-sub-status">
                                                <option value="">Tất cả</option>
                                                <option value="SUBMITTED">Chờ chấm điểm</option>
                                                <option value="COMPLETED">Đã hoàn thành</option>
                                                <option value="IN_PROGRESS">Đang làm</option>
                                            </select>
                                        </div>
                                        <div class="activity-filter-buttons">
                                            <button type="submit" class="button button-primary button-sm">Tìm kiếm &amp; Lọc</button>
                                            <button type="button" class="button button-sm" data-sub-filter-reset hidden>Xóa Bộ lọc</button>
                                        </div>
                                    </form>
                                </details>

                                <ul class="application-count" aria-label="Thống kê bài nộp">
                                    <li class="badge status-pill status-yellow">Chờ chấm điểm: <strong><c:out value="${pendingGradingCount}"/></strong></li>
                                    <li class="badge status-pill status-green">Đã hoàn thành: <strong><c:out value="${completedCount}"/></strong></li>
                                    <li class="badge status-pill status-blue">Đang làm: <strong><c:out value="${inProgressCount}"/></strong></li>
                                </ul>

                                <div class="table-wrap activity-table-wrap">
                                    <table>
                                        <thead>
                                        <tr>
                                            <th scope="col" class="col-code">Mã SV</th>
                                            <th scope="col" class="col-object-name">Họ tên</th>
                                            <th scope="col" class="col-academic">Khoa / Ngành</th>
                                            <th scope="col" class="col-academic">CTĐT / Lớp</th>
                                            <th scope="col" class="col-single-line">Email</th>
                                            <th scope="col" class="col-datetime">Thời gian nộp</th>
                                            <th scope="col" class="col-number">Điểm số</th>
                                            <th scope="col" class="col-status activity-status-column">Trạng thái</th>
                                            <th scope="col" class="activity-action-column">Thao tác</th>
                                        </tr>
                                        </thead>
                                        <tbody data-submissions-tbody>
                                        <c:choose>
                                            <c:when test="${empty submissions}">
                                                <tr>
                                                    <td class="empty-state activity-empty-state" colspan="9">
                                                        <span class="empty-state-mark" aria-hidden="true">+</span>
                                                        <strong>Chưa có sinh viên nộp bài</strong>
                                                        <span>Danh sách bài làm sẽ xuất hiện tại đây sau khi sinh viên thực hiện bài đánh giá.</span>
                                                    </td>
                                                </tr>
                                            </c:when>
                                            <c:otherwise>
                                                <c:forEach items="${submissions}" var="sub">
                                                    <c:set var="stObj" value="${studentMap[sub.studentId]}"/>
                                                    <tr data-submission-row
                                                        data-student-code="<c:out value='${not empty stObj ? stObj.code : sub.studentId}'/>"
                                                        data-student-name="<c:out value='${not empty stObj ? stObj.name : ""}'/>"
                                                        data-department="<c:out value='${not empty stObj ? stObj.departmentName : ""}'/>"
                                                        data-major="<c:out value='${not empty stObj ? stObj.majorName : ""}'/>"
                                                        data-program="<c:out value='${not empty stObj ? stObj.academicProgramName : ""}'/>"
                                                        data-class-code="<c:out value='${not empty stObj ? stObj.mainClassCode : ""}'/>"
                                                        data-email="<c:out value='${not empty stObj ? stObj.email : ""}'/>"
                                                        data-status="<c:out value='${sub.status}'/>">
                                                        <td class="col-code">
                                                            <span class="activity-code">
                                                                <c:out value="${not empty stObj and not empty stObj.code ? stObj.code : sub.studentId}"/>
                                                            </span>
                                                        </td>
                                                        <td class="col-object-name" title="<c:out value='${not empty stObj ? stObj.name : ""}'/>">
                                                            <strong class="activity-name">
                                                                <c:choose>
                                                                    <c:when test="${not empty stObj and not empty stObj.name}"><c:out value="${stObj.name}"/></c:when>
                                                                    <c:otherwise>Sinh viên #<c:out value="${sub.studentId}"/></c:otherwise>
                                                                </c:choose>
                                                            </strong>
                                                        </td>
                                                        <td class="col-academic" title="<c:out value='${not empty stObj ? stObj.departmentName : ""} - ${not empty stObj ? stObj.majorName : ""}'/>">
                                                            <c:out value="${not empty stObj and not empty stObj.departmentName ? stObj.departmentName : '—'}"/>
                                                            <c:if test="${not empty stObj and not empty stObj.majorName}">
                                                                <br><small class="text-muted"><c:out value="${stObj.majorName}"/></small>
                                                            </c:if>
                                                        </td>
                                                        <td class="col-academic" title="<c:out value='${not empty stObj ? stObj.academicProgramName : ""} - ${not empty stObj ? stObj.mainClassCode : ""}'/>">
                                                            <c:out value="${not empty stObj and not empty stObj.academicProgramName ? stObj.academicProgramName : '—'}"/>
                                                            <c:if test="${not empty stObj and not empty stObj.mainClassCode}">
                                                                <br><small class="text-muted"><c:out value="${stObj.mainClassCode}"/></small>
                                                            </c:if>
                                                        </td>
                                                        <td class="col-single-line" title="<c:out value='${not empty stObj ? stObj.email : ""}'/>">
                                                            <c:out value="${not empty stObj and not empty stObj.email ? stObj.email : '—'}"/>
                                                        </td>
                                                        <td class="activity-time col-datetime">
                                                            <c:choose>
                                                                <c:when test="${not empty submittedAtDisplay[sub.id]}"><c:out value="${submittedAtDisplay[sub.id]}"/></c:when>
                                                                <c:otherwise>—</c:otherwise>
                                                            </c:choose>
                                                        </td>
                                                        <td class="col-number">
                                                            <c:choose>
                                                                <c:when test="${sub.finalScore != null}">
                                                                    <span class="assessment-score-pill assessment-score-pill--green">
                                                                        <fmt:formatNumber value="${sub.finalScore}" minFractionDigits="1" maxFractionDigits="2"/>
                                                                    </span>
                                                                </c:when>
                                                                <c:otherwise>—</c:otherwise>
                                                            </c:choose>
                                                        </td>
                                                        <td class="col-status activity-status-column">
                                                            <c:choose>
                                                                <c:when test="${sub.status == 'COMPLETED'}">
                                                                    <span class="badge status-pill status-green">Đã hoàn thành</span>
                                                                </c:when>
                                                                <c:when test="${sub.status == 'SUBMITTED'}">
                                                                    <span class="badge status-pill status-yellow">Chờ chấm điểm</span>
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <span class="badge status-pill status-blue">Đang làm</span>
                                                                </c:otherwise>
                                                            </c:choose>
                                                        </td>
                                                        <td class="activity-action-column">
                                                            <div class="actions activity-actions">
                                                                <a class="activity-action-link action-btn action-btn--view"
                                                                   href="${pageContext.request.contextPath}/assessment?action=grade&amp;attemptId=${sub.id}&amp;from=manage">
                                                                    <span class="action-btn__icon" aria-hidden="true">
                                                                        <svg viewBox="0 0 24 24"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg>
                                                                    </span>
                                                                    ${sub.status == 'SUBMITTED' ? 'Chấm bài' : 'Xem bài'}
                                                                </a>
                                                            </div>
                                                        </td>
                                                    </tr>
                                                </c:forEach>
                                                <tr data-empty-filter-row hidden>
                                                    <td class="empty-state activity-empty-state" colspan="9">
                                                        <span class="empty-state-mark" aria-hidden="true">+</span>
                                                        <strong>Không tìm thấy sinh viên phù hợp</strong>
                                                        <span>Hãy thử thay đổi từ khóa hoặc bộ lọc.</span>
                                                    </td>
                                                </tr>
                                            </c:otherwise>
                                        </c:choose>
                                        </tbody>
                                    </table>
                                </div>

                                <c:if test="${not empty submissions}">
                                    <div class="activity-pagination">
                                        <span class="activity-pagination-summary" data-sub-pagination-summary>
                                            Hiển thị 1–<c:out value="${fn:length(submissions)}"/> trong <c:out value="${fn:length(submissions)}"/> sinh viên
                                        </span>
                                    </div>
                                </c:if>
                            </div>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </section>
        </main>
    </div>
</div>

<template id="assessment-question-template">
    <article class="registration-question-card assessment-builder-q-card" data-question-card>
        <div class="assessment-builder-q-top">
            <div class="assessment-builder-q-title-group">
                <strong class="registration-question-number" data-question-number>Câu 1</strong>
                <span class="assessment-q-type assessment-q-type--mcq" data-question-type-badge>Trắc nghiệm</span>
            </div>
            <div class="assessment-builder-q-tools">
                <button type="button" class="assessment-tool-btn" data-move-question="up" title="Chuyển lên">↑</button>
                <button type="button" class="assessment-tool-btn" data-move-question="down" title="Chuyển xuống">↓</button>
                <button type="button" class="assessment-tool-btn" data-duplicate-question title="Nhân bản câu hỏi">Nhân bản</button>
                <button type="button" class="registration-remove-question" data-remove-question aria-label="Xóa câu hỏi" title="Xóa câu hỏi">×</button>
            </div>
        </div>

        <div class="assessment-builder-q-fields">
            <div class="field assessment-builder-q-content">
                <label>Nội dung câu hỏi <span class="required-mark">*</span></label>
                <textarea rows="2" data-question-content required placeholder="Nhập nội dung câu hỏi đánh giá..."></textarea>
            </div>
            <div class="field">
                <label>Loại câu hỏi</label>
                <select data-question-type>
                    <option value="MCQ">Trắc nghiệm (1 đáp án đúng)</option>
                    <option value="ESSAY">Tự luận (Chấm tay)</option>
                </select>
            </div>
            <div class="field">
                <label>Điểm tối đa <span class="required-mark">*</span></label>
                <input type="number" step="0.25" min="0.25" max="100" data-question-score required value="2.5">
            </div>
        </div>

        <div class="registration-choice-editor assessment-choice-editor" data-choice-editor>
            <div class="assessment-choice-editor-head">
                <span class="assessment-choice-editor-title">Danh sách phương án trả lời</span>
                <span class="assessment-choice-editor-sub">Chọn nút tròn ở đầu dòng để đánh dấu <strong>1 đáp án đúng</strong></span>
            </div>
            <input type="hidden" value="0" data-choice-count>
            <div class="registration-choice-list assessment-builder-choice-list" data-choice-list></div>
            <div style="margin-top: 10px;">
                <button type="button" class="button button-secondary button-sm registration-add-choice" data-add-choice>
                    + Thêm phương án
                </button>
            </div>
        </div>

        <div class="assessment-essay-editor-hint" data-essay-hint hidden>
            Sinh viên nhập câu trả lời dạng văn bản tự luận. Cán bộ phụ trách sẽ chấm điểm và nhận xét tại tab <strong>Bài nộp đã gửi</strong>.
        </div>
    </article>
</template>

<template id="assessment-choice-template">
    <div class="registration-choice-row assessment-builder-choice-row" data-choice-row>
        <label class="assessment-correct-radio" title="Đánh dấu là đáp án đúng">
            <input type="radio" data-choice-correct>
            <span class="assessment-correct-letter" data-choice-letter>A</span>
        </label>
        <input type="text" placeholder="Nhập nội dung phương án..." data-choice-input>
        <span class="assessment-correct-badge">Đáp án đúng</span>
        <button type="button" class="registration-remove-question" data-remove-choice aria-label="Xóa phương án" title="Xóa phương án">×</button>
    </div>
</template>

<script src="${pageContext.request.contextPath}/assets/js/action-menu.js?v=5"></script>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
<script src="${pageContext.request.contextPath}/assets/js/activity.js?v=20261107-5"></script>
<script>
    (function () {
        /* 1. Inline Builder Toggle & Question/Option Editor */
        var editorPane = document.getElementById('assessment-editor-pane');
        var viewPane = document.getElementById('assessment-view-pane');
        var emptyCreateBox = document.getElementById('assessment-empty-create-box');
        var toggleBuilderBtn = document.querySelector('[data-toggle-builder]');
        var openBuilderBtns = document.querySelectorAll('[data-open-builder]');
        var openBuilderQuestionBtns = document.querySelectorAll('[data-open-builder-questions]');
        var cancelBuilderBtns = document.querySelectorAll('[data-cancel-builder]');
        var builderForm = document.getElementById('assessment-builder-form');

        function activateInfoTab() {
            var infoBtn = document.getElementById('btn-tab-form-info');
            var infoPane = document.getElementById('tab-form-info');
            var appBtn = document.getElementById('btn-tab-form-applications');
            var appPane = document.getElementById('tab-form-applications');
            if (infoBtn && infoPane && appBtn && appPane) {
                appBtn.classList.remove('active');
                appBtn.setAttribute('aria-selected', 'false');
                appPane.classList.remove('active');
                infoBtn.classList.add('active');
                infoBtn.setAttribute('aria-selected', 'true');
                infoPane.classList.add('active');
            }
        }

        function setBuilderOpen(open, scrollToQuestions) {
            if (!editorPane) return;
            if (open) {
                activateInfoTab();
                editorPane.hidden = false;
                if (viewPane) viewPane.hidden = true;
                if (emptyCreateBox) emptyCreateBox.hidden = true;
                openBuilderBtns.forEach(function (b) { b.hidden = true; });
                if (toggleBuilderBtn) toggleBuilderBtn.textContent = 'Xem đề bài';

                /* Close open action-menu dropdowns if any */
                document.querySelectorAll('details.action-menu[open]').forEach(function (d) {
                    d.removeAttribute('open');
                });

                /* Auto-seed 1 starter MCQ question when creating a brand-new assessment */
                if (!viewPane && questionList && questionList.querySelectorAll('[data-question-card]').length === 0) {
                    addQuestionCard('MCQ', '', 10, ['', '', '', ''], 0);
                }

                var scrollTarget = scrollToQuestions
                    ? (document.getElementById('question-builder') || editorPane)
                    : editorPane;
                scrollTarget.scrollIntoView({ behavior: 'smooth', block: 'start' });
            } else {
                editorPane.hidden = true;
                if (viewPane) viewPane.hidden = false;
                if (emptyCreateBox) emptyCreateBox.hidden = false;
                openBuilderBtns.forEach(function (b) { b.hidden = false; });
                if (toggleBuilderBtn) toggleBuilderBtn.textContent = 'Chỉnh sửa';
            }
        }

        if (toggleBuilderBtn) {
            toggleBuilderBtn.addEventListener('click', function () {
                setBuilderOpen(editorPane ? editorPane.hidden : false, false);
            });
        }
        openBuilderBtns.forEach(function (btn) {
            btn.addEventListener('click', function () {
                setBuilderOpen(true, false);
            });
        });
        openBuilderQuestionBtns.forEach(function (btn) {
            btn.addEventListener('click', function () {
                setBuilderOpen(true, true);
            });
        });
        cancelBuilderBtns.forEach(function (btn) {
            btn.addEventListener('click', function () {
                setBuilderOpen(false, false);
            });
        });

        var questionList = builderForm ? builderForm.querySelector('[data-question-list]') : null;
        var questionCountInput = builderForm ? builderForm.querySelector('[data-question-count-input]') : null;
        var questionCountBadge = builderForm ? builderForm.querySelector('[data-question-count]') : null;
        var totalScoreBadge = builderForm ? builderForm.querySelector('[data-builder-total-score]') : null;
        var builderEmpty = builderForm ? builderForm.querySelector('[data-builder-empty]') : null;
        var publishAfterSaveInput = builderForm ? builderForm.querySelector('[data-publish-after-save]') : null;
        var questionTemplate = document.getElementById('assessment-question-template');
        var choiceTemplate = document.getElementById('assessment-choice-template');

        function letterForIndex(idx) {
            return idx < 26 ? String.fromCharCode(65 + idx) : String(idx + 1);
        }

        function createChoiceElement(text, isCorrect) {
            var frag = choiceTemplate.content.cloneNode(true);
            var row = frag.querySelector('[data-choice-row]');
            var input = row.querySelector('[data-choice-input]');
            var radio = row.querySelector('[data-choice-correct]');
            input.value = text || '';
            radio.checked = Boolean(isCorrect);
            row.classList.toggle('is-correct', Boolean(isCorrect));
            return row;
        }

        function syncChoicesForCard(card, qIdx) {
            var typeSelect = card.querySelector('[data-question-type]');
            var isMcq = !typeSelect || typeSelect.value === 'MCQ';
            var choiceEditor = card.querySelector('[data-choice-editor]');
            var essayHint = card.querySelector('[data-essay-hint]');
            var typeBadge = card.querySelector('[data-question-type-badge]');
            var choiceCountEl = card.querySelector('[data-choice-count]');
            var choiceListEl = card.querySelector('[data-choice-list]');

            if (typeBadge) {
                typeBadge.textContent = isMcq ? 'Trắc nghiệm' : 'Tự luận';
                typeBadge.className = 'assessment-q-type ' + (isMcq ? 'assessment-q-type--mcq' : 'assessment-q-type--essay');
            }
            if (choiceEditor) choiceEditor.hidden = !isMcq;
            if (essayHint) essayHint.hidden = isMcq;

            if (!choiceListEl || !choiceCountEl) return;

            if (isMcq && choiceListEl.querySelectorAll('[data-choice-row]').length < 2) {
                while (choiceListEl.querySelectorAll('[data-choice-row]').length < 2) {
                    var idxNow = choiceListEl.querySelectorAll('[data-choice-row]').length;
                    choiceListEl.appendChild(createChoiceElement('', idxNow === 0));
                }
            }

            var rows = Array.from(choiceListEl.querySelectorAll('[data-choice-row]'));
            choiceCountEl.name = 'choiceCount_' + qIdx;
            choiceCountEl.value = String(isMcq ? rows.length : 0);

            var hasChecked = rows.some(function (r) {
                var rd = r.querySelector('[data-choice-correct]');
                return rd && rd.checked;
            });
            if (isMcq && rows.length > 0 && !hasChecked) {
                var firstRd = rows[0].querySelector('[data-choice-correct]');
                if (firstRd) firstRd.checked = true;
            }

            rows.forEach(function (row, cIdx) {
                var radio = row.querySelector('[data-choice-correct]');
                var letter = row.querySelector('[data-choice-letter]');
                var textInput = row.querySelector('[data-choice-input]');
                var removeBtn = row.querySelector('[data-remove-choice]');

                if (letter) {
                    letter.textContent = letterForIndex(cIdx);
                }
                if (radio) {
                    radio.name = 'correctChoice_' + qIdx;
                    radio.value = String(cIdx);
                    radio.disabled = !isMcq;
                    row.classList.toggle('is-correct', Boolean(isMcq && radio.checked));
                }
                if (textInput) {
                    textInput.name = 'choice_' + qIdx + '_' + cIdx;
                    textInput.placeholder = 'Phương án ' + letterForIndex(cIdx) + '...';
                    textInput.disabled = !isMcq;
                    textInput.required = isMcq;
                }
                if (removeBtn) {
                    removeBtn.disabled = rows.length <= 2;
                }
            });
        }

        function updateTotalScoreDisplay() {
            if (!questionList || !totalScoreBadge) return;
            var cards = Array.from(questionList.querySelectorAll('[data-question-card]'));
            var sum = 0;
            cards.forEach(function (card) {
                var scoreInput = card.querySelector('[data-question-score]');
                var val = scoreInput ? parseFloat(scoreInput.value) : 0;
                if (!isNaN(val) && val > 0) {
                    sum += val;
                }
            });
            var rounded = Math.round(sum * 100) / 100;
            var formatted = Number.isInteger(rounded) ? rounded.toFixed(1) : String(rounded);
            totalScoreBadge.textContent = 'Tổng điểm: ' + formatted + ' / 10.0';
            var isTen = Math.abs(rounded - 10) < 0.001;
            totalScoreBadge.classList.toggle('assessment-score-pill--green', isTen && cards.length > 0);
        }

        function renumberQuestions() {
            if (!questionList) return;
            var cards = Array.from(questionList.querySelectorAll('[data-question-card]'));
            cards.forEach(function (card, qIdx) {
                var numEl = card.querySelector('[data-question-number]');
                var contentEl = card.querySelector('[data-question-content]');
                var typeEl = card.querySelector('[data-question-type]');
                var scoreEl = card.querySelector('[data-question-score]');
                var upBtn = card.querySelector('[data-move-question="up"]');
                var downBtn = card.querySelector('[data-move-question="down"]');

                if (numEl) numEl.textContent = 'Câu ' + (qIdx + 1);
                if (contentEl) contentEl.name = 'questionContent_' + qIdx;
                if (typeEl) typeEl.name = 'questionType_' + qIdx;
                if (scoreEl) scoreEl.name = 'questionScore_' + qIdx;
                if (upBtn) upBtn.disabled = (qIdx === 0);
                if (downBtn) downBtn.disabled = (qIdx === cards.length - 1);

                syncChoicesForCard(card, qIdx);
            });

            if (questionCountInput) {
                questionCountInput.value = String(cards.length);
            }
            if (questionCountBadge) {
                questionCountBadge.textContent = cards.length + ' câu hỏi';
            }
            if (builderEmpty) {
                builderEmpty.hidden = cards.length > 0;
            }
            updateTotalScoreDisplay();
        }

        function addQuestionCard(type, content, score, optionTexts, correctIdx) {
            if (!questionList || !questionTemplate) return null;
            var frag = questionTemplate.content.cloneNode(true);
            var card = frag.querySelector('[data-question-card]');
            var contentEl = card.querySelector('[data-question-content]');
            var typeEl = card.querySelector('[data-question-type]');
            var scoreEl = card.querySelector('[data-question-score]');
            var choiceListEl = card.querySelector('[data-choice-list]');

            if (typeEl) typeEl.value = type || 'MCQ';
            if (contentEl) contentEl.value = content || '';
            if (scoreEl) scoreEl.value = score != null ? score : 2.5;

            var seedOptions = (optionTexts && optionTexts.length) ? optionTexts : ['', '', '', ''];
            var targetCorrect = typeof correctIdx === 'number' && correctIdx >= 0 ? correctIdx : 0;
            if (choiceListEl) {
                seedOptions.forEach(function (txt, idx) {
                    choiceListEl.appendChild(createChoiceElement(txt, idx === targetCorrect));
                });
            }

            questionList.appendChild(card);
            renumberQuestions();
            if (contentEl) contentEl.focus();
            return card;
        }

        if (builderForm && questionList) {
            /* Auto-seed 1 starter MCQ question if opened initially in create mode with 0 questions */
            if (editorPane && !editorPane.hidden && !viewPane
                && questionList.querySelectorAll('[data-question-card]').length === 0) {
                addQuestionCard('MCQ', '', 10, ['', '', '', ''], 0);
            } else {
                renumberQuestions();
            }

            builderForm.querySelectorAll('[data-add-question]').forEach(function (btn) {
                btn.addEventListener('click', function () {
                    var qType = btn.getAttribute('data-add-question') || 'MCQ';
                    var existingCount = questionList.querySelectorAll('[data-question-card]').length;
                    var suggestedScore = existingCount === 0 ? 10 : 2;
                    addQuestionCard(qType, '', suggestedScore, ['', '', '', ''], 0);
                });
            });

            var distributeBtn = builderForm.querySelector('[data-distribute-score]');
            if (distributeBtn) {
                distributeBtn.addEventListener('click', function () {
                    var cards = Array.from(questionList.querySelectorAll('[data-question-card]'));
                    if (cards.length === 0) return;
                    var n = cards.length;
                    var baseHundredths = Math.floor(1000 / n);
                    var remainder = 1000 - baseHundredths * n;
                    cards.forEach(function (card, idx) {
                        var scoreInput = card.querySelector('[data-question-score]');
                        if (scoreInput) {
                            var hundredths = baseHundredths + (idx >= n - remainder ? 1 : 0);
                            scoreInput.value = (hundredths / 100).toFixed(hundredths % 10 === 0 ? 1 : 2);
                        }
                    });
                    updateTotalScoreDisplay();
                });
            }

            questionList.addEventListener('click', function (event) {
                var removeChoiceBtn = event.target.closest('[data-remove-choice]');
                if (removeChoiceBtn) {
                    var choiceRow = removeChoiceBtn.closest('[data-choice-row]');
                    var choiceListEl = removeChoiceBtn.closest('[data-choice-list]');
                    if (choiceRow && choiceListEl && choiceListEl.querySelectorAll('[data-choice-row]').length > 2) {
                        choiceRow.remove();
                        renumberQuestions();
                    }
                    return;
                }

                var addChoiceBtn = event.target.closest('[data-add-choice]');
                if (addChoiceBtn) {
                    var card = addChoiceBtn.closest('[data-question-card]');
                    var listEl = card ? card.querySelector('[data-choice-list]') : null;
                    if (listEl) {
                        var newRow = createChoiceElement('', false);
                        listEl.appendChild(newRow);
                        renumberQuestions();
                        var inp = newRow.querySelector('[data-choice-input]');
                        if (inp) inp.focus();
                    }
                    return;
                }

                var moveBtn = event.target.closest('[data-move-question]');
                if (moveBtn) {
                    var dir = moveBtn.getAttribute('data-move-question');
                    var qCard = moveBtn.closest('[data-question-card]');
                    if (!qCard) return;
                    if (dir === 'up' && qCard.previousElementSibling) {
                        questionList.insertBefore(qCard, qCard.previousElementSibling);
                        renumberQuestions();
                    } else if (dir === 'down' && qCard.nextElementSibling) {
                        questionList.insertBefore(qCard.nextElementSibling, qCard);
                        renumberQuestions();
                    }
                    return;
                }

                var dupBtn = event.target.closest('[data-duplicate-question]');
                if (dupBtn) {
                    var srcCard = dupBtn.closest('[data-question-card]');
                    if (!srcCard) return;
                    var srcType = srcCard.querySelector('[data-question-type]').value;
                    var srcContent = srcCard.querySelector('[data-question-content]').value;
                    var srcScore = srcCard.querySelector('[data-question-score]').value;
                    var srcOpts = [];
                    var srcCorrect = 0;
                    srcCard.querySelectorAll('[data-choice-row]').forEach(function (r, idx) {
                        srcOpts.push(r.querySelector('[data-choice-input]').value);
                        if (r.querySelector('[data-choice-correct]').checked) srcCorrect = idx;
                    });
                    var cloned = addQuestionCard(srcType, srcContent, srcScore, srcOpts, srcCorrect);
                    if (cloned && srcCard.nextElementSibling) {
                        questionList.insertBefore(cloned, srcCard.nextElementSibling);
                        renumberQuestions();
                    }
                    return;
                }

                var removeQBtn = event.target.closest('[data-remove-question]');
                if (removeQBtn) {
                    var targetCard = removeQBtn.closest('[data-question-card]');
                    if (targetCard) {
                        targetCard.remove();
                        renumberQuestions();
                    }
                }
            });

            questionList.addEventListener('change', function (event) {
                if (event.target.matches('[data-question-type]') || event.target.matches('[data-choice-correct]')) {
                    renumberQuestions();
                }
            });

            questionList.addEventListener('input', function (event) {
                if (event.target.matches('[data-question-score]')) {
                    updateTotalScoreDisplay();
                }
            });
        }

        if (builderForm) {
            var saveDraftBtn = builderForm.querySelector('[data-save-draft-btn]');
            var savePublishBtn = builderForm.querySelector('[data-save-publish-btn]');

            if (saveDraftBtn && publishAfterSaveInput) {
                saveDraftBtn.addEventListener('click', function () {
                    publishAfterSaveInput.value = 'false';
                });
            }
            if (savePublishBtn && publishAfterSaveInput) {
                savePublishBtn.addEventListener('click', function () {
                    publishAfterSaveInput.value = 'true';
                });
            }

            builderForm.addEventListener('submit', function (event) {
                if (questionList) {
                    renumberQuestions();
                }
                var titleInput = document.getElementById('assessment-title-input');
                if (titleInput && !titleInput.value.trim()) {
                    event.preventDefault();
                    alert('Vui lòng nhập tiêu đề bài đánh giá.');
                    titleInput.focus();
                    return;
                }
                if (questionList) {
                    var cards = Array.from(questionList.querySelectorAll('[data-question-card]'));
                    var wantPublish = publishAfterSaveInput && publishAfterSaveInput.value === 'true';
                    if (wantPublish && cards.length === 0) {
                        event.preventDefault();
                        alert('Vui lòng thêm ít nhất 1 câu hỏi trước khi công bố bài đánh giá.');
                        return;
                    }
                    for (var i = 0; i < cards.length; i++) {
                        var card = cards[i];
                        var cEl = card.querySelector('[data-question-content]');
                        var sEl = card.querySelector('[data-question-score]');
                        var tEl = card.querySelector('[data-question-type]');
                        if (!cEl.value.trim()) {
                            event.preventDefault();
                            alert('Vui lòng nhập nội dung cho Câu ' + (i + 1) + '.');
                            cEl.focus();
                            return;
                        }
                        var sVal = parseFloat(sEl.value);
                        if (isNaN(sVal) || sVal <= 0) {
                            event.preventDefault();
                            alert('Điểm tối đa của Câu ' + (i + 1) + ' phải lớn hơn 0.');
                            sEl.focus();
                            return;
                        }
                        if (!tEl || tEl.value === 'MCQ') {
                            var optRows = Array.from(card.querySelectorAll('[data-choice-row]'));
                            if (optRows.length < 2) {
                                event.preventDefault();
                                alert('Câu trắc nghiệm số ' + (i + 1) + ' cần ít nhất 2 phương án trả lời.');
                                return;
                            }
                            for (var j = 0; j < optRows.length; j++) {
                                var oInp = optRows[j].querySelector('[data-choice-input]');
                                if (!oInp.value.trim()) {
                                    event.preventDefault();
                                    alert('Vui lòng nhập nội dung phương án ' + letterForIndex(j) + ' của Câu ' + (i + 1) + '.');
                                    oInp.focus();
                                    return;
                                }
                            }
                        }
                    }
                }
            });
        }

        /* 2. Submissions Tab Filtering */
        var rows = Array.from(document.querySelectorAll('[data-submission-row]'));
        var filterForm = document.getElementById('submission-filter-form');
        if (filterForm && rows.length > 0) {
            var keywordInput = document.getElementById('filter-sub-keyword');
            var departmentSelect = document.getElementById('filter-sub-department');
            var majorSelect = document.getElementById('filter-sub-major');
            var programSelect = document.getElementById('filter-sub-program');
            var classSelect = document.getElementById('filter-sub-class');
            var statusSelect = document.getElementById('filter-sub-status');
            var resetBtn = filterForm.querySelector('[data-sub-filter-reset]');
            var badge = document.querySelector('[data-sub-filter-badge]');
            var emptyFilterRow = document.querySelector('[data-empty-filter-row]');
            var paginationSummary = document.querySelector('[data-sub-pagination-summary]');
            var headerCount = document.querySelector('[data-header-sub-count]');

            function populateSelectOptions(selectEl, attrName) {
                if (!selectEl) return;
                var values = Array.from(new Set(rows.map(function (r) {
                    return (r.getAttribute(attrName) || '').trim();
                }).filter(Boolean))).sort(function (a, b) {
                    return a.localeCompare(b, 'vi');
                });
                values.forEach(function (val) {
                    var opt = document.createElement('option');
                    opt.value = val;
                    opt.textContent = val;
                    selectEl.appendChild(opt);
                });
            }

            populateSelectOptions(departmentSelect, 'data-department');
            populateSelectOptions(majorSelect, 'data-major');
            populateSelectOptions(programSelect, 'data-program');
            populateSelectOptions(classSelect, 'data-class-code');

            function applyFilter() {
                var kw = (keywordInput.value || '').trim().toLowerCase();
                var dept = departmentSelect ? departmentSelect.value : '';
                var major = majorSelect ? majorSelect.value : '';
                var prog = programSelect ? programSelect.value : '';
                var cls = classSelect ? classSelect.value : '';
                var st = statusSelect ? statusSelect.value : '';
                var activeFilters = 0;
                if (kw) activeFilters++;
                if (dept) activeFilters++;
                if (major) activeFilters++;
                if (prog) activeFilters++;
                if (cls) activeFilters++;
                if (st) activeFilters++;

                if (badge) {
                    badge.hidden = activeFilters === 0;
                    badge.textContent = activeFilters;
                }
                if (resetBtn) {
                    resetBtn.hidden = activeFilters === 0;
                }

                var visibleCount = 0;
                rows.forEach(function (row) {
                    var code = (row.getAttribute('data-student-code') || '').toLowerCase();
                    var name = (row.getAttribute('data-student-name') || '').toLowerCase();
                    var email = (row.getAttribute('data-email') || '').toLowerCase();
                    var rowDept = (row.getAttribute('data-department') || '').trim();
                    var rowMajor = (row.getAttribute('data-major') || '').trim();
                    var rowProg = (row.getAttribute('data-program') || '').trim();
                    var rowClass = (row.getAttribute('data-class-code') || '').trim();
                    var rowStatus = (row.getAttribute('data-status') || '').trim();

                    var matchKw = !kw || code.indexOf(kw) !== -1 || name.indexOf(kw) !== -1 || email.indexOf(kw) !== -1;
                    var matchDept = !dept || rowDept === dept;
                    var matchMajor = !major || rowMajor === major;
                    var matchProg = !prog || rowProg === prog;
                    var matchCls = !cls || rowClass === cls;
                    var matchSt = !st || rowStatus === st;
                    var visible = matchKw && matchDept && matchMajor && matchProg && matchCls && matchSt;
                    row.hidden = !visible;
                    if (visible) visibleCount++;
                });

                if (emptyFilterRow) {
                    emptyFilterRow.hidden = visibleCount > 0;
                }
                if (paginationSummary) {
                    paginationSummary.textContent = visibleCount === 0
                        ? 'Hiển thị 0 trong ' + rows.length + ' sinh viên'
                        : 'Hiển thị 1–' + visibleCount + ' trong ' + rows.length + ' sinh viên';
                }
                if (headerCount) {
                    headerCount.textContent = activeFilters > 0
                        ? visibleCount + ' / ' + rows.length + ' sinh viên làm bài'
                        : rows.length + ' sinh viên làm bài';
                }
            }

            filterForm.addEventListener('submit', function (e) {
                e.preventDefault();
                applyFilter();
            });
            keywordInput.addEventListener('input', applyFilter);
            if (departmentSelect) departmentSelect.addEventListener('change', applyFilter);
            if (majorSelect) majorSelect.addEventListener('change', applyFilter);
            if (programSelect) programSelect.addEventListener('change', applyFilter);
            if (classSelect) classSelect.addEventListener('change', applyFilter);
            if (statusSelect) statusSelect.addEventListener('change', applyFilter);
            if (resetBtn) {
                resetBtn.addEventListener('click', function () {
                    keywordInput.value = '';
                    if (departmentSelect) departmentSelect.value = '';
                    if (majorSelect) majorSelect.value = '';
                    if (programSelect) programSelect.value = '';
                    if (classSelect) classSelect.value = '';
                    if (statusSelect) statusSelect.value = '';
                    applyFilter();
                });
            }
        }

        /* 3. URL Tab & Auto-Submit Activity Combobox */
        var urlParams = new URLSearchParams(window.location.search);
        var tab = urlParams.get('tab');
        if (tab === 'submissions' || tab === 'applications') {
            var tabBtn = document.getElementById('btn-tab-form-applications');
            var tabPane = document.getElementById('tab-form-applications');
            var infoBtn = document.getElementById('btn-tab-form-info');
            var infoPane = document.getElementById('tab-form-info');
            if (tabBtn && tabPane && infoBtn && infoPane) {
                infoBtn.classList.remove('active');
                infoBtn.setAttribute('aria-selected', 'false');
                infoPane.classList.remove('active');
                tabBtn.classList.add('active');
                tabBtn.setAttribute('aria-selected', 'true');
                tabPane.classList.add('active');
            }
        }

        document.querySelectorAll('.multi-select-combobox[data-auto-submit]').forEach(function (combobox) {
            var form = combobox.closest('form');
            if (!form) return;
            combobox.addEventListener('change', function (event) {
                var input = event.target;
                if (!input || input.type !== 'radio' || !input.checked) return;
                form.submit();
            });
        });
    })();
</script>
</body>
</html>
