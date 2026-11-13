<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="rcTitle" value="Hồ sơ ứng tuyển" scope="request"/>
<c:set var="rcHero" value="Hồ sơ ứng tuyển" scope="request"/>
<c:set var="rcEyebrow" value="EAMS / QUẢN LÝ TUYỂN BAN TỔ CHỨC" scope="request"/>
<c:set var="rcSubtitle" value="Xem và xét duyệt hồ sơ ứng tuyển ban tổ chức." scope="request"/>
<c:set var="rcAction" value="<a class=\"button button-outline\" href=\"${pageContext.request.contextPath}/organizer-recruitment?activityId=${round.activityId}\">Quay lại đợt tuyển</a>" scope="request"/>
<%@ include file="_top.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="base" value="${ctx}/organizer-recruitment?action=applications&id=${round.id}"/>
<c:set var="pq">${positionFilter != null ? '&positionId=' : ''}${positionFilter}</c:set>
<section class="card rc-card">
    <div class="rc-head" style="margin:0">
        <div>
            <div class="rc-sub" style="text-transform:uppercase;font-size:.72rem;font-weight:700;margin-top:6px">Hồ sơ ứng tuyển · <c:out value="${round.title}"/></div>
            <h2 style="font-size:1.6rem"><c:out value="${round.activityName}"/></h2>
        </div>
        <div style="display:flex;gap:10px;align-items:center">
            <span class="badge status-pill ${roundBadges[round.status]}">${roundLabels[round.status]}</span>
        </div>
    </div>
</section>

<div class="rc-stats">
    <c:forEach items="${round.positions}" var="p">
        <div class="card rc-stat">
            <span><c:out value="${p.name}"/></span>
            <strong>${p.acceptedCount} / ${p.quota}</strong>
            <div class="rc-bar"><i style="width:${p.quota > 0 ? p.acceptedCount * 100 / p.quota : 0}%"></i></div>
            <small class="rc-sub">đã trúng tuyển · còn ${p.remaining}</small>
        </div>
    </c:forEach>
</div>

