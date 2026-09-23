<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<html>
<head>
    <title>Quản lý Q&A - Ban tổ chức</title>
</head>
<body>
    <h2>Danh sách câu hỏi đang chờ duyệt (Pending Questions)</h2>
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
                <td>${pq.title}</td>
                <td>${pq.content}</td>
                <td>${pq.anonymous ? "Ẩn danh" : "Công khai"}</td>
                <td>${pq.createdAt}</td>
            </tr>
        </c:forEach>
    </table>
</body>
</html>