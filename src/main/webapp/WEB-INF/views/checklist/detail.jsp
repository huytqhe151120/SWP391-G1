<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<% request.setAttribute("activeNav", "checklist"); %>
<c:url var="checklistUrl" value="/checklist">
    <c:param name="activityId" value="${taskDetail.task.activityId}"/>
</c:url>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chi tiết công việc | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/checklist.css?v=20261106-2">
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
                        <p class="checklist-eyebrow">CHI TIẾT CHECKLIST</p>
                        <h1><c:out value="${taskDetail.task.name}"/></h1>
                        <p class="subtitle">Mã công việc: <strong><c:out value="${taskDetail.task.code}"/></strong></p>
                    </div>
                    <div class="checklist-header-actions">
                        <c:choose>
                            <c:when test="${canManageChecklist}">
                                <a class="checklist-back-link" href="${checklistUrl}">Quay lại</a>
                                <a class="checklist-create-button"
                                   href="${pageContext.request.contextPath}/checklist?action=edit&amp;id=${taskDetail.task.id}&amp;activityId=${taskDetail.task.activityId}">Chỉnh sửa</a>
                            </c:when>
                            <c:otherwise>
                                <a class="checklist-back-link"
                                   href="${pageContext.request.contextPath}/notifications">Quay lại thông báo</a>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </header>

                <section class="checklist-detail-card">
                    <h2>Thông tin công việc</h2>
                    <dl class="checklist-detail-grid">
                        <div><dt>ID</dt><dd>#<c:out value="${taskDetail.task.id}"/></dd></div>
                        <div><dt>Mã công việc</dt><dd><c:out value="${taskDetail.task.code}"/></dd></div>
                        <div><dt>Tên công việc</dt><dd><c:out value="${taskDetail.task.name}"/></dd></div>
                        <div><dt>Deadline</dt><dd><c:out value="${taskDetail.task.deadlineDisplay}"/></dd></div>
                        <div><dt>Mức độ ưu tiên</dt><dd>
                            <span class="checklist-priority-badge ${taskDetail.task.priority == 'HIGH' ? 'checklist-priority-high' : taskDetail.task.priority == 'LOW' ? 'checklist-priority-low' : 'checklist-priority-medium'}">
                                <c:out value="${taskDetail.task.priorityDisplay}"/>
                            </span>
                        </dd></div>
                        <div><dt>Trạng thái</dt><dd><span class="checklist-status"><c:out value="${taskDetail.task.statusDisplay}"/></span></dd></div>
                        <div class="checklist-detail-wide">
                            <dt>Công việc chuẩn bị</dt>
                            <dd>
                                <c:choose>
                                    <c:when test="${not empty taskDetail.task.description}">
                                        <c:out value="${taskDetail.task.description}"/>
                                    </c:when>
                                    <c:otherwise><span class="checklist-muted">Chưa có công việc chuẩn bị.</span></c:otherwise>
                                </c:choose>
                            </dd>
                        </div>
                    </dl>
                </section>

                <section class="checklist-detail-card">
                    <h2>Công việc tiên quyết</h2>
                    <c:choose>
                        <c:when test="${empty taskDetail.prerequisiteTasks}">
                            <p class="checklist-detail-empty">Công việc này không có công việc tiên quyết.</p>
                        </c:when>
                        <c:otherwise>
                            <div class="checklist-table-wrap">
                                <table class="checklist-table checklist-detail-table">
                                    <thead><tr><th>STT</th><th>Mã công việc</th><th>Tên công việc</th><th>Trạng thái</th></tr></thead>
                                    <tbody>
                                    <c:forEach var="relatedTask" items="${taskDetail.prerequisiteTasks}" varStatus="row">
                                        <tr>
                                            <td><c:out value="${row.index + 1}"/></td>
                                            <td><a href="${pageContext.request.contextPath}/checklist?action=detail&amp;id=${relatedTask.id}&amp;activityId=${taskDetail.task.activityId}"><c:out value="${relatedTask.code}"/></a></td>
                                            <td><c:out value="${relatedTask.name}"/></td>
                                            <td>
                                                <c:out value="${relatedTask.statusDisplay}"/>
                                                <c:if test="${not relatedTask.prerequisitesCompleted}">
                                                    <small class="checklist-prerequisite-locked">
                                                        Đang chờ công việc tiên quyết
                                                    </small>
                                                </c:if>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>

                <section class="checklist-detail-card">
                    <h2>Công việc kế tiếp</h2>
                    <c:choose>
                        <c:when test="${empty taskDetail.nextTasks}">
                            <p class="checklist-detail-empty">Chưa có công việc nào phụ thuộc vào công việc này.</p>
                        </c:when>
                        <c:otherwise>
                            <div class="checklist-table-wrap">
                                <table class="checklist-table checklist-detail-table">
                                    <thead><tr><th>STT</th><th>Mã công việc</th><th>Tên công việc</th><th>Trạng thái</th></tr></thead>
                                    <tbody>
                                    <c:forEach var="relatedTask" items="${taskDetail.nextTasks}" varStatus="row">
                                        <tr>
                                            <td><c:out value="${row.index + 1}"/></td>
                                            <td><a href="${pageContext.request.contextPath}/checklist?action=detail&amp;id=${relatedTask.id}&amp;activityId=${taskDetail.task.activityId}"><c:out value="${relatedTask.code}"/></a></td>
                                            <td><c:out value="${relatedTask.name}"/></td>
                                            <td>
                                                <c:out value="${relatedTask.statusDisplay}"/>
                                                <c:if test="${not relatedTask.prerequisitesCompleted}">
                                                    <small class="checklist-prerequisite-locked">
                                                        Đang chờ công việc tiên quyết
                                                    </small>
                                                </c:if>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>

                <section class="checklist-detail-card">
                    <h2>Nhân viên nội bộ được phân công</h2>
                    <c:choose>
                        <c:when test="${empty taskDetail.staff}">
                            <p class="checklist-detail-empty">Chưa phân công nhân viên nội bộ.</p>
                        </c:when>
                        <c:otherwise>
                            <div class="checklist-table-wrap">
                                <table class="checklist-table checklist-assignee-table">
                                    <thead><tr>
                                        <th>STT</th><th>Mã nhân sự</th><th>Họ và tên</th>
                                        <th>Trạng thái nhân sự</th><th>Chức vụ</th><th>Thời gian phân công</th>
                                    </tr></thead>
                                    <tbody>
                                    <c:forEach var="staff" items="${taskDetail.staff}" varStatus="row">
                                        <tr>
                                            <td><c:out value="${row.index + 1}"/></td>
                                            <td><c:out value="${staff.code}"/></td>
                                            <td><c:out value="${staff.name}"/></td>
                                            <td><c:out value="${staff.statusDisplay}"/></td>
                                            <td><c:out value="${empty staff.position ? 'Chưa cập nhật' : staff.position}"/></td>
                                            <td><c:out value="${staff.assignedAtDisplay}"/></td>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>

                <section class="checklist-detail-card">
                    <h2>Nhân viên đối tác được phân công</h2>
                    <c:choose>
                        <c:when test="${empty taskDetail.partnerStaff}">
                            <p class="checklist-detail-empty">Chưa phân công nhân viên đối tác.</p>
                        </c:when>
                        <c:otherwise>
                            <div class="checklist-table-wrap">
                                <table class="checklist-table checklist-assignee-table">
                                    <thead><tr>
                                        <th>STT</th><th>Mã đối tác</th><th>Họ và tên</th><th>Công ty</th>
                                        <th>Trạng thái nhân sự</th><th>Chức vụ</th><th>Thời gian phân công</th>
                                    </tr></thead>
                                    <tbody>
                                    <c:forEach var="partnerStaff" items="${taskDetail.partnerStaff}" varStatus="row">
                                        <tr>
                                            <td><c:out value="${row.index + 1}"/></td>
                                            <td><c:out value="${partnerStaff.code}"/></td>
                                            <td><c:out value="${partnerStaff.name}"/></td>
                                            <td><c:out value="${empty partnerStaff.companyName ? 'Chưa cập nhật' : partnerStaff.companyName}"/></td>
                                            <td><c:out value="${partnerStaff.statusDisplay}"/></td>
                                            <td><c:out value="${empty partnerStaff.position ? 'Chưa cập nhật' : partnerStaff.position}"/></td>
                                            <td><c:out value="${partnerStaff.assignedAtDisplay}"/></td>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>
            </main>
        </div>
    </div>
</div>
</body>
</html>
