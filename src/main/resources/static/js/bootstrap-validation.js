document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('form.needs-validation').forEach(form => {
        const inputs = form.querySelectorAll('input, textarea, select');
        // Tracks inputs that have been interacted with by the user.
        const interactedInputs = new Set();

        /**
         * Validates a single input element and applies Bootstrap validation classes.
         * @param {HTMLElement} input - The input element to validate.
         */
        const validateInput = input => {
            input.setCustomValidity(''); // Clears any custom validation messages.
            // Removes previous validation states.
            input.classList.remove('is-valid', 'is-invalid');
            // Adds 'is-valid' or 'is-invalid' based on the input's validity.
            input.classList.add(input.checkValidity() ? 'is-valid' : 'is-invalid');
        };

        // Attaches event listeners to each input for real-time validation.
        inputs.forEach(input => {
            // Marks input as interacted upon focus.
            input.addEventListener('focus', () => interactedInputs.add(input));
            // Handles validation on blur and input events if the input has been interacted with.
            const handleValidationEvent = () => {
                if (interactedInputs.has(input)) validateInput(input);
            };
            input.addEventListener('blur', handleValidationEvent);
            input.addEventListener('input', handleValidationEvent);
        });

        // Handles form submission.
        form.addEventListener('submit', function (event) {
            // Validates all inputs on submit, marking them as interacted.
            inputs.forEach(input => {
                interactedInputs.add(input);
                validateInput(input);
            });

            // Prevents form submission if validation fails.
            if (!form.checkValidity()) {
                event.preventDefault(); // Stops the default form submission.
                event.stopPropagation(); // Stops the event from bubbling up.
                // Scrolls to and focuses on the first invalid input.
                const firstInvalid = form.querySelector(':invalid');
                if (firstInvalid) {
                    firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    firstInvalid.focus();
                }
            }
            // Adds 'was-validated' class to show validation feedback.
            form.classList.add('was-validated');
        });
    });
});

/**
 * Validates a form and, if valid, opens a Bootstrap modal.
 * @param {string} formId - The ID of the form to validate (defaults to 'requestForm').
 * @param {string} modalId - The ID of the modal to open (defaults to 'confirmModal').
 */
function validateAndOpenModal(formId = 'requestForm', modalId = 'confirmModal') {
    const form = document.getElementById(formId);
    if (!form) return; // Exits if the form is not found.

    // Adds 'was-validated' class to all required inputs to show immediate feedback.
    form.querySelectorAll('input[required], textarea[required], select[required]')
        .forEach(input => input.classList.add('was-validated'));

    // Checks form validity.
    if (form.checkValidity()) {
        // If valid, opens the specified Bootstrap modal.
        const modal = document.getElementById(modalId);
        if (modal) new bootstrap.Modal(modal).show();
    } else {
        // If invalid, scrolls to and focuses on the first invalid input.
        const firstInvalid = form.querySelector(':invalid');
        if (firstInvalid) {
            firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
            firstInvalid.focus();
        }
    }
}

// Makes the function globally accessible.
window.validateAndOpenModal = validateAndOpenModal;