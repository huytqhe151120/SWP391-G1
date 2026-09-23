<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<html>
<head>
    <title>Kênh Hỏi Đáp Q&A</title>
</head>
<body>
    <h2>Đặt câu hỏi mới (Submit Question)</h2>
    <c:if test="${not empty message}"><p style="color: green;">${message}</p></c:if>
    <c:if test="${not empty error}"><p style="color: red;">${error}</p></c:if>

    <form action="${pageContext.request.contextPath}/qa" method="post">
        <label>Tiêu đề:</label><br>
        <input type="text" name="title" required><br><br>
        <label>Nội dung câu hỏi:</label><br>
        <textarea name="content" rows="4" required></textarea><br><br>
        <input type="checkbox" name="isAnonymous" id="anon">
        <label for="anon">Gửi ẩn danh</label><br><br>
        <button type="submit">Gửi câu hỏi</button>
    </form>

    <hr>
    <h2>Danh sách câu hỏi đã được giải đáp</h2>
    <c:forEach var="q" items="${questions}">
        <div style="border: 1px solid #ccc; margin-bottom: 10px; padding: 10px;">
            <h4>${q.title}</h4>
            <p>${q.content}</p>
            <small>Đăng bởi: ${q.anonymous ? "Ẩn danh" : "Sinh viên"} - ${q.createdAt}</small>
        </div>
    </c:forEach>
</body>
</html>