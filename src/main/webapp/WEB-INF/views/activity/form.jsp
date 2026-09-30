<%@ page pageEncoding="UTF-8" %>
<c:set var="semesterValue" value="${not empty formData ? formData.semesterId : activity.semesterId}"/>
<c:set var="typeValue" value="${not empty formData ? formData.activityTypeId : activity.activityTypeId}"/>
<c:set var="departmentValue" value="${not empty formData ? formData.responsibleDepartmentId : activity.responsibleDepartmentId}"/>
<c:set var="staffValue" value="${not empty formData ? formData.responsibleStaffId : activity.responsibleStaffId}"/>
<c:set var="companyValue" value="${not empty formData ? formData.partnerCompanyId : activity.partnerCompanyId}"/>
<c:set var="partnerStaffValue" value="${not empty formData ? formData.partnerStaffId : activity.partnerStaffId}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:choose><c:when test="${isEdit}">Cập nhật hoạt động</c:when><c:otherwise>Tạo hoạt động</c:otherwise></c:choose> | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>
<main class="page activity-list-page activity-form-page">
    <header class="activity-hero">
        <div class="activity-hero-copy">
            <span class="eyebrow">SWP391-G1 <span aria-hidden="true">/</span> QUẢN LÝ HOẠT ĐỘNG</span>
            <h1><c:choose><c:when test="${isEdit}">Cập nhật hoạt động</c:when><c:otherwise>Tạo hoạt động mới</c:otherwise></c:choose></h1>
            <p>Điền thông tin để quản lý và theo dõi hoạt động ngoại khóa.</p>
        </div>
        <a class="button activity-create-button" href="${pageContext.request.contextPath}/activities">
            <svg class="ui-icon" aria-hidden="true"><use href="${pageContext.request.contextPath}/assets/icons/ui-icons.svg#arrow-left"/></svg>
            Danh sách hoạt động
        </a>
    </header>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-error activity-alert"><c:out value="${errorMessage}"/></div>
    </c:if>

    <section class="card activity-form-card">
        <div class="activity-table-heading">
            <div>
                <h2>Thông tin hoạt động</h2>
                <p>Các trường có dấu <span class="required-mark">*</span> là bắt buộc</p>
            </div>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/activities">
            <input type="hidden" name="action" value="${isEdit ? 'update' : 'create'}">
            <c:if test="${isEdit}">
                <input type="hidden" name="id" value="${activity.id}">
            </c:if>

            <div class="activity-form-content">
            <div class="activity-form-section">
                <h3>Thông tin cơ bản</h3>
                <p>Thông tin nhận diện và phân loại hoạt động.</p>
            </div>
            <div class="form-grid activity-form-grid">
                <div class="field">
                    <label for="code">Mã hoạt động <span class="required-mark">*</span></label>
                    <input id="code" name="code" type="text" maxlength="50" required
                           value="<c:out value='${not empty formData ? formData.code : activity.code}'/>">
                </div>
                <div class="field">
                    <label for="name">Tên hoạt động <span class="required-mark">*</span></label>
                    <input id="name" name="name" type="text" maxlength="255" required
                           value="<c:out value='${not empty formData ? formData.name : activity.name}'/>">
                </div>
                <div class="field">
                    <label for="semesterId">Học kỳ <span class="required-mark">*</span></label>
                    <select id="semesterId" name="semesterId" required>
                        <option value="">-- Chọn học kỳ --</option>
                        <c:forEach var="semester" items="${semesters}">
                            <option value="${semester.id}" <c:if test="${semesterValue == semester.id}">selected</c:if>>
                                <c:out value="${semester.code}"/> - <c:out value="${semester.name}"/>
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="field">
                    <label for="activityTypeId">Loại hoạt động <span class="required-mark">*</span></label>
                    <select id="activityTypeId" name="activityTypeId" required>
                        <option value="">-- Chọn loại hoạt động --</option>
                        <c:forEach var="type" items="${activityTypes}">
                            <option value="${type.id}" <c:if test="${typeValue == type.id}">selected</c:if>>
                                <c:out value="${type.code}"/> - <c:out value="${type.name}"/>
                            </option>
                        </c:forEach>
                    </select>
                </div>
            </div>

            <div class="activity-form-section">
                <h3>Phụ trách và đối tác</h3>
                <p>Chọn đơn vị, nhân viên tổ chức và thông tin đối tác nếu có.</p>
            </div>
            <div class="form-grid activity-form-grid">
                <div class="field">
                    <label for="responsibleDepartmentId">Đơn vị phụ trách <span class="required-mark">*</span></label>
                    <select id="responsibleDepartmentId" name="responsibleDepartmentId" required>
                        <option value="">-- Chọn đơn vị --</option>
                        <c:forEach var="department" items="${departments}">
                            <option value="${department.id}" <c:if test="${departmentValue == department.id}">selected</c:if>>
                                <c:out value="${department.code}"/> - <c:out value="${department.name}"/>
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="field">
                    <label for="responsibleStaffId">Nhân viên phụ trách <span class="required-mark">*</span></label>
                    <select id="responsibleStaffId" name="responsibleStaffId" required>
                        <option value="">-- Chọn nhân viên --</option>
                        <c:forEach var="staff" items="${staffs}">
                            <option value="${staff.id}" <c:if test="${staffValue == staff.id}">selected</c:if>>
                                <c:out value="${staff.code}"/> - <c:out value="${staff.name}"/>
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="field">
                    <label for="partnerCompanyId">Công ty đối tác</label>
                    <select id="partnerCompanyId" name="partnerCompanyId">
                        <option value="">-- Không có --</option>
                        <c:forEach var="company" items="${partnerCompanies}">
                            <option value="${company.id}" <c:if test="${companyValue == company.id}">selected</c:if>>
                                <c:out value="${company.code}"/> - <c:out value="${company.name}"/>
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="field">
                    <label for="partnerStaffId">Nhân viên công ty đối tác</label>
                    <select id="partnerStaffId" name="partnerStaffId"
                            data-selected="<c:out value='${partnerStaffValue}'/>"
                            <c:if test="${empty companyValue or companyValue == 0}">disabled</c:if>>
                        <option value="">-- Không có --</option>
                        <c:forEach var="partnerStaff" items="${partnerStaffs}">
                            <option value="${partnerStaff.id}"
                                    <c:if test="${partnerStaffValue == partnerStaff.id}">selected</c:if>>
                                <c:out value="${partnerStaff.name}"/>
                            </option>
                        </c:forEach>
                    </select>
                    <span id="partnerStaffMessage" class="field-hint" aria-live="polite"></span>
                </div>
            </div>

            <div class="activity-form-section">
                <h3>Thời gian và địa điểm</h3>
                <p>Sắp xếp thời gian diễn ra và nơi tổ chức hoạt động.</p>
            </div>
            <div class="form-grid activity-form-grid">
                <div class="field">
                    <label for="startTime">Thời gian bắt đầu</label>
                    <input id="startTime" name="startTime" type="datetime-local" lang="en-GB" step="1"
                           value="<c:out value='${not empty formData ? formData.startTime : activity.startTime}'/>">
                </div>
                <div class="field">
                    <label for="endTime">Thời gian kết thúc</label>
                    <input id="endTime" name="endTime" type="datetime-local" lang="en-GB" step="1"
                           value="<c:out value='${not empty formData ? formData.endTime : activity.endTime}'/>">
                </div>
                <div class="field field-wide">
                    <label for="address">Địa điểm</label>
                    <input id="address" name="address" type="text" maxlength="500"
                           value="<c:out value='${not empty formData ? formData.address : activity.address}'/>">
                </div>
            </div>

            <div class="activity-form-section">
                <h3>Điểm và mô tả</h3>
                <p>Cấu hình điểm thưởng, điểm phạt và thông tin bổ sung.</p>
            </div>
            <div class="form-grid activity-form-grid">
                <div class="field">
                    <label for="bonusPoint">Điểm thưởng</label>
                    <input id="bonusPoint" name="bonusPoint" type="number" min="0" step="0.01"
                           placeholder="Ví dụ: 5"
                           value="<c:out value='${not empty formData ? formData.bonusPoint : activity.bonusPoint}'/>">
                </div>
                <div class="field">
                    <label for="penaltyPoint">Điểm phạt</label>
                    <input id="penaltyPoint" name="penaltyPoint" type="number" min="0" step="0.01"
                           placeholder="Ví dụ: 2"
                           value="<c:out value='${not empty formData ? formData.penaltyPoint : activity.penaltyPoint}'/>">
                </div>
                <div class="field field-wide">
                    <label for="description">Mô tả</label>
                    <textarea id="description" name="description"><c:out value="${not empty formData ? formData.description : activity.description}"/></textarea>
                </div>
            </div>
            </div>

            <div class="form-actions activity-form-actions">
                <a class="button activity-cancel-button" href="${pageContext.request.contextPath}/activities">Hủy</a>
                <button class="button button-primary" type="submit">
                    <c:choose><c:when test="${isEdit}">Lưu thay đổi</c:when><c:otherwise>Tạo hoạt động</c:otherwise></c:choose>
                </button>
            </div>
        </form>
    </section>
