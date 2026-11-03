(() => {
    const builder = document.querySelector("#question-builder");
    if (!builder) return;

    const list = builder.querySelector("[data-question-list]");
    const total = builder.querySelector("[data-question-total]");
    const countLabel = builder.querySelector("[data-question-count]");
    const maxQuestions = 10;
    const maxChoices = 10;

    function updateCard(card, index) {
        card.querySelector("[data-question-number]").textContent = `Câu ${index + 1}`;
        card.querySelector("[data-required-value]").name = `questionRequired_${index}`;
        card.querySelector("[data-required-toggle]").onchange = event => {
            card.querySelector("[data-required-value]").value = event.target.checked ? "true" : "false";
        };
        card.querySelector("[data-question-type]").name = `questionType_${index}`;
        card.querySelector(".registration-question-label input").name = `questionLabel_${index}`;

        const type = card.querySelector("[data-question-type]").value;
        const choiceEditor = card.querySelector("[data-choice-editor]");
        choiceEditor.hidden = type !== "SELECT" && type !== "MULTISELECT";
        choiceEditor.querySelectorAll("input").forEach(input => {
            input.disabled = choiceEditor.hidden;
        });
        const choiceRows = [...card.querySelectorAll(".registration-choice-row")];
        choiceRows.forEach((row, choiceIndex) => {
            row.querySelector("input").name = `choice_${index}_${choiceIndex}`;
            row.querySelector("[data-choice-marker]").textContent = type === "MULTISELECT" ? "" : `${choiceIndex + 1}.`;
            row.querySelector("[data-choice-marker]").classList.toggle("registration-choice-checkbox-marker",
                type === "MULTISELECT");
        });
        card.querySelector("[data-choice-count]").name = `choiceCount_${index}`;
        card.querySelector("[data-choice-count]").value = choiceRows.length;
        card.querySelector("[data-choice-limit]").textContent = `${choiceRows.length} / ${maxChoices} lựa chọn`;
        card.querySelector("[data-add-choice]").disabled = choiceRows.length >= maxChoices;
    }

    function update() {
        const cards = [...list.querySelectorAll("[data-question-card]")];
        cards.forEach(updateCard);
        total.value = cards.length;
        countLabel.textContent = `${cards.length} / ${maxQuestions} câu hỏi`;
        builder.querySelectorAll("[data-add-question]").forEach(button => {
            button.disabled = cards.length >= maxQuestions;
        });
    }

    function addChoice(card) {
        const rows = card.querySelector("[data-choice-list]");
        if (rows.children.length >= maxChoices) return;
        const row = document.createElement("label");
        row.className = "registration-choice-row";
        row.innerHTML = '<span class="registration-choice-marker" data-choice-marker></span>' +
            '<input type="text" maxlength="200" required>' +
            '<button type="button" data-remove-choice aria-label="Xóa lựa chọn">×</button>';
        rows.append(row);
        update();
        row.querySelector("input").focus();
    }

    function addQuestion(type) {
        if (list.children.length >= maxQuestions) return;
        const card = document.createElement("article");
        card.className = "registration-question-card";
        card.dataset.questionCard = "";
        card.innerHTML = `
            <input type="hidden" data-required-value value="false">
            <input type="hidden" data-choice-count value="0">
            <div class="registration-question-card-heading">
                <span class="registration-question-number" data-question-number></span>
                <select aria-label="Loại câu hỏi" data-question-type>
                    <option value="TEXTAREA">Tự luận</option>
                    <option value="SELECT">Dropdown (chọn 1)</option>
                    <option value="MULTISELECT">Nhiều lựa chọn</option>
                </select>
                <label class="registration-question-required">
                    <input type="checkbox" data-required-toggle>Bắt buộc
                </label>
                <button class="registration-remove-question" type="button" data-remove-question
                        aria-label="Xóa câu hỏi">×</button>
            </div>
            <label class="registration-question-label">Nội dung câu hỏi
                <input type="text" maxlength="200" required>
            </label>
            <div class="registration-choice-editor" data-choice-editor>
                <div class="registration-choice-list" data-choice-list></div>
                <button class="registration-add-choice" type="button" data-add-choice>+ Thêm lựa chọn</button>
                <small data-choice-limit></small>
            </div>`;
        list.append(card);
        card.querySelector("[data-question-type]").value = type;
        if (type !== "TEXTAREA") {
            addChoice(card);
            addChoice(card);
        }
        update();
        card.querySelector(".registration-question-label input").focus();
    }

    builder.addEventListener("click", event => {
        const addQuestionButton = event.target.closest("[data-add-question]");
        if (addQuestionButton) {
            addQuestion(addQuestionButton.dataset.addQuestion);
            return;
        }

        const removeQuestionButton = event.target.closest("[data-remove-question]");
        if (removeQuestionButton && list.children.length > 0) {
            removeQuestionButton.closest("[data-question-card]").remove();
            update();
            return;
        }

        const addChoiceButton = event.target.closest("[data-add-choice]");
        if (addChoiceButton) {
            addChoice(addChoiceButton.closest("[data-question-card]"));
            return;
        }

        const removeChoiceButton = event.target.closest("[data-remove-choice]");
        if (removeChoiceButton) {
            removeChoiceButton.closest(".registration-choice-row").remove();
            update();
        }
    });

    builder.addEventListener("change", event => {
        if (event.target.matches("[data-question-type]")) update();
    });

    update();
})();
