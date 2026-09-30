<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Activity List</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/css/activity.css">
</head>

<body>

<div class="page activity-list-page">

    <!-- ================= HERO ================= -->
    <div class="activity-hero page-header">

        <div class="activity-hero-copy">

            <span class="eyebrow">ACTIVITY MANAGEMENT</span>

            <h1>Danh sách hoạt động ngoại khóa</h1>

            <p>Quản lý, theo dõi và phê duyệt hoạt động ngoại khóa</p>

        </div>

        <!-- Create -->
        <a class="button activity-create-button"
           href="${pageContext.request.contextPath}/activity?action=create">
            <span class="plus-icon" aria-hidden="true">+</span>
            Thêm hoạt động
        </a>

    </div>


    <!-- ================= SEARCH + FILTER ================= -->
    <div class="toolbar activity-toolbar"
         style="justify-content: flex-start; flex-wrap: wrap; margin-bottom: 24px;">

        <!-- Search -->
        <div class="activity-search-wrapper"
             style="flex: 0 1 420px; min-width: 240px;">

            <input
                    type="text"
                    id="activitySearch"
                    class="activity-search"
                    placeholder="Tìm theo mã hoặc tên hoạt động..."
            >

        </div>


        <!-- Activity Status -->
        <select id="activityStatusFilter"
                class="activity-filter"
                style="width: auto; min-width: 220px;">
            <option value="">Tất cả Activity Status</option>
            <option value="UPCOMING">Upcoming</option>
            <option value="ONGOING">Ongoing</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
        </select>


        <!-- Approval Status -->
        <select id="approvalStatusFilter"
                class="activity-filter"
                style="width: auto; min-width: 220px;">
            <option value="">Tất cả Approval Status</option>
            <option value="DRAFT">Draft</option>
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
        </select>

    </div>


    <!-- ================= TABLE CARD ================= -->
    <div class="card activity-table-card">

        <div class="activity-table-heading">

            <div>
                <h2>Activity List</h2>
                <p>Danh sách hoạt động ngoại khóa</p>
            </div>

            <span class="activity-count">
                <c:out value="${activity.size()}"/> hoạt động
            </span>

        </div>


        <div class="table-wrap activity-table-wrap">

            <table>

                <thead>
                <tr>
                    <th scope="col">ID</th>
                    <th scope="col">Mã hoạt động</th>
                    <th scope="col">Tên hoạt động</th>
                    <th scope="col">Activity Type ID</th>
                    <th scope="col">Activity Status</th>
                    <th scope="col">Approval Status</th>
                    <th scope="col">Thao tác</th>
                </tr>
                </thead>


                <tbody>

                <c:forEach var="item" items="${activity}">

                    <!-- ================= ROW ================= -->
                    <tr class="activity-row"
                        data-activity-status="${item.activityStatus}"
                        data-approval-status="${item.approvalStatus}">

                        <!-- ID -->
                        <td><c:out value="${item.id}"/></td>

                        <!-- Code -->
                        <td>
                            <span class="activity-code">
                                <c:out value="${item.code}"/>
                            </span>
                        </td>

                        <!-- Name -->
                        <td>
                            <strong class="activity-name">
                                <c:out value="${item.name}"/>
                            </strong>
                        </td>

                        <!-- Activity Type -->
                        <td>
                            <c:out value="${item.activityType}"/>
                        </td>

                        <!-- Activity Status -->
                        <td>
                            <span class="badge activity-status">
                                <c:out value="${item.activityStatus}"/>
                            </span>
                        </td>

                        <!-- Approval Status -->
                        <td>
                            <c:choose>

                                <c:when test="${item.approvalStatus == 'PENDING'}">
                                    <span class="badge"
                                          style="background:#fff7e6; color:#9a6300;">Pending</span>
                                </c:when>

                                <c:when test="${item.approvalStatus == 'APPROVED'}">
                                    <span class="badge"
                                          style="background:#eaf7ef; color:#21633a;">Approved</span>
                                </c:when>

                                <c:when test="${item.approvalStatus == 'REJECTED'}">
                                    <span class="badge"
                                          style="background:#fff0f0; color:#9b2626;">Rejected</span>
                                </c:when>

                                <c:otherwise>
                                    <span class="badge"
                                          style="background:#f2f4f7; color:#687386;">
                                        <c:out value="${item.approvalStatus}"/>
                                    </span>
                                </c:otherwise>

                            </c:choose>
                        </td>


                        <!-- ================= ACTIONS ================= -->
                        <td>

                            <div class="actions activity-actions">

                                <!-- Detail -->
                                <a class="activity-action-link"
                                   href="${pageContext.request.contextPath}/activity?action=detail&amp;id=${item.id}">Chi tiết</a>

                                <!-- Edit -->
                                <a class="activity-action-link"
                                   href="${pageContext.request.contextPath}/activity?action=edit&amp;id=${item.id}">Sửa</a>

                                <!-- Delete (POST) -->
                                <form action="${pageContext.request.contextPath}/activity"
                                      method="post"
                                      class="delete-form"
                                      style="margin:0;">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="id" value="${item.id}">
                                    <a href="#" role="button"
                                       class="activity-action-link activity-action-delete"
                                       onclick="this.closest('form').requestSubmit(); return false;">Xóa</a>
                                </form>

                                <!-- Approve + Reject -->
                                <c:if test="${item.approvalStatus == 'PENDING'}">

                                    <!-- Approve -->
                                    <form action="${pageContext.request.contextPath}/activity"
                                          method="post"
                                          class="approve-form"
                                          style="margin:0;">
                                        <input type="hidden" name="action" value="approve">
                                        <input type="hidden" name="id" value="${item.id}">
                                        <a href="#" role="button"
                                           class="activity-action-link"
                                           onclick="this.closest('form').requestSubmit(); return false;">Duyệt</a>
                                    </form>

                                    <!-- Reject -->
                                    <form action="${pageContext.request.contextPath}/activity"
                                          method="post"
                                          class="reject-form"
                                          style="margin:0;">
                                        <input type="hidden" name="action" value="reject">
                                        <input type="hidden" name="id" value="${item.id}">
                                        <input type="hidden" name="note" class="reject-note" value="">
                                        <a href="#" role="button"
                                           class="activity-action-link activity-action-delete"
                                           onclick="this.closest('form').requestSubmit(); return false;">Từ chối</a>
                                    </form>

                                </c:if>

                            </div>

                        </td>

                    </tr>

                </c:forEach>

                </tbody>

            </table>

        </div>


        <!-- Empty state (JS hiện khi search/filter không có kết quả) -->
        <div id="activityEmptyState"
             class="activity-empty-state"
             style="display: none; height: auto; padding: 45px 20px; text-align: center;">

            <h3>Không tìm thấy hoạt động</h3>

            <p>Không có hoạt động nào phù hợp với điều kiện tìm kiếm.</p>

        </div>

    </div>

</div>


<!-- ================= JAVASCRIPT ================= -->
<script src="${pageContext.request.contextPath}/assets/js/activity.js"></script>

</body>
</html>
