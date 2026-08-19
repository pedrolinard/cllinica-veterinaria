'use strict';

/* ======================= State & storage ======================= */

const Store = {
  getToken: () => localStorage.getItem('vc_token'),
  getUser: () => {
    const raw = localStorage.getItem('vc_user');
    return raw ? JSON.parse(raw) : null;
  },
  setSession: (token, user) => {
    localStorage.setItem('vc_token', token);
    localStorage.setItem('vc_user', JSON.stringify(user));
  },
  clear: () => {
    localStorage.removeItem('vc_token');
    localStorage.removeItem('vc_user');
  },
};

/* ======================= API client ======================= */

class ApiError extends Error {
  constructor(data, status) {
    super((data && data.message) || 'Erro inesperado.');
    this.data = data;
    this.status = status;
  }
}

async function api(path, { method = 'GET', body, params } = {}) {
  let url = path;
  if (params) {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') qs.set(k, v);
    });
    const qsStr = qs.toString();
    if (qsStr) url += (url.includes('?') ? '&' : '?') + qsStr;
  }

  const headers = { 'Content-Type': 'application/json' };
  const token = Store.getToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;

  const res = await fetch(url, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 204) return null;

  let data = null;
  try {
    data = await res.json();
  } catch (e) {
    /* no body */
  }

  if (!res.ok) {
    if (res.status === 401) {
      Store.clear();
      renderRoot();
    }
    throw new ApiError(data, res.status);
  }

  return data;
}

/* ======================= Toasts ======================= */

function toast(message, type = 'success') {
  const container = document.getElementById('toast-container');
  const div = document.createElement('div');
  div.className = 'toast toast-' + type;
  div.textContent = message;
  container.appendChild(div);
  setTimeout(() => div.remove(), 4000);
}

function apiErrorMessage(err) {
  if (!(err instanceof ApiError)) return 'Erro inesperado.';
  let msg = err.message;
  if (err.data && err.data.fieldErrors && err.data.fieldErrors.length) {
    msg += ' ' + err.data.fieldErrors.map((f) => f.message).join(' ');
  }
  return msg;
}

/* ======================= Permissions ======================= */

const ROLES = {
  clientsWrite: ['ADMIN', 'RECEPTIONIST'],
  petsWrite: ['ADMIN', 'RECEPTIONIST'],
  appointmentsWrite: ['ADMIN', 'RECEPTIONIST'],
  appointmentsStatus: ['ADMIN', 'RECEPTIONIST', 'VET'],
  medicalRecordsWrite: ['ADMIN', 'VET'],
  medicalRecordsDelete: ['ADMIN'],
  servicesWrite: ['ADMIN'],
  usersWrite: ['ADMIN'],
};

function hasRole(list) {
  const user = Store.getUser();
  return !!user && list.includes(user.role);
}

/* ======================= Modal ======================= */

function openModal(title, bodyHtml) {
  closeModal();
  const backdrop = document.createElement('div');
  backdrop.className = 'modal-backdrop';
  backdrop.id = 'modal-backdrop';
  backdrop.innerHTML = `
    <div class="modal">
      <h3>${title}</h3>
      ${bodyHtml}
    </div>
  `;
  backdrop.addEventListener('click', (e) => {
    if (e.target === backdrop) closeModal();
  });
  document.body.appendChild(backdrop);
  return backdrop.querySelector('.modal');
}

function closeModal() {
  const existing = document.getElementById('modal-backdrop');
  if (existing) existing.remove();
}

function confirmDialog(message, onConfirm) {
  const modal = openModal('Confirmar ação', `
    <p class="text-dim">${message}</p>
    <div class="modal-actions">
      <button class="btn btn-secondary" id="confirm-cancel">Cancelar</button>
      <button class="btn btn-danger" id="confirm-ok">Confirmar</button>
    </div>
  `);
  modal.querySelector('#confirm-cancel').onclick = closeModal;
  modal.querySelector('#confirm-ok').onclick = async () => {
    closeModal();
    await onConfirm();
  };
}

function formError(container, err) {
  container.innerHTML = `<div class="error-box">${apiErrorMessage(err)}</div>`;
}

/* ======================= Formatting helpers ======================= */

