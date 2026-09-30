<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Nhân viên đối tác | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>
<main class="page activity-list-page">
    <header class="activity-hero">
        <div class="activity-hero-copy">
            <span class="eyebrow">SWP391-G1 <span aria-hidden="true">/</span> QUẢN LÝ ĐỐI TÁC</span>
            <h1>Nhân viên đối tác</h1>
            <p>Quản lý thông tin và công ty của các nhân viên đối tác.</p>
        </div>
        <a class="button activity-create-button" href="${pageContext.request.contextPath}/partner-staffs?action=create">
            <span class="plus-icon" aria-hidden="true">+</span>
            Thêm nhân viên
        </a>
    </header>

    <c:if test="${not empty sessionScope.successMessage}">
        <div class="alert alert-success activity-alert"><c:out value="${sessionScope.successMessage}"/></div>
        <c:remove var="successMessage" scope="session"/>
    </c:if>
    <c:if test="${not empty sessionScope.errorMessage}">
        <div class="alert alert-error activity-alert"><c:out value="${sessionScope.errorMessage}"/></div>
        <c:remove var="errorMessage" scope="session"/>
    </c:if>

    <section class="card activity-table-card" aria-labelledby="staff-table-title">
        <div class="activity-table-heading">
            <div>
                <h2 id="staff-table-title">Danh sách nhân viên</h2>
                <p>Thông tin nhân viên thuộc các công ty đối tác</p>
            </div>
            <span class="activity-count">
                <c:choose>
                    <c:when test="${empty partnerStaffs}">0 nhân viên</c:when>
                    <c:otherwise><c:out value="${partnerStaffs.size()}"/> nhân viên</c:otherwise>
                </c:choose>
            </span>
        </div>
        <form class="partner-staff-filter" method="get" action="${pageContext.request.contextPath}/partner-staffs">
            <label for="companyFilter">Lọc theo công ty</label>
            <select id="companyFilter" name="companyId" onchange="this.form.submit()">
                <option value="">Tất cả công ty</option>
                <c:forEach var="company" items="${partnerCompanies}">
                    <option value="${company.id}" <c:if test="${selectedCompanyId == company.id}">selected</c:if>>
                        <c:out value="${company.code}"/> - <c:out value="${company.name}"/>
                    </option>
                </c:forEach>
            </select>
            <noscript><button class="button" type="submit">Lọc</button></noscript>
        </form>
        <div class="table-wrap activity-table-wrap">
            <table class="partner-staff-table">
                <thead>
                <tr>
                    <th scope="col">Mã nhân viên</th>
                    <th scope="col">Họ và tên</th>
                    <th scope="col">Công ty</th>
                    <th scope="col">Chức vụ</th>
                    <th scope="col">Trạng thái</th>
                    <th scope="col">Thao tác</th>
                </tr>
                </thead>
                <tbody>
                <c:choose>
                    <c:when test="${empty partnerStaffs}">
                        <tr>
                            <td class="empty-state activity-empty-state" colspan="6">
                                <span class="empty-state-mark" aria-hidden="true">+</span>
                                <strong>Chưa có nhân viên đối tác</strong>
                                <span>Thêm nhân viên mới để bắt đầu quản lý danh sách.</span>
                                <a class="button button-primary" href="${pageContext.request.contextPath}/partner-staffs?action=create">Thêm nhân viên đầu tiên</a>
                            </td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="staff" items="${partnerStaffs}">
                            <tr>
                                <td><span class="activity-code"><c:out value="${staff.code}"/></span></td>
                                <td><strong class="activity-name"><c:out value="${staff.name}"/></strong></td>
                                <td>
                                    <span class="table-primary-text"><c:out value="${staff.companyName}" default="—"/></span>
                                    <c:if test="${not empty staff.companyCode}"><span class="partner-staff-company-code"><c:out value="${staff.companyCode}"/></span></c:if>
                                </td>
                                <td><c:out value="${staff.position}" default="—"/></td>
                                <td>
                                    <span class="badge ${staff.status == 'ACTIVE' ? 'partner-company-active' : 'partner-company-inactive'}">
                                        <c:choose>
                                            <c:when test="${staff.status == 'ACTIVE'}">Đang hoạt động</c:when>
                                            <c:otherwise>Ngừng hoạt động</c:otherwise>
                                        </c:choose>
                                    </span>
                                </td>
                                <td>
                                    <div class="actions activity-actions">
                                        <a class="activity-action-link" href="${pageContext.request.contextPath}/partner-staffs?action=detail&amp;id=${staff.id}">Chi tiết</a>
                                        <a class="activity-action-link" href="${pageContext.request.contextPath}/partner-staffs?action=edit&amp;id=${staff.id}">Sửa</a>
                                        <a class="activity-action-link activity-action-delete"
                                           href="${pageContext.request.contextPath}/partner-staffs?action=delete&amp;id=${staff.id}"
                                           onclick="return confirm('Bạn có chắc muốn xóa nhân viên đối tác này?');">Xóa</a>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
                </tbody>
            </table>
        </div>
    </section>
</main>
</body>
</html>
