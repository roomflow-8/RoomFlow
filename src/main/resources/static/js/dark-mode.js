document.addEventListener("DOMContentLoaded", function () {
    const darkModeMediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
    console.log(darkModeMediaQuery);

    const applyMode = () => {
        if (darkModeMediaQuery.matches) {
            document.body.classList.add("bg-gray-900", "text-white");
            document.body.classList.remove("bg-gray-50", "text-gray-900");
        } else {
            document.body.classList.add("bg-gray-50", "text-gray-900");
            document.body.classList.remove("bg-gray-900", "text-white");
        }
    }

    // 초기 적용
    applyMode();

    // 다크모드 변경 감지 시 자동 적용
    darkModeMediaQuery.addEventListener('change', applyMode);
});
