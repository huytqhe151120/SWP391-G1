<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><title>Partner staff detail</title></head>
<body>
<h1>Chi tiết partner staff</h1>
<dl>
    <dt>ID</dt><dd><c:out value="${staff.id}"/></dd><dt>Họ tên</dt><dd><c:out value="${staff.fullName}"/></dd>
    <dt>Email</dt><dd><c:out value="${staff.email}"/></dd><dt>Số điện thoại</dt><dd><c:out value="${staff.phone}"/></dd>
    <dt>Chức vụ</dt><dd><c:out value="${staff.position}"/></dd><dt>Partner ID</dt><dd><c:out value="${staff.partnerId}"/></dd>
    <dt>Active</dt><dd><c:out value="${staff.isActive}"/></dd><dt>Ngày tạo</dt><dd><c:out value="${staff.createdAt}"/></dd>
</dl>
<a href="${pageContext.request.contextPath}/partner-staff">Quay lại</a>
</body></html>