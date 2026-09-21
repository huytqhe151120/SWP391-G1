<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><title>Partner staff form</title></head>
<body>
<c:set var="editing" value="${not empty staff}"/>
<h1><c:choose><c:when test="${editing}">Sửa partner staff</c:when><c:otherwise>Thêm partner staff</c:otherwise></c:choose></h1>
<form action="${pageContext.request.contextPath}/partner-staff" method="post">
    <input type="hidden" name="action" value="${editing ? 'update' : 'create'}">
    <c:if test="${editing}"><input type="hidden" name="id" value="${staff.id}"></c:if>
    <label>Họ tên <input name="fullName" required value="<c:out value='${staff.fullName}'/>" ></label><br>
    <label>Email <input type="email" name="email" required value="<c:out value='${staff.email}'/>" ></label><br>
    <label>Số điện thoại <input name="phone" required value="<c:out value='${staff.phone}'/>" ></label><br>
    <label>Chức vụ <input name="position" value="<c:out value='${staff.position}'/>" ></label><br>
    <label>Partner ID <input type="number" name="partnerId" min="1" required value="<c:out value='${staff.partnerId}'/>" ></label><br>
    <label>Active <select name="isActive"><option value="true">Có</option><option value="false">Không</option></select></label><br>
    <button type="submit">Lưu</button>
</form>
<a href="${pageContext.request.contextPath}/partner-staff">Hủy</a>
</body></html>