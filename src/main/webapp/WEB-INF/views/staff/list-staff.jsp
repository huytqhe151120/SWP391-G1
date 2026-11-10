<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://swp391.g1/status" %>

<c:set var="activeNav" value="staff" scope="request"/>
<!DOCTYPE html>
<html lang="vi">

<head>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261111-2">
    <meta charset="UTF-8">
    <title>Staff List</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/css/staff.css?v=20261111-7">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261107-2">
</head>

<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <div class="activity-app-main">

<div class="page activity-list-page staff-list-page">

    <!-- ================= HEADER ================= -->
    <div class="activity-hero staff-hero">

        <div>

            <span class="eyebrow">
                SWP391-G1 <span aria-hidden="true">/</span> STAFF MANAGEMENT
            </span>

            <h1>
                Danh sách cán bộ / nhân viên
            </h1>

            <p>
                Quản lý và tra cứu thông tin cán bộ, nhân viên
            </p>

        </div>

        <div class="staff-hero-actions">
            <a href="${pageContext.request.contextPath}/home"
               class="staff-home-button">
                Trang chủ
            </a>
        </div>

    </div>

    <c:if test="${param.error == 'notFound'}">
        <div class="alert alert-error activity-alert" role="alert">Không tìm thấy nhân viên.</div>
    </c:if>

    <!-- ================= TABLE CARD ================= -->
    <div class="card activity-table-card staff-table-card">

        <div class="activity-table-heading">

            <div>

                <h2>
                    Danh sách cán bộ / nhân viên
                </h2>

                <p>
                    Danh sách cán bộ / nhân viên trong hệ thống
                </p>

            </div>

            <div class="activity-heading-actions">
                <span class="activity-count"><c:out value="${totalStaffCount}"/> cán bộ</span>
            </div>

        </div>

        <details class="activity-filter-panel activity-filter-panel-white" id="staff-filter-panel" open>
            <summary class="activity-filter-toggle">
                <span>Tìm kiếm &amp; Bộ lọc</span>
                <c:if test="${activeFilterCount > 0}">
                    <span class="activity-filter-badge"><c:out value="${activeFilterCount}"/></span>
                </c:if>
            </summary>
            <form class="activity-filter" action="${pageContext.request.contextPath}/staff" method="get">
                <input type="hidden" name="pageSize" value="${pageSize}">
                <div class="activity-filter-field activity-filter-search">
                    <label for="filter-keyword">Tìm kiếm</label>
                    <input id="filter-keyword" type="search" name="keyword" value="<c:out value='${keyword}'/>"
                           placeholder="Mã hoặc họ tên">
                </div>
                <div class="activity-filter-field">
                    <label for="filter-id">ID</label>
                    <input id="filter-id" type="search" name="id" value="<c:out value='${filterId}'/>"
                           placeholder="Nhập ID">
                </div>
                <div class="activity-filter-field">
                    <label for="filter-code">Mã cán bộ</label>
                    <input id="filter-code" type="text" name="code" value="<c:out value='${filterCode}'/>"
                           placeholder="Nhập mã cán bộ">
                </div>
                <div class="activity-filter-field">
                    <label for="filter-name">Họ và tên</label>
                    <input id="filter-name" type="text" name="name" value="<c:out value='${filterName}'/>"
                           placeholder="Nhập họ tên">
                </div>
                <div class="activity-filter-field">
                    <label for="filter-dob">Ngày sinh</label>
                    <input id="filter-dob" type="date" name="dob" value="<c:out value='${filterDob}'/>">
                </div>
                <div class="activity-filter-field">
                    <label for="filter-gender">Giới tính</label>
                    <select id="filter-gender" name="gender">
                        <option value="">Tất cả</option>
                        <option value="true" ${filterGender == 'true' ? 'selected' : ''}>Nam</option>
                        <option value="false" ${filterGender == 'false' ? 'selected' : ''}>Nữ</option>
                    </select>
                </div>
                <div class="activity-filter-field">
                    <label for="filter-account-id">Account ID</label>
                    <input id="filter-account-id" type="search" name="accountId" value="<c:out value='${filterAccountId}'/>"
                           placeholder="Nhập Account ID">
                </div>
                <div class="activity-filter-field">
                    <label for="filter-status">Trạng thái</label>
                    <select id="filter-status" name="status">
                        <option value="">Tất cả</option>
                        <option value="ACTIVE" ${filterStatus == 'ACTIVE' ? 'selected' : ''}>Đang hoạt động</option>
                        <option value="INACTIVE" ${filterStatus == 'INACTIVE' ? 'selected' : ''}>Không hoạt động</option>
                    </select>
                </div>
                <div class="activity-filter-buttons">
                    <button type="submit" class="button button-primary button-sm">Tìm kiếm &amp; Lọc</button>
                    <c:if test="${filtered}">
                        <a class="button button-sm"
                           href="${pageContext.request.contextPath}/staff?pageSize=${pageSize}">Xóa Bộ lọc</a>
                    </c:if>
                </div>
            </form>
        </details>


        <!-- ================= TABLE ================= -->
        <div class="staff-table-wrap activity-table-wrap">

            <table>

                <thead>

                <tr>
                    <th scope="col" class="col-code">ID</th>
                    <th scope="col" class="col-code">Mã cán bộ</th>
                    <th scope="col" class="col-object-name">Họ và tên</th>
                    <th scope="col" class="col-single-line">Ngày sinh</th>
                    <th scope="col" class="col-single-line">Giới tính</th>
                    <th scope="col" class="col-single-line">Account ID</th>
                    <th scope="col" class="col-status activity-status-column">Trạng thái</th>
                    <th scope="col" class="activity-action-column">Thao tác</th>
                </tr>

                </thead>


                <tbody>
                <c:choose>
                    <c:when test="${empty staffList}">
                        <tr>
                            <td class="empty-state activity-empty-state" colspan="8">
                                <span class="empty-state-mark" aria-hidden="true">+</span>
                                <c:choose>
                                    <c:when test="${filtered}">
                                        <strong>Không tìm thấy cán bộ phù hợp</strong>
                                        <span>Hãy thử thay đổi từ khóa hoặc bộ lọc.</span>
                                    </c:when>
                                    <c:otherwise>
                                        <strong>Chưa có cán bộ nào</strong>
                                        <span>Hiện chưa có cán bộ nào được ghi nhận.</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="staff" items="${staffList}">
                            <tr>
                                <td class="col-code"><span class="activity-code"><c:out value="${staff.id}"/></span></td>
                                <td class="col-code"><strong class="staff-code"><c:out value="${staff.code}"/></strong></td>
                                <td class="col-object-name" title="<c:out value='${staff.name}'/>">
                                    <strong class="activity-name" title="<c:out value='${staff.name}'/>"><c:out value="${staff.name}"/></strong>
                                </td>
                                <td class="col-single-line"><c:out value="${staff.dob}" default="—"/></td>
                                <td class="col-single-line">
                                    <c:choose>
                                        <c:when test="${staff.gender == 'true'}">Nam</c:when>
                                        <c:when test="${staff.gender == 'false'}">Nữ</c:when>
                                        <c:otherwise>—</c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="col-single-line">
                                    <c:choose>
                                        <c:when test="${staff.accountId > 0}"><c:out value="${staff.accountId}"/></c:when>
                                        <c:otherwise>—</c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="col-status activity-status-column"><span class="badge status-pill ${st:css(staff.status)}">${st:label(staff.status)}</span></td>
                                <td class="activity-action-column">
                                    <div class="actions activity-actions">
                                        <a class="activity-action-link action-btn action-btn--view"
                                           href="${pageContext.request.contextPath}/staff?action=detail&amp;id=${staff.id}">
                                            <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg></span>
                                            Chi tiết
                                        </a>
                                        <details class="action-menu">
                                            <summary aria-label="Thao tác khác"><span aria-hidden="true">⋯</span></summary>
                                            <div class="action-menu__items">
                                                <a class="action-menu-item action-menu-item--edit"
                                                   href="${pageContext.request.contextPath}/staff?action=edit&amp;id=${staff.id}">
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
            <form class="activity-page-size" action="${pageContext.request.contextPath}/staff"
                  method="get">
                <input type="hidden" name="page" value="1">
                <input type="hidden" name="keyword" value="<c:out value='${keyword}'/>">
                <input type="hidden" name="id" value="<c:out value='${filterId}'/>">
                <input type="hidden" name="code" value="<c:out value='${filterCode}'/>">
                <input type="hidden" name="name" value="<c:out value='${filterName}'/>">
                <input type="hidden" name="dob" value="<c:out value='${filterDob}'/>">
                <input type="hidden" name="gender" value="<c:out value='${filterGender}'/>">
                <input type="hidden" name="accountId" value="<c:out value='${filterAccountId}'/>">
                <input type="hidden" name="status" value="<c:out value='${filterStatus}'/>">
                <label for="staff-page-size">Hiển thị</label>
                <select id="staff-page-size" name="pageSize" onchange="this.form.submit()">
                    <option value="5" ${pageSize == 5 ? 'selected' : ''}>5</option>
                    <option value="10" ${pageSize == 10 ? 'selected' : ''}>10</option>
                    <option value="20" ${pageSize == 20 ? 'selected' : ''}>20</option>
                    <option value="25" ${pageSize == 25 ? 'selected' : ''}>25</option>
                </select>
                <span>bản ghi mỗi trang</span>
            </form>
            <span class="activity-pagination-summary">
                Hiển thị <c:out value="${firstStaffNumber}"/>–<c:out value="${lastStaffNumber}"/>
                trong <c:out value="${totalStaffCount}"/> cán bộ
            </span>
            <c:if test="${totalPages > 1}">
                <nav class="activity-pagination-nav" aria-label="Phân trang danh sách cán bộ">
                    <c:choose>
                        <c:when test="${currentPage > 1}">
                            <a class="activity-page-link"
                               href="${pageContext.request.contextPath}/staff?page=${currentPage - 1}&amp;pageSize=${pageSize}<c:out value="${filterQuery}"/>"
                               aria-label="Trang trước">‹</a>
                        </c:when>
                        <c:otherwise><span class="activity-page-link is-disabled"
                                           aria-hidden="true">‹</span></c:otherwise>
                    </c:choose>
                    <c:if test="${pageWindowStart > 1}">
                        <a class="activity-page-link"
                           href="${pageContext.request.contextPath}/staff?page=1&amp;pageSize=${pageSize}<c:out value="${filterQuery}"/>">1</a>
                        <c:if test="${pageWindowStart > 2}">
                            <span class="activity-page-ellipsis" aria-hidden="true">...</span>
                        </c:if>
                    </c:if>
                    <c:forEach var="pageNumber" begin="${pageWindowStart}" end="${pageWindowEnd}">
                        <a class="activity-page-link${pageNumber == currentPage ? ' is-active' : ''}"
                           href="${pageContext.request.contextPath}/staff?page=${pageNumber}&amp;pageSize=${pageSize}<c:out value="${filterQuery}"/>"
                           aria-current="${pageNumber == currentPage ? 'page' : ''}"><c:out value="${pageNumber}"/></a>
                    </c:forEach>
                    <c:if test="${pageWindowEnd < totalPages - 1}">
                        <span class="activity-page-ellipsis" aria-hidden="true">...</span>
                    </c:if>
                    <c:if test="${pageWindowEnd < totalPages}">
                        <a class="activity-page-link${totalPages == currentPage ? ' is-active' : ''}"
                           href="${pageContext.request.contextPath}/staff?page=${totalPages}&amp;pageSize=${pageSize}<c:out value="${filterQuery}"/>"
                           aria-current="${totalPages == currentPage ? 'page' : ''}"><c:out value="${totalPages}"/></a>
                    </c:if>
                    <c:choose>
                        <c:when test="${currentPage < totalPages}">
                            <a class="activity-page-link"
                               href="${pageContext.request.contextPath}/staff?page=${currentPage + 1}&amp;pageSize=${pageSize}<c:out value="${filterQuery}"/>"
                               aria-label="Trang tiếp theo">›</a>
                        </c:when>
                        <c:otherwise><span class="activity-page-link is-disabled"
                                           aria-hidden="true">›</span></c:otherwise>
                    </c:choose>
                </nav>
            </c:if>
        </div>
    </div>

</div>

<script src="${pageContext.request.contextPath}/assets/js/action-menu.js?v=5"></script>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>

        </div>
    </div>
</div>
</body>

</html>