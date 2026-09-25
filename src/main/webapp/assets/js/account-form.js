/**
 * account-form.js
 *
 * Filters the Role <select> options on the account create/edit forms based on
 * the selected Type. Each role option carries data-type="<account type>".
 * This is a UX helper only; the server always re-validates the submitted
 * (type, role) pair against account_type_role_domain.
 */
(function () {
    'use strict';

    function init() {
        var typeSelect = document.getElementById('type');
        var roleSelect = document.getElementById('role');
        if (!typeSelect || !roleSelect) {
            return;
        }

        function updateRoleOptions() {
            var selectedType = typeSelect.value;
            var visibleOptions = 0;
            var options = roleSelect.options;

            for (var i = 0; i < options.length; i++) {
                var option = options[i];
                var visible = !option.value || option.getAttribute('data-type') === selectedType;
                option.style.display = visible ? '' : 'none';
                if (option.selected && !visible) {
                    option.selected = false;
                }
                if (visible && option.value) {
                    visibleOptions++;
                }
            }
            roleSelect.disabled = visibleOptions === 0;
        }

        typeSelect.addEventListener('change', updateRoleOptions);
        updateRoleOptions();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();