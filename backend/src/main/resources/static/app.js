'use strict';

/**
 * Access token хранится только в памяти вкладки. Refresh token браузер получает
 * в HttpOnly/SameSite cookie.
 */
const state = {
    accessToken: null,
    user: null,
    departments: [],
    patients: { page: 0, size: 25, totalPages: 0, query: '', content: [] },
    selectedPatientIds: new Set(),
    activePatientForNotes: null,
    refreshPromise: null,
    users: []
};

const $ = (id) => document.getElementById(id);
const escapeHtml = (value) => String(value ?? '').replace(/[&<>'"]/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[ch]));
const roles = () => new Set((state.user?.roles || []).map(String));
const isAdmin = () => roles().has('ADMIN') || roles().has('ROLE_ADMIN');
const canEdit = () => roles().has('DOCTOR') || roles().has('ROLE_DOCTOR') || isAdmin();

/**
 * Коды ролей в API остаются английскими; в интерфейсе используются русские названия.
 */
function roleLabel(value) {
    const clean = String(value || '').replace('ROLE_', '');
    return ({
        ADMIN: 'Администратор',
        DOCTOR: 'Врач / Специалист',
        VIEWER: 'Наблюдатель'
    })[clean] || value;
}

/** Русские имена полей для ошибок валидации. */
function fieldLabel(value) {
    return ({
        externalId: 'номер медкарты',
        firstName: 'имя',
        lastName: 'фамилия',
        middleName: 'отчество',
        email: 'email',
        phone: 'телефон',
        birthDate: 'дата рождения',
        departmentId: 'отделение',
        status: 'статус',
        rehabilitationScore: 'шкала реабилитации',
        requiresAssistance: 'нуждается в уходе',
        tags: 'теги',
        version: 'версия',
        code: 'код отделения',
        name: 'название отделения',
        capacity: 'вместимость',
        temporaryPassword: 'временный пароль',
        currentPassword: 'текущий пароль',
        newPassword: 'новый пароль'
    })[value] || value;
}

/** Приводит первую букву текста после двоеточия к нижнему регистру. */
function lowerFirst(value) {
    const text = String(value ?? '');
    return text ? text.charAt(0).toLocaleLowerCase('ru-RU') + text.slice(1) : text;
}

/**
 * Нормализует сообщения вида «поле: Ошибка» в «поле: ошибка».
 */
function normalizeAfterColon(value) {
    return String(value ?? '').replace(/:\s*([А-ЯЁA-Z])/g,
        (_, letter) => `: ${letter.toLocaleLowerCase('ru-RU')}`);
}

/**
 * Приводит ввод телефона к формату +7-999-999-99-99.
 */
function formatRussianPhone(value) {
    let digits = String(value ?? '').replace(/\D/g, '');

    if (digits.startsWith('8')) {
        digits = `7${digits.slice(1)}`;
    }

    if (digits.startsWith('7')) {
        digits = digits.slice(1);
    }

    digits = digits.slice(0, 10);

    let result = '+7-';
    if (!digits.length) return result;

    result += digits.slice(0, 3);
    if (digits.length > 3) result += `-${digits.slice(3, 6)}`;
    if (digits.length > 6) result += `-${digits.slice(6, 8)}`;
    if (digits.length > 8) result += `-${digits.slice(8, 10)}`;

    return result;
}

/**
 * Маска ввода телефона для российских номеров: +7-999-999-99-99
 */
function bindPhoneMask(input) {
    if (!input) return;

    input.addEventListener('focus', () => {
        if (!input.value) input.value = '+7-';
    });

    input.addEventListener('keydown', event => {
        const allowedKeys = new Set([
            'Backspace', 'Delete', 'ArrowLeft', 'ArrowRight',
            'Home', 'End', 'Tab', 'Enter'
        ]);

        if (event.ctrlKey || event.metaKey || allowedKeys.has(event.key) || /^\d$/.test(event.key)) {
            return;
        }

        event.preventDefault();
    });

    input.addEventListener('input', () => {
        input.value = formatRussianPhone(input.value);
    });

    input.addEventListener('blur', () => {
        if (input.value === '+7-') input.value = '';
    });
}

function toast(message, error = false) {
    const node = $('toast');
    if (!node) return;
    node.textContent = message;
    node.className = `toast show${error ? ' error' : ''}`;
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => node.className = 'toast', 3500);
}

function showFormError(form, message) {
    const box = form.querySelector('[data-form-error]');
    if (!box) {
        toast(message, true);
        return;
    }
    box.textContent = message;
    box.hidden = false;
}

function clearFormError(form) {
    const box = form.querySelector('[data-form-error]');
    if (!box) return;
    box.textContent = '';
    box.hidden = true;
}

async function parseError(response) {
    try {
        const body = await response.json();
        const fields = body.validationErrors
            ? Object.entries(body.validationErrors)
                .map(([key, value]) => `${fieldLabel(key)}: ${lowerFirst(value)}`)
                .join('; ')
            : '';

        const message = `${body.message || response.statusText}${fields ? ` — ${fields}` : ''}${body.correlationId ? ` [${body.correlationId}]` : ''}`;
        return normalizeAfterColon(message);
    } catch (_) {
        return `${response.status} ${response.statusText}`;
    }
}

/**
 * Единый HTTP-клиент: перехватывает 401 и обновляет access JWT по refresh cookie.
 */
async function api(path, options = {}, retry = true) {
    const headers = new Headers(options.headers || {});
    if (state.accessToken) headers.set('Authorization', `Bearer ${state.accessToken}`);
    if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }
    const response = await fetch(`/api${path}`, {...options, headers, credentials: 'include'});
    if (response.status === 401 && retry) {
        const refreshed = await refreshAccessToken();
        if (refreshed) return api(path, options, false);
    }
    if (!response.ok) throw new Error(await parseError(response));
    if (response.status === 204) return null;
    const type = response.headers.get('content-type') || '';
    return type.includes('application/json') ? response.json() : response;
}

