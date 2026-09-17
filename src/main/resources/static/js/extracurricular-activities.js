(() => {
    const apiUrl = "/api/extracurricular-activities";
    const state = { activities: [], editingId: null };
    const form = document.querySelector("#activity-form");
    const modal = document.querySelector("#activity-modal");
    const tableBody = document.querySelector("#activity-table-body");
    const emptyState = document.querySelector("#empty-state");
    const notice = document.querySelector("#notice");
    const searchInput = document.querySelector("#search-input");
    const statusFilter = document.querySelector("#status-filter");
    const csrfToken = document.querySelector("meta[name='csrf-token']")?.content;
    const csrfHeader = document.querySelector("meta[name='csrf-header']")?.content;

    const statusLabels = {
        NOT_STARTED: "Not started",
        IN_PROGRESS: "In progress",
        COMPLETED: "Completed",
        CANCELLED: "Cancelled"
    };

    const escapeHtml = (value) => String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");

    const statusClass = (status) => String(status ?? "").toLowerCase().replace("_", "-");

    function showNotice(message) {
        notice.textContent = message;
        notice.hidden = false;
    }

    function clearNotice() {
        notice.hidden = true;
        notice.textContent = "";
    }

    async function request(url, options = {}) {
        const headers = { "Content-Type": "application/json", ...(options.headers ?? {}) };
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
        const response = await fetch(url, { ...options, headers });
        if (!response.ok) {
            const message = response.status === 404
                ? "That activity no longer exists. Refresh the register and try again."
                : `Request failed (${response.status}). Please try again.`;
            throw new Error(message);
        }
        return response.status === 204 ? null : response.json();
    }

    function getFilteredActivities() {
        const term = searchInput.value.trim().toLowerCase();
        const status = statusFilter.value;
        return state.activities.filter((activity) => {
            const searchable = [activity.code, activity.name, activity.address, activity.activityType]
                .filter(Boolean).join(" ").toLowerCase();
            return (!term || searchable.includes(term)) && (!status || activity.activityStatus === status);
        });
    }

    function renderSummary() {
        const total = state.activities.length;
        const inProgress = state.activities.filter((item) => item.activityStatus === "IN_PROGRESS").length;
        const cancelled = state.activities.filter((item) => item.activityStatus === "CANCELLED").length;
        const points = state.activities.reduce((sum, item) => sum + Number(item.bonusPoint || 0), 0);
        document.querySelector("#total-count").textContent = total;
        document.querySelector("#progress-count").textContent = inProgress;
        document.querySelector("#cancelled-count").textContent = cancelled;
        document.querySelector("#points-count").textContent = points.toLocaleString();
    }

    function renderTable() {
        const activities = getFilteredActivities();
        document.querySelector("#result-count").textContent = `${activities.length} of ${state.activities.length} records`;
        emptyState.hidden = activities.length !== 0;
        tableBody.innerHTML = activities.map((activity) => `
            <tr>
                <td>
                    <div class="activity-title">${escapeHtml(activity.name)}</div>
                    <div class="activity-code">${escapeHtml(activity.code)} · ${escapeHtml(activity.activityType)}</div>
                </td>
                <td><span class="secondary-text">Semester ${escapeHtml(activity.semesterId)}</span></td>
                <td><span class="secondary-text">${escapeHtml(activity.address || "Not assigned")}</span></td>
                <td><span class="status-badge ${statusClass(activity.activityStatus)}">${escapeHtml(statusLabels[activity.activityStatus] || activity.activityStatus)}</span></td>
                <td><span class="points">+${escapeHtml(activity.bonusPoint)} <small>/ -${escapeHtml(activity.penaltyPoint)}</small></span></td>
                <td>
                    <div class="row-actions">
                        <button class="action-button" data-action="edit" data-id="${escapeHtml(activity.id)}" type="button">Edit</button>
                        <button class="action-button danger" data-action="delete" data-id="${escapeHtml(activity.id)}" type="button">Delete</button>
                    </div>
                </td>
            </tr>
        `).join("");
        renderSummary();
    }

    async function loadActivities() {
        clearNotice();
        tableBody.innerHTML = `<tr><td colspan="6" class="secondary-text">Loading activity records...</td></tr>`;
        try {
            state.activities = await request(apiUrl, { method: "GET" });
            renderTable();
        } catch (error) {
            tableBody.innerHTML = "";
            emptyState.hidden = false;
            showNotice(error.message);
        }
    }

    function openModal(activity = null) {
        state.editingId = activity?.id ?? null;
        form.reset();
        document.querySelector("#modal-eyebrow").textContent = activity ? "EDIT RECORD" : "NEW RECORD";
        document.querySelector("#modal-title").textContent = activity ? "Edit activity" : "Create activity";
        document.querySelector("#save-button").textContent = activity ? "Save changes" : "Save activity";
        form.elements.activityStatus.value = activity?.activityStatus ?? "NOT_STARTED";
        form.elements.approvalStatus.value = activity?.approvalStatus ?? "PENDING";
        form.elements.bonusPoint.value = activity?.bonusPoint ?? 0;
        form.elements.penaltyPoint.value = activity?.penaltyPoint ?? 0;
        if (activity) {
            Object.keys(activity).forEach((key) => {
                if (form.elements[key] && activity[key] !== null && activity[key] !== undefined) {
                    form.elements[key].value = activity[key];
                }
            });
        }
        modal.hidden = false;
        form.elements.code.focus();
    }

    function closeModal() {
        modal.hidden = true;
        state.editingId = null;
    }

    function numberOrNull(value) {
        return value === "" ? null : Number(value);
    }

    function formPayload() {
        const data = new FormData(form);
        return {
            semesterId: numberOrNull(data.get("semesterId")),
            activityType: data.get("activityType"),
            code: data.get("code"),
            name: data.get("name"),
            responsibleDepartmentId: numberOrNull(data.get("responsibleDepartmentId")),
            responsibleStaffId: numberOrNull(data.get("responsibleStaffId")),
            partnerId: numberOrNull(data.get("partnerId")),
            partnerStaffId: numberOrNull(data.get("partnerStaffId")),
            bonusPoint: numberOrNull(data.get("bonusPoint")),
            penaltyPoint: numberOrNull(data.get("penaltyPoint")),
            address: data.get("address") || null,
            description: data.get("description") || null,
            activityStatus: data.get("activityStatus"),
            approvalStatus: data.get("approvalStatus")
        };
    }

    async function saveActivity(event) {
        event.preventDefault();
        clearNotice();
        const wasEditing = Boolean(state.editingId);
        const saveButton = document.querySelector("#save-button");
        saveButton.disabled = true;
        saveButton.textContent = "Saving...";
        try {
            const url = state.editingId ? `${apiUrl}/${state.editingId}` : apiUrl;
            await request(url, { method: state.editingId ? "PUT" : "POST", body: JSON.stringify(formPayload()) });
            closeModal();
            await loadActivities();
            showNotice(wasEditing ? "Activity updated successfully." : "Activity created successfully.");
        } catch (error) {
            showNotice(error.message);
        } finally {
            saveButton.disabled = false;
            saveButton.textContent = state.editingId ? "Save changes" : "Save activity";
        }
    }

    async function deleteActivity(id) {
        const activity = state.activities.find((item) => String(item.id) === String(id));
        if (!activity || !window.confirm(`Delete “${activity.name}”? This cannot be undone.`)) return;
        try {
            await request(`${apiUrl}/${id}`, { method: "DELETE" });
            await loadActivities();
            showNotice("Activity deleted successfully.");
        } catch (error) {
            showNotice(error.message);
        }
    }

    tableBody.addEventListener("click", (event) => {
        const button = event.target.closest("button[data-action]");
        if (!button) return;
        const activity = state.activities.find((item) => String(item.id) === button.dataset.id);
        if (button.dataset.action === "edit") openModal(activity);
        if (button.dataset.action === "delete") deleteActivity(button.dataset.id);
    });

    form.addEventListener("submit", saveActivity);
    searchInput.addEventListener("input", renderTable);
    statusFilter.addEventListener("change", renderTable);
    document.querySelector("#refresh-button").addEventListener("click", loadActivities);
    document.querySelector("#open-create").addEventListener("click", () => openModal());
    document.querySelector("#empty-create").addEventListener("click", () => openModal());
    document.querySelector("#close-modal").addEventListener("click", closeModal);
    document.querySelector("#cancel-modal").addEventListener("click", closeModal);
    modal.addEventListener("click", (event) => { if (event.target === modal) closeModal(); });
    document.addEventListener("keydown", (event) => { if (event.key === "Escape" && !modal.hidden) closeModal(); });

    loadActivities();
})();
