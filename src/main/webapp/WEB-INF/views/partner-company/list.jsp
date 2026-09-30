<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Partner Company List</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/css/partner-company.css">
</head>

<body>

<div class="page activity-list-page">

    <!-- ================= HERO ================= -->
    <div class="activity-hero page-header">

        <div class="activity-hero-copy">
            <span class="eyebrow">PARTNER MANAGEMENT</span>
            <h1>Danh sách đối tác</h1>
            <p>Quản lý thông tin các công ty đối tác của hoạt động ngoại khóa</p>
        </div>

        <a class="button activity-create-button"
           href="${pageContext.request.contextPath}/partner-company?action=create">
            <span class="plus-icon" aria-hidden="true">+</span>
            Thêm partner
        </a>

    </div>


    <!-- ================= TABLE CARD ================= -->
    <div class="card activity-table-card">

        <div class="activity-table-heading">

            <div>
                <h2>Partner List</h2>
                <p>Danh sách công ty đối tác trong hệ thống</p>
            </div>

            <span class="activity-count">
                <c:choose>
                    <c:when test="${empty partner}">0 đối tác</c:when>
                    <c:otherwise><c:out value="${partner.size()}"/> đối tác</c:otherwise>
                </c:choose>
            </span>

        </div>


        <div class="table-wrap activity-table-wrap">

            <table class="partner-company-table">

                <thead>
                <tr>
                    <th scope="col">ID</th>
                    <th scope="col">Tên</th>
                    <th scope="col">Email</th>
                    <th scope="col">Số điện thoại</th>
                    <th scope="col">Trạng thái</th>
                    <th scope="col">Thao tác</th>
                </tr>
                </thead>


                <tbody>

                <c:choose>

                    <c:when test="${empty partner}">
                        <tr>
                            <td class="empty-state" colspan="6">
                                Chưa có đối tác nào.
                            </td>
                        </tr>
                    </c:when>

                    <c:otherwise>

                        <c:forEach var="item" items="${partner}">

                            <tr>

                                <td><c:out value="${item.id}"/></td>

                                <td>
                                    <strong class="activity-name">
                                        <c:out value="${item.name}"/>
                                    </strong>
                                </td>

                                <td><c:out value="${item.email}" default="—"/></td>

                                <td><c:out value="${item.phone}" default="—"/></td>

                                <td>
                                    <c:choose>

                                        <c:when test="${item.status == 'ACTIVE'}">
                                            <span class="badge partner-company-active">Active</span>
                                        </c:when>

                                        <c:when test="${item.status == 'INACTIVE'}">
                                            <span class="badge partner-company-inactive">Inactive</span>
                                        </c:when>

                                        <c:otherwise>
                                            <span class="badge">
                                                <c:out value="${item.status}" default="—"/>
                                            </span>
                                        </c:otherwise>

                                    </c:choose>
                                </td>

                                <!-- ================= ACTIONS ================= -->
                                <td>

                                    <div class="actions activity-actions">

                                        <a class="activity-action-link"
                                           href="${pageContext.request.contextPath}/partner-company?action=detail&amp;id=${item.id}">Chi tiết</a>

                                        <a class="activity-action-link"
                                           href="${pageContext.request.contextPath}/partner-company?action=edit&amp;id=${item.id}">Sửa</a>

                                        <!-- Delete (POST) -->
                                        <form action="${pageContext.request.contextPath}/partner-company"
                                              method="post"
                                              style="margin:0;">
                                            <input type="hidden" name="action" value="delete">
                                            <input type="hidden" name="id" value="${item.id}">
                                            <a href="#" role="button"
                                               class="activity-action-link activity-action-delete"
                                               onclick="if (confirm('Bạn có chắc muốn xóa partner này?')) { this.closest('form').submit(); } return false;">Xóa</a>
                                        </form>

                                    </div>

                                </td>

                            </tr>

                        </c:forEach>

                    </c:otherwise>

                </c:choose>

                </tbody>

            </table>

        </div>

    </div>

</div>

</body>
</html>
