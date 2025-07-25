document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('form.needs-validation').forEach(form => {
        const inputs = form.querySelectorAll('input, textarea, select');
        const interactedInputs = new Set();

        const validateInput = input => {
            input.setCustomValidity('');
            input.classList.remove('is-valid', 'is-invalid');
            input.classList.add(input.checkValidity() ? 'is-valid' : 'is-invalid');
        };

        inputs.forEach(input => {
            input.addEventListener('focus', () => interactedInputs.add(input));
            const handleValidationEvent = () => {
                if (interactedInputs.has(input)) validateInput(input);
            };
            input.addEventListener('blur', handleValidationEvent);
            input.addEventListener('input', handleValidationEvent);
        });

        form.addEventListener('submit', function (event) {
            inputs.forEach(input => {
                interactedInputs.add(input);
                validateInput(input);
            });

            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
                const firstInvalid = form.querySelector(':invalid');
                if (firstInvalid) {
                    firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    firstInvalid.focus();
                }
            }
            form.classList.add('was-validated');
        });
    });
});

function validateAndOpenModal(formId = 'requestForm', modalId = 'confirmModal') {
    const form = document.getElementById(formId);
    if (!form) return;

    form.querySelectorAll('input[required], textarea[required], select[required]')
        .forEach(input => input.classList.add('was-validated'));

    if (form.checkValidity()) {
        const modal = document.getElementById(modalId);
        if (modal) new bootstrap.Modal(modal).show();
    } else {
        const firstInvalid = form.querySelector(':invalid');
        if (firstInvalid) {
            firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
            firstInvalid.focus();
        }
    }
}

window.validateAndOpenModal = validateAndOpenModal;