<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<% request.setAttribute("activeNav", "checklist"); %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Checklist công việc | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/checklist.css?v=20261029-10">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css">
</head>
<body>
<div class="activity-app-shell">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <div class="activity-app-main">
            <main class="page checklist-page">
                <header class="checklist-header">
                    <div>
                        <p class="checklist-eyebrow">CÔNG VIỆC ĐƯỢC GIAO</p>
                        <h1>Checklist công việc</h1>
                        <p class="subtitle">Danh sách công việc hiện có trong checklist.</p>
                    </div>
                    <div class="checklist-header-actions">
                        <span class="checklist-count"><c:out value="${fn:length(checklistTasks)}"/> công việc</span>
                        <a class="checklist-create-button" href="${pageContext.request.contextPath}/checklist?action=create">
                            <span aria-hidden="true">+</span> Tạo công việc
                        </a>
                    </div>
                </header>

                <c:if test="${param.created == '1'}">
                    <div class="checklist-success" role="status">Đã tạo công việc checklist thành công.</div>
                </c:if>
                <c:if test="${param.updated == '1'}">
                    <div class="checklist-success" role="status">Đã cập nhật công việc checklist.</div>
                </c:if>
                <c:if test="${param.deleted == '1'}">
                    <div class="checklist-success" role="status">Đã xóa công việc checklist.</div>
                </c:if>
                <c:if test="${param.notFound == '1'}">
                    <div class="checklist-error" role="alert">Công việc không tồn tại hoặc đã được xóa.</div>
                </c:if>
                <c:if test="${param.statusUpdated == '1'}">
                    <div class="checklist-success" role="status">Đã cập nhật trạng thái công việc.</div>
                </c:if>

                <c:choose>
                    <c:when test="${empty checklistTasks}">
                        <section class="checklist-empty">
                            <p>Chưa có công việc nào trong checklist.</p>
                        </section>
                    </c:when>
                    <c:otherwise>
                        <section class="checklist-table-wrap" aria-label="Danh sách công việc">
                            <table class="checklist-table">
                                <thead>
                                <tr>
                                    <th scope="col">ID</th>
                                    <th scope="col">Mã công việc</th>
                                    <th scope="col">Tên công việc</th>
                                    <th scope="col">Công việc chuẩn bị</th>
                                    <th scope="col">Hạn hoàn thành</th>
                                    <th scope="col">Trạng thái</th>
                                    <th scope="col">Thao tác</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="task" items="${checklistTasks}">
                                    <tr>
                                        <td class="checklist-table-id">#<c:out value="${task.id}"/></td>
                                        <td class="checklist-table-code"><c:out value="${task.code}"/></td>
                                        <td class="checklist-table-name"><c:out value="${task.name}"/></td>
                                        <td class="checklist-table-description">
                                            <c:choose>
                                                <c:when test="${not empty task.description}">
                                                    <c:out value="${task.description}"/>
                                                </c:when>
                                                <c:otherwise><span class="checklist-muted">Không có mô tả</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td><c:out value="${task.deadlineDisplay}"/></td>
                                        <td>
                                            <form class="checklist-inline-status" method="post"
                                                  action="${pageContext.request.contextPath}/checklist">
                                                <input type="hidden" name="action" value="updateStatus">
                                                <input type="hidden" name="id" value="${task.id}">
                                                <select name="status" aria-label="Trạng thái công việc">
                                                    <option value="Chưa hoàn thành" ${task.statusValue == 'Chưa hoàn thành' ? 'selected' : ''}>Chưa hoàn thành</option>
                                                    <option value="Đang hoàn thành" ${task.statusValue == 'Đang hoàn thành' ? 'selected' : ''}>Đang hoàn thành</option>
                                                    <option value="Đã hoàn thành" ${task.statusValue == 'Đã hoàn thành' ? 'selected' : ''}>Đã hoàn thành</option>
                                                </select>
                                                <button type="submit" aria-label="Lưu trạng thái">Lưu</button>
                                            </form>
                                        </td>
                                        <td class="checklist-actions-cell">
                                            <div class="checklist-actions">
                                                <a class="checklist-detail-link"
                                                   href="${pageContext.request.contextPath}/checklist?action=detail&amp;id=${task.id}">Chi tiết</a>
                                                <details class="action-menu">
                                                    <summary class="action-menu__summary">Thao tác</summary>
                                                    <div class="action-menu__items">
                                                        <a class="action-menu-item action-menu-item--edit"
                                                           href="${pageContext.request.contextPath}/checklist?action=edit&amp;id=${task.id}">
                                                            Chỉnh sửa
                                                        </a>
                                                        <form method="post" action="${pageContext.request.contextPath}/checklist"
                                                              onsubmit="return confirm('Bạn có chắc muốn xóa công việc này không?');">
                                                            <input type="hidden" name="action" value="delete">
                                                            <input type="hidden" name="id" value="${task.id}">
                                                            <button class="action-menu-item action-menu-item--delete" type="submit">Xóa</button>
                                                        </form>
                                                    </div>
                                                </details>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </section>
                    </c:otherwise>
                </c:choose>
            </main>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/action-menu.js" defer></script>
</body>
</html>