async function refreshAccessToken() {
    if (!state.refreshPromise) {
        state.refreshPromise = fetch('/api/auth/refresh', {
            method: 'POST',
            credentials: 'include',
            headers: {'Content-Type': 'application/json'},
            body: '{}'
        }).then(async response => {
            if (!response.ok) return false;
            const result = await response.json();
            state.accessToken = result.accessToken;
            state.user = result.user;
            return true;
        }).catch(() => false).finally(() => state.refreshPromise = null);
    }
    return state.refreshPromise;
}

async function boot() {
    bindEvents();
    if (await refreshAccessToken()) {
        await enterApplication();
    } else {
        showLogin();
    }
}

function showLogin() {
    $('app-view').hidden = true;
    $('login-view').hidden = false;
    state.accessToken = null;
    state.user = null;
}

async function enterApplication() {
    $('login-view').hidden = true;
    $('app-view').hidden = false;
    applyRoleVisibility();
    $('current-user').innerHTML = `<strong>${escapeHtml(state.user.displayName)}</strong><br>${escapeHtml(state.user.email)}<br>${escapeHtml((state.user.roles || []).map(roleLabel).join(', '))}`;

    if (state.user.passwordChangeRequired) {
        toast('Для продолжения необходимо сменить временный пароль', true);
        const changed = await openPasswordDialog(true);
        if (!changed) return;
    }

    try {
        await Promise.all([loadDepartments(), loadDashboard()]);
    } catch (error) {
        console.error('[Rehab Center] Initial data loading failed', error);
        toast(`Вход выполнен, но часть данных не загрузилась: ${error.message}`, true);
    }
}

function applyRoleVisibility() {
    const admin = isAdmin();
    const edit = canEdit();
    document.querySelectorAll('.admin-only').forEach(node => node.hidden = !admin);
    document.querySelectorAll('.doctor-only').forEach(node => node.hidden = !edit);
    document.querySelectorAll('.teacher-only').forEach(node => node.hidden = !edit);
}

function switchTab(name) {
    document.querySelectorAll('.tab').forEach(tab => tab.classList.toggle('active', tab.id === `tab-${name}`));
    document.querySelectorAll('.nav-item').forEach(button => {
        if (button.dataset.tab) button.classList.toggle('active', button.dataset.tab === name);
    });
    const titles = {
        dashboard: ['Аналитика', 'Ключевые показатели работы реабилитационного центра'],
        patients: ['Пациенты', 'Умный поиск, карточки и массовые операции'],
        departments: ['Отделения', 'Справочник отделений реабилитационного центра'],
        users: ['Пользователи', 'Роли, блокировка и временные пароли'],
        audit: ['Журнал аудита', 'История действий пользователей и аудит']
    };
    if (titles[name]) {
        [$('page-title').textContent, $('page-subtitle').textContent] = titles[name];
    }
    if (name === 'patients') loadPatients();
    if (name === 'departments') renderDepartments();
    if (name === 'users' && isAdmin()) loadUsers();
    if (name === 'audit' && isAdmin()) loadAudit();
    if (name === 'dashboard') loadDashboard();
}

async function loadDashboard() {
    try {
        const data = await api('/analytics/summary');
        const metrics = [
            ['Всего пациентов', data.total],
            ['Активные', data.active],
            ['В архиве', data.archived],
            ['Требуют ухода', data.requiringAssistance ?? data.withDebt ?? 0],
            ['Ср. балл реабилитации', (data.averageScore ?? data.averageGrade ?? '—')]
        ];
        $('metric-grid').innerHTML = metrics.map(([name, value]) => `<article class="metric card"><span class="muted">${name}</span><strong>${value}</strong></article>`).join('');
        renderBars('chart-departments', data.byDepartment || data.byGroup || []);
        renderBars('chart-status', data.byStatus || []);
        renderBars('chart-risk', data.byRisk || []);
    } catch (error) {
        toast(error.message, true);
    }
}

