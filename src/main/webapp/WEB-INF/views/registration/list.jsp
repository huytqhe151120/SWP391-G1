<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Quản lý form đăng ký</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/base.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/registration.css"></head><body>
<main class="registration-page">
    <div class="page-heading">
        <div><h1>Quản lý form đăng ký hoạt động</h1>
            <p>Tạo và quản lý form để sinh viên đăng ký tham gia hoạt động.</p></div>
        <a class="btn primary" href="${pageContext.request.contextPath}/registration-forms?action=create">+ Tạo form</a>
    </div>
    <c:if test="${param.msg eq 'saved'}"><p class="notice success">Đã lưu form đăng ký.</p></c:if><c:if
        test="${param.msg eq 'deleted'}"><p class="notice success">Đã xóa form.</p></c:if><c:if
        test="${param.msg eq 'deleteError'}"><p class="notice error">Không thể xóa form đã có sinh viên đăng
    ký.</p></c:if>
    <div class="table-wrap">
        <table>
            <thead>
            <tr>
                <th>Form</th>
                <th>Hoạt động</th>
                <th>Thời gian đăng ký</th>
                <th>Trạng thái</th>
                <th>Số đăng ký</th>
                <th>Thao tác</th>
            </tr>
            </thead>
            <tbody><c:forEach items="${forms}" var="f">
                <tr>
                    <td><strong><c:out value="${f.title}"/></strong>
                        <div class="muted"><c:out value="${f.description}"/></div>
                    </td>
                    <td><c:out value="${f.activityName}"/></td>
                    <td><c:out value="${f.openAt}"/> – <c:out value="${f.closeAt}"/></td>
                    <td><span class="status"><c:out value="${f.status}"/></span></td>
                    <td><c:out value="${f.registrationCount}"/></td>
                    <td class="actions"><a
                            href="${pageContext.request.contextPath}/registration-forms?action=edit&id=${f.id}">Sửa</a>
                        <form method="post" action="${pageContext.request.contextPath}/registration-forms"
                              onsubmit="return confirm('Bạn chắc chắn muốn xóa form này?')"><input type="hidden"
                                                                                                   name="action"
                                                                                                   value="delete"><input
                                type="hidden" name="id" value="${f.id}">
                            <button type="submit">Xóa</button>
                        </form>
                    </td>
                </tr>
            </c:forEach><c:if test="${empty forms}">
                <tr>
                    <td colspan="6" class="empty">Chưa có form đăng ký nào.</td>
                </tr>
            </c:if></tbody>
        </table>
    </div>
    <a class="back-link" href="${pageContext.request.contextPath}/home">← Trang chủ</a></main>
</body></html>
