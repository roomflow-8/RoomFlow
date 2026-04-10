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


/**
 * 비품 취소 사유 입력 후 form 전송
 * 다른 form에는 전혀 영향을 주지 않음
 */
let currentReservationId = null;

function openCancelModal(reservationId) {
    currentReservationId = reservationId;
    document.getElementById('cancelModal').classList.remove('hidden');
    document.getElementById('cancelReasonTextarea').value = '';
}

function closeCancelModal() {
    document.getElementById('cancelModal').classList.add('hidden');
}

function submitCancelForm() {
    const reason = document.getElementById('cancelReasonTextarea').value.trim();

    if (!reason) {
        alert('비품 취소 사유를 입력해주세요.');
        return;
    }

    const form = document.getElementById('cancelEquipmentsForm-' + currentReservationId);
    document.getElementById('cancelReasonInput-' + currentReservationId).value = reason;

    form.submit();
}

// 모달 배경 클릭 시 닫기
document.getElementById('cancelModal').addEventListener('click', function(e){
    if(e.target === this){ // 모달 배경을 클릭했을 때만
        closeCancelModal();
    }
});