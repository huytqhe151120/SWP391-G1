<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://swp391.g1/status" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="student-grades" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>EAMS | Chi tiết điểm hoạt động của sinh viên</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261109-4">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/student-grade.css?v=20261111-2">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-list-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> CHI TIẾT ĐIỂM HOẠT ĐỘNG SINH VIÊN</span>
                    <h1>Chi tiết điểm hoạt động của sinh viên</h1>
                    <p>Xem tiến trình, bảng điểm tổng quan và lịch sử tham gia tất cả các hoạt động ngoại khóa của sinh viên.</p>
                </div>
            </header>

            <c:if test="${not isStudent}">
                <div class="activity-page-back">
                    <a class="button button-back" href="${pageContext.request.contextPath}/student-grades?semesterId=${selectedSemesterId}">
                        <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5"/><path d="m12 19-7-7 7-7"/></svg>
                        Quay lại danh sách điểm sinh viên
                    </a>
                </div>
            </c:if>

            <section class="card activity-table-card grade-detail-card" aria-labelledby="student-grade-detail-title">
                <div class="activity-table-heading">
                    <div>
                        <h2 id="student-grade-detail-title">
                            <c:choose>
                                <c:when test="${not empty selectedStudent}">
                                    Hồ sơ điểm hoạt động: <c:out value="${selectedStudent.studentCode}"/> – <c:out value="${selectedStudent.studentName}"/>
                                </c:when>
                                <c:otherwise>Hồ sơ điểm hoạt động sinh viên</c:otherwise>
                            </c:choose>
                        </h2>
                        <p>
                            <c:choose>
                                <c:when test="${not empty selectedStudent}">
                                    Khoa: <c:out value="${selectedStudent.departmentName}" default="—"/> |
                                    Ngành: <c:out value="${selectedStudent.majorName}" default="—"/> |
                                    CTĐT: <c:out value="${selectedStudent.academicProgramName}" default="—"/> |
                                    Lớp: <c:out value="${selectedStudent.mainClassCode}" default="—"/>
                                </c:when>
                                <c:otherwise>Chọn sinh viên để tra cứu</c:otherwise>
                            </c:choose>
                        </p>
                    </div>
                    <c:if test="${not empty selectedStudent}">
                        <div class="activity-heading-actions">
                            <span class="activity-count">
                                Tham gia <c:out value="${selectedStudent.participationRatioText}"/> hoạt động
                            </span>
                        </div>
                    </c:if>
                </div>

                <%-- 1. Bộ chọn Học kỳ & Sinh viên (Searchable Combobox) --%>
                <form id="student-grade-filter-form" class="grade-selector-bar ${isStudent ? 'is-single-field' : ''}" action="${pageContext.request.contextPath}/student-grades" method="get">
                    <input type="hidden" name="action" value="studentDetail">
                    <div class="grade-selector-field">
                        <label for="semester-selector">Học kỳ</label>
                        <select id="semester-selector" name="semesterId" onchange="this.form.submit()">
                            <option value="0" ${selectedSemesterId == 0 ? 'selected' : ''}>Tất cả học kỳ</option>
                            <c:forEach var="sem" items="${semesters}">
                                <option value="${sem.id}" ${selectedSemesterId == sem.id ? 'selected' : ''}>
                                    <c:out value="${sem.code}"/> – <c:out value="${sem.name}"/><c:if test="${sem.id == activeSemesterId}"> (Đang diễn ra)</c:if>
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <c:if test="${not isStudent}">
                        <div class="grade-selector-field">
                            <label for="student-combobox-input">Chọn sinh viên</label>
                            <c:set var="currentStudentLabel" value=""/>
                            <c:if test="${not empty selectedStudent}">
                                <c:set var="currentStudentLabel">${selectedStudent.studentCode} – ${selectedStudent.studentName}${not empty selectedStudent.mainClassCode ? ' ('.concat(selectedStudent.mainClassCode).concat(')') : ''}</c:set>
                            </c:if>
                            <div class="grade-combobox" id="student-combobox">
                                <input type="hidden" name="studentId" id="student-selector-value" value="${selectedStudentId}">
                                <div class="grade-combobox-control">
                                    <svg class="grade-combobox-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                        <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
                                    </svg>
                                    <input id="student-combobox-input"
                                           type="text"
                                           class="grade-combobox-input"
                                           value="<c:out value='${currentStudentLabel}'/>"
                                           placeholder="Tìm hoặc chọn sinh viên theo Mã SV, Họ tên, Lớp..."
                                           role="combobox"
                                           aria-expanded="false"
                                           aria-autocomplete="list"
                                           aria-controls="student-combobox-list"
                                           autocomplete="off">
                                    <div class="grade-combobox-actions">
                                        <button type="button" id="student-combobox-clear" class="grade-combobox-clear" title="Xóa từ khóa để tìm sinh viên khác" aria-label="Xóa từ khóa">
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" aria-hidden="true">
                                                <path d="M18 6 6 18"/><path d="m6 6 12 12"/>
                                            </svg>
                                        </button>
                                        <button type="button" id="student-combobox-toggle" class="grade-combobox-toggle" title="Mở danh sách sinh viên" aria-label="Mở danh sách sinh viên" tabindex="-1">
                                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" aria-hidden="true">
                                                <path d="m6 9 6 6 6-6"/>
                                            </svg>
                                        </button>
                                    </div>
                                </div>
                                <ul id="student-combobox-list" class="grade-combobox-dropdown" role="listbox" hidden>
                                    <c:forEach var="st" items="${allStudents}">
                                        <c:set var="stLabel">${st.studentCode} – ${st.studentName}${not empty st.mainClassCode ? ' ('.concat(st.mainClassCode).concat(')') : ''}</c:set>
                                        <li class="grade-combobox-option ${selectedStudentId == st.studentId ? 'is-selected' : ''}"
                                            role="option"
                                            aria-selected="${selectedStudentId == st.studentId ? 'true' : 'false'}"
                                            data-student-id="${st.studentId}"
                                            data-label="<c:out value='${stLabel}'/>">
                                            <span class="grade-combobox-option-main">
                                                <span class="grade-combobox-code"><c:out value="${st.studentCode}"/></span>
                                                <span><c:out value="${st.studentName}"/></span>
                                            </span>
                                            <c:if test="${not empty st.mainClassCode}">
                                                <span class="grade-combobox-class"><c:out value="${st.mainClassCode}"/></span>
                                            </c:if>
                                        </li>
                                    </c:forEach>
                                    <li id="student-combobox-empty" class="grade-combobox-empty" hidden>
                                        Không tìm thấy sinh viên phù hợp
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </c:if>
                </form>

                <%-- 2. Thẻ Thông tin Tổng quan (Summary Cards) --%>
                <c:if test="${not empty selectedStudent}">
                    <div class="grade-summary-cards" aria-label="Thẻ thông tin tổng quan điểm sinh viên">
                        <div class="grade-summary-card">
                            <span class="grade-summary-card-label">Sinh viên</span>
                            <strong class="grade-summary-card-value"><c:out value="${selectedStudent.studentCode}" default="—"/></strong>
                            <span class="grade-summary-card-sub"><c:out value="${selectedStudent.studentName}" default="—"/> (<c:out value="${selectedStudent.participationRatioText}"/> HĐ)</span>
                        </div>
                        <div class="grade-summary-card">
                            <span class="grade-summary-card-label">Tổng điểm tham gia</span>
                            <strong class="grade-summary-card-value"><c:out value="${selectedStudent.formattedParticipationScore}"/></strong>
                            <span class="grade-summary-card-sub">ĐD: <c:out value="${selectedStudent.formattedAttendanceScore}"/> | ĐG: <c:out value="${selectedStudent.formattedParticipationEvalScore}"/></span>
                        </div>
                        <div class="grade-summary-card">
                            <span class="grade-summary-card-label">Tổng điểm thu hoạch</span>
                            <strong class="grade-summary-card-value"><c:out value="${selectedStudent.formattedSummaryScore}"/></strong>
                            <span class="grade-summary-card-sub">Tổng điểm TB: <c:out value="${selectedStudent.formattedAverageScore}"/></span>
                        </div>
                        <div class="grade-summary-card is-plus">
                            <span class="grade-summary-card-label">Tổng số điểm cộng</span>
                            <strong class="grade-summary-card-value"><c:out value="${selectedStudent.formattedPlusPoints}"/></strong>
                            <span class="grade-summary-card-sub">Từ các hoạt động có mặt</span>
                        </div>
                        <div class="grade-summary-card is-minus">
                            <span class="grade-summary-card-label">Tổng số điểm trừ</span>
                            <strong class="grade-summary-card-value"><c:out value="${selectedStudent.formattedMinusPoints}"/></strong>
                            <span class="grade-summary-card-sub">Trừ 5đ / hoạt động vắng mặt</span>
                        </div>
                        <div class="grade-summary-card is-total">
                            <span class="grade-summary-card-label">Tổng điểm tích lũy</span>
                            <strong class="grade-summary-card-value"><c:out value="${selectedStudent.formattedAccumulatedPoints}"/></strong>
                            <span class="grade-summary-card-sub">Điểm cộng − Điểm trừ</span>
                        </div>
                    </div>
                </c:if>

                <%-- 3. Bảng Danh sách điểm theo từng hoạt động của sinh viên --%>
                <div class="table-wrap activity-table-wrap">
                    <table>
                        <thead>
                        <tr>
                            <th scope="col" style="width: 48px; text-align: center;">STT</th>
                            <th scope="col" class="col-code">Mã HĐ</th>
                            <th scope="col" class="col-object-name">Tên hoạt động</th>
                            <th scope="col" class="col-status">Trạng thái HĐ</th>
                            <th scope="col" class="col-status">Điểm danh</th>
                            <th scope="col" style="text-align: center;">Điểm chuyên cần</th>
                            <th scope="col" style="text-align: center;">Điểm đánh giá</th>
                            <th scope="col" style="text-align: center;">Điểm tham gia</th>
                            <th scope="col" style="text-align: center;">Điểm thu hoạch</th>
                            <th scope="col" style="text-align: center;">Điểm TB</th>
                            <th scope="col" style="text-align: center;">Điểm tích lũy</th>
                            <th scope="col" class="activity-action-column">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:choose>
                            <c:when test="${empty activityGrades}">
                                <tr>
                                    <td class="empty-state activity-empty-state" colspan="12">
                                        <span class="empty-state-mark" aria-hidden="true">✓</span>
                                        <strong>Sinh viên chưa tham gia hoạt động nào</strong>
                                        <span>Danh sách điểm theo từng hoạt động của sinh viên sẽ hiển thị tại đây khi sinh viên có tên trong hoạt động.</span>
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="ag" items="${activityGrades}" varStatus="loop">
                                    <tr>
                                        <td style="text-align: center; font-weight: 600;"><c:out value="${loop.index + 1}"/></td>
                                        <td class="col-code"><span class="activity-code"><c:out value="${ag.activityCode}"/></span></td>
                                        <td class="col-object-name" title="<c:out value='${ag.activityName}'/>">
                                            <strong class="activity-name"><c:out value="${ag.activityName}"/></strong>
                                            <div class="text-muted" style="font-size: .76rem;">
                                                <c:if test="${not empty ag.semesterName}"><c:out value="${ag.semesterName}"/> • </c:if>
                                                <c:out value="${ag.organizationLevelName}" default="Cấp mặc định"/> (x<c:out value="${ag.formattedOrgLevelCoefficient}"/>)
                                                • <c:out value="${ag.registrationTypeLabel}"/>
                                            </div>
                                        </td>
                                        <td class="col-status">
                                            <span class="badge status-pill ${st:css(ag.activityStatus)}">${st:label(ag.activityStatus)}</span>
                                        </td>
                                        <td class="col-status">
                                            <span class="badge status-pill ${st:css(ag.attendanceStatus)}">${st:label(ag.attendanceStatus)}</span>
                                            <div class="text-muted" style="font-size: .73rem; margin-top: 2px;">
                                                <c:out value="${ag.attendedSessionCount}"/>/<c:out value="${ag.totalSessions}"/> buổi
                                            </div>
                                        </td>
                                        <td style="text-align: center;">
                                            <c:choose>
                                                <c:when test="${ag.attendanceStatus == 'NOT_YET'}"><span class="text-muted">Chưa điểm danh</span></c:when>
                                                <c:otherwise><strong><c:out value="${ag.formattedAttendanceScore}"/></strong> <span class="text-muted" style="font-size:.75rem;">/ 5</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td style="text-align: center;">
                                            <c:choose>
                                                <c:when test="${not ag.present}"><span class="text-muted">0 / 5</span></c:when>
                                                <c:when test="${empty ag.participationEvalScore}"><span class="text-muted">Chưa chấm</span></c:when>
                                                <c:otherwise><strong><c:out value="${ag.formattedEvalScore}"/></strong> <span class="text-muted" style="font-size:.75rem;">/ 5</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td style="text-align: center;">
                                            <c:choose>
                                                <c:when test="${ag.attendanceStatus == 'NOT_YET'}"><span class="text-muted">Chưa điểm danh</span></c:when>
                                                <c:otherwise><strong><c:out value="${ag.formattedParticipationScore}"/></strong> <span class="text-muted" style="font-size:.75rem;">/ 10</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td style="text-align: center;">
                                            <c:choose>
                                                <c:when test="${not ag.summaryRequired}">
                                                    <span class="text-muted">-</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span><c:out value="${ag.formattedSummaryScore}"/></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td style="text-align: center;">
                                            <c:choose>
                                                <c:when test="${ag.attendanceStatus == 'NOT_YET'}"><span class="text-muted">—</span></c:when>
                                                <c:otherwise><strong><c:out value="${ag.formattedAverageScore}"/></strong></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td style="text-align: center;">
                                            <c:choose>
                                                <c:when test="${ag.attendanceStatus == 'NOT_YET'}">
                                                    <span class="grade-pill grade-pill--neutral">0</span>
                                                </c:when>
                                                <c:when test="${ag.accumulatedScore < 0}">
                                                    <span class="grade-pill grade-pill--minus"><c:out value="${ag.formattedAccumulatedScore}"/></span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="grade-pill grade-pill--plus">+<c:out value="${ag.formattedAccumulatedScore}"/></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="activity-action-column">
                                            <div class="actions activity-actions">
                                                <a class="activity-action-link action-btn action-btn--view"
                                                   href="${pageContext.request.contextPath}/student-grades?action=activityDetail&amp;studentId=${selectedStudent.studentId}&amp;activityId=${ag.activityId}"
                                                   title="Xem chi tiết điểm hoạt động">
                                                    <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg></span>
                                                    Chi tiết
                                                </a>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                        </tbody>
                        <c:if test="${not empty activityGrades and not empty selectedStudent}">
                            <tfoot>
                            <tr style="background: #f8fafc; font-weight: 700;">
                                <td colspan="5" style="text-align: right;">Tổng các đầu điểm trong học kỳ:</td>
                                <td style="text-align: center;"><c:out value="${selectedStudent.formattedAttendanceScore}"/></td>
                                <td style="text-align: center;"><c:out value="${selectedStudent.formattedParticipationEvalScore}"/></td>
                                <td style="text-align: center;"><c:out value="${selectedStudent.formattedParticipationScore}"/></td>
                                <td style="text-align: center;"><c:out value="${selectedStudent.formattedSummaryScore}"/></td>
                                <td style="text-align: center;"><c:out value="${selectedStudent.formattedAverageScore}"/></td>
                                <td style="text-align: center;">
                                    <span class="grade-pill ${selectedStudent.totalAccumulatedPoints < 0 ? 'grade-pill--minus' : (selectedStudent.totalAccumulatedPoints > 0 ? 'grade-pill--plus' : 'grade-pill--neutral')}">
                                        <c:out value="${selectedStudent.totalAccumulatedPoints > 0 ? '+' : ''}${selectedStudent.formattedAccumulatedPoints}"/>
                                    </span>
                                </td>
                                <td></td>
                            </tr>
                            </tfoot>
                        </c:if>
                    </table>
                </div>
            </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
