<%@ page contentType="text/html;charset=UTF-8" language="java" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Login</title>
</head>
<body>

<h1>Login</h1>

<c:if test="${param.msg == 'loggedOut'}">
    <div class="message success">You have been logged out.</div>
</c:if>

<c:if test="${not empty loginError}">
    <div class="message error"><c:out value="${loginError}"/></div>
</c:if>

<%-- Nothing is echoed back except the username; the password is never kept. --%>
<form method="post" action="${pageContext.request.contextPath}/login">
    <label>Username
        <input type="text" name="username" value="${fn:escapeXml(username)}"
               autocomplete="username" required>
    </label>
    <label>Password
        <input type="password" name="password" autocomplete="current-password" required>
    </label>
    <button type="submit">Login</button>
</form>

</body>
</html>
