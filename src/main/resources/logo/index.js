document.addEventListener('DOMContentLoaded', function() {
    // Change the title of the HTML document
    document.title = "Ellithium Test Report";
    // Change the favicon
    const link = document.createElement('link');
    link.rel = 'icon';
    link.type = 'image/png'; // Change this to 'image/x-icon' if using .ico
    link.href = 'plugin/custom-logo/Ellithium.ico'; // Adjust the path as necessary
    document.head.appendChild(link);
});
