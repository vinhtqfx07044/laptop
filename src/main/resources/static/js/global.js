// Global logout function.
function doLogout() {
    // Confirms with the user before logging out.
    if (confirm('Bạn có chắc chắn muốn đăng xuất?')) {
        const logoutForm = document.getElementById('logoutForm');
        if (logoutForm) {
            logoutForm.submit();
        } else {
            globalThis.location.href = '/logout';
        }
    }
}

// Formats a number as Vietnamese currency.
function formatVietnameseCurrency(amount) {
    if (!amount) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
    }).format(amount) + ' ₫';
}

// Automatically formats all currency elements on page load.
document.addEventListener('DOMContentLoaded', function () {
    formatAllCurrencyElements();
});

// Iterates through all elements with the 'currency-format' class and formats their content.
function formatAllCurrencyElements() {
    const currencyElements = document.querySelectorAll('.currency-format');
    for (const element of currencyElements) {
        const amount = element.dataset.amount;
        if (amount !== null) {
            element.textContent = formatVietnameseCurrency(Number.parseFloat(amount));
        }
    }
}