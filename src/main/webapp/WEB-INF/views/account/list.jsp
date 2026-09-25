<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Accounts</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/account.css">
</head>
<body>
<nav class="top-nav">
    <a href="${pageContext.request.contextPath}/home">Home</a>
    <a href="${pageContext.request.contextPath}/accounts">Accounts</a>
</nav>

<h1>Account Management</h1>

<c:if test="${not empty generalError}">
    <div class="message error"><c:out value="${generalError}"/></div>
</c:if>
<c:if test="${not empty param.error}">
    <div class="message error">
        <c:choose>
            <c:when test="${param.error == 'notFound'}">Account not found.</c:when>
            <c:when test="${param.error == 'invalidStatus'}">Invalid status. Allowed values: ACTIVE, INACTIVE, BLOCKED.</c:when>
            <c:otherwise>Operation failed. Please try again.</c:otherwise>
        </c:choose>
    </div>
</c:if>
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

<div class="actions">
    <a class="button" href="${pageContext.request.contextPath}/accounts/create">Create New Account</a>
</div>

<div class="filters">
    <form method="get" action="${pageContext.request.contextPath}/accounts">
        <label>Search
            <input type="text" name="search" value="${fn:escapeXml(search)}" placeholder="Username">
        </label>
        <label>Type
            <select name="type">
                <option value="">All</option>
                <c:forEach items="${accountTypes}" var="t">
                    <option value="${fn:escapeXml(t)}" ${t == filterType ? 'selected' : ''}><c:out value="${t}"/></option>
                </c:forEach>
            </select>
        </label>
        <label>Role
            <select name="role">
                <option value="">All</option>
                <c:forEach items="${accountRoles}" var="r">
                    <option value="${fn:escapeXml(r)}" ${r == filterRole ? 'selected' : ''}><c:out value="${r}"/></option>
                </c:forEach>
            </select>
        </label>
        <label>Status
            <select name="status">
                <option value="">All</option>
                <c:forEach items="${accountStatuses}" var="s">
                    <option value="${fn:escapeXml(s)}" ${s == filterStatus ? 'selected' : ''}><c:out value="${s}"/></option>
                </c:forEach>
            </select>
        </label>
        <button type="submit">Filter</button>
        <a class="button-secondary" href="${pageContext.request.contextPath}/accounts">Reset</a>
    </form>
</div>

<c:choose>
    <c:when test="${empty accounts}">
        <p class="empty-state">No accounts found matching your criteria.</p>
    </c:when>
    <c:otherwise>
        <table class="account-table">
            <thead>
            <tr>
                <th>ID</th>
                <th>Username</th>
                <th>Type</th>
                <th>Role</th>
                <th>Status</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${accounts}" var="a">
                <tr>
                    <td>${a.id}</td>
                    <td><c:out value="${a.username}"/></td>
                    <td><c:out value="${a.type}"/></td>
                    <td><c:out value="${a.role}"/></td>
                    <td><span class="status-badge status-${a.status}"><c:out value="${a.status}"/></span></td>
                    <td class="row-actions">
                        <a href="${pageContext.request.contextPath}/accounts/${a.id}">View</a>
                        <a href="${pageContext.request.contextPath}/accounts/${a.id}/edit">Edit</a>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

</body>
</html>