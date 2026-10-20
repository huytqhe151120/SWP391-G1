<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Đăng ký hoạt động</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/base.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/registration.css"></head><body><main class="registration-page"><h1>Hoạt động đang mở đăng ký</h1><c:if
        test="${param.msg eq 'registered'}"><p class="notice success">Gửi đăng ký thành công.</p></c:if>
<div class="form-cards"><c:forEach items="${forms}" var="f">
    <article class="form-card"><span class="status open">Đang mở</span>
        <h2><c:out value="${f.title}"/></h2>
        <h3><c:out value="${f.activityName}"/></h3>
        <p><c:out value="${f.description}"/></p>
        <p class="muted">Hạn đăng ký: <c:out value="${f.closeAt}"/></p><a class="btn primary"
                                                                          href="${pageContext.request.contextPath}/registration-forms?action=register&id=${f.id}">Xem
            và đăng ký</a></article>
</c:forEach><c:if test="${empty forms}"><p>Hiện chưa có hoạt động nào mở đăng ký.</p></c:if></div>
<a class="back-link" href="${pageContext.request.contextPath}/home">← Trang chủ</a></main></body></html>
