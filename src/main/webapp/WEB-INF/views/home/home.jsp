<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>
    <main class="page">
        <section class="card">
            <h1>SWP391-G1</h1>
            <p class="subtitle">Hệ thống quản lý hoạt động ngoại khóa</p>
            <p><a class="button button-primary" href="${pageContext.request.contextPath}/activities">Quản lý hoạt động</a></p>
            <p><a class="button" href="${pageContext.request.contextPath}/partner-companies">Quản lý công ty đối tác</a></p>
            <p><a class="button" href="${pageContext.request.contextPath}/partner-staffs">Quản lý nhân viên đối tác</a></p>

            <%-- ADMIN và STAFF: hộp thư hỗ trợ 1-1 với sinh viên --%>
            <c:if test="${canUseSupportInbox}">
                <p><a class="button" href="${pageContext.request.contextPath}/organizer-inbox">Hộp thư hỗ trợ</a></p>
            </c:if>

            <%-- ADMIN và STAFF quản lý Q&A --%>
            <c:if test="${canManageQuestions}">
                <p><a class="button" href="${pageContext.request.contextPath}/qa-management">Quản lý câu hỏi Q&amp;A</a></p>
            </c:if>

            <%-- STUDENT: tạo và xem câu hỏi --%>
            <c:if test="${isStudent}">
                <p><a class="button" href="${pageContext.request.contextPath}/student-qa">Hỏi &amp; Đáp</a></p>
                <p><a class="button" href="${pageContext.request.contextPath}/student-inbox">Hộp thư của tôi</a></p>
            </c:if>

            <%-- UI hint only: AccountServlet still enforces the ADMIN rule server-side. --%>
            <c:if test="${canManageAccounts}">
                <p><a class="button" href="${pageContext.request.contextPath}/accounts">Quản lý tài khoản</a></p>
            </c:if>
            <form method="post" action="${pageContext.request.contextPath}/logout">
                <button class="button" type="submit">Đăng xuất</button>
            </form>
        </section>
    </main>
</body>
</html>
