// Global functions available to all pages


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