<div class="rc-layout">
    <div>
        <section class="card rc-card" style="margin:0 0 18px" data-page-container data-applications data-position-filter="${positionFilter}">
            <div class="rc-tabs">
                <a class="rc-tab ${statusFilter eq 'all' ? 'active' : ''}" href="${base}${pq}">Tất cả (${allCount})</a>
                <a class="rc-tab ${statusFilter eq 'PENDING' ? 'active' : ''}" href="${base}&status=PENDING${pq}">Chờ xét (${counts['PENDING'] == null ? 0 : counts['PENDING']})</a>
                <a class="rc-tab ${statusFilter eq 'INTERVIEW_INVITED' ? 'active' : ''}" href="${base}&status=INTERVIEW_INVITED${pq}">Mời phỏng vấn (${counts['INTERVIEW_INVITED'] == null ? 0 : counts['INTERVIEW_INVITED']})</a>
                <a class="rc-tab ${statusFilter eq 'INTERVIEWED' ? 'active' : ''}" href="${base}&status=INTERVIEWED${pq}">Đã phỏng vấn (${counts['INTERVIEWED'] == null ? 0 : counts['INTERVIEWED']})</a>
                <a class="rc-tab ${statusFilter eq 'ACCEPTED' ? 'active' : ''}" href="${base}&status=ACCEPTED${pq}">Trúng tuyển (${counts['ACCEPTED'] == null ? 0 : counts['ACCEPTED']})</a>
                <a class="rc-tab ${statusFilter eq 'REJECTED' ? 'active' : ''}" href="${base}&status=REJECTED${pq}">Không đạt (${counts['REJECTED'] == null ? 0 : counts['REJECTED']})</a>
                <a class="rc-tab ${statusFilter eq 'WITHDRAWN' ? 'active' : ''}" href="${base}&status=WITHDRAWN${pq}">Đã rút (${counts['WITHDRAWN'] == null ? 0 : counts['WITHDRAWN']})</a>
            </div>
            <form method="get" action="${ctx}/organizer-recruitment" style="display:flex;gap:10px;align-items:center;margin:12px 0">
                <input type="hidden" name="action" value="applications">
                <input type="hidden" name="id" value="${round.id}">
                <input type="hidden" name="status" value="${statusFilter}">
                <label for="positionId" class="rc-sub">Lọc theo ban</label>
                <select id="positionId" name="positionId" onchange="this.form.submit()">
                    <option value="">Tất cả ban</option>
                    <c:forEach items="${round.positions}" var="p"><option value="${p.id}" ${positionFilter == p.id ? 'selected' : ''}><c:out value="${p.name}"/></option></c:forEach>
                </select>
            </form>
            <div class="table-wrap">
                <table class="rc-table">
                    <thead>
                    <tr><th></th><th>Ứng viên</th><th>Nguyện vọng</th><th>Ngày nộp</th><th>Trạng thái</th><th>Phỏng vấn</th><th>Thao tác</th></tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${apps}" var="a">
                        <c:set var="iv" value="${a.interview}"/>
                        <tr data-page-item class="${selected != null and selected.id == a.id ? 'selected' : ''}">
                            <td><c:if test="${a.status eq 'PENDING' and round.interviewRequired}"><input type="checkbox" name="ids" value="${a.id}" form="interviewForm" aria-label="Chọn hồ sơ"></c:if></td>
                            <td><small class="rc-sub"><c:out value="${a.studentCode}"/></small><br><strong><c:out value="${a.studentName}"/></strong><br><small class="rc-sub"><c:out value="${a.className}"/></small></td>
                            <td>1. <c:out value="${a.choice1Name}"/><c:if test="${not empty a.choice2Name}"><br>2. <c:out value="${a.choice2Name}"/></c:if></td>
                            <td><c:out value="${dt:format(a.submittedAt)}"/></td>
                            <td><span class="badge status-pill ${appBadges[a.status]}">${appLabels[a.status]}</span>
                                <c:if test="${a.status eq 'ACCEPTED'}"><br><small class="rc-sub"><c:out value="${a.acceptedPositionName}"/></small></c:if></td>
                            <td>
                                <c:choose>
                                    <c:when test="${not empty iv and iv.status ne 'CANCELLED'}">
                                        <c:out value="${dt:format(iv.scheduledAt)}"/><br>
                                        <c:choose>
                                            <c:when test="${iv.status eq 'DONE'}"><small class="rc-sub">Đã xong</small></c:when>
                                            <c:when test="${iv.status eq 'CONFIRMED'}"><small class="rc-sub">Đã xác nhận</small></c:when>
                                            <c:when test="${iv.confirmOverdue}"><span class="rc-overdue">Quá hạn xác nhận</span></c:when>
                                            <c:otherwise><small class="rc-sub">Chờ xác nhận</small></c:otherwise>
                                        </c:choose>
                                    </c:when>
                                    <c:otherwise><small class="rc-sub">Chưa mời</small></c:otherwise>
                                </c:choose>
                            </td>
                            <td style="white-space:nowrap"><a href="${base}&appId=${a.id}&status=${statusFilter}${pq}#detail">Xem hồ sơ</a></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty apps}"><tr><td colspan="7" class="rc-sub">Không có hồ sơ nào.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
            <div class="activity-pagination" data-pagination hidden>
                <span class="activity-pagination-summary" aria-live="polite"></span>
                <nav class="activity-pagination-nav" aria-label="Phân trang hồ sơ ứng tuyển"></nav>
            </div>
            <c:if test="${round.interviewRequired}"><p class="rc-sub">Các hồ sơ đã chọn được giữ khi chuyển trang và sẽ được gửi cùng lời mời. <span data-selection-count aria-live="polite"></span></p></c:if>
        </section>

        <c:if test="${not empty selected}">
            <c:set var="iv" value="${selected.interview}"/>
            <section class="card rc-card" id="detail" style="margin:0">
                <div class="rc-head">
                    <div>
                        <h2><c:out value="${selected.studentName}"/> <small class="rc-sub">(<c:out value="${selected.studentCode}"/> · <c:out value="${selected.className}"/>)</small></h2>
                        <p class="rc-sub">Nộp lúc <c:out value="${dt:format(selected.submittedAt)}"/></p>
                    </div>
                    <span class="badge status-pill ${appBadges[selected.status]}">${appLabels[selected.status]}</span>
                </div>
                <p><b>Nguyện vọng 1:</b> <c:out value="${selected.choice1Name}"/> <small class="rc-sub">(còn ${remaining[selected.choice1PositionId]} chỉ tiêu)</small></p>
                <c:if test="${not empty selected.choice2Name}">
                    <p><b>Nguyện vọng 2:</b> <c:out value="${selected.choice2Name}"/> <small class="rc-sub">(còn ${remaining[selected.choice2PositionId]} chỉ tiêu)</small></p>
                </c:if>
                <c:if test="${not empty selected.note}"><p><b>Ghi chú của sinh viên:</b> <c:out value="${selected.note}"/></p></c:if>
                <c:forEach items="${selected.answers}" var="ans">
                    <p><b><c:out value="${ans.questionText}"/></b><br><c:out value="${ans.answerValue}"/></p>
                </c:forEach>

                <c:if test="${not empty iv and iv.status ne 'CANCELLED'}">
                    <div class="rc-interview" style="margin:12px 0">
                        <div>
                            <span><b>Thời gian:</b> <c:out value="${dt:format(iv.scheduledAt)}"/></span>
                            <span><b>Hình thức:</b> ${iv.format eq 'ONLINE' ? 'Trực tuyến' : 'Trực tiếp'}</span>
                            <span><b>${iv.format eq 'ONLINE' ? 'Liên kết' : 'Địa điểm'}:</b> <c:out value="${iv.location}"/></span>
                            <span><b>Hạn xác nhận:</b> <c:out value="${dt:format(iv.confirmDeadline)}"/></span>
                            <c:if test="${iv.confirmOverdue}"><span class="rc-overdue">Quá hạn xác nhận</span></c:if>
                            <c:if test="${not empty iv.evaluation}"><span><b>Nhận xét:</b> <c:out value="${iv.evaluation}"/></span></c:if>
                        </div>
                    </div>
                </c:if>

                <form method="post" action="${ctx}/organizer-recruitment" class="rc-field" style="margin:12px 0">
                    <input type="hidden" name="action" value="note">
                    <input type="hidden" name="roundId" value="${round.id}">
                    <input type="hidden" name="applicationId" value="${selected.id}">
                    <input type="hidden" name="status" value="${statusFilter}">
                    <label for="internalNote">Ghi chú nội bộ <span>(sinh viên không thấy)</span></label>
                    <textarea id="internalNote" name="internalNote" maxlength="1000"><c:out value="${selected.internalNote}"/></textarea>
                    <div><button type="submit" class="button button-sm">Lưu ghi chú</button></div>
                </form>

                <c:if test="${selected.status eq 'INTERVIEW_INVITED'}">
                    <form method="post" action="${ctx}/organizer-recruitment" class="rc-field" style="margin:12px 0">
                        <input type="hidden" name="action" value="completeInterview">
                        <input type="hidden" name="roundId" value="${round.id}">
                        <input type="hidden" name="applicationId" value="${selected.id}">
                        <input type="hidden" name="status" value="${statusFilter}">
                        <label for="evaluation">Sau buổi phỏng vấn: nhận xét</label>
                        <textarea id="evaluation" name="evaluation" maxlength="1000"></textarea>
                        <div style="display:flex;gap:10px">
                            <button type="submit" class="button button-primary button-sm" ${not interviewCanComplete ? 'disabled' : ''}>Đánh dấu đã phỏng vấn</button>
                        </div>
                        <c:if test="${not interviewCanComplete}"><small class="rc-sub">Chỉ ghi nhận sau giờ phỏng vấn.</small></c:if>
                    </form>
                    <form method="post" action="${ctx}/organizer-recruitment" onsubmit="return confirm('Hủy lịch phỏng vấn?');" style="margin-bottom:12px">
                        <input type="hidden" name="action" value="cancelInterview">
                        <input type="hidden" name="roundId" value="${round.id}">
                        <input type="hidden" name="applicationId" value="${selected.id}">
                        <input type="hidden" name="status" value="${statusFilter}">
                        <button type="submit" class="button button-sm">Hủy lịch phỏng vấn</button>
                        <span class="rc-sub">Đổi lịch: sửa thông tin ở khung bên phải rồi gửi lại.</span>
                    </form>
                    <form method="post" action="${ctx}/organizer-recruitment" style="margin-bottom:12px">
                        <input type="hidden" name="action" value="remind">
                        <input type="hidden" name="roundId" value="${round.id}">
                        <input type="hidden" name="applicationId" value="${selected.id}">
                        <input type="hidden" name="status" value="${statusFilter}">
                        <c:if test="${iv.status eq 'SCHEDULED'}"><button type="submit" class="button button-sm">Gọi lại (nhắc xác nhận)</button></c:if>
                    </form>
                </c:if>

                <c:if test="${not activityStarted}">
                    <c:if test="${(selected.status eq 'INTERVIEWED') or (selected.status eq 'PENDING' and not round.interviewRequired)}">
                        <form method="post" action="${ctx}/organizer-recruitment" class="rc-field" style="margin:12px 0">
                            <input type="hidden" name="action" value="accept">
                            <input type="hidden" name="roundId" value="${round.id}">
                            <input type="hidden" name="applicationId" value="${selected.id}">
                            <input type="hidden" name="status" value="${statusFilter}">
                            <label for="acceptPos">Ban được nhận</label>
                            <select id="acceptPos" name="positionId" required>
                                <c:forEach items="${round.positions}" var="p">
                                    <option value="${p.id}" ${p.remaining == 0 ? 'disabled' : ''} ${p.id == selected.choice1PositionId and p.remaining > 0 ? 'selected' : ''}>
                                        <c:out value="${p.name}"/> (còn ${p.remaining}/${p.quota})${p.id == selected.choice1PositionId ? ' · NV1' : (p.id == selected.choice2PositionId ? ' · NV2' : '')}</option>
                                </c:forEach>
                            </select>
                            <c:if test="${remaining[selected.choice1PositionId] == 0 and not empty selected.choice2Name and remaining[selected.choice2PositionId] > 0}">
                                <small class="rc-sub" style="color:#b54708">Nguyện vọng 1 đã đủ người. Còn chỉ tiêu ở nguyện vọng 2: <c:out value="${selected.choice2Name}"/>.</small>
                            </c:if>
                            <div><button type="submit" class="button button-primary button-sm">Trúng tuyển</button></div>
                        </form>
                    </c:if>
                    <c:if test="${selected.status eq 'PENDING' or selected.status eq 'INTERVIEW_INVITED' or selected.status eq 'INTERVIEWED'}">
                        <form method="post" action="${ctx}/organizer-recruitment" class="rc-field" style="margin:12px 0"
                              onsubmit="return confirm('Loại hồ sơ này?');">
                            <input type="hidden" name="action" value="reject">
                            <input type="hidden" name="roundId" value="${round.id}">
                            <input type="hidden" name="applicationId" value="${selected.id}">
                            <input type="hidden" name="status" value="${statusFilter}">
                            <label for="reason">Lý do loại <span>(ghi chú nội bộ)</span></label>
                            <input id="reason" name="reason" maxlength="500">
                            <div><button type="submit" class="button button-sm" style="color:#b42318">Loại</button></div>
                        </form>
                    </c:if>
                    <c:if test="${selected.status eq 'ACCEPTED' or selected.status eq 'REJECTED'}">
                        <form method="post" action="${ctx}/organizer-recruitment" onsubmit="return confirm('Thu hồi quyết định này?');">
                            <input type="hidden" name="action" value="revert">
                            <input type="hidden" name="roundId" value="${round.id}">
                            <input type="hidden" name="applicationId" value="${selected.id}">
                            <input type="hidden" name="status" value="${statusFilter}">
                            <button type="submit" class="button button-sm">Thu hồi quyết định</button>
                            <c:if test="${selected.status eq 'REJECTED' and not empty selected.decisionReason}"><span class="rc-sub">Lý do: <c:out value="${selected.decisionReason}"/></span></c:if>
                        </form>
                    </c:if>
                </c:if>
                <c:if test="${activityStarted}"><p class="rc-note">Hoạt động đã bắt đầu nên không thể thay đổi quyết định.</p></c:if>
                <c:if test="${selected.status eq 'WITHDRAWN' and not empty selected.withdrawReason}"><p class="rc-note">Lý do rút: <c:out value="${selected.withdrawReason}"/></p></c:if>
            </section>
        </c:if>
    </div>

    <aside class="card rc-card" style="margin:0">
        <h2>Mời phỏng vấn</h2>
        <c:choose>
            <c:when test="${not round.interviewRequired}"><p class="rc-sub">Đợt tuyển này không có vòng phỏng vấn. Staff cho trúng tuyển thẳng ở phần chi tiết hồ sơ.</p></c:when>
            <c:otherwise>
                <c:if test="${not empty selected and (selected.status eq 'PENDING' or selected.status eq 'INTERVIEW_INVITED')}">
                    <p><b><c:out value="${selected.studentCode}"/> · <c:out value="${selected.studentName}"/></b></p>
                </c:if>
                <form id="interviewForm" method="post" action="${ctx}/organizer-recruitment" style="margin-top:14px;display:grid;gap:12px">
                    <input type="hidden" name="action" value="invite">
                    <input type="hidden" name="roundId" value="${round.id}">
                    <input type="hidden" name="status" value="${statusFilter}">
                    <c:if test="${not empty selected and (selected.status eq 'PENDING' or selected.status eq 'INTERVIEW_INVITED')}">
                        <input type="hidden" name="applicationId" value="${selected.id}">
                    </c:if>
                    <div class="rc-field"><label>Thời gian <em>*</em></label><input type="datetime-local" name="scheduledAt" value="${ivInputs['scheduledAt']}" required></div>
                    <div class="rc-field"><label>Hình thức <em>*</em></label>
                        <select name="format">
                            <option value="OFFLINE" ${not empty selected.interview and selected.interview.format eq 'OFFLINE' ? 'selected' : ''}>Trực tiếp</option>
                            <option value="ONLINE" ${not empty selected.interview and selected.interview.format eq 'ONLINE' ? 'selected' : ''}>Trực tuyến</option>
                        </select></div>
                    <div class="rc-field"><label>Địa điểm hoặc liên kết <em>*</em></label>
                        <input name="location" maxlength="300" value="<c:out value='${selected.status eq "INTERVIEW_INVITED" ? selected.interview.location : ""}'/>" placeholder="Phòng hoặc liên kết" required></div>
                    <div class="rc-field"><label>Hạn xác nhận <em>*</em></label><input type="datetime-local" name="confirmDeadline" value="${ivInputs['confirmDeadline']}" required></div>
                    <div class="rc-field"><label>Ghi chú cho ứng viên <span>(không bắt buộc)</span></label>
                        <textarea name="note" maxlength="500"><c:out value="${selected.status eq 'INTERVIEW_INVITED' ? selected.interview.note : ''}"/></textarea></div>
                    <div class="rc-actions" style="margin:0;padding:0;border:0">
                        <button type="submit" class="button button-primary">${selected.status eq 'INTERVIEW_INVITED' ? 'Đổi lịch' : 'Gửi lời mời'}</button>
                    </div>
                </form>
            </c:otherwise>
        </c:choose>
    </aside>
</div>
<script src="${ctx}/assets/js/recruitment-pagination.js?v=20261113-1"></script>
<%@ include file="_bottom.jspf" %>
