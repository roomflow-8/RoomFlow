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
            maxDate: getOneMonthLater(),
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

function getOneMonthLater(date = new Date()) {
    const result = new Date(date);
    const originalDate = result.getDate();

    result.setMonth(result.getMonth() + 1);

    // 말일 보정
    if (result.getDate() !== originalDate) {
        result.setDate(0);
    }

    return result;
}

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

document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll(".reservation-form").forEach(form => {
        form.addEventListener("submit", function (e) {
            const checked = form.querySelectorAll('input[name="roomSlotIds"]:checked');

            if (checked.length === 0) {
                e.preventDefault();

                Swal.fire({
                    toast: true,
                    position: "top",
                    icon: "warning",
                    title: "예약 시간을 선택해주세요.",
                    showConfirmButton: false,
                    timer: 1800,
                    timerProgressBar: true
                });
            }
        });
    });
});