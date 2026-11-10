(function () {
    var attendanceForm = document.getElementById("attendance-form");
    var removeForm = document.getElementById("remove-participants-form");
    var batchGradeForm = document.getElementById("batch-grade-form");
    var batchGradeTrigger = document.querySelector("[data-batch-grade-trigger]");
    var batchGradeCountSpan = document.querySelector("[data-batch-grade-count]");
    var batchGradeTargetCount = document.querySelector("[data-batch-grade-target-count]");
    var batchGradeClose = document.querySelector("[data-batch-grade-close]");
    var batchGradeInputs = document.querySelector("[data-batch-grade-inputs]");

    var selectAll = document.querySelector("[data-select-all-participants]");
    var checkboxes = Array.from(document.querySelectorAll("[data-participant-select]"));
    var selects = Array.from(document.querySelectorAll("[data-attendance-select]"));
    var bulkButtons = Array.from(document.querySelectorAll("[data-bulk-status]"));
    var markAllButtons = Array.from(document.querySelectorAll("[data-mark-all-status]"));
    var singleGradeButtons = Array.from(document.querySelectorAll("[data-save-single-grade]"));
    var alertBox = document.getElementById("attendance-ajax-alert");
    var successBox = document.getElementById("attendance-ajax-success");

    var removeSelectionLabel = removeForm && removeForm.querySelector("[data-remove-count]");
    var removeSubmit = removeForm && removeForm.querySelector("[data-remove-submit]");
    var removeSubmitCount = removeForm && removeForm.querySelector("[data-remove-submit-count]");
    var removeInputsContainer = removeForm && removeForm.querySelector("[data-remove-inputs]");
    var initialRemoveText = removeSelectionLabel ? removeSelectionLabel.textContent.trim() : "";

    var selectByParticipantId = {};
    selects.forEach(function (select) {
        selectByParticipantId[select.dataset.participantId] = select;
    });

    var checkboxByParticipantId = {};
    checkboxes.forEach(function (checkbox) {
        checkboxByParticipantId[checkbox.dataset.participantId] = checkbox;
    });

    var pendingRequests = 0;
    var STATUS_CLASSES = ["status-green", "status-yellow", "status-red", "status-gray"];
    var PRESENT_STATUSES = ["ATTENDED", "IN_LATE", "OUT_EARLY"];

    function isPresentStatus(status) {
        return PRESENT_STATUSES.indexOf(status) !== -1;
    }

    function getStatusClass(status) {
        switch (status) {
            case "ATTENDED":
                return "status-green";
            case "IN_LATE":
            case "OUT_EARLY":
                return "status-yellow";
            case "ABSENT":
                return "status-red";
            default:
                return "status-gray";
        }
    }

    function applySelectStatusColor(select, status) {
        if (!select) {
            return;
        }
        STATUS_CLASSES.forEach(function (cls) {
            select.classList.remove(cls);
        });
        select.classList.add(getStatusClass(status || select.value));
    }

    function showError(message) {
        if (successBox) {
            successBox.hidden = true;
            successBox.textContent = "";
        }
        if (!alertBox) {
            return;
        }
        alertBox.textContent = message;
        alertBox.hidden = false;
    }

    function showSuccess(message) {
        if (alertBox) {
            alertBox.hidden = true;
            alertBox.textContent = "";
        }
        if (!successBox) {
            return;
        }
        successBox.textContent = message;
        successBox.hidden = false;
    }

    function clearError() {
        if (alertBox) {
            alertBox.hidden = true;
            alertBox.textContent = "";
        }
    }

    function updateStatusCountBadges(counts) {
        if (!counts) {
            return;
        }
        Object.keys(counts).forEach(function (status) {
            var el = document.querySelector('[data-status-count="' + status + '"]');
            if (el) {
                el.textContent = counts[status];
            }
        });
    }

    function applyParticipantScores(participantsMap) {
        if (!participantsMap) {
            return;
        }
        Object.keys(participantsMap).forEach(function (pid) {
            var info = participantsMap[pid];
            if (!info) {
                return;
            }
            var attChip = document.querySelector('[data-attendance-score-for="' + pid + '"]');
            if (attChip && info.attendanceScore !== undefined) {
                attChip.textContent = "(" + info.attendanceScore + "đ)";
            }
            var evalInput = document.querySelector('[data-eval-score-input="' + pid + '"]');
            var fbInput = document.querySelector('[data-eval-feedback-input="' + pid + '"]');
            var saveBtn = document.querySelector('[data-save-single-grade="' + pid + '"]');
            var present = Boolean(info.present);
            if (evalInput) {
                evalInput.disabled = !present;
                if (info.evalScore !== undefined) {
                    evalInput.value = info.evalScore;
                }
            }
            if (fbInput) {
                fbInput.disabled = !present;
                fbInput.placeholder = present ? "Nhận xét / Feedback..." : "Chỉ chấm khi có mặt";
                if (info.feedback !== undefined) {
                    fbInput.value = info.feedback;
                }
            }
            if (saveBtn) {
                saveBtn.disabled = !present;
            }
            var partScoreBadge = document.querySelector('[data-participation-score-for="' + pid + '"]');
            if (partScoreBadge && info.participationScore !== undefined) {
                partScoreBadge.textContent = info.participationScore;
            }
            var finalBadge = document.querySelector('[data-final-point-for="' + pid + '"]');
            if (finalBadge && info.finalPoint !== undefined) {
                finalBadge.textContent = info.finalPoint;
                var numericVal = parseFloat(info.finalPoint) || 0;
                finalBadge.classList.toggle("is-positive", numericVal > 0);
                finalBadge.classList.toggle("is-negative", numericVal < 0);
            }
            if (info.gradedBy) {
                var gradedByEl = document.querySelector('[data-graded-by-for="' + pid + '"]');
                if (!gradedByEl) {
                    var inlineWrap = document.querySelector('[data-grade-inline="' + pid + '"]');
                    if (inlineWrap && inlineWrap.parentNode) {
                        gradedByEl = document.createElement("small");
                        gradedByEl.className = "attendance-graded-by";
                        gradedByEl.setAttribute("data-graded-by-for", pid);
                        inlineWrap.parentNode.appendChild(gradedByEl);
                    }
                }
                if (gradedByEl) {
                    gradedByEl.textContent = "Chấm bởi: " + info.gradedBy;
                }
            }
        });
    }

    function getParticipantCurrentStatus(participantId) {
        var select = selectByParticipantId[participantId];
        if (!select) {
            return "";
        }
        return select.dataset.savedValue || select.value;
    }

    function getSelectedPresentParticipantIds() {
        var ids = [];
        checkboxes.forEach(function (checkbox) {
            if (!checkbox.checked) {
                return;
            }
            var pid = checkbox.dataset.participantId;
            if (isPresentStatus(getParticipantCurrentStatus(pid))) {
                ids.push(pid);
            }
        });
        return ids;
    }

    function getMatchingParticipantsForBulk(button) {
        var toStatus = button.dataset.to;
        var fromAttr = button.dataset.from;
        var fromSet = fromAttr ? fromAttr.split(",") : null;
        var ids = [];
        checkboxes.forEach(function (checkbox) {
            if (!checkbox.checked) {
                return;
            }
            var pid = checkbox.dataset.participantId;
            var current = getParticipantCurrentStatus(pid);
            if (!current || current === toStatus) {
                return;
            }
            if (fromSet && fromSet.indexOf(current) === -1) {
                return;
            }
            ids.push(pid);
        });
        return ids;
    }

    function refreshSelection() {
        var selectedCheckboxes = checkboxes.filter(function (cb) {
            return cb.checked;
        });
        var selectedCount = selectedCheckboxes.length;
        var removableCheckboxes = selectedCheckboxes.filter(function (cb) {
            return cb.dataset.removable === "true";
        });
        var removableCount = removableCheckboxes.length;
        var presentIds = getSelectedPresentParticipantIds();

        if (selectAll) {
            selectAll.disabled = checkboxes.length === 0;
            selectAll.checked = selectedCount > 0 && selectedCount === checkboxes.length;
            selectAll.indeterminate = selectedCount > 0 && selectedCount < checkboxes.length;
        }

        if (batchGradeTrigger) {
            var gradeDisabled = presentIds.length === 0 || pendingRequests > 0;
            batchGradeTrigger.disabled = gradeDisabled;
            batchGradeTrigger.hidden = gradeDisabled;
            batchGradeTrigger.style.display = gradeDisabled ? "none" : "";
            if (batchGradeCountSpan) {
                batchGradeCountSpan.textContent = presentIds.length;
            }
            if (batchGradeTargetCount) {
                batchGradeTargetCount.textContent = presentIds.length;
            }
            if (gradeDisabled && batchGradeForm) {
                batchGradeForm.hidden = true;
            }
        }

        bulkButtons.forEach(function (button) {
            var matchingIds = getMatchingParticipantsForBulk(button);
            var countSpan = button.querySelector("[data-bulk-count]");
            if (countSpan) {
                countSpan.textContent = matchingIds.length;
            }
            var isDisabled = matchingIds.length === 0 || pendingRequests > 0;
            button.disabled = isDisabled;
            button.hidden = isDisabled;
            button.style.display = isDisabled ? "none" : "";
        });

        markAllButtons.forEach(function (button) {
            var toStatus = button.dataset.markAllStatus;
            var hasChangeable = selects.some(function (select) {
                return (select.dataset.savedValue || select.value) !== toStatus;
            });
            button.disabled = !hasChangeable || pendingRequests > 0;
        });

        if (removeSubmit) {
            removeSubmit.disabled = removableCount === 0;
            if (removeSubmitCount) {
                removeSubmitCount.textContent = removableCount;
            }
        }
        if (removeSelectionLabel) {
            if (selectedCount === 0) {
                removeSelectionLabel.textContent = initialRemoveText;
            } else if (removableCount === selectedCount) {
                removeSelectionLabel.textContent = "Đã chọn " + selectedCount + " sinh viên (có thể xóa " + removableCount + " sinh viên).";
            } else {
                removeSelectionLabel.textContent = "Đã chọn " + selectedCount + " sinh viên (có thể xóa " + removableCount + " sinh viên chưa điểm danh; " + (selectedCount - removableCount) + " sinh viên đã điểm danh sẽ được bỏ qua khi xóa).";
            }
        }
    }

    function saveStatuses(statusMap) {
        if (!attendanceForm) {
            return;
        }
        var participantIds = Object.keys(statusMap);
        if (participantIds.length === 0) {
            return;
        }

        clearError();
        var activityId = attendanceForm.dataset.activityId;
        var params = new URLSearchParams();
        params.set("action", "saveAjax");
        params.set("activityId", activityId);

        var previousValues = {};
        participantIds.forEach(function (pid) {
            var select = selectByParticipantId[pid];
            if (select) {
                previousValues[pid] = select.dataset.savedValue || select.value;
                select.value = statusMap[pid];
                applySelectStatusColor(select, statusMap[pid]);
                select.disabled = true;
            }
            params.set("status_" + pid, statusMap[pid]);
        });

        pendingRequests++;
        refreshSelection();

        fetch(attendanceForm.getAttribute("action"), {
            method: "POST",
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
            },
            body: params.toString()
        }).then(function (response) {
            if (response.redirected) {
                var redirectedUrl = response.url || "";
                if (redirectedUrl.indexOf("/login") !== -1) {
                    throw new Error("Phiên đăng nhập đã hết hạn. Vui lòng tải lại trang và đăng nhập lại.");
                }
                throw new Error("Không thể lưu trạng thái điểm danh.");
            }
            return response.text().then(function (rawText) {
                var data = null;
                try {
                    data = JSON.parse(rawText);
                } catch (e) {
                    throw new Error("Không thể đọc phản hồi điểm danh. Vui lòng tải lại trang.");
                }
                if (!response.ok || !data || !data.ok) {
                    throw new Error((data && data.error) || "Không thể lưu trạng thái điểm danh.");
                }
                return data;
            });
        }).then(function (data) {
            participantIds.forEach(function (pid) {
                var select = selectByParticipantId[pid];
                var newStatus = statusMap[pid];
                var oldStatus = previousValues[pid];
                var changed = oldStatus !== newStatus;
                if (select) {
                    select.dataset.savedValue = newStatus;
                    applySelectStatusColor(select, newStatus);
                    select.disabled = false;
                }
                if (changed) {
                    var checkbox = checkboxByParticipantId[pid];
                    if (checkbox) {
                        checkbox.dataset.removable = "false";
                        checkbox.title = "Sinh viên đã được điểm danh (chỉ có thể thao tác điểm danh/chấm điểm, không thể xóa khỏi danh sách)";
                    }
                    if (!data.counts) {
                        var oldBadge = document.querySelector('[data-status-count="' + oldStatus + '"]');
                        var newBadge = document.querySelector('[data-status-count="' + newStatus + '"]');
                        if (oldBadge) {
                            oldBadge.textContent = Math.max(0, (parseInt(oldBadge.textContent, 10) || 0) - 1);
                        }
                        if (newBadge) {
                            newBadge.textContent = (parseInt(newBadge.textContent, 10) || 0) + 1;
                        }
                    }
                }
            });
            updateStatusCountBadges(data.counts);
            applyParticipantScores(data.participants);
        }).catch(function (err) {
            participantIds.forEach(function (pid) {
                var select = selectByParticipantId[pid];
                if (select && previousValues[pid] !== undefined) {
                    select.value = previousValues[pid];
                    applySelectStatusColor(select, previousValues[pid]);
                    select.disabled = false;
                }
            });
            showError(err && err.message ? err.message : "Đã xảy ra lỗi khi lưu điểm danh.");
        }).finally(function () {
            pendingRequests = Math.max(0, pendingRequests - 1);
            refreshSelection();
        });
    }

    function saveEvaluationGrades(participantIds, evalScore, feedback, isBatch) {
        if (!attendanceForm || !participantIds || participantIds.length === 0) {
            return;
        }
        var numericScore = parseFloat(String(evalScore).replace(",", "."));
        if (isNaN(numericScore) || numericScore < 0 || numericScore > 5) {
            showError("Điểm đánh giá tham gia phải nằm trong khoảng từ 0 đến 5 điểm.");
            return;
        }
        clearError();
        var activityId = attendanceForm.dataset.activityId;
        var params = new URLSearchParams();
        params.set("action", isBatch ? "batchGradeAjax" : "saveGradeAjax");
        params.set("activityId", activityId);
        params.set("evalScore", String(numericScore));
        params.set("feedback", feedback || "");
        participantIds.forEach(function (pid) {
            params.append("participantId", pid);
        });

        pendingRequests++;
        refreshSelection();

        fetch(attendanceForm.getAttribute("action"), {
            method: "POST",
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
            },
            body: params.toString()
        }).then(function (response) {
            return response.text().then(function (rawText) {
                var data = null;
                try {
                    data = JSON.parse(rawText);
                } catch (e) {
                    throw new Error("Không thể đọc phản hồi chấm điểm.");
                }
                if (!response.ok || !data || !data.ok) {
                    throw new Error((data && data.error) || "Không thể lưu điểm tham gia.");
                }
                return data;
            });
        }).then(function (data) {
            applyParticipantScores(data.participants);
            if (isBatch) {
                showSuccess("Đã chấm điểm đánh giá tham gia cho " + (data.updated || participantIds.length) + " sinh viên.");
                if (batchGradeForm) {
                    batchGradeForm.hidden = true;
                }
            } else {
                showSuccess("Đã lưu điểm đánh giá tham gia và cập nhật điểm tích lũy.");
            }
        }).catch(function (err) {
            showError(err && err.message ? err.message : "Đã xảy ra lỗi khi lưu điểm tham gia.");
        }).finally(function () {
            pendingRequests = Math.max(0, pendingRequests - 1);
            refreshSelection();
        });
    }

    selects.forEach(function (select) {
        select.addEventListener("change", function () {
            var pid = select.dataset.participantId;
            var newValue = select.value;
            if (newValue === select.dataset.savedValue) {
                return;
            }
            var payload = {};
            payload[pid] = newValue;
            saveStatuses(payload);
        });
    });

    singleGradeButtons.forEach(function (btn) {
        btn.addEventListener("click", function () {
            var pid = btn.getAttribute("data-save-single-grade");
            var scoreInput = document.querySelector('[data-eval-score-input="' + pid + '"]');
            var fbInput = document.querySelector('[data-eval-feedback-input="' + pid + '"]');
            if (!scoreInput) {
                return;
            }
            saveEvaluationGrades([pid], scoreInput.value, fbInput ? fbInput.value : "", false);
        });
    });

    if (batchGradeTrigger && batchGradeForm) {
        batchGradeTrigger.addEventListener("click", function () {
            batchGradeForm.hidden = !batchGradeForm.hidden;
            if (!batchGradeForm.hidden) {
                batchGradeForm.scrollIntoView({behavior: "smooth", block: "nearest"});
            }
        });
        if (batchGradeClose) {
            batchGradeClose.addEventListener("click", function () {
                batchGradeForm.hidden = true;
            });
        }
        batchGradeForm.addEventListener("submit", function (event) {
            event.preventDefault();
            var presentIds = getSelectedPresentParticipantIds();
            if (presentIds.length === 0) {
                showError("Vui lòng chọn ít nhất một sinh viên có mặt để chấm điểm hàng loạt.");
                return;
            }
            var scoreInput = document.getElementById("batch-eval-score");
            var fbInput = document.getElementById("batch-eval-feedback");
            saveEvaluationGrades(
                presentIds,
                scoreInput ? scoreInput.value : "5",
                fbInput ? fbInput.value : "",
                true
            );
        });
    }

    bulkButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            var matchingIds = getMatchingParticipantsForBulk(button);
            if (matchingIds.length === 0) {
                return;
            }
            var label = button.dataset.label || "Cập nhật";
            if (!window.confirm(label + " cho " + matchingIds.length + " sinh viên đã chọn?")) {
                return;
            }
            var toStatus = button.dataset.to;
            var payload = {};
            matchingIds.forEach(function (pid) {
                payload[pid] = toStatus;
            });
            saveStatuses(payload);
        });
    });

    markAllButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            var toStatus = button.dataset.markAllStatus;
            var matchingIds = [];
            selects.forEach(function (select) {
                var current = select.dataset.savedValue || select.value;
                if (current !== toStatus) {
                    matchingIds.push(select.dataset.participantId);
                }
            });
            if (matchingIds.length === 0) {
                return;
            }
            var openTrigger = document.querySelector('.action-menu__trigger[aria-expanded="true"]');
            if (openTrigger) {
                openTrigger.click();
            }
            var label = button.dataset.label || "Cập nhật tất cả";
            if (!window.confirm(label + " (" + matchingIds.length + " sinh viên trên trang này)?")) {
                return;
            }
            var payload = {};
            matchingIds.forEach(function (pid) {
                payload[pid] = toStatus;
            });
            saveStatuses(payload);
        });
    });

    checkboxes.forEach(function (checkbox) {
        checkbox.addEventListener("change", refreshSelection);
    });

    if (selectAll) {
        selectAll.addEventListener("change", function () {
            checkboxes.forEach(function (checkbox) {
                checkbox.checked = selectAll.checked;
            });
            refreshSelection();
        });
    }

    if (removeForm) {
        removeForm.addEventListener("submit", function (event) {
            if (!removeInputsContainer) {
                return;
            }
            removeInputsContainer.innerHTML = "";
            checkboxes.forEach(function (checkbox) {
                if (checkbox.checked && checkbox.dataset.removable === "true") {
                    var input = document.createElement("input");
                    input.type = "hidden";
                    input.name = "participantId";
                    input.value = checkbox.value;
                    removeInputsContainer.appendChild(input);
                }
            });
            if (removeInputsContainer.children.length === 0) {
                event.preventDefault();
            }
        });
    }

    window.addEventListener("beforeunload", function (event) {
        if (pendingRequests > 0) {
            event.preventDefault();
            event.returnValue = "";
        }
    });

    refreshSelection();
})();

