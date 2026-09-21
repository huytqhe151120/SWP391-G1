<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><title>Activities</title></head>
<body>
<h1>Extracurricular activities</h1>
<p><a href="${pageContext.request.contextPath}/activities?action=create">Thêm activity</a></p>
<table border="1" cellpadding="8">
    <thead><tr><th>ID</th><th>Code</th><th>Tên</th><th>Loại</th><th>Partner ID</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
    <tbody>
    <c:forEach var="activity" items="${activities}">
        <tr>
            <td><c:out value="${activity.id}"/></td>
            <td><c:out value="${activity.code}"/></td>
            <td><c:out value="${activity.name}"/></td>
            <td><c:out value="${activity.activityType}"/></td>
            <td><c:out value="${activity.partnerId}"/></td>
            <td><c:out value="${activity.activityStatus}"/></td>
            <td>
                <a href="${pageContext.request.contextPath}/activities?action=detail&id=${activity.id}">Chi tiết</a>
                <a href="${pageContext.request.contextPath}/activities?action=edit&id=${activity.id}">Sửa</a>
                <form action="${pageContext.request.contextPath}/activities" method="post" style="display:inline">
                    <input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="${activity.id}">
                    <button type="submit">Xóa</button>
                </form>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</body></html>