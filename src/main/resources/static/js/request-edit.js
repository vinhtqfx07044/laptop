let items = []; // Stores the list of service items.
let serviceSearchTimeout = null; // Timeout for service search debounce.

// HTML escape function to prevent XSS.
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}


// Calculates the line total with the same logic as the backend CurrencyUtils.
function calculateLineTotal(price, discount, quantity, vatRate) {
    if (!price) return 0;

    const safeDiscount = discount || 0;
    const safeVatRate = vatRate || 0;
    const net = (price - safeDiscount) * quantity;
    const withVat = net + (net * safeVatRate);

    // Rounds to match backend (HALF_UP, 0 decimal places).
    return Math.round(withVat);
}

const clearDatalistIfShort = (input) => {
    // If the input value is less than 3 characters, clear the datalist.
    if (input.value.length < 3) updateDatalist([]);
};

document.addEventListener('DOMContentLoaded', function () {
    const serviceInput = document.getElementById('serviceInput');
    const statusSelect = document.getElementById('status');
    const confirmModal = document.getElementById('confirmModal');
    const newImages = document.getElementById('newImages');

    if (serviceInput) {
        // Adds an input event listener to the service input for debounced search.
        serviceInput.addEventListener('input', function () {
            clearTimeout(serviceSearchTimeout);
            serviceSearchTimeout = setTimeout(() => searchAndUpdateDatalist(this.value), 300);
        });
        // Clears the datalist on focus or click if the input value is short.
        serviceInput.addEventListener('focus', () => clearDatalistIfShort(serviceInput));
        serviceInput.addEventListener('click', () => clearDatalistIfShort(serviceInput));
    }

    // Initializes items from server-provided data if available.
    if (window.serverItems?.length) {
        items = window.serverItems;
        renderTable();
    }

    // Stores the original value of the status select element.
    statusSelect?.setAttribute('data-original-value', statusSelect.value);

    // Clears new images and removes the image preview if the 'success' parameter is true in the URL.
    if (new URLSearchParams(location.search).get('success') === 'true') {
        if (newImages) newImages.value = '';
        document.getElementById('imagePreview')?.remove();
    }

    // Clears the confirmation note when the confirm modal is shown.
    confirmModal?.addEventListener('shown.bs.modal', () =>
        document.getElementById('confirmNote').value = '');

    // Adds event listeners for image validation and updating image slots.
    newImages?.addEventListener('change', validateMaxImageSlots);
    document.querySelectorAll('input[name="toDelete"]').forEach(cb =>
        cb.addEventListener('change', updateImageSlots));
});

function searchAndUpdateDatalist(query) {
    // Returns early if the query is too short.
    if (query.length < 2) return updateDatalist([]);

    // Fetches service items from the API based on the query.
    fetch(`/staff/service-items/search?q=${encodeURIComponent(query)}&size=20&page=0`)
        .then(res => res.json())
        .then(data => updateDatalist(data.content || []))
        .catch(() => { });
}

function updateDatalist(services) {
    const datalist = document.getElementById('serviceList');
    if (!datalist) return;

    // Clear existing options
    datalist.innerHTML = '';

    // Create options properly using DOM methods to avoid HTML escaping issues
    services.forEach(service => {
        const option = document.createElement('option');
        const price = formatVietnameseCurrency(service.price);
        const vat = (service.vatRate * 100).toFixed(1) + '%';
        const warranty = service.warrantyDays + ' ngày';

        // Set properties directly to avoid HTML attribute escaping issues
        option.value = service.name;
        option.label = `${service.name} - ${price} - VAT ${vat} - BH ${warranty}`;
        option.setAttribute('data-service', JSON.stringify(service));

        datalist.appendChild(option);
    });
}

function addItem() {
    // Prevents adding items if the request is locked.
    if (window.isRequestLocked) return alert('Không thể thêm hạng mục khi phiếu đã được khóa');

    const serviceInput = document.getElementById('serviceInput');
    const datalist = document.getElementById('serviceList');
    // Finds the selected option from the datalist based on the input value.
    const selectedOption = Array.from(datalist.options).find(opt => opt.value === serviceInput.value);

    // Alerts if no service is selected from the list.
    if (!selectedOption) return alert('Vui lòng chọn dịch vụ từ danh sách');

    const selectedService = JSON.parse(selectedOption.getAttribute('data-service'));
    // Checks if the selected service already exists in the items list.
    const existingItem = items.find(item => item.serviceItemId === selectedService.id);

    // Alerts if the service has already been added.
    if (existingItem) return alert(`Dịch vụ "${selectedService.name}" đã được thêm vào danh sách. Vui lòng chọn dịch vụ khác hoặc xóa dịch vụ cũ trước.`);

    // Adds the new service item to the items array with default quantity and discount.
    items.push({
        serviceItemId: selectedService.id,
        name: selectedService.name,
        price: selectedService.price,
        vatRate: selectedService.vatRate,
        warrantyDays: selectedService.warrantyDays,
        quantity: parseInt(document.getElementById('newQty').value) || 1,
        discount: parseFloat(document.getElementById('newDisc').value) || 0
    });

    renderTable();
    serviceInput.value = '';
    document.getElementById('newQty').value = '1';
    document.getElementById('newDisc').value = '0';
}

