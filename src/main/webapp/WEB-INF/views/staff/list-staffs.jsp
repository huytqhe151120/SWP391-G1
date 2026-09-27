<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Staff List</title>
</head>
<body>

<h1>Danh sách cán bộ / nhân viên</h1>

<table border="1" cellpadding="8">
    <thead>
    <tr>
        <th>ID</th>
        <th>Mã cán bộ</th>
        <th>Họ và tên</th>
        <th>Ngày sinh</th>
        <th>Giới tính</th>
        <th>Phòng ban / Chức vụ</th>
        <th>Trạng thái</th>
    </tr>
    </thead>

    <tbody>
    <c:forEach var="staff" items="${staffList}">
        <tr>
            <td><c:out value="${staff.id}"/></td>
            <td><c:out value="${staff.code}"/></td>
            <td><c:out value="${staff.name}"/></td>
            <td><c:out value="${staff.dob}"/></td>
            <td>
                <c:choose>
                    <c:when test="${staff.gender == true}">Nam</c:when>
                    <c:when test="${staff.gender == false}">Nữ</c:when>
                    <c:otherwise>--</c:otherwise>
                </c:choose>
            </td>
            <td>
                <c:choose>
                    <c:when test="${not empty staff.departmentInfo}">
                        <c:out value="${staff.departmentInfo}"/>
                    </c:when>
                    <c:otherwise>--</c:otherwise>
                </c:choose>
            </td>
            <td><c:out value="${staff.status}"/></td>
        </tr>
    </c:forEach>
    </tbody>
</table>

</body>
</html>
