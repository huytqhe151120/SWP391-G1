<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Activity Form</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css">
</head>

<body data-context-path="${pageContext.request.contextPath}">

<c:set var="editing" value="${not empty activity}"/>

<div class="page activity-form-page">

    <!-- ================= HERO ================= -->
    <div class="page-header activity-hero">
        <div class="activity-hero-copy">
            <span class="eyebrow">ACTIVITY MANAGEMENT</span>
            <h1>
                <c:choose>
                    <c:when test="${editing}">Sửa hoạt động ngoại khóa</c:when>
                    <c:otherwise>Thêm hoạt động ngoại khóa</c:otherwise>
                </c:choose>
            </h1>
            <p>Nhập đầy đủ thông tin hoạt động, đơn vị phụ trách và đối tác</p>
        </div>
    </div>

    <!-- ================= FORM CARD ================= -->
    <form action="${pageContext.request.contextPath}/activity" method="post"
          class="card activity-form-card">

        <input type="hidden" name="action" value="${editing ? 'update' : 'create'}">
        <c:if test="${editing}">
            <input type="hidden" name="id" value="${activity.id}">
        </c:if>

        <div class="activity-table-heading">
            <div>
                <h2>
                    <c:choose>
                        <c:when test="${editing}">Cập nhật hoạt động</c:when>
                        <c:otherwise>Hoạt động mới</c:otherwise>
                    </c:choose>
                </h2>
                <p>Các trường có dấu <span class="required-mark">*</span> là bắt buộc</p>
            </div>
        </div>

        <div class="activity-form-content">

            <!-- ===== Thông tin chung ===== -->
            <div class="activity-form-section">
                <h3>Thông tin chung</h3>
                <p>Mã, tên và loại hoạt động</p>
            </div>

            <div class="form-grid activity-form-grid">

                <div class="field">
                    <label for="code">Mã hoạt động <span class="required-mark">*</span></label>
                    <input id="code" name="code" required
                           value="<c:out value='${activity.code}'/>">
                </div>

                <div class="field">
                    <label for="name">Tên hoạt động <span class="required-mark">*</span></label>
                    <input id="name" name="name" required
                           value="<c:out value='${activity.name}'/>">
                </div>

                <div class="field">
                    <label for="semesterId">Semester ID <span class="required-mark">*</span></label>
                    <input id="semesterId" type="number" name="semesterId" min="1" required
                           value="<c:out value='${activity.semesterId}'/>">
                </div>

                <div class="field">
                    <label for="activityType">Activity type <span class="required-mark">*</span></label>
                    <input id="activityType" name="activityType" required
                           value="<c:out value='${activity.activityType}'/>">
                </div>

            </div>


            <!-- ===== Đơn vị phụ trách ===== -->
            <div class="activity-form-section">
                <h3>Đơn vị phụ trách</h3>
                <p>Phòng ban và cán bộ chịu trách nhiệm</p>
            </div>

            <div class="form-grid activity-form-grid">

                <div class="field">
                    <label for="responsibleDepartmentId">Department ID <span class="required-mark">*</span></label>
                    <input id="responsibleDepartmentId" type="number" name="responsibleDepartmentId"
                           min="1" required
                           value="<c:out value='${activity.responsibleDepartmentId}'/>">
                </div>

                <div class="field">
                    <label for="responsibleStaffId">Responsible staff ID <span class="required-mark">*</span></label>
                    <input id="responsibleStaffId" type="number" name="responsibleStaffId"
                           min="1" required
                           value="<c:out value='${activity.responsibleStaffId}'/>">
                </div>

            </div>


            <!-- ===== Đối tác ===== -->
            <div class="activity-form-section">
                <h3>Đối tác</h3>
                <p>Chọn partner trước, danh sách partner staff sẽ tự cập nhật</p>
            </div>

            <div class="form-grid activity-form-grid">

                <div class="field">
                    <label for="partnerId">Partner <span class="required-mark">*</span></label>
                    <select id="partnerId" name="partnerId" required>
                        <option value="">-- Chọn partner --</option>
                        <c:forEach var="partner" items="${partners}">
                            <option value="${partner.id}"
                                ${partner.id == activity.partnerId ? 'selected' : ''}>
                                <c:out value="${partner.name}"/>
                            </option>
                        </c:forEach>
                    </select>
                </div>

                <div class="field">
                    <label for="partnerStaffId">Partner staff <span class="required-mark">*</span></label>
                    <select id="partnerStaffId" name="partnerStaffId" required>
                        <option value="">-- Chọn partner staff --</option>
                        <c:forEach var="staff" items="${partnerStaffOptions}">
                            <option value="${staff.id}"
                                ${staff.id == activity.partnerStaffId ? 'selected' : ''}>
                                <c:out value="${staff.fullName}"/>
                            </option>
                        </c:forEach>
                    </select>
                </div>

            </div>


            <!-- ===== Điểm & trạng thái ===== -->
            <div class="activity-form-section">
                <h3>Điểm và trạng thái</h3>
                <p>Điểm cộng, điểm trừ và trạng thái hoạt động</p>
            </div>

            <div class="form-grid activity-form-grid">

                <div class="field">
                    <label for="bonusPoint">Điểm cộng <span class="required-mark">*</span></label>
                    <input id="bonusPoint" type="number" step="any" min="0" name="bonusPoint" required
                           value="<c:out value='${activity.bonusPoint}'/>">
                </div>

                <div class="field">
                    <label for="penaltyPoint">Điểm trừ <span class="required-mark">*</span></label>
                    <input id="penaltyPoint" type="number" step="any" min="0" name="penaltyPoint" required
                           value="<c:out value='${activity.penaltyPoint}'/>">
                </div>

                <div class="field">
                    <label for="activityStatus">Activity status <span class="required-mark">*</span></label>
                    <select id="activityStatus" name="activityStatus" required>
                        <option value="">-- Chọn trạng thái --</option>
                        <option value="UPCOMING"  ${activity.activityStatus == 'UPCOMING'  ? 'selected' : ''}>Upcoming</option>
                        <option value="ONGOING"   ${activity.activityStatus == 'ONGOING'   ? 'selected' : ''}>Ongoing</option>
                        <option value="COMPLETED" ${activity.activityStatus == 'COMPLETED' ? 'selected' : ''}>Completed</option>
                        <option value="CANCELLED" ${activity.activityStatus == 'CANCELLED' ? 'selected' : ''}>Cancelled</option>
                    </select>
                </div>

                <div class="field">
                    <label for="approvalStatus">Approval status <span class="required-mark">*</span></label>
                    <select id="approvalStatus" name="approvalStatus" required>
                        <option value="">-- Chọn trạng thái --</option>
                        <option value="DRAFT"    ${activity.approvalStatus == 'DRAFT'    ? 'selected' : ''}>Draft</option>
                        <option value="PENDING"  ${activity.approvalStatus == 'PENDING'  ? 'selected' : ''}>Pending</option>
                        <option value="APPROVED" ${activity.approvalStatus == 'APPROVED' ? 'selected' : ''}>Approved</option>
                        <option value="REJECTED" ${activity.approvalStatus == 'REJECTED' ? 'selected' : ''}>Rejected</option>
                    </select>
                </div>

            </div>


            <!-- ===== Chi tiết ===== -->
            <div class="activity-form-section">
                <h3>Chi tiết</h3>
                <p>Địa điểm và mô tả nội dung hoạt động</p>
            </div>

            <div class="form-grid activity-form-grid">

                <div class="field field-wide">
                    <label for="address">Địa chỉ</label>
                    <input id="address" name="address"
                           value="<c:out value='${activity.address}'/>">
                </div>

                <div class="field field-wide">
                    <label for="description">Mô tả</label>
                    <textarea id="description" name="description"><c:out value="${activity.description}"/></textarea>
                </div>

            </div>

        </div>


        <!-- ================= ACTIONS ================= -->
        <div class="form-actions activity-form-actions">
            <a class="button activity-cancel-button"
               href="${pageContext.request.contextPath}/activity">Hủy</a>
            <button type="submit" class="button button-primary">Lưu</button>
        </div>

    </form>

</div>

<script src="${pageContext.request.contextPath}/assets/js/activity.js"></script>

</body>
</html>
