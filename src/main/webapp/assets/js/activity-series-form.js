(function () {
    var form = document.getElementById("seriesForm");
    if (!form) { return; }
    var byId = function (id) { return document.getElementById(id); };
    var deptBox = byId("departmentCombobox");
    var staffBox = byId("staffCombobox");
    var hint = byId("responsibleStaffMessage");
    var version = 0;
    async function loadStaff() {
        var checked = deptBox.querySelector("input:checked");
        var api = staffBox.comboboxApi;
        var current = ++version;
        hint.textContent = "";
        api.setOptions([]);
        if (!checked) {
            api.setPlaceholder("Chọn đơn vị phụ trách trước");
            api.setDisabled(true);
            return;
        }
        api.setPlaceholder("Đang tải nhân viên...");
        api.setDisabled(true);
        var url = new URL(deptBox.dataset.staffEndpoint, document.baseURI);
        url.searchParams.set("action", "getStaffsByDepartment");
        url.searchParams.set("departmentId", checked.value);
        try {
            var res = await fetch(url.toString());
            if (!res.ok || !(res.headers.get("content-type") || "").includes("application/json")) {
                throw new Error("Không thể tải danh sách nhân sự của đơn vị.");
            }
            var staffs = await res.json();
            if (current !== version) { return; }
            api.setOptions(staffs.map(function (s) {
                return { id: s.id, name: [s.departmentCode, s.departmentName, s.code, s.name].filter(Boolean).join(" - ") };
            }));
            api.setPlaceholder("Chọn nhân viên");
            api.setDisabled(false);
        } catch (e) {
            if (current === version) {
                hint.textContent = e.message;
                api.setPlaceholder("Không có dữ liệu nhân viên");
            }
        }
    }
    deptBox.addEventListener("change", loadStaff);
    var initialChecked = deptBox.querySelector("input:checked");
    if (!initialChecked) {
        var staffApi = staffBox.comboboxApi;
        if (staffApi) {
            staffApi.setPlaceholder("Chọn đơn vị phụ trách trước");
            staffApi.setDisabled(true);
        }
    }
})();