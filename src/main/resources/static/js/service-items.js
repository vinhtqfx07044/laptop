// Executes when the DOM is fully loaded.
document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('serviceItemForm');

    if (form) {
        // Handles form submission to ensure the 'active' checkbox value is correctly sent.
        form.addEventListener('submit', function () {
            const activeCheckbox = document.getElementById('active');
            const existingHidden = form.querySelector('input[name="active"][type="hidden"]');

            // If the checkbox is unchecked and no hidden input exists, create one.
            if (!activeCheckbox.checked && !existingHidden) {
                const hiddenInput = document.createElement('input');
                Object.assign(hiddenInput, { type: 'hidden', name: 'active', value: 'false' });
                form.appendChild(hiddenInput);
            } else if (activeCheckbox.checked && existingHidden) {
                // If the checkbox is checked and a hidden input exists, remove it.
                existingHidden.remove();
            }
        });
    }

    // Attaches click listeners to all edit buttons.
    document.querySelectorAll('.edit-btn').forEach(button => {
        button.addEventListener('click', function () {
            const d = this.dataset; // Gets data attributes from the button.
            // Calls editServiceItem with data from the button.
            editServiceItem(d.id, d.name, parseFloat(d.price), parseFloat(d.vatRate), parseInt(d.warrantyDays), d.active === 'true');
        });
    });
});

/**
 * Populates the modal form with existing service item data for editing.
 */
function editServiceItem(id, name, price, vatRate, warrantyDays, active) {
    // Maps data to form fields.
    const fields = { serviceItemId: id, name, price, vatRate, warrantyDays };
    Object.entries(fields).forEach(([key, value]) => document.getElementById(key).value = value);

    // Sets the checked state of the 'active' checkbox.
    document.getElementById('active').checked = active === true || active === 'true';
    document.getElementById('serviceItemModalTitle').textContent = 'Chỉnh sửa dịch vụ';
    document.getElementById('serviceItemForm').action = `/staff/service-items/update/${id}`;

    // Shows the service item modal.
    new bootstrap.Modal(document.getElementById('serviceItemModal')).show();
}

/**
 * Resets the modal form for adding a new service item.
 */
function addServiceItem() {
    document.getElementById('serviceItemForm').reset();
    document.getElementById('serviceItemId').value = '';
    document.getElementById('active').checked = true;
    document.getElementById('serviceItemModalTitle').textContent = 'Thêm dịch vụ mới';
    document.getElementById('serviceItemForm').action = '/staff/service-items/create';

    // Shows the service item modal.
    new bootstrap.Modal(document.getElementById('serviceItemModal')).show();
}

// Event listener for when the service item modal is hidden.
document.getElementById('serviceItemModal').addEventListener('hidden.bs.modal', function () {
    document.getElementById('serviceItemForm').reset();
    document.getElementById('serviceItemId').value = '';
    document.getElementById('serviceItemModalTitle').textContent = 'Thêm dịch vụ mới';
    document.getElementById('serviceItemForm').action = '/staff/service-items/create';

    // Removes validation classes from form controls.
    document.querySelectorAll('#serviceItemForm .form-control').forEach(input => {
        input.classList.remove('is-valid', 'is-invalid');
    });
});