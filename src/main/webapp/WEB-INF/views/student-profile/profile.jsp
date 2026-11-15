<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="student-profile" scope="request"/>
<c:set var="rcTitle" value="Hồ sơ cá nhân" scope="request"/>
<c:set var="rcHero" value="Hồ sơ cá nhân" scope="request"/>
<c:set var="rcEyebrow" value="EAMS / CÁ NHÂN" scope="request"/>
<c:set var="rcSubtitle" value="Xem thông tin cá nhân của bạn." scope="request"/>
<%@ include file="../recruitment/_top.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<section class="card rc-card">
    <div class="rc-profile-header">
        <div class="rc-profile-avatar">
            <c:out value="${student.name != null ? student.name.substring(0, 1).toUpperCase() : 'S'}"/>
        </div>
        <div class="rc-profile-info">
            <h2><c:out value="${student.name}"/></h2>
            <p><c:out value="${student.code}"/></p>
        </div>
        <a class="button button-outline" href="${ctx}/student-profile?action=edit">Chỉnh sửa hồ sơ</a>
    </div>
</section>

<section class="card rc-card">
    <h3>Thông tin cá nhân</h3>
    <div class="rc-grid">
        <div class="rc-field"><label>Mã sinh viên</label><input value="<c:out value='${student.code}'/>" readonly></div>
        <div class="rc-field"><label>Họ và tên</label><input value="<c:out value='${student.name}'/>" readonly></div>
        <div class="rc-field"><label>Ngày sinh</label><input value="<c:out value='${student.dob}'/>" readonly></div>
        <div class="rc-field"><label>Giới tính</label><input value="<c:out value='${student.gender}'/>" readonly></div>
        <div class="rc-field"><label>Lớp</label><input value="<c:out value='${student.mainClassCode}'/>" readonly></div>
        <div class="rc-field"><label>Email</label><input value="<c:out value='${student.email}'/>" readonly></div>
        <div class="rc-field"><label>Số điện thoại</label><input value="<c:out value='${student.phoneNumber}'/>" readonly></div>
        <div class="rc-field"><label>Trạng thái</label><input value="<c:out value='${student.status}'/>" readonly></div>
    </div>
</section>

<%@ include file="../recruitment/_bottom.jspf" %>
