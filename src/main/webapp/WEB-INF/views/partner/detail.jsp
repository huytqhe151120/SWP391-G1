<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><title>Partner detail</title></head>
<body>
<h1>Chi tiết partner</h1>
<dl>
    <dt>ID</dt><dd><c:out value="${partner.id}"/></dd>
    <dt>Tên</dt><dd><c:out value="${partner.name}"/></dd>
    <dt>Email</dt><dd><c:out value="${partner.email}"/></dd>
    <dt>Số điện thoại</dt><dd><c:out value="${partner.phone}"/></dd>
    <dt>Địa chỉ</dt><dd><c:out value="${partner.address}"/></dd>
    <dt>Trạng thái</dt><dd><c:out value="${partner.status}"/></dd>
    <dt>Ngày tạo</dt><dd><c:out value="${partner.createdAt}"/></dd>
</dl>
<a href="${pageContext.request.contextPath}/partners">Quay lại</a>
</body></html>