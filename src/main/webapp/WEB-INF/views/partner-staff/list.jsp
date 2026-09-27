<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><title>Partner staff</title></head>
<body>
<h1>Partner staff</h1>
<p><a href="${pageContext.request.contextPath}/partner-staff?action=create">Thêm nhân viên</a></p>
<table border="1" cellpadding="8">
    <thead><tr><th>ID</th><th>Họ tên</th><th>Email</th><th>Chức vụ</th><th>Partner ID</th><th>Active</th><th>Thao tác</th></tr></thead>
    <tbody>
    <c:forEach var="staff" items="${staffList}">
        <tr>
            <td><c:out value="${staff.id}"/></td><td><c:out value="${staff.fullName}"/></td>
            <td><c:out value="${staff.email}"/></td><td><c:out value="${staff.position}"/></td>
            <td><c:out value="${staff.partnerId}"/></td><td><c:out value="${staff.isActive}"/></td>
            <td>
                <a href="${pageContext.request.contextPath}/partner-staff?action=detail&id=${staff.id}">Chi tiết</a>
                <a href="${pageContext.request.contextPath}/partner-staff?action=edit&id=${staff.id}">Sửa</a>
                <form action="${pageContext.request.contextPath}/partner-staff" method="post" style="display:inline">
                    <input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="${staff.id}">
                    <button type="submit">Xóa</button>
                </form>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</body></html>