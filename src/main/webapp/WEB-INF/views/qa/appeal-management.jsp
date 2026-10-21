<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Quản lý và xét duyệt yêu cầu phúc khảo điểm của sinh viên.">
    <title>Quản lý Phúc Khảo - SWP391 G1</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261015">
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        :root {
            --bg:      #0f1117;
            --surface: #1a1d27;
            --sf2:     #22263a;
            --border:  #2e3348;
            --accent:  #6366f1;
            --accent2: #4f46e5;
            --success: #10b981;
            --warning: #f59e0b;
            --danger:  #ef4444;
            --text:    #e2e8f0;
            --muted:   #94a3b8;
            --radius:  12px;
            --rsm:     8px;
        }
        body { font-family: 'Inter', sans-serif; background: var(--bg); color: var(--text); min-height: 100vh; }

        /* Header */
        .mgmt-header {
            background: var(--surface); border-bottom: 1px solid var(--border);
            padding: 0 2rem; height: 64px;
            display: flex; align-items: center; justify-content: space-between;
            position: sticky; top: 0; z-index: 100;
        }
        .logo { display: flex; align-items: center; gap: 10px; font-weight: 700; font-size: 1.05rem; color: var(--accent); }
        .logo-mark {
            width: 36px; height: 36px;
            background: linear-gradient(135deg, var(--accent), var(--accent2));
            border-radius: var(--rsm); display: flex; align-items: center; justify-content: center;
            font-size: 1.1rem; font-weight: 700; color: #fff;
        }
        .nav-links { display: flex; gap: 0.75rem; }
        .nav-link {
            color: var(--muted); text-decoration: none; font-size: 0.85rem; font-weight: 500;
            padding: 6px 12px; border-radius: var(--rsm); transition: all .2s;
        }
        .nav-link:hover, .nav-link.active { color: var(--text); background: var(--sf2); }

        /* Container */
        .container { max-width: 1200px; margin: 0 auto; padding: 2rem 1.5rem; }

        /* Page header */
        .page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; margin-bottom: 2rem; flex-wrap: wrap; }
        .page-title {
            font-size: 1.75rem; font-weight: 700;
            background: linear-gradient(135deg, var(--text) 40%, var(--accent));
            -webkit-background-clip: text; -webkit-text-fill-color: transparent;
        }
        .page-subtitle { color: var(--muted); font-size: 0.875rem; margin-top: 4px; }

        /* Stats */
        .stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; margin-bottom: 2rem; }
        @media(max-width:700px) { .stats { grid-template-columns: 1fr; } }
        .stat-card {
            background: var(--surface); border: 1px solid var(--border);
            border-radius: var(--radius); padding: 1.25rem 1.5rem;
            display: flex; align-items: center; gap: 1rem;
        }
        .stat-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 1.3rem; }
        .stat-icon.total    { background: rgba(99,102,241,.15); }
        .stat-icon.pending  { background: rgba(245,158,11,.15); }
        .stat-icon.resolved { background: rgba(16,185,129,.15); }
        .stat-value { font-size: 1.5rem; font-weight: 700; }
        .stat-label { font-size: 0.75rem; color: var(--muted); }

        /* Filter tabs */
        .filter-tabs {
            display: flex; gap: 0.5rem; background: var(--surface);
            border: 1px solid var(--border); border-radius: var(--radius);
            padding: 6px; margin-bottom: 1.5rem; width: fit-content;
        }
        .filter-tab {
            padding: 7px 18px; border-radius: var(--rsm); font-size: 0.85rem; font-weight: 500;
            text-decoration: none; color: var(--muted); transition: all .2s;
        }
        .filter-tab.active, .filter-tab:hover { background: var(--accent); color: #fff; }

        /* Notice */
        .notice {
            border-radius: var(--rsm); padding: 0.85rem 1.25rem;
            font-size: 0.875rem; margin-bottom: 1.5rem; border: 1px solid transparent;
        }
        .notice-success { background: rgba(16,185,129,.1); color: #6ee7b7; border-color: rgba(16,185,129,.25); }
        .notice-error   { background: rgba(239,68,68,.1);  color: #fca5a5; border-color: rgba(239,68,68,.25); }

        /* Appeal table / cards */
        .appeal-list { display: flex; flex-direction: column; gap: 1rem; }

        .appeal-card {
            background: var(--surface); border: 1px solid var(--border);
            border-radius: var(--radius); padding: 1.5rem;
            position: relative; overflow: hidden; transition: all .2s;
        }
        .appeal-card::before {
            content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 4px;
        }
        .appeal-card.pending::before  { background: var(--warning); }
        .appeal-card.approved::before { background: var(--success); }
        .appeal-card.rejected::before { background: var(--danger); }
        .appeal-card:hover { border-color: var(--accent); transform: translateY(-2px); box-shadow: 0 8px 28px rgba(99,102,241,.12); }

        .card-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; margin-bottom: 0.75rem; }
        .card-title { font-size: 1rem; font-weight: 600; }
        .card-subtitle { font-size: 0.8rem; color: var(--muted); margin-top: 2px; }
        .card-reason {
            font-size: 0.85rem; color: var(--muted); line-height: 1.55;
            display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
            margin-bottom: 1rem;
        }
        .card-meta { display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; font-size: 0.78rem; color: var(--muted); }
        .card-actions { display: flex; gap: 0.5rem; margin-top: 1rem; }

        /* Badges */
        .badge {
            display: inline-flex; align-items: center; gap: 4px;
            padding: 3px 10px; border-radius: 20px; font-size: 0.73rem; font-weight: 600;
        }
        .badge-pending  { background: rgba(245,158,11,.15); color: var(--warning); }
        .badge-approved { background: rgba(16,185,129,.15);  color: var(--success); }
        .badge-rejected { background: rgba(239,68,68,.15);   color: var(--danger); }

        /* Action buttons */
        .btn {
            border: none; border-radius: var(--rsm); padding: 6px 14px;
            font-size: 0.8rem; font-weight: 600; cursor: pointer; transition: all .15s;
            text-decoration: none; display: inline-flex; align-items: center; gap: 5px;
        }
        .btn-approve { background: rgba(16,185,129,.2); color: var(--success); }
        .btn-approve:hover { background: rgba(16,185,129,.35); }
        .btn-reject  { background: rgba(239,68,68,.15); color: var(--danger); }
        .btn-reject:hover  { background: rgba(239,68,68,.3); }
        .btn-detail  { background: var(--sf2); color: var(--muted); }
        .btn-detail:hover  { color: var(--text); }

        /* Modal */
        .modal-overlay {
            display: none; position: fixed; inset: 0;
            background: rgba(0,0,0,.6); z-index: 500;
            align-items: center; justify-content: center;
        }
        .modal-overlay.active { display: flex; }
        .modal {
            background: var(--surface); border: 1px solid var(--border);
            border-radius: var(--radius); padding: 2rem;
            width: 100%; max-width: 480px; animation: slideUp .2s ease;
        }
        @keyframes slideUp { from { opacity:0; transform: translateY(20px); } to { opacity:1; transform: translateY(0); } }
        .modal-title { font-size: 1.1rem; font-weight: 700; margin-bottom: 1.25rem; }
        .modal-form { display: flex; flex-direction: column; gap: 1rem; }
        .modal-form label { font-size: 0.8rem; font-weight: 600; color: var(--muted); display: block; margin-bottom: 4px; }
        .modal-form input,
        .modal-form textarea {
            width: 100%; background: var(--sf2); border: 1px solid var(--border);
            border-radius: var(--rsm); color: var(--text); font-size: 0.875rem;
            padding: 0.65rem 0.9rem; outline: none; transition: border-color .2s;
            font-family: inherit;
        }
        .modal-form input:focus,
        .modal-form textarea:focus { border-color: var(--accent); }
        .modal-form textarea { min-height: 100px; resize: vertical; }
        .modal-buttons { display: flex; gap: 0.75rem; justify-content: flex-end; margin-top: 0.5rem; }
        .btn-cancel { background: var(--sf2); color: var(--muted); border: none; border-radius: var(--rsm); padding: 8px 16px; font-size: 0.875rem; cursor: pointer; }
        .btn-confirm-approve { background: linear-gradient(135deg, var(--success), #059669); color: #fff; border: none; border-radius: var(--rsm); padding: 8px 18px; font-size: 0.875rem; font-weight: 600; cursor: pointer; }
        .btn-confirm-reject  { background: linear-gradient(135deg, var(--danger), #dc2626);  color: #fff; border: none; border-radius: var(--rsm); padding: 8px 18px; font-size: 0.875rem; font-weight: 600; cursor: pointer; }

        /* Empty state */
        .empty-state { text-align: center; padding: 4rem 2rem; background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); }
        .empty-icon { font-size: 3rem; margin-bottom: 1rem; }
        .empty-title { font-size: 1rem; font-weight: 600; margin-bottom: 0.4rem; }
        .empty-desc { color: var(--muted); font-size: 0.85rem; }
    </style>
</head>
<body>
<div class="activity-app-shell">
    <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
    <div class="activity-app-layout">
        <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

<header class="mgmt-header">
    <div class="logo">
        <div class="logo-mark">⚖</div>
        Quản lý Phúc Khảo
    </div>
    <nav class="nav-links">
        <a href="${pageContext.request.contextPath}/qa-management" class="nav-link">Q&amp;A</a>
        <a href="${pageContext.request.contextPath}/appeal?action=manage" class="nav-link active">Phúc Khảo</a>
        <a href="${pageContext.request.contextPath}/home" class="nav-link">Trang chủ</a>
    </nav>
</header>

<main class="container">

    <div class="page-header">
        <div>
            <h1 class="page-title">Quản lý Yêu cầu Phúc Khảo</h1>
            <p class="page-subtitle">Xét duyệt yêu cầu phúc khảo điểm của sinh viên</p>
        </div>
    </div>

    <%-- Alerts --%>
    <c:if test="${not empty successMsg}">
        <div class="notice notice-success" role="status"><c:out value="${successMsg}"/></div>
    </c:if>

    <%-- Stats --%>
    <div class="stats">
        <div class="stat-card">
            <div class="stat-icon total">📋</div>
            <div>
                <div class="stat-value">${appeals.size()}</div>
                <div class="stat-label">Tổng yêu cầu</div>
            </div>
        </div>
        <div class="stat-card">
            <div class="stat-icon pending">⏳</div>
            <div>
                <div class="stat-value">
                    <c:set var="pendingCount" value="0"/>
                    <c:forEach var="ap" items="${appeals}">
                        <c:if test="${ap.status == 'PENDING'}">
                            <c:set var="pendingCount" value="${pendingCount + 1}"/>
                        </c:if>
                    </c:forEach>
                    ${pendingCount}
                </div>
                <div class="stat-label">Đang chờ xét</div>
            </div>
        </div>
        <div class="stat-card">
            <div class="stat-icon resolved">✅</div>
            <div>
                <div class="stat-value">
                    <c:set var="resolvedCount" value="0"/>
                    <c:forEach var="ap" items="${appeals}">
                        <c:if test="${ap.status != 'PENDING'}">
                            <c:set var="resolvedCount" value="${resolvedCount + 1}"/>
                        </c:if>
                    </c:forEach>
                    ${resolvedCount}
                </div>
                <div class="stat-label">Đã xử lý</div>
            </div>
        </div>
    </div>

    <%-- Filter tabs --%>
    <div class="filter-tabs">
        <a href="${pageContext.request.contextPath}/appeal?action=manage"
           class="filter-tab ${filter == 'all' ? 'active' : ''}">Tất cả</a>
        <a href="${pageContext.request.contextPath}/appeal?action=manage&filter=pending"
           class="filter-tab ${filter == 'pending' ? 'active' : ''}">⏳ Chờ xét duyệt</a>
    </div>

    <%-- Appeal list --%>
    <div class="appeal-list">
        <c:choose>
            <c:when test="${empty appeals}">
                <div class="empty-state">
                    <div class="empty-icon">🎉</div>
                    <div class="empty-title">Không có yêu cầu nào</div>
                    <div class="empty-desc">Tất cả yêu cầu phúc khảo đã được xử lý!</div>
                </div>
            </c:when>
            <c:otherwise>
                <c:forEach var="ap" items="${appeals}">
                    <article class="appeal-card ${ap.status.toLowerCase()}">
                        <div class="card-top">
                            <div>
                                <div class="card-title">
                                    <c:out value="${ap.activityName}"/>
                                    <small style="color:var(--muted);font-weight:400"> · <c:out value="${ap.activityCode}"/></small>
                                </div>
                                <div class="card-subtitle">
                                    👤 <c:out value="${ap.studentName}"/> (<c:out value="${ap.studentCode}"/>)
                                </div>
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

                        <p class="card-reason"><c:out value="${ap.reason}"/></p>

                        <div class="card-meta">
                            <span>📅 ${dt:format(ap.createdAt)}</span>
                            <c:if test="${ap.oldPoint != null}">
                                <span>📊 Điểm cũ: <strong><c:out value="${ap.oldPoint}"/></strong></span>
                            </c:if>
                            <c:if test="${ap.newPoint != null}">
                                <span style="color:var(--success)">📈 Điểm mới: <strong><c:out value="${ap.newPoint}"/></strong></span>
                            </c:if>
                            <c:if test="${not empty ap.reviewerName}">
                                <span>🧑‍💼 Xét bởi: <c:out value="${ap.reviewerName}"/></span>
                            </c:if>
                        </div>

                        <c:if test="${ap.status == 'PENDING'}">
                            <div class="card-actions">
                                <button class="btn btn-approve"
                                        onclick="openApproveModal(${ap.id}, '<c:out value="${ap.studentName}"/>')"
                                        id="btn-approve-${ap.id}">
                                    ✅ Chấp thuận
                                </button>
                                <button class="btn btn-reject"
                                        onclick="openRejectModal(${ap.id}, '<c:out value="${ap.studentName}"/>')"
                                        id="btn-reject-${ap.id}">
                                    ❌ Từ chối
                                </button>
                            </div>
                        </c:if>
                    </article>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </div>

</main>

<%-- ── Approve Modal ─────────────────────────────────────────────────────── --%>
<div class="modal-overlay" id="approve-modal" role="dialog" aria-modal="true" aria-labelledby="approve-modal-title">
    <div class="modal">
        <h2 class="modal-title" id="approve-modal-title">✅ Chấp thuận Phúc Khảo</h2>
        <p style="color:var(--muted);font-size:.85rem;margin-bottom:1rem">
            Sinh viên: <strong id="approve-student-name"></strong>
        </p>
        <form class="modal-form" method="post"
              action="${pageContext.request.contextPath}/appeal?action=approve">
            <input type="hidden" name="appealId" id="approve-appeal-id">

            <div>
                <label for="newPoint">Điểm mới sau phúc khảo *</label>
                <input type="number" id="newPoint" name="newPoint"
                       min="0" max="10" step="0.01" required placeholder="Ví dụ: 7.5">
            </div>

            <div>
                <label for="approve-note">Ghi chú nội bộ (tùy chọn)</label>
                <textarea id="approve-note" name="reviewNote" maxlength="2000"
                          placeholder="Lý do chấp thuận, điều chỉnh cụ thể..."></textarea>
            </div>

            <div class="modal-buttons">
                <button type="button" class="btn-cancel" onclick="closeModals()">Hủy</button>
                <button type="submit" class="btn-confirm-approve" id="confirm-approve-btn">Xác nhận chấp thuận</button>
            </div>
        </form>
    </div>
</div>

<%-- ── Reject Modal ──────────────────────────────────────────────────────── --%>
<div class="modal-overlay" id="reject-modal" role="dialog" aria-modal="true" aria-labelledby="reject-modal-title">
    <div class="modal">
        <h2 class="modal-title" id="reject-modal-title">❌ Từ chối Phúc Khảo</h2>
        <p style="color:var(--muted);font-size:.85rem;margin-bottom:1rem">
            Sinh viên: <strong id="reject-student-name"></strong>
        </p>
        <form class="modal-form" method="post"
              action="${pageContext.request.contextPath}/appeal?action=reject">
            <input type="hidden" name="appealId" id="reject-appeal-id">

            <div>
                <label for="reject-note">Lý do từ chối (hiển thị cho sinh viên) *</label>
                <textarea id="reject-note" name="reviewNote" maxlength="2000" required
                          placeholder="Giải thích lý do từ chối yêu cầu phúc khảo..."></textarea>
            </div>

            <div class="modal-buttons">
                <button type="button" class="btn-cancel" onclick="closeModals()">Hủy</button>
                <button type="submit" class="btn-confirm-reject" id="confirm-reject-btn">Xác nhận từ chối</button>
            </div>
        </form>
    </div>
</div>

<script>
    function openApproveModal(appealId, studentName) {
        document.getElementById('approve-appeal-id').value = appealId;
        document.getElementById('approve-student-name').textContent = studentName;
        document.getElementById('approve-modal').classList.add('active');
    }

    function openRejectModal(appealId, studentName) {
        document.getElementById('reject-appeal-id').value = appealId;
        document.getElementById('reject-student-name').textContent = studentName;
        document.getElementById('reject-modal').classList.add('active');
    }

    function closeModals() {
        document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('active'));
    }

    // Close on backdrop click
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
        overlay.addEventListener('click', e => {
            if (e.target === overlay) closeModals();
        });
    });

    // Close on Escape
    document.addEventListener('keydown', e => {
        if (e.key === 'Escape') closeModals();
    });
</script>

    </div>
</div>
</body>
</html>
