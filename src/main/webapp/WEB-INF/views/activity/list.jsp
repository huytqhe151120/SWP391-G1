<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://swp391.g1/status" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="activities" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>EAMS | Hoạt động ngoại khóa</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261019-4">
</head>
<body>
<div class="activity-app-shell">
    <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>

    <div class="activity-app-layout">
        <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
        <main class="page activity-list-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> QUẢN LÝ HOẠT ĐỘNG NGOẠI KHÓA</span>
                    <h1>Hoạt động ngoại khóa</h1>
                    <p>Theo dõi và quản lý các hoạt động ngoại khóa của trường tại một nơi.</p>
                </div>
                <div class="account-hero-actions">
                    <c:if test="${canManageActivities}">
                        <a class="button button-primary activity-create-button" href="${pageContext.request.contextPath}/activities?action=create">
                            <span class="plus-icon" aria-hidden="true">+</span>
                            Tạo hoạt động
                        </a>
                    </c:if>
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

    <section class="card activity-table-card" aria-labelledby="activity-table-title">
        <div class="activity-table-heading">
            <div>
                <h2 id="activity-table-title">Danh sách hoạt động</h2>
                <p>Thông tin các hoạt động ngoại khóa đã được ghi nhận</p>
            </div>
            <span class="activity-count"><c:out value="${totalActivityCount}"/> hoạt động</span>
        </div>
        <details class="activity-filter-panel" id="activity-filter-panel">
        <summary class="activity-filter-toggle">
            <span>Tìm kiếm &amp; Bộ lọc</span>
            <c:if test="${activeFilterCount > 0}">
                <span class="activity-filter-badge"><c:out value="${activeFilterCount}"/></span>
            </c:if>
        </summary>
        <form class="activity-filter" action="${pageContext.request.contextPath}/activities" method="get">
            <input type="hidden" name="pageSize" value="${pageSize}">
            <div class="activity-filter-field activity-filter-search">
                <label for="filter-keyword">Tìm kiếm</label>
                <input id="filter-keyword" type="search" name="keyword" value="<c:out value='${keyword}'/>"
                       placeholder="Mã hoặc tên hoạt động">
            </div>
            <div class="activity-filter-field">
                <label for="filter-semester">Học kỳ</label>
                <select id="filter-semester" name="semesterId">
                    <option value="">Tất cả</option>
                    <c:forEach var="semester" items="${semesters}">
                        <option value="${semester.id}" ${selectedSemesterId == semester.id ? 'selected' : ''}><c:out value="${semester.name}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="activity-filter-field">
                <label for="filter-type">Loại hoạt động</label>
                <select id="filter-type" name="activityTypeId">
                    <option value="">Tất cả</option>
                    <c:forEach var="type" items="${activityTypes}">
                        <option value="${type.id}" ${selectedActivityTypeId == type.id ? 'selected' : ''}><c:out value="${type.name}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="activity-filter-field">
                <label for="filter-department">Đơn vị phụ trách chính</label>
                <select id="filter-department" name="departmentId">
                    <option value="">Tất cả</option>
                    <c:forEach var="department" items="${departments}">
                        <option value="${department.id}" ${selectedDepartmentId == department.id ? 'selected' : ''}><c:out value="${department.name}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="activity-filter-field">
                <label for="filter-activity-status">Trạng thái hoạt động</label>
                <select id="filter-activity-status" name="activityStatus">
                    <option value="">Tất cả</option>
                    <c:forEach var="status" items="${activityStatuses}">
                        <option value="${status}" ${selectedActivityStatus == status.name() ? 'selected' : ''}>${st:label(status)}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="activity-filter-field">
                <label for="filter-approval-status">Trạng thái duyệt</label>
                <select id="filter-approval-status" name="approvalStatus">
                    <option value="">Tất cả</option>
                    <c:forEach var="status" items="${approvalStatuses}">
                        <option value="${status}" ${selectedApprovalStatus == status.name() ? 'selected' : ''}>${st:label(status)}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="activity-filter-field">
                <label for="filter-start-date">Bắt đầu từ ngày</label>
                <input id="filter-start-date" class="js-date-input" type="text" name="startDate"
                       value="<c:out value='${startDate}'/>" placeholder="dd/mm/yyyy" inputmode="numeric"
                       maxlength="10" pattern="\d{2}/\d{2}/\d{4}" autocomplete="off"
                       title="Nhập ngày theo định dạng dd/mm/yyyy">
            </div>
            <div class="activity-filter-field">
                <label for="filter-end-date">Kết thúc đến ngày</label>
                <input id="filter-end-date" class="js-date-input" type="text" name="endDate"
                       value="<c:out value='${endDate}'/>" placeholder="dd/mm/yyyy" inputmode="numeric"
                       maxlength="10" pattern="\d{2}/\d{2}/\d{4}" autocomplete="off"
                       title="Nhập ngày theo định dạng dd/mm/yyyy">
            </div>
            <div class="activity-filter-buttons">
                <button type="submit" class="button button-primary button-sm">Tìm kiếm & Lọc</button>
                <c:if test="${filtered}">
                    <a class="button button-sm" href="${pageContext.request.contextPath}/activities?pageSize=${pageSize}">Xóa Bộ lọc</a>
                </c:if>
            </div>
        </form>
        </details>
        <div class="table-wrap activity-table-wrap">
            <table>
                <thead>
                <tr>
                    <th scope="col">Mã hoạt động</th>
                    <th scope="col">Tên hoạt động</th>
                    <th scope="col">Học kỳ</th>
                    <th scope="col">Loại hoạt động</th>
                    <th scope="col">Đơn vị phụ trách chính</th>
                    <th scope="col">Thời gian</th>
                    <th scope="col" class="activity-run-status"><span>Trạng thái</span><span>hoạt động</span></th>
                    <th scope="col" class="activity-status-column"><span>Trạng thái</span><span>duyệt HĐ</span></th>
                    <th scope="col">Thao tác</th>
                </tr>
                </thead>
                <tbody>
                <c:choose>
                    <c:when test="${empty activities}">
                    <tr>
                        <td class="empty-state activity-empty-state" colspan="9">
                            <span class="empty-state-mark" aria-hidden="true">+</span>
                            <c:choose>
                                <c:when test="${filtered}">
                                    <strong>Không tìm thấy hoạt động phù hợp</strong>
                                    <span>Hãy thử thay đổi từ khóa hoặc bộ lọc.</span>
                                </c:when>
                                <c:when test="${canManageActivities}">
                                    <strong>Chưa có hoạt động nào</strong>
                                    <span>Tạo hoạt động mới để bắt đầu quản lý danh sách.</span>
                                    <a class="button button-primary" href="${pageContext.request.contextPath}/activities?action=create">Tạo hoạt động đầu tiên</a>
                                </c:when>
                                <c:otherwise>
                                    <strong>Chưa có hoạt động nào</strong>
                                    <span>Hiện chưa có hoạt động nào được ghi nhận.</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="activity" items="${activities}">
                            <tr>
                                <td><span class="activity-code"><c:out value="${activity.code}"/></span></td>
                                <td><strong class="activity-name"><c:out value="${activity.name}"/></strong></td>
                                <td>
                                    <span class="table-primary-text"><c:out value="${activity.semesterName}" default="—"/></span>
                                </td>
                                <td><c:out value="${activity.activityTypeName}" default="—"/></td>
                                <td><c:out value="${activity.responsibleDepartmentName}" default="—"/></td>
                                <td class="activity-time">
                                    <c:choose>
                                        <c:when test="${not empty activity.startTime}">
                                            ${dt:format(activity.startTime)}
                                            <c:if test="${not empty activity.endTime}"><span>đến</span>${dt:format(activity.endTime)}</c:if>
                                        </c:when>
                                        <c:otherwise>Chưa xác định</c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="activity-run-status"><span class="badge status-pill ${st:css(activity.activityStatus)}">${st:label(activity.activityStatus)}</span></td>
                                <td class="activity-status-column"><span class="badge status-pill ${st:css(activity.approvalStatus)}">${st:label(activity.approvalStatus)}</span></td>
                                <td>
                                    <div class="actions activity-actions">
                                        <div class="activity-action-links">
                                            <a class="activity-action-link" href="${pageContext.request.contextPath}/activities?action=detail&amp;id=${activity.id}">Chi tiết</a>
                                            <c:if test="${canManageActivities && activity.activityStatus != 'ONGOING' && activity.activityStatus != 'COMPLETED'}">
                                                <a class="activity-action-link" href="${pageContext.request.contextPath}/activities?action=edit&amp;id=${activity.id}">Sửa</a>
                                                <form action="${pageContext.request.contextPath}/activities" method="post"
                                                      data-confirm="Xóa hoạt động này sẽ xóa các phân công đơn vị, nhân viên, liên kết đối tác và lịch sử lời mời liên kết của hoạt động. Thao tác này không thể hoàn tác. Bạn có chắc chắn muốn tiếp tục?">
                                                    <input type="hidden" name="action" value="delete">
                                                    <input type="hidden" name="id" value="${activity.id}">
                                                    <button type="submit" class="activity-action-link activity-action-delete">Xóa</button>
                                                </form>
                                            </c:if>
                                        </div>
                                        <c:if test="${activity.approvalStatus == 'REJECTED' && (canManageActivities || canApproveActivities)}">
                                            <div class="activity-review-actions-row">
                                                <c:if test="${canManageActivities}">
                                                <form class="activity-review-actions" action="${pageContext.request.contextPath}/activity/approval" method="post">
                                                    <input type="hidden" name="id" value="${activity.id}">
                                                    <button type="submit" name="action" value="submit" class="button button-send-approval button-sm">Gửi duyệt</button>
                                                </form>
                                                </c:if>
                                                <c:if test="${canApproveActivities}">
                                                    <form class="activity-review-actions" action="${pageContext.request.contextPath}/activity/approval" method="post">
                                                        <input type="hidden" name="id" value="${activity.id}">
                                                        <button type="submit" name="action" value="revokeRejection"
                                                                class="button button-sm button-revoke"
                                                                data-confirm="Hủy từ chối và đưa hoạt động về trạng thái chờ duyệt?">Hủy từ chối</button>
                                                    </form>
                                                </c:if>
                                            </div>
                                        </c:if>
                                        <c:if test="${canManageActivities && activity.approvalStatus == 'DRAFT'}">
                                            <form class="activity-review-actions" action="${pageContext.request.contextPath}/activity/approval" method="post">
                                                <input type="hidden" name="id" value="${activity.id}">
                                                <button type="submit" name="action" value="submit" class="button button-primary button-send-approval button-sm">Gửi duyệt</button>
                                            </form>
                                        </c:if>
                                        <c:if test="${canApproveActivities && activity.approvalStatus == 'PENDING'}">
                                            <form class="activity-review-actions" action="${pageContext.request.contextPath}/activity/approval" method="post">
                                                <input type="hidden" name="id" value="${activity.id}">
                                                <button type="submit" name="action" value="approve" class="button button-approve button-sm">Duyệt</button>
                                                <button type="submit" name="action" value="reject" class="button button-reject button-sm">Từ chối</button>
                                            </form>
                                        </c:if>
                                        <c:if test="${canApproveActivities && activity.approvalStatus == 'APPROVED' && activity.activityStatus == 'UPCOMING'}">
                                            <form class="activity-review-actions" action="${pageContext.request.contextPath}/activity/approval" method="post">
                                                <input type="hidden" name="id" value="${activity.id}">
                                                <button type="submit" name="action" value="revokeApproval"
                                                        class="button button-revoke button-sm"
                                                        data-confirm="Hủy duyệt và đưa hoạt động về trạng thái chờ duyệt?">Hủy duyệt</button>
                                            </form>
                                        </c:if>

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
            <form class="activity-page-size" action="${pageContext.request.contextPath}/activities" method="get">
                <input type="hidden" name="page" value="1">
                <input type="hidden" name="keyword" value="<c:out value='${keyword}'/>">
                <input type="hidden" name="semesterId" value="${selectedSemesterId > 0 ? selectedSemesterId : ''}">
                <input type="hidden" name="activityTypeId" value="${selectedActivityTypeId > 0 ? selectedActivityTypeId : ''}">
                <input type="hidden" name="departmentId" value="${selectedDepartmentId > 0 ? selectedDepartmentId : ''}">
                <input type="hidden" name="activityStatus" value="<c:out value='${selectedActivityStatus}'/>">
                <input type="hidden" name="approvalStatus" value="<c:out value='${selectedApprovalStatus}'/>">
                <input type="hidden" name="startDate" value="<c:out value='${startDate}'/>">
                <input type="hidden" name="endDate" value="<c:out value='${endDate}'/>">
                <label for="activity-page-size">Hiển thị</label>
                <select id="activity-page-size" name="pageSize" onchange="this.form.submit()">
                    <option value="5" ${pageSize == 5 ? 'selected' : ''}>5</option>
                    <option value="10" ${pageSize == 10 ? 'selected' : ''}>10</option>
                    <option value="20" ${pageSize == 20 ? 'selected' : ''}>20</option>
                    <option value="25" ${pageSize == 25 ? 'selected' : ''}>25</option>
                </select>
                <span>bản ghi mỗi trang</span>
            </form>
            <span class="activity-pagination-summary">
                Hiển thị <c:out value="${firstActivityNumber}"/>–<c:out value="${lastActivityNumber}"/>
                trong <c:out value="${totalActivityCount}"/> hoạt động
            </span>
            <c:if test="${totalPages > 1}">
                <nav class="activity-pagination-nav" aria-label="Phân trang danh sách hoạt động">
                    <c:choose>
                        <c:when test="${currentPage > 1}">
                            <a class="activity-page-link"
                               href="${pageContext.request.contextPath}/activities?page=${currentPage - 1}&amp;pageSize=${pageSize}<c:out value="${filterQuery}"/>"
                               aria-label="Trang trước">‹</a>
                        </c:when>
                        <c:otherwise><span class="activity-page-link is-disabled" aria-hidden="true">‹</span></c:otherwise>
                    </c:choose>
                    <c:forEach var="pageNumber" begin="1" end="${totalPages}">
                        <a class="activity-page-link${pageNumber == currentPage ? ' is-active' : ''}"
                           href="${pageContext.request.contextPath}/activities?page=${pageNumber}&amp;pageSize=${pageSize}<c:out value="${filterQuery}"/>"
                           aria-current="${pageNumber == currentPage ? 'page' : ''}"><c:out value="${pageNumber}"/></a>
                    </c:forEach>
                    <c:choose>
                        <c:when test="${currentPage < totalPages}">
                            <a class="activity-page-link"
                               href="${pageContext.request.contextPath}/activities?page=${currentPage + 1}&amp;pageSize=${pageSize}<c:out value="${filterQuery}"/>"
                               aria-label="Trang tiếp theo">›</a>
                        </c:when>
                        <c:otherwise><span class="activity-page-link is-disabled" aria-hidden="true">›</span></c:otherwise>
                    </c:choose>
                </nav>
            </c:if>
        </div>
    </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/activity.js?v=20261016-5"></script>
</body>
</html>
