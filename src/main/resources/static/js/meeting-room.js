document.addEventListener("DOMContentLoaded", function () {
    const hiddenInput = document.getElementById("selectedDate");
    const pickerInput = document.getElementById("calendarPicker");

    if (hiddenInput && pickerInput) {
        const selectedDate = hiddenInput.dataset.date || hiddenInput.value || "today";

        const picker = flatpickr(pickerInput, {
            inline: true,
            dateFormat: "Y-m-d",
            defaultDate: selectedDate,
            minDate: "today",
            maxDate: new Date().fp_incr(30),
            monthSelectorType: "static",
            onChange: function (selectedDates, dateStr) {
                hiddenInput.value = dateStr;
                hiddenInput.dataset.date = dateStr;
                picker.setDate(dateStr, false);
                window.location.href = `/rooms?date=${dateStr}`;
            }
        });

        picker.setDate(selectedDate, false);
    }
});

function toggleRoom(button) {
    const card = button.closest(".room-card");
    const detail = card.querySelector(".room-detail");

    if (!detail) return;

    if (detail.classList.contains("hidden")) {
        detail.classList.remove("hidden");
        button.innerText = "접기 ▲";
    } else {
        detail.classList.add("hidden");
        button.innerText = "시간 선택 ▼";
    }
}