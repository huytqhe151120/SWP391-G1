<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="partner-staff" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Nhân viên đối tác | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/partner-staff.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/list-pagination.css?v=1">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <div class="activity-app-main">
<main class="page activity-list-page">
    <header class="activity-hero">
        <div class="activity-hero-copy">
            <span class="eyebrow">SWP391-G1 <span aria-hidden="true">/</span> QUẢN LÝ ĐỐI TÁC</span>
            <h1>Nhân viên đối tác</h1>
            <p>Quản lý thông tin và công ty của các nhân viên đối tác.</p>
        </div>
    </header>

    <c:if test="${not empty sessionScope.successMessage}">
        <div class="alert alert-success activity-alert"><c:out value="${sessionScope.successMessage}"/></div>
        <c:remove var="successMessage" scope="session"/>
    </c:if>
    <c:if test="${not empty sessionScope.errorMessage}">
        <div class="alert alert-error activity-alert"><c:out value="${sessionScope.errorMessage}"/></div>
        <c:remove var="errorMessage" scope="session"/>
    </c:if>

    <section class="card activity-table-card" aria-labelledby="staff-table-title" data-paginated-list>
        <div class="activity-table-heading">
            <div>
                <h2 id="staff-table-title">Danh sách nhân viên</h2>
                <p>Thông tin nhân viên thuộc các công ty đối tác</p>
            </div>
            <div class="activity-heading-actions">
                <span class="activity-count">
                    <c:choose>
                        <c:when test="${empty partnerStaffs}">0 nhân viên</c:when>
                        <c:otherwise><c:out value="${partnerStaffs.size()}"/> nhân viên</c:otherwise>
                    </c:choose>
                </span>
                <a class="btn-create-activity" href="${pageContext.request.contextPath}/partner-staffs?action=create">
                    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M8 3v10M3 8h10"/></svg>
                    Thêm nhân viên
                </a>
            </div>
        </div>
        <form class="partner-staff-filter" method="get" action="${pageContext.request.contextPath}/partner-staffs">
            <label for="companyFilter">Lọc theo công ty</label>
            <select id="companyFilter" name="companyId">
                <option value="">Tất cả công ty</option>
                <c:forEach var="company" items="${partnerCompanies}">
                    <option value="${company.id}" <c:if test="${selectedCompanyId == company.id}">selected</c:if>>
                        <c:out value="${company.code}"/> - <c:out value="${company.name}"/>
                    </option>
                </c:forEach>
            </select>
            <noscript><button class="button" type="submit">Lọc</button></noscript>
        </form>
        <div class="table-wrap activity-table-wrap">
            <table class="partner-staff-table">
                <thead>
                <tr>
                    <th scope="col" class="col-code">Mã nhân viên</th>
                    <th scope="col" class="col-object-name">Họ và tên</th>
                    <th scope="col" class="col-single-line">Công ty</th>
                    <th scope="col" class="col-single-line">Chức vụ</th>
                    <th scope="col" class="col-status">Trạng thái</th>
                    <th scope="col">Thao tác</th>
                </tr>
                </thead>
                <tbody>
                <c:choose>
                    <c:when test="${empty partnerStaffs}">
                        <tr>
                            <td class="empty-state activity-empty-state" colspan="6">
                                <span class="empty-state-mark" aria-hidden="true">+</span>
                                <strong>Chưa có nhân viên đối tác</strong>
                                <span>Thêm nhân viên mới để bắt đầu quản lý danh sách.</span>
                                <a class="button button-primary" href="${pageContext.request.contextPath}/partner-staffs?action=create">Thêm nhân viên đầu tiên</a>
                            </td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="staff" items="${partnerStaffs}">
                            <tr data-page-item>
                                <td class="col-code"><span class="activity-code"><c:out value="${staff.code}"/></span></td>
                                <td class="col-object-name" title="<c:out value='${staff.name}'/>">
                                    <strong class="activity-name" title="<c:out value='${staff.name}'/>"><c:out value="${staff.name}"/></strong>
                                </td>
                                <td class="col-single-line" title="<c:out value='${staff.companyName}'/>">
                                    <span class="table-primary-text"><c:out value="${staff.companyName}" default="—"/></span>
                                    <c:if test="${not empty staff.companyCode}"><span class="partner-staff-company-code"><c:out value="${staff.companyCode}"/></span></c:if>
                                </td>
                                <td class="col-single-line" title="<c:out value='${staff.position}'/>"><c:out value="${staff.position}" default="—"/></td>
                                <td class="col-status">
                                    <span class="badge status-pill ${staff.status == 'ACTIVE' ? 'status-green' : 'status-gray'}">
                                        <c:choose>
                                            <c:when test="${staff.status == 'ACTIVE'}">Đang hoạt động</c:when>
                                            <c:otherwise>Không hoạt động</c:otherwise>
                                        </c:choose>
                                    </span>
                                </td>
                                <td>
                                    <div class="actions activity-actions">
                                        <a class="activity-action-link action-btn action-btn--view" href="${pageContext.request.contextPath}/partner-staffs?action=detail&amp;id=${staff.id}">
                                            <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg></span>
                                            Chi tiết
                                        </a>
                                        <details class="action-menu">
                                            <summary aria-label="Thao tác khác"><span aria-hidden="true">⋯</span></summary>
                                            <div class="action-menu__items">
                                                <a class="action-menu-item action-menu-item--edit" href="${pageContext.request.contextPath}/partner-staffs?action=edit&amp;id=${staff.id}">
                                                    <span class="action-menu-item__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"/></svg></span>
                                                    Chỉnh sửa
                                                </a>
                                                <a class="action-menu-item action-menu-item--delete activity-action-delete"
                                                   href="${pageContext.request.contextPath}/partner-staffs?action=delete&amp;id=${staff.id}"
                                                   data-confirm="Bạn có chắc muốn xóa nhân viên đối tác này?">
                                                    <span class="action-menu-item__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="m19 6-1 14H6L5 6"/><path d="M10 11v5"/><path d="M14 11v5"/></svg></span>
                                                    Xóa
                                                </a>
                                            </div>
                                        </details>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
                </tbody>
            </table>
        </div>
    </section>
</main>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/action-menu.js?v=5"></script>
<script src="${pageContext.request.contextPath}/assets/js/partner-staff.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
<script src="${pageContext.request.contextPath}/assets/js/list-pagination.js?v=1" defer></script>
</body>
</html>
