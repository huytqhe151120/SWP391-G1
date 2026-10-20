<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Gửi đăng ký</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/base.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/registration.css"></head><body><main class="registration-page narrow"><h1><c:out
        value="${form.title}"/></h1><h2><c:out value="${form.activityName}"/></h2><p><c:out
        value="${form.description}"/></p><c:if test="${not empty error}"><p class="notice error"><c:out
        value="${error}"/></p></c:if>
<form method="post" action="${pageContext.request.contextPath}/registration-forms" class="registration-form"><input
        type="hidden" name="action" value="register"><input type="hidden" name="id" value="${form.id}"><label>Ghi chú
    thêm (không bắt buộc)<textarea name="note" rows="5" maxlength="2000"
                                   placeholder="Thông tin bạn muốn ban tổ chức biết..."></textarea></label>
    <div class="form-actions">
        <button class="btn primary" type="submit">Gửi đăng ký</button>
        <a class="btn" href="${pageContext.request.contextPath}/registration-forms?action=student">Quay lại</a></div>
</form>
</main></body></html>
