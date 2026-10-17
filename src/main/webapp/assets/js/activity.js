(function () {
    var panel = document.getElementById("activity-filter-panel");
    if (!panel) {
        return;
    }
    var storageKey = "activityFilterPanelOpen";
    try {
        panel.open = localStorage.getItem(storageKey) === "true";
    } catch (error) {
        return;
    }
    panel.addEventListener("toggle", function () {
        try {
            localStorage.setItem(storageKey, String(panel.open));
        } catch (error) {
            // Storage unavailable: keep default behavior.
        }
    });
})();

document.querySelectorAll(".js-date-input").forEach(function (input) {
    input.addEventListener("input", function () {
        var digits = input.value.replace(/\D/g, "").slice(0, 8);
        var parts = [digits.slice(0, 2), digits.slice(2, 4), digits.slice(4)].filter(Boolean);
        input.value = parts.join("/");
    });
});

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

document.querySelectorAll("[data-multi-select]").forEach(function (combobox) {
    var trigger = combobox.querySelector(".multi-select-trigger");
    var label = combobox.querySelector("[data-selection-label]");
    var dropdown = combobox.querySelector(".multi-select-dropdown");
    var search = combobox.querySelector(".multi-select-search");
    var options = Array.from(combobox.querySelectorAll(".multi-select-option"));
    var emptyState = combobox.querySelector(".multi-select-empty");
    var count = combobox.querySelector(".multi-select-count");
    var initialLabel = label.textContent.trim();
    var itemLabel = combobox.dataset.itemLabel;

    function updateSelection() {
        var selectedOptions = options.filter(function (option) {
            return option.querySelector("input").checked;
        });
        var selectedCount = selectedOptions.length;
        if (selectedCount === 0) {
            label.textContent = initialLabel;
            count.textContent = "Chưa chọn " + itemLabel;
        } else {
            label.textContent = selectedOptions.slice(0, 2).map(function (option) {
                return option.querySelector("span").textContent.trim();
            }).join(", ") + (selectedCount > 2 ? " +" + (selectedCount - 2) : "");
            count.textContent = "Đã chọn " + selectedCount + " " + itemLabel;
        }
    }

    function filterOptions() {
        var query = search.value.trim().toLocaleLowerCase();
        var visibleCount = 0;
        options.forEach(function (option) {
            var matches = option.textContent.toLocaleLowerCase().includes(query);
            option.hidden = !matches;
            if (matches) {
                visibleCount++;
            }
        });
        emptyState.hidden = visibleCount !== 0;
    }

    function positionDropdown() {
        var spaceBelow = window.innerHeight - trigger.getBoundingClientRect().bottom;
        var spaceAbove = trigger.getBoundingClientRect().top;
        combobox.classList.toggle("opens-up", spaceBelow < 280 && spaceAbove > spaceBelow);
    }

    trigger.addEventListener("click", function () {
        var isExpanded = trigger.getAttribute("aria-expanded") === "true";
        trigger.setAttribute("aria-expanded", String(!isExpanded));
        dropdown.hidden = isExpanded;
        combobox.classList.toggle("is-open", !isExpanded);
        if (!isExpanded) {
            positionDropdown();
            search.focus();
        }
    });

    window.addEventListener("resize", function () {
        if (!dropdown.hidden) {
            positionDropdown();
        }
    });

    search.addEventListener("input", filterOptions);
    options.forEach(function (option) {
        option.querySelector("input").addEventListener("change", updateSelection);
    });

    combobox.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && !dropdown.hidden) {
            dropdown.hidden = true;
            trigger.setAttribute("aria-expanded", "false");
            combobox.classList.remove("is-open");
            combobox.classList.remove("opens-up");
            trigger.focus();
        }
    });

    document.addEventListener("click", function (event) {
        if (!combobox.contains(event.target) && !dropdown.hidden) {
            dropdown.hidden = true;
            trigger.setAttribute("aria-expanded", "false");
            combobox.classList.remove("is-open");
            combobox.classList.remove("opens-up");
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
        staffSelect.replaceChildren(new Option("-- Chọn nhân sự phụ trách chính --", ""));
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