function renderBars(id, values) {
    const target = $(id);
    if (!target) return;
    const max = Math.max(1, ...(values || []).map(item => item.count));

    const displayName = value => {
        if (id === 'chart-status') return statusLabel(value);
        if (id === 'chart-risk') return riskLabel(value);
        return value;
    };

    target.innerHTML = (values && values.length)
        ? values.map(item => `<div class="bar-row"><span>${escapeHtml(displayName(item.name))}</span><progress value="${item.count}" max="${max}"></progress><strong>${item.count}</strong></div>`).join('')
        : '<p class="muted">Нет данных</p>';
}

async function loadDepartments() {
    try {
        state.departments = await api('/departments?activeOnly=false');
        renderDepartments();
    } catch (error) {
        toast(error.message, true);
    }
}

function renderDepartments() {
    const body = $('departments-body');
    if (!body) return;
    body.innerHTML = state.departments.map(dept => `<tr>
        <td><strong>${escapeHtml(dept.code)}</strong></td>
        <td>${escapeHtml(dept.name || '—')}</td>
        <td>${dept.capacity ?? '—'}</td>
        <td><span class="badge ${dept.active ? 'success' : ''}">${dept.active ? 'Да' : 'Нет'}</span></td>
        <td class="admin-only"><div class="actions">
            <button class="secondary" data-dept-edit="${dept.id}">Изменить</button>
            <button class="secondary" data-dept-toggle="${dept.id}">${dept.active ? 'Отключить' : 'Включить'}</button>
        </div></td>
    </tr>`).join('') || '<tr><td colspan="5" class="muted">Отделения не найдены</td></tr>';
    applyRoleVisibility();
}

async function loadPatients(page = state.patients.page) {
    try {
        const q = encodeURIComponent(state.patients.query);
        const result = await api(`/patients?q=${q}&page=${page}&size=${state.patients.size}&sort=name,asc`);
        state.patients = {...state.patients, ...result};
        state.selectedPatientIds.clear();
        renderPatients();
    } catch (error) {
        toast(error.message, true);
    }
}

function renderPatients() {
    $('patients-body').innerHTML = state.patients.content.map(patient => `<tr class="${patient.archived ? 'archived' : ''}">
        <td class="doctor-only"><input type="checkbox" data-patient-select="${patient.id}"></td>
        <td><code>${escapeHtml(patient.externalId || '—')}</code></td>
        <td><strong>${escapeHtml(patient.fullName)}</strong></td>
        <td>${escapeHtml(patient.departmentCode || '—')}</td>
        <td>${escapeHtml(statusLabel(patient.status))}</td>
        <td>${patient.rehabilitationScore != null ? Number(patient.rehabilitationScore).toFixed(1) : '—'}</td>
        <td>${patient.requiresAssistance ? '<span class="badge warning">Да</span>' : 'Нет'}</td>
        <td><span class="badge risk-${patient.riskLevel}">${escapeHtml(riskLabel(patient.riskLevel))}</span></td>
        <td>${(patient.tags || []).map(tag => `<span class="badge">${escapeHtml(tag)}</span>`).join(' ') || '—'}</td>
        <td><div class="actions">
            <button class="secondary" data-patient-view="${patient.id}">Карточка</button>
            ${canEdit() ? `<button class="secondary" data-patient-edit="${patient.id}">Изменить</button><button class="secondary" data-patient-notes="${patient.id}">Заметки</button><button class="danger-outline" data-patient-archive="${patient.id}">${patient.archived ? 'Восстановить' : 'В архив'}</button>` : ''}
        </div></td>
    </tr>`).join('') || '<tr><td colspan="10" class="muted">Пациенты не найдены</td></tr>';
    $('patients-page').textContent = `Страница ${state.patients.page + 1} из ${Math.max(1, state.patients.totalPages)} · записей ${state.patients.totalElements}`;
    $('patients-prev').disabled = state.patients.first;
    $('patients-next').disabled = state.patients.last;
    $('select-all-patients').checked = false;
    applyRoleVisibility();
}

