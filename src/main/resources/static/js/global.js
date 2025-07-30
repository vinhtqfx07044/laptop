//Global logout function
function doLogout() {
    if (confirm('Bạn có chắc chắn muốn đăng xuất?')) {
        const logoutForm = document.getElementById('logoutForm');
        if (logoutForm) {
            logoutForm.submit();
        } else {
            window.location.href = '/logout';
        }
    }
}

// Vietnamese currency formatter
function formatVietnameseCurrency(amount) {
    if (!amount) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
    }).format(amount) + ' ₫';
}

// Auto-format all currency elements on page load
document.addEventListener('DOMContentLoaded', function () {
    formatAllCurrencyElements();
});

function formatAllCurrencyElements() {
    const currencyElements = document.querySelectorAll('.currency-format');
    currencyElements.forEach(element => {
        const amount = element.getAttribute('data-amount');
        if (amount !== null) {
            element.textContent = formatVietnameseCurrency(parseFloat(amount));
        }
    });
}