<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Chi tiết yêu cầu phúc khảo điểm sinh viên.">
    <title>Chi tiết Phúc Khảo #${appeal.id} - SWP391 G1</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261015">
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        :root {
            --bg: #0f1117; --surface: #1a1d27; --sf2: #22263a; --border: #2e3348;
            --accent: #6366f1; --accent2: #4f46e5;
            --success: #10b981; --warning: #f59e0b; --danger: #ef4444;
            --text: #e2e8f0; --muted: #94a3b8; --radius: 14px; --rsm: 8px;
        }
        body { font-family: 'Inter', sans-serif; background: var(--bg); color: var(--text); min-height: 100vh; }

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
            font-size: 1.1rem; color: #fff;
        }
        .nav-links { display: flex; gap: .75rem; }
        .nav-link { color: var(--muted); text-decoration: none; font-size: .85rem; font-weight: 500; padding: 6px 12px; border-radius: var(--rsm); transition: all .2s; }
        .nav-link:hover { color: var(--text); background: var(--sf2); }

        .container { max-width: 860px; margin: 0 auto; padding: 2rem 1.5rem; }

        .back-link { display: inline-flex; align-items: center; gap: 6px; color: var(--muted); text-decoration: none; font-size: .85rem; margin-bottom: 1.5rem; transition: color .2s; }
        .back-link:hover { color: var(--accent); }

        .detail-header { margin-bottom: 2rem; }
        .detail-header h1 { font-size: 1.5rem; font-weight: 700; margin-bottom: 0.3rem; }
        .detail-header p { color: var(--muted); font-size: .875rem; }

        .card { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); padding: 1.75rem; margin-bottom: 1.25rem; }
        .card-title { font-size: .85rem; font-weight: 600; color: var(--muted); text-transform: uppercase; letter-spacing: .05em; margin-bottom: 1rem; }

        .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
        @media(max-width:600px) { .info-grid { grid-template-columns: 1fr; } }
        .info-item label { font-size: .75rem; color: var(--muted); display: block; margin-bottom: 4px; }
        .info-item span { font-size: .9rem; font-weight: 500; }

        .badge { display: inline-flex; align-items: center; gap: 4px; padding: 4px 12px; border-radius: 20px; font-size: .78rem; font-weight: 600; }
        .badge-pending  { background: rgba(245,158,11,.15); color: var(--warning); }
        .badge-approved { background: rgba(16,185,129,.15);  color: var(--success); }
        .badge-rejected { background: rgba(239,68,68,.15);   color: var(--danger); }

        .reason-block { background: var(--sf2); border-radius: var(--rsm); padding: 1rem 1.25rem; font-size: .875rem; line-height: 1.65; color: var(--text); }

        .action-form { display: flex; flex-direction: column; gap: 1rem; }
        .action-form label { font-size: .8rem; font-weight: 600; color: var(--muted); display: block; margin-bottom: 4px; }
        .action-form input, .action-form textarea {
            width: 100%; background: var(--sf2); border: 1px solid var(--border);
            border-radius: var(--rsm); color: var(--text); font-size: .875rem;
            padding: .65rem .9rem; outline: none; font-family: inherit; transition: border-color .2s;
        }
        .action-form input:focus, .action-form textarea:focus { border-color: var(--accent); }
        .action-form textarea { min-height: 100px; resize: vertical; }

        .action-buttons { display: flex; gap: .75rem; flex-wrap: wrap; }
        .btn { border: none; border-radius: var(--rsm); padding: 9px 20px; font-size: .875rem; font-weight: 600; cursor: pointer; transition: all .15s; }
        .btn-approve { background: linear-gradient(135deg, var(--success), #059669); color: #fff; }
        .btn-approve:hover { opacity: .9; transform: translateY(-1px); }
        .btn-reject  { background: linear-gradient(135deg, var(--danger), #dc2626); color: #fff; }
        .btn-reject:hover  { opacity: .9; transform: translateY(-1px); }

        .point-updated { display: flex; align-items: center; gap: 1rem; font-size: 1rem; }
        .arrow { color: var(--muted); }
        .new-point { color: var(--success); font-size: 1.25rem; font-weight: 700; }
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
        <a href="${pageContext.request.contextPath}/appeal?action=manage" class="nav-link">← Danh sách</a>
        <a href="${pageContext.request.contextPath}/home" class="nav-link">Trang chủ</a>
    </nav>
</header>

<main class="container">

    <a class="back-link" href="${pageContext.request.contextPath}/appeal?action=manage">← Quay lại danh sách</a>

    <c:if test="${not empty successMsg}">
        <div style="background:rgba(16,185,129,.1);color:#6ee7b7;border:1px solid rgba(16,185,129,.25);border-radius:8px;padding:.85rem 1.25rem;margin-bottom:1.5rem;font-size:.875rem;">
            <c:out value="${successMsg}"/>
        </div>
    </c:if>

    <div class="detail-header">
        <h1>Yêu cầu Phúc Khảo #${appeal.id}</h1>
        <p>Xét duyệt và ghi nhận kết quả phúc khảo</p>
    </div>

    <%-- ── Appeal Info ─────────────────────────────────────────────────────── --%>
    <div class="card">
        <p class="card-title">Thông tin yêu cầu</p>
        <div class="info-grid">
            <div class="info-item">
                <label>Sinh viên</label>
                <span><c:out value="${appeal.studentName}"/> (<c:out value="${appeal.studentCode}"/>)</span>
            </div>
            <div class="info-item">
                <label>Hoạt động</label>
                <span><c:out value="${appeal.activityName}"/> · <c:out value="${appeal.activityCode}"/></span>
            </div>
            <div class="info-item">
                <label>Trạng thái</label>
                <span>
                    <c:choose>
                        <c:when test="${appeal.status == 'PENDING'}"><span class="badge badge-pending">⏳ Đang chờ</span></c:when>
                        <c:when test="${appeal.status == 'APPROVED'}"><span class="badge badge-approved">✅ Chấp thuận</span></c:when>
                        <c:otherwise><span class="badge badge-rejected">❌ Từ chối</span></c:otherwise>
                    </c:choose>
                </span>
            </div>
            <div class="info-item">
                <label>Ngày gửi</label>
                <span>${dt:format(appeal.createdAt)}</span>
            </div>
            <c:if test="${appeal.oldPoint != null}">
                <div class="info-item">
                    <label>Điểm hiện tại</label>
                    <span><c:out value="${appeal.oldPoint}"/></span>
                </div>
            </c:if>
            <c:if test="${appeal.newPoint != null}">
                <div class="info-item">
                    <label>Điểm sau phúc khảo</label>
                    <span style="color:var(--success);font-weight:700"><c:out value="${appeal.newPoint}"/></span>
                </div>
            </c:if>
            <c:if test="${not empty appeal.reviewerName}">
                <div class="info-item">
                    <label>Người xét duyệt</label>
                    <span><c:out value="${appeal.reviewerName}"/></span>
                </div>
            </c:if>
            <c:if test="${appeal.reviewedAt != null}">
                <div class="info-item">
                    <label>Ngày xét duyệt</label>
                    <span>${dt:format(appeal.reviewedAt)}</span>
                </div>
            </c:if>
        </div>
    </div>

    <div class="card">
        <p class="card-title">Lý do phúc khảo</p>
        <div class="reason-block"><c:out value="${appeal.reason}"/></div>
    </div>

    <c:if test="${not empty appeal.reviewNote}">
        <div class="card">
            <p class="card-title">Ghi chú xét duyệt</p>
            <div class="reason-block" style="color:var(--muted);font-style:italic">
                <c:out value="${appeal.reviewNote}"/>
            </div>
        </div>
    </c:if>

    <%-- ── Action area (only for PENDING) ─────────────────────────────────── --%>
    <c:if test="${appeal.status == 'PENDING'}">
        <div class="card">
            <p class="card-title">Xét duyệt</p>
            <form class="action-form" method="post"
                  action="${pageContext.request.contextPath}/appeal?action=approve">
                <input type="hidden" name="appealId" value="${appeal.id}">

                <div>
                    <label for="newPoint">Điểm mới *</label>
                    <input type="number" id="newPoint" name="newPoint" min="0" max="10" step="0.01" required placeholder="Ví dụ: 7.5">
                </div>
                <div>
                    <label for="approveNote">Ghi chú (tùy chọn)</label>
                    <textarea id="approveNote" name="reviewNote" maxlength="2000" placeholder="Điều chỉnh cụ thể, lý do chấp thuận..."></textarea>
                </div>
                <div class="action-buttons">
                    <button id="approve-btn" type="submit" class="btn btn-approve">✅ Chấp thuận phúc khảo</button>
                </div>
            </form>

            <hr style="border:none;border-top:1px solid var(--border);margin:1.5rem 0">

            <form class="action-form" method="post"
                  action="${pageContext.request.contextPath}/appeal?action=reject">
                <input type="hidden" name="appealId" value="${appeal.id}">
                <div>
                    <label for="rejectNote">Lý do từ chối (hiển thị cho sinh viên) *</label>
                    <textarea id="rejectNote" name="reviewNote" maxlength="2000" required
                              placeholder="Giải thích lý do từ chối..."></textarea>
                </div>
                <div class="action-buttons">
                    <button id="reject-btn" type="submit" class="btn btn-reject">❌ Từ chối phúc khảo</button>
                </div>
            </form>
        </div>
    </c:if>

</main>

    </div>
</div>
</body>
</html>
