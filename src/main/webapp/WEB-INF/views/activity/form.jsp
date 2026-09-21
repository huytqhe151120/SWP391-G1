<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><title>Activity form</title></head>
<body>
<c:set var="editing" value="${not empty activity}"/>
<h1><c:choose><c:when test="${editing}">Sửa activity</c:when><c:otherwise>Thêm activity</c:otherwise></c:choose></h1>
<form action="${pageContext.request.contextPath}/activities" method="post">
    <input type="hidden" name="action" value="${editing ? 'update' : 'create'}">
    <c:if test="${editing}"><input type="hidden" name="id" value="${activity.id}"></c:if>
    <label>Semester ID <input type="number" name="semesterId" min="1" required value="<c:out value='${activity.semesterId}'/>" ></label><br>
    <label>Activity type <input name="activityType" required value="<c:out value='${activity.activityType}'/>" ></label><br>
    <label>Code <input name="code" required value="<c:out value='${activity.code}'/>" ></label><br>
    <label>Tên <input name="name" required value="<c:out value='${activity.name}'/>" ></label><br>
    <label>Department ID <input type="number" name="responsibleDepartmentId" min="1" required value="<c:out value='${activity.responsibleDepartmentId}'/>" ></label><br>
    <label>Responsible staff ID <input type="number" name="responsibleStaffId" min="1" required value="<c:out value='${activity.responsibleStaffId}'/>" ></label><br>
    <label>Partner
        <select id="partnerId" name="partnerId" required>
            <option value="">-- Chọn partner --</option>
            <c:forEach var="partner" items="${partners}">
                <c:choose>
                    <c:when test="${partner.id == activity.partnerId}">
                        <option value="${partner.id}" selected><c:out value="${partner.name}"/></option>
                    </c:when>
                    <c:otherwise>
                        <option value="${partner.id}"><c:out value="${partner.name}"/></option>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
        </select>
    </label><br>
    <label>Partner staff
        <select id="partnerStaffId" name="partnerStaffId" required>
            <option value="">-- Chọn partner staff --</option>
            <c:forEach var="staff" items="${partnerStaffOptions}">
                <c:choose>
                    <c:when test="${staff.id == activity.partnerStaffId}">
                        <option value="${staff.id}" selected><c:out value="${staff.fullName}"/></option>
                    </c:when>
                    <c:otherwise>
                        <option value="${staff.id}"><c:out value="${staff.fullName}"/></option>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
        </select>
    </label><br>
    <label>Điểm cộng <input type="number" step="any" min="0" name="bonusPoint" required value="<c:out value='${activity.bonusPoint}'/>" ></label><br>
    <label>Điểm trừ <input type="number" step="any" min="0" name="penaltyPoint" required value="<c:out value='${activity.penaltyPoint}'/>" ></label><br>
    <label>Địa chỉ <input name="address" value="<c:out value='${activity.address}'/>" ></label><br>
    <label>Mô tả <textarea name="description"><c:out value="${activity.description}"/></textarea></label><br>
    <label>Activity status <input name="activityStatus" required value="<c:out value='${activity.activityStatus}'/>" ></label><br>
    <label>Approval status <input name="approvalStatus" required value="<c:out value='${activity.approvalStatus}'/>" ></label><br>
    <button type="submit">Lưu</button>
</form>
<a href="${pageContext.request.contextPath}/activities">Hủy</a>
    <script>
        const partnerSelect = document.getElementById('partnerId');
        const staffSelect = document.getElementById('partnerStaffId');
        const contextPath = '${pageContext.request.contextPath}';

        partnerSelect.addEventListener('change', async function () {
            const partnerId = this.value;
            staffSelect.innerHTML = '<option value="">-- Chọn partner staff --</option>';
            if (!partnerId) {
                return;
            }

            const response = await fetch(contextPath + '/partner-staff?action=options&partnerId=' + partnerId);
            if (!response.ok) {
                return;
            }
            const staffList = await response.json();
            staffList.forEach(function (staff) {
                const option = document.createElement('option');
                option.value = staff.id;
                option.textContent = staff.fullName + (staff.position ? ' - ' + staff.position : '');
                staffSelect.appendChild(option);
            });
        });
    </script>
</body></html>