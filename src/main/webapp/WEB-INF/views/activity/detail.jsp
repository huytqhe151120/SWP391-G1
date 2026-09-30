<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Activity Detail</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css">
</head>

<body>

<div class="page activity-form-page">

    <!-- ================= HERO ================= -->
    <div class="page-header activity-hero">
        <div class="activity-hero-copy">
            <span class="eyebrow">ACTIVITY MANAGEMENT</span>
            <h1>Chi tiết hoạt động ngoại khóa</h1>
            <p><c:out value="${activity.code}"/> - <c:out value="${activity.name}"/></p>
        </div>
    </div>

    <!-- ================= DETAIL CARD ================= -->
    <div class="card activity-detail-card">

        <div class="activity-table-heading">
            <div>
                <h2>Thông tin hoạt động</h2>
                <p>Chi tiết đầy đủ của hoạt động</p>
            </div>

            <a class="button"
               href="${pageContext.request.contextPath}/activity">Quay lại</a>
        </div>

        <div class="activity-detail-content">

            <!-- ===== Thông tin chung ===== -->
            <div class="activity-detail-section">
                <h3>Thông tin chung</h3>

                <dl class="detail-grid activity-detail-grid">
                    <div class="detail-item">
                        <dt>ID</dt>
                        <dd><c:out value="${activity.id}"/></dd>
                    </div>
                    <div class="detail-item">
                        <dt>Mã hoạt động</dt>
                        <dd><span class="activity-code"><c:out value="${activity.code}"/></span></dd>
                    </div>
                    <div class="detail-item">
                        <dt>Tên hoạt động</dt>
                        <dd><strong class="activity-name"><c:out value="${activity.name}"/></strong></dd>
                    </div>
                    <div class="detail-item">
                        <dt>Học kỳ ID</dt>
                        <dd><c:out value="${activity.semesterId}"/></dd>
                    </div>
                    <div class="detail-item">
                        <dt>Activity Type</dt>
                        <dd><c:out value="${activity.activityType}"/></dd>
                    </div>
                    <div class="detail-item">
                        <dt>Ngày tạo</dt>
                        <dd><c:out value="${activity.createdAt}"/></dd>
                    </div>
                </dl>
            </div>


            <!-- ===== Đơn vị phụ trách ===== -->
            <div class="activity-detail-section">
                <h3>Đơn vị phụ trách</h3>

                <dl class="detail-grid activity-detail-grid">
                    <div class="detail-item">
                        <dt>Đơn vị phụ trách ID</dt>
                        <dd><c:out value="${activity.responsibleDepartmentId}"/></dd>
                    </div>
                    <div class="detail-item">
                        <dt>Nhân viên phụ trách ID</dt>
                        <dd><c:out value="${activity.responsibleStaffId}"/></dd>
                    </div>
                    <div class="detail-item">
                        <dt>Partner Company ID</dt>
                        <dd>
                            <c:choose>
                                <c:when test="${activity.partnerId > 0}">
                                    <c:out value="${activity.partnerId}"/>
                                </c:when>
                                <c:otherwise>--</c:otherwise>
                            </c:choose>
                        </dd>
                    </div>
                    <div class="detail-item">
                        <dt>Partner Staff ID</dt>
                        <dd>
                            <c:choose>
                                <c:when test="${activity.partnerStaffId > 0}">
                                    <c:out value="${activity.partnerStaffId}"/>
                                </c:when>
                                <c:otherwise>--</c:otherwise>
                            </c:choose>
                        </dd>
                    </div>
                </dl>
            </div>


            <!-- ===== Điểm & trạng thái ===== -->
            <div class="activity-detail-section">
                <h3>Điểm và trạng thái</h3>

                <dl class="detail-grid activity-detail-grid">
                    <div class="detail-item">
                        <dt>Điểm cộng</dt>
                        <dd><c:out value="${activity.bonusPoint}"/></dd>
                    </div>
                    <div class="detail-item">
                        <dt>Điểm trừ</dt>
                        <dd><c:out value="${activity.penaltyPoint}"/></dd>
                    </div>
                    <div class="detail-item">
                        <dt>Activity Status</dt>
                        <dd><span class="badge activity-status"><c:out value="${activity.activityStatus}"/></span></dd>
                    </div>
                    <div class="detail-item">
                        <dt>Approval Status</dt>
                        <dd><span class="badge"><c:out value="${activity.approvalStatus}"/></span></dd>
                    </div>
                </dl>
            </div>


            <!-- ===== Chi tiết ===== -->
            <div class="activity-detail-section">
                <h3>Chi tiết</h3>

                <dl class="detail-grid activity-detail-grid">
                    <div class="detail-item">
                        <dt>Địa chỉ</dt>
                        <dd>
                            <c:choose>
                                <c:when test="${not empty activity.address}">
                                    <c:out value="${activity.address}"/>
                                </c:when>
                                <c:otherwise>--</c:otherwise>
                            </c:choose>
                        </dd>
                    </div>
                    <div class="detail-item">
                        <dt>Mô tả</dt>
                        <dd class="activity-detail-description"><c:out value="${activity.description}"/></dd>
                    </div>
                </dl>
            </div>

        </div>


        <!-- ================= ACTIONS ================= -->
        <div class="detail-actions activity-detail-actions">

            <a class="button"
               href="${pageContext.request.contextPath}/activity?action=edit&id=${activity.id}">Sửa</a>

            <c:if test="${activity.approvalStatus == 'PENDING'}">

                <form action="${pageContext.request.contextPath}/activity" method="post"
                      class="approve-form">
                    <input type="hidden" name="action" value="approve">
                    <input type="hidden" name="id" value="${activity.id}">
                    <button type="submit" class="button button-primary">Duyệt hoạt động</button>
                </form>

                <form action="${pageContext.request.contextPath}/activity" method="post"
                      class="reject-form" style="display:flex; gap:8px;">
                    <input type="hidden" name="action" value="reject">
                    <input type="hidden" name="id" value="${activity.id}">
                    <input type="text" name="note" class="reject-note"
                           placeholder="Lý do từ chối" style="width:240px;">
                    <button type="submit" class="button button-danger">Từ chối</button>
                </form>

            </c:if>

        </div>

    </div>

</div>

<script src="${pageContext.request.contextPath}/assets/js/activity.js"></script>

</body>
</html>
