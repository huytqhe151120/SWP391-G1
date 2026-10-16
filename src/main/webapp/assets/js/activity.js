document.querySelectorAll("a[data-confirm]").forEach(function (link) {
    link.addEventListener("click", function (event) {
        if (!window.confirm(link.dataset.confirm)) {
            event.preventDefault();
        }
    });
});

document.querySelectorAll("button[data-confirm]").forEach(function (button) {
    button.addEventListener("click", function (event) {
        if (!window.confirm(button.dataset.confirm)) {
            event.preventDefault();
        }
    });
});

document.querySelectorAll("form[data-confirm]").forEach(function (form) {
    form.addEventListener("submit", function (event) {
        if (!window.confirm(form.dataset.confirm)) {
            event.preventDefault();
        }
    });
});

(function () {
    var departmentSelect = document.getElementById("responsibleDepartmentId");
    var responsibleStaffSelect = document.getElementById("responsibleStaffId");
    var message = document.getElementById("responsibleStaffMessage");
    if (!departmentSelect || !responsibleStaffSelect || !message) {
        return;
    }

    var requestVersion = 0;
    var endpoint = departmentSelect.dataset.staffEndpoint;

    async function loadDepartmentStaff() {
        var departmentId = departmentSelect.value;
        var currentRequest = ++requestVersion;
        responsibleStaffSelect.replaceChildren(new Option("-- Chọn nhân viên --", ""));
        responsibleStaffSelect.disabled = !departmentId;
        message.textContent = "";
        if (!departmentId) {
            responsibleStaffSelect.options[0].textContent = "-- Chọn đơn vị phụ trách trước --";
            return;
        }

        var url = new URL(endpoint, document.baseURI);
        url.searchParams.set("action", "getStaffsByDepartment");
        url.searchParams.set("departmentId", departmentId);
        try {
            var response = await fetch(url.toString());
            if (!response.ok) {
                throw new Error("Không thể tải danh sách nhân sự của đơn vị.");
            }
            if (!response.headers.get("content-type")?.includes("application/json")) {
                throw new Error("Máy chủ không trả về dữ liệu nhân sự dạng JSON. Hãy tải lại ứng dụng.");
            }
            var staffs = await response.json();
            if (currentRequest !== requestVersion) {
                return;
            }
            staffs.forEach(function (staff) {
                responsibleStaffSelect.add(new Option(staff.code + " - " + staff.name, staff.id));
            });
        } catch (error) {
            if (currentRequest === requestVersion) {
                responsibleStaffSelect.replaceChildren(new Option("Không tải được danh sách nhân sự.", ""));
                message.textContent = error.message;
            }
        }
    }

    departmentSelect.addEventListener("change", loadDepartmentStaff);
})();

(function () {
    var table = document.querySelector(".activity-list-page .activity-table-wrap table");
    if (!table) {
        return;
    }

    function syncStickyColumnWidths() {
        var actionsCells = table.querySelectorAll("tr td:nth-child(9), tr th:nth-child(9)");
        var approvalCells = table.querySelectorAll("tr td:nth-child(8), tr th:nth-child(8)");
        var actionsWidth = 0;
        var approvalWidth = 0;

        actionsCells.forEach(function (cell) {
            actionsWidth = Math.max(actionsWidth, cell.getBoundingClientRect().width);
        });
        approvalCells.forEach(function (cell) {
            approvalWidth = Math.max(approvalWidth, cell.getBoundingClientRect().width);
        });
        if (actionsWidth > 0 && approvalWidth > 0) {
            table.style.setProperty("--activity-actions-col-width", actionsWidth + "px");
            table.style.setProperty("--activity-approval-col-width", approvalWidth + "px");
        }
    }

    window.addEventListener("load", syncStickyColumnWidths);
    window.addEventListener("resize", syncStickyColumnWidths);
})();

(function () {
    var companySelect = document.getElementById("partnerCompanyId");
    var staffSelect = document.getElementById("partnerStaffId");
    var message = document.getElementById("partnerStaffMessage");
    if (!companySelect || !staffSelect || !message) {
        return;
    }

    var selectedStaffId = staffSelect.dataset.selected;
    var endpoint = staffSelect.dataset.staffEndpoint;

    async function loadPartnerStaff() {
        var companyId = companySelect.value;
        staffSelect.replaceChildren(new Option("-- Không có --", ""));
        staffSelect.disabled = !companyId;
        message.textContent = "";
        if (!companyId) {
            return;
        }

        var url = new URL(endpoint, document.baseURI);
        url.searchParams.set("action", "getStaffsByCompany");
        url.searchParams.set("companyId", companyId);
        try {
            var response = await fetch(url.toString());
            if (!response.ok) {
                throw new Error("Không thể tải danh sách nhân viên đối tác.");
            }
            var staffs = await response.json();
            staffs.forEach(function (staff) {
                var option = new Option(staff.name, staff.id);
                if (String(staff.id) === selectedStaffId) {
                    option.selected = true;
                }
                staffSelect.add(option);
            });
        } catch (error) {
            message.textContent = error.message;
        }
    }

    companySelect.addEventListener("change", function () {
        selectedStaffId = "";
        loadPartnerStaff();
    });
    if (companySelect.value) {
        loadPartnerStaff();
    }
})();

(function () {
    var tabButtons = document.querySelectorAll(".activity-tab-btn");
    var tabPanes = document.querySelectorAll(".activity-tab-pane");
    if (!tabButtons.length || !tabPanes.length) {
        return;
    }

    function switchTab(targetId) {
        tabButtons.forEach(function (button) {
            var isMatch = button.getAttribute("data-tab") === targetId;
            button.classList.toggle("active", isMatch);
            button.setAttribute("aria-selected", isMatch ? "true" : "false");
        });
        tabPanes.forEach(function (pane) {
            pane.classList.toggle("active", pane.id === targetId);
        });
    }

    tabButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            var tabId = button.getAttribute("data-tab");
            switchTab(tabId);
            if (window.history && window.history.replaceState) {
                window.history.replaceState(null, null, "#" + tabId);
            }
        });
    });

    var currentHash = window.location.hash.slice(1);
    if (["tab-staff", "tab-info", "tab-partner"].indexOf(currentHash) !== -1) {
        switchTab(currentHash);
    }
})();