function fmtDate(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleDateString('pt-BR') + ' ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

function fmtDateOnly(isoDate) {
  if (!isoDate) return '—';
  const [y, m, d] = isoDate.split('-');
  return `${d}/${m}/${y}`;
}

function fmtMoney(cents) {
  if (cents === null || cents === undefined) return '—';
  return (cents / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function toDatetimeLocalValue(iso) {
  if (!iso) return '';
  return iso.slice(0, 16);
}

function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

/* ======================= Donut chart =======================
 * Fixed-order categorical palette (never reassigned per-value), validated
 * for CVD separation and contrast against the app's dark card surface.
 */
const CHART_COLORS = ['var(--series-1)', 'var(--series-2)', 'var(--series-3)', 'var(--series-4)', 'var(--series-5)', 'var(--series-6)'];

function renderDonutCard(title, segments, { centerLabel = 'total' } = {}) {
  const total = segments.reduce((sum, s) => sum + s.value, 0);
  const card = document.createElement('div');
  card.className = 'chart-card';

  if (total === 0) {
    card.innerHTML = `<h3>${escapeHtml(title)}</h3><div class="empty-state">Sem dados suficientes ainda.</div>`;
    return card;
  }

  const size = 160;
  const strokeWidth = 26;
  const r = (size - strokeWidth) / 2;
  const cx = size / 2;
  const cy = size / 2;
  const circumference = 2 * Math.PI * r;
  const gap = segments.length > 1 ? 3 : 0;

  let cursor = 0;
  const arcs = segments.map((seg, i) => {
    const frac = seg.value / total;
    const rawLen = frac * circumference;
    const len = Math.max(rawLen - gap, 0);
    const dashoffset = -cursor;
    cursor += rawLen;
    return { ...seg, color: CHART_COLORS[i % CHART_COLORS.length], len, dashoffset, pct: frac * 100 };
  });

  const svg = `
    <svg viewBox="0 0 ${size} ${size}" width="${size}" height="${size}" role="img" aria-label="${escapeHtml(title)}">
      <g transform="rotate(-90 ${cx} ${cy})">
        ${arcs.map((a) => `
          <circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="${a.color}"
            stroke-width="${strokeWidth}" stroke-linecap="round"
            stroke-dasharray="${a.len} ${circumference - a.len}" stroke-dashoffset="${a.dashoffset}">
            <title>${escapeHtml(a.label)}: ${a.value} (${a.pct.toFixed(0)}%)</title>
          </circle>
        `).join('')}
      </g>
      <text x="${cx}" y="${cy - 4}" text-anchor="middle" class="donut-total-value">${total}</text>
      <text x="${cx}" y="${cy + 14}" text-anchor="middle" class="donut-total-label">${escapeHtml(centerLabel)}</text>
    </svg>
  `;

  const legend = `
    <ul class="legend">
      ${arcs.map((a) => `
        <li class="legend-item">
          <span class="legend-swatch" style="background:${a.color}"></span>
          <span class="legend-label">${escapeHtml(a.label)}</span>
          <span class="legend-value">${a.value} · ${a.pct.toFixed(0)}%</span>
        </li>
      `).join('')}
    </ul>
  `;

  card.innerHTML = `<h3>${escapeHtml(title)}</h3><div class="donut-row">${svg}${legend}</div>`;
  return card;
}

/* ======================= Shared data fetchers (for selects) ======================= */

async function fetchAllClients() {
  const page = await api('/api/clients', { params: { size: 200 } });
  return page.content;
}

async function fetchAllPets(clientId) {
  const page = await api('/api/pets', { params: { size: 200, clientId } });
  return page.content;
}

async function fetchVets() {
  return api('/api/users/vets');
}

/* ======================= Pagination component ======================= */

function paginationControls(pageData, onChange) {
  const div = document.createElement('div');
  div.className = 'pagination';
  const info = document.createElement('span');
  info.textContent = `Página ${pageData.number + 1} de ${Math.max(pageData.totalPages, 1)} · ${pageData.totalElements} registro(s)`;
  const prev = document.createElement('button');
  prev.className = 'btn btn-secondary btn-sm';
  prev.textContent = '← Anterior';
  prev.disabled = pageData.first;
  prev.onclick = () => onChange(pageData.number - 1);
  const next = document.createElement('button');
  next.className = 'btn btn-secondary btn-sm';
  next.textContent = 'Próxima →';
  next.disabled = pageData.last;
  next.onclick = () => onChange(pageData.number + 1);
  div.appendChild(prev);
  div.appendChild(info);
  div.appendChild(next);
  return div;
}

/* ======================= Navigation ======================= */

const NAV_ITEMS = [
  { hash: '#/dashboard', label: 'Dashboard', icon: '⌂', roles: null },
  { hash: '#/clients', label: 'Clientes', icon: '◐', roles: null },
  { hash: '#/pets', label: 'Pets', icon: '◆', roles: null },
  { hash: '#/appointments', label: 'Consultas', icon: '▤', roles: null },
  { hash: '#/medical-records', label: 'Prontuários', icon: '✚', roles: null },
  { hash: '#/services', label: 'Serviços', icon: '☰', roles: null },
  { hash: '#/users', label: 'Funcionários', icon: '☺', roles: ['ADMIN'] },
];

function renderNav() {
  const user = Store.getUser();
  const navList = document.getElementById('nav-list');
  navList.innerHTML = '';
  NAV_ITEMS.forEach((item) => {
    if (item.roles && !item.roles.includes(user.role)) return;
    const div = document.createElement('div');
    div.className = 'nav-item' + (location.hash === item.hash || (!location.hash && item.hash === '#/dashboard') ? ' active' : '');
    div.innerHTML = `<span>${item.icon}</span><span>${item.label}</span>`;
    div.onclick = () => { location.hash = item.hash; };
    navList.appendChild(div);
  });

  document.getElementById('current-user-name').textContent = user.name;
  const roleEl = document.getElementById('current-user-role');
  roleEl.textContent = user.role;
  roleEl.className = 'role role-' + user.role;
}

/* ======================= Router ======================= */

const ROUTES = {
  '#/dashboard': viewDashboard,
  '#/clients': viewClients,
  '#/pets': viewPets,
  '#/appointments': viewAppointments,
  '#/medical-records': viewMedicalRecords,
  '#/services': viewServices,
  '#/users': viewUsers,
};

async function router() {
  if (!Store.getToken()) {
    renderRoot();
    return;
  }
  renderNav();
  const hash = location.hash || '#/dashboard';
  const view = ROUTES[hash];
  const main = document.getElementById('main-content');
  if (!view) {
    main.innerHTML = '<div class="empty-state">Página não encontrada.</div>';
    return;
  }
  const navItem = NAV_ITEMS.find((i) => i.hash === hash);
  if (navItem && navItem.roles && !hasRole(navItem.roles)) {
    main.innerHTML = '<div class="empty-state">Você não tem permissão para acessar esta página.</div>';
    return;
  }
  try {
    await view(main);
  } catch (err) {
    main.innerHTML = '';
    formError(main, err);
  }
}

window.addEventListener('hashchange', router);

/* ======================= View: Dashboard ======================= */

async function viewDashboard(main) {
  const user = Store.getUser();
  main.innerHTML = `
    <div class="view-header">
      <div>
        <h2>Olá, ${escapeHtml(user.name.split(' ')[0])} 👋</h2>
        <p>Visão geral da clínica.</p>
      </div>
    </div>
    <div class="stat-grid" id="stat-grid">
      <div class="stat-card"><div class="label">Clientes</div><div class="value">…</div></div>
      <div class="stat-card"><div class="label">Pets</div><div class="value">…</div></div>
      <div class="stat-card"><div class="label">Consultas</div><div class="value">…</div></div>
      <div class="stat-card"><div class="label">Serviços</div><div class="value">…</div></div>
    </div>
  `;

  const [clients, pets, appointments, services, allAppointments, allPets] = await Promise.all([
    api('/api/clients', { params: { size: 1 } }),
    api('/api/pets', { params: { size: 1 } }),
    api('/api/appointments', { params: { size: 1 } }),
    api('/api/services'),
    api('/api/appointments', { params: { size: 500 } }),
    api('/api/pets', { params: { size: 500 } }),
  ]);

  const grid = document.getElementById('stat-grid');
  grid.innerHTML = `
    <div class="stat-card"><div class="label">Clientes</div><div class="value">${clients.totalElements}</div></div>
    <div class="stat-card"><div class="label">Pets</div><div class="value">${pets.totalElements}</div></div>
    <div class="stat-card"><div class="label">Consultas</div><div class="value">${appointments.totalElements}</div></div>
    <div class="stat-card"><div class="label">Serviços</div><div class="value">${services.length}</div></div>
  `;

  const statusCounts = {};
  allAppointments.content.forEach((a) => { statusCounts[a.status] = (statusCounts[a.status] || 0) + 1; });
  const statusSegments = Object.keys(STATUS_LABELS)
    .filter((s) => statusCounts[s])
    .map((s) => ({ label: STATUS_LABELS[s], value: statusCounts[s] }));

  const speciesCounts = {};
  allPets.content.forEach((p) => {
    const key = (p.species || 'Não informado').trim();
    speciesCounts[key] = (speciesCounts[key] || 0) + 1;
  });
  const sortedSpecies = Object.entries(speciesCounts).sort((a, b) => b[1] - a[1]);
  const topSpecies = sortedSpecies.slice(0, 5).map(([label, value]) => ({ label, value }));
  const otherCount = sortedSpecies.slice(5).reduce((sum, [, v]) => sum + v, 0);
  if (otherCount > 0) topSpecies.push({ label: 'Outros', value: otherCount });

  const chartGrid = document.createElement('div');
  chartGrid.className = 'chart-grid';
  chartGrid.appendChild(renderDonutCard('Consultas por status', statusSegments, { centerLabel: 'consultas' }));
  chartGrid.appendChild(renderDonutCard('Pets por espécie', topSpecies, { centerLabel: 'pets' }));
  main.appendChild(chartGrid);
}

/* ======================= View: Clients ======================= */

const clientsState = { page: 0 };

async function viewClients(main) {
  const canWrite = hasRole(ROLES.clientsWrite);
  main.innerHTML = `
    <div class="view-header">
      <div><h2>Clientes</h2><p>Tutores cadastrados na clínica.</p></div>
      ${canWrite ? '<button class="btn btn-primary" id="new-client-btn">+ Novo cliente</button>' : ''}
    </div>
    <div id="clients-table"></div>
  `;
  if (canWrite) document.getElementById('new-client-btn').onclick = () => openClientForm();
  await loadClients();
}

async function loadClients() {
  const container = document.getElementById('clients-table');
  const page = await api('/api/clients', { params: { page: clientsState.page } });
  const canWrite = hasRole(ROLES.clientsWrite);

  if (page.content.length === 0) {
    container.innerHTML = '<div class="empty-state">Nenhum cliente cadastrado.</div>';
    return;
  }

  container.innerHTML = `
    <table>
      <thead><tr><th>Nome</th><th>Email</th><th>Telefone</th><th>Endereço</th><th>Pets</th>${canWrite ? '<th>Ações</th>' : ''}</tr></thead>
      <tbody>
        ${page.content.map((c) => `
          <tr data-id="${c.id}">
            <td>${escapeHtml(c.name)}</td>
            <td>${escapeHtml(c.email || '—')}</td>
            <td>${escapeHtml(c.phone)}</td>
            <td>${escapeHtml(c.address || '—')}</td>
            <td>${c.petsCount}</td>
            ${canWrite ? `<td class="row-actions">
              <button class="btn btn-secondary btn-sm" data-action="edit">Editar</button>
              <button class="btn btn-danger btn-sm" data-action="delete">Excluir</button>
            </td>` : ''}
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;

  if (canWrite) {
    container.querySelectorAll('tr[data-id]').forEach((row) => {
      const id = row.dataset.id;
      const client = page.content.find((c) => c.id === id);
      row.querySelector('[data-action="edit"]')?.addEventListener('click', () => openClientForm(client));
      row.querySelector('[data-action="delete"]')?.addEventListener('click', () => {
        confirmDialog(`Excluir o cliente <strong>${escapeHtml(client.name)}</strong>? Os pets vinculados também serão removidos.`, async () => {
          try {
            await api(`/api/clients/${id}`, { method: 'DELETE' });
            toast('Cliente removido.');
            await loadClients();
          } catch (err) {
            toast(apiErrorMessage(err), 'error');
          }
        });
      });
    });
  }

  container.appendChild(paginationControls(page, (newPage) => {
    clientsState.page = newPage;
    loadClients();
  }));
}

function openClientForm(client) {
  const isEdit = !!client;
  const modal = openModal(isEdit ? 'Editar cliente' : 'Novo cliente', `
    <div id="client-form-error"></div>
    <form id="client-form">
      <div class="field">
        <label>Nome</label>
        <input name="name" required value="${escapeHtml(client?.name || '')}" />
      </div>
      <div class="field">
        <label>Email</label>
        <input name="email" type="email" value="${escapeHtml(client?.email || '')}" />
      </div>
      <div class="field">
        <label>Telefone</label>
        <input name="phone" required value="${escapeHtml(client?.phone || '')}" />
      </div>
      <div class="field">
        <label>Endereço</label>
        <input name="address" value="${escapeHtml(client?.address || '')}" />
      </div>
      <div class="modal-actions">
        <button type="button" class="btn btn-secondary" id="client-cancel">Cancelar</button>
        <button type="submit" class="btn btn-primary" id="client-submit">${isEdit ? 'Salvar' : 'Cadastrar'}</button>
      </div>
    </form>
  `);

  modal.querySelector('#client-cancel').onclick = closeModal;
  modal.querySelector('#client-form').onsubmit = async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const body = {
      name: fd.get('name').trim(),
      email: fd.get('email').trim() || null,
      phone: fd.get('phone').trim(),
      address: fd.get('address').trim() || null,
    };
    const btn = modal.querySelector('#client-submit');
    btn.disabled = true;
    try {
      if (isEdit) {
        await api(`/api/clients/${client.id}`, { method: 'PUT', body });
        toast('Cliente atualizado.');
      } else {
        await api('/api/clients', { method: 'POST', body });
        toast('Cliente cadastrado.');
      }
      closeModal();
      await loadClients();
    } catch (err) {
      formError(modal.querySelector('#client-form-error'), err);
      btn.disabled = false;
    }
  };
}

/* ======================= View: Pets ======================= */

const petsState = { page: 0, clientId: '' };

async function viewPets(main) {
  const canWrite = hasRole(ROLES.petsWrite);
  const clients = await fetchAllClients();

  main.innerHTML = `
    <div class="view-header">
      <div><h2>Pets</h2><p>Pets cadastrados e seus tutores.</p></div>
      ${canWrite ? '<button class="btn btn-primary" id="new-pet-btn">+ Novo pet</button>' : ''}
    </div>
    <div class="toolbar">
      <select id="pets-client-filter">
        <option value="">Todos os tutores</option>
        ${clients.map((c) => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('')}
      </select>
    </div>
    <div id="pets-table"></div>
  `;

  document.getElementById('pets-client-filter').onchange = (e) => {
    petsState.clientId = e.target.value;
    petsState.page = 0;
    loadPets(clients);
  };

  if (canWrite) document.getElementById('new-pet-btn').onclick = () => openPetForm(clients);
  await loadPets(clients);
}

async function loadPets(clients) {
  const container = document.getElementById('pets-table');
  const page = await api('/api/pets', { params: { page: petsState.page, clientId: petsState.clientId || undefined } });
  const canWrite = hasRole(ROLES.petsWrite);

  if (page.content.length === 0) {
    container.innerHTML = '<div class="empty-state">Nenhum pet encontrado.</div>';
    return;
  }

  container.innerHTML = `
    <table>
      <thead><tr><th>Nome</th><th>Espécie</th><th>Raça</th><th>Nascimento</th><th>Peso (kg)</th><th>Tutor</th>${canWrite ? '<th>Ações</th>' : ''}</tr></thead>
      <tbody>
        ${page.content.map((p) => `
          <tr data-id="${p.id}">
            <td>${escapeHtml(p.name)}</td>
            <td>${escapeHtml(p.species)}</td>
            <td>${escapeHtml(p.breed || '—')}</td>
            <td>${fmtDateOnly(p.birthDate)}</td>
            <td>${p.weightKg ?? '—'}</td>
            <td>${escapeHtml(p.clientName)}</td>
            ${canWrite ? `<td class="row-actions">
              <button class="btn btn-secondary btn-sm" data-action="edit">Editar</button>
              <button class="btn btn-danger btn-sm" data-action="delete">Excluir</button>
            </td>` : ''}
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;

  if (canWrite) {
    container.querySelectorAll('tr[data-id]').forEach((row) => {
      const id = row.dataset.id;
      const pet = page.content.find((p) => p.id === id);
      row.querySelector('[data-action="edit"]')?.addEventListener('click', () => openPetForm(clients, pet));
      row.querySelector('[data-action="delete"]')?.addEventListener('click', () => {
        confirmDialog(`Excluir o pet <strong>${escapeHtml(pet.name)}</strong>?`, async () => {
          try {
            await api(`/api/pets/${id}`, { method: 'DELETE' });
            toast('Pet removido.');
            await loadPets(clients);
          } catch (err) {
            toast(apiErrorMessage(err), 'error');
          }
        });
      });
    });
  }

  container.appendChild(paginationControls(page, (newPage) => {
    petsState.page = newPage;
    loadPets(clients);
  }));
}

function openPetForm(clients, pet) {
  const isEdit = !!pet;
  const modal = openModal(isEdit ? 'Editar pet' : 'Novo pet', `
    <div id="pet-form-error"></div>
    <form id="pet-form">
      <div class="form-row">
        <div class="field">
          <label>Nome</label>
          <input name="name" required value="${escapeHtml(pet?.name || '')}" />
        </div>
        <div class="field">
          <label>Espécie</label>
          <input name="species" required value="${escapeHtml(pet?.species || '')}" />
        </div>
      </div>
      <div class="form-row">
        <div class="field">
          <label>Raça</label>
          <input name="breed" value="${escapeHtml(pet?.breed || '')}" />
        </div>
        <div class="field">
          <label>Peso (kg)</label>
          <input name="weightKg" type="number" step="0.1" min="0" value="${pet?.weightKg ?? ''}" />
        </div>
      </div>
      <div class="form-row">
        <div class="field">
          <label>Data de nascimento</label>
          <input name="birthDate" type="date" value="${pet?.birthDate || ''}" />
        </div>
        <div class="field">
          <label>Tutor</label>
          <select name="clientId" required>
            <option value="">Selecione…</option>
            ${clients.map((c) => `<option value="${c.id}" ${pet?.clientId === c.id ? 'selected' : ''}>${escapeHtml(c.name)}</option>`).join('')}
          </select>
        </div>
      </div>
      <div class="field">
        <label>Observações</label>
        <textarea name="notes">${escapeHtml(pet?.notes || '')}</textarea>
      </div>
      <div class="modal-actions">
        <button type="button" class="btn btn-secondary" id="pet-cancel">Cancelar</button>
        <button type="submit" class="btn btn-primary" id="pet-submit">${isEdit ? 'Salvar' : 'Cadastrar'}</button>
      </div>
    </form>
  `);

  modal.querySelector('#pet-cancel').onclick = closeModal;
  modal.querySelector('#pet-form').onsubmit = async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const body = {
      name: fd.get('name').trim(),
      species: fd.get('species').trim(),
      breed: fd.get('breed').trim() || null,
      birthDate: fd.get('birthDate') || null,
      weightKg: fd.get('weightKg') ? Number(fd.get('weightKg')) : null,
      notes: fd.get('notes').trim() || null,
      clientId: fd.get('clientId'),
    };
    const btn = modal.querySelector('#pet-submit');
    btn.disabled = true;
    try {
      if (isEdit) {
        await api(`/api/pets/${pet.id}`, { method: 'PUT', body });
        toast('Pet atualizado.');
      } else {
        await api('/api/pets', { method: 'POST', body });
        toast('Pet cadastrado.');
      }
      closeModal();
      await loadPets(clients);
    } catch (err) {
      formError(modal.querySelector('#pet-form-error'), err);
      btn.disabled = false;
    }
  };
}

/* ======================= View: Appointments ======================= */

const apptState = { page: 0, vetId: '', petId: '' };

const STATUS_LABELS = {
  SCHEDULED: 'Agendada',
  CONFIRMED: 'Confirmada',
  COMPLETED: 'Concluída',
  CANCELED: 'Cancelada',
};

async function viewAppointments(main) {
  const canWrite = hasRole(ROLES.appointmentsWrite);
  const canStatus = hasRole(ROLES.appointmentsStatus);
  const [pets, vets] = await Promise.all([fetchAllPets(), fetchVets()]);

  main.innerHTML = `
    <div class="view-header">
      <div><h2>Consultas</h2><p>Agenda de consultas da clínica.</p></div>
      ${canWrite ? '<button class="btn btn-primary" id="new-appt-btn">+ Nova consulta</button>' : ''}
    </div>
    <div class="toolbar">
      <select id="appt-pet-filter">
        <option value="">Todos os pets</option>
        ${pets.map((p) => `<option value="${p.id}">${escapeHtml(p.name)} (${escapeHtml(p.clientName)})</option>`).join('')}
      </select>
      <select id="appt-vet-filter">
        <option value="">Todos os veterinários</option>
        ${vets.map((v) => `<option value="${v.id}">${escapeHtml(v.name)}</option>`).join('')}
      </select>
    </div>
    <div id="appt-table"></div>
  `;

  document.getElementById('appt-pet-filter').onchange = (e) => { apptState.petId = e.target.value; apptState.page = 0; loadAppointments(pets, vets); };
  document.getElementById('appt-vet-filter').onchange = (e) => { apptState.vetId = e.target.value; apptState.page = 0; loadAppointments(pets, vets); };

  if (canWrite) document.getElementById('new-appt-btn').onclick = () => openAppointmentForm(pets, vets);
  await loadAppointments(pets, vets);
}

async function loadAppointments(pets, vets) {
  const container = document.getElementById('appt-table');
  const page = await api('/api/appointments', { params: { page: apptState.page, vetId: apptState.vetId || undefined, petId: apptState.petId || undefined } });
  const canWrite = hasRole(ROLES.appointmentsWrite);
  const canStatus = hasRole(ROLES.appointmentsStatus);
  const showActions = canWrite || canStatus;

  if (page.content.length === 0) {
    container.innerHTML = '<div class="empty-state">Nenhuma consulta encontrada.</div>';
    return;
  }

  container.innerHTML = `
    <table>
      <thead><tr><th>Data/Hora</th><th>Duração</th><th>Status</th><th>Pet</th><th>Veterinário</th><th>Motivo</th>${showActions ? '<th>Ações</th>' : ''}</tr></thead>
      <tbody>
        ${page.content.map((a) => `
          <tr data-id="${a.id}">
            <td>${fmtDate(a.scheduledAt)}</td>
            <td>${a.durationMin ?? '—'} min</td>
            <td><span class="status-pill status-${a.status}">${STATUS_LABELS[a.status]}</span></td>
            <td>${escapeHtml(a.petName)}</td>
            <td>${escapeHtml(a.vetName)}</td>
            <td>${escapeHtml(a.reason || '—')}</td>
            ${showActions ? `<td class="row-actions">
              ${statusButtons(a)}
              ${canWrite ? `<button class="btn btn-secondary btn-sm" data-action="edit">Editar</button>
              <button class="btn btn-danger btn-sm" data-action="delete">Excluir</button>` : ''}
            </td>` : ''}
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;

  function statusButtons(a) {
    if (!canStatus) return '';
    if (a.status === 'SCHEDULED') {
      return `<button class="btn btn-secondary btn-sm" data-action="confirm">Confirmar</button>
              <button class="btn btn-danger btn-sm" data-action="cancel">Cancelar</button>`;
    }
    if (a.status === 'CONFIRMED') {
      return `<button class="btn btn-secondary btn-sm" data-action="complete">Concluir</button>
              <button class="btn btn-danger btn-sm" data-action="cancel">Cancelar</button>`;
    }
    return '';
  }

  container.querySelectorAll('tr[data-id]').forEach((row) => {
    const id = row.dataset.id;
    const appt = page.content.find((a) => a.id === id);

    row.querySelector('[data-action="confirm"]')?.addEventListener('click', () => updateApptStatus(id, 'CONFIRMED', pets, vets));
    row.querySelector('[data-action="complete"]')?.addEventListener('click', () => updateApptStatus(id, 'COMPLETED', pets, vets));
    row.querySelector('[data-action="cancel"]')?.addEventListener('click', () => {
      confirmDialog('Cancelar esta consulta?', () => updateApptStatus(id, 'CANCELED', pets, vets));
    });
    row.querySelector('[data-action="edit"]')?.addEventListener('click', () => openAppointmentForm(pets, vets, appt));
    row.querySelector('[data-action="delete"]')?.addEventListener('click', () => {
      confirmDialog('Excluir esta consulta?', async () => {
        try {
          await api(`/api/appointments/${id}`, { method: 'DELETE' });
          toast('Consulta removida.');
          await loadAppointments(pets, vets);
        } catch (err) {
          toast(apiErrorMessage(err), 'error');
        }
      });
    });
  });

  container.appendChild(paginationControls(page, (newPage) => {
    apptState.page = newPage;
    loadAppointments(pets, vets);
  }));
}

async function updateApptStatus(id, status, pets, vets) {
  try {
    await api(`/api/appointments/${id}/status`, { method: 'PATCH', body: { status } });
    toast('Status atualizado.');
    await loadAppointments(pets, vets);
  } catch (err) {
    toast(apiErrorMessage(err), 'error');
  }
}

function openAppointmentForm(pets, vets, appt) {
  const isEdit = !!appt;
  const modal = openModal(isEdit ? 'Reagendar consulta' : 'Nova consulta', `
    <div id="appt-form-error"></div>
    <form id="appt-form">
      <div class="form-row">
        <div class="field">
          <label>Data e hora</label>
          <input name="scheduledAt" type="datetime-local" required value="${toDatetimeLocalValue(appt?.scheduledAt)}" />
        </div>
        <div class="field">
          <label>Duração (min)</label>
          <input name="durationMin" type="number" min="1" value="${appt?.durationMin ?? 30}" />
        </div>
      </div>
      <div class="form-row">
        <div class="field">
          <label>Pet</label>
          <select name="petId" required>
            <option value="">Selecione…</option>
            ${pets.map((p) => `<option value="${p.id}" ${appt?.petId === p.id ? 'selected' : ''}>${escapeHtml(p.name)} (${escapeHtml(p.clientName)})</option>`).join('')}
          </select>
        </div>
        <div class="field">
          <label>Veterinário</label>
          <select name="vetId" required>
            <option value="">Selecione…</option>
            ${vets.map((v) => `<option value="${v.id}" ${appt?.vetId === v.id ? 'selected' : ''}>${escapeHtml(v.name)}</option>`).join('')}
          </select>
        </div>
      </div>
      <div class="field">
        <label>Motivo</label>
        <textarea name="reason">${escapeHtml(appt?.reason || '')}</textarea>
      </div>
      <div class="modal-actions">
        <button type="button" class="btn btn-secondary" id="appt-cancel">Cancelar</button>
        <button type="submit" class="btn btn-primary" id="appt-submit">${isEdit ? 'Salvar' : 'Agendar'}</button>
      </div>
    </form>
  `);

  modal.querySelector('#appt-cancel').onclick = closeModal;
  modal.querySelector('#appt-form').onsubmit = async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const body = {
      scheduledAt: fd.get('scheduledAt'),
      durationMin: fd.get('durationMin') ? Number(fd.get('durationMin')) : null,
      reason: fd.get('reason').trim() || null,
      petId: fd.get('petId'),
      vetId: fd.get('vetId'),
    };
    const btn = modal.querySelector('#appt-submit');
    btn.disabled = true;
    try {
      if (isEdit) {
        await api(`/api/appointments/${appt.id}`, { method: 'PUT', body });
        toast('Consulta atualizada.');
      } else {
        await api('/api/appointments', { method: 'POST', body });
        toast('Consulta agendada.');
      }
      closeModal();
      await loadAppointments(pets, vets);
    } catch (err) {
      formError(modal.querySelector('#appt-form-error'), err);
      btn.disabled = false;
    }
  };
}

