let equipmentsData = [];
let selectedEquipments = new Map();
let requestInProgress = false;
let retryCount = 0;
let pollingInterval = null;
let reservationId;

const MAX_RETRY = 3;
const POLLING_INTERVAL = 10000; // 10초마다 폴링


document.addEventListener('DOMContentLoaded', () => {
    const dataContainer = document.getElementById('equip-data');

    if (!dataContainer) {
        console.error('데이터 컨테이너(#equip-data)를 찾을 수 없습니다!');
        return;
    }
    reservationId = dataContainer.dataset.resId;
    lucide.createIcons();
    initialLoad();
});

async function initialLoad() {
    await loadEquipments();
    startPolling();
}

/**
 * 비품 목록 조회 (Core 함수)
 */
async function fetchEquipments() {
    if (requestInProgress) { return null; }

    requestInProgress = true;

    try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 10000);

        const response = await fetch(`/api/v1/reservations/${reservationId}/equipments`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            },
            signal: controller.signal
        });

        clearTimeout(timeoutId);

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${errorText || response.statusText}`);
        }

        const result = await response.json();

        if (!result.success) {
            throw new Error(result.message || '비품 목록을 불러오는데 실패했습니다.');
        }

        return result.data || [];

    } catch (err) {
        console.error(`[${new Date().toISOString()}] API 에러:`, err);
        throw err;
    } finally {
        requestInProgress = false;
    }
}

/**
 * 비품 목록 로드
 */
async function loadEquipments() {
    const loading = document.getElementById('loading');
    const error = document.getElementById('error');
    const equipmentList = document.getElementById('equipment-list');
    const noEquipment = document.getElementById('no-equipment');

    // UI 초기화
    loading?.classList.remove('hidden');
    error?.classList.add('hidden');
    equipmentList?.classList.add('hidden');
    noEquipment?.classList.add('hidden');


    try {
        equipmentsData = await fetchEquipments();
        retryCount = 0; // 성공 시 재시도 카운트 리셋

        updateUI();
        console.log(`[${new Date().toISOString()}] 비품 ${equipmentsData.length}개 로드 완료`);

    } catch (err) {
        await handleLoadError(err, loading, error);
    } finally {
        loading.classList.add('hidden');
        lucide.createIcons();
    }
}

/**
 * 에러 처리 및 재시도 로직
 */
async function handleLoadError(err, loading, error) {
    console.error(`[${new Date().toISOString()}] 로드 에러:`, err);

    let errorMessage = '비품 목록을 불러오는데 실패했습니다.';

    if (err.name === 'AbortError') {
        errorMessage = '요청 시간이 초과되었습니다. 네트워크 상태를 확인해주세요.';
    } else if (err.message.includes('NetworkError') || err.message.includes('Failed to fetch')) {
        errorMessage = '네트워크 연결을 확인해주세요.';
    } else if (err.message) {
        errorMessage = err.message;
    }

    // 자동 재시도 (최대 3회, AbortError는 제외)
    if (retryCount < MAX_RETRY && err.name !== 'AbortError') {
        retryCount++;
        console.log(`재시도 ${retryCount}/${MAX_RETRY}...`);

        await new Promise(resolve => setTimeout(resolve, 1000 * retryCount));

        return loadEquipments(); // 재귀 호출
    }

    // 재시도 실패 시 에러 표시
    document.getElementById('error-message').textContent = errorMessage;
    error.classList.remove('hidden');
}

/**
 * 폴링 시작
 */
function startPolling() {
    if (pollingInterval) {
        clearInterval(pollingInterval);
    }

    pollingInterval = setInterval(async () => {
        try {
            console.log(`[${new Date().toISOString()}] 폴링 - 비품 목록 갱신`);

            const newData = await fetchEquipments();

            if (!newData) {
                return; // 요청 진행 중이면 스킵
            }

            // 데이터 변경 확인
            if (JSON.stringify(equipmentsData) !== JSON.stringify(newData)) {
                console.log('🔄 데이터 변경 감지 - UI 업데이트');

                equipmentsData = newData;
                updateUI();

            } else {
                console.log('✅ 데이터 변경 없음');
            }

        } catch (err) {
            console.error('폴링 중 에러 (계속 진행):', err);
        }
    }, POLLING_INTERVAL);
}

/**
 * 폴링 중지
 */
function stopPolling() {
    if (pollingInterval) {
        clearInterval(pollingInterval);
        pollingInterval = null;
        console.log('📡 폴링 중지');
    }
}

/**
 * UI 업데이트
 */
function updateUI() {
    const equipmentList = document.getElementById('equipment-list');
    const noEquipment = document.getElementById('no-equipment');

    if (equipmentsData.length === 0) {
        equipmentList.classList.add('hidden');
        noEquipment.classList.remove('hidden');
        document.getElementById('equipment-count').textContent = '비품 없음';
    } else {
        if (equipmentList.children.length > 0) {
            updateStockOnly();
        } else {
            renderEquipments(equipmentsData);
        }
        equipmentList.classList.remove('hidden');
        noEquipment.classList.add('hidden');
        document.getElementById('equipment-count').textContent = `총 ${equipmentsData.length}개 비품`;
    }
}

/**
 * 비품 목록 렌더링
 */
function renderEquipments(equipments) {
    const container = document.getElementById('equipment-list');

    if (!equipments || equipments.length === 0) {
        container.innerHTML = `
            <div class="col-span-full text-center py-12">
                <i data-lucide="package-x" class="w-16 h-16 mx-auto text-gray-400 mb-4"></i>
                <p class="text-gray-500">선택한 시간대에 사용 가능한 비품이 없습니다.</p>
            </div>
        `;
        lucide.createIcons();
        return;
    }

    const sortedEquipments = sortEquipmentsByAvailability(equipments);

    container.innerHTML = '';

    sortedEquipments.forEach((equipment, index) => {
        const card = createEquipmentCard(equipment);
        card.style.animationDelay = `${index * 0.05}s`;
        container.appendChild(card);
    });
    lucide.createIcons();
}

/**
 * 비품 정렬 (대여 가능 → 불가 순)
 */
function sortEquipmentsByAvailability(equipments) {
    return equipments.sort((a, b) => {
        const aAvailable = a.availableStock > 0 && a.status === 'AVAILABLE';
        const bAvailable = b.availableStock > 0 && b.status === 'AVAILABLE';

        if (aAvailable && !bAvailable) return -1;
        if (!aAvailable && bAvailable) return 1;

        if (aAvailable && bAvailable) {
            return b.availableStock - a.availableStock;
        }

        return a.equipmentName.localeCompare(b.equipmentName);
    });
}

/**
 * 비품 카드 생성
 */
function createEquipmentCard(equipment) {
    const isAvailable = equipment.availableStock > 0 && equipment.status === 'AVAILABLE';
    const iconName = getIconName(equipment.equipmentName);

    const card = document.createElement('div');
    card.className = `equipment-card bg-white rounded-lg shadow transition-all duration-200 fade-in ${!isAvailable ? 'opacity-60' : ''}`;
    card.dataset.equipmentId = equipment.equipmentId;

    card.innerHTML = `
        <div class="p-6 border-b border-gray-200">
            <div class="flex items-start justify-between">
                <div class="flex items-center gap-3 flex-1">
                    <div class="p-3 bg-blue-100 rounded-lg">
                        <i data-lucide="${iconName}" class="w-6 h-6 text-blue-600"></i>
                    </div>
                    <div class="flex-1">
                        <h3 class="text-lg font-semibold text-gray-900">${equipment.equipmentName}</h3>
                    </div>
                </div>
                <button class="text-blue-600 hover:bg-blue-50 p-2 rounded transition-colors"
                        onclick="showDetail(${equipment.equipmentId})"
                        title="상세 정보">
                    <i data-lucide="info" class="w-4 h-4"></i>
                </button>
            </div>
        </div>

        <div class="p-6">
            <div class="space-y-4">
                <div class="flex items-center justify-between">
                    <div>
                        <p class="text-sm text-gray-600">재고 현황</p>
                        <p class="text-lg font-semibold text-gray-900">
                            <span class="available-stock ${equipment.availableStock === 0 ? 'text-red-600' : 'text-blue-600'}">${equipment.availableStock}</span>
                            / <span class="total-stock">${equipment.totalStock}</span>
                        </p>
                    </div>
                    <span class="status-badge px-3 py-1 ${isAvailable ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'} rounded-full text-sm font-medium">
                        ${isAvailable ? '대여 가능' : '대여 불가'}
                    </span>
                </div>

                <div>
                    <p class="text-sm text-gray-600">대여료</p>
                    <p class="text-xl font-bold text-gray-900">${equipment.price.toLocaleString()}원</p>
                </div>

                ${isAvailable ? `
                <div class="pt-4 border-t">
                    <p class="text-sm text-gray-600 mb-2">수량 선택</p>
                   <div class="flex items-center gap-2">
                        <button class="btn-decrease flex-1 py-2 border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                                onclick="decreaseQuantity(${equipment.equipmentId})" disabled>
                            <i data-lucide="minus" class="w-4 h-4 mx-auto"></i>
                        </button>
                    
                        <div class="w-14 text-center font-semibold text-lg quantity-display">0</div>
                    
                        <button class="btn-increase flex-1 py-2 border border-gray-300 rounded-md hover:bg-gray-50 transition-all"
                                onclick="increaseQuantity(${equipment.equipmentId})"
                                data-max="${equipment.availableStock}">
                            <i data-lucide="plus" class="w-4 h-4 mx-auto"></i>
                        </button>
                    </div>
                    </div>
                </div>
                ` : `
                <div class="pt-4 border-t">
                    <p class="text-sm text-red-600 text-center">현재 대여 불가능합니다</p>
                </div>
                `}
            </div>
        </div>
    `;

    return card;
}

/**
 * 재고 정보만 업데이트
 */
function updateStockOnly() {
    let updateCount = 0;

    equipmentsData.forEach(equipment => {
        const card = findCard(equipment.equipmentId);
        if (!card) return;

        // 재고 숫자 업데이트
        const availableStockSpan = card.querySelector('.available-stock');
        const totalStockSpan = card.querySelector('.total-stock');
        const statusBadge = card.querySelector('.status-badge');

        if (availableStockSpan) {
            availableStockSpan.textContent = equipment.availableStock;
            availableStockSpan.className = equipment.availableStock === 0
                ? 'available-stock text-red-600'
                : 'available-stock text-blue-600';
        }

        if (totalStockSpan) {
            totalStockSpan.textContent = equipment.totalStock;
        }

        // 상태 배지 업데이트
        const isAvailable = equipment.availableStock > 0 && equipment.status === 'AVAILABLE';
        if (statusBadge) {
            statusBadge.className = `status-badge px-3 py-1 ${isAvailable ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'} rounded-full text-sm font-medium`;
            statusBadge.textContent = isAvailable ? '대여 가능' : '대여 불가';
        }

        // 카드 opacity
        if (!isAvailable) {
            card.classList.add('opacity-60');
        } else {
            card.classList.remove('opacity-60');
        }
        updateCount++;
    });
    console.log(`✅ 재고 업데이트 완료 - ${updateCount}개`);
}


