<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Account Detail</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/account.css">
</head>
<body>
<nav class="top-nav">
    <a href="${pageContext.request.contextPath}/home">Home</a>
    <a href="${pageContext.request.contextPath}/accounts">Accounts</a>
</nav>

<h1>Account Detail</h1>

<c:if test="${not empty param.msg}">
    <div class="message success">
        <c:choose>
            <c:when test="${param.msg == 'created'}">Account created successfully.</c:when>
            <c:when test="${param.msg == 'updated'}">Account updated successfully.</c:when>
            <c:when test="${param.msg == 'statusChanged'}">Account status changed successfully.</c:when>
            <c:otherwise>Operation completed successfully.</c:otherwise>
        </c:choose>
    </div>
</c:if>
<c:if test="${param.error == 'invalidStatus'}">
    <div class="message error">Invalid status. Allowed values: ACTIVE, INACTIVE, BLOCKED.</div>
</c:if>

<table class="account-table detail">
    <tbody>
    <tr><th>ID</th><td>${account.id}</td></tr>
    <tr><th>Username</th><td><c:out value="${account.username}"/></td></tr>
    <tr><th>Type</th><td><c:out value="${account.type}"/></td></tr>
    <tr><th>Role</th><td><c:out value="${account.role}"/></td></tr>
    <tr><th>Status</th><td><span class="status-badge status-${account.status}"><c:out value="${account.status}"/></span></td></tr>
    </tbody>
</table>

<h2>Linked Profile</h2>
<c:choose>
    <c:when test="${empty profiles}">
        <p class="empty-state">This account is not linked to any profile.</p>
    </c:when>
    <c:otherwise>
        <c:forEach items="${profiles}" var="p">
            <table class="account-table detail">
                <tbody>
                <tr><th>Profile type</th><td><c:out value="${p.profileType}"/></td></tr>
                <tr><th>Code</th><td><c:out value="${p.code}"/></td></tr>
                <tr><th>Name</th><td><c:out value="${p.name}"/></td></tr>
                <tr><th>Status</th><td><c:out value="${p.status}"/></td></tr>
                <c:if test="${not empty p.email}">
                    <tr><th>Email</th><td><c:out value="${p.email}"/></td></tr>
                </c:if>
                <c:if test="${not empty p.position}">
                    <tr><th>Position</th><td><c:out value="${p.position}"/></td></tr>
                </c:if>
                </tbody>
            </table>
        </c:forEach>
    </c:otherwise>
</c:choose>

<h2>Change Status</h2>
<form class="account-form inline-form" method="post" action="${pageContext.request.contextPath}/accounts/${account.id}/status">
    <label for="status">Status</label>
    <select id="status" name="status">
        <c:forEach items="${accountStatuses}" var="s">
            <option value="${fn:escapeXml(s)}" ${s == account.status ? 'selected' : ''}><c:out value="${s}"/></option>
        </c:forEach>
    </select>
    <button type="submit">Change Status</button>
</form>

<div class="form-actions">
    <a class="button" href="${pageContext.request.contextPath}/accounts/${account.id}/edit">Edit Account</a>
    <a class="button-secondary" href="${pageContext.request.contextPath}/accounts">Back to List</a>
</div>

</body>
</html>