function removeItem(i) {
    // Prevents removing items if the request is locked.
    if (window.isRequestLocked) return alert('Không thể xóa hạng mục khi phiếu đã được khóa');
    // Confirms deletion with the user.
    if (!confirm('Bạn có chắc chắn muốn xóa dịch vụ này?')) return;
    items.splice(i, 1);
    renderTable();
}

function renderTable() {
    const tbody = document.querySelector('#itemsTable tbody');
    if (!tbody) return; // Exits if the table body element is not found.

    let total = 0;
    // Generates table rows for each item in the 'items' array.
    tbody.innerHTML = items.map((it, i) => {
        const line = calculateLineTotal(it.price, it.discount, it.quantity, it.vatRate);
        total += line;

        // Determines if the delete button should be shown or a lock icon based on request lock status.
        const deleteBtn = window.isRequestLocked ?
            `<span class="text-muted"><i class="fas fa-lock"></i></span>` :
            `<button type="button" class="btn btn-sm btn-danger" onclick="removeItem(${i})"><i class="fas fa-trash"></i></button>`;

        // Create hidden inputs properly using DOM to avoid HTML attribute escaping issues
        // Include 'id' field to preserve existing items' database IDs
        const hiddenInputsHtml = ['id', 'serviceItemId', 'quantity', 'discount', 'vatRate', 'warrantyDays', 'name', 'price']
            .map(field => {
                let value;
                if (field === 'id') {
                    // For id field: use actual UUID or empty string (which becomes null in backend)
                    value = it[field] || '';
                } else if (field === 'quantity') {
                    value = it[field] || 1;
                } else {
                    value = it[field] || 0;
                }

                const stringValue = String(value);
                // Use proper HTML attribute escaping for double quotes
                const escapedValue = stringValue.replace(/"/g, '&quot;');
                return `<input type="hidden" name="items[${i}].${field}" value="${escapedValue}">`;
            })
            .join('');

        // Returns the HTML for a table row, including hidden inputs for form submission.
        return `<tr>
            <td>${escapeHtml(it.name)}</td>
            <td>${it.quantity}</td>
            <td>${formatVietnameseCurrency(it.price)}</td>
            <td>${formatVietnameseCurrency(it.discount)}</td>
            <td>${(it.vatRate * 100).toFixed(1)}%</td>
            <td class="fw-bold">${formatVietnameseCurrency(line)}</td>
            <td>${deleteBtn}</td>
            ${hiddenInputsHtml}
        </tr>`;
    }).join('');

    // Updates the total amount displayed, applying rounding to match backend behavior.
    const totalAmount = document.getElementById('totalAmount');
    if (totalAmount) totalAmount.textContent = formatVietnameseCurrency(Math.round(total));
}


function removeExistingItem(button) {
    // Confirms deletion with the user.
    if (!confirm('Bạn có chắc chắn muốn xóa dịch vụ này?')) return;

    const row = button.closest('tr');
    row.style.display = 'none';

    // Creates a hidden input field to mark the item for deletion on form submission.
    const hiddenInput = document.createElement('input');
    hiddenInput.type = 'hidden';
    hiddenInput.name = 'deletedItems[]';
    hiddenInput.value = row.getAttribute('data-index');
    document.querySelector('#requestForm').appendChild(hiddenInput);
}

function submitForm() {
    const form = document.getElementById('requestForm');
    const note = document.getElementById('confirmNote').value;

    // Removes any existing hidden note inputs to prevent duplicates.
    form.querySelectorAll('input[name="note"]').forEach(input => input.remove());

    // Creates a new hidden input for the confirmation note and appends it to the form.
    const hiddenNote = document.createElement('input');
    hiddenNote.type = 'hidden';
    hiddenNote.name = 'note';
    hiddenNote.value = note;
    form.appendChild(hiddenNote);


    // Hides the confirmation modal and submits the form.
    bootstrap.Modal.getInstance(document.getElementById('confirmModal'))?.hide();
    form.submit();
}

function validateMaxImageSlots(e) {
    const files = e.target.files;
    const existingImages = document.querySelectorAll('input[name="toDelete"]:not(:checked)').length;

    // If no files are selected or the total number of images (existing + new) is within the limit (5), return.
    if (!files.length || existingImages + files.length <= 5) return;

    // Calculates the number of remaining allowed slots.
    const maxAllowed = 5 - existingImages;
    // Alerts the user if the image limit is exceeded and suggests actions.
    alert(`Chỉ còn ${maxAllowed} slot trống. Hãy bỏ chọn bớt ảnh hoặc đánh dấu xóa thêm ảnh cũ.`);
    e.target.value = ''; // Clears the file input to prevent invalid submission.
}

function updateImageSlots() {
    const fileInput = document.getElementById('newImages');
    // Re-validates image slots if new files are selected (e.g., after a deletion checkbox is toggled).
    if (fileInput?.files.length) validateMaxImageSlots({ target: fileInput });
}