/**
 * 페이지 visibility 변경 시
 */
document.addEventListener('visibilitychange', function () {
    if (document.hidden) {
        console.log('페이지 숨김 - 폴링 중지');
        stopPolling();
    } else {
        console.log('페이지 표시 - 폴링 재시작');
        startPolling();
    }
});

function validateForm() {
    const inputs = document.querySelectorAll('.equipment-quantity-input');

    let hasQuantity = false;

    inputs.forEach(input => {
        if (parseInt(input.value) > 0) {
            hasQuantity = true;
        }
    });

    if (!hasQuantity) {
        showToast('비품을 선택해주세요.', 'error');
        return false;
    }

    return true;
}

// ==================== 아이콘 매핑 ====================
function getIconName(name) {
    const lowerName = name.toLowerCase();
    if (lowerName.includes('노트북') || lowerName.includes('크롬북') || lowerName.includes('태블릿')) return 'laptop';
    if (lowerName.includes('키보드')) return 'keyboard';
    if (lowerName.includes('마우스')) return 'mouse';
    if (lowerName.includes('마이크') || lowerName.includes('스피커')) return 'mic';
    if (lowerName.includes('헤드폰')) return 'headphones';
    if (lowerName.includes('웹캠') || lowerName.includes('카메라')) return 'video';
    if (lowerName.includes('케이블') || lowerName.includes('어댑터') || lowerName.includes('허브')) return 'cable';
    if (lowerName.includes('프로젝터')) return 'projector';
    if (lowerName.includes('화이트보드') || lowerName.includes('플립차트')) return 'presentation';
    if (lowerName.includes('충전기')) return 'battery-charging';
    if (lowerName.includes('거치대')) return 'smartphone';
    return 'package';
}

