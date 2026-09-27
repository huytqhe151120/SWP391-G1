<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><title>Partner form</title></head>
<body>
<c:set var="editing" value="${not empty partner}"/>
<h1><c:choose><c:when test="${editing}">Sửa partner</c:when><c:otherwise>Thêm partner</c:otherwise></c:choose></h1>
<form action="${pageContext.request.contextPath}/partners" method="post">
    <input type="hidden" name="action" value="${editing ? 'update' : 'create'}">
    <c:if test="${editing}"><input type="hidden" name="id" value="${partner.id}"></c:if>
    <label>Tên <input name="name" required value="<c:out value='${partner.name}'/>" ></label><br>
    <label>Email <input type="email" name="email" required value="<c:out value='${partner.email}'/>" ></label><br>
    <label>Số điện thoại <input name="phone" required value="<c:out value='${partner.phone}'/>" ></label><br>
    <label>Địa chỉ <input name="address" value="<c:out value='${partner.address}'/>" ></label><br>
    <label>Trạng thái <input name="status" required value="<c:out value='${partner.status}'/>" ></label><br>
    <button type="submit">Lưu</button>
</form>
<a href="${pageContext.request.contextPath}/partners">Hủy</a>
</body></html>