/* ======================= View: Medical Records ======================= */

const recordsState = { page: 0, petId: '' };

async function viewMedicalRecords(main) {
  const canWrite = hasRole(ROLES.medicalRecordsWrite);
  const canDelete = hasRole(ROLES.medicalRecordsDelete);
  const pets = await fetchAllPets();

  main.innerHTML = `
    <div class="view-header">
      <div><h2>Prontuários</h2><p>Histórico médico dos pets.</p></div>
      ${canWrite ? '<button class="btn btn-primary hidden" id="new-record-btn">+ Novo prontuário</button>' : ''}
    </div>
    <div class="toolbar">
      <select id="record-pet-filter">
        <option value="">Selecione um pet…</option>
        ${pets.map((p) => `<option value="${p.id}">${escapeHtml(p.name)} (${escapeHtml(p.clientName)})</option>`).join('')}
      </select>
    </div>
    <div id="record-table"><div class="empty-state">Selecione um pet para ver o histórico de prontuários.</div></div>
  `;

  const newBtn = document.getElementById('new-record-btn');

  document.getElementById('record-pet-filter').onchange = async (e) => {
    recordsState.petId = e.target.value;
    recordsState.page = 0;
    if (newBtn) newBtn.classList.toggle('hidden', !recordsState.petId);
    if (recordsState.petId) {
      await loadMedicalRecords(pets);
    } else {
      document.getElementById('record-table').innerHTML = '<div class="empty-state">Selecione um pet para ver o histórico de prontuários.</div>';
    }
  };

  if (canWrite && newBtn) {
    newBtn.onclick = () => openMedicalRecordForm(pets, recordsState.petId);
  }
}

