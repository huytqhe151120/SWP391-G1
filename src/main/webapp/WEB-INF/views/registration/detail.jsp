<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="st" uri="http://swp391.g1/status" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeNav" value="registration-forms" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chi tiết form đăng ký | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261020-11">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/registration.css?v=20261021-6">
</head>
<body>
<div class="activity-app-shell">
    <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
    <div class="activity-app-layout">
        <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
        <div class="activity-app-main">
            <main class="page activity-list-page activity-form-page activity-detail-page">
                <header class="activity-hero">
                    <div class="activity-hero-copy">
                        <span class="eyebrow">SWP391-G1 <span aria-hidden="true">/</span> QUẢN LÝ ĐĂNG KÝ</span>
                        <h1>Chi tiết form đăng ký</h1>
                        <p><c:out value="${form.title}"/></p>
                    </div>
                </header>

                <section class="card activity-form-card activity-detail-card registration-detail-card">
                    <div class="activity-table-heading">
                        <div>
                            <h2>Thông tin form</h2>
                            <p>Chi tiết nội dung và tình trạng đăng ký của form.</p>
                        </div>
                        <div class="registration-detail-actions">
                            <a class="button activity-cancel-button" href="${pageContext.request.contextPath}/registration-forms">Danh sách form</a>
                            <a class="button button-primary" href="${pageContext.request.contextPath}/registration-forms?action=edit&amp;id=${form.id}">Chỉnh sửa</a>
                        </div>
                    </div>

                    <div class="activity-detail-content">
                        <section class="activity-detail-section">
                            <h3>Thông tin cơ bản</h3>
                            <dl class="detail-grid activity-detail-grid">
                                <div class="detail-item">
                                    <dt>Tiêu đề form</dt>
                                    <dd><c:out value="${form.title}"/></dd>
                                </div>
                                <div class="detail-item">
                                    <dt>Hoạt động</dt>
                                    <dd><c:out value="${form.activityName}"/></dd>
                                </div>
                                <div class="detail-item">
                                    <dt>Trạng thái</dt>
                                    <dd><span class="badge status-pill ${st:css(form.status)}">${st:label(form.status)}</span></dd>
                                </div>
                                <div class="detail-item">
                                    <dt>Số lượng đăng ký</dt>
                                    <dd><span class="registration-count"><c:out value="${form.registrationCount}"/></span></dd>
                                </div>
                            </dl>
                        </section>

                        <section class="activity-detail-section">
                            <h3>Thời gian đăng ký</h3>
                            <dl class="detail-grid activity-detail-grid">
                                <div class="detail-item">
                                    <dt>Mở đăng ký</dt>
                                    <dd><c:choose><c:when test="${not empty form.openAt}"><c:out value="${fn:replace(form.openAt, 'T', ' ')}"/></c:when><c:otherwise>Không giới hạn</c:otherwise></c:choose></dd>
                                </div>
                                <div class="detail-item">
                                    <dt>Đóng đăng ký</dt>
                                    <dd><c:choose><c:when test="${not empty form.closeAt}"><c:out value="${fn:replace(form.closeAt, 'T', ' ')}"/></c:when><c:otherwise>Không giới hạn</c:otherwise></c:choose></dd>
                                </div>
                            </dl>
                        </section>

                        <section class="activity-detail-section">
                            <h3>Mô tả và hướng dẫn</h3>
                            <dl class="detail-grid activity-detail-grid">
                                <div class="detail-item field-wide">
                                    <dt>Nội dung</dt>
                                    <dd class="activity-detail-description"><c:out value="${form.description}" default="Chưa có mô tả"/></dd>
                                </div>
                            </dl>
                        </section>
                    </div>
                </section>
            </main>
        </div>
    </div>
</div>
</body>
</html>