(function () {
    var form = document.querySelector("[data-group-form]");
    if (!form) {
        return;
    }
    var levels = ["department", "major", "program", "class"].map(function (name) {
        return form.querySelector('[data-group-level="' + name + '"]');
    });
    var countLabel = form.querySelector("[data-group-count]");
    var submit = form.querySelector("[data-group-submit]");

    function syncOptions() {
        var selected = {};
        levels.forEach(function (select) {
            var level = select.dataset.groupLevel;
            Array.from(select.options).forEach(function (option) {
                if (!option.value) {
                    return;
                }
                var matches = Object.keys(selected).every(function (parent) {
                    return option.dataset[parent] === selected[parent];
                });
                option.hidden = !matches;
                option.disabled = !matches;
            });
            if (select.selectedOptions[0] && select.selectedOptions[0].disabled) {
                select.value = "";
            }
            if (select.value) {
                selected[level] = select.value;
            }
        });
    }

    function updateCount() {
        var mostSpecific = null;
        levels.forEach(function (select) {
            if (select.value) {
                mostSpecific = select.selectedOptions[0];
            }
        });
        var count = mostSpecific ? Number(mostSpecific.dataset.count) : 0;
        submit.disabled = count === 0;
        countLabel.textContent = mostSpecific
            ? "Sẽ thêm " + count + " sinh viên"
            : "Chọn khoa, ngành, chương trình đào tạo hoặc lớp";
    }

    levels.forEach(function (select) {
        select.addEventListener("change", function () {
            syncOptions();
            updateCount();
        });
    });
    syncOptions();
    updateCount();
})();

(function () {
    var toggle = document.querySelector("[data-add-toggle]");
    var panel = document.getElementById("attendance-add-panel");
    if (!toggle || !panel) {
        return;
    }
    function setOpen(open) {
        panel.hidden = !open;
        toggle.setAttribute("aria-expanded", open ? "true" : "false");
        if (open) {
            panel.scrollIntoView({behavior: "smooth", block: "nearest"});
        }
    }
    toggle.addEventListener("click", function (event) {
        event.preventDefault();
        setOpen(panel.hidden);
    });
    var close = panel.querySelector("[data-add-close]");
    if (close) {
        close.addEventListener("click", function () {
            setOpen(false);
            toggle.focus();
        });
    }
    if (!panel.hidden) {
        panel.scrollIntoView({block: "nearest"});
    }
})();
