<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Student List</title>
</head>
<body>

<h1>Danh sách sinh viên</h1>

<table border="1" cellpadding="8">
    <thead>
    <tr>
        <th>ID</th>
        <th>Mã sinh viên</th>
        <th>Họ và tên</th>
        <th>Lớp</th>
        <th>Ngày sinh</th>
        <th>Giới tính</th>
        <th>Email</th>
        <th>Số điện thoại</th>
        <th>Trạng thái</th>
    </tr>
    </thead>

    <tbody>
    <c:forEach var="student" items="${students}">
        <tr>
            <td><c:out value="${student.id}"/></td>
            <td><c:out value="${student.code}"/></td>
            <td><c:out value="${student.name}"/></td>
            <td>
                <c:choose>
                    <c:when test="${not empty student.mainClassCode}">
                        <c:out value="${student.mainClassCode}"/>
                    </c:when>
                    <c:otherwise>--</c:otherwise>
                </c:choose>
            </td>
            <td><c:out value="${student.dob}"/></td>
            <td>
                <c:choose>
                    <c:when test="${student.gender == true}">Nam</c:when>
                    <c:when test="${student.gender == false}">Nữ</c:when>
                    <c:otherwise>--</c:otherwise>
                </c:choose>
            </td>
            <td><c:out value="${student.email}"/></td>
            <td><c:out value="${student.phoneNumber}"/></td>
            <td><c:out value="${student.status}"/></td>
        </tr>
    </c:forEach>
    </tbody>
</table>

</body>
</html>