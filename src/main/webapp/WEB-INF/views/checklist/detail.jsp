<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<% request.setAttribute("activeNav", "checklist"); %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chi tiết công việc | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/checklist.css?v=20261029-9">
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
                        <a class="checklist-back-link" href="${pageContext.request.contextPath}/checklist">Quay lại</a>
                        <a class="checklist-create-button"
                           href="${pageContext.request.contextPath}/checklist?action=edit&amp;id=${taskDetail.task.id}">Chỉnh sửa</a>
                    </div>
                </header>

                <section class="checklist-detail-card">
                    <h2>Thông tin công việc</h2>
                    <dl class="checklist-detail-grid">
                        <div><dt>ID</dt><dd>#<c:out value="${taskDetail.task.id}"/></dd></div>
                        <div><dt>Mã công việc</dt><dd><c:out value="${taskDetail.task.code}"/></dd></div>
                        <div><dt>Tên công việc</dt><dd><c:out value="${taskDetail.task.name}"/></dd></div>
                        <div><dt>Hạn hoàn thành</dt><dd><c:out value="${taskDetail.task.deadlineDisplay}"/></dd></div>
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
                    <h2>Nhân viên nội bộ được phân công</h2>
                    <c:choose>
                        <c:when test="${empty taskDetail.staff}">
                            <p class="checklist-detail-empty">Chưa phân công nhân viên nội bộ.</p>
                        </c:when>
                        <c:otherwise>
                            <div class="checklist-assigned-list">
                                <c:forEach var="staff" items="${taskDetail.staff}">
                                    <div class="checklist-assigned-person">
                                        <strong><c:out value="${staff.name}"/></strong>
                                        <span><c:out value="${staff.code}"/></span>
                                    </div>
                                </c:forEach>
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
                            <div class="checklist-assigned-list">
                                <c:forEach var="partnerStaff" items="${taskDetail.partnerStaff}">
                                    <div class="checklist-assigned-person">
                                        <strong><c:out value="${partnerStaff.name}"/></strong>
                                        <span><c:out value="${partnerStaff.code}"/>
                                            <c:if test="${not empty partnerStaff.companyName}">
                                                · <c:out value="${partnerStaff.companyName}"/>
                                            </c:if>
                                        </span>
                                    </div>
                                </c:forEach>
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