function patientForm(patient = null, readOnly = false) {
    const deptOptions = ['<option value="">Без отделения</option>', ...state.departments.filter(d => d.active || d.id === patient?.departmentId).map(d => `<option value="${d.id}" ${d.id === patient?.departmentId ? 'selected' : ''}>${escapeHtml(d.code)} — ${escapeHtml(d.name || '')}</option>`)].join('');
    return `<div class="dialog-head"><h3>${patient ? (readOnly ? 'Карточка пациента' : 'Изменение пациента') : 'Новый пациент'}</h3><button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button></div>
    <div class="form-grid">
      <label>Фамилия<input name="lastName" required maxlength="100" value="${escapeHtml(patient?.lastName || '')}" ${readOnly ? 'disabled' : ''}></label>
      <label>Имя<input name="firstName" required maxlength="100" value="${escapeHtml(patient?.firstName || '')}" ${readOnly ? 'disabled' : ''}></label>
      <label>Отчество<input name="middleName" maxlength="100" value="${escapeHtml(patient?.middleName || '')}" ${readOnly ? 'disabled' : ''}></label>
      <label>Номер медкарты (ID)<input name="externalId" maxlength="100" value="${escapeHtml(patient?.externalId || '')}" ${readOnly ? 'disabled' : ''}></label>
      <label>Email<input name="email" type="email" maxlength="254" value="${escapeHtml(patient?.email || '')}" ${readOnly ? 'disabled' : ''}></label>
      <label>Телефон<input name="phone" type="tel" inputmode="numeric" autocomplete="tel" maxlength="16" pattern="\\+7-[0-9]{3}-[0-9]{3}-[0-9]{2}-[0-9]{2}" title="Формат: +7-999-999-99-99" placeholder="+7-999-999-99-99" value="${escapeHtml(patient?.phone ? formatRussianPhone(patient.phone) : '')}" ${readOnly ? 'disabled' : ''}></label>
      <label>Дата рождения<input name="birthDate" type="date" value="${patient?.birthDate || ''}" ${readOnly ? 'disabled' : ''}></label>
      <label>Отделение<select name="departmentId" ${readOnly ? 'disabled' : ''}>${deptOptions}</select></label>
      <label>Статус<select name="status" ${readOnly ? 'disabled' : ''}>${statusOptions(patient?.status)}</select></label>
      <label>Шкала реабилитации (0–100)<input name="rehabilitationScore" type="number" min="0" max="100" step="0.01" inputmode="decimal" placeholder="Например, 75.50" value="${patient?.rehabilitationScore ?? ''}" ${readOnly ? 'disabled' : ''}></label>
      <label class="inline"><input name="requiresAssistance" type="checkbox" ${patient?.requiresAssistance ? 'checked' : ''} ${readOnly ? 'disabled' : ''}> Нуждается в постороннем уходе</label>
      <label class="span-2">Теги через запятую (ЛФК, кардио, инфаркт)<input name="tags" maxlength="1000" value="${escapeHtml((patient?.tags || []).join(', '))}" ${readOnly ? 'disabled' : ''}></label>
    </div>
    ${readOnly ? '' : '<div class="form-message error" data-form-error hidden></div><button class="primary" type="submit">Сохранить</button>'}`;
}

async function openPatient(id = null, readOnly = false) {
    const patient = id ? await api(`/patients/${id}`) : null;
    const form = $('entity-form');
    form.innerHTML = patientForm(patient, readOnly);

    const phoneInput = form.elements.phone;
    if (phoneInput && !readOnly) {
        bindPhoneMask(phoneInput);
    }

    form.onsubmit = readOnly ? null : async event => {
        event.preventDefault();
        clearFormError(form);

        if (!form.reportValidity()) return;

        const data = new FormData(form);
        const payload = {
            externalId: empty(data.get('externalId')),
            firstName: data.get('firstName'),
            lastName: data.get('lastName'),
            middleName: empty(data.get('middleName')),
            email: empty(data.get('email')),
            phone: empty(data.get('phone')),
            birthDate: empty(data.get('birthDate')),
            departmentId: empty(data.get('departmentId')),
            status: data.get('status'),
            rehabilitationScore: empty(data.get('rehabilitationScore')) ? Number(data.get('rehabilitationScore')) : null,
            requiresAssistance: data.get('requiresAssistance') === 'on',
            tags: String(data.get('tags') || '').split(',').map(v => v.trim()).filter(Boolean),
            version: patient?.version ?? null
        };
        try {
            await api(id ? `/patients/${id}` : '/patients', {
                method: id ? 'PUT' : 'POST',
                body: JSON.stringify(payload)
            });
            $('entity-dialog').close();
            toast('Карточка пациента сохранена');
            await Promise.all([loadPatients(), loadDashboard()]);
        } catch (error) {
            showFormError(form, error.message);
        }
    };
    $('entity-dialog').showModal();
}

async function archivePatient(id) {
    const patient = state.patients.content.find(item => item.id === id);
    const action = patient?.archived ? 'восстановить' : 'переместить в архив';
    if (!confirm(`Действительно ${action} карту пациента?`)) return;
    try {
        await api(patient.archived ? `/patients/${id}/restore` : `/patients/${id}`, {
            method: patient.archived ? 'POST' : 'DELETE'
        });
        toast('Операция выполнена');
        await Promise.all([loadPatients(), loadDashboard()]);
    } catch (error) {
        toast(error.message, true);
    }
}