// ==================== 유틸리티 함수 ====================
function findEquipment(id) {
    return equipmentsData.find(e => e.equipmentId === id);
}

function findCard(id) {
    return document.querySelector(`[data-equipment-id="${id}"]`);
}

// ==================== 수량 조절 ====================
function increaseQuantity(id) {
    const equipment = findEquipment(id);
    const card = findCard(id);
    if (!equipment || !card) return;

    const quantityDisplay = card.querySelector('.quantity-display');
    const decreaseBtn = card.querySelector('.btn-decrease');
    const increaseBtn = card.querySelector('.btn-increase');
    const input = card.querySelector('.equipment-quantity-input');

    let quantity = parseInt(quantityDisplay.textContent);
    if (quantity < equipment.availableStock) {
        quantity++;
        quantityDisplay.textContent = quantity;
        input.value = quantity;

        decreaseBtn.disabled = false;

        if (quantity === equipment.availableStock) {
            increaseBtn.disabled = true;
        }
        selectedEquipments.set(id, {
            ...equipment,
            quantity: quantity
        });

        card.classList.add('selected-card');
        updateCart(equipment, quantity);
    }
}

function decreaseQuantity(id) {
    const equipment = findEquipment(id);
    const card = findCard(id);
    if (!equipment || !card) return;

    const quantityDisplay = card.querySelector('.quantity-display');
    const decreaseBtn = card.querySelector('.btn-decrease');
    const increaseBtn = card.querySelector('.btn-increase');
    const input = card.querySelector('.equipment-quantity-input');

    let quantity = parseInt(quantityDisplay.textContent);
    if (quantity > 0) {
        quantity--;
        quantityDisplay.textContent = quantity;
        input.value = quantity;

        increaseBtn.disabled = false;

        if (quantity === 0) {
            decreaseBtn.disabled = true;
            card.classList.remove('selected-card');
            selectedEquipments.delete(id);
        } else {
            selectedEquipments.set(id, {
                ...equipment,
                quantity: quantity
            });
        }
        updateCart(equipment, quantity);
    }
}