async function loadMedicalRecords(pets) {
  const container = document.getElementById('record-table');
  const page = await api('/api/medical-records', { params: { petId: recordsState.petId, page: recordsState.page } });
  const canWrite = hasRole(ROLES.medicalRecordsWrite);
  const canDelete = hasRole(ROLES.medicalRecordsDelete);
  const showActions = canWrite || canDelete;

  if (page.content.length === 0) {
    container.innerHTML = '<div class="empty-state">Nenhum prontuário registrado para este pet.</div>';
    return;
  }

  container.innerHTML = `
    <table>
      <thead><tr><th>Data</th><th>Diagnóstico</th><th>Peso (kg)</th><th>Vacinas</th><th>Prescrições</th>${showActions ? '<th>Ações</th>' : ''}</tr></thead>
      <tbody>
        ${page.content.map((r) => `
          <tr data-id="${r.id}">
            <td>${fmtDate(r.createdAt)}</td>
            <td>${escapeHtml(r.diagnosis || '—')}</td>
            <td>${r.weightKg ?? '—'}</td>
            <td>${escapeHtml(r.vaccines || '—')}</td>
            <td>${escapeHtml(r.prescriptions || '—')}</td>
            ${showActions ? `<td class="row-actions">
              ${canWrite ? '<button class="btn btn-secondary btn-sm" data-action="edit">Editar</button>' : ''}
              ${canDelete ? '<button class="btn btn-danger btn-sm" data-action="delete">Excluir</button>' : ''}
            </td>` : ''}
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;

  container.querySelectorAll('tr[data-id]').forEach((row) => {
    const id = row.dataset.id;
    const record = page.content.find((r) => r.id === id);
    row.querySelector('[data-action="edit"]')?.addEventListener('click', () => openMedicalRecordForm(pets, recordsState.petId, record));
    row.querySelector('[data-action="delete"]')?.addEventListener('click', () => {
      confirmDialog('Excluir este prontuário?', async () => {
        try {
          await api(`/api/medical-records/${id}`, { method: 'DELETE' });
          toast('Prontuário removido.');
          await loadMedicalRecords(pets);
        } catch (err) {
          toast(apiErrorMessage(err), 'error');
        }
      });
    });
  });

  container.appendChild(paginationControls(page, (newPage) => {
    recordsState.page = newPage;
    loadMedicalRecords(pets);
  }));
}

