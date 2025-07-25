document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('serviceItemForm');
    
    if (form) {
        form.addEventListener('submit', function() {
            const activeCheckbox = document.getElementById('active');
            const existingHidden = form.querySelector('input[name="active"][type="hidden"]');
            
            if (!activeCheckbox.checked && !existingHidden) {
                const hiddenInput = document.createElement('input');
                Object.assign(hiddenInput, {type: 'hidden', name: 'active', value: 'false'});
                form.appendChild(hiddenInput);
            } else if (activeCheckbox.checked && existingHidden) {
                existingHidden.remove();
            }
        });
    }
    
    document.querySelectorAll('.edit-btn').forEach(button => {
        button.addEventListener('click', function() {
            const d = this.dataset;
            editServiceItem(d.id, d.name, parseFloat(d.price), parseFloat(d.vatRate), parseInt(d.warrantyDays), d.active === 'true');
        });
    });
});

function editServiceItem(id, name, price, vatRate, warrantyDays, active) {
    const fields = {serviceItemId: id, name, price, vatRate, warrantyDays};
    Object.entries(fields).forEach(([key, value]) => document.getElementById(key).value = value);
    
    document.getElementById('active').checked = active === true || active === 'true';
    document.getElementById('serviceItemModalTitle').textContent = 'Chỉnh sửa dịch vụ';
    document.getElementById('serviceItemForm').action = '/staff/service-items/update/' + id;
    
    new bootstrap.Modal(document.getElementById('serviceItemModal')).show();
}

function addServiceItem() {
    document.getElementById('serviceItemForm').reset();
    document.getElementById('serviceItemId').value = '';
    document.getElementById('active').checked = true;
    document.getElementById('serviceItemModalTitle').textContent = 'Thêm dịch vụ mới';
    document.getElementById('serviceItemForm').action = '/staff/service-items/create';
    
    new bootstrap.Modal(document.getElementById('serviceItemModal')).show();
}

document.getElementById('serviceItemModal').addEventListener('hidden.bs.modal', function () {
    document.getElementById('serviceItemForm').reset();
    document.getElementById('serviceItemId').value = '';
    document.getElementById('serviceItemModalTitle').textContent = 'Thêm dịch vụ mới';
    document.getElementById('serviceItemForm').action = '/staff/service-items/create';
    
    document.querySelectorAll('#serviceItemForm .form-control').forEach(input => {
        input.classList.remove('is-valid', 'is-invalid');
    });
});