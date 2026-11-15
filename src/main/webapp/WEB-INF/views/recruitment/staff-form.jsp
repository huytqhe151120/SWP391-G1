<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="rcTitle" value="${round.id > 0 ? 'Sửa đợt tuyển' : 'Tạo đợt tuyển'}" scope="request"/>
<c:set var="rcHero" value="${round.id > 0 ? 'Sửa đợt tuyển' : 'Tạo đợt tuyển'}" scope="request"/>
<c:set var="rcEyebrow" value="EAMS / QUẢN LÝ TUYỂN BAN TỔ CHỨC" scope="request"/>
<c:set var="rcSubtitle" value="${round.id > 0 ? 'Chỉnh sửa thông tin đợt tuyển ban tổ chức.' : 'Tạo đợt tuyển mới cho ban tổ chức của hoạt động.'}" scope="request"/>
<%@ include file="_top.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:url var="roundBack" value="/organizer-recruitment">
    <c:if test="${round.activityId > 0}"><c:param name="activityId" value="${round.activityId}"/></c:if>
</c:url>
<form class="card rc-card" id="roundForm" method="post" action="${ctx}/organizer-recruitment">
    <input type="hidden" name="action" value="save">
    <input type="hidden" name="id" value="${round.id}">
    <input type="hidden" name="intent" id="intent" value="draft">
    <c:if test="${locked}">
        <div class="rc-alert info">Đợt tuyển đã có hồ sơ nên vị trí và câu hỏi bị khóa. Bạn vẫn sửa được thông tin chung và thời gian.</div>
    </c:if>

    <div class="rc-step-title"><span class="rc-step-no">1</span>
        <div><h3>Thông tin đợt tuyển</h3><small>Hoạt động, tiêu đề và thời gian nhận hồ sơ</small></div></div>
    <div class="rc-grid">
        <div class="rc-field"><label>Hoạt động <em>*</em></label>
            <c:choose>
                <c:when test="${round.activityId > 0}">
                    <input type="hidden" name="activityId" value="${round.activityId}">
                    <select disabled><c:forEach items="${activities}" var="a"><option ${a.activityId == round.activityId ? 'selected' : ''}><c:out value="${a.activityName}"/></option></c:forEach></select>
                </c:when>
                <c:otherwise>
                    <select name="activityId" id="activityId" required>
                        <option value="">-- Chọn hoạt động --</option>
                        <c:forEach items="${activities}" var="a"><option value="${a.activityId}" ${a.activityId == round.activityId ? 'selected' : ''}><c:out value="${a.activityName}"/></option></c:forEach>
                    </select>
                </c:otherwise>
            </c:choose></div>
        <div class="rc-field"><label>Tiêu đề đợt tuyển <em>*</em></label><input name="title" maxlength="200" value="<c:out value='${round.title}'/>" required></div>
        <div class="rc-field"><label>Mở nhận hồ sơ</label><input type="datetime-local" name="openAt" value="${openAtValue}"></div>
        <div class="rc-field"><label>Đóng nhận hồ sơ</label><input type="datetime-local" name="closeAt" value="${closeAtValue}"></div>
        <div class="rc-field full"><label>Mô tả và hướng dẫn <span>(không bắt buộc)</span></label>
            <textarea name="description" placeholder="Giới thiệu vai trò, yêu cầu và quyền lợi của ban tổ chức..."><c:out value="${round.description}"/></textarea></div>
    </div>
    <div class="rc-alert info" id="participationHint" hidden></div>

    <div class="rc-step-title"><span class="rc-step-no">2</span>
        <div><h3>Vị trí tuyển</h3><small>Mỗi vị trí có chỉ tiêu riêng (tối đa 10)</small></div></div>
    <div id="positions"></div>
    <div class="rc-add-row">
        <button type="button" class="button button-sm" id="addPosition">+ Thêm vị trí</button>
        <span id="positionCount"></span>
    </div>

    <div class="rc-step-title"><span class="rc-step-no">3</span>
        <div><h3>Câu hỏi bổ sung</h3><small>Tối đa 10 câu, đặt riêng cho đợt tuyển này</small></div></div>
    <div id="questions"></div>
    <div class="rc-add-row">
        <button type="button" class="button button-sm" data-add-q="TEXT">+ Tự luận</button>
        <button type="button" class="button button-sm" data-add-q="DROPDOWN">+ Dropdown</button>
        <button type="button" class="button button-sm" data-add-q="MULTI_CHOICE">+ Nhiều lựa chọn</button>
        <span id="questionCount"></span>
    </div>

    <div class="rc-step-title"><span class="rc-step-no">4</span>
        <div><h3>Phỏng vấn</h3><small>Bật nếu cần phỏng vấn trước khi chọn</small></div></div>
    <div class="rc-field"><div class="rc-checks"><label style="width:100%"><input type="checkbox" name="interview" ${round.interviewRequired ? 'checked' : ''}>
        Có vòng phỏng vấn. Staff đặt lịch cho từng ứng viên sau khi xét hồ sơ.</label></div></div>

    <div class="rc-actions">
        <a class="button" href="<c:out value='${roundBack}'/>">Hủy</a>
        <button type="submit" class="button" data-intent="draft">Lưu bản nháp</button>
        <button type="submit" class="button button-primary" data-intent="open">Lưu và mở tuyển</button>
    </div>
