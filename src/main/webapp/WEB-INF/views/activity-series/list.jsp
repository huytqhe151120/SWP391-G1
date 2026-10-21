<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="activity-series" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chuỗi hoạt động | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261020-11">
</head>
<body>
<div class="activity-app-shell">
    <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>

    <div class="activity-app-layout">
        <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
        <main class="page activity-list-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> QUẢN LÝ CHUỖI HOẠT ĐỘNG</span>
                    <h1>Chuỗi hoạt động</h1>
                    <p>Quản lý chuỗi hoạt động lặp lại theo tháng/năm và tạo các sự kiện lặp lại.</p>
                </div>
            </header>

    <c:if test="${not empty sessionScope.successMessage}">
        <div class="alert alert-success activity-alert"><c:out value="${sessionScope.successMessage}"/></div>
        <c:remove var="successMessage" scope="session"/>
    </c:if>
    <c:if test="${not empty sessionScope.errorMessage}">
        <div class="alert alert-error activity-alert"><c:out value="${sessionScope.errorMessage}"/></div>
        <c:remove var="errorMessage" scope="session"/>
    </c:if>

    <section class="card activity-table-card" aria-labelledby="series-table-title">
        <div class="activity-table-heading">
            <div>
                <h2 id="series-table-title">Danh sách chuỗi hoạt động</h2>
                <p>Quản lý chuỗi hoạt động và tạo sự kiện lặp lại.</p>
            </div>
            <div class="activity-heading-actions">
                <c:if test="${canManageActivities}">
                    <a class="btn-create-activity"
                       onclick="location.href='${pageContext.request.contextPath}/activity-series?action=create'">
                        Tạo chuỗi hoạt động
                    </a>
                </c:if>
            </div>
        </div>

        <table class="activity-table">
            <thead>
                <tr>
                    <th>Mã chuỗi</th>
                    <th>Tên chuỗi</th>
                    <th>Chế độ lặp</th>
                    <th>Khoảng cách</th>
                    <th>Bắt đầu</th>
                    <th>Kết thúc</th>
                    <th>Trạng thái duyệt</th>
                    <th class="activity-table-actions">Hành động</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="s" items="${seriesList}">
                    <tr>
                        <td><c:out value="${s.seriesCode}"/></td>
                        <td><c:out value="${s.name}"/></td>
                        <td><c:out value="${s.recurrenceMode}"/></td>
                        <td><c:out value="${s.recurrenceInterval}"/></td>
                        <td><c:out value="${s.seriesStartDate}"/></td>
                        <td><c:out value="${s.seriesEndDate}"/></td>
                        <td><c:out value="${s.approvalStatus}"/></td>
                        <td class="activity-table-actions">
                            <a class="button button-outline"
                               href="${pageContext.request.contextPath}/activity-series?action=detail&id=${s.id}">Chi tiết</a>
                            <c:if test="${canManageActivities}">
                                <a class="button button-outline"
                                   href="${pageContext.request.contextPath}/activity-series?action=edit&id=${s.id}">Sửa</a>
                                <form method="post"
                                      action="${pageContext.request.contextPath}/activity-series"
                                      style="display:inline;"
                                      onsubmit="return confirm('Bạn có chắc chắn muốn xóa chuỗi hoạt động này?');">
                                    <input type="hidden" name="action" value="delete"/>
                                    <input type="hidden" name="id" value="${s.id}"/>
                                    <button class="button button-danger-light" type="submit">Xóa</button>
                                </form>
                                <form method="post"
                                      action="${pageContext.request.contextPath}/activity-series"
                                      style="display:inline;"
                                      onsubmit="return confirm('Bạn có chắc chắn muốn tạo sự kiện lặp lại cho chuỗi này?');">
                                    <input type="hidden" name="action" value="generate"/>
                                    <input type="hidden" name="seriesId" value="${s.id}"/>
                                    <button class="button button-primary" type="submit">Tạo sự kiện lặp</button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </section>
        </main>
    </div>
</div>
</body>
</html>