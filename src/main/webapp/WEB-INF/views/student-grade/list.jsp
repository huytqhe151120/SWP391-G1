<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="student-grades" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>EAMS | Danh sách điểm hoạt động của sinh viên</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261109-4">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/student-grade.css?v=20261110-1">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-list-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> ĐIỂM HOẠT ĐỘNG SINH VIÊN</span>
                    <h1>Danh sách điểm hoạt động của sinh viên</h1>
                    <p>Quản lý và tra cứu tổng quan số hoạt động tham gia, tổng điểm cộng, tổng điểm trừ và tổng điểm tích lũy của sinh viên.</p>
                </div>
            </header>

            <section class="card activity-table-card" aria-labelledby="grade-list-title">
                <div class="activity-table-heading">
                    <div>
                        <h2 id="grade-list-title">
                            Bảng điểm tổng kết hoạt động sinh viên
                            <c:choose>
                                <c:when test="${not empty selectedSemester}">
                                    – <c:out value="${selectedSemester.code}"/> (<c:out value="${selectedSemester.name}"/>)
                                </c:when>
                                <c:otherwise>– Tất cả học kỳ</c:otherwise>
                            </c:choose>
                        </h2>
                        <p>Các đầu điểm trong học kỳ được ghi nhận theo tổng điểm của các hoạt động ngoại khóa sinh viên tham gia trong học kỳ</p>
                    </div>
                    <div class="activity-heading-actions">
                        <span class="activity-count">
                            <c:choose>
                                <c:when test="${filtered}">
                                    <c:out value="${filteredStudentsCount}"/> / <c:out value="${totalStudentsCount}"/> sinh viên
                                </c:when>
                                <c:otherwise>
                                    <c:out value="${totalStudentsCount}"/> sinh viên
                                </c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                </div>

                <details class="activity-filter-panel" open>
                    <summary class="activity-filter-toggle">
                        <span>Tìm kiếm &amp; Bộ lọc</span>
                        <c:if test="${activeFilterCount > 0}">
                            <span class="activity-filter-badge"><c:out value="${activeFilterCount}"/></span>
                        </c:if>
                    </summary>
                    <form class="activity-filter" action="${pageContext.request.contextPath}/student-grades" method="get">
                        <input type="hidden" name="action" value="list">
                        <input type="hidden" name="pageSize" value="${pageSize}">
                        <div class="activity-filter-field">
                            <label for="filter-semester">Học kỳ</label>
                            <select id="filter-semester" name="semesterId">
                                <option value="0" ${selectedSemesterId == 0 ? 'selected' : ''}>Tất cả học kỳ</option>
                                <c:forEach var="sem" items="${semesters}">
                                    <option value="${sem.id}" ${selectedSemesterId == sem.id ? 'selected' : ''}>
                                        <c:out value="${sem.code}"/> – <c:out value="${sem.name}"/><c:if test="${sem.status == 'ACTIVE'}"> (Đang hoạt động)</c:if>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="activity-filter-field">
                            <label for="filter-keyword">Từ khóa (Mã SV / Tên SV)</label>
                            <input id="filter-keyword" type="search" name="keyword" value="<c:out value='${keyword}'/>"
                                   placeholder="Nhập mã hoặc tên sinh viên...">
                        </div>
                        <div class="activity-filter-field">
                            <label for="filter-department">Khoa</label>
                            <select id="filter-department" name="department">
                                <option value="">Tất cả</option>
                                <c:forEach var="item" items="${departments}">
                                    <option value="<c:out value='${item}'/>" ${selectedDepartment == item ? 'selected' : ''}><c:out value="${item}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="activity-filter-field">
                            <label for="filter-major">Ngành</label>
                            <select id="filter-major" name="major">
                                <option value="">Tất cả</option>
                                <c:forEach var="item" items="${majors}">
                                    <option value="<c:out value='${item}'/>" ${selectedMajor == item ? 'selected' : ''}><c:out value="${item}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="activity-filter-field">
                            <label for="filter-program">Chương trình đào tạo</label>
                            <select id="filter-program" name="academicProgram">
                                <option value="">Tất cả</option>
                                <c:forEach var="item" items="${academicPrograms}">
                                    <option value="<c:out value='${item}'/>" ${selectedAcademicProgram == item ? 'selected' : ''}><c:out value="${item}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="activity-filter-field">
                            <label for="filter-class">Lớp</label>
                            <select id="filter-class" name="mainClass">
                                <option value="">Tất cả</option>
                                <c:forEach var="item" items="${mainClasses}">
                                    <option value="<c:out value='${item}'/>" ${selectedMainClass == item ? 'selected' : ''}><c:out value="${item}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="activity-filter-buttons">
                            <button type="submit" class="button button-primary button-sm">Tìm kiếm &amp; Lọc</button>
                            <c:if test="${filtered}">
                                <a class="button button-sm" href="${pageContext.request.contextPath}/student-grades?pageSize=${pageSize}">Xóa bộ lọc</a>
                            </c:if>
                        </div>
                    </form>
                </details>

                <div class="table-wrap activity-table-wrap">
                    <table>
                        <thead>
                        <tr>
                            <th scope="col" class="activity-index-column">STT</th>
                            <th scope="col" class="col-code">Mã sinh viên</th>
                            <th scope="col" class="col-object-name">Tên sinh viên</th>
                            <th scope="col" class="col-academic">Khoa / Ngành</th>
                            <th scope="col" class="col-academic">CTĐT / Lớp</th>
                            <th scope="col" style="text-align: center;" title="Số hoạt động có mặt / Tổng số hoạt động đăng ký trong học kỳ">Số HĐ tham gia</th>
                            <th scope="col" style="text-align: center;" title="Tổng điểm tham gia (Chuyên cần + Đánh giá) của các hoạt động trong học kỳ">Điểm tham gia</th>
                            <th scope="col" style="text-align: center;" title="Tổng điểm bài thu hoạch của các hoạt động trong học kỳ">Điểm thu hoạch</th>
                            <th scope="col" style="text-align: center;">Tổng điểm cộng</th>
                            <th scope="col" style="text-align: center;">Tổng điểm trừ</th>
                            <th scope="col" style="text-align: center;">Tổng điểm tích lũy</th>
                            <th scope="col" class="activity-action-column">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:choose>
                            <c:when test="${empty summaries}">
                                <tr>
                                    <td class="empty-state activity-empty-state" colspan="12">
                                        <span class="empty-state-mark" aria-hidden="true">★</span>
                                        <strong>Không tìm thấy sinh viên phù hợp</strong>
                                        <span>Hãy thử điều chỉnh bộ lọc hoặc từ khóa tìm kiếm.</span>
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="item" items="${summaries}" varStatus="loop">
                                    <tr>
                                        <td><c:out value="${firstItemNumber + loop.index}"/></td>
                                        <td class="col-code"><span class="activity-code"><c:out value="${item.studentCode}" default="—"/></span></td>
                                        <td class="col-object-name" title="<c:out value='${item.studentName}'/>">
                                            <strong class="activity-name"><c:out value="${item.studentName}" default="—"/></strong>
                                        </td>
                                        <td class="col-academic" title="<c:out value='${item.departmentName} - ${item.majorName}'/>">
                                            <c:out value="${item.departmentName}" default="—"/>
                                            <c:if test="${not empty item.majorName}"><br><small class="text-muted"><c:out value="${item.majorName}"/></small></c:if>
                                        </td>
                                        <td class="col-academic" title="<c:out value='${item.academicProgramName} - ${item.mainClassCode}'/>">
                                            <c:out value="${item.academicProgramName}" default="—"/>
                                            <c:if test="${not empty item.mainClassCode}"><br><small class="text-muted">Lớp: <c:out value="${item.mainClassCode}"/></small></c:if>
                                        </td>
                                        <td style="text-align: center;">
                                            <span class="grade-ratio-badge" title="Có mặt ${item.attendedActivityCount} / Đăng ký ${item.registeredActivityCount} hoạt động">
                                                <c:out value="${item.participationRatioText}"/>
                                            </span>
                                        </td>
                                        <td style="text-align: center;" title="Chuyên cần: ${item.formattedAttendanceScore}đ | Đánh giá: ${item.formattedParticipationEvalScore}đ">
                                            <strong><c:out value="${item.formattedParticipationScore}"/></strong>
                                        </td>
                                        <td style="text-align: center;">
                                            <strong><c:out value="${item.formattedSummaryScore}"/></strong>
                                        </td>
                                        <td style="text-align: center;">
                                            <span class="grade-pill ${item.totalPlusPoints > 0 ? 'grade-pill--plus' : 'grade-pill--neutral'}">
                                                <c:out value="${item.formattedPlusPoints}"/>
                                            </span>
                                        </td>
                                        <td style="text-align: center;">
                                            <span class="grade-pill ${item.totalMinusPoints > 0 ? 'grade-pill--minus' : 'grade-pill--neutral'}">
                                                <c:out value="${item.formattedMinusPoints}"/>
                                            </span>
                                        </td>
                                        <td style="text-align: center;">
                                            <span class="grade-pill grade-pill--total">
                                                <c:out value="${item.formattedAccumulatedPoints}"/>
                                            </span>
                                        </td>
                                        <td class="activity-action-column">
                                            <div class="actions activity-actions">
                                                <a class="activity-action-link action-btn action-btn--view"
                                                   href="${pageContext.request.contextPath}/student-grades?action=studentDetail&amp;studentId=${item.studentId}&amp;semesterId=${selectedSemesterId}"
                                                   title="Xem chi tiết điểm hoạt động của sinh viên">
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
                    </table>
                </div>

                <c:if test="${filteredStudentsCount > 0}">
                    <c:set var="pageBaseUrl" value="${pageContext.request.contextPath}/student-grades?action=list&pageSize=${pageSize}${filterQuery}"/>
                    <div class="activity-pagination">
                        <form class="activity-page-size" action="${pageContext.request.contextPath}/student-grades" method="get">
                            <input type="hidden" name="action" value="list">
                            <input type="hidden" name="page" value="1">
                            <input type="hidden" name="semesterId" value="${selectedSemesterId}">
                            <input type="hidden" name="keyword" value="<c:out value='${keyword}'/>">
                            <input type="hidden" name="department" value="<c:out value='${selectedDepartment}'/>">
                            <input type="hidden" name="major" value="<c:out value='${selectedMajor}'/>">
                            <input type="hidden" name="academicProgram" value="<c:out value='${selectedAcademicProgram}'/>">
                            <input type="hidden" name="mainClass" value="<c:out value='${selectedMainClass}'/>">
                            <label for="grade-page-size">Hiển thị</label>
                            <select id="grade-page-size" name="pageSize" onchange="this.form.submit()">
                                <c:forEach var="size" items="${pageSizes}">
                                    <option value="${size}" ${pageSize == size ? 'selected' : ''}>${size}</option>
                                </c:forEach>
                            </select>
                            <span>bản ghi mỗi trang</span>
                        </form>
                        <span class="activity-pagination-summary">
                            Hiển thị <c:out value="${firstItemNumber}"/>–<c:out value="${lastItemNumber}"/>
                            trong <c:out value="${filteredStudentsCount}"/> sinh viên
                        </span>
                        <c:if test="${totalPages > 1}">
                            <nav class="activity-pagination-nav" aria-label="Phân trang danh sách điểm">
                                <c:choose>
                                    <c:when test="${currentPage > 1}">
                                        <a class="activity-page-link" href="<c:out value='${pageBaseUrl}&page=${currentPage - 1}'/>" aria-label="Trang trước">‹</a>
                                    </c:when>
                                    <c:otherwise><span class="activity-page-link is-disabled" aria-hidden="true">‹</span></c:otherwise>
                                </c:choose>
                                <c:forEach var="pageNumber" begin="1" end="${totalPages}">
                                    <a class="activity-page-link${pageNumber == currentPage ? ' is-active' : ''}"
                                       href="<c:out value='${pageBaseUrl}&page=${pageNumber}'/>"
                                       aria-current="${pageNumber == currentPage ? 'page' : ''}"><c:out value="${pageNumber}"/></a>
                                </c:forEach>
                                <c:choose>
                                    <c:when test="${currentPage < totalPages}">
                                        <a class="activity-page-link" href="<c:out value='${pageBaseUrl}&page=${currentPage + 1}'/>" aria-label="Trang tiếp theo">›</a>
                                    </c:when>
                                    <c:otherwise><span class="activity-page-link is-disabled" aria-hidden="true">›</span></c:otherwise>
                                </c:choose>
                            </nav>
                        </c:if>
                    </div>
                </c:if>
            </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>
