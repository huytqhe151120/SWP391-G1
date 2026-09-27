<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Partners</title>
</head>
<body>
<h1>Partners</h1>
<p><a href="${pageContext.request.contextPath}/partners?action=create">Thêm partner</a></p>
<table border="1" cellpadding="8">
    <thead><tr><th>ID</th><th>Tên</th><th>Email</th><th>Số điện thoại</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
    <tbody>
    <c:forEach var="partner" items="${partners}">
        <tr>
            <td><c:out value="${partner.id}"/></td>
            <td><c:out value="${partner.name}"/></td>
            <td><c:out value="${partner.email}"/></td>
            <td><c:out value="${partner.phone}"/></td>
            <td><c:out value="${partner.status}"/></td>
            <td>
                <a href="${pageContext.request.contextPath}/partners?action=detail&id=${partner.id}">Chi tiết</a>
                <a href="${pageContext.request.contextPath}/partners?action=edit&id=${partner.id}">Sửa</a>
                <form action="${pageContext.request.contextPath}/partners" method="post" style="display:inline">
                    <input type="hidden" name="action" value="delete">
                    <input type="hidden" name="id" value="${partner.id}">
                    <button type="submit">Xóa</button>
                </form>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</body>
</html>