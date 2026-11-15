<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="rcTitle" value="Ứng tuyển ban tổ chức" scope="request"/>
<c:set var="rcHero" value="Ứng tuyển ban tổ chức" scope="request"/>
<c:set var="rcEyebrow" value="EAMS / TUYỂN BAN TỔ CHỨC" scope="request"/>
<c:set var="rcSubtitle" value="Nộp hồ sơ ứng tuyển vào ban tổ chức của hoạt động." scope="request"/>
<c:set var="rcAction" value="<a class=\"button button-outline\" href=\"${pageContext.request.contextPath}/organizer-recruitment\">Quay lại danh sách</a>" scope="request"/>
<%@ include file="_top.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<section class="card rc-card rc-summary">
    <div>
        <span class="badge status-pill status-green">Đang mở tuyển</span>
        <span class="badge status-pill status-blue">Ban tổ chức</span>
    </div>
    <dl>
        <div><dt>Hoạt động</dt><dd><c:out value="${round.activityName}"/></dd></div>
        <div><dt>Hạn nộp hồ sơ</dt><dd><c:choose><c:when test="${not empty round.closeAt}"><c:out value="${dt:format(round.closeAt)}"/></c:when><c:otherwise>Không giới hạn</c:otherwise></c:choose></dd></div>
        <div><dt>Chỉ tiêu còn lại</dt><dd>${round.totalRemaining} / ${round.totalQuota}</dd></div>
    </dl>
    <c:if test="${not empty round.description}"><p class="rc-sub"><c:out value="${round.description}"/></p></c:if>
</section>