async function openMedicalRecordForm(pets, petId, record) {
  const isEdit = !!record;
  const modal = openModal(isEdit ? 'Editar prontuário' : 'Novo prontuário', `<p class="text-dim">Carregando consultas concluídas…</p>`);

  let completedAppts = [];
  try {
    const page = await api('/api/appointments', { params: { petId, size: 200 } });
    completedAppts = page.content.filter((a) => a.status === 'COMPLETED');
  } catch (err) {
    modal.innerHTML = `<h3>${isEdit ? 'Editar' : 'Novo'} prontuário</h3>`;
    formError(modal, err);
    return;
  }

  if (completedAppts.length === 0 && !isEdit) {
    modal.innerHTML = `
      <h3>Novo prontuário</h3>
      <div class="error-box">Este pet não possui nenhuma consulta concluída. É preciso concluir uma consulta antes de registrar um prontuário.</div>
      <div class="modal-actions"><button class="btn btn-secondary" id="record-close">Fechar</button></div>
    `;
    modal.querySelector('#record-close').onclick = closeModal;
    return;
  }

  const pet = pets.find((p) => p.id === petId);

  modal.innerHTML = `
    <h3>${isEdit ? 'Editar prontuário' : 'Novo prontuário'}</h3>
    <div id="record-form-error"></div>
    <form id="record-form">
      <div class="field">
        <label>Pet</label>
        <input value="${escapeHtml(pet?.name || record?.petName || '')}" disabled />
      </div>
      <div class="field">
        <label>Consulta concluída</label>
        <select name="appointmentId" required>
          <option value="">Selecione…</option>
          ${completedAppts.map((a) => `<option value="${a.id}" ${record?.appointmentId === a.id ? 'selected' : ''}>${fmtDate(a.scheduledAt)} — ${escapeHtml(a.vetName)}</option>`).join('')}
          ${record && !completedAppts.find((a) => a.id === record.appointmentId) ? `<option value="${record.appointmentId}" selected>Consulta atual</option>` : ''}
        </select>
      </div>
      <div class="field">
        <label>Diagnóstico</label>
        <textarea name="diagnosis">${escapeHtml(record?.diagnosis || '')}</textarea>
      </div>
      <div class="form-row">
        <div class="field">
          <label>Peso (kg)</label>
          <input name="weightKg" type="number" step="0.1" min="0" value="${record?.weightKg ?? ''}" />
        </div>
        <div class="field">
          <label>Vacinas</label>
          <input name="vaccines" value="${escapeHtml(record?.vaccines || '')}" />
        </div>
      </div>
      <div class="field">
        <label>Prescrições</label>
        <textarea name="prescriptions">${escapeHtml(record?.prescriptions || '')}</textarea>
      </div>
      <div class="field">
        <label>Observações</label>
        <textarea name="notes">${escapeHtml(record?.notes || '')}</textarea>
      </div>
      <div class="modal-actions">
        <button type="button" class="btn btn-secondary" id="record-cancel">Cancelar</button>
        <button type="submit" class="btn btn-primary" id="record-submit">${isEdit ? 'Salvar' : 'Registrar'}</button>
      </div>
    </form>
  `;

  modal.querySelector('#record-cancel').onclick = closeModal;
  modal.querySelector('#record-form').onsubmit = async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const body = {
      appointmentId: fd.get('appointmentId'),
      diagnosis: fd.get('diagnosis').trim() || null,
      notes: fd.get('notes').trim() || null,
      weightKg: fd.get('weightKg') ? Number(fd.get('weightKg')) : null,
      vaccines: fd.get('vaccines').trim() || null,
      prescriptions: fd.get('prescriptions').trim() || null,
    };
    const btn = modal.querySelector('#record-submit');
    btn.disabled = true;
    try {
      if (isEdit) {
        await api(`/api/medical-records/${record.id}`, { method: 'PUT', body });
        toast('Prontuário atualizado.');
      } else {
        await api('/api/medical-records', { method: 'POST', body });
        toast('Prontuário registrado.');
      }
      closeModal();
      await loadMedicalRecords(pets);
    } catch (err) {
      formError(modal.querySelector('#record-form-error'), err);
      btn.disabled = false;
    }
  };
}

