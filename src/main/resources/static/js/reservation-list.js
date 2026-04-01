document.querySelectorAll('.cancel-form').forEach(form => {
    form.addEventListener('submit', function (e) {
        e.preventDefault();

        Swal.fire({
            title: '회의실을 취소하시겠습니까?',
            input: 'text',
            inputLabel: '취소 사유를 입력해주세요',
            inputPlaceholder: '예: 일정 변경',
            showCancelButton: true,
            confirmButtonText: '취소하기',
            cancelButtonText: '닫기'
        }).then(result => {

            if (!result.isConfirmed) return;

            const reasonInput = document.createElement('input');
            reasonInput.type = 'hidden';
            reasonInput.name = 'reason';
            reasonInput.value = result.value ?? '';

            form.appendChild(reasonInput);
            form.submit();
        });
    });
});