(function () {
    "use strict";

    var statusTabs = document.querySelectorAll("[data-feedback-filter]");
    var activityRows = document.querySelectorAll(".feedback-activity-row");
    var statusEmptyStates = document.querySelectorAll("[data-feedback-empty]");
    function updateStudentFeedbackList(filter) {
        var visibleCount = 0;
        activityRows.forEach(function (row) {
            var visible = row.dataset.feedbackState === filter;
            row.hidden = !visible;
            if (visible) visibleCount++;
        });
        statusEmptyStates.forEach(function (emptyState) {
            emptyState.hidden = emptyState.dataset.feedbackEmpty !== filter || visibleCount > 0;
        });
    }
    statusTabs.forEach(function (tab) {
        tab.addEventListener("click", function () {
            statusTabs.forEach(function (item) {
                item.classList.toggle("is-active", item === tab);
                item.setAttribute("aria-selected", item === tab ? "true" : "false");
            });
            updateStudentFeedbackList(tab.dataset.feedbackFilter);
        });
    });
    var activeFeedbackTab = document.querySelector("[data-feedback-filter].is-active");
    if (activeFeedbackTab) updateStudentFeedbackList(activeFeedbackTab.dataset.feedbackFilter);

    var typeFilter = document.getElementById("feedbackTypeFilter");
    var semesterFilter = document.getElementById("feedbackSemesterFilter");
    var departmentFilter = document.getElementById("feedbackDepartmentFilter");
    var statusFilter = document.getElementById("feedbackStatusFilter");
    var applyFiltersButton = document.getElementById("feedbackApplyFilters");
    var clearFiltersButton = document.getElementById("feedbackClearFilters");
    var managementRows = Array.from(document.querySelectorAll(".feedback-management-table tbody tr[data-status]"));
    var feedbackPagination = document.getElementById("feedbackPagination");
    var feedbackPaginationSummary = document.getElementById("feedbackPaginationSummary");
    var feedbackSort = document.getElementById("feedbackSort");
    var feedbackPageSize = 10;
    var feedbackCurrentPage = 1;
    var appliedFilters = {semester: "", type: "", department: "", status: ""};
    
    // Only run management list logic if we're on the management list page
    if (managementRows.length > 0) {
        managementRows.forEach(function (row, index) {
            row.dataset.originalPosition = String(index);
        });
    }

    function renderManagementRows() {
        if (!semesterFilter || !typeFilter || !departmentFilter || !statusFilter || managementRows.length === 0) return;
        var matches = managementRows.filter(function (row) {
            var matchesType = !appliedFilters.type || row.dataset.type === appliedFilters.type;
            var matchesSemester = !appliedFilters.semester || row.dataset.semester === appliedFilters.semester;
            var matchesDepartment = !appliedFilters.department || row.dataset.department === appliedFilters.department;
            var matchesStatus = !appliedFilters.status || row.dataset.status === appliedFilters.status;
            return matchesSemester && matchesType && matchesDepartment && matchesStatus;
        });
        if (feedbackSort && feedbackSort.value !== "default") {
            var sortingByRating = feedbackSort.value.indexOf("rating") === 0;
            var sortKey = sortingByRating ? "average" : "responseRate";
            var descending = feedbackSort.value.endsWith("desc");
            matches.sort(function (a, b) {
                var aValue = Number(a.dataset[sortKey]);
                var bValue = Number(b.dataset[sortKey]);
                var aHasRating = aValue >= 0;
                var bHasRating = bValue >= 0;
                if (sortingByRating && aHasRating !== bHasRating) return aHasRating ? -1 : 1;
                var difference = aValue - bValue;
                if (difference !== 0) return descending ? -difference : difference;
                return Number(a.dataset.originalPosition) - Number(b.dataset.originalPosition);
            });
        } else {
            matches.sort(function (a, b) {
                return Number(a.dataset.originalPosition) - Number(b.dataset.originalPosition);
            });
        }

        var sortedRows = new Set(matches);
        var tableBody = managementRows[0].parentElement;
        matches.forEach(function (row) {
            tableBody.appendChild(row);
        });
        managementRows.forEach(function (row) {
            if (!sortedRows.has(row)) tableBody.appendChild(row);
        });

        var pageCount = Math.max(1, Math.ceil(matches.length / feedbackPageSize));
        feedbackCurrentPage = Math.min(feedbackCurrentPage, pageCount);
        var firstIndex = (feedbackCurrentPage - 1) * feedbackPageSize;
        var visibleRows = new Set(matches.slice(firstIndex, firstIndex + feedbackPageSize));
        managementRows.forEach(function (row) {
            row.hidden = !visibleRows.has(row);
        });

        if (feedbackPaginationSummary) {
            var firstItem = matches.length === 0 ? 0 : firstIndex + 1;
            var lastItem = Math.min(firstIndex + feedbackPageSize, matches.length);
            feedbackPaginationSummary.textContent =
                "Hiển thị " + firstItem + "–" + lastItem + " trong " + matches.length + " hoạt động";
        }
        if (feedbackPagination) {
            feedbackPagination.replaceChildren();
            if (pageCount > 1) {
                function addPageButton(label, page, disabled, current) {
                    var button = document.createElement("button");
                    button.type = "button";
                    button.className = "activity-page-link" + (current ? " is-active" : "");
                    button.textContent = label;
                    button.disabled = disabled;
                    if (current) button.setAttribute("aria-current", "page");
                    button.addEventListener("click", function () {
                        feedbackCurrentPage = page;
                        renderManagementRows();
                    });
                    feedbackPagination.appendChild(button);
                }
                addPageButton("‹", feedbackCurrentPage - 1, feedbackCurrentPage <= 1, false);
                for (var pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
                    addPageButton(String(pageNumber), pageNumber, false, pageNumber === feedbackCurrentPage);
                }
                addPageButton("›", feedbackCurrentPage + 1, feedbackCurrentPage >= pageCount, false);
            }
        }
    }
    if (applyFiltersButton) {
        applyFiltersButton.addEventListener("click", function () {
            appliedFilters = {
                semester: semesterFilter.value,
                type: typeFilter.value,
                department: departmentFilter.value,
                status: statusFilter.value
            };
            feedbackCurrentPage = 1;
            renderManagementRows();
        });
    }
    if (clearFiltersButton) {
        clearFiltersButton.addEventListener("click", function () {
            semesterFilter.value = "";
            typeFilter.value = "";
            departmentFilter.value = "";
            statusFilter.value = "";
            appliedFilters = {semester: "", type: "", department: "", status: ""};
            feedbackCurrentPage = 1;
            renderManagementRows();
        });
    }
    var filterToggle = document.getElementById("feedbackFilterToggle");
    var filtersPanel = document.getElementById("feedbackFilters");
    if (filterToggle && filtersPanel) {
        filterToggle.addEventListener("click", function () {
            var expanded = filterToggle.getAttribute("aria-expanded") === "true";
            filterToggle.setAttribute("aria-expanded", String(!expanded));
            filtersPanel.hidden = expanded;
            filterToggle.querySelector("span").textContent = expanded ? "Mở bộ lọc" : "Thu gọn bộ lọc";
        });
    }
    [semesterFilter, typeFilter, departmentFilter].forEach(function (filter) {
        if (!filter) return;
        var seenValues = new Set();
        Array.from(filter.options).forEach(function (option) {
            if (!option.value) return;
            if (seenValues.has(option.value)) option.remove();
            else seenValues.add(option.value);
        });
    });

    if (feedbackSort) {
        feedbackSort.addEventListener("change", function () {
            feedbackCurrentPage = 1;
            renderManagementRows();
        });
    }
    document.querySelectorAll(".feedback-management-table tbody tr[data-report-href]").forEach(function (row) {
        row.addEventListener("click", function (event) {
            if (event.target.closest("a, button")) return;
            window.location.href = row.dataset.reportHref;
        });
        row.addEventListener("keydown", function (event) {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                window.location.href = row.dataset.reportHref;
            }
        });
    });
    renderManagementRows();

    var answerTabs = document.querySelectorAll("[data-answer-tab]");
    var answers = Array.from(document.querySelectorAll(".feedback-answer"));
    var questionLabel = document.querySelector("[data-question-label]");
    var answerPagination = document.getElementById("feedbackAnswerPagination");
    var answerEmpty = document.querySelector(".feedback-answer-empty");
    var answerPageSize = 5;
    var activeAnswerKey = "bestPart";
    var currentAnswerPage = 1;
    var questionLabels = {
        bestPart: "Điều bạn hài lòng nhất ở hoạt động này là gì?",
        improvement: "Điều gì cần được cải thiện?",
        favorite: "Bạn học hỏi hoặc nhận được gì từ hoạt động?",
        suggestions: "Bạn có đề xuất gì cho các hoạt động sau?",
        note: "Ghi chú thêm"
    };
    function renderAnswerPage(key, page) {
        if (!answerPagination) return;
        var attribute = "data-answer-" + key.toLowerCase();
        var matchingAnswers = answers.filter(function (answer) {
            return (answer.getAttribute(attribute) || "").trim().length > 0;
        });
        var pageCount = Math.ceil(matchingAnswers.length / answerPageSize);
        currentAnswerPage = Math.max(1, Math.min(page, pageCount || 1));
        var start = (currentAnswerPage - 1) * answerPageSize;
        var visibleAnswers = new Set(matchingAnswers.slice(start, start + answerPageSize));
        answers.forEach(function (answer) {
            var content = answer.getAttribute(attribute);
            answer.querySelector(".feedback-answer-body").textContent = content || "Không có nội dung.";
            answer.hidden = !visibleAnswers.has(answer);
        });
        if (answerEmpty) answerEmpty.hidden = matchingAnswers.length > 0 || answers.length === 0;
        answerPagination.replaceChildren();
        if (pageCount <= 1) return;
        for (var pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
            (function (number) {
                var button = document.createElement("button");
                button.type = "button";
                button.className = "activity-page-link" + (number === currentAnswerPage ? " is-active" : "");
                button.textContent = String(number);
                if (number === currentAnswerPage) button.setAttribute("aria-current", "page");
                button.addEventListener("click", function () { renderAnswerPage(activeAnswerKey, number); });
                answerPagination.appendChild(button);
            })(pageNumber);
        }
    }
    answerTabs.forEach(function (tab) {
        tab.addEventListener("click", function () {
            var key = tab.dataset.answerTab;
            activeAnswerKey = key;
            currentAnswerPage = 1;
            answerTabs.forEach(function (item) {
                item.classList.toggle("is-active", item === tab);
            });
            if (questionLabel) questionLabel.textContent = questionLabels[key] || "";
            renderAnswerPage(key, 1);
        });
    });
    if (answerPagination) renderAnswerPage(activeAnswerKey, 1);

    var pendingSearch = document.getElementById("feedbackPendingSearch");
    if (pendingSearch) {
        var pendingStudents = Array.from(document.querySelectorAll("[data-pending-student]"));
        var noSearchResults = document.getElementById("feedbackNoSearchResults");
        pendingSearch.addEventListener("input", function () {
            var query = this.value.trim().toLocaleLowerCase();
            var visibleCount = 0;
            pendingStudents.forEach(function (student) {
                var visible = student.dataset.search.toLocaleLowerCase().includes(query);
                student.hidden = !visible;
                if (visible) visibleCount++;
            });
            if (noSearchResults) noSearchResults.hidden = visibleCount > 0;
        });
    }

    document.querySelectorAll(".feedback-stars").forEach(function (group) {
        var inputs = group.querySelectorAll('input[type="radio"]');
        function paintStars() {
            var selected = group.querySelector("input:checked");
            var selectedValue = selected ? Number(selected.value) : 0;
            inputs.forEach(function (input) {
                input.closest(".feedback-star").classList.toggle("is-selected", Number(input.value) <= selectedValue);
            });
        }
        inputs.forEach(function (input) {
            input.addEventListener("change", paintStars);
        });
        paintStars();
    });

    var feedbackForm = document.querySelector(".feedback-form");
    if (feedbackForm) {
        var submitButton = feedbackForm.querySelector('button[type="submit"]');
        var textFields = feedbackForm.querySelectorAll("textarea");
        var submissionStarted = false;
        textFields.forEach(function (field) {
            var counter = feedbackForm.querySelector('[data-character-count-for="' + field.id + '"]');
            function updateCharacterCount() {
                if (counter) counter.textContent = field.value.length + " / 500";
                if (field.hasAttribute("minlength") && field.value.trim().length > 0) {
                    field.setCustomValidity(field.value.trim().length < 5
                        ? "Câu trả lời cần có ít nhất 5 ký tự sau khi bỏ khoảng trắng đầu cuối."
                        : "");
                } else {
                    field.setCustomValidity("");
                }
            }
            field.addEventListener("input", updateCharacterCount);
            updateCharacterCount();
        });
        var formChanged = false;
        feedbackForm.addEventListener("input", function () { formChanged = true; });
        feedbackForm.addEventListener("change", function () { formChanged = true; });
        feedbackForm.addEventListener("submit", function (event) {
            if (!submitButton) return;
            if (submissionStarted) {
                event.preventDefault();
                return;
            }
            var invalidField = Array.from(feedbackForm.querySelectorAll("textarea[minlength]"))
                .find(function (field) { return field.value.trim().length < 5; });
            if (invalidField) {
                invalidField.setCustomValidity("Câu trả lời cần có ít nhất 5 ký tự sau khi bỏ khoảng trắng đầu cuối.");
                event.preventDefault();
                invalidField.reportValidity();
                return;
            }
            submissionStarted = true;
            submitButton.disabled = true;
            submitButton.textContent = "Đang gửi...";
            formChanged = false;
        });
        window.addEventListener("beforeunload", function (event) {
            if (!submitButton || submitButton.disabled || !formChanged) return;
            event.preventDefault();
            event.returnValue = "";
        });
    }
})();
