<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hộp thư sinh viên - SWP391 G1</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

        :root {
            --bg: #0f1117;
            --surface: #1a1d27;
            --surface2: #22263a;
            --border: #2e3348;
            --accent: #6c63ff;
            --accent2: #8b5cf6;
            --text: #e2e8f0;
            --text-muted: #94a3b8;
            --radius: 12px;
        }

        body { font-family: 'Inter', sans-serif; background: var(--bg); color: var(--text); min-height: 100vh; }

        .header {
            background: var(--surface); border-bottom: 1px solid var(--border);
            padding: 0 2rem; display: flex; align-items: center;
            justify-content: space-between; height: 64px;
            position: sticky; top: 0; z-index: 100;
        }

        .logo { display: flex; align-items: center; gap: 10px; font-weight: 700; color: var(--accent); }
        .logo-icon {
            width: 36px; height: 36px;
            background: linear-gradient(135deg, var(--accent), var(--accent2));
            border-radius: 8px; display: flex; align-items: center; justify-content: center;
        }

        .nav-links { display: flex; gap: 0.5rem; }
        .nav-link {
            color: var(--text-muted); text-decoration: none; font-size: 0.875rem;
            font-weight: 500; padding: 6px 12px; border-radius: 8px; transition: all 0.2s;
        }
        .nav-link:hover { color: var(--text); background: var(--surface2); }
        .nav-link.active { color: var(--text); background: var(--surface2); }

        .container { max-width: 1100px; margin: 0 auto; padding: 2rem; }

        .page-title {
            font-size: 1.75rem; font-weight: 700; margin-bottom: 0.25rem;
            background: linear-gradient(135deg, #e2e8f0, var(--accent));
            -webkit-background-clip: text; -webkit-text-fill-color: transparent;
        }
        .page-subtitle { color: var(--text-muted); font-size: 0.875rem; margin-bottom: 2rem; }

        /* Layout: sidebar + main */
        .inbox-layout { display: grid; grid-template-columns: 320px 1fr; gap: 1.5rem; }

        /* Staff list sidebar */
        .staff-sidebar {
            background: var(--surface); border: 1px solid var(--border);
            border-radius: var(--radius); overflow: hidden;
        }

        .sidebar-header {
            padding: 1.25rem 1.5rem; border-bottom: 1px solid var(--border);
            font-weight: 700; font-size: 0.9rem; color: var(--text-muted);
        }

        .staff-list { max-height: 70vh; overflow-y: auto; }

        .staff-item {
            display: flex; align-items: center; gap: 0.875rem;
            padding: 1rem 1.5rem; text-decoration: none; color: var(--text);
            border-bottom: 1px solid var(--border); transition: all 0.15s;
        }
        .staff-item:hover, .staff-item.active { background: var(--surface2); }
        .staff-item:last-child { border-bottom: none; }

        .staff-avatar {
            width: 42px; height: 42px; border-radius: 50%; flex-shrink: 0;
            background: linear-gradient(135deg, var(--accent), var(--accent2));
            display: flex; align-items: center; justify-content: center;
            font-weight: 700; font-size: 1rem; color: #fff;
        }

        .staff-info .staff-name { font-weight: 600; font-size: 0.9rem; }
        .staff-info .staff-code { font-size: 0.75rem; color: var(--text-muted); margin-top: 2px; }

        .arrow { margin-left: auto; color: var(--text-muted); font-size: 0.875rem; }

        /* Recent messages panel */
        .messages-panel {
            background: var(--surface); border: 1px solid var(--border);
            border-radius: var(--radius); padding: 2rem;
        }

        .panel-title { font-size: 1rem; font-weight: 700; margin-bottom: 1.5rem; }

        .empty-panel { text-align: center; padding: 3rem; color: var(--text-muted); }
        .empty-icon { font-size: 2.5rem; margin-bottom: 0.75rem; }

        .recent-item {
            display: flex; gap: 1rem; align-items: flex-start;
            padding: 1rem; border-radius: 10px; border: 1px solid var(--border);
            margin-bottom: 0.75rem; background: var(--surface2);
        }

        .msg-avatar {
            width: 38px; height: 38px; border-radius: 50%; flex-shrink: 0;
            background: linear-gradient(135deg, #10b981, #059669);
            display: flex; align-items: center; justify-content: center;
            font-size: 0.875rem; font-weight: 700; color: #fff;
        }

        .msg-info .msg-to { font-size: 0.75rem; color: var(--text-muted); margin-bottom: 4px; }
        .msg-info .msg-content {
            font-size: 0.875rem; color: var(--text);
            display: -webkit-box; -webkit-line-clamp: 2;
            -webkit-box-orient: vertical; overflow: hidden;
        }
        .msg-date { margin-left: auto; font-size: 0.75rem; color: var(--text-muted); white-space: nowrap; }

        @media (max-width: 768px) {
            .inbox-layout { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>

<header class="header">
    <div class="logo">
        <div class="logo-icon">📩</div>
        &nbsp;Student Inbox
    </div>
    <nav class="nav-links">
        <a href="${pageContext.request.contextPath}/qa-management" class="nav-link">Q&A</a>
        <a href="${pageContext.request.contextPath}/student-inbox" class="nav-link active">Inbox</a>
    </nav>
</header>

<main class="container">
    <h1 class="page-title">Hộp thư của tôi</h1>
    <p class="page-subtitle">Gửi tin nhắn riêng tư đến ban tổ chức / nhân viên hỗ trợ</p>

    <div class="inbox-layout">

        <%-- Staff sidebar --%>
        <div class="staff-sidebar">
            <div class="sidebar-header">📋 Chọn người nhận</div>
            <div class="staff-list">
                <c:choose>
                    <c:when test="${empty staffList}">
                        <div style="padding: 2rem; text-align:center; color: var(--text-muted); font-size:0.875rem;">
                            Không có nhân viên nào
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="staff" items="${staffList}">
                            <a href="${pageContext.request.contextPath}/student-inbox?action=chat&staffId=${staff.id}"
                               class="staff-item">
                                <div class="staff-avatar">
                                    ${staff.name.substring(0,1)}
                                </div>
                                <div class="staff-info">
                                    <div class="staff-name">${staff.name}</div>
                                    <div class="staff-code">${staff.code}</div>
                                </div>
                                <span class="arrow">→</span>
                            </a>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <%-- Recent messages panel --%>
        <div class="messages-panel">
            <div class="panel-title">📬 Tin nhắn đã gửi gần đây</div>
            <c:choose>
                <c:when test="${empty recentMessages}">
                    <div class="empty-panel">
                        <div class="empty-icon">📭</div>
                        <div>Bạn chưa gửi tin nhắn nào.<br/>Chọn một nhân viên bên trái để bắt đầu.</div>
                    </div>
                </c:when>
                <c:otherwise>
                    <c:forEach var="msg" items="${recentMessages}">
                        <div class="recent-item">
                            <div class="msg-avatar">
                                ${msg.staffName != null ? msg.staffName.substring(0,1) : 'S'}
                            </div>
                            <div class="msg-info">
                                <div class="msg-to">Gửi tới: ${msg.staffName} (${msg.staffCode})</div>
                                <div class="msg-content">${msg.content}</div>
                            </div>
                            <div class="msg-date">${msg.sentAt}</div>
                        </div>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </div>

    </div>
</main>

</body>
</html>
