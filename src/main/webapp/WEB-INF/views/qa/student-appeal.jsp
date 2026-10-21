<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="Gửi yêu cầu phúc khảo điểm hoạt động ngoại khóa và theo dõi trạng thái xử lý.">
    <title>Phúc Khảo Điểm - SWP391-G1</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261015">
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

        :root {
            --bg:        #0f1117;
            --surface:   #1a1d27;
            --surface2:  #22263a;
            --border:    #2e3348;
            --accent:    #6366f1;
            --accent2:   #4f46e5;
            --success:   #10b981;
            --warning:   #f59e0b;
            --danger:    #ef4444;
            --text:      #e2e8f0;
            --muted:     #94a3b8;
            --radius:    14px;
            --radius-sm: 8px;
        }

        body {
            font-family: 'Inter', sans-serif;
            background: var(--bg);
            color: var(--text);
            min-height: 100vh;
        }

        /* ── Header ── */
        .ap-header {
            background: var(--surface);
            border-bottom: 1px solid var(--border);
            padding: 0 2rem;
            height: 64px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            position: sticky; top: 0; z-index: 100;
        }
        .ap-brand {
            display: flex; align-items: center; gap: 10px;
            font-weight: 700; font-size: 1.05rem; color: var(--accent);
            text-decoration: none;
        }
        .ap-brand-mark {
            width: 36px; height: 36px;
            background: linear-gradient(135deg, var(--accent), var(--accent2));
            border-radius: var(--radius-sm);
            display: flex; align-items: center; justify-content: center;
            font-size: 1.1rem; font-weight: 700; color: #fff;
        }
        .ap-nav { display: flex; gap: 0.75rem; align-items: center; }
        .ap-nav a {
            color: var(--muted); text-decoration: none;
            font-size: 0.85rem; font-weight: 500;
            padding: 6px 12px; border-radius: var(--radius-sm);
            transition: all 0.2s;
        }
        .ap-nav a:hover { color: var(--text); background: var(--surface2); }

        /* ── Layout ── */
        .ap-container { max-width: 1100px; margin: 0 auto; padding: 2rem 1.5rem; }

        .ap-hero {
            padding: 2.5rem 0 2rem;
            display: flex; align-items: flex-start; justify-content: space-between;
            gap: 1.5rem; flex-wrap: wrap;
        }
        .ap-hero h1 {
            font-size: 2rem; font-weight: 700;
            background: linear-gradient(135deg, #e2e8f0 30%, var(--accent));
            -webkit-background-clip: text; -webkit-text-fill-color: transparent;
            margin-bottom: 0.4rem;
        }
        .ap-hero p { color: var(--muted); font-size: 0.9rem; line-height: 1.6; }
        .student-chip {
            display: flex; align-items: center; gap: 10px;
            background: var(--surface); border: 1px solid var(--border);
            border-radius: 50px; padding: 6px 16px 6px 8px;
            white-space: nowrap;
        }
        .student-avatar {
            width: 32px; height: 32px;
            background: linear-gradient(135deg, var(--accent), var(--accent2));
            border-radius: 50%; display: flex; align-items: center; justify-content: center;
            font-size: 0.75rem; font-weight: 700; color: #fff;
        }
        .student-chip strong { font-size: 0.85rem; display: block; }
        .student-chip small  { font-size: 0.75rem; color: var(--muted); }

        /* ── Alerts ── */
        .notice {
            border-radius: var(--radius-sm); padding: 0.85rem 1.25rem;
            font-size: 0.875rem; margin-bottom: 1.5rem;
            border: 1px solid transparent;
        }
        .notice-success { background: rgba(16,185,129,.1); color: #6ee7b7; border-color: rgba(16,185,129,.25); }
        .notice-error   { background: rgba(239,68,68,.1);  color: #fca5a5; border-color: rgba(239,68,68,.25); }

        /* ── Grid ── */
        .ap-grid {
            display: grid;
            grid-template-columns: 420px 1fr;
            gap: 2rem;
            align-items: start;
        }
        @media(max-width: 900px) { .ap-grid { grid-template-columns: 1fr; } }

        /* ── Card ── */
        .ap-card {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: var(--radius);
            padding: 1.75rem;
        }
        .ap-card-title {
            font-size: 1rem; font-weight: 700; margin-bottom: 1.25rem;
            display: flex; align-items: center; gap: 8px;
        }
        .ap-card-title span.pill {
            font-size: 0.7rem; font-weight: 600;
            background: rgba(99,102,241,.15); color: var(--accent);
            padding: 2px 8px; border-radius: 20px;
        }

        /* ── Form ── */
        .ap-form { display: flex; flex-direction: column; gap: 1rem; }
        .ap-form label { font-size: 0.8rem; font-weight: 600; color: var(--muted); margin-bottom: 4px; display: block; }
        .ap-form select,
        .ap-form input[type="number"],
        .ap-form textarea {
            width: 100%; background: var(--surface2); border: 1px solid var(--border);
            border-radius: var(--radius-sm); color: var(--text); font-size: 0.875rem;
            padding: 0.65rem 0.9rem; outline: none; transition: border-color .2s;
            font-family: inherit;
        }
        .ap-form select:focus,
        .ap-form input:focus,
        .ap-form textarea:focus { border-color: var(--accent); }
        .ap-form textarea { resize: vertical; min-height: 120px; }
        .ap-form .field-hint { font-size: 0.75rem; color: var(--muted); margin-top: 4px; }

        .btn-submit {
            background: linear-gradient(135deg, var(--accent), var(--accent2));
            color: #fff; border: none; border-radius: var(--radius-sm);
            padding: 0.75rem 1.5rem; font-size: 0.9rem; font-weight: 600;
            cursor: pointer; transition: opacity .2s, transform .15s;
            width: 100%;
        }
        .btn-submit:hover { opacity: .9; transform: translateY(-1px); }

        /* ── Appeal list ── */
        .appeal-list { display: flex; flex-direction: column; gap: 0.85rem; }

        .appeal-item {
            background: var(--surface2);
            border: 1px solid var(--border);
            border-radius: var(--radius);
            padding: 1.25rem 1.5rem;
            position: relative; overflow: hidden;
            transition: transform .2s, box-shadow .2s;
        }
        .appeal-item:hover { transform: translateY(-2px); box-shadow: 0 6px 24px rgba(99,102,241,.12); }
        .appeal-item::before {
            content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 4px;
        }
        .appeal-item.pending::before  { background: var(--warning); }
        .appeal-item.approved::before { background: var(--success); }
        .appeal-item.rejected::before { background: var(--danger); }

        .appeal-toprow { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-bottom: 0.5rem; }
        .appeal-activity { font-weight: 600; font-size: 0.95rem; }
        .appeal-activity small { color: var(--muted); font-weight: 400; font-size: 0.8rem; }
        .appeal-reason { font-size: 0.85rem; color: var(--muted); line-height: 1.5; margin-bottom: 0.75rem; }
        .appeal-meta { display: flex; gap: 1rem; flex-wrap: wrap; font-size: 0.78rem; color: var(--muted); }

        .badge {
            display: inline-flex; align-items: center; gap: 4px;
            padding: 3px 10px; border-radius: 20px;
            font-size: 0.73rem; font-weight: 600;
        }
        .badge-pending  { background: rgba(245,158,11,.15); color: var(--warning); }
        .badge-approved { background: rgba(16,185,129,.15);  color: var(--success); }
        .badge-rejected { background: rgba(239,68,68,.15);   color: var(--danger); }

        .point-change {
            font-size: 0.8rem; color: var(--success); font-weight: 600;
        }
        .review-note {
            margin-top: 0.5rem; padding: 0.5rem 0.75rem;
            background: rgba(255,255,255,.04); border-radius: var(--radius-sm);
            font-size: 0.8rem; color: var(--muted); font-style: italic;
        }

        .empty-state {
            text-align: center; padding: 3rem 2rem;
            color: var(--muted); font-size: 0.9rem;
        }
        .empty-icon { font-size: 2.5rem; margin-bottom: 0.75rem; }
    </style>
</head>
<body>
<div class="activity-app-shell">
    <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
    <div class="activity-app-layout">
        <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

<header class="ap-header">
    <a class="ap-brand" href="${pageContext.request.contextPath}/home">
        <div class="ap-brand-mark">⚖</div>
        <span>Phúc Khảo Điểm</span>
    </a>
    <nav class="ap-nav">
        <a href="${pageContext.request.contextPath}/student-qa">Hỏi &amp; Đáp</a>
        <a href="${pageContext.request.contextPath}/home">Trang chủ</a>
    </nav>
</header>

<main class="ap-container">

    <div class="ap-hero">
        <div>
            <h1>Yêu cầu Phúc Khảo</h1>
            <p>Gửi yêu cầu xem xét lại điểm hoạt động và theo dõi kết quả xử lý từ nhà trường.</p>
        </div>
        <div class="student-chip">
            <div class="student-avatar">SV</div>
            <span>
                <strong><c:out value="${student.name}"/></strong>
                <small><c:out value="${student.code}"/></small>
            </span>
        </div>
    </div>

    <%-- Notifications --%>
    <c:if test="${not empty successMsg}">
        <div class="notice notice-success" role="status"><c:out value="${successMsg}"/></div>
    </c:if>
    <c:if test="${not empty errorMsg}">
        <div class="notice notice-error" role="alert"><c:out value="${errorMsg}"/></div>
    </c:if>

    <div class="ap-grid">

        <%-- ── Left: Submit Form ─────────────────────────────────────────── --%>
        <section class="ap-card" aria-labelledby="form-title">
            <p class="ap-card-title" id="form-title">
                📋 Gửi yêu cầu mới
                <span class="pill">AP-06</span>
            </p>

            <form class="ap-form" method="post"
                  action="${pageContext.request.contextPath}/appeal?action=submit">

                <div>
                    <label for="activityId">Hoạt động cần phúc khảo *</label>
                    <select id="activityId" name="activityId" required>
                        <option value="">-- Chọn hoạt động --</option>
                        <c:forEach var="act" items="${eligibleActivities}">
                            <option value="${act.activityId}">
                                <c:out value="${act.activityName}"/> (<c:out value="${act.activityCode}"/>)
                                — Điểm: <c:out value="${act.oldPoint}"/>
                            </option>
                        </c:forEach>
                    </select>
                    <span class="field-hint">Chỉ hiển thị hoạt động đã kết thúc và đã có điểm chính thức.</span>
                </div>

                <div>
                    <label for="reason">Lý do phúc khảo *</label>
                    <textarea id="reason" name="reason" maxlength="4000" required
                              placeholder="Mô tả rõ lý do bạn muốn phúc khảo điểm (tối đa 4.000 ký tự)..."></textarea>
                </div>

                <button id="submit-appeal-btn" class="btn-submit" type="submit">Gửi yêu cầu phúc khảo</button>
            </form>
        </section>

        <%-- ── Right: My Appeals ─────────────────────────────────────────── --%>
        <section class="ap-card" aria-labelledby="list-title">
            <p class="ap-card-title" id="list-title">
                📂 Yêu cầu của bạn
                <span class="pill">${myAppeals.size()} yêu cầu</span>
            </p>

            <c:choose>
                <c:when test="${empty myAppeals}">
                    <div class="empty-state">
                        <div class="empty-icon">📭</div>
                        <p>Chưa có yêu cầu phúc khảo nào.</p>
                        <p>Gửi yêu cầu đầu tiên bằng biểu mẫu bên trái.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="appeal-list">
                        <c:forEach var="ap" items="${myAppeals}">
                            <article class="appeal-item ${ap.status.toLowerCase()}" role="article">
                                <div class="appeal-toprow">
                                    <div class="appeal-activity">
                                        <c:out value="${ap.activityName}"/>
                                        <small>&nbsp;·&nbsp;<c:out value="${ap.activityCode}"/></small>
                                    </div>
                                    <c:choose>
                                        <c:when test="${ap.status == 'PENDING'}">
                                            <span class="badge badge-pending">⏳ Đang chờ</span>
                                        </c:when>
                                        <c:when test="${ap.status == 'APPROVED'}">
                                            <span class="badge badge-approved">✅ Chấp thuận</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-rejected">❌ Từ chối</span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>

                                <p class="appeal-reason"><c:out value="${ap.reason}"/></p>

                                <c:if test="${ap.status == 'APPROVED' && ap.newPoint != null}">
                                    <p class="point-change">
                                        📈 Điểm cập nhật:
                                        <c:if test="${ap.oldPoint != null}">
                                            <c:out value="${ap.oldPoint}"/> →
                                        </c:if>
                                        <strong><c:out value="${ap.newPoint}"/></strong>
                                    </p>
                                </c:if>

                                <c:if test="${not empty ap.reviewNote}">
                                    <div class="review-note">💬 <c:out value="${ap.reviewNote}"/></div>
                                </c:if>

                                <div class="appeal-meta">
                                    <span>📅 Gửi lúc: ${dt:format(ap.createdAt)}</span>
                                    <c:if test="${ap.reviewedAt != null}">
                                        <span>🗓 Xét duyệt: ${dt:format(ap.reviewedAt)}</span>
                                    </c:if>
                                    <c:if test="${not empty ap.reviewerName}">
                                        <span>👤 Xét bởi: <c:out value="${ap.reviewerName}"/></span>
                                    </c:if>
                                </div>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>

    </div><%-- /ap-grid --%>
</main>

    </div>
</div>
</body>
</html>
