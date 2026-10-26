<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="/WEB-INF/tld/datetime.tld" %>
<c:set var="activeNav" value="home" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Trang chủ | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261022-4">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/home.css">
</head>
<body>
<div class="activity-app-shell">
    <%-- Sidebar with role-based items --%>
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <%-- Original header --%>
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>

        <div class="activity-app-main">
            <main class="home-page-layout">
                <%-- ============================================ --%>
                <%-- 1. HERO SECTION (Layout matching Image 1)    --%>
                <%-- Color format matching original navy/blue     --%>
                <%-- ============================================ --%>
                <section class="home-hero-card" aria-labelledby="home-hero-title">
                    <div class="home-hero-content">
                        <div class="home-pill-tag">
                            <span class="home-pill-icon">🎓</span> Không gian hoạt động sinh viên
                        </div>

                        <h1 id="home-hero-title" class="home-hero-title">
                            Khám phá &amp; đăng ký<br>
                            <span class="home-title-highlight">hoạt động ngoại khóa.</span>
                        </h1>

                        <p class="home-hero-subtitle">
                            Từ hội thảo công nghệ đến ngày hội văn hóa &mdash; tìm kiếm hoạt động phù hợp,
                            đăng ký tham gia và tích lũy điểm rèn luyện nhanh chóng.
                        </p>

                        <%-- Search bar --%>
                        <form class="home-search-bar" action="${pageContext.request.contextPath}/activities" method="get">
                            <div class="home-search-wrap">
                                <span class="home-search-icon" aria-hidden="true">
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                                </span>
                                <input type="text" name="keyword" class="home-search-input"
                                       placeholder="Tìm kiếm sự kiện, câu lạc bộ, hội thảo..."
                                       autocomplete="off" aria-label="Tìm kiếm sự kiện">
                            </div>
                            <button type="submit" class="home-search-btn">Tìm kiếm</button>
                        </form>

                        <%-- 3 Metric stats --%>
                        <c:if test="${empty homeDataError}"><div class="home-stats-row">
                            <div class="home-stat-col">
                                <strong class="home-stat-num">${activityCount}</strong>
                                <span class="home-stat-lbl">Hoạt động đã duyệt</span>
                            </div>
                            <div class="home-stat-col">
                                <strong class="home-stat-num">${upcomingCount}</strong>
                                <span class="home-stat-lbl">Hoạt động sắp diễn ra</span>
                            </div>
                            <div class="home-stat-col">
                                <strong class="home-stat-num">${departmentCount}</strong>
                                <span class="home-stat-lbl">Đơn vị phụ trách</span>
                            </div>
                        </div></c:if>
                        <c:if test="${not empty homeDataError}"><p role="alert"><c:out value="${homeDataError}"/></p></c:if>
                    </div>

                    <%-- Right Column: Featured Event Card --%>
                    <c:if test="${not empty featuredActivity}">
                        <div class="home-hero-card-col"><div class="home-featured-box">
                            <div class="home-featured-top">
                                <div class="home-featured-badge">${featuredActivity.activityStatus == 'ONGOING' ? 'ĐANG DIỄN RA' : 'SẮP DIỄN RA'}</div>
                                <h3 class="home-featured-title"><c:out value="${featuredActivity.name}"/></h3>
                            </div>
                            <div class="home-featured-body">
                                <p>${dt:format(featuredActivity.startTime)}</p>
                                <p><c:out value="${featuredActivity.address}" default="Chưa cập nhật địa điểm"/></p>
                                <a href="${pageContext.request.contextPath}/activities?action=detail&amp;id=${featuredActivity.id}" class="home-featured-btn">Xem chi tiết</a>
                            </div>
                        </div></div>
                    </c:if>
                </section>

                <%-- ============================================ --%>
                <%-- 2. UPCOMING EVENTS (Layout matching Image 1) --%>
                <%-- ============================================ --%>
                <section class="home-upcoming-block" aria-labelledby="upcoming-block-title">
                    <div class="home-upcoming-nav">
                        <h2 id="upcoming-block-title" class="home-block-title">Hoạt động sắp diễn ra</h2>

                    </div>

                    <%-- 3-Column Events Grid --%>
                    <div class="home-events-grid" id="upcomingEventsGrid">
                        <c:forEach var="act" items="${upcomingActivities}">
                            <article class="home-event-card">
                                <div class="home-card-header home-header-workshop">
                                    <div class="home-date-chip"><span class="home-date-num">${act.startTime.dayOfMonth}</span><span class="home-date-mon">TH${act.startTime.monthValue}</span></div>
                                    <div class="home-cat-chip"><c:out value="${act.activityTypeName}"/></div>
                                </div>
                                <div class="home-card-main">
                                    <span class="home-cat-chip">${act.activityStatus == 'ONGOING' ? 'Đang diễn ra' : 'Sắp diễn ra'}</span>
                                    <h3 class="home-card-title"><c:out value="${act.name}"/></h3>
                                    <p class="home-card-snippet"><c:out value="${act.description}" default="Chưa cập nhật mô tả"/></p>
                                    <p class="home-card-meta-line">${dt:format(act.startTime)}</p>
                                    <p class="home-card-meta-line"><c:out value="${act.address}" default="Chưa cập nhật địa điểm"/></p>
                                    <a href="${pageContext.request.contextPath}/activities?action=detail&amp;id=${act.id}" class="home-card-action-btn">Xem chi tiết</a>
                                </div>
                            </article>
                        </c:forEach>
                    </div>
                    <c:if test="${empty upcomingActivities and empty homeDataError}"><p>Chưa có hoạt động sắp diễn ra.</p></c:if>
                </section>

                <section class="home-upcoming-block" aria-labelledby="ongoing-block-title">
                    <div class="home-upcoming-nav">
                        <h2 id="ongoing-block-title" class="home-block-title">Hoạt động đang diễn ra</h2>

                    </div>

                    <%-- 3-Column Events Grid --%>
                    <div class="home-events-grid" id="ongoingEventsGrid">
                        <c:forEach var="act" items="${ongoingActivities}">
                            <article class="home-event-card">
                                <div class="home-card-header home-header-workshop">
                                    <div class="home-date-chip"><span class="home-date-num">${act.startTime.dayOfMonth}</span><span class="home-date-mon">TH${act.startTime.monthValue}</span></div>
                                    <div class="home-cat-chip"><c:out value="${act.activityTypeName}"/></div>
                                </div>
                                <div class="home-card-main">
                                    <span class="home-cat-chip">${act.activityStatus == 'ONGOING' ? 'Đang diễn ra' : 'Sắp diễn ra'}</span>
                                    <h3 class="home-card-title"><c:out value="${act.name}"/></h3>
                                    <p class="home-card-snippet"><c:out value="${act.description}" default="Chưa cập nhật mô tả"/></p>
                                    <p class="home-card-meta-line">${dt:format(act.startTime)}</p>
                                    <p class="home-card-meta-line"><c:out value="${act.address}" default="Chưa cập nhật địa điểm"/></p>
                                    <a href="${pageContext.request.contextPath}/activities?action=detail&amp;id=${act.id}" class="home-card-action-btn">Xem chi tiết</a>
                                </div>
                            </article>
                        </c:forEach>
                    </div>
                    <c:if test="${empty ongoingActivities and empty homeDataError}"><p>Chưa có hoạt động đang diễn ra.</p></c:if>
                </section>
            </main>
        </div>
    </div>
</div>


</body>
</html>
