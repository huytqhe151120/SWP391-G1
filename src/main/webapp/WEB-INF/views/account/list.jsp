<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://swp391.g1/status" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeNav" value="accounts" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý tài khoản | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/account.css?v=20261110-3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261107-2">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <div class="activity-app-main">
<main class="page activity-list-page account-page">
    <header class="activity-hero">
        <div class="activity-hero-copy">
            <span class="eyebrow">SWP391-G1 <span aria-hidden="true">/</span> QUẢN TRỊ HỆ THỐNG</span>
            <h1>Quản lý tài khoản</h1>
            <p>Theo dõi tài khoản, phân quyền và trạng thái truy cập hệ thống.</p>
        </div>
        <div class="account-hero-actions">
            <a class="button account-home-button" href="${pageContext.request.contextPath}/home">Trang chủ</a>
        </div>
    </header>

    <c:if test="${not empty generalError}">
        <div class="alert alert-error activity-alert" role="alert"><c:out value="${generalError}"/></div>
    </c:if>
    <c:if test="${not empty param.error}">
        <div class="alert alert-error activity-alert" role="alert">
            <c:choose>
                <c:when test="${param.error == 'notFound'}">Không tìm thấy tài khoản.</c:when>
                <c:when test="${param.error == 'invalidStatus'}">Trạng thái không hợp lệ. Các giá trị cho phép: ACTIVE, INACTIVE, BLOCKED.</c:when>
                <c:otherwise>Không thể thực hiện thao tác. Vui lòng thử lại.</c:otherwise>
            </c:choose>
        </div>
    </c:if>
    <c:if test="${not empty param.msg}">
        <div class="alert alert-success activity-alert" role="status">
            <c:choose>
                <c:when test="${param.msg == 'created'}">Tạo tài khoản thành công.</c:when>
                <c:when test="${param.msg == 'updated'}">Cập nhật tài khoản thành công.</c:when>
                <c:when test="${param.msg == 'statusChanged'}">Cập nhật trạng thái tài khoản thành công.</c:when>
                <c:otherwise>Thao tác đã hoàn tất.</c:otherwise>
            </c:choose>
        </div>
    </c:if>

    <section class="card activity-table-card account-table-card" aria-labelledby="account-table-title">
        <div class="activity-table-heading">
            <div>
                <h2 id="account-table-title">Danh sách tài khoản</h2>
                <p>Các tài khoản phù hợp với tiêu chí đã chọn</p>
            </div>
            <div class="activity-heading-actions">
                <span class="activity-count"><c:out value="${totalAccountCount}"/> tài khoản</span>
                <a class="btn-create-activity" href="${pageContext.request.contextPath}/accounts/create">
                    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M8 3v10M3 8h10"/></svg>
                    Tạo tài khoản
                </a>
            </div>
        </div>

        <details class="activity-filter-panel activity-filter-panel-white" id="account-filter-panel" open>
            <summary class="activity-filter-toggle">
                <span>Tìm kiếm &amp; Bộ lọc</span>
                <c:if test="${activeFilterCount > 0}">
                    <span class="activity-filter-badge"><c:out value="${activeFilterCount}"/></span>
                </c:if>
            </summary>
            <form class="activity-filter" action="${pageContext.request.contextPath}/accounts" method="get">
                <input type="hidden" name="pageSize" value="${pageSize}">
                <div class="activity-filter-field activity-filter-search">
                    <label for="filter-search">Tìm kiếm</label>
                    <input id="filter-search" type="search" name="search" value="<c:out value='${search}'/>"
                           placeholder="Tên đăng nhập">
                </div>
                <div class="activity-filter-field activity-filter-search">
                    <label for="filter-id">ID</label>
                    <input id="filter-id" type="search" name="id" value="<c:out value='${filterId}'/>"
                           placeholder="Nhập ID">
                </div>
                <div class="activity-filter-field">
                    <label for="filter-type">Loại tài khoản</label>
                    <select id="filter-type" name="type">
                        <option value="">Tất cả</option>
                        <c:forEach items="${accountTypes}" var="t">
                            <option value="${fn:escapeXml(t)}" ${t == filterType ? 'selected' : ''}><c:out value="${t}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="activity-filter-field">
                    <label for="filter-role">Vai trò</label>
                    <select id="filter-role" name="role">
                        <option value="">Tất cả</option>
                        <c:forEach items="${accountRoles}" var="r">
                            <option value="${fn:escapeXml(r)}" ${r == filterRole ? 'selected' : ''}><c:out value="${r}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="activity-filter-field">
                    <label for="filter-status">Trạng thái</label>
                    <select id="filter-status" name="status">
                        <option value="">Tất cả</option>
                        <c:forEach items="${accountStatuses}" var="s">
                            <option value="${fn:escapeXml(s)}" ${s == filterStatus ? 'selected' : ''}>${st:label(s)}</option>
                        </c:forEach>
                    </select>
                </div>
                <div class="activity-filter-buttons">
                    <button type="submit" class="button button-primary button-sm">Tìm kiếm &amp; Lọc</button>
                    <c:if test="${filtered}">
                        <a class="button button-sm"
                           href="${pageContext.request.contextPath}/accounts?pageSize=${pageSize}">Xóa Bộ lọc</a>
                    </c:if>
                </div>
            </form>
        </details>
        <div class="table-wrap activity-table-wrap">
            <table class="account-table">
                <thead>
                <tr>
                    <th scope="col" class="col-code">ID</th>
                    <th scope="col" class="col-object-name">Tên đăng nhập</th>
                    <th scope="col" class="col-single-line">Loại</th>
                    <th scope="col" class="col-single-line">Vai trò</th>
                    <th scope="col" class="col-status">Trạng thái</th>
                    <th scope="col">Thao tác</th>
                </tr>
                </thead>
                <tbody>
                <c:choose>
                    <c:when test="${empty accounts}">
                        <tr>
                            <td class="empty-state activity-empty-state" colspan="6">
                                <span class="empty-state-mark" aria-hidden="true">+</span>
                                <c:choose>
                                    <c:when test="${filtered}">
                                        <strong>Không tìm thấy tài khoản phù hợp</strong>
                                        <span>Hãy thử thay đổi từ khóa hoặc bộ lọc.</span>
                                    </c:when>
                                    <c:otherwise>
                                        <strong>Chưa có tài khoản nào</strong>
                                        <span>Tạo tài khoản mới để bắt đầu quản lý danh sách.</span>
                                        <a class="button button-primary" href="${pageContext.request.contextPath}/accounts/create">Tạo tài khoản đầu tiên</a>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach items="${accounts}" var="a">
                            <tr>
                                <td class="col-code"><span class="activity-code"><c:out value="${a.id}"/></span></td>
                                <td class="col-object-name" title="<c:out value='${a.username}'/>">
                                    <strong class="activity-name" title="<c:out value='${a.username}'/>"><c:out value="${a.username}"/></strong>
                                </td>
                                <td class="col-single-line" title="<c:out value='${a.type}'/>"><c:out value="${a.type}"/></td>
                                <td class="col-single-line" title="<c:out value='${a.role}'/>"><c:out value="${a.role}"/></td>
                                <td class="col-status"><span class="badge status-pill account-status account-status-${fn:escapeXml(a.status)}">${st:label(a.status)}</span></td>
                                <td>
                                    <div class="actions activity-actions">
                                        <a class="activity-action-link action-btn action-btn--view" href="${pageContext.request.contextPath}/accounts/${a.id}">
                                            <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg></span>
                                            Chi tiết
                                        </a>
                                        <details class="action-menu">
                                            <summary aria-label="Thao tác khác"><span aria-hidden="true">⋯</span></summary>
                                            <div class="action-menu__items">
                                                <a class="action-menu-item action-menu-item--edit" href="${pageContext.request.contextPath}/accounts/${a.id}/edit">
                                                    <span class="action-menu-item__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"/></svg></span>
                                                    Chỉnh sửa
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
        <div class="activity-pagination">
            <form class="activity-page-size" action="${pageContext.request.contextPath}/accounts"
                  method="get">
                <input type="hidden" name="page" value="1">
                <input type="hidden" name="id" value="<c:out value='${filterId}'/>">
                <input type="hidden" name="search" value="<c:out value='${search}'/>">
                <input type="hidden" name="type" value="<c:out value='${filterType}'/>">
                <input type="hidden" name="role" value="<c:out value='${filterRole}'/>">
                <input type="hidden" name="status" value="<c:out value='${filterStatus}'/>">
                <label for="account-page-size">Hiển thị</label>
                <select id="account-page-size" name="pageSize" onchange="this.form.submit()">
                    <option value="5" ${pageSize == 5 ? 'selected' : ''}>5</option>
                    <option value="10" ${pageSize == 10 ? 'selected' : ''}>10</option>
                    <option value="20" ${pageSize == 20 ? 'selected' : ''}>20</option>
                    <option value="25" ${pageSize == 25 ? 'selected' : ''}>25</option>
                </select>
                <span>bản ghi mỗi trang</span>
            </form>
            <span class="activity-pagination-summary">
                Hiển thị <c:out value="${firstAccountNumber}"/>–<c:out value="${lastAccountNumber}"/>
                trong <c:out value="${totalAccountCount}"/> tài khoản
            </span>
            <c:if test="${totalPages > 1}">
                <nav class="activity-pagination-nav" aria-label="Phân trang danh sách tài khoản">
                    <c:choose>
                        <c:when test="${currentPage > 1}">
                            <a class="activity-page-link"
                               href="${pageContext.request.contextPath}/accounts?page=${currentPage - 1}&amp;pageSize=${pageSize}<c:out value="${filterQuery}"/>"
                               aria-label="Trang trước">‹</a>
                        </c:when>
                        <c:otherwise><span class="activity-page-link is-disabled"
                                           aria-hidden="true">‹</span></c:otherwise>
                    </c:choose>
                    <c:if test="${pageWindowStart > 1}">
                        <a class="activity-page-link"
                           href="${pageContext.request.contextPath}/accounts?page=1&amp;pageSize=${pageSize}<c:out value="${filterQuery}"/>">1</a>
                        <c:if test="${pageWindowStart > 2}">
                            <span class="activity-page-ellipsis" aria-hidden="true">...</span>
                        </c:if>
                    </c:if>
                    <c:forEach var="pageNumber" begin="${pageWindowStart}" end="${pageWindowEnd}">
                        <a class="activity-page-link${pageNumber == currentPage ? ' is-active' : ''}"
                           href="${pageContext.request.contextPath}/accounts?page=${pageNumber}&amp;pageSize=${pageSize}<c:out value="${filterQuery}"/>"
                           aria-current="${pageNumber == currentPage ? 'page' : ''}"><c:out value="${pageNumber}"/></a>
                    </c:forEach>
                    <c:if test="${pageWindowEnd < totalPages - 1}">
                        <span class="activity-page-ellipsis" aria-hidden="true">...</span>
                    </c:if>
                    <c:if test="${pageWindowEnd < totalPages}">
                        <a class="activity-page-link${totalPages == currentPage ? ' is-active' : ''}"
                           href="${pageContext.request.contextPath}/accounts?page=${totalPages}&amp;pageSize=${pageSize}<c:out value="${filterQuery}"/>"
                           aria-current="${totalPages == currentPage ? 'page' : ''}"><c:out value="${totalPages}"/></a>
                    </c:if>
                    <c:choose>
                        <c:when test="${currentPage < totalPages}">
                            <a class="activity-page-link"
                               href="${pageContext.request.contextPath}/accounts?page=${currentPage + 1}&amp;pageSize=${pageSize}<c:out value="${filterQuery}"/>"
                               aria-label="Trang tiếp theo">›</a>
                        </c:when>
                        <c:otherwise><span class="activity-page-link is-disabled"
                                           aria-hidden="true">›</span></c:otherwise>
                    </c:choose>
                </nav>
            </c:if>
        </div>
    </section>
</main>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/action-menu.js?v=5"></script>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>