<form class="card rc-card" method="post" action="${ctx}/organizer-recruitment">
    <input type="hidden" name="action" value="apply">
    <input type="hidden" name="roundId" value="${round.id}">
    <p class="rc-sub">Các mục có dấu * là bắt buộc. Bạn có thể rút hồ sơ trước khi được mời phỏng vấn.</p>

    <div class="rc-step-title"><span class="rc-step-no">1</span>
        <div><h3>Thông tin sinh viên</h3><small>Lấy từ hồ sơ của bạn, không chỉnh sửa tại đây</small></div></div>
    <div class="rc-grid">
        <div class="rc-field"><label>Mã sinh viên</label><input value="<c:out value='${student.code}'/>" readonly></div>
        <div class="rc-field"><label>Họ và tên</label><input value="<c:out value='${student.name}'/>" readonly></div>
        <div class="rc-field"><label>Lớp</label><input value="<c:out value='${student.mainClassCode}'/>" readonly></div>
        <div class="rc-field"><label>Khóa</label><input value="<c:out value='${cohort}'/>" readonly></div>
        <div class="rc-field"><label>Email</label><input value="<c:out value='${student.email}'/>" readonly></div>
        <div class="rc-field"><label>Số điện thoại</label><input value="<c:out value='${student.phoneNumber}'/>" readonly></div>
        <div class="rc-field"><label>Ngày sinh</label><input value="<c:out value='${student.dob}'/>" readonly></div>
    </div>

    <div class="rc-step-title"><span class="rc-step-no">2</span>
        <div><h3>Vị trí ứng tuyển</h3><small>Chọn tối đa 2 nguyện vọng, xếp theo thứ tự ưu tiên</small></div></div>
    <div class="rc-grid">
        <div class="rc-field"><label for="wish1">Nguyện vọng 1 <em>*</em></label>
            <select id="wish1" name="wish1" required>
                <option value="">Chọn một vị trí</option>
                <c:forEach items="${round.positions}" var="p">
                    <option value="${p.id}" ${p.remaining == 0 ? 'disabled' : ''} ${param.wish1 eq p.id ? 'selected' : ''}><c:out value="${p.name}"/> (còn ${p.remaining})</option>
                </c:forEach>
            </select></div>
        <div class="rc-field"><label for="wish2">Nguyện vọng 2 <span>(không bắt buộc)</span></label>
            <select id="wish2" name="wish2">
                <option value="">Không chọn</option>
                <c:forEach items="${round.positions}" var="p">
                    <option value="${p.id}" ${p.remaining == 0 ? 'disabled' : ''} ${param.wish2 eq p.id ? 'selected' : ''}><c:out value="${p.name}"/> (còn ${p.remaining})</option>
                </c:forEach>
            </select></div>
    </div>
    <div class="rc-slot-list">
        <c:forEach items="${round.positions}" var="p">
            <div><c:out value="${p.name}"/><span>Còn ${p.remaining} / ${p.quota}</span></div>
        </c:forEach>
    </div>

    <c:if test="${not empty round.questions}">
        <div class="rc-step-title"><span class="rc-step-no">3</span>
            <div><h3>Câu hỏi của ban tổ chức</h3><small>Do ban tổ chức đặt cho đợt tuyển này</small></div></div>
        <c:forEach items="${round.questions}" var="q" varStatus="st">
            <c:set var="given" value="${answerMap[q.id]}"/>
            <div class="rc-field full" ${st.first ? '' : 'style="margin-top:14px"'}>
                <label><c:out value="${q.questionText}"/> <c:choose><c:when test="${q.required}"><em>*</em></c:when><c:otherwise><span>(không bắt buộc)</span></c:otherwise></c:choose></label>
                <c:choose>
                    <c:when test="${q.questionType eq 'DROPDOWN'}">
                        <select name="q_${q.id}" ${q.required ? 'required' : ''}>
                            <option value="">Chọn một đáp án</option>
                            <c:forEach items="${q.optionList}" var="o">
                                <option value="<c:out value='${o}'/>" ${not empty given and given.contains(o) ? 'selected' : ''}><c:out value="${o}"/></option>
                            </c:forEach>
                        </select>
                    </c:when>
                    <c:when test="${q.questionType eq 'MULTI_CHOICE'}">
                        <div class="rc-checks">
                            <c:forEach items="${q.optionList}" var="o">
                                <label><input type="checkbox" name="q_${q.id}" value="<c:out value='${o}'/>" ${not empty given and given.contains(o) ? 'checked' : ''}> <c:out value="${o}"/></label>
                            </c:forEach>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <textarea name="q_${q.id}" maxlength="4000" placeholder="Nhập câu trả lời của bạn..." ${q.required ? 'required' : ''}><c:if test="${not empty given}"><c:out value="${given[0]}"/></c:if></textarea>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:forEach>
    </c:if>

    <div class="rc-step-title"><span class="rc-step-no">${empty round.questions ? 3 : 4}</span>
        <div><h3>Ghi chú</h3><small>Không bắt buộc</small></div></div>
    <div class="rc-field full"><label>Ghi chú thêm <span>(không bắt buộc)</span></label>
        <textarea name="note" maxlength="1000" placeholder="Điều bạn muốn nhắn thêm tới ban tổ chức..."><c:out value="${param.note}"/></textarea></div>

    <c:if test="${hasParticipation}">
        <div class="rc-alert">Bạn đang có đăng ký tham gia ở hoạt động này. Nếu trúng tuyển ban tổ chức, đăng ký tham gia đó sẽ được hủy.</div>
    </c:if>

    <div class="rc-actions">
        <a class="button" href="${ctx}/organizer-recruitment">Hủy</a>
        <button type="submit" class="button button-primary">Nộp hồ sơ</button>
    </div>
</form>
<script>
    (function () {
        var w1 = document.getElementById('wish1'), w2 = document.getElementById('wish2');
        function sync() {
            Array.prototype.forEach.call(w2.options, function (o) {
                o.disabled = o.dataset.full === '1' || (o.value !== '' && o.value === w1.value);
            });
            if (w2.value === w1.value) { w2.value = ''; }
        }
        Array.prototype.forEach.call(w2.options, function (o) { if (o.disabled) { o.dataset.full = '1'; } });
        w1.addEventListener('change', sync);
        sync();
    })();
</script>
<%@ include file="_bottom.jspf" %>
