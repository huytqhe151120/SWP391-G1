<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="student-profile-edit" scope="request"/>
<c:set var="rcTitle" value="Cập nhật hồ sơ" scope="request"/>
<c:set var="rcHero" value="Cập nhật hồ sơ" scope="request"/>
<c:set var="rcEyebrow" value="EAMS / CÁ NHÂN" scope="request"/>
<c:set var="rcSubtitle" value="Cập nhật thông tin cá nhân của bạn." scope="request"/>
<%@ include file="../recruitment/_top.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<c:if test="${not empty message}">
    <c:choose>
        <c:when test="${messageType eq 'success'}">
            <div class="alert alert-success activity-alert"
                 role="status">
                <c:out value="${message}"/>
            </div>
        </c:when>

        <c:otherwise>
            <div class="alert alert-error activity-alert"
                 role="alert">
                <c:out value="${message}"/>
            </div>
        </c:otherwise>
    </c:choose>
</c:if>

<form class="card rc-card" method="post" action="${ctx}/student-profile">
    <input type="hidden" name="action" value="update">
    
    <div class="rc-step-title"><span class="rc-step-no">1</span>
        <div><h3>Thông tin cá nhân</h3><small>Cập nhật thông tin của bạn</small></div></div>
    
    <div class="rc-grid">
        <div class="rc-field"><label>Mã sinh viên</label><input value="<c:out value='${student.code}'/>" readonly></div>
        <div class="rc-field"><label>Lớp</label><input value="<c:out value='${student.mainClassCode}'/>" readonly></div>
        <div class="rc-field"><label>Họ và tên <em>*</em></label><input name="name" maxlength="100" value="<c:out value='${student.name}'/>" required></div>
        <div class="rc-field"><label>Ngày sinh <em>*</em></label><input type="date" name="dob" value="<c:out value='${student.dob}'/>" required></div>
        <div class="rc-field">
            <label>Giới tính <em>*</em></label>
            <select name="gender" required>
                <option value="">-- Chọn giới tính --</option>
                <option value="true"
                ${student.gender eq 'true' ? 'selected' : ''}>
                    Nam
                </option>
                <option value="false"
                ${student.gender eq 'false' ? 'selected' : ''}>
                    Nữ
                </option>
            </select>
        </div>
        <div class="rc-field"><label>Email <em>*</em></label><input type="email" name="email" maxlength="100" value="<c:out value='${student.email}'/>" required></div>
        <div class="rc-field"><label>Số điện thoại <em>*</em></label><input type="tel" name="phoneNumber" maxlength="15" value="<c:out value='${student.phoneNumber}'/>" required></div>
    </div>

    <div class="rc-step-title"><span class="rc-step-no">2</span>
        <div><h3>Thay đổi mật khẩu</h3><small>Để trống nếu không muốn thay đổi</small></div></div>
    
    <div class="rc-grid">
        <div class="rc-field"><label>Mật khẩu mới</label><input type="password" name="newPassword" maxlength="255" placeholder="Nhập mật khẩu mới (tùy chọn)"></div>
        <div class="rc-field"><label>Xác nhận mật khẩu mới</label><input type="password" name="confirmPassword" maxlength="255" placeholder="Nhập lại mật khẩu mới"></div>
    </div>

    <div class="rc-actions">
        <a class="button" href="${ctx}/student-profile">Hủy</a>
        <button type="submit" class="button button-primary">Lưu thay đổi</button>
    </div>
</form>

<%@ include file="../recruitment/_bottom.jspf" %>