async function openNotes(patientId) {
    state.activePatientForNotes = patientId;
    await renderNotes();
    $('notes-dialog').showModal();
}

async function renderNotes() {
    try {
        const list = await api(`/patients/${state.activePatientForNotes}/notes`);
        $('notes-list').innerHTML = list.map(note => `<article class="note">
            <div class="note-meta">
                <span>${escapeHtml(note.authorName)} · ${formatDate(note.createdAt)}</span>
                <span>${note.pinned ? '📌' : ''}</span>
            </div>
            <p>${escapeHtml(note.body).replace(/\n/g, '<br>')}</p>
            <div class="actions">
                <button class="danger-outline" data-note-delete="${note.id}">Удалить</button>
            </div>
        </article>`).join('') || '<p class="muted">Заметок пока нет</p>';
    } catch (error) {
        toast(error.message, true);
    }
}

async function bulkAction() {
    if (!state.selectedPatientIds.size) return toast('Выберите хотя бы одного пациента', true);
    const action = prompt('Действие: ARCHIVE, RESTORE, SET_STATUS, ADD_TAG, REMOVE_TAG', 'ARCHIVE');
    if (!action) return;
    const payload = {
        ids: [...state.selectedPatientIds],
        action: action.toUpperCase(),
        status: null,
        tag: null
    };
    if (payload.action === 'SET_STATUS') {
        payload.status = prompt('Статус: UNDER_OBSERVATION, IN_REHABILITATION, COMPLETED, DISCHARGED', 'IN_REHABILITATION');
    }
    if (payload.action === 'ADD_TAG' || payload.action === 'REMOVE_TAG') {
        payload.tag = prompt('Название тега');
    }
    try {
        const result = await api('/patients/bulk', {method: 'POST', body: JSON.stringify(payload)});
        toast(`Успешно: ${result.succeeded}; ошибок: ${result.failures.length}`);
        await Promise.all([loadPatients(), loadDashboard()]);
    } catch (error) {
        toast(error.message, true);
    }
}

async function exportCsv() {
    try {
        const response = await api(`/patients/export?q=${encodeURIComponent(state.patients.query)}`);
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `patients-${new Date().toISOString().slice(0, 10)}.csv`;
        link.click();
        URL.revokeObjectURL(url);
    } catch (error) {
        toast(error.message, true);
    }
}

function departmentForm(dept = null) {
    return `<div class="dialog-head"><h3>${dept ? 'Изменение' : 'Новое'} отделение</h3><button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button></div>
    <label>Код отделения<input name="code" required maxlength="50" placeholder="НАПРИМЕР: НЕВРО" value="${escapeHtml(dept?.code || '')}"></label>
    <label>Название отделения<input name="name" maxlength="200" placeholder="Отделение неврологии и нейрореабилитации" value="${escapeHtml(dept?.name || '')}"></label>
    <label>Вместимость (коек)<input name="capacity" type="number" min="1" max="1000" value="${dept?.capacity ?? ''}"></label>
    <label class="inline"><input name="active" type="checkbox" ${dept?.active !== false ? 'checked' : ''}> Отделение активно</label>
    <button class="primary" type="submit">Сохранить</button>`;
}

async function openDepartment(id = null) {
    const dept = id ? state.departments.find(d => d.id === id) : null;
    const form = $('entity-form');
    form.innerHTML = departmentForm(dept);
    form.onsubmit = async event => {
        event.preventDefault();
        const data = new FormData(form);
        const payload = {
            code: data.get('code'),
            name: empty(data.get('name')),
            capacity: empty(data.get('capacity')) ? Number(data.get('capacity')) : null,
            active: data.get('active') === 'on',
            version: dept?.version ?? null
        };
        try {
            await api(id ? `/departments/${id}` : '/departments', {
                method: id ? 'PUT' : 'POST',
                body: JSON.stringify(payload)
            });
            $('entity-dialog').close();
            toast('Отделение сохранено');
            await loadDepartments();
        } catch (error) {
            toast(error.message, true);
        }
    };
    $('entity-dialog').showModal();
}

async function toggleDepartment(id) {
    const dept = state.departments.find(d => d.id === id);
    if (!dept) return;
    try {
        await api(`/departments/${id}/active?active=${!dept.active}&version=${dept.version}`, {method: 'PATCH'});
        await loadDepartments();
    } catch (error) {
        toast(error.message, true);
    }
}