<script>
    (function () {
        var form = document.getElementById('student-grade-filter-form');
        var combobox = document.getElementById('student-combobox');
        var input = document.getElementById('student-combobox-input');
        var hiddenInput = document.getElementById('student-selector-value');
        var list = document.getElementById('student-combobox-list');
        var clearBtn = document.getElementById('student-combobox-clear');
        var toggleBtn = document.getElementById('student-combobox-toggle');
        var emptyEl = document.getElementById('student-combobox-empty');

        if (!combobox || !input || !hiddenInput || !list) return;

        var options = Array.from(list.querySelectorAll('.grade-combobox-option'));
        var initialLabel = input.value || '';
        var activeIndex = -1;

        function normalize(str) {
            return (str || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '')
                .replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim();
        }

        function updateClearBtn() {
            if (clearBtn) {
                clearBtn.hidden = !input.value.trim();
            }
        }

        function getVisibleOptions() {
            return options.filter(function (opt) { return !opt.hidden; });
        }

        function setActiveOption(idx) {
            var visible = getVisibleOptions();
            options.forEach(function (opt) { opt.classList.remove('is-active'); });
            if (idx < 0 || idx >= visible.length) {
                activeIndex = -1;
                return;
            }
            activeIndex = idx;
            var target = visible[activeIndex];
            target.classList.add('is-active');
            target.scrollIntoView({ block: 'nearest' });
        }

        function filterOptions(query) {
            var q = normalize(query);
            var matchCount = 0;
            options.forEach(function (opt) {
                var label = opt.getAttribute('data-label') || opt.textContent;
                var match = !q || normalize(label).indexOf(q) !== -1;
                opt.hidden = !match;
                if (match) matchCount++;
            });
            if (emptyEl) {
                emptyEl.hidden = matchCount > 0;
            }
            setActiveOption(matchCount > 0 ? 0 : -1);
            updateClearBtn();
        }

        function openDropdown(filterByCurrentValue) {
            list.hidden = false;
            combobox.classList.add('is-open');
            input.setAttribute('aria-expanded', 'true');
            if (filterByCurrentValue) {
                filterOptions(input.value);
            } else {
                filterOptions('');
                var selectedOpt = list.querySelector('.grade-combobox-option.is-selected');
                if (selectedOpt) {
                    setTimeout(function () {
                        selectedOpt.scrollIntoView({ block: 'nearest' });
                    }, 0);
                }
            }
        }

        function closeDropdown(restoreSelected) {
            list.hidden = true;
            combobox.classList.remove('is-open');
            input.setAttribute('aria-expanded', 'false');
            options.forEach(function (opt) { opt.classList.remove('is-active'); });
            activeIndex = -1;
            if (restoreSelected) {
                input.value = initialLabel;
                updateClearBtn();
            }
        }

        function selectOption(opt) {
            if (!opt) return;
            var studentId = opt.getAttribute('data-student-id');
            var label = opt.getAttribute('data-label') || opt.textContent.trim();
            initialLabel = label;
            input.value = label;
            var changed = String(hiddenInput.value) !== String(studentId);
            hiddenInput.value = studentId;
            options.forEach(function (item) {
                var isMatch = item === opt;
                item.classList.toggle('is-selected', isMatch);
                item.setAttribute('aria-selected', isMatch ? 'true' : 'false');
            });
            closeDropdown(false);
            if (form && changed) {
                form.submit();
            }
        }

        input.addEventListener('focus', function () {
            openDropdown(false);
            input.select();
        });

        input.addEventListener('click', function () {
            if (list.hidden) {
                openDropdown(false);
            }
        });

        input.addEventListener('input', function () {
            if (list.hidden) {
                openDropdown(true);
            } else {
                filterOptions(input.value);
            }
        });

        input.addEventListener('keydown', function (e) {
            var visible = getVisibleOptions();
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                if (list.hidden) {
                    openDropdown(false);
                } else if (visible.length > 0) {
                    setActiveOption((activeIndex + 1) % visible.length);
                }
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                if (list.hidden) {
                    openDropdown(false);
                } else if (visible.length > 0) {
                    setActiveOption((activeIndex - 1 + visible.length) % visible.length);
                }
            } else if (e.key === 'Enter') {
                e.preventDefault();
                if (!list.hidden && visible.length > 0) {
                    var idx = activeIndex >= 0 ? activeIndex : 0;
                    selectOption(visible[idx]);
                }
            } else if (e.key === 'Escape') {
                if (!list.hidden) {
                    e.preventDefault();
                    closeDropdown(true);
                    input.blur();
                }
            }
        });

        if (clearBtn) {
            clearBtn.addEventListener('click', function (e) {
                e.stopPropagation();
                input.value = '';
                openDropdown(false);
                input.focus();
            });
        }

        if (toggleBtn) {
            toggleBtn.addEventListener('click', function (e) {
                e.stopPropagation();
                if (list.hidden) {
                    openDropdown(false);
                    input.focus();
                } else {
                    closeDropdown(true);
                }
            });
        }

        options.forEach(function (opt) {
            opt.addEventListener('mousedown', function (e) {
                e.preventDefault();
                selectOption(opt);
            });
        });

        document.addEventListener('mousedown', function (e) {
            if (!combobox.contains(e.target)) {
                closeDropdown(true);
            }
        });

        updateClearBtn();
    })();
</script>
</body>
</html>
