let items = [];
let serviceSearchTimeout = null;

// HTML escape function to prevent XSS
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Vietnamese currency formatter
function formatVietnameseCurrency(amount) {
    if (!amount) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
    }).format(amount) + ' ₫';
}

const clearDatalistIfShort = (input) => {
    if (input.value.length < 3) updateDatalist([]);
};

document.addEventListener('DOMContentLoaded', function () {
    const serviceInput = document.getElementById('serviceInput');
    const statusSelect = document.getElementById('status');
    const confirmModal = document.getElementById('confirmModal');
    const newImages = document.getElementById('newImages');

    if (serviceInput) {
        serviceInput.addEventListener('input', function () {
            clearTimeout(serviceSearchTimeout);
            serviceSearchTimeout = setTimeout(() => searchAndUpdateDatalist(this.value), 300);
        });
        serviceInput.addEventListener('focus', () => clearDatalistIfShort(serviceInput));
        serviceInput.addEventListener('click', () => clearDatalistIfShort(serviceInput));
    }

    if (window.serverItems?.length) {
        items = window.serverItems;
        renderTable();
    }

    statusSelect?.setAttribute('data-original-value', statusSelect.value);

    if (new URLSearchParams(location.search).get('success') === 'true') {
        if (newImages) newImages.value = '';
        document.getElementById('imagePreview')?.remove();
    }

    confirmModal?.addEventListener('shown.bs.modal', () =>
        document.getElementById('confirmNote').value = '');

    newImages?.addEventListener('change', validateMaxImageSlots);
    document.querySelectorAll('input[name="toDelete"]').forEach(cb =>
        cb.addEventListener('change', updateImageSlots));
});

function searchAndUpdateDatalist(query) {
    if (query.length < 2) return updateDatalist([]);

    fetch(`/staff/service-items/search?q=${encodeURIComponent(query)}&size=20&page=0`)
        .then(res => res.json())
        .then(data => updateDatalist(data.content || []))
        .catch(() => { });
}

function updateDatalist(services) {
    const datalist = document.getElementById('serviceList');
    if (!datalist) return;

    datalist.innerHTML = services.map(service => {
        const price = formatVietnameseCurrency(service.price);
        const vat = (service.vatRate * 100).toFixed(1) + '%';
        const warranty = service.warrantyDays + ' ngày';
        return `<option value="${escapeHtml(service.name)}" 
                        label="${escapeHtml(service.name)} - ${price} - VAT ${vat} - BH ${warranty}"
                        data-service='${escapeHtml(JSON.stringify(service))}'></option>`;
    }).join('');
}

function addItem() {
    if (window.isRequestLocked) return alert('Không thể thêm hạng mục khi phiếu đã được khóa');

    const serviceInput = document.getElementById('serviceInput');
    const datalist = document.getElementById('serviceList');
    const selectedOption = Array.from(datalist.options).find(opt => opt.value === serviceInput.value);

    if (!selectedOption) return alert('Vui lòng chọn dịch vụ từ danh sách');

    const selectedService = JSON.parse(selectedOption.getAttribute('data-service'));
    const existingItem = items.find(item => item.serviceItemId === selectedService.id);

    if (existingItem) return alert(`Dịch vụ "${selectedService.name}" đã được thêm vào danh sách. Vui lòng chọn dịch vụ khác hoặc xóa dịch vụ cũ trước.`);

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
    if (window.isRequestLocked) return alert('Không thể xóa hạng mục khi phiếu đã được khóa');
    if (!confirm('Bạn có chắc chắn muốn xóa dịch vụ này?')) return;
    items.splice(i, 1);
    renderTable();
}

function renderTable() {
    const tbody = document.querySelector('#itemsTable tbody');
    if (!tbody) return;

    let total = 0;
    tbody.innerHTML = items.map((it, i) => {
        const net = (it.price - it.discount) * it.quantity;
        const line = net + (net * it.vatRate);
        total += line;

        const deleteBtn = window.isRequestLocked ?
            `<span class="text-muted"><i class="fas fa-lock"></i></span>` :
            `<button type="button" class="btn btn-sm btn-danger" onclick="removeItem(${i})"><i class="fas fa-trash"></i></button>`;

        return `<tr>
            <td>${escapeHtml(it.name)}</td>
            <td>${it.quantity}</td>
            <td>${formatVietnameseCurrency(it.price)}</td>
            <td>${formatVietnameseCurrency(it.discount)}</td>
            <td>${(it.vatRate * 100).toFixed(1)}%</td>
            <td class="fw-bold">${formatVietnameseCurrency(line)}</td>
            <td>${deleteBtn}</td>
            ${['serviceItemId', 'quantity', 'discount', 'vatRate', 'warrantyDays', 'name', 'price']
                .map(field => `<input type="hidden" name="items[${i}].${field}" value="${escapeHtml(String(it[field] || (field === 'quantity' ? 1 : 0)))}">`)
                .join('')}
        </tr>`;
    }).join('');

    const totalAmount = document.getElementById('totalAmount');
    if (totalAmount) totalAmount.textContent = formatVietnameseCurrency(total);
}


function removeExistingItem(button) {
    if (!confirm('Bạn có chắc chắn muốn xóa dịch vụ này?')) return;

    const row = button.closest('tr');
    row.style.display = 'none';

    const hiddenInput = document.createElement('input');
    hiddenInput.type = 'hidden';
    hiddenInput.name = 'deletedItems[]';
    hiddenInput.value = row.getAttribute('data-index');
    document.querySelector('#requestForm').appendChild(hiddenInput);
}

function submitForm() {
    const form = document.getElementById('requestForm');
    const note = document.getElementById('confirmNote').value;

    form.querySelectorAll('input[name="note"]').forEach(input => input.remove());

    const hiddenNote = document.createElement('input');
    hiddenNote.type = 'hidden';
    hiddenNote.name = 'note';
    hiddenNote.value = note;
    form.appendChild(hiddenNote);

    bootstrap.Modal.getInstance(document.getElementById('confirmModal'))?.hide();
    form.submit();
}

function validateMaxImageSlots(e) {
    const files = e.target.files;
    const existingImages = document.querySelectorAll('input[name="toDelete"]:not(:checked)').length;

    if (!files.length || existingImages + files.length <= 5) return;

    const maxAllowed = 5 - existingImages;
    alert(`Chỉ còn ${maxAllowed} slot trống. Hãy bỏ chọn bớt ảnh hoặc đánh dấu xóa thêm ảnh cũ.`);
    e.target.value = '';
}

function updateImageSlots() {
    const fileInput = document.getElementById('newImages');
    if (fileInput?.files.length) validateMaxImageSlots({ target: fileInput });
}