/* ======================= View: Services ======================= */

let servicesOnlyActive = false;

async function viewServices(main) {
  const canWrite = hasRole(ROLES.servicesWrite);
  main.innerHTML = `
    <div class="view-header">
      <div><h2>Serviços</h2><p>Catálogo de serviços da clínica.</p></div>
      ${canWrite ? '<button class="btn btn-primary" id="new-service-btn">+ Novo serviço</button>' : ''}
    </div>
    <div class="toolbar">
      <label style="display:flex;align-items:center;gap:6px;color:var(--text-dim);font-size:13px;">
        <input type="checkbox" id="only-active-toggle" ${servicesOnlyActive ? 'checked' : ''} /> Somente ativos
      </label>
    </div>
    <div id="services-table"></div>
  `;

  document.getElementById('only-active-toggle').onchange = (e) => {
    servicesOnlyActive = e.target.checked;
    loadServices();
  };

  if (canWrite) document.getElementById('new-service-btn').onclick = () => openServiceForm();
  await loadServices();
}

async function loadServices() {
  const container = document.getElementById('services-table');
  const services = await api('/api/services', { params: { onlyActive: servicesOnlyActive } });
  const canWrite = hasRole(ROLES.servicesWrite);

  if (services.length === 0) {
    container.innerHTML = '<div class="empty-state">Nenhum serviço cadastrado.</div>';
    return;
  }

  container.innerHTML = `
    <table>
      <thead><tr><th>Nome</th><th>Descrição</th><th>Preço</th><th>Duração</th><th>Ativo</th>${canWrite ? '<th>Ações</th>' : ''}</tr></thead>
      <tbody>
        ${services.map((s) => `
          <tr data-id="${s.id}">
            <td>${escapeHtml(s.name)}</td>
            <td>${escapeHtml(s.description || '—')}</td>
            <td>${fmtMoney(s.priceCents)}</td>
            <td>${s.durationMin ? s.durationMin + ' min' : '—'}</td>
            <td>${s.active ? 'Sim' : 'Não'}</td>
            ${canWrite ? `<td class="row-actions">
              <button class="btn btn-secondary btn-sm" data-action="edit">Editar</button>
              <button class="btn btn-danger btn-sm" data-action="delete">Excluir</button>
            </td>` : ''}
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;

  if (canWrite) {
    container.querySelectorAll('tr[data-id]').forEach((row) => {
      const id = row.dataset.id;
      const service = services.find((s) => s.id === id);
      row.querySelector('[data-action="edit"]')?.addEventListener('click', () => openServiceForm(service));
      row.querySelector('[data-action="delete"]')?.addEventListener('click', () => {
        confirmDialog(`Excluir o serviço <strong>${escapeHtml(service.name)}</strong>?`, async () => {
          try {
            await api(`/api/services/${id}`, { method: 'DELETE' });
            toast('Serviço removido.');
            await loadServices();
          } catch (err) {
            toast(apiErrorMessage(err), 'error');
          }
        });
      });
    });
  }
}

