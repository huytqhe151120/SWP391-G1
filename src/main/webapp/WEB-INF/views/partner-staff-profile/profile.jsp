<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="partner-staff-profile" scope="request"/>
<c:set var="rcTitle" value="Thông tin cá nhân" scope="request"/>
<c:set var="rcHero" value="Thông tin cá nhân" scope="request"/>
<c:set var="rcEyebrow" value="EAMS / CÁ NHÂN" scope="request"/>
<c:set var="rcSubtitle" value="Xem thông tin cá nhân của bạn." scope="request"/>
<c:set var="rcAction" scope="request">
    <a class="button profile-edit-button"
       href="${pageContext.request.contextPath}/partner-staff-profile?action=edit">
        Cập nhật hồ sơ
    </a>
</c:set>
<%@ include file="../recruitment/_top.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<section class="card rc-card">
    <div class="activity-table-heading">
        <div>
            <h2>Thông tin cá nhân</h2>
            <p>Chi tiết thông tin hồ sơ của bạn</p>
        </div>
    </div>
    <div class="rc-grid">
        <div class="rc-field"><label>Mã nhân viên</label><input value="<c:out value='${staff.code}'/>" readonly></div>
        <div class="rc-field"><label>Công ty</label><input value="<c:out value='${staff.companyName}'/>" readonly></div>
        <div class="rc-field"><label>Họ và tên</label><input value="<c:out value='${staff.name}'/>" readonly></div>
        <div class="rc-field"><label>Ngày sinh</label><input value="<c:out value='${staff.dob}'/>" readonly></div>
        <div class="rc-field"><label>Giới tính</label><input value="<c:out value="${staff.gender eq true ? 'Nam' : staff.gender eq false ? 'Nữ' : 'Khác'}"/>" readonly></div>
        <div class="rc-field"><label>Vị trí</label><input value="<c:out value='${staff.position}'/>" readonly></div>
        <div class="rc-field"><label>Trạng thái</label><input value="<c:out value='${staff.status}'/>" readonly></div>
    </div>
</section>

<%@ include file="../recruitment/_bottom.jspf" %>
