document.addEventListener('DOMContentLoaded', function() {
    // Use setTimeout to ensure the overrides happen after original values are set
    setTimeout(() => {
        // Change the title of the HTML document
        document.title = "Ellithium Test Report"; 

        // Change the favicon
        const link = document.createElement('link');
        link.rel = 'icon';
        link.type = 'image/png'; // Change this to 'image/x-icon' if using .ico
        link.href = 'https://raw.githubusercontent.com/Abdelrhman-Ellithy/Ellithium/refs/heads/main/src/main/resources/logo/Ellithium.ico?raw=true'; // Adjust the path as necessary
        
        // Remove any existing favicon link elements
        const existingFavicon = document.querySelector("link[rel='icon']");
        if (existingFavicon) {
            existingFavicon.parentNode.removeChild(existingFavicon);
        }

        document.head.appendChild(link);
    }, 0); // 0 ms delay to ensure the overrides occur after the initial load
});