async function loadUsers() {
    try {
        const page = await api('/admin/users?page=0&size=100&sort=email,asc');
        state.users = page.content;
        $('users-body').innerHTML = page.content.map(user => `<tr>
            <td>${escapeHtml(user.email)}</td>
            <td>${escapeHtml(user.displayName)}</td>
            <td>${(user.roles || []).map(r => `<span class="badge">${escapeHtml(roleLabel(r))}</span>`).join(' ')}</td>
            <td>${user.enabled ? 'Да' : 'Нет'}</td>
            <td>${user.passwordChangeRequired ? 'Да' : 'Нет'}</td>
            <td><div class="actions">
                <button class="secondary" data-user-edit="${user.id}">Изменить</button>
                <button class="secondary" data-user-reset="${user.id}">Сбросить пароль</button>
            </div></td>
        </tr>`).join('') || '<tr><td colspan="6" class="muted">Пользователи не найдены</td></tr>';
    } catch (error) {
        toast(error.message, true);
    }
}

function userForm(user = null) {
    const checked = role => (user?.roles || []).includes(role) || (!user && role === 'DOCTOR');
    return `<div class="dialog-head"><h3>${user ? 'Изменение' : 'Новый'} пользователь</h3><button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button></div>
    <label>Email<input name="email" type="email" required maxlength="254" value="${escapeHtml(user?.email || '')}"></label>
    <label>Отображаемое имя<input name="displayName" required maxlength="160" value="${escapeHtml(user?.displayName || '')}"></label>
    ${user ? '' : '<label>Временный пароль<input name="temporaryPassword" type="password" required minlength="12" maxlength="128"></label>'}
    <fieldset><legend>Роли</legend>${['VIEWER', 'DOCTOR', 'ADMIN'].map(r => `<label class="inline"><input type="checkbox" name="roles" value="${r}" ${checked(r) ? 'checked' : ''}> ${escapeHtml(roleLabel(r))}</label>`).join('')}</fieldset>
    ${user ? `<label class="inline"><input type="checkbox" name="enabled" ${user.enabled ? 'checked' : ''}> Активен</label>` : ''}
    <button class="primary" type="submit">Сохранить</button>`;
}

async function openUser(id = null) {
    const user = id ? state.users.find(u => u.id === id) : null;
    const form = $('entity-form');
    form.innerHTML = userForm(user);
    form.onsubmit = async event => {
        event.preventDefault();
        const data = new FormData(form);
        const payload = {
            email: data.get('email'),
            displayName: data.get('displayName'),
            roles: data.getAll('roles'),
            ...(user ? {enabled: data.get('enabled') === 'on', version: user.version} : {temporaryPassword: data.get('temporaryPassword')})
        };
        try {
            await api(id ? `/admin/users/${id}` : '/admin/users', {
                method: id ? 'PUT' : 'POST',
                body: JSON.stringify(payload)
            });
            $('entity-dialog').close();
            toast('Пользователь сохранён');
            await loadUsers();
        } catch (error) {
            toast(error.message, true);
        }
    };
    $('entity-dialog').showModal();
}

async function resetUserPassword(id) {
    const password = prompt('Новый временный пароль (не менее 12 символов, буквы в двух регистрах, цифра и спецсимвол)');
    if (!password) return;
    try {
        await api(`/admin/users/${id}/reset-password`, {
            method: 'POST',
            body: JSON.stringify({temporaryPassword: password})
        });
        toast('Пароль сброшен');
        await loadUsers();
    } catch (error) {
        toast(error.message, true);
    }
}

async function loadAudit() {
    try {
        const q = encodeURIComponent(($('audit-search').value || '').trim());
        const page = await api(`/admin/audit?q=${q}&page=0&size=100&sort=occurred,desc`);
        $('audit-body').innerHTML = page.content.map(item => `<tr>
            <td>${formatDate(item.occurredAt)}</td>
            <td>${escapeHtml(item.actorEmail || 'system')}</td>
            <td>${escapeHtml(item.eventType)}</td>
            <td>${escapeHtml(item.entityType || '—')}</td>
            <td>${escapeHtml(item.summary)}</td>
            <td><code>${escapeHtml(item.correlationId || '—')}</code></td>
        </tr>`).join('') || '<tr><td colspan="6" class="muted">Записи аудита не найдены</td></tr>';
    } catch (error) {
        toast(error.message, true);
    }
}

