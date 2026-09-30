<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý Q&A - Ban tổ chức</title>
</head>
<body>
    <h2>Danh sách câu hỏi đang chờ duyệt (Pending Questions)</h2>
    <c:if test="${not empty databaseError}">
        <p role="alert">${databaseError}</p>
    </c:if>
    <table border="1" cellpadding="8">
        <tr>
            <th>ID</th>
            <th>Tiêu đề</th>
            <th>Nội dung</th>
            <th>Chế độ</th>
            <th>Thời gian gửi</th>
        </tr>
        <c:forEach var="pq" items="${pendingQuestions}">
            <tr>
                <td>${pq.id}</td>
                <td><c:out value="${pq.title}"/></td>
                <td><c:out value="${pq.content}"/></td>
                <td>${pq.anonymous ? "Ẩn danh" : "Công khai"}</td>
                <td><c:out value="${pq.createdAt}"/></td>
            </tr>
        </c:forEach>
    </table>
</body>
</html>