// ==================== 장바구니 관리 ====================
function updateCart(equipment, quantity) {
    const id = String(equipment.equipmentId);

    if (quantity > 0) {
        selectedEquipments.set(id, {
            id: id,
            name: equipment.equipmentName,
            price: equipment.price,
            quantity: quantity,
            available: equipment.availableStock
        });
    } else {
        selectedEquipments.delete(id);
    }

    renderCart();
}

function renderCart() {
    const cartEmpty = document.getElementById('cart-empty');
    const cartItems = document.getElementById('cart-items');
    const cartSummary = document.getElementById('cart-summary');

    if (selectedEquipments.size === 0) {
        cartEmpty.classList.remove('hidden');
        cartItems.classList.add('hidden');
        cartSummary.classList.add('hidden');
        return;
    }

    cartEmpty.classList.add('hidden');
    cartItems.classList.remove('hidden');
    cartSummary.classList.remove('hidden');

    let html = '';
    let totalQuantity = 0;
    let totalFee = 0;

    selectedEquipments.forEach((item) => {
        const subtotal = item.quantity * item.price;
        totalQuantity += item.quantity;
        totalFee += subtotal;

        const equipment = findEquipment(parseInt(item.id));
        const iconName = equipment ? getIconName(equipment.equipmentName) : 'package';

        html += `
                    <div class="bg-white border border-gray-200 rounded-lg p-4 relative hover:shadow-md transition-shadow">
                        <button onclick="removeFromCart('${item.id}')"
                                class="absolute top-2 right-2 text-red-600 hover:bg-red-50 p-1 rounded transition-colors"
                                title="제거">
                            <i data-lucide="x" class="w-4 h-4"></i>
                        </button>

                        <div class="flex items-start gap-3 pr-8">
                            <div class="p-2 bg-blue-100 rounded">
                                <i data-lucide="${iconName}" class="w-5 h-5 text-blue-600"></i>
                            </div>
                            <div class="flex-1">
                                <p class="font-semibold text-sm text-gray-900">${item.name}</p>
                                <div class="flex items-center gap-2 mt-2">
                                    <button onclick="decreaseQuantity(${item.id})"
                                            class="h-7 w-7 border border-gray-300 rounded hover:bg-gray-50 flex items-center justify-center transition-colors">
                                        <i data-lucide="minus" class="w-3 h-3"></i>
                                    </button>
                                    <span class="w-8 text-center font-semibold">${item.quantity}</span>
                                    <button onclick="increaseQuantity(${item.id})"
                                            class="h-7 w-7 border border-gray-300 rounded hover:bg-gray-50 flex items-center justify-center transition-colors"
                                            ${item.quantity >= item.available ? 'disabled style="opacity:0.5;cursor:not-allowed;"' : ''}>
                                        <i data-lucide="plus" class="w-3 h-3"></i>
                                    </button>
                                </div>
                                <div class="mt-3 text-sm">
                                    <div class="flex justify-between text-gray-600">
                                        <span>단가:</span>
                                        <span class="font-medium">${item.price.toLocaleString()}원</span>
                                    </div>
                                    <div class="flex justify-between font-semibold mt-1">
                                        <span>소계:</span>
                                        <span class="text-blue-600">${subtotal.toLocaleString()}원</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
    });

    cartItems.innerHTML = html;
    document.getElementById('total-quantity').textContent = totalQuantity;
    document.getElementById('total-fee').textContent = totalFee.toLocaleString();

    lucide.createIcons();
}

function removeFromCart(id) {
    selectedEquipments.delete(id);

    const card = findCard(parseInt(id));
    if (card) {
        card.querySelector('.quantity-display').textContent = '0';
        card.querySelector('.btn-decrease').disabled = true;
        card.querySelector('.btn-increase').disabled = false;
        card.classList.remove('selected-card');
    }

    renderCart();
}

// ==================== 제출 ====================
function submitEquipments(event) {
    if (selectedEquipments.size === 0) {
        showToast('비품을 선택해주세요.', 'error');
        return;
    }

    // 중복 클릭 방지
    if (requestInProgress) {
        console.log('요청이 이미 진행 중입니다.');
        return;
    }

    // 예약 정보 가져오기
    const equipData = document.getElementById('equip-data');
    if (!equipData) {
        showToast('예약 정보를 찾을 수 없습니다.', 'error');
        return;
    }
    const reservationId = equipData.dataset.resId;  // data-res-id
    const reservationStatus = equipData.dataset.reservationStatus;

    const equipmentList = Array.from(selectedEquipments.values()).map(item => ({
        equipmentId: parseInt(item.id),
        quantity: item.quantity,
    }));

    console.log('선택된 비품:', equipmentList);
    console.log('예약 상태:', reservationStatus);

    // 로딩 표시
    const submitBtn = event.target;
    const originalText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i data-lucide="loader" class="w-4 h-4 inline animate-spin"></i> 처리중...';
    lucide.createIcons();

    requestInProgress = true;

// 예약 상태에 따라 다른 API 호출 ⭐
    if (reservationStatus === 'CONFIRMED') {
        // 시나리오 2: 기존 예약에 비품만 확정
        confirmEquipmentsOnly(reservationId, equipmentList, submitBtn, originalText);
    } else {
        // 시나리오 1: 회의실 + 비품 함께 확정
        confirmReservationWithEquipments(reservationId, equipmentList, submitBtn, originalText);
    }
}

// ==================== 상세 정보 ====================
/**
 * 비품 상세 정보 표시
 */
function showDetail(id) {
    const equipment = findEquipment(id);
    if (!equipment) return;

    // 1. 데이터 매칭 (서버에서 받은 실제 데이터 중심)
    document.getElementById('modal-name').textContent = equipment.equipmentName;

    // 설명이 데이터에 없다면 기본값 표시, 있다면 해당 값 표시
    document.getElementById('modal-description').textContent =
        equipment.description || `${equipment.equipmentName}에 대한 상세 정보입니다. 업무 및 회의 시 대여하여 사용 가능합니다.`;

    // 2. 실시간 재고 및 가격 정보
    document.getElementById('modal-stock').textContent = `${equipment.availableStock} / ${equipment.totalStock}`;
    document.getElementById('modal-price').textContent = `${equipment.price.toLocaleString()}원`;

    // 3. 대여 가능 상태 배지 처리
    const isAvailable = equipment.availableStock > 0;
    const badgeContainer = document.getElementById('modal-status-badge');

    if (isAvailable) {
        badgeContainer.innerHTML = `
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                <span class="w-1.5 h-1.5 rounded-full bg-green-500 mr-1.5"></span>
                대여 가능
            </span>`;
    } else {
        badgeContainer.innerHTML = `
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
                <span class="w-1.5 h-1.5 rounded-full bg-red-500 mr-1.5"></span>
                대여 불가
            </span>`;
    }

    // 4. 아이콘 동적 변경
    const modalImage = document.getElementById('modal-image');
    const modalIcon = document.getElementById('modal-icon');
    if (equipment.imageUrl && equipment.imageUrl.trim() !== "") {
        // 1. 이미지 URL이 있는 경우
        modalImage.src = equipment.imageUrl;
        modalImage.classList.remove('hidden'); // 이미지 보이기
        modalIcon.classList.add('hidden');    // 아이콘 숨기기
    } else {
        // 2. 이미지 URL이 비어있는 경우 (빈 주소일 때)
        modalImage.src = "";
        modalImage.classList.add('hidden');    // 이미지 숨기기
        modalIcon.classList.remove('hidden'); // 아이콘 보이기

        // 기존 아이콘 동적 변경 로직 유지
        const iconName = getIconName(equipment.equipmentName);
        modalIcon.setAttribute('data-lucide', iconName);
        lucide.createIcons();
    }
    // 5. 모달 열기 및 아이콘 새로고침
    const modal = document.getElementById('detail-dialog');
    modal.classList.remove('hidden');

    lucide.createIcons();
}

/**
 * 다이얼로그 닫기
 */
function closeDetail() {
    const modal = document.getElementById('detail-dialog');
    modal.classList.add('hidden');
}

// 배경 클릭 시 닫기
window.onclick = function (event) {
    const modal = document.getElementById('detail-dialog');
    if (event.target == modal) {
        closeDetail();
    }
}

// ==================== 이벤트 리스너 ====================
// 에러 핸들링
window.addEventListener('error', function (e) {
    console.error('전역 에러:', e.error);
});

// 토스트 함수 추가
window.showToast = function(message, type = 'success', duration=3000, onClose) {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `
    px-4 py-3 rounded-lg shadow-lg text-white
    opacity-0 translate-y-2 transition-all duration-300
    ${type === 'error' ? 'bg-red-500' : 'bg-green-500'}
    `;
    toast.textContent = message;

    container.appendChild(toast);
    requestAnimationFrame(() => {
        toast.classList.remove('opacity-0', 'translate-y-2');
    });

    setTimeout(() => {
        /*toast.classList.remove('show');*/
        toast.classList.add('opacity-0', 'translate-y-2');
        setTimeout(() => {
            toast.remove();
            onClose?.();
        }, 100);
    }, duration);
};

function confirmBack(event) {
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
            history.back();
        }
    });

    return false;
}