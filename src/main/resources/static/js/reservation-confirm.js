function confirmGoBack(event, form) {
    event.preventDefault();

    Swal.fire({
        title: '이전 단계로 이동하시겠습니까?',
        html: '이전 단계로 이동 시<br><b>예약 정보가 초기화됩니다.</b>',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#000',
        cancelButtonColor: '#9ca3af',
        confirmButtonText: '이동',
        cancelButtonText: '취소'
    }).then((result) => {
        if (result.isConfirmed) {
            form.submit();
        }
    });

    return false;
}