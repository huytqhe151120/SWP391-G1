<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="staff-profile" scope="request"/>
<c:set var="rcTitle" value="Thông tin cá nhân" scope="request"/>
<c:set var="rcHero" value="Thông tin cá nhân" scope="request"/>
<c:set var="rcEyebrow" value="EAMS / CÁ NHÂN" scope="request"/>
<c:set var="rcSubtitle" value="Xem thông tin cá nhân của bạn." scope="request"/>
<c:set var="rcAction" scope="request">
    <a class="button profile-edit-button"
       href="${pageContext.request.contextPath}/staff-profile?action=edit">
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
        <div class="rc-field"><label>Họ và tên</label><input value="<c:out value='${staff.name}'/>" readonly></div>
        <div class="rc-field"><label>Ngày sinh</label><input value="<c:out value='${staff.dob}'/>" readonly></div>
        <div class="rc-field">
            <label>Giới tính</label>
            <c:choose>
                <c:when test="${empty staff.gender}">
                    <input value="Chưa cập nhật" readonly>
                </c:when>
                <c:when test="${staff.gender eq 'true'}">
                    <input value="Nam" readonly>
                </c:when>
                <c:otherwise>
                    <input value="Nữ" readonly>
                </c:otherwise>
            </c:choose>
        </div>
        <div class="rc-field"><label>Trạng thái</label><input value="<c:out value='${staff.status}'/>" readonly></div>
        <div class="rc-field">
            <label>Đơn vị công tác</label>

            <c:choose>
                <c:when test="${empty staffDepartmentNames}">
                    <input value="Chưa phân công" readonly>
                </c:when>

                <c:otherwise>
                    <c:forEach items="${staffDepartmentNames}" var="departmentName">
                        <input value="<c:out value='${departmentName}'/>"
                               readonly>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</section>
<section class="card rc-card">
    <div class="activity-table-heading">
        <div>
            <h2>Thông tin tài khoản</h2>
            <p>Thông tin đăng nhập và vai trò trong hệ thống</p>
        </div>
    </div>

    <div class="rc-grid">
        <div class="rc-field">
            <label>Tên đăng nhập</label>
            <input value="<c:out value='${profileAccount.username}'/>"
                   readonly>
        </div>

        <div class="rc-field">
            <label>Loại tài khoản</label>
            <c:choose>
                <c:when test="${profileAccount.type eq 'STUDENT'}">
                    <input value="Sinh viên" readonly>
                </c:when>
                <c:when test="${profileAccount.type eq 'STAFF'}">
                    <input value="Nhân viên" readonly>
                </c:when>
                <c:when test="${profileAccount.type eq 'PARTNER_STAFF'}">
                    <input value="Nhân viên đối tác" readonly>
                </c:when>
                <c:when test="${profileAccount.type eq 'ADMIN'}">
                    <input value="Quản trị viên" readonly>
                </c:when>
                <c:otherwise>
                    <input value="<c:out value='${profileAccount.type}'/>"
                           readonly>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="rc-field">
            <label>Vai trò trong hệ thống</label>
            <input value="<c:out value='${profileAccount.roleLabel}'/>"
                   readonly>
        </div>
    </div>
</section>

<section class="card rc-card">
    <div class="activity-table-heading">
        <div>
            <h2>Thông tin tài khoản</h2>
            <p>Thông tin đăng nhập và vai trò trong hệ thống</p>
        </div>
    </div>

    <div class="rc-grid">
        <div class="rc-field">
            <label>Tên đăng nhập</label>
            <input value="<c:out value='${profileAccount.username}'/>"
                   readonly>
        </div>

        <div class="rc-field">
            <label>Loại tài khoản</label>
            <c:choose>
                <c:when test="${profileAccount.type eq 'STUDENT'}">
                    <input value="Sinh viên" readonly>
                </c:when>
                <c:when test="${profileAccount.type eq 'STAFF'}">
                    <input value="Nhân viên" readonly>
                </c:when>
                <c:when test="${profileAccount.type eq 'PARTNER_STAFF'}">
                    <input value="Nhân viên đối tác" readonly>
                </c:when>
                <c:when test="${profileAccount.type eq 'ADMIN'}">
                    <input value="Quản trị viên" readonly>
                </c:when>
                <c:otherwise>
                    <input value="<c:out value='${profileAccount.type}'/>"
                           readonly>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="rc-field">
            <label>Vai trò trong hệ thống</label>
            <input value="<c:out value='${profileAccount.roleLabel}'/>"
                   readonly>
        </div>
    </div>
</section>

<%@ include file="../recruitment/_bottom.jspf" %>
