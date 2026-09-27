<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Activity List</title>

</head>
<body>

<h1>Danh sách hoạt động ngoại khóa</h1>

<p>
    <a href="${pageContext.request.contextPath}/activities?action=create">
        Thêm hoạt động
    </a>
</p>

<table border="1" cellpadding="8">
    <thead>
    <tr>
        <th>ID</th>
        <th>Mã hoạt động</th>
        <th>Tên hoạt động</th>
        <th>Activity Type ID</th>
        <th>Activity Status</th>
        <th>Approval Status</th>
        <th>Thao tác</th>
    </tr>
    </thead>

    <tbody>
    <c:forEach var="activity" items="${activities}">
        <tr>
            <td><c:out value="${activity.id}"/></td>
            <td><c:out value="${activity.code}"/></td>
            <td><c:out value="${activity.name}"/></td>
            <td><c:out value="${activity.activityType}"/></td>
            <td><c:out value="${activity.activityStatus}"/></td>
            <td><c:out value="${activity.approvalStatus}"/></td>

            <td>
                <a href="${pageContext.request.contextPath}/activities?action=detail&id=${activity.id}">
                    Chi tiết
                </a>

                <a href="${pageContext.request.contextPath}/activities?action=edit&id=${activity.id}">
                    Sửa
                </a>

                <form action="${pageContext.request.contextPath}/activities"
                      method="post"
                      style="display:inline">
                    <input type="hidden" name="action" value="delete">
                    <input type="hidden" name="id" value="${activity.id}">
                    <button type="submit">Xóa</button>
                </form>

                <c:if test="${activity.approvalStatus == 'PENDING'}">
                    <form action="${pageContext.request.contextPath}/activities"
                          method="post"
                          style="display:inline">
                        <input type="hidden" name="action" value="approve">
                        <input type="hidden" name="id" value="${activity.id}">
                        <button type="submit">Duyệt</button>
                    </form>

                    <form action="${pageContext.request.contextPath}/activities"
                          method="post"
                          style="display:inline">
                        <input type="hidden" name="action" value="reject">
                        <input type="hidden" name="id" value="${activity.id}">
                        <input type="hidden" name="note" value="Activity rejected by staff">
                        <button type="submit">Từ chối</button>
                    </form>
                </c:if>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</body>
</html>