function openPasswordDialog(required = false) {
    return new Promise(resolve => {
        const dialog = $('entity-dialog');
        const form = $('entity-form');

        const currentPasswordField = required
            ? ''
            : '<label>Текущий пароль<input name="current" type="password" required maxlength="128" autocomplete="current-password"></label>';

        form.innerHTML = `<div class="dialog-head"><h3>${required ? 'Установка постоянного пароля' : 'Смена пароля'}</h3>${required ? '' : '<button type="button" data-close-dialog="entity-dialog" class="icon-button">×</button>'}</div>
            ${currentPasswordField}
            <label>Новый пароль<input name="next" type="password" required minlength="12" maxlength="128" autocomplete="new-password"></label>
            <label>Повторите новый пароль<input name="confirm" type="password" required minlength="12" maxlength="128" autocomplete="new-password"></label>
            <p class="muted">Минимум 12 символов: заглавные и строчные буквы, цифра и специальный символ.</p>
            <button class="primary" type="submit">${required ? 'Установить пароль' : 'Изменить'}</button>`;

        let completed = false;

        const onClose = () => {
            dialog.removeEventListener('close', onClose);
            if (!completed) resolve(false);
        };
        dialog.addEventListener('close', onClose);

        form.onsubmit = async event => {
            event.preventDefault();

            const data = new FormData(form);
            const next = String(data.get('next') || '');
            const confirm = String(data.get('confirm') || '');

            if (next !== confirm) {
                toast('Новые пароли не совпадают', true);
                return;
            }

            try {
                if (required) {
                    await api('/auth/change-initial-password', {
                        method: 'POST',
                        body: JSON.stringify({newPassword: next})
                    });

                    const refreshed = await refreshAccessToken();
                    if (!refreshed) {
                        completed = true;
                        dialog.close();
                        showLogin();
                        toast('Пароль изменён, но сессию не удалось обновить. Войдите новым паролем.', true);
                        resolve(false);
                        return;
                    }

                    state.user.passwordChangeRequired = false;
                    completed = true;
                    dialog.close();
                    toast('Постоянный пароль установлен');
                    resolve(true);
                    return;
                }

                await api('/auth/change-password', {
                    method: 'POST',
                    body: JSON.stringify({
                        currentPassword: data.get('current'),
                        newPassword: next
                    })
                });

                completed = true;
                dialog.close();
                toast('Пароль изменён. Выполните вход заново.');
                resolve(true);
                await logout();
            } catch (error) {
                toast(error.message, true);
            }
        };

        dialog.showModal();
    });
}

async function logout() {
    try {
        await fetch('/api/auth/logout', {
            method: 'POST',
            credentials: 'include',
            headers: {'Content-Type': 'application/json'},
            body: '{}'
        });
    } catch (_) {}
    showLogin();
}

function bindEvents() {
    $('login-form').addEventListener('submit', async event => {
        event.preventDefault();

        const form = $('login-form');
        const button = form.querySelector('button[type="submit"]');
        const errorNode = $('login-error');

        errorNode.hidden = true;
        button.disabled = true;
        const originalCaption = button.textContent;
        button.textContent = 'Входим…';

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                credentials: 'include',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    email: $('login-email').value,
                    password: $('login-password').value,
                    clientType: 'WEB'
                })
            });

            if (!response.ok) {
                throw new Error(await parseError(response));
            }

            const result = await response.json();
            state.accessToken = result.accessToken;
            state.user = result.user;
            $('login-password').value = '';

            await enterApplication();
        } catch (error) {
            console.error('[Rehab Center] Login flow failed', error);
            showLogin();
            errorNode.textContent = error.message || 'Не удалось выполнить вход';
            errorNode.hidden = false;
        } finally {
            button.disabled = false;
            button.textContent = originalCaption;
        }
    });

    $('navigation').addEventListener('click', event => {
        const button = event.target.closest('[data-tab]');
        if (button && !button.hidden) switchTab(button.dataset.tab);
    });

    $('logout').addEventListener('click', logout);
    $('change-password').addEventListener('click', () => openPasswordDialog(false));

    $('search-patients').addEventListener('click', () => {
        state.patients.query = $('patient-search').value.trim();
        state.patients.page = 0;
        loadPatients(0);
    });
    $('patient-search').addEventListener('keydown', event => {
        if (event.key === 'Enter') $('search-patients').click();
    });

    $('patients-prev').addEventListener('click', () => loadPatients(state.patients.page - 1));
    $('patients-next').addEventListener('click', () => loadPatients(state.patients.page + 1));
    $('add-patient').addEventListener('click', () => openPatient());
    $('bulk-action').addEventListener('click', bulkAction);
    $('export-csv').addEventListener('click', exportCsv);
    $('import-csv').addEventListener('click', () => {
        $('csv-result').textContent = '';
        $('csv-apply').disabled = true;
        $('csv-dialog').showModal();
    });

    $('add-department').addEventListener('click', () => openDepartment());
    $('add-user').addEventListener('click', () => openUser());
    $('search-audit').addEventListener('click', loadAudit);

    $('patients-body').addEventListener('click', handlePatientTableClick);
    $('patients-body').addEventListener('change', event => {
        const checkbox = event.target.closest('[data-patient-select]');
        if (checkbox) {
            if (checkbox.checked) {
                state.selectedPatientIds.add(checkbox.dataset.patientSelect);
            } else {
                state.selectedPatientIds.delete(checkbox.dataset.patientSelect);
            }
        }
    });

    $('select-all-patients').addEventListener('change', event => {
        document.querySelectorAll('[data-patient-select]').forEach(cb => {
            cb.checked = event.target.checked;
            if (event.target.checked) {
                state.selectedPatientIds.add(cb.dataset.patientSelect);
            } else {
                state.selectedPatientIds.delete(cb.dataset.patientSelect);
            }
        });
    });

    $('departments-body').addEventListener('click', event => {
        const edit = event.target.closest('[data-dept-edit]');
        const toggle = event.target.closest('[data-dept-toggle]');
        if (edit) openDepartment(edit.dataset.deptEdit);
        if (toggle) toggleDepartment(toggle.dataset.deptToggle);
    });

    $('users-body').addEventListener('click', event => {
        const edit = event.target.closest('[data-user-edit]');
        const reset = event.target.closest('[data-user-reset]');
        if (edit) openUser(edit.dataset.userEdit);
        if (reset) resetUserPassword(reset.dataset.userReset);
    });

    $('note-form').addEventListener('submit', async event => {
        event.preventDefault();
        try {
            await api(`/patients/${state.activePatientForNotes}/notes`, {
                method: 'POST',
                body: JSON.stringify({
                    body: $('note-body').value,
                    pinned: $('note-pinned').checked,
                    version: null
                })
            });
            $('note-body').value = '';
            $('note-pinned').checked = false;
            await renderNotes();
        } catch (error) {
            toast(error.message, true);
        }
    });

    $('notes-list').addEventListener('click', async event => {
        const button = event.target.closest('[data-note-delete]');
        if (button && confirm('Удалить медицинскую заметку?')) {
            try {
                await api(`/patients/${state.activePatientForNotes}/notes/${button.dataset.noteDelete}`, {
                    method: 'DELETE'
                });
                await renderNotes();
            } catch (error) {
                toast(error.message, true);
            }
        }
    });

    $('csv-preview').addEventListener('click', () => runCsv(false));
    $('csv-apply').addEventListener('click', () => runCsv(true));

    document.body.addEventListener('click', event => {
        const close = event.target.closest('[data-close-dialog]');
        if (close) $(close.dataset.closeDialog).close();
    });
}

