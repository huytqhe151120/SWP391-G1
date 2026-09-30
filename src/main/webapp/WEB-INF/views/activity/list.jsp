<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Hoạt động ngoại khóa | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>
<main class="page activity-list-page">
    <header class="activity-hero">
        <div class="activity-hero-copy">
            <span class="eyebrow">SWP391-G1 <span aria-hidden="true">/</span> QUẢN LÝ HOẠT ĐỘNG NGOẠI KHÓA</span>
            <h1>Hoạt động ngoại khóa</h1>
            <p>Theo dõi và quản lý các hoạt động ngoại khóa của trường tại một nơi.</p>
        </div>
        <div class="account-hero-actions">
            <a class="button account-home-button" href="${pageContext.request.contextPath}/home">Trang chủ</a>
            <a class="button button-primary activity-create-button" href="${pageContext.request.contextPath}/activities?action=create">
                <span class="plus-icon" aria-hidden="true">+</span>
                Tạo hoạt động
            </a>
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
            <span class="activity-count">
                <c:choose>
                    <c:when test="${empty activities}">0 hoạt động</c:when>
                    <c:otherwise><c:out value="${activities.size()}"/> hoạt động</c:otherwise>
                </c:choose>
            </span>
        </div>
        <div class="table-wrap activity-table-wrap">
            <table>
                <thead>
                <tr>
                    <th scope="col">Mã hoạt động</th>
                    <th scope="col">Tên hoạt động</th>
                    <th scope="col">Học kỳ</th>
                    <th scope="col">Loại hoạt động</th>
                    <th scope="col">Đơn vị phụ trách</th>
                    <th scope="col">Thời gian</th>
                    <th scope="col">Trạng thái hoạt động</th>
                    <th scope="col">Trạng thái duyệt</th>
                    <th scope="col">Thao tác</th>
                </tr>
                </thead>
                <tbody>
                <c:choose>
                    <c:when test="${empty activities}">
                    <tr>
                        <td class="empty-state activity-empty-state" colspan="9">
                            <span class="empty-state-mark" aria-hidden="true">+</span>
                            <strong>Chưa có hoạt động nào</strong>
                            <span>Tạo hoạt động mới để bắt đầu quản lý danh sách.</span>
                            <a class="button button-primary" href="${pageContext.request.contextPath}/activities?action=create">Tạo hoạt động đầu tiên</a>
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
                                            <c:out value="${activity.startTime}"/>
                                            <c:if test="${not empty activity.endTime}"><span>đến</span><c:out value="${activity.endTime}"/></c:if>
                                        </c:when>
                                        <c:otherwise>Chưa xác định</c:otherwise>
                                    </c:choose>
                                </td>
                                <td><span class="badge activity-status"><c:out value="${activity.activityStatus}" default="Chưa xác định"/></span></td>
                                <td><span class="badge activity-status"><c:out value="${activity.approvalStatus}" default="Chưa xác định"/></span></td>
                                <td>
                                    <div class="actions activity-actions">
                                        <a class="activity-action-link" href="${pageContext.request.contextPath}/activities?action=detail&amp;id=${activity.id}">Chi tiết</a>
                                        <a class="activity-action-link" href="${pageContext.request.contextPath}/activities?action=edit&amp;id=${activity.id}">Sửa</a>
                                        <a class="activity-action-link activity-action-delete"
                                           href="${pageContext.request.contextPath}/activities?action=delete&amp;id=${activity.id}"
                                           onclick="return confirm('Bạn có chắc muốn xóa hoạt động này?');">Xóa</a>
                                        <c:if test="${activity.approvalStatus == 'DRAFT' || activity.approvalStatus == 'REJECTED'}">
                                            <form action="${pageContext.request.contextPath}/activity/approval" method="post">
                                                <input type="hidden" name="id" value="${activity.id}">
                                                <button type="submit" name="action" value="submit" class="activity-action-link">Gửi duyệt</button>
                                            </form>
                                        </c:if>
                                        <c:if test="${activity.approvalStatus == 'PENDING'}">
                                            <form action="${pageContext.request.contextPath}/activity/approval" method="post">
                                                <input type="hidden" name="id" value="${activity.id}">
                                                <button type="submit" name="action" value="approve" class="activity-action-link">Duyệt</button>
                                                <button type="submit" name="action" value="reject" class="activity-action-link activity-action-delete">Từ chối</button>
                                            </form>
                                        </c:if>
                                        <c:if test="${activity.approvalStatus == 'APPROVED' && activity.activityStatus == 'UPCOMING'}">
                                            <form action="${pageContext.request.contextPath}/activity/approval" method="post">
                                                <input type="hidden" name="id" value="${activity.id}">
                                                <button type="submit" name="action" value="revokeApproval"
                                                        class="activity-action-link activity-action-delete"
                                                        onclick="return confirm('Hủy duyệt và đưa hoạt động về trạng thái chờ duyệt?');">Hủy duyệt</button>
                                            </form>
                                        </c:if>
                                        <c:if test="${activity.approvalStatus == 'REJECTED'}">
                                            <form action="${pageContext.request.contextPath}/activity/approval" method="post">
                                                <input type="hidden" name="id" value="${activity.id}">
                                                <button type="submit" name="action" value="revokeRejection"
                                                        class="activity-action-link"
                                                        onclick="return confirm('Hủy từ chối và đưa hoạt động về trạng thái chờ duyệt?');">Hủy từ chối</button>
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
    </section>
</main>
</body>
</html>
