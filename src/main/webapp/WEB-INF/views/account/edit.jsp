<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Edit Account</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/account.css">
</head>
<body>
<nav class="top-nav">
    <a href="${pageContext.request.contextPath}/home">Home</a>
    <a href="${pageContext.request.contextPath}/accounts">Accounts</a>
</nav>

<h1>Edit Account #${account.id}</h1>

<c:if test="${not empty generalError}">
    <div class="message error"><c:out value="${generalError}"/></div>
</c:if>
<c:if test="${not empty fieldErrors['id']}">
    <div class="message error"><c:out value="${fieldErrors['id']}"/></div>
</c:if>

<form id="accountForm" class="account-form" method="post" action="${pageContext.request.contextPath}/accounts/${account.id}/edit">
    <div class="form-group">
        <label for="username">Username</label>
        <input type="text" id="username" name="username" value="${fn:escapeXml(account.username)}" maxlength="100" required>
        <c:if test="${not empty fieldErrors['username']}">
            <span class="field-error"><c:out value="${fieldErrors['username']}"/></span>
        </c:if>
    </div>
    <div class="form-group">
        <label for="password">Password</label>
        <input type="password" id="password" name="password" maxlength="255" placeholder="Leave blank to keep current password">
        <span class="hint">Leave blank to keep the current password.</span>
        <c:if test="${not empty fieldErrors['password']}">
            <span class="field-error"><c:out value="${fieldErrors['password']}"/></span>
        </c:if>
    </div>
    <div class="form-group">
        <label for="type">Type</label>
        <select id="type" name="type">
            <option value="">-- Select Type --</option>
            <c:forEach items="${accountTypes}" var="t">
                <option value="${fn:escapeXml(t)}" ${t == account.type ? 'selected' : ''}><c:out value="${t}"/></option>
            </c:forEach>
        </select>
        <c:if test="${not empty fieldErrors['type']}">
            <span class="field-error"><c:out value="${fieldErrors['type']}"/></span>
        </c:if>
    </div>
    <div class="form-group">
        <label for="role">Role</label>
        <select id="role" name="role">
            <option value="">-- Select Role --</option>
            <c:forEach items="${rolesByType}" var="entry">
                <c:forEach items="${entry.value}" var="r">
                    <option value="${fn:escapeXml(r)}" data-type="${fn:escapeXml(entry.key)}" ${r == account.role ? 'selected' : ''}><c:out value="${r}"/></option>
                </c:forEach>
            </c:forEach>
        </select>
        <span class="hint">Available roles are filtered by the selected type.</span>
        <c:if test="${not empty fieldErrors['role']}">
            <span class="field-error"><c:out value="${fieldErrors['role']}"/></span>
        </c:if>
    </div>
    <div class="form-group">
        <label for="status">Status</label>
        <select id="status" name="status">
            <option value="">-- Select Status --</option>
            <c:forEach items="${accountStatuses}" var="s">
                <option value="${fn:escapeXml(s)}" ${s == account.status ? 'selected' : ''}><c:out value="${s}"/></option>
            </c:forEach>
        </select>
        <c:if test="${not empty fieldErrors['status']}">
            <span class="field-error"><c:out value="${fieldErrors['status']}"/></span>
        </c:if>
    </div>
    <div class="form-actions">
        <button type="submit">Save Changes</button>
        <a class="button-secondary" href="${pageContext.request.contextPath}/accounts/${account.id}">Cancel</a>
    </div>
</form>

<script src="${pageContext.request.contextPath}/assets/js/account-form.js"></script>
</body>
</html>