function handlePatientTableClick(event) {
    const view = event.target.closest('[data-patient-view]');
    const edit = event.target.closest('[data-patient-edit]');
    const notes = event.target.closest('[data-patient-notes]');
    const archive = event.target.closest('[data-patient-archive]');
    if (view) openPatient(view.dataset.patientView, true);
    if (edit) openPatient(edit.dataset.patientEdit, false);
    if (notes) openNotes(notes.dataset.patientNotes);
    if (archive) archivePatient(archive.dataset.patientArchive);
}

async function runCsv(apply) {
    const file = $('csv-file').files[0];
    if (!file) return toast('Выберите CSV-файл', true);
    const form = new FormData();
    form.append('file', file);
    try {
        const result = await api(`/patients/import/${apply ? 'apply' : 'preview'}?mode=${$('csv-mode').value}`, {
            method: 'POST',
            body: form
        });
        $('csv-result').textContent = JSON.stringify(result, null, 2);
        $('csv-apply').disabled = apply ? true : result.invalidRows > 0;
        if (apply) {
            toast('Импорт завершён');
            await Promise.all([loadPatients(), loadDashboard()]);
        }
    } catch (error) {
        toast(error.message, true);
        $('csv-result').textContent = error.message;
    }
}

function statusOptions(selected = 'IN_REHABILITATION') {
    return ['UNDER_OBSERVATION', 'IN_REHABILITATION', 'COMPLETED', 'DISCHARGED'].map(v =>
        `<option value="${v}" ${v === selected ? 'selected' : ''}>${statusLabel(v)}</option>`
    ).join('');
}

function statusLabel(value) {
    return ({
        UNDER_OBSERVATION: 'На наблюдении',
        IN_REHABILITATION: 'В процессе реабилитации',
        COMPLETED: 'Курс завершен',
        DISCHARGED: 'Выписан'
    })[value] || value;
}

function riskLabel(value) {
    return ({
        NOT_APPLICABLE: 'Не оценивается',
        LOW: 'Низкий',
        MEDIUM: 'Средний',
        HIGH: 'Высокий',
        CRITICAL: 'Критический'
    })[value] || value;
}

function formatDate(value) {
    return value ? new Intl.DateTimeFormat('ru-RU', {dateStyle: 'short', timeStyle: 'short'}).format(new Date(value)) : '—';
}

function empty(value) {
    const normalized = String(value ?? '').trim();
    return normalized || null;
}

boot().catch(error => {
    console.error('[Rehab Center] Boot failed', error);
    showLogin();
    toast(error.message || 'Не удалось инициализировать приложение', true);
});

