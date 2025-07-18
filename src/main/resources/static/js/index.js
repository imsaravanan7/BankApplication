
document.addEventListener("DOMContentLoaded", function () {
    var hamburger = document.querySelector(".hamburger");
    var navLinkDrop = document.querySelector(".nav-links-drop");

    if (hamburger && navLinkDrop) {
        hamburger.addEventListener("click", function (event) {
            event.stopPropagation(); // Prevent click from bubbling up
            navLinkDrop.classList.toggle("hide");
        });

        // Prevent clicks inside the dropdown from closing it
        navLinkDrop.addEventListener("click", function (event) {
            event.stopPropagation();
        });

        // Click outside => close dropdown
        document.addEventListener("click", function () {
            navLinkDrop.classList.add("hide");
        });
    }

    // Restrict input to max 10 characters
    document.querySelectorAll(".restrictInput").forEach(function (input) {
        input.addEventListener("input", function () {
            this.value = this.value.slice(0, 10);
        });
    });
});