function openServiceForm(service) {
  const isEdit = !!service;
  const priceReais = service ? (service.priceCents / 100).toFixed(2) : '';
  const modal = openModal(isEdit ? 'Editar serviço' : 'Novo serviço', `
    <div id="service-form-error"></div>
    <form id="service-form">
      <div class="field">
        <label>Nome</label>
        <input name="name" required value="${escapeHtml(service?.name || '')}" />
      </div>
      <div class="field">
        <label>Descrição</label>
        <textarea name="description">${escapeHtml(service?.description || '')}</textarea>
      </div>
      <div class="form-row">
        <div class="field">
          <label>Preço (R$)</label>
          <input name="price" type="number" step="0.01" min="0" required value="${priceReais}" />
        </div>
        <div class="field">
          <label>Duração (min)</label>
          <input name="durationMin" type="number" min="1" value="${service?.durationMin ?? ''}" />
        </div>
      </div>
      <div class="field">
        <label style="display:flex;align-items:center;gap:6px;">
          <input type="checkbox" name="active" ${service ? (service.active ? 'checked' : '') : 'checked'} /> Ativo
        </label>
      </div>
      <div class="modal-actions">
        <button type="button" class="btn btn-secondary" id="service-cancel">Cancelar</button>
        <button type="submit" class="btn btn-primary" id="service-submit">${isEdit ? 'Salvar' : 'Cadastrar'}</button>
      </div>
    </form>
  `);

  modal.querySelector('#service-cancel').onclick = closeModal;
  modal.querySelector('#service-form').onsubmit = async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const body = {
      name: fd.get('name').trim(),
      description: fd.get('description').trim() || null,
      priceCents: Math.round(Number(fd.get('price')) * 100),
      durationMin: fd.get('durationMin') ? Number(fd.get('durationMin')) : null,
      active: fd.get('active') === 'on',
    };
    const btn = modal.querySelector('#service-submit');
    btn.disabled = true;
    try {
      if (isEdit) {
        await api(`/api/services/${service.id}`, { method: 'PUT', body });
        toast('Serviço atualizado.');
      } else {
        await api('/api/services', { method: 'POST', body });
        toast('Serviço cadastrado.');
      }
      closeModal();
      await loadServices();
    } catch (err) {
      formError(modal.querySelector('#service-form-error'), err);
      btn.disabled = false;
    }
  };
}