</main>
<script>
    (() => {
        const companySelect = document.getElementById("partnerCompanyId");
        const staffSelect = document.getElementById("partnerStaffId");
        const message = document.getElementById("partnerStaffMessage");
        let selectedStaffId = staffSelect.dataset.selected;
        const endpoint = "${pageContext.request.contextPath}/activities?action=getStaffsByCompany&companyId=";

        const loadPartnerStaff = async () => {
            const companyId = companySelect.value;
            staffSelect.replaceChildren(new Option("-- Không có --", ""));
            staffSelect.disabled = !companyId;
            message.textContent = "";
            if (!companyId) return;

            try {
                const response = await fetch(endpoint + encodeURIComponent(companyId));
                if (!response.ok) throw new Error("Không thể tải danh sách nhân viên đối tác.");
                const staffs = await response.json();
                staffs.forEach((staff) => {
                    const option = new Option(staff.name, staff.id);
                    if (String(staff.id) === selectedStaffId) option.selected = true;
                    staffSelect.add(option);
                });
            } catch (error) {
                message.textContent = error.message;
            }
        };

        companySelect.addEventListener("change", () => {
            selectedStaffId = "";
            loadPartnerStaff();
        });
        if (companySelect.value) loadPartnerStaff();
    })();
</script>
</body>
</html>
