(function () {
    // Shared date-time picker: same widget as the activity form.
    // Shown as dd/mm/yyyy HH:mm (24h), submitted as yyyy-MM-ddTHH:mm.
    if (typeof flatpickr === "undefined") {
        return;
    }

    function enhance(input) {
        if (input._flatpickr) {
            return;
        }
        var required = input.required;
        var disabled = input.disabled;
        var options = {
            locale: "vn",
            enableTime: true,
            time_24hr: true,
            minuteIncrement: 1,
            dateFormat: "Y-m-d\\TH:i",
            altInput: true,
            altFormat: "d/m/Y H:i",
            monthSelectorType: "dropdown",
            allowInput: true,
            disableMobile: true
        };
        if (input.min) { options.minDate = input.min.replace("T", " "); }
        if (input.max) { options.maxDate = input.max.replace("T", " "); }

        var picker = flatpickr(input, options);
        var alt = picker.altInput;
        if (input.id) {
            alt.id = input.id + "Display";
            var label = document.querySelector("label[for='" + input.id + "']");
            if (label) { label.setAttribute("for", alt.id); }
        }
        alt.placeholder = "dd/mm/yyyy HH:mm";
        alt.maxLength = 16;
        alt.inputMode = "numeric";
        alt.autocomplete = "off";
        // The original input becomes hidden, so validation must live on the visible one.
        input.required = false;
        alt.required = required;
        alt.disabled = disabled;

        alt.addEventListener("input", function (e) {
            if (e.inputType && e.inputType.indexOf("delete") === 0) { return; }
            var d = alt.value.replace(/\D/g, "").slice(0, 12);
            var out = d.slice(0, 2);
            if (d.length > 2) { out += "/" + d.slice(2, 4); }
            if (d.length > 4) { out += "/" + d.slice(4, 8); }
            if (d.length > 8) { out += " " + d.slice(8, 10); }
            if (d.length > 10) { out += ":" + d.slice(10, 12); }
            alt.value = out;
        });
    }

    function init() {
        document.querySelectorAll("input[type='datetime-local']").forEach(enhance);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
}());
