document.addEventListener("DOMContentLoaded", function () {

    // ==================================================
    // 1. SEARCH + FILTER ACTIVITY
    // ==================================================

    const searchInput =
        document.getElementById("activitySearch");

    const activityStatusFilter =
        document.getElementById("activityStatusFilter");

    const approvalStatusFilter =
        document.getElementById("approvalStatusFilter");

    const rows =
        document.querySelectorAll(".activity-row");

    const emptyMessage =
        document.getElementById("activityEmptyState");


    function filterActivities() {

        const keyword =
            searchInput
                ? searchInput.value.trim().toLowerCase()
                : "";

        const activityStatus =
            activityStatusFilter
                ? activityStatusFilter.value.toLowerCase()
                : "";

        const approvalStatus =
            approvalStatusFilter
                ? approvalStatusFilter.value.toLowerCase()
                : "";


        let visibleCount = 0;


        rows.forEach(function (row) {

            const rowText =
                row.textContent.toLowerCase();

            const rowActivityStatus =
                (row.dataset.activityStatus || "")
                    .toLowerCase();

            const rowApprovalStatus =
                (row.dataset.approvalStatus || "")
                    .toLowerCase();


            const matchKeyword =
                keyword === "" ||
                rowText.includes(keyword);


            const matchActivityStatus =
                activityStatus === "" ||
                rowActivityStatus === activityStatus;


            const matchApprovalStatus =
                approvalStatus === "" ||
                rowApprovalStatus === approvalStatus;


            const visible =
                matchKeyword &&
                matchActivityStatus &&
                matchApprovalStatus;


            row.style.display =
                visible ? "" : "none";


            if (visible) {
                visibleCount++;
            }
        });


        if (emptyMessage) {

            emptyMessage.style.display =
                visibleCount === 0
                    ? ""
                    : "none";
        }
    }


    if (searchInput) {

        searchInput.addEventListener(
            "input",
            filterActivities
        );
    }


    if (activityStatusFilter) {

        activityStatusFilter.addEventListener(
            "change",
            filterActivities
        );
    }


    if (approvalStatusFilter) {

        approvalStatusFilter.addEventListener(
            "change",
            filterActivities
        );
    }


    // ==================================================
    // 2. DELETE
    // ==================================================

    document
        .querySelectorAll(".delete-form")
        .forEach(function (form) {

            form.addEventListener(
                "submit",
                function (event) {

                    const confirmed =
                        confirm(
                            "Bạn có chắc chắn muốn xóa hoạt động này không?"
                        );


                    if (!confirmed) {

                        event.preventDefault();
                    }
                }
            );
        });


    // ==================================================
    // 3. APPROVE
    // ==================================================

    document
        .querySelectorAll(".approve-form")
        .forEach(function (form) {

            form.addEventListener(
                "submit",
                function (event) {

                    const confirmed =
                        confirm(
                            "Bạn có chắc chắn muốn phê duyệt hoạt động này không?"
                        );


                    if (!confirmed) {

                        event.preventDefault();
                    }
                }
            );
        });


    // ==================================================
    // 4. REJECT
    // ==================================================

    document
        .querySelectorAll(".reject-form")
        .forEach(function (form) {

            form.addEventListener(
                "submit",
                function (event) {

                    const noteInput =
                        form.querySelector(".reject-note");


                    let note =
                        noteInput
                            ? noteInput.value.trim()
                            : "";


                    // Nếu chưa nhập lý do
                    if (note === "") {

                        note =
                            prompt(
                                "Nhập lý do từ chối:"
                            );


                        // Cancel prompt
                        if (note === null) {

                            event.preventDefault();

                            return;
                        }


                        note = note.trim();


                        if (note === "") {

                            alert(
                                "Vui lòng nhập lý do từ chối."
                            );

                            event.preventDefault();

                            return;
                        }


                        if (noteInput) {

                            noteInput.value = note;
                        }
                    }


                    const confirmed =
                        confirm(
                            "Bạn có chắc chắn muốn từ chối hoạt động này không?"
                        );


                    if (!confirmed) {

                        event.preventDefault();
                    }
                }
            );
        });


    // ==================================================
    // 5. PARTNER -> PARTNER STAFF
    // ==================================================

    const partnerSelect =
        document.getElementById("partnerId");

    const partnerStaffSelect =
        document.getElementById(
            "partnerStaffId"
        );


    if (
        partnerSelect &&
        partnerStaffSelect
    ) {

        const contextPath =
            document.body.dataset.contextPath || "";


        partnerSelect.addEventListener(
            "change",
            async function () {

                const partnerId =
                    this.value;


                partnerStaffSelect.innerHTML =
                    '<option value="">-- Chọn partner staff --</option>';


                if (!partnerId) {

                    return;
                }


                try {

                    const response =
                        await fetch(
                            contextPath +
                            "/partner-staff?action=options&partnerId=" +
                            encodeURIComponent(partnerId)
                        );


                    if (!response.ok) {

                        throw new Error(
                            "Cannot load partner staff"
                        );
                    }


                    const staffList =
                        await response.json();


                    staffList.forEach(
                        function (staff) {

                            const option =
                                document.createElement(
                                    "option"
                                );


                            option.value =
                                staff.id;


                            option.textContent =
                                staff.fullName +
                                (
                                    staff.position
                                        ? " - " +
                                        staff.position
                                        : ""
                                );


                            partnerStaffSelect
                                .appendChild(
                                    option
                                );
                        }
                    );

                } catch (error) {

                    console.error(error);


                    partnerStaffSelect.innerHTML =
                        '<option value="">Không tải được partner staff</option>';
                }
            }
        );
    }
});