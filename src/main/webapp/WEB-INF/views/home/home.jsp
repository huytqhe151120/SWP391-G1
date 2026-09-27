<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>SWP391-G1</title>
</head>
<body>
    <h1>SWP391-G1</h1>
    <p>Servlet + JSP is working</p>
    <%-- UI hint only: AccountServlet still enforces the ADMIN rule server-side. --%>
    <c:if test="${canManageAccounts}">
        <p><a href="${pageContext.request.contextPath}/accounts">Account Management</a></p>
    </c:if>
    <form method="post" action="${pageContext.request.contextPath}/logout">
        <button type="submit">Logout</button>
    </form>
</body>
</html>