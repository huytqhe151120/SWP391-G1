<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Form đăng ký</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/base.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/registration.css"></head><body><main class="registration-page narrow"><h1><c:choose><c:when
        test="${form.id > 0}">Chỉnh sửa form đăng ký</c:when><c:otherwise>Tạo form đăng ký</c:otherwise></c:choose></h1><p>Form chỉ hiển thị cho sinh viên khi trạng thái là OPEN, thời gian hợp lệ và hoạt động đã được duyệt.</p><c:if
        test="${not empty error}"><p class="notice error"><c:out value="${error}"/></p></c:if>
<form method="post" action="${pageContext.request.contextPath}/registration-forms" class="registration-form"><input
        type="hidden" name="action" value="save"><input type="hidden" name="id" value="${form.id}">
    <label>Hoạt động <select name="activityId" required>
        <option value="">-- Chọn hoạt động --</option>
        <c:forEach items="${activities}" var="a">
            <option value="${a.id}" <c:if test="${form.activityId == a.id}">selected</c:if>><c:out value="${a.code}"/> -
                <c:out value="${a.name}"/> (<c:out value="${a.approvalStatus}"/>)
            </option>
        </c:forEach></select></label>
    <label>Tiêu đề form <input name="title" maxlength="200" required value="<c:out value='${form.title}'/>"
                               placeholder="Ví dụ: Form đăng ký tham gia workshop"></label>
    <label>Mô tả / hướng dẫn <textarea name="description" rows="5" maxlength="4000"><c:out
            value="${form.description}"/></textarea></label>
    <label>Thời gian mở <input type="datetime-local" name="openAt" value="${form.openAt}"></label><label>Thời gian đóng
        <input type="datetime-local" name="closeAt" value="${form.closeAt}"></label>
    <label>Trạng thái <select name="status">
        <option value="DRAFT" <c:if test="${form.status ne 'OPEN'}">selected</c:if>>Bản nháp</option>
        <option value="OPEN" <c:if test="${form.status eq 'OPEN'}">selected</c:if>>Mở đăng ký</option>
    </select></label>
    <c:if test="${form.registrationCount > 0}"><p class="notice">Form đã có đăng ký; không thể sửa thông tin. Bạn có thể
        quay lại danh sách.</p></c:if>
    <div class="form-actions">
        <button class="btn primary" type="submit">Lưu form</button>
        <a class="btn" href="${pageContext.request.contextPath}/registration-forms">Hủy</a></div>
</form>
</main></body></html>