</form>
<script type="application/json" id="initialData">${initialJson}</script>
<script>
    (function () {
        var MAX = 10, LOCKED = ${locked ? 'true' : 'false'};
        var TYPE_LABEL = {TEXT: 'Tự luận', DROPDOWN: 'Dropdown', MULTI_CHOICE: 'Nhiều lựa chọn'};
        var PART_OPEN = {<c:forEach items="${participationOpen}" var="e" varStatus="st">"${e.key}": "${e.value}"${st.last ? '' : ','}</c:forEach>};
        var data = JSON.parse(document.getElementById('initialData').textContent);
        var form = document.getElementById('roundForm');
        var positions = document.getElementById('positions'), questions = document.getElementById('questions');
        function esc(s) { return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;'); }
        var ro = LOCKED ? ' readonly' : '';

        function refresh() {
            document.getElementById('positionCount').textContent = positions.children.length + ' / ' + MAX + ' vị trí';
            document.getElementById('questionCount').textContent = questions.children.length + ' / ' + MAX + ' câu hỏi';
            document.getElementById('addPosition').disabled = LOCKED || positions.children.length >= MAX;
            document.querySelectorAll('[data-add-q]').forEach(function (b) { b.disabled = LOCKED || questions.children.length >= MAX; });
            Array.prototype.forEach.call(questions.children, function (q, i) { q.querySelector('.q-no').textContent = 'Câu ' + (i + 1) + ' '; });
        }

        function addPosition(name, total) {
            if (positions.children.length >= MAX) { return; }
            var row = document.createElement('div');
            row.className = 'rc-row';
            row.innerHTML = '<input name="posName" placeholder="Tên vị trí" maxlength="100" value="' + esc(name) + '" required' + ro + '>'
                + '<input name="posTotal" type="number" min="1" max="1000" placeholder="Chỉ tiêu" value="' + esc(total) + '" required' + ro + '>'
                + '<button type="button" class="button" aria-label="Xóa vị trí"' + (LOCKED ? ' disabled' : '') + '>×</button>';
            row.querySelector('button').onclick = function () { row.remove(); refresh(); };
            positions.appendChild(row);
            refresh();
        }

        function addQuestion(type, text, required, options) {
            if (questions.children.length >= MAX) { return; }
            var box = document.createElement('div');
            box.className = 'rc-question';
            box.dataset.type = type;
            var html = '<div class="rc-question-head"><span><span class="q-no"></span><span class="rc-type">' + TYPE_LABEL[type] + '</span></span>'
                + '<span><label><input type="checkbox" class="q-req"' + (required ? ' checked' : '') + (LOCKED ? ' disabled' : '') + '> Bắt buộc</label> '
                + '<button type="button" class="button button-sm q-del" aria-label="Xóa câu hỏi"' + (LOCKED ? ' disabled' : '') + '>×</button></span></div>'
                + '<div class="rc-field"><label>Nội dung câu hỏi</label><input class="q-text" maxlength="500" value="' + esc(text) + '" placeholder="Nhập câu hỏi" required' + ro + '></div>';
            if (type === 'TEXT') {
                html += '<div class="rc-options"><input disabled placeholder="Sinh viên gõ câu trả lời tự do"></div>';
            } else {
                html += '<div class="rc-options">';
                (options && options.length ? options : ['', '']).forEach(function (o, i) {
                    html += '<input class="q-opt-input" placeholder="Lựa chọn ' + (i + 1) + '" value="' + esc(o) + '"' + ro + '>';
                });
                html += '</div>' + (LOCKED ? '' : '<div class="rc-add-row"><button type="button" class="button button-sm q-opt">+ Thêm lựa chọn</button></div>');
            }
            box.innerHTML = html;
            box.querySelector('.q-del').onclick = function () { box.remove(); refresh(); };
            var addOpt = box.querySelector('.q-opt');
            if (addOpt) {
                addOpt.onclick = function () {
                    var wrap = box.querySelector('.rc-options'), inp = document.createElement('input');
                    inp.className = 'q-opt-input';
                    inp.placeholder = 'Lựa chọn ' + (wrap.children.length + 1);
                    wrap.appendChild(inp);
                };
            }
            questions.appendChild(box);
            refresh();
        }

        document.getElementById('addPosition').onclick = function () { addPosition('', ''); };
        document.querySelectorAll('[data-add-q]').forEach(function (b) {
            b.onclick = function () { addQuestion(b.dataset.addQ, '', true, null); };
        });

        data.positions.forEach(function (p) { addPosition(p.name, p.quota); });
        if (!data.positions.length) { addPosition('', ''); }
        data.questions.forEach(function (q) { addQuestion(q.type, q.text, q.required, q.options); });

        document.querySelectorAll('[data-intent]').forEach(function (b) {
            b.addEventListener('click', function () { document.getElementById('intent').value = b.dataset.intent; });
        });

        form.addEventListener('submit', function () {
            form.querySelectorAll('.q-gen').forEach(function (n) { n.remove(); });
            if (LOCKED) { return; }
            Array.prototype.forEach.call(questions.children, function (box) {
                var opts = Array.prototype.map.call(box.querySelectorAll('.q-opt-input'), function (i) { return i.value.trim(); })
                    .filter(function (v) { return v; }).join('\n');
                [['qText', box.querySelector('.q-text').value], ['qType', box.dataset.type],
                    ['qRequired', box.querySelector('.q-req').checked ? 'true' : 'false'], ['qOptions', opts]].forEach(function (kv) {
                    var h = document.createElement('input');
                    h.type = 'hidden'; h.className = 'q-gen'; h.name = kv[0]; h.value = kv[1];
                    form.appendChild(h);
                });
            });
        });

        var activity = document.getElementById('activityId'), hint = document.getElementById('participationHint');
        function showHint() {
            var id = activity ? activity.value : '${round.activityId}';
            if (id && PART_OPEN[id]) {
                hint.textContent = 'Đợt đăng ký tham gia của hoạt động này mở từ ' + PART_OPEN[id] + '. Nên đóng hồ sơ ban tổ chức trước thời điểm đó.';
                hint.hidden = false;
            } else { hint.hidden = true; }
        }
        if (activity) { activity.addEventListener('change', showHint); }
        showHint();
    })();
</script>
<%@ include file="_bottom.jspf" %>
