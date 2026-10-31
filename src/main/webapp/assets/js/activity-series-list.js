(function () {
    var ctx = "";
    var expandedSeriesId = null;
    var occurrenceCache = {};
    var loadingSeriesId = null;

    function init() {
        var ctxEl = document.querySelector("script[src*='activity-series-list.js']");
        if (ctxEl) {
            var src = ctxEl.getAttribute("src") || "";
            var m = src.match(/\?v=/);
            if (m) ctx = src.substring(0, m.index);
        }
        setupExpandButtons();
        setupRetryButtons();
    }

    function setupExpandButtons() {
        document.querySelectorAll(".series-expand-btn").forEach(function (btn) {
            btn.addEventListener("click", function (e) {
                e.stopPropagation();
                var seriesId = parseInt(btn.getAttribute("data-series-id"), 10);
                toggleSeries(seriesId, btn);
            });
        });
    }

    function setupRetryButtons() {
        document.querySelectorAll(".series-retry-btn").forEach(function (btn) {
            btn.addEventListener("click", function (e) {
                e.stopPropagation();
                var seriesId = parseInt(btn.getAttribute("data-series-id"), 10);
                loadOccurrences(seriesId);
            });
        });
    }

    function toggleSeries(seriesId, btn) {
        if (loadingSeriesId === seriesId) return;
        if (expandedSeriesId === seriesId) {
            collapseSeries(seriesId, btn);
        } else {
            if (expandedSeriesId !== null) {
                var prevBtn = document.querySelector(".series-expand-btn[data-series-id=" + expandedSeriesId + "]");
                if (prevBtn) collapseSeries(expandedSeriesId, prevBtn);
            }
            expandSeries(seriesId, btn);
        }
    }

    function collapseSeries(seriesId, btn) {
        var childRow = document.querySelector(".series-child-row[data-series-id=" + seriesId + "]");
        var parentRow = document.querySelector(".series-parent-row[data-series-id=" + seriesId + "]");
        if (childRow) childRow.style.display = "none";
        if (parentRow) parentRow.setAttribute("data-expanded", "false");
        if (btn) {
            btn.setAttribute("aria-expanded", "false");
            var chevron = btn.querySelector(".chevron");
            if (chevron) chevron.textContent = "▸";
        }
        expandedSeriesId = null;
    }

    function expandSeries(seriesId, btn) {
        var childRow = document.querySelector(".series-child-row[data-series-id=" + seriesId + "]");
        var parentRow = document.querySelector(".series-parent-row[data-series-id=" + seriesId + "]");
        if (childRow) childRow.style.display = "";
        if (parentRow) parentRow.setAttribute("data-expanded", "true");
        if (btn) {
            btn.setAttribute("aria-expanded", "true");
            var chevron = btn.querySelector(".chevron");
            if (chevron) chevron.textContent = "▾";
        }
        expandedSeriesId = seriesId;
        if (occurrenceCache[seriesId]) {
            renderOccurrences(seriesId, occurrenceCache[seriesId]);
        } else if (!loadingSeriesId || loadingSeriesId !== seriesId) {
            loadOccurrences(seriesId);
        }
    }

    function loadOccurrences(seriesId) {
        loadingSeriesId = seriesId;
        var loadingEl = document.getElementById("series-loading-" + seriesId);
        var errorEl = document.getElementById("series-error-" + seriesId);
        var emptyEl = document.getElementById("series-empty-" + seriesId);
        var bodyEl = document.getElementById("occurrence-body-" + seriesId);
        if (loadingEl) loadingEl.style.display = "flex";
        if (errorEl) errorEl.style.display = "none";
        if (emptyEl) emptyEl.style.display = "none";
        if (bodyEl) bodyEl.innerHTML = "";
        var url = ctx + "/activity-series?action=listOccurrences&seriesId=" + seriesId;
        fetch(url, { credentials: "same-origin" })
            .then(function (response) {
                if (!response.ok) throw new Error("HTTP " + response.status);
                return response.json();
            })
            .then(function (data) {
                if (loadingSeriesId !== seriesId) return;
                loadingSeriesId = null;
                if (loadingEl) loadingEl.style.display = "none";
                if (data.success && Array.isArray(data.data)) {
                    occurrenceCache[seriesId] = data.data;
                    if (data.data.length === 0) {
                        if (emptyEl) emptyEl.style.display = "block";
                    } else {
                        renderOccurrences(seriesId, data.data);
                    }
                } else {
                    showError(seriesId, data.message || "Không thể tải dữ liệu.");
                }
            })
            .catch(function (err) {
                if (loadingSeriesId !== seriesId) return;
                loadingSeriesId = null;
                if (loadingEl) loadingEl.style.display = "none";
                showError(seriesId, err.message || "Lỗi kết nối.");
            });
    }

    function showError(seriesId, message) {
        var errorEl = document.getElementById("series-error-" + seriesId);
        var errorText = errorEl ? errorEl.querySelector(".error-text") : null;
        if (errorEl) errorEl.style.display = "flex";
        if (errorText) errorText.textContent = message;
    }

    function renderOccurrences(seriesId, occurrences) {
        var bodyEl = document.getElementById("occurrence-body-" + seriesId);
        if (!bodyEl) return;
        bodyEl.innerHTML = "";
        occurrences.forEach(function (a) {
            var tr = document.createElement("tr");
            var statusClass = "";
            var statusLabel = "";
            if (a.approvalStatus) {
                var lower = a.approvalStatus.toLowerCase();
                if (lower === "approved") statusClass = "status-green";
                else if (lower === "rejected") statusClass = "status-red";
                else if (lower === "pending") statusClass = "status-yellow";
                else statusClass = "status-gray";
                statusLabel = a.approvalStatus;
            }
            var actStatusClass = "";
            var actStatusLabel = "";
            if (a.activityStatus) {
                var al = a.activityStatus.toLowerCase();
                actStatusClass = "status-" + al;
                actStatusLabel = a.activityStatus;
            }
            var startTime = a.startTime ? a.startTime.replace("T", " ") : "";
            var endTime = a.endTime ? a.endTime.replace("T", " ") : "";
            var detailUrl = ctx + "/activities?action=detail&id=" + a.id;
            tr.innerHTML =
                "<td><code>" + escapeHtml(a.code) + "</code></td>" +
                "<td><a href=\"" + detailUrl + "\">" + escapeHtml(a.name) + "</a></td>" +
                "<td>" + escapeHtml(startTime) + "</td>" +
                "<td>" + escapeHtml(endTime) + "</td>" +
                "<td><span class=\"badge " + actStatusClass + "\">" + escapeHtml(actStatusLabel) + "</span></td>" +
                "<td><span class=\"badge " + statusClass + "\">" + escapeHtml(statusLabel) + "</span></td>" +
                "<td class=\"activity-table-actions\"><a class=\"button button-outline\" href=\"" + detailUrl + "\">Chi tiết</a></td>";
            bodyEl.appendChild(tr);
        });
    }

    function escapeHtml(text) {
        if (!text) return "";
        var div = document.createElement("div");
        div.textContent = text;
        return div.innerHTML;
    }

    document.addEventListener("DOMContentLoaded", init);
})();