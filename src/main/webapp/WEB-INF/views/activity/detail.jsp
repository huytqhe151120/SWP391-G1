<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="st" uri="http://swp391.g1/status" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chi tiết hoạt động | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261014">
</head>
<body>
<main class="page activity-list-page activity-form-page activity-detail-page">
    <header class="activity-hero">
        <div class="activity-hero-copy">
            <span class="eyebrow">SWP391-G1 <span aria-hidden="true">/</span> QUẢN LÝ HOẠT ĐỘNG</span>
            <h1>Chi tiết hoạt động</h1>
            <p><span class="activity-code"><c:out value="${activity.code}"/></span> <c:out value="${activity.name}"/></p>
        </div>
        <a class="button activity-create-button" href="${pageContext.request.contextPath}/activities">
            <svg class="ui-icon" aria-hidden="true"><use href="${pageContext.request.contextPath}/assets/icons/ui-icons.svg#arrow-left"/></svg>
            Danh sách hoạt động
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

    <section class="card activity-form-card activity-detail-card">
        <div class="activity-table-heading">
            <div>
                <h2>Chi tiết hoạt động</h2>
                <p>Thông tin tổ chức, thời gian và nhân sự phụ trách hoạt động.</p>
            </div>
            <c:if test="${canManageActivities}">
                <a class="button button-primary"
                   href="${pageContext.request.contextPath}/activities?action=edit&amp;id=${activity.id}">Chỉnh sửa</a>
            </c:if>
        </div>

        <div class="activity-tabs-nav" role="tablist">
            <button type="button" class="activity-tab-btn active" id="btn-tab-info" data-tab="tab-info" role="tab" aria-selected="true" aria-controls="tab-info">
                Thông tin chi tiết hoạt động
            </button>
            <button type="button" class="activity-tab-btn" id="btn-tab-staff" data-tab="tab-staff" role="tab" aria-selected="false" aria-controls="tab-staff">
                Nhân sự phụ trách
                <c:if test="${not empty assignedStaffs}">
                    <span class="activity-tab-badge"><c:out value="${assignedStaffs.size()}"/></span>
                </c:if>
            </button>
            <button type="button" class="activity-tab-btn" id="btn-tab-partner" data-tab="tab-partner" role="tab" aria-selected="false" aria-controls="tab-partner">
                Nhân sự đối tác phụ trách
                <c:if test="${not empty assignedPartnerStaffs}">
                    <span class="activity-tab-badge"><c:out value="${assignedPartnerStaffs.size()}"/></span>
                </c:if>
            </button>
        </div>

        <div class="activity-detail-content">
            <!-- TAB 1: THÔNG TIN CHI TIẾT HOẠT ĐỘNG -->
            <div id="tab-info" class="activity-tab-pane active" role="tabpanel" aria-labelledby="btn-tab-info">
                <section class="activity-detail-section">
                    <h3>Thông tin cơ bản</h3>
                    <dl class="detail-grid activity-detail-grid">
                        <div class="detail-item"><dt>Mã hoạt động</dt><dd><c:out value="${activity.code}" default="—"/></dd></div>
                        <div class="detail-item"><dt>Tên hoạt động</dt><dd><c:out value="${activity.name}" default="—"/></dd></div>
                        <div class="detail-item"><dt>Học kỳ</dt><dd><c:out value="${activity.semesterCode}" default="—"/> <c:if test="${not empty activity.semesterName}">- <c:out value="${activity.semesterName}"/></c:if></dd></div>
                        <div class="detail-item"><dt>Loại hoạt động</dt><dd><c:out value="${activity.activityTypeCode}" default="—"/> <c:if test="${not empty activity.activityTypeName}">- <c:out value="${activity.activityTypeName}"/></c:if></dd></div>
                        <div class="detail-item"><dt>Trạng thái hoạt động</dt><dd><span class="badge status-pill ${st:css(activity.activityStatus)}">${st:label(activity.activityStatus)}</span></dd></div>
                        <div class="detail-item"><dt>Trạng thái duyệt</dt><dd><span class="badge status-pill ${st:css(activity.approvalStatus)}">${st:label(activity.approvalStatus)}</span></dd></div>
                    </dl>
                </section>

                <section class="activity-detail-section">
                    <h3>Phụ trách và đối tác</h3>
                    <dl class="detail-grid activity-detail-grid">
                        <div class="detail-item"><dt>Đơn vị phụ trách</dt><dd><c:out value="${activity.responsibleDepartmentCode}" default="—"/> <c:if test="${not empty activity.responsibleDepartmentName}">- <c:out value="${activity.responsibleDepartmentName}"/></c:if></dd></div>
                        <div class="detail-item"><dt>Nhân viên phụ trách chính</dt><dd><c:out value="${activity.responsibleStaffCode}" default="—"/> <c:if test="${not empty activity.responsibleStaffName}">- <c:out value="${activity.responsibleStaffName}"/></c:if></dd></div>
                        <div class="detail-item"><dt>Công ty đối tác</dt><dd><c:out value="${activity.partnerCompanyName}" default="Không có"/></dd></div>
                        <div class="detail-item"><dt>Nhân viên đối tác phụ trách chính</dt><dd><c:out value="${activity.partnerStaffName}" default="Không có"/></dd></div>
                    </dl>
                </section>

                <section class="activity-detail-section">
                    <h3>Thời gian, địa điểm và điểm số</h3>
                    <dl class="detail-grid activity-detail-grid">
                        <div class="detail-item"><dt>Bắt đầu</dt>                        <dd><c:out value="${dt:format(activity.startTime)}" default="Chưa xác định"/></dd></div>
                                                <div class="detail-item"><dt>Kết thúc</dt><dd><c:out value="${dt:format(activity.endTime)}" default="Chưa xác định"/></dd></div>
                                                <div class="detail-item"><dt>Bắt buộc điền form</dt><dd><c:out value="${activity.registrationFormRequired ? 'Có' : 'Không'}"/></dd></div>
                        <div class="detail-item"><dt>Điểm thưởng</dt><dd><c:out value="${activity.bonusPoint}" default="—"/></dd></div>
                        <div class="detail-item"><dt>Điểm phạt</dt><dd><c:out value="${activity.penaltyPoint}" default="—"/></dd></div>
                        <div class="detail-item field-wide"><dt>Địa điểm</dt><dd><c:out value="${activity.address}" default="Chưa cập nhật"/></dd></div>
                        <div class="detail-item field-wide"><dt>Mô tả</dt><dd class="activity-detail-description"><c:out value="${activity.description}" default="Chưa có mô tả"/></dd></div>
                    </dl>
                </section>
            </div>

            <!-- TAB 2: THÔNG TIN NHÂN SỰ PHỤ TRÁCH -->
            <div id="tab-staff" class="activity-tab-pane" role="tabpanel" aria-labelledby="btn-tab-staff">
                <section class="activity-detail-section" style="grid-column: 1 / -1;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                        <div>
                            <h3 style="margin: 0;">Danh sách nhân sự phụ trách</h3>
                            <p style="margin: 0.25rem 0 0; font-size: 0.875rem; color: var(--text-muted, #666);">
                                Các nhân sự thuộc phòng ban phụ trách (<c:out value="${activity.responsibleDepartmentName}" default="${activity.responsibleDepartmentCode}"/>) tham gia điều phối hoạt động.
                            </p>
                        </div>
                        <span class="activity-count">
                            <c:choose>
                                <c:when test="${empty assignedStaffs}">0 nhân sự</c:when>
                                <c:otherwise><c:out value="${assignedStaffs.size()}"/> nhân sự</c:otherwise>
                            </c:choose>
                        </span>
                    </div>

                    <div class="table-wrap activity-table-wrap" style="margin-bottom: 1.5rem;">
                        <table>
                            <thead>
                            <tr>
                                <th scope="col" style="width: 60px;">STT</th>
                                <th scope="col">Mã nhân sự</th>
                                <th scope="col">Họ và tên</th>
                                <th scope="col">Trạng thái nhân sự</th>
                                <th scope="col">Chức vụ</th>
                                <th scope="col">Thời gian phân công</th>
                                <c:if test="${canRemoveStaff}">
                                    <th scope="col" style="text-align: right; width: 120px;">Thao tác</th>
                                </c:if>
                            </tr>
                            </thead>
                            <tbody>
                            <c:choose>
                                <c:when test="${empty assignedStaffs}">
                                    <tr>
                                        <td colspan="${canRemoveStaff ? 7 : 6}" style="text-align: center; padding: 1.5rem; color: var(--text-muted, #888);">
                                            Chưa có nhân sự nào được phân công phụ trách hoạt động này.
                                        </td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${assignedStaffs}" var="ast" varStatus="loop">
                                        <tr>
                                            <td>${loop.index + 1}</td>
                                            <td><strong><c:out value="${ast.staffCode}"/></strong></td>
                                            <td><c:out value="${ast.staffName}"/></td>
                                            <td><span class="badge status-pill ${st:css(ast.staffStatus)}">${st:label(ast.staffStatus)}</span></td>
                                            <td><c:out value="${ast.position}" default="Nhân viên"/></td>
                                            <td><c:out value="${ast.formattedAssignedOn}"/></td>
                                            <c:if test="${canRemoveStaff}">
                                                <td style="text-align: right;">
                                                    <form action="${pageContext.request.contextPath}/activities" method="post" style="display:inline"
                                                      onsubmit="return confirm('Bạn có chắc chắn muốn hủy phân công nhân sự này?');">
                                                    <input type="hidden" name="action" value="unassignStaff">
                                                    <input type="hidden" name="activityId" value="${activity.id}">
                                                    <input type="hidden" name="staffId" value="${ast.staffId}">
                                                    <button type="submit" class="button" style="padding: 0.25rem 0.5rem; font-size: 0.8125rem; color: #dc2626; border-color: #fca5a5;">
                                                        Gỡ bỏ
                                                    </button>
                                                    </form>
                                                </td>
                                            </c:if>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                            </tbody>
                        </table>
                    </div>

                    <c:if test="${canAddStaff}">
                        <div style="background: var(--surface-secondary, #f8fafc); border: 1px solid var(--border, #e2e8f0); border-radius: 8px; padding: 1.25rem;">
                            <h4 style="margin: 0 0 0.5rem; font-size: 1rem;">Phân công nhân sự mới</h4>
                            <p style="margin: 0 0 1rem; font-size: 0.875rem; color: var(--text-muted, #64748b);">
                                Chỉ nhân sự thuộc phòng ban phụ trách (<strong><c:out value="${activity.responsibleDepartmentName}" default="${activity.responsibleDepartmentCode}"/></strong>) mới có thể được phân công.
                            </p>
                            <c:choose>
                                <c:when test="${not empty availableStaffs}">
                                    <form action="${pageContext.request.contextPath}/activities" method="post" style="display: flex; gap: 0.75rem; align-items: center; flex-wrap: wrap;">
                                        <input type="hidden" name="action" value="assignStaff">
                                        <input type="hidden" name="activityId" value="${activity.id}">
                                        <div style="flex: 1; min-width: 260px;">
                                            <select name="staffId" required class="button" style="width: 100%; text-align: left; padding: 0.5rem 0.75rem; background: #fff;">
                                                <option value="">-- Chọn nhân sự để phân công --</option>
                                                <c:forEach items="${availableStaffs}" var="st">
                                                    <option value="${st.id}">
                                                    <c:out value="${st.code}"/> - <c:out value="${st.name}"/>
                                                    </option>
                                                </c:forEach>
                                            </select>
                                        </div>
                                        <button type="submit" class="button button-primary">
                                            + Phân công vào hoạt động
                                        </button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <p style="margin: 0; font-style: italic; font-size: 0.875rem; color: var(--text-muted, #64748b);">
                                        Không còn nhân sự nào thuộc phòng ban phụ trách chưa được phân công.
                                    </p>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:if>
                </section>
            </div>

            <!-- TAB 3: THÔNG TIN NHÂN SỰ ĐỐI TÁC PHỤ TRÁCH -->
            <div id="tab-partner" class="activity-tab-pane" role="tabpanel" aria-labelledby="btn-tab-partner">
                <section class="activity-detail-section" style="grid-column: 1 / -1;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                        <div>
                            <h3 style="margin: 0;">Danh sách nhân sự đối tác phụ trách</h3>
                            <p style="margin: 0.25rem 0 0; font-size: 0.875rem; color: var(--text-muted, #666);">
                                Nhân sự đối tác chính: <strong><c:out value="${activity.partnerStaffName}" default="Chưa có"/></strong>
                                — Công ty: <c:out value="${activity.partnerCompanyName}" default="Không có"/>
                            </p>
                        </div>
                        <span class="activity-count"><c:out value="${empty assignedPartnerStaffs ? 0 : assignedPartnerStaffs.size()}"/> nhân sự</span>
                    </div>

                    <div class="table-wrap activity-table-wrap" style="margin-bottom: 1.5rem;">
                        <table>
                            <thead>
                            <tr>
                                <th scope="col" style="width: 60px;">STT</th>
                                <th scope="col">Mã nhân sự</th>
                                <th scope="col">Họ và tên</th>
                                <th scope="col">Công ty</th>
                                <th scope="col">Chức vụ</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col">Thời gian phân công</th>
                                <c:if test="${canManagePartnerStaff}">
                                    <th scope="col" style="text-align: right; width: 120px;">Thao tác</th>
                                </c:if>
                            </tr>
                            </thead>
                            <tbody>
                            <c:choose>
                                <c:when test="${empty assignedPartnerStaffs}">
                                    <tr>
                                        <td colspan="${canManagePartnerStaff ? 8 : 7}" style="text-align: center; padding: 1.5rem; color: var(--text-muted, #888);">
                                            Chưa có nhân sự đối tác nào được phân công phụ trách hoạt động này.
                                        </td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${assignedPartnerStaffs}" var="aps" varStatus="loop">
                                        <tr>
                                            <td>${loop.index + 1}</td>
                                            <td><strong><c:out value="${aps.partnerStaffCode}"/></strong></td>
                                            <td><c:out value="${aps.partnerStaffName}"/></td>
                                            <td><c:out value="${aps.companyName}" default="—"/></td>
                                            <td><c:out value="${aps.position}" default="—"/></td>
                                            <td><span class="badge status-pill ${st:css(aps.status)}">${st:label(aps.status)}</span></td>
                                            <td><c:out value="${aps.formattedAssignedOn}"/></td>
                                            <c:if test="${canManagePartnerStaff}">
                                                <td style="text-align: right;">
                                                    <form action="${pageContext.request.contextPath}/activities" method="post" style="display:inline"
                                                          onsubmit="return confirm('Bạn có chắc chắn muốn gỡ nhân sự đối tác này?');">
                                                        <input type="hidden" name="action" value="unassignPartnerStaff">
                                                        <input type="hidden" name="activityId" value="${activity.id}">
                                                        <input type="hidden" name="partnerStaffId" value="${aps.partnerStaffId}">
                                                        <button type="submit" class="button" style="padding: 0.25rem 0.5rem; font-size: 0.8125rem; color: #dc2626; border-color: #fca5a5;">
                                                            Gỡ bỏ
                                                        </button>
                                                    </form>
                                                </td>
                                            </c:if>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                            </tbody>
                        </table>
                    </div>

                    <c:if test="${canManagePartnerStaff}">
                        <div style="background: var(--surface-secondary, #f8fafc); border: 1px solid var(--border, #e2e8f0); border-radius: 8px; padding: 1.25rem;">
                            <h4 style="margin: 0 0 0.5rem; font-size: 1rem;">Phân công nhân sự đối tác mới</h4>
                            <p style="margin: 0 0 1rem; font-size: 0.875rem; color: var(--text-muted, #64748b);">
                                Chỉ nhân sự thuộc công ty đối tác của hoạt động (<strong><c:out value="${activity.partnerCompanyName}" default="—"/></strong>) mới có thể được phân công; không bao gồm nhân sự đối tác phụ trách chính.
                            </p>
                            <c:choose>
                                <c:when test="${not empty availablePartnerStaffs}">
                                    <form action="${pageContext.request.contextPath}/activities" method="post" style="display: flex; gap: 0.75rem; align-items: center; flex-wrap: wrap;">
                                        <input type="hidden" name="action" value="assignPartnerStaff">
                                        <input type="hidden" name="activityId" value="${activity.id}">
                                        <div style="flex: 1; min-width: 260px;">
                                            <select name="partnerStaffId" required class="button" style="width: 100%; text-align: left; padding: 0.5rem 0.75rem; background: #fff;">
                                                <option value="">-- Chọn nhân sự đối tác để phân công --</option>
                                                <c:forEach items="${availablePartnerStaffs}" var="pst">
                                                    <option value="${pst.id}"><c:out value="${pst.code}"/> - <c:out value="${pst.name}"/></option>
                                                </c:forEach>
                                            </select>
                                        </div>
                                        <button type="submit" class="button button-primary">+ Phân công vào hoạt động</button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <p style="margin: 0; font-style: italic; font-size: 0.875rem; color: var(--text-muted, #64748b);">
                                        Không còn nhân sự đối tác nào có thể được phân công.
                                    </p>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:if>
                </section>
            </div>
        </div>

        <div class="detail-actions activity-detail-actions">
            <a class="button" href="${pageContext.request.contextPath}/activities">Đóng</a>

            <%-- DRAFT / REJECTED: gửi duyệt --%>
            <c:if test="${activity.approvalStatus == 'DRAFT' || activity.approvalStatus == 'REJECTED'}">
                <form action="${pageContext.request.contextPath}/activity/approval" method="post" style="display:inline">
                    <input type="hidden" name="id" value="${activity.id}">
                    <button type="submit" name="action" value="submit" class="button button-primary">Gửi duyệt</button>
                </form>
            </c:if>

            <%-- PENDING: duyệt / từ chối --%>
            <c:if test="${canApproveActivities && activity.approvalStatus == 'PENDING'}">
                <form action="${pageContext.request.contextPath}/activity/approval" method="post" style="display:inline">
                    <input type="hidden" name="id" value="${activity.id}">
                    <button type="submit" name="action" value="approve" class="button button-primary">Duyệt</button>
                    <button type="submit" name="action" value="reject" class="button">Từ chối</button>
                </form>
            </c:if>

            <%-- APPROVED (chỉ khi hoạt động chưa diễn ra): hủy duyệt --%>
            <c:if test="${canApproveActivities && activity.approvalStatus == 'APPROVED' && activity.activityStatus == 'UPCOMING'}">
                <form action="${pageContext.request.contextPath}/activity/approval" method="post" style="display:inline">
                    <input type="hidden" name="id" value="${activity.id}">
                    <button type="submit" name="action" value="revokeApproval" class="button"
                            onclick="return confirm('Hủy duyệt và đưa hoạt động về trạng thái chờ duyệt?');">Hủy duyệt</button>
                </form>
            </c:if>

            <%-- REJECTED: hủy từ chối --%>
            <c:if test="${canApproveActivities && activity.approvalStatus == 'REJECTED'}">
                <form action="${pageContext.request.contextPath}/activity/approval" method="post" style="display:inline">
                    <input type="hidden" name="id" value="${activity.id}">
                    <button type="submit" name="action" value="revokeRejection" class="button"
                            onclick="return confirm('Hủy từ chối và đưa hoạt động về trạng thái chờ duyệt?');">Hủy từ chối</button>
                </form>
            </c:if>
        </div>
    </section>
</main>
<script>
    (function () {
        var tabBtns = document.querySelectorAll('.activity-tab-btn');
        var tabPanes = document.querySelectorAll('.activity-tab-pane');

        function switchTab(targetId) {
            tabBtns.forEach(function (btn) {
                var isMatch = btn.getAttribute('data-tab') === targetId;
                btn.classList.toggle('active', isMatch);
                btn.setAttribute('aria-selected', isMatch ? 'true' : 'false');
            });
            tabPanes.forEach(function (pane) {
                pane.classList.toggle('active', pane.id === targetId);
            });
        }

        tabBtns.forEach(function (btn) {
            btn.addEventListener('click', function () {
                var tabId = this.getAttribute('data-tab');
                switchTab(tabId);
                if (window.history && window.history.replaceState) {
                    window.history.replaceState(null, null, '#' + tabId);
                }
            });
        });

        // Tự động mở tab dựa trên hash URL (ví dụ #tab-staff khi vừa submit xong phân công)
        var currentHash = window.location.hash.replace('#', '');
        if (currentHash === 'tab-staff' || currentHash === 'tab-info' || currentHash === 'tab-partner') {
            switchTab(currentHash);
        }
    })();
</script>
</body>
</html>