/* ======================= View: Users (ADMIN only) ======================= */

async function viewUsers(main) {
  main.innerHTML = `
    <div class="view-header">
      <div><h2>Funcionários</h2><p>Contas de acesso da equipe da clínica.</p></div>
      <button class="btn btn-primary" id="new-user-btn">+ Novo funcionário</button>
    </div>
    <div id="users-table"></div>
  `;
  document.getElementById('new-user-btn').onclick = () => openUserForm();
  await loadUsers();
}

async function loadUsers() {
  const container = document.getElementById('users-table');
  const users = await api('/api/users');
  const me = Store.getUser();

  container.innerHTML = `
    <table>
      <thead><tr><th>Nome</th><th>Email</th><th>Papel</th><th>Ações</th></tr></thead>
      <tbody>
        ${users.map((u) => `
          <tr data-id="${u.id}">
            <td>${escapeHtml(u.name)}</td>
            <td>${escapeHtml(u.email)}</td>
            <td><span class="role role-${u.role}">${u.role}</span></td>
            <td class="row-actions">
              <button class="btn btn-secondary btn-sm" data-action="edit">Editar</button>
              <button class="btn btn-danger btn-sm" data-action="delete" ${u.id === me.id ? 'disabled title="Você não pode remover sua própria conta"' : ''}>Excluir</button>
            </td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;

  container.querySelectorAll('tr[data-id]').forEach((row) => {
    const id = row.dataset.id;
    const user = users.find((u) => u.id === id);
    row.querySelector('[data-action="edit"]')?.addEventListener('click', () => openUserForm(user));
    const delBtn = row.querySelector('[data-action="delete"]');
    if (delBtn && !delBtn.disabled) {
      delBtn.addEventListener('click', () => {
        confirmDialog(`Excluir o funcionário <strong>${escapeHtml(user.name)}</strong>?`, async () => {
          try {
            await api(`/api/users/${id}`, { method: 'DELETE' });
            toast('Funcionário removido.');
            await loadUsers();
          } catch (err) {
            toast(apiErrorMessage(err), 'error');
          }
        });
      });
    }
  });
}

function openUserForm(user) {
  const isEdit = !!user;
  const modal = openModal(isEdit ? 'Editar funcionário' : 'Novo funcionário', `
    <div id="user-form-error"></div>
    <form id="user-form">
      <div class="field">
        <label>Nome</label>
        <input name="name" required value="${escapeHtml(user?.name || '')}" />
      </div>
      <div class="field">
        <label>Email</label>
        <input name="email" type="email" required value="${escapeHtml(user?.email || '')}" />
      </div>
      ${!isEdit ? `
      <div class="field">
        <label>Senha</label>
        <input name="password" type="password" minlength="6" required />
        <div class="hint">Mínimo de 6 caracteres.</div>
      </div>` : ''}
      <div class="field">
        <label>Papel</label>
        <select name="role" required>
          <option value="ADMIN" ${user?.role === 'ADMIN' ? 'selected' : ''}>ADMIN</option>
          <option value="VET" ${user?.role === 'VET' ? 'selected' : ''}>VET</option>
          <option value="RECEPTIONIST" ${user?.role === 'RECEPTIONIST' ? 'selected' : ''}>RECEPTIONIST</option>
        </select>
      </div>
      <div class="modal-actions">
        <button type="button" class="btn btn-secondary" id="user-cancel">Cancelar</button>
        <button type="submit" class="btn btn-primary" id="user-submit">${isEdit ? 'Salvar' : 'Cadastrar'}</button>
      </div>
    </form>
  `);

  modal.querySelector('#user-cancel').onclick = closeModal;
  modal.querySelector('#user-form').onsubmit = async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const body = isEdit
      ? { name: fd.get('name').trim(), email: fd.get('email').trim(), role: fd.get('role') }
      : { name: fd.get('name').trim(), email: fd.get('email').trim(), password: fd.get('password'), role: fd.get('role') };
    const btn = modal.querySelector('#user-submit');
    btn.disabled = true;
    try {
      if (isEdit) {
        await api(`/api/users/${user.id}`, { method: 'PUT', body });
        toast('Funcionário atualizado.');
      } else {
        await api('/api/users', { method: 'POST', body });
        toast('Funcionário cadastrado.');
      }
      closeModal();
      await loadUsers();
    } catch (err) {
      formError(modal.querySelector('#user-form-error'), err);
      btn.disabled = false;
    }
  };
}

/* ======================= Auth flow ======================= */

function renderRoot() {
  const loginScreen = document.getElementById('login-screen');
  const appShell = document.getElementById('app-shell');

  if (Store.getToken() && Store.getUser()) {
    loginScreen.classList.add('hidden');
    appShell.classList.remove('hidden');
    router();
  } else {
    appShell.classList.add('hidden');
    loginScreen.classList.remove('hidden');
  }
}

function setupLogin() {
  const form = document.getElementById('login-form');
  const errorBox = document.getElementById('login-error');

  document.querySelectorAll('.demo-creds div[data-email]').forEach((el) => {
    el.addEventListener('click', () => {
      document.getElementById('login-email').value = el.dataset.email;
      document.getElementById('login-password').value = el.dataset.password;
    });
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorBox.innerHTML = '';
    const email = document.getElementById('login-email').value.trim();
    const password = document.getElementById('login-password').value;
    const btn = document.getElementById('login-submit');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span> Entrando…';
    try {
      const data = await api('/api/auth/login', { method: 'POST', body: { email, password } });
      Store.setSession(data.token, data.user);
      location.hash = '#/dashboard';
      renderRoot();
    } catch (err) {
      formError(errorBox, err);
    } finally {
      btn.disabled = false;
      btn.textContent = 'Entrar';
    }
  });
}

function setupLogout() {
  document.getElementById('logout-btn').addEventListener('click', async () => {
    try {
      await api('/api/auth/logout', { method: 'POST' });
    } catch (err) {
      /* mesmo se a revogação no servidor falhar, a sessão local é limpa abaixo */
    }
    Store.clear();
    location.hash = '';
    renderRoot();
  });
}

document.addEventListener('DOMContentLoaded', () => {
  setupLogin();
  setupLogout();
  renderRoot();
});
